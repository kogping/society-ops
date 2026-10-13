package io.github.kogping.societyops.requests.application;

import io.github.kogping.societyops.requests.domain.Reimbursement;
import io.github.kogping.societyops.requests.domain.RequestId;
import io.github.kogping.societyops.shared.PageResult;

import java.util.Optional;

public interface RequestRepository {

    Optional<Reimbursement> findById(RequestId id);

    void save(Reimbursement reimbursement);

    PageResult<Reimbursement> search(RequestQuery query);
}
