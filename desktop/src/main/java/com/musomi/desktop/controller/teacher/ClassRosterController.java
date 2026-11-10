package com.musomi.desktop.controller.teacher;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.model.dto.TeacherClassResponse;
import com.musomi.desktop.util.TaskRunner;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Controller for the Class Roster screen (/fxml/teacher/class-roster.fxml).
 *
 * Displays a searchable list of students for a specific class.
 */
public class ClassRosterController {

    private static final Logger log = LoggerFactory.getLogger(ClassRosterController.class);

    // -------------------------------------------------------------------------
    // Inner model
    // -------------------------------------------------------------------------

    public static class StudentRow {
        private final String number;
        private final String admNo;
        private final String name;
        private final String gender;
        private final String status;

        public StudentRow(String number, String admNo, String name,
                          String gender, String status) {
            this.number = number;
            this.admNo  = admNo;
            this.name   = name;
            this.gender = gender;
            this.status = status;
        }

        public String getNumber() { return number; }
        public String getAdmNo()  { return admNo;  }
        public String getName()   { return name;   }
        public String getGender() { return gender; }
        public String getStatus() { return status; }
    }

    // -------------------------------------------------------------------------
    // FXML bindings
    // -------------------------------------------------------------------------

    @FXML private Label                            classNameLabel;
    @FXML private Label                            studentCountLabel;
    @FXML private TextField                        searchField;
    @FXML private TableView<StudentRow>            rosterTable;
    @FXML private TableColumn<StudentRow, String>  numberColumn;
    @FXML private TableColumn<StudentRow, String>  admNoColumn;
    @FXML private TableColumn<StudentRow, String>  nameColumn;
    @FXML private TableColumn<StudentRow, String>  genderColumn;
    @FXML private TableColumn<StudentRow, String>  statusColumn;
    @FXML private VBox                             emptyState;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final ObservableList<StudentRow> allStudents =
            FXCollections.observableArrayList();

    private FilteredList<StudentRow> filteredStudents;

    /** Called by MyClassesController after FXML load to inject context. */
    private Runnable backCallback;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @FXML
    private void initialize() {
        setupTableColumns();

        filteredStudents = new FilteredList<>(allStudents, row -> true);
        rosterTable.setItems(filteredStudents);

        rosterTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        setupSearch();
    }

    // -------------------------------------------------------------------------
    // Public API — called by MyClassesController
    // -------------------------------------------------------------------------

    /**
     * Injects the selected class context into this controller.
     *
     * @param className   display name of the class (e.g. "S3 Blue")
     * @param studentCount enrolled student count shown in subtitle
     * @param backCallback runnable to invoke when the Back button is clicked
     */
    public void setClassData(String className, int studentCount,
                             Runnable backCallback) {
        this.backCallback = backCallback;
        updateClassLabels(className, studentCount);
        setStatus("Loading roster...", "Please wait while the students are loaded.");

        // The current My Classes caller supplies the display name and count only.
        // Resolve the endpoint IDs from the authenticated teacher's class list.
        TaskRunner.run(
            () -> resolveClassIds(className),
            ids -> loadStudents(ids.classId(), ids.streamId()),
            error -> showError(error)
        );
    }

    /**
     * Loads a roster when the caller already has the backend class and stream IDs.
     */
    public void setClassData(Long classId, Long streamId, String className,
                             int studentCount, Runnable backCallback) {
        this.backCallback = backCallback;
        updateClassLabels(className, studentCount);
        loadStudents(classId, streamId);
    }

    private void updateClassLabels(String className, int studentCount) {
        if (classNameLabel != null) {
            classNameLabel.setText(className);
        }
        if (studentCountLabel != null) {
            studentCountLabel.setText(studentCount + " students");
        }
    }

    private ClassIds resolveClassIds(String displayName) {
        TeacherClassResponse[] classes = ApiClient.getInstance().get(
                "/teacher/classes", TeacherClassResponse[].class);
        return resolveClassIdsFromList(classes, displayName);
    }

    static ClassIds resolveClassIdsFromList(TeacherClassResponse[] classes, String displayName) {
        if (classes == null) {
            throw new IllegalStateException("The server returned no class list.");
        }

        List<ClassIds> matches = new ArrayList<>();
        for (TeacherClassResponse teacherClass : classes) {
            if (teacherClass == null || teacherClass.getClassId() == null) {
                continue;
            }
            String resolvedName = joinClassAndStream(
                    teacherClass.getClassName(), teacherClass.getStreamName());
            if (resolvedName.equalsIgnoreCase(displayName == null ? "" : displayName.trim())) {
                ClassIds ids = new ClassIds(teacherClass.getClassId(), teacherClass.getStreamId());
                if (!matches.contains(ids)) {
                    matches.add(ids);
                }
            }
        }

        if (matches.isEmpty()) {
            throw new IllegalStateException(
                    "Could not find backend IDs for class \"" + displayName + "\".");
        }
        if (matches.size() > 1) {
            throw new IllegalStateException(
                    "More than one class assignment matches \"" + displayName + "\".");
        }
        return matches.get(0);
    }

    static String joinClassAndStream(String className, String streamName) {
        String name = className == null ? "" : className.trim();
        String stream = streamName == null ? "" : streamName.trim();
        return stream.isEmpty() ? name : name + " " + stream;
    }

    static String buildRosterEndpointPath(Long classId, Long streamId) {
        if (classId == null) {
            throw new IllegalArgumentException("Class ID is required.");
        }
        if (streamId == null) {
            return "/teacher/classes/" + classId + "/students";
        }
        return "/teacher/classes/" + classId + "/streams/" + streamId + "/students";
    }

    private void loadStudents(Long classId, Long streamId) {
        allStudents.clear();
        if (classId == null) {
            showError(new IllegalArgumentException("Class ID is required."));
            return;
        }
        setStatus("Loading roster...", "Please wait while the students are loaded.");

        String path = buildRosterEndpointPath(classId, streamId);
        TaskRunner.run(
            () -> ApiClient.getInstance().get(path, StudentResponse[].class),
            this::showStudents,
            error -> {
                if (streamId == null) {
                    log.warn("Whole-class roster endpoint {} unavailable for classId={}: {}",
                            path, classId, error.getMessage());
                    setStatus("Whole-class roster unavailable",
                            "This class assignment covers the whole class (no specific stream). "
                            + "Loading a whole-class roster requires backend support for "
                            + "GET /api/v1" + path + ".");
                } else {
                    showError(error);
                }
            }
        );
    }

    private void showStudents(StudentResponse[] response) {
        List<StudentRow> rows = new ArrayList<>();
        if (response != null) {
            for (StudentResponse student : response) {
                if (student == null) {
                    continue;
                }
                rows.add(new StudentRow(
                        Integer.toString(rows.size() + 1),
                        student.admissionNumber() == null ? "" : student.admissionNumber(),
                        student.fullName() == null ? "" : student.fullName(),
                        student.gender() == null ? "" : student.gender(),
                        "—"));
            }
        }

        allStudents.setAll(rows);
        if (rows.isEmpty()) {
            setStatus("No students enrolled", "This class currently has no students.");
        } else {
            updateEmptyState();
            if (studentCountLabel != null) {
                studentCountLabel.setText(rows.size() + " students");
            }
        }
    }

    private void showError(Exception error) {
        log.error("Unable to load class roster", error);
        String message = error.getMessage();
        setStatus("Unable to load roster",
                message == null || message.isBlank()
                        ? "Please check your connection and try again."
                        : message);
    }

    private void setStatus(String title, String message) {
        if (emptyState.getChildren().size() >= 2
                && emptyState.getChildren().get(0) instanceof Label titleLabel
                && emptyState.getChildren().get(1) instanceof Label messageLabel) {
            titleLabel.setText(title);
            messageLabel.setText(message);
        }
        emptyState.setVisible(true);
        emptyState.setManaged(true);
        rosterTable.setVisible(false);
        rosterTable.setManaged(false);
    }

    record ClassIds(Long classId, Long streamId) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record StudentResponse(
            @JsonProperty("id") Long id,
            @JsonProperty("admissionNumber") String admissionNumber,
            @JsonProperty("fullName") String fullName,
            @JsonProperty("gender") String gender) {}

    // -------------------------------------------------------------------------
    // Event handlers
    // -------------------------------------------------------------------------

    @FXML
    private void handleBack() {
        if (backCallback != null) {
            backCallback.run();
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void setupTableColumns() {
        numberColumn.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getNumber())
        );
        admNoColumn.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getAdmNo())
        );
        nameColumn.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getName())
        );
        genderColumn.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getGender())
        );
        statusColumn.setCellValueFactory(
                d -> new SimpleStringProperty(d.getValue().getStatus())
        );
    }

    private void setupSearch() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String term = newVal == null ? "" : newVal.trim().toLowerCase();

            filteredStudents.setPredicate(row -> {
                if (term.isEmpty()) return true;
                return row.getName().toLowerCase().contains(term)
                    || row.getAdmNo().toLowerCase().contains(term);
            });

            updateEmptyState();
        });
    }

    private void updateEmptyState() {
        boolean isEmpty = filteredStudents.isEmpty();
        emptyState.setVisible(isEmpty);
        emptyState.setManaged(isEmpty);
        rosterTable.setVisible(!isEmpty);
        rosterTable.setManaged(!isEmpty);
    }
}
