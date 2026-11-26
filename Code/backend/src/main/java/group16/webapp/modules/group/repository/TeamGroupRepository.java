package group16.webapp.modules.group.repository;

import group16.webapp.modules.group.entity.TeamGroup;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TeamGroupRepository extends JpaRepository<TeamGroup, Long> {

    /**
     * Loads the group with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE). Every membership mutation takes this
     * lock before reading memberships, so concurrent mutations of one group are serialized.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from TeamGroup g where g.id = :id")
    Optional<TeamGroup> findByIdForUpdate(@Param("id") Long id);
}
