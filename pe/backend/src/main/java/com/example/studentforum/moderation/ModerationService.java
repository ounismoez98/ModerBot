package com.example.studentforum.moderation;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ModerationService {

    private static final int MAX_CACHE_ENTRIES = 1_000;

    private final HuggingFaceClientService huggingFaceClientService;
    private final ConcurrentHashMap<String, ModerationResponse> cache = new ConcurrentHashMap<>();

    public ModerationService(HuggingFaceClientService huggingFaceClientService) {
        this.huggingFaceClientService = huggingFaceClientService;
    }

    public ModerationResponse analyze(String text) {
        validateText(text);
        return cache.computeIfAbsent(text, this::analyzeUncached);
    }

    public List<ModerationResponse> analyzeBatch(List<String> comments) {
        if (comments == null || comments.isEmpty()) {
            throw new ModerationInputException("La liste de commentaires ne peut pas être vide.");
        }
        if (comments.size() > 100) {
            throw new ModerationInputException("Une analyse par lot est limitée à 100 commentaires.");
        }
        comments.forEach(ModerationService::validateText);
        return comments.stream().map(this::analyze).toList();
    }

    public Decision decisionFor(double toxicityScore) {
        if (!Double.isFinite(toxicityScore) || toxicityScore < 0 || toxicityScore > 1) {
            throw new ModerationInputException("Le score de toxicité doit être compris entre 0 et 1.");
        }
        if (toxicityScore < 0.30) {
            return Decision.PUBLIER;
        }
        if (toxicityScore < 0.70) {
            return Decision.A_VERIFIER;
        }
        return Decision.BLOQUER;
    }

    public static void validateText(String text) {
        if (text == null || text.isBlank()) {
            throw new ModerationInputException("Le commentaire ne peut pas être vide.");
        }
        if (text.length() > 10_000) {
            throw new ModerationInputException("Le commentaire ne doit pas dépasser 10 000 caractères.");
        }
    }

    private ModerationResponse analyzeUncached(String text) {
        double scoreToxic = huggingFaceClientService.toxicityScore(text);
        if (!Double.isFinite(scoreToxic) || scoreToxic < 0 || scoreToxic > 1) {
            throw new HuggingFaceUnavailableException("Le score de toxicité reçu est invalide.");
        }

        Map<String, Double> subjectScores = huggingFaceClientService.subjectScores(text);
        String subject = subjectScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElseThrow(() -> new HuggingFaceUnavailableException(
                        "Le modèle de sujet n'a renvoyé aucun score."
                ));

        Decision decision = decisionFor(scoreToxic);
        String explanation = switch (decision) {
            case PUBLIER -> "Score inférieur à 30 % : avis IA favorable. Le modérateur reste décisionnaire.";
            case A_VERIFIER -> "Score compris entre 30 % et 70 % : une vérification humaine est recommandée.";
            case BLOQUER -> "Score supérieur ou égal à 70 % : avis IA défavorable. Le modérateur décide du rejet ou de la publication.";
        };
        ModerationResponse response = new ModerationResponse(
                text,
                scoreToxic,
                1.0 - scoreToxic,
                subject,
                subjectScores,
                decision,
                null,
                explanation
        );
        if (cache.size() >= MAX_CACHE_ENTRIES) {
            cache.clear();
        }
        return response;
    }
}
