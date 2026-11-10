package com.musomi.manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SetStudentPasswordRequest(
        @NotBlank
        @Size(min = 8, max = 128)
        String password
) {
}
