package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TopicResponse(
        Long id,
        Long subjectId,
        String name,
        String description,
        Integer sortOrder
) {
    @Override
    public String toString() {
        return name == null ? "" : name;
    }
}
