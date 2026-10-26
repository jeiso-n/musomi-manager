package com.musomi.desktop.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResult<T>(
        List<T> data,
        int page,
        int size,
        long total,
        int totalPages
) {
}
