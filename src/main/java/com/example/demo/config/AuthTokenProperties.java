package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the authentication token service.
 * <p>
 * Bound to the {@code auth.token} prefix in {@code application.properties}.
 */
@ConfigurationProperties(prefix = "auth.token")
public class AuthTokenProperties {

    private String authUrl;
    private String redirectUrl;
    private String userName;
    private String password;
    private String appName;
    private String secretName;
    private String vaultConfigPath;
    private long tokenTtlSeconds = 10;

    public String getAuthUrl() {
        return authUrl;
    }

    public void setAuthUrl(String authUrl) {
        this.authUrl = authUrl;
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getSecretName() {
        return secretName;
    }

    public void setSecretName(String secretName) {
        this.secretName = secretName;
    }

    public String getVaultConfigPath() {
        return vaultConfigPath;
    }

    public void setVaultConfigPath(String vaultConfigPath) {
        this.vaultConfigPath = vaultConfigPath;
    }

    public long getTokenTtlSeconds() {
        return tokenTtlSeconds;
    }

    public void setTokenTtlSeconds(long tokenTtlSeconds) {
        this.tokenTtlSeconds = tokenTtlSeconds;
    }
}
