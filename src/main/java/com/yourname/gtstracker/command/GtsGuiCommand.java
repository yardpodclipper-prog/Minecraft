package com.yourname.gtstracker.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yourname.gtstracker.data.ListingSnapshotCache;
import com.yourname.gtstracker.data.ListingSnapshotProvider;
import com.yourname.gtstracker.ui.bloomberg.BloombergGUI;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.CommandManager;

/**
 * Registers /gts gui and opens the Bloomberg GUI instead of placeholder chat output.
 */
public final class GtsGuiCommand {
    private static ListingSnapshotCache snapshotCache;

    private GtsGuiCommand() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> create(ListingSnapshotProvider snapshotProvider) {
        snapshotCache = new ListingSnapshotCache(snapshotProvider);

        return CommandManager.literal("gts")
            .then(CommandManager.literal("gui")
                .executes(context -> {
                    MinecraftClient client = context.getSource().getClient();
                    client.setScreen(new BloombergGUI(snapshotCache));
                    return Command.SINGLE_SUCCESS;
                }));
    }
}
