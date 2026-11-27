package com.voicemagic.spell;

import com.voicemagic.component.SpellComponent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A fully assembled spell built from spoken components.
 * The player speaks a sequence of words; each matched component fills a slot.
 */
public class SpellBlueprint {
    public final List<SpellComponent> components = new ArrayList<>();

    // Resolved after parsing
    public final Set<String> elements = new HashSet<>();
    public final Set<String> modifiers = new HashSet<>();
    public String primaryTarget = "Target:Entity"; // default
    public String rangeType = "Range:Projectile";  // default
    public int totalCost = 0;
    public int totalScore = 0;
    public float powerMultiplier = 1.0f;
    public float rangeMultiplier = 1.0f;
    public int charge = 0; // net +Charge / -Charge

    public void add(SpellComponent comp) {
        components.add(comp);
        elements.addAll(comp.elements);
        modifiers.addAll(comp.modifiers);
        totalCost += comp.cost;
        totalScore += comp.score;
    }

    public void finalizeStats() {
        // Apply +Cost / -Cost modifiers
        if (modifiers.contains("+Cost")) {
            powerMultiplier *= 1.5f;
            totalCost = (int)(totalCost * 1.4f);
        }
        if (modifiers.contains("-Cost")) {
            powerMultiplier *= 0.7f;
            totalCost = Math.max(1, (int)(totalCost * 0.6f));
        }

        // Range modifiers
        if (modifiers.contains("+Range")) rangeMultiplier *= 1.5f;
        if (modifiers.contains("-Range")) rangeMultiplier *= 0.6f;

        // Charge
        if (modifiers.contains("+Charge")) charge += 1;
        if (modifiers.contains("-Charge")) charge -= 1;

        // Target / Range type (last one wins, or first)
        for (String m : modifiers) {
            if (m.startsWith("Target:")) primaryTarget = m;
            if (m.startsWith("Range:")) rangeType = m;
        }
    }

    public boolean isValid() {
        // Must have at least one element
        return !elements.isEmpty();
    }

    public String describe() {
        return "Elements=" + elements +
               " Mods=" + modifiers +
               " Cost=" + totalCost +
               " Power×" + String.format("%.1f", powerMultiplier) +
               " Range×" + String.format("%.1f", rangeMultiplier) +
               " Charge=" + charge +
               " " + primaryTarget + " " + rangeType;
    }
}
