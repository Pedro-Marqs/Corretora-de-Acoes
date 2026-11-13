package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Base64;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

class JwtSecurityConfigurationTests {
    private final JwtSecurityConfiguration configuration = new JwtSecurityConfiguration();

    @Test
    void propertiesApplySafeDefaultsOnlyWhenValuesAreMissing() {
        JwtSecurityProperties defaults = new JwtSecurityProperties(
                false, true, null, "", null, null);
        JwtSecurityProperties configured = new JwtSecurityProperties(
                true, false, "issuer", "audience", Duration.ofMinutes(5), "secret");

        assertThat(defaults.issuer()).isEqualTo("corretora-api");
        assertThat(defaults.audience()).isEqualTo("corretora-investors");
        assertThat(defaults.accessTokenTtl()).isEqualTo(Duration.ofMinutes(10));
        assertThat(configured.issuer()).isEqualTo("issuer");
        assertThat(configured.audience()).isEqualTo("audience");
        assertThat(configured.accessTokenTtl()).isEqualTo(Duration.ofMinutes(5));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtSecurityProperties(
                true, false, "issuer", "audience", Duration.ZERO, "secret"))
                .isInstanceOf(IllegalArgumentException.class);
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> new JwtSecurityProperties(
                true, false, "issuer", "audience", Duration.ofMinutes(-1), "secret"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void secretMustBeBase64AndAtLeast256Bits() {
        String validSecret = Base64.getEncoder().encodeToString(new byte[32]);
        JwtSecurityProperties valid = propertiesWithSecret(validSecret);
        SecretKey secretKey = configuration.jwtSecretKey(valid);

        assertThat(secretKey.getAlgorithm()).isEqualTo("HmacSHA256");
        assertThatThrownBy(() -> configuration.jwtSecretKey(propertiesWithSecret(" ")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.jwtSecretKey(propertiesWithSecret("not-base64")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> configuration.jwtSecretKey(propertiesWithSecret(
                Base64.getEncoder().encodeToString(new byte[16]))))
                .isInstanceOf(IllegalStateException.class);
    }

    private JwtSecurityProperties propertiesWithSecret(String secret) {
        return new JwtSecurityProperties(true, false, "issuer", "audience",
                Duration.ofMinutes(10), secret);
    }
}
