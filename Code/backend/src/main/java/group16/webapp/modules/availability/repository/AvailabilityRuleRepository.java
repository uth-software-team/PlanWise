package group16.webapp.modules.availability.repository;

import group16.webapp.modules.availability.entity.AvailabilityRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AvailabilityRuleRepository extends JpaRepository<AvailabilityRuleEntity, Long> {

    List<AvailabilityRuleEntity> findByUserIdOrderByDayOfWeekAscStartTimeAsc(Long userId);

    Optional<AvailabilityRuleEntity> findByIdAndUserId(Long id, Long userId);

    boolean existsByUserIdAndActiveTrue(Long userId);

    List<AvailabilityRuleEntity> findByUserIdAndActiveTrue(Long userId);
}