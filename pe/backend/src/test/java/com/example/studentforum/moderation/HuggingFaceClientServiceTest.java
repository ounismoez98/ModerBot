package com.example.studentforum.moderation;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HuggingFaceClientServiceTest {

    private static final String TOXICITY_URL = "https://hf.example/toxicity";
    private static final String SUBJECT_URL = "https://hf.example/subjects";
    private static final String TOKEN = "hf_test_token";

    @Test
    void addsBearerTokenAndExtractsToxicAndNonToxicScores() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        HuggingFaceClientService client = client(restTemplate, TOKEN);
        server.expect(requestTo(TOXICITY_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andRespond(withSuccess("""
                        [[{"label":"toxic","score":0.87},
                          {"label":"severe_toxic","score":0.24}]]
                        """, MediaType.APPLICATION_JSON));

        assertEquals(0.87, client.toxicityScore("insulting comment"));
        server.verify();
    }

    @Test
    void classifiesCommentAmongConfiguredSubjects() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        HuggingFaceClientService client = client(restTemplate, TOKEN);
        server.expect(requestTo(SUBJECT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer " + TOKEN))
                .andExpect(content().json("""
                        {"inputs":"Réviser pour l'examen",
                         "parameters":{"candidate_labels":["cours","examen","stage","hors sujet"],"multi_label":false}}
                        """))
                .andRespond(withSuccess("""
                        {"sequence":"Réviser pour l'examen",
                         "labels":["examen","cours","stage","hors sujet"],
                         "scores":[0.8,0.1,0.06,0.04]}
                        """, MediaType.APPLICATION_JSON));

        Map<String, Double> scores = client.subjectScores("Réviser pour l'examen");

        assertEquals(0.8, scores.get("examen"));
        server.verify();
    }

    @Test
    void supportsInferenceProviderZeroShotLabelScoreResponse() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        HuggingFaceClientService client = client(restTemplate, TOKEN);
        server.expect(requestTo(SUBJECT_URL))
                .andRespond(withSuccess("""
                        [{"label":"cours","score":0.72},
                         {"label":"examen","score":0.18},
                         {"label":"stage","score":0.08},
                         {"label":"hors sujet","score":0.02}]
                        """, MediaType.APPLICATION_JSON));

        Map<String, Double> scores = client.subjectScores("Cours de mathématiques");

        assertEquals(0.72, scores.get("cours"));
        assertEquals(4, scores.size());
        server.verify();
    }

    @Test
    void doesNotCallHuggingFaceWhenTokenIsMissingOrPlaceholder() {
        HuggingFaceClientService emptyTokenClient = client(new RestTemplate(), " ");
        HuggingFaceClientService placeholderClient = client(new RestTemplate(), "hf_YOUR_DEFAULT_TOKEN_HERE");

        assertThrows(HuggingFaceUnavailableException.class,
                () -> emptyTokenClient.toxicityScore("test"));
        assertThrows(HuggingFaceUnavailableException.class,
                () -> placeholderClient.subjectScores("test"));
    }

    @Test
    void handlesUnauthorizedAndForbiddenTokensExplicitly() {
        for (org.springframework.http.HttpStatus status : new org.springframework.http.HttpStatus[] {
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                org.springframework.http.HttpStatus.FORBIDDEN
        }) {
            RestTemplate restTemplate = new RestTemplate();
            MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
            HuggingFaceClientService client = client(restTemplate, TOKEN);
            server.expect(requestTo(TOXICITY_URL))
                    .andRespond(withStatus(status));

            assertThrows(HuggingFaceAuthenticationException.class,
                    () -> client.toxicityScore("test"));
            server.verify();
        }
    }

    @Test
    void translatesHuggingFaceServerErrorsToUnavailableException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        HuggingFaceClientService client = client(restTemplate, TOKEN);
        server.expect(requestTo(TOXICITY_URL))
                .andRespond(withStatus(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE));

        assertThrows(HuggingFaceUnavailableException.class,
                () -> client.toxicityScore("test"));
        server.verify();
    }

    @Test
    void rejectsToxicityScoresOutsideTheProbabilityRange() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        HuggingFaceClientService client = client(restTemplate, TOKEN);
        server.expect(requestTo(TOXICITY_URL))
                .andRespond(withSuccess("""
                        [{"label":"toxic","score":1.4}]
                        """, MediaType.APPLICATION_JSON));

        assertThrows(HuggingFaceUnavailableException.class,
                () -> client.toxicityScore("test"));
        server.verify();
    }

    private HuggingFaceClientService client(RestTemplate restTemplate, String token) {
        return new HuggingFaceClientService(
                restTemplate,
                new ObjectMapper(),
                token,
                TOXICITY_URL,
                SUBJECT_URL
        );
    }
}
