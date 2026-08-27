package com.projeto.gestao.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.CvmDatasetClient;
import com.projeto.gestao.infra.client.CvmDatasetParser;
import com.projeto.gestao.infra.client.ExternalHttpResponse;

class ExternalAdapterIsolationTests {

    @Test
    void cvmDependencyFailureIsNotRegulatoryRejection() {
        AtomicInteger calls = new AtomicInteger();
        CvmDatasetClient client = new CvmDatasetClient(URI.create("https://example.test/cvm.zip"),
                Duration.ofSeconds(1), (uri, timeout) -> {
                    calls.incrementAndGet();
                    return new ExternalHttpResponse(503, new byte[0]);
                });
        var adapter = new CvmRegulatoryRegistryAdapter(client, new CvmDatasetParser(),
                Duration.ofHours(24), Clock.systemUTC());

        assertThatThrownBy(() -> adapter.findByCnpj("47960950000121"))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.reason())
                                .isEqualTo(ExternalDataFailure.Reason.SERVER_ERROR));
        assertThat(calls).hasValue(1);
    }

    @Test
    void adaptersHaveNoTransactionsOrPersistenceResponsibilities() {
        List<Class<?>> adapters = List.of(
                BrasilApiCompanyRegistryAdapter.class,
                ViaCepPostalAddressAdapter.class,
                CvmRegulatoryRegistryAdapter.class);

        assertThat(adapters).allSatisfy(adapter -> {
            assertThat(adapter.isAnnotationPresent(Transactional.class)).isFalse();
            assertThat(Arrays.stream(adapter.getDeclaredMethods()))
                    .allSatisfy(method -> assertThat(method.isAnnotationPresent(Transactional.class)).isFalse());
            assertThat(Arrays.stream(adapter.getDeclaredFields()).map(Field::getType).map(Class::getPackageName))
                    .noneMatch(packageName -> packageName.contains("repository"));
        });
    }
}
