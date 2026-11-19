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
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * V7 team_groups/group_members (+ fk_tasks_group), V8 task_assignments and V9 ai_request_logs on real
 * PostgreSQL 16. Each rejected row breaks exactly one constraint, and the failing statement is the last one in
 * its test because PostgreSQL aborts the transaction after an error. Behaviour tests of the V8 partial unique
 * index belong to AS-PG. Data changes roll back after each test.
 *
 * <p>The tasks rows use the V4 columns from TK-01; re-check them against the merged V4 before integration.
 */
@Transactional
class GroupAssignmentAiMigrationPgTests extends PostgresContainerTest {

	private static final Map<String, String> TASK = Map.of(
			"title", "'Write report'",
			"description", "''",
			"priority", "'MEDIUM'",
			"category", "'WORK'",
			"estimated_duration_minutes", "30");

	private static final Map<String, String> MEMBER = Map.of("role", "'MEMBER'");

	private static final Map<String, String> AI_OK = Map.of(
			"request_type", "'TASK_PARSE'",
			"prompt_hash", "'" + "a".repeat(64) + "'",
			"tokens_used", "120",
			"latency_ms", "850",
			"success", "TRUE");

	@Autowired
	private JdbcTemplate jdbc;

	// ---------------------------------------------------------------- V7 team_groups / group_members

	@Test
	void groupAndMemberDefaults() {
		long userId = insertUser("pg-group-defaults@example.com");
		long groupId = insertGroup(userId);
		long memberId = insertRow("group_members", Map.of("group_id", String.valueOf(groupId),
				"user_id", String.valueOf(userId), "role", "'LEADER'"));

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, joined_at, left_at FROM group_members WHERE id = ?", memberId);
		assertThat(row.get("status")).isEqualTo("ACTIVE");
		assertThat(row.get("joined_at")).isNotNull();
		assertThat(row.get("left_at")).isNull();
	}

	@Test
	void inactiveMembershipWithLeftAtIsAccepted() {
		long userId = insertUser("pg-member-inactive@example.com");
		long groupId = insertGroup(userId);
		insertRow("group_members", with(member(groupId, userId),
				"status", "'INACTIVE'", "left_at", "CURRENT_TIMESTAMP + interval '1 minute'"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidMembers")
	void groupMembersRejectInvalidRows(String caseName, Map<String, String> overrides, String violation) {
		long userId = insertUser("pg-member-invalid@example.com");
		long groupId = insertGroup(userId);
		Map<String, String> row = new LinkedHashMap<>(member(groupId, userId));
		row.putAll(overrides);
		assertRejected(() -> insertRow("group_members", row), violation);
	}

	static Stream<Arguments> invalidMembers() {
		return Stream.of(
				Arguments.of("unknown role", Map.of("role", "'OWNER'"), "ck_group_members_role"),
				Arguments.of("unknown status", Map.of("status", "'LEFT'"), "ck_group_members_status"),
				Arguments.of("ACTIVE with left_at", Map.of("left_at", "CURRENT_TIMESTAMP"), "ck_group_members_left_at"),
				Arguments.of("INACTIVE without left_at", Map.of("status", "'INACTIVE'"), "ck_group_members_left_at"),
				Arguments.of("left before joined", Map.of("status", "'INACTIVE'",
						"left_at", "CURRENT_TIMESTAMP - interval '1 day'"), "ck_group_members_left_after_join"),
				Arguments.of("null role", Map.of("role", "NULL"), "column \"role\""));
	}

	@Test
	void groupMembershipIsUniquePerGroupAndUser() {
		long userId = insertUser("pg-member-unique@example.com");
		long groupId = insertGroup(userId);
		insertRow("group_members", member(groupId, userId));

		assertRejected(() -> insertRow("group_members", member(groupId, userId)), "uk_group_members_group_user");
	}

	@Test
	void groupMembersRejectUnknownGroup() {
		long userId = insertUser("pg-member-nogroup@example.com");
		assertRejected(() -> insertRow("group_members", member(-1L, userId)), "fk_group_members_group");
	}

	@ParameterizedTest(name = "group_name = {0}")
	@MethodSource("blankTexts")
	void teamGroupsRejectBlankName(String caseName, String literal) {
		long userId = insertUser("pg-group-blank@example.com");
		assertRejected(() -> insertRow("team_groups", Map.of("created_by_user_id", String.valueOf(userId),
				"group_name", literal, "description", "''")), "ck_team_groups_group_name_not_blank");
	}

	@Test
	void teamGroupsRejectUnknownCreator() {
		assertRejected(() -> insertGroup(-1L), "fk_team_groups_created_by_user");
	}

	// ---------------------------------------------------------------- V7 fk_tasks_group

	@Test
	void personalTaskWithNullGroupIsAccepted() {
		long userId = insertUser("pg-task-personal@example.com");
		long taskId = insertTask(userId, null);

		assertThat(jdbc.queryForObject("SELECT group_id FROM tasks WHERE id = ?", Long.class, taskId)).isNull();
	}

	@Test
	void groupTaskWithExistingGroupIsAccepted() {
		long userId = insertUser("pg-task-group@example.com");
		long groupId = insertGroup(userId);
		long taskId = insertTask(userId, groupId);

		assertThat(jdbc.queryForObject("SELECT group_id FROM tasks WHERE id = ?", Long.class, taskId))
				.isEqualTo(groupId);
	}

	@Test
	void groupTaskWithUnknownGroupIsRejected() {
		long userId = insertUser("pg-task-nogroup@example.com");
		assertRejected(() -> insertTask(userId, -1L), "fk_tasks_group");
	}

	// ---------------------------------------------------------------- V8 task_assignments

	@Test
	void newAssignmentDefaultsToPendingWithoutResponse() {
		Fixture f = fixture("pg-assign-default");
		long id = insertRow("task_assignments", f.assignment());

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, decline_reason, responded_at FROM task_assignments WHERE id = ?", id);
		assertThat(row.get("status")).isEqualTo("PENDING");
		assertThat(row.get("decline_reason")).isNull();
		assertThat(row.get("responded_at")).isNull();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("validAssignmentStates")
	void assignmentsAcceptValidStates(String caseName, Map<String, String> overrides) {
		Fixture f = fixture("pg-assign-valid");
		Map<String, String> row = new LinkedHashMap<>(f.assignment());
		row.putAll(overrides);
		insertRow("task_assignments", row);
	}

	static Stream<Arguments> validAssignmentStates() {
		return Stream.of(
				Arguments.of("ACCEPTED with responded_at", Map.of("status", "'ACCEPTED'",
						"responded_at", "CURRENT_TIMESTAMP")),
				Arguments.of("DECLINED with a reason", Map.of("status", "'DECLINED'",
						"responded_at", "CURRENT_TIMESTAMP", "decline_reason", "'Busy this week'")),
				Arguments.of("DECLINED reason surrounded by whitespace", Map.of("status", "'DECLINED'",
						"responded_at", "CURRENT_TIMESTAMP", "decline_reason", "E'\\t Busy \\n'")),
				Arguments.of("REVOKED while PENDING keeps NULL responded_at", Map.of("status", "'REVOKED'")),
				Arguments.of("REVOKED after accept keeps responded_at", Map.of("status", "'REVOKED'",
						"responded_at", "CURRENT_TIMESTAMP")));
	}

	@ParameterizedTest(name = "decline_reason = {0}")
	@MethodSource("missingOrBlankTexts")
	void declinedAssignmentRequiresNonBlankReason(String caseName, String literal) {
		Fixture f = fixture("pg-assign-decline");
		Map<String, String> row = new LinkedHashMap<>(f.assignment());
		row.putAll(Map.of("status", "'DECLINED'", "responded_at", "CURRENT_TIMESTAMP", "decline_reason", literal));
		assertRejected(() -> insertRow("task_assignments", row), "ck_task_assignments_decline_reason");
	}

	static Stream<Arguments> missingOrBlankTexts() {
		return Stream.concat(Stream.of(Arguments.of("NULL", "NULL")), blankTexts());
	}

	/** Present but without any non-whitespace character. */
	static Stream<Arguments> blankTexts() {
		return Stream.of(
				Arguments.of("empty", "''"),
				Arguments.of("spaces", "'   '"),
				Arguments.of("tabs and newlines", "E'\\t\\n\\r '"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidAssignmentStates")
	void assignmentsRejectInvalidStates(String caseName, Map<String, String> overrides, String violation) {
		Fixture f = fixture("pg-assign-invalid");
		Map<String, String> row = new LinkedHashMap<>(f.assignment());
		row.putAll(overrides);
		assertRejected(() -> insertRow("task_assignments", row), violation);
	}

	static Stream<Arguments> invalidAssignmentStates() {
		return Stream.of(
				Arguments.of("unknown status", Map.of("status", "'DONE'"), "ck_task_assignments_status"),
				Arguments.of("PENDING with responded_at", Map.of("responded_at", "CURRENT_TIMESTAMP"),
						"ck_task_assignments_responded_at"),
				Arguments.of("ACCEPTED without responded_at", Map.of("status", "'ACCEPTED'"),
						"ck_task_assignments_responded_at"),
				Arguments.of("DECLINED without responded_at", Map.of("status", "'DECLINED'",
						"decline_reason", "'Busy'"), "ck_task_assignments_responded_at"),
				Arguments.of("unknown task", Map.of("task_id", "-1"), "fk_task_assignments_task"),
				Arguments.of("unknown assignee", Map.of("assignee_user_id", "-1"), "fk_task_assignments_assignee"),
				Arguments.of("unknown assigner", Map.of("assigned_by_user_id", "-1"),
						"fk_task_assignments_assigned_by"));
	}

	// ---------------------------------------------------------------- V9 ai_request_logs

	@ParameterizedTest(name = "{0}")
	@MethodSource("validAiLogs")
	void aiLogsAcceptValidRows(String caseName, Map<String, String> overrides) {
		long userId = insertUser("pg-ai-valid@example.com");
		Map<String, String> row = new LinkedHashMap<>(with(AI_OK, "user_id", String.valueOf(userId)));
		row.putAll(overrides);
		insertRow("ai_request_logs", row);
	}

	static Stream<Arguments> validAiLogs() {
		return Stream.of(
				Arguments.of("success with known usage", Map.of()),
				Arguments.of("success with known zero tokens", Map.of("tokens_used", "0")),
				Arguments.of("failure with unknown usage", Map.of("tokens_used", "NULL", "success", "FALSE",
						"error_code", "'AI_UNAVAILABLE'")));
	}

	@Test
	void unknownTokenUsageStaysNullAndKnownZeroStaysZero() {
		long userId = insertUser("pg-ai-tokens@example.com");
		long unknown = insertRow("ai_request_logs", with(AI_OK, "user_id", String.valueOf(userId),
				"tokens_used", "NULL", "success", "FALSE", "error_code", "'AI_UNAVAILABLE'"));
		long zero = insertRow("ai_request_logs", with(AI_OK, "user_id", String.valueOf(userId), "tokens_used", "0"));

		assertThat(jdbc.queryForObject("SELECT tokens_used FROM ai_request_logs WHERE id = ?", Integer.class, unknown))
				.isNull();
		assertThat(jdbc.queryForObject("SELECT tokens_used FROM ai_request_logs WHERE id = ?", Integer.class, zero))
				.isZero();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidAiLogs")
	void aiLogsRejectInvalidRows(String caseName, Map<String, String> overrides, String violation) {
		long userId = insertUser("pg-ai-invalid@example.com");
		Map<String, String> row = new LinkedHashMap<>(with(AI_OK, "user_id", String.valueOf(userId)));
		row.putAll(overrides);
		assertRejected(() -> insertRow("ai_request_logs", row), violation);
	}

	static Stream<Arguments> invalidAiLogs() {
		return Stream.of(
				Arguments.of("unknown request type", Map.of("request_type", "'SUMMARIZE'"),
						"ck_ai_request_logs_request_type"),
				Arguments.of("negative tokens", Map.of("tokens_used", "-1"), "ck_ai_request_logs_tokens_used"),
				Arguments.of("negative latency", Map.of("latency_ms", "-1"), "ck_ai_request_logs_latency"),
				Arguments.of("success with error code", Map.of("error_code", "'AI_UNAVAILABLE'"),
						"ck_ai_request_logs_outcome"),
				Arguments.of("failure without error code", Map.of("success", "FALSE"), "ck_ai_request_logs_outcome"),
				Arguments.of("null latency", Map.of("latency_ms", "NULL"), "column \"latency_ms\""));
	}

	@Test
	void aiLogsRejectUnknownUser() {
		assertRejected(() -> insertRow("ai_request_logs", with(AI_OK, "user_id", "-1")), "fk_ai_request_logs_user");
	}

	// ---------------------------------------------------------------- indexes

	@Test
	void indexesExist() {
		List<String> names = jdbc.queryForList("SELECT indexname FROM pg_indexes WHERE tablename IN "
				+ "('group_members', 'task_assignments', 'ai_request_logs')", String.class);
		assertThat(names).contains("uk_group_members_group_user", "idx_group_members_user",
				"uk_task_assignments_active_task", "idx_task_assignments_assignee_status", "idx_task_assignments_task",
				"idx_ai_request_logs_user_created");
		assertThat(jdbc.queryForObject("SELECT indexdef FROM pg_indexes WHERE indexname = 'uk_task_assignments_active_task'",
				String.class))
				.contains("UNIQUE").contains("(task_id)").contains("'PENDING'").contains("'ACCEPTED'");
	}

	// ---------------------------------------------------------------- helpers

	/** A group with a leader, a member and one group task; the assignment row is valid as PENDING. */
	private record Fixture(long leaderId, long memberId, long taskId) {
		Map<String, String> assignment() {
			return Map.of("task_id", String.valueOf(taskId), "assignee_user_id", String.valueOf(memberId),
					"assigned_by_user_id", String.valueOf(leaderId));
		}
	}

	private Fixture fixture(String prefix) {
		long leaderId = insertUser(prefix + "-leader@example.com");
		long memberId = insertUser(prefix + "-member@example.com");
		long groupId = insertGroup(leaderId);
		return new Fixture(leaderId, memberId, insertTask(leaderId, groupId));
	}

	private static Map<String, String> member(long groupId, long userId) {
		return with(MEMBER, "group_id", String.valueOf(groupId), "user_id", String.valueOf(userId));
	}

	private long insertGroup(long creatorId) {
		return insertRow("team_groups", Map.of("created_by_user_id", String.valueOf(creatorId),
				"group_name", "'Team A'", "description", "''"));
	}

	private long insertTask(long ownerId, Long groupId) {
		return insertRow("tasks", with(TASK, "owner_user_id", String.valueOf(ownerId),
				"group_id", groupId == null ? "NULL" : String.valueOf(groupId)));
	}

	/** Column values are SQL literals from this class (test data only, never user input). */
	private long insertRow(String table, Map<String, String> columns) {
		String sql = "INSERT INTO " + table + " (" + String.join(", ", columns.keySet()) + ") VALUES ("
				+ String.join(", ", columns.values()) + ") RETURNING id";
		Long id = jdbc.queryForObject(sql, Long.class);
		assertThat(id).isNotNull();
		return id;
	}

	private static Map<String, String> with(Map<String, String> base, String... overrides) {
		Map<String, String> row = new LinkedHashMap<>(base);
		for (int i = 0; i < overrides.length; i += 2) {
			row.put(overrides[i], overrides[i + 1]);
		}
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
