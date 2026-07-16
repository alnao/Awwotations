package it.alnao.awwotations.fx;

import it.alnao.awwotations.fx.model.Board;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.HashMap;
import java.util.Map;

/**
 * Create/edit dialog for a board. Returns the JSON payload expected by
 * POST/PUT on the boards API (title, color, favorite).
 */
public class BoardEditorDialog extends Dialog<Map<String, Object>> {

    public BoardEditorDialog(Board existing) {
        setTitle(existing == null ? "New board" : "Edit board");

        TextField titleField = new TextField(existing == null ? "" : existing.title);
        ColorPicker colorPicker = new ColorPicker(ColorUtil.parse(existing == null ? null : existing.color));

        // Display order; digits only, empty on create = auto-assigned by the backend (max+1)
        TextField orderField = new TextField(existing == null ? "" : String.valueOf(existing.order));
        orderField.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d{0,9}") ? change : null));
        orderField.setPrefColumnCount(4);
        orderField.setPromptText("auto");

        CheckBox favoriteCheck = new CheckBox("Favorite");
        favoriteCheck.setSelected(existing != null && existing.favorite);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(6));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Color"), colorPicker);
        grid.addRow(2, new Label("Order"), orderField);
        grid.addRow(3, favoriteCheck);
        GridPane.setHgrow(titleField, Priority.ALWAYS);

        getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        var saveButton = getDialogPane().lookupButton(saveType);
        saveButton.setDisable(titleField.getText().isBlank());
        titleField.textProperty().addListener((o, a, b) -> saveButton.setDisable(b.isBlank()));

        setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", titleField.getText().trim());
            payload.put("color", ColorUtil.toHex(colorPicker.getValue()));
            payload.put("favorite", favoriteCheck.isSelected());
            String orderText = orderField.getText().trim();
            if (!orderText.isEmpty()) {
                payload.put("order", Integer.parseInt(orderText));
            }
            return payload;
        });
    }
}
