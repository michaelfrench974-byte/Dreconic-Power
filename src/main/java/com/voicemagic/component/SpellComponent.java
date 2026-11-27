package com.voicemagic.component;

import java.util.List;

/**
 * One editable spell component.
 * Edit the JSON files in data/voicemagic/spell_components/ to add or change these.
 *
 * Fields:
 * - word          : spoken trigger (case-insensitive match)
 * - elements      : list of base elements this word provides (Ice, Fire, Lightning, Earth, Light, Dark)
 * - modifiers     : list of modifiers this word provides
 * - score         : how much this contributes to spell power / recognition priority
 * - cost          : mana cost added when this component is used
 * - description   : optional tooltip text
 */
public class SpellComponent {
    public final String id;
    public final String word;
    public final List<String> elements;
    public final List<String> modifiers;
    public final int score;
    public final int cost;
    public final String description;

    public SpellComponent(String id, String word, List<String> elements, List<String> modifiers,
                          int score, int cost, String description) {
        this.id = id;
        this.word = word.toLowerCase().trim();
        this.elements = elements != null ? elements : List.of();
        this.modifiers = modifiers != null ? modifiers : List.of();
        this.score = score;
        this.cost = cost;
        this.description = description != null ? description : "";
    }

    public boolean matches(String spoken) {
        return word.equalsIgnoreCase(spoken.trim());
    }
}
