package com.norboc.serverfixtweaks;

import com.norboc.serverfixtweaks.config.ServerFixTweaksConfig;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ServerFixTweaks.MOD_ID)
public final class ServerFixTweaks {
    public static final String MOD_ID = "serverfixtweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ServerFixTweaks(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, ServerFixTweaksConfig.SPEC);
        LOGGER.info("ServerFix Tweaks loaded");
    }
}
