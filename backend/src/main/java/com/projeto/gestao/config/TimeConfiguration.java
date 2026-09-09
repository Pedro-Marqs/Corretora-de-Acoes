package com.projeto.gestao.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    public static final ZoneId BRASILIA_ZONE = ZoneId.of("America/Sao_Paulo");

    @Bean
    Clock applicationClock() {
        return Clock.system(BRASILIA_ZONE);
    }
}
