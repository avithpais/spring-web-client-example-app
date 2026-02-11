package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.service.RawWebClientPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Demonstrates using the library's raw {@link org.springframework.web.reactive.function.client.WebClient}
 * bean directly — same TLS/pool infrastructure, no wrapper classes.
 * <p>
 * Start the app and call:
 *   GET    http://localhost:8080/api/raw/webclient/posts/1
 *   GET    http://localhost:8080/api/raw/webclient/posts
 *   POST   http://localhost:8080/api/raw/webclient/posts         (JSON body)
 *   PUT    http://localhost:8080/api/raw/webclient/posts/1        (JSON body)
 *   PATCH  http://localhost:8080/api/raw/webclient/posts/1/title?title=New+Title
 *   DELETE http://localhost:8080/api/raw/webclient/posts/1
 *   GET    http://localhost:8080/api/raw/webclient/posts/1/duplicate
 */
@RestController
@RequestMapping("/api/raw/webclient/posts")
public class RawWebClientPostController {

    private RawWebClientPostService rawWebClientPostService;

    @Autowired
    public void setRawWebClientPostService(RawWebClientPostService rawWebClientPostService) {
        this.rawWebClientPostService = rawWebClientPostService;
    }

    @GetMapping("/{id}")
    public Mono<Post> getPost(@PathVariable long id) {
        return rawWebClientPostService.getPost(id);
    }

    @GetMapping
    public Mono<String> getAllPosts() {
        return rawWebClientPostService.getAllPostsRaw();
    }

    @PostMapping
    public Mono<Post> createPost(@RequestBody Post post) {
        return rawWebClientPostService.createPost(post);
    }

    @PutMapping("/{id}")
    public Mono<Post> updatePost(@PathVariable long id, @RequestBody Post post) {
        return rawWebClientPostService.updatePost(id, post);
    }

    @PatchMapping("/{id}/title")
    public Mono<Post> patchTitle(@PathVariable long id, @RequestParam String title) {
        return rawWebClientPostService.patchPostTitle(id, title);
    }

    @DeleteMapping("/{id}")
    public Mono<String> deletePost(@PathVariable long id) {
        return rawWebClientPostService.deletePost(id);
    }

    @GetMapping("/{id}/duplicate")
    public Mono<Post> duplicatePost(@PathVariable long id) {
        return rawWebClientPostService.fetchAndDuplicate(id);
    }
}
