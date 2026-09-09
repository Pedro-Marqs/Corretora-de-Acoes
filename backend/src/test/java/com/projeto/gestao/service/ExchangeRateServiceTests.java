package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.Instant;
import com.projeto.gestao.api.exception.ExternalDependencyException;
import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test void externalFailureReturnsLastValidEvenWhenStale() {
        when(external.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "awesome", "timeout"));
        when(cache.findExchangeRate(freshness)).thenReturn(cached("5.10", true));
        assertThat(service.resolveUsdBrl().stale()).isTrue();
    }

    @Test void absenceOfUsableRateBlocksFinancialResult() {
        when(external.currentRate()).thenThrow(new ExternalDataFailure(
                ExternalDataFailure.Reason.TIMEOUT, "awesome", "timeout"));
        assertThatThrownBy(service::resolveUsdBrl).isInstanceOf(ExternalDependencyException.class);
    }

    private static CachedExchangeRate cached(String rate, boolean stale) {
        return new CachedExchangeRate(new BigDecimal(rate), "cache", Instant.EPOCH, Instant.EPOCH, stale);
    }
}
