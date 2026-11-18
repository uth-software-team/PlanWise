/**
 * Team groups and memberships; LEADER/MEMBER roles are checked per groupId.
 *
 * <p>Owner: TV4 Ho Ngoc Anh Thu. ERD tables: team_groups, group_members.
 *
 * <p>Rules (see Code/backend/README.md, section 3):
 * <ul>
 *   <li>Other modules may only depend on the public interfaces in this module's {@code api} package.</li>
 *   <li>Never import another module's entity, repository or internal service.</li>
 *   <li>References to other modules' tables are stored as raw {@code Long} ids (no cross-module {@code @ManyToOne}).</li>
 * </ul>
 */
package group16.webapp.modules.group;
