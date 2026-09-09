package com.projeto.gestao.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("dev")
@EnableConfigurationProperties(LocalDatabaseProperties.class)
public class LocalDatabaseConfiguration {

    @Bean
    DataSource localDataSource(LocalDatabaseProperties properties) {
        String url = "jdbc:postgresql://%s:%d/%s".formatted(
                properties.host(), properties.port(), properties.name());

        return DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver")
                .url(url)
                .username(properties.username())
                .password(properties.password())
                .build();
    }
}
