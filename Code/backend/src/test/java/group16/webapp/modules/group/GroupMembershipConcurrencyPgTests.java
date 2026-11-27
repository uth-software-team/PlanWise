package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.RoleUpdateRequest;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.modules.group.service.GroupService;
import group16.webapp.support.PostgresContainerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Two ACTIVE LEADERs leave the same group at the same time. The group row lock serializes the two service
 * transactions, so exactly one leaves and the other gets 409 DATA_CONFLICT; one ACTIVE LEADER always remains.
 * Deliberately not {@code @Transactional}: each worker thread runs its own committed service transaction.
 */
class GroupMembershipConcurrencyPgTests extends PostgresContainerTest {

	private static final int TIMEOUT_SECONDS = 30;

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TeamGroupRepository groupRepository;

	@Autowired
	private GroupMemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbc;

	@Autowired
	private PlatformTransactionManager transactionManager;

	private Long groupId;
	private final List<Long> userIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		// Only this test's fixture rows; each statement is independent of the others.
		if (groupId != null) {
			jdbc.update("DELETE FROM group_members WHERE group_id = ?", groupId);
			jdbc.update("DELETE FROM team_groups WHERE id = ?", groupId);
		}
		for (Long userId : userIds) {
			jdbc.update("DELETE FROM users WHERE id = ?", userId);
		}
	}

	@Test
	void twoLeadersLeavingConcurrentlyLeaveExactlyOneActiveLeader() throws Exception {
		User first = newUser("pg-leader-1@example.com");
		User second = newUser("pg-leader-2@example.com");
		groupId = groupRepository.save(new TeamGroup(first.getId(), "Concurrent", "desc")).getId();
		memberRepository.save(new GroupMember(groupId, first.getId(), GroupRole.LEADER, Instant.now()));
		memberRepository.save(new GroupMember(groupId, second.getId(), GroupRole.LEADER, Instant.now()));
		assertThat(activeLeaders()).isEqualTo(2);

		CyclicBarrier barrier = new CyclicBarrier(2);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<ErrorCode> a = executor.submit(leave(barrier, first.getId()));
			Future<ErrorCode> b = executor.submit(leave(barrier, second.getId()));
			List<ErrorCode> outcomes = Arrays.asList(a.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
					b.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));

			// null = the call succeeded.
			assertThat(outcomes).containsExactlyInAnyOrder(null, ErrorCode.DATA_CONFLICT);
		} finally {
			executor.shutdownNow();
			assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(activeLeaders()).isEqualTo(1);
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM group_members WHERE group_id = ? AND status = 'INACTIVE' AND left_at IS NOT NULL",
				Integer.class, groupId)).isEqualTo(1);
	}

	/**
	 * Regression: updateGroup must lock the group before checking the caller's role. The creator's demotion of the
	 * leader holds the group lock in an open transaction; the leader's rename blocks on that lock, and only after the
	 * demotion commits does it see that the caller is no longer a LEADER. No sleeps: the rename is known to be
	 * blocked when PostgreSQL reports a backend waiting on a lock, and every wait is bounded.
	 */
	@Test
	void renameBlockedBehindDemotionIsForbiddenAndLeavesGroupUnchanged() throws Exception {
		User creator = newUser("pg-creator@example.com");
		User leader = newUser("pg-demoted@example.com");
		groupId = groupRepository.save(new TeamGroup(creator.getId(), "Original", "original desc")).getId();
		memberRepository.save(new GroupMember(groupId, creator.getId(), GroupRole.LEADER, Instant.now()));
		memberRepository.save(new GroupMember(groupId, leader.getId(), GroupRole.LEADER, Instant.now()));

		CountDownLatch demotedUncommitted = new CountDownLatch(1);
		CountDownLatch commitDemotion = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			// Transaction 1: the creator demotes the leader and keeps the transaction (and group lock) open.
			Future<?> demotion = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
				groupService.changeMemberRole(creator.getId(), groupId, leader.getId(), new RoleUpdateRequest(GroupRole.MEMBER));
				demotedUncommitted.countDown();
				await(commitDemotion);
			}));
			assertThat(demotedUncommitted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

			// Transaction 2: the demoted leader renames the group; it must block behind the lock.
			Future<ErrorCode> rename = executor.submit(() -> {
				try {
					groupService.updateGroup(leader.getId(), groupId, new GroupRequest("Hijacked", "changed"));
					return null;
				} catch (AppException e) {
					return e.getErrorCode();
				}
			});
			awaitBlockedOnLock();
			assertThat(rename.isDone()).as("rename is blocked until the demotion commits").isFalse();

			commitDemotion.countDown();
			demotion.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			assertThat(rename.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(ErrorCode.FORBIDDEN);
		} finally {
			commitDemotion.countDown();
			executor.shutdownNow();
			assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
		}

		TeamGroup stored = groupRepository.findById(groupId).orElseThrow();
		assertThat(stored.getGroupName()).isEqualTo("Original");
		assertThat(stored.getDescription()).isEqualTo("original desc");
		assertThat(memberRepository.findByGroupIdAndUserId(groupId, leader.getId()).orElseThrow().getRole())
				.isEqualTo(GroupRole.MEMBER);
	}

	/**
	 * Regression for the peer-leader rule (any ACTIVE LEADER may demote another, none of them the creator) and for
	 * lock-before-role-check: A and B are the only ACTIVE LEADERs (the creator has left) and demote each other. A's
	 * demotion of B holds the group lock in an open transaction; B's demotion of A blocks on that lock and, once A
	 * commits, rechecks B's role and is FORBIDDEN. Exactly one ACTIVE LEADER and one ACTIVE MEMBER remain.
	 */
	@Test
	void twoLeadersDemotingEachOtherLeaveExactlyOneActiveLeader() throws Exception {
		User creator = newUser("pg-mutual-creator@example.com");
		User leaderA = newUser("pg-mutual-a@example.com");
		User leaderB = newUser("pg-mutual-b@example.com");
		groupId = groupRepository.save(new TeamGroup(creator.getId(), "Mutual", "desc")).getId();
		GroupMember formerCreator = new GroupMember(groupId, creator.getId(), GroupRole.LEADER, Instant.now());
		formerCreator.deactivate(Instant.now());
		memberRepository.save(formerCreator);
		memberRepository.save(new GroupMember(groupId, leaderA.getId(), GroupRole.LEADER, Instant.now()));
		memberRepository.save(new GroupMember(groupId, leaderB.getId(), GroupRole.LEADER, Instant.now()));
		assertThat(activeLeaders()).isEqualTo(2);

		CountDownLatch demotedUncommitted = new CountDownLatch(1);
		CountDownLatch commitDemotion = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			// Transaction 1: A demotes B and keeps the transaction (and group lock) open.
			Future<?> first = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
				groupService.changeMemberRole(leaderA.getId(), groupId, leaderB.getId(), new RoleUpdateRequest(GroupRole.MEMBER));
				demotedUncommitted.countDown();
				await(commitDemotion);
			}));
			assertThat(demotedUncommitted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

			// Transaction 2: B demotes A; it must block behind the lock.
			Future<ErrorCode> second = executor.submit(() -> {
				try {
					groupService.changeMemberRole(leaderB.getId(), groupId, leaderA.getId(), new RoleUpdateRequest(GroupRole.MEMBER));
					return null;
				} catch (AppException e) {
					return e.getErrorCode();
				}
			});
			awaitBlockedOnLock();
			assertThat(second.isDone()).as("B's demotion is blocked until A's demotion commits").isFalse();

			commitDemotion.countDown();
			first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			assertThat(second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isEqualTo(ErrorCode.FORBIDDEN);
		} finally {
			commitDemotion.countDown();
			executor.shutdownNow();
			assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(activeLeaders()).isEqualTo(1);
		assertThat(jdbc.queryForObject(
				"SELECT count(*) FROM group_members WHERE group_id = ? AND role = 'MEMBER' AND status = 'ACTIVE'",
				Integer.class, groupId)).isEqualTo(1);
		GroupMember a = memberRepository.findByGroupIdAndUserId(groupId, leaderA.getId()).orElseThrow();
		GroupMember b = memberRepository.findByGroupIdAndUserId(groupId, leaderB.getId()).orElseThrow();
		assertThat(a.getRole()).isEqualTo(GroupRole.LEADER);
		assertThat(a.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(b.getRole()).isEqualTo(GroupRole.MEMBER);
		assertThat(b.getStatus()).isEqualTo(MembershipStatus.ACTIVE);
		assertThat(memberRepository.findByGroupIdAndUserId(groupId, creator.getId()).orElseThrow().getStatus())
				.isEqualTo(MembershipStatus.INACTIVE);
	}

	private static void await(CountDownLatch latch) {
		try {
			if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("timed out waiting for the test to release the transaction");
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}

	/** Polls (bounded) until some PostgreSQL backend waits on a lock held by another transaction on team_groups. */
	private void awaitBlockedOnLock() {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
		while (System.nanoTime() < deadline) {
			Integer waiting = jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE wait_event_type = 'Lock'"
					+ " AND datname = current_database() AND query ILIKE '%team_groups%'", Integer.class);
			if (waiting != null && waiting > 0) {
				return;
			}
			java.util.concurrent.locks.LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
		}
		throw new IllegalStateException("the second transaction never blocked on the group lock");
	}

	/** Waits for the other thread, then self-leaves; returns null on success or the AppException's code. */
	private Callable<ErrorCode> leave(CyclicBarrier barrier, Long userId) {
		return () -> {
			barrier.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			try {
				groupService.removeMember(userId, groupId, userId);
				return null;
			} catch (AppException e) {
				return e.getErrorCode();
			}
		};
	}

	private User newUser(String email) {
		User user = userRepository.save(new User(email, "$2a$10$hash", "Pg " + email, null));
		userIds.add(user.getId());
		return user;
	}

	private int activeLeaders() {
		return jdbc.queryForObject(
				"SELECT count(*) FROM group_members WHERE group_id = ? AND role = 'LEADER' AND status = 'ACTIVE'",
				Integer.class, groupId);
	}
}
