package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

class MarketIntegrationConfigurationTests {
    @Test
    void bindsMarketSourcesCredentialsAndIndependentTimeouts() {
        var environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", Map.ofEntries(
                Map.entry("app.integrations.brapi.base-url", "https://brapi.test/api"),
                Map.entry("app.integrations.brapi.token", "secret-brapi"),
                Map.entry("app.integrations.brapi.connect-timeout", "2s"),
                Map.entry("app.integrations.brapi.read-timeout", "4s"),
                Map.entry("app.integrations.twelve-data.base-url", "https://twelve.test"),
                Map.entry("app.integrations.twelve-data.api-key", "secret-twelve"),
                Map.entry("app.integrations.twelve-data.connect-timeout", "3s"),
                Map.entry("app.integrations.twelve-data.read-timeout", "5s"),
                Map.entry("app.integrations.awesome-api.base-url", "https://awesome.test"),
                Map.entry("app.integrations.awesome-api.api-key", "secret-awesome"),
                Map.entry("app.integrations.awesome-api.connect-timeout", "6s"),
                Map.entry("app.integrations.awesome-api.read-timeout", "7s"))));

        ExternalIntegrationProperties properties = Binder.get(environment)
                .bind("app.integrations", ExternalIntegrationProperties.class)
                .orElseThrow(() -> new AssertionError("Market integration properties were not bound"));

        assertThat(properties.brapi().baseUrl()).hasToString("https://brapi.test/api");
        assertThat(properties.brapi().token()).isEqualTo("secret-brapi");
        assertThat(properties.brapi().connectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.twelveData().apiKey()).isEqualTo("secret-twelve");
        assertThat(properties.twelveData().readTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.awesomeApi().apiKey()).isEqualTo("secret-awesome");
        assertThat(properties.awesomeApi().connectTimeout()).isEqualTo(Duration.ofSeconds(6));
    }
}
