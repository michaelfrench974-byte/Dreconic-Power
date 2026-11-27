package com.voicemagic.voice;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.voicemagic.VoiceMagicMod;
import com.voicemagic.component.ComponentRegistry;
import com.voicemagic.component.SpellComponent;
import net.neoforged.fml.loading.FMLPaths;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Wraps Vosk (offline speech recognition). All Vosk classes are referenced ONLY here.
 *
 * Model location:  config/voicemagic/model/   (contents of vosk-model-small-en-us-0.15)
 */
public class VoskEngine {
    private static final Gson GSON = new Gson();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "VoiceMagic-Vosk");
        t.setDaemon(true);
        return t;
    });

    private static volatile Model model;
    private static volatile boolean starting = false;
    private static final Map<UUID, Recognizer> RECOGNIZERS = new ConcurrentHashMap<>();

    /** Start loading the model in the background (safe to call more than once). */
    public static synchronized void start() {
        if (starting || model != null) return;
        starting = true;
        VoiceManager.engineState = "loading model...";
        EXEC.submit(() -> {
            try {
                Path base = FMLPaths.CONFIGDIR.get().resolve("voicemagic");
                Files.createDirectories(base);
                extractNatives(base.resolve("natives"));

                Path modelDir = findModelDir(base.resolve("model"));
                if (modelDir == null) {
                    VoiceManager.engineState = "MODEL MISSING - put the Vosk model in config/voicemagic/model";
                    VoiceMagicMod.LOGGER.warn("Vosk model not found in {}. Download vosk-model-small-en-us-0.15 " +
                            "from https://alphacephei.com/vosk/models and unzip its CONTENTS there.", base.resolve("model"));
                    starting = false;
                    return;
                }
                LibVosk.setLogLevel(LogLevel.WARNINGS);
                model = new Model(modelDir.toString());
                VoiceManager.engineReady = true;
                VoiceManager.engineState = "ready";
                VoiceMagicMod.LOGGER.info("Vosk speech model loaded from {}", modelDir);
            } catch (Throwable t) {
                VoiceManager.engineState = "ERROR: " + t.getClass().getSimpleName() + " - " + t.getMessage();
                VoiceMagicMod.LOGGER.error("Could not start speech recognition", t);
                starting = false;
            }
        });
    }

    /** Throw away recognizers so they are rebuilt with the current component words. */
    public static void invalidate() {
        EXEC.submit(() -> {
            for (Recognizer r : RECOGNIZERS.values()) {
                try { r.close(); } catch (Throwable ignored) {}
            }
            RECOGNIZERS.clear();
        });
    }

    /**
     * Feed audio. pcm16k = 16 kHz mono 16-bit samples.
     * endOfSpeech = true when the player stopped talking (flushes the sentence).
     */
    public static void feed(UUID player, short[] pcm16k, boolean endOfSpeech, Consumer<String> onText) {
        if (!VoiceManager.engineReady) return;
        EXEC.submit(() -> {
            try {
                Recognizer rec = RECOGNIZERS.computeIfAbsent(player, id -> newRecognizer());
                if (rec == null) return;

                if (pcm16k != null && pcm16k.length > 0) {
                    byte[] bytes = new byte[pcm16k.length * 2];
                    for (int i = 0; i < pcm16k.length; i++) {
                        bytes[i * 2] = (byte) (pcm16k[i] & 0xFF);
                        bytes[i * 2 + 1] = (byte) ((pcm16k[i] >> 8) & 0xFF);
                    }
                    if (rec.acceptWaveForm(bytes, bytes.length)) {
                        emit(rec.getResult(), onText);
                    }
                }
                if (endOfSpeech) {
                    emit(rec.getFinalResult(), onText);
                    rec.reset();
                }
            } catch (Throwable t) {
                VoiceMagicMod.LOGGER.error("Speech recognition error", t);
            }
        });
    }

    private static void emit(String json, Consumer<String> onText) {
        try {
            JsonObject o = GSON.fromJson(json, JsonObject.class);
            if (o != null && o.has("text")) {
                String text = o.get("text").getAsString().trim();
                if (!text.isEmpty()) onText.accept(text);
            }
        } catch (Exception ignored) {
        }
    }

    /** Recognizer limited to the words in our component list - far more accurate for short magic words. */
    private static Recognizer newRecognizer() {
        try {
            JsonArray words = new JsonArray();
            for (SpellComponent c : ComponentRegistry.getAll()) {
                words.add(c.word);
            }
            words.add("[unk]");
            return new Recognizer(model, 16000.0f, GSON.toJson(words));
        } catch (Throwable t) {
            VoiceMagicMod.LOGGER.error("Could not create recognizer", t);
            return null;
        }
    }

    /** Accepts config/voicemagic/model or a single sub-folder inside it that contains the "am" folder. */
    private static Path findModelDir(Path dir) throws java.io.IOException {
        if (!Files.isDirectory(dir)) return null;
        if (Files.isDirectory(dir.resolve("am"))) return dir;
        try (var s = Files.list(dir)) {
            for (Path p : (Iterable<Path>) s::iterator) {
                if (Files.isDirectory(p) && Files.isDirectory(p.resolve("am"))) return p;
            }
        }
        return null;
    }

    /**
     * Vosk's native library lives inside the Vosk jar. Because Minecraft loads mods in a special way,
     * we copy the natives to disk ourselves and tell JNA where they are.
     */
    private static void extractNatives(Path dir) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase();
            String[] resources;
            if (os.contains("win")) {
                resources = new String[]{
                        "win32-x86-64/libvosk.dll", "win32-x86-64/libgcc_s_seh-1.dll",
                        "win32-x86-64/libstdc++-6.dll", "win32-x86-64/libwinpthread-1.dll"};
            } else if (os.contains("linux")) {
                resources = new String[]{"linux-x86-64/libvosk.so"};
            } else {
                return; // macOS: let JNA try on its own
            }
            Files.createDirectories(dir);
            ClassLoader cl = VoskEngine.class.getClassLoader();
            for (String res : resources) {
                try (InputStream in = cl.getResourceAsStream(res)) {
                    if (in == null) continue;
                    String name = res.substring(res.lastIndexOf('/') + 1);
                    Path target = dir.resolve(name);
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                    if (name.equals("libvosk.dll")) {
                        Files.copy(target, dir.resolve("vosk.dll"), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (Exception e) {
                    VoiceMagicMod.LOGGER.warn("Could not extract {}: {}", res, e.toString());
                }
            }
            com.sun.jna.NativeLibrary.addSearchPath("vosk", dir.toAbsolutePath().toString());
        } catch (Throwable t) {
            VoiceMagicMod.LOGGER.warn("Native extraction skipped: {}", t.toString());
        }
    }
}
