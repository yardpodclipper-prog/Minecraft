package com.yourname.gtstracker.ui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yourname.gtstracker.GTSTrackerMod;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandHandlerServiceAvailabilityTest {

    @AfterEach
    void resetModInstance() throws Exception {
        setModInstance(null);
    }

    @SuppressWarnings("unchecked")
    @Test
    void ingestTestReturnsNonSuccessWhenServiceUnavailable() throws Exception {
        setModInstance(null);

        Method buildRootCommand = CommandHandler.class.getDeclaredMethod("buildRootCommand", String.class);
        buildRootCommand.setAccessible(true);

        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register((LiteralArgumentBuilder<FabricClientCommandSource>) buildRootCommand.invoke(null, "gts"));

        FabricClientCommandSource source = Mockito.mock(FabricClientCommandSource.class);
        Mockito.when(source.getPlayer()).thenReturn(null);

        int result = dispatcher.execute("gts ingesttest test message", source);

        assertEquals(0, result);
    }

    private static void setModInstance(GTSTrackerMod mod) throws Exception {
        Field field = GTSTrackerMod.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mod);
    }
}
