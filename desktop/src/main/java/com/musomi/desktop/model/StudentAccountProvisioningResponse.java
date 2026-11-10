package com.musomi.desktop.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StudentAccountProvisioningResponse(
        int createdCount,
        int alreadyLinkedCount,
        int skippedCount,
        List<StudentAccountCredentialResponse> createdAccounts,
        List<String> skippedAdmissions
) {
    public StudentAccountProvisioningResponse {
        createdAccounts = createdAccounts == null ? List.of() : List.copyOf(createdAccounts);
        skippedAdmissions = skippedAdmissions == null ? List.of() : List.copyOf(skippedAdmissions);
    }
}
