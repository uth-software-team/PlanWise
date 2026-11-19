package group16.webapp.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import group16.webapp.support.PostgresContainerTest;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * V4 tasks, V5 availability_rules and V6 fixed_events on real PostgreSQL 16: defaults, CHECK / NOT NULL / FK
 * constraints and indexes. Each rejected row breaks exactly one constraint, and the failing statement is the last
 * one in its test because PostgreSQL aborts the transaction after an error. Data changes roll back after each test.
 * The tasks.group_id foreign key arrives with V7 and is tested in GroupAssignmentAiMigrationPgTests (GR-01).
 */
@Transactional
class TaskAvailabilityMigrationPgTests extends PostgresContainerTest {

	private static final Map<String, String> TASK = Map.of(
			"title", "'Write report'",
			"description", "''",
			"priority", "'MEDIUM'",
			"category", "'WORK'",
			"estimated_duration_minutes", "30");

	private static final Map<String, String> RULE = Map.of(
			"day_of_week", "1",
			"start_time", "TIME '09:00'",
			"end_time", "TIME '17:00'");

	private static final Map<String, String> ONCE = Map.of(
			"title", "'Lecture'",
			"recurrence_type", "'ONCE'",
			"start_at", "TIMESTAMPTZ '2026-11-20 09:00+07'",
			"end_at", "TIMESTAMPTZ '2026-11-20 11:00+07'");

	private static final Map<String, String> WEEKLY = Map.of(
			"title", "'Gym'",
			"recurrence_type", "'WEEKLY'",
			"day_of_week", "1",
			"start_time", "TIME '09:00'",
			"end_time", "TIME '11:00'");

	@Autowired
	private JdbcTemplate jdbc;

	// ---------------------------------------------------------------- V4 tasks

	@Test
	void newTaskDefaultsToTodoWithVersionZeroAndNoGroup() {
		long taskId = insertRow("tasks", "owner_user_id", insertUser("pg-task-default@example.com"), TASK);

		Map<String, Object> row = jdbc.queryForMap(
				"SELECT status, version, group_id, deleted_at FROM tasks WHERE id = ?", taskId);
		assertThat(row).containsEntry("status", "TODO").containsEntry("version", 0);
		assertThat(row.get("group_id")).isNull();
		assertThat(row.get("deleted_at")).isNull();
	}

	@ParameterizedTest(name = "{0} = {1}")
	@MethodSource("documentedTaskEnumValues")
	void tasksAcceptEveryDocumentedEnumValue(String column, String value) {
		insertRow("tasks", "owner_user_id", insertUser("pg-task-enum@example.com"),
				with(TASK, column, "'" + value + "'"));
	}

	static Stream<Arguments> documentedTaskEnumValues() {
		return Stream.of(
				Stream.of("LOW", "MEDIUM", "HIGH", "URGENT").map(v -> Arguments.of("priority", v)),
				Stream.of("STUDY", "WORK", "PERSONAL", "PROJECT", "OTHER").map(v -> Arguments.of("category", v)),
				Stream.of("TODO", "IN_PROGRESS", "COMPLETED", "CANCELLED").map(v -> Arguments.of("status", v)))
				.flatMap(s -> s);
	}

	@Test
	void tasksAcceptDurationBoundariesEmptyDescriptionAndDeadline() {
		insertRow("tasks", "owner_user_id", insertUser("pg-task-bounds@example.com"), with(TASK,
				"estimated_duration_minutes", "1",
				"actual_duration_minutes", "0",
				"deadline", "TIMESTAMPTZ '2026-11-30 18:00+07'",
				"title", "E'\\t Write report \\n'"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidTasks")
	void tasksRejectInvalidRows(String caseName, Map<String, String> row, String violation) {
		long userId = insertUser("pg-task-invalid@example.com");
		assertRejected(() -> insertRow("tasks", "owner_user_id", userId, row), violation);
	}

	static Stream<Arguments> invalidTasks() {
		return Stream.of(
				Arguments.of("empty title", with(TASK, "title", "''"), "ck_tasks_title_not_blank"),
				Arguments.of("spaces title", with(TASK, "title", "'   '"), "ck_tasks_title_not_blank"),
				Arguments.of("tabs/newlines title", with(TASK, "title", "E'\\t\\n\\r '"), "ck_tasks_title_not_blank"),
				Arguments.of("unknown priority", with(TASK, "priority", "'CRITICAL'"), "ck_tasks_priority"),
				Arguments.of("lower-case priority", with(TASK, "priority", "'medium'"), "ck_tasks_priority"),
				Arguments.of("unknown category", with(TASK, "category", "'HOBBY'"), "ck_tasks_category"),
				Arguments.of("unknown status", with(TASK, "status", "'DONE'"), "ck_tasks_status"),
				Arguments.of("zero estimate", with(TASK, "estimated_duration_minutes", "0"),
						"ck_tasks_estimated_duration_positive"),
				Arguments.of("negative estimate", with(TASK, "estimated_duration_minutes", "-5"),
						"ck_tasks_estimated_duration_positive"),
				Arguments.of("negative actual duration", with(TASK, "actual_duration_minutes", "-1"),
						"ck_tasks_actual_duration_non_negative"),
				Arguments.of("null description", with(TASK, "description", "NULL"), "column \"description\""),
				Arguments.of("null priority", with(TASK, "priority", "NULL"), "column \"priority\""),
				Arguments.of("null category", with(TASK, "category", "NULL"), "column \"category\""),
				Arguments.of("null estimate", with(TASK, "estimated_duration_minutes", "NULL"),
						"column \"estimated_duration_minutes\""));
	}

	@Test
	void tasksRejectUnknownOwner() {
		assertRejected(() -> insertRow("tasks", "owner_user_id", -1L, TASK), "fk_tasks_owner_user");
	}

	// ---------------------------------------------------------------- V5 availability_rules

	@ParameterizedTest(name = "day_of_week = {0}")
	@ValueSource(ints = {1, 7})
	void rulesAcceptIsoWeekdayBounds(int day) {
		insertRow("availability_rules", "user_id", insertUser("pg-rule-day@example.com"),
				with(RULE, "day_of_week", String.valueOf(day)));
	}

	@Test
	void ruleIsActiveByDefaultAndMayStartAtMidnight() {
		long ruleId = insertRow("availability_rules", "user_id", insertUser("pg-rule-active@example.com"),
				with(RULE, "start_time", "TIME '00:00'", "end_time", "TIME '08:00'"));

		assertThat(jdbc.queryForObject("SELECT is_active FROM availability_rules WHERE id = ?", Boolean.class, ruleId))
				.isTrue();
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidRules")
	void rulesRejectInvalidRows(String caseName, Map<String, String> row, String violation) {
		long userId = insertUser("pg-rule-invalid@example.com");
		assertRejected(() -> insertRow("availability_rules", "user_id", userId, row), violation);
	}

	static Stream<Arguments> invalidRules() {
		return Stream.of(
				Arguments.of("day 0", with(RULE, "day_of_week", "0"), "ck_availability_rules_day_of_week"),
				Arguments.of("day 8", with(RULE, "day_of_week", "8"), "ck_availability_rules_day_of_week"),
				Arguments.of("start = end", with(RULE, "end_time", "TIME '09:00'"), "ck_availability_rules_time_range"),
				Arguments.of("reversed", with(RULE, "start_time", "TIME '17:00'", "end_time", "TIME '09:00'"),
						"ck_availability_rules_time_range"),
				Arguments.of("ends at 24:00", with(RULE, "end_time", "TIME '24:00'"),
						"ck_availability_rules_time_range"),
				Arguments.of("null day", with(RULE, "day_of_week", "NULL"), "column \"day_of_week\""),
				Arguments.of("null end", with(RULE, "end_time", "NULL"), "column \"end_time\""));
	}

	@Test
	void rulesRejectUnknownUser() {
		assertRejected(() -> insertRow("availability_rules", "user_id", -1L, RULE), "fk_availability_rules_user");
	}

	// ---------------------------------------------------------------- V6 fixed_events

	@ParameterizedTest(name = "{0}")
	@MethodSource("validFixedEvents")
	void fixedEventsAcceptValidRows(String caseName, Map<String, String> row) {
		insertRow("fixed_events", "user_id", insertUser("pg-event-valid@example.com"), row);
	}

	static Stream<Arguments> validFixedEvents() {
		return Stream.of(
				Arguments.of("ONCE", ONCE),
				Arguments.of("ONCE ending at midnight the next day", with(ONCE,
						"start_at", "TIMESTAMPTZ '2026-11-20 22:00+07'",
						"end_at", "TIMESTAMPTZ '2026-11-21 00:00+07'")),
				Arguments.of("ONCE spanning several days", with(ONCE,
						"end_at", "TIMESTAMPTZ '2026-11-22 10:00+07'")),
				Arguments.of("ONCE with unused weekly fields (no DB exclusivity)", with(ONCE,
						"day_of_week", "3", "start_time", "TIME '08:00'", "end_time", "TIME '09:00'")),
				Arguments.of("WEEKLY", WEEKLY),
				Arguments.of("WEEKLY on Sunday from midnight", with(WEEKLY,
						"day_of_week", "7", "start_time", "TIME '00:00'")),
				Arguments.of("WEEKLY open-ended", with(WEEKLY, "effective_from", "DATE '2026-11-23'")),
				Arguments.of("WEEKLY single-day effective range", with(WEEKLY,
						"effective_from", "DATE '2026-11-23'", "effective_until", "DATE '2026-11-23'")));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidFixedEvents")
	void fixedEventsRejectInvalidRows(String caseName, Map<String, String> row, String violation) {
		long userId = insertUser("pg-event-invalid@example.com");
		assertRejected(() -> insertRow("fixed_events", "user_id", userId, row), violation);
	}

	static Stream<Arguments> invalidFixedEvents() {
		return Stream.of(
				Arguments.of("spaces title", with(ONCE, "title", "'  '"), "ck_fixed_events_title_not_blank"),
				Arguments.of("tabs/newlines title", with(ONCE, "title", "E'\\t\\n'"), "ck_fixed_events_title_not_blank"),
				Arguments.of("null recurrence type", with(ONCE, "recurrence_type", "NULL"),
						"column \"recurrence_type\""),
				Arguments.of("unknown recurrence type", with(ONCE, "recurrence_type", "'DAILY'"),
						"ck_fixed_events_recurrence_type"),
				Arguments.of("lower-case recurrence type", with(ONCE, "recurrence_type", "'once'"),
						"ck_fixed_events_recurrence_type"),
				Arguments.of("ONCE without end_at", with(ONCE, "end_at", "NULL"), "ck_fixed_events_once"),
				Arguments.of("ONCE without timestamps", with(ONCE, "start_at", "NULL", "end_at", "NULL"),
						"ck_fixed_events_once"),
				Arguments.of("WEEKLY without day_of_week", with(WEEKLY, "day_of_week", "NULL"),
						"ck_fixed_events_weekly"),
				Arguments.of("WEEKLY without end_time", with(WEEKLY, "end_time", "NULL"), "ck_fixed_events_weekly"),
				Arguments.of("ONCE with day_of_week 9", with(ONCE, "day_of_week", "9"),
						"ck_fixed_events_day_of_week"),
				Arguments.of("WEEKLY day 0", with(WEEKLY, "day_of_week", "0"), "ck_fixed_events_day_of_week"),
				Arguments.of("WEEKLY 09:00-24:00", with(WEEKLY, "end_time", "TIME '24:00'"),
						"ck_fixed_events_time_range"),
				Arguments.of("WEEKLY reversed times", with(WEEKLY,
						"start_time", "TIME '11:00'", "end_time", "TIME '09:00'"), "ck_fixed_events_time_range"),
				Arguments.of("WEEKLY start = end", with(WEEKLY, "end_time", "TIME '09:00'"),
						"ck_fixed_events_time_range"),
				Arguments.of("ONCE with lone start_time 24:00", with(ONCE, "start_time", "TIME '24:00'"),
						"ck_fixed_events_time_range"),
				Arguments.of("ONCE start = end", with(ONCE, "end_at", "TIMESTAMPTZ '2026-11-20 09:00+07'"),
						"ck_fixed_events_at_range"),
				Arguments.of("ONCE reversed", with(ONCE, "end_at", "TIMESTAMPTZ '2026-11-20 08:00+07'"),
						"ck_fixed_events_at_range"),
				Arguments.of("WEEKLY with reversed unused timestamps", with(WEEKLY,
						"start_at", "TIMESTAMPTZ '2026-11-20 11:00+07'",
						"end_at", "TIMESTAMPTZ '2026-11-20 09:00+07'"), "ck_fixed_events_at_range"),
				Arguments.of("effective_until before effective_from", with(WEEKLY,
						"effective_from", "DATE '2026-11-24'", "effective_until", "DATE '2026-11-23'"),
						"ck_fixed_events_effective_range"));
	}

	@Test
	void fixedEventsRejectUnknownUser() {
		assertRejected(() -> insertRow("fixed_events", "user_id", -1L, ONCE), "fk_fixed_events_user");
	}

	// ---------------------------------------------------------------- indexes

	@Test
	void foreignKeyIndexesExist() {
		List<String> names = jdbc.queryForList(
				"SELECT indexname FROM pg_indexes WHERE tablename IN ('tasks', 'availability_rules', 'fixed_events')",
				String.class);
		assertThat(names).contains(
				"idx_tasks_owner_user", "idx_tasks_group", "idx_availability_rules_user_day", "idx_fixed_events_user");
	}

	// ---------------------------------------------------------------- helpers

	/** Column values are SQL literals from the constants above (test data only, never user input). */
	private long insertRow(String table, String ownerColumn, long ownerId, Map<String, String> columns) {
		String sql = "INSERT INTO " + table + " (" + ownerColumn + ", " + String.join(", ", columns.keySet())
				+ ") VALUES (?, " + String.join(", ", columns.values()) + ") RETURNING id";
		Long id = jdbc.queryForObject(sql, Long.class, ownerId);
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
