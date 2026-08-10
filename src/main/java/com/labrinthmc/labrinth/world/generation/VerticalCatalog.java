package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;

/**
 * Deterministic, bounded content for the vertical layer boundaries.
 *
 * <p>A vertical decision belongs to one horizontal cell and one boundary
 * between adjacent floors. The piece is then rematerialized by each
 * intersecting chunk without loading or asking any neighboring chunk to
 * generate it.
 */
public final class VerticalCatalog {
    public static final int BASE_FLOOR_Y = 4;
    public static final int FLOOR_SPACING = 16;
    public static final int MIN_FLOOR = -1;
    public static final int MAX_FLOOR = 1;
    public static final int WIDTH = 7;
    public static final int DEPTH = 7;
    public static final int HEIGHT = FLOOR_SPACING;
    public static final int VERTICAL_CHANCE_PERCENT = 12;

    // Odd-width vertical pieces use the cell's integer center coordinate, the
    // same boundary coordinate used by room and corridor connectors.
    private static final int CENTER_OFFSET =
            (GenerationGrid.CELL_SIZE_BLOCKS - WIDTH + 1) / 2;
    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "vertical_selection");
    private static final List<VerticalKind> RANDOM_KINDS = List.of(
            VerticalKind.STAIR_UP,
            VerticalKind.STAIR_DOWN,
            VerticalKind.LADDER_SHAFT);
    // Only traversable vertical pieces participate in live selection. The
    // drop and elevator definitions remain catalogued for later authored
    // implementations, but their placeholder landings must not appear as
    // unexplained wheat pits or chain-filled dead shafts in generated worlds.
    // The stair path begins at the outer wall, then follows the stairwell's
    // perimeter so the first rise is reachable from the horizontal shell.
    private static final int[][] STAIR_PATH = {
            {3, 0}, {3, 1}, {3, 2}, {2, 2},
            {1, 2}, {1, 3}, {1, 4}, {1, 5},
            {2, 5}, {3, 5}, {4, 5}, {5, 5},
            {5, 4}, {5, 3}, {5, 2}, {5, 1}
    };
    private static final Map<VerticalKind, StructurePiece> DEFINITIONS = createDefinitions();

    private VerticalCatalog() {
    }

    public enum VerticalKind {
        NONE,
        STAIR_UP,
        STAIR_DOWN,
        LADDER_SHAFT,
        DROP_SHAFT,
        ELEVATOR_PLACEHOLDER
    }

    public static int floorY(int floorIndex) {
        if (!isValidFloor(floorIndex)) {
            throw new IllegalArgumentException("unsupported Labrinth floor: " + floorIndex);
        }
        return Math.addExact(BASE_FLOOR_Y, Math.multiplyExact(floorIndex, FLOOR_SPACING));
    }

    public static boolean isValidFloor(int floorIndex) {
        return floorIndex >= MIN_FLOOR && floorIndex <= MAX_FLOOR;
    }

    public static boolean isValidBoundary(int lowerFloor) {
        return isValidFloor(lowerFloor) && isValidFloor(lowerFloor + 1);
    }

    public static List<StructurePiece> definitions() {
        return DEFINITIONS.values().stream().toList();
    }

    public static StructurePiece definition(VerticalKind kind) {
        Objects.requireNonNull(kind, "kind");
        return DEFINITIONS.get(kind);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int lowerFloor) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        validateBoundary(lowerFloor);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        RandomSource random = factory.at(
                Math.toIntExact(cell.x()),
                lowerFloor,
                Math.toIntExact(cell.z()));
        return select(cell, lowerFloor, random);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            int lowerFloor) {
        Objects.requireNonNull(cell, "cell");
        validateBoundary(lowerFloor);
        return select(
                cell,
                lowerFloor,
                RandomSource.create(GenerationSeeds.verticalSeed(worldSeed, cell, lowerFloor)));
    }

    /** Materialize only the selected vertical piece's target-chunk intersection. */
    public static void place(ChunkAccess chunk, Selection selection) {
        place(chunk, selection, RegionCatalog.standard(), null);
    }

    public static void place(
            ChunkAccess chunk,
            Selection selection,
            RegionDefinition region) {
        place(chunk, selection, region, null);
    }

    /**
     * Materialize a vertical piece with passages cut only where the same
     * cell's horizontal piece provides a walkable approach. This keeps wall
     * openings deterministic and avoids inspecting or loading neighboring
     * world blocks during generation.
     */
    public static void place(
            ChunkAccess chunk,
            Selection selection,
            RegionDefinition region,
            LabrinthContentCatalog.Placement horizontalPlacement) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(region, "region");
        if (!selection.present()) {
            return;
        }
        GenerationGrid.Bounds bounds = selection.piece().bounds();
        ChunkPosBounds chunkBounds = ChunkPosBounds.forChunk(chunk.getPos());
        long minX = Math.max(bounds.minBlockX(), chunkBounds.minX());
        long maxX = Math.min(bounds.maxBlockXExclusive(), chunkBounds.maxXExclusive());
        long minZ = Math.max(bounds.minBlockZ(), chunkBounds.minZ());
        long maxZ = Math.min(bounds.maxBlockZExclusive(), chunkBounds.maxZExclusive());
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (long worldZ = minZ; worldZ < maxZ; worldZ++) {
            for (long worldX = minX; worldX < maxX; worldX++) {
                // Include the upper-floor footprint deliberately. Horizontal
                // room/corridor renderers do not write air, so this explicit
                // pass preserves the stair landing while clearing its opening.
                for (int worldY = bounds.minY(); worldY <= selection.upperY(); worldY++) {
                    chunk.setBlockState(
                            blockPos.set(
                                    Math.toIntExact(worldX),
                                    worldY,
                                    Math.toIntExact(worldZ)),
                            blockStateAt(selection,
                                    Math.toIntExact(worldX),
                                    worldY,
                                    Math.toIntExact(worldZ),
                                    region,
                                    horizontalPlacement),
                            false);
                }
            }
        }
    }

    public static boolean contains(Selection selection, int worldX, int worldY, int worldZ) {
        Objects.requireNonNull(selection, "selection");
        if (!selection.present()) {
            return false;
        }
        GenerationGrid.Bounds bounds = selection.piece().bounds();
        return worldX >= bounds.minBlockX()
                && worldX < bounds.maxBlockXExclusive()
                && worldZ >= bounds.minBlockZ()
                && worldZ < bounds.maxBlockZExclusive()
                && worldY >= bounds.minY()
                && worldY <= selection.upperY();
    }

    public static BlockState blockStateAt(
            Selection selection,
            int worldX,
            int worldY,
            int worldZ) {
        return blockStateAt(selection, worldX, worldY, worldZ, RegionCatalog.standard());
    }

    public static BlockState blockStateAt(
            Selection selection,
            int worldX,
            int worldY,
            int worldZ,
            RegionDefinition region) {
        return blockStateAt(selection, worldX, worldY, worldZ, region, null);
    }

    public static BlockState blockStateAt(
            Selection selection,
            int worldX,
            int worldY,
            int worldZ,
            RegionDefinition region,
            LabrinthContentCatalog.Placement horizontalPlacement) {
        Objects.requireNonNull(selection, "selection");
        Objects.requireNonNull(region, "region");
        if (!contains(selection, worldX, worldY, worldZ)) {
            return Blocks.AIR.defaultBlockState();
        }
        int localX = Math.toIntExact(worldX - selection.piece().origin().x());
        int localZ = Math.toIntExact(worldZ - selection.piece().origin().z());
        if (worldY == selection.upperY()) {
            return upperFloorState();
        }

        int localY = worldY - selection.lowerY();
        boolean stairPath = selection.kind() == VerticalKind.STAIR_UP
                || selection.kind() == VerticalKind.STAIR_DOWN;
        if ((localX == 0 || localX == WIDTH - 1 || localZ == 0 || localZ == DEPTH - 1)
                && !(stairPath && isStairPath(localY, localX, localZ))) {
            return hasHorizontalPassage(
                            horizontalPlacement,
                            worldX,
                            worldY,
                            worldZ,
                            localX,
                            localZ)
                    ? Blocks.AIR.defaultBlockState()
                    : Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }

        BlockState state = switch (selection.kind()) {
            case STAIR_UP, STAIR_DOWN -> stairState(localY, localX, localZ);
            case LADDER_SHAFT -> localX == WIDTH / 2 && localZ == 1
                    ? Blocks.LADDER.defaultBlockState().setValue(LadderBlock.FACING, Direction.NORTH)
                    : Blocks.AIR.defaultBlockState();
            case DROP_SHAFT -> localY == 0 && localX >= 2 && localX <= 4 && localZ >= 2 && localZ <= 4
                    ? Blocks.HAY_BLOCK.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            case ELEVATOR_PLACEHOLDER -> elevatorState(localY, localX, localZ);
            case NONE -> Blocks.AIR.defaultBlockState();
        };
        return region.paletteState(state, localY, HEIGHT, localX, localZ);
    }

    private static Selection select(
            GenerationGrid.Cell cell,
            int lowerFloor,
            RandomSource random) {
        VerticalKind kind;
        if (cell.x() == 0 && cell.z() == 0) {
            // Keep the spawn cell connected to both adjacent floors: the lower
            // boundary slopes down, while the upper boundary slopes up.
            kind = lowerFloor == MIN_FLOOR
                    ? VerticalKind.STAIR_DOWN
                    : VerticalKind.STAIR_UP;
        } else if (random.nextInt(100) >= VERTICAL_CHANCE_PERCENT) {
            kind = VerticalKind.NONE;
        } else {
            kind = RANDOM_KINDS.get(random.nextInt(RANDOM_KINDS.size()));
        }

        if (kind == VerticalKind.NONE) {
            return new Selection(VerticalKind.NONE, null, null, lowerFloor);
        }
        StructurePiece definition = DEFINITIONS.get(kind);
        long originX = Math.addExact(GenerationGrid.blockOriginX(cell), CENTER_OFFSET);
        long originZ = Math.addExact(GenerationGrid.blockOriginZ(cell), CENTER_OFFSET);
        PlacedStructurePiece piece = definition.placedAt(
                new StructurePiece.BlockPoint(originX, floorY(lowerFloor), originZ),
                StructurePiece.Rotation.NONE,
                StructurePiece.Mirror.NONE);
        if (!GenerationConstraints.LABRINTH.contains(piece.bounds())) {
            throw new IllegalStateException("vertical piece exceeds Labrinth height: " + piece.bounds());
        }
        return new Selection(kind, definition, piece, lowerFloor);
    }

    private static BlockState stairState(int localY, int localX, int localZ) {
        if (localY == 0 && !isStairPath(localY, localX, localZ)) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        if (!isStairPath(localY, localX, localZ)) {
            return Blocks.AIR.defaultBlockState();
        }
        Direction ascending = localY < STAIR_PATH.length - 1
                ? directionBetween(STAIR_PATH[localY], STAIR_PATH[localY + 1])
                : directionBetween(STAIR_PATH[localY - 1], STAIR_PATH[localY]);
        // Vanilla's bottom-half stair FACING points toward its high side. Use
        // the next path position directly so each block can be climbed toward
        // the upper endpoint instead of presenting its back to the player.
        Direction facing = ascending;
        return Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, facing)
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static BlockState upperFloorState() {
        // The upper boundary is an intentional open shaft. The final stair
        // ends below this layer and the upper room/corridor floor remains
        // available immediately outside the seven-block footprint.
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean hasHorizontalPassage(
            LabrinthContentCatalog.Placement horizontalPlacement,
            int worldX,
            int worldY,
            int worldZ,
            int localX,
            int localZ) {
        if (horizontalPlacement == null
                || worldY < horizontalPlacement.piece().bounds().minY() + 1
                || worldY >= horizontalPlacement.piece().bounds().minY() + 5) {
            return false;
        }
        if (localZ == 0 && isWalkableHorizontalAir(
                horizontalPlacement, worldX, worldY, worldZ - 1)) {
            return true;
        }
        if (localX == WIDTH - 1 && isWalkableHorizontalAir(
                horizontalPlacement, worldX + 1, worldY, worldZ)) {
            return true;
        }
        if (localZ == DEPTH - 1 && isWalkableHorizontalAir(
                horizontalPlacement, worldX, worldY, worldZ + 1)) {
            return true;
        }
        return localX == 0 && isWalkableHorizontalAir(
                horizontalPlacement, worldX - 1, worldY, worldZ);
    }

    private static boolean isWalkableHorizontalAir(
            LabrinthContentCatalog.Placement horizontalPlacement,
            int worldX,
            int worldY,
            int worldZ) {
        BlockState passage = LabrinthContentCatalog.blockStateAt(
                horizontalPlacement,
                worldX,
                worldY,
                worldZ);
        BlockState floor = LabrinthContentCatalog.blockStateAt(
                horizontalPlacement,
                worldX,
                horizontalPlacement.piece().bounds().minY(),
                worldZ);
        return passage.isAir() && !floor.isAir();
    }

    private static boolean isStairPath(int localY, int localX, int localZ) {
        return localY >= 0
                && localY < STAIR_PATH.length
                && STAIR_PATH[localY][0] == localX
                && STAIR_PATH[localY][1] == localZ;
    }

    private static Direction directionBetween(int[] from, int[] to) {
        int deltaX = Integer.compare(to[0], from[0]);
        int deltaZ = Integer.compare(to[1], from[1]);
        if (deltaX > 0) {
            return Direction.EAST;
        }
        if (deltaX < 0) {
            return Direction.WEST;
        }
        if (deltaZ > 0) {
            return Direction.SOUTH;
        }
        if (deltaZ < 0) {
            return Direction.NORTH;
        }
        return Direction.NORTH;
    }

    private static BlockState elevatorState(int localY, int localX, int localZ) {
        if (localY == 0 || localY == HEIGHT - 1) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        return localX == WIDTH / 2 && localZ == DEPTH / 2
                ? Blocks.CHAIN.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
    }

    private static Map<VerticalKind, StructurePiece> createDefinitions() {
        EnumMap<VerticalKind, StructurePiece> definitions = new EnumMap<>(VerticalKind.class);
        definitions.put(VerticalKind.STAIR_UP, createDefinition(VerticalKind.STAIR_UP, true));
        definitions.put(VerticalKind.STAIR_DOWN, createDefinition(VerticalKind.STAIR_DOWN, false));
        definitions.put(VerticalKind.LADDER_SHAFT, createDefinition(VerticalKind.LADDER_SHAFT, true));
        definitions.put(VerticalKind.DROP_SHAFT, createDefinition(VerticalKind.DROP_SHAFT, true));
        definitions.put(VerticalKind.ELEVATOR_PLACEHOLDER,
                createDefinition(VerticalKind.ELEVATOR_PLACEHOLDER, true));
        return Map.copyOf(definitions);
    }

    private static StructurePiece createDefinition(VerticalKind kind, boolean lowerIsUp) {
        Connector.Type lowerType;
        Connector.Type upperType;
        if (kind == VerticalKind.LADDER_SHAFT
                || kind == VerticalKind.DROP_SHAFT
                || kind == VerticalKind.ELEVATOR_PLACEHOLDER) {
            lowerType = Connector.Type.SHAFT;
            upperType = Connector.Type.SHAFT;
        } else if (lowerIsUp) {
            lowerType = Connector.Type.STAIR_UP;
            upperType = Connector.Type.STAIR_DOWN;
        } else {
            lowerType = Connector.Type.STAIR_DOWN;
            upperType = Connector.Type.STAIR_UP;
        }
        return StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath(
                                "labrinth", "vertical/" + kind.name().toLowerCase()),
                        ResourceLocation.fromNamespaceAndPath(
                                "labrinth", "generated/vertical/" + kind.name().toLowerCase()),
                        kind == VerticalKind.STAIR_UP || kind == VerticalKind.STAIR_DOWN
                                ? StructurePiece.Kind.STAIRWAY
                                : StructurePiece.Kind.SHAFT,
                        WIDTH,
                        HEIGHT,
                        DEPTH)
                .weight(1)
                .rarity(StructurePiece.Rarity.UNCOMMON)
                .rotations(EnumSet.of(StructurePiece.Rotation.NONE))
                .mirror(StructurePiece.Mirror.NONE)
                .depthRange(0, GenerationConstraints.LABRINTH.maxDepth())
                .allowedRegions(RegionCatalog.REGION_IDS)
                .connectors(List.of(
                        verticalConnector(Connector.Direction.DOWN, lowerType, 0),
                        verticalConnector(Connector.Direction.UP, upperType, HEIGHT)))
                .placementConditions(new StructurePiece.PlacementConditions(1, false))
                .build();
    }

    private static Connector verticalConnector(
            Connector.Direction direction,
            Connector.Type type,
            int y) {
        return new Connector(
                new Connector.Position(WIDTH / 2, y, DEPTH / 2),
                direction,
                type,
                3,
                4,
                StructurePiece.Rotation.NONE,
                true);
    }

    private static void validateBoundary(int lowerFloor) {
        if (!isValidBoundary(lowerFloor)) {
            throw new IllegalArgumentException("unsupported Labrinth floor boundary: " + lowerFloor);
        }
    }

    private record ChunkPosBounds(long minX, long minZ, long maxXExclusive, long maxZExclusive) {
        private static ChunkPosBounds forChunk(net.minecraft.world.level.ChunkPos chunkPos) {
            long minX = chunkPos.getMinBlockX();
            long minZ = chunkPos.getMinBlockZ();
            return new ChunkPosBounds(
                    minX,
                    minZ,
                    minX + GenerationGrid.CHUNK_SIZE_BLOCKS,
                    minZ + GenerationGrid.CHUNK_SIZE_BLOCKS);
        }
    }

    public record Selection(
            VerticalKind kind,
            StructurePiece definition,
            PlacedStructurePiece piece,
            int lowerFloor) {
        public Selection {
            Objects.requireNonNull(kind, "kind");
            if (!isValidBoundary(lowerFloor)) {
                throw new IllegalArgumentException("invalid vertical boundary: " + lowerFloor);
            }
            if (kind == VerticalKind.NONE) {
                if (definition != null || piece != null) {
                    throw new IllegalArgumentException("empty vertical selection cannot contain a piece");
                }
            } else if (definition == null || piece == null) {
                throw new IllegalArgumentException("vertical selection must contain a piece");
            }
        }

        public boolean present() {
            return kind != VerticalKind.NONE;
        }

        public int lowerY() {
            return floorY(lowerFloor);
        }

        public int upperY() {
            return floorY(lowerFloor + 1);
        }
    }
}
