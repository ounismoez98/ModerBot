package com.example.studentforum.controller;

import com.example.studentforum.config.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthenticationController.class)
@Import(SecurityConfiguration.class)
@TestPropertySource(properties = {
        "app.moderator.username=moderator",
        "app.moderator.password=unit-test-strong-password",
        "app.student.username=student",
        "app.student.password=unit-test-student-password"
})
class AuthenticationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginCreatesRestorableStudentSessionAndLogoutInvalidatesIt() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"username":"student","password":"unit-test-student-password"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("student"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("STUDENT"));

        mockMvc.perform(post("/api/auth/logout").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
