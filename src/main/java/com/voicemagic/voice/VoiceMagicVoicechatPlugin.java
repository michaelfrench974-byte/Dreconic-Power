package com.voicemagic.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.VoicechatServerStartedEvent;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;
import com.voicemagic.VoiceMagicMod;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Hooks into Simple Voice Chat: every microphone packet a player sends reaches us on the server,
 * we decode it, and pass it to Vosk. Finished sentences go to VoiceManager -> spell casting.
 */
@ForgeVoicechatPlugin
public class VoiceMagicVoicechatPlugin implements VoicechatPlugin {

    private final Map<UUID, OpusDecoder> decoders = new ConcurrentHashMap<>();

    @Override
    public String getPluginId() {
        return "voicemagic";
    }

    @Override
    public void initialize(VoicechatApi api) {
        VoiceMagicMod.LOGGER.info("Voice Magic hooked into Simple Voice Chat.");
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(VoicechatServerStartedEvent.class, e -> {
            VoiceManager.voiceChatSeen = true;
            try {
                VoskEngine.start();
            } catch (Throwable t) {
                VoiceManager.engineState = "ERROR: speech library failed to load (" + t.getClass().getSimpleName() + ")";
                VoiceMagicMod.LOGGER.error("Vosk could not be started", t);
            }
        });
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        if (!VoiceManager.engineReady) return;

        VoicechatConnection connection = event.getSenderConnection();
        if (connection == null) return;
        Object raw = connection.getPlayer().getPlayer();
        if (!(raw instanceof ServerPlayer player)) return;
        if (!VoiceManager.isEnabled(player.getUUID())) return;

        UUID id = player.getUUID();
        VoicechatServerApi api = event.getVoicechat();
        OpusDecoder decoder = decoders.computeIfAbsent(id, k -> api.createDecoder());

        byte[] opus = event.getPacket().getOpusEncodedData();
        boolean end = opus == null || opus.length == 0;

        short[] pcm16k = null;
        if (!end) {
            short[] pcm48k = decoder.decode(opus);
            // 48 kHz -> 16 kHz by averaging each group of 3 samples
            int n = pcm48k.length / 3;
            pcm16k = new short[n];
            for (int i = 0; i < n; i++) {
                int sum = pcm48k[i * 3] + pcm48k[i * 3 + 1] + pcm48k[i * 3 + 2];
                pcm16k[i] = (short) (sum / 3);
            }
        } else {
            decoder.resetState();
        }

        var server = player.getServer();
        if (server == null) return;
        VoskEngine.feed(id, pcm16k, end, text -> VoiceManager.onSpeech(server, id, text));
    }
}
