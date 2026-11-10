package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AcademicYearResponse(
        Long id,
        Integer year,
        Boolean isCurrent
) {
    @Override
    public String toString() {
        return year == null ? "" : year.toString();
    }
}

