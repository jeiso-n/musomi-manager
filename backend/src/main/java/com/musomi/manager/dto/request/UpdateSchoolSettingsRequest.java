package com.musomi.manager.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateSchoolSettingsRequest(
        @jakarta.validation.constraints.Size(max = 200)
        String schoolName,
        @jakarta.validation.constraints.Size(max = 2000)
        String address,
        @jakarta.validation.constraints.Size(max = 50)
        String phone,
        @jakarta.validation.constraints.Size(max = 100)
        String email,
        @Size(max = 4000)
        String gradingScale,
        @Size(max = 2000)
        String reportHeader,
        @Size(max = 2000)
        String reportFooter,
        Long currentTermId
) {
    public UpdateSchoolSettingsRequest(String gradingScale, String reportHeader,
            String reportFooter, Long currentTermId) {
        this(null, null, null, null, gradingScale, reportHeader, reportFooter, currentTermId);
    }
}
