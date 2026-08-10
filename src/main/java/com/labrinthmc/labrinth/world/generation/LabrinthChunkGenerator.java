package com.labrinthmc.labrinth.world.generation;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.labrinthmc.labrinth.world.corridor.CorridorCatalog;
import com.labrinthmc.labrinth.world.corridor.CorridorSelectionConfig;
import com.labrinthmc.labrinth.world.corridor.StraightCorridor;
import com.labrinthmc.labrinth.world.landmark.LandmarkCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.StructureSet;

/**
 * Chunk generator for the executable Labrinth room, corridor, and vertical
 * content system. Generation is local to the target chunk; the bounded
 * neighboring-cell loops only materialize already-determined pieces whose
 * bounds cross into it.
 */
public final class LabrinthChunkGenerator extends ChunkGenerator {
    public static final MapCodec<LabrinthChunkGenerator> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            BiomeSource.CODEC.fieldOf("biome_source")
                    .forGetter(LabrinthChunkGenerator::getBiomeSource),
            CorridorSelectionConfig.CODEC
                    .optionalFieldOf("corridor_config", CorridorCatalog.DEFAULT_CONFIG)
                    .forGetter(LabrinthChunkGenerator::corridorConfig))
            .apply(instance, instance.stable(LabrinthChunkGenerator::new)));

    private final CorridorSelectionConfig corridorConfig;

    public LabrinthChunkGenerator(BiomeSource biomeSource) {
        this(biomeSource, CorridorCatalog.DEFAULT_CONFIG);
    }

    public LabrinthChunkGenerator(
            BiomeSource biomeSource,
            CorridorSelectionConfig corridorConfig) {
        super(Objects.requireNonNull(biomeSource, "biomeSource"));
        this.corridorConfig = Objects.requireNonNull(corridorConfig, "corridorConfig");
    }

    public CorridorSelectionConfig corridorConfig() {
        return corridorConfig;
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public ChunkGeneratorStructureState createState(
            HolderLookup<StructureSet> structureSets,
            RandomState randomState,
            long seed) {
        return ChunkGeneratorStructureState.createForFlat(
                randomState,
                seed,
                biomeSource,
                Stream.empty());
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(
            Blender blender,
            RandomState randomState,
            StructureManager structureManager,
            ChunkAccess chunk) {
        for (int floorIndex = VerticalCatalog.MIN_FLOOR;
                floorIndex <= VerticalCatalog.MAX_FLOOR;
                floorIndex++) {
            final int currentFloor = floorIndex;
            forEachIntersectingContent(
                    chunk.getPos(),
                    randomState,
                    corridorConfig,
                    currentFloor,
                    placement -> LabrinthContentCatalog.place(chunk, placement));
        }
        for (int lowerFloor = VerticalCatalog.MIN_FLOOR;
                lowerFloor < VerticalCatalog.MAX_FLOOR;
                lowerFloor++) {
            final int currentBoundary = lowerFloor;
            forEachIntersectingVertical(
                    chunk.getPos(),
                    randomState,
                    corridorConfig,
                    currentBoundary,
                    vertical -> VerticalCatalog.place(
                            chunk,
                            vertical.selection(),
                            vertical.region(),
                            vertical.horizontalPlacement()));
        }
        // Landmark placement is sector-owned. Smaller pieces are filtered out
        // above so this final pass can materialize a landmark without being
        // overwritten by a neighboring cell's ordinary content.
        for (LandmarkCatalog.Instance landmark : LandmarkCatalog.intersecting(
                randomState,
                chunk.getPos())) {
            LandmarkCatalog.place(chunk, landmark);
        }
        return CompletableFuture.completedFuture(chunk);
    }

    @Override
    public void buildSurface(
            WorldGenRegion region,
            StructureManager structureManager,
            RandomState randomState,
            ChunkAccess chunk) {
        // Rooms, corridors, and vertical pieces already own their complete
        // shell. No vanilla terrain surface pass is needed.
    }

    @Override
    public void applyCarvers(
            WorldGenRegion region,
            long seed,
            RandomState randomState,
            BiomeManager biomeManager,
            StructureManager structureManager,
            ChunkAccess chunk,
            GenerationStep.Carving carving) {
        // No terrain or carver pass is needed for the enclosed generated layers.
    }

    @Override
    public void applyBiomeDecoration(
            net.minecraft.world.level.WorldGenLevel level,
            ChunkAccess chunk,
            StructureManager structureManager) {
        // Avoid vanilla decoration scans while the_void supplies no features.
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Mob population is intentionally deferred to the later spawn phase.
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor heightAccessor) {
        return FLOOR_SPAWN_HEIGHT;
    }

    @Override
    public int getGenDepth() {
        return GenerationConstraints.LABRINTH.maxYExclusive() - GenerationConstraints.LABRINTH.minY();
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return GenerationConstraints.LABRINTH.minY();
    }

    @Override
    public int getBaseHeight(
            int x,
            int z,
            Heightmap.Types heightmap,
        LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        final int[] highest = {heightAccessor.getMinBuildHeight()};
        for (int floorIndex = VerticalCatalog.MIN_FLOOR;
                floorIndex <= VerticalCatalog.MAX_FLOOR;
                floorIndex++) {
            final int currentFloor = floorIndex;
            forEachIntersectingContent(
                    GenerationGrid.chunkForBlock(x, z),
                    randomState,
                    corridorConfig,
                    currentFloor,
                    placement -> {
                        var bounds = placement.piece().bounds();
                        if (x >= bounds.minBlockX() && x < bounds.maxBlockXExclusive()
                                && z >= bounds.minBlockZ() && z < bounds.maxBlockZExclusive()) {
                            highest[0] = Math.max(highest[0], bounds.maxYExclusive());
                        }
                    });
        }
        for (int lowerFloor = VerticalCatalog.MIN_FLOOR;
                lowerFloor < VerticalCatalog.MAX_FLOOR;
                lowerFloor++) {
            final int currentBoundary = lowerFloor;
            forEachIntersectingVertical(
                    GenerationGrid.chunkForBlock(x, z),
                    randomState,
                    corridorConfig,
                    currentBoundary,
                    vertical -> {
                        if (VerticalCatalog.contains(
                                vertical.selection(),
                                x,
                                vertical.selection().lowerY(),
                                z)) {
                            highest[0] = Math.max(highest[0], vertical.selection().upperY());
                        }
                    });
        }
        for (LandmarkCatalog.Instance landmark : LandmarkCatalog.intersecting(
                randomState,
                GenerationGrid.chunkForBlock(x, z))) {
            var bounds = landmark.piece().bounds();
            if (x >= bounds.minBlockX() && x < bounds.maxBlockXExclusive()
                    && z >= bounds.minBlockZ() && z < bounds.maxBlockZExclusive()) {
                highest[0] = Math.max(highest[0], bounds.maxYExclusive());
            }
        }
        return highest[0];
    }

    @Override
    public NoiseColumn getBaseColumn(
            int x,
            int z,
            LevelHeightAccessor heightAccessor,
            RandomState randomState) {
        int minY = heightAccessor.getMinBuildHeight();
        BlockState[] states = new BlockState[heightAccessor.getHeight()];
        java.util.Arrays.fill(states, Blocks.AIR.defaultBlockState());
        for (int floorIndex = VerticalCatalog.MIN_FLOOR;
                floorIndex <= VerticalCatalog.MAX_FLOOR;
                floorIndex++) {
            final int currentFloor = floorIndex;
            forEachIntersectingContent(
                    GenerationGrid.chunkForBlock(x, z),
                    randomState,
                    corridorConfig,
                    currentFloor,
                    placement -> {
                        var bounds = placement.piece().bounds();
                        if (x < bounds.minBlockX() || x >= bounds.maxBlockXExclusive()
                                || z < bounds.minBlockZ() || z >= bounds.maxBlockZExclusive()) {
                            return;
                        }
                        int startY = Math.max(minY, bounds.minY());
                        int endY = Math.min(heightAccessor.getMaxBuildHeight(), bounds.maxYExclusive());
                        for (int y = startY; y < endY; y++) {
                            BlockState state = LabrinthContentCatalog.blockStateAt(placement, x, y, z);
                            if (!state.isAir()) {
                                states[y - minY] = state;
                            }
                        }
                    });
        }
        for (int lowerFloor = VerticalCatalog.MIN_FLOOR;
                lowerFloor < VerticalCatalog.MAX_FLOOR;
                lowerFloor++) {
            final int currentBoundary = lowerFloor;
            forEachIntersectingVertical(
                    GenerationGrid.chunkForBlock(x, z),
                    randomState,
                    corridorConfig,
                    currentBoundary,
                    vertical -> {
                        if (!VerticalCatalog.contains(
                                vertical.selection(),
                                x,
                                vertical.selection().lowerY(),
                                z)) {
                            return;
                        }
                        int startY = Math.max(minY, vertical.selection().lowerY());
                        int endY = Math.min(
                                heightAccessor.getMaxBuildHeight(),
                                vertical.selection().upperY() + 1);
                        for (int y = startY; y < endY; y++) {
                            // Vertical content owns its footprint, including
                            // intentional air, so it can carve floor openings.
                            states[y - minY] = VerticalCatalog.blockStateAt(
                                    vertical.selection(),
                                    x,
                                    y,
                                    z,
                                    vertical.region(),
                                    vertical.horizontalPlacement());
                        }
                    });
        }
        for (LandmarkCatalog.Instance landmark : LandmarkCatalog.intersecting(
                randomState,
                GenerationGrid.chunkForBlock(x, z))) {
            var bounds = landmark.piece().bounds();
            if (x < bounds.minBlockX() || x >= bounds.maxBlockXExclusive()
                    || z < bounds.minBlockZ() || z >= bounds.maxBlockZExclusive()) {
                continue;
            }
            int startY = Math.max(minY, bounds.minY());
            int endY = Math.min(heightAccessor.getMaxBuildHeight(), bounds.maxYExclusive());
            for (int y = startY; y < endY; y++) {
                BlockState state = LandmarkCatalog.blockStateAt(landmark, x, y, z);
                if (!state.isAir()) {
                    states[y - minY] = state;
                }
            }
        }
        return new NoiseColumn(minY, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState randomState, BlockPos pos) {
        info.add("Labrinth room, corridor, vertical, and landmark catalog");
        info.add("Cell: " + GenerationGrid.cellForBlock(pos.getX(), pos.getZ()));
        GenerationGrid.Cell cell = GenerationGrid.cellForBlock(pos.getX(), pos.getZ());
        info.add("Depth: " + DepthCatalog.profile(randomState, cell, 0).depth());
    }

    private static final int FLOOR_SPAWN_HEIGHT = StraightCorridor.FLOOR_Y + 1;

    private static void forEachIntersectingContent(
            ChunkPos chunkPos,
            RandomState randomState,
            CorridorSelectionConfig corridorConfig,
            int floorIndex,
            Consumer<LabrinthContentCatalog.Placement> consumer) {
        GenerationGrid.Cell center = GenerationGrid.cellForChunk(chunkPos.x, chunkPos.z);
        List<LandmarkCatalog.Instance> landmarks = LandmarkCatalog.intersecting(randomState, chunkPos);
        GenerationGrid.Chunk targetChunk = new GenerationGrid.Chunk(chunkPos.x, chunkPos.z);
        // Every selected piece stays within its owning cell except the bounded
        // three-block origin margin, so one-cell lookaround is sufficient.
        for (long cellX = center.x() - 1; cellX <= center.x() + 1; cellX++) {
            for (long cellZ = center.z() - 1; cellZ <= center.z() + 1; cellZ++) {
                GenerationGrid.Cell cell = new GenerationGrid.Cell(cellX, cellZ);
                int depth = DepthCatalog.depthAt(randomState, cell, floorIndex);
                LabrinthContentCatalog.Placement placement = LabrinthContentCatalog.placement(
                        randomState,
                        cell,
                        corridorConfig,
                        depth,
                        floorIndex);
                if (placement.piece().intersects(targetChunk)
                        && landmarks.stream().noneMatch(landmark ->
                                LandmarkCatalog.overlaps(landmark, placement.piece().bounds()))) {
                    consumer.accept(placement);
                }
            }
        }
    }

    private static void forEachIntersectingContent(
            GenerationGrid.Chunk chunk,
            RandomState randomState,
            CorridorSelectionConfig corridorConfig,
            int floorIndex,
            Consumer<LabrinthContentCatalog.Placement> consumer) {
        forEachIntersectingContent(
                new ChunkPos(Math.toIntExact(chunk.x()), Math.toIntExact(chunk.z())),
                randomState,
                corridorConfig,
                floorIndex,
                consumer);
    }

    private static void forEachIntersectingVertical(
            ChunkPos chunkPos,
            RandomState randomState,
            CorridorSelectionConfig corridorConfig,
            int lowerFloor,
            Consumer<VerticalPlacement> consumer) {
        GenerationGrid.Cell center = GenerationGrid.cellForChunk(chunkPos.x, chunkPos.z);
        List<LandmarkCatalog.Instance> landmarks = LandmarkCatalog.intersecting(randomState, chunkPos);
        GenerationGrid.Chunk targetChunk = new GenerationGrid.Chunk(chunkPos.x, chunkPos.z);
        // Vertical pieces are centered within their owning cell, so the same
        // bounded one-cell lookaround covers every chunk intersection.
        for (long cellX = center.x() - 1; cellX <= center.x() + 1; cellX++) {
            for (long cellZ = center.z() - 1; cellZ <= center.z() + 1; cellZ++) {
                GenerationGrid.Cell cell = new GenerationGrid.Cell(cellX, cellZ);
                VerticalCatalog.Selection selection = VerticalCatalog.select(
                        randomState,
                        cell,
                        lowerFloor);
                if (selection.present()
                        && selection.piece().intersects(targetChunk)
                        && landmarks.stream().noneMatch(landmark ->
                                LandmarkCatalog.overlaps(landmark, selection.piece().bounds()))) {
                    int depth = DepthCatalog.depthAt(randomState, cell, lowerFloor);
                    RegionDefinition region = RegionCatalog.select(
                            randomState,
                            cell,
                            depth,
                            lowerFloor);
                    LabrinthContentCatalog.Placement horizontalPlacement =
                            LabrinthContentCatalog.placement(
                                    randomState,
                                    cell,
                                    corridorConfig,
                                    depth,
                                    lowerFloor);
                    consumer.accept(new VerticalPlacement(
                            selection,
                            region,
                            horizontalPlacement));
                }
            }
        }
    }

    private static void forEachIntersectingVertical(
            GenerationGrid.Chunk chunk,
            RandomState randomState,
            CorridorSelectionConfig corridorConfig,
            int lowerFloor,
            Consumer<VerticalPlacement> consumer) {
        forEachIntersectingVertical(
                new ChunkPos(Math.toIntExact(chunk.x()), Math.toIntExact(chunk.z())),
                randomState,
                corridorConfig,
                lowerFloor,
                consumer);
    }

    private record VerticalPlacement(
            VerticalCatalog.Selection selection,
            RegionDefinition region,
            LabrinthContentCatalog.Placement horizontalPlacement) {
    }
}
