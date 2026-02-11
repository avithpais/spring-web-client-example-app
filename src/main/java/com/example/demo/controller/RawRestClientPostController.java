package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.service.RawRestClientPostService;
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

/**
 * Demonstrates using the library's raw {@link org.springframework.web.client.RestClient}
 * bean directly — same TLS/pool infrastructure, no wrapper classes.
 * <p>
 * Start the app and call:
 *   GET    http://localhost:8080/api/raw/restclient/posts/1
 *   GET    http://localhost:8080/api/raw/restclient/posts
 *   POST   http://localhost:8080/api/raw/restclient/posts         (JSON body)
 *   PUT    http://localhost:8080/api/raw/restclient/posts/1       (JSON body)
 *   PATCH  http://localhost:8080/api/raw/restclient/posts/1/title?title=New+Title
 *   DELETE http://localhost:8080/api/raw/restclient/posts/1
 *   GET    http://localhost:8080/api/raw/restclient/posts/1/duplicate
 */
@RestController
@RequestMapping("/api/raw/restclient/posts")
public class RawRestClientPostController {

    private RawRestClientPostService rawRestClientPostService;

    @Autowired
    public void setRawRestClientPostService(RawRestClientPostService rawRestClientPostService) {
        this.rawRestClientPostService = rawRestClientPostService;
    }

    @GetMapping("/{id}")
    public Post getPost(@PathVariable long id) {
        return rawRestClientPostService.getPost(id);
    }

    @GetMapping
    public String getAllPosts() {
        return rawRestClientPostService.getAllPostsRaw();
    }

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        return rawRestClientPostService.createPost(post);
    }

    @PutMapping("/{id}")
    public Post updatePost(@PathVariable long id, @RequestBody Post post) {
        return rawRestClientPostService.updatePost(id, post);
    }

    @PatchMapping("/{id}/title")
    public Post patchTitle(@PathVariable long id, @RequestParam String title) {
        return rawRestClientPostService.patchPostTitle(id, title);
    }

    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable long id) {
        return rawRestClientPostService.deletePost(id);
    }

    @GetMapping("/{id}/duplicate")
    public Post duplicatePost(@PathVariable long id) {
        return rawRestClientPostService.fetchAndDuplicate(id);
    }
}
