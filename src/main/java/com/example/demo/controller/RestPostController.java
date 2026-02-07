package com.example.demo.controller;

import com.example.demo.model.Post;
import com.example.demo.service.RestPostService;
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
 * REST controller that exposes endpoints to demonstrate the RestClient library (synchronous).
 * <p>
 * This is the synchronous counterpart to {@link PostController} (reactive).
 * <p>
 * Start the app and call:
 *   GET    http://localhost:8080/api/rest/posts/1
 *   GET    http://localhost:8080/api/rest/posts
 *   POST   http://localhost:8080/api/rest/posts         (JSON body)
 *   PUT    http://localhost:8080/api/rest/posts/1       (JSON body)
 *   PATCH  http://localhost:8080/api/rest/posts/1/title?title=New+Title
 *   DELETE http://localhost:8080/api/rest/posts/1
 *   GET    http://localhost:8080/api/rest/posts/1/duplicate
 */
@RestController
@RequestMapping("/api/rest/posts")
public class RestPostController {

    private RestPostService restPostService;

    @Autowired
    public void setRestPostService(RestPostService restPostService) {
        this.restPostService = restPostService;
    }

    @GetMapping("/{id}")
    public Post getPost(@PathVariable long id) {
        return restPostService.getPost(id);
    }

    @GetMapping
    public String getAllPosts() {
        return restPostService.getAllPostsRaw();
    }

    @PostMapping
    public Post createPost(@RequestBody Post post) {
        return restPostService.createPost(post);
    }

    @PutMapping("/{id}")
    public Post updatePost(@PathVariable long id, @RequestBody Post post) {
        return restPostService.updatePost(id, post);
    }

    @PatchMapping("/{id}/title")
    public Post patchTitle(@PathVariable long id, @RequestParam String title) {
        return restPostService.patchPostTitle(id, title);
    }

    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable long id) {
        return restPostService.deletePost(id);
    }

    @GetMapping("/{id}/duplicate")
    public Post duplicatePost(@PathVariable long id) {
        return restPostService.fetchAndDuplicate(id);
    }
}
