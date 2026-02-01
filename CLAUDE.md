# Project Memory — spring-web-client-example-app

## Overview
Spring Boot WebFlux demo application that consumes `spring-web-client` to call JSONPlaceholder REST API. Demonstrates per-request timeout/retry overrides, per-request filter selection, bearer token injection with StampedLock, reactive composition, and blocking usage.

## Environment
- **JDK:** `C:\software\openjdk-25.0.2` (Java 25, compiles to target 17)
- **Maven:** `C:\software\apache-maven-3.9.12`
- **Build command:** `export JAVA_HOME="C:/software/openjdk-25.0.2" && "C:/software/apache-maven-3.9.12/bin/mvn" -f "C:/projects/spring-web-client-example-app/pom.xml" clean test`
- **Prerequisite:** `spring-web-client` must be installed first: `mvn -f "C:/projects/spring-web-client/pom.xml" clean install`
- **Spring Boot:** 4.0.1
- **Java target:** 17

## Build Status (as of 2026-01-31)
- `mvn clean test` — **8 tests, 0 failures, BUILD SUCCESS**

## Key Components

### Per-Request Filter Selection
Filters are NOT auto-registered on the shared WebClient. Each service injects the filter beans it needs and attaches them per-request:
```java
@Autowired private BearerTokenFilterFunction bearerTokenFilter;
@Autowired private CorrelationIdFilterFunction correlationIdFilter;

// Authenticated call — uses bearer token
WebServiceRequest.<Post>builder()
        .url(...)
        .filter(correlationIdFilter)
        .filter(bearerTokenFilter)
        .build();

// Public call — no bearer token needed
WebServiceRequest.<String>builder()
        .url(...)
        .filter(correlationIdFilter)
        .build();
```

### AuthTokenService (implements BearerTokenProvider)
- Uses `StampedLock` with three-tier locking: optimistic read -> read lock -> write lock (with promotion)
- Caches token in `TokenHolder` singleton (classic Java singleton, not Spring bean)
- Token TTL configurable via `auth.token.token-ttl-seconds` (default 10s)
- Uses `java.net.http.HttpClient` (JDK built-in) for auth server calls to avoid circular dependency
- Package-private `fetchToken()` method for test stubbing

### PostService
- Demonstrates all HTTP verbs (GET, POST, PUT, PATCH, DELETE)
- Uses `ServiceClient` and `WebServiceRequest` with per-request filter selection
- Shows per-request overrides: `timeoutMs(5000)`, `maxRetries(5)`, `retryIntervalMs(500)`
- Shows authenticated vs public calls (with/without `bearerTokenFilter`)
- `fetchAndDuplicate()` demonstrates reactive chaining with `flatMap`

### BlockingUsageExample
- Shows `.block()` usage for servlet/thread-per-request contexts
- Demonstrates per-request filter attachment in blocking style

### Configuration
- Library config: `webclient.http.*` prefix
- Auth config: `auth.token.*` prefix (authUrl, redirectUrl, userName, password, appName, secretName, vaultConfigPath, tokenTtlSeconds)
- `TokenProviderConfig` — just `@EnableConfigurationProperties(AuthTokenProperties.class)`

### Source Files (9 source, 1 test)
```
src/main/java/com/example/demo/
├── auth/AuthTokenService.java, TokenHolder.java
├── config/AuthTokenProperties.java, TokenProviderConfig.java
├── controller/PostController.java
├── model/Post.java
├── service/PostService.java, BlockingUsageExample.java
└── DemoApplication.java

src/test/java/com/example/demo/
└── auth/AuthTokenServiceTest.java (8 tests: caching, expiry, concurrent access, singleton)
```

## Dependencies
- `com.webclient:spring-web-client:1.0.0` (local)
- `spring-boot-starter-webflux`
- `com.fasterxml.jackson.core:jackson-databind`
- `spring-boot-starter-test` (test)
- `reactor-test` (test)

## History
- Originally lived inside `C:\projects\web-client\example-app\` — moved to `C:\projects\spring-web-client-example-app\` as a standalone Maven project
- Old class names `HttpRequestSpec`/`ReactiveHttpClient`/`ReactiveHttpClientImpl` renamed to `WebServiceRequest`/`ServiceClient`/`WebServiceClient`
- Module renamed from `example-app` (artifactId `demo-app`) to `spring-web-client-example-app`
- Old `CachingBearerTokenProvider` inner class in `TokenProviderConfig` was replaced by `AuthTokenService`
- Virtual threads / Scheduler support was removed from the library
- Filters moved from auto-registered on shared WebClient to per-request via `WebServiceRequest.builder().filter()`
