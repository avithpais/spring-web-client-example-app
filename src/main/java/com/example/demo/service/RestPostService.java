package com.example.demo.service;

import com.example.demo.model.Post;
import com.webclient.lib.auth.BearerTokenInterceptor;
import com.webclient.lib.client.RestServiceClient;
import com.webclient.lib.interceptor.CorrelationIdInterceptor;
import com.webclient.lib.interceptor.RequestLoggingInterceptor;
import com.webclient.lib.model.RestServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Demonstrates using the library's {@link RestServiceClient} (synchronous) to call
 * JSONPlaceholder (https://jsonplaceholder.typicode.com) — a free fake REST API.
 * <p>
 * This is the synchronous counterpart to {@link PostService} (reactive).
 * Each method shows a different HTTP verb, configuration style, and
 * per-request interceptor selection. Interceptors are <b>not</b> applied globally —
 * each request declares which interceptors it needs via the builder's
 * {@code interceptor()} method.
 */
@Service
public class RestPostService {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private RestServiceClient restServiceClient;
    private BearerTokenInterceptor bearerTokenInterceptor;
    private CorrelationIdInterceptor correlationIdInterceptor;
    private RequestLoggingInterceptor requestLoggingInterceptor;

    @Autowired
    public void setRestServiceClient(RestServiceClient restServiceClient) {
        this.restServiceClient = restServiceClient;
    }

    @Autowired
    public void setBearerTokenInterceptor(BearerTokenInterceptor bearerTokenInterceptor) {
        this.bearerTokenInterceptor = bearerTokenInterceptor;
    }

    @Autowired
    public void setCorrelationIdInterceptor(CorrelationIdInterceptor correlationIdInterceptor) {
        this.correlationIdInterceptor = correlationIdInterceptor;
    }

    @Autowired
    public void setRequestLoggingInterceptor(RequestLoggingInterceptor requestLoggingInterceptor) {
        this.requestLoggingInterceptor = requestLoggingInterceptor;
    }

    /**
     * GET a single post by ID (synchronous).
     * Demonstrates: authenticated call with all three interceptors,
     * per-request timeout override (5 seconds instead of global default).
     */
    public Post getPost(long id) {
        RestServiceRequest<Post> spec = RestServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(Post.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(bearerTokenInterceptor)
                .interceptor(requestLoggingInterceptor)
                .timeoutMs(5000)
                .build();

        return restServiceClient.execute(spec);
    }

    /**
     * GET all posts as a JSON string (synchronous).
     * Demonstrates: public call — no bearer token needed, only correlation
     * ID and logging interceptors.
     */
    public String getAllPostsRaw() {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url(BASE_URL + "/posts")
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(String.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(requestLoggingInterceptor)
                .build();

        return restServiceClient.execute(spec);
    }

    /**
     * POST to create a new post (synchronous).
     * Demonstrates: authenticated POST with JSON body, custom headers,
     * per-request retry override (5 retries with 500ms backoff, 10s total timeout).
     */
    public Post createPost(Post post) {
        RestServiceRequest<Post> spec = RestServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts")
                .method(HttpMethod.POST)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(post)
                .header("X-Request-Source", "spring-web-client-example-app")
                .responseType(Post.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(bearerTokenInterceptor)
                .interceptor(requestLoggingInterceptor)
                .maxRetries(5)
                .retryIntervalMs(500)
                .timeoutMs(10000)
                .build();

        return restServiceClient.execute(spec);
    }

    /**
     * PUT to update an existing post (synchronous).
     * Demonstrates: authenticated PUT verb with full replacement body.
     */
    public Post updatePost(long id, Post post) {
        RestServiceRequest<Post> spec = RestServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.PUT)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(post)
                .responseType(Post.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(bearerTokenInterceptor)
                .build();

        return restServiceClient.execute(spec);
    }

    /**
     * PATCH to partially update a post (synchronous).
     * Demonstrates: authenticated PATCH verb with partial body.
     */
    public Post patchPostTitle(long id, String newTitle) {
        RestServiceRequest<Post> spec = RestServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.PATCH)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("title", newTitle))
                .responseType(Post.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(bearerTokenInterceptor)
                .build();

        return restServiceClient.execute(spec);
    }

    /**
     * DELETE a post (synchronous).
     * Demonstrates: authenticated DELETE verb, raw String response.
     */
    public String deletePost(long id) {
        RestServiceRequest<String> spec = RestServiceRequest.<String>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.DELETE)
                .responseType(String.class)
                .interceptor(correlationIdInterceptor)
                .interceptor(bearerTokenInterceptor)
                .build();

        return restServiceClient.execute(spec);
    }

    /**
     * Demonstrates composing multiple calls synchronously.
     * Fetches a post, then creates a modified copy.
     */
    public Post fetchAndDuplicate(long sourceId) {
        Post original = getPost(sourceId);
        Post copy = new Post();
        copy.setUserId(original.getUserId());
        copy.setTitle("Copy of: " + original.getTitle());
        copy.setBody(original.getBody());
        return createPost(copy);
    }
}
