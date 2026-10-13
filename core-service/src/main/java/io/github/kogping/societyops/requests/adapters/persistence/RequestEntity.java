package io.github.kogping.societyops.requests.adapters.persistence;

import io.github.kogping.societyops.requests.domain.Portfolio;
import io.github.kogping.societyops.requests.domain.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "requests")
class RequestEntity {

    @Id
    UUID id;

    @Version
    Long version;

    String requester;

    @Enumerated(EnumType.STRING)
    RequestStatus status;

    @Column(name = "claimant_first_name")
    String claimantFirstName;
    @Column(name = "claimant_last_name")
    String claimantLastName;
    @Column(name = "claimant_email")
    String claimantEmail;
    @Column(name = "bank_bsb")
    String bankBsb;
    @Column(name = "bank_account_number")
    String bankAccountNumber;

    @Enumerated(EnumType.STRING)
    Portfolio portfolio;

    @Column(name = "event_name")
    String eventName;
    @Column(name = "event_date")
    LocalDate eventDate;

    @Column(precision = 19, scale = 4)
    BigDecimal amount;
    String currency;

    @Column(name = "declaration_signature")
    String declarationSignature;

    @Column(name = "decided_by")
    String decidedBy;
    @Column(name = "decided_at")
    Instant decidedAt;
    @Column(name = "rejection_reason")
    String rejectionReason;
    @Column(name = "paid_by")
    String paidBy;
    @Column(name = "paid_at")
    Instant paidAt;

    @Column(name = "created_at")
    Instant createdAt;
    @Column(name = "updated_at")
    Instant updatedAt;

    protected RequestEntity() {
    }
}
