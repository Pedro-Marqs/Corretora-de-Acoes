package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class DashboardPeriodTests {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-08T12:30:00-03:00");
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2018-02-03T09:15:00-02:00");

    @ParameterizedTest
    @MethodSource("periods")
    void calculatesEverySupportedStartWithCalendarArithmetic(
            String value, OffsetDateTime expectedStart) {
        DashboardPeriod period = DashboardPeriod.from(value);

        assertThat(period.start(NOW, CREATED_AT)).isEqualTo(expectedStart);
        assertThat(period.value()).isEqualTo(value);
    }

    private static java.util.stream.Stream<Arguments> periods() {
        return java.util.stream.Stream.of(
                Arguments.of("4W", NOW.minusWeeks(4)),
                Arguments.of("3M", NOW.minusMonths(3)),
                Arguments.of("6M", NOW.minusMonths(6)),
                Arguments.of("1Y", NOW.minusYears(1)),
                Arguments.of("5Y", NOW.minusYears(5)),
                Arguments.of("MAX", CREATED_AT));
    }
}
