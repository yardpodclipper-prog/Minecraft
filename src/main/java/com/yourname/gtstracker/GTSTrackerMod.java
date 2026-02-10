package com.yourname.gtstracker;

import com.yourname.gtstracker.chat.GTSChatMonitor;
import com.yourname.gtstracker.config.ConfigModel;
import com.yourname.gtstracker.service.ListingIngestionService;
import net.fabricmc.api.ClientModInitializer;

/**
 * Client entrypoint for GTSTracker.
 */
public class GTSTrackerMod implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize dependencies in order.
        initializeDatabase();

        ConfigModel config = new ConfigModel();
        ListingIngestionService ingestionService = new ListingIngestionService();

        GTSChatMonitor chatMonitor = new GTSChatMonitor(ingestionService, config);
        chatMonitor.register();
    }

    private void initializeDatabase() {
        // Placeholder for DB initialization.
    }
}
