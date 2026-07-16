package it.alnao.awwotations.fx;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class App extends Application {
    private static final double MIN_WIDTH = 200;
    private static final double MIN_HEIGHT = 150;

    private Config config;
    private MainView view;
    private Stage stage;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        config = new Config();
        view = new MainView(config);

        Scene scene = new Scene(view.getRoot(), 720, 560);
        String css = getClass().getResource("/compact.css").toExternalForm();
        scene.getStylesheets().add(css);

        stage.setTitle("Awwotations FX");
        stage.setScene(scene);
        restoreWindowState();
        stage.show();

        if (config.wasCreatedOnStartup()) {
            // First run: the config file was just created with defaults,
            // open the administration dialog so the user can fill it in.
            view.openConfigDialog();
        }
        view.loadBoards();
    }

    /** Apply the window geometry saved in the config file, if any and still visible. */
    private void restoreWindowState() {
        double w = config.getDouble("WINDOW_WIDTH", -1);
        double h = config.getDouble("WINDOW_HEIGHT", -1);
        if (w >= MIN_WIDTH && h >= MIN_HEIGHT) {
            stage.setWidth(w);
            stage.setHeight(h);
        }

        double x = config.getDouble("WINDOW_X", Double.NaN);
        double y = config.getDouble("WINDOW_Y", Double.NaN);
        if (!Double.isNaN(x) && !Double.isNaN(y)
                && !Screen.getScreensForRectangle(x, y, Math.max(w, MIN_WIDTH), Math.max(h, MIN_HEIGHT)).isEmpty()) {
            // Only restore the position while it is still on a connected screen.
            stage.setX(x);
            stage.setY(y);
        }

        if (Boolean.parseBoolean(config.get("WINDOW_MAXIMIZED", "false"))) {
            stage.setMaximized(true);
        }
    }

    /** Called on window close and Platform.exit(): persist geometry, stop workers. */
    @Override
    public void stop() {
        saveWindowState();
        if (view != null) {
            view.shutdown();
        }
    }

    private void saveWindowState() {
        if (stage == null || config == null) {
            return;
        }
        Map<String, String> values = new HashMap<>();
        values.put("WINDOW_MAXIMIZED", String.valueOf(stage.isMaximized()));
        if (!stage.isMaximized()) {
            // When maximized, keep the previous normal geometry for un-maximized restarts.
            values.put("WINDOW_X", String.valueOf(Math.round(stage.getX())));
            values.put("WINDOW_Y", String.valueOf(Math.round(stage.getY())));
            values.put("WINDOW_WIDTH", String.valueOf(Math.round(stage.getWidth())));
            values.put("WINDOW_HEIGHT", String.valueOf(Math.round(stage.getHeight())));
        }
        try {
            config.save(values);
        } catch (IOException e) {
            System.err.println("Cannot save window state: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        // All UI text is English, including built-in JavaFX dialog buttons.
        Locale.setDefault(Locale.ENGLISH);
        launch(args);
    }
}
