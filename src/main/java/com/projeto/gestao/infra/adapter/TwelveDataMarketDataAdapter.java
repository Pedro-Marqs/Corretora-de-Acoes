package com.projeto.gestao.infra.adapter;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsMarketDataPort;
import com.projeto.gestao.infra.client.TwelveDataClient;
import com.projeto.gestao.infra.client.dto.TwelveDataQuoteResponse;

@Component
public class TwelveDataMarketDataAdapter implements UsMarketDataPort {
    private static final Map<String, Set<String>> US_EXCHANGES_BY_MIC = Map.of(
            "XNAS", Set.of("NASDAQ"),
            "XNGS", Set.of("NASDAQ"),
            "XNMS", Set.of("NASDAQ"),
            "XNCM", Set.of("NASDAQ"),
            "XNYS", Set.of("NYSE"),
            "XASE", Set.of("NYSE AMERICAN", "NYSE MKT", "AMEX"),
            "ARCX", Set.of("NYSE ARCA"),
            "BATS", Set.of("CBOE", "CBOE BZX", "BATS"));

    private final TwelveDataClient client;
    private final Clock clock;

    public TwelveDataMarketDataAdapter(TwelveDataClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public MarketQuote findQuote(String ticker) {
        String normalized = normalizeTicker(ticker);
        TwelveDataQuoteResponse response = client.findQuote(normalized);
        if ("error".equalsIgnoreCase(response.status())) {
            throw structuredFailure(response);
        }
        require(response.symbol(), "symbol");
        if (!normalized.equals(response.symbol().trim().toUpperCase(Locale.ROOT))) {
            throw incomplete("symbol divergente");
        }
        String name = required(response.name(), "name");
        String exchange = required(response.exchange(), "exchange").toUpperCase(Locale.ROOT);
        String mic = required(response.micCode(), "mic_code").toUpperCase(Locale.ROOT);
        if (!US_EXCHANGES_BY_MIC.getOrDefault(mic, Set.of()).contains(exchange)) {
            throw new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_INPUT, TwelveDataClient.SOURCE,
                    "Mercado não suportado pela integração Twelve Data");
        }
        if (!"USD".equals(response.currency())) {
            throw incomplete("currency");
        }
        if (response.close() == null || response.close().signum() <= 0) {
            throw incomplete("close");
        }
        if (response.timestamp() == null || response.timestamp() <= 0) {
            throw incomplete("timestamp");
        }
        return new MarketQuote(normalized, name, Market.US, Currency.USD,
                response.close().setScale(2, RoundingMode.HALF_UP),
                Instant.ofEpochSecond(response.timestamp()), clock.instant(), TwelveDataClient.SOURCE);
    }

    private static ExternalDataFailure structuredFailure(TwelveDataQuoteResponse response) {
        int code = response.code() == null ? 0 : response.code();
        ExternalDataFailure.Reason reason = switch (code) {
            case 401, 403 -> ExternalDataFailure.Reason.AUTHENTICATION;
            case 404 -> ExternalDataFailure.Reason.NOT_FOUND;
            case 429 -> ExternalDataFailure.Reason.RATE_LIMITED;
            default -> code >= 500
                    ? ExternalDataFailure.Reason.SERVER_ERROR
                    : ExternalDataFailure.Reason.INVALID_RESPONSE;
        };
        return new ExternalDataFailure(reason, TwelveDataClient.SOURCE,
                "Erro estruturado recebido da Twelve Data");
    }

    private static String normalizeTicker(String ticker) {
        String normalized = ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9.]{1,20}")) {
            throw new ExternalDataFailure(ExternalDataFailure.Reason.INVALID_INPUT, TwelveDataClient.SOURCE,
                    "Ticker norte-americano inválido");
        }
        return normalized;
    }

    private static String required(String value, String field) {
        require(value, field);
        return value.trim();
    }

    private static void require(String value, String field) {
        if (value == null || value.isBlank()) {
            throw incomplete(field);
        }
    }

    private static ExternalDataFailure incomplete(String field) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INCOMPLETE_RESPONSE,
                TwelveDataClient.SOURCE, "Campo obrigatório ausente ou inválido: " + field);
    }
}
