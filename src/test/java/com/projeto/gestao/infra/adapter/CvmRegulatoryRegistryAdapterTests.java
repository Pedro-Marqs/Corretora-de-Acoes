package com.projeto.gestao.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Test;

import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.CvmDatasetClient;
import com.projeto.gestao.infra.client.CvmDatasetParser;
import com.projeto.gestao.infra.client.ExternalHttpResponse;
import com.projeto.gestao.infra.client.ExternalHttpTransport;

class CvmRegulatoryRegistryAdapterTests {

    private static final String XP_CNPJ = "02332886000104";
    private static final Charset CVM_CHARSET = Charset.forName("windows-1252");

    @Test
    void findsKnownCtvmAndReturnsNegativeForAbsentCompany() throws IOException {
        var adapter = adapter(successTransport(fixtureZip()), new MutableClock(Instant.EPOCH), Duration.ofHours(24));

        var xp = adapter.findByCnpj("02.332.886/0001-04");
        var absent = adapter.findByCnpj("47.960.950/0001-21");

        assertThat(xp.registered()).isTrue();
        assertThat(xp.activeCtvm()).isTrue();
        assertThat(xp.participants()).hasSize(3);
        assertThat(absent.registered()).isFalse();
        assertThat(absent.activeCtvm()).isFalse();
    }

    @Test
    void concurrentInitialRequestsPerformSingleDownload() throws Exception {
        AtomicInteger downloads = new AtomicInteger();
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        byte[] zip = fixtureZip();
        ExternalHttpTransport transport = (uri, timeout) -> {
            downloads.incrementAndGet();
            entered.countDown();
            await(release);
            return new ExternalHttpResponse(200, zip);
        };
        var adapter = adapter(transport, new MutableClock(Instant.EPOCH), Duration.ofHours(24));

        var executor = Executors.newFixedThreadPool(6);
        try {
            List<java.util.concurrent.Future<Boolean>> results = new ArrayList<>();
            for (int index = 0; index < 6; index++) {
                results.add(executor.submit(() -> adapter.findByCnpj(XP_CNPJ).activeCtvm()));
            }
            assertThat(entered.await(2, TimeUnit.SECONDS)).isTrue();
            release.countDown();
            for (var result : results) {
                assertThat(result.get(2, TimeUnit.SECONDS)).isTrue();
            }
        } finally {
            executor.shutdownNow();
        }
        assertThat(downloads).hasValue(1);
    }

    @Test
    void failedRefreshDoesNotPublishPartialSnapshot() throws IOException {
        AtomicInteger downloads = new AtomicInteger();
        byte[] valid = fixtureZip();
        ExternalHttpTransport transport = (uri, timeout) -> downloads.getAndIncrement() == 0
                ? new ExternalHttpResponse(200, valid)
                : new ExternalHttpResponse(200, new byte[] {1, 2, 3});
        MutableClock clock = new MutableClock(Instant.EPOCH);
        var adapter = adapter(transport, clock, Duration.ofHours(1));

        assertThat(adapter.findByCnpj(XP_CNPJ).activeCtvm()).isTrue();
        clock.advance(Duration.ofHours(2));
        assertThatThrownBy(() -> adapter.findByCnpj(XP_CNPJ))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.reason())
                                .isEqualTo(ExternalDataFailure.Reason.INVALID_RESPONSE));

        clock.set(Instant.EPOCH.plus(Duration.ofMinutes(30)));
        assertThat(adapter.findByCnpj(XP_CNPJ).activeCtvm()).isTrue();
        assertThat(downloads).hasValue(2);
    }

    @Test
    void invalidZipAndMissingColumnsAreRejected() throws IOException {
        CvmDatasetParser parser = new CvmDatasetParser();
        assertThatThrownBy(() -> parser.parse(new byte[0]))
                .isInstanceOf(ExternalDataFailure.class);
        assertThatThrownBy(() -> parser.parse(zip("CNPJ;SIT\n02332886000104;ATIVO\n")))
                .isInstanceOfSatisfying(ExternalDataFailure.class,
                        failure -> assertThat(failure.reason())
                                .isEqualTo(ExternalDataFailure.Reason.INVALID_RESPONSE));
    }

    private static CvmRegulatoryRegistryAdapter adapter(ExternalHttpTransport transport, Clock clock,
            Duration validity) {
        CvmDatasetClient client = new CvmDatasetClient(URI.create("https://example.test/cvm.zip"),
                Duration.ofSeconds(1), transport);
        return new CvmRegulatoryRegistryAdapter(client, new CvmDatasetParser(), validity, clock);
    }

    private static ExternalHttpTransport successTransport(byte[] zip) {
        return (uri, timeout) -> new ExternalHttpResponse(200, zip);
    }

    private static byte[] fixtureZip() throws IOException {
        String csv = Files.readString(Path.of("src/test/resources/t17/cvm-participants.csv"), StandardCharsets.UTF_8);
        return zip(csv);
    }

    private static byte[] zip(String csv) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, CVM_CHARSET)) {
            zip.putNextEntry(new ZipEntry("cad_intermed.csv"));
            zip.write(csv.getBytes(CVM_CHARSET));
            zip.closeEntry();
        }
        return output.toByteArray();
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> instant;

        private MutableClock(Instant instant) {
            this.instant = new AtomicReference<>(instant);
        }

        void advance(Duration duration) {
            instant.updateAndGet(value -> value.plus(duration));
        }

        void set(Instant value) {
            instant.set(value);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant.get();
        }
    }
}
