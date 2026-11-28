package group16.webapp.modules.group.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.entity.GroupMember;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * B18 (iii): the single successor rule shared by DELETE and {@code GroupResponse.successorUserId} (D16, O1, O10).
 * The list is already in contract order (joinedAt, then membership id), so ties are resolved by the caller's query.
 */
class PickSuccessorTest {

	private static final Instant T = Instant.parse("2026-11-29T00:00:00Z");

	@Test
	void picksFirstOtherMemberInGivenOrder() {
		List<GroupMember> members = List.of(member(1L, GroupRole.LEADER), member(2L, GroupRole.MEMBER),
				member(3L, GroupRole.MEMBER));

		assertThat(GroupServiceImpl.pickSuccessor(members, users(1L, 2L, 3L), 1L)).isEqualTo(2L);
	}

	@Test
	void excludesTheLeaderWhereverItIsInTheOrder() {
		List<GroupMember> members = List.of(member(2L, GroupRole.MEMBER), member(1L, GroupRole.LEADER),
				member(3L, GroupRole.MEMBER));

		assertThat(GroupServiceImpl.pickSuccessor(members, users(1L, 2L, 3L), 1L)).isEqualTo(2L);
		assertThat(GroupServiceImpl.pickSuccessor(members, users(1L, 2L, 3L), 2L)).isEqualTo(1L);
	}

	@Test
	void skipsInactiveAndMissingAccounts() {
		List<GroupMember> members = List.of(member(1L, GroupRole.LEADER), member(2L, GroupRole.MEMBER),
				member(3L, GroupRole.MEMBER), member(4L, GroupRole.MEMBER));
		Map<Long, UserSummary> users = Map.of(1L, user(1L, true), 2L, user(2L, false), 4L, user(4L, true));

		assertThat(GroupServiceImpl.pickSuccessor(members, users, 1L)).isEqualTo(4L);
	}

	@Test
	void returnsNullWithoutEligibleMember() {
		assertThat(GroupServiceImpl.pickSuccessor(List.of(member(1L, GroupRole.LEADER)), users(1L), 1L)).isNull();
		assertThat(GroupServiceImpl.pickSuccessor(List.of(member(1L, GroupRole.LEADER), member(2L, GroupRole.MEMBER)),
				Map.of(1L, user(1L, true), 2L, user(2L, false)), 1L)).isNull();
		assertThat(GroupServiceImpl.pickSuccessor(List.of(), Map.of(), 1L)).isNull();
	}

	private static GroupMember member(Long userId, GroupRole role) {
		return new GroupMember(10L, userId, role, T);
	}

	private static UserSummary user(Long id, boolean active) {
		return new UserSummary(id, "u" + id + "@example.com", "User " + id, "UTC", active);
	}

	private static Map<Long, UserSummary> users(Long... ids) {
		java.util.Map<Long, UserSummary> map = new java.util.HashMap<>();
		for (Long id : ids) {
			map.put(id, user(id, true));
		}
		return map;
	}
}
