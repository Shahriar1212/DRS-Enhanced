package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.DashboardSummary;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.DisasterType;
import com.cqu.drs.model.SeverityLevel;
import com.cqu.drs.model.Status;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.util.PriorityCalculator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dashboard: live summary cards, the prioritised list of all disaster reports
 * and the assessment panel (severity -&gt; priority score -&gt; automatic
 * coordination of the relevant departments).
 */
public class DashboardController {

    @FXML private Label openReportsValue;
    @FXML private Label criticalValue;
    @FXML private Label tasksValue;
    @FXML private Label alertsValue;
    @FXML private Label shelteredValue;
    @FXML private Label damageValue;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private TableView<DisasterReport> reportsTable;
    @FXML private TableColumn<DisasterReport, Integer> idCol;
    @FXML private TableColumn<DisasterReport, DisasterType> typeCol;
    @FXML private TableColumn<DisasterReport, String> locationCol;
    @FXML private TableColumn<DisasterReport, SeverityLevel> severityCol;
    @FXML private TableColumn<DisasterReport, Integer> priorityCol;
    @FXML private TableColumn<DisasterReport, Status> statusCol;
    @FXML private TableColumn<DisasterReport, String> reporterCol;
    @FXML private TableColumn<DisasterReport, String> phoneCol;
    @FXML private TableColumn<DisasterReport, LocalDateTime> dateCol;
    @FXML private TableColumn<DisasterReport, String> signedCol;

    @FXML private TextArea detailsArea;
    @FXML private ComboBox<SeverityLevel> severityCombo;
    @FXML private Label previewLabel;
    @FXML private Button assessButton;
    @FXML private Button resolveButton;
    @FXML private Label statusLabel;

    private final ObservableList<DisasterReport> reports = FXCollections.observableArrayList();
    private FilteredList<DisasterReport> filtered;

    @FXML
    public void initialize() {
        Ui.column(idCol, DisasterReport::getReportId);
        Ui.column(typeCol, DisasterReport::getType);
        Ui.column(locationCol, DisasterReport::getLocation);
        Ui.column(severityCol, DisasterReport::getSeverity);
        Ui.column(priorityCol, DisasterReport::getPriorityScore);
        Ui.column(statusCol, DisasterReport::getStatus);
        Ui.column(reporterCol, DisasterReport::getReporterName);
        Ui.column(phoneCol, DisasterReport::getReporterPhone);
        Ui.dateColumn(dateCol, DisasterReport::getDateReported);
        Ui.column(signedCol, r -> r.getSignatureValid() == null ? "-" : r.getSignatureValid() ? "✔ valid" : "✖ INVALID");
        severityCol.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(SeverityLevel item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("sev-LOW", "sev-MEDIUM", "sev-HIGH", "sev-CRITICAL");
                setText(empty ? null : item == null ? "Not assessed" : item.name());
                if (!empty && item != null) {
                    getStyleClass().add("sev-" + item.name());
                }
            }
        });
        reportsTable.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(DisasterReport item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("row-resolved");
                if (!empty && item != null && item.getStatus() == Status.RESOLVED) {
                    getStyleClass().add("row-resolved");
                }
            }
        });

        filtered = new FilteredList<>(reports, r -> true);
        reportsTable.setItems(filtered);
        statusFilter.setItems(FXCollections.observableArrayList("All statuses", "Open only", "Not assessed",
                "REPORTED", "ASSESSED", "IN_PROGRESS", "RESOLVED"));
        statusFilter.getSelectionModel().selectFirst();
        statusFilter.valueProperty().addListener((o, a, b) -> applyFilter());
        searchField.textProperty().addListener((o, a, b) -> applyFilter());

        severityCombo.setItems(FXCollections.observableArrayList(SeverityLevel.values()));
        severityCombo.getSelectionModel().select(SeverityLevel.HIGH);
        severityCombo.valueProperty().addListener((o, a, b) -> updatePreview());
        reportsTable.getSelectionModel().selectedItemProperty().addListener((o, a, r) -> {
            showDetails(r);
            updatePreview();
        });

        boolean canAssess = ClientSession.can(Command.ASSESS_REPORT);
        assessButton.setDisable(!canAssess);
        resolveButton.setDisable(!ClientSession.can(Command.RESOLVE_REPORT));
        severityCombo.setDisable(!canAssess);
        if (!canAssess) {
            previewLabel.setText("Read-only: only coordinators can assess reports.");
        }
        refresh();
    }

    private void applyFilter() {
        String text = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();
        String st = statusFilter.getValue();
        filtered.setPredicate(r -> {
            boolean textOk = text.isEmpty() || r.getLocation().toLowerCase().contains(text)
                    || r.getType().name().toLowerCase().contains(text)
                    || r.getDescription().toLowerCase().contains(text)
                    || r.getReporterName().toLowerCase().contains(text);
            boolean statusOk = st == null || st.startsWith("All")
                    || (st.equals("Open only") && r.getStatus() != Status.RESOLVED)
                    || (st.equals("Not assessed") && r.getSeverity() == null)
                    || r.getStatus().name().equals(st);
            return textOk && statusOk;
        });
    }

    private void showDetails(DisasterReport r) {
        if (r == null) {
            detailsArea.clear();
            return;
        }
        detailsArea.setText("Report #" + r.getReportId() + " - " + r.getType() + " at " + r.getLocation()
                + "\nReported " + Ui.format(r.getDateReported()) + " by " + r.getReporterName()
                + " (" + (r.getReporterPhone() == null ? "no phone" : r.getReporterPhone()) + "), account '"
                + r.getReportedBy() + "'"
                + "\nDigital signature: " + (r.getSignatureValid() == null ? "none"
                : r.getSignatureValid() ? "VALID (SHA256withRSA, non-repudiable)" : "INVALID - content changed after signing")
                + "\nLast updated: " + Ui.format(r.getLastUpdated())
                + "\n\n" + r.getDescription());
    }

    private void updatePreview() {
        DisasterReport r = reportsTable.getSelectionModel().getSelectedItem();
        SeverityLevel s = severityCombo.getValue();
        if (r != null && s != null && ClientSession.can(Command.ASSESS_REPORT)) {
            previewLabel.setText("Priority score will be " + PriorityCalculator.calculate(r.getType(), s)
                    + " (severity " + s.getWeight() + " x 10 + " + r.getType() + " weight "
                    + PriorityCalculator.typeWeight(r.getType()) + " x 5)");
        }
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_REPORTS), resp -> {
            if (!resp.isSuccess()) {
                Ui.status(statusLabel, resp);
                return;
            }
            Integer selected = reportsTable.getSelectionModel().getSelectedItem() == null ? null
                    : reportsTable.getSelectionModel().getSelectedItem().getReportId();
            @SuppressWarnings("unchecked")
            List<DisasterReport> list = (List<DisasterReport>) resp.getData();
            reports.setAll(list);
            if (selected != null) {
                reports.stream().filter(r -> r.getReportId() == selected).findFirst()
                        .ifPresent(r -> reportsTable.getSelectionModel().select(r));
            }
        });
        ServerCall.send(new Request(Command.DASHBOARD_SUMMARY), resp -> {
            if (resp.isSuccess()) {
                DashboardSummary s = (DashboardSummary) resp.getData();
                openReportsValue.setText(String.valueOf(s.getOpenReports()));
                criticalValue.setText(String.valueOf(s.getCriticalReports()));
                tasksValue.setText(String.valueOf(s.getActiveTasks()));
                alertsValue.setText(String.valueOf(s.getActiveAlerts()));
                shelteredValue.setText(s.getPeopleSheltered() + " / " + s.getShelterCapacity());
                damageValue.setText(String.format("$%,.0f", s.getTotalDamageCost()));
            }
        });
    }

    @FXML
    private void assess() {
        DisasterReport r = reportsTable.getSelectionModel().getSelectedItem();
        if (r == null) {
            Ui.status(statusLabel, false, "Select a report in the table first");
            return;
        }
        Request req = new Request(Command.ASSESS_REPORT).with("reportId", r.getReportId())
                .with("severity", severityCombo.getValue());
        ServerCall.send(req, resp -> {
            Ui.status(statusLabel, resp);
            refresh();
        });
    }

    @FXML
    private void resolve() {
        DisasterReport r = reportsTable.getSelectionModel().getSelectedItem();
        if (r == null) {
            Ui.status(statusLabel, false, "Select a report in the table first");
            return;
        }
        if (!Ui.confirm("Resolve report #" + r.getReportId(), "Mark this disaster as resolved?")) {
            return;
        }
        ServerCall.send(new Request(Command.RESOLVE_REPORT).with("reportId", r.getReportId()), resp -> {
            Ui.status(statusLabel, resp);
            refresh();
        });
    }
}
