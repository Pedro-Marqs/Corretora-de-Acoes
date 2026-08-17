package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.TimeZone;

import javax.sql.DataSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class ProfileConfigurationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void testProfileUsesH2InPostgreSqlCompatibilityMode() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("""
                        SELECT SETTING_VALUE
                        FROM INFORMATION_SCHEMA.SETTINGS
                        WHERE SETTING_NAME = 'MODE'
                        """)) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("H2");
            assertThat(result.next()).isTrue();
            assertThat(result.getString("SETTING_VALUE")).isEqualTo("PostgreSQL");
        }
    }

    @Test
    void applicationUsesSaoPauloTimeZone() {
        assertThat(environment.getProperty("app.time-zone"))
                .isEqualTo("America/Sao_Paulo");
        assertThat(objectMapper.getSerializationConfig().getTimeZone())
                .isEqualTo(TimeZone.getTimeZone("America/Sao_Paulo"));
    }

}
