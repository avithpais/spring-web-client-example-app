package com.example.demo.auth;

import com.example.demo.config.AuthTokenProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.webclient.lib.auth.BearerTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.StampedLock;

/**
 * Bearer token provider that authenticates against an OAuth/token endpoint and
 * caches the result in {@link TokenHolder}.
 * <p>
 * Thread safety uses {@link StampedLock} with three tiers:
 * <ol>
 *   <li><b>Optimistic read</b> &mdash; lock-free fast path for the common
 *       case where the token is cached and valid.</li>
 *   <li><b>Read lock</b> &mdash; fallback if the optimistic stamp was
 *       invalidated by a concurrent write.</li>
 *   <li><b>Write lock</b> &mdash; acquired only when the token is expired.
 *       The read lock is promoted via
 *       {@link StampedLock#tryConvertToWriteLock(long)}; if promotion fails
 *       (other readers present), the read lock is released and a full write
 *       lock is acquired. A re-check under the write lock prevents duplicate
 *       fetches when multiple threads race to refresh.</li>
 * </ol>
 * <p>
 * The actual HTTP call to the auth server uses {@link java.net.http.HttpClient}
 * (JDK built-in) rather than the library's own {@code ServiceClient}, to
 * avoid circular dependency &mdash; this service <em>provides</em> the token
 * that the library injects into every outgoing request. The call is blocking,
 * which is acceptable because the {@code BearerTokenFilterFunction} calls
 * {@code getToken()} synchronously and the StampedLock ensures only one thread
 * performs the refresh at a time.
 */
@Service
public class AuthTokenService implements BearerTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenService.class);

    private final AuthTokenProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TokenHolder tokenHolder = TokenHolder.getInstance();
    private final StampedLock lock = new StampedLock();

    public AuthTokenService(AuthTokenProperties properties, ObjectMapper objectMapper) {
        this(properties, objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build());
    }

    /** Package-private constructor for unit testing with a custom HttpClient. */
    AuthTokenService(AuthTokenProperties properties,
                     ObjectMapper objectMapper,
                     HttpClient httpClient) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    // ------------------------------------------------------------------ //
    //  BearerTokenProvider
    // ------------------------------------------------------------------ //

    @Override
    public String getToken() {

        // --- 1. Optimistic read (lock-free fast path) -------------------
        long stamp = lock.tryOptimisticRead();
        String cachedToken = tokenHolder.getToken();
        Instant expiresAt = tokenHolder.getExpiresAt();

        if (lock.validate(stamp) && cachedToken != null && Instant.now().isBefore(expiresAt)) {
            log.debug("Returning cached token (optimistic read)");
            return cachedToken;
        }

        // --- 2. Read lock (optimistic read failed or token expired) -----
        stamp = lock.readLock();
        try {
            cachedToken = tokenHolder.getToken();
            expiresAt = tokenHolder.getExpiresAt();

            if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
                log.debug("Returning cached token (read lock)");
                return cachedToken;
            }

            // --- 3. Promote read lock -> write lock ----------------------
            long writeStamp = lock.tryConvertToWriteLock(stamp);
            if (writeStamp != 0L) {
                // Promotion succeeded — we now hold a write lock.
                stamp = writeStamp;
                log.debug("Promoted read lock to write lock");
            } else {
                // Promotion failed (other readers present).
                // Release the read lock and acquire a full write lock.
                lock.unlockRead(stamp);
                stamp = lock.writeLock();
                log.debug("Acquired write lock after failed promotion");

                // Re-check: another thread may have refreshed while we waited.
                cachedToken = tokenHolder.getToken();
                expiresAt = tokenHolder.getExpiresAt();
                if (cachedToken != null && Instant.now().isBefore(expiresAt)) {
                    log.debug("Token already refreshed by another thread");
                    return cachedToken;
                }
            }

            // --- 4. Under write lock — fetch new token ------------------
            log.info("Token expired or absent, fetching from {}", properties.getAuthUrl());
            String newToken = fetchToken();
            Instant newExpiry = Instant.now().plusSeconds(properties.getTokenTtlSeconds());
            tokenHolder.update(newToken, newExpiry);
            log.info("Token refreshed, expires at {}", newExpiry);
            return newToken;

        } finally {
            lock.unlock(stamp);
        }
    }

    // ------------------------------------------------------------------ //
    //  Token fetch (blocking HTTP call)
    // ------------------------------------------------------------------ //

    /**
     * Posts credentials to the auth server and extracts the {@code access_token}
     * from the JSON response.
     * <p>
     * Package-private so it can be stubbed in unit tests.
     */
    String fetchToken() {
        try {
            Map<String, String> requestBody = Map.of(
                    "userName", properties.getUserName(),
                    "password", properties.getPassword(),
                    "appName", properties.getAppName(),
                    "secretName", properties.getSecretName(),
                    "redirectUrl", properties.getRedirectUrl(),
                    "vaultConfigPath", properties.getVaultConfigPath()
            );

            String jsonBody = objectMapper.writeValueAsString(requestBody);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getAuthUrl()))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new AuthTokenException(
                        "Auth server returned HTTP " + response.statusCode()
                                + ": " + response.body());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String accessToken = json.path("access_token").asText(null);

            if (accessToken == null || accessToken.isBlank()) {
                throw new AuthTokenException(
                        "Auth response missing access_token field");
            }

            return accessToken;

        } catch (AuthTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new AuthTokenException("Failed to fetch auth token", e);
        }
    }

    // ------------------------------------------------------------------ //
    //  Exception
    // ------------------------------------------------------------------ //

    public static class AuthTokenException extends RuntimeException {

        public AuthTokenException(String message) {
            super(message);
        }

        public AuthTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
