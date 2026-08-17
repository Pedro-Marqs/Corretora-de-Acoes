package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LocalDatabaseConfigurationTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withConfiguration(AutoConfigurations.of(
                    PropertyPlaceholderAutoConfiguration.class,
                    ValidationAutoConfiguration.class))
            .withUserConfiguration(LocalDatabaseConfiguration.class)
            .withPropertyValues(
                    "spring.profiles.active=local",
                    "DB_HOST=database.local",
                    "POSTGRES_PORT=55432",
                    "POSTGRES_DB=gestao_acoes",
                    "POSTGRES_USER=app_user",
                    "POSTGRES_PASSWORD=local_password");

    @Test
    void localProfileActivatesDevAndCreatesPostgreSqlDataSource() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getEnvironment().getActiveProfiles())
                    .containsExactly("local", "dev");

            assertThat(context).hasSingleBean(HikariDataSource.class);
            HikariDataSource dataSource = context.getBean(HikariDataSource.class);
            assertThat(dataSource.getJdbcUrl())
                    .isEqualTo("jdbc:postgresql://database.local:55432/gestao_acoes");
            assertThat(dataSource.getUsername()).isEqualTo("app_user");
            assertThat(dataSource.getPassword()).isEqualTo("local_password");
        });
    }

    @Test
    void localProfileRejectsMissingRequiredDatabaseProperties() {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(
                        PropertyPlaceholderAutoConfiguration.class,
                        ValidationAutoConfiguration.class))
                .withUserConfiguration(LocalDatabaseConfiguration.class)
                .withPropertyValues("spring.profiles.active=local")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.database.name")
                            .hasStackTraceContaining("app.database.username")
                            .hasStackTraceContaining("app.database.password");
                });
    }
}
