package com.projeto.gestao.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.database")
public record LocalDatabaseProperties(
        @NotBlank String host,
        @Min(1) @Max(65_535) int port,
        @NotBlank String name,
        @NotBlank String username,
        @NotBlank String password) {
}
