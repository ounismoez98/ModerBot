package com.example.studentforum.moderation;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class HuggingFaceClientService {

    private static final List<String> SUBJECTS = List.of("cours", "examen", "stage", "hors sujet");
    private static final String PLACEHOLDER_TOKEN = "hf_YOUR_DEFAULT_TOKEN_HERE";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String token;
    private final String toxicityUrl;
    private final String subjectUrl;

    @Autowired
    public HuggingFaceClientService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${huggingface.api.token:hf_YOUR_DEFAULT_TOKEN_HERE}") String token,
            @Value("${huggingface.toxicity.url}") String toxicityUrl,
            @Value("${huggingface.subject.url}") String subjectUrl
    ) {
        this(
                restTemplateBuilder
                        .connectTimeout(Duration.ofSeconds(5))
                        .readTimeout(Duration.ofSeconds(30))
                        .build(),
                objectMapper,
                token,
                toxicityUrl,
                subjectUrl
        );
    }

    HuggingFaceClientService(
            RestTemplate restTemplate,
            ObjectMapper objectMapper,
            String token,
            String toxicityUrl,
            String subjectUrl
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.token = token;
        this.toxicityUrl = toxicityUrl;
        this.subjectUrl = subjectUrl;
    }

    public double toxicityScore(String text) {
        JsonNode response = post(toxicityUrl, Map.of(
                "inputs", text,
                "parameters", Map.of("top_k", 6)
        ));
        JsonNode predictions = predictionArray(response);
        for (JsonNode prediction : predictions) {
            String label = normalizeLabel(prediction.path("label").asText(""));
            if ("toxic".equals(label) || "toxicity".equals(label)) {
                return validatedScore(prediction.path("score"));
            }
        }
        throw new HuggingFaceUnavailableException(
                "Le modèle de toxicité n'a pas renvoyé le score attendu pour l'étiquette toxic."
        );
    }

    public Map<String, Double> subjectScores(String text) {
        Map<String, Object> parameters = new LinkedHashMap<>();
        parameters.put("candidate_labels", SUBJECTS);
        parameters.put("multi_label", false);

        JsonNode response = post(subjectUrl, Map.of("inputs", text, "parameters", parameters));
        JsonNode result = response;
        if (result == null) {
            throw new HuggingFaceUnavailableException(
                    "Le modèle de sujet a renvoyé une réponse invalide."
            );
        }
        if (result.isArray() && result.size() == 1
                && (result.get(0).isArray() || result.get(0).has("labels"))) {
            result = result.get(0);
        }

        Map<String, Double> scoreBySubject = result.isArray()
                ? parseSubjectPredictions(result)
                : parseSubjectLabelScores(result);
        if (scoreBySubject.size() != SUBJECTS.size()) {
            throw new HuggingFaceUnavailableException(
                    "Le modèle de sujet n'a pas renvoyé un score pour chaque catégorie attendue."
            );
        }
        return Map.copyOf(scoreBySubject);
    }

    private Map<String, Double> parseSubjectPredictions(JsonNode predictions) {
        Map<String, Double> scoreBySubject = new LinkedHashMap<>();
        for (JsonNode prediction : predictions) {
            String label = prediction.path("label").asText("");
            if (!SUBJECTS.contains(label)) {
                throw new HuggingFaceUnavailableException(
                        "Le modèle de sujet a renvoyé une catégorie inconnue."
                );
            }
            if (scoreBySubject.put(label, validatedScore(prediction.get("score"))) != null) {
                throw new HuggingFaceUnavailableException(
                        "Le modèle de sujet a renvoyé une catégorie en double."
                );
            }
        }
        return scoreBySubject;
    }

    private Map<String, Double> parseSubjectLabelScores(JsonNode result) {
        JsonNode labels = result.path("labels");
        JsonNode scores = result.path("scores");
        if (!labels.isArray() || !scores.isArray() || labels.size() != scores.size() || labels.isEmpty()) {
            throw new HuggingFaceUnavailableException(
                    "Le modèle de sujet a renvoyé une réponse invalide."
            );
        }

        Map<String, Double> scoreBySubject = new LinkedHashMap<>();
        for (int index = 0; index < labels.size(); index++) {
            String label = labels.get(index).asText("");
            if (!SUBJECTS.contains(label)) {
                throw new HuggingFaceUnavailableException(
                        "Le modèle de sujet a renvoyé une catégorie inconnue."
                );
            }
            if (scoreBySubject.put(label, validatedScore(scores.get(index))) != null) {
                throw new HuggingFaceUnavailableException(
                        "Le modèle de sujet a renvoyé une catégorie en double."
                );
            }
        }
        return scoreBySubject;
    }

    private JsonNode post(String url, Map<String, Object> payload) {
        requireToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token.trim());
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            RequestCallback requestCallback = restTemplate.httpEntityCallback(
                    new HttpEntity<>(payload, headers),
                    JsonNode.class
            );
            ResponseExtractor<JsonNode> responseExtractor = response -> {
                try {
                    return objectMapper.readTree(response.getBody());
                } catch (IOException exception) {
                    throw new RestClientException("Hugging Face a renvoyé un JSON invalide.", exception);
                }
            };
            return restTemplate.execute(url, HttpMethod.POST, requestCallback, responseExtractor);
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden exception) {
            throw new HuggingFaceAuthenticationException(
                    "Le jeton Hugging Face est invalide ou n'a pas la permission Inference Providers.",
                    exception
            );
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new HuggingFaceUnavailableException(
                    "Hugging Face est indisponible pour le moment. Réessayez plus tard.",
                    exception
            );
        } catch (RestClientException exception) {
            throw new HuggingFaceUnavailableException(
                    "L'appel Hugging Face a échoué. Vérifiez les URLs des modèles et réessayez.",
                    exception
            );
        }
    }

    private void requireToken() {
        if (token == null || token.isBlank() || PLACEHOLDER_TOKEN.equals(token.trim())) {
            throw new HuggingFaceUnavailableException(
                    "La clé Hugging Face n'est pas configurée. Définissez la variable d'environnement HF_API_TOKEN."
            );
        }
    }

    private JsonNode predictionArray(JsonNode response) {
        if (response == null || !response.isArray() || response.isEmpty()) {
            throw new HuggingFaceUnavailableException(
                    "Le modèle de toxicité a renvoyé une réponse invalide."
            );
        }
        JsonNode predictions = response;
        if (predictions.get(0).isArray()) {
            predictions = predictions.get(0);
        }
        if (!predictions.isArray() || predictions.isEmpty()) {
            throw new HuggingFaceUnavailableException(
                    "Le modèle de toxicité n'a renvoyé aucun score."
            );
        }
        return predictions;
    }

    private double validatedScore(JsonNode scoreNode) {
        if (scoreNode == null || !scoreNode.isNumber()) {
            throw new HuggingFaceUnavailableException("Hugging Face a renvoyé un score invalide.");
        }
        double score = scoreNode.asDouble();
        if (!Double.isFinite(score) || score < 0 || score > 1) {
            throw new HuggingFaceUnavailableException("Hugging Face a renvoyé un score hors limites.");
        }
        return score;
    }

    private String normalizeLabel(String label) {
        return label.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
    }
}
