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
    public int baseCaust = 0;
    public int investedCaust = 0;
    /** Compatibility field used by the casting pipeline; always equals investedCaust. */
    public int totalCost = 0;
    public int totalScore = 0;
    public float potency = 1.0f;
    public float powerMultiplier = 1.0f;
    public float rangeMultiplier = 1.0f;
    public int charge = 0; // net +Charge / -Charge

    public void add(SpellComponent comp) {
        components.add(comp);
        elements.addAll(comp.elements);
        modifiers.addAll(comp.modifiers);
        boolean investmentModifier = comp.modifiers.contains("+Caust") || comp.modifiers.contains("-Caust");
        if (!investmentModifier) baseCaust += Math.max(0, comp.caust);
        totalScore += comp.score;
    }

    public void finalizeStats() {
        // Base Caust is the normal mana requirement. Caust modifiers alter the
        // amount invested without changing that baseline.
        int caustAdjustment = 0;
        for (SpellComponent comp : components) {
            if (comp.modifiers.contains("+Caust")) caustAdjustment += Math.max(0, comp.caust);
            if (comp.modifiers.contains("-Caust")) caustAdjustment -= Math.max(0, comp.caust);
        }
        baseCaust = Math.max(1, baseCaust);
        investedCaust = Math.max(1, baseCaust + caustAdjustment);
        potency = Math.max(0.1f, (float) investedCaust / (float) baseCaust);
        totalCost = investedCaust;
        powerMultiplier *= potency;

        // Range modifiers
        if (modifiers.contains("+Range")) rangeMultiplier *= 1.5f;
        if (modifiers.contains("-Range")) rangeMultiplier *= 0.6f;

        // Charge
        if (modifiers.contains("+Charge")) charge += 1;
        if (modifiers.contains("-Charge")) charge -= 1;

        // Target / Range type (last one wins)
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
               " BaseCaust=" + baseCaust +
               " InvestedCaust=" + investedCaust +
               " Potency×" + String.format("%.2f", potency) +
               " Power×" + String.format("%.1f", powerMultiplier) +
               " Range×" + String.format("%.1f", rangeMultiplier) +
               " Charge=" + charge +
               " " + primaryTarget + " " + rangeType;
    }
}
