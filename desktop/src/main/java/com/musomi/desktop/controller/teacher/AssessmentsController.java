package com.musomi.desktop.controller.teacher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.api.TeacherAssessmentApi;
import com.musomi.desktop.model.ClassResponse;
import com.musomi.desktop.model.CreateAssessmentRequest;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.model.TermResponse;
import com.musomi.desktop.model.TopicResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * Controller for /fxml/teacher/assessments.fxml.
 *
 * Assessment data is loaded from and persisted to the teacher API.
 *
 * Panels (all live inside the same FXML StackPane):
 *   listPane   — assessment list table
 *   formPane   — create / edit form overlay
 *   reviewPane — review & publish overlay
 *   viewPane   — read-only view for published assessments
 */
public class AssessmentsController {

    private static final Logger log = LoggerFactory.getLogger(AssessmentsController.class);

    // =========================================================================
    // Combo options
    // =========================================================================

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy");
    private static final DateTimeFormatter API_DATE_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE;

    // =========================================================================
    // FXML — list panel
    // =========================================================================

    @FXML private Button  createBtn;
    @FXML private VBox    listPane;
    @FXML private TableView<AssessmentData> assessmentTable;
    @FXML private TableColumn<AssessmentData, String> nameColumn;
    @FXML private TableColumn<AssessmentData, String> classColumn;
    @FXML private TableColumn<AssessmentData, String> subjectColumn;
    @FXML private TableColumn<AssessmentData, String> topicColumn;
    @FXML private TableColumn<AssessmentData, String> dateColumn;
    @FXML private TableColumn<AssessmentData, String> maxMarksColumn;
    @FXML private TableColumn<AssessmentData, String> statusColumn;
    @FXML private TableColumn<AssessmentData, AssessmentData> actionsColumn;

    // =========================================================================
    // FXML — create / edit form panel
    // =========================================================================

    @FXML private VBox       formPane;
    @FXML private Label      formTitle;
    @FXML private Label      errorLabel;
    @FXML private Label      successLabel;
    @FXML private TextField  nameField;
    @FXML private ComboBox<ClassResponse> classCombo;
    @FXML private ComboBox<SubjectResponse> subjectCombo;
    @FXML private ComboBox<TermResponse> termCombo;
    @FXML private ComboBox<TopicResponse> topicCombo;
    @FXML private DatePicker datePicker;
    @FXML private TextField  marksField;
    @FXML private Button     saveDraftBtn;

    // =========================================================================
    // FXML — review / publish panel
    // =========================================================================

    @FXML private ScrollPane reviewPane;
    @FXML private Label  reviewNameLabel;
    @FXML private Label  reviewClassLabel;
    @FXML private Label  reviewSubjectLabel;
    @FXML private Label  reviewTopicLabel;
    @FXML private Label  reviewDateLabel;
    @FXML private Label  reviewMaxMarksLabel;
    @FXML private Label  reviewCompletenessLabel;
    @FXML private Label  reviewWarningLabel;
    @FXML private VBox   reviewMarksContainer;
    @FXML private Button publishBtn;

    // =========================================================================
    // FXML — view (read-only) panel
    // =========================================================================

    @FXML private ScrollPane viewPane;
    @FXML private Label  viewNameLabel;
    @FXML private Label  viewClassLabel;
    @FXML private Label  viewSubjectLabel;
    @FXML private Label  viewTopicLabel;
    @FXML private Label  viewDateLabel;
    @FXML private Label  viewMaxMarksLabel;
    @FXML private VBox   viewMarksContainer;

    // =========================================================================
    // State
    // =========================================================================

    /** Assessment currently being edited in the form. */
    private AssessmentData editingAssessment = null;

    /** Assessment currently open in the review pane. */
    private AssessmentData reviewingAssessment = null;
    private boolean reviewComplete;
    private final List<AssessmentData> assessments = new ArrayList<>();
    private final TeacherAssessmentApi teacherAssessmentApi = new TeacherAssessmentApi();

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @FXML
    private void initialize() {
        setupAssessmentTable();
        setupReferenceSelectors();
        assessmentTable.setPlaceholder(new Label("Loading assessments..."));
        refreshList();
    }

    private void setupReferenceSelectors() {
        classCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(ClassResponse value) {
                return value == null ? "" : value.name();
            }
            @Override public ClassResponse fromString(String value) { return null; }
        });
        subjectCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(SubjectResponse value) {
                return value == null ? "" : value.name();
            }
            @Override public SubjectResponse fromString(String value) { return null; }
        });
        termCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(TermResponse value) {
                return value == null ? "" : value.name();
            }
            @Override public TermResponse fromString(String value) { return null; }
        });
        topicCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(TopicResponse value) {
                return value == null ? "" : value.name();
            }
            @Override public TopicResponse fromString(String value) { return null; }
        });
        subjectCombo.valueProperty().addListener(
                (observable, oldValue, newValue) -> loadTopics(newValue));
        loadReferenceData();
    }

    private void loadReferenceData() {
        TaskRunner.run(
                () -> new ReferenceData(
                        teacherAssessmentApi.getMyClasses(),
                        teacherAssessmentApi.getSubjects(),
                        teacherAssessmentApi.getTerms()),
                data -> {
                    classCombo.getItems().setAll(data.classes());
                    subjectCombo.getItems().setAll(data.subjects());
                    termCombo.getItems().setAll(data.terms());
                },
                error -> {
                    log.error("Unable to load assessment reference data", error);
                    Toast.error("Unable to load assessment options: " + error.getMessage());
                });
    }

    private void loadTopics(SubjectResponse subject) {
        topicCombo.getItems().clear();
        if (subject == null || subject.id() == null) {
            return;
        }
        TaskRunner.run(
                () -> teacherAssessmentApi.getTopics(subject.id()),
                topicCombo.getItems()::setAll,
                error -> {
                    log.error("Unable to load topics for subject {}", subject.id(), error);
                    Toast.error("Unable to load topics: " + error.getMessage());
                });
    }

    private void setupAssessmentTable() {
        nameColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().title()));
        classColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().displayClass()));
        subjectColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().subjectName()));
        topicColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().topicNames()));
        dateColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().displayDate()));
        maxMarksColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().maxScore().stripTrailingZeros().toPlainString()));
        statusColumn.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().status()));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(status);
                    badge.getStyleClass().add("PUBLISHED".equalsIgnoreCase(status)
                            ? "badge-published" : "badge-draft");
                    setText(null);
                    setGraphic(badge);
                }
            }
        });
        actionsColumn.setCellValueFactory(data ->
                new SimpleObjectProperty<>(data.getValue()));
        actionsColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(AssessmentData assessment, boolean empty) {
                super.updateItem(assessment, empty);
                if (empty || assessment == null) {
                    setGraphic(null);
                    return;
                }

                HBox actions = new HBox(6);
                actions.setAlignment(Pos.CENTER_LEFT);
                if (assessment.isDraft()) {
                    Button edit = actionButton("Edit", "assessment-action-edit");
                    edit.setOnAction(event -> openEditForm(assessment));
                    Button review = actionButton("Review", "assessment-action-review");
                    review.setOnAction(event -> openReviewPane(assessment));
                    actions.getChildren().addAll(edit, review);
                } else {
                    Button view = actionButton("View", "assessment-action-view");
                    view.setOnAction(event -> openViewPane(assessment));
                    actions.getChildren().add(view);
                }
                setGraphic(actions);
            }
        });
        assessmentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        assessmentTable.setItems(javafx.collections.FXCollections.observableArrayList());
    }

    // =========================================================================
    // List rendering
    // =========================================================================

    private void refreshList() {
        assessmentTable.setPlaceholder(new Label("Loading assessments..."));
        TaskRunner.run(
            () -> ApiClient.getInstance().get(
                    "/teacher/assessments", AssessmentResponse[].class),
            response -> {
                List<AssessmentData> items = new ArrayList<>();
                if (response != null) {
                    for (AssessmentResponse assessment : response) {
                        if (assessment != null && assessment.id() != null) {
                            items.add(new AssessmentData(assessment));
                        }
                    }
                }
                assessments.clear();
                assessments.addAll(items);
                assessmentTable.getItems().setAll(items);
                assessmentTable.setPlaceholder(new Label(
                        items.isEmpty() ? "No assessments yet." : "No assessments match."));
            },
            error -> {
                log.error("Unable to load teacher assessments", error);
                assessmentTable.setPlaceholder(new Label(
                        "Unable to load assessments: " + error.getMessage()));
                Toast.error("Unable to load assessments.");
            }
        );
    }

    private static <T> void selectById(ComboBox<T> comboBox, Long id) {
        if (id == null) {
            comboBox.getSelectionModel().clearSelection();
            return;
        }
        for (T item : comboBox.getItems()) {
            Long itemId = null;
            if (item instanceof ClassResponse value) {
                itemId = value.id();
            } else if (item instanceof SubjectResponse value) {
                itemId = value.id();
            } else if (item instanceof TermResponse value) {
                itemId = value.id();
            } else if (item instanceof TopicResponse value) {
                itemId = value.id();
            }
            if (id.equals(itemId)) {
                comboBox.setValue(item);
                return;
            }
        }
        comboBox.getSelectionModel().clearSelection();
    }

    private Button actionButton(String text, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        return b;
    }

    // =========================================================================
    // Panel visibility
    // =========================================================================

    private enum ActivePanel { LIST, FORM, REVIEW, VIEW }

    private void showPanel(ActivePanel panel) {
        boolean showList   = panel == ActivePanel.LIST;
        boolean showForm   = panel == ActivePanel.FORM;
        boolean showReview = panel == ActivePanel.REVIEW;
        boolean showView   = panel == ActivePanel.VIEW;

        listPane.setVisible(showList);
        listPane.setManaged(showList);
        createBtn.setDisable(!showList);

        formPane.setVisible(showForm);
        formPane.setManaged(showForm);

        reviewPane.setVisible(showReview);
        reviewPane.setManaged(showReview);

        viewPane.setVisible(showView);
        viewPane.setManaged(showView);

        if (showList) hideMessages();
    }

    // =========================================================================
    // Create / Edit form
    // =========================================================================

    @FXML
    private void handleCreateAssessment() {
        editingAssessment = null;
        formTitle.setText("Create Assessment");
        saveDraftBtn.setText("Save Draft");
        clearForm();
        showPanel(ActivePanel.FORM);
    }

    private void openEditForm(AssessmentData a) {
        editingAssessment = a;
        formTitle.setText("Edit Assessment");
        saveDraftBtn.setText("Save Changes");
        populateForm(a);
        showPanel(ActivePanel.FORM);
    }

    @FXML
    private void handleCancelForm() {
        showPanel(ActivePanel.LIST);
    }

    private void clearForm() {
        nameField.clear();
        classCombo.getSelectionModel().clearSelection();
        subjectCombo.getSelectionModel().clearSelection();
        termCombo.getSelectionModel().clearSelection();
        topicCombo.getSelectionModel().clearSelection();
        topicCombo.getItems().clear();
        datePicker.setValue(null);
        marksField.clear();
        hideMessages();
    }

    private void populateForm(AssessmentData a) {
        nameField.setText(a.title());
        selectById(classCombo, a.classId());
        selectById(subjectCombo, a.subjectId());
        selectById(termCombo, a.termId());
        if (a.topics() != null && !a.topics().isEmpty()) {
            selectById(topicCombo, a.topics().get(0).id());
        }
        marksField.setText(a.maxScore().stripTrailingZeros().toPlainString());
        datePicker.setValue(a.assessmentDate());
        hideMessages();
    }

    @FXML
    private void handleSaveDraft() {
        hideMessages();

        String name     = nameField.getText()    == null ? "" : nameField.getText().trim();
        ClassResponse selectedClass = classCombo.getValue();
        SubjectResponse selectedSubject = subjectCombo.getValue();
        TermResponse selectedTerm = termCombo.getValue();
        TopicResponse selectedTopic = topicCombo.getValue();
        LocalDate date  = datePicker.getValue();
        String marksStr = marksField.getText()   == null ? "" : marksField.getText().trim();

        if (name.isEmpty())    { showError("Assessment Name is required."); return; }
        if (selectedClass == null) { showError("Class is required."); return; }
        if (selectedSubject == null) { showError("Subject is required."); return; }
        if (selectedTerm == null) { showError("Term is required."); return; }
        if (selectedTopic == null) { showError("Topic is required."); return; }
        if (date == null)      { showError("Assessment Date is required."); return; }
        if (marksStr.isEmpty()){ showError("Maximum Marks is required."); return; }

        int maxMarks;
        try {
            maxMarks = Integer.parseInt(marksStr);
        } catch (NumberFormatException ex) {
            showError("Maximum Marks must be a whole number (e.g. 40).");
            return;
        }
        if (maxMarks <= 0)    { showError("Maximum Marks must be a positive number greater than zero."); return; }
        if (maxMarks > 1000)  { showError("Maximum Marks cannot exceed 1000."); return; }

        if (editingAssessment == null) {
            CreateAssessmentRequest request = new CreateAssessmentRequest(
                    selectedClass.id(), null, selectedSubject.id(), selectedTerm.id(),
                    name, "EXAM", date, BigDecimal.valueOf(maxMarks),
                    List.of(selectedTopic.id()));
            saveDraftBtn.setDisable(true);
            TaskRunner.run(
                    () -> teacherAssessmentApi.createAssessment(request),
                    response -> {
                        saveDraftBtn.setDisable(false);
                        Toast.success("Assessment created");
                        refreshList();
                        showPanel(ActivePanel.LIST);
                    },
                    error -> {
                        saveDraftBtn.setDisable(false);
                        log.error("Unable to create assessment", error);
                        showError("Unable to create assessment: " + error.getMessage());
                    });
            return;
        }
        if (editingAssessment.termId() == null || editingAssessment.topics().isEmpty()) {
            showError("This assessment is missing the term or topic IDs required by the backend.");
            return;
        }
        if (!selectedClass.id().equals(editingAssessment.classId())
                || !selectedSubject.id().equals(editingAssessment.subjectId())
                || !selectedTopic.id().equals(editingAssessment.topics().get(0).id())) {
            showError("Changing class, subject, or topics is not supported while editing.");
            return;
        }

        AssessmentUpdateRequest request = new AssessmentUpdateRequest(
                editingAssessment.streamId(),
                editingAssessment.termId(),
                name,
                editingAssessment.type() == null ? "EXAM" : editingAssessment.type(),
                date.format(API_DATE_FORMAT),
                BigDecimal.valueOf(maxMarks),
                editingAssessment.topics().stream().map(TopicData::id).toList());
        saveDraftBtn.setDisable(true);
        TaskRunner.run(
            () -> ApiClient.getInstance().put(
                    "/teacher/assessments/" + editingAssessment.id(),
                    request,
                    AssessmentResponse.class),
            response -> {
                saveDraftBtn.setDisable(false);
                if (response == null) {
                    showError("The server did not confirm the assessment update.");
                    return;
                }
                showSuccess("Assessment updated successfully.");
                refreshList();
                javafx.animation.PauseTransition pause =
                        new javafx.animation.PauseTransition(javafx.util.Duration.millis(1200));
                pause.setOnFinished(evt -> showPanel(ActivePanel.LIST));
                pause.play();
            },
            error -> {
                saveDraftBtn.setDisable(false);
                log.error("Unable to update assessment {}", editingAssessment.id(), error);
                showError("Unable to save assessment: " + error.getMessage());
            }
        );
    }

    // =========================================================================
    // Review & Publish panel
    // =========================================================================

    private void openReviewPane(AssessmentData a) {
        reviewingAssessment = a;
        reviewComplete = false;

        populateAssessmentLabels(a, true);
        reviewCompletenessLabel.setText("Loading marks...");
        reviewWarningLabel.setVisible(false);
        reviewWarningLabel.setManaged(false);
        publishBtn.setDisable(true);
        reviewMarksContainer.getChildren().clear();
        showPanel(ActivePanel.REVIEW);
        TaskRunner.run(
            () -> ApiClient.getInstance().get(
                    "/teacher/assessments/" + a.id() + "/grid", AssessmentGrid.class),
            grid -> {
                if (reviewingAssessment != null && a.id().equals(reviewingAssessment.id())) {
                    showReviewGrid(a, grid);
                }
            },
            error -> {
                if (reviewingAssessment == null || !a.id().equals(reviewingAssessment.id())) {
                    return;
                }
                log.error("Unable to load review grid for assessment {}", a.id(), error);
                reviewCompletenessLabel.setText("Unable to load assessment marks.");
                reviewWarningLabel.setText(error.getMessage());
                reviewWarningLabel.setVisible(true);
                reviewWarningLabel.setManaged(true);
            }
        );
    }

    private void showReviewGrid(AssessmentData assessment, AssessmentGrid grid) {
        List<ReviewMark> marks = mapMarks(grid);
        int completed = (int) marks.stream().filter(mark -> mark.score() != null).count();
        int total = marks.size();
        reviewCompletenessLabel.setText(
                completed + " of " + total + " students have marks entered.");
        reviewComplete = total > 0 && completed == total;
        if (!reviewComplete) {
            int missing = total - completed;
            reviewWarningLabel.setText(total == 0
                    ? "No students are enrolled in this assessment."
                    : missing + " student" + (missing == 1 ? "" : "s")
                            + " still need marks before this assessment can be published.");
            reviewWarningLabel.setVisible(true);
            reviewWarningLabel.setManaged(true);
        } else {
            reviewWarningLabel.setVisible(false);
            reviewWarningLabel.setManaged(false);
        }
        publishBtn.setDisable(!reviewComplete);
        buildMarksRows(reviewMarksContainer, marks, gridMaxScore(assessment, grid));
    }

    @FXML
    private void handleCancelReview() {
        reviewingAssessment = null;
        showPanel(ActivePanel.LIST);
    }

    @FXML
    private void handlePublish() {
        if (reviewingAssessment == null) return;
        if (!reviewComplete) return;

        AssessmentData a = reviewingAssessment;
        int total = a.studentCount() == null ? 0 : a.studentCount().intValue();

        // ── Confirmation dialog ───────────────────────────────────────────────
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Publish Assessment");
        confirm.setHeaderText("Publish: " + a.title());
        confirm.setContentText(
            "Assessment:    " + a.title() + "\n" +
            "Class:             " + a.displayClass() + "\n" +
            "Students:       " + total + "\n" +
            "Completed:     " + total + " / " + total + "\n" +
            "Max Mark:      " + a.maxScore().stripTrailingZeros().toPlainString() + "\n\n" +
            "Published assessments become read-only and cannot be edited.\n" +
            "Are you sure you want to publish?");

        confirm.getButtonTypes().setAll(ButtonType.CANCEL, ButtonType.OK);
        // Relabel OK button
        ((javafx.scene.control.ButtonBar) confirm.getDialogPane().lookup(".button-bar"))
                .getButtons()
                .stream()
                .filter(n -> n instanceof Button &&
                        ButtonType.OK.getText().equals(((Button) n).getText()))
                .findFirst()
                .ifPresent(n -> ((Button) n).setText("Publish"));

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        publishBtn.setDisable(true);
        TaskRunner.run(
            () -> ApiClient.getInstance().post(
                    "/teacher/assessments/" + a.id() + "/publish", null, Void.class),
            ignored -> {
                reviewingAssessment = null;
                refreshList();
                showPanel(ActivePanel.LIST);
                showListSuccess("Assessment [" + a.title() + "] published successfully.");
            },
            error -> {
                publishBtn.setDisable(false);
                log.error("Unable to publish assessment {}", a.id(), error);
                Toast.error("Unable to publish assessment: " + error.getMessage());
            }
        );
    }

    // =========================================================================
    // View (read-only) panel
    // =========================================================================

    private void openViewPane(AssessmentData a) {
        populateAssessmentLabels(a, false);
        viewMarksContainer.getChildren().clear();
        showPanel(ActivePanel.VIEW);
        TaskRunner.run(
            () -> ApiClient.getInstance().get(
                    "/teacher/assessments/" + a.id() + "/grid", AssessmentGrid.class),
            grid -> {
                if (grid != null) {
                    buildMarksRows(viewMarksContainer, mapMarks(grid), gridMaxScore(a, grid));
                }
            },
            error -> {
                log.error("Unable to load marks for assessment {}", a.id(), error);
                viewMarksContainer.getChildren().setAll(
                        new Label("Unable to load marks: " + error.getMessage()));
            }
        );
    }

    private BigDecimal gridMaxScore(AssessmentData assessment, AssessmentGrid grid) {
        if (grid != null && grid.assessment() != null && grid.assessment().maxScore() != null) {
            return grid.assessment().maxScore();
        }
        return assessment.maxScore();
    }

    private void populateAssessmentLabels(AssessmentData a, boolean review) {
        if (review) {
            reviewNameLabel.setText(a.title());
            reviewClassLabel.setText(a.displayClass());
            reviewSubjectLabel.setText(a.subjectName());
            reviewTopicLabel.setText(a.topicNames());
            reviewDateLabel.setText(a.displayDate());
            reviewMaxMarksLabel.setText(a.maxScore().stripTrailingZeros().toPlainString());
        } else {
            viewNameLabel.setText(a.title());
            viewClassLabel.setText(a.displayClass());
            viewSubjectLabel.setText(a.subjectName());
            viewTopicLabel.setText(a.topicNames());
            viewDateLabel.setText(a.displayDate());
            viewMaxMarksLabel.setText(a.maxScore().stripTrailingZeros().toPlainString());
        }
    }

    @FXML
    private void handleCloseView() {
        showPanel(ActivePanel.LIST);
    }

    // =========================================================================
    // Marks table builder (shared by review and view panels)
    // =========================================================================

    private List<ReviewMark> mapMarks(AssessmentGrid grid) {
        if (grid == null || grid.students() == null) {
            return List.of();
        }
        Map<Long, ScoreData> scoresByStudent = new HashMap<>();
        if (grid.scores() != null) {
            for (ScoreData score : grid.scores()) {
                if (score != null && score.studentId() != null) {
                    scoresByStudent.put(score.studentId(), score);
                }
            }
        }
        List<ReviewMark> marks = new ArrayList<>();
        for (StudentData student : grid.students()) {
            if (student == null) {
                continue;
            }
            ScoreData score = scoresByStudent.get(student.id());
            marks.add(new ReviewMark(
                    Integer.toString(marks.size() + 1),
                    student.fullName() == null ? "" : student.fullName(),
                    score == null ? null : score.score()));
        }
        return marks;
    }

    private void buildMarksRows(VBox container, List<ReviewMark> marks,
                                BigDecimal maxMarks) {
        container.getChildren().clear();

        // Header row
        HBox header = new HBox(0);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(10, 16, 10, 16));
        header.setStyle(
            "-fx-background-color: #F8FAFC;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-width: 1 1 0 1;" +
            "-fx-background-radius: 8px 8px 0 0;");
        header.getChildren().addAll(
            colHeader("#", 40),
            colHeader("Student", 200),
            colHeader("Mark", 80),
            colHeader("Max", 60),
            colHeader("Grade", 70),
            colHeader("Remark", 120)
        );
        container.getChildren().add(header);

        for (int i = 0; i < marks.size(); i++) {
            ReviewMark mark = marks.get(i);
            boolean isEven = (i % 2 == 0);
            String bg = isEven ? "white" : "#FAFBFC";

            HBox row = new HBox(0);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(11, 16, 11, 16));
            row.setStyle(
                "-fx-background-color: " + bg + ";" +
                "-fx-border-color: #E2E8F0;" +
                "-fx-border-width: 0 1 1 1;");

            boolean valid = mark.score() != null;
            String markText = valid
                    ? mark.score().stripTrailingZeros().toPlainString() : "—";
            String gradeText = gradeFor(mark.score(), maxMarks);
            String remarkText = remarkFor(gradeText);

            Label markLabel = new Label(markText);
            markLabel.setMinWidth(80);
            markLabel.setStyle(
                "-fx-font-size:13px;" +
                (valid ? "-fx-text-fill:#0F172A;-fx-font-weight:600;"
                       : "-fx-text-fill:#EF4444;-fx-font-weight:500;"));

            Label gradeLabel = new Label(gradeText);
            gradeLabel.setMinWidth(70);
            gradeLabel.setStyle(
                "-fx-font-size:13px;" +
                "-fx-font-weight:600;" +
                gradeColor(gradeText));

            row.getChildren().addAll(
                colValue(mark.number(), 40),
                colValue(mark.name(), 200),
                markLabel,
                colValue(maxMarks.stripTrailingZeros().toPlainString(), 60),
                gradeLabel,
                colValue(remarkText, 120)
            );
            container.getChildren().add(row);
        }
    }

    private static String gradeFor(BigDecimal score, BigDecimal maxScore) {
        if (score == null || maxScore == null || maxScore.signum() <= 0) {
            return "";
        }
        BigDecimal percentage = score.multiply(BigDecimal.valueOf(100))
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

    private static String joinClassAndStream(String className, String streamName) {
        String name = className == null ? "" : className.trim();
        String stream = streamName == null ? "" : streamName.trim();
        return stream.isEmpty() ? name : name + " " + stream;
    }

    private static String topicNames(List<TopicData> topics) {
        if (topics == null) {
            return "";
        }
        return topics.stream()
                .filter(topic -> topic != null && topic.name() != null)
                .map(TopicData::name)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    private record AssessmentData(
            Long id,
            String title,
            String className,
            String streamName,
            String subjectName,
            LocalDate assessmentDate,
            BigDecimal maxScore,
            String status,
            Long studentCount,
            Long enteredCount,
            Long classId,
            Long streamId,
            Long subjectId,
            Long termId,
            String type,
            List<TopicData> topics) {
        private AssessmentData(AssessmentResponse response) {
            this(response.id(),
                    response.title() == null ? "" : response.title(),
                    response.className(),
                    response.streamName(),
                    response.subjectName() == null ? "" : response.subjectName(),
                    response.assessmentDate(),
                    response.maxScore() == null ? BigDecimal.ZERO : response.maxScore(),
                    response.status() == null ? "" : response.status(),
                    response.studentCount(),
                    response.enteredCount(),
                    response.classId(),
                    response.streamId(),
                    response.subjectId(),
                    response.termId(),
                    response.type(),
                    response.topics() == null ? List.of() : response.topics());
        }

        private String displayClass() {
            return joinClassAndStream(className, streamName);
        }

        private String topicNames() {
            return AssessmentsController.topicNames(topics);
        }

        private String displayDate() {
            return assessmentDate == null ? "" : assessmentDate.format(DISPLAY_FORMAT);
        }

        private boolean isDraft() {
            return "DRAFT".equalsIgnoreCase(status);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AssessmentResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("title") String title,
            @JsonProperty("type") String type,
            @JsonProperty("assessmentDate") LocalDate assessmentDate,
            @JsonProperty("maxScore") BigDecimal maxScore,
            @JsonProperty("status") String status,
            @JsonProperty("classId") Long classId,
            @JsonProperty("className") String className,
            @JsonProperty("streamId") Long streamId,
            @JsonProperty("streamName") String streamName,
            @JsonProperty("subjectId") Long subjectId,
            @JsonProperty("subjectName") String subjectName,
            @JsonProperty("termId") Long termId,
            @JsonProperty("topics") List<TopicData> topics,
            @JsonProperty("studentCount") Long studentCount,
            @JsonProperty("enteredCount") Long enteredCount) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TopicData(
            @JsonProperty("id") Long id,
            @JsonProperty("name") String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AssessmentGrid(
            @JsonProperty("assessment") GridAssessment assessment,
            @JsonProperty("students") List<StudentData> students,
            @JsonProperty("scores") List<ScoreData> scores) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GridAssessment(
            @JsonProperty("maxScore") BigDecimal maxScore) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StudentData(
            @JsonProperty("id") Long id,
            @JsonProperty("fullName") String fullName) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ScoreData(
            @JsonProperty("scoreId") Long scoreId,
            @JsonProperty("studentId") Long studentId,
            @JsonProperty("score") BigDecimal score) {}

    private record ReviewMark(String number, String name, BigDecimal score) {}

    private record AssessmentUpdateRequest(
            Long streamId,
            Long termId,
            String title,
            String type,
            String assessmentDate,
            BigDecimal maxScore,
            List<Long> topicIds) {}

    private record ReferenceData(
            List<ClassResponse> classes,
            List<SubjectResponse> subjects,
            List<TermResponse> terms) {}

    private Label colHeader(String text, double width) {
        Label l = new Label(text);
        l.setMinWidth(width);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size:12px;-fx-font-weight:700;-fx-text-fill:#64748B;");
        return l;
    }

    private Label colValue(String text, double width) {
        Label l = new Label(text);
        l.setMinWidth(width);
        l.setPrefWidth(width);
        l.setStyle("-fx-font-size:13px;-fx-text-fill:#475569;");
        return l;
    }

    private String gradeColor(String grade) {
        return switch (grade) {
            case "A"  -> "-fx-text-fill:#047857;";
            case "B"  -> "-fx-text-fill:#0369A1;";
            case "C"  -> "-fx-text-fill:#6D28D9;";
            case "D"  -> "-fx-text-fill:#B45309;";
            case "E","F" -> "-fx-text-fill:#DC2626;";
            default   -> "-fx-text-fill:#94A3B8;";
        };
    }

    // =========================================================================
    // Messages
    // =========================================================================

    @FXML private Label listSuccessLabel;

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }

    private void showSuccess(String msg) {
        successLabel.setText("✓  " + msg);
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    private void showListSuccess(String msg) {
        listSuccessLabel.setText("✓  " + msg);
        listSuccessLabel.setVisible(true);
        listSuccessLabel.setManaged(true);
        javafx.animation.PauseTransition t =
                new javafx.animation.PauseTransition(javafx.util.Duration.seconds(4));
        t.setOnFinished(e -> {
            listSuccessLabel.setVisible(false);
            listSuccessLabel.setManaged(false);
        });
        t.play();
    }

    private void hideMessages() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
        successLabel.setVisible(false);
        successLabel.setManaged(false);
    }
}
