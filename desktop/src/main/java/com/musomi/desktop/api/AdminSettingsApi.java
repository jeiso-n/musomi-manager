package com.musomi.desktop.api;

import com.musomi.desktop.model.SchoolSettingsResponse;
import com.musomi.desktop.model.UpdateSchoolSettingsRequest;

public final class AdminSettingsApi {

    private static final AdminSettingsApi INSTANCE = new AdminSettingsApi();
    private final ApiClient client = ApiClient.getInstance();

    private AdminSettingsApi() {
    }

    public static AdminSettingsApi getInstance() {
        return INSTANCE;
    }

    public SchoolSettingsResponse getSettings() {
        return client.get("/admin/settings", SchoolSettingsResponse.class);
    }

    public SchoolSettingsResponse updateSettings(UpdateSchoolSettingsRequest request) {
        return client.put("/admin/settings", request, SchoolSettingsResponse.class);
    }
}
