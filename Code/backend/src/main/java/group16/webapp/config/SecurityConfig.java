package group16.webapp.config;

import group16.webapp.common.exception.ErrorCode;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

/**
 * - Stateless JWT (HS256) authentication via Spring OAuth2 Resource Server
 * - CORS for the frontend
 * - Endpoint authorization rules
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /** Paths are relative to the context-path (/api). Logout is NOT public: it needs the current user. */
    private static final String[] PUBLIC_ENDPOINTS = {
            "/v1/auth/register", "/v1/auth/login", "/v1/auth/password/**",
            "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**"
    };

    /** HS256 requires a key of at least 256 bits. */
    static final int MIN_JWT_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final List<String> allowedOrigins;

    public SecurityConfig(@Value("${app.jwt.secret}") String jwtSecret,
                          @Value("${app.cors.allowed-origins}") String allowedOrigins) {
        this.secretKey = new SecretKeySpec(validateJwtSecret(jwtSecret), "HmacSHA256");
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // No system-wide roles (actors are Guest/User only). LEADER/MEMBER are per-group
                        // and are enforced in the service layer by groupId, not by URL rules here.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint((req, res, ex) -> writeError(res, ErrorCode.UNAUTHENTICATED)))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, e) -> writeError(res, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((req, res, e) -> writeError(res, ErrorCode.FORBIDDEN)));
        return http.build();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /** Maps the JWT "roles" claim (e.g. ["ADMIN"]) to authorities ROLE_ADMIN. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("roles");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /** Fail fast at startup instead of signing tokens with a missing or weak key. Never logs the value. */
    static byte[] validateJwtSecret(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Define it in Code/backend/.env or as an environment variable.");
        }
        byte[] bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_JWT_SECRET_BYTES) {
            throw new IllegalStateException("JWT_SECRET must be at least " + MIN_JWT_SECRET_BYTES
                    + " bytes (got " + bytes.length + ").");
        }
        return bytes;
    }

    /** 401/403 raised by the security filter chain (before reaching controllers). */
    private static void writeError(HttpServletResponse res, ErrorCode ec) throws IOException {
        res.setStatus(ec.getStatus().value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding(StandardCharsets.UTF_8.name());
        res.getWriter().write("{\"success\":false,\"code\":\"" + ec.name() + "\",\"message\":\""
                + ec.getMessage() + "\",\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
