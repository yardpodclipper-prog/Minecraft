package com.yourname.gtstracker;

import com.yourname.gtstracker.config.ConfigManager;
import com.yourname.gtstracker.config.ConfigModel;
import com.yourname.gtstracker.chat.GTSChatMonitor;
import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.ingest.ListingIngestionService;
import com.yourname.gtstracker.ui.CommandHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GTSTrackerMod implements ClientModInitializer {
    public static final String MOD_ID = "gtstracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GTSTrackerMod instance;

    private ConfigModel config;
    private DatabaseManager databaseManager;
    private ListingIngestionService ingestionService;
    private GTSChatMonitor chatMonitor;

    public static GTSTrackerMod getInstance() {
        return instance;
    }

    public ConfigModel getConfig() {
        return config;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ListingIngestionService getIngestionService() {
        return ingestionService;
    }

    @Override
    public void onInitializeClient() {
        try {
            instance = this;
            this.config = ConfigManager.load();

            this.databaseManager = new DatabaseManager();
            this.databaseManager.initialize();

            this.ingestionService = new ListingIngestionService(this.databaseManager);
            this.chatMonitor = new GTSChatMonitor(this.ingestionService, this.config);
            this.chatMonitor.register();

            CommandHandler.register();
            LOGGER.info(
                "Cobblemon GTS Tracker initialized. Environment: mc={}, fabric-loader={}, cobblemonLoaded={}",
                FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"),
                FabricLoader.getInstance().getModContainer("fabricloader").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"),
                FabricLoader.getInstance().isModLoaded("cobblemon")
            );
        } catch (RuntimeException e) {
            LOGGER.error("GTSTracker failed during client initialization. Commands/GUI may be unavailable.", e);
            throw e;
        }
    }
}
