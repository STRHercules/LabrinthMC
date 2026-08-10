package com.labrinthmc.labrinth.world.region;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Immutable visual, content, and generation rules for one Labrinth region. */
public record RegionDefinition(
        ResourceLocation id,
        int weight,
        Set<ResourceLocation> roomPool,
        Set<ResourceLocation> corridorPool,
        Palette palette,
        LightingRules lightingRules,
        DecorationRules decorationRules,
        MobRules mobRules,
        LootModifiers lootModifiers,
        AmbientProperties ambientProperties,
        GenerationConditions generationConditions) {
    public RegionDefinition {
        Objects.requireNonNull(id, "id");
        if (weight < 0) {
            throw new IllegalArgumentException("region weight must not be negative");
        }
        roomPool = Set.copyOf(Objects.requireNonNull(roomPool, "roomPool"));
        corridorPool = Set.copyOf(Objects.requireNonNull(corridorPool, "corridorPool"));
        Objects.requireNonNull(palette, "palette");
        Objects.requireNonNull(lightingRules, "lightingRules");
        Objects.requireNonNull(decorationRules, "decorationRules");
        Objects.requireNonNull(mobRules, "mobRules");
        Objects.requireNonNull(lootModifiers, "lootModifiers");
        Objects.requireNonNull(ambientProperties, "ambientProperties");
        Objects.requireNonNull(generationConditions, "generationConditions");
        if (roomPool.isEmpty() || corridorPool.isEmpty()) {
            throw new IllegalArgumentException("regions require room and corridor pools");
        }
    }

    public boolean eligible(int depth, int floorIndex) {
        return generationConditions.eligible(depth, floorIndex);
    }

    public boolean allowsRoom(ResourceLocation roomId) {
        return roomPool.contains(Objects.requireNonNull(roomId, "roomId"));
    }

    public boolean allowsCorridor(ResourceLocation corridorId) {
        return corridorPool.contains(Objects.requireNonNull(corridorId, "corridorId"));
    }

    /** Apply the region palette without changing non-structural room props. */
    public BlockState paletteState(
            BlockState base,
            int localY,
            int pieceHeight,
            int localX,
            int localZ) {
        return palette.apply(base, localY, pieceHeight, localX, localZ, lightingRules);
    }

    /** Add only deterministic, bounded decoration to an otherwise empty cell. */
    public BlockState decorationState(
            BlockState base,
            int localX,
            int localY,
            int localZ,
            int pieceHeight,
            long worldX,
            long worldY,
            long worldZ) {
        return decorationRules.apply(
                base,
                localX,
                localY,
                localZ,
                pieceHeight,
                worldX,
                worldY,
                worldZ,
                block(palette.accentId()));
    }

    public record Palette(
            ResourceLocation floorId,
            ResourceLocation wallId,
            ResourceLocation ceilingId,
            ResourceLocation lightId,
            ResourceLocation accentId) {
        public Palette {
            Objects.requireNonNull(floorId, "floorId");
            Objects.requireNonNull(wallId, "wallId");
            Objects.requireNonNull(ceilingId, "ceilingId");
            Objects.requireNonNull(lightId, "lightId");
            Objects.requireNonNull(accentId, "accentId");
        }

        private BlockState apply(
                BlockState base,
                int localY,
                int pieceHeight,
                int localX,
                int localZ,
                LightingRules lighting) {
            Objects.requireNonNull(base, "base");
            if (base.isAir()) {
                return base;
            }
            if (isLight(base)) {
                // Lanterns are non-solid and can be attached to a ceiling or
                // floor. A generated light must occupy the same full block
                // volume on every side of a connector, so use a solid light
                // block for all palette lights and outage states alike.
                return Blocks.GLOWSTONE.defaultBlockState();
            }
            if (base.is(Blocks.DEEPSLATE_BRICKS) || base.is(Blocks.DEEPSLATE_BRICK_WALL)) {
                return block(wallId).defaultBlockState();
            }
            if (localY == 0 && isStructural(base)) {
                return block(floorId).defaultBlockState();
            }
            if (localY == pieceHeight - 1 && isStructural(base)) {
                return block(ceilingId).defaultBlockState();
            }
            return base;
        }

        private static boolean isLight(BlockState state) {
            return state.is(Blocks.SEA_LANTERN)
                    || state.is(Blocks.GLOWSTONE)
                    || state.is(Blocks.LANTERN)
                    || state.is(Blocks.SOUL_LANTERN)
                    || state.is(Blocks.END_ROD);
        }

        private static boolean isStructural(BlockState state) {
            return state.is(Blocks.POLISHED_DEEPSLATE)
                    || state.is(Blocks.DEEPSLATE_TILES)
                    || state.is(Blocks.POLISHED_BLACKSTONE)
                    || state.is(Blocks.MOSSY_COBBLESTONE)
                    || state.is(Blocks.STONE_BRICKS)
                    || state.is(Blocks.PRISMARINE_BRICKS)
                    || state.is(Blocks.PURPLE_CONCRETE)
                    || state.is(Blocks.OBSIDIAN)
                    || state.is(Blocks.CRYING_OBSIDIAN);
        }

        private static boolean outage(int x, int y, int z, int percent) {
            long value = x * 31L + y * 17L + z * 13L;
            return Math.floorMod(value, 100L) < percent;
        }
    }

    public record LightingRules(boolean enabled, int outagePercent) {
        public LightingRules {
            if (outagePercent < 0 || outagePercent > 100) {
                throw new IllegalArgumentException("outagePercent must be between 0 and 100");
            }
        }
    }

    public record DecorationRules(
            boolean debris,
            boolean pipes,
            boolean vegetation,
            boolean vines,
            boolean waterlogged,
            boolean alternateGeometry,
            int densityPercent,
            ResourceLocation debrisBlockId,
            ResourceLocation pipeBlockId,
            ResourceLocation vegetationBlockId) {
        public DecorationRules {
            if (densityPercent < 0 || densityPercent > 100) {
                throw new IllegalArgumentException("densityPercent must be between 0 and 100");
            }
            Objects.requireNonNull(debrisBlockId, "debrisBlockId");
            Objects.requireNonNull(pipeBlockId, "pipeBlockId");
            Objects.requireNonNull(vegetationBlockId, "vegetationBlockId");
        }

        public boolean enabled() {
            return densityPercent > 0
                    && (debris || pipes || vegetation || vines || waterlogged || alternateGeometry);
        }

        private BlockState apply(
                BlockState base,
                int localX,
                int localY,
                int localZ,
                int pieceHeight,
                long worldX,
                long worldY,
                long worldZ,
                Block accent) {
            if (!base.isAir() || densityPercent == 0 || localY <= 0 || localY >= pieceHeight - 1) {
                return base;
            }
            long value = worldX * 31L ^ worldY * 17L ^ worldZ * 13L;
            // Generic decoration is a sparse dressing pass, not a second
            // terrain generator. Full blocks in every empty cell made rooms
            // and corridors impassable; authored pieces can still place dense
            // vegetation, water, or debris explicitly.
            int sparseDensity = Math.max(1, densityPercent / 6);
            if (Math.floorMod(value, 100L) >= sparseDensity) {
                return base;
            }
            if (pipes && localY == pieceHeight - 2
                    && (localX == 1 || localZ == 1)) {
                return Blocks.CHAIN.defaultBlockState();
            }
            if (vines && localY == 2 && (localX == 1 || localZ == 1)) {
                return Blocks.VINE.defaultBlockState();
            }
            if (vegetation && localY == 1) {
                return Blocks.MOSS_CARPET.defaultBlockState();
            }
            if (debris && localY == 1) {
                return Blocks.COBBLESTONE_SLAB.defaultBlockState();
            }
            return alternateGeometry && localY == 1
                    ? Blocks.POLISHED_BLACKSTONE_SLAB.defaultBlockState()
                    : base;
        }
    }

    private static Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.get(id);
    }

    public record MobRules(boolean naturalSpawnsAllowed, int spawnWeight, Set<ResourceLocation> mobTags) {
        public MobRules {
            if (spawnWeight < 0) {
                throw new IllegalArgumentException("spawnWeight must not be negative");
            }
            mobTags = Set.copyOf(Objects.requireNonNull(mobTags, "mobTags"));
        }
    }

    public record LootModifiers(int rarityBonusPercent, Optional<ResourceLocation> lootTag) {
        public LootModifiers {
            if (rarityBonusPercent < 0) {
                throw new IllegalArgumentException("rarityBonusPercent must not be negative");
            }
            lootTag = Objects.requireNonNull(lootTag, "lootTag");
        }
    }

    public record AmbientProperties(float ambientLight, boolean fogged, Optional<ResourceLocation> ambientSound) {
        public AmbientProperties {
            if (ambientLight < 0.0F || ambientLight > 1.0F) {
                throw new IllegalArgumentException("ambientLight must be between 0 and 1");
            }
            ambientSound = Objects.requireNonNull(ambientSound, "ambientSound");
        }
    }

    public record GenerationConditions(int minDepth, int maxDepth, int minFloor, int maxFloor) {
        public GenerationConditions {
            if (minDepth < 0 || maxDepth < minDepth) {
                throw new IllegalArgumentException("invalid region depth range");
            }
            if (!com.labrinthmc.labrinth.world.generation.VerticalCatalog.isValidFloor(minFloor)
                    || !com.labrinthmc.labrinth.world.generation.VerticalCatalog.isValidFloor(maxFloor)
                    || maxFloor < minFloor) {
                throw new IllegalArgumentException("invalid region floor range");
            }
        }

        public boolean eligible(int depth, int floorIndex) {
            return depth >= minDepth
                    && depth <= maxDepth
                    && floorIndex >= minFloor
                    && floorIndex <= maxFloor;
        }
    }
}
