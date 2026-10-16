package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.AlertLevel;
import com.cqu.drs.model.AlertStatus;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.EvacuationAlert;
import com.cqu.drs.model.Shelter;
import com.cqu.drs.model.Status;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.util.EvacuationPlanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NEW FEATURE 1 GUI - Evacuation Warning &amp; Shelter Management.
 * Warnings tab: issue tiered evacuation warnings and lift them.
 * Shelters tab: shelter capacity/occupancy, check-in/out, recommendation, add shelter.
 */
public class EvacuationController {

    // ---- warnings tab ----
    @FXML private VBox issueBox;
    @FXML private ComboBox<DisasterReport> reportCombo;
    @FXML private TextField zoneField;
    @FXML private ComboBox<AlertLevel> levelCombo;
    @FXML private Label recommendedLabel;
    @FXML private ComboBox<Shelter> alertShelterCombo;
    @FXML private TextArea messageArea;
    @FXML private Button liftButton;

    @FXML private TableView<EvacuationAlert> alertsTable;
    @FXML private TableColumn<EvacuationAlert, Integer> alertIdCol;
    @FXML private TableColumn<EvacuationAlert, AlertLevel> alertLevelCol;
    @FXML private TableColumn<EvacuationAlert, String> alertZoneCol;
    @FXML private TableColumn<EvacuationAlert, String> alertReportCol;
    @FXML private TableColumn<EvacuationAlert, String> alertShelterCol;
    @FXML private TableColumn<EvacuationAlert, AlertStatus> alertStatusCol;
    @FXML private TableColumn<EvacuationAlert, String> alertByCol;
    @FXML private TableColumn<EvacuationAlert, LocalDateTime> alertAtCol;
    @FXML private TextArea alertMessageView;

    // ---- shelters tab ----
    @FXML private Tab sheltersTab;
    @FXML private TableView<Shelter> sheltersTable;
    @FXML private TableColumn<Shelter, String> shNameCol;
    @FXML private TableColumn<Shelter, String> shLocationCol;
    @FXML private TableColumn<Shelter, Double> shOccupancyCol;
    @FXML private TableColumn<Shelter, String> shCountCol;
    @FXML private TableColumn<Shelter, Integer> shFreeCol;
    @FXML private TableColumn<Shelter, String> shStatusCol;
    @FXML private TableColumn<Shelter, String> shContactCol;

    @FXML private VBox occupancyBox;
    @FXML private TextField peopleField;
    @FXML private TextField recommendZoneField;
    @FXML private TextField recommendPeopleField;
    @FXML private VBox addShelterBox;
    @FXML private TextField shelterNameField;
    @FXML private TextField shelterLocationField;
    @FXML private TextField shelterCapacityField;
    @FXML private TextField shelterContactField;

    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        levelCombo.setItems(FXCollections.observableArrayList(AlertLevel.values()));
        Ui.reportCombo(reportCombo);
        reportCombo.valueProperty().addListener((o, a, r) -> onReportSelected(r));

        Ui.column(alertIdCol, EvacuationAlert::getAlertId);
        Ui.column(alertLevelCol, EvacuationAlert::getLevel);
        Ui.column(alertZoneCol, EvacuationAlert::getZone);
        Ui.column(alertReportCol, EvacuationAlert::getReportSummary);
        Ui.column(alertShelterCol, a -> a.getShelterName() == null ? "-" : a.getShelterName());
        Ui.column(alertStatusCol, EvacuationAlert::getStatus);
        Ui.column(alertByCol, EvacuationAlert::getIssuedBy);
        Ui.dateColumn(alertAtCol, EvacuationAlert::getIssuedAt);
        alertLevelCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(AlertLevel item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("alert-ADVICE", "alert-WATCH_AND_ACT", "alert-EMERGENCY_WARNING");
                setText(empty || item == null ? null : item.getLabel());
                if (!empty && item != null) {
                    getStyleClass().add("alert-" + item.name());
                }
            }
        });
        alertsTable.getSelectionModel().selectedItemProperty().addListener((o, a, al) ->
                alertMessageView.setText(al == null ? "" : al.getMessage()
                        + (al.getLiftedAt() == null ? "" : "\nLifted " + Ui.format(al.getLiftedAt()))));

        Ui.column(shNameCol, Shelter::getName);
        Ui.column(shLocationCol, Shelter::getLocation);
        Ui.column(shOccupancyCol, Shelter::getOccupancyRatio);
        Ui.column(shCountCol, s -> s.getOccupancy() + " / " + s.getCapacity());
        Ui.column(shFreeCol, Shelter::getFreeSpaces);
        Ui.column(shStatusCol, Shelter::getStatus);
        Ui.column(shContactCol, Shelter::getContact);
        shOccupancyCol.setCellFactory(c -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    bar.setProgress(item);
                    bar.setPrefWidth(140);
                    bar.getStyleClass().removeAll("bar-ok", "bar-warn", "bar-full");
                    bar.getStyleClass().add(item >= 1 ? "bar-full" : item >= 0.8 ? "bar-warn" : "bar-ok");
                    setGraphic(bar);
                }
            }
        });

        boolean canIssue = ClientSession.can(Command.ISSUE_ALERT);
        issueBox.setDisable(!canIssue);
        liftButton.setDisable(!ClientSession.can(Command.LIFT_ALERT));
        occupancyBox.setDisable(!ClientSession.can(Command.CHECK_IN_EVACUEES));
        addShelterBox.setDisable(!ClientSession.can(Command.ADD_SHELTER));
        if (!canIssue) {
            recommendedLabel.setText("Only coordinators can issue warnings.");
        }
        refresh();
    }

    private void onReportSelected(DisasterReport r) {
        if (r == null) {
            return;
        }
        AlertLevel rec = EvacuationPlanner.recommendAlertLevel(r.getSeverity());
        levelCombo.setValue(rec);
        if (zoneField.getText().isBlank()) {
            zoneField.setText(r.getLocation());
        }
        recommendedLabel.setText("Recommended for severity " + (r.getSeverity() == null ? "not assessed" : r.getSeverity())
                + ": " + rec.getLabel());
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_ALERTS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<EvacuationAlert> list = (List<EvacuationAlert>) resp.getData();
                alertsTable.setItems(FXCollections.observableArrayList(list));
            } else {
                Ui.status(statusLabel, resp);
            }
        });
        ServerCall.send(new Request(Command.LIST_SHELTERS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Shelter> list = (List<Shelter>) resp.getData();
                Shelter selected = sheltersTable.getSelectionModel().getSelectedItem();
                sheltersTable.setItems(FXCollections.observableArrayList(list));
                if (selected != null) {
                    list.stream().filter(s -> s.getShelterId() == selected.getShelterId()).findFirst()
                            .ifPresent(s -> sheltersTable.getSelectionModel().select(s));
                }
                alertShelterCombo.setItems(FXCollections.observableArrayList(
                        list.stream().filter(s -> s.canAccept(1)).collect(Collectors.toList())));
            }
        });
        if (ClientSession.can(Command.LIST_REPORTS)) {
            ServerCall.send(new Request(Command.LIST_REPORTS), resp -> {
                if (resp.isSuccess()) {
                    @SuppressWarnings("unchecked")
                    List<DisasterReport> list = (List<DisasterReport>) resp.getData();
                    reportCombo.setItems(FXCollections.observableArrayList(list.stream()
                            .filter(r -> r.getStatus() != Status.RESOLVED).collect(Collectors.toList())));
                }
            });
        }
    }

    @FXML
    private void draftMessage() {
        DisasterReport r = reportCombo.getValue();
        if (r == null || levelCombo.getValue() == null || zoneField.getText().isBlank()) {
            Ui.status(statusLabel, false, "Choose a report, zone and level first");
            return;
        }
        messageArea.setText(EvacuationPlanner.draftMessage(r, levelCombo.getValue(), zoneField.getText().trim(),
                alertShelterCombo.getValue()));
    }

    @FXML
    private void issueAlert() {
        DisasterReport r = reportCombo.getValue();
        if (r == null || zoneField.getText().isBlank()) {
            Ui.status(statusLabel, false, "Choose the disaster report and enter the affected zone");
            return;
        }
        Request req = new Request(Command.ISSUE_ALERT).with("reportId", r.getReportId())
                .with("zone", zoneField.getText()).with("level", levelCombo.getValue())
                .with("message", messageArea.getText());
        if (alertShelterCombo.getValue() != null) {
            req.with("shelterId", alertShelterCombo.getValue().getShelterId());
        }
        ServerCall.send(req, resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                messageArea.clear();
                zoneField.clear();
                refresh();
            }
        });
    }

    @FXML
    private void liftAlert() {
        EvacuationAlert a = alertsTable.getSelectionModel().getSelectedItem();
        if (a == null) {
            Ui.status(statusLabel, false, "Select an alert to lift");
            return;
        }
        ServerCall.send(new Request(Command.LIFT_ALERT).with("alertId", a.getAlertId()), resp -> {
            Ui.status(statusLabel, resp);
            refresh();
        });
    }

    @FXML
    private void checkIn() {
        changeOccupancy(Command.CHECK_IN_EVACUEES);
    }

    @FXML
    private void checkOut() {
        changeOccupancy(Command.CHECK_OUT_EVACUEES);
    }

    private void changeOccupancy(Command cmd) {
        Shelter s = sheltersTable.getSelectionModel().getSelectedItem();
        Integer people = Ui.parseInt(peopleField);
        if (s == null) {
            Ui.status(statusLabel, false, "Select a shelter in the table");
            return;
        }
        if (people == null || people <= 0) {
            Ui.status(statusLabel, false, "Number of evacuees must be a whole number greater than zero");
            return;
        }
        ServerCall.send(new Request(cmd).with("shelterId", s.getShelterId()).with("people", people), resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                peopleField.clear();
            }
            refresh();
        });
    }

    @FXML
    private void recommendShelter() {
        Integer people = Ui.parseInt(recommendPeopleField);
        if (people == null || people <= 0) {
            Ui.status(statusLabel, false, "Enter how many people need shelter");
            return;
        }
        ServerCall.send(new Request(Command.RECOMMEND_SHELTER).with("zone", recommendZoneField.getText())
                .with("people", people), resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                Shelter rec = (Shelter) resp.getData();
                sheltersTable.getItems().stream().filter(s -> s.getShelterId() == rec.getShelterId()).findFirst()
                        .ifPresent(s -> sheltersTable.getSelectionModel().select(s));
                peopleField.setText(String.valueOf(people));
            }
        });
    }

    @FXML
    private void addShelter() {
        Integer cap = Ui.parseInt(shelterCapacityField);
        if (shelterNameField.getText().isBlank() || shelterLocationField.getText().isBlank() || cap == null || cap <= 0) {
            Ui.status(statusLabel, false, "Shelter name, location and a positive capacity are required");
            return;
        }
        ServerCall.send(new Request(Command.ADD_SHELTER).with("name", shelterNameField.getText())
                .with("location", shelterLocationField.getText()).with("capacity", cap)
                .with("contact", shelterContactField.getText()), resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                shelterNameField.clear();
                shelterLocationField.clear();
                shelterCapacityField.clear();
                shelterContactField.clear();
                refresh();
            }
        });
    }
}
