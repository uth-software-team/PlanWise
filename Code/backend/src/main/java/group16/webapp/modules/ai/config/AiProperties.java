package group16.webapp.modules.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * {@code app.ai.*} settings (CG-08). Defaults live in {@code application.properties}: {@code app.ai.timeout=5s}
 * (BR-12), model {@code gemini-3.5-flash-lite} (E6, TV1 2026-11-28). The key comes only from
 * {@code GEMINI_API_KEY}; blank means {@link group16.webapp.modules.ai.client.FakeAiClient}.
 *
 * @param timeout total time allowed for one provider call, including reading the response body
 */
@ConfigurationProperties("app.ai")
public record AiProperties(Duration timeout, Gemini gemini) {

    public AiProperties {
        if (timeout == null || timeout.isNegative() || timeout.isZero()) {
            throw new IllegalStateException("app.ai.timeout must be positive");
        }
        if (gemini == null) {
            throw new IllegalStateException("app.ai.gemini.* must be configured");
        }
    }

    /** True when a non-blank API key is configured. */
    public boolean hasApiKey() {
        return StringUtils.hasText(gemini.apiKey());
    }

    /**
     * @param apiKey  secret; never logged ({@link #toString()} masks it)
     * @param model   Gemini model id used in the request path
     * @param baseUrl API origin, e.g. {@code https://generativelanguage.googleapis.com}
     */
    public record Gemini(String apiKey, String model, String baseUrl) {

        public Gemini {
            if (!StringUtils.hasText(model)) {
                throw new IllegalStateException("app.ai.gemini.model must not be blank");
            }
            if (!StringUtils.hasText(baseUrl)) {
                throw new IllegalStateException("app.ai.gemini.base-url must not be blank");
            }
        }

        @Override
        public String toString() {
            return "Gemini[apiKey=" + (StringUtils.hasText(apiKey) ? "****" : "<empty>")
                    + ", model=" + model + ", baseUrl=" + baseUrl + "]";
        }
    }
}
