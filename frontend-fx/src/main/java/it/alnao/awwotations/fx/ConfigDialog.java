package it.alnao.awwotations.fx;

import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Administration dialog: edits the values stored in the config file
 * (~/.config/jawwotations.config). Returns the key/value map to save.
 */
public class ConfigDialog extends Dialog<Map<String, String>> {

    public ConfigDialog(Config config) {
        setTitle("Configuration");

        Label pathLabel = new Label(config.configFilePath());
        pathLabel.getStyleClass().add("note-preview");

        ComboBox<String> styleCombo = new ComboBox<>();
        styleCombo.getItems().addAll("php", "aws");
        styleCombo.setValue(config.apiStyle() == ApiStyle.AWS ? "aws" : "php");

        TextField boardUrlField = new TextField(config.boardApiUrl());
        TextField notesUrlField = new TextField(config.notesApiUrl());
        TextField baseUrlField = new TextField(config.baseApiUrl());
        TextField tokenField = new TextField(config.jwtToken());
        boardUrlField.setPrefColumnCount(36);

        // php uses the two script URLs, aws uses the base URL only
        Runnable syncEnabled = () -> {
            boolean aws = "aws".equals(styleCombo.getValue());
            boardUrlField.setDisable(aws);
            notesUrlField.setDisable(aws);
            baseUrlField.setDisable(!aws);
        };
        styleCombo.valueProperty().addListener((obs, o, n) -> syncEnabled.run());
        syncEnabled.run();

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(6));
        grid.addRow(0, new Label("File"), pathLabel);
        grid.addRow(1, new Label("API style"), styleCombo);
        grid.addRow(2, new Label("Board URL"), boardUrlField);
        grid.addRow(3, new Label("Notes URL"), notesUrlField);
        grid.addRow(4, new Label("Base URL"), baseUrlField);
        grid.addRow(5, new Label("JWT token"), tokenField);
        GridPane.setHgrow(boardUrlField, Priority.ALWAYS);
        GridPane.setHgrow(notesUrlField, Priority.ALWAYS);
        GridPane.setHgrow(baseUrlField, Priority.ALWAYS);
        GridPane.setHgrow(tokenField, Priority.ALWAYS);

        getDialogPane().setContent(grid);
        ButtonType saveType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(saveType, ButtonType.CANCEL);

        setResultConverter(button -> {
            if (button != saveType) {
                return null;
            }
            Map<String, String> values = new LinkedHashMap<>();
            values.put("API_STYLE", styleCombo.getValue());
            values.put("API_BOARD_URL", boardUrlField.getText().trim());
            values.put("API_NOTES_URL", notesUrlField.getText().trim());
            values.put("API_BASE_URL", baseUrlField.getText().trim());
            values.put("JWT_TOKEN", tokenField.getText().trim());
            return values;
        });
    }
}
