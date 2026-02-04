package com.example.demo.client;

import com.example.demo.model.Post;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring-managed API client for consuming the Post REST API.
 *
 * This demonstrates how another Spring application would typically consume
 * endpoints that return Mono<Post> or Flux<Post>.
 *
 * Usage in a controller or service:
 * <pre>
 * {@code
 * @RestController
 * public class MyController {
 *     private final PostApiClient postApiClient;
 *
 *     public MyController(PostApiClient postApiClient) {
 *         this.postApiClient = postApiClient;
 *     }
 *
 *     @GetMapping("/my-posts/{id}")
 *     public Mono<Post> getPost(@PathVariable long id) {
 *         // Simply return the Mono - Spring WebFlux handles subscription
 *         return postApiClient.getPost(id);
 *     }
 *
 *     @GetMapping("/my-posts/{id}/enriched")
 *     public Mono<EnrichedPost> getEnrichedPost(@PathVariable long id) {
 *         // Transform the response before returning
 *         return postApiClient.getPost(id)
 *                 .map(post -> new EnrichedPost(post, LocalDateTime.now()));
 *     }
 * }
 * }
 * </pre>
 */
@Component
public class PostApiClient {

    private final WebClient webClient;

    public PostApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${post.api.base-url:http://localhost:8080}") String baseUrl) {
        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Get a single post by ID.
     *
     * The caller receives a Mono<Post> and decides how to handle it:
     * - In a WebFlux controller: return it directly
     * - In a reactive pipeline: use map/flatMap for transformations
     * - In blocking code: call .block() (not recommended in reactive apps)
     */
    public Mono<Post> getPost(long id) {
        return webClient.get()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * Get all posts as a Flux (stream of posts).
     */
    public Flux<Post> getAllPosts() {
        return webClient.get()
                .uri("/api/posts")
                .retrieve()
                .bodyToFlux(Post.class);
    }

    /**
     * Create a new post.
     */
    public Mono<Post> createPost(Post post) {
        return webClient.post()
                .uri("/api/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * Update an existing post.
     */
    public Mono<Post> updatePost(long id, Post post) {
        return webClient.put()
                .uri("/api/posts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class);
    }

    /**
     * Delete a post.
     */
    public Mono<Void> deletePost(long id) {
        return webClient.delete()
                .uri("/api/posts/{id}", id)
                .retrieve()
                .bodyToMono(Void.class);
    }
}
