package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.Department;
import com.cqu.drs.model.DepartmentType;
import com.cqu.drs.model.Resource;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.util.List;

/** Department directory and resource inventory with low-stock highlighting and restocking. */
public class ResourceController {

    @FXML private TableView<Department> departmentsTable;
    @FXML private TableColumn<Department, Integer> deptIdCol;
    @FXML private TableColumn<Department, String> deptNameCol;
    @FXML private TableColumn<Department, DepartmentType> deptTypeCol;
    @FXML private TableColumn<Department, String> deptContactCol;
    @FXML private TableColumn<Department, Integer> deptResCol;

    @FXML private TableView<Resource> resourcesTable;
    @FXML private TableColumn<Resource, Integer> resIdCol;
    @FXML private TableColumn<Resource, String> resDeptCol;
    @FXML private TableColumn<Resource, String> resNameCol;
    @FXML private TableColumn<Resource, Integer> resQtyCol;
    @FXML private TableColumn<Resource, Integer> resReorderCol;
    @FXML private TableColumn<Resource, String> resStockCol;
    @FXML private TableColumn<Resource, LocalDateTime> resUpdatedCol;

    @FXML private TextField restockField;
    @FXML private Button restockButton;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        Ui.column(deptIdCol, Department::getDepartmentId);
        Ui.column(deptNameCol, Department::getName);
        Ui.column(deptTypeCol, Department::getType);
        Ui.column(deptContactCol, Department::getContactInfo);
        Ui.column(deptResCol, d -> d.getResources().size());

        Ui.column(resIdCol, Resource::getResourceId);
        Ui.column(resDeptCol, Resource::getDepartmentName);
        Ui.column(resNameCol, Resource::getName);
        Ui.column(resQtyCol, Resource::getQuantityAvailable);
        Ui.column(resReorderCol, Resource::getReorderLevel);
        Ui.column(resStockCol, r -> r.isLowStock() ? "LOW - reorder" : "OK");
        Ui.dateColumn(resUpdatedCol, Resource::getLastUpdated);
        resourcesTable.setRowFactory(t -> new TableRow<>() {
            @Override
            protected void updateItem(Resource item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("row-low");
                if (!empty && item != null && item.isLowStock()) {
                    getStyleClass().add("row-low");
                }
            }
        });
        boolean canRestock = ClientSession.can(Command.RESTOCK_RESOURCE);
        restockButton.setDisable(!canRestock);
        restockField.setDisable(!canRestock);
        refresh();
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_DEPARTMENTS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Department> list = (List<Department>) resp.getData();
                departmentsTable.setItems(FXCollections.observableArrayList(list));
            } else {
                Ui.status(statusLabel, resp);
            }
        });
        ServerCall.send(new Request(Command.LIST_RESOURCES), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Resource> list = (List<Resource>) resp.getData();
                resourcesTable.setItems(FXCollections.observableArrayList(list));
            }
        });
    }

    @FXML
    private void restock() {
        Resource r = resourcesTable.getSelectionModel().getSelectedItem();
        Integer qty = Ui.parseInt(restockField);
        if (r == null) {
            Ui.status(statusLabel, false, "Select a resource to restock");
            return;
        }
        if (qty == null || qty <= 0) {
            Ui.status(statusLabel, false, "Quantity must be a whole number greater than zero");
            return;
        }
        ServerCall.send(new Request(Command.RESTOCK_RESOURCE).with("resourceId", r.getResourceId())
                .with("quantity", qty), resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                restockField.clear();
            }
            refresh();
        });
    }
}
