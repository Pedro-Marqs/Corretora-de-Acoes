package com.projeto.gestao.scheduler;

import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import com.projeto.gestao.service.MarketRefreshService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MarketDataScheduler {
    static final String BR_CRON = "0 */5 * * * *";
    static final String DAILY_CRON = "0 0 10 * * *";
    static final String ZONE = "America/Sao_Paulo";

    private final MarketRefreshService refresh;
    private final Clock clock;
    private final AtomicBoolean brazilRunning = new AtomicBoolean();
    private final AtomicBoolean dailyRunning = new AtomicBoolean();
    private volatile LocalDate lastDailyExecution;

    public MarketDataScheduler(MarketRefreshService refresh, Clock clock) {
        this.refresh = refresh;
        this.clock = clock;
    }

    @Scheduled(cron = BR_CRON, zone = ZONE)
    public void refreshBrazilianQuotes() {
        if (!brazilRunning.compareAndSet(false, true)) return;
        try {
            refresh.refreshBrazilianQuotes();
        } finally {
            brazilRunning.set(false);
        }
    }

    @Scheduled(cron = DAILY_CRON, zone = ZONE)
    public void refreshDailyMarketData() {
        if (!dailyRunning.compareAndSet(false, true)) return;
        try {
            LocalDate today = LocalDate.now(clock);
            if (today.equals(lastDailyExecution)) return;
            boolean completed = true;
            try {
                refresh.refreshUnitedStatesQuotes();
            } catch (RuntimeException exception) {
                completed = false;
            }
            try {
                refresh.refreshUsdBrl();
            } catch (RuntimeException exception) {
                completed = false;
            }
            if (completed) lastDailyExecution = today;
        } finally {
            dailyRunning.set(false);
        }
    }
}
