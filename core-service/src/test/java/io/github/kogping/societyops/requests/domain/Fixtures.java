package io.github.kogping.societyops.requests.domain;

import io.github.kogping.societyops.shared.Money;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

final class Fixtures {

    static final Instant NOW = Instant.parse("2026-10-14T02:00:00Z");
    static final MemberId MEMBER = new MemberId("member");
    static final MemberId TREASURER = new MemberId("treasurer");

    private Fixtures() {
    }

    static ReimbursementDetails details() {
        return new ReimbursementDetails(
                new Claimant("Alex", "Nguyen", "alex@example.com"),
                new BankDetails("062-000", "12345678"),
                Portfolio.SOCIALS, "Welcome BBQ", LocalDate.of(2026, 10, 10),
                Money.aud(new BigDecimal("84.50")), "Alex Nguyen");
    }

    static Reimbursement submitted() {
        return Reimbursement.submit(RequestId.newId(), MEMBER, details(), NOW);
    }

    /** A reimbursement in the given status, built by restoring stored state. */
    static Reimbursement in(RequestStatus status) {
        boolean decided = status != RequestStatus.SUBMITTED;
        return Reimbursement.restore(RequestId.newId(), MEMBER, details(), status, NOW, NOW,
                decided ? TREASURER : null, decided ? NOW : null,
                status == RequestStatus.REJECTED ? "No receipt" : null,
                status == RequestStatus.PAID ? TREASURER : null, status == RequestStatus.PAID ? NOW : null);
    }
}
