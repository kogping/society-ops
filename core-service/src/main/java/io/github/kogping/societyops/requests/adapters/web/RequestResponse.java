package io.github.kogping.societyops.requests.adapters.web;

import io.github.kogping.societyops.requests.domain.Portfolio;
import io.github.kogping.societyops.requests.domain.Reimbursement;
import io.github.kogping.societyops.requests.domain.ReimbursementDetails;
import io.github.kogping.societyops.requests.domain.RequestStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record RequestResponse(
        @NotNull UUID id,
        @NotNull RequestStatus status,
        @NotNull String requester,
        @NotNull Claimant claimant,
        @NotNull Bank bank,
        @NotNull Portfolio portfolio,
        @NotNull String eventName,
        @NotNull LocalDate eventDate,
        @NotNull BigDecimal amount,
        @NotNull String currency,
        @NotNull String declarationSignature,
        @NotNull Instant createdAt,
        @NotNull Instant updatedAt,
        @Schema(nullable = true) String decidedBy,
        @Schema(nullable = true) Instant decidedAt,
        @Schema(nullable = true) String rejectionReason,
        @Schema(nullable = true) String paidBy,
        @Schema(nullable = true) Instant paidAt) {

    public record Claimant(@NotNull String firstName, @NotNull String lastName, @NotNull String email) {
    }

    public record Bank(@NotNull String bsb, @NotNull String accountNumber) {
    }

    static RequestResponse from(Reimbursement r, boolean showBankDetails) {
        ReimbursementDetails d = r.details();
        String account = showBankDetails ? d.bankDetails().accountNumber() : d.bankDetails().maskedAccountNumber();
        return new RequestResponse(
                r.id().value(), r.status(), r.requester().value(),
                new Claimant(d.claimant().firstName(), d.claimant().lastName(), d.claimant().email()),
                new Bank(d.bankDetails().bsb(), account),
                d.portfolio(), d.eventName(), d.eventDate(),
                d.amount().amount(), d.amount().currency().getCurrencyCode(),
                d.declarationSignature(), r.createdAt(), r.updatedAt(),
                r.decidedBy() == null ? null : r.decidedBy().value(), r.decidedAt(), r.rejectionReason(),
                r.paidBy() == null ? null : r.paidBy().value(), r.paidAt());
    }
}
