package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import com.musomi.desktop.api.AdminSettingsApi;
import com.musomi.desktop.model.SchoolSettingsResponse;
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
    private void initialize() {

        termComboBox.getItems().addAll(
                "Term 1",
                "Term 2",
                "Term 3"
        );

        TaskRunner.run(settingsApi::getSettings, this::populateSettings,
                error -> Toast.error("Unable to load settings: " + error.getMessage()));
    }

    @FXML
    private void handleSave() {

        String schoolName =
                schoolNameField.getText().trim();

        String academicYear =
                academicYearField.getText().trim();

        if (schoolName.isEmpty()) {
            Toast.error("School name is required.");
            return;
        }

        UpdateSchoolSettingsRequest request = new UpdateSchoolSettingsRequest(
                schoolName, null, null, null, null, null, null, null);
        TaskRunner.run(() -> settingsApi.updateSettings(request),
                response -> Toast.success("Settings saved successfully."),
                error -> Toast.error("Unable to save settings: " + error.getMessage()));
    }

    private void populateSettings(SchoolSettingsResponse settings) {
        if (settings == null) {
            return;
        }
        schoolNameField.setText(settings.schoolName() == null ? "" : settings.schoolName());
        academicYearField.setText(settings.currentAcademicYearId() == null
                ? "" : settings.currentAcademicYearId().toString());
    }
}