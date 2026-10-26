package com.musomi.desktop.model;

public record SubjectResponse(
        Long id,
        String code,
        String name,
        String description,
        boolean isCore,
        boolean isActive
) {}