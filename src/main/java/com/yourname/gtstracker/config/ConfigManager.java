package com.yourname.gtstracker.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.yourname.gtstracker.GTSTrackerMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ConfigManager {
    private static final Path CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("gtstracker.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ConfigManager() {
    }

    public static ConfigModel load() {
        if (Files.exists(CONFIG_FILE)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_FILE)) {
                ConfigModel model = GSON.fromJson(reader, ConfigModel.class);
                return model != null ? model : new ConfigModel();
            } catch (IOException e) {
                GTSTrackerMod.LOGGER.error("Failed to load config.", e);
            }
        }

        ConfigModel defaults = new ConfigModel();
        save(defaults);
        return defaults;
    }

    public static void save(ConfigModel config) {
        try {
            Files.createDirectories(CONFIG_FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_FILE)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            GTSTrackerMod.LOGGER.error("Failed to save config.", e);
        }
    }
}
