package group16.webapp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables @CreatedDate / @LastModifiedDate (see common/entity/AuditableEntity).
 * No AuditorAware: the ERD has no created_by/updated_by columns; ownership is stored
 * explicitly per table (e.g. tasks.owner_user_id).
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
