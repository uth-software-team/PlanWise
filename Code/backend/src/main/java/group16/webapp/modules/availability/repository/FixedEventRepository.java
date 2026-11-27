package group16.webapp.modules.availability.repository;

import group16.webapp.modules.availability.entity.FixedEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FixedEventRepository extends JpaRepository<FixedEventEntity, Long> {

    List<FixedEventEntity> findByUserIdOrderByCreatedAtAscIdAsc(Long userId);

    List<FixedEventEntity> findByUserId(Long userId);

    Optional<FixedEventEntity> findByIdAndUserId(Long id, Long userId);
}