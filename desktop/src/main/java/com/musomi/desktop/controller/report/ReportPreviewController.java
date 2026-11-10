package com.musomi.desktop.controller.report;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ResourceBundle;
import java.net.URL;

import com.musomi.desktop.api.AdminClassApi;
import com.musomi.desktop.api.AdminSettingsApi;
import com.musomi.desktop.api.TeacherClassApi;
import com.musomi.desktop.config.Session;
import com.musomi.desktop.model.ClassPerformanceSummaryResponse;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.model.ReportResponse;
import com.musomi.desktop.model.TermResponse;
import com.musomi.desktop.model.dto.TeacherClassResponse;
import com.musomi.desktop.service.ReportService;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** JavaFX presentation for authenticated report generation and class performance summaries. */
public class ReportPreviewController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(ReportPreviewController.class);

    @FXML private ComboBox<ClassOption> classComboBox;
    @FXML private ComboBox<StreamOption> streamComboBox;
    @FXML private ComboBox<TermResponse> termComboBox;
    @FXML private Button generateButton;
    @FXML private Label studentCountLabel;
    @FXML private Label completeCountLabel;
    @FXML private Label partialCountLabel;
    @FXML private Label noResultsCountLabel;
    @FXML private Label classAverageLabel;
    @FXML private Label gradeDistributionLabel;
    @FXML private TableView<ReportRow> reportTable;
    @FXML private TableColumn<ReportRow, String> studentNameCol;
    @FXML private TableColumn<ReportRow, String> statusCol;
    @FXML private TableColumn<ReportRow, String> completenessCol;
    @FXML private TableColumn<ReportRow, String> averageCol;
    @FXML private TableColumn<ReportRow, String> gradeCol;
    @FXML private TableColumn<ReportRow, String> positionCol;
    @FXML private TableColumn<ReportRow, Void> downloadCol;
    @FXML private TableView<SubjectRow> subjectTable;
    @FXML private TableColumn<SubjectRow, String> subjectNameCol;
    @FXML private TableColumn<SubjectRow, String> subjectAverageCol;
    @FXML private TableColumn<SubjectRow, String> studentsWithMarksCol;
    @FXML private TableColumn<SubjectRow, String> studentsMissingMarksCol;
    @FXML private TableColumn<SubjectRow, String> subjectCompletenessCol;
    @FXML private Label emptyStateTitle;
    @FXML private Label emptyStateMessage;

    private final AdminClassApi classApi = new AdminClassApi();
    private final AdminSettingsApi settingsApi = AdminSettingsApi.getInstance();
    private final TeacherClassApi teacherClassApi = new TeacherClassApi();
    private final ReportService reportService = new ReportService();
    private List<TeacherClassResponse> teacherAssignments = List.of();

    @Override
    public void initialize(URL url, ResourceBundle resources) {
        configureTables();
        generateButton.setDisable(true);
        classComboBox.setDisable(true);
        streamComboBox.setDisable(true);
        termComboBox.setDisable(true);
        classComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateStreamOptions(newValue));
        streamComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateGenerateAvailability());
        termComboBox.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> updateGenerateAvailability());

        loadReferenceData();
    }

    private void configureTables() {
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        completenessCol.setCellValueFactory(new PropertyValueFactory<>("completeness"));
        averageCol.setCellValueFactory(new PropertyValueFactory<>("average"));
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        positionCol.setCellValueFactory(new PropertyValueFactory<>("position"));
        reportTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        reportTable.setEditable(false);
        downloadCol.setCellFactory(column -> new TableCell<>() {
            private final Button downloadButton = new Button("Download PDF");
            {
                downloadButton.getStyleClass().add("table-action-button");
                downloadButton.setOnAction(event -> {
                    ReportRow row = getTableView().getItems().get(getIndex());
                    if (row != null) {
                        saveReportPdf(row.report());
                    }
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : downloadButton);
            }
        });

        subjectNameCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        subjectAverageCol.setCellValueFactory(new PropertyValueFactory<>("average"));
        studentsWithMarksCol.setCellValueFactory(new PropertyValueFactory<>("studentsWithMarks"));
        studentsMissingMarksCol.setCellValueFactory(new PropertyValueFactory<>("studentsMissingMarks"));
        subjectCompletenessCol.setCellValueFactory(new PropertyValueFactory<>("completeness"));
        subjectTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        subjectTable.setEditable(false);
    }

    private void loadReferenceData() {
        boolean teacher = isTeacher();
        TaskRunner.run(() -> {
            List<ClassResponse> classes = classApi.getClasses();
            List<TermResponse> terms = settingsApi.getTerms();
            List<TeacherClassResponse> assignments = teacher
                    ? teacherClassApi.getMyClasses() : List.of();
            return new ReferenceData(classes, terms, assignments);
        }, data -> {
            teacherAssignments = data.assignments();
            List<ClassResponse> availableClasses = data.classes().stream()
                    .filter(Objects::nonNull)
                    .filter(ClassResponse::isActive)
                    .filter(item -> !teacher || teacherAssignments.stream()
                            .anyMatch(assignment -> Objects.equals(assignment.classId(), item.id())))
                    .sorted(Comparator.comparing(ClassResponse::name,
                            Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                    .toList();
            classComboBox.setItems(FXCollections.observableArrayList(
                    availableClasses.stream().map(ClassOption::new).toList()));
            termComboBox.setItems(FXCollections.observableArrayList(
                    data.terms().stream().filter(Objects::nonNull)
                            .sorted(Comparator.comparing(TermResponse::isCurrent,
                                    Comparator.nullsLast(Comparator.reverseOrder()))
                                    .thenComparing(TermResponse::startDate,
                                            Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList()));

            classComboBox.setDisable(false);
            termComboBox.setDisable(false);
            if (!termComboBox.getItems().isEmpty()) {
                TermResponse current = termComboBox.getItems().stream()
                        .filter(term -> Boolean.TRUE.equals(term.isCurrent()))
                        .findFirst().orElse(termComboBox.getItems().get(0));
                termComboBox.getSelectionModel().select(current);
            }
            if (!classComboBox.getItems().isEmpty()) {
                classComboBox.getSelectionModel().selectFirst();
            } else {
                showEmpty("No classes available",
                        teacher ? "No classes are currently assigned to your teacher account."
                                : "Create or activate a class before generating reports.");
            }
            if (termComboBox.getItems().isEmpty()) {
                showEmpty("No terms configured", "Create a school term before generating reports.");
            }
            updateGenerateAvailability();
        }, error -> {
            log.error("Unable to load report selectors", error);
            showEmpty("Reports unavailable", errorMessage(error));
            Toast.error("Unable to load classes and terms.");
        });
    }

    private void updateStreamOptions(ClassOption selectedClass) {
        if (selectedClass == null) {
            streamComboBox.getItems().clear();
            streamComboBox.setDisable(true);
            updateGenerateAvailability();
            return;
        }

        List<StreamOption> options;
        if (isTeacher()) {
            Map<String, StreamOption> unique = new LinkedHashMap<>();
            teacherAssignments.stream()
                    .filter(item -> Objects.equals(item.classId(), selectedClass.id()))
                    .forEach(item -> {
                        StreamOption option = new StreamOption(item.streamId(),
                                item.streamName() == null || item.streamName().isBlank()
                                        ? "All streams" : item.streamName());
                        unique.putIfAbsent(item.streamId() == null ? "ALL" : item.streamId().toString(), option);
                    });
            options = new ArrayList<>(unique.values());
        } else {
            options = List.of(new StreamOption(null, "All streams"));
        }

        streamComboBox.setItems(FXCollections.observableArrayList(options));
        streamComboBox.setDisable(options.isEmpty());
        if (!options.isEmpty()) {
            streamComboBox.getSelectionModel().selectFirst();
        }
        updateGenerateAvailability();
    }

    private void updateGenerateAvailability() {
        generateButton.setDisable(classComboBox.getValue() == null
                || streamComboBox.getValue() == null
                || termComboBox.getValue() == null);
    }

    @FXML
    private void handleGenerateReports() {
        ClassOption selectedClass = classComboBox.getValue();
        StreamOption selectedStream = streamComboBox.getValue();
        TermResponse selectedTerm = termComboBox.getValue();
        if (selectedClass == null || selectedStream == null || selectedTerm == null) {
            Toast.warning("Select a class, stream and term first.");
            return;
        }

        generateButton.setDisable(true);
        generateButton.setText("Generating...");
        showEmpty("Generating reports", "The server is calculating report cards and saving result snapshots.");
        TaskRunner.run(() -> {
            List<ReportResponse> reports = reportService.generateReports(
                    selectedClass.id(), selectedStream.id(), selectedTerm.id());
            ClassPerformanceSummaryResponse summary = reportService.getClassSummary(
                    selectedClass.id(), selectedStream.id(), selectedTerm.id());
            return new GeneratedData(reports, summary);
        }, data -> {
            generateButton.setText("Generate / Refresh Reports");
            updateGenerateAvailability();
            showSummary(data.summary());
            reportTable.setItems(FXCollections.observableArrayList(
                    data.reports().stream().map(ReportRow::new).toList()));
            reportTable.setVisible(!data.reports().isEmpty());
            reportTable.setManaged(!data.reports().isEmpty());
            if (data.reports().isEmpty()) {
                emptyStateTitle.setText("No student reports generated");
                emptyStateMessage.setText("There may be no active students in this class or stream.");
                emptyStateTitle.setVisible(true);
                emptyStateMessage.setVisible(true);
            } else {
                emptyStateTitle.setVisible(false);
                emptyStateMessage.setVisible(false);
            }
            Toast.success("Generated " + data.reports().size() + " student report(s).");
        }, error -> {
            log.error("Report generation failed", error);
            generateButton.setText("Generate / Refresh Reports");
            updateGenerateAvailability();
            showEmpty("Report generation failed", errorMessage(error));
            Toast.error(errorMessage(error));
        });
    }

    private void showSummary(ClassPerformanceSummaryResponse summary) {
        if (summary == null) {
            showEmpty("Summary unavailable", "The server returned no class summary.");
            return;
        }
        studentCountLabel.setText(Integer.toString(summary.totalStudents()));
        completeCountLabel.setText(Integer.toString(summary.completeReports()));
        partialCountLabel.setText(Integer.toString(summary.partialReports()));
        noResultsCountLabel.setText(Integer.toString(summary.noResultsReports()));
        classAverageLabel.setText(summary.classAverage() == null
                ? "N/A" : formatAverage(summary.classAverage()) + "%");
        gradeDistributionLabel.setText(formatGradeDistribution(summary.gradeDistribution()));

        List<SubjectRow> subjects = summary.subjects() == null ? List.of()
                : summary.subjects().stream().map(SubjectRow::new).toList();
        subjectTable.setItems(FXCollections.observableArrayList(subjects));
    }

    private void saveReportPdf(ReportResponse report) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save student report PDF");
        String safeName = report.studentName() == null ? "student"
                : report.studentName().replaceAll("[^A-Za-z0-9._-]", "_");
        chooser.setInitialFileName("report-" + safeName + "-" + report.id() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));
        Window owner = reportTable.getScene() == null ? null : reportTable.getScene().getWindow();
        java.io.File destination = chooser.showSaveDialog(owner);
        if (destination == null) {
            return;
        }

        TaskRunner.run(() -> {
            byte[] pdf = reportService.downloadPdf(report.id());
            Files.write(destination.toPath(), pdf);
            return destination.toPath();
        }, savedPath -> Toast.success("PDF saved: " + savedPath.getFileName()),
                error -> {
                    log.error("Unable to download report {}", report.id(), error);
                    Toast.error("Unable to download PDF: " + errorMessage(error));
                });
    }

    private void showEmpty(String title, String message) {
        reportTable.setVisible(false);
        reportTable.setManaged(false);
        emptyStateTitle.setText(title);
        emptyStateMessage.setText(message);
        emptyStateTitle.setVisible(true);
        emptyStateMessage.setVisible(true);
    }

    private boolean isTeacher() {
        return "TEACHER".equalsIgnoreCase(Session.getInstance().getRole());
    }

    private static String formatAverage(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }

    private static String formatGradeDistribution(Map<String, Integer> distribution) {
        if (distribution == null || distribution.isEmpty()) {
            return "No complete reports yet";
        }
        return List.of("A", "B", "C", "D", "E", "F").stream()
                .map(grade -> grade + ": " + distribution.getOrDefault(grade, 0))
                .reduce((left, right) -> left + "   " + right)
                .orElse("No grade data");
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "An unexpected error occurred. Please try again."
                : error.getMessage();
    }

    private record ReferenceData(List<ClassResponse> classes, List<TermResponse> terms,
                                 List<TeacherClassResponse> assignments) {}
    private record GeneratedData(List<ReportResponse> reports, ClassPerformanceSummaryResponse summary) {}

    public static final class ClassOption {
        private final ClassResponse value;
        private ClassOption(ClassResponse value) { this.value = value; }
        public Long id() { return value.id(); }
        @Override public String toString() { return value.name(); }
    }

    public record StreamOption(Long id, String name) {
        @Override public String toString() { return name; }
    }

    public static final class ReportRow {
        private final ReportResponse report;
        private ReportRow(ReportResponse report) { this.report = report; }
        public ReportResponse report() { return report; }
        public String getStudentName() { return report.studentName() == null ? "—" : report.studentName(); }
        public String getStatus() { return displayStatus(report.reportStatus()); }
        public String getCompleteness() {
            return safeCount(report.recordedAssessmentCount()) + " / "
                    + safeCount(report.expectedAssessmentCount());
        }
        public String getAverage() {
            return report.overallAverage() == null ? "N/A" : formatAverage(report.overallAverage()) + "%";
        }
        public String getGrade() { return report.overallGrade() == null ? "—" : report.overallGrade(); }
        public String getPosition() {
            return report.position() == null ? "Not ranked"
                    : report.position() + " / " + safeCount(report.rankingCohortSize());
        }
    }

    public static final class SubjectRow {
        private final ClassPerformanceSummaryResponse.SubjectPerformance subject;
        private SubjectRow(ClassPerformanceSummaryResponse.SubjectPerformance subject) {
            this.subject = subject;
        }
        public String getSubjectName() { return subject.subjectName() == null ? "—" : subject.subjectName(); }
        public String getAverage() {
            return subject.average() == null ? "N/A" : formatAverage(subject.average()) + "%";
        }
        public String getStudentsWithMarks() { return Integer.toString(subject.studentsWithMarks()); }
        public String getStudentsMissingMarks() { return Integer.toString(subject.studentsMissingMarks()); }
        public String getCompleteness() {
            return subject.recordedAssessmentCount() + " / " + subject.expectedAssessmentCount();
        }
    }

    private static String displayStatus(String status) {
        if (status == null) return "Unknown";
        return switch (status.toUpperCase()) {
            case "COMPLETE" -> "Complete";
            case "PARTIAL" -> "Partial - marks missing";
            case "NO_RESULTS" -> "No published results";
            default -> status;
        };
    }

    private static int safeCount(Integer value) {
        return value == null ? 0 : value;
    }
}
