package com.yourname.gtstracker.ui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHandlerRegistrationTest {

    @Test
    void registersBothRootAliasesWithEquivalentSubcommands() {
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();

        CommandHandler.registerAliasCommands(dispatcher);

        CommandNode<FabricClientCommandSource> gtsRoot = dispatcher.getRoot().getChild("gts");
        CommandNode<FabricClientCommandSource> trackerRoot = dispatcher.getRoot().getChild("gtstracker");

        assertNotNull(gtsRoot, "Expected /gts alias to be registered");
        assertNotNull(trackerRoot, "Expected /gtstracker root to be registered");

        Set<String> expectedSubcommands = Set.of("status", "ingesttest", "gui");

        Set<String> gtsSubcommands = gtsRoot.getChildren().stream()
            .map(CommandNode::getName)
            .collect(Collectors.toSet());
        Set<String> trackerSubcommands = trackerRoot.getChildren().stream()
            .map(CommandNode::getName)
            .collect(Collectors.toSet());

        assertEquals(expectedSubcommands, gtsSubcommands);
        assertEquals(expectedSubcommands, trackerSubcommands);
    }

    @Test
    void ingesttestCommandKeepsGreedyMessageArgument() {
        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();

        CommandHandler.registerAliasCommands(dispatcher);

        CommandNode<FabricClientCommandSource> ingesttest = dispatcher
            .getRoot()
            .getChild("gtstracker")
            .getChild("ingesttest");

        assertNotNull(ingesttest);
        assertTrue(ingesttest.getChild("message") != null, "Expected greedy 'message' argument under ingesttest");
    }
}
