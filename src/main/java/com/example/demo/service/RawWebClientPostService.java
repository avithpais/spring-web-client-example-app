package com.example.demo.service;

import com.example.demo.model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Demonstrates using the raw {@link WebClient} bean directly — without
 * {@code WebServiceClient} or {@code WebServiceRequest}.
 * <p>
 * The injected {@code WebClient} is auto-configured by the library and
 * shares the same Netty {@code HttpClient}, connection pool, and TLS
 * context as the higher-level wrapper.  You get full infrastructure
 * (pooling, SSL, connection timeouts) but manage filters, retries, and
 * per-request timeouts yourself using Spring's native WebClient API.
 */
@Service
public class RawWebClientPostService {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private WebClient webClient;

    @Autowired
    public void setWebClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public Mono<Post> getPost(long id) {
        return webClient.get()
                .uri(BASE_URL + "/posts/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(Post.class);
    }

    public Mono<String> getAllPostsRaw() {
        return webClient.get()
                .uri(BASE_URL + "/posts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<Post> createPost(Post post) {
        return webClient.post()
                .uri(BASE_URL + "/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class);
    }

    public Mono<Post> updatePost(long id, Post post) {
        return webClient.put()
                .uri(BASE_URL + "/posts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(post)
                .retrieve()
                .bodyToMono(Post.class);
    }

    public Mono<Post> patchPostTitle(long id, String newTitle) {
        return webClient.patch()
                .uri(BASE_URL + "/posts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(java.util.Map.of("title", newTitle))
                .retrieve()
                .bodyToMono(Post.class);
    }

    public Mono<String> deletePost(long id) {
        return webClient.delete()
                .uri(BASE_URL + "/posts/{id}", id)
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<Post> fetchAndDuplicate(long sourceId) {
        return getPost(sourceId)
                .map(original -> {
                    Post copy = new Post();
                    copy.setUserId(original.getUserId());
                    copy.setTitle("Copy of: " + original.getTitle());
                    copy.setBody(original.getBody());
                    return copy;
                })
                .flatMap(this::createPost);
    }
}
