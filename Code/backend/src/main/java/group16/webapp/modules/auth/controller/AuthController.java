package group16.webapp.modules.auth.controller;

import group16.webapp.common.dto.ApiResponse;
import group16.webapp.modules.auth.dto.AuthResponse;
import group16.webapp.modules.auth.dto.LoginRequest;
import group16.webapp.modules.auth.dto.RegisterRequest;
import group16.webapp.modules.auth.dto.UserResponse;
import group16.webapp.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC-05 account endpoints. register/login are public; logout requires a valid token (SecurityConfig).
 * {@code @SecurityRequirements} (empty) and {@code @ResponseStatus} only align the OpenAPI document with
 * that runtime behavior; the returned ResponseEntity still sets the actual status.
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Registered", authService.register(request)));
    }

    @PostMapping("/login")
    @SecurityRequirements
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok("Logged in", authService.login(request));
    }

    /** Stateless JWT (G7): the client discards its token; the server keeps no session to end. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
