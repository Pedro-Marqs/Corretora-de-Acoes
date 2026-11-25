package com.projeto.gestao.infra.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ExternalHttpRetryPolicyTests {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-24T18:00:00Z"), ZoneOffset.UTC);

    @Test
    void honorsRetryAfterAndRetriesAtMostOnce() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            if (calls.getAndIncrement() == 0) {
                return response(429, Map.of("Retry-After", "2"));
            }
            return response(200, Map.of());
        }, "AwesomeAPI");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(Duration.ofSeconds(2));
    }

    @Test
    void logsRateLimitRecoveryWithoutProviderCredentials() {
        AtomicInteger calls = new AtomicInteger();
        Logger logger = (Logger) LoggerFactory.getLogger(ExternalHttpRetryPolicy.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Level previousLevel = logger.getLevel();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, ignored -> { });
            policy.execute(() -> calls.getAndIncrement() == 0
                    ? response(429, Map.of("Retry-After", "0"))
                    : response(200, Map.of()), "Twelve Data");

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(messages).contains("provider=Twelve Data")
                    .contains("reason=RATE_LIMITED")
                    .contains("decision=RETRY")
                    .contains("decision=RECOVERED")
                    .doesNotContain("Retry-After")
                    .doesNotContain("apikey");
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    @Test
    void usesShortBoundedBackoffWhenRetryAfterIsMissingOrInvalid() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            if (calls.getAndIncrement() == 0) {
                return response(429, Map.of("Retry-After", "invalid"));
            }
            return response(200, Map.of());
        }, "Twelve Data");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(ExternalHttpRetryPolicy.DEFAULT_BACKOFF);
    }

    @Test
    void retriesWithBoundedBackoffWhenProviderRequestsDelayBeyondOperationalLimit() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            calls.incrementAndGet();
            return response(429, Map.of("Retry-After", "6"));
        }, "AwesomeAPI");

        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(ExternalHttpRetryPolicy.DEFAULT_BACKOFF);
    }

    @Test
    void honorsRfc1123RetryAfterWithinLimit() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            if (calls.getAndIncrement() == 0) {
                return response(429, Map.of("Retry-After", "Tue, 24 Nov 2026 18:00:02 GMT"));
            }
            return response(200, Map.of());
        }, "AwesomeAPI");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(Duration.ofSeconds(2));
    }

    @Test
    void treatsPastRfc1123RetryAfterAsImmediateRetry() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            if (calls.getAndIncrement() == 0) {
                return response(429, Map.of("Retry-After", "Tue, 24 Nov 2026 17:59:59 GMT"));
            }
            return response(200, Map.of());
        }, "Twelve Data");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(Duration.ZERO);
    }

    @Test
    void usesBoundedBackoffForNegativeRetryAfter() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            if (calls.getAndIncrement() == 0) {
                return response(429, Map.of("Retry-After", "-1"));
            }
            return response(200, Map.of());
        }, "AwesomeAPI");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(ExternalHttpRetryPolicy.DEFAULT_BACKOFF);
    }

    @Test
    void usesDefaultBackoffWhenRetryAfterIsMissingOrBlank() {
        for (String retryAfter : new String[] {null, "   "}) {
            AtomicInteger calls = new AtomicInteger();
            ArrayList<Duration> delays = new ArrayList<>();
            ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

            ExternalHttpResponse response = policy.execute(() -> {
                if (calls.getAndIncrement() == 0) {
                    return retryAfter == null
                            ? response(429, Map.of())
                            : response(429, Map.of("Retry-After", retryAfter));
                }
                return response(200, Map.of());
            }, "AwesomeAPI");

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(calls).hasValue(2);
            assertThat(delays).containsExactly(ExternalHttpRetryPolicy.DEFAULT_BACKOFF);
        }
    }

    @Test
    void boundsRfc1123RetryAfterBeyondOperationalLimit() {
        AtomicInteger calls = new AtomicInteger();
        ArrayList<Duration> delays = new ArrayList<>();
        ExternalHttpRetryPolicy policy = new ExternalHttpRetryPolicy(CLOCK, delays::add);

        ExternalHttpResponse response = policy.execute(() -> {
            calls.incrementAndGet();
            return response(429, Map.of("Retry-After", "Tue, 24 Nov 2026 18:00:06 GMT"));
        }, "Twelve Data");

        assertThat(response.statusCode()).isEqualTo(429);
        assertThat(calls).hasValue(2);
        assertThat(delays).containsExactly(ExternalHttpRetryPolicy.DEFAULT_BACKOFF);
    }

    private static ExternalHttpResponse response(int status, Map<String, String> headers) {
        return new ExternalHttpResponse(status, "{}".getBytes(StandardCharsets.UTF_8), headers);
    }
}
