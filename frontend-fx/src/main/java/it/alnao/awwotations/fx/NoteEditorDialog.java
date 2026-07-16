package it.alnao.awwotations.fx;

import it.alnao.awwotations.fx.model.Note;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Create/edit dialog for a note. Returns the JSON payload expected by
 * POST/PUT on AwwotazioniNotes.php (boardId is added by the caller).
 */
public class NoteEditorDialog extends Dialog<Map<String, Object>> {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Defaults for layout fields, unused by the list UI but required by the API.
    private static final double DEFAULT_POS = 40;
    private static final double DEFAULT_WIDTH = 220;
    private static final double DEFAULT_HEIGHT = 180;

    public NoteEditorDialog(Note existing) {
        setTitle(existing == null ? "New note" : "Edit note");

        TextField titleField = new TextField(existing == null ? "" : existing.title);
        TextArea textArea = new TextArea(existing == null ? "" : existing.text);
        textArea.setPrefRowCount(6);

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("TEXT", "MD", "HTML", "CODE_JAVA", "CODE_PYTHON", "CODE_SQL", "CODE_BASH", "CODE_JS");
        typeCombo.setEditable(true);
        typeCombo.setValue(existing == null || existing.textType == null ? "TEXT" : existing.textType);

        ColorPicker colorPicker = new ColorPicker(ColorUtil.parse(existing == null ? null : existing.color));

        TextField dateField = new TextField(existing == null || existing.userDateTime == null
                ? LocalDateTime.now().format(DATE_FORMAT)
                : existing.userDateTime);

        CheckBox pinnedCheck = new CheckBox("Pinned");
        pinnedCheck.setSelected(existing != null && existing.pinned);
        CheckBox favoriteCheck = new CheckBox("Favorite");
        favoriteCheck.setSelected(existing != null && existing.favorite);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(6));
        grid.addRow(0, new Label("Title"), titleField);
        grid.addRow(1, new Label("Text"), textArea);
        grid.addRow(2, new Label("Type"), typeCombo);
        grid.addRow(3, new Label("Color"), colorPicker);
        grid.addRow(4, new Label("Date"), dateField);
        grid.addRow(5, pinnedCheck, favoriteCheck);
        GridPane.setHgrow(titleField, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setHgrow(textArea, javafx.scene.layout.Priority.ALWAYS);

        getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        // Save disabled until title and text are non-empty (API rejects them empty)
        var saveButton = getDialogPane().lookupButton(saveType);
        Runnable validate = () -> saveButton.setDisable(
                titleField.getText().isBlank() || textArea.getText().isBlank());
        titleField.textProperty().addListener((o, a, b) -> validate.run());
        textArea.textProperty().addListener((o, a, b) -> validate.run());
        validate.run();

        setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", titleField.getText().trim());
            payload.put("text", textArea.getText());
            payload.put("textType", typeCombo.getValue() == null ? "TEXT" : typeCombo.getValue().trim());
            payload.put("userDateTime", dateField.getText().trim());
            payload.put("color", ColorUtil.toHex(colorPicker.getValue()));
            payload.put("pinned", pinnedCheck.isSelected());
            payload.put("favorite", favoriteCheck.isSelected());
            payload.put("links", existing == null ? new ArrayList<>() : existing.links);
            payload.put("iconMain", existing == null ? null : existing.iconMain);
            payload.put("iconSecondary", existing == null ? null : existing.iconSecondary);
            // layout fields required by the API; preserved on edit, defaults on create
            payload.put("posX", existing == null ? DEFAULT_POS : existing.posX);
            payload.put("posY", existing == null ? DEFAULT_POS : existing.posY);
            payload.put("width", existing == null ? DEFAULT_WIDTH : existing.width);
            payload.put("height", existing == null ? DEFAULT_HEIGHT : existing.height);
            return payload;
        });
    }

}
