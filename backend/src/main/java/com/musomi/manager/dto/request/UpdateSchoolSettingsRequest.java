package com.musomi.manager.dto.request;

import jakarta.validation.constraints.Size;

public record UpdateSchoolSettingsRequest(
        @Size(max = 4000)
        String gradingScale,
        @Size(max = 2000)
        String reportHeader,
        @Size(max = 2000)
        String reportFooter,
        Long currentTermId
) {
}
