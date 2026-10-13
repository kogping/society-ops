package io.github.kogping.societyops.shared;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;

public record Money(BigDecimal amount, Currency currency) {

    public static final Currency AUD = Currency.getInstance("AUD");

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        try {
            amount = amount.setScale(currency.getDefaultFractionDigits());
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "Amount has more than %d decimal places for %s".formatted(
                            currency.getDefaultFractionDigits(), currency.getCurrencyCode()), e);
        }
    }

    public static Money aud(BigDecimal amount) {
        return new Money(amount, AUD);
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }
}
