package group16.webapp.modules.ai.client;

import java.util.Map;

/**
 * Used when {@code GEMINI_API_KEY} is missing or blank (CG-08). Sends no HTTP request, ignores the prompt and
 * always answers an empty draft {@code {}} with 0 tokens, so the parse flow works offline and the user fills in
 * the task manually.
 */
public class FakeAiClient implements AiClient {

    static final String EMPTY_DRAFT = "{}";

    @Override
    public AiResponse generateJson(String prompt, Map<String, Object> responseJsonSchema) {
        return new AiResponse(EMPTY_DRAFT, 0);
    }
}
