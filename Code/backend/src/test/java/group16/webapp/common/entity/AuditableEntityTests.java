package group16.webapp.common.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Verifies the shared mapped superclasses against a test-only entity on the isolated H2 test DB:
 * identity id generation and automatic created_at/updated_at auditing.
 */
@SpringBootTest
@ActiveProfiles("test")
class AuditableEntityTests {

	@Autowired
	private SampleAuditableRepository repository;

	@Test
	void assignsIdAndAuditTimestampsOnInsert() {
		Instant before = Instant.now();

		SampleAuditable saved = repository.saveAndFlush(new SampleAuditable("first"));

		assertThat(saved.getId()).isNotNull().isPositive();
		assertThat(saved.getCreatedAt()).isNotNull().isAfterOrEqualTo(before);
		assertThat(saved.getUpdatedAt()).isNotNull().isAfterOrEqualTo(saved.getCreatedAt());
	}

	@Test
	void refreshesUpdatedAtButKeepsCreatedAtOnUpdate() throws InterruptedException {
		SampleAuditable saved = repository.saveAndFlush(new SampleAuditable("before"));
		Instant createdAt = saved.getCreatedAt();
		Instant firstUpdatedAt = saved.getUpdatedAt();

		Thread.sleep(5);
		saved.setName("after");
		SampleAuditable updated = repository.saveAndFlush(saved);

		assertThat(updated.getCreatedAt()).isEqualTo(createdAt);
		assertThat(updated.getUpdatedAt()).isAfter(firstUpdatedAt);
		// Reload by id to confirm the change was actually persisted.
		assertThat(repository.findById(updated.getId()).orElseThrow().getName()).isEqualTo("after");
	}

}
