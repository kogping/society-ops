package io.github.kogping.societyops.requests.domain;

import java.util.Objects;

public sealed interface RequestCommand {

    MemberId by();

    record Approve(MemberId by) implements RequestCommand {
        public Approve {
            Objects.requireNonNull(by, "by");
        }
    }

    record Reject(String reason, MemberId by) implements RequestCommand {
        public Reject {
            Objects.requireNonNull(by, "by");
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("A rejection reason is required");
            }
            reason = reason.strip();
            if (reason.length() > 500) {
                throw new IllegalArgumentException("Rejection reason must be at most 500 characters");
            }
        }
    }

    record MarkPaid(MemberId by) implements RequestCommand {
        public MarkPaid {
            Objects.requireNonNull(by, "by");
        }
    }
}
