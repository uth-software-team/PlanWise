package group16.webapp.modules.group.api;

/**
 * Role inside one group (design doc GroupRole). There are no system-wide roles; a group with ACTIVE members has
 * exactly one ACTIVE LEADER (team decision D16). Permission checks go through these methods, so a later role change
 * only touches this enum.
 */
public enum GroupRole {
    LEADER,
    MEMBER;

    public boolean canUpdateGroup() {
        return this == LEADER;
    }

    public boolean canAddMembers() {
        return this == LEADER;
    }

    public boolean canRemoveMembers() {
        return this == LEADER;
    }

    public boolean canChangeMemberRoles() {
        return this == LEADER;
    }

    public boolean canTransferLeadership() {
        return this == LEADER;
    }
}
