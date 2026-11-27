package group16.webapp.modules.schedule.repository;

import group16.webapp.modules.schedule.api.ProposalStatus;
import group16.webapp.modules.schedule.domain.ScheduleProposal;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ScheduleProposalRepository extends JpaRepository<ScheduleProposal, Long> {
    Optional<ScheduleProposal> findByUserIdAndStatus(Long userId, ProposalStatus status);
    Optional<ScheduleProposal> findByIdAndUserId(Long id, Long userId);
    List<ScheduleProposal> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Loads the caller's proposal with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE). Confirm takes it before
     * checking status/version or reading constraint data, so two confirms of one batch are serialized and the
     * second one sees the committed result (409 DATA_CONFLICT / STALE_VERSION), never its own confirmed sessions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from ScheduleProposal p where p.id = :id and p.userId = :userId")
    Optional<ScheduleProposal> findByIdAndUserIdForUpdate(@Param("id") Long id, @Param("userId") Long userId);
}
