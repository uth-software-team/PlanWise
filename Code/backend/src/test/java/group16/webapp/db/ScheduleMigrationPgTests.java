package group16.webapp.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import group16.webapp.support.PostgresContainerTest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * V10 schedule_proposals + schedule_sessions on real PostgreSQL 16. Each rejected row breaks exactly one
 * constraint, and the failing statement is the last one in its test because PostgreSQL aborts the transaction
 * after an error. Data changes roll back after each test.
 */
@Transactional
class ScheduleMigrationPgTests extends PostgresContainerTest {

	private static final Map<String, String> PROPOSAL = Map.of(
			"week_start", "DATE '2026-11-23'",
			"horizon_start", "TIMESTAMPTZ '2026-11-23 00:00:00+07'",
			"horizon_end", "TIMESTAMPTZ '2026-11-30 00:00:00+07'",
			"source", "'ENGINE'");

	private static final Map<String, String> TASK = Map.of(
			"title", "'Write report'",
			"description", "''",
			"priority", "'MEDIUM'",
			"category", "'WORK'",
			"estimated_duration_minutes", "60");

	private static final String START = "TIMESTAMPTZ '2026-11-23 09:00:00+07'";
	private static final String END = "TIMESTAMPTZ '2026-11-23 10:00:00+07'";

	@Autowired
	private JdbcTemplate jdbc;

	// ---------------------------------------------------------------- schedule_proposals

	@Test
	void proposalDefaults() {
		long userId = insertUser("pg-proposal-defaults@example.com");
		long id = insertProposal(userId);

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, version, created_at, confirmed_at FROM schedule_proposals WHERE id = ?", id);
		assertThat(row.get("status")).isEqualTo("PROPOSED");
		assertThat(row.get("version")).isEqualTo(0);
		assertThat(row.get("created_at")).isNotNull();
		assertThat(row.get("confirmed_at")).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("validProposals")
	void proposalsAcceptValidRows(String caseName, Map<String, String> overrides) {
		long userId = insertUser("pg-proposal-valid@example.com");
		insertRow("schedule_proposals", with(proposal(userId), overrides));
	}

	static Stream<Arguments> validProposals() {
		return Stream.of(
				Arguments.of("AI source", Map.of("source", "'AI'")),
				Arguments.of("CONFIRMED with confirmed_at", Map.of("status", "'CONFIRMED'",
						"confirmed_at", "CURRENT_TIMESTAMP")),
				Arguments.of("CANCELLED without confirmed_at", Map.of("status", "'CANCELLED'")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidProposals")
	void proposalsRejectInvalidRows(String caseName, Map<String, String> overrides, String violation) {
		long userId = insertUser("pg-proposal-invalid@example.com");
		assertRejected(() -> insertRow("schedule_proposals", with(proposal(userId), overrides)), violation);
	}

	static Stream<Arguments> invalidProposals() {
		return Stream.of(
				Arguments.of("unknown source", Map.of("source", "'MANUAL'"), "ck_schedule_proposals_source"),
				Arguments.of("unknown status", Map.of("status", "'COMPLETED'"), "ck_schedule_proposals_status"),
				Arguments.of("equal horizon", Map.of("horizon_end", "TIMESTAMPTZ '2026-11-23 00:00:00+07'"),
						"ck_schedule_proposals_horizon"),
				Arguments.of("reversed horizon", Map.of("horizon_end", "TIMESTAMPTZ '2026-11-22 00:00:00+07'"),
						"ck_schedule_proposals_horizon"),
				Arguments.of("CONFIRMED without confirmed_at", Map.of("status", "'CONFIRMED'"),
						"ck_schedule_proposals_confirmed_at"),
				Arguments.of("PROPOSED with confirmed_at", Map.of("confirmed_at", "CURRENT_TIMESTAMP"),
						"ck_schedule_proposals_confirmed_at"),
				Arguments.of("CANCELLED with confirmed_at", Map.of("status", "'CANCELLED'",
						"confirmed_at", "CURRENT_TIMESTAMP"), "ck_schedule_proposals_confirmed_at"),
				Arguments.of("unknown user", Map.of("user_id", "-1"), "fk_schedule_proposals_user"),
				Arguments.of("null user", Map.of("user_id", "NULL"), "column \"user_id\""),
				Arguments.of("null week_start", Map.of("week_start", "NULL"), "column \"week_start\""),
				Arguments.of("null horizon_start", Map.of("horizon_start", "NULL"), "column \"horizon_start\""),
				Arguments.of("null horizon_end", Map.of("horizon_end", "NULL"), "column \"horizon_end\""),
				Arguments.of("null source", Map.of("source", "NULL"), "column \"source\""),
				Arguments.of("null status", Map.of("status", "NULL"), "column \"status\""),
				Arguments.of("null version", Map.of("version", "NULL"), "column \"version\""));
	}

	@Test
	void secondProposedBatchForSameUserIsRejected() {
		long userId = insertUser("pg-proposal-active@example.com");
		insertProposal(userId);

		assertRejected(() -> insertProposal(userId), "uk_schedule_proposals_active_user");
	}

	@Test
	void differentUsersMayEachHaveOneProposedBatch() {
		long userA = insertUser("pg-proposal-user-a@example.com");
		long userB = insertUser("pg-proposal-user-b@example.com");
		insertProposal(userA);
		insertProposal(userB);

		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM schedule_proposals WHERE status = 'PROPOSED' AND user_id IN (?, ?)",
				Integer.class, userA, userB)).isEqualTo(2);
	}

	@Test
	void oneProposedBatchMayCoexistWithCancelledAndConfirmedBatches() {
		long userId = insertUser("pg-proposal-history@example.com");
		insertRow("schedule_proposals", with(proposal(userId), Map.of("status", "'CANCELLED'")));
		insertRow("schedule_proposals", with(proposal(userId), Map.of("status", "'CANCELLED'")));
		insertRow("schedule_proposals", with(proposal(userId), Map.of("status", "'CONFIRMED'",
				"confirmed_at", "CURRENT_TIMESTAMP")));
		insertProposal(userId);

		assertThat(jdbc.queryForObject("SELECT count(*) FROM schedule_proposals WHERE user_id = ?",
				Integer.class, userId)).isEqualTo(4);
	}

	@Test
	void activeProposalIndexIsPartialOnProposed() {
		assertThat(jdbc.queryForObject(
				"SELECT indexdef FROM pg_indexes WHERE indexname = 'uk_schedule_proposals_active_user'",
				String.class))
				.contains("UNIQUE").contains("(user_id)").contains("'PROPOSED'");
	}

	// ---------------------------------------------------------------- schedule_sessions

	@Test
	void sessionDefaults() {
		Fixture f = fixture("pg-session-defaults");
		long id = insertRow("schedule_sessions", f.session());

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, version, created_at, updated_at FROM schedule_sessions WHERE id = ?", id);
		assertThat(row.get("status")).isEqualTo("PROPOSED");
		assertThat(row.get("version")).isEqualTo(0);
		assertThat(row.get("created_at")).isNotNull();
		assertThat(row.get("updated_at")).isNotNull();
	}

	@ParameterizedTest(name = "status = {0}")
	@MethodSource("sessionStatuses")
	void sessionsAcceptEveryStatus(String status) {
		Fixture f = fixture("pg-session-status");
		insertRow("schedule_sessions", with(f.session(), Map.of("status", "'" + status + "'")));
	}

	static Stream<String> sessionStatuses() {
		return Stream.of("PROPOSED", "CONFIRMED", "COMPLETED", "CANCELLED");
	}

	@Test
	void adjacentSessionsAreAccepted() {
		Fixture f = fixture("pg-session-adjacent");
		insertRow("schedule_sessions", f.session());
		insertRow("schedule_sessions", with(f.session(), Map.of(
				"start_at", END, "end_at", "TIMESTAMPTZ '2026-11-23 11:00:00+07'")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidSessions")
	void sessionsRejectInvalidRows(String caseName, Map<String, String> overrides, String violation) {
		Fixture f = fixture("pg-session-invalid");
		assertRejected(() -> insertRow("schedule_sessions", with(f.session(), overrides)), violation);
	}

	static Stream<Arguments> invalidSessions() {
		return Stream.of(
				Arguments.of("unknown status", Map.of("status", "'DONE'"), "ck_schedule_sessions_status"),
				Arguments.of("equal times", Map.of("end_at", START), "ck_schedule_sessions_time_range"),
				Arguments.of("reversed times", Map.of("end_at", "TIMESTAMPTZ '2026-11-23 08:00:00+07'"),
						"ck_schedule_sessions_time_range"),
				Arguments.of("unknown task", Map.of("task_id", "-1"), "fk_schedule_sessions_task"),
				Arguments.of("unknown proposal", Map.of("proposal_id", "-1"), "fk_schedule_sessions_proposal_user"),
				Arguments.of("null proposal_id", Map.of("proposal_id", "NULL"), "column \"proposal_id\""),
				Arguments.of("null user_id", Map.of("user_id", "NULL"), "column \"user_id\""),
				Arguments.of("null task_id", Map.of("task_id", "NULL"), "column \"task_id\""),
				Arguments.of("null start_at", Map.of("start_at", "NULL"), "column \"start_at\""),
				Arguments.of("null end_at", Map.of("end_at", "NULL"), "column \"end_at\""),
				Arguments.of("null status", Map.of("status", "NULL"), "column \"status\""),
				Arguments.of("null version", Map.of("version", "NULL"), "column \"version\""));
	}

	@Test
	void sessionOfAnotherUserThanTheProposalOwnerIsRejected() {
		Fixture f = fixture("pg-session-owner");
		long otherUserId = insertUser("pg-session-owner-other@example.com");

		assertRejected(() -> insertRow("schedule_sessions", with(f.session(),
				Map.of("user_id", String.valueOf(otherUserId)))), "fk_schedule_sessions_proposal_user");
	}

	@Test
	void sessionUniqueKeyCoversIdProposalAndUser() {
		assertThat(jdbc.queryForObject(
				"SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'uk_schedule_sessions_id_proposal_user'",
				String.class)).isEqualTo("UNIQUE (id, proposal_id, user_id)");
	}

	// ---------------------------------------------------------------- helpers

	/** One user with a task and a PROPOSED batch; the session row is valid as PROPOSED. */
	private record Fixture(long userId, long taskId, long proposalId) {
		Map<String, String> session() {
			return Map.of("proposal_id", String.valueOf(proposalId), "task_id", String.valueOf(taskId),
					"user_id", String.valueOf(userId), "start_at", START, "end_at", END);
		}
	}

	private Fixture fixture(String prefix) {
		long userId = insertUser(prefix + "@example.com");
		long taskId = insertRow("tasks", with(TASK, Map.of("owner_user_id", String.valueOf(userId))));
		return new Fixture(userId, taskId, insertProposal(userId));
	}

	private static Map<String, String> proposal(long userId) {
		return with(PROPOSAL, Map.of("user_id", String.valueOf(userId)));
	}

	private long insertProposal(long userId) {
		return insertRow("schedule_proposals", proposal(userId));
	}

	/** Column values are SQL literals from this class (test data only, never user input). */
	private long insertRow(String table, Map<String, String> columns) {
		String sql = "INSERT INTO " + table + " (" + String.join(", ", columns.keySet()) + ") VALUES ("
				+ String.join(", ", columns.values()) + ") RETURNING id";
		Long id = jdbc.queryForObject(sql, Long.class);
		assertThat(id).isNotNull();
		return id;
	}

	private static Map<String, String> with(Map<String, String> base, Map<String, String> overrides) {
		Map<String, String> row = new LinkedHashMap<>(base);
		row.putAll(overrides);
		return row;
	}

	private static void assertRejected(ThrowingCallable insert, String violation) {
		assertThatThrownBy(insert)
				.isInstanceOf(DataIntegrityViolationException.class)
				.hasMessageContaining(violation);
	}

	private long insertUser(String email) {
		Long id = jdbc.queryForObject(
				"INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'Pg') RETURNING id",
				Long.class, email);
		assertThat(id).isNotNull();
		return id;
	}
}
