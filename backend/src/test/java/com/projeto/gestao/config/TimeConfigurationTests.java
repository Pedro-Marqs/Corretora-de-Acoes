package com.projeto.gestao.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class TimeConfigurationTests {
    @Test
    void springContextPublishesOneInjectableClockUsingBrasiliaTimeZone() {
        try (AnnotationConfigApplicationContext context =
                new AnnotationConfigApplicationContext(TimeConfiguration.class)) {
            Clock clock = context.getBean(Clock.class);

            assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
            assertThat(clock.getZone()).isEqualTo(TimeConfiguration.BRASILIA_ZONE);
        }
    }

    @Test
    void applicationClockCanBeReplacedByAFixedClockForTests() {
        Instant instant = Instant.parse("2026-08-17T13:00:00Z");
        Clock fixedClock = Clock.fixed(instant, TimeConfiguration.BRASILIA_ZONE);

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.setAllowBeanDefinitionOverriding(true);
            context.register(TimeConfiguration.class);
            context.registerBean("applicationClock", Clock.class, () -> fixedClock);
            context.refresh();

            Clock injectedClock = context.getBean(Clock.class);
            OffsetDateTime dateTime = OffsetDateTime.now(injectedClock);

            assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
            assertThat(injectedClock).isSameAs(fixedClock);
            assertThat(dateTime.toString()).isEqualTo("2026-08-17T10:00-03:00");
        }
    }
}
