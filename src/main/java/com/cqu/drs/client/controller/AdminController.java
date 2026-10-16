package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientApp;
import com.cqu.drs.client.LegacyDataReader;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.AuditEntry;
import com.cqu.drs.model.Role;
import com.cqu.drs.model.User;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.security.AccessControl;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Administration: user accounts and roles (access rights), the signed audit
 * log with integrity verification, the DRS-Initial data import, and the
 * permission matrix.
 */
public class AdminController {

    // users
    @FXML private TableView<User> usersTable;
    @FXML private TableColumn<User, Integer> uIdCol;
    @FXML private TableColumn<User, String> uNameCol;
    @FXML private TableColumn<User, String> uFullCol;
    @FXML private TableColumn<User, Role> uRoleCol;
    @FXML private TableColumn<User, String> uActiveCol;
    @FXML private TableColumn<User, LocalDateTime> uCreatedCol;
    @FXML private TextField newUsernameField;
    @FXML private TextField newFullNameField;
    @FXML private PasswordField newPasswordField;
    @FXML private ComboBox<Role> newRoleCombo;

    // audit
    @FXML private TableView<AuditEntry> auditTable;
    @FXML private TableColumn<AuditEntry, Long> auIdCol;
    @FXML private TableColumn<AuditEntry, LocalDateTime> auTimeCol;
    @FXML private TableColumn<AuditEntry, String> auUserCol;
    @FXML private TableColumn<AuditEntry, String> auActionCol;
    @FXML private TableColumn<AuditEntry, String> auDetailsCol;
    @FXML private TableColumn<AuditEntry, String> auHashCol;
    @FXML private TableColumn<AuditEntry, String> auVerifiedCol;
    @FXML private Label auditResultLabel;

    // import
    @FXML private TextArea importLogArea;

    // permissions
    @FXML private TableView<Map.Entry<Command, Role>> permTable;
    @FXML private TableColumn<Map.Entry<Command, Role>, Command> permCmdCol;
    @FXML private TableColumn<Map.Entry<Command, Role>, Role> permRoleCol;

    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        Ui.column(uIdCol, User::getUserId);
        Ui.column(uNameCol, User::getUsername);
        Ui.column(uFullCol, User::getFullName);
        Ui.column(uRoleCol, User::getRole);
        Ui.column(uActiveCol, u -> u.isActive() ? "Active" : "Disabled");
        Ui.dateColumn(uCreatedCol, User::getCreatedAt);
        newRoleCombo.setItems(FXCollections.observableArrayList(Role.values()));
        newRoleCombo.getSelectionModel().select(Role.FIELD_OFFICER);

        Ui.column(auIdCol, AuditEntry::getAuditId);
        Ui.dateColumn(auTimeCol, AuditEntry::getTimestamp);
        Ui.column(auUserCol, AuditEntry::getUsername);
        Ui.column(auActionCol, AuditEntry::getAction);
        Ui.column(auDetailsCol, AuditEntry::getDetails);
        Ui.column(auHashCol, e -> e.getEntryHash() == null ? "" : e.getEntryHash().substring(0, 16) + "...");
        Ui.column(auVerifiedCol, e -> e.getVerified() == null ? "not checked"
                : e.getVerified() ? "✔ valid" : "✖ TAMPERED");
        auVerifiedCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("status-ok", "status-error");
                if (!empty && item != null && item.contains("valid")) {
                    getStyleClass().add("status-ok");
                } else if (!empty && item != null && item.contains("TAMPERED")) {
                    getStyleClass().add("status-error");
                }
            }
        });

        Ui.column(permCmdCol, Map.Entry::getKey);
        Ui.column(permRoleCol, Map.Entry::getValue);
        List<Map.Entry<Command, Role>> perms = new ArrayList<>(AccessControl.permissionTable().entrySet());
        perms.sort(Map.Entry.comparingByValue());
        permTable.setItems(FXCollections.observableArrayList(perms));

        refreshUsers();
        refreshAudit();
    }

    @FXML
    private void refreshUsers() {
        ServerCall.send(new Request(Command.LIST_USERS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<User> list = (List<User>) resp.getData();
                usersTable.setItems(FXCollections.observableArrayList(list));
            } else {
                Ui.status(statusLabel, resp);
            }
        });
    }

    @FXML
    private void createUser() {
        Request req = new Request(Command.CREATE_USER).with("username", newUsernameField.getText().trim())
                .with("fullName", newFullNameField.getText()).with("password", newPasswordField.getText())
                .with("role", newRoleCombo.getValue());
        ServerCall.send(req, resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                newUsernameField.clear();
                newFullNameField.clear();
                newPasswordField.clear();
                refreshUsers();
            }
        });
    }

    @FXML
    private void enableUser() {
        setActive(true);
    }

    @FXML
    private void disableUser() {
        setActive(false);
    }

    private void setActive(boolean active) {
        User u = usersTable.getSelectionModel().getSelectedItem();
        if (u == null) {
            Ui.status(statusLabel, false, "Select a user");
            return;
        }
        ServerCall.send(new Request(Command.SET_USER_ACTIVE).with("userId", u.getUserId()).with("active", active), resp -> {
            Ui.status(statusLabel, resp);
            refreshUsers();
        });
    }

    @FXML
    private void refreshAudit() {
        ServerCall.send(new Request(Command.LIST_AUDIT_LOG), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<AuditEntry> list = (List<AuditEntry>) resp.getData();
                auditTable.setItems(FXCollections.observableArrayList(list));
                auditResultLabel.setText(list.size() + " entries. Click 'Verify integrity' to check the hash chain and signatures.");
            }
        });
    }

    @FXML
    private void verifyAudit() {
        ServerCall.send(new Request(Command.VERIFY_AUDIT_LOG), resp -> {
            Ui.status(auditResultLabel, resp);
            if (resp.getData() != null) {
                @SuppressWarnings("unchecked")
                List<AuditEntry> list = (List<AuditEntry>) resp.getData();
                auditTable.setItems(FXCollections.observableArrayList(list));
            }
        });
    }

    @FXML
    private void importBundled() {
        try {
            sendImport(LegacyDataReader.readBundled(), "bundled DRS-Initial data");
        } catch (IOException e) {
            Ui.status(statusLabel, false, e.getMessage());
        }
    }

    @FXML
    private void importFromFolder() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose the folder with the DRS-Initial CSV files");
        File start = new File("data/drs-initial");
        if (start.isDirectory()) {
            chooser.setInitialDirectory(start.getAbsoluteFile());
        }
        File dir = chooser.showDialog(ClientApp.getPrimaryStage());
        if (dir == null) {
            return;
        }
        try {
            sendImport(LegacyDataReader.readFolder(dir.toPath()), dir.getAbsolutePath());
        } catch (IOException e) {
            Ui.status(statusLabel, false, e.getMessage());
        }
    }

    private void sendImport(HashMap<String, List<String[]>> data, String source) {
        StringBuilder sb = new StringBuilder("Read from " + source + ":\n");
        data.forEach((k, v) -> sb.append("  ").append(k).append(": ").append(v.size()).append(" row(s)\n"));
        sb.append("Sending to server...\n");
        importLogArea.setText(sb.toString());
        HashMap<String, ArrayList<String[]>> payload = new HashMap<>();
        data.forEach((k, v) -> payload.put(k, new ArrayList<>(v)));
        ServerCall.send(new Request(Command.IMPORT_LEGACY_DATA).with("data", (Serializable) payload), resp -> {
            importLogArea.appendText((resp.isSuccess() ? "SUCCESS: " : "FAILED: ") + resp.getMessage() + "\n");
            Ui.status(statusLabel, resp);
            refreshAudit();
        });
    }
}
