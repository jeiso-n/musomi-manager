package com.musomi.desktop.api;

import java.util.Arrays;
import java.util.List;

import com.musomi.desktop.model.AcademicYearResponse;
import com.musomi.desktop.model.SchoolSettingsResponse;
import com.musomi.desktop.model.TermResponse;
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

    public List<AcademicYearResponse> getAcademicYears() {
        AcademicYearResponse[] years = client.get("/admin/academic-years", AcademicYearResponse[].class);
        return years == null ? List.of() : Arrays.asList(years);
    }

    public List<TermResponse> getTerms() {
        TermResponse[] terms = client.get("/admin/terms", TermResponse[].class);
        return terms == null ? List.of() : Arrays.asList(terms);
    }

    public List<TermResponse> getTerms(Long academicYearId) {
        String path = academicYearId == null
                ? "/admin/terms"
                : "/admin/terms?yearId=" + academicYearId;
        TermResponse[] terms = client.get(path, TermResponse[].class);
        return terms == null ? List.of() : Arrays.asList(terms);
    }

    public AcademicYearResponse setCurrentAcademicYear(Long academicYearId) {
        return client.put("/admin/academic-years/" + academicYearId + "/current",
                null, AcademicYearResponse.class);
    }

    public TermResponse setCurrentTerm(Long termId) {
        return client.put("/admin/terms/" + termId + "/current",
                null, TermResponse.class);
    }
}
