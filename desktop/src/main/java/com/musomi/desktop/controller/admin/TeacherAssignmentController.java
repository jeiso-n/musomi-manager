package com.musomi.desktop.controller.admin;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

public class TeacherAssignmentController {

    @FXML
    private ComboBox<String> teacherComboBox;

    @FXML
    private ComboBox<String> classComboBox;

    @FXML
    private ComboBox<String> subjectComboBox;

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

    @FXML
    private void initialize() {
        teacherComboBox.setItems(
                FXCollections.observableArrayList(
                        "John Doe",
                        "Mary Smith",
                        "Peter Brown"
                )
        );

        classComboBox.setItems(
                FXCollections.observableArrayList(
                        "Senior One",
                        "Senior Two",
                        "Senior Three",
                        "Senior Four"
                )
        );

        subjectComboBox.setItems(
                FXCollections.observableArrayList(
                        "Mathematics",
                        "English",
                        "Biology",
                        "Chemistry",
                        "Physics"
                )
        );

        teacherColumn.setCellValueFactory(
                new PropertyValueFactory<>("teacher")
        );

        classColumn.setCellValueFactory(
                new PropertyValueFactory<>("className")
        );

        subjectColumn.setCellValueFactory(
                new PropertyValueFactory<>("subject")
        );

        assignmentsTable.setItems(assignments);

        assignmentsTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );


        updateAssignmentStatistics();


        messageLabel.setText(
                "Select a teacher, class and subject."
        );
    }

    @FXML
    private void assignTeacher() {
        String teacher = teacherComboBox.getValue();
        String className = classComboBox.getValue();
        String subject = subjectComboBox.getValue();

        if (teacher == null || className == null || subject == null) {
            messageLabel.setText(
                    "Please select a teacher, class and subject."
            );
            return;
        }

        boolean duplicate = assignments.stream().anyMatch(assignment ->
                assignment.getTeacher().equals(teacher)
                        && assignment.getClassName().equals(className)
                        && assignment.getSubject().equals(subject)
        );

        if (duplicate) {
            messageLabel.setText(
                    "This assignment already exists."
            );
            return;
        }

        assignments.add(
                new Assignment(teacher, className, subject)
        );

        updateAssignmentStatistics();

        messageLabel.setText(
                "Assignment added. It is temporary and not saved to the backend."
        );

        clearSelections();
    }


    private void updateAssignmentStatistics() {
        totalAssignmentsLabel.setText(
                String.valueOf(assignments.size())
        );

        long distinctTeachers = assignments.stream()
                .map(Assignment::getTeacher)
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
        assignmentsTable.refresh();

        messageLabel.setText(
                "Assignments list refreshed."
        );
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

        boolean removed = assignments.remove(selected);

        if (removed) {
            assignmentsTable.refresh();
            messageLabel.setText("Assignment removed successfully.");
        } else {
            messageLabel.setText("Could not remove the selected assignment.");
        }
        updateAssignmentStatistics();
    }



    public static class Assignment {

        private final String teacher;
        private final String className;
        private final String subject;

        public Assignment(
                String teacher,
                String className,
                String subject
        ) {
            this.teacher = teacher;
            this.className = className;
            this.subject = subject;
        }

        public String getTeacher() {
            return teacher;
        }

        public String getClassName() {
            return className;
        }

        public String getSubject() {
            return subject;
        }
    }
}
