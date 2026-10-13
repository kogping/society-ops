package io.github.kogping.societyops.requests.adapters.web;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PageResponse<T>(@NotNull List<T> content, @NotNull int page, @NotNull int size,
                       @NotNull long totalElements, @NotNull int totalPages) {

    static <T> PageResponse<T> of(List<T> content, int page, int size, long totalElements) {
        return new PageResponse<>(content, page, size, totalElements, (int) Math.ceil((double) totalElements / size));
    }
}
