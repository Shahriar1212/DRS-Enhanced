package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.EvacuationAlert;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Status;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.util.Validation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.List;

/**
 * "Report a Disaster" screen (all users): the report form with client-side
 * validation, the user's own reports, and the currently active warnings.
 */
public class ReportController {

    @FXML private ComboBox<DisasterType> typeCombo;
    @FXML private TextField locationField;
    @FXML private TextArea descriptionArea;
    @FXML private TextField reporterNameField;
    @FXML private TextField reporterPhoneField;
    @FXML private Label statusLabel;

    @FXML private TableView<DisasterReport> myReportsTable;
    @FXML private TableColumn<DisasterReport, Integer> idCol;
    @FXML private TableColumn<DisasterReport, DisasterType> typeCol;
    @FXML private TableColumn<DisasterReport, String> locationCol;
    @FXML private TableColumn<DisasterReport, SeverityLevel> severityCol;
    @FXML private TableColumn<DisasterReport, Status> statusCol;
    @FXML private TableColumn<DisasterReport, LocalDateTime> dateCol;

    @FXML private ListView<String> alertsList;

    @FXML
    public void initialize() {
        typeCombo.setItems(FXCollections.observableArrayList(DisasterType.values()));
        typeCombo.getSelectionModel().selectFirst();
        reporterNameField.setText(ClientSession.getUser().getFullName());

        Ui.column(idCol, DisasterReport::getReportId);
        Ui.column(typeCol, DisasterReport::getType);
        Ui.column(locationCol, DisasterReport::getLocation);
        Ui.column(severityCol, DisasterReport::getSeverity);
        Ui.column(statusCol, DisasterReport::getStatus);
        Ui.dateColumn(dateCol, DisasterReport::getDateReported);
        refresh();
    }

    @FXML
    private void submitReport() {
        if (typeCombo.getValue() == null || Validation.isBlank(locationField.getText())
                || Validation.isBlank(descriptionArea.getText()) || Validation.isBlank(reporterNameField.getText())) {
            Ui.status(statusLabel, false, "Please complete disaster type, location, description and your name");
            return;
        }
        if (!Validation.isBlank(reporterPhoneField.getText()) && !Validation.isValidPhone(reporterPhoneField.getText())) {
            Ui.status(statusLabel, false, "Phone number must contain 8-15 digits");
            return;
        }
        Request req = new Request(Command.REPORT_DISASTER)
                .with("type", typeCombo.getValue())
                .with("location", locationField.getText())
                .with("description", descriptionArea.getText())
                .with("reporterName", reporterNameField.getText())
                .with("reporterPhone", reporterPhoneField.getText());
        ServerCall.send(req, resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                locationField.clear();
                descriptionArea.clear();
                reporterPhoneField.clear();
                typeCombo.getSelectionModel().selectFirst();
                refresh();
            }
        });
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_MY_REPORTS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<DisasterReport> list = (List<DisasterReport>) resp.getData();
                myReportsTable.setItems(FXCollections.observableArrayList(list));
            }
        });
        ServerCall.send(new Request(Command.LIST_ALERTS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<EvacuationAlert> list = (List<EvacuationAlert>) resp.getData();
                alertsList.setItems(FXCollections.observableArrayList(list.stream()
                        .filter(a -> a.getStatus() == com.cqu.drs.model.AlertStatus.ACTIVE)
                        .map(a -> a.getLevel().getLabel().toUpperCase() + " - " + a.getZone() + ": " + a.getMessage())
                        .toList()));
                if (alertsList.getItems().isEmpty()) {
                    alertsList.getItems().add("No active warnings.");
                }
            }
        });
    }
}
