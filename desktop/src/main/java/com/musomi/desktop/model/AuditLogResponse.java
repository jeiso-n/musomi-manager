package com.musomi.desktop.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuditLogResponse(
        Long id,
        String createdAt,
        String userName,
        String action,
        String entityType,
        String details
) {
}
