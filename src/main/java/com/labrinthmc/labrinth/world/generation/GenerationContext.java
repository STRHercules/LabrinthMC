package com.labrinthmc.labrinth.world.generation;

import java.util.Objects;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/**
 * Immutable input bundle for one generation decision.
 *
 * <p>The random source is intentionally owned by this context and is created
 * from the target chunk coordinates. Callers must not share it across cells;
 * all repeatable decisions should instead use {@link GenerationSeeds}.
 */
public record GenerationContext(
        long worldSeed,
        long dimensionSeed,
        GenerationGrid.Cell cell,
        GenerationGrid.Chunk chunk,
        int depth,
        ResourceLocation region,
        GenerationNeighbors neighbors,
        RandomSource random,
        GenerationConstraints constraints) {
    public GenerationContext {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(neighbors, "neighbors");
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(constraints, "constraints");
        if (!cell.equals(chunk.cell())) {
            throw new IllegalArgumentException("context cell must own its chunk");
        }
        if (!constraints.allowsDepth(depth)) {
            throw new IllegalArgumentException("depth is outside generation constraints");
        }
    }

    public static GenerationContext create(
            long worldSeed,
            GenerationGrid.Chunk chunk,
            int depth,
            ResourceLocation region,
            GenerationConstraints constraints) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(constraints, "constraints");
        GenerationGrid.Cell cell = chunk.cell();
        return new GenerationContext(
                worldSeed,
                GenerationSeeds.dimensionSeed(worldSeed),
                cell,
                chunk,
                depth,
                region,
                GenerationNeighbors.forCell(worldSeed, cell),
                RandomSource.create(GenerationSeeds.contextSeed(worldSeed, chunk)),
                constraints);
    }

    public static GenerationContext create(long worldSeed, GenerationGrid.Chunk chunk, int depth) {
        return create(
                worldSeed,
                chunk,
                depth,
                RegionCatalog.select(worldSeed, chunk.cell(), depth, 0).id(),
                GenerationConstraints.LABRINTH);
    }

    /** Create a context with the depth derived from the owning cell. */
    public static GenerationContext create(long worldSeed, GenerationGrid.Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        int depth = DepthCatalog.depthAt(worldSeed, chunk.cell(), 0);
        return create(worldSeed, chunk, depth);
    }
}
