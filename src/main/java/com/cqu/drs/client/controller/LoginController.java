package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientApp;
import com.cqu.drs.client.ClientConfig;
import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.ServerConnection;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.User;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.Map;

/**
 * Controller for the login screen: connects to the server, authenticates,
 * and lets members of the public create a reporter account.
 */
public class LoginController {

    @FXML private TextField serverField;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML private VBox registerBox;
    @FXML private TextField regFullNameField;
    @FXML private TextField regUsernameField;
    @FXML private PasswordField regPasswordField;

    @FXML
    public void initialize() {
        serverField.setText(ClientConfig.serverAddress());
        registerBox.setVisible(false);
        registerBox.setManaged(false);
        passwordField.setOnAction(e -> login());
    }

    /** Creates the connection object for the address typed in the Server box. */
    private boolean prepareConnection() {
        String[] parts = serverField.getText().trim().split(":");
        try {
            String host = parts[0];
            int port = parts.length > 1 ? Integer.parseInt(parts[1]) : 5050;
            ServerConnection current = ClientSession.getConnection();
            if (current == null || !current.getHost().equals(host) || current.getPort() != port) {
                if (current != null) {
                    current.close();
                }
                ClientSession.setConnection(new ServerConnection(host, port));
            }
            return true;
        } catch (NumberFormatException e) {
            Ui.status(statusLabel, false, "Server must be host:port, e.g. localhost:5050");
            return false;
        }
    }

    @FXML
    private void login() {
        if (usernameField.getText().isBlank() || passwordField.getText().isEmpty()) {
            Ui.status(statusLabel, false, "Enter your username and password");
            return;
        }
        if (!prepareConnection()) {
            return;
        }
        loginButton.setDisable(true);
        statusLabel.setText("Connecting...");
        Request req = new Request(Command.LOGIN)
                .with("username", usernameField.getText().trim())
                .with("password", passwordField.getText());
        ServerCall.send(req, resp -> {
            loginButton.setDisable(false);
            if (!resp.isSuccess()) {
                Ui.status(statusLabel, resp);
                passwordField.clear();
                return;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) resp.getData();
            ClientSession.getConnection().setSessionToken((String) data.get("token"));
            ClientSession.setUser((User) data.get("user"));
            try {
                ClientApp.showMain();
            } catch (IOException e) {
                Ui.status(statusLabel, false, "Cannot open main window: " + e.getMessage());
            }
        });
    }

    @FXML
    private void toggleRegister() {
        boolean show = !registerBox.isVisible();
        registerBox.setVisible(show);
        registerBox.setManaged(show);
    }

    @FXML
    private void register() {
        if (!prepareConnection()) {
            return;
        }
        Request req = new Request(Command.REGISTER_PUBLIC_USER)
                .with("fullName", regFullNameField.getText())
                .with("username", regUsernameField.getText().trim())
                .with("password", regPasswordField.getText());
        ServerCall.send(req, resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                usernameField.setText(regUsernameField.getText().trim());
                regFullNameField.clear();
                regUsernameField.clear();
                regPasswordField.clear();
                toggleRegister();
            }
        });
    }
}
