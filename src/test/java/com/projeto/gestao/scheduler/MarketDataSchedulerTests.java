package com.projeto.gestao.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.projeto.gestao.service.MarketRefreshService;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class MarketDataSchedulerTests {
    private static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");
    private final MarketRefreshService refresh = org.mockito.Mockito.mock(MarketRefreshService.class);

    @Test
    void declaresExpectedSchedulesInBrasilia() throws Exception {
        Scheduled brazil = schedule("refreshBrazilianQuotes");
        Scheduled daily = schedule("refreshDailyMarketData");

        assertThat(brazil.cron()).isEqualTo("0 */5 * * * *");
        assertThat(brazil.zone()).isEqualTo("America/Sao_Paulo");
        assertThat(daily.cron()).isEqualTo("0 0 10 * * *");
        assertThat(daily.zone()).isEqualTo("America/Sao_Paulo");
    }

    @Test
    void dailyCycleRunsOnlyOncePerBrasiliaDate() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-02T13:00:00Z"), BRASILIA);
        MarketDataScheduler scheduler = new MarketDataScheduler(refresh, clock);

        scheduler.refreshDailyMarketData();
        scheduler.refreshDailyMarketData();

        verify(refresh).refreshUnitedStatesQuotes();
        verify(refresh).refreshUsdBrl();
    }

    @Test
    void brazilianCycleDoesNotOverlap() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        org.mockito.Mockito.doAnswer(invocation -> {
            entered.countDown();
            assertThat(release.await(5, TimeUnit.SECONDS)).isTrue();
            return null;
        }).when(refresh).refreshBrazilianQuotes();
        MarketDataScheduler scheduler = new MarketDataScheduler(refresh,
                Clock.fixed(Instant.parse("2026-09-02T13:00:00Z"), BRASILIA));
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(scheduler::refreshBrazilianQuotes);
            assertThat(entered.await(5, TimeUnit.SECONDS)).isTrue();
            scheduler.refreshBrazilianQuotes();
            release.countDown();
            executor.shutdown();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            release.countDown();
            executor.shutdownNow();
        }

        verify(refresh, times(1)).refreshBrazilianQuotes();
    }

    @Test
    void dailyCycleRunsExchangeRateEvenWhenUsRefreshFails() {
        doThrow(new IllegalStateException("temporary failure")).when(refresh).refreshUnitedStatesQuotes();
        MarketDataScheduler scheduler = new MarketDataScheduler(refresh,
                Clock.fixed(Instant.parse("2026-09-02T13:00:00Z"), BRASILIA));

        scheduler.refreshDailyMarketData();

        verify(refresh).refreshUnitedStatesQuotes();
        verify(refresh).refreshUsdBrl();
    }

    private static Scheduled schedule(String methodName) throws Exception {
        Method method = MarketDataScheduler.class.getMethod(methodName);
        return method.getAnnotation(Scheduled.class);
    }
}
