package com.musomi.desktop.controller.admin;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.util.TaskRunner;

import java.util.List;

public class AddClassController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<String> levelComboBox;

    private final AdminClassApi adminClassApi = new AdminClassApi();
    private final ApiClient apiClient = ApiClient.getInstance();
    private List<ClassLevel> levels = List.of();
    private List<AcademicYear> academicYears = List.of();

    @FXML
    private void initialize() {

        TaskRunner.run(
                () -> new ReferenceData(
                        apiClient.get("/admin/class-levels", ClassLevel[].class),
                        apiClient.get("/admin/academic-years", AcademicYear[].class)),
                data -> {
                    levels = data.levels() == null ? List.of() : List.of(data.levels());
                    academicYears = data.years() == null ? List.of() : List.of(data.years());
                    levelComboBox.getItems().setAll(
                            levels.stream().map(ClassLevel::name).toList());
                },
                error -> showAlert("Unable to load class data", errorMessage(error))
        );
    }

    @FXML
    private void handleCancel() {

        levelComboBox.getScene()
                .getWindow()
                .hide();
    }

    @FXML
    private void handleCreateClass() {

        String name = nameField.getText().trim();

        String level = levelComboBox.getValue();

        if (name.isEmpty() || level == null) {

            showAlert(
                    "Missing Information",
                    "Please fill in all fields."
            );

            return;
        }

        ClassLevel levelData = levels.stream()
                .filter(levelItem -> levelItem.name().equals(level))
                .findFirst()
                .orElse(null);
        AcademicYear currentYear = academicYears.stream()
                .filter(AcademicYear::isCurrent)
                .findFirst()
                .orElse(null);
        if (levelData == null || currentYear == null) {
            showAlert("Missing setup", "A class level and current academic year are required.");
            return;
        }

        TaskRunner.run(
                () -> adminClassApi.createClass(
                        name, levelData.id(), currentYear.id(), null),
                ignored -> levelComboBox.getScene().getWindow().hide(),
                error -> showAlert("Unable to create class", errorMessage(error))
        );
    }

    private void showAlert(
            String title,
            String message
    ) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);

        alert.showAndWait();
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "The request failed. Please try again."
                : error.getMessage();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClassLevel(Long id, String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AcademicYear(Long id, Integer year, boolean isCurrent) {}

    private record ReferenceData(ClassLevel[] levels, AcademicYear[] years) {}
}