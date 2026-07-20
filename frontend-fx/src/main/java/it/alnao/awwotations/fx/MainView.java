package it.alnao.awwotations.fx;

import it.alnao.awwotations.fx.model.Board;
import it.alnao.awwotations.fx.model.Note;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Main window: menu bar (File/Board), board selector with a favorites filter
 * on top, compact notes list (ul-like) below.
 */
public class MainView {
    private final Config config;
    private ApiClient api;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "api-worker");
        t.setDaemon(true);
        return t;
    });

    private final BorderPane root = new BorderPane();
    private final ComboBox<Board> boardCombo = new ComboBox<>();
    private final ToggleButton favoritesToggle = new ToggleButton("☆");
    private final ToggleButton notesFavoritesToggle = new ToggleButton("☆");
    private final ObservableList<Note> notes = FXCollections.observableArrayList();
    private final ListView<Note> notesList = new ListView<>(notes);
    private final Label statusLabel = new Label();

    private List<Board> allBoards = List.of();
    private List<Note> allNotes = List.of();

    public MainView(Config config) {
        this.config = config;
        this.api = new ApiClient(config);
        buildTop();
        buildNotesList();
        buildBottomBar();
    }

    public BorderPane getRoot() {
        return root;
    }

    public void shutdown() {
        executor.shutdownNow();
    }

    // ---------- layout ----------

    private void buildTop() {
        boardCombo.setPromptText("Select board");
        boardCombo.setMaxWidth(Double.MAX_VALUE);
        boardCombo.valueProperty().addListener((obs, old, selected) -> {
            if (selected != null) {
                loadNotes(selected.boardId);
            } else {
                allNotes = List.of();
                notes.clear();
            }
        });

        favoritesToggle.setTooltip(new Tooltip("Show only favorite boards"));
        favoritesToggle.selectedProperty().addListener((obs, o, selected) -> {
            favoritesToggle.setText(selected ? "★" : "☆");
            applyBoardFilter();
        });

        notesFavoritesToggle.setTooltip(new Tooltip("Show only favorite notes"));
        notesFavoritesToggle.selectedProperty().addListener((obs, o, selected) -> {
            notesFavoritesToggle.setText(selected ? "★" : "☆");
            applyNotesFilter();
        });

        HBox selectorBar = new HBox(2, favoritesToggle, boardCombo, notesFavoritesToggle);
        selectorBar.setPadding(new Insets(2));
        selectorBar.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(boardCombo, Priority.ALWAYS);

        root.setTop(new VBox(buildMenuBar(), selectorBar));
    }

    private MenuBar buildMenuBar() {
        MenuItem configItem = new MenuItem("Config file...");
        configItem.setOnAction(e -> openConfigDialog());

        MenuItem reloadItem = new MenuItem("Reload");
        reloadItem.setAccelerator(KeyCombination.keyCombination("F5"));
        reloadItem.setOnAction(e -> loadBoards());

        MenuItem closeItem = new MenuItem("Close");
        closeItem.setAccelerator(KeyCombination.keyCombination("Shortcut+Q"));
        closeItem.setOnAction(e -> Platform.exit());

        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(configItem, reloadItem, new SeparatorMenuItem(), closeItem);

        MenuItem addBoard = new MenuItem("Add...");
        addBoard.setOnAction(e -> onCreateBoard());

        MenuItem editBoard = new MenuItem("Edit active...");
        editBoard.setOnAction(e -> onEditBoard());

        MenuItem deleteBoard = new MenuItem("Delete active");
        deleteBoard.setOnAction(e -> onDeleteBoard());

        Menu boardMenu = new Menu("Board");
        boardMenu.getItems().addAll(addBoard, editBoard, deleteBoard);

        return new MenuBar(fileMenu, boardMenu);
    }

    private void buildNotesList() {
        notesList.setCellFactory(list -> new NoteCell());
        notesList.setPlaceholder(new Label("No notes"));
        root.setCenter(notesList);
    }

    private void buildBottomBar() {
        Button newNote = new Button("+ note");
        newNote.setOnAction(e -> onCreateNote());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox bottom = new HBox(4, newNote, spacer, statusLabel);
        bottom.setPadding(new Insets(2));
        bottom.setAlignment(Pos.CENTER_LEFT);
        root.setBottom(bottom);
    }

    // ---------- configuration ----------

    public void openConfigDialog() {
        ConfigDialog dialog = new ConfigDialog(config);
        dialog.showAndWait().ifPresent(values -> {
            try {
                config.save(values);
                api = new ApiClient(config);
                loadBoards();
            } catch (IOException e) {
                setStatus("Cannot save config: " + e.getMessage());
            }
        });
    }

    // ---------- data loading ----------

    public void loadBoards() {
        runAsync(api::listBoards, boards -> {
            // Backends sort by order; defensive client-side sort for older deployments.
            allBoards = boards.stream()
                    .sorted(java.util.Comparator.comparingInt(b -> b.order))
                    .toList();
            applyBoardFilter();
        });
    }

    /** Fill the combo with all boards or only favorites, keeping the selection when possible. */
    private void applyBoardFilter() {
        Board previous = boardCombo.getValue();
        List<Board> visible = favoritesToggle.isSelected()
                ? allBoards.stream().filter(b -> b.favorite).toList()
                : allBoards;
        boardCombo.getItems().setAll(visible);

        Board toSelect = null;
        if (previous != null) {
            for (Board b : visible) {
                if (b.boardId.equals(previous.boardId)) {
                    toSelect = b;
                    break;
                }
            }
        }
        if (toSelect == null && !visible.isEmpty()) {
            toSelect = visible.get(0);
        }
        boardCombo.setValue(toSelect);
        if (toSelect == null) {
            allNotes = List.of();
            notes.clear();
        }
    }

    private void loadNotes(String boardId) {
        runAsync(() -> api.listNotes(boardId), loaded -> {
            allNotes = loaded;
            applyNotesFilter();
        });
    }

    /** Show all notes of the active board or only the favorite ones. */
    private void applyNotesFilter() {
        Board activeBoard = boardCombo.getValue();
        String orderNotes = activeBoard != null ? activeBoard.orderNotes : "POS_X";
        if (orderNotes == null) {
            orderNotes = "POS_X";
        }

        List<Note> filtered = notesFavoritesToggle.isSelected()
                ? allNotes.stream().filter(n -> n.favorite).toList()
                : allNotes;

        final String sortKey = orderNotes;
        List<Note> sorted = filtered.stream().sorted((a, b) -> {
            switch (sortKey) {
                case "CREATE_DESC":
                    return getSafeString(b.createdAt).compareTo(getSafeString(a.createdAt));
                case "CREATE_ASC":
                    return getSafeString(a.createdAt).compareTo(getSafeString(b.createdAt));
                case "USER_DATE_DESC":
                    return getSafeString(b.userDateTime).compareTo(getSafeString(a.userDateTime));
                case "USER_DATE_ASC":
                    return getSafeString(a.userDateTime).compareTo(getSafeString(b.userDateTime));
                case "TITLE":
                    return getSafeString(a.title).compareTo(getSafeString(b.title));
                case "POS_Y":
                    return Double.compare(a.posY, b.posY);
                case "POS_X":
                default:
                    return Double.compare(a.posX, b.posX);
            }
        }).toList();

        notes.setAll(sorted);
    }

    private String getSafeString(String s) {
        return s == null ? "" : s;
    }

    private void reloadCurrentNotes() {
        Board board = boardCombo.getValue();
        if (board != null) {
            loadNotes(board.boardId);
        }
    }

    // ---------- board actions ----------

    private void onCreateBoard() {
        BoardEditorDialog dialog = new BoardEditorDialog(null);
        dialog.showAndWait().ifPresent(payload ->
                runAsync(() -> api.createBoard(payload), created -> loadBoards()));
    }

    private void onEditBoard() {
        Board board = boardCombo.getValue();
        if (board == null) {
            setStatus("Select a board first");
            return;
        }
        BoardEditorDialog dialog = new BoardEditorDialog(board);
        dialog.showAndWait().ifPresent(payload ->
                runAsync(() -> api.updateBoard(board.boardId, payload), updated -> loadBoards()));
    }

    private void onDeleteBoard() {
        Board board = boardCombo.getValue();
        if (board == null) {
            setStatus("Select a board first");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete board \"" + board.title + "\" and all its notes?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                runAsync(() -> {
                    api.deleteBoard(board.boardId);
                    return null;
                }, ignored -> loadBoards());
            }
        });
    }

    // ---------- note actions ----------

    private void onCreateNote() {
        Board board = boardCombo.getValue();
        if (board == null) {
            setStatus("Select a board first");
            return;
        }
        NoteEditorDialog dialog = new NoteEditorDialog(null);
        dialog.showAndWait().ifPresent(payload ->
                runAsync(() -> api.createNote(board.boardId, payload), created -> reloadCurrentNotes()));
    }

    private void onEditNote(Note note) {
        if (note.isLocked()) {
            setStatus("Note locked (" + note.status + "): move to MODIFIED to edit");
            return;
        }
        NoteEditorDialog dialog = new NoteEditorDialog(note);
        dialog.showAndWait().ifPresent(payload ->
                runAsync(() -> api.updateNote(note.boardId, note.noteId, payload), updated -> reloadCurrentNotes()));
    }

    private void onDeleteNote(Note note) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete note \"" + note.title + "\"?", ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                runAsync(() -> {
                    api.deleteNote(note.boardId, note.noteId);
                    return null;
                }, ignored -> reloadCurrentNotes());
            }
        });
    }

    private void onChangeStatus(Note note, String newStatus) {
        runAsync(() -> api.changeStatus(note.boardId, note.noteId, newStatus), updated -> reloadCurrentNotes());
    }

    private void onTogglePin(Note note) {
        runAsync(() -> api.togglePin(note.boardId, note.noteId), updated -> reloadCurrentNotes());
    }

    private void onToggleFavorite(Note note) {
        runAsync(() -> api.toggleFavorite(note.boardId, note.noteId), updated -> reloadCurrentNotes());
    }

    // ---------- helpers ----------

    private <T> void runAsync(Supplier<T> call, Consumer<T> onSuccess) {
        setStatus("");
        executor.submit(() -> {
            try {
                T result = call.get();
                Platform.runLater(() -> onSuccess.accept(result));
            } catch (Exception e) {
                Platform.runLater(() -> setStatus(e.getMessage()));
            }
        });
    }

    private void setStatus(String message) {
        statusLabel.setText(message == null ? "" : message);
    }

    /**
     * Compact two-line note cell: header row (color dot, markers, title, type,
     * date, status, actions), full text in a read-only textbox below.
     */
    private class NoteCell extends ListCell<Note> {
        private final Circle colorDot = new Circle(4);
        private final Label markers = new Label();
        private final Label title = new Label();
        private final Label typeLabel = new Label();
        private final Label dateLabel = new Label();
        private final Label status = new Label();
        private final MenuButton actions = new MenuButton("⋮");
        private final TextArea textBox = new TextArea();
        private final VBox box;

        NoteCell() {
            title.getStyleClass().add("note-title");
            typeLabel.getStyleClass().add("note-status");
            dateLabel.getStyleClass().add("note-status");
            status.getStyleClass().add("note-status");

            textBox.setEditable(false);
            textBox.setWrapText(true);
            textBox.setFocusTraversable(false);
            textBox.getStyleClass().add("note-text");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(4, colorDot, markers, title, typeLabel, dateLabel, spacer, status, actions);
            header.setAlignment(Pos.CENTER_LEFT);
            box = new VBox(2, header, textBox);

            setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && getItem() != null) {
                    onEditNote(getItem());
                }
            });
        }

        @Override
        protected void updateItem(Note note, boolean empty) {
            super.updateItem(note, empty);
            if (empty || note == null) {
                setGraphic(null);
                setContextMenu(null);
                return;
            }

            try {
                colorDot.setFill(Color.web(note.color == null ? "#cccccc" : note.color));
            } catch (IllegalArgumentException e) {
                colorDot.setFill(Color.GRAY);
            }

            StringBuilder mark = new StringBuilder();
            if (note.pinned) {
                mark.append("📌");
            }
            if (note.favorite) {
                mark.append("★");
            }
            markers.setText(mark.toString());

            title.setText(note.title);
            typeLabel.setText(note.textType == null ? "" : note.textType);
            dateLabel.setText(formatUserDate(note.userDateTime));
            status.setText("[" + note.status + "]");

            String text = note.text == null ? "" : note.text;
            textBox.setText(text);
            // Size the textbox on the line count, capped to keep rows compact.
            int lines = (int) Math.max(1, text.lines().count());
            textBox.setPrefRowCount(Math.min(lines, 8));

            actions.getItems().setAll(buildActionItems(note));
            setContextMenu(buildRowContextMenu(note));
            setGraphic(box);
        }

        /** Format the note userDateTime (yyyy-MM-dd...) as dd/MM/yy. */
        private String formatUserDate(String userDateTime) {
            if (userDateTime == null || userDateTime.length() < 10) {
                return "";
            }
            try {
                return LocalDate.parse(userDateTime.substring(0, 10))
                        .format(DateTimeFormatter.ofPattern("dd/MM/yy"));
            } catch (DateTimeParseException e) {
                return userDateTime;
            }
        }

        private List<MenuItem> buildActionItems(Note note) {
            MenuItem edit = new MenuItem("Edit");
            edit.setDisable(note.isLocked());
            edit.setOnAction(e -> onEditNote(note));

            MenuItem pin = new MenuItem(note.pinned ? "Unpin" : "Pin");
            pin.setOnAction(e -> onTogglePin(note));

            MenuItem favorite = new MenuItem(note.favorite ? "Unfavorite" : "Favorite");
            favorite.setOnAction(e -> onToggleFavorite(note));

            MenuItem delete = new MenuItem("Delete");
            delete.setOnAction(e -> onDeleteNote(note));

            java.util.ArrayList<MenuItem> items = new java.util.ArrayList<>(List.of(edit, pin, favorite, delete));
            for (String target : Note.transitionsFrom(note.status, api.style())) {
                MenuItem transition = new MenuItem("→ " + target);
                transition.setOnAction(e -> onChangeStatus(note, target));
                items.add(transition);
            }
            return items;
        }

        private ContextMenu buildRowContextMenu(Note note) {
            ContextMenu menu = new ContextMenu();
            menu.getItems().setAll(buildActionItems(note));
            return menu;
        }
    }
}
