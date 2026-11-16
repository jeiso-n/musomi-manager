package com.musomi.desktop.controller.admin;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

/** Edits the class name and its real backend class-level reference. */
public class EditClassController {

    @FXML private TextField nameField;
    @FXML private ComboBox<String> levelComboBox;
    @FXML private ComboBox<String> statusComboBox;

    private final AdminClassApi adminClassApi = new AdminClassApi();
    private final ApiClient apiClient = ApiClient.getInstance();
    private List<ClassLevel> levels = List.of();
    private ClassesController.ClassRow classRow;
    private boolean saved;

    @FXML
    private void initialize() {
        statusComboBox.getItems().setAll("Active", "Inactive");
        // The current backend update contract does not accept an active/status field.
        // Keep it visible for context, but don't present it as an editable setting.
        statusComboBox.setDisable(true);
        levelComboBox.setDisable(true);
        levelComboBox.setPromptText("Loading levels...");

        TaskRunner.run(() -> {
            ClassLevel[] response = apiClient.get("/admin/class-levels", ClassLevel[].class);
            return response == null ? List.<ClassLevel>of() : List.of(response);
        }, loaded -> {
            levels = loaded;
            levelComboBox.getItems().setAll(levels.stream().map(EditClassController::displayLevelOption).toList());
            levelComboBox.setDisable(false);
            selectCurrentLevel();
        }, error -> {
            levelComboBox.setPromptText("Unable to load levels");
            Toast.error(errorMessage(error));
        });
    }

    public void setClassRow(ClassesController.ClassRow row) {
        classRow = row;
        nameField.setText(row.getName());
        statusComboBox.setValue(row.getStatus());
        selectCurrentLevel();
    }

    private void selectCurrentLevel() {
        if (classRow == null || levels.isEmpty()) return;
        levels.stream()
                .filter(level -> level.id() != null && level.id().equals(classRow.getClassLevelId()))
                .findFirst()
                .ifPresent(level -> levelComboBox.setValue(displayLevelOption(level)));
    }

    @FXML
    private void handleCancel() {
        statusComboBox.getScene().getWindow().hide();
    }

    @FXML
    private void handleSave() {
        if (classRow == null) {
            Toast.error("No class was selected.");
            return;
        }
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        String selectedLevel = levelComboBox.getValue();
        if (name.isEmpty() || selectedLevel == null) {
            Toast.warning("Enter a class name and select a level.");
            return;
        }
        ClassLevel level = levels.stream()
                .filter(item -> displayLevelOption(item).equals(selectedLevel))
                .findFirst().orElse(null);
        if (level == null) {
            Toast.error("Select a valid class level.");
            return;
        }
        AdminClassApi.UpdateClassRequest request = new AdminClassApi.UpdateClassRequest(
                name, level.id(), classRow.getAcademicYearId(), null);
        TaskRunner.run(() -> adminClassApi.updateClass(classRow.getId(), request), ignored -> {
            saved = true;
            Toast.success("Class updated successfully.");
            statusComboBox.getScene().getWindow().hide();
        }, error -> Toast.error(errorMessage(error)));
    }

    public boolean isSaved() { return saved; }
    public String getName() { return nameField.getText().trim(); }
    public String getLevel() { return levelComboBox.getValue(); }

    private static String displayLevelOption(ClassLevel level) {
        if (level == null || level.name() == null) return "Unknown level";
        String name = level.name().trim();
        return switch (name.toUpperCase(java.util.Locale.ROOT)) {
            case "S1" -> "Senior One (O-Level)";
            case "S2" -> "Senior Two (O-Level)";
            case "S3" -> "Senior Three (O-Level)";
            case "S4" -> "Senior Four (O-Level)";
            case "S5" -> "Senior Five (A-Level)";
            case "S6" -> "Senior Six (A-Level)";
            default -> name;
        };
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "Unable to update class." : error.getMessage();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClassLevel(Long id, String name) { }
}
