package group16.webapp.modules.schedule.repository;

import group16.webapp.modules.schedule.api.ProposalStatus;
import group16.webapp.modules.schedule.domain.ScheduleProposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScheduleProposalRepository extends JpaRepository<ScheduleProposal, Long> {
    Optional<ScheduleProposal> findByUserIdAndStatus(Long userId, ProposalStatus status);
    Optional<ScheduleProposal> findByIdAndUserId(Long id, Long userId);
    List<ScheduleProposal> findByUserIdOrderByCreatedAtDesc(Long userId);
}
