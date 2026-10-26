package com.musomi.manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request to create a subject. */
public record CreateSubjectRequest(

        @NotBlank
        @Size(max = 20)
        String code,

        @NotBlank
        @Size(max = 100)
        String name,

        String description,

        Boolean isCore,

        Boolean isActive
) {
    public CreateSubjectRequest(String code, String name, String description, Boolean isCore) {
        this(code, name, description, isCore, true);
    }
}
