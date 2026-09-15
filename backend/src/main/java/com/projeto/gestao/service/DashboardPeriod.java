package com.projeto.gestao.service;

import java.time.OffsetDateTime;

public enum DashboardPeriod {
    FOUR_WEEKS("4W") {
        @Override OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt) {
            return now.minusWeeks(4);
        }
    },
    THREE_MONTHS("3M") {
        @Override OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt) {
            return now.minusMonths(3);
        }
    },
    SIX_MONTHS("6M") {
        @Override OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt) {
            return now.minusMonths(6);
        }
    },
    ONE_YEAR("1Y") {
        @Override OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt) {
            return now.minusYears(1);
        }
    },
    FIVE_YEARS("5Y") {
        @Override OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt) {
            return now.minusYears(5);
        }
    },
    MAXIMUM("MAX") {
        @Override OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt) {
            return accountCreatedAt;
        }
    };

    private final String value;

    DashboardPeriod(String value) {
        this.value = value;
    }

    abstract OffsetDateTime start(OffsetDateTime now, OffsetDateTime accountCreatedAt);

    public String value() {
        return value;
    }

    public static DashboardPeriod from(String value) {
        for (DashboardPeriod period : values()) {
            if (period.value.equals(value)) {
                return period;
            }
        }
        throw new IllegalArgumentException("Unsupported dashboard period");
    }
}
