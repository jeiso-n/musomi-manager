package com.musomi.desktop.api;

import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.musomi.desktop.model.AuditLogResponse;

public final class AdminAuditApi {

    private static final class Holder {
        private static final AdminAuditApi INSTANCE = new AdminAuditApi();
    }

    private final ApiClient client = ApiClient.getInstance();

    private AdminAuditApi() {
    }

    public static AdminAuditApi getInstance() {
        return Holder.INSTANCE;
    }

    public List<AuditLogResponse> getAuditLogs() {
        List<AuditLogResponse> logs = client.get("/admin/audit",
                new TypeReference<List<AuditLogResponse>>() {});
        return logs == null ? List.of() : logs;
    }
}
