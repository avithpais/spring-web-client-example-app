package com.example.demo.client;

import com.example.demo.model.Post;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;

/**
 * Demonstrates different ways to consume Mono<Post> responses from a reactive REST API.
 *
 * When a Spring WebFlux controller returns Mono<Post>, the client has several options
 * for handling the response depending on the application context (reactive vs blocking).
 */
public class PostClientExample {

    private final WebClient webClient;

    public PostClientExample(String baseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    // ============================================================
    // 1. REACTIVE APPROACH - Recommended for WebFlux applications
    // ============================================================

    /**
     * Returns Mono<Post> directly - lets the framework handle subscription.
     * Use this in reactive controllers or when composing with other reactive streams.
     */
    public Mono<Post> getPostReactive(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * Subscribe with callbacks - for fire-and-forget scenarios or side effects.
     */
    public void getPostWithSubscribe(long id) {
        webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .subscribe(
                        post -> System.out.println("Received: " + post),
                        error -> System.err.println("Error: " + error.getMessage()),
                        () -> System.out.println("Completed!")
                );
    }

    /**
     * Transform the response using map() - for synchronous transformations.
     */
    public Mono<String> getPostTitle(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .map(Post::getTitle);
    }

    /**
     * Chain multiple reactive calls using flatMap() - for async operations.
     */
    public Mono<Post> getPostThenUpdate(long id, String newTitle) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .flatMap(post -> {
                    post.setTitle(newTitle);
                    return webClient.put()
                            .uri("/api/posts/{id}", id)
                            .bodyValue(post)
                            .retrieve()
                            .bodyToMono(Post.class);
                });
    }

    // ============================================================
    // 2. BLOCKING APPROACH - For traditional servlet applications
    // ============================================================

    /**
     * Block and wait for the result - ONLY use in servlet/MVC contexts.
     * WARNING: Never use block() inside a reactive pipeline or on Netty threads.
     */
    public Post getPostBlocking(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .block();
    }

    /**
     * Block with timeout - safer version with explicit timeout.
     */
    public Post getPostBlockingWithTimeout(long id, Duration timeout) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .block(timeout);
    }

    // ============================================================
    // 3. ERROR HANDLING PATTERNS
    // ============================================================

    /**
     * Handle errors with fallback value.
     */
    public Mono<Post> getPostWithFallback(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .onErrorReturn(createDefaultPost());
    }

    /**
     * Handle errors with alternative Mono.
     */
    public Mono<Post> getPostWithFallbackMono(long id, long fallbackId) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .onErrorResume(error -> {
                    System.err.println("Primary request failed: " + error.getMessage());
                    return getPostReactive(fallbackId);
                });
    }

    /**
     * Transform specific errors to custom exceptions.
     */
    public Mono<Post> getPostWithErrorMapping(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .onStatus(
                        status -> status.value() == 404,
                        response -> Mono.error(new PostNotFoundException(id))
                )
                .onStatus(
                        status -> status.is5xxServerError(),
                        response -> Mono.error(new ServiceUnavailableException())
                )
                .bodyToMono(Post.class);
    }

    // ============================================================
    // 4. ADVANCED PATTERNS
    // ============================================================

    /**
     * Add timeout to reactive call.
     */
    public Mono<Post> getPostWithTimeout(long id, Duration timeout) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .timeout(timeout);
    }

    /**
     * Retry on failure with exponential backoff.
     */
    public Mono<Post> getPostWithRetry(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofMillis(100)));
    }

    /**
     * Cache the result for subsequent subscribers.
     */
    public Mono<Post> getPostCached(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .cache(Duration.ofMinutes(5));
    }

    /**
     * Execute side effects without changing the stream.
     */
    public Mono<Post> getPostWithLogging(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .doOnSubscribe(sub -> System.out.println("Starting request for post " + id))
                .doOnNext(post -> System.out.println("Received post: " + post.getTitle()))
                .doOnError(error -> System.err.println("Request failed: " + error.getMessage()))
                .doFinally(signal -> System.out.println("Request completed with signal: " + signal));
    }

    // ============================================================
    // HELPER METHODS AND CLASSES
    // ============================================================

    private Post createDefaultPost() {
        Post post = new Post();
        post.setId(-1L);
        post.setTitle("Default Post");
        post.setBody("This is a fallback post");
        post.setUserId(0L);
        return post;
    }

    public static class PostNotFoundException extends RuntimeException {
        public PostNotFoundException(long id) {
            super("Post not found with id: " + id);
        }
    }

    public static class ServiceUnavailableException extends RuntimeException {
        public ServiceUnavailableException() {
            super("Service is temporarily unavailable");
        }
    }

    // ============================================================
    // DEMO MAIN METHOD
    // ============================================================

    public static void main(String[] args) throws InterruptedException {
        // Example: Consuming a local service running on port 8080
        PostClientExample client = new PostClientExample("http://localhost:8080");

        System.out.println("=== Reactive Subscribe Example ===");
        CountDownLatch latch = new CountDownLatch(1);

        client.getPostReactive(1)
                .subscribe(
                        post -> {
                            System.out.println("Post ID: " + post.getId());
                            System.out.println("Title: " + post.getTitle());
                            System.out.println("Body: " + post.getBody());
                        },
                        error -> {
                            System.err.println("Error: " + error.getMessage());
                            latch.countDown();
                        },
                        latch::countDown
                );

        latch.await();

        System.out.println("\n=== Blocking Example ===");
        try {
            Post post = client.getPostBlocking(1);
            System.out.println("Blocking result: " + post);
        } catch (Exception e) {
            System.err.println("Blocking error: " + e.getMessage());
        }
    }
}
