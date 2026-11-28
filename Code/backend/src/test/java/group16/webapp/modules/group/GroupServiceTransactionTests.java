package group16.webapp.modules.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.group.dto.GroupRequest;
import group16.webapp.modules.group.entity.GroupMember;
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

/**
 * Group creation is one transaction: when saving the LEADER membership fails, the new group must not be persisted.
 * The test class is deliberately not {@code @Transactional}, so the service's own transaction commits or rolls back.
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

	@AfterEach
	void cleanUp() {
		memberRepository.deleteAllInBatch();
		groupRepository.deleteAllInBatch();
		if (userId != null) {
			userRepository.deleteById(userId);
		}
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
}
