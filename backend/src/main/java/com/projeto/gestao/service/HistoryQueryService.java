package com.projeto.gestao.service;

import java.time.OffsetDateTime;
import java.util.UUID;

import com.projeto.gestao.api.exception.AuthenticationException;
import com.projeto.gestao.api.exception.HistoryQueryValidationException;
import com.projeto.gestao.domain.model.AccountStatus;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.Movement;
import com.projeto.gestao.domain.model.MovementType;
import com.projeto.gestao.repository.AccountBrokerRepository;
import com.projeto.gestao.repository.AccountRepository;
import com.projeto.gestao.repository.MovementRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryQueryService {
    public static final int PAGE_SIZE = 20;

    private final MovementRepository movements;
    private final AccountRepository accounts;
    private final AccountBrokerRepository associations;

    public HistoryQueryService(MovementRepository movements, AccountRepository accounts,
            AccountBrokerRepository associations) {
        this.movements = movements;
        this.accounts = accounts;
        this.associations = associations;
    }

    @Transactional(readOnly = true)
    public HistoryPage query(UUID accountId, int page, OffsetDateTime from, OffsetDateTime to,
            MovementType type, String ticker, UUID brokerId, Market market) {
        if (accountId == null || accounts.findByIdAndStatus(accountId, AccountStatus.ACTIVE).isEmpty()) {
            throw new AuthenticationException();
        }
        if (page < 0 || from != null && to != null && !from.isBefore(to)) {
            throw new HistoryQueryValidationException();
        }

        String normalizedTicker = ticker == null ? null : ticker.trim().toUpperCase(java.util.Locale.ROOT);
        String brokerName = null;
        if (brokerId != null) {
            brokerName = associations.findByIdAndAccountId(brokerId, accountId)
                    .map(association -> association.getBroker().getTradeName()).orElse(null);
            if (brokerName == null) {
                return empty(page);
            }
        }

        Specification<Movement> filter = accountIs(accountId);
        if (from != null) filter = filter.and(occurredAtOrAfter(from));
        if (to != null) filter = filter.and(occurredBefore(to));
        if (type != null) filter = filter.and(typeIs(type));
        if (normalizedTicker != null) filter = filter.and(tickerIs(normalizedTicker));
        if (brokerName != null) filter = filter.and(brokerIs(brokerName));
        if (market != null) filter = filter.and(marketIs(market));

        Page<Movement> result = movements.findAll(filter, PageRequest.of(page, PAGE_SIZE,
                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
        return new HistoryPage(result.getContent().stream().map(HistoryMovementView::from).toList(),
                page, PAGE_SIZE, result.getTotalElements(), result.getTotalPages());
    }

    private static HistoryPage empty(int page) {
        return new HistoryPage(java.util.List.of(), page, PAGE_SIZE, 0, 0);
    }

    private static Specification<Movement> accountIs(UUID accountId) {
        return (root, query, builder) -> builder.equal(root.get("account").get("id"), accountId);
    }

    private static Specification<Movement> occurredAtOrAfter(OffsetDateTime from) {
        return (root, query, builder) -> builder.greaterThanOrEqualTo(root.get("occurredAt"), from);
    }

    private static Specification<Movement> occurredBefore(OffsetDateTime to) {
        return (root, query, builder) -> builder.lessThan(root.get("occurredAt"), to);
    }

    private static Specification<Movement> typeIs(MovementType type) {
        return (root, query, builder) -> builder.equal(root.get("movementType"), type);
    }

    private static Specification<Movement> tickerIs(String ticker) {
        return (root, query, builder) -> builder.equal(builder.upper(root.get("ticker")), ticker);
    }

    private static Specification<Movement> brokerIs(String brokerName) {
        return (root, query, builder) -> builder.or(
                builder.equal(root.get("brokerName"), brokerName),
                builder.equal(root.get("originBrokerName"), brokerName),
                builder.equal(root.get("destinationBrokerName"), brokerName));
    }

    private static Specification<Movement> marketIs(Market market) {
        return (root, query, builder) -> builder.equal(root.get("market"), market);
    }
}
