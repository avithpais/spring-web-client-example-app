package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.service.PostService;
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
 * REST controller that exposes endpoints to demonstrate the WebClient library.
 *
 * Start the app and call:
 *   GET    http://localhost:8080/api/posts/1
 *   GET    http://localhost:8080/api/posts
 *   POST   http://localhost:8080/api/posts         (JSON body)
 *   PUT    http://localhost:8080/api/posts/1        (JSON body)
 *   PATCH  http://localhost:8080/api/posts/1/title?title=New+Title
 *   DELETE http://localhost:8080/api/posts/1
 *   GET    http://localhost:8080/api/posts/1/duplicate
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    private PostService postService;

    @Autowired
    public void setPostService(PostService postService) {
        this.postService = postService;
    }

    @GetMapping("/{id}")
    public Mono<Post> getPost(@PathVariable long id) {
        return postService.getPost(id);
    }

    @GetMapping
    public Mono<String> getAllPosts() {
        return postService.getAllPostsRaw();
    }

    @PostMapping
    public Mono<Post> createPost(@RequestBody Post post) {
        return postService.createPost(post);
    }

    @PutMapping("/{id}")
    public Mono<Post> updatePost(@PathVariable long id, @RequestBody Post post) {
        return postService.updatePost(id, post);
    }

    @PatchMapping("/{id}/title")
    public Mono<Post> patchTitle(@PathVariable long id,
                                 @RequestParam String title) {
        return postService.patchPostTitle(id, title);
    }

    @DeleteMapping("/{id}")
    public Mono<String> deletePost(@PathVariable long id) {
        return postService.deletePost(id);
    }

    @GetMapping("/{id}/duplicate")
    public Mono<Post> duplicatePost(@PathVariable long id) {
        return postService.fetchAndDuplicate(id);
    }
}
