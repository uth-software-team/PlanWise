package group16.webapp.modules.schedule.domain;

import group16.webapp.common.entity.BaseEntity;
import group16.webapp.modules.schedule.api.ProposalSource;
import group16.webapp.modules.schedule.api.ProposalStatus;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "schedule_proposals")
@EntityListeners(AuditingEntityListener.class)
public class ScheduleProposal extends BaseEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "week_start", nullable = false)
    private LocalDate weekStart;

    @Column(name = "horizon_start", nullable = false)
    private Instant horizonStart;

    @Column(name = "horizon_end", nullable = false)
    private Instant horizonEnd;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 12)
    private ProposalSource source = ProposalSource.ENGINE;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private ProposalStatus status = ProposalStatus.PROPOSED;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getWeekStart() { return weekStart; }
    public void setWeekStart(LocalDate weekStart) { this.weekStart = weekStart; }
    public Instant getHorizonStart() { return horizonStart; }
    public void setHorizonStart(Instant horizonStart) { this.horizonStart = horizonStart; }
    public Instant getHorizonEnd() { return horizonEnd; }
    public void setHorizonEnd(Instant horizonEnd) { this.horizonEnd = horizonEnd; }
    public ProposalSource getSource() { return source; }
    public void setSource(ProposalSource source) { this.source = source; }
    public ProposalStatus getStatus() { return status; }
    public void setStatus(ProposalStatus status) { this.status = status; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant confirmedAt) { this.confirmedAt = confirmedAt; }
}
