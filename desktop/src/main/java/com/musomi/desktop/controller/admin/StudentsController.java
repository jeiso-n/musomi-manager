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
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.api.AdminStudentApi;
import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.model.StudentResponse;
import com.musomi.desktop.model.StudentAccountCredentialResponse;
import com.musomi.desktop.model.StudentAccountProvisioningResponse;
import com.musomi.desktop.util.Modal;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

@RequiredArgsConstructor
@Slf4j
public class StudentsController {

    private final AdminStudentApi adminStudentApi = new AdminStudentApi();
    private final AdminClassApi adminClassApi = new AdminClassApi();

    @FXML
    private TableView<StudentRow> studentsTable;

    @FXML
    private TableColumn<StudentRow, String> nameColumn;

    @FXML
    private TableColumn<StudentRow, String> admissionNumberColumn;

    @FXML
    private TableColumn<StudentRow, String> classColumn;

    @FXML
    private TableColumn<StudentRow, String> statusColumn;

    @FXML
    private TableColumn<StudentRow, String> actionsColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<StudentRow> allStudents =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        nameColumn.setCellValueFactory(
                data -> data.getValue().nameProperty()
        );

        admissionNumberColumn.setCellValueFactory(
                data -> data.getValue().admissionNumberProperty()
        );

        classColumn.setCellValueFactory(
                data -> data.getValue().classNameProperty()
        );

        statusColumn.setCellValueFactory(
                data -> data.getValue().statusProperty()
        );

        /*
         * Table action buttons
         */
        actionsColumn.setCellFactory(column ->
                new TableCell<StudentRow, String>() {

                    private final Button editButton =
                            new Button("Edit");

                    private final Button resetPasswordButton =
                            new Button("Reset Password");

                    private final Button deactivateButton =
                            new Button("Deactivate");

                    private final HBox container =
                            new HBox(
                                    8,
                                    editButton,
                                    resetPasswordButton,
                                    deactivateButton
                            );

                    {
                        /*
                         * Apply professional table button styles
                         */
                        editButton.getStyleClass().add(
                                "table-action-button"
                        );

                        resetPasswordButton.getStyleClass().add("table-action-button");

                        deactivateButton.getStyleClass().addAll(
                                "table-action-button",
                                "table-action-danger"
                        );

                        /*
                         * Reset student portal password
                         */
                        resetPasswordButton.setOnAction(event -> {
                            StudentRow student = getTableView().getItems().get(getIndex());
                            handleResetStudentPassword(student);
                        });

                        /*
                         * Edit student
                         */
                        editButton.setOnAction(event -> {

                            StudentRow student =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            handleEditStudent(student);
                        });

                        /*
                         * Deactivate student
                         */
                        deactivateButton.setOnAction(event -> {

                            StudentRow student =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            handleDeactivateStudent(student);
                        });
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty
                    ) {

                        super.updateItem(item, empty);

                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(container);
                        }
                    }
                }
        );

        studentsTable.setItems(allStudents);
        loadStudentsFromBackend();

        /*
         * Search
         */
        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filterStudents(newValue)
        );
    }

    private void loadStudentsFromBackend() {
        TaskRunner.run(
                () -> adminStudentApi.getStudents(0, 100, ""),
                result -> {
                    List<StudentRow> students = result.data().stream()
                            .map(this::toStudentRow)
                            .toList();
                    allStudents.setAll(students);
                    filterStudents(searchField.getText());
                },
                error -> {
                    log.error("Failed to load students", error);
                    Toast.error(errorMessage(error));
                }
        );
    }

    private StudentRow toStudentRow(StudentResponse student) {
        return new StudentRow(
                student.id(),
                student.fullName(),
                student.admissionNumber(),
                student.currentClass(),
                student.status(),
                student.currentClassId(),
                student.gender(),
                student.dateOfBirth(),
                student.currentStreamId()
        );
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "Unable to load students."
                : error.getMessage();
    }

    /*
     * Search students
     */
    private void filterStudents(String searchText) {

        String search =
                searchText.toLowerCase().trim();

        if (search.isEmpty()) {

            studentsTable.setItems(allStudents);

            return;
        }

        ObservableList<StudentRow> filteredStudents =
                FXCollections.observableArrayList();

        for (StudentRow student : allStudents) {

            if (student.getName()
                    .toLowerCase()
                    .contains(search)

                    || student.getAdmissionNumber()
                    .toLowerCase()
                    .contains(search)

                    || student.getClassName()
                    .toLowerCase()
                    .contains(search)

                    || student.getStatus()
                    .toLowerCase()
                    .contains(search)) {

                filteredStudents.add(student);
            }
        }

        studentsTable.setItems(filteredStudents);
    }

    @FXML
    private void provisionStudentAccounts() {
        if (!Modal.confirm("Create portal login accounts for active students who do not have one? Temporary passwords will be shown once. Copy and store them securely.")) {
            return;
        }
        TaskRunner.run(
                adminStudentApi::provisionStudentAccounts,
                this::showProvisionedCredentials,
                error -> {
                    log.error("Failed to provision student portal accounts", error);
                    Toast.error(errorMessage(error));
                }
        );
    }

    private void handleResetStudentPassword(StudentRow student) {
        if (!Modal.confirm("Generate a new temporary portal password for "
                + student.getName() + "? Their current password will stop working immediately.")) {
            return;
        }
        TaskRunner.run(
                () -> adminStudentApi.resetStudentPassword(student.getId()),
                this::showResetCredential,
                error -> {
                    log.error("Failed to reset student portal password for student {}", student.getId(), error);
                    Toast.error(errorMessage(error));
                }
        );
    }

    private void showResetCredential(StudentAccountCredentialResponse credential) {
        if (credential == null) {
            Toast.error("The server did not return the new student credential.");
            return;
        }
        String output = "STUDENT PORTAL TEMPORARY LOGIN\n\n"
                + "Student: " + credential.fullName() + "\n"
                + "Admission Number: " + credential.admissionNumber() + "\n"
                + "Username: " + credential.username() + "\n"
                + "Temporary Password: " + credential.temporaryPassword() + "\n\n"
                + "Give this password only to the student. It is shown once.";
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Password Reset");
        alert.setHeaderText("Temporary password generated");
        alert.setContentText("Copy and securely share this credential before closing this window.");
        TextArea details = new TextArea(output);
        details.setEditable(false);
        details.setWrapText(false);
        details.setPrefRowCount(8);
        details.setPrefColumnCount(65);
        alert.getDialogPane().setExpandableContent(details);
        alert.getDialogPane().setExpanded(true);
        ButtonType copyButton = new ButtonType("Copy credential", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(copyButton);
        alert.showAndWait().filter(copyButton::equals).ifPresent(ignored -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(output);
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            Toast.success("Student credential copied to clipboard");
        });
    }

    private void showProvisionedCredentials(StudentAccountProvisioningResponse result) {
        if (result == null || result.createdAccounts().isEmpty()) {
            Toast.success("No new accounts created. " + (result == null ? "" : result.alreadyLinkedCount() + " students already have accounts."));
            return;
        }
        StringBuilder output = new StringBuilder("STUDENT PORTAL LOGIN CREDENTIALS\n")
                .append("Distribute each credential only to its student and store this list securely.\n\n")
                .append("Student\tAdmission Number\tUsername\tTemporary Password\n");
        for (StudentAccountCredentialResponse credential : result.createdAccounts()) {
            output.append(credential.fullName()).append('\t')
                    .append(credential.admissionNumber()).append('\t')
                    .append(credential.username()).append('\t')
                    .append(credential.temporaryPassword()).append('\n');
        }
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Student Portal Accounts Created");
        alert.setHeaderText(result.createdCount() + " student login account(s) created");
        alert.setContentText("Temporary passwords are shown only in this window. Copy and store them securely before closing. Already linked: "
                + result.alreadyLinkedCount() + ". Skipped due to account conflicts: " + result.skippedCount() + ".");
        TextArea credentials = new TextArea(output.toString());
        credentials.setEditable(false);
        credentials.setWrapText(false);
        credentials.setPrefRowCount(Math.min(20, result.createdCount() + 4));
        credentials.setPrefColumnCount(90);
        alert.getDialogPane().setExpandableContent(credentials);
        alert.getDialogPane().setExpanded(true);
        ButtonType copyButton = new ButtonType("Copy credentials", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(copyButton);
        alert.showAndWait().filter(copyButton::equals).ifPresent(ignored -> {
            ClipboardContent clipboardContent = new ClipboardContent();
            clipboardContent.putString(output.toString());
            Clipboard.getSystemClipboard().setContent(clipboardContent);
            Toast.success("Student credentials copied to clipboard");
        });
        if (!result.skippedAdmissions().isEmpty()) {
            log.warn("Some student accounts were skipped due to username conflicts: {}", result.skippedAdmissions());
        }
    }
    /*
     * Add Student
     */
    @FXML
    private void showAddStudentDialog() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/add-student.fxml"
                    )
            );

            Parent root = loader.load();

            AddStudentController controller =
                    loader.getController();

            Stage dialog = new Stage();

            dialog.setTitle("Add Student");

            dialog.initModality(
                    Modality.APPLICATION_MODAL
            );

            /*
             * Important:
             * Use a styled Scene so the dialog receives
             * the application's CSS files.
             */
            dialog.setScene(
                    createStyledScene(root)
            );

            dialog.setResizable(false);

            dialog.showAndWait();
            if (controller.isStudentCreated()) {
                loadStudentsFromBackend();
            }

        } catch (Exception e) {
            log.error("Unable to open add-student dialog", e);
            Toast.error(errorMessage(e));
        }
    }

    /*
     * Edit Student
     */
    private void handleEditStudent(
            StudentRow student
    ) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/edit-student.fxml"
                    )
            );

            Parent root = loader.load();

            EditStudentController controller =
                    loader.getController();

            controller.setStudentRow(student);

            Stage dialog = new Stage();

            dialog.setTitle("Edit Student");

            dialog.initModality(
                    Modality.APPLICATION_MODAL
            );

            /*
             * Important:
             * Use the same styled Scene for Edit Student.
             */
            dialog.setScene(
                    createStyledScene(root)
            );

            dialog.setResizable(false);

            dialog.showAndWait();
            if (controller.isSaved()) {
                loadStudentsFromBackend();
            }

        } catch (Exception e) {
            log.error("Unable to open edit-student dialog", e);
            Toast.error(errorMessage(e));
        }
    }

    /*
     * Deactivate Student
     */
    private void handleDeactivateStudent(
            StudentRow student
    ) {

        if (!Modal.confirm("Deactivate " + student.getName() + "?")) {
            return;
        }
        TaskRunner.run(
                () -> {
                    adminStudentApi.deactivateStudent(student.getId());
                    return null;
                },
                ignored -> {
                    Toast.success("Student deactivated");
                    loadStudentsFromBackend();
                },
                error -> {
                    log.error("Failed to deactivate student {}", student.getId(), error);
                    Toast.error(errorMessage(error));
                }
        );
    }

    /*
     * Apply application CSS to dialog windows
     */
    private Scene createStyledScene(Parent root) {

        Scene scene = new Scene(root);

        var styles = getClass()
                .getResource("/css/styles.css");

        var components = getClass()
                .getResource("/css/components.css");

        if (styles != null) {

            scene.getStylesheets().add(
                    styles.toExternalForm()
            );
        }

        if (components != null) {

            scene.getStylesheets().add(
                    components.toExternalForm()
            );
        }

        return scene;
    }

    /*
     * Student row model
     */
    public static class StudentRow {

        private final Long id;

        private final SimpleStringProperty name;

        private final SimpleStringProperty admissionNumber;

        private final SimpleStringProperty className;

        private final SimpleStringProperty status;
        private final Long classId;
        private final String gender;
        private final Object dateOfBirth;
        private final Long currentStreamId;

        public StudentRow(
                Long id,
                String name,
                String admissionNumber,
                String className,
                String status
        ) {
            this(id, name, admissionNumber, className, status, null, null, null, null);
        }

        public StudentRow(Long id, String name, String admissionNumber, String className,
                          String status, Long classId) {
            this(id, name, admissionNumber, className, status, classId, null, null, null);
        }

        public StudentRow(Long id, String name, String admissionNumber, String className,
                          String status, Long classId, String gender, Object dateOfBirth,
                          Long currentStreamId) {

            this.id = id;
            this.name =
                    new SimpleStringProperty(name);

            this.admissionNumber =
                    new SimpleStringProperty(
                            admissionNumber
                    );

            this.className =
                    new SimpleStringProperty(
                            className
                    );

            this.status =
                    new SimpleStringProperty(status);
            this.classId = classId;
            this.gender = gender;
            this.dateOfBirth = dateOfBirth;
            this.currentStreamId = currentStreamId;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public Long getId() {
            return id;
        }

        public SimpleStringProperty admissionNumberProperty() {
            return admissionNumber;
        }

        public SimpleStringProperty classNameProperty() {
            return className;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getName() {
            return name.get();
        }

        public String getAdmissionNumber() {
            return admissionNumber.get();
        }

        public String getClassName() {
            return className.get();
        }

        public String getStatus() {
            return status.get();
        }
        
        public Long getClassId() {
            return classId;
        }

        public String getGender() {
            return gender;
        }

        public Object getDateOfBirth() {
            return dateOfBirth;
        }

        public Long getCurrentStreamId() {
            return currentStreamId;
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public void setAdmissionNumber(
                String admissionNumber
        ) {

            this.admissionNumber.set(
                    admissionNumber
            );
        }

        public void setClassName(
                String className
        ) {

            this.className.set(className);
        }

        public void setStatus(String status) {
            this.status.set(status);
        }
    }

    private Long findClassId(String className) {
        return adminClassApi.getClasses().stream()
                .filter(item -> item.name().equals(className))
                .map(ClassResponse::id)
                .findFirst()
                .orElse(null);
    }
}