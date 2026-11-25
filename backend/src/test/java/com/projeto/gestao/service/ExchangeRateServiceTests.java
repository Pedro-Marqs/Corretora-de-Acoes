package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class ExchangeRateServiceTests {
    UsdBrlExchangeRatePort external = mock(UsdBrlExchangeRatePort.class);
    MarketCachePersistenceService cache = mock(MarketCachePersistenceService.class);
    MarketDataFreshness freshness = mock(MarketDataFreshness.class);
    ExchangeRateService service;

    @BeforeEach void setUp() { service = new ExchangeRateService(external, cache, freshness); }

    @Test void persistsValidExternalRate() {
        UsdBrlRate received = new UsdBrlRate(new BigDecimal("5.20"), Instant.EPOCH, Instant.EPOCH, "awesome");
        CachedExchangeRate stored = cached("5.20", false);
        when(external.currentRate()).thenReturn(received);
        when(cache.store(received, freshness)).thenReturn(stored);
        assertThat(service.resolveUsdBrl()).isSameAs(stored);
    }

    @Test void freshCacheIsReturnedWithoutCallingExternalProvider() {
        CachedExchangeRate fresh = cached("5.20", false);
        when(cache.findExchangeRate(freshness)).thenReturn(fresh);

        assertThat(service.resolveUsdBrl()).isSameAs(fresh);
        verifyNoInteractions(external);
        verify(cache).findExchangeRate(freshness);
    }

    @Test void externalFailureReturnsLastValidEvenWhenStale() {
        when(external.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "awesome", "timeout"));
        when(cache.findExchangeRate(freshness)).thenReturn(cached("5.10", true));
        assertThat(service.resolveUsdBrl().stale()).isTrue();
    }

    @Test void rateLimitReturnsStaleCacheWithoutReplacingIt() {
        when(external.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.RATE_LIMITED, "AwesomeAPI", "rate limited", 429));
        CachedExchangeRate fallback = cached("5.10", true);
        when(cache.findExchangeRate(freshness)).thenReturn(fallback);

        assertThat(service.resolveUsdBrl()).isSameAs(fallback);
        verify(cache, never()).store(any(UsdBrlRate.class), eq(freshness));
    }

    @Test void absenceOfUsableRateBlocksFinancialResult() {
        when(external.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "awesome", "timeout"));
        assertThatThrownBy(service::resolveUsdBrl).isInstanceOf(ExternalDependencyException.class);
    }

    @Test void concurrentResolutionsShareOneExternalRefresh() throws Exception {
        CachedExchangeRate stale = cached("5.10", true);
        CachedExchangeRate fresh = cached("5.20", false);
        AtomicInteger externalCalls = new AtomicInteger();
        var refreshed = new java.util.concurrent.atomic.AtomicBoolean();
        CountDownLatch secondAdmission = new CountDownLatch(1);
        CountDownLatch externalStarted = new CountDownLatch(1);
        CountDownLatch releaseExternal = new CountDownLatch(1);
        ExchangeRateService concurrentService = serviceWithSecondAdmission(secondAdmission);
        when(cache.findExchangeRate(freshness)).thenAnswer(invocation -> refreshed.get() ? fresh : stale);
        when(external.currentRate()).thenAnswer(invocation -> {
            externalCalls.incrementAndGet();
            externalStarted.countDown();
            releaseExternal.await(2, TimeUnit.SECONDS);
            return new UsdBrlRate(new BigDecimal("5.20"), Instant.EPOCH, Instant.EPOCH, "awesome");
        });
        when(cache.store(any(UsdBrlRate.class), eq(freshness))).thenAnswer(invocation -> {
            refreshed.set(true);
            return fresh;
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(concurrentService::resolveUsdBrl);
            assertThat(externalStarted.await(1, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(concurrentService::resolveUsdBrl);
            assertThat(secondAdmission.await(1, TimeUnit.SECONDS)).isTrue();
            releaseExternal.countDown();

            assertThat(first.get(2, TimeUnit.SECONDS)).isEqualTo(fresh);
            assertThat(second.get(2, TimeUnit.SECONDS)).isEqualTo(fresh);
            assertThat(externalCalls).hasValue(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test void concurrentFailureIsSharedAndLaterResolutionCanRetry() throws Exception {
        AtomicInteger externalCalls = new AtomicInteger();
        CountDownLatch externalStarted = new CountDownLatch(1);
        CountDownLatch releaseExternal = new CountDownLatch(1);
        CachedExchangeRate refreshed = cached("5.20", false);
        CountDownLatch secondAdmission = new CountDownLatch(1);
        ExchangeRateService concurrentService = serviceWithSecondAdmission(secondAdmission);
        when(cache.findExchangeRate(freshness)).thenReturn(null);
        when(external.currentRate()).thenAnswer(invocation -> {
            if (externalCalls.incrementAndGet() == 1) {
                externalStarted.countDown();
                releaseExternal.await(2, TimeUnit.SECONDS);
                throw new ExternalDataFailure(ExternalDataFailure.Reason.TIMEOUT,
                        "AwesomeAPI", "timeout");
            }
            return new UsdBrlRate(new BigDecimal("5.20"), Instant.EPOCH, Instant.EPOCH, "awesome");
        });
        when(cache.store(any(UsdBrlRate.class), eq(freshness))).thenReturn(refreshed);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(concurrentService::resolveUsdBrl);
            assertThat(externalStarted.await(1, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(concurrentService::resolveUsdBrl);
            assertThat(secondAdmission.await(1, TimeUnit.SECONDS)).isTrue();
            releaseExternal.countDown();

            assertFailed(first);
            assertFailed(second);
            assertThat(externalCalls).hasValue(1);

            assertThat(service.resolveUsdBrl()).isSameAs(refreshed);
            assertThat(externalCalls).hasValue(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test void concurrentFailureCannotAdmitSecondRefreshAfterInitialCacheReadRace() throws Exception {
        CachedExchangeRate firstStale = mock(CachedExchangeRate.class);
        CachedExchangeRate secondStale = mock(CachedExchangeRate.class);
        CachedExchangeRate ordinaryStale = cached("5.10", true);
        AtomicInteger cacheReads = new AtomicInteger();
        AtomicInteger externalCalls = new AtomicInteger();
        AtomicBoolean oldRaceReached = new AtomicBoolean();
        CompletableFuture<Void> secondAdmissionObserved = new CompletableFuture<>();
        AtomicBoolean firstRegistered = new AtomicBoolean();
        CountDownLatch firstInitialRead = new CountDownLatch(1);
        CountDownLatch releaseFirstInitialRead = new CountDownLatch(1);
        CountDownLatch releaseSecondInitialRead = new CountDownLatch(1);
        CountDownLatch externalStarted = new CountDownLatch(1);
        CountDownLatch releaseExternal = new CountDownLatch(1);
        ConcurrentMap<String, CompletableFuture<CachedExchangeRate>> refreshes = trackingRefreshes(
                secondAdmissionObserved, firstRegistered);
        ExchangeRateService raceService = new ExchangeRateService(external, cache, freshness, refreshes);

        when(firstStale.stale()).thenAnswer(invocation -> {
            firstInitialRead.countDown();
            releaseFirstInitialRead.await(2, TimeUnit.SECONDS);
            return true;
        });
        when(secondStale.stale()).thenAnswer(invocation -> {
            oldRaceReached.set(true);
            secondAdmissionObserved.complete(null);
            releaseSecondInitialRead.await(2, TimeUnit.SECONDS);
            return true;
        });
        when(cache.findExchangeRate(freshness)).thenAnswer(invocation -> {
            int read = cacheReads.incrementAndGet();
            if (read == 1) {
                return firstStale;
            }
            if (read == 2 && !firstRegistered.get()) {
                return secondStale;
            }
            if (read == 2) {
                return ordinaryStale;
            }
            return null;
        });
        when(external.currentRate()).thenAnswer(invocation -> {
            externalCalls.incrementAndGet();
            externalStarted.countDown();
            releaseExternal.await(2, TimeUnit.SECONDS);
            throw new ExternalDataFailure(ExternalDataFailure.Reason.TIMEOUT,
                    "AwesomeAPI", "timeout");
        });

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<CachedExchangeRate> first = executor.submit(raceService::resolveUsdBrl);
            assertThat(firstInitialRead.await(1, TimeUnit.SECONDS)).isTrue();
            Future<CachedExchangeRate> second = executor.submit(raceService::resolveUsdBrl);
            secondAdmissionObserved.get(1, TimeUnit.SECONDS);
            releaseFirstInitialRead.countDown();
            assertThat(externalStarted.await(1, TimeUnit.SECONDS)).isTrue();
            releaseExternal.countDown();

            assertFailed(first);
            if (oldRaceReached.get()) {
                releaseSecondInitialRead.countDown();
            }
            assertFailed(second);
            assertThat(externalCalls).hasValue(1);
        } finally {
            releaseFirstInitialRead.countDown();
            releaseSecondInitialRead.countDown();
            releaseExternal.countDown();
            executor.shutdownNow();
        }
    }

    @Test void failureLogsContainControlledProviderFieldsWithoutCredentials() {
        String secret = "should-never-be-logged";
        Logger logger = (Logger) LoggerFactory.getLogger(ExchangeRateService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        Level previousLevel = logger.getLevel();
        logger.addAppender(appender);
        logger.setLevel(Level.TRACE);
        try {
            when(external.currentRate()).thenThrow(new ExternalDataFailure(
                    ExternalDataFailure.Reason.AUTHENTICATION, "AwesomeAPI",
                    "provider detail " + secret, 401));

            assertThatThrownBy(service::resolveUsdBrl).isInstanceOf(ExternalDependencyException.class);

            String messages = appender.list.stream()
                    .map(ILoggingEvent::getFormattedMessage)
                    .reduce("", (left, right) -> left + "\n" + right);
            assertThat(messages).contains("provider=AwesomeAPI")
                    .contains("reason=AUTHENTICATION")
                    .contains("status=401")
                    .doesNotContain(secret);
        } finally {
            logger.detachAppender(appender);
            logger.setLevel(previousLevel);
            appender.stop();
        }
    }

    private static CachedExchangeRate cached(String rate, boolean stale) {
        return new CachedExchangeRate(new BigDecimal(rate), "cache", Instant.EPOCH, Instant.EPOCH, stale);
    }

    private static void assertFailed(Future<?> future) throws Exception {
        try {
            future.get(2, TimeUnit.SECONDS);
            throw new AssertionError("A resolução deveria falhar");
        } catch (ExecutionException exception) {
            assertThat(exception.getCause()).isInstanceOf(ExternalDependencyException.class);
        }
    }

    private ExchangeRateService serviceWithSecondAdmission(CountDownLatch secondAdmission) {
        return new ExchangeRateService(external, cache, freshness, trackingRefreshes(
                new CompletableFuture<>() {
                    @Override
                    public boolean complete(Void value) {
                        secondAdmission.countDown();
                        return super.complete(value);
                    }
                }, new AtomicBoolean()));
    }

    private static ConcurrentMap<String, CompletableFuture<CachedExchangeRate>> trackingRefreshes(
            CompletableFuture<Void> secondAdmissionObserved, AtomicBoolean firstRegistered) {
        return new ConcurrentHashMap<>() {
            @Override
            public CompletableFuture<CachedExchangeRate> putIfAbsent(
                    String key, CompletableFuture<CachedExchangeRate> value) {
                CompletableFuture<CachedExchangeRate> existing = super.putIfAbsent(key, value);
                if (existing != null) {
                    secondAdmissionObserved.complete(null);
                } else {
                    firstRegistered.set(true);
                }
                return existing;
            }
        };
    }
}
