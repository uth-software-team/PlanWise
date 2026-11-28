package group16.webapp.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.modules.ai.client.AiResponse;
import group16.webapp.modules.ai.client.FakeAiClient;
import group16.webapp.modules.ai.client.TaskDraftJsonSchema;
import group16.webapp.modules.ai.dto.TaskDraftResponse;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** AI-01: the fallback client used when GEMINI_API_KEY is missing. */
class FakeAiClientTests {

	@Test
	void f1AnswersAnEmptyDraftWithZeroTokensForAnyPrompt() {
		FakeAiClient client = new FakeAiClient();

		AiResponse first = client.generateJson("Ôn tập chương 3", TaskDraftJsonSchema.SCHEMA);
		AiResponse second = client.generateJson("anything else", TaskDraftJsonSchema.SCHEMA);

		assertThat(first).isEqualTo(new AiResponse("{}", 0)).isEqualTo(second);
		TaskDraftResponse draft = JsonMapper.builder().build().readValue(first.jsonText(), TaskDraftResponse.class);
		assertThat(draft).isEqualTo(new TaskDraftResponse(null, null, null, null, null));
	}

	@Test
	void f1HoldsNoHttpClientOrOtherState() {
		assertThat(Arrays.stream(FakeAiClient.class.getDeclaredFields())
				.filter(field -> !Modifier.isStatic(field.getModifiers())))
				.isEmpty();
	}
}
