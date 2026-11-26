package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.support.AuthenticatedMvcTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * GR-02 group CRUD through the real API (H2, real JWTs). No test-managed transaction: each request commits like in
 * production, and {@link #cleanUp()} removes every row this class created, independent of the base class setup.
 */
class GroupControllerTests extends AuthenticatedMvcTest {

	private static final String CALLER_EMAIL = "caller@example.com";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private JwtService jwtService;

	@Autowired
	private TeamGroupRepository groupRepository;

	@MockitoSpyBean
	private GroupMemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbc;

	private final List<Long> extraUserIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAllInBatch();
		groupRepository.deleteAllInBatch();
		userRepository.deleteAllById(extraUserIds);
		extraUserIds.clear();
	}

	// ---------- POST /v1/groups ----------

	@Test
	void createGroupReturns201WithCreatorAsActiveLeader() throws Exception {
		User caller = caller();

		callAuthenticated(HttpMethod.POST, "/v1/groups",
				"{\"groupName\":\"  Team A  \",\"description\":\"Course project\"}")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.id").isNumber())
				.andExpect(jsonPath("$.data.groupName").value("Team A"))
				.andExpect(jsonPath("$.data.description").value("Course project"))
				.andExpect(jsonPath("$.data.createdByUserId").value(caller.getId()))
				.andExpect(jsonPath("$.data.members", hasSize(1)))
				.andExpect(jsonPath("$.data.members[0].userId").value(caller.getId()))
				.andExpect(jsonPath("$.data.members[0].fullName").value("Caller"))
				.andExpect(jsonPath("$.data.members[0].email").value(CALLER_EMAIL))
				.andExpect(jsonPath("$.data.members[0].role").value("LEADER"))
				.andExpect(jsonPath("$.data.members[0].status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.members[0].joinedAt").isNotEmpty());

		List<TeamGroup> groups = groupRepository.findAll();
		assertThat(groups).hasSize(1);
		assertThat(groups.getFirst().getCreatedByUserId()).isEqualTo(caller.getId());
		List<GroupMember> members = memberRepository.findAll();
		assertThat(members).singleElement().satisfies(m -> {
			assertThat(m.getGroupId()).isEqualTo(groups.getFirst().getId());
			assertThat(m.getUserId()).isEqualTo(caller.getId());
			assertThat(m.getRole()).isEqualTo(GroupRole.LEADER);
			assertThat(m.getLeftAt()).isNull();
		});
	}

	@ParameterizedTest
	@ValueSource(strings = {"{\"groupName\":\"Team A\"}", "{\"groupName\":\"Team A\",\"description\":null}"})
	void createGroupStoresMissingOrNullDescriptionAsEmptyString(String body) throws Exception {
		callAuthenticated(HttpMethod.POST, "/v1/groups", body)
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.description").value(""));

		assertThat(groupRepository.findAll()).singleElement()
				.satisfies(g -> assertThat(g.getDescription()).isEmpty());
	}

	// ---------- GET /v1/groups ----------

	@Test
	void listMyGroupsReturnsOnlyActiveMembershipsWithMyRole() throws Exception {
		User caller = caller();
		User other = newUser("other@example.com");
		TeamGroup led = seedGroup(caller, "Led");
		TeamGroup joined = seedGroup(other, "Joined");
		addMember(joined, caller, GroupRole.MEMBER);
		TeamGroup left = seedGroup(other, "Left");
		deactivate(addMember(left, caller, GroupRole.MEMBER));
		seedGroup(other, "Foreign");

		callAuthenticated(HttpMethod.GET, "/v1/groups", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(2)))
				.andExpect(jsonPath("$.data[0].id").value(led.getId()))
				.andExpect(jsonPath("$.data[0].groupName").value("Led"))
				.andExpect(jsonPath("$.data[0].myRole").value("LEADER"))
				.andExpect(jsonPath("$.data[1].id").value(joined.getId()))
				.andExpect(jsonPath("$.data[1].myRole").value("MEMBER"));
	}

	@Test
	void listMyGroupsIsEmptyWithoutMemberships() throws Exception {
		callAuthenticated(HttpMethod.GET, "/v1/groups", null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data", hasSize(0)));
	}

	@Test
	void deactivatedCallerWithValidTokenGets401() throws Exception {
		User caller = caller();
		caller.deactivate();
		userRepository.save(caller);

		callAuthenticated(HttpMethod.GET, "/v1/groups", null)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
	}

	// ---------- GET /v1/groups/{id} ----------

	@Test
	void activeMemberSeesGroupWithActiveMembersOnly() throws Exception {
		User caller = caller();
		User leader = newUser("leader@example.com");
		User former = newUser("former@example.com");
		TeamGroup group = seedGroup(leader, "Team");
		addMember(group, caller, GroupRole.MEMBER);
		deactivate(addMember(group, former, GroupRole.MEMBER));
		clearInvocations(memberRepository);

		callAuthenticated(HttpMethod.GET, "/v1/groups/" + group.getId(), null)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.id").value(group.getId()))
				.andExpect(jsonPath("$.data.createdByUserId").value(leader.getId()))
				.andExpect(jsonPath("$.data.members", hasSize(2)))
				.andExpect(jsonPath("$.data.members[0].userId").value(leader.getId()))
				.andExpect(jsonPath("$.data.members[0].role").value("LEADER"))
				.andExpect(jsonPath("$.data.members[1].userId").value(caller.getId()))
				.andExpect(jsonPath("$.data.members[1].role").value("MEMBER"));

		// Proves the spy sees the call, so the never() checks in the 403 tests are meaningful.
		verify(memberRepository).findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(group.getId(), MembershipStatus.ACTIVE);
	}

	@Test
	void nonMemberReadIsForbiddenWithoutLoadingMembers() throws Exception {
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");
		clearInvocations(memberRepository);

		callAuthenticated(HttpMethod.GET, "/v1/groups/" + group.getId(), null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"))
				.andExpect(jsonPath("$.data").doesNotExist());

		verify(memberRepository, never()).findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(any(), any());
	}

	@Test
	void inactiveMemberReadIsForbiddenWithoutLoadingMembers() throws Exception {
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");
		deactivate(addMember(group, caller(), GroupRole.MEMBER));
		clearInvocations(memberRepository);

		callAuthenticated(HttpMethod.GET, "/v1/groups/" + group.getId(), null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		verify(memberRepository, never()).findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(any(), any());
	}

	@Test
	void readingMissingGroupReturns404() throws Exception {
		callAuthenticated(HttpMethod.GET, "/v1/groups/999999", null)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	// ---------- PUT /v1/groups/{id} ----------

	@Test
	void activeLeaderUpdatesGroup() throws Exception {
		TeamGroup group = seedGroup(caller(), "Old name");

		callAuthenticated(HttpMethod.PUT, "/v1/groups/" + group.getId(), "{\"groupName\":\" New name \"}")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.groupName").value("New name"))
				.andExpect(jsonPath("$.data.description").value(""))
				.andExpect(jsonPath("$.data.members", hasSize(1)));

		TeamGroup stored = groupRepository.findById(group.getId()).orElseThrow();
		assertThat(stored.getGroupName()).isEqualTo("New name");
		assertThat(stored.getDescription()).isEmpty();
	}

	@Test
	void memberUpdateIsForbiddenAndLeavesGroupUnchanged() throws Exception {
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");
		addMember(group, caller(), GroupRole.MEMBER);

		assertForbiddenUpdateLeavesGroupUnchanged(group);
	}

	@Test
	void nonMemberUpdateIsForbiddenAndLeavesGroupUnchanged() throws Exception {
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");

		assertForbiddenUpdateLeavesGroupUnchanged(group);
	}

	@Test
	void inactiveLeaderUpdateIsForbiddenAndLeavesGroupUnchanged() throws Exception {
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");
		deactivate(addMember(group, caller(), GroupRole.LEADER));

		assertForbiddenUpdateLeavesGroupUnchanged(group);
	}

	@Test
	void updatingMissingGroupReturns404() throws Exception {
		callAuthenticated(HttpMethod.PUT, "/v1/groups/999999", "{\"groupName\":\"Team\"}")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void leaderOfAnotherGroupCannotUpdateThisGroup() throws Exception {
		User caller = caller();
		seedGroup(caller, "Mine");
		TeamGroup foreign = seedGroup(newUser("leader@example.com"), "Foreign");

		callAs(caller, HttpMethod.PUT, "/v1/groups/" + foreign.getId(), "{\"groupName\":\"Hijacked\"}")
				.andExpect(status().isForbidden());

		assertThat(groupRepository.findById(foreign.getId()).orElseThrow().getGroupName()).isEqualTo("Foreign");
	}

	@Test
	void leaderOfOneGroupButMemberOfTargetCannotUpdateTarget() throws Exception {
		User caller = caller();
		TeamGroup groupA = seedGroup(caller, "A");
		TeamGroup groupB = seedGroup(newUser("leader@example.com"), "B");
		addMember(groupB, caller, GroupRole.MEMBER);

		assertForbiddenUpdateLeavesGroupUnchanged(groupB);

		assertThat(groupRepository.findById(groupA.getId()).orElseThrow().getGroupName()).isEqualTo("A");
		assertThat(memberRepository.findByGroupIdAndUserIdAndStatus(groupA.getId(), caller.getId(),
				MembershipStatus.ACTIVE)).get().extracting(GroupMember::getRole).isEqualTo(GroupRole.LEADER);
		assertThat(memberRepository.findByGroupIdAndUserIdAndStatus(groupB.getId(), caller.getId(),
				MembershipStatus.ACTIVE)).get().extracting(GroupMember::getRole).isEqualTo(GroupRole.MEMBER);
	}

	private void assertForbiddenUpdateLeavesGroupUnchanged(TeamGroup group) throws Exception {
		TeamGroup before = groupRepository.findById(group.getId()).orElseThrow();
		clearInvocations(memberRepository);

		callAuthenticated(HttpMethod.PUT, "/v1/groups/" + group.getId(),
				"{\"groupName\":\"Hijacked\",\"description\":\"changed\"}")
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));

		TeamGroup after = groupRepository.findById(group.getId()).orElseThrow();
		assertThat(after.getGroupName()).isEqualTo(before.getGroupName());
		assertThat(after.getDescription()).isEqualTo(before.getDescription());
		assertThat(after.getUpdatedAt()).isEqualTo(before.getUpdatedAt());
		verify(memberRepository, never()).findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(any(), any());
	}

	// ---------- fixtures ----------

	/** The caller created by AuthenticatedMvcTest (looked up, so nothing depends on setup order). */
	private User caller() {
		return userRepository.findByEmail(CALLER_EMAIL).orElseThrow();
	}

	private User newUser(String email) {
		User user = userRepository.save(new User(email, "$2a$10$hash", "User " + email, null));
		extraUserIds.add(user.getId());
		return user;
	}

	private TeamGroup seedGroup(User leader, String name) {
		TeamGroup group = groupRepository.save(new TeamGroup(leader.getId(), name, "seeded"));
		addMember(group, leader, GroupRole.LEADER);
		return group;
	}

	private GroupMember addMember(TeamGroup group, User user, GroupRole role) {
		return memberRepository.save(new GroupMember(group.getId(), user.getId(), role,
				Instant.now().truncatedTo(ChronoUnit.MILLIS)));
	}

	/** GR-03 owns leaving; tests set the INACTIVE state directly (left_at is required for INACTIVE in V7). */
	private void deactivate(GroupMember member) {
		jdbc.update("UPDATE group_members SET status = 'INACTIVE', left_at = ? WHERE id = ?",
				java.sql.Timestamp.from(member.getJoinedAt().plusSeconds(1)), member.getId());
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
