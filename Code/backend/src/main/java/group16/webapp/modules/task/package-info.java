/**
 * Personal and group tasks: CRUD, status lifecycle, soft delete, computed overdue flag.
 *
 * <p>Owner: TV3 Phan Ky Khoi. ERD tables: tasks.
 *
 * <p>Rules (see Code/backend/README.md, section 3):
 * <ul>
 *   <li>Other modules may only depend on the public interfaces in this module's {@code api} package.</li>
 *   <li>Never import another module's entity, repository or internal service.</li>
 *   <li>References to other modules' tables are stored as raw {@code Long} ids (no cross-module {@code @ManyToOne}).</li>
 * </ul>
 */
package group16.webapp.modules.task;
