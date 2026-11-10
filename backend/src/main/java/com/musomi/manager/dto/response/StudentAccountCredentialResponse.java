package com.musomi.manager.dto.response;

public record StudentAccountCredentialResponse(
        Long studentId,
        String fullName,
        String admissionNumber,
        String username,
        String temporaryPassword
) {
}
