package com.labrinthmc.labrinth.world.corridor;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.GenerationSeeds;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import com.labrinthmc.labrinth.world.generation.VerticalCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Materialized straight-corridor pieces. The standard piece spans one cell;
 * short and medium pieces reuse the same shell with centered footprints.
 */
public final class StraightCorridor {
    public static final int WIDTH = 7;
    public static final int HEIGHT = 6;
    public static final int LENGTH = GenerationGrid.CELL_SIZE_BLOCKS;
    public static final int SHORT_LENGTH = GenerationGrid.CELL_SIZE_BLOCKS / 2;
    public static final int MEDIUM_LENGTH = (SHORT_LENGTH + LENGTH) / 2;
    public static final int LONG_LENGTH = LENGTH;
    public static final int FLOOR_Y = VerticalCatalog.BASE_FLOOR_Y;
    private static final int LIGHT_SPACING = 8;
    private static final int SPAWN_MARGIN = 3;

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "corridor/straight");
    private static final ResourceLocation TEMPLATE =
            ResourceLocation.fromNamespaceAndPath("labrinth", "generated/straight_corridor");
    private static final ResourceLocation SHORT_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "corridor/short_straight");
    private static final ResourceLocation SHORT_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath("labrinth", "generated/short_straight_corridor");
    private static final ResourceLocation MEDIUM_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "corridor/medium_straight");
    private static final ResourceLocation MEDIUM_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath("labrinth", "generated/medium_straight_corridor");
    private static final ResourceLocation LONG_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "corridor/long_straight");
    private static final ResourceLocation LONG_TEMPLATE =
            ResourceLocation.fromNamespaceAndPath("labrinth", "generated/long_straight_corridor");
    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "straight_corridor");

    private static final StructurePiece DEFINITION = createDefinition(ID, TEMPLATE, LENGTH);
    private static final StructurePiece SHORT_DEFINITION =
            createDefinition(SHORT_ID, SHORT_TEMPLATE, SHORT_LENGTH);
    private static final StructurePiece MEDIUM_DEFINITION =
            createDefinition(MEDIUM_ID, MEDIUM_TEMPLATE, MEDIUM_LENGTH);
    private static final StructurePiece LONG_DEFINITION =
            createDefinition(LONG_ID, LONG_TEMPLATE, LONG_LENGTH);

    private StraightCorridor() {
    }

    public static StructurePiece definition() {
        return DEFINITION;
    }

    /** Return the reusable half-cell straight-corridor definition. */
    public static StructurePiece shortDefinition() {
        return SHORT_DEFINITION;
    }

    /** Return the reusable three-quarter-cell straight-corridor definition. */
    public static StructurePiece mediumDefinition() {
        return MEDIUM_DEFINITION;
    }

    /** Return the reusable full-cell long straight-corridor definition. */
    public static StructurePiece longDefinition() {
        return LONG_DEFINITION;
    }

    /** Create the deterministic placement for one cell and one axis. */
    public static PlacedStructurePiece placedAt(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(rotation, "rotation");
        return DEFINITION.placedAt(originFor(cell, rotation), rotation, StructurePiece.Mirror.NONE);
    }

    /** Create a centered short-corridor placement without making a selection decision. */
    public static PlacedStructurePiece shortPlacedAt(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(rotation, "rotation");
        return SHORT_DEFINITION.placedAt(
                shortOriginFor(cell, rotation),
                rotation,
                StructurePiece.Mirror.NONE);
    }

    /** Create a centered medium-corridor placement without making a selection decision. */
    public static PlacedStructurePiece mediumPlacedAt(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(rotation, "rotation");
        return MEDIUM_DEFINITION.placedAt(
                mediumOriginFor(cell, rotation),
                rotation,
                StructurePiece.Mirror.NONE);
    }

    /** Create a full-cell long-corridor placement without making a selection decision. */
    public static PlacedStructurePiece longPlacedAt(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(rotation, "rotation");
        return LONG_DEFINITION.placedAt(
                longOriginFor(cell, rotation),
                rotation,
                StructurePiece.Mirror.NONE);
    }

    /**
     * Seed utility used by framework checks and non-Minecraft callers. The
     * live generator uses the game's positional random factory so the same
     * choice also incorporates the world seed.
     */
    public static StructurePiece.Rotation rotationForSeed(
            long worldSeed,
            GenerationGrid.Cell cell) {
        return GenerationSeeds.corridorIndex(worldSeed, cell, 0, 0, 2) == 0
                ? StructurePiece.Rotation.NONE
                : StructurePiece.Rotation.CLOCKWISE_90;
    }

    /** Derive the corridor axis from the world-seeded generation random state. */
    public static StructurePiece.Rotation rotationFor(
            RandomState randomState,
            GenerationGrid.Cell cell) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        PositionalRandomFactory randomFactory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        RandomSource random = randomFactory.at(
                Math.toIntExact(cell.x()),
                0,
                Math.toIntExact(cell.z()));
        return random.nextBoolean()
                ? StructurePiece.Rotation.NONE
                : StructurePiece.Rotation.CLOCKWISE_90;
    }

    /**
     * Keep ordinary corridors centered in their cell. The origin cell gets a
     * small negative margin so the vanilla spawn point (0, 0) lands inside
     * the first hallway for either supported axis.
     */
    public static StructurePiece.BlockPoint originFor(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        return originFor(cell, rotation, LENGTH);
    }

    /** Return the short variant's centered origin, with the origin-cell spawn margin. */
    public static StructurePiece.BlockPoint shortOriginFor(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        return originFor(cell, rotation, SHORT_LENGTH);
    }

    /** Return the medium variant's centered origin, with the origin-cell spawn margin. */
    public static StructurePiece.BlockPoint mediumOriginFor(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        return originFor(cell, rotation, MEDIUM_LENGTH);
    }

    /** Return the long variant's centered origin, with the origin-cell spawn margin. */
    public static StructurePiece.BlockPoint longOriginFor(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation) {
        return originFor(cell, rotation, LONG_LENGTH);
    }

    private static StructurePiece.BlockPoint originFor(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation,
            int length) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(rotation, "rotation");
        long cellX = GenerationGrid.blockOriginX(cell);
        long cellZ = GenerationGrid.blockOriginZ(cell);
        if (cell.x() == 0 && cell.z() == 0) {
            return new StructurePiece.BlockPoint(
                    cellX - SPAWN_MARGIN,
                    FLOOR_Y,
                    cellZ - SPAWN_MARGIN);
        }

        // Align the corridor centerline with the cell boundary connector
        // coordinate used by rooms and junctions.
        int crossAxisOffset = (GenerationGrid.CELL_SIZE_BLOCKS - WIDTH + 1) / 2;
        int axisOffset = (GenerationGrid.CELL_SIZE_BLOCKS - length + 1) / 2;
        return switch (rotation) {
            case NONE, CLOCKWISE_180 -> new StructurePiece.BlockPoint(
                    cellX + crossAxisOffset,
                    FLOOR_Y,
                    cellZ + axisOffset);
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new StructurePiece.BlockPoint(
                    cellX + axisOffset,
                    FLOOR_Y,
                    cellZ + crossAxisOffset);
        };
    }

    /**
     * Materialize only the portion intersecting the target chunk. The piece
     * is evaluated from its immutable definition on every intersecting chunk;
     * no neighboring chunks are loaded or mutated.
     */
    public static void place(ChunkAccess chunk, PlacedStructurePiece placed) {
        place(chunk, placed, EnumSet.allOf(GenerationGrid.Direction.class));
    }

    /** Materialize a straight corridor while capping connectors not in the open set. */
    public static void place(
            ChunkAccess chunk,
            PlacedStructurePiece placed,
            java.util.Set<GenerationGrid.Direction> openDirections) {
        place(chunk, placed, openDirections, RegionCatalog.standard());
    }

    public static void place(
            ChunkAccess chunk,
            PlacedStructurePiece placed,
            java.util.Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(placed, "placed");
        Objects.requireNonNull(openDirections, "openDirections");
        Objects.requireNonNull(region, "region");
        int length = lengthFor(placed.definition());
        if (length < 0) {
            throw new IllegalArgumentException("placed piece is not a supported straight corridor");
        }

        ChunkPos chunkPos = chunk.getPos();
        int minChunkX = chunkPos.getMinBlockX();
        int minChunkZ = chunkPos.getMinBlockZ();
        int maxChunkX = minChunkX + 16;
        int maxChunkZ = minChunkZ + 16;
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        for (int localZ = 0; localZ < length; localZ++) {
            for (int localX = 0; localX < WIDTH; localX++) {
                for (int localY = 0; localY < HEIGHT; localY++) {
                    BlockState state = blockStateForLocal(
                            localX,
                            localY,
                            localZ,
                            length,
                            placed.rotation(),
                            openDirections,
                            region);
                    if (state.isAir()) {
                        continue;
                    }
                    StructurePiece.BlockPoint world = transformBlock(
                            placed.origin(),
                            localX,
                            localY,
                            localZ,
                            placed.rotation(),
                            placed.mirror(),
                            length);
                    if (world.x() >= minChunkX && world.x() < maxChunkX
                            && world.z() >= minChunkZ && world.z() < maxChunkZ) {
                        chunk.setBlockState(
                                blockPos.set(Math.toIntExact(world.x()), world.y(), Math.toIntExact(world.z())),
                                state,
                                false);
                    }
                }
            }
        }
    }

    /** Return the generated state at a world coordinate, or air outside it. */
    public static BlockState blockStateAt(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ) {
        return blockStateAt(
                placed,
                worldX,
                worldY,
                worldZ,
                EnumSet.allOf(GenerationGrid.Direction.class));
    }

    /** Return the generated state while treating closed connectors as capped. */
    public static BlockState blockStateAt(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ,
            java.util.Set<GenerationGrid.Direction> openDirections) {
        return blockStateAt(
                placed,
                worldX,
                worldY,
                worldZ,
                openDirections,
                RegionCatalog.standard());
    }

    public static BlockState blockStateAt(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ,
            java.util.Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        Objects.requireNonNull(placed, "placed");
        Objects.requireNonNull(openDirections, "openDirections");
        Objects.requireNonNull(region, "region");
        int length = lengthFor(placed.definition());
        if (length < 0 || !contains(placed, worldX, worldY, worldZ)) {
            return Blocks.AIR.defaultBlockState();
        }

        LocalPoint local = inverseTransform(placed, worldX, worldY, worldZ);
        return local == null
                ? Blocks.AIR.defaultBlockState()
                : blockStateForLocal(
                        local.x(),
                        local.y(),
                        local.z(),
                        length,
                        placed.rotation(),
                        openDirections,
                        region);
    }

    private static StructurePiece createDefinition(
            ResourceLocation id,
            ResourceLocation template,
            int length) {
        return StructurePiece.builder(
                        id,
                        template,
                        StructurePiece.Kind.CORRIDOR,
                        WIDTH,
                        HEIGHT,
                        length)
                .weight(1)
                .rotations(EnumSet.allOf(StructurePiece.Rotation.class))
                .mirror(StructurePiece.Mirror.NONE)
                .connectors(List.of(
                        new Connector(
                                new Connector.Position(WIDTH / 2, 1, 0),
                                Connector.Direction.NORTH,
                                Connector.Type.STANDARD,
                                WIDTH - 2,
                                HEIGHT - 2,
                                StructurePiece.Rotation.NONE,
                                true),
                        new Connector(
                                new Connector.Position(WIDTH / 2, 1, length),
                                Connector.Direction.SOUTH,
                                Connector.Type.STANDARD,
                                WIDTH - 2,
                                HEIGHT - 2,
                                StructurePiece.Rotation.NONE,
                                true)))
                .build();
    }

    private static int lengthFor(StructurePiece definition) {
        if (definition == DEFINITION) {
            return LENGTH;
        }
        if (definition == SHORT_DEFINITION) {
            return SHORT_LENGTH;
        }
        if (definition == MEDIUM_DEFINITION) {
            return MEDIUM_LENGTH;
        }
        if (definition == LONG_DEFINITION) {
            return LONG_LENGTH;
        }
        return -1;
    }

    static boolean supports(StructurePiece definition) {
        return lengthFor(definition) > 0;
    }

    private static boolean contains(PlacedStructurePiece placed, int worldX, int worldY, int worldZ) {
        var bounds = placed.bounds();
        return worldX >= bounds.minBlockX()
                && worldX < bounds.maxBlockXExclusive()
                && worldZ >= bounds.minBlockZ()
                && worldZ < bounds.maxBlockZExclusive()
                && worldY >= bounds.minY()
                && worldY < bounds.maxYExclusive();
    }

    private static BlockState blockStateForLocal(
            int localX,
            int localY,
            int localZ,
            int length,
            StructurePiece.Rotation rotation,
            java.util.Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        BlockState state;
        if (localY == 0) {
            state = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else if (localY == HEIGHT - 1) {
            state = localX == WIDTH / 2 && localZ % LIGHT_SPACING == LIGHT_SPACING / 2
                    ? Blocks.SEA_LANTERN.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else if (localX == 0 || localX == WIDTH - 1) {
            state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        } else if (localZ == 0
                && !openDirections.contains(GenerationGrid.Direction.NORTH.rotated(rotation))) {
            state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        } else if (localZ == length - 1
                && !openDirections.contains(GenerationGrid.Direction.SOUTH.rotated(rotation))) {
            state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        } else {
            state = Blocks.AIR.defaultBlockState();
        }
        return region.paletteState(state, localY, HEIGHT, localX, localZ);
    }

    private static StructurePiece.BlockPoint transformBlock(
            StructurePiece.BlockPoint origin,
            int localX,
            int localY,
            int localZ,
            StructurePiece.Rotation rotation,
            StructurePiece.Mirror mirror,
            int length) {
        int mirroredX = localX;
        int mirroredZ = localZ;
        if (mirror == StructurePiece.Mirror.LEFT_RIGHT) {
            mirroredX = WIDTH - 1 - localX;
        } else if (mirror == StructurePiece.Mirror.FRONT_BACK) {
            mirroredZ = length - 1 - localZ;
        }

        long transformedX;
        long transformedZ;
        switch (rotation) {
            case NONE -> {
                transformedX = mirroredX;
                transformedZ = mirroredZ;
            }
            case CLOCKWISE_90 -> {
                transformedX = length - 1L - mirroredZ;
                transformedZ = mirroredX;
            }
            case CLOCKWISE_180 -> {
                transformedX = WIDTH - 1L - mirroredX;
                transformedZ = length - 1L - mirroredZ;
            }
            case COUNTERCLOCKWISE_90 -> {
                transformedX = mirroredZ;
                transformedZ = WIDTH - 1L - mirroredX;
            }
            default -> throw new IllegalStateException("unhandled corridor rotation: " + rotation);
        }
        return origin.add(transformedX, localY, transformedZ);
    }

    private static LocalPoint inverseTransform(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ) {
        long transformedX = worldX - placed.origin().x();
        long transformedZ = worldZ - placed.origin().z();
        int length = lengthFor(placed.definition());
        int width = placed.definition().width();
        long transformedWidth = StructurePiece.transformedWidth(width, length, placed.rotation());
        long transformedDepth = StructurePiece.transformedDepth(width, length, placed.rotation());
        if (transformedX < 0 || transformedX >= transformedWidth
                || transformedZ < 0 || transformedZ >= transformedDepth) {
            return null;
        }

        long mirroredX;
        long mirroredZ;
        switch (placed.rotation()) {
            case NONE -> {
                mirroredX = transformedX;
                mirroredZ = transformedZ;
            }
            case CLOCKWISE_90 -> {
                mirroredX = transformedZ;
                mirroredZ = length - 1L - transformedX;
            }
            case CLOCKWISE_180 -> {
                mirroredX = WIDTH - 1L - transformedX;
                mirroredZ = length - 1L - transformedZ;
            }
            case COUNTERCLOCKWISE_90 -> {
                mirroredX = WIDTH - 1L - transformedZ;
                mirroredZ = transformedX;
            }
            default -> throw new IllegalStateException("unhandled corridor rotation: " + placed.rotation());
        }

        int localX = Math.toIntExact(mirroredX);
        int localZ = Math.toIntExact(mirroredZ);
        if (placed.mirror() == StructurePiece.Mirror.LEFT_RIGHT) {
            localX = width - 1 - localX;
        } else if (placed.mirror() == StructurePiece.Mirror.FRONT_BACK) {
            localZ = length - 1 - localZ;
        }
        return new LocalPoint(localX, worldY - placed.origin().y(), localZ);
    }

    private record LocalPoint(int x, int y, int z) {
    }
}
