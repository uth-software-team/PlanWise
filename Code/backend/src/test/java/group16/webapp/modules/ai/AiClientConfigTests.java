package group16.webapp.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.modules.ai.client.AiClient;
import group16.webapp.modules.ai.client.AiResponse;
import group16.webapp.modules.ai.client.FakeAiClient;
import group16.webapp.modules.ai.client.GeminiAiClient;
import group16.webapp.modules.ai.client.TaskDraftJsonSchema;
import group16.webapp.modules.ai.config.AiClientConfig;
import group16.webapp.modules.ai.config.AiProperties;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

/** AI-01: client selection by GEMINI_API_KEY and app.ai.* binding. */
class AiClientConfigTests {

	private final ApplicationContextRunner runner = new ApplicationContextRunner()
			.withUserConfiguration(AiClientConfig.class)
			.withPropertyValues(
					"app.ai.timeout=5s",
					"app.ai.gemini.model=gemini-3.5-flash-lite",
					"app.ai.gemini.base-url=https://gemini.test");

	/** C1 + C4: the real application starts without a key (test profile blanks it) and uses the defaults. */
	@Nested
	@SpringBootTest
	@ActiveProfiles("test")
	class ApplicationWithoutKey {

		@Autowired
		ApplicationContext context;

		@Autowired
		AiClient aiClient;

		@Autowired
		AiProperties properties;

		@Test
		void c1StartsWithFakeClientAndNoGeminiBean() {
			assertThat(aiClient).isInstanceOf(FakeAiClient.class);
			assertThat(context.getBeansOfType(GeminiAiClient.class)).isEmpty();
			assertThat(aiClient.generateJson("Ôn tập chương 3", TaskDraftJsonSchema.SCHEMA))
					.isEqualTo(new AiResponse("{}", 0));
		}

		@Test
		void c4DefaultsAreFiveSecondsAndTheChosenModel() {
			assertThat(properties.timeout()).isEqualTo(Duration.ofSeconds(5));
			assertThat(properties.gemini().model()).isEqualTo("gemini-3.5-flash-lite");
			assertThat(properties.gemini().baseUrl()).isEqualTo("https://generativelanguage.googleapis.com");
			assertThat(properties.hasApiKey()).isFalse();
		}
	}

	@Test
	void c2MissingOrWhitespaceKeyChoosesFake() {
		runner.run(context -> assertThat(context.getBean(AiClient.class)).isInstanceOf(FakeAiClient.class));
		runner.withPropertyValues("app.ai.gemini.api-key=")
				.run(context -> assertThat(context.getBean(AiClient.class)).isInstanceOf(FakeAiClient.class));
		runner.withPropertyValues("app.ai.gemini.api-key=   ")
				.run(context -> assertThat(context.getBean(AiClient.class)).isInstanceOf(FakeAiClient.class));
	}

	@Test
	void c3KeyChoosesGeminiWithoutCallingIt() {
		runner.withPropertyValues("app.ai.gemini.api-key=test-key")
				.run(context -> assertThat(context.getBean(AiClient.class)).isInstanceOf(GeminiAiClient.class));
	}

	@Test
	void c4TimeoutBindsAsDurationThroughTheSameProperty() {
		runner.withPropertyValues("app.ai.timeout=300ms")
				.run(context -> assertThat(context.getBean(AiProperties.class).timeout())
						.isEqualTo(Duration.ofMillis(300)));
	}

	@Test
	void c4NonPositiveTimeoutFailsStartup() {
		runner.withPropertyValues("app.ai.timeout=0s")
				.run(context -> assertThat(context).hasFailed());
	}

	@Test
	void keyIsMaskedInToString() {
		AiProperties.Gemini gemini = new AiProperties.Gemini("secret-value", "gemini-3.5-flash-lite", "https://x");
		assertThat(gemini.toString()).doesNotContain("secret-value").contains("****");
	}
}
