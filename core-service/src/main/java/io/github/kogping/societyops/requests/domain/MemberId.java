package io.github.kogping.societyops.requests.domain;

import java.util.Objects;

public record MemberId(String value) {

    public MemberId {
        Objects.requireNonNull(value, "value");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Member id must not be blank");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
