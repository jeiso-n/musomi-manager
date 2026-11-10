package com.musomi.desktop.controller.admin;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.musomi.desktop.model.AcademicYearResponse;
import com.musomi.desktop.model.SchoolSettingsResponse;
import com.musomi.desktop.model.TermResponse;
import com.musomi.desktop.model.UpdateSchoolSettingsRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SettingsControllerTest {

    private static final SchoolSettingsResponse SAMPLE_SETTINGS = new SchoolSettingsResponse(
            1L,
            1L,
            "St. Mary's Secondary School",
            null,
            "Plot 12 Kampala Road",
            "+256700111222",
            "info@stmarys.ug",
            "[{\"grade\":\"A\",\"min\":80,\"max\":100}]",
            "Excellence in Education",
            "Headteacher Signature Required",
            3L,
            3L
    );

    private static final List<AcademicYearResponse> SAMPLE_YEARS = List.of(
            new AcademicYearResponse(3L, 2026, true),
            new AcademicYearResponse(2L, 2025, false)
    );

    private static final List<TermResponse> TERMS_ONLY_2026 = List.of(
            new TermResponse(3L, 3L, "Term 1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 1), true),
            new TermResponse(4L, 3L, "Term 2", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 8, 20), false),
            new TermResponse(5L, 3L, "Term 3", LocalDate.of(2026, 9, 14), LocalDate.of(2026, 12, 4), false)
    );

    private static final List<TermResponse> TERMS_2025_AND_2026 = List.of(
            new TermResponse(1L, 2L, "Term 1", LocalDate.of(2025, 2, 1), LocalDate.of(2025, 5, 1), false),
            new TermResponse(2L, 2L, "Term 2", LocalDate.of(2025, 5, 25), LocalDate.of(2025, 8, 20), false),
            new TermResponse(3L, 3L, "Term 1", LocalDate.of(2026, 2, 1), LocalDate.of(2026, 5, 1), true),
            new TermResponse(4L, 3L, "Term 2", LocalDate.of(2026, 5, 25), LocalDate.of(2026, 8, 20), false)
    );

    @Test
    @DisplayName("resolveAcademicYearDisplay shows 4-digit year 2026 instead of raw database ID 3")
    void resolveAcademicYearDisplay_returnsReadableYearInsteadOfDatabaseId() {
        String display = SettingsController.resolveAcademicYearDisplay(
                SAMPLE_SETTINGS, SAMPLE_YEARS, TERMS_ONLY_2026);

        assertEquals("2026", display);
    }

    @Test
    @DisplayName("resolveCurrentTermName selects the current term name from settings")
    void resolveCurrentTermName_returnsMatchingTermName() {
        String termName = SettingsController.resolveCurrentTermName(
                SAMPLE_SETTINGS, TERMS_ONLY_2026, TERMS_ONLY_2026);

        assertEquals("Term 1", termName);
    }

    @Test
    @DisplayName("resolveTermSelection resolves selected term ID for active academic year")
    void resolveTermSelection_matchesTermInYear() {
        TermResponse selected = SettingsController.resolveTermSelection(
                "Term 2", 3L, SAMPLE_SETTINGS, TERMS_ONLY_2026);

        assertNotNull(selected);
        assertEquals(4L, selected.id());
        assertEquals(3L, selected.academicYearId());
    }

    @Test
    @DisplayName("buildUpdateRequest preserves address, phone, email, gradingScale, reportHeader, and reportFooter")
    void buildUpdateRequest_preservesAllExistingSchoolSettingsFields() {
        UpdateSchoolSettingsRequest request = SettingsController.buildUpdateRequest(
                SAMPLE_SETTINGS, "St. Mary's College", 4L);

        assertEquals("St. Mary's College", request.schoolName());
        assertEquals("Plot 12 Kampala Road", request.address());
        assertEquals("+256700111222", request.phone());
        assertEquals("info@stmarys.ug", request.email());
        assertEquals("[{\"grade\":\"A\",\"min\":80,\"max\":100}]", request.gradingScale());
        assertEquals("Excellence in Education", request.reportHeader());
        assertEquals("Headteacher Signature Required", request.reportFooter());
        assertEquals(4L, request.currentTermId());
    }

    @Test
    @DisplayName("Saving before settings load (loadedSettings == null) is rejected and does not build a null-overwriting request")
    void prepareSavePlan_rejectsSaveWhenSettingsNotLoaded() {
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> SettingsController.prepareSavePlan(
                        null,
                        "St. Mary's Secondary School",
                        "2026",
                        "Term 1",
                        SAMPLE_YEARS,
                        TERMS_ONLY_2026));
        assertTrue(ex.getMessage().contains("Settings have not finished loading"));

        assertThrows(
                IllegalStateException.class,
                () -> SettingsController.buildUpdateRequest(null, "St. Mary's", 3L));
    }

    @Test
    @DisplayName("Selecting 2025 when terms only exist for 2026 returns empty term list and never falls back to 2026 terms")
    void filterTermsForYear_doesNotFallbackToOtherYearsWhenSelectedYearHasNoTerms() {
        List<TermResponse> termsFor2025 = SettingsController.filterTermsForYear(TERMS_ONLY_2026, 2L);

        assertTrue(termsFor2025.isEmpty());
        assertNull(SettingsController.resolveTermSelection("Term 1", 2L, SAMPLE_SETTINGS, TERMS_ONLY_2026));
        assertNull(SettingsController.resolveTermSelection(null, 2L, SAMPLE_SETTINGS, TERMS_ONLY_2026));
    }

    @Test
    @DisplayName("Attempting to save an academic year that has no configured terms is rejected with a clear message")
    void prepareSavePlan_rejectsAcademicYearWithNoTerms() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> SettingsController.prepareSavePlan(
                        SAMPLE_SETTINGS,
                        "St. Mary's Secondary School",
                        "2025",
                        "Term 1",
                        SAMPLE_YEARS,
                        TERMS_ONLY_2026));

        assertTrue(ex.getMessage().contains("No terms are configured for academic year 2025"));
    }

    @Test
    @DisplayName("Switching between academic years selects the term belonging to the target academic year")
    void switchingBetweenAcademicYears_selectsCorrectTermForEachYear() {
        SettingsController.SavePlan plan2025 = SettingsController.prepareSavePlan(
                SAMPLE_SETTINGS,
                "St. Mary's Secondary School",
                "2025",
                "Term 2",
                SAMPLE_YEARS,
                TERMS_2025_AND_2026);
        assertEquals(2L, plan2025.targetYear().id());
        assertEquals(2L, plan2025.targetTerm().id());
        assertEquals(2L, plan2025.targetTerm().academicYearId());
        assertEquals(2L, plan2025.request().currentTermId());

        SettingsController.SavePlan plan2026 = SettingsController.prepareSavePlan(
                SAMPLE_SETTINGS,
                "St. Mary's Secondary School",
                "2026",
                "Term 2",
                SAMPLE_YEARS,
                TERMS_2025_AND_2026);
        assertEquals(3L, plan2026.targetYear().id());
        assertEquals(4L, plan2026.targetTerm().id());
        assertEquals(3L, plan2026.targetTerm().academicYearId());
        assertEquals(4L, plan2026.request().currentTermId());
    }

    @Test
    @DisplayName("Attempting to save an unknown academic year or invalid term for the year is rejected")
    void prepareSavePlan_rejectsInvalidYearOrTermCombination() {
        IllegalArgumentException unknownYearEx = assertThrows(
                IllegalArgumentException.class,
                () -> SettingsController.prepareSavePlan(
                        SAMPLE_SETTINGS,
                        "St. Mary's Secondary School",
                        "2024",
                        "Term 1",
                        SAMPLE_YEARS,
                        TERMS_2025_AND_2026));
        assertTrue(unknownYearEx.getMessage().contains("2024"));

        IllegalArgumentException invalidTermEx = assertThrows(
                IllegalArgumentException.class,
                () -> SettingsController.prepareSavePlan(
                        SAMPLE_SETTINGS,
                        "St. Mary's Secondary School",
                        "2025",
                        "Term 3",
                        SAMPLE_YEARS,
                        TERMS_2025_AND_2026));
        assertTrue(invalidTermEx.getMessage().contains("valid term"));
    }
}
