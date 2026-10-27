package group16.webapp.config;

import group16.webapp.common.util.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Auto-fills BaseEntity audit columns:
 * createdAt/updatedAt (time) and createdBy/updatedBy (id of the logged-in user).
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<Long> auditorAware() {
        return SecurityUtils::getCurrentUserId;
    }
}
