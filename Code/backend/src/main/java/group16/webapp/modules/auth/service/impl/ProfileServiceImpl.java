package group16.webapp.modules.auth.service.impl;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.auth.dto.ProfileUpdateRequest;
import group16.webapp.modules.auth.dto.UserResponse;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.mapper.UserMapper;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserRepository userRepository;

    public ProfileServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrent(Long userId) {
        return UserMapper.toResponse(requireActiveUser(userId));
    }

    @Override
    @Transactional
    public UserResponse update(Long userId, ProfileUpdateRequest request) {
        String timezone = request.timezone().trim();
        if (!DateUtils.isValidTimezone(timezone)) {
            throw invalidField("timezone", "must be a valid IANA timezone, e.g. Asia/Ho_Chi_Minh");
        }
        boolean hasStart = request.planningStart() != null;
        boolean hasEnd = request.planningEnd() != null;
        if (hasStart != hasEnd) {
            throw invalidField(hasStart ? "planningEnd" : "planningStart",
                    "planningStart and planningEnd must be sent together");
        }
        if (hasStart && !request.planningStart().isBefore(request.planningEnd())) {
            throw new AppException(ErrorCode.INVALID_TIME_RANGE, "planningStart must be before planningEnd",
                    Map.of("planningEnd", "must be after planningStart"));
        }

        User user = requireActiveUser(userId);
        user.updateProfile(request.fullName().trim(), timezone);
        if (hasStart) {
            user.updatePlanningWindow(request.planningStart(), request.planningEnd());
        }
        return UserMapper.toResponse(userRepository.saveAndFlush(user));
    }

    /** A token stays valid for 24h, so a deleted or deactivated account must be rejected here. */
    private User requireActiveUser(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHENTICATED));
    }

    private static AppException invalidField(String field, String message) {
        return new AppException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), Map.of(field, message));
    }
}
