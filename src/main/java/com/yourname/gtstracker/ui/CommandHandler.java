package com.yourname.gtstracker.ui;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.yourname.gtstracker.GTSTrackerMod;
import com.yourname.gtstracker.database.models.ListingData;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import java.util.Optional;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

public final class CommandHandler {
    private CommandHandler() {
    }

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
            dispatcher.register(literal("gts")
                .then(literal("status")
                    .executes(context -> {
                        if (context.getSource().getPlayer() != null) {
                            int count = GTSTrackerMod.getInstance().getDatabaseManager().getTotalListingsCount();
                            context.getSource().getPlayer().sendMessage(Text.literal(
                                "GTS Tracker ready | Listings in DB: " + count), false);
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(literal("ingesttest")
                    .then(argument("message", StringArgumentType.greedyString())
                        .executes(context -> {
                            if (context.getSource().getPlayer() != null) {
                                String raw = StringArgumentType.getString(context, "message");
                                Optional<ListingData> listing = GTSTrackerMod.getInstance()
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
                .then(literal("next")
                    .executes(context -> {
                        if (context.getSource().getPlayer() != null) {
                            context.getSource().getPlayer().sendMessage(Text.literal(
                                "Next: chat packet hook, listing DAO queries, alert rules, Bloomberg overview screen."), false);
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
                .then(literal("gui")
                    .executes(context -> {
                        if (context.getSource().getPlayer() != null) {
                            context.getSource().getPlayer().sendMessage(Text.literal(
                                "Bloomberg GUI scaffold is not wired yet."), false);
                        }
                        return Command.SINGLE_SUCCESS;
                    }))
            )
        );
    }
}
