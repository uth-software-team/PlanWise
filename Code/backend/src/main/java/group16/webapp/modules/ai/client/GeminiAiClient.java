package group16.webapp.modules.ai.client;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Google Gemini over REST {@code POST /v1beta/models/{model}:generateContent} (D7, CG-08). The API key goes in the
 * {@code x-goog-api-key} header, never in the URL. JSON output is requested with
 * {@code generationConfig.responseMimeType = application/json} plus {@code responseJsonSchema}; sampling
 * parameters (temperature, topP, topK) are not sent because Gemini 3.5 Flash-Lite deprecates and ignores them.
 * The 5 s limit (BR-12) is enforced by the request factory built in
 * {@link group16.webapp.modules.ai.config.AiClientConfig}, including the time spent reading the body.
 *
 * <p>Every provider failure becomes {@code AppException(AI_UNAVAILABLE)}. Logs carry only a failure category,
 * never the prompt, the key or response content.
 */
public class GeminiAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiClient.class);

    static final String API_KEY_HEADER = "x-goog-api-key";
    static final String GENERATE_PATH = "/v1beta/models/{model}:generateContent";

    private static final JsonMapper JSON = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
            .build();

    private final RestClient restClient;
    private final String model;
    private final String apiKey;

    public GeminiAiClient(RestClient restClient, String model, String apiKey) {
        this.restClient = restClient;
        this.model = model;
        this.apiKey = apiKey;
    }

    @Override
    public AiResponse generateJson(String prompt, Map<String, Object> responseJsonSchema) {
        String responseBody = send(requestBody(prompt, responseJsonSchema));
        return parseResponse(responseBody);
    }

    private static String requestBody(String prompt, Map<String, Object> responseJsonSchema) {
        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseJsonSchema", responseJsonSchema));
        return JSON.writeValueAsString(body);
    }

    private String send(String requestBody) {
        try {
            return restClient.post()
                    .uri(GENERATE_PATH, model)
                    .header(API_KEY_HEADER, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            throw unavailable("http_" + ex.getStatusCode().value());
        } catch (RestClientException | UncheckedIOException ex) {
            // Timeouts (connect, headers or body) and other I/O errors; the message may contain response data.
            throw unavailable("transport_" + ex.getClass().getSimpleName());
        }
    }

    private static AiResponse parseResponse(String responseBody) {
        JsonNode root = readJson(responseBody, "response_not_json");
        if (!root.isObject()) {
            throw unavailable("response_not_object");
        }
        JsonNode blockReason = root.path("promptFeedback").path("blockReason");
        if (!blockReason.isMissingNode() && !blockReason.isNull()) {
            throw unavailable("prompt_blocked");
        }
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw unavailable("no_candidates");
        }
        JsonNode candidate = candidates.path(0);
        if (!"STOP".equals(candidate.path("finishReason").asString(null))) {
            throw unavailable("finish_reason");
        }
        String text = answerText(candidate.path("content").path("parts"));
        if (!StringUtils.hasText(text)) {
            throw unavailable("empty_text");
        }
        if (!readJson(text, "text_not_json").isObject()) {
            throw unavailable("text_not_object");
        }
        return new AiResponse(text, tokensUsed(root.path("usageMetadata").path("totalTokenCount")));
    }

    /** Joins the text of every non-thought part, in order. */
    private static String answerText(JsonNode parts) {
        if (!parts.isArray()) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        for (JsonNode part : parts) {
            JsonNode partText = part.path("text");
            if (!part.path("thought").asBoolean(false) && partText.isString()) {
                text.append(partText.stringValue());
            }
        }
        return text.toString();
    }

    private static Integer tokensUsed(JsonNode totalTokenCount) {
        if (totalTokenCount.isIntegralNumber() && totalTokenCount.canConvertToInt()
                && totalTokenCount.intValue() >= 0) {
            return totalTokenCount.intValue();
        }
        return null;
    }

    private static JsonNode readJson(String json, String failure) {
        if (json == null) {
            throw unavailable(failure);
        }
        try {
            return JSON.readTree(json);
        } catch (JacksonException ex) {
            throw unavailable(failure);
        }
    }

    private static AppException unavailable(String reason) {
        log.warn("Gemini request failed: {}", reason);
        return new AppException(ErrorCode.AI_UNAVAILABLE);
    }
}
