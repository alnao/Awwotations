package it.alnao.awwotations.fx;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Application configuration.
 *
 * The .env file (working directory or frontend-fx/) only holds CONFIG_FILE,
 * the path of the real configuration file (default ~/.config/jawwotations.config,
 * KEY=VALUE lines, # comments). If the config file does not exist it is created
 * with default values and {@link #wasCreatedOnStartup()} returns true so the
 * application can open the administration dialog.
 *
 * System environment variables override file values.
 */
public final class Config {
    public static final String DEFAULT_CONFIG_PATH = "~/.config/jawwotations.config";

    /** Keys stored in the config file, in write order. */
    private static final List<String> KEYS = List.of(
            "API_STYLE", "API_BOARD_URL", "API_NOTES_URL", "API_BASE_URL", "JWT_TOKEN",
            "WINDOW_X", "WINDOW_Y", "WINDOW_WIDTH", "WINDOW_HEIGHT", "WINDOW_MAXIMIZED");

    private static final Map<String, String> DEFAULTS = Map.ofEntries(
            Map.entry("API_STYLE", "php"),
            Map.entry("API_BOARD_URL", "http://localhost/Php/PhpMyWeb12papi/AwwotazioniBoard.php"),
            Map.entry("API_NOTES_URL", "http://localhost/Php/PhpMyWeb12papi/AwwotazioniNotes.php"),
            Map.entry("API_BASE_URL", ""),
            Map.entry("JWT_TOKEN", ""),
            Map.entry("WINDOW_X", ""),
            Map.entry("WINDOW_Y", ""),
            Map.entry("WINDOW_WIDTH", ""),
            Map.entry("WINDOW_HEIGHT", ""),
            Map.entry("WINDOW_MAXIMIZED", ""));

    private final Path configFile;
    private final Map<String, String> values = new HashMap<>();
    private boolean createdOnStartup = false;

    public Config() {
        Map<String, String> env = parseFile(findEnvFile());
        String rawPath = System.getenv("CONFIG_FILE");
        if (rawPath == null || rawPath.isBlank()) {
            rawPath = env.getOrDefault("CONFIG_FILE", DEFAULT_CONFIG_PATH);
        }
        this.configFile = expandHome(rawPath);

        if (!Files.isRegularFile(configFile)) {
            try {
                writeConfigFile(DEFAULTS);
                createdOnStartup = true;
            } catch (IOException e) {
                System.err.println("Cannot create " + configFile + ": " + e.getMessage());
            }
        }
        values.putAll(parseFile(configFile));
    }

    /** True when the config file did not exist and was created with defaults. */
    public boolean wasCreatedOnStartup() {
        return createdOnStartup;
    }

    public String configFilePath() {
        return configFile.toString();
    }

    /** Persist the given values to the config file and reload them in memory. */
    public void save(Map<String, String> newValues) throws IOException {
        Map<String, String> merged = new LinkedHashMap<>();
        for (String key : KEYS) {
            merged.put(key, newValues.getOrDefault(key, values.getOrDefault(key, DEFAULTS.get(key))));
        }
        writeConfigFile(merged);
        values.clear();
        values.putAll(merged);
    }

    private void writeConfigFile(Map<String, String> content) throws IOException {
        Path parent = configFile.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("# Jawwotations FX configuration\n");
        sb.append("# API_STYLE: php (API_BOARD_URL/API_NOTES_URL) or aws (API_BASE_URL)\n");
        sb.append("# WINDOW_* keys store the last window position/size (managed by the app)\n");
        for (String key : KEYS) {
            sb.append(key).append('=').append(content.getOrDefault(key, "")).append('\n');
        }
        Files.writeString(configFile, sb.toString());
    }

    private static Path expandHome(String path) {
        String trimmed = path.trim();
        if (trimmed.equals("~") || trimmed.startsWith("~/")) {
            return Path.of(System.getProperty("user.home") + trimmed.substring(1));
        }
        return Path.of(trimmed);
    }

    private static Path findEnvFile() {
        Path[] candidates = {
            Path.of(".env"),
            Path.of("frontend-fx", ".env")
        };
        for (Path p : candidates) {
            if (Files.isRegularFile(p)) {
                return p;
            }
        }
        return null;
    }

    private static Map<String, String> parseFile(Path file) {
        Map<String, String> result = new HashMap<>();
        if (file == null || !Files.isRegularFile(file)) {
            return result;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int eq = trimmed.indexOf('=');
                if (eq <= 0) {
                    continue;
                }
                String key = trimmed.substring(0, eq).trim();
                String value = trimmed.substring(eq + 1).trim();
                // strip optional surrounding quotes
                if (value.length() >= 2 && (value.startsWith("\"") && value.endsWith("\"")
                        || value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }
                result.put(key, value);
            }
        } catch (IOException e) {
            System.err.println("Cannot read " + file + ": " + e.getMessage());
        }
        return result;
    }

    public double getDouble(String key, double fallback) {
        try {
            return Double.parseDouble(get(key, ""));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public String get(String key, String fallback) {
        String env = System.getenv(key);
        if (env != null && !env.isBlank()) {
            return env;
        }
        String value = values.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    /** API flavor: "php" (default) or "aws". */
    public ApiStyle apiStyle() {
        return "aws".equalsIgnoreCase(get("API_STYLE", "php")) ? ApiStyle.AWS : ApiStyle.PHP;
    }

    /** Base URL of the AWS API Gateway (used only when API_STYLE=aws). */
    public String baseApiUrl() {
        String url = get("API_BASE_URL", "");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public String boardApiUrl() {
        return get("API_BOARD_URL", DEFAULTS.get("API_BOARD_URL"));
    }

    public String notesApiUrl() {
        return get("API_NOTES_URL", DEFAULTS.get("API_NOTES_URL"));
    }

    public String jwtToken() {
        return get("JWT_TOKEN", "");
    }
}
