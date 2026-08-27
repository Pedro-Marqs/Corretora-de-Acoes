package com.projeto.gestao.service;

import static org.assertj.core.api.Assertions.assertThat;
import java.lang.reflect.Method;
import java.util.Arrays;
import com.projeto.gestao.domain.port.UsMarketDataPort;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class MarketArchitectureTests {
    @Test void orchestrationServicesDoNotOpenTransactionsAroundExternalCalls() {
        assertThat(AssetCatalogService.class.getAnnotation(Transactional.class)).isNull();
        assertThat(ExchangeRateService.class.getAnnotation(Transactional.class)).isNull();
        assertThat(Arrays.stream(AssetCatalogService.class.getDeclaredMethods())
                .map(Method::getAnnotations)
                .flatMap(Arrays::stream)
                .noneMatch(annotation -> annotation.annotationType() == Transactional.class)).isTrue();
    }

    @Test void normalUsConsultationHasNoTwelveDataDependency() {
        assertThat(Arrays.stream(AssetCatalogService.class.getDeclaredFields())
                .noneMatch(field -> field.getType() == UsMarketDataPort.class)).isTrue();
    }
}
