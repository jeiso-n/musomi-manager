package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UserResponse(
        Long id,
        String username,
        String fullName,
        String role,
        boolean isActive
) {
}
