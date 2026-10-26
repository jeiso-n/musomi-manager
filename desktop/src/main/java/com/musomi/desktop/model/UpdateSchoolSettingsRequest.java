package com.musomi.desktop.model;

public record UpdateSchoolSettingsRequest(
        String schoolName,
        String address,
        String phone,
        String email,
        String gradingScale,
        String reportHeader,
        String reportFooter,
        Long currentTermId
) {
}
