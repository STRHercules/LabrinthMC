package com.labrinthmc.labrinth.world.room;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.GenerationConnectionRules;
import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.GenerationSeeds;
import com.labrinthmc.labrinth.world.generation.DepthCatalog;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import com.labrinthmc.labrinth.world.generation.VerticalCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Registered, deterministic room definitions and their bounded materializer. */
public final class RoomCatalog {
    public static final int FLOOR_Y = VerticalCatalog.BASE_FLOOR_Y;
    public static final int HEIGHT = 8;
    public static final int CELL_SIZE = GenerationGrid.CELL_SIZE_BLOCKS;
    public static final int APERTURE_WIDTH = 5;
    public static final int APERTURE_HEIGHT = 4;
    public static final ResourceLocation STANDARD_REGION =
            ResourceLocation.fromNamespaceAndPath("labrinth", "standard");

    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "room_selection");
    private static final long SELECTION_LOCAL_X = 0x524F4F4D53454CL;
    private static final long SELECTION_LOCAL_Z = 0x524F4F4D535459L;

    // Smaller rooms remain full-length on one axis so their retained doorway
    // endpoints still land on a neighboring cell boundary.
    private static final List<RoomDefinition> DEFINITIONS = List.of(
            create(
                    RoomKind.EMPTY,
                    "empty",
                    4,
                    StructurePiece.Rarity.COMMON,
                    RoomDefinition.InteriorStyle.EMPTY,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    null,
                    List.of(),
                new StructurePiece.PlacementConditions(1, false)),
            create(
                    RoomKind.SMALL_STORAGE,
                    "small_storage",
                    6,
                    StructurePiece.Rarity.COMMON,
                    RoomDefinition.InteriorStyle.STORAGE,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    "chests/small_storage",
                    List.of("storage/shelves"),
                    new StructurePiece.PlacementConditions(1, true)),
            create(
                    RoomKind.LARGE_CHAMBER,
                    "large_chamber",
                    2,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.CHAMBER,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST,
                            GenerationGrid.Direction.SOUTH, GenerationGrid.Direction.WEST),
                    null,
                    List.of("chamber/pillars"),
                    new StructurePiece.PlacementConditions(2, true)),
            create(
                    RoomKind.UTILITY,
                    "utility",
                    4,
                    StructurePiece.Rarity.COMMON,
                    RoomDefinition.InteriorStyle.UTILITY,
                    Set.of(GenerationGrid.Direction.EAST, GenerationGrid.Direction.WEST),
                    "chests/utility",
                    List.of("utility/workstations"),
                    new StructurePiece.PlacementConditions(1, true)),
            create(
                    RoomKind.CROSS_ROOM,
                    "cross_room",
                    3,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.CROSS,
                    EnumSet.allOf(GenerationGrid.Direction.class),
                    null,
                    List.of("cross/columns"),
                    new StructurePiece.PlacementConditions(2, true)),
            create(
                    RoomKind.LONG_RECTANGULAR,
                    "long_rectangular",
                    4,
                    StructurePiece.Rarity.COMMON,
                    RoomDefinition.InteriorStyle.LONG,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    null,
                    List.of("long/trim"),
                    new StructurePiece.PlacementConditions(1, true)),
            create(
                    RoomKind.MULTI_EXIT,
                    "multi_exit",
                    2,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.MULTI_EXIT,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST,
                            GenerationGrid.Direction.SOUTH),
                    null,
                    List.of("multi_exit/marker"),
                    new StructurePiece.PlacementConditions(2, true)),
            create(
                    RoomKind.DEAD_END_REWARD,
                    "dead_end_reward",
                    1,
                    StructurePiece.Rarity.RARE,
                    RoomDefinition.InteriorStyle.REWARD,
                    Set.of(GenerationGrid.Direction.NORTH),
                    "chests/dead_end_reward",
                    List.of("reward/pedestal"),
                    new StructurePiece.PlacementConditions(1, true)),
            create(
                    RoomKind.DECORATIVE,
                    "decorative",
                    2,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.DECORATIVE,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    null,
                    List.of("decorative/ambient"),
                    new StructurePiece.PlacementConditions(1, false)),
            create(
                    RoomKind.RARE_TEST,
                    "rare_test",
                    1,
                    StructurePiece.Rarity.VERY_RARE,
                    RoomDefinition.InteriorStyle.RARE,
                    EnumSet.allOf(GenerationGrid.Direction.class),
                    "chests/rare_test",
                    List.of("rare/test_marker"),
                    new StructurePiece.PlacementConditions(2, true)),
            createSized(
                    RoomKind.SMALL_CHAMBER,
                    "small_chamber",
                    4,
                    StructurePiece.Rarity.COMMON,
                    RoomDefinition.InteriorStyle.CHAMBER,
                    32,
                    6,
                    CELL_SIZE,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    null,
                    List.of("chamber/pillars"),
                    new StructurePiece.PlacementConditions(1, true)),
            createSized(
                    RoomKind.MEDIUM_CHAMBER,
                    "medium_chamber",
                    3,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.CHAMBER,
                    48,
                    8,
                    CELL_SIZE,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    null,
                    List.of("chamber/pillars"),
                    new StructurePiece.PlacementConditions(1, true)),
            createSized(
                    RoomKind.WIDE_CHAMBER,
                    "wide_chamber",
                    3,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.CHAMBER,
                    CELL_SIZE,
                    10,
                    32,
                    Set.of(GenerationGrid.Direction.EAST, GenerationGrid.Direction.WEST),
                    null,
                    List.of("chamber/pillars"),
                    new StructurePiece.PlacementConditions(1, true)),
            createSized(
                    RoomKind.TALL_CHAMBER,
                    "tall_chamber",
                    2,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.CHAMBER,
                    CELL_SIZE,
                    12,
                    CELL_SIZE,
                    EnumSet.allOf(GenerationGrid.Direction.class),
                    null,
                    List.of("chamber/pillars"),
                    new StructurePiece.PlacementConditions(2, true)),
            createSized(
                    RoomKind.GRAND_CHAMBER,
                    "grand_chamber",
                    1,
                    StructurePiece.Rarity.RARE,
                    RoomDefinition.InteriorStyle.CHAMBER,
                    CELL_SIZE,
                    14,
                    CELL_SIZE,
                    EnumSet.allOf(GenerationGrid.Direction.class),
                    null,
                    List.of("chamber/pillars"),
                    new StructurePiece.PlacementConditions(2, true)),
            createSized(
                    RoomKind.SMALL_STORAGE_VARIANT,
                    "small_storage_variant",
                    3,
                    StructurePiece.Rarity.COMMON,
                    RoomDefinition.InteriorStyle.STORAGE,
                    32,
                    6,
                    CELL_SIZE,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    "chests/small_storage",
                    List.of("storage/shelves"),
                    new StructurePiece.PlacementConditions(1, true)),
            createSized(
                    RoomKind.WIDE_GALLERY,
                    "wide_gallery",
                    2,
                    StructurePiece.Rarity.UNCOMMON,
                    RoomDefinition.InteriorStyle.LONG,
                    CELL_SIZE,
                    9,
                    32,
                    Set.of(GenerationGrid.Direction.EAST, GenerationGrid.Direction.WEST),
                    null,
                    List.of("long/trim"),
                    new StructurePiece.PlacementConditions(1, true)),
            createSized(
                    RoomKind.TALL_ARCHIVE,
                    "tall_archive",
                    1,
                    StructurePiece.Rarity.RARE,
                    RoomDefinition.InteriorStyle.LONG,
                    48,
                    12,
                    CELL_SIZE,
                    Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH),
                    "chests/utility",
                    List.of("long/trim"),
                    new StructurePiece.PlacementConditions(2, true)));

    private static final Map<StructurePiece, RoomDefinition> BY_PIECE = createPieceMap();
    private static final Map<RoomKind, RoomDefinition> BY_KIND = createKindMap();
    private static final Map<ResourceLocation, RoomDefinition> BY_ID = createIdMap();

    private RoomCatalog() {
    }

    public static List<RoomDefinition> definitions() {
        return DEFINITIONS;
    }

    public static RoomDefinition definition(RoomKind kind) {
        return BY_KIND.get(Objects.requireNonNull(kind, "kind"));
    }

    public static RoomDefinition definition(ResourceLocation id) {
        return BY_ID.get(Objects.requireNonNull(id, "id"));
    }

    public static Selection select(RandomState randomState, GenerationGrid.Cell cell) {
        return select(randomState, cell, 0, STANDARD_REGION);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region) {
        return select(randomState, cell, depth, region, 0);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(region, "region");
        validateFloor(floorIndex);
        validateDepth(depth);
        validateRegion(region, depth, floorIndex);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        RandomSource random = factory.at(
                Math.toIntExact(cell.x()),
                Math.addExact(depth, floorIndex),
                Math.toIntExact(cell.z()));
        return select(random, cell, depth, region, floorIndex);
    }

    public static Selection select(long worldSeed, GenerationGrid.Cell cell) {
        return select(worldSeed, cell, 0, STANDARD_REGION);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region) {
        return select(worldSeed, cell, depth, region, 0);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(region, "region");
        validateFloor(floorIndex);
        validateDepth(depth);
        validateRegion(region, depth, floorIndex);
        RandomSource random = RandomSource.create(GenerationSeeds.roomSeed(
                worldSeed,
                cell,
                SELECTION_LOCAL_X + depth,
                SELECTION_LOCAL_Z + floorIndex));
        return select(random, cell, depth, region, floorIndex);
    }

    public static Placement placement(RandomState randomState, GenerationGrid.Cell cell) {
        return placement(randomState, cell, 0, STANDARD_REGION);
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region) {
        return placement(randomState, cell, depth, region, 0);
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        Selection selection = select(randomState, cell, depth, region, floorIndex);
        return new Placement(selection, Set.of());
    }

    public static Placement placement(long worldSeed, GenerationGrid.Cell cell) {
        return placement(worldSeed, cell, 0, STANDARD_REGION);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region) {
        return placement(worldSeed, cell, depth, region, 0);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        Selection selection = select(worldSeed, cell, depth, region, floorIndex);
        return new Placement(selection, Set.of());
    }

    /** Materialize only the target chunk's intersection with a room. */
    public static void place(ChunkAccess chunk, Placement placement) {
        place(chunk, placement, RegionCatalog.standard());
    }

    /** Materialize a room using the selected region palette. */
    public static void place(
            ChunkAccess chunk,
            Placement placement,
            RegionDefinition region) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(region, "region");
        PlacedStructurePiece placed = placement.piece();
        definitionFor(placed.definition());
        var bounds = placed.bounds();
        ChunkPos chunkPos = chunk.getPos();
        long minX = Math.max(bounds.minBlockX(), (long) chunkPos.getMinBlockX());
        long maxX = Math.min(
                bounds.maxBlockXExclusive(),
                (long) chunkPos.getMinBlockX() + GenerationGrid.CHUNK_SIZE_BLOCKS);
        long minZ = Math.max(bounds.minBlockZ(), (long) chunkPos.getMinBlockZ());
        long maxZ = Math.min(
                bounds.maxBlockZExclusive(),
                (long) chunkPos.getMinBlockZ() + GenerationGrid.CHUNK_SIZE_BLOCKS);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (long worldZ = minZ; worldZ < maxZ; worldZ++) {
            for (long worldX = minX; worldX < maxX; worldX++) {
                for (int worldY = bounds.minY(); worldY < bounds.maxYExclusive(); worldY++) {
                    BlockState state = blockStateAt(
                            placement,
                            Math.toIntExact(worldX),
                            worldY,
                            Math.toIntExact(worldZ),
                            region);
                    if (!state.isAir()) {
                        chunk.setBlockState(
                                blockPos.set(
                                        Math.toIntExact(worldX),
                                        worldY,
                                        Math.toIntExact(worldZ)),
                                state,
                                false);
                    }
                }
            }
        }
    }

    public static BlockState blockStateAt(
            Placement placement,
            int worldX,
            int worldY,
            int worldZ) {
        return blockStateAt(placement, worldX, worldY, worldZ, RegionCatalog.standard());
    }

    public static BlockState blockStateAt(
            Placement placement,
            int worldX,
            int worldY,
            int worldZ,
            RegionDefinition region) {
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(region, "region");
        PlacedStructurePiece placed = placement.piece();
        RoomDefinition definition = definitionFor(placed.definition());
        var bounds = placed.bounds();
        if (worldX < bounds.minBlockX() || worldX >= bounds.maxBlockXExclusive()
                || worldZ < bounds.minBlockZ() || worldZ >= bounds.maxBlockZExclusive()
                || worldY < bounds.minY() || worldY >= bounds.maxYExclusive()) {
            return Blocks.AIR.defaultBlockState();
        }
        LocalPoint local = inverseTransform(placed, worldX, worldY, worldZ);
        return local == null
                ? Blocks.AIR.defaultBlockState()
                : blockStateForLocal(
                        placed,
                        definition,
                        local.x(),
                        local.y(),
                        local.z(),
                        placed.rotation(),
                        placement.openDirections(),
                        region);
    }

    public static boolean supports(StructurePiece definition) {
        return BY_PIECE.containsKey(definition);
    }

    private static Selection select(
            RandomSource random,
            GenerationGrid.Cell cell,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        List<RoomDefinition> candidates = DEFINITIONS.stream()
                .filter(definition -> definition.piece().weight() > 0)
                .filter(definition -> definition.eligible(depth, region))
                .filter(definition -> DepthCatalog.roomAllowed(definition, depth))
                .filter(definition -> RegionCatalog.resolve(region).allowsRoom(definition.id()))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("no eligible room definitions for depth/region");
        }
        RoomDefinition definition = weightedChoice(random, candidates, depth);
        List<StructurePiece.Rotation> rotations = Arrays.stream(StructurePiece.Rotation.values())
                .filter(definition.piece().allowedRotations()::contains)
                .toList();
        StructurePiece.Rotation rotation = rotations.get(random.nextInt(rotations.size()));
        PlacedStructurePiece piece = definition.piece().placedAt(
                originFor(cell, definition.piece(), rotation, floorIndex),
                rotation,
                StructurePiece.Mirror.NONE);
        EnumSet<GenerationGrid.Direction> connectorDirections =
                EnumSet.noneOf(GenerationGrid.Direction.class);
        for (Connector connector : definition.piece().connectors()) {
            connectorDirections.add(toGridDirection(connector.direction()).rotated(rotation));
        }
        return new Selection(definition, rotation, piece, connectorDirections);
    }

    private static RoomDefinition weightedChoice(
            RandomSource random,
            List<RoomDefinition> candidates,
            int depth) {
        long totalWeight = candidates.stream()
                .mapToLong(candidate -> DepthCatalog.roomWeight(candidate, depth))
                .sum();
        if (totalWeight <= 0 || totalWeight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("room weights must have a positive integer total");
        }
        int choice = random.nextInt((int) totalWeight);
        for (RoomDefinition candidate : candidates) {
            choice -= DepthCatalog.roomWeight(candidate, depth);
            if (choice < 0) {
                return candidate;
            }
        }
        throw new IllegalStateException("weighted room choice did not resolve");
    }

    private static StructurePiece.BlockPoint originFor(
            GenerationGrid.Cell cell,
            StructurePiece piece,
            StructurePiece.Rotation rotation,
            int floorIndex) {
        long cellX = GenerationGrid.blockOriginX(cell);
        long cellZ = GenerationGrid.blockOriginZ(cell);
        int transformedWidth = StructurePiece.transformedWidth(piece.width(), piece.depth(), rotation);
        int transformedDepth = StructurePiece.transformedDepth(piece.width(), piece.depth(), rotation);
        return new StructurePiece.BlockPoint(
                cellX + (CELL_SIZE - transformedWidth) / 2,
                VerticalCatalog.floorY(floorIndex),
                cellZ + (CELL_SIZE - transformedDepth) / 2);
    }

    private static void validateFloor(int floorIndex) {
        if (!VerticalCatalog.isValidFloor(floorIndex)) {
            throw new IllegalArgumentException("unsupported Labrinth floor: " + floorIndex);
        }
    }

    private static void validateDepth(int depth) {
        if (depth < DepthCatalog.MIN_DEPTH || depth > DepthCatalog.MAX_DEPTH) {
            throw new IllegalArgumentException("unsupported Labrinth depth: " + depth);
        }
    }

    private static void validateRegion(ResourceLocation region, int depth, int floorIndex) {
        if (!RegionCatalog.resolve(region).eligible(depth, floorIndex)) {
            throw new IllegalArgumentException(
                    "region is not eligible at depth " + depth + " and floor " + floorIndex + ": " + region);
        }
    }

    private static BlockState blockStateForLocal(
            PlacedStructurePiece placed,
            RoomDefinition definition,
            int localX,
            int localY,
            int localZ,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        GenerationConnectionRules.LocalCenter center =
                GenerationConnectionRules.localCellCenter(placed);
        int width = definition.piece().width();
        int depth = definition.piece().depth();
        int height = definition.piece().height();
        BlockState state;
        if (localY == 0) {
            state = floorState(definition.interiorStyle(), localX, localZ);
        } else if (localY == height - 1) {
            state = isLight(localX, localZ, center)
                    ? Blocks.SEA_LANTERN.defaultBlockState()
                    : Blocks.DEEPSLATE_TILES.defaultBlockState();
        } else if (isBoundary(localX, localZ, width, depth)
                && !isOpenFaceCell(
                        placed,
                        definition,
                        localX,
                        localY,
                        localZ,
                        rotation,
                        openDirections,
                        center,
                        width,
                        depth)) {
            state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        } else if (isSpawnMarker(definition, localX, localY, localZ)) {
            state = Blocks.SOUL_TORCH.defaultBlockState();
        } else {
            state = interiorState(
                    definition.interiorStyle(),
                    localX,
                    localY,
                    localZ,
                    width,
                    depth);
        }
        return region.paletteState(
                state,
                localY,
                height,
                localX,
                localZ);
    }

    private static boolean isSpawnMarker(
            RoomDefinition definition,
            int localX,
            int localY,
            int localZ) {
        return definition.spawnMarkers().stream()
                .anyMatch(marker -> marker.x() == localX
                        && marker.y() == localY
                        && marker.z() == localZ);
    }

    private static BlockState floorState(
            RoomDefinition.InteriorStyle style,
            int localX,
            int localZ) {
        if (style == RoomDefinition.InteriorStyle.DECORATIVE
                && (localX + localZ) % 13 == 0) {
            return Blocks.MOSSY_COBBLESTONE.defaultBlockState();
        }
        if (style == RoomDefinition.InteriorStyle.RARE
                && (localX * 3 + localZ) % 17 == 0) {
            return Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        }
        return (localX + localZ) % 29 == 0
                ? Blocks.DEEPSLATE_TILES.defaultBlockState()
                : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
    }

    private static boolean isLight(
            int localX,
            int localZ,
            GenerationConnectionRules.LocalCenter center) {
        return (localX == center.x() && localZ % 8 == 4)
                || (localZ == center.z() && localX % 8 == 4);
    }

    private static boolean isBoundary(int localX, int localZ, int width, int depth) {
        return localX == 0 || localX == width - 1
                || localZ == 0 || localZ == depth - 1;
    }

    private static boolean isOpenFaceCell(
            PlacedStructurePiece placed,
            RoomDefinition definition,
            int localX,
            int localY,
            int localZ,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> openDirections,
            GenerationConnectionRules.LocalCenter center,
            int width,
            int depth) {
        // Match the four-block vertical aperture declared by every horizontal
        // connector. Keeping the ceiling and floor in the shell prevents a
        // room from exposing a taller hole than the hallway can actually use.
        if (localY < 1 || localY >= 1 + APERTURE_HEIGHT) {
            return false;
        }
        for (GenerationGrid.Direction localDirection : GenerationGrid.Direction.values()) {
            if (!hasBaseConnector(definition, localDirection)) {
                continue;
            }
            boolean atFace = switch (localDirection) {
                case NORTH -> localZ == 0;
                case EAST -> localX == width - 1;
                case SOUTH -> localZ == depth - 1;
                case WEST -> localX == 0;
            };
            if (!atFace || !openDirections.contains(localDirection.rotated(rotation))) {
                continue;
            }
            int across = localDirection == GenerationGrid.Direction.NORTH
                    || localDirection == GenerationGrid.Direction.SOUTH ? localX : localZ;
            int apertureCenter = localDirection == GenerationGrid.Direction.NORTH
                    || localDirection == GenerationGrid.Direction.SOUTH
                    ? center.x()
                    : center.z();
            if (Math.abs(across - apertureCenter) <= APERTURE_WIDTH / 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasBaseConnector(
            RoomDefinition definition,
            GenerationGrid.Direction direction) {
        return definition.piece().connectors().stream()
                .map(connector -> toGridDirection(connector.direction()))
                .anyMatch(direction::equals);
    }

    private static BlockState interiorState(
            RoomDefinition.InteriorStyle style,
            int localX,
            int localY,
            int localZ,
            int width,
            int depth) {
        return switch (style) {
            case EMPTY -> Blocks.AIR.defaultBlockState();
            case STORAGE -> storageState(localX, localY, localZ, width, depth);
            case CHAMBER -> chamberState(localX, localY, localZ, width, depth);
            case UTILITY -> utilityState(localX, localY, localZ, width, depth);
            case CROSS -> crossState(localX, localY, localZ, width, depth);
            case LONG -> longState(localX, localY, localZ, width, depth);
            case MULTI_EXIT -> multiExitState(localX, localY, localZ, width, depth);
            case REWARD -> rewardState(localX, localY, localZ, width, depth);
            case DECORATIVE -> decorativeState(localX, localY, localZ, width, depth);
            case RARE -> rareState(localX, localY, localZ, width, depth);
        };
    }

    private static BlockState storageState(int x, int y, int z, int width, int depth) {
        int sideInset = Math.max(3, width / 6);
        if (y == 1 && (x == sideInset || x == width - 1 - sideInset) && z % 8 == 3) {
            return Blocks.BARREL.defaultBlockState();
        }
        int chestInset = Math.max(4, width / 4);
        if (y == 1 && (x == chestInset || x == width - 1 - chestInset) && z % 16 == 7) {
            return Blocks.CHEST.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState chamberState(int x, int y, int z, int width, int depth) {
        int xInset = Math.max(4, width / 5);
        int zInset = Math.max(4, depth / 5);
        if (y <= 3 && (x == xInset || x == width - 1 - xInset)
                && (z == zInset || z == depth - 1 - zInset)) {
            return Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState utilityState(int x, int y, int z, int width, int depth) {
        if (y != 1) {
            return Blocks.AIR.defaultBlockState();
        }
        int centerZ = depth / 3;
        int step = Math.max(5, width / 7);
        if (x == step && z == centerZ) {
            return Blocks.CRAFTING_TABLE.defaultBlockState();
        }
        if (x == step * 2 && z == centerZ) {
            return Blocks.BLAST_FURNACE.defaultBlockState();
        }
        if (x == step * 3 && z == centerZ) {
            return Blocks.ANVIL.defaultBlockState();
        }
        if (x == step * 4 && z == centerZ) {
            return Blocks.CAULDRON.defaultBlockState();
        }
        if (x == step * 5 && z == centerZ) {
            return Blocks.LEVER.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState crossState(int x, int y, int z, int width, int depth) {
        int xInset = Math.max(4, width / 5);
        int zInset = Math.max(4, depth / 5);
        if (y <= 2 && (x == xInset || x == width - 1 - xInset)
                && (z == zInset || z == depth - 1 - zInset)) {
            return Blocks.CHISELED_DEEPSLATE.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState longState(int x, int y, int z, int width, int depth) {
        int xInset = Math.max(4, width / 6);
        if (y <= 2 && (x == xInset || x == width - 1 - xInset) && z % 16 == 8) {
            return Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState multiExitState(int x, int y, int z, int width, int depth) {
        if (y == 1 && x == width / 2 && z == depth / 2) {
            return Blocks.LODESTONE.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState rewardState(int x, int y, int z, int width, int depth) {
        if (y == 1 && x == width / 2 && z == Math.max(4, depth - 16)) {
            return Blocks.CHEST.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState decorativeState(int x, int y, int z, int width, int depth) {
        if (y == 2 && x == width / 2 && z == depth / 2) {
            return Blocks.COBWEB.defaultBlockState();
        }
        if (y == 1 && (x == Math.max(3, width / 4) || x == width - 1 - Math.max(3, width / 4))
                && z == Math.max(3, depth / 4)) {
            return Blocks.CHAIN.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState rareState(int x, int y, int z, int width, int depth) {
        if (y == 1 && x == width / 2 && z == depth / 2) {
            return Blocks.CHEST.defaultBlockState();
        }
        if (y == 1 && x == Math.max(4, width / 4) && z == Math.max(4, depth / 4)) {
            return Blocks.AMETHYST_BLOCK.defaultBlockState();
        }
        if (y == 1 && x == width - 1 - Math.max(4, width / 4)
                && z == depth - 1 - Math.max(4, depth / 4)) {
            return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        }
        if (y == 1 && x == width / 2 && z == Math.max(4, depth / 2 - 4)) {
            return Blocks.LEVER.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static StructurePiece createPiece(
            RoomKind kind,
            String path,
            int weight,
            StructurePiece.Rarity rarity,
            RoomDefinition.InteriorStyle style,
            Set<GenerationGrid.Direction> directions,
            String lootPath,
            List<String> decorationPaths,
            StructurePiece.PlacementConditions placementConditions) {
        return createPiece(
                kind,
                path,
                weight,
                rarity,
                style,
                CELL_SIZE,
                HEIGHT,
                CELL_SIZE,
                directions,
                lootPath,
                decorationPaths,
                placementConditions);
    }

    private static StructurePiece createPiece(
            RoomKind kind,
            String path,
            int weight,
            StructurePiece.Rarity rarity,
            RoomDefinition.InteriorStyle style,
            int width,
            int height,
            int depth,
            Set<GenerationGrid.Direction> directions,
            String lootPath,
            List<String> decorationPaths,
            StructurePiece.PlacementConditions placementConditions) {
        StructurePiece.Builder builder = StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "room/" + path),
                        ResourceLocation.fromNamespaceAndPath("labrinth", "generated/room/" + path),
                        StructurePiece.Kind.ROOM,
                        width,
                        height,
                        depth)
                .weight(weight)
                .rarity(rarity)
                .rotations(EnumSet.allOf(StructurePiece.Rotation.class))
                .mirror(StructurePiece.Mirror.NONE)
                .depthRange(0, 32)
                .allowedRegions(RegionCatalog.REGION_IDS)
                .connectors(directions.stream()
                        .sorted()
                        .map(direction -> connector(direction, width, depth))
                        .toList())
                .placementConditions(placementConditions)
                .decorations(new StructurePiece.DecorationRules(decorationPaths.stream()
                        .map(value -> ResourceLocation.fromNamespaceAndPath("labrinth", "room/" + value))
                        .toList()));
        if (lootPath != null) {
            builder.loot(StructurePiece.LootConfiguration.table(
                    ResourceLocation.fromNamespaceAndPath("labrinth", lootPath)));
        }
        return builder.build();
    }

    private static RoomDefinition create(
            RoomKind kind,
            String path,
            int weight,
            StructurePiece.Rarity rarity,
            RoomDefinition.InteriorStyle style,
            Set<GenerationGrid.Direction> directions,
            String lootPath,
            List<String> decorationPaths,
            StructurePiece.PlacementConditions placementConditions) {
        return createSized(
                kind,
                path,
                weight,
                rarity,
                style,
                CELL_SIZE,
                HEIGHT,
                CELL_SIZE,
                directions,
                lootPath,
                decorationPaths,
                placementConditions);
    }

    private static RoomDefinition createSized(
            RoomKind kind,
            String path,
            int weight,
            StructurePiece.Rarity rarity,
            RoomDefinition.InteriorStyle style,
            int width,
            int height,
            int depth,
            Set<GenerationGrid.Direction> directions,
            String lootPath,
            List<String> decorationPaths,
            StructurePiece.PlacementConditions placementConditions) {
        return new RoomDefinition(
                kind,
                createPiece(
                        kind,
                        path,
                        weight,
                        rarity,
                        style,
                        width,
                        height,
                        depth,
                        directions,
                        lootPath,
                        decorationPaths,
                        placementConditions),
                style,
                spawnMarkers(kind));
    }

    private static List<RoomDefinition.SpawnMarker> spawnMarkers(RoomKind kind) {
        return switch (kind) {
            case LARGE_CHAMBER -> List.of(new RoomDefinition.SpawnMarker(
                    ResourceLocation.fromNamespaceAndPath("labrinth", "spawn/chamber"),
                    CELL_SIZE / 2,
                    1,
                    CELL_SIZE / 2));
            case CROSS_ROOM -> List.of(new RoomDefinition.SpawnMarker(
                    ResourceLocation.fromNamespaceAndPath("labrinth", "spawn/cross"),
                    CELL_SIZE / 2,
                    1,
                    CELL_SIZE / 2));
            case RARE_TEST -> List.of(new RoomDefinition.SpawnMarker(
                    ResourceLocation.fromNamespaceAndPath("labrinth", "spawn/rare_test"),
                    CELL_SIZE / 2,
                    1,
                    CELL_SIZE / 2 + 8));
            default -> List.of();
        };
    }

    private static Connector connector(GenerationGrid.Direction direction) {
        return connector(direction, CELL_SIZE, CELL_SIZE);
    }

    private static Connector connector(
            GenerationGrid.Direction direction,
            int width,
            int depth) {
        return switch (direction) {
            case NORTH -> new Connector(
                    new Connector.Position(width / 2, 1, 0),
                    Connector.Direction.NORTH,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
            case EAST -> new Connector(
                    new Connector.Position(width, 1, depth / 2),
                    Connector.Direction.EAST,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
            case SOUTH -> new Connector(
                    new Connector.Position(width / 2, 1, depth),
                    Connector.Direction.SOUTH,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
            case WEST -> new Connector(
                    new Connector.Position(0, 1, depth / 2),
                    Connector.Direction.WEST,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
        };
    }

    private static Map<StructurePiece, RoomDefinition> createPieceMap() {
        IdentityHashMap<StructurePiece, RoomDefinition> values = new IdentityHashMap<>();
        for (RoomDefinition definition : DEFINITIONS) {
            values.put(definition.piece(), definition);
        }
        return Map.copyOf(values);
    }

    private static Map<RoomKind, RoomDefinition> createKindMap() {
        EnumMap<RoomKind, RoomDefinition> values = new EnumMap<>(RoomKind.class);
        for (RoomDefinition definition : DEFINITIONS) {
            values.put(definition.kind(), definition);
        }
        return Map.copyOf(values);
    }

    private static Map<ResourceLocation, RoomDefinition> createIdMap() {
        Map<ResourceLocation, RoomDefinition> values = new java.util.HashMap<>();
        for (RoomDefinition definition : DEFINITIONS) {
            values.put(definition.id(), definition);
        }
        return Map.copyOf(values);
    }

    private static RoomDefinition definitionFor(StructurePiece piece) {
        RoomDefinition definition = BY_PIECE.get(piece);
        if (definition == null) {
            throw new IllegalArgumentException("unknown room definition: " + piece.id());
        }
        return definition;
    }

    private static GenerationGrid.Direction toGridDirection(Connector.Direction direction) {
        return switch (direction) {
            case NORTH -> GenerationGrid.Direction.NORTH;
            case EAST -> GenerationGrid.Direction.EAST;
            case SOUTH -> GenerationGrid.Direction.SOUTH;
            case WEST -> GenerationGrid.Direction.WEST;
            case UP, DOWN -> throw new IllegalArgumentException("rooms are horizontal in Phase 5");
        };
    }

    private static LocalPoint inverseTransform(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ) {
        int width = placed.definition().width();
        int depth = placed.definition().depth();
        long transformedX = worldX - placed.origin().x();
        long transformedZ = worldZ - placed.origin().z();
        int transformedWidth = StructurePiece.transformedWidth(width, depth, placed.rotation());
        int transformedDepth = StructurePiece.transformedDepth(width, depth, placed.rotation());
        if (transformedX < 0 || transformedX >= transformedWidth
                || transformedZ < 0 || transformedZ >= transformedDepth) {
            return null;
        }
        long localX;
        long localZ;
        switch (placed.rotation()) {
            case NONE -> {
                localX = transformedX;
                localZ = transformedZ;
            }
            case CLOCKWISE_90 -> {
                localX = transformedZ;
                localZ = depth - 1L - transformedX;
            }
            case CLOCKWISE_180 -> {
                localX = width - 1L - transformedX;
                localZ = depth - 1L - transformedZ;
            }
            case COUNTERCLOCKWISE_90 -> {
                localX = width - 1L - transformedZ;
                localZ = transformedX;
            }
            default -> throw new IllegalStateException("unhandled room rotation: " + placed.rotation());
        }
        return new LocalPoint(
                Math.toIntExact(localX),
                worldY - placed.origin().y(),
                Math.toIntExact(localZ));
    }

    public record Selection(
            RoomDefinition definition,
            StructurePiece.Rotation rotation,
            PlacedStructurePiece piece,
            Set<GenerationGrid.Direction> connectorDirections) {
        public Selection {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(rotation, "rotation");
            Objects.requireNonNull(piece, "piece");
            connectorDirections = Set.copyOf(connectorDirections);
        }

        public RoomKind kind() {
            return definition.kind();
        }
    }

    public record Placement(
            Selection selection,
            Set<GenerationGrid.Direction> openDirections) {
        public Placement {
            Objects.requireNonNull(selection, "selection");
            openDirections = Set.copyOf(openDirections);
        }

        public PlacedStructurePiece piece() {
            return selection.piece();
        }

        public RoomKind kind() {
            return selection.kind();
        }
    }

    private record LocalPoint(int x, int y, int z) {
    }
}
