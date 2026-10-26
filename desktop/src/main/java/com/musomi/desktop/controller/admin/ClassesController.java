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

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

@RequiredArgsConstructor
@Slf4j
public class ClassesController {

    private final AdminClassApi adminClassApi = new AdminClassApi();

    @FXML
    private TableView<ClassRow> classesTable;

    @FXML
    private TableColumn<ClassRow, String> nameColumn;

    @FXML
    private TableColumn<ClassRow, String> levelColumn;

    @FXML
    private TableColumn<ClassRow, String> studentsColumn;

    @FXML
    private TableColumn<ClassRow, String> statusColumn;

    @FXML
    private TableColumn<ClassRow, String> actionsColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<ClassRow> allClasses =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        nameColumn.setCellValueFactory(data -> data.getValue().nameProperty());
        levelColumn.setCellValueFactory(data -> data.getValue().levelProperty());
        studentsColumn.setCellValueFactory(data -> data.getValue().studentsProperty());
        statusColumn.setCellValueFactory(data -> data.getValue().statusProperty());

        actionsColumn.setCellFactory(column -> new TableCell<>() {
            private final Button editButton = new Button("Edit");

            {
                editButton.getStyleClass().add("table-action-button");
                editButton.setOnAction(event -> {
                    ClassRow classRow = getTableView().getItems().get(getIndex());
                    handleEditClass(classRow);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(8, editButton));
            }
        });

        classesTable.setItems(allClasses);
        loadClassesFromBackend();

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) -> filterClasses(newValue));
        classesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
    }

    private void loadClassesFromBackend() {
        TaskRunner.run(
                adminClassApi::getClasses,
                classes -> {
                    List<ClassRow> rows = classes.stream()
                            .map(this::toClassRow)
                            .toList();
                    allClasses.setAll(rows);
                    filterClasses(searchField.getText());
                },
                error -> {
                    log.error("Failed to load classes", error);
                    Toast.error(errorMessage(error));
                });
    }

    private ClassRow toClassRow(ClassResponse classResponse) {
        return new ClassRow(
                classResponse.id(),
                classResponse.name(),
                classResponse.classLevelId() == null
                        ? ""
                        : String.valueOf(classResponse.classLevelId()),
                "",
                classResponse.isActive() ? "Active" : "Inactive",
                classResponse.classLevelId(), classResponse.academicYearId());
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "Unable to load classes."
                : error.getMessage();
    }

    private void filterClasses(String searchText) {
        String search = searchText == null ? "" : searchText.toLowerCase().trim();
        if (search.isEmpty()) {
            classesTable.setItems(allClasses);
            return;
        }

        ObservableList<ClassRow> filteredClasses = FXCollections.observableArrayList();
        for (ClassRow classRow : allClasses) {
            if (classRow.getName().toLowerCase().contains(search)
                    || classRow.getLevel().toLowerCase().contains(search)
                    || classRow.getStatus().toLowerCase().contains(search)) {
                filteredClasses.add(classRow);
            }
        }
        classesTable.setItems(filteredClasses);
    }

    @FXML
    private void showAddClassDialog() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/add-class.fxml"));
            Parent root = loader.load();
            Stage dialog = new Stage();
            dialog.setTitle("Add Class");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(createStyledScene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
        } catch (Exception e) {
            log.error("Unable to open add-class dialog", e);
            Toast.error(errorMessage(e));
        }
        loadClassesFromBackend();
    }

    private void handleEditClass(ClassRow classRow) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/admin/edit-class.fxml"));
            Parent root = loader.load();
            EditClassController controller = loader.getController();
            controller.setClassRow(classRow);
            Stage dialog = new Stage();
            dialog.setTitle("Edit Class");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setScene(createStyledScene(root));
            dialog.setResizable(false);
            dialog.showAndWait();
            if (controller.isSaved()) {
                loadClassesFromBackend();
            }
        } catch (Exception e) {
            log.error("Unable to open edit-class dialog", e);
            Toast.error(errorMessage(e));
        }
        loadClassesFromBackend();
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

    public static class ClassRow {

        private final Long id;
        private final SimpleStringProperty name;
        private final SimpleStringProperty level;
        private final SimpleStringProperty students;
        private final SimpleStringProperty status;
        private final Long classLevelId;
        private final Long academicYearId;

        public ClassRow(Long id, String name, String level, String students, String status) {
            this(id, name, level, students, status, null, null);
        }

        public ClassRow(Long id, String name, String level, String students, String status,
                        Long classLevelId, Long academicYearId) {
            this.id = id;
            this.name = new SimpleStringProperty(name);
            this.level = new SimpleStringProperty(level);
            this.students = new SimpleStringProperty(students);
            this.status = new SimpleStringProperty(status);
            this.classLevelId = classLevelId;
            this.academicYearId = academicYearId;
        }

        public Long getId() {
            return id;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty levelProperty() {
            return level;
        }

        public SimpleStringProperty studentsProperty() {
            return students;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getName() {
            return name.get();
        }

        public String getLevel() {
            return level.get();
        }

        public String getStudents() {
            return students.get();
        }

        public String getStatus() {
            return status.get();
        }
        public Long getClassLevelId() { return classLevelId; }
        public Long getAcademicYearId() { return academicYearId; }

        public void setName(String name) {
            this.name.set(name);
        }

        public void setLevel(String level) {
            this.level.set(level);
        }

        public void setStatus(String status) {
            this.status.set(status);
        }
    }
}
