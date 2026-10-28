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
import com.musomi.desktop.controller.teacher.AssessmentStore;
import com.musomi.desktop.controller.teacher.AssessmentStore.Assessment;
import com.musomi.desktop.controller.teacher.AssessmentStore.StudentMark;
import com.musomi.desktop.util.GradeFormatter;
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

        List<Assessment> assessments = AssessmentStore.getAssessments();
        List<String> classNames = new ArrayList<>();
        if (assessments != null) {
            assessments.stream()
                    .filter(assessment -> assessment != null
                            && assessment.getClassName() != null
                            && !assessment.getClassName().isBlank())
                    .map(assessment -> assessment.getClassName().trim())
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .forEach(classNames::add);
        }
        classComboBox.setItems(FXCollections.observableArrayList(classNames));
        classComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldClass, newClass) -> refreshReport(newClass));

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

        List<Assessment> assessments = AssessmentStore.getAssessments();
        if (assessments == null) {
            assessments = List.of();
        }
        List<Assessment> publishedAssessments = assessments.stream()
                .filter(assessment -> assessment != null
                        && assessment.getClassName() != null
                        && className.equals(assessment.getClassName().trim())
                        && "Published".equals(assessment.getStatus()))
                .toList();
        if (publishedAssessments.isEmpty()) {
            showEmpty("No published assessments",
                    "Publish marks for this class to see the report.");
            return;
        }

        Map<String, List<BigDecimal>> scoresByStudent = new LinkedHashMap<>();
        Map<String, BigDecimal> percentageTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> scoreTotals = new LinkedHashMap<>();
        Map<String, BigDecimal> maxTotals = new LinkedHashMap<>();
        BigDecimal classPercentageTotal = BigDecimal.ZERO;
        long classMarkCount = 0;

        for (Assessment assessment : publishedAssessments) {
            List<StudentMark> marks = AssessmentStore.getMarks(assessment);
            if (marks == null) {
                continue;
            }
            int maxMarks = assessment.getMaxMarks();
            BigDecimal maxScore = BigDecimal.valueOf(maxMarks);
            for (StudentMark mark : marks) {
                if (mark == null || mark.getStudent() == null || mark.getStudent().isBlank()) {
                    continue;
                }
                String studentName = mark.getStudent().trim();
                scoresByStudent.computeIfAbsent(studentName, ignored -> new ArrayList<>());
                if (!mark.hasValidMark(maxMarks)) {
                    continue;
                }
                try {
                    BigDecimal score = new BigDecimal(mark.getMark().trim());
                    BigDecimal percentage = score.multiply(HUNDRED)
                            .divide(maxScore, 12, RoundingMode.HALF_UP);
                    scoresByStudent.get(studentName).add(score);
                    percentageTotals.merge(studentName, percentage, BigDecimal::add);
                    scoreTotals.merge(studentName, score, BigDecimal::add);
                    maxTotals.merge(studentName, maxScore, BigDecimal::add);
                    classPercentageTotal = classPercentageTotal.add(percentage);
                    classMarkCount++;
                } catch (NumberFormatException exception) {
                    log.debug("Ignoring unparseable mark for student {}", studentName, exception);
                }
            }
        }

        List<ReportRow> rows = new ArrayList<>();
        for (Map.Entry<String, List<BigDecimal>> entry : scoresByStudent.entrySet()) {
            String studentName = entry.getKey();
            int count = entry.getValue().size();
            String averageDisplay = "—";
            String gradeDisplay = "—";
            if (count > 0) {
                BigDecimal average = percentageTotals.get(studentName)
                        .divide(BigDecimal.valueOf(count), 1, RoundingMode.HALF_UP);
                averageDisplay = formatPercent(average);
                String grade = GradeFormatter.from(
                        scoreTotals.get(studentName), maxTotals.get(studentName));
                if (grade != null) {
                    gradeDisplay = grade;
                }
            }
            rows.add(new ReportRow(studentName, count, averageDisplay, gradeDisplay));
        }
        rows.sort(Comparator.comparing(ReportRow::getStudentName, String.CASE_INSENSITIVE_ORDER));

        if (rows.isEmpty()) {
            showEmpty("No marks entered",
                    "Students in this class have no valid marks yet.");
            return;
        }
        reportTable.setItems(FXCollections.observableArrayList(rows));
        studentCountLabel.setText(Integer.toString(rows.size()));
        classAverageLabel.setText(classMarkCount == 0 ? "—" : formatPercent(
                classPercentageTotal.divide(BigDecimal.valueOf(classMarkCount), 1, RoundingMode.HALF_UP)));
        reportTable.setVisible(true);
        reportTable.setManaged(true);
        emptyState.setVisible(false);
        emptyState.setManaged(false);
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
