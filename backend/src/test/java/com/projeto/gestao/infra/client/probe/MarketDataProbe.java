package com.projeto.gestao.infra.client.probe;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

final class MarketDataProbe {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> US_MICS = Set.of(
            "XNAS", "XNGS", "XNMS", "XNCM", "XNYS", "XASE", "ARCX", "BATS");

    private MarketDataProbe() {
    }

    static EquityQuote mapBrapi(int status, String json) {
        return mapBrapi(status, json, "BR");
    }

    static EquityQuote mapBrapi(int status, String json, String requestedMarket) {
        if (!"BR".equalsIgnoreCase(requestedMarket)) {
            throw failure(Kind.UNSUPPORTED_MARKET, "Brapi probe accepts only market BR");
        }
        checkStatus(status);
        JsonNode root = parse(json);
        JsonNode results = root.path("results");
        if (!results.isArray() || results.isEmpty()) {
            throw failure(Kind.NOT_FOUND, "Brapi returned no quote");
        }
        JsonNode item = results.get(0);
        JsonNode data = item.path("data");
        String requested = required(item, "requestedSymbol");
        String symbol = required(item, "symbol");
        String name = optional(data, "longName");
        if (name.isBlank()) {
            name = required(data, "shortName");
        }
        return new EquityQuote(requested, symbol, name, "BR", required(data, "currency"),
                decimal(data, "regularMarketPrice"), instant(data, "regularMarketTime"));
    }

    static EquityQuote mapTwelveData(int status, String json) {
        checkStatus(status);
        JsonNode root = parse(json);
        if ("error".equalsIgnoreCase(optional(root, "status"))) {
            int code = root.path("code").asInt(0);
            Kind kind = switch (code) {
                case 401, 403 -> Kind.AUTHENTICATION;
                case 404 -> Kind.NOT_FOUND;
                case 429 -> Kind.RATE_LIMITED;
                default -> code >= 500 ? Kind.UNAVAILABLE : Kind.INCOMPLETE_RESPONSE;
            };
            throw failure(kind, optional(root, "message"));
        }
        String mic = required(root, "mic_code").toUpperCase(Locale.ROOT);
        if (!US_MICS.contains(mic)) {
            throw failure(Kind.UNSUPPORTED_MARKET, "Non-US MIC: " + mic);
        }
        return new EquityQuote(required(root, "symbol"), required(root, "symbol"),
                required(root, "name"), "US", required(root, "currency"), decimal(root, "close"),
                Instant.ofEpochSecond(longValue(root, "timestamp")));
    }

    static FxQuote mapAwesomeApi(int status, String json) {
        checkStatus(status);
        JsonNode root = parse(json);
        JsonNode quote = root.path("USDBRL");
        if (quote.isMissingNode() || quote.isNull()) {
            throw failure(status == 404 ? Kind.NOT_FOUND : Kind.INCOMPLETE_RESPONSE,
                    "AwesomeAPI returned no USD-BRL quote");
        }
        String base = required(quote, "code");
        String target = required(quote, "codein");
        if (!"USD".equals(base) || !"BRL".equals(target)) {
            throw failure(Kind.UNSUPPORTED_MARKET, "Unexpected AwesomeAPI pair: " + base + '-' + target);
        }
        return new FxQuote(base, target, decimal(quote, "bid"),
                decimal(quote, "ask"), Instant.ofEpochSecond(longValue(quote, "timestamp")),
                required(quote, "create_date"));
    }

    static BrapiPolicy sustainableBrapiPolicy(int uniqueTickers, int tickersPerRequest, int monthlyQuota,
            int activeMinutesPerDay, int activeDaysPerMonth, int providerRefreshMinutes,
            int safetyPercent) {
        if (uniqueTickers < 1 || tickersPerRequest < 1 || monthlyQuota < 1 || activeMinutesPerDay < 1
                || activeDaysPerMonth < 1 || providerRefreshMinutes < 1 || safetyPercent < 1
                || safetyPercent > 100) {
            throw new IllegalArgumentException("Policy inputs must be positive and safetyPercent <= 100");
        }
        long batches = ceilDiv(uniqueTickers, tickersPerRequest);
        long safeQuota = Math.max(1L, Math.multiplyExact((long) monthlyQuota, safetyPercent) / 100L);
        long minimumDailyConsumption = Math.multiplyExact(batches, activeDaysPerMonth);
        if (minimumDailyConsumption > safeQuota) {
            throw new IllegalStateException("Brapi quota cannot support even one cycle per active day");
        }
        long maxCycles = Math.max(1L, safeQuota / batches);
        long activeMinutes = Math.multiplyExact((long) activeMinutesPerDay, activeDaysPerMonth);
        long quotaInterval = ceilDiv(activeMinutes, maxCycles);
        long interval = Math.max(providerRefreshMinutes, quotaInterval);
        long projected = projectedBrapiRequests(activeMinutesPerDay, activeDaysPerMonth, batches, interval);
        while (projected > safeQuota) {
            interval++;
            projected = projectedBrapiRequests(activeMinutesPerDay, activeDaysPerMonth, batches, interval);
        }
        return new BrapiPolicy(interval, batches, projected, safeQuota);
    }

    static int twelveDataDailyCredits(int uniqueSymbols, int creditsPerSymbol) {
        if (uniqueSymbols < 1 || creditsPerSymbol < 1) {
            throw new IllegalArgumentException("Credit inputs must be positive");
        }
        return Math.multiplyExact(uniqueSymbols, creditsPerSymbol);
    }

    private static long projectedBrapiRequests(int activeMinutesPerDay, int activeDaysPerMonth,
            long batches, long interval) {
        return Math.multiplyExact(
                Math.multiplyExact(ceilDiv(activeMinutesPerDay, interval), activeDaysPerMonth), batches);
    }

    private static long ceilDiv(long dividend, long divisor) {
        return dividend / divisor + (dividend % divisor == 0 ? 0 : 1);
    }

    private static void checkStatus(int status) {
        if (status == 404) {
            throw failure(Kind.NOT_FOUND, "Instrument not found");
        }
        if (status == 429) {
            throw failure(Kind.RATE_LIMITED, "Provider quota exceeded");
        }
        if (status < 200 || status >= 300) {
            throw failure(Kind.UNAVAILABLE, "Provider unavailable: HTTP " + status);
        }
    }

    private static JsonNode parse(String json) {
        try {
            JsonNode node = JSON.readTree(json);
            if (node == null || node.isNull()) {
                throw failure(Kind.INCOMPLETE_RESPONSE, "Empty JSON response");
            }
            return node;
        } catch (IOException exception) {
            throw failure(Kind.INCOMPLETE_RESPONSE, "Invalid JSON response");
        }
    }

    private static String required(JsonNode node, String field) {
        String value = optional(node, field);
        if (value.isBlank()) {
            throw failure(Kind.INCOMPLETE_RESPONSE, "Missing field: " + field);
        }
        return value;
    }

    private static String optional(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "" : value.asText().trim();
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        try {
            return new BigDecimal(required(node, field));
        } catch (NumberFormatException exception) {
            throw failure(Kind.INCOMPLETE_RESPONSE, "Invalid decimal field: " + field);
        }
    }

    private static long longValue(JsonNode node, String field) {
        try {
            return Long.parseLong(required(node, field));
        } catch (NumberFormatException exception) {
            throw failure(Kind.INCOMPLETE_RESPONSE, "Invalid integer field: " + field);
        }
    }

    private static Instant instant(JsonNode node, String field) {
        try {
            return Instant.parse(required(node, field));
        } catch (RuntimeException exception) {
            throw failure(Kind.INCOMPLETE_RESPONSE, "Invalid timestamp field: " + field);
        }
    }

    private static ProbeFailure failure(Kind kind, String message) {
        return new ProbeFailure(kind, message == null || message.isBlank() ? kind.name() : message);
    }

    record EquityQuote(String requestedSymbol, String symbol, String name, String market, String currency,
            BigDecimal price, Instant marketTime) {
    }

    record FxQuote(String base, String quote, BigDecimal bid, BigDecimal ask, Instant marketTime,
            String localMarketTime) {
    }

    record BrapiPolicy(long intervalMinutes, long requestsPerCycle, long projectedMonthlyRequests,
            long safeMonthlyBudget) {
    }

    enum Kind {
        NOT_FOUND, UNSUPPORTED_MARKET, INCOMPLETE_RESPONSE, AUTHENTICATION, RATE_LIMITED, UNAVAILABLE
    }

    static final class ProbeFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private final Kind kind;

        ProbeFailure(Kind kind, String message) {
            super(message);
            this.kind = kind;
        }

        Kind kind() {
            return kind;
        }
    }
}
