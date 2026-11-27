package com.voicemagic.component;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.voicemagic.VoiceMagicMod;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Loads all spell components from JSON files.
 * Place / edit files in: config/voicemagic/spell_components/  (or the datapack path)
 * Each file can contain one or many components.
 */
public class ComponentRegistry {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<String, SpellComponent> BY_WORD = new HashMap<>();
    private static final Map<String, SpellComponent> BY_ID = new HashMap<>();

    public static void loadComponents() {
        BY_WORD.clear();
        BY_ID.clear();

        Path configDir = FMLPaths.CONFIGDIR.get().resolve("voicemagic").resolve("spell_components");
        try {
            Files.createDirectories(configDir);
            // Also copy defaults from resources if the folder is empty
            copyDefaultsIfEmpty(configDir);

            try (var stream = Files.walk(configDir)) {
                stream.filter(p -> p.toString().endsWith(".json"))
                      .forEach(ComponentRegistry::loadFile);
            }
        } catch (IOException e) {
            VoiceMagicMod.LOGGER.error("Failed to load spell components", e);
        }

        VoiceMagicMod.LOGGER.info("Registered {} spell components", BY_ID.size());
    }

    private static void loadFile(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            JsonElement root = GSON.fromJson(reader, JsonElement.class);
            if (root.isJsonArray()) {
                for (JsonElement el : root.getAsJsonArray()) {
                    parseOne(el.getAsJsonObject());
                }
            } else if (root.isJsonObject()) {
                parseOne(root.getAsJsonObject());
            }
        } catch (Exception e) {
            VoiceMagicMod.LOGGER.error("Failed to parse component file {}", path, e);
        }
    }

    private static void parseOne(JsonObject obj) {
        String id = obj.get("id").getAsString();
        String word = obj.get("word").getAsString();
        int score = obj.has("score") ? obj.get("score").getAsInt() : 10;
        int cost = obj.has("cost") ? obj.get("cost").getAsInt() : 5;
        String desc = obj.has("description") ? obj.get("description").getAsString() : "";

        List<String> elements = new ArrayList<>();
        if (obj.has("elements")) {
            JsonArray arr = obj.getAsJsonArray("elements");
            for (JsonElement e : arr) elements.add(e.getAsString());
        }

        List<String> modifiers = new ArrayList<>();
        if (obj.has("modifiers")) {
            JsonArray arr = obj.getAsJsonArray("modifiers");
            for (JsonElement e : arr) modifiers.add(e.getAsString());
        }

        SpellComponent comp = new SpellComponent(id, word, elements, modifiers, score, cost, desc);
        BY_ID.put(id, comp);
        BY_WORD.put(comp.word, comp);
    }

    public static SpellComponent getByWord(String word) {
        return BY_WORD.get(word.toLowerCase().trim());
    }

    public static Collection<SpellComponent> getAll() {
        return BY_ID.values();
    }

    public static Optional<SpellComponent> findBestMatch(String spoken) {
        // Simple exact match first. Later you can add fuzzy / multi-word matching.
        SpellComponent exact = getByWord(spoken);
        if (exact != null) return Optional.of(exact);

        // Fallback: contains match (useful while testing)
        for (SpellComponent c : BY_WORD.values()) {
            if (spoken.toLowerCase().contains(c.word)) {
                return Optional.of(c);
            }
        }
        return Optional.empty();
    }

    private static void copyDefaultsIfEmpty(Path configDir) throws IOException {
        if (Files.list(configDir).findAny().isPresent()) return;

        // Write the starter component set so the user has something to edit immediately
        Path starter = configDir.resolve("starter_components.json");
        String json = """
        [
          {
            "id": "ice_base",
            "word": "frost",
            "elements": ["Ice"],
            "modifiers": [],
            "score": 10,
            "cost": 8,
            "description": "Base ice element"
          },
          {
            "id": "fire_base",
            "word": "flame",
            "elements": ["Fire"],
            "modifiers": [],
            "score": 10,
            "cost": 8,
            "description": "Base fire element"
          },
          {
            "id": "lightning_base",
            "word": "spark",
            "elements": ["Lightning"],
            "modifiers": [],
            "score": 10,
            "cost": 10,
            "description": "Base lightning element"
          },
          {
            "id": "earth_base",
            "word": "stone",
            "elements": ["Earth"],
            "modifiers": [],
            "score": 10,
            "cost": 8,
            "description": "Base earth element"
          },
          {
            "id": "construct",
            "word": "build",
            "elements": [],
            "modifiers": ["Construct"],
            "score": 15,
            "cost": 12,
            "description": "Construct / form structure"
          },
          {
            "id": "remove",
            "word": "break",
            "elements": [],
            "modifiers": ["Remove"],
            "score": 12,
            "cost": 10,
            "description": "Remove / destroy"
          },
          {
            "id": "move",
            "word": "push",
            "elements": [],
            "modifiers": ["Move"],
            "score": 12,
            "cost": 10,
            "description": "Move / force"
          },
          {
            "id": "trap",
            "word": "bind",
            "elements": [],
            "modifiers": ["Trap"],
            "score": 15,
            "cost": 14,
            "description": "Trap / immobilize"
          },
          {
            "id": "ignite",
            "word": "burn",
            "elements": [],
            "modifiers": ["Ignite"],
            "score": 12,
            "cost": 10,
            "description": "Ignite / set on fire"
          },
          {
            "id": "spread",
            "word": "spread",
            "elements": [],
            "modifiers": ["Spread"],
            "score": 14,
            "cost": 12,
            "description": "Spread effect"
          },
          {
            "id": "spear",
            "word": "pierce",
            "elements": [],
            "modifiers": ["Spear"],
            "score": 15,
            "cost": 12,
            "description": "Spear / pierce form"
          },
          {
            "id": "plus_charge",
            "word": "charge",
            "elements": [],
            "modifiers": ["+Charge"],
            "score": 10,
            "cost": 8,
            "description": "Positive charge"
          },
          {
            "id": "minus_charge",
            "word": "drain",
            "elements": [],
            "modifiers": ["-Charge"],
            "score": 10,
            "cost": 8,
            "description": "Negative charge (player default)"
          },
          {
            "id": "chain",
            "word": "chain",
            "elements": [],
            "modifiers": ["Chain"],
            "score": 18,
            "cost": 15,
            "description": "Chain to nearby targets"
          },
          {
            "id": "plus_range",
            "word": "far",
            "elements": [],
            "modifiers": ["+Range"],
            "score": 8,
            "cost": 6,
            "description": "Increase range"
          },
          {
            "id": "minus_range",
            "word": "near",
            "elements": [],
            "modifiers": ["-Range"],
            "score": 5,
            "cost": 3,
            "description": "Decrease range"
          },
          {
            "id": "curve_left",
            "word": "left",
            "elements": [],
            "modifiers": ["Curve Left"],
            "score": 8,
            "cost": 5,
            "description": "Curve trajectory left"
          },
          {
            "id": "curve_right",
            "word": "right",
            "elements": [],
            "modifiers": ["Curve Right"],
            "score": 8,
            "cost": 5,
            "description": "Curve trajectory right"
          },
          {
            "id": "curve_up",
            "word": "rise",
            "elements": [],
            "modifiers": ["Curve Up"],
            "score": 8,
            "cost": 5,
            "description": "Curve trajectory up"
          },
          {
            "id": "curve_down",
            "word": "fall",
            "elements": [],
            "modifiers": ["Curve Down"],
            "score": 8,
            "cost": 5,
            "description": "Curve trajectory down"
          },
          {
            "id": "light",
            "word": "holy",
            "elements": ["Light"],
            "modifiers": [],
            "score": 12,
            "cost": 10,
            "description": "Light - extra damage to undead, heals living"
          },
          {
            "id": "dark",
            "word": "shadow",
            "elements": ["Dark"],
            "modifiers": [],
            "score": 12,
            "cost": 10,
            "description": "Dark - pure damage"
          },
          {
            "id": "plus_cost",
            "word": "empower",
            "elements": [],
            "modifiers": ["+Cost"],
            "score": 20,
            "cost": 15,
            "description": "Increase all spell stats (and cost)"
          },
          {
            "id": "minus_cost",
            "word": "weaken",
            "elements": [],
            "modifiers": ["-Cost"],
            "score": 5,
            "cost": 2,
            "description": "Decrease all spell stats (and cost)"
          },
          {
            "id": "target_self",
            "word": "self",
            "elements": [],
            "modifiers": ["Target:Self"],
            "score": 5,
            "cost": 0,
            "description": "Target the caster"
          },
          {
            "id": "target_block",
            "word": "block",
            "elements": [],
            "modifiers": ["Target:Block"],
            "score": 5,
            "cost": 0,
            "description": "Target the looked-at block"
          },
          {
            "id": "target_entity",
            "word": "foe",
            "elements": [],
            "modifiers": ["Target:Entity"],
            "score": 5,
            "cost": 0,
            "description": "Target a nearby entity"
          },
          {
            "id": "range_projectile",
            "word": "bolt",
            "elements": [],
            "modifiers": ["Range:Projectile"],
            "score": 10,
            "cost": 5,
            "description": "Cast as a projectile"
          },
          {
            "id": "range_area",
            "word": "nova",
            "elements": [],
            "modifiers": ["Range:Area"],
            "score": 15,
            "cost": 12,
            "description": "Area effect around caster"
          }
        ]
        """;
        Files.writeString(starter, json);
        VoiceMagicMod.LOGGER.info("Wrote starter component file to {}", starter);
    }
}
