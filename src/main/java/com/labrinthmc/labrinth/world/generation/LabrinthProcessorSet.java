package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.region.RegionCatalog;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Labrinth-owned palette processors. They intentionally transform only
 * structural blocks; containers, beds, crops, doors, and mob-facing blocks
 * keep their native behavior.
 */
public enum LabrinthProcessorSet {
    STANDARD,
    ABANDONED,
    FLOODED,
    OVERGROWN,
    ANCIENT,
    CORRUPTED,
    NETHER_INFLUENCED;

    public static LabrinthProcessorSet forRegion(ResourceLocation region) {
        Objects.requireNonNull(region, "region");
        if (RegionCatalog.ABANDONED_ID.equals(region)) {
            return ABANDONED;
        }
        if (RegionCatalog.FLOODED_ID.equals(region)) {
            return FLOODED;
        }
        if (RegionCatalog.OVERGROWN_ID.equals(region)) {
            return OVERGROWN;
        }
        if (RegionCatalog.ANCIENT_ID.equals(region)) {
            return ANCIENT;
        }
        if (RegionCatalog.CORRUPTED_ID.equals(region)) {
            return CORRUPTED;
        }
        return STANDARD;
    }

    public BlockState apply(BlockState base, int localY, int pieceHeight, int localX, int localZ) {
        Objects.requireNonNull(base, "base");
        if (base.isAir() || !isStructural(base)) {
            return base;
        }
        return switch (this) {
            case STANDARD -> base;
            case ABANDONED -> abandoned(base);
            case FLOODED -> flooded(base, localY);
            case OVERGROWN -> overgrown(base, localY, localX, localZ);
            case ANCIENT -> ancient(base);
            case CORRUPTED -> corrupted(base, localY, localX, localZ);
            case NETHER_INFLUENCED -> nether(base);
        };
    }

    private static BlockState abandoned(BlockState base) {
        if (base.is(Blocks.STONE_BRICKS)) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (base.is(Blocks.DEEPSLATE_BRICKS)) {
            return Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
        }
        if (base.is(Blocks.DEEPSLATE_TILES)) {
            return Blocks.CRACKED_DEEPSLATE_TILES.defaultBlockState();
        }
        return base;
    }

    private static BlockState flooded(BlockState base, int localY) {
        return localY == 0 ? Blocks.PRISMARINE_BRICKS.defaultBlockState() : base;
    }

    private static BlockState overgrown(BlockState base, int localY, int localX, int localZ) {
        if (localY == 0) {
            return Blocks.MOSS_BLOCK.defaultBlockState();
        }
        if (Math.floorMod(localX * 7 + localZ * 11, 13) == 0) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        return base;
    }

    private static BlockState ancient(BlockState base) {
        if (base.is(Blocks.STONE_BRICKS)) {
            return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        }
        return base;
    }

    private static BlockState corrupted(BlockState base, int localY, int localX, int localZ) {
        return Math.floorMod(localX * 5 + localY * 3 + localZ * 7, 17) == 0
                ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                : base;
    }

    private static BlockState nether(BlockState base) {
        return base.is(Blocks.DEEPSLATE_BRICKS) || base.is(Blocks.STONE_BRICKS)
                ? Blocks.BLACKSTONE.defaultBlockState()
                : base;
    }

    private static boolean isStructural(BlockState state) {
        return state.is(Blocks.STONE)
                || state.is(Blocks.STONE_BRICKS)
                || state.is(Blocks.CRACKED_STONE_BRICKS)
                || state.is(Blocks.CHISELED_STONE_BRICKS)
                || state.is(Blocks.DEEPSLATE)
                || state.is(Blocks.DEEPSLATE_BRICKS)
                || state.is(Blocks.DEEPSLATE_TILES)
                || state.is(Blocks.POLISHED_DEEPSLATE)
                || state.is(Blocks.BLACKSTONE)
                || state.is(Blocks.POLISHED_BLACKSTONE)
                || state.is(Blocks.MOSSY_COBBLESTONE)
                || state.is(Blocks.TUFF)
                || state.is(Blocks.PRISMARINE_BRICKS)
                || state.is(Blocks.OBSIDIAN);
    }
}
