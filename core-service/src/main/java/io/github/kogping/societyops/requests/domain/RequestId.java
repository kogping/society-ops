package io.github.kogping.societyops.requests.domain;

import java.util.Objects;
import java.util.UUID;

public record RequestId(UUID value) {

    public RequestId {
        Objects.requireNonNull(value, "value");
    }

    public static RequestId newId() {
        return new RequestId(UUID.randomUUID());
    }

    public static RequestId of(String value) {
        return new RequestId(UUID.fromString(value));
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
