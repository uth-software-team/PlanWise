package group16.webapp.support;

import org.junit.jupiter.api.Tag;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for PostgreSQL tests (run with {@code ./gradlew pgTest}; requires Docker).
 *
 * <p>Uses a throwaway {@code postgres:16} container (same image as compose.yaml), shared by every subclass in
 * the JVM, never the developer's database. {@link ServiceConnection} replaces the H2 datasource of the
 * {@code test} profile; Flyway runs all migrations and Hibernate validates the entities against them.
 *
 * <p>Subclasses only add tests; keep the same configuration so the Spring context is reused.
 */
@Tag("pg")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        // Application migrations + test-only tables for test entities (versions >= 9000, src/test/resources).
        "spring.flyway.locations=classpath:db/migration,classpath:db/test-migration",
        "spring.jpa.hibernate.ddl-auto=validate"
})
public abstract class PostgresContainerTest {

    @ServiceConnection
    protected static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16");

    static {
        // Singleton container: started once per JVM, removed by Testcontainers when the JVM exits.
        POSTGRES.start();
    }
}
