package io.github.kogping.societyops.requests.application;

import io.github.kogping.societyops.requests.domain.MemberId;
import io.github.kogping.societyops.requests.domain.RequestStatus;

import java.util.Optional;

public record RequestQuery(Optional<RequestStatus> status, Optional<MemberId> requester,
                           boolean newestFirst, int page, int size) {
}
