package com.musomi.desktop.controller.admin;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import com.musomi.desktop.api.AdminAuditApi;
import com.musomi.desktop.model.AuditLogResponse;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

public class AuditLogController {

    @FXML
    private TableView<AuditRow> auditTable;

    @FXML
    private TableColumn<AuditRow, String> dateColumn;

    @FXML
    private TableColumn<AuditRow, String> userColumn;

    @FXML
    private TableColumn<AuditRow, String> actionColumn;

    @FXML
    private TableColumn<AuditRow, String> moduleColumn;

    @FXML
    private TableColumn<AuditRow, String> descriptionColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<AuditRow> allLogs =
            FXCollections.observableArrayList();
    private final AdminAuditApi adminAuditApi = AdminAuditApi.getInstance();

    @FXML
    private void initialize() {

        dateColumn.setCellValueFactory(
                data -> data.getValue().dateProperty()
        );

        userColumn.setCellValueFactory(
                data -> data.getValue().userProperty()
        );

        actionColumn.setCellValueFactory(
                data -> data.getValue().actionProperty()
        );

        moduleColumn.setCellValueFactory(
                data -> data.getValue().moduleProperty()
        );

        descriptionColumn.setCellValueFactory(
                data -> data.getValue().descriptionProperty()
        );

        auditTable.setItems(allLogs);
        loadLogsFromBackend();

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filterLogs(newValue)
        );
    }

    private void loadLogsFromBackend() {
        TaskRunner.run(
                adminAuditApi::getAuditLogs,
                logs -> {
                    allLogs.setAll(logs.stream()
                            .map(this::toAuditRow)
                            .toList());
                    filterLogs(searchField.getText());
                },
                error -> Toast.error(error.getMessage() == null
                        ? "Unable to load audit logs." : error.getMessage()));
    }

    private AuditRow toAuditRow(AuditLogResponse log) {
        return new AuditRow(log.createdAt(), log.userName(), log.action(),
                log.entityType(), log.details());
    }

    private void filterLogs(String searchText) {

        String search =
                searchText.toLowerCase().trim();

        if (search.isEmpty()) {

            auditTable.setItems(allLogs);

            return;
        }

        ObservableList<AuditRow> filteredLogs =
                FXCollections.observableArrayList();

        for (AuditRow log : allLogs) {

            if (log.getDate().toLowerCase().contains(search)
                    || log.getUser().toLowerCase().contains(search)
                    || log.getAction().toLowerCase().contains(search)
                    || log.getModule().toLowerCase().contains(search)
                    || log.getDescription().toLowerCase().contains(search)) {

                filteredLogs.add(log);
            }
        }

        auditTable.setItems(filteredLogs);
    }

    public static class AuditRow {

        private final SimpleStringProperty date;
        private final SimpleStringProperty user;
        private final SimpleStringProperty action;
        private final SimpleStringProperty module;
        private final SimpleStringProperty description;

        public AuditRow(
                String date,
                String user,
                String action,
                String module,
                String description
        ) {

            this.date = new SimpleStringProperty(date);
            this.user = new SimpleStringProperty(user);
            this.action = new SimpleStringProperty(action);
            this.module = new SimpleStringProperty(module);
            this.description =
                    new SimpleStringProperty(description);
        }

        public SimpleStringProperty dateProperty() {
            return date;
        }

        public SimpleStringProperty userProperty() {
            return user;
        }

        public SimpleStringProperty actionProperty() {
            return action;
        }

        public SimpleStringProperty moduleProperty() {
            return module;
        }

        public SimpleStringProperty descriptionProperty() {
            return description;
        }

        public String getDate() {
            return date.get();
        }

        public String getUser() {
            return user.get();
        }

        public String getAction() {
            return action.get();
        }

        public String getModule() {
            return module.get();
        }

        public String getDescription() {
            return description.get();
        }
    }
}