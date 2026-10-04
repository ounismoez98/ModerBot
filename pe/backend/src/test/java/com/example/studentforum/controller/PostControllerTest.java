package com.example.studentforum.controller;

import com.example.studentforum.entity.PostRequest;
import com.example.studentforum.moderation.Decision;
import com.example.studentforum.moderation.ModerationResponse;
import com.example.studentforum.moderation.ModerationService;
import com.example.studentforum.service.PostService;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PostControllerTest {

    @Test
    void queuesPostsEvenWhenAiRecommendsReviewOrBlock() {
        PostService postService = mock(PostService.class);
        ModerationService moderationService = mock(ModerationService.class);
        PostController controller = new PostController(postService, moderationService);
        PostRequest request = new PostRequest("Title", "Content", "Student");
        ModerationResponse moderation = new ModerationResponse(
                "Title\nContent",
                0.3,
                0.7,
                "cours",
                Map.of("cours", 1.0),
                Decision.A_VERIFIER,
                null,
                null
        );
        when(moderationService.analyze("Title\nContent")).thenReturn(moderation);

        PostRequest authenticatedRequest = new PostRequest(request.title(), request.content(), "student");
        when(postService.submit(authenticatedRequest, moderation))
                .thenReturn(new com.example.studentforum.entity.Post("Title", "Content", "Student"));

        var response = controller.create(request, () -> "student");

        assertEquals(201, response.getStatusCode().value());
        verify(postService).submit(authenticatedRequest, moderation);
    }
}
