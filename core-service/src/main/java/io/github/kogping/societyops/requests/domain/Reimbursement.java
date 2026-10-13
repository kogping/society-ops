package io.github.kogping.societyops.requests.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

public final class Reimbursement {

    private static final ZoneId SOCIETY_ZONE = ZoneId.of("Australia/Sydney");

    private final RequestId id;
    private final MemberId requester;
    private final ReimbursementDetails details;
    private final Instant createdAt;

    private RequestStatus status;
    private Instant updatedAt;
    private MemberId decidedBy;
    private Instant decidedAt;
    private String rejectionReason;
    private MemberId paidBy;
    private Instant paidAt;

    private Reimbursement(RequestId id, MemberId requester, ReimbursementDetails details,
                          RequestStatus status, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.requester = Objects.requireNonNull(requester, "requester");
        this.details = Objects.requireNonNull(details, "details");
        this.status = Objects.requireNonNull(status, "status");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public static Reimbursement submit(RequestId id, MemberId requester, ReimbursementDetails details, Instant now) {
        LocalDate today = now.atZone(SOCIETY_ZONE).toLocalDate();
        if (details.eventDate().isAfter(today)) {
            throw new IllegalArgumentException("Event date cannot be in the future");
        }
        return new Reimbursement(id, requester, details, RequestStatus.SUBMITTED, now, now);
    }

    /** Rebuilds a stored reimbursement. Used by persistence adapters only; performs no transition checks. */
    public static Reimbursement restore(RequestId id, MemberId requester, ReimbursementDetails details,
                                        RequestStatus status, Instant createdAt, Instant updatedAt,
                                        MemberId decidedBy, Instant decidedAt, String rejectionReason,
                                        MemberId paidBy, Instant paidAt) {
        Reimbursement r = new Reimbursement(id, requester, details, status, createdAt, updatedAt);
        r.decidedBy = decidedBy;
        r.decidedAt = decidedAt;
        r.rejectionReason = rejectionReason;
        r.paidBy = paidBy;
        r.paidAt = paidAt;
        return r;
    }

    public void handle(RequestCommand command, Instant now) {
        switch (command) {
            case RequestCommand.Approve(var by) -> {
                requireStatus(RequestStatus.SUBMITTED, command);
                status = RequestStatus.APPROVED;
                decidedBy = by;
                decidedAt = now;
            }
            case RequestCommand.Reject(var reason, var by) -> {
                requireStatus(RequestStatus.SUBMITTED, command);
                status = RequestStatus.REJECTED;
                decidedBy = by;
                decidedAt = now;
                rejectionReason = reason;
            }
            case RequestCommand.MarkPaid(var by) -> {
                requireStatus(RequestStatus.APPROVED, command);
                status = RequestStatus.PAID;
                paidBy = by;
                paidAt = now;
            }
        }
        updatedAt = now;
    }

    private void requireStatus(RequestStatus expected, RequestCommand command) {
        if (status != expected) {
            throw new IllegalTransitionException(status, command);
        }
    }

    public RequestId id() { return id; }
    public MemberId requester() { return requester; }
    public ReimbursementDetails details() { return details; }
    public RequestStatus status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public MemberId decidedBy() { return decidedBy; }
    public Instant decidedAt() { return decidedAt; }
    public String rejectionReason() { return rejectionReason; }
    public MemberId paidBy() { return paidBy; }
    public Instant paidAt() { return paidAt; }
}
