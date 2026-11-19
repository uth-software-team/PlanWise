package group16.webapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Writes the F-05 contract snapshot docs/api/openapi-v1.json. Runs only via {@code ./gradlew exportOpenApi}
 * (tag "openapi" is excluded from {@code test}/{@code build}); without the output path property it is
 * skipped, so no other run can rewrite the snapshot. Keys are sorted for byte-stable output.
 */
@Tag("openapi")
@SpringBootTest(properties = "springdoc.writer-with-order-by-keys=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OpenApiSnapshotExportTests {

	static final String OUTPUT_PROPERTY = "openapi.snapshot.path";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exportSnapshot() throws Exception {
		String output = System.getProperty(OUTPUT_PROPERTY);
		Assumptions.assumeTrue(output != null && !output.isBlank(), "run via ./gradlew exportOpenApi");

		String json = mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
		Map<String, Object> document = OpenApiContractTests.parse(json);

		assertThat(OpenApiContractTests.operations(document))
				.containsExactlyInAnyOrderElementsOf(OpenApiContractTests.EXPECTED_OPERATIONS);
		assertThat(OpenApiContractTests.unresolvedRefs(document)).isEmpty();

		JsonMapper writer = JsonMapper.builder()
				.enable(SerializationFeature.INDENT_OUTPUT)
				.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
				.build();
		Path target = Path.of(output);
		Files.createDirectories(target.getParent());
		Files.writeString(target, writer.writeValueAsString(document).replace("\r\n", "\n") + "\n",
				StandardCharsets.UTF_8);
	}
}
