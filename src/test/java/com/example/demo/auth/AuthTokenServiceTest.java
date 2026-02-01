package com.example.demo.auth;

import com.example.demo.config.AuthTokenProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthTokenServiceTest {

    private AuthTokenProperties properties;
    private ObjectMapper objectMapper;
    private TokenHolder tokenHolder;

    @BeforeEach
    void setUp() {
        properties = new AuthTokenProperties();
        properties.setAuthUrl("https://auth.example.com/oauth/token");
        properties.setRedirectUrl("https://myapp.example.com/callback");
        properties.setUserName("testUser");
        properties.setPassword("testPass");
        properties.setAppName("testApp");
        properties.setSecretName("testSecret");
        properties.setVaultConfigPath("/vault/test");
        properties.setTokenTtlSeconds(10);

        objectMapper = new ObjectMapper();
        tokenHolder = TokenHolder.getInstance();
        tokenHolder.clear();
    }

    @AfterEach
    void tearDown() {
        tokenHolder.clear();
    }

    @Test
    void getToken_returnsCachedToken_whenValid() {
        // Pre-populate the cache with a valid token
        tokenHolder.update("cached-token-123", Instant.now().plusSeconds(60));

        AuthTokenService service = createServiceWithStubFetch("should-not-be-called");

        String token = service.getToken();
        assertEquals("cached-token-123", token);
    }

    @Test
    void getToken_fetchesNewToken_whenExpired() {
        // Pre-populate with an expired token
        tokenHolder.update("expired-token", Instant.now().minusSeconds(5));

        AuthTokenService service = createServiceWithStubFetch("fresh-token-456");

        String token = service.getToken();
        assertEquals("fresh-token-456", token);
    }

    @Test
    void getToken_fetchesNewToken_whenCacheEmpty() {
        AuthTokenService service = createServiceWithStubFetch("first-token-789");

        String token = service.getToken();
        assertEquals("first-token-789", token);
    }

    @Test
    void getToken_updatesTokenHolder_afterFetch() {
        AuthTokenService service = createServiceWithStubFetch("new-token");

        service.getToken();

        assertEquals("new-token", tokenHolder.getToken());
        assertNotNull(tokenHolder.getExpiresAt());
        // The expiry should be ~10 seconds from now (the configured TTL)
        Instant expectedMin = Instant.now().plusSeconds(8);
        Instant expectedMax = Instant.now().plusSeconds(12);
        Instant actual = tokenHolder.getExpiresAt();
        assert actual.isAfter(expectedMin) && actual.isBefore(expectedMax)
                : "Token expiry " + actual + " not within expected range";
    }

    @Test
    void getToken_throwsAuthTokenException_onFetchFailure() {
        AuthTokenService service = new AuthTokenService(properties, objectMapper, null) {
            @Override
            String fetchToken() {
                throw new AuthTokenService.AuthTokenException("Auth server down");
            }
        };

        assertThrows(AuthTokenService.AuthTokenException.class, service::getToken);
    }

    @Test
    void getToken_concurrentAccess_onlyFetchesOnce() throws Exception {
        AtomicInteger fetchCount = new AtomicInteger(0);

        AuthTokenService service = new AuthTokenService(properties, objectMapper, null) {
            @Override
            String fetchToken() {
                fetchCount.incrementAndGet();
                // Simulate slow auth server
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return "concurrent-token";
            }
        };

        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<String> failureMessage = new AtomicReference<>();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    String token = service.getToken();
                    if (!"concurrent-token".equals(token)) {
                        failureMessage.set("Unexpected token: " + token);
                    }
                } catch (Exception e) {
                    failureMessage.set("Exception: " + e.getMessage());
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads at once
        doneLatch.await();
        executor.shutdown();

        if (failureMessage.get() != null) {
            throw new AssertionError(failureMessage.get());
        }

        // Only 1 thread should have actually called fetchToken()
        assertEquals(1, fetchCount.get(),
                "Expected exactly 1 fetch call, but got " + fetchCount.get());
    }

    @Test
    void tokenHolder_isSingleton() {
        TokenHolder instance1 = TokenHolder.getInstance();
        TokenHolder instance2 = TokenHolder.getInstance();
        assert instance1 == instance2 : "TokenHolder should be a singleton";
    }

    @Test
    void tokenHolder_clearResetsState() {
        tokenHolder.update("some-token", Instant.now().plusSeconds(60));
        tokenHolder.clear();

        assert tokenHolder.getToken() == null : "Token should be null after clear";
        assert tokenHolder.getExpiresAt().equals(Instant.MIN) : "ExpiresAt should be MIN after clear";
    }

    // ------------------------------------------------------------------ //
    //  Helper — creates a service with a stubbed fetchToken()
    // ------------------------------------------------------------------ //

    private AuthTokenService createServiceWithStubFetch(String tokenToReturn) {
        return new AuthTokenService(properties, objectMapper, null) {
            @Override
            String fetchToken() {
                return tokenToReturn;
            }
        };
    }
}
