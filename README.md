# Voice Magic – Microphone Component Spell System

A NeoForge 1.21.1 magic mod where you **speak component words** to assemble and cast spells.

You edit a simple JSON file to add new words, change caust values, and define which elements / modifiers a word provides. The rest of the system (mana, levels, slots, effects) is already wired.

---

## Core Design (exactly as requested)

### Spell Components (editable)
Each component has:
- **Word** – the spoken trigger
- **Elements** – zero or more of: `Ice`, `Fire`, `Lightning`, `Earth`, `Light`, `Dark`
- **Modifiers** – zero or more of the list below
- **score** – recognition / power weight
- **caust** – baseline mana value / investment value for this component

### Supported Elements & Modifiers
**Ice** → Construct, Remove, Move, Trap  
**Fire** → Ignite, Spread, Move, Remove  
**Lightning** → Spear, +Charge, -Charge, Chain  
**Earth** → Construct, Remove, Move, Trap  

Also available as free-floating modifiers:
- `+Range` / `-Range`
- `+Charge` / `-Charge` (charges attract, never repel; player starts with natural **-Charge**)
- `Curve Left` / `Curve Right` / `Curve Up` / `Curve Down`
- `Light` (extra damage to undead, heals non-undead)
- `Dark` (damage)
- `+Caust` / `-Caust` (change invested mana and therefore potency)
- `Target:Self` / `Target:Block` / `Target:Entity`
- `Range:Projectile` / `Range:Area`

### Caust and spell potency

- **Base Caust** is the normal mana requirement of the spell, calculated from its non-Caust-modifier components.
- **Invested Caust** is the mana actually spent. `+Caust` components add their configured `caust` value; `-Caust` components subtract it.
- **Potency** is `Invested Caust / Base Caust`. Continuous effects (such as damage, healing, and duration) scale with potency. Discrete block effects scale in whole units (2× potency affects two blocks).
- The caster must have at least the invested Caust available. A spell cannot spend more mana than the caster currently has.

A component JSON entry uses `caust`, for example:

```json
{
  "id": "extra_mana",
  "word": "surge",
  "elements": [],
  "modifiers": ["+Caust"],
  "score": 8,
  "caust": 10,
  "description": "Invest 10 additional mana"
}
```

## Caster Progression
| Level | Max Mana | Slots                                      |
|-------|----------|--------------------------------------------|
| 1     | 100      | Range/Target, Element, Mod, Mod            |
| 2     | 200      | Range/Target, Element, Mod, Mod, Mod       |
| 3     | 400      | Range/Target, Range/Target*, Element, Mod, Mod, Mod |
| 4+    | +100 each| same as level 3                            |

- Level up by successfully casting **100** spells.
- The second Range/Target slot at level 3+ is optional.

---

## How to use (testing right now)

1. Build the mod with `gradle build` from this project folder (requires Java 21 and Gradle; see the build note below), then put `build/libs/voicemagic-1.0.0.jar` in your mods folder.
2. In-game commands:
   - `/voicemagic list` – show every loaded component word
   - `/voicemagic cast frost bolt burn` – speak a sequence of words (spaces separate components)
   - `/voicemagic status` – see your level, mana, progress
   - `/voicemagic reload` – reload the JSON files without restarting (op)
   - `/voicemagic setlevel <n>` – force a level (op)

Example:
```
/voicemagic cast flame bolt burn
/voicemagic cast frost nova bind
/voicemagic cast spark chain charge
/voicemagic cast stone build self
```

---

## Editing components

After first launch the mod creates:

```
config/voicemagic/spell_components/starter_components.json
```

Edit that file (or add more `.json` files in the same folder).  
Each entry looks like:

```json
{
  "id": "my_fireball",
  "word": "inferno",
  "elements": ["Fire"],
  "modifiers": ["Ignite", "Spread", "Range:Projectile"],
  "score": 25,
  "caust": 22,
  "description": "Big fire projectile that spreads"
}
```

Then run `/voicemagic reload` (or restart).

---

## Project structure

```
src/main/java/com/voicemagic/
├── VoiceMagicMod.java
├── capability/
│   ├── CasterData.java          ← mana, level, slots
│   └── CasterDataAttachment.java
├── component/
│   ├── SpellComponent.java
│   └── ComponentRegistry.java   ← loads the editable JSON
├── spell/
│   ├── SpellBlueprint.java      ← assembled spell
│   └── SpellExecutor.java       ← turns blueprint into world effects
├── event/ModEvents.java         ← commands + mana regen
├── client/VoiceInputHandler.java
└── network/ModNetworking.java
```

---

## Next steps for you

1. Play with the starter words and see how combinations feel.
2. Add / rename words and rebalance Caust values and scores in the JSON.
3. Expand `SpellExecutor` with better particles, real projectiles, block constructs, etc.
4. Wire a real speech recognizer when you are happy with the component set.

Enjoy building the language of magic.

---

## Voice casting (always listening, via Simple Voice Chat)

Requires **Simple Voice Chat 2.6.x for NeoForge 1.21.1** on the client and server. Since you already have Simple Voice Chat installed, you do not need to install it again; just make sure it is the NeoForge 1.21.1 build and is present on the server too when playing multiplayer.

1. Install Simple Voice Chat and set your microphone up in its settings (press `V` in-game).
   Set the input mode to **Voice activation**, not push-to-talk, for hands-free casting.
2. Download `vosk-model-small-en-us-0.15` from https://alphacephei.com/vosk/models
3. Unzip it so the folder `config/voicemagic/model/` contains the folders `am`, `conf`, `graph`, ...
   (a single extra sub-folder level is also accepted).
4. Start the game. `/voicemagic voice` shows: listening, Simple Voice Chat connection, speech engine state.
5. Say at least **two** component words in a row, e.g. "flame bolt". Single stray words are ignored.

Commands: `/voicemagic voice on|off|status`


## Build note

This source project targets Minecraft 1.21.1 / NeoForge 21.1.92 and Java 21. The source archive does not include a compiled mod jar. Run `gradle build` in this folder on a machine with internet access so Gradle can download the NeoForge and Vosk dependencies. The output jar will be in `build/libs/`.
