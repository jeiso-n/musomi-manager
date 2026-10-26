package com.musomi.desktop.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TermResponse(
        Long id,
        Long academicYearId,
        String name,
        LocalDate startDate,
        LocalDate endDate,
        Boolean isCurrent
) {
    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
