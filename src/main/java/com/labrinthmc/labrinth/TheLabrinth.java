package com.labrinthmc.labrinth;

import com.mojang.logging.LogUtils;
import com.labrinthmc.labrinth.registry.ModBlocks;
import com.labrinthmc.labrinth.registry.ModChunkGenerators;
import com.labrinthmc.labrinth.registry.ModItems;
import com.labrinthmc.labrinth.world.landmark.LandmarkCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.room.RoomCatalog;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TheLabrinth.MOD_ID)
public final class TheLabrinth {
    public static final String MOD_ID = "labrinth";
    private static final Logger LOGGER = LogUtils.getLogger();

    public TheLabrinth(IEventBus modEventBus) {
        // Keep registry wiring on the common mod bus; client renderers belong under the client package.
        ModBlocks.register(modEventBus);
        ModChunkGenerators.register(modEventBus);
        ModItems.register(modEventBus);
        LOGGER.info("The Labrinth foundation initialized with {} room, {} region, and {} landmark definitions.",
                RoomCatalog.definitions().size(),
                RegionCatalog.definitions().size(),
                LandmarkCatalog.definitions().size());
    }
}
