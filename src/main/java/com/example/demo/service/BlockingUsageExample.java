package com.example.demo.service;

import com.example.demo.model.Post;
import com.webclient.lib.auth.BearerTokenFilterFunction;
import com.webclient.lib.client.ServiceClient;
import com.webclient.lib.filter.CorrelationIdFilterFunction;
import com.webclient.lib.model.WebServiceRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

/**
 * Demonstrates using the library in a traditional blocking (non-reactive) style.
 *
 * If your application uses Spring MVC (servlet-based) instead of WebFlux,
 * you can call .block() on the returned Mono to get a synchronous result.
 *
 * NOTE: Never call .block() inside a reactive pipeline or on a Netty event
 * loop thread — it will throw IllegalStateException. Only use .block() in
 * servlet/thread-per-request contexts.
 */
@Service
public class BlockingUsageExample {

    private static final String BASE_URL = "https://jsonplaceholder.typicode.com";

    private ServiceClient serviceClient;
    private BearerTokenFilterFunction bearerTokenFilter;
    private CorrelationIdFilterFunction correlationIdFilter;

    @Autowired
    public void setServiceClient(ServiceClient serviceClient) {
        this.serviceClient = serviceClient;
    }

    @Autowired
    public void setBearerTokenFilter(BearerTokenFilterFunction bearerTokenFilter) {
        this.bearerTokenFilter = bearerTokenFilter;
    }

    @Autowired
    public void setCorrelationIdFilter(CorrelationIdFilterFunction correlationIdFilter) {
        this.correlationIdFilter = correlationIdFilter;
    }

    /**
     * Synchronous GET — blocks the calling thread until the response arrives.
     * Uses bearer token and correlation ID filters.
     */
    public Post getPostBlocking(long id) {
        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts/" + id)
                .method(HttpMethod.GET)
                .acceptType(MediaType.APPLICATION_JSON)
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return serviceClient.execute(spec).block();
    }

    /**
     * Synchronous POST — blocks until the created resource is returned.
     * Uses bearer token and correlation ID filters.
     */
    public Post createPostBlocking(String title, String body, long userId) {
        Post post = new Post();
        post.setTitle(title);
        post.setBody(body);
        post.setUserId(userId);

        WebServiceRequest<Post> spec = WebServiceRequest.<Post>builder()
                .url(BASE_URL + "/posts")
                .method(HttpMethod.POST)
                .contentType(MediaType.APPLICATION_JSON)
                .acceptType(MediaType.APPLICATION_JSON)
                .body(post)
                .responseType(Post.class)
                .filter(correlationIdFilter)
                .filter(bearerTokenFilter)
                .build();

        return serviceClient.execute(spec).block();
    }
}
