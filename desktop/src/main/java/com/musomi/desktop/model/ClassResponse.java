package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ClassResponse(
        Long id,
        String name,
        Long classLevelId,
        Long academicYearId,
        Long classTeacherId,
        boolean isActive
) {
}
