package group16.webapp.modules.auth.service.impl;

import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.api.UserSummary;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserApiImpl implements UserApi {

    private final UserRepository userRepository;

    public UserApiImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserSummary> findById(Long userId) {
        return userId == null ? Optional.empty() : userRepository.findById(userId).map(UserApiImpl::toSummary);
    }

    @Override
    public Optional<UserSummary> findByEmail(String email) {
        String normalized = UserApi.normalizeEmail(email);
        return normalized == null || normalized.isEmpty()
                ? Optional.empty()
                : userRepository.findByEmail(normalized).map(UserApiImpl::toSummary);
    }

    private static UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getEmail(), user.getFullName(), user.getTimezone(), user.isActive());
    }
}
