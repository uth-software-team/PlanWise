package group16.webapp.common.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Test-only entity (test classpath, H2 create-drop) used to verify {@link AuditableEntity}. */
@Entity
@Table(name = "test_sample_auditable")
class SampleAuditable extends AuditableEntity {

	private String name;

	protected SampleAuditable() {}

	SampleAuditable(String name) { this.name = name; }

	void setName(String name) { this.name = name; }
}
