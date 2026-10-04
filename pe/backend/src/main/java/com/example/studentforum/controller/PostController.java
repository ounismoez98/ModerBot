package com.example.studentforum.controller;

import com.example.studentforum.entity.Post;
import com.example.studentforum.entity.PostRequest;
import com.example.studentforum.moderation.ModerationResponse;
import com.example.studentforum.moderation.ModerationService;
import com.example.studentforum.service.PostService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.security.Principal;

@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final ModerationService moderationService;

    public PostController(PostService postService, ModerationService moderationService) {
        this.postService = postService;
        this.moderationService = moderationService;
    }

    @PostMapping
    public ResponseEntity<Post> create(@Valid @RequestBody PostRequest request, Principal principal) {
        ModerationResponse moderation = moderationService.analyze(request.title() + "\n" + request.content());
        PostRequest submittedRequest = new PostRequest(request.title(), request.content(), principal.getName());
        Post post = postService.submit(submittedRequest, moderation);
        return ResponseEntity.created(URI.create("/api/posts/" + post.getId())).body(post);
    }

    @GetMapping
    public List<Post> findAll() {
        return postService.findAll();
    }

    @GetMapping("/{id}")
    public Post findById(@PathVariable Long id) {
        return postService.findById(id);
    }

    @PutMapping("/{id}")
    public Post update(@PathVariable Long id, @Valid @RequestBody PostRequest request) {
        ModerationResponse moderation = moderationService.analyze(request.title() + "\n" + request.content());
        return postService.update(id, request, moderation);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
