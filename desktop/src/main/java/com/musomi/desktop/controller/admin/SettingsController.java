package com.musomi.desktop.controller.admin;

import java.util.List;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import com.musomi.desktop.api.AdminSettingsApi;
import com.musomi.desktop.model.AcademicYearResponse;
import com.musomi.desktop.model.SchoolSettingsResponse;
import com.musomi.desktop.model.TermResponse;
import com.musomi.desktop.model.UpdateSchoolSettingsRequest;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class SettingsController {

    private final AdminSettingsApi settingsApi = AdminSettingsApi.getInstance();

    @FXML
    private TextField schoolNameField;

    @FXML
    private TextField academicYearField;

    @FXML
    private ComboBox<String> termComboBox;

    @FXML
    private Button saveButton;

    private SchoolSettingsResponse loadedSettings;
    private List<AcademicYearResponse> academicYears = List.of();
    private List<TermResponse> terms = List.of();
    private boolean updatingUi;

    private record SettingsData(
            SchoolSettingsResponse settings,
            List<AcademicYearResponse> academicYears,
            List<TermResponse> terms
    ) {}

    record SavePlan(
            AcademicYearResponse targetYear,
            TermResponse targetTerm,
            UpdateSchoolSettingsRequest request
    ) {}

    @FXML
    private void initialize() {
        setSaveEnabled(false);
        academicYearField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!updatingUi) {
                updateTermOptionsForYearText(newVal);
            }
        });

        TaskRunner.run(
                this::fetchSettingsData,
                this::populateSettingsData,
                error -> {
                    setSaveEnabled(false);
                    Toast.error("Unable to load settings: " + error.getMessage());
                });
    }

    private void setSaveEnabled(boolean enabled) {
        if (saveButton != null) {
            saveButton.setDisable(!enabled);
        }
    }

    private SettingsData fetchSettingsData() {
        SchoolSettingsResponse settings = settingsApi.getSettings();
        List<AcademicYearResponse> years = settingsApi.getAcademicYears();
        List<TermResponse> allTerms = settingsApi.getTerms();
        return new SettingsData(settings, years, allTerms);
    }

    @FXML
    private void handleSave() {
        if (loadedSettings == null) {
            Toast.error("Settings have not finished loading yet. Please reload before saving.");
            return;
        }

        String schoolName = schoolNameField.getText();
        String academicYearText = academicYearField.getText();
        String selectedTermName = termComboBox.getValue();

        final SavePlan plan;
        try {
            plan = prepareSavePlan(
                    loadedSettings,
                    schoolName,
                    academicYearText,
                    selectedTermName,
                    academicYears,
                    terms);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Toast.error(ex.getMessage());
            return;
        }

        setSaveEnabled(false);
        TaskRunner.run(
                () -> {
                    if (plan.targetYear() != null && plan.targetYear().id() != null) {
                        settingsApi.setCurrentAcademicYear(plan.targetYear().id());
                    }
                    if (plan.targetTerm() != null && plan.targetTerm().id() != null) {
                        settingsApi.setCurrentTerm(plan.targetTerm().id());
                    }
                    settingsApi.updateSettings(plan.request());
                    return fetchSettingsData();
                },
                data -> {
                    populateSettingsData(data);
                    Toast.success("Settings saved successfully.");
                },
                error -> {
                    setSaveEnabled(loadedSettings != null);
                    Toast.error("Unable to save settings: " + error.getMessage());
                });
    }

    static SavePlan prepareSavePlan(
            SchoolSettingsResponse existing,
            String schoolName,
            String academicYearText,
            String selectedTermName,
            List<AcademicYearResponse> academicYears,
            List<TermResponse> terms) {
        if (existing == null) {
            throw new IllegalStateException(
                    "Settings have not finished loading yet. Please reload before saving.");
        }
        String trimmedSchoolName = schoolName == null ? "" : schoolName.trim();
        if (trimmedSchoolName.isEmpty()) {
            throw new IllegalArgumentException("School name is required.");
        }

        String trimmedYearText = academicYearText == null ? "" : academicYearText.trim();
        AcademicYearResponse matchedYear = resolveAcademicYearByInput(
                trimmedYearText, existing, academicYears);
        if (!trimmedYearText.isEmpty() && matchedYear == null) {
            throw new IllegalArgumentException(
                    "Academic year \"" + trimmedYearText + "\" does not exist.");
        }

        Long targetYearId = matchedYear != null
                ? matchedYear.id()
                : resolveActiveAcademicYearId(existing, academicYears, terms);

        List<TermResponse> yearTerms = filterTermsForYear(terms, targetYearId);
        if (targetYearId != null && yearTerms.isEmpty()) {
            String yearLabel = matchedYear != null && matchedYear.year() != null
                    ? matchedYear.year().toString()
                    : (!trimmedYearText.isEmpty() ? trimmedYearText : targetYearId.toString());
            throw new IllegalArgumentException(
                    "No terms are configured for academic year " + yearLabel + ".");
        }

        TermResponse matchedTerm = resolveTermSelection(
                selectedTermName, targetYearId, existing, terms);
        if (targetYearId != null && matchedTerm == null) {
            throw new IllegalArgumentException(
                    "Please select a valid term for the chosen academic year.");
        }

        Long targetTermId = matchedTerm != null ? matchedTerm.id() : existing.currentTermId();
        UpdateSchoolSettingsRequest request = buildUpdateRequest(
                existing, trimmedSchoolName, targetTermId);
        return new SavePlan(matchedYear, matchedTerm, request);
    }

    private void populateSettingsData(SettingsData data) {
        if (data == null || data.settings() == null) {
            setSaveEnabled(false);
            return;
        }
        this.loadedSettings = data.settings();
        this.academicYears = data.academicYears() == null ? List.of() : data.academicYears();
        this.terms = data.terms() == null ? List.of() : data.terms();
        populateSettings(this.loadedSettings);
        setSaveEnabled(true);
    }

    private void populateSettings(SchoolSettingsResponse settings) {
        if (settings == null) {
            return;
        }
        updatingUi = true;
        try {
            schoolNameField.setText(settings.schoolName() == null ? "" : settings.schoolName());

            String yearDisplay = resolveAcademicYearDisplay(settings, academicYears, terms);
            academicYearField.setText(yearDisplay);

            Long activeYearId = resolveActiveAcademicYearId(settings, academicYears, terms);
            List<TermResponse> filteredTerms = filterTermsForYear(terms, activeYearId);
            List<String> termNames = filteredTerms.stream()
                    .map(TermResponse::name)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            termComboBox.getItems().setAll(termNames);

            String selectedTerm = resolveCurrentTermName(settings, filteredTerms);
            termComboBox.setValue(selectedTerm);
            termComboBox.setPromptText(termNames.isEmpty()
                    ? "No terms available for this academic year"
                    : "Select current term");
        } finally {
            updatingUi = false;
        }
    }

    private void updateTermOptionsForYearText(String yearText) {
        String trimmed = yearText == null ? "" : yearText.trim();
        AcademicYearResponse matchedYear = resolveAcademicYearByInput(
                trimmed, loadedSettings, academicYears);
        if (!trimmed.isEmpty() && matchedYear == null) {
            termComboBox.getItems().clear();
            termComboBox.setValue(null);
            termComboBox.setPromptText("Unknown academic year");
            return;
        }

        Long yearId = matchedYear != null
                ? matchedYear.id()
                : resolveActiveAcademicYearId(loadedSettings, academicYears, terms);
        List<TermResponse> filteredTerms = filterTermsForYear(terms, yearId);
        List<String> termNames = filteredTerms.stream()
                .map(TermResponse::name)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        String previousSelection = termComboBox.getValue();
        termComboBox.getItems().setAll(termNames);
        if (termNames.isEmpty()) {
            termComboBox.setValue(null);
            termComboBox.setPromptText("No terms available for this academic year");
        } else if (previousSelection != null && termNames.contains(previousSelection)) {
            termComboBox.setValue(previousSelection);
            termComboBox.setPromptText("Select current term");
        } else {
            String fallback = resolveCurrentTermName(loadedSettings, filteredTerms);
            termComboBox.setValue(fallback != null && termNames.contains(fallback)
                    ? fallback : termNames.get(0));
            termComboBox.setPromptText("Select current term");
        }
    }

    static String resolveAcademicYearDisplay(
            SchoolSettingsResponse settings,
            List<AcademicYearResponse> years,
            List<TermResponse> terms) {
        Long yearId = resolveActiveAcademicYearId(settings, years, terms);
        if (years != null && yearId != null) {
            for (AcademicYearResponse year : years) {
                if (year != null && yearId.equals(year.id()) && year.year() != null) {
                    return year.year().toString();
                }
            }
        }
        if (years != null) {
            for (AcademicYearResponse year : years) {
                if (year != null && Boolean.TRUE.equals(year.isCurrent()) && year.year() != null) {
                    return year.year().toString();
                }
            }
        }
        return "";
    }

    static Long resolveActiveAcademicYearId(
            SchoolSettingsResponse settings,
            List<AcademicYearResponse> years,
            List<TermResponse> terms) {
        if (settings != null && settings.currentAcademicYearId() != null) {
            return settings.currentAcademicYearId();
        }
        if (settings != null && settings.currentTermId() != null && terms != null) {
            for (TermResponse term : terms) {
                if (term != null && settings.currentTermId().equals(term.id())
                        && term.academicYearId() != null) {
                    return term.academicYearId();
                }
            }
        }
        if (years != null) {
            for (AcademicYearResponse year : years) {
                if (year != null && Boolean.TRUE.equals(year.isCurrent()) && year.id() != null) {
                    return year.id();
                }
            }
        }
        return null;
    }

    static List<TermResponse> filterTermsForYear(List<TermResponse> terms, Long academicYearId) {
        if (terms == null || terms.isEmpty() || academicYearId == null) {
            return List.of();
        }
        return terms.stream()
                .filter(t -> t != null && academicYearId.equals(t.academicYearId()))
                .toList();
    }

    static String resolveCurrentTermName(
            SchoolSettingsResponse settings,
            List<TermResponse> filteredTerms) {
        return resolveCurrentTermName(settings, filteredTerms, filteredTerms);
    }

    static String resolveCurrentTermName(
            SchoolSettingsResponse settings,
            List<TermResponse> filteredTerms,
            List<TermResponse> allTerms) {
        if (filteredTerms == null || filteredTerms.isEmpty()) {
            return null;
        }
        Long currentTermId = settings != null ? settings.currentTermId() : null;
        if (currentTermId != null) {
            for (TermResponse term : filteredTerms) {
                if (term != null && currentTermId.equals(term.id()) && term.name() != null) {
                    return term.name();
                }
            }
        }
        for (TermResponse term : filteredTerms) {
            if (term != null && Boolean.TRUE.equals(term.isCurrent()) && term.name() != null) {
                return term.name();
            }
        }
        return filteredTerms.get(0) != null ? filteredTerms.get(0).name() : null;
    }

    static AcademicYearResponse resolveAcademicYearByInput(
            String yearText,
            SchoolSettingsResponse settings,
            List<AcademicYearResponse> years) {
        if (years == null || years.isEmpty()) {
            return null;
        }
        String trimmed = yearText == null ? "" : yearText.trim();
        if (!trimmed.isEmpty()) {
            for (AcademicYearResponse year : years) {
                if (year != null && year.year() != null
                        && trimmed.equals(year.year().toString())) {
                    return year;
                }
            }
            return null;
        }
        if (settings != null && settings.currentAcademicYearId() != null) {
            for (AcademicYearResponse year : years) {
                if (year != null && settings.currentAcademicYearId().equals(year.id())) {
                    return year;
                }
            }
        }
        for (AcademicYearResponse year : years) {
            if (year != null && Boolean.TRUE.equals(year.isCurrent())) {
                return year;
            }
        }
        return null;
    }

    static TermResponse resolveTermSelection(
            String selectedTermName,
            Long targetYearId,
            SchoolSettingsResponse settings,
            List<TermResponse> terms) {
        List<TermResponse> yearTerms = filterTermsForYear(terms, targetYearId);
        if (yearTerms.isEmpty()) {
            return null;
        }
        String trimmed = selectedTermName == null ? "" : selectedTermName.trim();
        if (!trimmed.isEmpty()) {
            for (TermResponse term : yearTerms) {
                if (term != null && term.name() != null
                        && trimmed.equalsIgnoreCase(term.name().trim())) {
                    return term;
                }
            }
            return null;
        }
        if (settings != null && settings.currentTermId() != null) {
            for (TermResponse term : yearTerms) {
                if (term != null && settings.currentTermId().equals(term.id())) {
                    return term;
                }
            }
        }
        for (TermResponse term : yearTerms) {
            if (term != null && Boolean.TRUE.equals(term.isCurrent())) {
                return term;
            }
        }
        return null;
    }

    static UpdateSchoolSettingsRequest buildUpdateRequest(
            SchoolSettingsResponse existing,
            String schoolName,
            Long selectedTermId) {
        if (existing == null) {
            throw new IllegalStateException(
                    "Settings have not finished loading yet. Please reload before saving.");
        }
        Long effectiveTermId = selectedTermId != null
                ? selectedTermId
                : existing.currentTermId();
        return new UpdateSchoolSettingsRequest(
                schoolName,
                existing.address(),
                existing.phone(),
                existing.email(),
                existing.gradingScale(),
                existing.reportHeader(),
                existing.reportFooter(),
                effectiveTermId
        );
    }
}