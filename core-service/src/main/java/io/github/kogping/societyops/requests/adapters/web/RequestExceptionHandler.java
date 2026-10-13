package io.github.kogping.societyops.requests.adapters.web;

import io.github.kogping.societyops.requests.application.RequestNotFoundException;
import io.github.kogping.societyops.requests.domain.IllegalTransitionException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestExceptionHandler {

    @ExceptionHandler(RequestNotFoundException.class)
    ProblemDetail notFound(RequestNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalTransitionException.class)
    ProblemDetail illegalTransition(IllegalTransitionException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setTitle("Illegal state transition");
        problem.setProperty("currentStatus", e.from());
        return problem;
    }
}
