package group16.webapp.modules.schedule.repository;

import group16.webapp.modules.schedule.api.SessionStatus;
import group16.webapp.modules.schedule.domain.ScheduleSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ScheduleSessionRepository extends JpaRepository<ScheduleSession, Long> {
    List<ScheduleSession> findByProposalIdOrderByStartAtAsc(Long proposalId);
    Optional<ScheduleSession> findByIdAndUserId(Long id, Long userId);
    List<ScheduleSession> findByUserIdAndStatusInAndStartAtLessThanAndEndAtGreaterThan(
            Long userId, Collection<SessionStatus> statuses, Instant endAt, Instant startAt);
    /** The user's sessions of the given tasks and statuses (remaining-duration check; never the whole history). */
    List<ScheduleSession> findByUserIdAndTaskIdInAndStatusIn(
            Long userId, Collection<Long> taskIds, Collection<SessionStatus> statuses);
}
