package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

class AdminJwtSecurityConfigurationTests {
    private final AdminJwtSecurityConfiguration configuration = new AdminJwtSecurityConfiguration();

    @Test
    void propertiesUseAdministrativeDefaultsAndRequiredTtls() {
        AdminJwtSecurityProperties defaults = new AdminJwtSecurityProperties(
                false, null, "", null, null);

        assertThat(defaults.issuer()).isEqualTo("corretora-admin");
        assertThat(defaults.audience()).isEqualTo("corretora-admin-api");
        assertThat(defaults.accessTokenTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(defaults.refreshTokenTtl()).isEqualTo(Duration.ofHours(8));
        assertThatThrownBy(() -> new AdminJwtSecurityProperties(
                false, "issuer", "audience", Duration.ZERO, "secret"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new AdminJwtSecurityProperties(
                false, "issuer", "audience", Duration.ofMinutes(1), "secret",
                Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void administrativeSecretMustBeBase64AndAtLeast256Bits() {
        String validSecret = Base64.getEncoder().encodeToString(new byte[32]);
        SecretKey secretKey = configuration.adminJwtSecretKey(properties(validSecret));

        assertThat(secretKey.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThatThrownBy(() -> configuration.adminJwtSecretKey(properties(" ")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.adminJwtSecretKey(properties("not-base64")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.adminJwtSecretKey(properties(
                Base64.getEncoder().encodeToString(new byte[16]))))
                .isInstanceOf(IllegalStateException.class);
    }

    private AdminJwtSecurityProperties properties(String secret) {
        return new AdminJwtSecurityProperties(true, "corretora-admin",
                "corretora-admin-api", Duration.ofMinutes(10), secret, Duration.ofHours(8));
    }
}
