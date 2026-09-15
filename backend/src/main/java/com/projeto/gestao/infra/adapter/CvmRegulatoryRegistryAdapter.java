package com.projeto.gestao.infra.adapter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import com.projeto.gestao.config.ExternalIntegrationProperties;
import com.projeto.gestao.domain.model.RegulatoryParticipant;
import com.projeto.gestao.domain.model.RegulatoryRegistration;
import com.projeto.gestao.domain.port.RegulatoryRegistryPort;
import com.projeto.gestao.infra.client.CvmDatasetClient;
import com.projeto.gestao.infra.client.CvmDatasetParser;
import com.projeto.gestao.infra.client.dto.CvmParticipantRow;

@Component
public class CvmRegulatoryRegistryAdapter implements RegulatoryRegistryPort {

    static final String CTVM_CATEGORY = "CORRETORAS";
    static final String ACTIVE_STATUS = "EM FUNCIONAMENTO NORMAL";

    private final CvmDatasetClient client;
    private final CvmDatasetParser parser;
    private final Duration validity;
    private final Clock clock;
    private final AtomicReference<Snapshot> snapshot = new AtomicReference<>();
    private final Object refreshMonitor = new Object();

    @Autowired
    public CvmRegulatoryRegistryAdapter(CvmDatasetClient client, CvmDatasetParser parser,
            ExternalIntegrationProperties properties, Clock clock) {
        this(client, parser, properties.cvm().snapshotValidity(), clock);
    }

    public CvmRegulatoryRegistryAdapter(CvmDatasetClient client, CvmDatasetParser parser,
            Duration validity, Clock clock) {
        this.client = client;
        this.parser = parser;
        this.validity = validity;
        this.clock = clock;
    }

    @Override
    public RegulatoryRegistration findByCnpj(String cnpj) {
        String normalized = ExternalIdentifierNormalizer.cnpj(cnpj, CvmDatasetClient.SOURCE);
        List<CvmParticipantRow> rows = currentSnapshot().participants().getOrDefault(normalized, List.of());
        List<RegulatoryParticipant> participants = rows.stream()
                .map(row -> new RegulatoryParticipant(row.category(), row.legalName(), row.tradeName(),
                        row.status(), row.cvmCode()))
                .toList();
        boolean activeCtvm = rows.stream().anyMatch(row -> CTVM_CATEGORY.equals(row.category())
                && ACTIVE_STATUS.equals(row.status()));
        return new RegulatoryRegistration(normalized, !rows.isEmpty(), activeCtvm, participants);
    }

    private Snapshot currentSnapshot() {
        Instant now = clock.instant();
        Snapshot current = snapshot.get();
        if (isValid(current, now)) {
            return current;
        }
        synchronized (refreshMonitor) {
            now = clock.instant();
            current = snapshot.get();
            if (isValid(current, now)) {
                return current;
            }
            Map<String, List<CvmParticipantRow>> parsed = parser.parse(client.download());
            Snapshot replacement = new Snapshot(now, parsed);
            snapshot.set(replacement);
            return replacement;
        }
    }

    private boolean isValid(Snapshot value, Instant now) {
        return value != null && now.isBefore(value.loadedAt().plus(validity));
    }

    record Snapshot(Instant loadedAt, Map<String, List<CvmParticipantRow>> participants) {
        Snapshot {
            participants = Map.copyOf(participants);
        }
    }
}
