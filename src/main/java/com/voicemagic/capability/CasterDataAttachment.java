package com.voicemagic.capability;

import com.voicemagic.VoiceMagicMod;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class CasterDataAttachment {
    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, VoiceMagicMod.MODID);

    public static final Supplier<AttachmentType<CasterData>> CASTER_DATA =
            ATTACHMENT_TYPES.register("caster_data", () ->
                    AttachmentType.serializable(CasterData::new)
                            .copyOnDeath()
                            .build());

    // Keep data on respawn / dimension change
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath()) {
            CasterData oldData = event.getOriginal().getData(CASTER_DATA);
            CasterData newData = event.getEntity().getData(CASTER_DATA);
            newData.setLevel(oldData.getLevel());
        }
    }
}
