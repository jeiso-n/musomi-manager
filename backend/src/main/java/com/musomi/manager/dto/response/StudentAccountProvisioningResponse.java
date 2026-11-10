package com.musomi.manager.dto.response;

import java.util.List;

public record StudentAccountProvisioningResponse(
        int createdCount,
        int alreadyLinkedCount,
        int skippedCount,
        List<StudentAccountCredentialResponse> createdAccounts,
        List<String> skippedAdmissions
) {
}
