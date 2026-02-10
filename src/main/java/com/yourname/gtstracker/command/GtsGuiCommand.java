package com.yourname.gtstracker.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yourname.gtstracker.data.ListingSnapshotCache;
import com.yourname.gtstracker.data.ListingSnapshotProvider;
import com.yourname.gtstracker.ui.bloomberg.BloombergGUI;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

/**
 * Registers /gts gui and opens the Bloomberg GUI instead of placeholder chat output.
 */
public final class GtsGuiCommand {
    private static ListingSnapshotCache snapshotCache;

    private GtsGuiCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> create(ListingSnapshotProvider snapshotProvider) {
        return literal("gts")
            .then(literal("gui")
                .executes(context -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    client.setScreen(new BloombergGUI(getOrCreateCache(snapshotProvider), true));
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private static synchronized ListingSnapshotCache getOrCreateCache(ListingSnapshotProvider snapshotProvider) {
        if (snapshotCache == null || snapshotCache.isClosed()) {
            snapshotCache = new ListingSnapshotCache(snapshotProvider);
        }
        return snapshotCache;
    }
}
