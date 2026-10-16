package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.Department;
import com.cqu.drs.model.Resource;
import com.cqu.drs.model.ResponseTask;
import com.cqu.drs.model.Status;
import com.cqu.drs.model.Volunteer;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Coordination screen: response tasks created by assessment, volunteer
 * assignment, resource allocation from the task's department and task status.
 */
public class CoordinationController {

    @FXML private TableView<ResponseTask> tasksTable;
    @FXML private TableColumn<ResponseTask, Integer> taskIdCol;
    @FXML private TableColumn<ResponseTask, String> reportCol;
    @FXML private TableColumn<ResponseTask, String> descriptionCol;
    @FXML private TableColumn<ResponseTask, String> departmentCol;
    @FXML private TableColumn<ResponseTask, Integer> priorityCol;
    @FXML private TableColumn<ResponseTask, Status> statusCol;
    @FXML private TableColumn<ResponseTask, String> volunteerCol;

    @FXML private ComboBox<Volunteer> volunteerCombo;
    @FXML private ComboBox<Resource> resourceCombo;
    @FXML private TextField quantityField;
    @FXML private ComboBox<Status> statusCombo;
    @FXML private Button assignButton;
    @FXML private Button allocateButton;
    @FXML private Button statusButton;
    @FXML private Label statusLabel;

    /** Department id -&gt; resources, so the resource list follows the selected task in O(1). */
    private Map<Integer, List<Resource>> resourcesByDepartment = Map.of();

    @FXML
    public void initialize() {
        Ui.column(taskIdCol, ResponseTask::getTaskId);
        Ui.column(reportCol, t -> t.getDisasterReport().getSummary());
        Ui.column(descriptionCol, ResponseTask::getDescription);
        Ui.column(departmentCol, ResponseTask::getDepartmentName);
        Ui.column(priorityCol, ResponseTask::getPriority);
        Ui.column(statusCol, ResponseTask::getStatus);
        Ui.column(volunteerCol, ResponseTask::getVolunteerName);

        statusCombo.setItems(FXCollections.observableArrayList(Status.ASSIGNED, Status.IN_PROGRESS, Status.RESOLVED));
        statusCombo.getSelectionModel().select(Status.RESOLVED);
        tasksTable.getSelectionModel().selectedItemProperty().addListener((o, a, t) -> refreshResourceCombo(t));

        assignButton.setDisable(!ClientSession.can(Command.ASSIGN_VOLUNTEER));
        volunteerCombo.setDisable(!ClientSession.can(Command.ASSIGN_VOLUNTEER));
        allocateButton.setDisable(!ClientSession.can(Command.ALLOCATE_RESOURCE));
        resourceCombo.setDisable(!ClientSession.can(Command.ALLOCATE_RESOURCE));
        statusButton.setDisable(!ClientSession.can(Command.UPDATE_TASK_STATUS));
        refresh();
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_TASKS), resp -> {
            if (!resp.isSuccess()) {
                Ui.status(statusLabel, resp);
                return;
            }
            ResponseTask selected = tasksTable.getSelectionModel().getSelectedItem();
            @SuppressWarnings("unchecked")
            List<ResponseTask> list = (List<ResponseTask>) resp.getData();
            tasksTable.setItems(FXCollections.observableArrayList(list));
            if (selected != null) {
                list.stream().filter(t -> t.getTaskId() == selected.getTaskId()).findFirst()
                        .ifPresent(t -> tasksTable.getSelectionModel().select(t));
            }
        });
        ServerCall.send(new Request(Command.LIST_VOLUNTEERS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Volunteer> list = (List<Volunteer>) resp.getData();
                volunteerCombo.setItems(FXCollections.observableArrayList(
                        list.stream().filter(Volunteer::isAvailable).collect(Collectors.toList())));
            }
        });
        ServerCall.send(new Request(Command.LIST_DEPARTMENTS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Department> depts = (List<Department>) resp.getData();
                resourcesByDepartment = depts.stream().collect(Collectors.toMap(
                        Department::getDepartmentId, d -> new ArrayList<>(d.getResources())));
                refreshResourceCombo(tasksTable.getSelectionModel().getSelectedItem());
            }
        });
    }

    private void refreshResourceCombo(ResponseTask task) {
        if (task == null) {
            resourceCombo.setItems(FXCollections.observableArrayList());
            resourceCombo.setPromptText("Select a task first");
            return;
        }
        List<Resource> list = resourcesByDepartment.getOrDefault(task.getDepartment().getDepartmentId(), List.of());
        resourceCombo.setItems(FXCollections.observableArrayList(list));
        resourceCombo.setPromptText(list.isEmpty() ? "No stock held by " + task.getDepartmentName() : "Select a resource");
    }

    @FXML
    private void assignVolunteer() {
        ResponseTask task = tasksTable.getSelectionModel().getSelectedItem();
        Volunteer v = volunteerCombo.getValue();
        if (task == null || v == null) {
            Ui.status(statusLabel, false, "Select a task and an available volunteer");
            return;
        }
        ServerCall.send(new Request(Command.ASSIGN_VOLUNTEER).with("taskId", task.getTaskId())
                .with("volunteerId", v.getVolunteerId()), resp -> {
            Ui.status(statusLabel, resp);
            refresh();
        });
    }

    @FXML
    private void allocateResource() {
        ResponseTask task = tasksTable.getSelectionModel().getSelectedItem();
        Resource r = resourceCombo.getValue();
        Integer qty = Ui.parseInt(quantityField);
        if (task == null || r == null) {
            Ui.status(statusLabel, false, "Select a task and a resource");
            return;
        }
        if (qty == null || qty <= 0) {
            Ui.status(statusLabel, false, "Quantity must be a whole number greater than zero");
            return;
        }
        ServerCall.send(new Request(Command.ALLOCATE_RESOURCE).with("taskId", task.getTaskId())
                .with("resourceId", r.getResourceId()).with("quantity", qty), resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                quantityField.clear();
            }
            refresh();
        });
    }

    @FXML
    private void updateStatus() {
        ResponseTask task = tasksTable.getSelectionModel().getSelectedItem();
        if (task == null || statusCombo.getValue() == null) {
            Ui.status(statusLabel, false, "Select a task and the new status");
            return;
        }
        ServerCall.send(new Request(Command.UPDATE_TASK_STATUS).with("taskId", task.getTaskId())
                .with("status", statusCombo.getValue()), resp -> {
            Ui.status(statusLabel, resp);
            refresh();
        });
    }
}
