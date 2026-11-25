package com.projeto.gestao.infra.client;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.projeto.gestao.domain.port.ExternalDataFailure;

/** Limita retries de rate limit sem introduzir uma dependência de resiliência. */
public final class ExternalHttpRetryPolicy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalHttpRetryPolicy.class);
    static final Duration MAX_RETRY_DELAY = Duration.ofSeconds(5);
    static final Duration DEFAULT_BACKOFF = Duration.ofMillis(200);

    @FunctionalInterface
    public interface Sleeper {
        void sleep(Duration delay) throws InterruptedException;
    }

    private final Clock clock;
    private final Sleeper sleeper;

    public ExternalHttpRetryPolicy() {
        this(Clock.systemUTC(), delay -> Thread.sleep(delay.toMillis()));
    }

    ExternalHttpRetryPolicy(Clock clock, Sleeper sleeper) {
        this.clock = clock;
        this.sleeper = sleeper;
    }

    public ExternalHttpResponse execute(Supplier<ExternalHttpResponse> request, String source) {
        ExternalHttpResponse response = request.get();
        if (response.statusCode() != 429) {
            return response;
        }

        LOGGER.warn("market-data provider={} reason=RATE_LIMITED decision=RETRY status=429", source);
        Duration delay = retryDelay(response.header("Retry-After"));
        try {
            sleeper.sleep(delay);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TRANSPORT_ERROR, source,
                    "Retry externo interrompido", exception);
        }
        ExternalHttpResponse retried = request.get();
        if (retried.statusCode() == 429) {
            LOGGER.warn("market-data provider={} reason=RATE_LIMITED decision=EXHAUSTED status=429", source);
        } else {
            LOGGER.info("market-data provider={} reason=RATE_LIMITED decision=RECOVERED initialStatus=429 finalStatus={}",
                    source, retried.statusCode());
        }
        return retried;
    }

    private Duration retryDelay(String retryAfter) {
        if (retryAfter == null || retryAfter.isBlank()) {
            return DEFAULT_BACKOFF;
        }
        String value = retryAfter.trim();
        try {
            long seconds = Long.parseLong(value);
            if (seconds < 0) {
                return DEFAULT_BACKOFF;
            }
            Duration delay = Duration.ofSeconds(seconds);
            return delay.compareTo(MAX_RETRY_DELAY) <= 0 ? delay : DEFAULT_BACKOFF;
        } catch (NumberFormatException ignored) {
            try {
                Instant retryAt = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME)
                        .toInstant();
                Duration delay = Duration.between(clock.instant(), retryAt);
                if (delay.isNegative()) {
                    return Duration.ZERO;
                }
                return delay.compareTo(MAX_RETRY_DELAY) <= 0 ? delay : DEFAULT_BACKOFF;
            } catch (DateTimeParseException invalidDate) {
                return DEFAULT_BACKOFF;
            }
        }
    }
}
