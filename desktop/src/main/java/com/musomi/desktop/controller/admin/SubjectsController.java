package com.musomi.desktop.controller.admin;

import java.util.List;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.api.AdminSubjectApi;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

@RequiredArgsConstructor
@Slf4j
public class SubjectsController {

    private final AdminSubjectApi adminSubjectApi = new AdminSubjectApi();

    @FXML
    private TableView<SubjectRow> subjectsTable;

    @FXML
    private TableColumn<SubjectRow, String> nameColumn;

    @FXML
    private TableColumn<SubjectRow, String> codeColumn;

    @FXML
    private TableColumn<SubjectRow, String> statusColumn;

    @FXML
    private TableColumn<SubjectRow, String> actionsColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<SubjectRow> allSubjects =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        codeColumn.setCellValueFactory(data -> data.getValue().codeProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = new Button("Edit");
            private final Button deactivateButton = new Button("Deactivate");
            private final HBox buttons = new HBox(8, editButton, deactivateButton);

            {
                editButton.getStyleClass().add("table-action-button");
                deactivateButton.getStyleClass().addAll(
                        "table-action-button", "table-action-danger");
                editButton.setOnAction(event -> {
                    SubjectRow subject = getTableView().getItems().get(getIndex());
                    handleEditSubject(subject);
                });
                deactivateButton.setOnAction(event -> {
                    SubjectRow subject = getTableView().getItems().get(getIndex());
                    handleDeactivateSubject(subject);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : buttons);
            }
        });

        subjectsTable.setItems(allSubjects);
        loadSubjectsFromBackend();

        subjectsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterSubjects(newValue));
    }

    private void loadSubjectsFromBackend() {
        TaskRunner.run(
                adminSubjectApi::getSubjects,
                subjects -> {
                    List<SubjectRow> rows = subjects.stream()
                            .map(this::toSubjectRow)
                            .toList();
                    allSubjects.setAll(rows);
                    filterSubjects(searchField.getText());
                },
                error -> {
                    log.error("Failed to load subjects", error);
                    Toast.error(errorMessage(error));
                });
    }

    private SubjectRow toSubjectRow(SubjectResponse subject) {
        return new SubjectRow(
                subject.id(),
                subject.name(),
                subject.code(),
                subject.isActive() ? "Active" : "Inactive");
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "Unable to load subjects."
                : error.getMessage();
    }

    private void filterSubjects(String searchText) {
        String search = searchText == null ? "" : searchText.toLowerCase().trim();
        if (search.isEmpty()) {
            subjectsTable.setItems(allSubjects);
            return;
        }

        ObservableList<SubjectRow> filteredSubjects =
                FXCollections.observableArrayList();
        for (SubjectRow subject : allSubjects) {
            if (subject.getName().toLowerCase().contains(search)
                    || subject.getCode().toLowerCase().contains(search)
                    || subject.getStatus().toLowerCase().contains(search)) {
                filteredSubjects.add(subject);
            }
        }
        subjectsTable.setItems(filteredSubjects);
    }

    @FXML
    private void showAddSubjectDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/add-subject.fxml"));
            Parent root = loader.load();
            AddSubjectController controller = loader.getController();
            Stage dialog = new Stage();
            dialog.setTitle("Add Subject");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(createStyledScene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
            if (controller.getCreatedName() != null) {
                TaskRunner.run(
                        () -> adminSubjectApi.createSubject(
                                controller.getCreatedCode(), controller.getCreatedName()),
                        ignored -> loadSubjectsFromBackend(),
                        error -> {
                            log.error("Failed to create subject", error);
                            Toast.error(errorMessage(error));
                        });
            }
        } catch (Exception e) {
            log.error("Unable to open add-subject dialog", e);
            Toast.error(errorMessage(e));
        }
        loadSubjectsFromBackend();
    }

    private void handleEditSubject(SubjectRow subject) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/edit-subject.fxml"));
            Parent root = loader.load();
            EditSubjectController controller = loader.getController();
            controller.setSubjectRow(subject);
            Stage dialog = new Stage();
            dialog.setTitle("Edit Subject");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(createStyledScene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
            if (controller.isSaved()) {
                TaskRunner.run(
                        () -> adminSubjectApi.updateSubject(
                                subject.getId(), controller.getCode(), controller.getName(),
                                "Active".equals(controller.getStatus())),
                        ignored -> loadSubjectsFromBackend(),
                        error -> {
                            log.error("Failed to update subject", error);
                            Toast.error(errorMessage(error));
                        });
            }
        } catch (Exception e) {
            log.error("Unable to open edit-subject dialog", e);
            Toast.error(errorMessage(e));
        }
        loadSubjectsFromBackend();
    }

    private void handleDeactivateSubject(SubjectRow subject) {
        TaskRunner.run(
                () -> {
                    adminSubjectApi.deactivateSubject(subject.getId());
                    return null;
                },
                ignored -> {
                    Toast.success("Subject deactivated");
                    loadSubjectsFromBackend();
                },
                error -> Toast.error(errorMessage(error)));
    }

    @FXML
    private void deactivateSelectedSubject() {
        SubjectRow selected = subjectsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Toast.error("Select a subject first.");
            return;
        }
        handleDeactivateSubject(selected);
    }

    private Scene createStyledScene(Parent root) {
        Scene scene = new Scene(root);
        var styles = getClass().getResource("/css/styles.css");
        var components = getClass().getResource("/css/components.css");
        if (styles != null) {
            scene.getStylesheets().add(styles.toExternalForm());
        }
        if (components != null) {
            scene.getStylesheets().add(components.toExternalForm());
        }
        return scene;
    }

    public static class SubjectRow {

        private final Long id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty code;
        private final SimpleStringProperty status;

        public SubjectRow(Long id, String name, String code, String status) {
            this.id = id;
            this.name = new SimpleStringProperty(name);
            this.code = new SimpleStringProperty(code);
            this.status = new SimpleStringProperty(status);
        }

        public Long getId() {
            return id;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty codeProperty() {
            return code;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getName() {
            return name.get();
        }

        public String getCode() {
            return code.get();
        }

        public String getStatus() {
            return status.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public void setCode(String code) {
            this.code.set(code);
        }

        public void setStatus(String status) {
            this.status.set(status);
        }
    }
}
