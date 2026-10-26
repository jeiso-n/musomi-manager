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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.musomi.desktop.api.AdminUserApi;
import com.musomi.desktop.model.UserResponse;
import com.musomi.desktop.util.Modal;
import com.musomi.desktop.util.TaskRunner;
import com.musomi.desktop.util.Toast;

@RequiredArgsConstructor
@Slf4j
public class UsersController {

    private final AdminUserApi adminUserApi = new AdminUserApi();

    @FXML
    private TableView<UserRow> usersTable;

    @FXML
    private TableColumn<UserRow, String> nameColumn;

    @FXML
    private TableColumn<UserRow, String> usernameColumn;

    @FXML
    private TableColumn<UserRow, String> roleColumn;

    @FXML
    private TableColumn<UserRow, String> statusColumn;

    @FXML
    private TableColumn<UserRow, String> actionsColumn;

    @FXML
    private TextField searchField;

    private final ObservableList<UserRow> allUsers =
            FXCollections.observableArrayList();

    @FXML
    private void initialize() {

        nameColumn.setCellValueFactory(
                data -> data.getValue().nameProperty()
        );

        usernameColumn.setCellValueFactory(
                data -> data.getValue().usernameProperty()
        );

        roleColumn.setCellValueFactory(
                data -> data.getValue().roleProperty()
        );

        statusColumn.setCellValueFactory(
                data -> data.getValue().statusProperty()
        );

        actionsColumn.setCellFactory(column -> new TableCell<>() {

            private final Button editButton =
                    new Button("Edit");

            private final Button deactivateButton =
                    new Button("Deactivate");

            private final HBox container =
                    new HBox(8, editButton, deactivateButton);

            {
                // Standard table action styling
                editButton.getStyleClass().add("table-action-button");

                deactivateButton.getStyleClass().addAll(
                        "table-action-button",
                        "table-action-danger"
                );

                editButton.setOnAction(event -> {

                    UserRow user = getTableView()
                            .getItems()
                            .get(getIndex());

                    handleEditUser(user);
                });

                deactivateButton.setOnAction(event -> {

                    UserRow user = getTableView()
                            .getItems()
                            .get(getIndex());

                    handleDeactivateUser(user);
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
        });

        usersTable.setItems(allUsers);
        loadUsersFromBackend();

        searchField.textProperty().addListener(
                (observable, oldValue, newValue) ->
                        filterUsers(newValue)
        );
        usersTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );
    }

    private void loadUsersFromBackend() {
        TaskRunner.run(
                () -> adminUserApi.getUsers(0, 100, ""),
                result -> {
                    List<UserRow> users = result.data().stream()
                            .map(this::toUserRow)
                            .toList();
                    allUsers.setAll(users);
                    filterUsers(searchField.getText());
                },
                error -> {
                    log.error("Failed to load users", error);
                    Toast.error(errorMessage(error));
                }
        );
    }

    private UserRow toUserRow(UserResponse user) {
        return new UserRow(
                user.id(),
                user.fullName(),
                user.username(),
                formatRole(user.role()),
                user.isActive() ? "Active" : "Inactive"
        );
    }

    private static String formatRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }
        return switch (role) {
            case "ADMIN" -> "Administrator";
            case "TEACHER" -> "Teacher";
            case "STUDENT" -> "Student";
            default -> role;
        };
    }

    private static String errorMessage(Exception error) {
        return error.getMessage() == null || error.getMessage().isBlank()
                ? "Unable to load users."
                : error.getMessage();
    }

    private void filterUsers(String searchText) {

        String search = searchText.toLowerCase().trim();

        if (search.isEmpty()) {
            usersTable.setItems(allUsers);
            return;
        }

        ObservableList<UserRow> filteredUsers =
                FXCollections.observableArrayList();

        for (UserRow user : allUsers) {

            if (user.getName().toLowerCase().contains(search)
                    || user.getUsername()
                    .toLowerCase()
                    .contains(search)
                    || user.getRole()
                    .toLowerCase()
                    .contains(search)
                    || user.getStatus()
                    .toLowerCase()
                    .contains(search)) {

                filteredUsers.add(user);
            }
        }

        usersTable.setItems(filteredUsers);
    }

    @FXML
    private void showAddUserDialog() {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/add-user.fxml"
                    )
            );

            Parent root = loader.load();

            AddUserController controller =
                    loader.getController();

            Stage dialog = new Stage();

            dialog.setTitle("Add User");

            dialog.initModality(
                    Modality.APPLICATION_MODAL
            );

            Scene scene = createStyledScene(root);

            dialog.setScene(scene);

            dialog.setResizable(false);

            dialog.showAndWait();
            if (controller.isUserCreated()) {
                loadUsersFromBackend();
            }

        } catch (Exception e) {
            log.error("Unable to open add-user dialog", e);
            Toast.error(errorMessage(e));
        }
        loadUsersFromBackend();
    }

    private void handleEditUser(UserRow user) {

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/fxml/admin/edit-user.fxml"
                    )
            );

            Parent root = loader.load();

            EditUserController controller =
                    loader.getController();

            controller.setUser(user);

            Stage dialog = new Stage();

            dialog.setTitle("Edit User");

            dialog.initModality(
                    Modality.APPLICATION_MODAL
            );

            Scene scene = createStyledScene(root);

            dialog.setScene(scene);

            dialog.setResizable(false);

            dialog.showAndWait();
            if (controller.isSaved()) {
                TaskRunner.run(
                        () -> adminUserApi.updateUser(user.getId(), controller.getFullName(), null, null),
                        ignored -> {
                            Toast.success("User updated");
                            loadUsersFromBackend();
                        },
                        error -> Toast.error(errorMessage(error)));
            }

        } catch (Exception e) {
            log.error("Unable to open edit-user dialog", e);
            Toast.error(errorMessage(e));
        }
        loadUsersFromBackend();
    }

    /**
     * Creates a Scene for Admin dialogs and applies
     * the same CSS used by the main application.
     */
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

    private void handleDeactivateUser(UserRow user) {
        if (!Modal.confirm("Deactivate " + user.getName() + "?")) {
            return;
        }
        TaskRunner.run(
                () -> {
                    adminUserApi.deactivateUser(user.getId());
                    return null;
                },
                ignored -> {
                    Toast.success("User deactivated");
                    loadUsersFromBackend();
                },
                error -> {
                    log.error("Failed to deactivate user {}", user.getId(), error);
                    Toast.error(errorMessage(error));
                }
        );
    }

    public static class UserRow {

        private final Long id;

        private final SimpleStringProperty name;

        private final SimpleStringProperty username;

        private final SimpleStringProperty role;

        private final SimpleStringProperty status;

        public UserRow(
                Long id,
                String name,
                String username,
                String role,
                String status
        ) {

            this.id = id;
            this.name =
                    new SimpleStringProperty(name);

            this.username =
                    new SimpleStringProperty(username);

            this.role =
                    new SimpleStringProperty(role);

            this.status =
                    new SimpleStringProperty(status);
        }

        public SimpleStringProperty nameProperty() {
            return name;
        }

        public Long getId() {
            return id;
        }

        public SimpleStringProperty usernameProperty() {
            return username;
        }

        public SimpleStringProperty roleProperty() {
            return role;
        }

        public SimpleStringProperty statusProperty() {
            return status;
        }

        public String getName() {
            return name.get();
        }

        public String getUsername() {
            return username.get();
        }

        public String getRole() {
            return role.get();
        }

        public String getStatus() {
            return status.get();
        }

        public void setName(String name) {
            this.name.set(name);
        }

        public void setUsername(String username) {
            this.username.set(username);
        }

        public void setRole(String role) {
            this.role.set(role);
        }

        public void setStatus(String status) {
            this.status.set(status);
        }
    }
}