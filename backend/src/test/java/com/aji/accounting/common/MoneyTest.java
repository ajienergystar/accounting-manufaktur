package com.aji.accounting.common;

import com.aji.accounting.common.util.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyTest {

    @Test
    void multiplyRoundsHalfUpToTwoDecimals() {
        assertThat(Money.multiply(new BigDecimal("3"), new BigDecimal("1000.005")))
                .isEqualByComparingTo("3000.02");
    }

    @Test
    void divideCostByZeroReturnsZero() {
        assertThat(Money.divideCost(new BigDecimal("100"), BigDecimal.ZERO)).isEqualByComparingTo("0");
    }

    @Test
    void divideCostUsesFourDecimals() {
        assertThat(Money.divideCost(new BigDecimal("100"), new BigDecimal("3")))
                .isEqualByComparingTo("33.3333");
    }
}
