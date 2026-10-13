package io.github.kogping.societyops.requests.adapters.persistence;

import io.github.kogping.societyops.requests.application.RequestQuery;
import io.github.kogping.societyops.requests.application.RequestRepository;
import io.github.kogping.societyops.requests.domain.BankDetails;
import io.github.kogping.societyops.requests.domain.Claimant;
import io.github.kogping.societyops.requests.domain.MemberId;
import io.github.kogping.societyops.requests.domain.Reimbursement;
import io.github.kogping.societyops.requests.domain.ReimbursementDetails;
import io.github.kogping.societyops.requests.domain.RequestId;
import io.github.kogping.societyops.shared.Money;
import io.github.kogping.societyops.shared.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.Currency;
import java.util.Optional;

@Repository
class JpaRequestRepository implements RequestRepository {

    private final RequestJpaRepository jpa;

    JpaRequestRepository(RequestJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Optional<Reimbursement> findById(RequestId id) {
        return jpa.findById(id.value()).map(JpaRequestRepository::toDomain);
    }

    /**
     * Copies domain state onto the managed entity. Within one transaction the entity is the same instance
     * that findById loaded, so Hibernate's @Version check catches a concurrent update at flush.
     */
    @Override
    public void save(Reimbursement r) {
        RequestEntity e = jpa.findById(r.id().value()).orElseGet(RequestEntity::new);
        ReimbursementDetails d = r.details();
        e.id = r.id().value();
        e.requester = r.requester().value();
        e.status = r.status();
        e.claimantFirstName = d.claimant().firstName();
        e.claimantLastName = d.claimant().lastName();
        e.claimantEmail = d.claimant().email();
        e.bankBsb = d.bankDetails().bsb();
        e.bankAccountNumber = d.bankDetails().accountNumber();
        e.portfolio = d.portfolio();
        e.eventName = d.eventName();
        e.eventDate = d.eventDate();
        e.amount = d.amount().amount();
        e.currency = d.amount().currency().getCurrencyCode();
        e.declarationSignature = d.declarationSignature();
        e.decidedBy = r.decidedBy() == null ? null : r.decidedBy().value();
        e.decidedAt = r.decidedAt();
        e.rejectionReason = r.rejectionReason();
        e.paidBy = r.paidBy() == null ? null : r.paidBy().value();
        e.paidAt = r.paidAt();
        e.createdAt = r.createdAt();
        e.updatedAt = r.updatedAt();
        jpa.save(e);
    }

    @Override
    public PageResult<Reimbursement> search(RequestQuery q) {
        Specification<RequestEntity> spec = (root, cq, cb) -> cb.conjunction();
        if (q.status().isPresent()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("status"), q.status().get()));
        }
        if (q.requester().isPresent()) {
            spec = spec.and((root, cq, cb) -> cb.equal(root.get("requester"), q.requester().get().value()));
        }
        Sort.Direction direction = q.newestFirst() ? Sort.Direction.DESC : Sort.Direction.ASC;
        // id as tie-breaker keeps pages stable when timestamps collide.
        Sort sort = Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
        Page<RequestEntity> page = jpa.findAll(spec, PageRequest.of(q.page(), q.size(), sort));
        return new PageResult<>(page.getContent().stream().map(JpaRequestRepository::toDomain).toList(),
                q.page(), q.size(), page.getTotalElements());
    }

    private static Reimbursement toDomain(RequestEntity e) {
        ReimbursementDetails details = new ReimbursementDetails(
                new Claimant(e.claimantFirstName, e.claimantLastName, e.claimantEmail),
                new BankDetails(e.bankBsb, e.bankAccountNumber),
                e.portfolio, e.eventName, e.eventDate,
                new Money(e.amount, Currency.getInstance(e.currency)),
                e.declarationSignature);
        return Reimbursement.restore(new RequestId(e.id), new MemberId(e.requester), details, e.status,
                e.createdAt, e.updatedAt,
                e.decidedBy == null ? null : new MemberId(e.decidedBy), e.decidedAt, e.rejectionReason,
                e.paidBy == null ? null : new MemberId(e.paidBy), e.paidAt);
    }
}
