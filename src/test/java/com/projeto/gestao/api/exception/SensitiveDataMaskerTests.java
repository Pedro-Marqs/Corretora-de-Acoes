package com.projeto.gestao.api.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataMaskerTests {
    @Test
    void masksCpfEmailAndSecretsWithoutKeepingTheirValues() {
        String masked = SensitiveDataMasker.mask(
                "cpf=123.456.789-00 email=investidor@example.com; senha=uma senha secreta; "
                        + "cookie=session value; api_key=key value; Authorization: Bearer token com espaços");

        assertThat(masked)
                .contains("123.***.***-00", "i***@example.com", "senha=***", "cookie=***", "api_key=***",
                        "Authorization=***")
                .doesNotContain("123.456.789-00", "investidor@example.com", "uma senha secreta", "session value",
                        "key value", "token com espaços", "Bearer");
    }

    @Test
    void acceptsNullAndTextWithoutSensitiveData() {
        assertThat(SensitiveDataMasker.mask(null)).isNull();
        assertThat(SensitiveDataMasker.mask("operação concluída")).isEqualTo("operação concluída");
    }
}
