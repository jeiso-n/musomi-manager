package com.musomi.desktop.controller.teacher;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musomi.desktop.api.ApiClient;
import com.musomi.desktop.model.dto.TeacherClassResponse;
import com.musomi.desktop.util.TaskRunner;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * Controller for the My Classes screen (/fxml/teacher/my-classes.fxml).
 */
public class MyClassesController {

    private static final Logger log = LoggerFactory.getLogger(MyClassesController.class);

    // -------------------------------------------------------------------------
    // Inner model
    // -------------------------------------------------------------------------

    public static class ClassCard {
        private final Long    classId;
        private final Long    streamId;
        private final String  className;
        private final String  subject;
        private final int     students;
        private final int     assessments;
        private final int     draftAssessments;

        public ClassCard(String className, String subject,
                         int students, int assessments, int draftAssessments) {
            this(null, null, className, subject, students, assessments, draftAssessments);
        }

        public ClassCard(Long classId, Long streamId, String className, String subject,
                         int students, int assessments, int draftAssessments) {
            this.classId      = classId;
            this.streamId     = streamId;
            this.className    = className;
            this.subject      = subject;
            this.students     = students;
            this.assessments  = assessments;
            this.draftAssessments = draftAssessments;
        }

        public Long   getClassId()     { return classId;      }
        public Long   getStreamId()    { return streamId;     }
        public String getClassName()   { return className;    }
        public String getSubject()     { return subject;      }
        public int    getStudents()    { return students;     }
        public int    getAssessments() { return assessments;  }
        public int    getDraftAssessments() { return draftAssessments; }
    }

    // -------------------------------------------------------------------------
    // FXML bindings
    // -------------------------------------------------------------------------

    @FXML
    private FlowPane cardsPane;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @FXML
    private void initialize() {
        showStatus("Loading classes...", false);
        TaskRunner.run(
            () -> ApiClient.getInstance().get(
                    "/teacher/classes", TeacherClassResponse[].class),
            this::showClasses,
            error -> {
                log.error("Unable to load teacher classes", error);
                String message = error.getMessage();
                showStatus("Unable to load classes."
                        + (message == null || message.isBlank() ? "" : " " + message), true);
            }
        );
    }

    private void showClasses(TeacherClassResponse[] response) {
        List<ClassCard> classes = new ArrayList<>();
        if (response != null) {
            for (TeacherClassResponse teacherClass : response) {
                if (teacherClass == null) {
                    continue;
                }
                String className = teacherClass.getClassName() == null
                        ? "" : teacherClass.getClassName();
                String streamName = teacherClass.getStreamName();
                if (streamName != null && !streamName.isBlank()) {
                    className = className.isBlank()
                            ? streamName : className + " " + streamName;
                }
                classes.add(new ClassCard(
                        teacherClass.getClassId(),
                        teacherClass.getStreamId(),
                        className,
                        teacherClass.getSubjectName() == null ? "" : teacherClass.getSubjectName(),
                        teacherClass.getStudentCount(),
                        teacherClass.getAssessmentCount(),
                        teacherClass.getDraftCount()));
            }
        }

        if (classes.isEmpty()) {
            showStatus("No classes are assigned to your account.", false);
            return;
        }
        buildCards(classes);
    }

    private void showStatus(String message, boolean error) {
        Label status = new Label(message);
        status.setWrapText(true);
        status.setMaxWidth(600);
        status.setStyle(error
                ? "-fx-text-fill: #B91C1C; -fx-font-size: 14px;"
                : "-fx-text-fill: #64748B; -fx-font-size: 14px;");
        cardsPane.getChildren().setAll(status);
    }

    // -------------------------------------------------------------------------
    // Card building
    // -------------------------------------------------------------------------

    private void buildCards(List<ClassCard> classes) {
        cardsPane.getChildren().clear();

        for (ClassCard cls : classes) {
            cardsPane.getChildren().add(buildCard(cls));
        }
    }

    /** Builds a single class card matching the Musomi design system. */
    private VBox buildCard(ClassCard cls) {

        // ── Card container ──────────────────────────────────────────────────
        VBox card = new VBox(14);
        card.setPrefWidth(280);
        card.setMaxWidth(320);
        card.setPadding(new Insets(20));
        card.setStyle(
            "-fx-background-color: #FFFFFF;" +
            "-fx-background-radius: 12px;" +
            "-fx-border-color: #E2E8F0;" +
            "-fx-border-radius: 12px;" +
            "-fx-border-width: 1px;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 4, 0, 0, 1);"
        );

        // ── Class name ───────────────────────────────────────────────────────
        Label nameLabel = new Label(cls.getClassName());
        nameLabel.setStyle(
            "-fx-font-size: 20px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #0F172A;"
        );

        // ── Subject ──────────────────────────────────────────────────────────
        Label subjectLabel = new Label(cls.getSubject());
        subjectLabel.setStyle(
            "-fx-font-size: 13px;" +
            "-fx-text-fill: #64748B;"
        );

        // ── Separator region ─────────────────────────────────────────────────
        Region sep = new Region();
        sep.setPrefHeight(1);
        sep.setStyle("-fx-background-color: #F1F5F9;");

        // ── Stats row ────────────────────────────────────────────────────────
        HBox statsRow = new HBox(20);
        statsRow.setAlignment(Pos.CENTER_LEFT);
        statsRow.getChildren().addAll(
            buildStat(String.valueOf(cls.getStudents()), "Students"),
            buildStat(String.valueOf(cls.getAssessments()), "Assessments")
        );

        // ── Pending marks badge ───────────────────────────────────────────────
        HBox pendingRow = new HBox(8);
        pendingRow.setAlignment(Pos.CENTER_LEFT);

        int drafts = cls.getDraftAssessments();
        Label pendingLabel;

        if (drafts > 0) {
            pendingLabel = new Label(drafts + " Draft Assessment" + (drafts == 1 ? "" : "s"));
            pendingLabel.setStyle(
                "-fx-background-color: #FFFBEB;" +
                "-fx-text-fill: #B45309;" +
                "-fx-padding: 4 10;" +
                "-fx-background-radius: 999px;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;"
            );
        } else {
            pendingLabel = new Label("No Draft Assessments");
            pendingLabel.setStyle(
                "-fx-background-color: #ECFDF5;" +
                "-fx-text-fill: #047857;" +
                "-fx-padding: 4 10;" +
                "-fx-background-radius: 999px;" +
                "-fx-font-size: 12px;" +
                "-fx-font-weight: 600;"
            );
        }
        pendingRow.getChildren().add(pendingLabel);

        // ── View Class button ─────────────────────────────────────────────────
        Button viewBtn = new Button("View Class →");
        viewBtn.setMaxWidth(Double.MAX_VALUE);
        viewBtn.setStyle(
            "-fx-background-color: #4F46E5;" +
            "-fx-text-fill: #FFFFFF;" +
            "-fx-background-radius: 8px;" +
            "-fx-padding: 10 18;" +
            "-fx-font-weight: 600;" +
            "-fx-font-size: 14px;" +
            "-fx-cursor: hand;"
        );
        viewBtn.setOnAction(e -> openRoster(cls));
        VBox.setVgrow(viewBtn, Priority.NEVER);

        card.getChildren().addAll(nameLabel, subjectLabel, sep, statsRow, pendingRow, viewBtn);
        return card;
    }

    /** Builds a small vertical stat: big number + label below it. */
    private VBox buildStat(String value, String label) {
        VBox stat = new VBox(2);
        stat.setAlignment(Pos.CENTER_LEFT);

        Label valLabel = new Label(value);
        valLabel.setStyle(
            "-fx-font-size: 22px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #0F172A;"
        );

        Label txtLabel = new Label(label);
        txtLabel.setStyle(
            "-fx-font-size: 12px;" +
            "-fx-text-fill: #64748B;"
        );

        stat.getChildren().addAll(valLabel, txtLabel);
        return stat;
    }

    // -------------------------------------------------------------------------
    // Navigation to Class Roster
    // -------------------------------------------------------------------------

    private javafx.scene.layout.StackPane findContentArea() {
        javafx.scene.Node current = cardsPane;
        while (current != null) {
            if ("contentArea".equals(current.getId()) && current instanceof javafx.scene.layout.StackPane sp) {
                return sp;
            }
            current = current.getParent();
        }
        if (cardsPane != null && cardsPane.getScene() != null) {
            javafx.scene.Node node = cardsPane.getScene().lookup("#contentArea");
            if (node instanceof javafx.scene.layout.StackPane sp) {
                return sp;
            }
        }
        return null;
    }

    private void openRoster(ClassCard cls) {
        try {
            javafx.scene.layout.StackPane sp = findContentArea();
            if (sp == null) {
                log.warn("Could not locate contentArea StackPane");
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/teacher/class-roster.fxml")
            );
            Node roster = loader.load();

            ClassRosterController rosterCtrl = loader.getController();
            if (cls.getClassId() != null) {
                rosterCtrl.setClassData(
                        cls.getClassId(),
                        cls.getStreamId(),
                        cls.getClassName(),
                        cls.getStudents(),
                        () -> returnToMyClasses(sp)
                );
            } else {
                rosterCtrl.setClassData(
                        cls.getClassName(),
                        cls.getStudents(),
                        () -> returnToMyClasses(sp)
                );
            }

            // Replace the content in the parent StackPane
            sp.getChildren().setAll(roster);

        } catch (Exception ex) {
            log.error("Failed to load class-roster.fxml", ex);
        }
    }

    /** Called by ClassRosterController when the Back button is pressed. */
    private void returnToMyClasses(javafx.scene.layout.StackPane sp) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/teacher/my-classes.fxml")
            );
            Node myClasses = loader.load();
            if (sp != null) {
                sp.getChildren().setAll(myClasses);
            }
        } catch (Exception ex) {
            log.error("Failed to return to My Classes view", ex);
        }
    }
}
