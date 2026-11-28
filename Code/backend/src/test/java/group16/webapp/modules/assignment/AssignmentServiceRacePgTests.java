package group16.webapp.modules.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.assignment.dto.AssignmentCreateRequest;
import group16.webapp.modules.assignment.repository.TaskAssignmentRepository;
import group16.webapp.modules.assignment.service.AssignmentService;
import group16.webapp.support.PostgresContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;

/**
 * AS-PG: when two creates for the same task both pass the BR-04 pre-check (a race), the V8 partial unique index
 * rejects the second insert and the service answers 409 ASSIGNMENT_ALREADY_ACTIVE. The race is simulated by making
 * the pre-check always report "no active assignment". Rows are committed (no test transaction) and removed after.
 */
class AssignmentServiceRacePgTests extends PostgresContainerTest {

	@Autowired
	private AssignmentService service;

	@MockitoSpyBean
	private TaskAssignmentRepository repository;

	@Autowired
	private JdbcTemplate jdbc;

	private final List<Long> userIds = new ArrayList<>();
	private Long groupId;

	@AfterEach
	void cleanUp() {
		if (groupId != null) {
			jdbc.update("DELETE FROM task_assignments WHERE task_id IN (SELECT id FROM tasks WHERE group_id = ?)",
					groupId);
			jdbc.update("DELETE FROM tasks WHERE group_id = ?", groupId);
			jdbc.update("DELETE FROM group_members WHERE group_id = ?", groupId);
			jdbc.update("DELETE FROM team_groups WHERE id = ?", groupId);
		}
		userIds.forEach(id -> jdbc.update("DELETE FROM users WHERE id = ?", id));
		userIds.clear();
		groupId = null;
	}

	@Test
	void concurrentCreateThatPassesThePreCheckIsRejectedByTheIndex() {
		long leader = insertUser("race-leader@example.com");
		long memberA = insertUser("race-a@example.com");
		long memberB = insertUser("race-b@example.com");
		groupId = jdbc.queryForObject("INSERT INTO team_groups (created_by_user_id, group_name, description) "
				+ "VALUES (?, 'Race', '') RETURNING id", Long.class, leader);
		jdbc.update("INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'LEADER')", groupId, leader);
		jdbc.update("INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'MEMBER')", groupId, memberA);
		jdbc.update("INSERT INTO group_members (group_id, user_id, role) VALUES (?, ?, 'MEMBER')", groupId, memberB);
		Long taskId = jdbc.queryForObject("INSERT INTO tasks (owner_user_id, group_id, title, description, priority, "
				+ "category, estimated_duration_minutes) VALUES (?, ?, 'Report', '', 'HIGH', 'PROJECT', 60) "
				+ "RETURNING id", Long.class, leader, groupId);
		doReturn(false).when(repository).existsByTaskIdAndStatusIn(anyLong(), any());

		service.create(leader, new AssignmentCreateRequest(memberA, null, taskId));

		assertThatThrownBy(() -> service.create(leader, new AssignmentCreateRequest(memberB, null, taskId)))
				.isInstanceOfSatisfying(AppException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.ASSIGNMENT_ALREADY_ACTIVE));
		assertThat(jdbc.queryForList("SELECT assignee_user_id FROM task_assignments WHERE task_id = ?", Long.class,
				taskId)).containsExactly(memberA);
	}

	private long insertUser(String email) {
		Long id = jdbc.queryForObject(
				"INSERT INTO users (email, password_hash, full_name) VALUES (?, 'x', 'Race') RETURNING id",
				Long.class, email);
		assertThat(id).isNotNull();
		userIds.add(id);
		return id;
	}
}
