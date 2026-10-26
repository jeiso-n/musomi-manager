package com.musomi.desktop.controller.report;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.musomi.desktop.api.TeacherAssessmentApi;
import com.musomi.desktop.model.AssessmentResponse;
import com.musomi.desktop.util.GradeFormatter;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class ReportPreviewController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReportPreviewController.class);
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    @FXML private ComboBox<String> classComboBox;
    @FXML private Label studentCountLabel;
    @FXML private Label classAverageLabel;
    @FXML private TableView<ReportRow> reportTable;
    @FXML private TableColumn<ReportRow, String> studentNameCol;
    @FXML private TableColumn<ReportRow, String> countCol;
    @FXML private TableColumn<ReportRow, String> averageCol;
    @FXML private TableColumn<ReportRow, String> gradeCol;
    @FXML private VBox emptyState;
    @FXML private Label emptyStateTitle;
    @FXML private Label emptyStateMessage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        countCol.setCellValueFactory(new PropertyValueFactory<>("assessmentsCounted"));
        averageCol.setCellValueFactory(new PropertyValueFactory<>("averageDisplay"));
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("gradeDisplay"));
        setCellAlignment(countCol, Pos.CENTER_RIGHT);
        setCellAlignment(averageCol, Pos.CENTER_RIGHT);
        setCellAlignment(gradeCol, Pos.CENTER);
        reportTable.setEditable(false);
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        classComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldClass, newClass) -> refreshReport(newClass));
        TaskRunner.run(
                () -> new TeacherAssessmentApi().getMyAssessments(),
                this::populateAssessments,
                error -> {
                    log.error("Unable to load teacher assessments for reports", error);
                    classComboBox.getItems().clear();
                    showEmpty("Reports unavailable", "Unable to load your assessments.");
                    Toast.error("Unable to load assessments.");
                });
    }

    private void populateAssessments(List<AssessmentResponse> assessments) {
        List<String> classNames = new ArrayList<>();
        if (assessments != null) {
            assessments.stream()
                    .filter(item -> item != null && item.className() != null
                            && !item.className().isBlank())
                    .map(item -> item.className().trim())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(classNames::add);
        }
        classComboBox.setItems(FXCollections.observableArrayList(classNames));
        if (classNames.isEmpty()) {
            showEmpty("No assessments yet",
                    "Create an assessment first, then return to this screen.");
        } else {
            classComboBox.getSelectionModel().selectFirst();
        }
    }

    private void refreshReport(String className) {
        log.debug("Refreshing local report for class {}", className);
        try {
            updateReport(className);
        } catch (RuntimeException exception) {
            log.error("Unable to build local report for class {}", className, exception);
            showEmpty("Report unavailable", "An unexpected error occurred while building this report.");
            Toast.error("Unable to build the class report.");
        }
    }

    private void updateReport(String className) {
        if (className == null || className.isBlank()) {
            showEmpty("No class selected", "Select a class to see its report.");
            return;
        }

        showEmpty("Report data unavailable",
                "Assessment selection is loaded from the server. Report marks are not available yet.");
    }

    private void showEmpty(String title, String message) {
        reportTable.setVisible(false);
        reportTable.setManaged(false);
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        emptyStateTitle.setText(title);
        emptyStateMessage.setText(message);
        studentCountLabel.setText("0");
        classAverageLabel.setText("—");
    }

    private static String formatPercent(BigDecimal value) {
        return value.setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    private static void setCellAlignment(
            TableColumn<ReportRow, String> column, Pos alignment) {
        column.setCellFactory(ignored -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setAlignment(alignment);
            }
        });
    }

    public static class ReportRow {
        private final String studentName;
        private final int assessmentsCounted;
        private final String averageDisplay;
        private final String gradeDisplay;

        public ReportRow(String studentName, int assessmentsCounted,
                         String averageDisplay, String gradeDisplay) {
            this.studentName = studentName;
            this.assessmentsCounted = assessmentsCounted;
            this.averageDisplay = averageDisplay;
            this.gradeDisplay = gradeDisplay;
        }

        public String getStudentName() {
            return studentName;
        }

        public String getAssessmentsCounted() {
            return Integer.toString(assessmentsCounted);
        }

        public String getAverageDisplay() {
            return averageDisplay;
        }

        public String getGradeDisplay() {
            return gradeDisplay;
        }
    }
}
