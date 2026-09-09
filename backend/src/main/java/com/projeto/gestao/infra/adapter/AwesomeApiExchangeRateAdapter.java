package com.projeto.gestao.infra.adapter;

import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Component;

import com.projeto.gestao.domain.model.UsdBrlRate;
import com.projeto.gestao.domain.port.ExternalDataFailure;
import com.projeto.gestao.domain.port.UsdBrlExchangeRatePort;
import com.projeto.gestao.infra.client.AwesomeApiClient;
import com.projeto.gestao.infra.client.dto.AwesomeApiQuoteResponse;

@Component
public class AwesomeApiExchangeRateAdapter implements UsdBrlExchangeRatePort {
    private final AwesomeApiClient client;
    private final Clock clock;

    public AwesomeApiExchangeRateAdapter(AwesomeApiClient client, Clock clock) {
        this.client = client;
        this.clock = clock;
    }

    @Override
    public UsdBrlRate currentRate() {
        AwesomeApiQuoteResponse response = client.currentUsdBrl();
        AwesomeApiQuoteResponse.Quote quote = response.usdBrl();
        if (quote == null) {
            throw incomplete("USDBRL");
        }
        if (!"USD".equals(quote.code()) || !"BRL".equals(quote.codein())) {
            throw incomplete("code/codein");
        }
        if (quote.bid() == null || quote.bid().signum() <= 0) {
            throw incomplete("bid");
        }
        if (quote.timestamp() == null || quote.timestamp() <= 0) {
            throw incomplete("timestamp");
        }
        return new UsdBrlRate(quote.bid().setScale(2, RoundingMode.HALF_UP),
                Instant.ofEpochSecond(quote.timestamp()), clock.instant(), AwesomeApiClient.SOURCE);
    }

    private static ExternalDataFailure incomplete(String field) {
        return new ExternalDataFailure(ExternalDataFailure.Reason.INCOMPLETE_RESPONSE,
                AwesomeApiClient.SOURCE, "Campo obrigatório ausente ou inválido: " + field);
    }
}
