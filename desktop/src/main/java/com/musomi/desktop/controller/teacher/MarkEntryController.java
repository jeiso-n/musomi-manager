package com.musomi.desktop.controller.teacher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.util.TaskRunner;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

public class MarkEntryController {

    private static final Logger log = LoggerFactory.getLogger(MarkEntryController.class);
    private static final Pattern MARK_INPUT = Pattern.compile("\\d*(\\.\\d{0,2})?");

    @FXML private ComboBox<String> classComboBox;
    @FXML private ComboBox<String> subjectComboBox;
    @FXML private ComboBox<String> topicComboBox;
    @FXML private ComboBox<AssessmentOption> assessmentComboBox;
    @FXML private TextField searchField;
    @FXML private TableView<MarkRow> marksTable;
    @FXML private TableColumn<MarkRow, String> numberColumn;
    @FXML private TableColumn<MarkRow, String> studentColumn;
    @FXML private TableColumn<MarkRow, String> markColumn;
    @FXML private TableColumn<MarkRow, String> totalColumn;
    @FXML private TableColumn<MarkRow, String> gradeColumn;
    @FXML private TableColumn<MarkRow, String> remarkColumn;
    @FXML private Label progressLabel;
    @FXML private Label statusLabel;
    @FXML private ProgressBar progressBar;
    @FXML private Button saveAllButton;

    private final ObservableList<MarkRow> allRows = FXCollections.observableArrayList();
    private FilteredList<MarkRow> filteredRows;
    private final List<AssessmentOption> assessments = new ArrayList<>();
    private AssessmentOption selectedAssessment;
    private Object pendingSelection;
    private boolean updatingFilters;
    private boolean savingScores;
    private boolean saveRequested;

    @FXML
    private void initialize() {
        setupTable();
        filteredRows = new FilteredList<>(allRows, row -> true);
        marksTable.setItems(filteredRows);
        setupSearch();
        setupFilters();
        setupKeyboardNavigation();
        clearSelection("Loading assessments...");
        TaskRunner.run(
            () -> ApiClient.getInstance().get(
                    "/teacher/assessments", AssessmentResponse[].class),
            this::showAssessments,
            this::showLoadError
        );
    }

    private void setupFilters() {
        assessmentComboBox.setConverter(new StringConverter<>() {
            @Override
            public String toString(AssessmentOption assessment) {
                return assessment == null ? "" : assessment.label();
            }

            @Override
            public AssessmentOption fromString(String value) {
                return null;
            }
        });
        assessmentComboBox.setPrefWidth(320);
        assessmentComboBox.setCellFactory(list -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AssessmentOption assessment, boolean empty) {
                super.updateItem(assessment, empty);
                setText(empty || assessment == null ? null : assessment.label());
                setTooltip(empty || assessment == null
                        ? null : new javafx.scene.control.Tooltip(assessment.label()));
            }
        });
        assessmentComboBox.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(AssessmentOption assessment, boolean empty) {
                super.updateItem(assessment, empty);
                setText(empty || assessment == null ? null : assessment.label());
                setTooltip(empty || assessment == null
                        ? null : new javafx.scene.control.Tooltip(assessment.label()));
            }
        });

        classComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                updateSubjectAndTopicOptions();
                refreshAssessmentChoices(null);
            }
        });
        subjectComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                updateTopicOptions();
                refreshAssessmentChoices(null);
            }
        });
        topicComboBox.valueProperty().addListener((obs, oldValue, newValue) -> {
            if (!updatingFilters) {
                refreshAssessmentChoices(null);
            }
        });
        assessmentComboBox.valueProperty().addListener(
                (obs, oldValue, newValue) -> showAssessment(newValue));
    }

    private void showAssessments(AssessmentResponse[] response) {
        assessments.clear();
        if (response != null) {
            for (AssessmentResponse assessment : response) {
                if (assessment != null && assessment.id() != null) {
                    assessments.add(new AssessmentOption(assessment));
                }
            }
        }
        if (assessments.isEmpty()) {
            classComboBox.getItems().clear();
            subjectComboBox.getItems().clear();
            topicComboBox.getItems().clear();
            assessmentComboBox.getItems().clear();
            clearSelection("No assessments are available yet.");
            return;
        }

        List<AssessmentOption> pending = assessments.stream()
                .filter(AssessmentOption::isPending)
                .toList();
        AssessmentOption initial = pending.isEmpty() ? assessments.get(0) : pending.get(0);
        classComboBox.setItems(options(assessments.stream()
                .map(AssessmentOption::className).collect(Collectors.toSet())));
        classComboBox.setValue(initial.className());
        subjectComboBox.setItems(subjectOptions(initial.className()));
        subjectComboBox.setValue(initial.subject());
        topicComboBox.setItems(topicOptions(initial.className(), initial.subject()));
        topicComboBox.setValue(initial.topic());
        refreshAssessmentChoices(initial.id());
        if (pendingSelection != null) {
            Object selection = pendingSelection;
            pendingSelection = null;
            setData(selection);
        }
    }

    private ObservableList<String> options(Set<String> values) {
        return FXCollections.observableArrayList(new TreeSet<>(values));
    }

    private ObservableList<String> subjectOptions(String className) {
        return options(assessments.stream()
                .filter(assessment -> className == null || className.equals(assessment.className()))
                .map(AssessmentOption::subject)
                .collect(Collectors.toSet()));
    }

    private ObservableList<String> topicOptions(String className, String subject) {
        return options(assessments.stream()
                .filter(assessment -> className == null || className.equals(assessment.className()))
                .filter(assessment -> subject == null || subject.equals(assessment.subject()))
                .map(AssessmentOption::topic)
                .collect(Collectors.toSet()));
    }

    private void updateSubjectAndTopicOptions() {
        updatingFilters = true;
        try {
            ObservableList<String> subjects = subjectOptions(classComboBox.getValue());
            subjectComboBox.setItems(subjects);
            if (!subjects.contains(subjectComboBox.getValue())) {
                subjectComboBox.setValue(null);
            }

            ObservableList<String> topics = topicOptions(classComboBox.getValue(), subjectComboBox.getValue());
            topicComboBox.setItems(topics);
            if (!topics.contains(topicComboBox.getValue())) {
                topicComboBox.setValue(null);
            }
        } finally {
            updatingFilters = false;
        }
    }

    private void updateTopicOptions() {
        updatingFilters = true;
        try {
            ObservableList<String> topics = topicOptions(
                    classComboBox.getValue(), subjectComboBox.getValue());
            topicComboBox.setItems(topics);
            if (!topics.contains(topicComboBox.getValue())) {
                topicComboBox.setValue(null);
            }
        } finally {
            updatingFilters = false;
        }
    }

    private void refreshAssessmentChoices(Long preferredId) {
        String className = classComboBox.getValue();
        String subject = subjectComboBox.getValue();
        String topic = topicComboBox.getValue();

        List<AssessmentOption> matches = assessments.stream()
                .filter(assessment -> className == null || className.equals(assessment.className()))
                .filter(assessment -> subject == null || subject.equals(assessment.subject()))
                .filter(assessment -> topic == null || topic.equals(assessment.topic()))
                .toList();

        assessmentComboBox.setItems(FXCollections.observableArrayList(matches));
        AssessmentOption next = matches.stream()
                .filter(assessment -> preferredId != null && preferredId.equals(assessment.id()))
                .findFirst()
                .orElseGet(() -> matches.stream()
                        .filter(AssessmentOption::isPending)
                        .findFirst()
                        .orElseGet(() -> matches.stream().findFirst().orElse(null)));
        assessmentComboBox.setValue(next);
    }

    /** Receives an assessment from another view and selects its backend equivalent. */
    public void setData(Object data) {
        if (data == null) {
            return;
        }
        if (assessments.isEmpty()) {
            pendingSelection = data;
            return;
        }
        try {
            String name = String.valueOf(data.getClass().getMethod("getName").invoke(data));
            String className = String.valueOf(data.getClass().getMethod("getClassName").invoke(data));
            String subject = String.valueOf(data.getClass().getMethod("getSubject").invoke(data));
            assessments.stream()
                    .filter(assessment -> assessment.title().equals(name)
                            && assessment.className().equals(className)
                            && assessment.subject().equals(subject))
                    .findFirst()
                    .ifPresent(assessment -> {
                        classComboBox.setValue(assessment.className());
                        subjectComboBox.setValue(assessment.subject());
                        topicComboBox.setValue(assessment.topic());
                        refreshAssessmentChoices(assessment.id());
                    });
        } catch (ReflectiveOperationException exception) {
            log.warn("Ignoring unsupported assessment selection data", exception);
        }
    }

    private void showAssessment(AssessmentOption assessment) {
        selectedAssessment = assessment;
        allRows.clear();

        if (assessment == null) {
            clearSelection("No assessment matches the selected filters.");
            return;
        }
        clearSelection("Loading student mark grid...");
        selectedAssessment = assessment;
        Long assessmentId = assessment.id();
        TaskRunner.run(
            () -> ApiClient.getInstance().get(
                    "/teacher/assessments/" + assessmentId + "/grid", AssessmentGrid.class),
            grid -> {
                if (selectedAssessment != null && assessmentId.equals(selectedAssessment.id())) {
                    showGrid(grid);
                }
            },
            error -> {
                if (selectedAssessment != null && assessmentId.equals(selectedAssessment.id())) {
                    showLoadError(error);
                }
            }
        );
    }

    private void clearSelection(String message) {
        selectedAssessment = null;
        allRows.clear();
        marksTable.setEditable(false);
        marksTable.setPlaceholder(new Label(message));
        saveAllButton.setDisable(true);
        statusLabel.setText(message);
        updateProgress();
    }

    private void showLoadError(Exception error) {
        log.error("Unable to load assessment data", error);
        clearSelection("Unable to load assessment data: " + error.getMessage());
    }

    private void showGrid(AssessmentGrid grid) {
        if (grid == null || grid.students() == null) {
            clearSelection("The server returned an empty mark grid.");
            return;
        }
        Map<Long, ScoreEntry> scoresByStudent = new HashMap<>();
        if (grid.scores() != null) {
            for (ScoreEntry score : grid.scores()) {
                if (score != null && score.studentId() != null) {
                    scoresByStudent.put(score.studentId(), score);
                }
            }
        }

        allRows.clear();
        BigDecimal maxMarks = grid.assessment() != null
                && grid.assessment().maxScore() != null
                ? grid.assessment().maxScore()
                : selectedAssessment.maxScore();
        if (maxMarks == null || maxMarks.signum() <= 0) {
            clearSelection("The assessment has an invalid maximum score.");
            return;
        }
        for (StudentEntry student : grid.students()) {
            if (student == null || student.id() == null) {
                continue;
            }
            ScoreEntry score = scoresByStudent.get(student.id());
            allRows.add(new MarkRow(
                    student.id(),
                    score == null ? null : score.scoreId(),
                    Integer.toString(allRows.size() + 1),
                    student.fullName() == null ? "" : student.fullName(),
                    score == null || score.score() == null ? "" : score.score().stripTrailingZeros().toPlainString(),
                    maxMarks));
        }

        boolean editable = selectedAssessment.isDraft();
        marksTable.setEditable(editable);
        saveAllButton.setDisable(!editable || allRows.isEmpty() || savingScores);
        statusLabel.setText(editable
                ? "Draft marks save to the server when committed."
                : "Published assessments are read-only.");
        marksTable.setPlaceholder(new Label(allRows.isEmpty()
                ? "No students are enrolled in this class."
                : "No students match your search."));
        updateProgress();
    }

    private void setupTable() {
        numberColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNumber()));
        studentColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getStudent()));
        markColumn.setCellValueFactory(data -> data.getValue().markProperty());
        totalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(String.valueOf(data.getValue().getMaxMarks())));
        gradeColumn.setCellValueFactory(data -> data.getValue().gradeProperty());
        remarkColumn.setCellValueFactory(data -> data.getValue().remarkProperty());

        markColumn.setCellFactory(column -> new MarkCell());
        gradeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                getStyleClass().removeIf(style -> style.matches("grade-[a-f]"));
                if (empty || grade == null || grade.isBlank()) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(grade);
                    setGraphic(null);
                    getStyleClass().add("grade-" + grade.toLowerCase());
                }
            }
        });
        marksTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        marksTable.setPlaceholder(new Label("Select an assessment to enter marks."));
    }

    private void setupSearch() {
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase();
            filteredRows.setPredicate(row -> query.isEmpty()
                    || row.getStudent().toLowerCase().contains(query)
                    || row.getNumber().toLowerCase().contains(query));
        });
    }

    private void setupKeyboardNavigation() {
        marksTable.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() != KeyCode.ENTER && event.getCode() != KeyCode.TAB) {
                return;
            }

            if (marksTable.getEditingCell() != null) {
                return;
            }

            int row = marksTable.getSelectionModel().getSelectedIndex();
            if (row < 0 && !filteredRows.isEmpty()) {
                row = 0;
            }
            if (row >= 0 && row < filteredRows.size()) {
                event.consume();
                startEditing(row);
            }
        });
    }

    private void startEditing(int row) {
        if (selectedAssessment == null || !selectedAssessment.isDraft()
                || row < 0 || row >= filteredRows.size()) {
            return;
        }

        marksTable.getSelectionModel().select(row);
        marksTable.getFocusModel().focus(row, markColumn);
        marksTable.scrollTo(row);
        Platform.runLater(() -> marksTable.edit(row, markColumn));
    }

    private void moveToNextStudent(int currentIndex) {
        int nextIndex = currentIndex + 1;
        if (nextIndex < filteredRows.size()) {
            Platform.runLater(() -> startEditing(nextIndex));
        } else {
            statusLabel.setText("Last visible student updated. Use the arrow keys to review the table.");
        }
    }

    private void updateProgress() {
        int total = allRows.size();
        int entered = (int) allRows.stream()
                .filter(row -> isValidMark(row.getMark(), row.getMaxMarks()))
                .count();
        progressLabel.setText(entered + " / " + total + " students marked");
        progressBar.setProgress(total == 0 ? 0 : (double) entered / total);
    }

    @FXML
    private void saveAll() {
        saveScores();
    }

    private void saveScores() {
        if (selectedAssessment == null || allRows.isEmpty() || !selectedAssessment.isDraft()) {
            return;
        }
        if (savingScores) {
            saveRequested = true;
            statusLabel.setText("Additional mark changes queued for saving...");
            return;
        }
        savingScores = true;
        AssessmentOption savingAssessment = selectedAssessment;
        List<MarkRow> savingRows = new ArrayList<>(allRows);
        List<ScorePayload> scores = savingRows.stream()
                .map(row -> new ScorePayload(
                        row.getScoreId(),
                        row.getStudentId(),
                        parseScore(row.getMark())))
                .toList();
        BatchScoresRequest request = new BatchScoresRequest(scores);
        String path = "/teacher/assessments/" + savingAssessment.id() + "/scores/batch";
        saveAllButton.setDisable(true);
        statusLabel.setText("Saving marks...");
        TaskRunner.run(
            () -> ApiClient.getInstance().post(path, request, ScoreResponse[].class),
            response -> {
                if (response == null) {
                    log.error("Server returned no saved score response for assessment {}",
                            savingAssessment.id());
                    finishScoreSave(savingAssessment, "The server did not confirm the saved marks.");
                    return;
                }
                Map<Long, ScoreResponse> savedByStudent = new HashMap<>();
                for (ScoreResponse saved : response) {
                    if (saved != null && saved.studentId() != null) {
                        savedByStudent.put(saved.studentId(), saved);
                    }
                }
                for (MarkRow row : savingRows) {
                    ScoreResponse saved = savedByStudent.get(row.getStudentId());
                    if (saved != null) {
                        row.setScoreId(saved.scoreId());
                    }
                }
                finishScoreSave(savingAssessment, null);
            },
            error -> {
                log.error("Unable to save marks for assessment {}", savingAssessment.id(), error);
                finishScoreSave(savingAssessment, "Unable to save marks: " + error.getMessage());
            }
        );
    }

    private void finishScoreSave(AssessmentOption savedAssessment, String errorMessage) {
        savingScores = false;
        if (saveRequested) {
            saveRequested = false;
            saveScores();
            return;
        }
        saveAllButton.setDisable(selectedAssessment == null
                || !selectedAssessment.isDraft() || allRows.isEmpty());
        if (selectedAssessment != null
                && savedAssessment.id().equals(selectedAssessment.id())) {
            statusLabel.setText(errorMessage == null ? "Marks saved successfully." : errorMessage);
            if (errorMessage == null) {
                updateProgress();
                marksTable.refresh();
            }
        }
    }

    private void showValidationMessage(String message) {
        statusLabel.setText(message);
        log.debug("Mark entry validation: {}", message);
    }

    public static class MarkRow {
        private final Long studentId;
        private Long scoreId;
        private final String number;
        private final String student;
        private final BigDecimal maxMarks;
        private final StringProperty mark = new SimpleStringProperty();
        private final StringProperty grade = new SimpleStringProperty();
        private final StringProperty remark = new SimpleStringProperty();

        private MarkRow(Long studentId, Long scoreId, String number,
                        String student, String initialMark, BigDecimal maxMarks) {
            this.studentId = studentId;
            this.scoreId = scoreId;
            this.number = number;
            this.student = student;
            this.maxMarks = maxMarks;
            mark.set(initialMark);
            updatePreview(initialMark);
        }

        public String getNumber() {
            return number;
        }

        public String getStudent() {
            return student;
        }

        public String getMark() {
            return mark.get();
        }

        public Long getStudentId() {
            return studentId;
        }

        public Long getScoreId() {
            return scoreId;
        }

        public void setScoreId(Long scoreId) {
            this.scoreId = scoreId;
        }

        public BigDecimal getMaxMarks() {
            return maxMarks;
        }

        public StringProperty markProperty() {
            return mark;
        }

        public StringProperty gradeProperty() {
            return grade;
        }

        public StringProperty remarkProperty() {
            return remark;
        }

        private void updatePreview(String value) {
            String letterGrade = gradeFor(value, maxMarks);
            grade.set(letterGrade);
            remark.set(remarkFor(letterGrade));
        }

        private void setMark(String value) {
            String normalized = value == null ? "" : value.trim();
            mark.set(normalized);
            updatePreview(normalized);
        }
    }

    private static boolean isValidMark(String value, BigDecimal maxScore) {
        if (value == null || value.isBlank() || maxScore == null || maxScore.signum() <= 0) {
            return false;
        }
        try {
            BigDecimal score = new BigDecimal(value.trim());
            return score.signum() >= 0 && score.compareTo(maxScore) <= 0;
        } catch (NumberFormatException exception) {
            return false;
        }
    }

    private static BigDecimal parseScore(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }

    private static String gradeFor(String value, BigDecimal maxScore) {
        if (!isValidMark(value, maxScore)) {
            return "";
        }
        BigDecimal percentage = new BigDecimal(value.trim())
                .multiply(BigDecimal.valueOf(100))
                .divide(maxScore, 4, java.math.RoundingMode.HALF_UP);
        if (percentage.compareTo(BigDecimal.valueOf(80)) >= 0) return "A";
        if (percentage.compareTo(BigDecimal.valueOf(70)) >= 0) return "B";
        if (percentage.compareTo(BigDecimal.valueOf(60)) >= 0) return "C";
        if (percentage.compareTo(BigDecimal.valueOf(50)) >= 0) return "D";
        if (percentage.compareTo(BigDecimal.valueOf(40)) >= 0) return "E";
        return "F";
    }

    private static String remarkFor(String grade) {
        return switch (grade) {
            case "A" -> "Excellent";
            case "B" -> "Very Good";
            case "C" -> "Good";
            case "D" -> "Satisfactory";
            case "E" -> "Needs Improvement";
            case "F" -> "Fail";
            default -> "";
        };
    }

    private static String displayName(String className, String streamName) {
        String name = className == null ? "" : className.trim();
        String stream = streamName == null ? "" : streamName.trim();
        return stream.isEmpty() ? name : name + " " + stream;
    }

    private static String topicNames(List<TopicResponse> topics) {
        if (topics == null) {
            return "";
        }
        return topics.stream()
                .filter(topic -> topic != null && topic.name() != null)
                .map(TopicResponse::name)
                .collect(Collectors.joining(", "));
    }

    private record AssessmentOption(Long id, String title, String className,
                                    String subject, String topic, String status,
                                    BigDecimal maxScore, Long studentCount,
                                    Long enteredCount) {
        private AssessmentOption(AssessmentResponse response) {
            this(response.id(),
                    response.title() == null ? "" : response.title(),
                    displayName(response.className(), response.streamName()),
                    response.subjectName() == null ? "" : response.subjectName(),
                    topicNames(response.topics()),
                    response.status() == null ? "" : response.status(),
                    response.maxScore() == null ? BigDecimal.ZERO : response.maxScore(),
                    response.studentCount() == null ? 0L : response.studentCount(),
                    response.enteredCount() == null ? 0L : response.enteredCount());
        }

        private String label() {
            return title + " — " + className + " — " + subject;
        }

        private boolean isDraft() {
            return "DRAFT".equalsIgnoreCase(status);
        }

        private boolean isPending() {
            return isDraft() || enteredCount < studentCount;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AssessmentResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("title") String title,
            @JsonProperty("className") String className,
            @JsonProperty("streamName") String streamName,
            @JsonProperty("subjectName") String subjectName,
            @JsonProperty("topics") List<TopicResponse> topics,
            @JsonProperty("maxScore") BigDecimal maxScore,
            @JsonProperty("status") String status,
            @JsonProperty("studentCount") Long studentCount,
            @JsonProperty("enteredCount") Long enteredCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TopicResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AssessmentGrid(
            @JsonProperty("assessment") GridAssessment assessment,
            @JsonProperty("students") List<StudentEntry> students,
            @JsonProperty("scores") List<ScoreEntry> scores) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GridAssessment(
            @JsonProperty("maxScore") BigDecimal maxScore,
            @JsonProperty("status") String status) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StudentEntry(
            @JsonProperty("id") Long id,
            @JsonProperty("fullName") String fullName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ScoreEntry(
            @JsonProperty("scoreId") Long scoreId,
            @JsonProperty("studentId") Long studentId,
            @JsonProperty("score") BigDecimal score) {}

    private record ScorePayload(Long scoreId, Long studentId, BigDecimal score) {}

    private record BatchScoresRequest(List<ScorePayload> scores) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ScoreResponse(
            @JsonProperty("scoreId") Long scoreId,
            @JsonProperty("studentId") Long studentId,
            @JsonProperty("score") BigDecimal score) {}

    private final class MarkCell extends TableCell<MarkRow, String> {
        private final TextField editor = new TextField();
        private String originalValue;

        private MarkCell() {
            getStyleClass().add("mark-score-cell");
            editor.getStyleClass().add("mark-score-editor");
            editor.setTextFormatter(new javafx.scene.control.TextFormatter<String>(change ->
                    MARK_INPUT.matcher(change.getControlNewText()).matches() ? change : null));
            editor.textProperty().addListener((observable, oldValue, newValue) -> {
                if (isEditing() && getTableRow() != null && getTableRow().getItem() != null) {
                    getTableRow().getItem().updatePreview(newValue);
                    statusLabel.setText("Editing mark for " + getTableRow().getItem().getStudent());
                }
            });
            editor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ESCAPE) {
                    cancelEdit();
                    event.consume();
                } else if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.TAB) {
                    int currentIndex = getIndex();
                    if (commitValue(true)) {
                        event.consume();
                        moveToNextStudent(currentIndex);
                    } else {
                        event.consume();
                    }
                }
            });
            editor.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
                if (wasFocused && !isFocused && isEditing() && !commitValue(false)) {
                    cancelEdit();
                }
            });
        }

        @Override
        public void startEdit() {
            if (isEmpty() || selectedAssessment == null
                    || !selectedAssessment.isDraft()) {
                return;
            }
            super.startEdit();
            originalValue = getItem() == null ? "" : getItem();
            editor.setText(originalValue);
            setText(null);
            setGraphic(editor);
            editor.requestFocus();
            editor.selectAll();
        }

        @Override
        public void cancelEdit() {
            super.cancelEdit();
            if (getTableRow() != null && getTableRow().getItem() != null) {
                getTableRow().getItem().updatePreview(originalValue);
            }
            setGraphic(null);
            setText(getItem());
        }

        @Override
        protected void updateItem(String value, boolean empty) {
            super.updateItem(value, empty);
            if (empty) {
                setText(null);
                setGraphic(null);
            } else if (isEditing()) {
                setText(null);
                setGraphic(editor);
            } else {
                setText(value);
                setGraphic(null);
            }
        }

        private boolean commitValue(boolean keepEditingOnFailure) {
            String value = editor.getText() == null ? "" : editor.getText().trim();
            MarkRow row = getTableRow() == null ? null : getTableRow().getItem();
            if (row == null) {
                cancelEdit();
                return false;
            }
            if (!value.isEmpty()) {
                try {
                    BigDecimal score = new BigDecimal(value);
                    if (score.signum() < 0 || score.compareTo(row.getMaxMarks()) > 0) {
                        showValidationMessage("Enter a mark from 0 to "
                                + row.getMaxMarks().stripTrailingZeros().toPlainString() + ".");
                        if (keepEditingOnFailure) {
                            editor.requestFocus();
                            editor.selectAll();
                        }
                        return false;
                    }
                } catch (NumberFormatException ex) {
                    showValidationMessage("Enter a valid numeric mark.");
                    if (keepEditingOnFailure) {
                        editor.requestFocus();
                        editor.selectAll();
                    }
                    return false;
                }
            }

            int rowIndex = getIndex();
            row.setMark(value);
            commitEdit(value);
            updateProgress();
            saveScores();
            log.debug("Queued mark save for student '{}' in assessment '{}'",
                    row.getStudent(), selectedAssessment.title());
            marksTable.getSelectionModel().select(rowIndex);
            marksTable.getFocusModel().focus(rowIndex, markColumn);
            return true;
        }
    }
}
