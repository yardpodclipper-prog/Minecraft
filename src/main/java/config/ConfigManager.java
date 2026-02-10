package config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public final class ConfigManager {
    private static final String USERNAME_PATTERN = "[A-Za-z0-9_][A-Za-z0-9_' ]*";
    private static final String NAME_PATTERN = "[A-Za-z0-9_][A-Za-z0-9_' ]*";

    private static ConfigModel config;

    private ConfigManager() {
    }

    public static synchronized void initialize() {
        if (config == null) {
            config = loadConfig(Path.of("config.properties"));
        }
    }

    public static synchronized ConfigModel getConfig() {
        if (config == null) {
            initialize();
        }
        return config;
    }

    static ConfigModel loadConfig(Path path) {
        Properties properties = new Properties();
        if (Files.exists(path)) {
            try (InputStream in = Files.newInputStream(path)) {
                properties.load(in);
            } catch (IOException ignored) {
                // Fallback to defaults if config cannot be read.
            }
        }

        ConfigModel model = new ConfigModel();
        model.setPokemonParserPatterns(readPatterns(
                properties,
                "parser.patterns.pokemon",
                defaultPokemonPatterns()
        ));
        model.setItemParserPatterns(readPatterns(
                properties,
                "parser.patterns.item",
                defaultItemPatterns()
        ));

        return model;
    }

    private static List<String> readPatterns(Properties properties, String key, List<String> defaults) {
        String configured = properties.getProperty(key);
        if (configured == null || configured.isBlank()) {
            return defaults;
        }

        return Arrays.stream(configured.split("\\|\\|"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static List<String> defaultPokemonPatterns() {
        return List.of(
                "(?i)\\[(?<seller>" + USERNAME_PATTERN + ")\\]\\s+listed\\s+(?<species>" + NAME_PATTERN + ")\\s+for\\s+\\$?(?<price>[0-9,._ ]+)",
                "(?i)seller\\s*[:=-]\\s*(?<seller>" + USERNAME_PATTERN + ").*?pokemon\\s*[:=-]\\s*(?<species>" + NAME_PATTERN + ").*?price\\s*[:=-]\\s*\\$?(?<price>[0-9,._ ]+)",
                "(?i)(?<seller>" + USERNAME_PATTERN + ")\\s+is\\s+selling\\s+(?<species>" + NAME_PATTERN + ")\\s+for\\s+\\$?(?<price>[0-9,._ ]+)"
        );
    }

    private static List<String> defaultItemPatterns() {
        return List.of(
                "(?i)\\[(?<seller>" + USERNAME_PATTERN + ")\\]\\s+listed\\s+(?<item>" + NAME_PATTERN + ")\\s+for\\s+\\$?(?<price>[0-9,._ ]+)",
                "(?i)seller\\s*[:=-]\\s*(?<seller>" + USERNAME_PATTERN + ").*?item\\s*[:=-]\\s*(?<item>" + NAME_PATTERN + ").*?price\\s*[:=-]\\s*\\$?(?<price>[0-9,._ ]+)",
                "(?i)(?<seller>" + USERNAME_PATTERN + ")\\s+is\\s+selling\\s+(?<item>" + NAME_PATTERN + ")\\s+for\\s+\\$?(?<price>[0-9,._ ]+)"
        );
    }
}
