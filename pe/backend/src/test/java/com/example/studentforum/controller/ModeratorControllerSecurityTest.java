package com.example.studentforum.controller;

import com.example.studentforum.config.SecurityConfiguration;
import com.example.studentforum.service.PostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ModeratorController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = {
        "app.moderator.username=moderator",
        "app.moderator.password=unit-test-strong-password",
        "app.student.username=student",
        "app.student.password=unit-test-student-password"
})
class ModeratorControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PostService postService;

    @Test
    void moderationQueueRejectsRequestsWithoutModeratorCredentials() throws Exception {
        mockMvc.perform(get("/api/moderation/queue"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(postService);
    }

    @Test
    void moderationQueueAllowsAuthenticatedModerator() throws Exception {
        when(postService.findPending()).thenReturn(java.util.List.of());

        mockMvc.perform(get("/api/moderation/queue")
                        .with(httpBasic("moderator", "unit-test-strong-password")))
                .andExpect(status().isOk());
    }

    @Test
    void moderationQueueRejectsStudentRole() throws Exception {
        mockMvc.perform(get("/api/moderation/queue")
                        .with(httpBasic("student", "unit-test-student-password")))
                .andExpect(status().isForbidden());

        verifyNoInteractions(postService);
    }
}
