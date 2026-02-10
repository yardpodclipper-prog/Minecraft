package com.yourname.gtstracker;

import com.yourname.gtstracker.chat.GTSChatMonitor;
import com.yourname.gtstracker.compat.CompatibilityReporter;
import com.yourname.gtstracker.config.ConfigManager;
import com.yourname.gtstracker.config.ConfigModel;
import com.yourname.gtstracker.database.DatabaseManager;
import com.yourname.gtstracker.ingest.ListingIngestionService;
import com.yourname.gtstracker.ui.CommandHandler;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public final class GTSTrackerMod implements ClientModInitializer {
    public static final String MOD_ID = "gtstracker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static GTSTrackerMod instance;

    private final Supplier<ConfigModel> configLoader;
    private final Supplier<DatabaseManager> databaseManagerFactory;
    private final Function<DatabaseManager, ListingIngestionService> ingestionServiceFactory;
    private final BiFunction<ListingIngestionService, ConfigModel, GTSChatMonitor> chatMonitorFactory;
    private final Runnable compatibilityLogger;
    private final Runnable commandRegistrar;
    private final StartupLogger startupLogger;

    private ConfigModel config;
    private DatabaseManager databaseManager;
    private ListingIngestionService ingestionService;
    private GTSChatMonitor chatMonitor;

    public GTSTrackerMod() {
        this(
            ConfigManager::load,
            DatabaseManager::new,
            ListingIngestionService::new,
            GTSChatMonitor::new,
            CompatibilityReporter::logStartupCompatibility,
            CommandHandler::register,
            new DefaultStartupLogger()
        );
    }

    GTSTrackerMod(
        Supplier<ConfigModel> configLoader,
        Supplier<DatabaseManager> databaseManagerFactory,
        Function<DatabaseManager, ListingIngestionService> ingestionServiceFactory,
        BiFunction<ListingIngestionService, ConfigModel, GTSChatMonitor> chatMonitorFactory,
        Runnable compatibilityLogger,
        Runnable commandRegistrar,
        StartupLogger startupLogger
    ) {
        this.configLoader = configLoader;
        this.databaseManagerFactory = databaseManagerFactory;
        this.ingestionServiceFactory = ingestionServiceFactory;
        this.chatMonitorFactory = chatMonitorFactory;
        this.compatibilityLogger = compatibilityLogger;
        this.commandRegistrar = commandRegistrar;
        this.startupLogger = startupLogger;
    }

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
        instance = this;
        LOGGER.info("Starting Cobblemon GTS Tracker initialization.");

        try {
            this.config = ConfigManager.load();

            this.databaseManager = databaseManagerFactory.get();
            this.databaseManager.initialize();
            if (this.databaseManager.getConnection() == null) {
                throw new IllegalStateException("Database initialization did not produce a connection.");
            }

            this.ingestionService = ingestionServiceFactory.apply(this.databaseManager);
            this.chatMonitor = chatMonitorFactory.apply(this.ingestionService, this.config);
            this.chatMonitor.register();

            CompatibilityReporter.logStartupCompatibility();
            CommandHandler.register();
            LOGGER.info(
                "Cobblemon GTS Tracker initialized. Environment: mc={}, fabric-loader={}, cobblemonLoaded={}.",
                FabricLoader.getInstance().getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"),
                FabricLoader.getInstance().getModContainer("fabricloader").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("unknown"),
                FabricLoader.getInstance().isModLoaded("cobblemon")
            );
        } catch (RuntimeException e) {
            LOGGER.error("GTSTracker failed during client initialization. Startup aborted.", e);
            throw e;
        }
    }
}
