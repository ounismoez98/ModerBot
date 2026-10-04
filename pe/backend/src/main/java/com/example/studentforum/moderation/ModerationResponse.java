package com.example.studentforum.moderation;

import java.util.Map;

public record ModerationResponse(
        String commentaire,
        Double scoreToxic,
        Double scoreNonToxic,
        String sujet,
        Map<String, Double> scoresSujets,
        Decision decision,
        String erreur,
        String explication
) {
}
