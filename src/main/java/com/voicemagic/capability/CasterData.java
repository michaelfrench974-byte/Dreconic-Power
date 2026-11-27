package com.voicemagic.capability;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.util.INBTSerializable;

/**
 * Persistent data for every player.
 *
 * Level 1: 100 mana, 4 slots  (Range/Target, Element, Mod, Mod)
 * Level 2: 200 mana, 5 slots  (Range/Target, Element, Mod, Mod, Mod)
 * Level 3: 400 mana, 6 slots  (Range/Target, Range/Target*, Element, Mod, Mod, Mod)
 * Every level after 3: +100 max mana
 *
 * 100 successful casts → level up.
 */
public class CasterData implements INBTSerializable<CompoundTag> {
    private int level = 1;
    private int mana = 100;
    private int maxMana = 100;
    private int castsTowardNextLevel = 0;
    private int naturalCharge = -1; // player starts with negative charge

    public static final int CASTS_PER_LEVEL = 100;

    public int getLevel() { return level; }
    public int getMana() { return mana; }
    public int getMaxMana() { return maxMana; }
    public int getCastsTowardNextLevel() { return castsTowardNextLevel; }
    public int getNaturalCharge() { return naturalCharge; }

    public int getSlotCount() {
        return switch (level) {
            case 1 -> 4;
            case 2 -> 5;
            default -> 6; // level 3+
        };
    }

    /** Second Range/Target slot is optional at level 3+ */
    public boolean hasOptionalSecondRangeTarget() {
        return level >= 3;
    }

    public boolean trySpendMana(int amount) {
        if (mana >= amount) {
            mana -= amount;
            return true;
        }
        return false;
    }

    public void restoreMana(int amount) {
        mana = Math.min(maxMana, mana + amount);
    }

    public void fullRestore() {
        mana = maxMana;
    }

    /** Call after a successful cast. Returns true if the player leveled up. */
    public boolean onSuccessfulCast() {
        castsTowardNextLevel++;
        if (castsTowardNextLevel >= CASTS_PER_LEVEL) {
            castsTowardNextLevel = 0;
            levelUp();
            return true;
        }
        return false;
    }

    private void levelUp() {
        level++;
        if (level == 2) {
            maxMana = 200;
        } else if (level == 3) {
            maxMana = 400;
        } else {
            maxMana += 100;
        }
        mana = maxMana; // full heal on level up
    }

    public void setLevel(int newLevel) {
        this.level = Math.max(1, newLevel);
        recalculateMaxMana();
        mana = Math.min(mana, maxMana);
    }

    private void recalculateMaxMana() {
        if (level == 1) maxMana = 100;
        else if (level == 2) maxMana = 200;
        else maxMana = 400 + (level - 3) * 100;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Level", level);
        tag.putInt("Mana", mana);
        tag.putInt("MaxMana", maxMana);
        tag.putInt("Casts", castsTowardNextLevel);
        tag.putInt("Charge", naturalCharge);
        return tag;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag tag) {
        level = tag.getInt("Level");
        mana = tag.getInt("Mana");
        maxMana = tag.getInt("MaxMana");
        castsTowardNextLevel = tag.getInt("Casts");
        naturalCharge = tag.contains("Charge") ? tag.getInt("Charge") : -1;
        if (maxMana <= 0) recalculateMaxMana();
    }

    public static CasterData get(Player player) {
        return player.getData(CasterDataAttachment.CASTER_DATA);
    }
}
