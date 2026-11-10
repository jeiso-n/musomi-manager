package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StudentAccountCredentialResponse(
        Long studentId,
        String fullName,
        String admissionNumber,
        String username,
        String temporaryPassword
) {
}
