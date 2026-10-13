package io.github.kogping.societyops.requests.application;

import io.github.kogping.societyops.requests.domain.MemberId;
import io.github.kogping.societyops.requests.domain.Reimbursement;
import io.github.kogping.societyops.requests.domain.ReimbursementDetails;
import io.github.kogping.societyops.requests.domain.RequestCommand;
import io.github.kogping.societyops.requests.domain.RequestId;
import io.github.kogping.societyops.shared.PageResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@Transactional
public class RequestService {

    private final RequestRepository repository;
    private final Clock clock;

    RequestService(RequestRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Reimbursement submit(MemberId requester, ReimbursementDetails details) {
        Reimbursement reimbursement = Reimbursement.submit(RequestId.newId(), requester, details, now());
        repository.save(reimbursement);
        return reimbursement;
    }

    public Reimbursement handle(RequestId id, RequestCommand command) {
        Reimbursement reimbursement = repository.findById(id).orElseThrow(() -> new RequestNotFoundException(id));
        reimbursement.handle(command, now());
        repository.save(reimbursement);
        return reimbursement;
    }

    @Transactional(readOnly = true)
    public Reimbursement get(RequestId id, Viewer viewer) {
        return repository.findById(id)
                .filter(r -> viewer.privileged() || r.requester().equals(viewer.id()))
                .orElseThrow(() -> new RequestNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public PageResult<Reimbursement> list(RequestQuery query, Viewer viewer) {
        RequestQuery effective = viewer.privileged() ? query
                : new RequestQuery(query.status(), java.util.Optional.of(viewer.id()),
                        query.newestFirst(), query.page(), query.size());
        return repository.search(effective);
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }
}
