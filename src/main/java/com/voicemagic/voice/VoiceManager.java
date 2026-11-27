package com.voicemagic.voice;

import com.voicemagic.event.ModEvents;
import com.voicemagic.VoiceMagicMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Safe facade between the game and the voice pieces.
 * This class does NOT reference Simple Voice Chat or Vosk classes, so the mod still loads
 * (just without voice) if either is missing.
 */
public class VoiceManager {
    /** A spoken line must contain at least this many known words to cast (stops accidental casts from chatting). */
    public static final int MIN_WORDS = 2;

    /** Players who have voice casting turned off. (Default = everyone is listening.) */
    private static final Set<UUID> DISABLED = ConcurrentHashMap.newKeySet();

    /** Set by the plugin when Simple Voice Chat has started its server side. */
    public static volatile boolean voiceChatSeen = false;
    /** Human readable engine state: loading / ready / error text. */
    public static volatile String engineState = "not started";
    public static volatile boolean engineReady = false;

    public static boolean isEnabled(UUID id) {
        return !DISABLED.contains(id);
    }

    public static void setEnabled(ServerPlayer player, boolean on) {
        if (on) DISABLED.remove(player.getUUID());
        else DISABLED.add(player.getUUID());
    }

    public static String statusLine(ServerPlayer player) {
        StringBuilder sb = new StringBuilder();
        sb.append(isEnabled(player.getUUID()) ? "listening ON" : "listening OFF");
        sb.append(" | Simple Voice Chat: ").append(voiceChatSeen ? "connected" : "not detected");
        sb.append(" | Speech engine: ").append(engineState);
        return sb.toString();
    }

    /** Called from the voice thread with a finished sentence. Hops to the server thread to cast. */
    public static void onSpeech(MinecraftServer server, UUID playerId, String text) {
        if (text == null) return;
        String cleaned = text.replace("[unk]", " ").trim();
        if (cleaned.isEmpty()) return;
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player == null || !isEnabled(playerId)) return;
            VoiceMagicMod.LOGGER.info("Heard {}: \"{}\"", player.getGameProfile().getName(), cleaned);
            ModEvents.handleSpoken(player, cleaned, true, MIN_WORDS);
        });
    }

    /** Called after /voicemagic reload so the recognizer vocabulary is rebuilt. */
    public static void onComponentsReloaded() {
        try {
            VoskEngine.invalidate();
        } catch (Throwable ignored) {
            // Vosk not available - nothing to refresh
        }
    }
}
