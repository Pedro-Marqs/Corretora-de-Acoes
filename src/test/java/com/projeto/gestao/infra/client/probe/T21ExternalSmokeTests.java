package com.projeto.gestao.infra.client.probe;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("external")
class T21ExternalSmokeTests {
    private static final String BRAPI_QUOTE = "https://brapi.dev/api/v2/stocks/quote?symbols=";
    private static final String TWELVE_QUOTE = "https://api.twelvedata.com/quote?symbol=";
    private static final String AWESOME_QUOTE = "https://economia.awesomeapi.com.br/json/last/USD-BRL";
    private static HttpClient http;

    @BeforeAll
    static void enableOnlyByExplicitOptIn() {
        Assumptions.assumeTrue("true".equalsIgnoreCase(System.getenv("T21_EXTERNAL_SMOKE")),
                "Set T21_EXTERNAL_SMOKE=true to access live market-data providers");
        http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Test
    void verifiesBrapiSandboxQuote() throws Exception {
        HttpResponse<String> response = get(BRAPI_QUOTE + "PETR4", bearer(System.getenv("BRAPI_TOKEN")));
        var quote = MarketDataProbe.mapBrapi(response.statusCode(), response.body());

        assertThat(quote.symbol()).isEqualTo("PETR4");
        assertThat(quote.requestedSymbol()).isEqualTo("PETR4");
        assertThat(quote.name()).isNotBlank();
        assertThat(quote.market()).isEqualTo("BR");
        assertThat(quote.currency()).isEqualTo("BRL");
        assertThat(quote.price()).isPositive();
        assertThat(quote.marketTime()).isNotNull();
    }

    @Test
    void verifiesTwelveDataRepresentativeUsQuotesAndRejectedMarket() throws Exception {
        String apiKey = System.getenv("TWELVE_DATA_API_KEY");
        Assumptions.assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Set TWELVE_DATA_API_KEY for Twelve Data live verification");
        var apple = twelveQuote("AAPL", "NASDAQ", apiKey);
        var cocaCola = twelveQuote("KO", "NYSE", apiKey);
        HttpResponse<String> canadian = get(twelveUrl("SHOP", "TSX", apiKey), "");

        assertThat(apple.market()).isEqualTo("US");
        assertThat(apple.name()).isNotBlank();
        assertThat(apple.currency()).isEqualTo("USD");
        assertThat(apple.price()).isPositive();
        assertThat(apple.marketTime()).isNotNull();
        assertThat(cocaCola.market()).isEqualTo("US");
        assertThat(cocaCola.name()).isNotBlank();
        assertThat(cocaCola.marketTime()).isNotNull();
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> MarketDataProbe.mapTwelveData(canadian.statusCode(), canadian.body()))
                .isInstanceOfSatisfying(MarketDataProbe.ProbeFailure.class,
                        failure -> assertThat(failure.kind()).isEqualTo(MarketDataProbe.Kind.UNSUPPORTED_MARKET));
    }

    @Test
    void verifiesAwesomeApiUsdBrl() throws Exception {
        HttpResponse<String> response = get(AWESOME_QUOTE, bearer(System.getenv("AWESOME_API_KEY")));
        var quote = MarketDataProbe.mapAwesomeApi(response.statusCode(), response.body());

        assertThat(quote.base()).isEqualTo("USD");
        assertThat(quote.quote()).isEqualTo("BRL");
        assertThat(quote.bid()).isPositive();
        assertThat(quote.ask()).isPositive();
        assertThat(quote.marketTime()).isNotNull();
        assertThat(quote.localMarketTime()).isNotBlank();
    }

    private static MarketDataProbe.EquityQuote twelveQuote(String symbol, String exchange, String apiKey)
            throws Exception {
        HttpResponse<String> response = get(twelveUrl(symbol, exchange, apiKey), "");
        return MarketDataProbe.mapTwelveData(response.statusCode(), response.body());
    }

    private static String twelveUrl(String symbol, String exchange, String apiKey) {
        return TWELVE_QUOTE + encode(symbol) + "&exchange=" + encode(exchange) + "&apikey=" + encode(apiKey);
    }

    private static HttpResponse<String> get(String url, String authorization) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(30)).GET();
        if (!authorization.isBlank()) {
            builder.header("Authorization", authorization);
        }
        return http.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private static String bearer(String token) {
        return token == null || token.isBlank() ? "" : "Bearer " + token;
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
