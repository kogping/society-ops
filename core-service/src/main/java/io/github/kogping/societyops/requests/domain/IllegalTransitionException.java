package io.github.kogping.societyops.requests.domain;

public class IllegalTransitionException extends RuntimeException {

    private final RequestStatus from;

    public IllegalTransitionException(RequestStatus from, RequestCommand command) {
        super("Cannot %s a request that is %s".formatted(command.getClass().getSimpleName(), from));
        this.from = from;
    }

    public RequestStatus from() {
        return from;
    }
}
