package com.example.studentforum.moderation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationServiceTest {

    @Mock
    private HuggingFaceClientService huggingFaceClient;

    private ModerationService moderationService;

    @BeforeEach
    void setUp() {
        moderationService = new ModerationService(huggingFaceClient);
    }

    @Test
    void appliesPublishReviewAndBlockThresholdsExactly() {
        assertEquals(Decision.PUBLIER, moderationService.decisionFor(0.0));
        assertEquals(Decision.PUBLIER, moderationService.decisionFor(0.299999));
        assertEquals(Decision.A_VERIFIER, moderationService.decisionFor(0.30));
        assertEquals(Decision.A_VERIFIER, moderationService.decisionFor(0.699999));
        assertEquals(Decision.BLOQUER, moderationService.decisionFor(0.70));
        assertEquals(Decision.BLOQUER, moderationService.decisionFor(1.0));
    }

    @Test
    void rejectsNonFiniteOrOutOfRangeScores() {
        for (double score : new double[] {-0.1, 1.1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(ModerationInputException.class, () -> moderationService.decisionFor(score));
        }
    }

    @Test
    void returnsExplicitToxicAndNonToxicScoresAndHighestSubject() {
        when(huggingFaceClient.toxicityScore("Commentaire utile")).thenReturn(0.87);
        when(huggingFaceClient.subjectScores("Commentaire utile"))
                .thenReturn(Map.of("cours", 0.1, "examen", 0.8, "stage", 0.05, "hors sujet", 0.05));

        ModerationResponse result = moderationService.analyze("Commentaire utile");

        assertEquals(0.87, result.scoreToxic());
        assertEquals(0.13, result.scoreNonToxic(), 0.000001);
        assertEquals("examen", result.sujet());
        assertEquals(Decision.BLOQUER, result.decision());
        assertEquals(0.8, result.scoresSujets().get("examen"));
    }

    @Test
    void cachesSuccessfulModerationForRepeatedText() {
        when(huggingFaceClient.toxicityScore("Cours de mathématiques")).thenReturn(0.1);
        when(huggingFaceClient.subjectScores("Cours de mathématiques"))
                .thenReturn(Map.of("cours", 0.8, "examen", 0.1, "stage", 0.05, "hors sujet", 0.05));

        ModerationResponse first = moderationService.analyze("Cours de mathématiques");
        ModerationResponse second = moderationService.analyze("Cours de mathématiques");

        assertEquals(first, second);
        verify(huggingFaceClient, times(1)).toxicityScore("Cours de mathématiques");
        verify(huggingFaceClient, times(1)).subjectScores("Cours de mathématiques");
    }

    @Test
    void validatesSingleAndBatchInputBeforeCallingModels() {
        assertThrows(ModerationInputException.class, () -> moderationService.analyze("  "));
        assertThrows(ModerationInputException.class, () -> moderationService.analyze("x".repeat(10_001)));
        assertThrows(ModerationInputException.class, () -> moderationService.analyzeBatch(null));
        assertThrows(ModerationInputException.class, () -> moderationService.analyzeBatch(java.util.List.of()));
        assertThrows(ModerationInputException.class, () -> moderationService.analyzeBatch(
                java.util.Arrays.asList("valid comment", " ")
        ));
        verifyNoInteractions(huggingFaceClient);
    }
}
