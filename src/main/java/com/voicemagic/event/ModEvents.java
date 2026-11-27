package com.voicemagic.event;

import com.voicemagic.VoiceMagicMod;
import com.voicemagic.capability.CasterData;
import com.voicemagic.capability.CasterDataAttachment;
import com.voicemagic.component.ComponentRegistry;
import com.voicemagic.component.SpellComponent;
import com.voicemagic.spell.SpellBlueprint;
import com.voicemagic.spell.SpellExecutor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import com.voicemagic.voice.VoiceManager;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class ModEvents {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("voicemagic")
                .then(Commands.literal("cast")
                    .then(Commands.argument("words", StringArgumentType.greedyString())
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            String spoken = StringArgumentType.getString(ctx, "words");
                            return handleSpoken(player, spoken);
                        })))
                .then(Commands.literal("status")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        CasterData data = CasterData.get(player);
                        player.sendSystemMessage(Component.literal(
                            "§6Caster Level: " + data.getLevel() +
                            "\n§bMana: " + data.getMana() + " / " + data.getMaxMana() +
                            "\n§eSlots: " + data.getSlotCount() +
                            "\n§aCasts to next level: " + data.getCastsTowardNextLevel() + "/" + CasterData.CASTS_PER_LEVEL +
                            "\n§7Natural Charge: " + data.getNaturalCharge()));
                        return 1;
                    }))
                .then(Commands.literal("reload")
                    .requires(s -> s.hasPermission(2))
                    .executes(ctx -> {
                        ComponentRegistry.loadComponents();
                        VoiceManager.onComponentsReloaded();
                        ctx.getSource().sendSuccess(() -> Component.literal("Reloaded " + ComponentRegistry.getAll().size() + " components."), true);
                        return 1;
                    }))
                .then(Commands.literal("setlevel")
                    .requires(s -> s.hasPermission(2))
                    .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            int lvl = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "level");
                            CasterData.get(player).setLevel(lvl);
                            player.sendSystemMessage(Component.literal("Set level to " + lvl));
                            return 1;
                        })))
                .then(Commands.literal("voice")
                    .then(Commands.literal("on").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        VoiceManager.setEnabled(player, true);
                        player.sendSystemMessage(Component.literal("§dVoice casting ON. Just speak your spell."));
                        return 1;
                    }))
                    .then(Commands.literal("off").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        VoiceManager.setEnabled(player, false);
                        player.sendSystemMessage(Component.literal("§dVoice casting OFF."));
                        return 1;
                    }))
                    .then(Commands.literal("status").executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§dVoice: " + VoiceManager.statusLine(player)));
                        return 1;
                    }))
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§dVoice: " + VoiceManager.statusLine(player)));
                        return 1;
                    }))
                .then(Commands.literal("list")
                    .executes(ctx -> {
                        ServerPlayer player = ctx.getSource().getPlayerOrException();
                        player.sendSystemMessage(Component.literal("§eAvailable component words:"));
                        for (SpellComponent c : ComponentRegistry.getAll()) {
                            player.sendSystemMessage(Component.literal("  §f" + c.word + " §7→ " + c.elements + " " + c.modifiers + " (Caust " + c.caust + ")"));
                        }
                        return 1;
                    }))
        );
    }

    /** Core entry point used by both the command and (later) the microphone listener */
    public static int handleSpoken(ServerPlayer player, String spokenLine) {
        return handleSpoken(player, spokenLine, false, 1);
    }

    /**
     * @param quiet     if true, say nothing when the line contains too few known words
     *                  (used for always-listening mode so normal chatter is ignored)
     * @param minWords  minimum number of recognised component words required to cast
     */
    public static int handleSpoken(ServerPlayer player, String spokenLine, boolean quiet, int minWords) {
        CasterData data = CasterData.get(player);
        int maxSlots = data.getSlotCount();

        // Split into words and try to match components
        String[] tokens = spokenLine.toLowerCase().trim().split("\\s+");
        SpellBlueprint blueprint = new SpellBlueprint();

        for (String token : tokens) {
            if (blueprint.components.size() >= maxSlots) break;
            Optional<SpellComponent> match = ComponentRegistry.findBestMatch(token);
            if (match.isPresent()) {
                blueprint.add(match.get());
            }
        }

        if (blueprint.components.size() < Math.max(1, minWords)) {
            if (!quiet) {
                player.sendSystemMessage(Component.literal("§cNo known spell components in: \"" + spokenLine + "\""));
            }
            return 0;
        }

        blueprint.finalizeStats();
        player.sendSystemMessage(Component.literal("§dAssembled: " + blueprint.describe()));

        boolean success = SpellExecutor.tryCast(player, blueprint);
        return success ? 1 : 0;
    }

    // Slow mana regen
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide) return;
        if (player.tickCount % 40 == 0) { // every 2 seconds
            CasterData data = CasterData.get(player);
            if (data.getMana() < data.getMaxMana()) {
                data.restoreMana(1 + data.getLevel() / 3);
            }
        }
    }
}
