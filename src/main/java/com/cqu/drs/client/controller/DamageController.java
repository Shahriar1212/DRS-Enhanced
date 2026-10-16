package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.DamageAssessment;
import com.cqu.drs.model.DamageLevel;
import com.cqu.drs.model.DisasterReport;
import com.cqu.drs.model.InfrastructureCategory;
import com.cqu.drs.model.JobStatus;
import com.cqu.drs.model.RecoverySummary;
import com.cqu.drs.model.RestorationJob;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.util.RestorationPlanner;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Slider;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * NEW FEATURE 2 GUI - Damage Assessment &amp; Restoration Planning.
 * Record infrastructure damage, see the restoration job automatically raised for
 * the responsible department, update job progress and view recovery totals.
 */
public class DamageController {

    private static final String ALL = "All disasters";

    @FXML private ComboBox<DisasterReport> reportCombo;
    @FXML private ComboBox<InfrastructureCategory> categoryCombo;
    @FXML private ComboBox<DamageLevel> levelCombo;
    @FXML private TextField costField;
    @FXML private TextField affectedField;
    @FXML private TextArea notesArea;
    @FXML private Label previewLabel;
    @FXML private Button recordButton;

    @FXML private TableView<DamageAssessment> assessmentsTable;
    @FXML private TableColumn<DamageAssessment, Integer> aIdCol;
    @FXML private TableColumn<DamageAssessment, String> aReportCol;
    @FXML private TableColumn<DamageAssessment, InfrastructureCategory> aCategoryCol;
    @FXML private TableColumn<DamageAssessment, DamageLevel> aLevelCol;
    @FXML private TableColumn<DamageAssessment, String> aCostCol;
    @FXML private TableColumn<DamageAssessment, Integer> aAffectedCol;
    @FXML private TableColumn<DamageAssessment, String> aByCol;
    @FXML private TableColumn<DamageAssessment, LocalDateTime> aAtCol;

    @FXML private ComboBox<Object> filterCombo;
    @FXML private TableView<RestorationJob> jobsTable;
    @FXML private TableColumn<RestorationJob, Integer> jIdCol;
    @FXML private TableColumn<RestorationJob, Integer> jReportCol;
    @FXML private TableColumn<RestorationJob, String> jDescCol;
    @FXML private TableColumn<RestorationJob, String> jDeptCol;
    @FXML private TableColumn<RestorationJob, Integer> jPriorityCol;
    @FXML private TableColumn<RestorationJob, Integer> jDaysCol;
    @FXML private TableColumn<RestorationJob, Integer> jProgressCol;
    @FXML private TableColumn<RestorationJob, JobStatus> jStatusCol;
    @FXML private Slider progressSlider;
    @FXML private Label progressValueLabel;
    @FXML private Button progressButton;

    @FXML private Label summaryCostLabel;
    @FXML private Label summaryAffectedLabel;
    @FXML private Label summaryJobsLabel;
    @FXML private ProgressBar summaryProgressBar;
    @FXML private Label summaryProgressLabel;

    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        categoryCombo.setItems(FXCollections.observableArrayList(InfrastructureCategory.values()));
        levelCombo.setItems(FXCollections.observableArrayList(DamageLevel.values()));
        Ui.reportCombo(reportCombo);
        Ui.reportCombo(filterCombo);
        categoryCombo.valueProperty().addListener((o, a, b) -> updatePreview());
        levelCombo.valueProperty().addListener((o, a, b) -> updatePreview());
        costField.textProperty().addListener((o, a, b) -> updatePreview());
        affectedField.textProperty().addListener((o, a, b) -> updatePreview());

        Ui.column(aIdCol, DamageAssessment::getAssessmentId);
        Ui.column(aReportCol, DamageAssessment::getReportSummary);
        Ui.column(aCategoryCol, DamageAssessment::getCategory);
        Ui.column(aLevelCol, DamageAssessment::getDamageLevel);
        Ui.column(aCostCol, a -> String.format("$%,.0f", a.getEstimatedCost()));
        Ui.column(aAffectedCol, DamageAssessment::getAffectedPeople);
        Ui.column(aByCol, DamageAssessment::getAssessedBy);
        Ui.dateColumn(aAtCol, DamageAssessment::getAssessedAt);

        Ui.column(jIdCol, RestorationJob::getJobId);
        Ui.column(jReportCol, RestorationJob::getReportId);
        Ui.column(jDescCol, RestorationJob::getDescription);
        Ui.column(jDeptCol, RestorationJob::getDepartmentName);
        Ui.column(jPriorityCol, RestorationJob::getPriority);
        Ui.column(jDaysCol, RestorationJob::getEstimatedDays);
        Ui.column(jProgressCol, RestorationJob::getProgress);
        Ui.column(jStatusCol, RestorationJob::getStatus);
        jProgressCol.setCellFactory(c -> new TableCell<>() {
            private final ProgressBar bar = new ProgressBar();
            private final Label text = new Label();
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    bar.setProgress(item / 100.0);
                    bar.setPrefWidth(90);
                    text.setText(" " + item + "%");
                    setGraphic(new javafx.scene.layout.HBox(bar, text));
                }
            }
        });
        jobsTable.getSelectionModel().selectedItemProperty().addListener((o, a, j) -> {
            if (j != null) {
                progressSlider.setValue(j.getProgress());
            }
        });
        progressSlider.valueProperty().addListener((o, a, v) ->
                progressValueLabel.setText(Math.round(v.doubleValue()) + "%"));

        filterCombo.valueProperty().addListener((o, a, b) -> refreshJobs());
        recordButton.setDisable(!ClientSession.can(Command.RECORD_DAMAGE));
        progressButton.setDisable(!ClientSession.can(Command.UPDATE_JOB_PROGRESS));
        refresh();
    }

    private void updatePreview() {
        DamageLevel level = levelCombo.getValue();
        InfrastructureCategory cat = categoryCombo.getValue();
        if (level == null || cat == null) {
            previewLabel.setText("Choose a category and damage level to preview the restoration job.");
            return;
        }
        double cost = parseCost();
        Integer affected = Ui.parseInt(affectedField);
        int people = affected == null ? 0 : affected;
        previewLabel.setText("Job will go to " + cat.getResponsibleDepartment() + " with priority "
                + RestorationPlanner.priority(level, people) + ", estimated "
                + RestorationPlanner.estimatedDays(level, Math.max(cost, 0)) + " days.");
    }

    private double parseCost() {
        try {
            return Double.parseDouble(costField.getText().trim().replace(",", "").replace("$", ""));
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_REPORTS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<DisasterReport> list = (List<DisasterReport>) resp.getData();
                DisasterReport sel = reportCombo.getValue();
                reportCombo.setItems(FXCollections.observableArrayList(list));
                if (sel != null) {
                    list.stream().filter(r -> r.getReportId() == sel.getReportId()).findFirst().ifPresent(reportCombo::setValue);
                }
                Object f = filterCombo.getValue();
                List<Object> filters = new ArrayList<>();
                filters.add(ALL);
                filters.addAll(list);
                filterCombo.setItems(FXCollections.observableArrayList(filters));
                filterCombo.setValue(f == null ? ALL : f instanceof DisasterReport
                        ? list.stream().filter(r -> r.getReportId() == ((DisasterReport) f).getReportId())
                        .findFirst().map(r -> (Object) r).orElse(ALL) : ALL);
            } else {
                Ui.status(statusLabel, resp);
            }
        });
        ServerCall.send(new Request(Command.LIST_DAMAGE), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<DamageAssessment> list = (List<DamageAssessment>) resp.getData();
                assessmentsTable.setItems(FXCollections.observableArrayList(list));
            }
        });
        refreshJobs();
    }

    private int filterReportId() {
        Object f = filterCombo.getValue();
        return f instanceof DisasterReport ? ((DisasterReport) f).getReportId() : 0;
    }

    private void refreshJobs() {
        int reportId = filterReportId();
        Request jobs = new Request(Command.LIST_RESTORATION_JOBS);
        Request summary = new Request(Command.RECOVERY_SUMMARY);
        if (reportId > 0) {
            jobs.with("reportId", reportId);
            summary.with("reportId", reportId);
        }
        ServerCall.send(jobs, resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<RestorationJob> list = (List<RestorationJob>) resp.getData();
                RestorationJob sel = jobsTable.getSelectionModel().getSelectedItem();
                jobsTable.setItems(FXCollections.observableArrayList(list));
                if (sel != null) {
                    list.stream().filter(j -> j.getJobId() == sel.getJobId()).findFirst()
                            .ifPresent(j -> jobsTable.getSelectionModel().select(j));
                }
            }
        });
        ServerCall.send(summary, resp -> {
            if (resp.isSuccess()) {
                RecoverySummary s = (RecoverySummary) resp.getData();
                summaryCostLabel.setText(String.format("$%,.0f", s.getTotalEstimatedCost()));
                summaryAffectedLabel.setText(String.valueOf(s.getTotalAffectedPeople()));
                summaryJobsLabel.setText(s.getCompletedJobs() + " / " + s.getTotalJobs());
                summaryProgressBar.setProgress(s.getAverageProgress() / 100.0);
                summaryProgressLabel.setText(String.format("%.0f%% restored", s.getAverageProgress()));
            }
        });
    }

    @FXML
    private void recordDamage() {
        DisasterReport r = reportCombo.getValue();
        double cost = parseCost();
        Integer affected = Ui.parseInt(affectedField);
        if (r == null || categoryCombo.getValue() == null || levelCombo.getValue() == null) {
            Ui.status(statusLabel, false, "Choose the disaster, infrastructure category and damage level");
            return;
        }
        if (cost < 0) {
            Ui.status(statusLabel, false, "Estimated cost must be a number (e.g. 250000)");
            return;
        }
        if (affected == null || affected < 0) {
            Ui.status(statusLabel, false, "People affected must be a whole number (0 or more)");
            return;
        }
        Request req = new Request(Command.RECORD_DAMAGE).with("reportId", r.getReportId())
                .with("category", categoryCombo.getValue()).with("damageLevel", levelCombo.getValue())
                .with("estimatedCost", cost).with("affectedPeople", affected).with("notes", notesArea.getText());
        ServerCall.send(req, resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                costField.clear();
                affectedField.clear();
                notesArea.clear();
                refresh();
            }
        });
    }

    @FXML
    private void updateProgress() {
        RestorationJob j = jobsTable.getSelectionModel().getSelectedItem();
        if (j == null) {
            Ui.status(statusLabel, false, "Select a restoration job");
            return;
        }
        int progress = (int) Math.round(progressSlider.getValue());
        ServerCall.send(new Request(Command.UPDATE_JOB_PROGRESS).with("jobId", j.getJobId())
                .with("progress", progress), resp -> {
            Ui.status(statusLabel, resp);
            refreshJobs();
        });
    }
}
