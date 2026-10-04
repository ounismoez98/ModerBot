package com.example.studentforum.controller;

import com.example.studentforum.moderation.Decision;
import com.example.studentforum.moderation.ModerationResponse;
import com.example.studentforum.moderation.ModerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ModerationControllerTest {

    @Mock
    private ModerationService moderationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ModerationController(moderationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void analyzesOneCommentAndReturnsDetailedScores() throws Exception {
        when(moderationService.analyze("A comment about exams")).thenReturn(response("A comment about exams"));

        mockMvc.perform(post("/api/moderation/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"A comment about exams"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentaire").value("A comment about exams"))
                .andExpect(jsonPath("$.scoreToxic").value(0.87))
                .andExpect(jsonPath("$.scoreNonToxic").value(0.13))
                .andExpect(jsonPath("$.sujet").value("examen"))
                .andExpect(jsonPath("$.scoresSujets.examen").value(0.8))
                .andExpect(jsonPath("$.decision").value("BLOQUER"))
                .andExpect(jsonPath("$.explication").doesNotExist())
                .andExpect(jsonPath("$.erreur").doesNotExist());

        verify(moderationService).analyze("A comment about exams");
    }

    @Test
    void analyzesCommentBatch() throws Exception {
        when(moderationService.analyzeBatch(List.of("First comment", "Second comment")))
                .thenReturn(List.of(response("First comment"), response("Second comment")));

        mockMvc.perform(post("/api/moderation/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"commentaires":["First comment","Second comment"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].commentaire").value("First comment"))
                .andExpect(jsonPath("$[1].commentaire").value("Second comment"));
    }

    private ModerationResponse response(String text) {
        return new ModerationResponse(
                text,
                0.87,
                0.13,
                "examen",
                Map.of("cours", 0.1, "examen", 0.8, "stage", 0.05, "hors sujet", 0.05),
                Decision.BLOQUER,
                null,
                null
        );
    }
}
