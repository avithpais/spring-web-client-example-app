# Project Memory — spring-web-client-example-app

## Overview
Spring Boot WebFlux demo application that consumes `spring-web-client` to call JSONPlaceholder REST API. Demonstrates per-request timeout/retry overrides, per-request filter/interceptor selection, bearer token injection with StampedLock, reactive composition, blocking usage, and both WebClient (reactive) and RestClient (synchronous) patterns.

## Environment
- **JDK:** `C:\software\openjdk-25.0.2` (Java 25, compiles to target 17)
- **Maven:** `C:\software\apache-maven-3.9.12`
- **Build command:** `export JAVA_HOME="C:/software/openjdk-25.0.2" && "C:/software/apache-maven-3.9.12/bin/mvn" -f "C:/projects/spring-web-client-example-app/pom.xml" clean test`
- **Prerequisite:** `spring-web-client` must be installed first: `mvn -f "C:/projects/spring-web-client/pom.xml" clean install`
- **Spring Boot:** 4.0.1
- **Java target:** 17

## Build Status (as of 2026-02-07)
- `mvn clean test` — **8 tests, 0 failures, BUILD SUCCESS**

## Key Components

### Per-Request Filter/Interceptor Selection
Filters and interceptors are NOT auto-registered. Each service injects the beans it needs and attaches them per-request:

**WebClient (reactive):**
```java
@Autowired private WebServiceClient webServiceClient;
@Autowired private BearerTokenFilterFunction bearerTokenFilter;
@Autowired private CorrelationIdFilterFunction correlationIdFilter;

WebServiceRequest.<Post>builder()
        .url(...)
        .filter(correlationIdFilter)
        .filter(bearerTokenFilter)
        .build();
```

**RestClient (synchronous):**
```java
@Autowired private RestServiceClient restServiceClient;
@Autowired private BearerTokenInterceptor bearerTokenInterceptor;
@Autowired private CorrelationIdInterceptor correlationIdInterceptor;

RestServiceRequest.<Post>builder()
        .url(...)
        .interceptor(correlationIdInterceptor)
        .interceptor(bearerTokenInterceptor)
        .build();
```

### AuthTokenService (implements BearerTokenProvider)
- Uses `StampedLock` with three-tier locking: optimistic read -> read lock -> write lock (with promotion)
- Caches token in `TokenHolder` singleton (classic Java singleton, not Spring bean)
- Token TTL configurable via `auth.token.token-ttl-seconds` (default 10s)
- Uses `java.net.http.HttpClient` (JDK built-in) for auth server calls to avoid circular dependency
- Package-private `fetchToken()` method for test stubbing

### PostService (Reactive)
- Demonstrates all HTTP verbs (GET, POST, PUT, PATCH, DELETE) with `WebServiceClient`
- Uses `WebServiceRequest` with per-request filter selection
- Shows per-request overrides: `timeoutMs(5000)`, `maxRetries(5)`, `retryIntervalMs(500)`
- Shows authenticated vs public calls (with/without `bearerTokenFilter`)
- `fetchAndDuplicate()` demonstrates reactive chaining with `flatMap`

### RestPostService (Synchronous)
- Demonstrates all HTTP verbs with `RestServiceClient`
- Uses `RestServiceRequest` with per-request interceptor selection
- Returns `T` directly instead of `Mono<T>`

### BlockingUsageExample
- Shows `.block()` usage for servlet/thread-per-request contexts
- Demonstrates per-request filter attachment in blocking style

### Configuration
- Library config: `webclient.http.*` prefix
- Auth config: `auth.token.*` prefix (authUrl, redirectUrl, userName, password, appName, secretName, vaultConfigPath, tokenTtlSeconds)
- `TokenProviderConfig` — just `@EnableConfigurationProperties(AuthTokenProperties.class)`

### Source Files (14 source, 1 test)
```
src/main/java/com/example/demo/
├── auth/AuthTokenService.java, TokenHolder.java
├── config/AuthTokenProperties.java, TokenProviderConfig.java
├── controller/PostController.java, RestPostController.java
├── client/PostApiClient.java, PostClientExample.java
├── model/Post.java
├── service/PostService.java, RestPostService.java, BlockingUsageExample.java
└── DemoApplication.java

src/test/java/com/example/demo/
└── auth/AuthTokenServiceTest.java (8 tests)
```

## Dependencies
- `com.webclient:spring-web-client:1.0.0` (local)
- `spring-boot-starter-webflux`
- `com.fasterxml.jackson.core:jackson-databind`
- `spring-boot-starter-test` (test)
- `reactor-test` (test)
