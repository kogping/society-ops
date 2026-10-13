package io.github.kogping.societyops.requests.domain;

import io.github.kogping.societyops.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.Stream;

import static io.github.kogping.societyops.requests.domain.Fixtures.MEMBER;
import static io.github.kogping.societyops.requests.domain.Fixtures.NOW;
import static io.github.kogping.societyops.requests.domain.Fixtures.TREASURER;
import static io.github.kogping.societyops.requests.domain.RequestStatus.APPROVED;
import static io.github.kogping.societyops.requests.domain.RequestStatus.PAID;
import static io.github.kogping.societyops.requests.domain.RequestStatus.REJECTED;
import static io.github.kogping.societyops.requests.domain.RequestStatus.SUBMITTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReimbursementTest {

    private static final RequestCommand APPROVE = new RequestCommand.Approve(TREASURER);
    private static final RequestCommand REJECT = new RequestCommand.Reject("No receipt", TREASURER);
    private static final RequestCommand PAY = new RequestCommand.MarkPaid(TREASURER);

    /** Every status x command pair. A null expectation means the move is illegal. */
    static Stream<Arguments> transitionTable() {
        return Stream.of(
                Arguments.of(SUBMITTED, APPROVE, APPROVED),
                Arguments.of(SUBMITTED, REJECT, REJECTED),
                Arguments.of(SUBMITTED, PAY, null),
                Arguments.of(APPROVED, APPROVE, null),
                Arguments.of(APPROVED, REJECT, null),
                Arguments.of(APPROVED, PAY, PAID),
                Arguments.of(REJECTED, APPROVE, null),
                Arguments.of(REJECTED, REJECT, null),
                Arguments.of(REJECTED, PAY, null),
                Arguments.of(PAID, APPROVE, null),
                Arguments.of(PAID, REJECT, null),
                Arguments.of(PAID, PAY, null));
    }

    @ParameterizedTest(name = "{0} + {1} -> {2}")
    @MethodSource("transitionTable")
    void followsTheTransitionTable(RequestStatus from, RequestCommand command, RequestStatus expected) {
        Reimbursement r = Fixtures.in(from);

        if (expected == null) {
            assertThatThrownBy(() -> r.handle(command, NOW.plusSeconds(60)))
                    .isInstanceOf(IllegalTransitionException.class)
                    .satisfies(e -> assertThat(((IllegalTransitionException) e).from()).isEqualTo(from));
            assertThat(r.status()).isEqualTo(from);
            assertThat(r.updatedAt()).isEqualTo(NOW);
        } else {
            r.handle(command, NOW.plusSeconds(60));
            assertThat(r.status()).isEqualTo(expected);
            assertThat(r.updatedAt()).isEqualTo(NOW.plusSeconds(60));
        }
    }

    @Test
    void submitStartsInSubmitted() {
        Reimbursement r = Fixtures.submitted();
        assertThat(r.status()).isEqualTo(SUBMITTED);
        assertThat(r.requester()).isEqualTo(MEMBER);
        assertThat(r.createdAt()).isEqualTo(NOW).isEqualTo(r.updatedAt());
        assertThat(r.decidedBy()).isNull();
    }

    @Test
    void approveRecordsWhoAndWhen() {
        Reimbursement r = Fixtures.submitted();
        r.handle(APPROVE, NOW.plusSeconds(5));
        assertThat(r.decidedBy()).isEqualTo(TREASURER);
        assertThat(r.decidedAt()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void rejectRecordsReason() {
        Reimbursement r = Fixtures.submitted();
        r.handle(REJECT, NOW.plusSeconds(5));
        assertThat(r.rejectionReason()).isEqualTo("No receipt");
        assertThat(r.decidedBy()).isEqualTo(TREASURER);
    }

    @Test
    void payRecordsWhoAndWhen() {
        Reimbursement r = Fixtures.submitted();
        r.handle(APPROVE, NOW.plusSeconds(5));
        r.handle(PAY, NOW.plusSeconds(10));
        assertThat(r.paidBy()).isEqualTo(TREASURER);
        assertThat(r.paidAt()).isEqualTo(NOW.plusSeconds(10));
    }

    @Test
    void rejectRequiresAReason() {
        assertThatThrownBy(() -> new RequestCommand.Reject(" ", TREASURER))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RequestCommand.Reject(null, TREASURER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void eventCannotBeInTheFuture() {
        // 02:00 UTC on 14 Oct is already midday on 14 Oct in Sydney, so the 15th is still the future.
        ReimbursementDetails d = withEventDate(LocalDate.of(2026, 10, 15));
        assertThatThrownBy(() -> Reimbursement.submit(RequestId.newId(), MEMBER, d, NOW))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");
    }

    @Test
    void eventDateIsJudgedInSydneyTime() {
        // 20:00 UTC on the 13th is already the 14th in Sydney (UTC+11 in daylight saving).
        ReimbursementDetails d = withEventDate(LocalDate.of(2026, 10, 14));
        assertThat(Reimbursement.submit(RequestId.newId(), MEMBER, d, NOW.minus(Duration.ofHours(6))))
                .isNotNull();
    }

    @Test
    void amountMustBePositive() {
        assertThatThrownBy(() -> new ReimbursementDetails(Fixtures.details().claimant(), Fixtures.details().bankDetails(),
                Portfolio.IT, "Hack night", LocalDate.of(2026, 10, 1), Money.aud(BigDecimal.ZERO), "Alex Nguyen"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("greater than zero");
    }

    @Test
    void bankDetailsAreValidatedAndNeverPrinted() {
        assertThatThrownBy(() -> new BankDetails("12345", "12345678")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BankDetails("062000", "123")).isInstanceOf(IllegalArgumentException.class);
        BankDetails bank = new BankDetails("062-000", "12345678");
        assertThat(bank.bsb()).isEqualTo("062000");
        assertThat(bank.toString()).doesNotContain("062").doesNotContain("12345678");
    }

    @Test
    void claimantEmailIsValidated() {
        assertThatThrownBy(() -> new Claimant("A", "B", "not-an-email")).isInstanceOf(IllegalArgumentException.class);
    }

    private static ReimbursementDetails withEventDate(LocalDate date) {
        ReimbursementDetails d = Fixtures.details();
        return new ReimbursementDetails(d.claimant(), d.bankDetails(), d.portfolio(), d.eventName(), date,
                d.amount(), d.declarationSignature());
    }
}
