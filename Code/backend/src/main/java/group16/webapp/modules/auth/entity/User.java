package group16.webapp.modules.auth.entity;

import group16.webapp.common.entity.AuditableEntity;
import group16.webapp.common.util.DateUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalTime;

/**
 * Account of a registered user (ERD "USERS", migration V1). Email is stored trimmed and lower-cased.
 * Defaults mirror the V1 column defaults because Hibernate always writes every column on insert.
 */
@Entity
@Table(name = "users")
public class User extends AuditableEntity {

    public static final LocalTime DEFAULT_PLANNING_START = LocalTime.of(8, 0);
    public static final LocalTime DEFAULT_PLANNING_END = LocalTime.of(18, 0);

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "timezone", nullable = false, length = 50)
    private String timezone = DateUtils.DEFAULT_TIMEZONE;

    @Column(name = "planning_start", nullable = false)
    private LocalTime planningStart = DEFAULT_PLANNING_START;

    @Column(name = "planning_end", nullable = false)
    private LocalTime planningEnd = DEFAULT_PLANNING_END;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    protected User() {}

    public User(String email, String passwordHash, String fullName, String timezone) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        if (timezone != null) {
            this.timezone = timezone;
        }
    }

    /** Class diagram: updateProfile(name, zoneId). Caller validates the IANA timezone. */
    public void updateProfile(String fullName, String timezone) {
        this.fullName = fullName;
        this.timezone = timezone;
    }

    /** Display window only (BR-07: it does not limit the scheduling engine). Caller ensures start < end. */
    public void updatePlanningWindow(LocalTime planningStart, LocalTime planningEnd) {
        this.planningStart = planningStart;
        this.planningEnd = planningEnd;
    }

    public void deactivate() { this.active = false; }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getTimezone() { return timezone; }
    public LocalTime getPlanningStart() { return planningStart; }
    public LocalTime getPlanningEnd() { return planningEnd; }
    public boolean isActive() { return active; }
}
