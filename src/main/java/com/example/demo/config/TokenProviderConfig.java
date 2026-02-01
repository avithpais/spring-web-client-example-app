package com.example.demo.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Enables binding of {@link AuthTokenProperties} to the {@code auth.token}
 * prefix in {@code application.properties}.
 * <p>
 * The {@link com.example.demo.auth.AuthTokenService} is registered as a
 * {@code @Service} and implements {@link com.webclient.lib.auth.BearerTokenProvider}.
 * The library's auto-configuration detects it and injects it into the
 * {@link com.webclient.lib.auth.BearerTokenFilterFunction}, which adds the
 * Authorization header to every outgoing request automatically.
 */
@Configuration
@EnableConfigurationProperties(AuthTokenProperties.class)
public class TokenProviderConfig {
}
