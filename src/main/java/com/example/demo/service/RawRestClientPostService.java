package com.example.demo.service;

import com.example.demo.model.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Demonstrates using the raw {@link RestClient} bean directly — without
 * {@code RestServiceClient} or {@code RestServiceRequest}.
 * <p>
 * The injected {@code RestClient} is auto-configured by the library and
 * shares the same Netty {@code HttpClient}, connection pool, and TLS
 * context as the higher-level wrapper.  You get full infrastructure
 * (pooling, SSL, connection timeouts) but manage interceptors, retries,
 * and per-request timeouts yourself using Spring's native RestClient API.
 */
@Service
public class RawRestClientPostService {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private RestClient restClient;

    @Autowired
    public void setRestClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public Post getPost(long id) {
        return restClient.get()
                .uri(BASE_URL + "/posts/{id}", id)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Post.class);
    }

    public String getAllPostsRaw() {
        return restClient.get()
                .uri(BASE_URL + "/posts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(String.class);
    }

    public Post createPost(Post post) {
        return restClient.post()
                .uri(BASE_URL + "/posts")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(post)
                .retrieve()
                .body(Post.class);
    }

    public Post updatePost(long id, Post post) {
        return restClient.put()
                .uri(BASE_URL + "/posts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(post)
                .retrieve()
                .body(Post.class);
    }

    public Post patchPostTitle(long id, String newTitle) {
        return restClient.patch()
                .uri(BASE_URL + "/posts/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("title", newTitle))
                .retrieve()
                .body(Post.class);
    }

    public String deletePost(long id) {
        return restClient.delete()
                .uri(BASE_URL + "/posts/{id}", id)
                .retrieve()
                .body(String.class);
    }

    public Post fetchAndDuplicate(long sourceId) {
        Post original = getPost(sourceId);
        Post copy = new Post();
        copy.setUserId(original.getUserId());
        copy.setTitle("Copy of: " + original.getTitle());
        copy.setBody(original.getBody());
        return createPost(copy);
    }
}
