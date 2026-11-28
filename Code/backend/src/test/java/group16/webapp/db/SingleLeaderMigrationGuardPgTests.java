package group16.webapp.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import group16.webapp.support.PostgresContainerTest;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * E3: the V11 guard rejects data that breaks "exactly one ACTIVE LEADER per group", PostgreSQL rolls the whole
 * migration back (no history row, no index), and after the data is fixed (cutover M3) a plain re-run applies V11
 * without {@code flyway repair}. Runs the application migrations in a throwaway schema, so the shared test schema
 * is untouched. Deliberately not {@code @Transactional}: Flyway commits its own transactions.
 */
class SingleLeaderMigrationGuardPgTests extends PostgresContainerTest {

	private static final String SCHEMA = "v11_guard";

	@Autowired
	private DataSource dataSource;

	@Autowired
	private JdbcTemplate jdbc;

	@AfterEach
	void dropSchema() {
		jdbc.execute("DROP SCHEMA IF EXISTS " + SCHEMA + " CASCADE");
	}

	@Test
	void guardRollsBackCleanlyAndRerunSucceedsAfterDataFix() {
		flyway("10").migrate();
		long a = insertUser("a@example.com");
		long b = insertUser("b@example.com");
		long c = insertUser("c@example.com");
		long twoLeaders = insertGroup(a);
		long noLeader = insertGroup(c);
		long emptyGroup = insertGroup(c);
		insertMember(twoLeaders, a, "LEADER", "ACTIVE");
		insertMember(twoLeaders, b, "LEADER", "ACTIVE");
		insertMember(noLeader, c, "MEMBER", "ACTIVE");
		// A group whose members all left is not checked (D16 applies to groups with ACTIVE members).
		insertMember(emptyGroup, a, "LEADER", "INACTIVE");

		assertThatThrownBy(() -> flyway(null).migrate())
				.isInstanceOf(FlywayException.class)
				.hasStackTraceContaining("V11: groups without exactly one ACTIVE LEADER: " + twoLeaders + ", " + noLeader);
		assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("10");
		assertThat(historyRows("11")).as("failed V11 left no history row").isZero();
		assertThat(indexCount()).as("index rolled back").isZero();

		// Cutover M3: one keeper per group.
		jdbc.update("UPDATE " + SCHEMA + ".group_members SET role = 'MEMBER' WHERE group_id = ? AND user_id = ?",
				twoLeaders, b);
		jdbc.update("UPDATE " + SCHEMA + ".group_members SET role = 'LEADER' WHERE group_id = ? AND user_id = ?",
				noLeader, c);

		flyway(null).migrate();
		assertThat(flyway(null).info().current().getVersion().getVersion()).isEqualTo("11");
		assertThat(jdbc.queryForObject("SELECT success FROM " + SCHEMA + ".flyway_schema_history WHERE version = '11'",
				Boolean.class)).isTrue();
		assertThat(indexCount()).isEqualTo(1);
	}

	/** Application migrations only (no test-only tables); {@code target} null = latest. */
	private Flyway flyway(String target) {
		var config = Flyway.configure()
				.dataSource(dataSource)
				.schemas(SCHEMA)
				.createSchemas(true)
				.locations("classpath:db/migration");
		if (target != null) {
			config.target(target);
		}
		return config.load();
	}

	private int historyRows(String version) {
		return jdbc.queryForObject("SELECT count(*) FROM " + SCHEMA + ".flyway_schema_history WHERE version = ?",
				Integer.class, version);
	}

	private int indexCount() {
		return jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE schemaname = ? AND indexname = "
				+ "'uk_group_members_one_active_leader'", Integer.class, SCHEMA);
	}

	private long insertUser(String email) {
		return jdbc.queryForObject("INSERT INTO " + SCHEMA + ".users (email, password_hash, full_name) "
				+ "VALUES (?, 'x', 'Pg') RETURNING id", Long.class, email);
	}

	private long insertGroup(long creatorId) {
		return jdbc.queryForObject("INSERT INTO " + SCHEMA + ".team_groups (created_by_user_id, group_name, "
				+ "description) VALUES (?, 'Team', '') RETURNING id", Long.class, creatorId);
	}

	private void insertMember(long groupId, long userId, String role, String status) {
		jdbc.update("INSERT INTO " + SCHEMA + ".group_members (group_id, user_id, role, status, left_at) "
						+ "VALUES (?, ?, ?, ?, CASE WHEN ? = 'INACTIVE' THEN CURRENT_TIMESTAMP END)",
				groupId, userId, role, status, status);
	}
}
