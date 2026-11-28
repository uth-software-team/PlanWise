package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.group.api.GroupRole;
import group16.webapp.modules.group.api.MembershipStatus;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.dto.LeaderTransferRequest;
import group16.webapp.modules.group.entity.GroupMember;
import group16.webapp.modules.group.entity.TeamGroup;
import group16.webapp.modules.group.repository.GroupMemberRepository;
import group16.webapp.modules.group.repository.TeamGroupRepository;
import group16.webapp.modules.group.service.GroupService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.transaction.TestTransaction;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Group creation and leadership hand-over are single transactions: when a later write fails, earlier writes of the
 * same call must not remain (B3, B4). H2 has no V11 index, so these tests cover the rollback only. The test class is
 * deliberately not {@code @Transactional}, so the service's own transaction commits or rolls back.
 */
@SpringBootTest
@ActiveProfiles("test")
class GroupServiceTransactionTests {

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private TeamGroupRepository groupRepository;

	@MockitoSpyBean
	private GroupMemberRepository memberRepository;

	private Long userId;
	private final List<Long> extraUserIds = new ArrayList<>();

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAllInBatch();
		groupRepository.deleteAllInBatch();
		if (userId != null) {
			userRepository.deleteById(userId);
		}
		userRepository.deleteAllById(extraUserIds);
		extraUserIds.clear();
	}

	@Test
	void failedMembershipSaveRollsBackTheNewGroup() {
		assertThat(TestTransaction.isActive()).as("must run outside a test-managed transaction").isFalse();
		userId = userRepository.save(new User("rollback@example.com", "$2a$10$hash", "Rollback", null)).getId();
		long groupsBefore = groupRepository.count();
		doThrow(new DataIntegrityViolationException("simulated membership failure"))
				.when(memberRepository).save(any(GroupMember.class));

		assertThatThrownBy(() -> groupService.createGroup(userId, new GroupRequest("Team", "desc")))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertThat(groupRepository.count()).isEqualTo(groupsBefore);
		assertThat(groupRepository.findAll()).noneMatch(g -> g.getCreatedByUserId().equals(userId));
		assertThat(memberRepository.count()).isZero();
	}

	/** B3: the promotion fails after the caller's demotion was flushed. */
	@Test
	void failedPromotionRollsBackTheTransfer() {
		Fixture f = fixture("transfer");
		failSecondSaveAndFlush();

		assertThatThrownBy(() -> groupService.transferLeadership(f.leaderId(), f.groupId(),
				new LeaderTransferRequest(f.memberId())))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertRole(f.groupId(), f.leaderId(), GroupRole.LEADER, MembershipStatus.ACTIVE);
		assertRole(f.groupId(), f.memberId(), GroupRole.MEMBER, MembershipStatus.ACTIVE);
	}

	/** B4: the successor's promotion fails after the LEADER's deactivation was flushed. */
	@Test
	void failedPromotionRollsBackTheLeaderLeaving() {
		Fixture f = fixture("leave");
		failSecondSaveAndFlush();

		assertThatThrownBy(() -> groupService.removeMember(f.leaderId(), f.groupId(), f.leaderId(), f.memberId()))
				.isInstanceOf(DataIntegrityViolationException.class);

		assertRole(f.groupId(), f.leaderId(), GroupRole.LEADER, MembershipStatus.ACTIVE);
		assertRole(f.groupId(), f.memberId(), GroupRole.MEMBER, MembershipStatus.ACTIVE);
	}

	private record Fixture(Long groupId, Long leaderId, Long memberId) {}

	private Fixture fixture(String prefix) {
		Long leaderId = newUser(prefix + "-leader@example.com");
		Long memberId = newUser(prefix + "-member@example.com");
		Long groupId = groupRepository.save(new TeamGroup(leaderId, "Team", "desc")).getId();
		Instant now = Instant.now().truncatedTo(ChronoUnit.MILLIS);
		memberRepository.save(new GroupMember(groupId, leaderId, GroupRole.LEADER, now));
		memberRepository.save(new GroupMember(groupId, memberId, GroupRole.MEMBER, now.plusSeconds(1)));
		return new Fixture(groupId, leaderId, memberId);
	}

	/**
	 * The first saveAndFlush (old LEADER) really runs; the second (new LEADER) fails. The repository is an interface
	 * proxy, so the real call goes through the spy's default answer (which delegates to the bean).
	 */
	private void failSecondSaveAndFlush() {
		org.mockito.stubbing.Answer<?> delegate = mockingDetails(memberRepository).getMockCreationSettings()
				.getDefaultAnswer();
		AtomicInteger calls = new AtomicInteger();
		doAnswer(invocation -> {
			if (calls.getAndIncrement() == 0) {
				return delegate.answer(invocation);
			}
			throw new DataIntegrityViolationException("simulated promotion failure");
		}).when(memberRepository).saveAndFlush(any(GroupMember.class));
	}

	private void assertRole(Long groupId, Long userId, GroupRole role, MembershipStatus status) {
		GroupMember row = memberRepository.findByGroupIdAndUserId(groupId, userId).orElseThrow();
		assertThat(row.getRole()).isEqualTo(role);
		assertThat(row.getStatus()).isEqualTo(status);
		assertThat(row.getLeftAt()).isNull();
	}

	private Long newUser(String email) {
		Long id = userRepository.save(new User(email, "$2a$10$hash", "User " + email, null)).getId();
		extraUserIds.add(id);
		return id;
	}
}
