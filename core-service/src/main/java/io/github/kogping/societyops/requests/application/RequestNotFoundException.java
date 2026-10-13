package io.github.kogping.societyops.requests.application;

import io.github.kogping.societyops.requests.domain.RequestId;

public class RequestNotFoundException extends RuntimeException {

    public RequestNotFoundException(RequestId id) {
        super("Request " + id + " was not found");
    }
}
