package com.cqu.drs.client;

import com.cqu.drs.protocol.Response;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;

/** Small GUI helpers shared by the controllers (formatting, status messages, table cells). */
public final class Ui {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private Ui() {
    }

    public static String format(LocalDateTime t) {
        return t == null ? "" : t.format(DATE_TIME);
    }

    /** Shows a green success or red error message in a status label. */
    public static void status(Label label, Response r) {
        status(label, r.isSuccess(), r.getMessage());
    }

    public static void status(Label label, boolean ok, String message) {
        label.getStyleClass().removeAll("status-ok", "status-error");
        label.getStyleClass().add(ok ? "status-ok" : "status-error");
        label.setText((ok ? "✔ " : "✖ ") + message);
    }

    /** Parses a whole number from a text field or returns null. */
    public static Integer parseInt(TextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Makes a column display a value derived from the row object. */
    public static <S, T> void column(TableColumn<S, T> col, Function<S, T> getter) {
        col.setCellValueFactory(c -> new javafx.beans.property.SimpleObjectProperty<>(getter.apply(c.getValue())));
    }

    /** Date column formatted dd/MM/yyyy HH:mm. */
    public static <S> void dateColumn(TableColumn<S, LocalDateTime> col, Function<S, LocalDateTime> getter) {
        column(col, getter);
        col.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format(item));
            }
        });
    }

    /** Shows disaster reports in combo boxes as "#3 HURRICANE - Gladstone (CRITICAL)". */
    public static void reportCombo(javafx.scene.control.ComboBox<?> combo) {
        javafx.util.StringConverter<Object> conv = new javafx.util.StringConverter<>() {
            @Override
            public String toString(Object o) {
                if (o instanceof com.cqu.drs.model.DisasterReport) {
                    com.cqu.drs.model.DisasterReport r = (com.cqu.drs.model.DisasterReport) o;
                    return r.getSummary() + " (" + (r.getSeverity() == null ? "not assessed" : r.getSeverity()) + ")";
                }
                return o == null ? "" : o.toString();
            }

            @Override
            public Object fromString(String s) {
                return null;
            }
        };
        @SuppressWarnings("unchecked")
        javafx.scene.control.ComboBox<Object> c = (javafx.scene.control.ComboBox<Object>) combo;
        c.setConverter(conv);
        c.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(Object item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : conv.toString(item));
            }
        });
    }

    /** Asks the user to confirm an irreversible action. */
    public static boolean confirm(String title, String message) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(title);
        return a.showAndWait().filter(b -> b == ButtonType.YES).isPresent();
    }
}
