package com.musomi.desktop.model;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StudentResponse(
        Long id,
        String admissionNumber,
        String fullName,
        String gender,
        LocalDate dateOfBirth,
        @JsonAlias("currentClassName") String currentClass,
        @JsonAlias("currentStreamName") String currentStream,
        String status,
        String guardianName,
        String guardianPhone,
        Long currentClassId,
        Long currentStreamId
) {
    public StudentResponse(
            Long id,
            String admissionNumber,
            String fullName,
            String gender,
            String currentClass,
            String currentStream,
            String status,
            String guardianName,
            String guardianPhone,
            Long currentClassId,
            Long currentStreamId
    ) {
        this(id, admissionNumber, fullName, gender, null, currentClass, currentStream,
                status, guardianName, guardianPhone, currentClassId, currentStreamId);
    }
}
