package group16.webapp.modules.ai.client;

import java.util.Map;

/**
 * Provider adapter for AI suggestions (CG-08). Implementations never write to the database and never log the
 * prompt. Selected by {@link group16.webapp.modules.ai.config.AiClientConfig}: {@link FakeAiClient} when no
 * {@code GEMINI_API_KEY} is set, otherwise {@link GeminiAiClient}.
 */
public interface AiClient {

    /**
     * Asks the provider for a JSON answer that follows {@code responseJsonSchema}.
     *
     * @return the provider text, guaranteed to be exactly one syntactically valid JSON object. Field types and
     *         values are NOT validated here; the caller (AI-02) must validate every field it uses.
     * @throws group16.webapp.common.exception.AppException with {@code AI_UNAVAILABLE} on any provider failure
     *         (timeout, HTTP error such as 429, blocked or incomplete answer, invalid JSON)
     */
    AiResponse generateJson(String prompt, Map<String, Object> responseJsonSchema);
}
