package intercoach.config;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("production")
public class ProductionConfigurationGuard {

    private static final String DEVELOPMENT_SECRET =
            "change-this-development-secret-before-production-12345";

    public ProductionConfigurationGuard(
            Environment environment,
            CodeExecutionProperties execution
    ) {
        requireValue(environment, "spring.datasource.url");
        requireValue(environment, "spring.datasource.username");
        String password = requireValue(environment, "spring.datasource.password");
        String secret = requireValue(environment, "intercoach.jwt.secret");
        requireValue(environment, "spring.ai.openai.api-key");

        // Inspect effective values so environment overrides cannot weaken the profile.
        if ("codecoach".equals(password)) {
            throw new IllegalStateException(
                    "Production database password must not use the development default."
            );
        }
        if (DEVELOPMENT_SECRET.equals(secret) || secret.strip().length() < 32) {
            throw new IllegalStateException(
                    "Production JWT secret must be at least 32 non-padding characters "
                            + "and must not use the development default."
            );
        }
        if (execution.getMode() != CodeExecutionProperties.ExecutionMode.DOCKER
                || !execution.isRequireOsIsolation()) {
            throw new IllegalStateException(
                    "Production requires Docker execution with OS isolation enabled."
            );
        }
    }

    private String requireValue(Environment environment, String property) {
        String value = environment.getProperty(property);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Production requires a non-blank " + property + "."
            );
        }
        return value;
    }
}
