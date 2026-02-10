package com.yourname.gtstracker.chat;

import com.yourname.gtstracker.config.ConfigModel;
import com.yourname.gtstracker.service.ListingIngestionService;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors inbound client chat/game messages and forwards plain text payloads to ingestion.
 */
public class GTSChatMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(GTSChatMonitor.class);

    private final ListingIngestionService ingestionService;
    private final ConfigModel config;

    public GTSChatMonitor(ListingIngestionService ingestionService, ConfigModel config) {
        this.ingestionService = ingestionService;
        this.config = config;
    }

    public void register() {
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) -> {
            processIncoming(message);
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            processIncoming(message);
        });
    }

    private void processIncoming(Text message) {
        if (!config.isChatMonitoringEnabled()) {
            return;
        }

        String plainMessage = message.getString();
        ingestionService.ingestChatMessage(plainMessage);

        LOGGER.debug("GTS chat monitor ingested incoming message: {}", plainMessage);

        if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null) {
                client.player.sendMessage(Text.literal("[GTSTracker] Captured chat message."), false);
            }
        }
    }
}
