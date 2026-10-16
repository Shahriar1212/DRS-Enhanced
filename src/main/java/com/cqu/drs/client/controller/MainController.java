package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientApp;
import com.cqu.drs.client.ClientSession;
import com.cqu.drs.model.Role;
import com.cqu.drs.model.User;
import com.cqu.drs.protocol.Command;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Main window shell: header with the signed-in user, a role-aware navigation
 * sidebar and the content area that each feature view is loaded into.
 */
public class MainController {

    @FXML private Label userLabel;
    @FXML private Label roleLabel;
    @FXML private Label connectionLabel;
    @FXML private StackPane contentPane;

    @FXML private ToggleButton dashboardNav;
    @FXML private ToggleButton reportNav;
    @FXML private ToggleButton coordinationNav;
    @FXML private ToggleButton volunteerNav;
    @FXML private ToggleButton resourceNav;
    @FXML private ToggleButton evacuationNav;
    @FXML private ToggleButton damageNav;
    @FXML private ToggleButton adminNav;
    @FXML private ToggleGroup navGroup;

    @FXML
    public void initialize() {
        User u = ClientSession.getUser();
        userLabel.setText(u.getFullName());
        roleLabel.setText(u.getRole().getLabel());
        connectionLabel.setText("Connected to DRS server " + ClientSession.getConnection().getHost() + ":"
                + ClientSession.getConnection().getPort() + "  |  Signed in as " + u.getUsername()
                + "  |  F12 = save screenshot");

        show(dashboardNav, ClientSession.can(Command.LIST_REPORTS));
        show(reportNav, true);
        show(coordinationNav, ClientSession.can(Command.LIST_TASKS));
        show(volunteerNav, ClientSession.can(Command.LIST_VOLUNTEERS));
        show(resourceNav, ClientSession.can(Command.LIST_RESOURCES));
        show(evacuationNav, true);
        show(damageNav, ClientSession.can(Command.LIST_DAMAGE));
        show(adminNav, ClientSession.can(Command.LIST_AUDIT_LOG));
        evacuationNav.setText(u.getRole() == Role.REPORTER ? "Warnings & Shelters" : "Evacuation & Shelters");

        // keep one nav button selected at all times
        navGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT == null && oldT != null) {
                oldT.setSelected(true);
            }
        });

        if (dashboardNav.isVisible()) {
            dashboardNav.setSelected(true);
            showDashboard();
        } else {
            reportNav.setSelected(true);
            showReport();
        }
    }

    private void show(ToggleButton b, boolean visible) {
        b.setVisible(visible);
        b.setManaged(visible);
    }

    @FXML
    private void showDashboard() {
        load("DashboardView.fxml");
    }

    @FXML
    private void showReport() {
        load("ReportView.fxml");
    }

    @FXML
    private void showCoordination() {
        load("CoordinationView.fxml");
    }

    @FXML
    private void showVolunteers() {
        load("VolunteerView.fxml");
    }

    @FXML
    private void showResources() {
        load("ResourceView.fxml");
    }

    @FXML
    private void showEvacuation() {
        load("EvacuationView.fxml");
    }

    @FXML
    private void showDamage() {
        load("DamageView.fxml");
    }

    @FXML
    private void showAdmin() {
        load("AdminView.fxml");
    }

    @FXML
    private void logout() throws IOException {
        ClientSession.getConnection().logoutAndClose();
        ClientSession.clear();
        ClientApp.showLogin();
    }

    private void load(String fxml) {
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/com/cqu/drs/client/view/" + fxml));
            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load view " + fxml, e);
        }
    }
}
