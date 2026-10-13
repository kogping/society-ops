package io.github.kogping.societyops.requests.domain;

import io.github.kogping.societyops.shared.Money;

import java.time.LocalDate;
import java.util.Objects;

public record ReimbursementDetails(
        Claimant claimant,
        BankDetails bankDetails,
        Portfolio portfolio,
        String eventName,
        LocalDate eventDate,
        Money amount,
        String declarationSignature) {

    public ReimbursementDetails {
        Objects.requireNonNull(claimant, "claimant");
        Objects.requireNonNull(bankDetails, "bankDetails");
        Objects.requireNonNull(portfolio, "portfolio");
        Objects.requireNonNull(eventDate, "eventDate");
        Objects.requireNonNull(amount, "amount");
        eventName = Claimant.requireText(eventName, "Event name", 200);
        declarationSignature = Claimant.requireText(declarationSignature, "Declaration signature", 200);
        if (!amount.isPositive()) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }
}
