package group16.webapp.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import group16.webapp.support.PostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Every Flyway migration applies on real PostgreSQL 16 and the JPA entities match the schema
 * (the context only starts if {@code ddl-auto=validate} passes). Also checks PostgreSQL-only constraints
 * that the H2 tests cannot cover. Data changes roll back after each test.
 */
@Transactional
class MigrationPgTests extends PostgresContainerTest {

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbc;

	/** Test-only migrations (src/test/resources/db/test-migration) use versions from here on. */
	private static final int FIRST_TEST_ONLY_VERSION = 9000;

	@Test
	void allApplicationMigrationsAreAppliedSuccessfullyAndContiguous() throws IOException {
		PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
		Resource[] appFiles = resolver.getResources("classpath:db/migration/V*__*.sql");
		Resource[] testFiles = resolver.getResources("classpath:db/test-migration/V*__*.sql");
		MigrationInfo[] applied = flyway.info().applied();

		assertThat(appFiles).isNotEmpty();
		assertThat(flyway.info().pending()).isEmpty();
		assertThat(applied).hasSize(appFiles.length + testFiles.length);
		assertThat(applied).allSatisfy(m -> assertThat(m.getState()).isEqualTo(MigrationState.SUCCESS));

		List<String> appVersions = Arrays.stream(applied)
				.map(m -> m.getVersion().getVersion())
				.filter(v -> Integer.parseInt(v) < FIRST_TEST_ONLY_VERSION)
				.toList();
		assertThat(appVersions)
				.as("application migration versions are contiguous from 1 (Flyway runs them in order)")
				.containsExactlyElementsOf(IntStream.rangeClosed(1, appFiles.length).mapToObj(String::valueOf).toList());
	}

	@Test
	void usersRejectReversedPlanningWindow() {
		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO users (email, password_hash, full_name, planning_start, planning_end) "
						+ "VALUES ('pg-window@example.com', 'x', 'Pg', TIME '18:00', TIME '08:00')"))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_users_planning_window");
	}

	@Test
	void notificationsRejectUnknownType() {
		long userId = insertUser("pg-notify@example.com");
		jdbc.update("INSERT INTO notifications (user_id, type, title) VALUES (?, 'TASK_ASSIGNED', 'ok')", userId);

		assertThatThrownBy(() -> jdbc.update(
				"INSERT INTO notifications (user_id, type, title) VALUES (?, 'NOT_A_TYPE', 'bad')", userId))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("ck_notifications_type");
	}

	@Test
	void passwordResetTokenHashIsUnique() {
		long userId = insertUser("pg-reset@example.com");
		String insert = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) "
				+ "VALUES (?, ?, now() + interval '30 minutes')";
		jdbc.update(insert, userId, "a".repeat(64));

		assertThatThrownBy(() -> jdbc.update(insert, userId, "a".repeat(64)))
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining("uk_password_reset_tokens_token_hash");
	}

	private long insertUser(String email) {
		Long id = jdbc.queryForObject(
				"INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'Pg') RETURNING id",
				Long.class, email);
		assertThat(id).isNotNull();
		return id;
	}
}
