package com.cqu.drs.client.controller;

import com.cqu.drs.client.ClientSession;
import com.cqu.drs.client.ServerCall;
import com.cqu.drs.client.Ui;
import com.cqu.drs.model.Volunteer;
import com.cqu.drs.protocol.Command;
import com.cqu.drs.protocol.Request;
import com.cqu.drs.util.Validation;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.util.List;

/** Volunteer registration and list (phone numbers masked unless the user may see personal data). */
public class VolunteerController {

    @FXML private GridPane formGrid;
    @FXML private TextField nameField;
    @FXML private TextField phoneField;
    @FXML private TextField skillField;
    @FXML private Button registerButton;
    @FXML private Label statusLabel;
    @FXML private Label privacyLabel;

    @FXML private TableView<Volunteer> volunteersTable;
    @FXML private TableColumn<Volunteer, Integer> idCol;
    @FXML private TableColumn<Volunteer, String> nameCol;
    @FXML private TableColumn<Volunteer, String> phoneCol;
    @FXML private TableColumn<Volunteer, String> skillCol;
    @FXML private TableColumn<Volunteer, String> availableCol;
    @FXML private TableColumn<Volunteer, LocalDateTime> registeredCol;

    @FXML
    public void initialize() {
        Ui.column(idCol, Volunteer::getVolunteerId);
        Ui.column(nameCol, Volunteer::getName);
        Ui.column(phoneCol, Volunteer::getPhone);
        Ui.column(skillCol, Volunteer::getSkill);
        Ui.column(availableCol, v -> v.isAvailable() ? "Available" : "On a task");
        Ui.dateColumn(registeredCol, Volunteer::getRegisteredAt);
        boolean canRegister = ClientSession.can(Command.REGISTER_VOLUNTEER);
        formGrid.setDisable(!canRegister);
        registerButton.setDisable(!canRegister);
        privacyLabel.setText(ClientSession.can(Command.ASSIGN_VOLUNTEER)
                ? "Phone numbers are stored AES-256 encrypted and decrypted for your role."
                : "Phone numbers are masked for your role (privacy). Only coordinators can register volunteers.");
        refresh();
    }

    @FXML
    private void refresh() {
        ServerCall.send(new Request(Command.LIST_VOLUNTEERS), resp -> {
            if (resp.isSuccess()) {
                @SuppressWarnings("unchecked")
                List<Volunteer> list = (List<Volunteer>) resp.getData();
                volunteersTable.setItems(FXCollections.observableArrayList(list));
            } else {
                Ui.status(statusLabel, resp);
            }
        });
    }

    @FXML
    private void registerVolunteer() {
        if (Validation.isBlank(nameField.getText())) {
            Ui.status(statusLabel, false, "Please enter a name");
            return;
        }
        if (!Validation.isBlank(phoneField.getText()) && !Validation.isValidPhone(phoneField.getText())) {
            Ui.status(statusLabel, false, "Phone number must contain 8-15 digits");
            return;
        }
        ServerCall.send(new Request(Command.REGISTER_VOLUNTEER).with("name", nameField.getText())
                .with("phone", phoneField.getText()).with("skill", skillField.getText()), resp -> {
            Ui.status(statusLabel, resp);
            if (resp.isSuccess()) {
                nameField.clear();
                phoneField.clear();
                skillField.clear();
                refresh();
            }
        });
    }
}
