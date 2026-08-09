package com.labrinthmc.labrinth;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TheLabrinth.MOD_ID)
public final class TheLabrinth {
    public static final String MOD_ID = "labrinth";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TheLabrinth(IEventBus modEventBus) {
        // Keep the foundation entry point common-only so dedicated servers load no client classes.
        LOGGER.info("The Labrinth foundation initialized.");
    }
}
