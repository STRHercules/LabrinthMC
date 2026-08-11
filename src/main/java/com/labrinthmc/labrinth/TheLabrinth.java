package com.labrinthmc.labrinth;

import com.mojang.logging.LogUtils;
import com.labrinthmc.labrinth.config.LabrinthConfig;
import com.labrinthmc.labrinth.event.LabrinthEvents;
import com.labrinthmc.labrinth.registry.ModBlocks;
import com.labrinthmc.labrinth.registry.ModChunkGenerators;
import com.labrinthmc.labrinth.registry.ModItems;
import com.labrinthmc.labrinth.world.landmark.LandmarkCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.room.RoomCatalog;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

@Mod(TheLabrinth.MOD_ID)
public final class TheLabrinth {
    public static final String MOD_ID = "labrinth";
    public static final ResourceLocation LABRINTH_DIMENSION =
            ResourceLocation.fromNamespaceAndPath(MOD_ID, "labrinth");
    private static final Logger LOGGER = LogUtils.getLogger();

    public TheLabrinth(IEventBus modEventBus, ModContainer modContainer) {
        modContainer.registerConfig(ModConfig.Type.COMMON, LabrinthConfig.SPEC);
        NeoForge.EVENT_BUS.register(LabrinthEvents.class);
        NeoForge.EVENT_BUS.register(com.labrinthmc.labrinth.command.LabrinthCommands.class);

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
