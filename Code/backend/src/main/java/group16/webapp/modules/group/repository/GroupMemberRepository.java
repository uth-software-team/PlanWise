package group16.webapp.modules.group.repository;

import group16.webapp.modules.group.api.MembershipStatus;
import group16.webapp.modules.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    /** The caller's membership in one group with the given status (used for access checks). */
    Optional<GroupMember> findByGroupIdAndUserIdAndStatus(Long groupId, Long userId, MembershipStatus status);

    /** The membership row of a user in a group regardless of status (one row per group and user). */
    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long userId);

    /** All of one user's memberships with the given status ("my groups"). */
    List<GroupMember> findByUserIdAndStatus(Long userId, MembershipStatus status);

    /** A group's members with the given status, oldest first. Call only after the caller's access is checked. */
    List<GroupMember> findByGroupIdAndStatusOrderByJoinedAtAscIdAsc(Long groupId, MembershipStatus status);
}
