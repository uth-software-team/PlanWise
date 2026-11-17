package group16.webapp.common.entity;

import org.springframework.data.jpa.repository.JpaRepository;

/** Test-only repository for {@link SampleAuditable}. */
interface SampleAuditableRepository extends JpaRepository<SampleAuditable, Long> {}
