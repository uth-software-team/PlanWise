package group16.webapp.modules.auth;

import static org.assertj.core.api.Assertions.assertThat;

import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;

@SpringBootTest
@ActiveProfiles("test")
class UserApiTests {

	@Autowired
	private UserApi userApi;

	@Autowired
	private UserRepository userRepository;

	private User saved;

	@BeforeEach
	void setUp() {
		userRepository.deleteAll();
		saved = userRepository.save(new User("anna@example.com", "$2a$10$hash", "Anna", null));
	}

	@Test
	void newUserGetsV1Defaults() {
		User user = userRepository.findById(saved.getId()).orElseThrow();
		assertThat(user.getTimezone()).isEqualTo("Asia/Ho_Chi_Minh");
		assertThat(user.getPlanningStart()).isEqualTo(User.DEFAULT_PLANNING_START);
		assertThat(user.getPlanningEnd()).isEqualTo(User.DEFAULT_PLANNING_END);
		assertThat(user.isActive()).isTrue();
		assertThat(user.getCreatedAt()).isNotNull();
	}

	@Test
	void findByEmailNormalizesInput() {
		assertThat(userApi.findByEmail("  ANNA@Example.COM ")).map(UserSummary::id).contains(saved.getId());
		assertThat(userApi.findByEmail("nobody@example.com")).isEmpty();
		assertThat(userApi.findByEmail("   ")).isEmpty();
		assertThat(userApi.findByEmail(null)).isEmpty();
	}

	@Test
	void findByIdReturnsSummaryWithoutPassword() {
		UserSummary summary = userApi.findById(saved.getId()).orElseThrow();
		assertThat(summary.email()).isEqualTo("anna@example.com");
		assertThat(summary.fullName()).isEqualTo("Anna");
		assertThat(summary.timezone()).isEqualTo("Asia/Ho_Chi_Minh");
		assertThat(Arrays.stream(UserSummary.class.getRecordComponents()).map(c -> c.getName()))
				.doesNotContain("passwordHash", "password");
		assertThat(userApi.findById(-1L)).isEmpty();
		assertThat(userApi.findById(null)).isEmpty();
	}
}
