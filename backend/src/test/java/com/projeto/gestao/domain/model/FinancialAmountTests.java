package com.projeto.gestao.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.math.BigDecimal;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class FinancialAmountTests {
    @ParameterizedTest
    @CsvSource({
        "10.120, 10.12",
        "10.121, 10.12",
        "10.122, 10.12",
        "10.123, 10.12",
        "10.124, 10.12",
        "10.125, 10.13",
        "10.126, 10.13",
        "10.127, 10.13",
        "10.128, 10.13",
        "10.129, 10.13"
    })
    void roundsEveryPossibleThirdDecimalDigitUsingHalfUp(String input, String expected) {
        assertThat(FinancialAmount.of(input).value()).isEqualByComparingTo(expected);
    }

    @ParameterizedTest
    @CsvSource({
        "-10.124, -10.12",
        "-10.125, -10.13"
    })
    void roundsNegativeAmountsUsingHalfUp(String input, String expected) {
        assertThat(FinancialAmount.of(input).value()).isEqualByComparingTo(expected);
    }

    @Test
    void addsAmountsWithoutFloatingPointArithmetic() {
        FinancialAmount result = FinancialAmount.of("10.23").add(FinancialAmount.of("2.10"));

        assertThat(result.value()).isEqualByComparingTo("12.33");
    }

    @Test
    void subtractsAndMultipliesDecimalAmountsWithoutFloatingPointArithmetic() {
        FinancialAmount result = FinancialAmount.of("10.23")
                .subtract(FinancialAmount.of("2.10"))
                .multiply(new BigDecimal("1.5"));

        assertThat(result.value()).isEqualByComparingTo("12.20");
        assertThat(FinancialAmount.zero().value()).isEqualByComparingTo("0.00");
    }

    @Test
    void multipliesAmountByAnIntegerQuantity() {
        FinancialAmount result = FinancialAmount.of("12.345").multiply(3);

        assertThat(result.value()).isEqualByComparingTo("37.05");
    }

    @Test
    void roundsExternalExchangeRateBeforeConvertingUsdToBrl() {
        FinancialAmount result = FinancialAmount.of("10.01")
                .convertUsdToBrl(new BigDecimal("5.2379"));

        assertThat(result.value()).isEqualByComparingTo("52.45");
    }

    @Test
    void rejectsNullValuesAndInvalidExchangeRates() {
        assertThatNullPointerException().isThrownBy(() -> new FinancialAmount(null));
        assertThatNullPointerException().isThrownBy(() -> FinancialAmount.of("1.00").add(null));
        assertThatNullPointerException()
                .isThrownBy(() -> FinancialAmount.of("1.00").convertUsdToBrl(null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> FinancialAmount.of("1.00").convertUsdToBrl(BigDecimal.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> FinancialAmount.of("1.00").convertUsdToBrl(new BigDecimal("-1.00")));
    }
}
