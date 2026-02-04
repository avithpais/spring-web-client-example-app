package com.example.demo.client;

import com.example.demo.model.Post;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * A guide showing how to work with Mono<T> responses.
 *
 * IMPORTANT CONCEPT:
 * - Mono<T> is a container that WILL hold a value T in the future
 * - You don't "unwrap" it directly — you describe what to do when the value arrives
 * - The code inside map/flatMap only runs when the Mono emits its value
 */
public class MonoUsageGuide {

    private final WebClient webClient = WebClient.create("http://localhost:8080");

    // ================================================================
    // SCENARIO 1: Getting a value and printing it
    // ================================================================

    /**
     * IMPERATIVE WAY (Traditional Java with .block())
     * - Blocks the thread until the HTTP response arrives
     * - Returns a normal Post object you can use directly
     * - Use ONLY in servlet/MVC apps, NEVER in reactive pipelines
     */
    public void printPostImperative(long id) {
        // .block() waits and extracts the Post from Mono<Post>
        Post post = webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .block();  // <-- Thread waits here until response arrives

        // Now we have a regular Post object
        System.out.println("Title: " + post.getTitle());
        System.out.println("Body: " + post.getBody());
    }

    /**
     * REACTIVE WAY (WebFlux style with .subscribe())
     * - Does NOT block — returns immediately
     * - The lambda runs later when the HTTP response arrives
     * - Use in reactive applications
     */
    public void printPostReactive(long id) {
        webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .subscribe(post -> {  // <-- This lambda runs LATER when data arrives
                    System.out.println("Title: " + post.getTitle());
                    System.out.println("Body: " + post.getBody());
                });

        // Code here runs IMMEDIATELY, before the HTTP response arrives!
        System.out.println("Request sent, not waiting...");
    }

    // ================================================================
    // SCENARIO 2: Transform a value (get title in uppercase)
    // ================================================================

    /**
     * IMPERATIVE WAY
     */
    public String getUppercaseTitleImperative(long id) {
        Post post = webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .block();

        return post.getTitle().toUpperCase();
    }

    /**
     * REACTIVE WAY — use map() to transform inside the Mono
     * Returns Mono<String> — the transformation happens when data arrives
     */
    public Mono<String> getUppercaseTitleReactive(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .map(post -> post.getTitle().toUpperCase());  // Transform inside Mono
    }

    // ================================================================
    // SCENARIO 3: Chain multiple operations (get post, then update it)
    // ================================================================

    /**
     * IMPERATIVE WAY — sequential blocking calls
     */
    public Post getAndUpdateTitleImperative(long id, String newTitle) {
        // First call: get the post (blocks)
        Post post = webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .block();

        // Modify it
        post.setTitle(newTitle);

        // Second call: update the post (blocks again)
        Post updated = webClient.put()
                .uri("/api/posts/{id}", id)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class)
                .block();

        return updated;
    }

    /**
     * REACTIVE WAY — use flatMap() to chain async operations
     *
     * flatMap is used when your transformation returns another Mono.
     * - map():     Post -> String           (sync transformation)
     * - flatMap(): Post -> Mono<Post>       (async operation)
     */
    public Mono<Post> getAndUpdateTitleReactive(long id, String newTitle) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .flatMap(post -> {                    // flatMap because we return Mono
                    post.setTitle(newTitle);
                    return webClient.put()
                            .uri("/api/posts/{id}", id)
                            .bodyValue(post)
                            .retrieve()
                            .bodyToMono(Post.class);
                });
    }

    // ================================================================
    // SCENARIO 4: Handle errors
    // ================================================================

    /**
     * IMPERATIVE WAY — try/catch
     */
    public Post getPostWithFallbackImperative(long id) {
        try {
            return webClient.get()
                    .uri("/api/posts/{id}", id)
                    .retrieve()
                    .bodyToMono(Post.class)
                    .block();
        } catch (Exception e) {
            System.err.println("Failed: " + e.getMessage());
            return createDefaultPost();
        }
    }

    /**
     * REACTIVE WAY — onErrorReturn / onErrorResume
     */
    public Mono<Post> getPostWithFallbackReactive(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .doOnError(e -> System.err.println("Failed: " + e.getMessage()))
                .onErrorReturn(createDefaultPost());  // Return default on any error
    }

    // ================================================================
    // SCENARIO 5: Conditional logic
    // ================================================================

    /**
     * IMPERATIVE WAY
     */
    public String describePostImperative(long id) {
        Post post = webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .block();

        if (post.getTitle().length() > 50) {
            return "Long post: " + post.getTitle().substring(0, 50) + "...";
        } else {
            return "Short post: " + post.getTitle();
        }
    }

    /**
     * REACTIVE WAY — logic inside map()
     */
    public Mono<String> describePostReactive(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class)
                .map(post -> {
                    if (post.getTitle().length() > 50) {
                        return "Long post: " + post.getTitle().substring(0, 50) + "...";
                    } else {
                        return "Short post: " + post.getTitle();
                    }
                });
    }

    // ================================================================
    // HELPER
    // ================================================================

    private Post createDefaultPost() {
        Post post = new Post();
        post.setId(-1L);
        post.setTitle("Default");
        post.setBody("Fallback post");
        return post;
    }

    // ================================================================
    // QUICK REFERENCE
    // ================================================================
    /*
     * | Want to...                    | Imperative        | Reactive                    |
     * |-------------------------------|-------------------|-----------------------------|
     * | Get the value                 | .block()          | .subscribe(val -> ...)      |
     * | Transform value               | post.getTitle()   | .map(post -> post.getTitle())|
     * | Chain async calls             | call1(); call2(); | .flatMap(r1 -> call2())     |
     * | Handle errors                 | try/catch         | .onErrorReturn(default)     |
     * | Do side effect (logging)      | System.out.print  | .doOnNext(v -> log(v))      |
     * | Add timeout                   | (thread timeout)  | .timeout(Duration.of(...))  |
     * | Retry on failure              | while loop        | .retry(3)                   |
     *
     * WHEN TO USE WHICH:
     * - Imperative (.block()): Spring MVC apps, CLI tools, tests, anywhere non-reactive
     * - Reactive (Mono/Flux): Spring WebFlux apps, high-concurrency services
     *
     * GOLDEN RULE:
     * - Never call .block() inside a reactive pipeline or on Netty threads
     * - In WebFlux controllers, just return Mono<T> — Spring subscribes for you
     */
}
