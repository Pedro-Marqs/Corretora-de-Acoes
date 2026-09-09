package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.context.support.GenericApplicationContext;

class LocalSecurityPropertyTests {
    @Test
    void localProfileExplicitlyDisablesSecureCookiesForHttpDevelopment() {
        GenericApplicationContext context = new GenericApplicationContext();
        context.getEnvironment().setActiveProfiles("local");
        new ConfigDataApplicationContextInitializer().initialize(context);
        context.refresh();
        try {
            assertThat(context.getEnvironment().getProperty(
                    "app.security.session-cookie-secure", Boolean.class)).isFalse();
        } finally {
            context.close();
        }
    }
}
