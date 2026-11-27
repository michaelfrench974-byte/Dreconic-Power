# Voice Magic – Microphone Component Spell System

A NeoForge 1.21.1 magic mod where you **speak component words** to assemble and cast spells.

You edit a simple JSON file to add new words, change costs, and define which elements / modifiers a word provides. The rest of the system (mana, levels, slots, effects) is already wired.

---

## Core Design (exactly as requested)

### Spell Components (editable)
Each component has:
- **Word** – the spoken trigger
- **Elements** – zero or more of: `Ice`, `Fire`, `Lightning`, `Earth`, `Light`, `Dark`
- **Modifiers** – zero or more of the list below
- **score** – recognition / power weight
- **cost** – mana cost added by this component

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
- `+Cost` (increases all stats of the spell) / `-Cost` (decreases all stats)
- `Target:Self` / `Target:Block` / `Target:Entity`
- `Range:Projectile` / `Range:Area`

### Caster Progression
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

1. Build the mod (`./gradlew build`) and put the jar in your mods folder.
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
  "cost": 22,
  "description": "Big fire projectile that spreads"
}
```

Then run `/voicemagic reload` (or restart).

---

## Adding real microphone support

The current client key (`V`) is a placeholder.  
To make it actually listen to your mic:

1. Add an offline speech library (recommended: **Vosk** – already used by several existing voice-cast mods).
2. While the cast key is held, capture audio → run recognition → get a string.
3. Send that string to the server (a simple network packet) and call:

```java
ModEvents.handleSpoken(serverPlayer, recognizedText);
```

The whole spell assembly + mana + effect pipeline is already ready to receive that string.

Popular reference mods that already do offline mic → text:
- Iron’s Spells: Incantation
- VoiceChat Spells
- Microphone Text Input / Speech-to-Text

You can either depend on one of them or embed Vosk yourself.

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
2. Add / rename words and rebalance costs & scores in the JSON.
3. Expand `SpellExecutor` with better particles, real projectiles, block constructs, etc.
4. Wire a real speech recognizer when you are happy with the component set.

Enjoy building the language of magic.
