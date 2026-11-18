package group16.webapp.modules.auth.service.impl;

import group16.webapp.common.exception.AppException;
import group16.webapp.common.exception.ErrorCode;
import group16.webapp.common.util.DateUtils;
import group16.webapp.modules.auth.api.UserApi;
import group16.webapp.modules.auth.dto.AuthResponse;
import group16.webapp.modules.auth.dto.LoginRequest;
import group16.webapp.modules.auth.dto.RegisterRequest;
import group16.webapp.modules.auth.dto.UserResponse;
import group16.webapp.modules.auth.entity.User;
import group16.webapp.modules.auth.mapper.UserMapper;
import group16.webapp.modules.auth.repository.UserRepository;
import group16.webapp.modules.auth.service.AuthService;
import group16.webapp.modules.auth.service.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthServiceImpl implements AuthService {

    /** BCrypt only uses the first 72 bytes of its input; longer passwords are rejected, not truncated. */
    static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    /** Compared against when the email is unknown, so response time does not reveal which emails exist. */
    private final String dummyPasswordHash;

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode("planwise-timing-equalizer");
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        String email = UserApi.normalizeEmail(request.email());
        String timezone = blankToNull(request.timezone());
        if (timezone != null && !DateUtils.isValidTimezone(timezone)) {
            throw invalidField("timezone", "must be a valid IANA timezone, e.g. Asia/Ho_Chi_Minh");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            throw invalidField("password", "must be at most " + MAX_PASSWORD_BYTES + " bytes");
        }
        if (userRepository.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.fullName().trim(), timezone);
        try {
            return UserMapper.toResponse(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException e) {
            // Two concurrent sign-ups with the same email: the unique key decides, report it the same way.
            throw new AppException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Optional<User> found = userRepository.findByEmail(UserApi.normalizeEmail(request.email()));
        String hash = found.map(User::getPasswordHash).orElse(dummyPasswordHash);
        boolean passwordMatches = request.password().getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES
                && passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !passwordMatches || !found.get().isActive()) {
            throw new AppException(ErrorCode.INVALID_CREDENTIALS);
        }
        User user = found.get();
        JwtService.IssuedToken token = jwtService.issue(user);
        return new AuthResponse(token.value(), AuthResponse.BEARER, token.expiresAt(), UserMapper.toResponse(user));
    }

    private static AppException invalidField(String field, String message) {
        return new AppException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(), Map.of(field, message));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
