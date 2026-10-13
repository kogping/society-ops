package io.github.kogping.societyops.requests.application;

import io.github.kogping.societyops.requests.domain.MemberId;

public record Viewer(MemberId id, boolean privileged) {
}
