package com.projeto.gestao.infra.adapter;

import java.math.RoundingMode;
import java.time.Clock;
import java.util.Locale;

import org.springframework.stereotype.Component;

import com.projeto.gestao.domain.model.Currency;
import com.projeto.gestao.domain.model.Market;
import com.projeto.gestao.domain.model.MarketQuote;
import com.projeto.gestao.domain.port.BrazilMarketDataPort;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.infra.client.BrapiClient;
import com.projeto.gestao.infra.client.dto.BrapiQuoteResponse;

@Component
public class BrapiMarketDataAdapter implements BrazilMarketDataPort {
    private final BrapiClient client;
    private final Clock clock;

    public BrapiMarketDataAdapter(BrapiClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public MarketQuote findQuote(String ticker) {
        String normalized = normalizeTicker(ticker);
        BrapiQuoteResponse response = client.findQuote(normalized);
        if (response.results() == null || response.results().isEmpty()) {
            throw failure(ExternalDataFailure.Reason.NOT_FOUND, "Ativo brasileiro não encontrado");
        }
        BrapiQuoteResponse.Result result = response.results().get(0);
        require(result.requestedSymbol(), "requestedSymbol");
        require(result.symbol(), "symbol");
        if (!normalized.equals(result.requestedSymbol().trim().toUpperCase(Locale.ROOT))
                || !normalized.equals(result.symbol().trim().toUpperCase(Locale.ROOT))) {
            throw failure(ExternalDataFailure.Reason.INCOMPLETE_RESPONSE, "Ticker divergente na resposta Brapi");
        }
        BrapiQuoteResponse.Data data = result.data();
        if (data == null) {
            throw incomplete("data");
        }
        String name = optional(data.longName());
        if (name.isBlank()) {
            name = required(data.shortName(), "shortName");
        }
        if (!"BRL".equals(data.currency())) {
            throw incomplete("currency");
        }
        if (data.regularMarketPrice() == null || data.regularMarketPrice().signum() <= 0) {
            throw incomplete("regularMarketPrice");
        }
        if (data.regularMarketTime() == null) {
            throw incomplete("regularMarketTime");
        }
        return new MarketQuote(normalized, name, Market.BR, Currency.BRL,
                data.regularMarketPrice().setScale(2, RoundingMode.HALF_UP), data.regularMarketTime(),
                clock.instant(), BrapiClient.SOURCE);
    }

    private static String normalizeTicker(String ticker) {
        String normalized = ticker == null ? "" : ticker.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z0-9.]{1,20}")) {
            throw failure(ExternalDataFailure.Reason.INVALID_INPUT, "Ticker brasileiro inválido");
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

    private static String optional(String value) {
        return value == null ? "" : value.trim();
    }

    private static ExternalDataFailure incomplete(String field) {
        return failure(ExternalDataFailure.Reason.INCOMPLETE_RESPONSE,
                "Campo obrigatório ausente ou inválido: " + field);
    }

    private static ExternalDataFailure failure(ExternalDataFailure.Reason reason, String message) {
        return new ExternalDataFailure(reason, BrapiClient.SOURCE, message);
    }
}
