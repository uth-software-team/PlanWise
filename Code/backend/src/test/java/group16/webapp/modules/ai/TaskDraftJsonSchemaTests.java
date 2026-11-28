package group16.webapp.modules.ai;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.modules.ai.client.TaskDraftJsonSchema;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** AI-01 K1: the provider schema matches the approved TaskDraftResponse contract plus the two agreed constraints. */
class TaskDraftJsonSchemaTests {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Test
	void k1SchemaIsTheFiveOptionalDraftFieldsWithMinimumAndNoExtraProperties() {
		String expected = """
				{
				  "type": "object",
				  "properties": {
				    "title": { "type": "string" },
				    "deadline": { "type": "string", "format": "date-time" },
				    "priority": { "type": "string", "enum": ["LOW", "MEDIUM", "HIGH", "URGENT"] },
				    "category": { "type": "string", "enum": ["STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER"] },
				    "estimatedDurationMinutes": { "type": "integer", "minimum": 1 }
				  },
				  "additionalProperties": false
				}
				""";

		JsonNode actual = JSON.valueToTree(TaskDraftJsonSchema.SCHEMA);
		assertThat(actual).isEqualTo(JSON.readTree(expected));
		assertThat(TaskDraftJsonSchema.SCHEMA).doesNotContainKey("required");
	}
}
