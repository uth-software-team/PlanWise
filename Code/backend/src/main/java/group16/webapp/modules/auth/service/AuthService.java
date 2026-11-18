package group16.webapp.modules.auth.service;

import group16.webapp.modules.auth.dto.AuthResponse;
import group16.webapp.modules.auth.dto.LoginRequest;
import group16.webapp.modules.auth.dto.RegisterRequest;
import group16.webapp.modules.auth.dto.UserResponse;

public interface AuthService {

    /** Creates an account. 409 EMAIL_ALREADY_EXISTS if the (normalized) email is taken. */
    UserResponse register(RegisterRequest request);

    /** 401 INVALID_CREDENTIALS for unknown email, wrong password or inactive account (same message). */
    AuthResponse login(LoginRequest request);
}
