package com.example.studentforum.controller;

import com.example.studentforum.entity.Post;
import com.example.studentforum.entity.PostStatus;
import com.example.studentforum.service.PostService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequestMapping("/api/moderation/queue")
public class ModeratorController {

    private final PostService postService;

    public ModeratorController(PostService postService) {
        this.postService = postService;
    }

    @GetMapping
    public List<Post> findPending() {
        return postService.findPending();
    }

    @GetMapping("/statistics")
    public PostService.ModerationStatistics statistics() {
        return postService.statistics();
    }

    @PostMapping("/{id}/publish")
    public Post publish(
            @PathVariable Long id,
            @Valid @RequestBody ModerationDecisionRequest request,
            Authentication authentication
    ) {
        return postService.decide(id, PostStatus.PUBLISHED, request.reason(), authentication.getName());
    }

    @PostMapping("/{id}/reject")
    public Post reject(
            @PathVariable Long id,
            @Valid @RequestBody ModerationDecisionRequest request,
            Authentication authentication
    ) {
        return postService.decide(id, PostStatus.REJECTED, request.reason(), authentication.getName());
    }
}
