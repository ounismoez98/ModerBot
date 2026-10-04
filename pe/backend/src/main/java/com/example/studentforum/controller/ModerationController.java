package com.example.studentforum.controller;

import com.example.studentforum.moderation.CommentRequest;
import com.example.studentforum.moderation.ModerationResponse;
import com.example.studentforum.moderation.ModerationService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
@RequestMapping("/api/moderation")
public class ModerationController {

    private final ModerationService moderationService;

    public ModerationController(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @PostMapping("/analyze")
    public ModerationResponse analyze(@RequestBody CommentRequest request) {
        return moderationService.analyze(request.text());
    }

    @PostMapping("/batch")
    public List<ModerationResponse> analyzeBatch(@RequestBody CommentRequest request) {
        return moderationService.analyzeBatch(request.commentaires());
    }
}
