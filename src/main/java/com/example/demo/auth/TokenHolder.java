package com.example.demo.auth;

import java.time.Instant;

/**
 * Singleton cache that holds the current bearer token and its expiry time.
 * <p>
 * Thread safety is provided by the caller ({@link AuthTokenService}) via
 * {@link java.util.concurrent.locks.StampedLock}. The {@code volatile} fields
 * guarantee visibility for the optimistic-read path, which does not acquire a
 * real lock.
 * <p>
 * This class intentionally uses the classic singleton pattern (not a Spring
 * bean) so the cached token survives independently of the Spring context
 * lifecycle and is accessible from any thread without injection.
 */
public final class TokenHolder {

    private static final TokenHolder INSTANCE = new TokenHolder();

    private volatile String token;
    private volatile Instant expiresAt = Instant.MIN;

    private TokenHolder() {
    }

    public static TokenHolder getInstance() {
        return INSTANCE;
    }

    public String getToken() {
        return token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    /**
     * Replaces the cached token and its expiry. Must be called under a write
     * lock in {@link AuthTokenService}.
     */
    public void update(String token, Instant expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    /**
     * Clears the cached token. Intended for testing and shutdown hooks.
     */
    public void clear() {
        this.token = null;
        this.expiresAt = Instant.MIN;
    }
}
