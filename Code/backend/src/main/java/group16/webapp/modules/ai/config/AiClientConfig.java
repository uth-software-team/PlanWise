package group16.webapp.modules.ai.config;

import group16.webapp.modules.ai.client.AiClient;
import group16.webapp.modules.ai.client.FakeAiClient;
import group16.webapp.modules.ai.client.GeminiAiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Chooses the {@link AiClient}: {@link FakeAiClient} when {@code GEMINI_API_KEY} is missing or blank, otherwise
 * {@link GeminiAiClient} on a {@link RestClient} with a JDK request factory.
 *
 * <p>Timeout (BR-12): {@code app.ai.timeout} is the connect timeout and the factory read timeout. In Spring
 * Framework 7 the JDK factory starts one timer when the request is sent and closes the response body when it
 * fires, so the limit also covers reading the body (checked by GeminiAiClientTimeoutTests).
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiProperties.class)
public class AiClientConfig {

    private static final Logger log = LoggerFactory.getLogger(AiClientConfig.class);

    @Bean
    public AiClient aiClient(AiProperties properties) {
        return createClient(properties);
    }

    /** Builds the client exactly as the bean does; also used by tests with a short timeout. */
    public static AiClient createClient(AiProperties properties) {
        if (!properties.hasApiKey()) {
            log.warn("GEMINI_API_KEY is not set: using FakeAiClient (no Gemini calls, empty drafts)");
            return new FakeAiClient();
        }
        AiProperties.Gemini gemini = properties.gemini();
        RestClient restClient = RestClient.builder()
                .baseUrl(gemini.baseUrl())
                .requestFactory(requestFactory(properties.timeout()))
                .build();
        return new GeminiAiClient(restClient, gemini.model(), gemini.apiKey().strip());
    }

    static JdkClientHttpRequestFactory requestFactory(Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);
        return factory;
    }
}
