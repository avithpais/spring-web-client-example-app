# spring-web-client-example-app

A Spring Boot WebFlux application that demonstrates how to use `spring-web-client` to call downstream REST APIs with per-request filter selection, per-request timeout/retry overrides, bearer token injection with `StampedLock`, and reactive composition.

## Requirements

- Java 17+
- Spring Boot 4.0+
- `spring-web-client` JAR installed in your local Maven repository

## Setup

1. Build and install the library first:

```bash
cd ../spring-web-client
mvn clean install
```

2. Build and run the example app:

```bash
cd ../spring-web-client-example-app
mvn clean spring-boot:run
```

The app starts on port `8080` and calls [JSONPlaceholder](https://jsonplaceholder.typicode.com) as a fake downstream API.

## Endpoints

| Method | URL | Description |
|---|---|---|
| GET | `/api/posts/{id}` | Fetch a single post (authenticated, 5s timeout) |
| GET | `/api/posts` | Fetch all posts as raw JSON (public, no bearer token) |
| POST | `/api/posts` | Create a post (authenticated, 5 retries, 500ms backoff, 10s timeout) |
| PUT | `/api/posts/{id}` | Full update of a post (authenticated) |
| PATCH | `/api/posts/{id}/title?title=New` | Partial update (authenticated) |
| DELETE | `/api/posts/{id}` | Delete a post (authenticated) |
| GET | `/api/posts/{id}/duplicate` | Fetch + create a copy (chained calls, authenticated) |

## What This Demonstrates

### Per-Request Filter Selection

Filters are **not** auto-applied to every call. Each service injects the filter beans it needs and attaches them per-request via the `WebServiceRequest` builder. This means bearer token injection can be skipped for public endpoints:

```java
@Autowired private BearerTokenFilterFunction bearerTokenFilter;
@Autowired private CorrelationIdFilterFunction correlationIdFilter;
@Autowired private RequestLoggingFilterFunction requestLoggingFilter;

// Authenticated call — includes bearer token
public Mono<Post> getPost(long id) {
    WebServiceRequest<Post> request = WebServiceRequest.<Post>builder()
            .url(BASE_URL + "/posts/" + id)
            .responseType(Post.class)
            .filter(correlationIdFilter)
            .filter(bearerTokenFilter)
            .filter(requestLoggingFilter)
            .timeoutMs(5000)
            .build();
    return serviceClient.execute(request);
}

// Public call — no bearer token, only correlation ID and logging
public Mono<String> getAllPostsRaw() {
    WebServiceRequest<String> request = WebServiceRequest.<String>builder()
            .url(BASE_URL + "/posts")
            .responseType(String.class)
            .filter(correlationIdFilter)
            .filter(requestLoggingFilter)
            .build();
    return serviceClient.execute(request);
}
```

### Per-Request Timeout and Retry Overrides

Different endpoints use different timeout/retry settings:

```java
// Fast lookup — tight 5s timeout, global retry defaults
WebServiceRequest.<Post>builder()
        .url(BASE_URL + "/posts/" + id)
        .responseType(Post.class)
        .timeoutMs(5000)
        .build();

// Write operation — more retries, longer timeout
WebServiceRequest.<Post>builder()
        .url(BASE_URL + "/posts")
        .method(HttpMethod.POST)
        .body(post)
        .responseType(Post.class)
        .maxRetries(5)
        .retryIntervalMs(500)
        .timeoutMs(10000)
        .build();
```

### Bearer Token with StampedLock

`AuthTokenService` implements `BearerTokenProvider` using a three-tier `StampedLock` pattern:

1. **Optimistic read** — lock-free fast path when the token is cached and valid
2. **Read lock** — fallback if the optimistic stamp was invalidated by a concurrent write
3. **Write lock** — acquired only when the token needs refreshing, with read-to-write lock promotion via `tryConvertToWriteLock()`. If promotion fails (other readers present), the read lock is released and a full write lock is acquired. A re-check under the write lock prevents duplicate fetches when multiple threads race to refresh.

The token is cached in `TokenHolder` (a classic Java singleton) and refreshed every 10 seconds (configurable via `auth.token.token-ttl-seconds`).

The auth server call uses `java.net.http.HttpClient` (JDK built-in) rather than the library's `ServiceClient`, to avoid circular dependency — this service provides the token that the library injects into requests.

### Blocking Usage

`BlockingUsageExample` shows how to use the library from a Spring MVC (servlet) context by calling `.block()` on the returned `Mono`. Filters are attached per-request in the same way:

```java
public Post getPostBlocking(long id) {
    WebServiceRequest<Post> request = WebServiceRequest.<Post>builder()
            .url(BASE_URL + "/posts/" + id)
            .responseType(Post.class)
            .filter(correlationIdFilter)
            .filter(bearerTokenFilter)
            .build();
    return serviceClient.execute(request).block();
}
```

### Reactive Composition

`PostService.fetchAndDuplicate()` demonstrates chaining multiple service calls:

```java
return getPost(sourceId)
        .map(original -> { /* transform */ })
        .flatMap(this::createPost);
```

## Configuration

See `src/main/resources/application.properties` for all available settings:

- **Library settings** (`webclient.http.*`) — pool, timeout, retry, SSL
- **Auth token settings** (`auth.token.*`) — auth URL, credentials, TTL

### Auth Token Properties

| Property | Description |
|---|---|
| `auth.token.auth-url` | OAuth/token endpoint URL |
| `auth.token.redirect-url` | Redirect URL for auth flow |
| `auth.token.user-name` | Service account username |
| `auth.token.password` | Service account password |
| `auth.token.app-name` | Application name for auth |
| `auth.token.secret-name` | Secret name for auth |
| `auth.token.vault-config-path` | Vault configuration path |
| `auth.token.token-ttl-seconds` | Token TTL in seconds (default: 10) |

## Project Structure

```
src/main/java/com/example/demo/
├── auth/
│   ├── AuthTokenService.java       # BearerTokenProvider with StampedLock
│   └── TokenHolder.java            # Singleton token cache (volatile fields)
├── config/
│   ├── AuthTokenProperties.java    # @ConfigurationProperties for auth.token.*
│   └── TokenProviderConfig.java    # @EnableConfigurationProperties
├── controller/
│   └── PostController.java         # REST endpoints
├── model/
│   └── Post.java                   # JSONPlaceholder post model
├── service/
│   ├── PostService.java            # Reactive service with per-request filters
│   └── BlockingUsageExample.java   # Blocking (.block()) usage with filters
└── DemoApplication.java            # Spring Boot entry point

src/test/java/com/example/demo/
└── auth/
    └── AuthTokenServiceTest.java   # 8 tests: caching, expiry, concurrent access, singleton
```

## License

Internal example — see your organization's licensing policy.
