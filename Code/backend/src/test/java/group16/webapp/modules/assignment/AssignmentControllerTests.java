package group16.webapp.modules.assignment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import group16.webapp.modules.assignment.api.AssignmentApi;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.modules.task.api.SchedulableTask;
import group16.webapp.modules.task.api.TaskApi;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * AS-01/AS-02 through the real API (H2, real JWTs). Rows commit like in production and are removed after each test.
 * Leader rights are checked per request (D16); AS-T1 covers a leadership transfer. The V8 partial unique index is
 * not on H2: its behaviour is covered by the {@code @Tag("pg")} tests (AS-PG).
 */
class AssignmentControllerTests extends AuthenticatedMvcTest {

	private static final String CALLER_EMAIL = "caller@example.com";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TeamGroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private AssignmentApi assignmentApi;

	@Autowired
	private TaskApi taskApi;

	private final List<Long> extraUserIds = new ArrayList<>();

	/** Caller (A) leads the group; B and C are ACTIVE members. */
	private User leader;
	private User memberB;
	private User memberC;
	private TeamGroup group;

	@BeforeEach
	void seed() {
		leader = userRepository.findByEmail(CALLER_EMAIL).orElseThrow();
		memberB = newUser("b@example.com");
		memberC = newUser("c@example.com");
		group = groupRepository.save(new TeamGroup(leader.getId(), "Team A", ""));
		addMember(group, leader, GroupRole.LEADER);
		addMember(group, memberB, GroupRole.MEMBER);
		addMember(group, memberC, GroupRole.MEMBER);
	}

	@AfterEach
	void cleanUp() {
		jdbc.update("DELETE FROM task_assignments");
		jdbc.update("DELETE FROM tasks");
		memberRepository.deleteAllInBatch();
		groupRepository.deleteAllInBatch();
		userRepository.deleteAllById(extraUserIds);
		extraUserIds.clear();
	}

	// ---------- POST /v1/assignments (AS-01) ----------

	@Test
	void leaderCreatesGroupTaskAndPendingAssignment() throws Exception {
		callAuthenticated(HttpMethod.POST, "/v1/assignments", newTaskBody(group.getId(), memberC.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andExpect(jsonPath("$.data.assignmentStatus").value("PENDING"))
				.andExpect(jsonPath("$.data.assigneeUserId").value(memberC.getId()))
				.andExpect(jsonPath("$.data.assignedByUserId").value(leader.getId()))
				.andExpect(jsonPath("$.data.declineReason").value(nullValue()))
				.andExpect(jsonPath("$.data.respondedAt").value(nullValue()))
				.andExpect(jsonPath("$.data.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.data.task.title").value("Write report"))
				.andExpect(jsonPath("$.data.task.priority").value("HIGH"))
				.andExpect(jsonPath("$.data.task.taskStatus").value("TODO"))
				.andExpect(jsonPath("$.data.task.deadline").value("2026-11-30T11:00:00Z"));

		assertThat(jdbc.queryForList("SELECT group_id FROM tasks", Long.class)).containsExactly(group.getId());
		assertThat(jdbc.queryForList("SELECT owner_user_id FROM tasks", Long.class)).containsExactly(leader.getId());
	}

	@Test
	void leaderMayAssignATaskToThemself() throws Exception {
		callAuthenticated(HttpMethod.POST, "/v1/assignments", newTaskBody(group.getId(), leader.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.assigneeUserId").value(leader.getId()));
	}

	@Test
	void nonLeaderCannotCreateAndNoTaskIsCreated() throws Exception {
		callAs(memberB, HttpMethod.POST, "/v1/assignments", newTaskBody(group.getId(), memberC.getId()))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasks", Long.class)).isZero();
	}

	@Test
	void unknownGroupIs404() throws Exception {
		callAuthenticated(HttpMethod.POST, "/v1/assignments", newTaskBody(group.getId() + 1000, memberC.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void assigneeMustBeAnActiveMemberWithAnActiveAccount() throws Exception {
		User outsider = newUser("outsider@example.com");
		User leftMember = newUser("left@example.com");
		GroupMember left = addMember(group, leftMember, GroupRole.MEMBER);
		jdbc.update("UPDATE group_members SET status = 'INACTIVE', left_at = ? WHERE id = ?",
				java.sql.Timestamp.from(left.getJoinedAt().plusSeconds(1)), left.getId());
		memberB.deactivate();
		userRepository.save(memberB);

		for (Long assignee : List.of(outsider.getId(), leftMember.getId(), memberB.getId(), -1L)) {
			callAuthenticated(HttpMethod.POST, "/v1/assignments", newTaskBody(group.getId(), assignee))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasks", Long.class)).isZero();
	}

	@Test
	void reassignmentNeedsNoActiveAssignment() throws Exception {
		long first = createAssignment(memberC);
		long taskId = taskIdOf(first);

		// PENDING -> 409
		callAuthenticated(HttpMethod.POST, "/v1/assignments", reassignBody(taskId, memberB.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ASSIGNMENT_ALREADY_ACTIVE"));

		// ACCEPTED -> 409
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + first + "/accept", null).andExpect(status().isOk());
		callAuthenticated(HttpMethod.POST, "/v1/assignments", reassignBody(taskId, memberB.getId()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ASSIGNMENT_ALREADY_ACTIVE"));

		// REVOKED -> a new assignment of the same task is allowed
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + first + "/revoke", null).andExpect(status().isOk());
		callAuthenticated(HttpMethod.POST, "/v1/assignments", reassignBody(taskId, memberB.getId()))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.task.taskId").value(taskId))
				.andExpect(jsonPath("$.data.assigneeUserId").value(memberB.getId()))
				.andExpect(jsonPath("$.data.assignmentStatus").value("PENDING"));

		assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tasks", Long.class)).isEqualTo(1L);
	}

	@Test
	void reassignmentAfterDeclineIsAllowed() throws Exception {
		long first = createAssignment(memberC);
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + first + "/decline", "{\"reason\":\"Busy\"}")
				.andExpect(status().isOk());

		callAuthenticated(HttpMethod.POST, "/v1/assignments", reassignBody(taskIdOf(first), memberB.getId()))
				.andExpect(status().isCreated());
	}

	@Test
	void reassigningPersonalOrMissingTaskIsTheSame404() throws Exception {
		String personal = callAuthenticated(HttpMethod.POST, "/v1/tasks", "{\"title\":\"Mine\",\"priority\":\"LOW\","
				+ "\"category\":\"PERSONAL\",\"estimatedDurationMinutes\":30}")
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		long personalId = ((Number) JsonPath.read(personal, "$.data.id")).longValue();

		for (long taskId : new long[] {personalId, personalId + 1000}) {
			callAuthenticated(HttpMethod.POST, "/v1/assignments", reassignBody(taskId, memberC.getId()))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
		}
	}

	// ---------- GET /v1/assignments ----------

	@Test
	void inboxListsOnlyTheCallersAssignmentsWithStatusFilter() throws Exception {
		long toC = createAssignment(memberC);
		long toB = createAssignment(memberB);
		long toC2 = createAssignment(memberC);
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + toC + "/accept", null).andExpect(status().isOk());

		callAs(memberC, HttpMethod.GET, "/v1/assignments", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id").value(contains((int) toC2, (int) toC)));
		callAs(memberC, HttpMethod.GET, "/v1/assignments?status=ACCEPTED", null)
				.andExpect(jsonPath("$.data[*].id").value(contains((int) toC)));
		callAs(memberB, HttpMethod.GET, "/v1/assignments", null)
				.andExpect(jsonPath("$.data[*].id").value(contains((int) toB)));
		callAuthenticated(HttpMethod.GET, "/v1/assignments", null)
				.andExpect(jsonPath("$.data", hasSize(0)));
	}

	@Test
	void groupListNeedsTheLeader() throws Exception {
		long toC = createAssignment(memberC);
		long toB = createAssignment(memberB);

		callAuthenticated(HttpMethod.GET, "/v1/assignments?groupId=" + group.getId(), null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id").value(contains((int) toB, (int) toC)));
		callAuthenticated(HttpMethod.GET, "/v1/assignments?groupId=" + group.getId() + "&status=REVOKED", null)
				.andExpect(jsonPath("$.data", hasSize(0)));
		callAs(memberB, HttpMethod.GET, "/v1/assignments?groupId=" + group.getId(), null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		callAuthenticated(HttpMethod.GET, "/v1/assignments?groupId=" + (group.getId() + 1000), null)
				.andExpect(status().isNotFound());
	}

	// ---------- accept / decline (AS-02, BR-05) ----------

	@Test
	void assigneeAcceptsOnceAndOthersCannot() throws Exception {
		long id = createAssignment(memberC);

		callAs(memberB, HttpMethod.POST, "/v1/assignments/" + id + "/accept", null)
				.andExpect(status().isForbidden());
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + id + "/accept", null)
				.andExpect(status().isForbidden());
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + id + "/accept", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.assignmentStatus").value("ACCEPTED"))
				.andExpect(jsonPath("$.data.respondedAt").isNotEmpty());
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + id + "/accept", null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + id + "/decline", "{\"reason\":\"Late\"}")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + (id + 1000) + "/accept", null)
				.andExpect(status().isNotFound());
	}

	@Test
	void assigneeDeclinesWithAStrippedReason() throws Exception {
		long id = createAssignment(memberC);

		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + id + "/decline", "{\"reason\":\"  Busy this week \"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.assignmentStatus").value("DECLINED"))
				.andExpect(jsonPath("$.data.declineReason").value("Busy this week"))
				.andExpect(jsonPath("$.data.respondedAt").isNotEmpty());
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + id + "/accept", null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
	}

	// ---------- revoke ----------

	@Test
	void leaderRevokesPendingAndAcceptedAssignmentsKeepingRespondedAt() throws Exception {
		long pending = createAssignment(memberC);
		long accepted = createAssignment(memberB);
		String acceptedAt = JsonPath.read(callAs(memberB, HttpMethod.POST, "/v1/assignments/" + accepted + "/accept",
				null).andReturn().getResponse().getContentAsString(), "$.data.respondedAt");

		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + pending + "/revoke", null)
				.andExpect(status().isForbidden());
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + pending + "/revoke", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.assignmentStatus").value("REVOKED"))
				.andExpect(jsonPath("$.data.respondedAt").value(nullValue()));
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + accepted + "/revoke", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.assignmentStatus").value("REVOKED"))
				.andExpect(jsonPath("$.data.respondedAt").value(acceptedAt));
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + pending + "/revoke", null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + (pending + 1000) + "/revoke", null)
				.andExpect(status().isNotFound());
	}

	/** AS-T1 (D16): rights follow the current LEADER, not assigned_by_user_id. */
	@Test
	void afterLeadershipTransferTheNewLeaderManagesOldAssignments() throws Exception {
		long id = createAssignment(memberC);
		callAuthenticated(HttpMethod.POST, "/v1/groups/" + group.getId() + "/transfer-leadership",
				"{\"newLeaderUserId\":" + memberB.getId() + "}")
				.andExpect(status().isOk());

		callAuthenticated(HttpMethod.GET, "/v1/assignments?groupId=" + group.getId(), null)
				.andExpect(status().isForbidden());
		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + id + "/revoke", null)
				.andExpect(status().isForbidden());
		callAs(memberB, HttpMethod.GET, "/v1/assignments?groupId=" + group.getId(), null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[*].id").value(contains((int) id)))
				.andExpect(jsonPath("$.data[0].assignedByUserId").value(leader.getId()));
		callAs(memberB, HttpMethod.POST, "/v1/assignments/" + id + "/revoke", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.assignmentStatus").value("REVOKED"));
	}

	// ---------- AssignmentApi (HC-08, used by schedule generate/confirm) ----------

	@Test
	void acceptedTaskIdsFeedTheSchedulableTasksOfTheAssignee() throws Exception {
		long accepted = createAssignment(memberC);
		long pending = createAssignment(memberC);
		long declined = createAssignment(memberC);
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + accepted + "/accept", null).andExpect(status().isOk());
		callAs(memberC, HttpMethod.POST, "/v1/assignments/" + declined + "/decline", "{\"reason\":\"No\"}")
				.andExpect(status().isOk());

		assertThat(assignmentApi.acceptedTaskIds(memberC.getId())).containsExactly(taskIdOf(accepted));
		assertThat(assignmentApi.acceptedTaskIds(memberB.getId())).isEmpty();
		assertThat(taskApi.schedulableTasks(memberC.getId(), assignmentApi.acceptedTaskIds(memberC.getId())))
				.extracting(SchedulableTask::taskId)
				.containsExactly(taskIdOf(accepted))
				.doesNotContain(taskIdOf(pending), taskIdOf(declined));

		callAuthenticated(HttpMethod.POST, "/v1/assignments/" + accepted + "/revoke", null).andExpect(status().isOk());
		assertThat(assignmentApi.acceptedTaskIds(memberC.getId())).isEmpty();
	}

	// ---------- helpers ----------

	private long createAssignment(User assignee) throws Exception {
		String body = callAuthenticated(HttpMethod.POST, "/v1/assignments",
				newTaskBody(group.getId(), assignee.getId()))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return ((Number) JsonPath.read(body, "$.data.id")).longValue();
	}

	private long taskIdOf(long assignmentId) {
		Long taskId = jdbc.queryForObject("SELECT task_id FROM task_assignments WHERE id = ?", Long.class,
				assignmentId);
		assertThat(taskId).isNotNull();
		return taskId;
	}

	private static String newTaskBody(long groupId, long assigneeId) {
		return "{\"assigneeUserId\":" + assigneeId + ",\"newTask\":{\"groupId\":" + groupId
				+ ",\"title\":\"Write report\",\"description\":\"\",\"deadline\":\"2026-11-30T11:00:00Z\","
				+ "\"priority\":\"HIGH\",\"category\":\"PROJECT\",\"estimatedDurationMinutes\":90}}";
	}

	private static String reassignBody(long taskId, long assigneeId) {
		return "{\"assigneeUserId\":" + assigneeId + ",\"taskId\":" + taskId + "}";
	}

	private User newUser(String email) {
		User user = userRepository.save(new User(email, "$2a$10$hash", "User " + email, null));
		extraUserIds.add(user.getId());
		return user;
	}

	private GroupMember addMember(TeamGroup g, User user, GroupRole role) {
		return memberRepository.save(new GroupMember(g.getId(), user.getId(), role,
				Instant.now().truncatedTo(ChronoUnit.MILLIS)));
	}

	private ResultActions callAs(User user, HttpMethod method, String uri, String jsonBody) throws Exception {
		MockHttpServletRequestBuilder builder = request(method, uri)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtService.issue(user).value());
		if (jsonBody != null) {
			builder.contentType(MediaType.APPLICATION_JSON).content(jsonBody);
		}
		return mockMvc.perform(builder);
	}
}
