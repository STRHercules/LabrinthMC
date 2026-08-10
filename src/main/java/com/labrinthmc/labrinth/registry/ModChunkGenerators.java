package com.labrinthmc.labrinth.registry;

import com.labrinthmc.labrinth.TheLabrinth;
import com.labrinthmc.labrinth.world.generation.LabrinthChunkGenerator;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Common registration for Labrinth-owned chunk-generator codecs. */
public final class ModChunkGenerators {
    public static final DeferredRegister<MapCodec<? extends ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(BuiltInRegistries.CHUNK_GENERATOR, TheLabrinth.MOD_ID);

    static {
        CHUNK_GENERATORS.register("labrinth", () -> LabrinthChunkGenerator.CODEC);
    }

    private ModChunkGenerators() {
    }

    public static void register(IEventBus modEventBus) {
        CHUNK_GENERATORS.register(modEventBus);
    }
}
