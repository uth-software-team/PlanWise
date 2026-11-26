package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.service.GroupService;
import group16.webapp.support.PostgresContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

/**
 * Group creation is one transaction: when PostgreSQL itself rejects the LEADER membership row, the new group must
 * not remain. Deliberately not {@code @Transactional}, so the service's own transaction commits or rolls back.
 * A temporary CHECK constraint (NOT VALID, so it only checks new rows) rejects memberships of the test user only.
 */
class GroupServicePostgresRollbackTests extends PostgresContainerTest {

	private static final String CONSTRAINT = "ck_test_reject_membership_user";

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JdbcTemplate jdbc;

	private Long userId;

	@AfterEach
	void cleanUp() {
		// Runs even when the test failed; each step is independent so one failure cannot skip the rest.
		try {
			jdbc.execute("ALTER TABLE group_members DROP CONSTRAINT IF EXISTS " + CONSTRAINT);
		} finally {
			if (userId != null) {
				jdbc.update("DELETE FROM group_members WHERE user_id = ?", userId);
				jdbc.update("DELETE FROM team_groups WHERE created_by_user_id = ?", userId);
				jdbc.update("DELETE FROM users WHERE id = ?", userId);
			}
		}
	}

	@Test
	void postgresRejectingMembershipInsertRollsBackTheNewGroup() {
		userId = userRepository.save(new User("pg-rollback@example.com", "$2a$10$hash", "Rollback", null)).getId();
		jdbc.execute("ALTER TABLE group_members ADD CONSTRAINT " + CONSTRAINT
				+ " CHECK (user_id <> " + userId + ") NOT VALID");

		assertThatThrownBy(() -> groupService.createGroup(userId, new GroupRequest("Team", "desc")))
				.isInstanceOf(DataIntegrityViolationException.class)
				.rootCause()
				.isInstanceOfSatisfying(SQLException.class, e -> {
					assertThat(e.getSQLState()).isEqualTo("23514");
					assertThat(e.getMessage()).contains(CONSTRAINT);
				});

		assertThat(count("team_groups", "created_by_user_id")).as("group rolled back").isZero();
		assertThat(count("group_members", "user_id")).as("membership not stored").isZero();
	}

	private int count(String table, String userColumn) {
		return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + userColumn + " = ?",
				Integer.class, userId);
	}
}
