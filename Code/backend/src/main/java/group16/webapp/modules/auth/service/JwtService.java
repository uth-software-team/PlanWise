package group16.webapp.modules.auth.service;

import group16.webapp.modules.auth.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/**
 * Issues HS256 access tokens (team decision CG-04): sub = user id, claim "email", lifetime
 * {@code app.jwt.expiration-minutes} (default 24h). No refresh token (D11) and no role claim (D8).
 */
@Service
public class JwtService {

    public static final String ISSUER = "planwise";

    private final JwtEncoder jwtEncoder;
    private final Duration lifetime;

    public JwtService(JwtEncoder jwtEncoder, @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        if (expirationMinutes <= 0) {
            throw new IllegalStateException("app.jwt.expiration-minutes must be positive");
        }
        this.jwtEncoder = jwtEncoder;
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(lifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String value = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(value, expiresAt);
    }

    public record IssuedToken(String value, Instant expiresAt) {}
}
