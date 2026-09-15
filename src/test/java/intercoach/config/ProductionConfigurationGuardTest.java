package intercoach.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionConfigurationGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withUserConfiguration(TestConfiguration.class);

    @Test
    void productionBindsIsolatedExecutionAndDeploymentDefaults() {
        production().run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ProductionConfigurationGuard.class);
            CodeExecutionProperties execution = context.getBean(CodeExecutionProperties.class);
            assertThat(execution.getMode())
                    .isEqualTo(CodeExecutionProperties.ExecutionMode.DOCKER);
            assertThat(execution.isRequireOsIsolation()).isTrue();
            assertThat(context.getEnvironment().getProperty("spring.flyway.baseline-on-migrate"))
                    .isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("server.shutdown"))
                    .isEqualTo("graceful");
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "spring.datasource.url", "spring.datasource.username",
            "spring.datasource.password", "intercoach.jwt.secret", "spring.ai.openai.api-key"
    })
    void productionRejectsMissingCredentials(String property) {
        production().withPropertyValues(property + "= ").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Production requires a non-blank " + property + "."
            );
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "short", "                                ",
            "change-this-development-secret-before-production-12345"
    })
    void productionRejectsWeakOrDefaultJwtSecrets(String secret) {
        production().withPropertyValues("intercoach.jwt.secret=" + secret)
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void productionRejectsDevelopmentDatabasePassword() {
        production().withPropertyValues("spring.datasource.password=codecoach")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasRootCauseMessage(
                            "Production database password must not use the development default."
                    );
                });
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "intercoach.execution.mode=local",
            "intercoach.execution.require-os-isolation=false"
    })
    void productionRejectsDisabledIsolation(String override) {
        production().withPropertyValues(override).run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "Production requires Docker execution with OS isolation enabled."
            );
        });
    }

    @Test
    void developmentRemainsUsableWithoutProductionCredentials() {
        runner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ProductionConfigurationGuard.class);
        });
    }

    private ApplicationContextRunner production() {
        return runner.withPropertyValues(
                "spring.profiles.active=production",
                "spring.datasource.url=jdbc:postgresql://db:5432/intercoach",
                "spring.datasource.username=intercoach",
                "spring.datasource.password=test-database-secret",
                "intercoach.jwt.secret=test-signing-key-with-at-least-32-characters",
                "spring.ai.openai.api-key=test-api-key"
        );
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CodeExecutionProperties.class)
    @Import(ProductionConfigurationGuard.class)
    static class TestConfiguration {
    }
}
