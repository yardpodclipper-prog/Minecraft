package com.yourname.gtstracker.ui;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.yourname.gtstracker.GTSTrackerMod;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandHandlerServiceAvailabilityTest {

    @AfterEach
    void resetModInstance() throws Exception {
        setModInstance(null);
    }

    @SuppressWarnings("unchecked")
    @Test
    void ingestTestReturnsNonSuccessAndNotifiesPlayerWhenServiceUnavailable() throws Exception {
        setModInstance(null);

        Method buildRootCommand = CommandHandler.class.getDeclaredMethod("buildRootCommand", String.class);
        buildRootCommand.setAccessible(true);

        CommandDispatcher<FabricClientCommandSource> dispatcher = new CommandDispatcher<>();
        dispatcher.register((LiteralArgumentBuilder<FabricClientCommandSource>) buildRootCommand.invoke(null, "gts"));

        FabricClientCommandSource source = Mockito.mock(FabricClientCommandSource.class);
        ClientPlayerEntity player = Mockito.mock(ClientPlayerEntity.class);
        Mockito.when(source.getPlayer()).thenReturn(player);

        int result = dispatcher.execute("gts ingesttest test message", source);

        assertEquals(0, result);

        ArgumentCaptor<Text> messageCaptor = ArgumentCaptor.forClass(Text.class);
        Mockito.verify(player).sendMessage(messageCaptor.capture(), Mockito.eq(false));
        assertTrue(messageCaptor.getValue().getString().contains("Service unavailable"));
    }

    private static void setModInstance(GTSTrackerMod mod) throws Exception {
        Field field = GTSTrackerMod.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, mod);
    }
}
