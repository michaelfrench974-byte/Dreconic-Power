package com.voicemagic;

import com.voicemagic.capability.CasterDataAttachment;
import com.voicemagic.component.ComponentRegistry;
import com.voicemagic.event.ModEvents;
import com.voicemagic.network.ModNetworking;
import com.voicemagic.spell.SpellExecutor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(VoiceMagicMod.MODID)
public class VoiceMagicMod {
    public static final String MODID = "voicemagic";
    public static final Logger LOGGER = LoggerFactory.getLogger(MODID);

    public VoiceMagicMod(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        CasterDataAttachment.ATTACHMENT_TYPES.register(modEventBus);
        ModNetworking.register(modEventBus);
        NeoForge.EVENT_BUS.register(ModEvents.class);
        NeoForge.EVENT_BUS.register(CasterDataAttachment.class);

        LOGGER.info("Voice Magic loaded. Speak the components.");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ComponentRegistry.loadComponents();
            SpellExecutor.init();
            LOGGER.info("Loaded {} spell components.", ComponentRegistry.getAll().size());
        });
    }
}
