package com.norboc.serverfixtweaks;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(ServerFixTweaks.MOD_ID)
public final class ServerFixTweaks {
    public static final String MOD_ID = "serverfixtweaks";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public ServerFixTweaks(IEventBus modEventBus, ModContainer modContainer) {
        LOGGER.info("ServerFix Tweaks loaded");
    }
}
