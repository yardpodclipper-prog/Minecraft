package com.yourname.gtstracker.ui;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yourname.gtstracker.GTSTrackerMod;
import com.yourname.gtstracker.compat.CompatibilityReporter;
import com.yourname.gtstracker.data.ListingSnapshot;
import com.yourname.gtstracker.data.ListingSnapshotCache;
import com.yourname.gtstracker.database.models.ListingData;
import com.yourname.gtstracker.ui.bloomberg.BloombergGUI;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class CommandHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);
    private static ListingSnapshotCache snapshotCache;

    private CommandHandler() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(buildRootCommand("gts"));
            dispatcher.register(buildRootCommand("gtstracker"));
        });
    }

    private static synchronized ListingSnapshotCache getOrCreateSnapshotCache() {
        if (snapshotCache == null || snapshotCache.isClosed()) {
            snapshotCache = new ListingSnapshotCache(ListingSnapshot::empty);
        }
        return snapshotCache;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildRootCommand(String rootName) {
        return literal(rootName)
            .then(literal("status")
                .executes(context -> {
                    if (context.getSource().getPlayer() != null) {
                        String summary = CompatibilityReporter.summarizeRuntime();
                        context.getSource().getPlayer().sendMessage(Text.translatable("command.gtstracker.status"), false);
                        context.getSource().getPlayer().sendMessage(Text.literal("[GTSTracker] " + summary), false);
                        GTSTrackerMod.LOGGER.info("Status command invoked: {}", summary);
                    }
                    return Command.SINGLE_SUCCESS;
                }))
            .then(literal("ingesttest")
                .then(argument("message", StringArgumentType.greedyString())
                    .executes(context -> {
                        GTSTrackerMod mod = GTSTrackerMod.getInstance();
                        if (mod == null) {
                            return handleServiceUnavailable(context.getSource(), "ingesttest", "GTSTrackerMod.instance");
                        }
                        if (mod.getIngestionService() == null) {
                            return handleServiceUnavailable(context.getSource(), "ingesttest", "ListingIngestionService");
                        }

                        if (context.getSource().getPlayer() != null) {
                            String raw = StringArgumentType.getString(context, "message");
                            Optional<ListingData> listing = mod
                                .getIngestionService()
                                .ingestChatMessage(raw);

                            if (listing.isPresent()) {
                                context.getSource().getPlayer().sendMessage(Text.literal(
                                    "Parsed and stored listing: " + listing.get().getDisplayName()), false);
                            } else {
                                context.getSource().getPlayer().sendMessage(Text.literal(
                                    "Message did not match GTS parser."), false);
                            }
                        }
                        return Command.SINGLE_SUCCESS;
                    })))
            .then(literal("gui")
                .executes(context -> {
                    GTSTrackerMod mod = GTSTrackerMod.getInstance();
                    if (mod == null) {
                        return handleServiceUnavailable(context.getSource(), "gui", "GTSTrackerMod.instance");
                    }
                    if (mod.getDatabaseManager() == null) {
                        return handleServiceUnavailable(context.getSource(), "gui", "DatabaseManager");
                    }

                    try {
                        MinecraftClient client = MinecraftClient.getInstance();
                        client.setScreen(new BloombergGUI(getOrCreateSnapshotCache(), true));
                        LOGGER.info("Opened Bloomberg GUI via /{} gui", rootName);
                        return Command.SINGLE_SUCCESS;
                    } catch (RuntimeException exception) {
                        LOGGER.error("Failed to open Bloomberg GUI via /{} gui", rootName, exception);
                        if (context.getSource().getPlayer() != null) {
                            context.getSource().getPlayer().sendMessage(
                                Text.literal("[GTSTracker] Failed to open GUI. Check logs."),
                                false
                            );
                        }
                        return 0;
                    }
                }));
    }

    private static int handleServiceUnavailable(
        FabricClientCommandSource source,
        String commandName,
        String missingDependency
    ) {
        LOGGER.warn("Command unavailable: command={}, missingDependency={}", commandName, missingDependency);
        if (source.getPlayer() != null) {
            source.getPlayer().sendMessage(Text.literal("[GTSTracker] Service unavailable; check latest.log"), false);
        }
        return 0;
    }
}
