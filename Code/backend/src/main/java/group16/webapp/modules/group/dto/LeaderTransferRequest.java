package group16.webapp.modules.group.dto;

import jakarta.validation.constraints.NotNull;

/** The ACTIVE LEADER hands leadership to another ACTIVE member with an active account (D16). */
public record LeaderTransferRequest(
        @NotNull Long newLeaderUserId
) {
}
