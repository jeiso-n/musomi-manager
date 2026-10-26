package com.musomi.desktop.controller.admin;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.api.ApiException;
import com.musomi.desktop.model.dto.UserResponse;
import com.musomi.desktop.util.TaskRunner;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.StringConverter;

public class TeacherAssignmentController {

    @FXML
    private ComboBox<UserResponse> teacherComboBox;

    @FXML
    private ComboBox<ClassResponse> classComboBox;

    @FXML
    private ComboBox<SubjectResponse> subjectComboBox;

    @FXML
    private Label messageLabel;

    @FXML
    private Label totalAssignmentsLabel;

    @FXML
    private Label assignedTeachersLabel;

    @FXML
    private TableView<Assignment> assignmentsTable;

    @FXML
    private TableColumn<Assignment, String> teacherColumn;

    @FXML
    private TableColumn<Assignment, String> classColumn;

    @FXML
    private TableColumn<Assignment, String> subjectColumn;

    private final ObservableList<Assignment> assignments =
            FXCollections.observableArrayList();
    private final ApiClient apiClient = ApiClient.getInstance();

    @FXML
    private void initialize() {
        teacherComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(UserResponse teacher) {
                return teacher == null ? "" : teacher.fullName();
            }

            @Override
            public UserResponse fromString(String value) {
                return null;
            }
        });
        classComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(ClassResponse classResponse) {
                return classResponse == null ? "" : classResponse.name();
            }

            @Override
            public ClassResponse fromString(String value) {
                return null;
            }
        });
        subjectComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(SubjectResponse subject) {
                return subject == null ? "" : subject.name();
            }

            @Override
            public SubjectResponse fromString(String value) {
                return null;
            }
        });

        teacherColumn.setCellValueFactory(
                new PropertyValueFactory<>("teacherName")
        );

        classColumn.setCellValueFactory(
                new PropertyValueFactory<>("className")
        );

        subjectColumn.setCellValueFactory(
                new PropertyValueFactory<>("subjectName")
        );

        assignmentsTable.setItems(assignments);

        assignmentsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        updateAssignmentStatistics();

        messageLabel.setText(
                "Select a teacher, class and subject."
        );

        loadDropdownData();
        loadAssignments();
    }

    @FXML
    private void assignTeacher() {
        UserResponse teacher = teacherComboBox.getValue();
        ClassResponse classResponse = classComboBox.getValue();
        SubjectResponse subject = subjectComboBox.getValue();

        if (teacher == null || classResponse == null || subject == null) {
            messageLabel.setText(
                    "Please select a teacher, class and subject."
            );
            return;
        }

        if (classResponse.academicYearId() == null) {
            messageLabel.setText(
                    "The selected class has no academic year."
            );
            return;
        }

        CreateAssignmentRequest request = new CreateAssignmentRequest(
                teacher.id(),
                subject.id(),
                classResponse.id(),
                null,
                classResponse.academicYearId()
        );

        TaskRunner.run(
                () -> apiClient.post(
                        "/admin/assignments",
                        request,
                        AssignmentResponse.class
                ),
                result -> {
                    messageLabel.setText("Teacher assigned successfully.");
                    clearSelections();
                    loadAssignments();
                },
                this::showApiError
        );
    }

    private void updateAssignmentStatistics() {
        totalAssignmentsLabel.setText(
                String.valueOf(assignments.size())
        );

        long distinctTeachers = assignments.stream()
                .map(Assignment::getTeacherName)
                .distinct()
                .count();

        assignedTeachersLabel.setText(
                String.valueOf(distinctTeachers)
        );
    }


    @FXML
    private void clearForm() {
        clearSelections();

        messageLabel.setText(
                "Select a teacher, class and subject."
        );
    }

    private void clearSelections() {
        teacherComboBox.getSelectionModel().clearSelection();
        classComboBox.getSelectionModel().clearSelection();
        subjectComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    private void refreshAssignments() {
        loadAssignments();
    }

    private void loadDropdownData() {
        TaskRunner.run(
                () -> {
                    List<UserResponse> teachers = fetchTeachers();
                    ClassResponse[] classes = apiClient.get(
                            "/admin/classes",
                            ClassResponse[].class
                    );
                    SubjectResponse[] subjects = apiClient.get(
                            "/admin/subjects",
                            SubjectResponse[].class
                    );
                    return new DropdownData(
                            teachers,
                            classes == null ? List.of() : List.of(classes),
                            subjects == null ? List.of() : List.of(subjects)
                    );
                },
                data -> {
                    teacherComboBox.setItems(FXCollections.observableArrayList(data.teachers()));
                    classComboBox.setItems(FXCollections.observableArrayList(data.classes()));
                    subjectComboBox.setItems(FXCollections.observableArrayList(data.subjects()));
                },
                this::showApiError
        );
    }

    private List<UserResponse> fetchTeachers() {
        List<UserResponse> teachers = new ArrayList<>();
        UserResponse[] response = apiClient.get(
                "/teacher/teachers", UserResponse[].class);
        if (response == null) {
            return teachers;
        }
        teachers.addAll(List.of(response));
        return teachers;
    }

    private void loadAssignments() {
        TaskRunner.run(
                () -> {
                    AssignmentResponse[] result = apiClient.get(
                            "/admin/assignments",
                            AssignmentResponse[].class
                    );
                    if (result == null) {
                        return List.<Assignment>of();
                    }
                    return List.of(result).stream()
                            .map(Assignment::new)
                            .toList();
                },
                result -> {
                    assignments.setAll(result);
                    assignmentsTable.refresh();
                    updateAssignmentStatistics();
                },
                this::showApiError
        );
    }

    private void showApiError(Exception error) {
        if (error instanceof ApiException apiException
                && "NETWORK_ERROR".equals(apiException.getCode())) {
            messageLabel.setText("Failed to connect to server.");
            return;
        }
        String message = error.getMessage();
        messageLabel.setText(message == null || message.isBlank()
                ? "The request failed. Please try again."
                : message);
    }

    @FXML
    private void removeAssignment() {
        Assignment selected =
                assignmentsTable.getSelectionModel().getSelectedItem();

        if (selected == null) {
            messageLabel.setText(
                    "Please click an assignment row in the table first."
            );
            return;
        }

        if (selected.getId() == null) {
            messageLabel.setText("The selected assignment has no identifier.");
            return;
        }

        TaskRunner.run(
                () -> {
                    apiClient.delete("/admin/assignments/" + selected.getId());
                    return null;
                },
                result -> {
                    assignments.remove(selected);
                    assignmentsTable.refresh();
                    updateAssignmentStatistics();
                    messageLabel.setText("Assignment removed successfully.");
                    loadAssignments();
                },
                this::showApiError
        );
    }

    public static class Assignment {

        private final Long id;
        private final String teacherName;
        private final String className;
        private final String subjectName;
        private final Integer academicYear;

        public Assignment(AssignmentResponse response) {
            this.id = response.id();
            this.teacherName = response.teacherName();
            this.className = response.className();
            this.subjectName = response.subjectName();
            this.academicYear = response.academicYear();
        }

        public Long getId() {
            return id;
        }

        public String getTeacherName() {
            return teacherName;
        }

        public String getClassName() {
            return className;
        }

        public String getSubjectName() {
            return subjectName;
        }

        public Integer getAcademicYear() {
            return academicYear;
        }
    }

    private record DropdownData(
            List<UserResponse> teachers,
            List<ClassResponse> classes,
            List<SubjectResponse> subjects
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record UserPageResponse(
            List<UserResponse> data,
            int page,
            int size,
            long total,
            int totalPages
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ClassResponse(
            Long id,
            String name,
            Long academicYearId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SubjectResponse(
            Long id,
            String name
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AssignmentResponse(
            Long id,
            String teacherName,
            String className,
            String subjectName,
            Integer academicYear
    ) {
    }

    private record CreateAssignmentRequest(
            Long teacherId,
            Long subjectId,
            Long classId,
            Long streamId,
            Long academicYearId
    ) {
    }
}
