package com.voicemagic.network;

import com.voicemagic.VoiceMagicMod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Placeholder for future client → server voice packets.
 * When you add real microphone recognition on the client, send the recognized text here.
 */
public class ModNetworking {
    public static void register(IEventBus modBus) {
        modBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(VoiceMagicMod.MODID);
        // Example future packet:
        // registrar.playToServer(SpokenWordsPayload.TYPE, SpokenWordsPayload.STREAM_CODEC, SpokenWordsPayload::handle);
    }
}
