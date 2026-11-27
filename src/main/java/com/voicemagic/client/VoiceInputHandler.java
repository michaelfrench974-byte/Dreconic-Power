package com.voicemagic.client;

import com.voicemagic.VoiceMagicMod;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Client-side placeholder for microphone input.
 *
 * Right now it just opens a simple chat-style prompt when you press the key
 * so you can test the full spell pipeline without a real mic library.
 *
 * To add real offline recognition later:
 *  1. Add Vosk (or Whisper-jni) as a dependency
 *  2. Capture audio while the key is held
 *  3. Run recognition
 *  4. Send the resulting string to the server via a network packet
 *     that calls ModEvents.handleSpoken(player, text)
 */
@EventBusSubscriber(modid = VoiceMagicMod.MODID, value = Dist.CLIENT)
public class VoiceInputHandler {

    public static final KeyMapping CAST_KEY = new KeyMapping(
            "key.voicemagic.cast",
            GLFW.GLFW_KEY_V,
            "key.categories.voicemagic"
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CAST_KEY);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (CAST_KEY.consumeClick()) {
            // For now: open chat with a hint so the player can type the words
            // (or later replace this whole block with real mic capture)
            mc.player.displayClientMessage(
                    net.minecraft.network.chat.Component.literal(
                            "§d[Voice Magic] Hold V + speak, or use §f/voicemagic cast <words>§d for testing"),
                    true);
        }
    }
}
