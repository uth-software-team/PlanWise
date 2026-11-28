package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.LeaderTransferRequest;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Leadership hand-over on real PostgreSQL with the V11 index (B18) and concurrent membership writes (P1-P4).
 * The group row lock serializes the service transactions: the first call keeps its transaction (and the lock) open,
 * the second is proven to be blocked on the lock, then the first commits and the second re-reads the new state.
 * Every case checks each member's final role and status and that exactly one ACTIVE LEADER remains.
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
	private Long a;
	private Long b;
	private Long c;

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

	// ---------- B18: the hand-over write order is accepted by the V11 index ----------

	@Test
	void transferAndLeaderLeaveSucceedWithTheIndexInPlace() {
		assertThat(jdbc.queryForObject("SELECT count(*) FROM pg_indexes WHERE indexname = "
				+ "'uk_group_members_one_active_leader'", Integer.class)).as("V11 index present").isEqualTo(1);
		fixture("b18");

		groupService.transferLeadership(a, groupId, new LeaderTransferRequest(b));
		assertStates(Map.of(a, "MEMBER/ACTIVE", b, "LEADER/ACTIVE", c, "MEMBER/ACTIVE"));

		// B is the LEADER now; A joined first, so A is B's successor.
		groupService.removeMember(b, groupId, b, a);
		assertStates(Map.of(a, "LEADER/ACTIVE", b, "LEADER/INACTIVE", c, "MEMBER/ACTIVE"));
	}

	// ---------- P1: transfer(A->B) vs leave(A, expected B) ----------

	@Test
	void transferBeforeLeaveMakesTheLeaveStale() throws Exception {
		fixture("p1t");

		ErrorCode second = serialized(() -> groupService.transferLeadership(a, groupId, new LeaderTransferRequest(b)),
				() -> groupService.removeMember(a, groupId, a, b));

		assertThat(second).isEqualTo(ErrorCode.STALE_VERSION);
		assertStates(Map.of(a, "MEMBER/ACTIVE", b, "LEADER/ACTIVE", c, "MEMBER/ACTIVE"));
	}

	@Test
	void leaveBeforeTransferMakesTheTransferForbidden() throws Exception {
		fixture("p1l");

		ErrorCode second = serialized(() -> groupService.removeMember(a, groupId, a, b),
				() -> groupService.transferLeadership(a, groupId, new LeaderTransferRequest(b)));

		assertThat(second).isEqualTo(ErrorCode.FORBIDDEN);
		assertStates(Map.of(a, "LEADER/INACTIVE", b, "LEADER/ACTIVE", c, "MEMBER/ACTIVE"));
	}

	// ---------- P2: transfer(A->B) vs transfer(A->C) ----------

	@Test
	void secondConcurrentTransferIsForbidden() throws Exception {
		fixture("p2");

		ErrorCode second = serialized(() -> groupService.transferLeadership(a, groupId, new LeaderTransferRequest(b)),
				() -> groupService.transferLeadership(a, groupId, new LeaderTransferRequest(c)));

		assertThat(second).isEqualTo(ErrorCode.FORBIDDEN);
		assertStates(Map.of(a, "MEMBER/ACTIVE", b, "LEADER/ACTIVE", c, "MEMBER/ACTIVE"));
	}

	// ---------- P3: leave(A, expected B) vs leave(B) ----------

	@Test
	void successorLeavingFirstMakesTheLeaderLeaveStale() throws Exception {
		fixture("p3b");

		ErrorCode second = serialized(() -> groupService.removeMember(b, groupId, b, null),
				() -> groupService.removeMember(a, groupId, a, b));

		// The successor is now C, so A's request no longer matches.
		assertThat(second).isEqualTo(ErrorCode.STALE_VERSION);
		assertStates(Map.of(a, "LEADER/ACTIVE", b, "MEMBER/INACTIVE", c, "MEMBER/ACTIVE"));
	}

	@Test
	void leaderLeavingFirstMakesTheSuccessorLeaveStale() throws Exception {
		fixture("p3a");

		ErrorCode second = serialized(() -> groupService.removeMember(a, groupId, a, b),
				() -> groupService.removeMember(b, groupId, b, null));

		// B became LEADER, but B's request was sent as a MEMBER (no successor).
		assertThat(second).isEqualTo(ErrorCode.STALE_VERSION);
		assertStates(Map.of(a, "LEADER/INACTIVE", b, "LEADER/ACTIVE", c, "MEMBER/ACTIVE"));
	}

	// ---------- P4: transfer vs rename by the former LEADER ----------

	@Test
	void renameBlockedBehindTransferIsForbiddenAndLeavesGroupUnchanged() throws Exception {
		fixture("p4");

		ErrorCode second = serialized(() -> groupService.transferLeadership(a, groupId, new LeaderTransferRequest(b)),
				() -> groupService.updateGroup(a, groupId, new GroupRequest("Hijacked", "changed")));

		assertThat(second).isEqualTo(ErrorCode.FORBIDDEN);
		TeamGroup stored = groupRepository.findById(groupId).orElseThrow();
		assertThat(stored.getGroupName()).isEqualTo("Original");
		assertThat(stored.getDescription()).isEqualTo("original desc");
		assertStates(Map.of(a, "MEMBER/ACTIVE", b, "LEADER/ACTIVE", c, "MEMBER/ACTIVE"));
	}

	/**
	 * Runs {@code first} in a transaction kept open (holding the group lock), starts {@code second}, waits until
	 * PostgreSQL reports it blocked on a lock, then commits {@code first}. No sleeps; every wait is bounded.
	 * Returns null when {@code second} succeeded, else its AppException code.
	 */
	private ErrorCode serialized(Runnable first, Runnable second) throws Exception {
		CountDownLatch firstDone = new CountDownLatch(1);
		CountDownLatch commitFirst = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> firstTx = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
				first.run();
				firstDone.countDown();
				await(commitFirst);
			}));
			assertThat(firstDone.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).as("first call ran").isTrue();

			Future<ErrorCode> secondTx = executor.submit(() -> {
				try {
					second.run();
					return null;
				} catch (AppException e) {
					return e.getErrorCode();
				}
			});
			awaitBlockedOnLock();
			assertThat(secondTx.isDone()).as("second call is blocked until the first commits").isFalse();

			commitFirst.countDown();
			firstTx.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return secondTx.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} finally {
			commitFirst.countDown();
			executor.shutdownNow();
			assertThat(executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
		}
	}

	/** A is the ACTIVE LEADER; B and C are MEMBERs with active accounts; join order A, B, C. */
	private void fixture(String prefix) {
		a = newUser("pg-" + prefix + "-a@example.com").getId();
		b = newUser("pg-" + prefix + "-b@example.com").getId();
		c = newUser("pg-" + prefix + "-c@example.com").getId();
		groupId = groupRepository.save(new TeamGroup(a, "Original", "original desc")).getId();
		Instant t = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MILLIS);
		memberRepository.save(new GroupMember(groupId, a, GroupRole.LEADER, t));
		memberRepository.save(new GroupMember(groupId, b, GroupRole.MEMBER, t.plusSeconds(1)));
		memberRepository.save(new GroupMember(groupId, c, GroupRole.MEMBER, t.plusSeconds(2)));
	}

	/** Each member's "ROLE/STATUS", plus exactly one ACTIVE LEADER in the group. */
	private void assertStates(Map<Long, String> expected) {
		Map<Long, String> actual = new LinkedHashMap<>();
		jdbc.query("SELECT user_id, role, status FROM group_members WHERE group_id = ?",
				rs -> { actual.put(rs.getLong("user_id"), rs.getString("role") + "/" + rs.getString("status")); },
				groupId);
		assertThat(actual).containsExactlyInAnyOrderEntriesOf(expected);
		assertThat(activeLeaders()).as("exactly one ACTIVE LEADER").isEqualTo(1);
		assertThat(jdbc.queryForObject("SELECT count(*) FROM group_members WHERE group_id = ? AND "
				+ "((status = 'INACTIVE') <> (left_at IS NOT NULL))", Integer.class, groupId)).isZero();
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
