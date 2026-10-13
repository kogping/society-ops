package io.github.kogping.societyops.shared;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void normalisesScaleToTheCurrency() {
        assertThat(Money.aud(new BigDecimal("12.5")).amount()).isEqualTo(new BigDecimal("12.50"));
        assertThat(Money.aud(new BigDecimal("12")).amount()).hasToString("12.00");
    }

    @Test
    void equalAmountsAreEqualRegardlessOfInputScale() {
        assertThat(Money.aud(new BigDecimal("5"))).isEqualTo(Money.aud(new BigDecimal("5.00")));
    }

    @Test
    void rejectsAmountsNeedingRounding() {
        assertThatThrownBy(() -> Money.aud(new BigDecimal("1.234")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decimal places");
    }

    @Test
    void respectsCurrenciesWithoutFractionDigits() {
        Money yen = new Money(new BigDecimal("100"), Currency.getInstance("JPY"));
        assertThat(yen.amount()).hasToString("100");
        assertThatThrownBy(() -> new Money(new BigDecimal("1.5"), Currency.getInstance("JPY")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNulls() {
        assertThatThrownBy(() -> new Money(null, Money.AUD)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void isPositive() {
        assertThat(Money.aud(new BigDecimal("0.01")).isPositive()).isTrue();
        assertThat(Money.aud(BigDecimal.ZERO).isPositive()).isFalse();
        assertThat(Money.aud(new BigDecimal("-1")).isPositive()).isFalse();
    }
}
