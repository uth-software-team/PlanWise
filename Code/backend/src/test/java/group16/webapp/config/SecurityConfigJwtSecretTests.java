package group16.webapp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Plain unit test (no Spring context): JWT secret must be present and at least 32 bytes. */
class SecurityConfigJwtSecretTests {

	private static final String ORIGINS = "http://localhost:5173";

	@Test
	void rejectsMissingSecret() {
		assertThatThrownBy(() -> new SecurityConfig(null, ORIGINS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET is not set");
	}

	@Test
	void rejectsBlankSecret() {
		assertThatThrownBy(() -> new SecurityConfig("   ", ORIGINS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("JWT_SECRET is not set");
	}

	@Test
	void rejectsSecretShorterThan32Bytes() {
		String shortSecret = "a".repeat(31);
		assertThatThrownBy(() -> new SecurityConfig(shortSecret, ORIGINS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("at least 32 bytes")
				.hasMessageNotContaining(shortSecret);
	}

	@Test
	void rejectsEnvExamplePlaceholderSecret() {
		// Value shipped in Code/backend/.env.example: long enough, but publicly known (SA-02).
		String placeholder = "change-me-to-a-random-string-of-at-least-32-bytes";
		assertThatThrownBy(() -> new SecurityConfig(placeholder, ORIGINS))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("placeholder")
				.hasMessageNotContaining(placeholder);
	}

	@Test
	void rejectsPlaceholderPrefixIgnoringCaseAndSurroundingSpace() {
		assertThatThrownBy(() -> SecurityConfig.validateJwtSecret("  CHANGE-ME-" + "x".repeat(40)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("placeholder");
	}

	@Test
	void acceptsSecretOfExactly32Bytes() {
		assertThat(SecurityConfig.validateJwtSecret("a".repeat(32))).hasSize(32);
	}

	@Test
	void countsUtf8BytesNotCharacters() {
		// 16 characters x 2 bytes each in UTF-8 = 32 bytes -> accepted.
		assertThat(SecurityConfig.validateJwtSecret("é".repeat(16))).hasSize(32);
	}

}
