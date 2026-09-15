## Context

See `proposal.md` and the modified `ativos-cotacoes-cambio` and `historico-registro-patrimonial` specifications. T23 already provides the market catalog, cached quote entities, repositories, and external adapters. The scheduler must use the existing application services without introducing financial side effects.

## Goals / Non-Goals

**Goals:**

- Expose deterministic scheduled entry points for Brazilian, US, and USD/BRL refreshes.
- Use the configured Brasília clock for daily eligibility and make scheduling behavior testable.
- Coordinate same-cycle execution so concurrent triggers cannot overlap.
- Keep failed refreshes from replacing valid cached values and avoid historical writes.

**Non-Goals:**

- Adding HTTP endpoints or user-triggered refresh.
- Reworking the T23 catalog or external adapter contracts.
- Creating portfolio snapshots as part of a quote refresh.

## Decisions

- Keep scheduling in a dedicated `scheduler` package and delegate refresh work to the existing quote/currency services. This preserves the controller/service boundary and avoids duplicating cache policy.
- Use Spring scheduling expressions for the five-minute and 10:00 Brasília triggers, while the service also checks the controlled clock and the last daily execution date. The date guard protects against duplicate invocations and makes direct unit tests deterministic.
- Use an in-process per-cycle lock for overlap prevention. A database lock or distributed scheduler would add infrastructure that is outside the single-process architecture.
- Refresh only symbols discoverable through existing position data. A failed external result is handled as a non-success by the service, leaving the prior cache row untouched and updating only its stale state when supported by the T23 model.
- Keep scheduler methods free of transaction boundaries that could create movements or snapshots; quote cache persistence remains isolated from financial history.

## Risks / Trade-offs

- [Process restart] An in-memory overlap lock does not coordinate across processes, but the application is intentionally a single Spring Boot process.
- [Missed daily trigger] A stopped process can miss 10:00; the date guard avoids duplicate runs but does not backfill missed executions, which is acceptable for the first local-only version.
- [External outage] Cached values may become stale; preserving them and exposing stale status prevents fabricated prices and keeps later operations subject to the existing validity rules.
