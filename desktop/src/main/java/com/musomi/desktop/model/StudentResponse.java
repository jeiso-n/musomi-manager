package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StudentResponse(
        Long id,
        String admissionNumber,
        String fullName,
        String gender,
        @JsonAlias("currentClassName") String currentClass,
        @JsonAlias("currentStreamName") String currentStream,
        String status,
        String guardianName,
        String guardianPhone,
        Long currentClassId,
        Long currentStreamId
) {
}
