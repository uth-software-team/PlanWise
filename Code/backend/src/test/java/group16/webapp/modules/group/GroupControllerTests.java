package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.JwtService;
import group16.webapp.modules.group.api.GroupAccessApi;
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

	@Autowired
	private GroupAccessApi accessApi;

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

	// ---------- POST /v1/groups/{id}/members ----------

	@Test
	void leaderAddsExistingUserAsActiveMember() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User target = newUser("target@example.com");

		callAuthenticated(HttpMethod.POST, membersUri(group), addBody("target@example.com"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").value(target.getId()))
				.andExpect(jsonPath("$.data.email").value("target@example.com"))
				.andExpect(jsonPath("$.data.role").value("MEMBER"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"))
				.andExpect(jsonPath("$.data.joinedAt").isNotEmpty());

		GroupMember row = row(group, target);
		assertThat(row.getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row.getLeftAt()).isNull();
	}

	@Test
	void addingMissingEmailReturns404() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");

		callAuthenticated(HttpMethod.POST, membersUri(group), addBody("nobody@example.com"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void addingInactiveAccountReturns404AndCreatesNoRow() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User target = newUser("inactive@example.com");
		target.deactivate();
		userRepository.save(target);

		callAuthenticated(HttpMethod.POST, membersUri(group), addBody("inactive@example.com"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

		assertThat(memberRepository.findByGroupIdAndUserId(group.getId(), target.getId())).isEmpty();
	}

	@Test
	void addingToMissingGroupReturns404() throws Exception {
		callAuthenticated(HttpMethod.POST, "/v1/groups/999999/members", addBody("x@example.com"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	void nonLeadersCannotAddEvenForUnknownEmail() throws Exception {
		User caller = caller();
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");
		TeamGroup inactiveGroup = seedGroup(newUser("leader2@example.com"), "Team2");
		deactivate(addMember(inactiveGroup, caller, GroupRole.LEADER));
		TeamGroup foreign = seedGroup(newUser("leader3@example.com"), "Foreign");
		seedGroup(caller, "Mine");
		addMember(group, caller, GroupRole.MEMBER);

		// 403 comes before the target lookup, so an unknown email does not leak as 404.
		for (TeamGroup g : List.of(group, inactiveGroup, foreign)) {
			callAuthenticated(HttpMethod.POST, membersUri(g), addBody("nobody@example.com"))
					.andExpect(status().isForbidden())
					.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		}
	}

	@Test
	void addingActiveMemberReturns409AndKeepsRoleAndTimestamps() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User target = newUser("other-leader@example.com");
		addMember(group, target, GroupRole.LEADER);
		GroupSnapshot before = snapshot(row(group, target));

		callAuthenticated(HttpMethod.POST, membersUri(group), addBody("other-leader@example.com"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));

		assertThat(snapshot(row(group, target))).isEqualTo(before);
	}

	@Test
	void addingInactiveMemberReactivatesSameRowAsMember() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User target = newUser("former@example.com");
		Instant oldJoin = Instant.now().minus(2, ChronoUnit.HOURS).truncatedTo(ChronoUnit.MILLIS);
		GroupMember seeded = memberRepository.save(new GroupMember(group.getId(), target.getId(), GroupRole.LEADER, oldJoin));
		deactivate(seeded);

		callAuthenticated(HttpMethod.POST, membersUri(group), addBody("FORMER@example.com"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.userId").value(target.getId()))
				.andExpect(jsonPath("$.data.role").value("MEMBER"))
				.andExpect(jsonPath("$.data.status").value("ACTIVE"));

		GroupMember row = row(group, target);
		assertThat(row.getId()).isEqualTo(seeded.getId());
		assertThat(row.getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row.getLeftAt()).isNull();
		assertThat(row.getJoinedAt()).isAfter(oldJoin);
		assertThat(memberRepository.findAll().stream().filter(m -> m.getUserId().equals(target.getId()))).hasSize(1);
	}

	// ---------- PATCH /v1/groups/{id}/members/{userId} ----------

	@Test
	void leaderPromotesMemberAndSameRoleRequestIsNoOp() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User target = newUser("target@example.com");
		addMember(group, target, GroupRole.MEMBER);

		callAuthenticated(HttpMethod.PATCH, memberUri(group, target), roleBody("LEADER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userId").value(target.getId()))
				.andExpect(jsonPath("$.data.role").value("LEADER"));
		assertThat(row(group, target).getRole()).isEqualTo(GroupRole.LEADER);

		GroupSnapshot before = snapshot(row(group, target));
		callAuthenticated(HttpMethod.PATCH, memberUri(group, target), roleBody("LEADER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("LEADER"));
		assertThat(snapshot(row(group, target))).isEqualTo(before);
	}

	@Test
	void ordinaryLeaderPromotesAndDemotesPeersIncludingCreator() throws Exception {
		User creator = newUser("creator@example.com");
		User ordinary = caller();
		TeamGroup group = seedGroup(creator, "Team");
		addMember(group, ordinary, GroupRole.LEADER);
		User member = newUser("member@example.com");
		addMember(group, member, GroupRole.MEMBER);

		callAuthenticated(HttpMethod.PATCH, memberUri(group, member), roleBody("LEADER")).andExpect(status().isOk());

		callAuthenticated(HttpMethod.PATCH, memberUri(group, creator), roleBody("MEMBER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("MEMBER"));
		callAuthenticated(HttpMethod.PATCH, memberUri(group, member), roleBody("MEMBER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("MEMBER"));

		assertThat(row(group, creator).getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row(group, creator).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row(group, member).getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row(group, ordinary).getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(groupRepository.findById(group.getId()).orElseThrow().getCreatedByUserId())
				.isEqualTo(creator.getId());
	}

	@Test
	void creatorDemotesAnotherLeader() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User other = newUser("other@example.com");
		addMember(group, other, GroupRole.LEADER);

		callAuthenticated(HttpMethod.PATCH, memberUri(group, other), roleBody("MEMBER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("MEMBER"));

		assertThat(row(group, other).getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row(group, other).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	void creatorSelfDemotionKeepsCreatedByAndRemainingLeadersManagePeers() throws Exception {
		User creator = caller();
		TeamGroup group = seedGroup(creator, "Team");
		User leader2 = newUser("leader2@example.com");
		User leader3 = newUser("leader3@example.com");
		addMember(group, leader2, GroupRole.LEADER);
		addMember(group, leader3, GroupRole.LEADER);

		callAuthenticated(HttpMethod.PATCH, memberUri(group, creator), roleBody("MEMBER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("MEMBER"));

		assertThat(row(group, creator).getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(groupRepository.findById(group.getId()).orElseThrow().getCreatedByUserId())
				.isEqualTo(creator.getId());
		// The remaining (non-creator) leaders demote each other like any peer, while another leader remains.
		callAs(leader2, HttpMethod.PATCH, memberUri(group, leader3), roleBody("MEMBER"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.role").value("MEMBER"));
		assertThat(row(group, leader3).getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row(group, leader3).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row(group, leader2).getRole()).isEqualTo(GroupRole.LEADER);
	}

	@Test
	void lastLeaderSelfDemotionReturns409AndKeepsRole() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User member = newUser("member@example.com");
		addMember(group, member, GroupRole.MEMBER);
		GroupSnapshot before = snapshot(row(group, caller()));

		callAuthenticated(HttpMethod.PATCH, memberUri(group, caller()), roleBody("MEMBER"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));

		assertThat(snapshot(row(group, caller()))).isEqualTo(before);
	}

	@Test
	void roleChangeTargetsAndCallersAreChecked() throws Exception {
		User caller = caller();
		TeamGroup group = seedGroup(caller, "Team");
		User inactive = newUser("inactive@example.com");
		deactivate(addMember(group, inactive, GroupRole.MEMBER));
		User stranger = newUser("stranger@example.com");

		callAuthenticated(HttpMethod.PATCH, memberUri(group, inactive), roleBody("LEADER"))
				.andExpect(status().isNotFound());
		callAuthenticated(HttpMethod.PATCH, memberUri(group, stranger), roleBody("LEADER"))
				.andExpect(status().isNotFound());
		callAuthenticated(HttpMethod.PATCH, "/v1/groups/999999/members/" + stranger.getId(), roleBody("LEADER"))
				.andExpect(status().isNotFound());
		assertThat(row(group, inactive).getStatus()).isEqualTo(MembershipStatus.INACTIVE);

		// A MEMBER of the group, and a LEADER of another group only, are forbidden.
		User member = newUser("member@example.com");
		addMember(group, member, GroupRole.MEMBER);
		callAs(member, HttpMethod.PATCH, memberUri(group, member), roleBody("LEADER")).andExpect(status().isForbidden());
		TeamGroup other = seedGroup(newUser("leader@example.com"), "Other");
		User otherMember = newUser("om@example.com");
		addMember(other, otherMember, GroupRole.MEMBER);
		callAuthenticated(HttpMethod.PATCH, memberUri(other, otherMember), roleBody("LEADER"))
				.andExpect(status().isForbidden());
		assertThat(row(other, otherMember).getRole()).isEqualTo(GroupRole.MEMBER);
	}

	// ---------- DELETE /v1/groups/{id}/members/{userId} ----------

	@Test
	void leaderRemovesMemberKeepingRowAndMarkingItInactive() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		User target = newUser("target@example.com");
		GroupMember seeded = addMember(group, target, GroupRole.MEMBER);

		callAuthenticated(HttpMethod.DELETE, memberUri(group, target), null)
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));

		GroupMember row = row(group, target);
		assertThat(row.getId()).isEqualTo(seeded.getId());
		assertThat(row.getStatus()).isEqualTo(MembershipStatus.INACTIVE);
		assertThat(row.getLeftAt()).isNotNull();
		assertThat(row.getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(row.getJoinedAt()).isEqualTo(seeded.getJoinedAt());
		assertThat(row.getGroupId()).isEqualTo(group.getId());
	}

	@Test
	void memberSelfLeavesAndRepeatedLeaveIsForbidden() throws Exception {
		User caller = caller();
		TeamGroup group = seedGroup(newUser("leader@example.com"), "Team");
		addMember(group, caller, GroupRole.MEMBER);

		callAuthenticated(HttpMethod.DELETE, memberUri(group, caller), null)
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		assertThat(row(group, caller).getStatus()).isEqualTo(MembershipStatus.INACTIVE);
		assertThat(row(group, caller).getLeftAt()).isNotNull();

		callAuthenticated(HttpMethod.DELETE, memberUri(group, caller), null)
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void removalTargetsAndCallersAreChecked() throws Exception {
		User caller = caller();
		TeamGroup group = seedGroup(caller, "Team");
		User inactive = newUser("inactive@example.com");
		deactivate(addMember(group, inactive, GroupRole.MEMBER));
		User stranger = newUser("stranger@example.com");

		callAuthenticated(HttpMethod.DELETE, memberUri(group, inactive), null).andExpect(status().isNotFound());
		callAuthenticated(HttpMethod.DELETE, memberUri(group, stranger), null).andExpect(status().isNotFound());
		callAuthenticated(HttpMethod.DELETE, "/v1/groups/999999/members/" + stranger.getId(), null)
				.andExpect(status().isNotFound());

		// MEMBER removing someone else, and a LEADER of another group removing in this group: 403.
		User member = newUser("member@example.com");
		User victim = newUser("victim@example.com");
		addMember(group, member, GroupRole.MEMBER);
		addMember(group, victim, GroupRole.MEMBER);
		callAs(member, HttpMethod.DELETE, memberUri(group, victim), null).andExpect(status().isForbidden());
		TeamGroup foreign = seedGroup(newUser("leader@example.com"), "Foreign");
		User foreignMember = newUser("fm@example.com");
		addMember(foreign, foreignMember, GroupRole.MEMBER);
		callAuthenticated(HttpMethod.DELETE, memberUri(foreign, foreignMember), null)
				.andExpect(status().isForbidden());
		assertThat(row(group, victim).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row(foreign, foreignMember).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
	}

	@Test
	void ordinaryLeaderRemovesCreatorAndPeerLeader() throws Exception {
		User creator = newUser("creator@example.com");
		User ordinary = caller();
		TeamGroup group = seedGroup(creator, "Team");
		addMember(group, ordinary, GroupRole.LEADER);
		User peer = newUser("peer@example.com");
		addMember(group, peer, GroupRole.LEADER);

		callAuthenticated(HttpMethod.DELETE, memberUri(group, creator), null).andExpect(status().isNoContent());
		callAuthenticated(HttpMethod.DELETE, memberUri(group, peer), null).andExpect(status().isNoContent());

		// Rows are kept: INACTIVE with the former role; the caller remains the only ACTIVE LEADER.
		assertThat(row(group, creator).getStatus()).isEqualTo(MembershipStatus.INACTIVE);
		assertThat(row(group, creator).getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(row(group, peer).getStatus()).isEqualTo(MembershipStatus.INACTIVE);
		assertThat(row(group, peer).getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(row(group, ordinary).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row(group, ordinary).getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(groupRepository.findById(group.getId()).orElseThrow().getCreatedByUserId())
				.isEqualTo(creator.getId());
	}

	@Test
	void creatorSelfLeaveKeepsCreatedByAndRemainingLeaderRemovesPeerLeader() throws Exception {
		User creator = caller();
		TeamGroup group = seedGroup(creator, "Team");
		User leader2 = newUser("leader2@example.com");
		User leader3 = newUser("leader3@example.com");
		User member = newUser("member@example.com");
		addMember(group, leader2, GroupRole.LEADER);
		addMember(group, leader3, GroupRole.LEADER);
		addMember(group, member, GroupRole.MEMBER);

		callAuthenticated(HttpMethod.DELETE, memberUri(group, creator), null).andExpect(status().isNoContent());

		assertThat(row(group, creator).getStatus()).isEqualTo(MembershipStatus.INACTIVE);
		assertThat(row(group, creator).getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(groupRepository.findById(group.getId()).orElseThrow().getCreatedByUserId())
				.isEqualTo(creator.getId());
		callAs(leader2, HttpMethod.DELETE, memberUri(group, member), null).andExpect(status().isNoContent());
		callAs(leader2, HttpMethod.DELETE, memberUri(group, leader3), null).andExpect(status().isNoContent());
		assertThat(row(group, leader3).getStatus()).isEqualTo(MembershipStatus.INACTIVE);
		assertThat(row(group, leader3).getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(row(group, leader2).getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(row(group, leader2).getRole()).isEqualTo(GroupRole.LEADER);
	}

	@Test
	void lastLeaderSelfLeaveReturns409AndKeepsMembership() throws Exception {
		TeamGroup group = seedGroup(caller(), "Team");
		addMember(group, newUser("member@example.com"), GroupRole.MEMBER);
		GroupSnapshot before = snapshot(row(group, caller()));

		callAuthenticated(HttpMethod.DELETE, memberUri(group, caller()), null)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DATA_CONFLICT"));

		assertThat(snapshot(row(group, caller()))).isEqualTo(before);
	}

	// ---------- GroupAccessApi ----------

	@Test
	void groupAccessApiDistinguishesActiveInactiveAndMissing() {
		User leader = newUser("leader@example.com");
		User member = newUser("member@example.com");
		User former = newUser("former@example.com");
		User stranger = newUser("stranger@example.com");
		TeamGroup group = seedGroup(leader, "Team");
		TeamGroup otherGroup = seedGroup(stranger, "Other");
		addMember(group, member, GroupRole.MEMBER);
		deactivate(addMember(group, former, GroupRole.LEADER));
		Long missing = 999999L;

		accessApi.requireMember(group.getId(), leader.getId());
		accessApi.requireMember(group.getId(), member.getId());
		accessApi.requireLeader(group.getId(), leader.getId());
		assertForbidden(() -> accessApi.requireLeader(group.getId(), member.getId()));
		assertForbidden(() -> accessApi.requireMember(group.getId(), former.getId()));
		assertForbidden(() -> accessApi.requireLeader(group.getId(), former.getId()));
		assertForbidden(() -> accessApi.requireMember(group.getId(), stranger.getId()));
		// Lookups are scoped to the requested group: a leader elsewhere is nobody here.
		assertForbidden(() -> accessApi.requireLeader(group.getId(), stranger.getId()));
		assertThatThrownBy(() -> accessApi.requireMember(missing, leader.getId()))
				.isInstanceOfSatisfying(AppException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));
		assertThatThrownBy(() -> accessApi.requireLeader(missing, leader.getId()))
				.isInstanceOfSatisfying(AppException.class,
						e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND));

		assertThat(accessApi.isActiveMember(group.getId(), leader.getId())).isTrue();
		assertThat(accessApi.isActiveMember(group.getId(), member.getId())).isTrue();
		assertThat(accessApi.isActiveMember(group.getId(), former.getId())).isFalse();
		assertThat(accessApi.isActiveMember(group.getId(), stranger.getId())).isFalse();
		assertThat(accessApi.isActiveMember(missing, leader.getId())).isFalse();

		assertThat(accessApi.memberIds(group.getId())).containsExactlyInAnyOrder(leader.getId(), member.getId());
		assertThat(accessApi.memberIds(otherGroup.getId())).containsExactly(stranger.getId());
		assertThat(accessApi.memberIds(missing)).isEmpty();
	}

	private static void assertForbidden(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(AppException.class,
				e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));
	}

	// ---------- fixtures ----------

	private static String membersUri(TeamGroup group) {
		return "/v1/groups/" + group.getId() + "/members";
	}

	private static String memberUri(TeamGroup group, User user) {
		return membersUri(group) + "/" + user.getId();
	}

	private static String addBody(String email) {
		return "{\"email\":\"" + email + "\"}";
	}

	private static String roleBody(String role) {
		return "{\"role\":\"" + role + "\"}";
	}

	private GroupMember row(TeamGroup group, User user) {
		return memberRepository.findByGroupIdAndUserId(group.getId(), user.getId()).orElseThrow();
	}

	private record GroupSnapshot(Long id, GroupRole role, MembershipStatus status, Instant joinedAt, Instant leftAt) {}

	private static GroupSnapshot snapshot(GroupMember m) {
		return new GroupSnapshot(m.getId(), m.getRole(), m.getStatus(), m.getJoinedAt(), m.getLeftAt());
	}

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

	/** Seeds the INACTIVE state directly (left_at is required for INACTIVE in V7). */
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
