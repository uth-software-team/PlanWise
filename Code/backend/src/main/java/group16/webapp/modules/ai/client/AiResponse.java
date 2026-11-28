package group16.webapp.modules.ai.client;

/**
 * Result of {@link AiClient#generateJson}.
 *
 * @param jsonText   exactly one JSON object as text
 * @param tokensUsed provider token usage; 0 for the fake client, {@code null} when the provider reports none
 *                   (matches {@code ai_request_logs.tokens_used}, V9)
 */
public record AiResponse(String jsonText, Integer tokensUsed) {
}
