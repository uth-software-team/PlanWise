package group16.webapp.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI: http://localhost:8080/api/swagger-ui.html
 * Click "Authorize" and paste the accessToken returned by /v1/auth/login.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "PlanWise API", version = "v1", description = "PlanWise backend REST API"),
        // Relative to the host serving the docs; matches server.servlet.context-path.
        servers = @Server(url = "/api"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfig {
}
