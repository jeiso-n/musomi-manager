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
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.musomi.desktop.api.AdminSubjectApi;
import com.musomi.desktop.api.AdminTopicApi;
import com.musomi.desktop.model.SubjectResponse;
import com.musomi.desktop.model.TopicResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class TopicsController {

    private static final Logger log = LoggerFactory.getLogger(TopicsController.class);
    private final AdminSubjectApi adminSubjectApi = new AdminSubjectApi();
    private final AdminTopicApi adminTopicApi = new AdminTopicApi();

    @FXML
    private TableView<TopicRow> topicsTable;

    @FXML
    private TableColumn<TopicRow, String> nameColumn;

    @FXML
    private TableColumn<TopicRow, String> subjectColumn;

    @FXML
    private TableColumn<TopicRow, String> statusColumn;

    @FXML
    private TableColumn<TopicRow, String> actionsColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<TopicRow> allTopics =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        nameColumn.setCellValueFactory(
                data -> data.getValue().nameProperty()
        );

        subjectColumn.setCellValueFactory(
                data -> data.getValue().subjectProperty()
        );

        statusColumn.setCellValueFactory(
                data -> data.getValue().statusProperty()
        );

        actionsColumn.setCellFactory(column ->
                new TableCell<TopicRow, String>() {

                    private final Button editButton =
                            new Button("Edit");

                    private final Button deactivateButton =
                            new Button("Deactivate");

                    private final HBox container =
                            new HBox(
                                    8,
                                    editButton,
                                    deactivateButton
                            );

                    {
                        // Table action button styling
                        editButton.getStyleClass().add(
                                "table-action-button"
                        );

                        deactivateButton.getStyleClass().addAll(
                                "table-action-button",
                                "table-action-danger"
                        );

                        editButton.setOnAction(event -> {

                            TopicRow topic =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            handleEditTopic(topic);
                        });

                        deactivateButton.setOnAction(event -> {

                            TopicRow topic =
                                    getTableView()
                                            .getItems()
                                            .get(getIndex());

                            handleDeactivateTopic(topic);
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

        loadTopicsFromBackend();

        topicsTable.setItems(allTopics);

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filterTopics(newValue)
        );
    }

    private void loadTopicsFromBackend() {
        TaskRunner.run(
                () -> {
                    List<SubjectResponse> subjects = adminSubjectApi.getSubjects();
                    return subjects.stream()
                            .flatMap(subject -> adminTopicApi.getTopics(subject.id()).stream()
                                    .map(topic -> new TopicRow(
                                            topic.id(), topic.subjectId(), topic.name(),
                                            subject.name(), "Active")))
                            .toList();
                },
                topics -> {
                    allTopics.setAll(topics);
                    filterTopics(searchField.getText());
                },
                error -> {
                    log.error("Failed to load topics", error);
                    Toast.error(error.getMessage() == null
                            ? "Unable to load topics." : error.getMessage());
                });
    }

    private void filterTopics(String searchText) {

        String search =
                searchText.toLowerCase().trim();

        if (search.isEmpty()) {

            topicsTable.setItems(allTopics);

            return;
        }

        ObservableList<TopicRow> filteredTopics =
                FXCollections.observableArrayList();

        for (TopicRow topic : allTopics) {

            if (topic.getName()
                    .toLowerCase()
                    .contains(search)

                    || topic.getSubject()
                    .toLowerCase()
                    .contains(search)

                    || topic.getStatus()
                    .toLowerCase()
                    .contains(search)) {

                filteredTopics.add(topic);
            }
        }

        topicsTable.setItems(filteredTopics);
    }

    @FXML
    private void showAddTopicDialog() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/add-topic.fxml"
                    )
            );

            Parent root = loader.load();

            AddTopicController controller =
                    loader.getController();

            Stage dialog = new Stage();

            dialog.setTitle("Add Topic");

            dialog.initModality(
                    Modality.APPLICATION_MODAL
            );

            dialog.setScene(
                    createStyledScene(root)
            );

            dialog.setResizable(false);

            dialog.showAndWait();

            if (controller.isTopicCreated()) {
                loadTopicsFromBackend();
            }

        } catch (Exception e) {

            log.error("Failed to load topic dialog", e);
        }
    }

    private void handleEditTopic(
            TopicRow topic
    ) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/edit-topic.fxml"
                    )
            );

            Parent root = loader.load();

            EditTopicController controller =
                    loader.getController();

            controller.setTopicRow(topic);

            Stage dialog = new Stage();

            dialog.setTitle("Edit Topic");

            dialog.initModality(
                    Modality.APPLICATION_MODAL
            );

            dialog.setScene(
                    createStyledScene(root)
            );

            dialog.setResizable(false);

            dialog.showAndWait();

            if (controller.isSaved() && topic.getId() != null) {
                loadTopicsFromBackend();
            }

            topicsTable.refresh();

        } catch (Exception e) {

            log.error("Failed to load topic editor", e);
        }
    }

    private Scene createStyledScene(Parent root) {

        Scene scene = new Scene(root);

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/styles.css")
                        .toExternalForm()
        );

        scene.getStylesheets().add(
                getClass()
                        .getResource("/css/components.css")
                        .toExternalForm()
        );

        return scene;
    }

    private void handleDeactivateTopic(
            TopicRow topic
    ) {

        TaskRunner.run(
                () -> {
                    adminTopicApi.deactivateTopic(topic.getId());
                    return null;
                },
                ignored -> {
                    Toast.success("Topic deactivated");
                    loadTopicsFromBackend();
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to deactivate topic." : error.getMessage()));
    }

    public static class TopicRow {

        private final Long id;
        private final Long subjectId;
        private final SimpleStringProperty name;

        private final SimpleStringProperty subject;

        private final SimpleStringProperty status;

        public TopicRow(
                String name,
                String subject,
                String status
        ) {
            this(null, null, name, subject, status);
        }

        public TopicRow(
                Long id,
                Long subjectId,
                String name,
                String subject,
                String status
        ) {
            this.id = id;
            this.subjectId = subjectId;

            this.name =
                    new SimpleStringProperty(name);

            this.subject =
                    new SimpleStringProperty(subject);

            this.status =
                    new SimpleStringProperty(status);
        }

        public Long getId() {
            return id;
        }

        public Long getSubjectId() {
            return subjectId;
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public SimpleStringProperty subjectProperty() {
            return subject;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getName() {
            return name.get();
        }

        public String getSubject() {
            return subject.get();
        }

        public String getStatus() {
            return status.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public void setSubject(String subject) {
            this.subject.set(subject);
        }

        public void setStatus(String status) {
            this.status.set(status);
        }
    }
}