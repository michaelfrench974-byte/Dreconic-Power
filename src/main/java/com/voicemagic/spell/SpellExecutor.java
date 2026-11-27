package com.voicemagic.spell;

import com.voicemagic.VoiceMagicMod;
import com.voicemagic.capability.CasterData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Turns a finished SpellBlueprint into actual world effects.
 * This is the place you expand with real particle systems, projectiles, etc.
 */
public class SpellExecutor {

    public static void init() {
        // Register any custom entities / particles later
    }

    public static boolean tryCast(ServerPlayer player, SpellBlueprint spell) {
        CasterData data = CasterData.get(player);

        if (!spell.isValid()) {
            player.sendSystemMessage(Component.literal("§cSpell needs at least one Element."));
            return false;
        }

        if (!data.trySpendMana(spell.totalCost)) {
            player.sendSystemMessage(Component.literal("§cNot enough mana! (" + data.getMana() + "/" + data.getMaxMana() + ")"));
            return false;
        }

        // Apply the spell
        applySpell(player, spell);

        boolean leveled = data.onSuccessfulCast();
        if (leveled) {
            player.sendSystemMessage(Component.literal("§a§lLevel Up! You are now level " + data.getLevel() +
                    "  (" + data.getMaxMana() + " mana, " + data.getSlotCount() + " slots)"));
        } else {
            player.sendSystemMessage(Component.literal("§bSpell cast! Mana: " + data.getMana() + "/" + data.getMaxMana() +
                    "  Progress: " + data.getCastsTowardNextLevel() + "/" + CasterData.CASTS_PER_LEVEL));
        }

        VoiceMagicMod.LOGGER.info("{} cast: {}", player.getName().getString(), spell.describe());
        return true;
    }

    private static void applySpell(ServerPlayer player, SpellBlueprint spell) {
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        float power = 4.0f * spell.powerMultiplier;
        double range = 12.0 * spell.rangeMultiplier;

        // Determine targets based on range type + target type
        if (spell.rangeType.equals("Range:Area")) {
            applyArea(player, spell, power, range);
        } else {
            // Projectile style – currently raycast for simplicity
            HitResult hit = player.pick(range, 0, false);
            applyTargeted(player, spell, power, hit);
        }
    }

    private static void applyArea(ServerPlayer player, SpellBlueprint spell, float power, double range) {
        AABB box = player.getBoundingBox().inflate(range);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != player && e.isAlive());

        for (LivingEntity target : entities) {
            applyElementEffects(player, target, null, spell, power);
        }

        // Block effects in radius (simple)
        BlockPos center = player.blockPosition();
        int r = (int) Math.ceil(range / 2);
        for (int x = -r; x <= r; x++) {
            for (int y = -1; y <= 2; y++) {
                for (int z = -r; z <= r; z++) {
                    if (x * x + z * z <= r * r) {
                        BlockPos pos = center.offset(x, y, z);
                        applyBlockEffects(player, pos, spell, power);
                    }
                }
            }
        }
    }

    private static void applyTargeted(ServerPlayer player, SpellBlueprint spell, float power, HitResult hit) {
        if (spell.primaryTarget.equals("Target:Self")) {
            applyElementEffects(player, player, null, spell, power);
            return;
        }

        if (hit.getType() == HitResult.Type.ENTITY) {
            EntityHitResult ehr = (EntityHitResult) hit;
            if (ehr.getEntity() instanceof LivingEntity living) {
                applyElementEffects(player, living, null, spell, power);
            }
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            BlockHitResult bhr = (BlockHitResult) hit;
            applyBlockEffects(player, bhr.getBlockPos(), spell, power);
            // Also try to affect entities near the block
            AABB near = new AABB(bhr.getBlockPos()).inflate(1.5);
            for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, near)) {
                if (e != player) applyElementEffects(player, e, bhr.getBlockPos(), spell, power * 0.6f);
            }
        }
    }

    private static void applyElementEffects(ServerPlayer caster, LivingEntity target, BlockPos blockPos,
                                            SpellBlueprint spell, float power) {
        for (String element : spell.elements) {
            switch (element) {
                case "Fire" -> {
                    if (spell.modifiers.contains("Ignite") || spell.modifiers.contains("Spread")) {
                        target.igniteForSeconds((int)(power * 1.5f));
                    }
                    target.hurt(caster.damageSources().onFire(), power);
                }
                case "Ice" -> {
                    target.setTicksFrozen((int)(power * 40));
                    target.hurt(caster.damageSources().freeze(), power * 0.7f);
                    if (spell.modifiers.contains("Trap")) {
                        // simple slow
                        target.setDeltaMovement(target.getDeltaMovement().multiply(0.2, 0.5, 0.2));
                    }
                }
                case "Lightning" -> {
                    target.hurt(caster.damageSources().lightningBolt(), power * 1.2f);
                    if (spell.modifiers.contains("Chain")) {
                        // simple chain: damage nearby
                        AABB box = target.getBoundingBox().inflate(4);
                        for (LivingEntity other : target.level().getEntitiesOfClass(LivingEntity.class, box)) {
                            if (other != target && other != caster) {
                                other.hurt(caster.damageSources().lightningBolt(), power * 0.5f);
                            }
                        }
                    }
                }
                case "Earth" -> {
                    target.hurt(caster.damageSources().fallingBlock(target), power);
                    if (spell.modifiers.contains("Move")) {
                        Vec3 away = target.position().subtract(caster.position()).normalize().scale(1.2 * power / 4);
                        target.setDeltaMovement(target.getDeltaMovement().add(away));
                    }
                }
                case "Light" -> {
                    boolean undead = target.getType().getCategory().isFriendly() == false &&
                            (target instanceof Monster || target.getType().toString().toLowerCase().contains("undead") ||
                             target.getType().toString().toLowerCase().contains("zombie") ||
                             target.getType().toString().toLowerCase().contains("skeleton"));
                    if (undead) {
                        target.hurt(caster.damageSources().magic(), power * 1.8f);
                    } else {
                        target.heal(power * 0.8f);
                    }
                }
                case "Dark" -> {
                    target.hurt(caster.damageSources().magic(), power * 1.1f);
                }
            }
        }

        // Generic Move / Remove / Construct handled lightly
        if (spell.modifiers.contains("Remove") && target != caster) {
            // extra damage
            target.hurt(caster.damageSources().magic(), power * 0.4f);
        }
    }

    private static void applyBlockEffects(ServerPlayer caster, BlockPos pos, SpellBlueprint spell, float power) {
        var level = caster.level();
        int units = Math.max(1, (int)Math.floor(spell.potency + 1.0e-4f));

        // Discrete block effects scale in whole units: 1x affects one block,
        // 2x affects two, etc. The line is deliberately short and horizontal
        // to make the extra affected blocks predictable while testing.
        for (int i = 0; i < units; i++) {
            BlockPos affected = pos.east(i);
            var state = level.getBlockState(affected);

            if (spell.elements.contains("Fire") && (spell.modifiers.contains("Ignite") || spell.modifiers.contains("Spread"))) {
                if (level.getBlockState(affected.above()).isAir() && state.isSolid()) {
                    level.setBlock(affected.above(), Blocks.FIRE.defaultBlockState(), 3);
                }
            }

            if (spell.elements.contains("Ice") && spell.modifiers.contains("Construct")) {
                if (level.getBlockState(affected.above()).isAir()) {
                    level.setBlock(affected.above(), Blocks.PACKED_ICE.defaultBlockState(), 3);
                }
            }

            if (spell.elements.contains("Earth") && spell.modifiers.contains("Construct")) {
                if (level.getBlockState(affected.above()).isAir()) {
                    level.setBlock(affected.above(), Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
            }

            if (spell.modifiers.contains("Remove")) {
                if (!state.isAir() && state.getDestroySpeed(level, affected) >= 0 && state.getDestroySpeed(level, affected) < 50) {
                    level.destroyBlock(affected, true, caster);
                }
            }
        }
    }

}
