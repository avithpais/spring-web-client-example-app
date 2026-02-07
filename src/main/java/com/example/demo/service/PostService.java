package com.example.demo.service;

import com.example.demo.model.Post;
import com.webclient.lib.auth.BearerTokenFilterFunction;
import com.webclient.lib.client.WebServiceClient;
import com.webclient.lib.filter.CorrelationIdFilterFunction;
import com.webclient.lib.filter.RequestLoggingFilterFunction;
import com.webclient.lib.model.WebServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Demonstrates using the library's {@link WebServiceClient} to call
 * JSONPlaceholder (https://jsonplaceholder.typicode.com) — a free fake REST API.
 * <p>
 * Each method shows a different HTTP verb, configuration style, and
 * per-request filter selection.  Filters are <b>not</b> applied globally —
 * each request declares which filters it needs via the builder's
 * {@code filter()} method.
 */
@Service
public class PostService {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private WebServiceClient webServiceClient;
    private BearerTokenFilterFunction bearerTokenFilter;
    private CorrelationIdFilterFunction correlationIdFilter;
    private RequestLoggingFilterFunction requestLoggingFilter;

    @Autowired
    public void setWebServiceClient(WebServiceClient webServiceClient) {
        this.webServiceClient = webServiceClient;
    }

    @Autowired
    public void setBearerTokenFilter(BearerTokenFilterFunction bearerTokenFilter) {
        this.bearerTokenFilter = bearerTokenFilter;
    }

    @Autowired
    public void setCorrelationIdFilter(CorrelationIdFilterFunction correlationIdFilter) {
        this.correlationIdFilter = correlationIdFilter;
    }

    @Autowired
    public void setRequestLoggingFilter(RequestLoggingFilterFunction requestLoggingFilter) {
        this.requestLoggingFilter = requestLoggingFilter;
    }

    /**
     * GET a single post by ID.
     * Demonstrates: authenticated call with all three filters,
     * per-request timeout override (5 seconds instead of global default).
     */
    public Mono<Post> getPost(long id) {
        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .filter(requestLoggingFilter)
                .timeoutMs(5000)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * GET all posts as a JSON string.
     * Demonstrates: public call — no bearer token needed, only correlation
     * ID and logging filters.
     */
    public Mono<String> getAllPostsRaw() {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url(BASE_URL + "/posts")
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(String.class)
                .filter(correlationIdFilter)
                .filter(requestLoggingFilter)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * POST to create a new post.
     * Demonstrates: authenticated POST with JSON body, custom headers,
     * per-request retry override (5 retries with 500ms backoff, 10s total timeout).
     */
    public Mono<Post> createPost(Post post) {
        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts")
                .method(HttpMethod.POST)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(post)
                .header("X-Request-Source", "spring-web-client-example-app")
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .filter(requestLoggingFilter)
                .maxRetries(5)
                .retryIntervalMs(500)
                .timeoutMs(10000)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * PUT to update an existing post.
     * Demonstrates: authenticated PUT verb with full replacement body.
     */
    public Mono<Post> updatePost(long id, Post post) {
        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.PUT)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(post)
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * PATCH to partially update a post.
     * Demonstrates: authenticated PATCH verb with partial body.
     */
    public Mono<Post> patchPostTitle(long id, String newTitle) {
        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.PATCH)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(java.util.Map.of("title", newTitle))
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * DELETE a post.
     * Demonstrates: authenticated DELETE verb, raw String response.
     */
    public Mono<String> deletePost(long id) {
        WebServiceRequest<String> spec = WebServiceRequest.<String>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.DELETE)
                .responseType(String.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * GET with custom headers — no bearer token filter.
     * Demonstrates: public call with manual Authorization header override,
     * correlation ID and logging only.
     */
    public Mono<Post> getPostWithCustomHeaders(long id) {
        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .header("X-Tenant-Id", "tenant-42")
                .header("Authorization", "Bearer per-request-override-token")
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(requestLoggingFilter)
                .build();

        return webServiceClient.execute(spec);
    }

    /**
     * Demonstrates composing multiple calls reactively.
     * Fetches a post, then creates a modified copy.
     */
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
