package group16.webapp.modules.auth.repository;

import group16.webapp.modules.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Internal to the auth module. Other modules use {@link group16.webapp.modules.auth.api.UserApi}. */
public interface UserRepository extends JpaRepository<User, Long> {

    /** {@code email} must already be normalized (trimmed, lower-case). */
    Optional<User> findByEmail(String email);

    /** {@code email} must already be normalized (trimmed, lower-case). */
    boolean existsByEmail(String email);
}
