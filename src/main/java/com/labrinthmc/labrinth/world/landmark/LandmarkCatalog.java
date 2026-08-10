package com.labrinthmc.labrinth.world.landmark;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.DepthCatalog;
import com.labrinthmc.labrinth.world.generation.GenerationConstraints;
import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.GenerationNeighbors;
import com.labrinthmc.labrinth.world.generation.GenerationSeeds;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import com.labrinthmc.labrinth.world.generation.VerticalCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Sector-owned landmark selection and chunk-local procedural materialization. */
public final class LandmarkCatalog {
    public static final int SECTOR_SIZE_CELLS = 32;
    public static final int LANDMARK_CHANCE_PERCENT = 16;

    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "landmark_selection");
    private static final List<LandmarkDefinition> DEFINITIONS = List.of(
            create(
                    "grand_hall",
                    8,
                    48,
                    48,
                    16,
                    0,
                    32,
                    -1,
                    1,
                    RegionCatalog.REGION_IDS,
                    LandmarkDefinition.Style.GRAND_HALL,
                    Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH)),
            create(
                    "central_stairwell",
                    6,
                    32,
                    32,
                    32,
                    0,
                    24,
                    -1,
                    0,
                    Set.of(RegionCatalog.STANDARD_ID, RegionCatalog.INDUSTRIAL_ID,
                            RegionCatalog.ABANDONED_ID, RegionCatalog.ANCIENT_ID),
                    LandmarkDefinition.Style.CENTRAL_STAIRWELL,
                    Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH)),
            create(
                    "massive_storage_complex",
                    5,
                    48,
                    32,
                    16,
                    2,
                    32,
                    -1,
                    1,
                    Set.of(RegionCatalog.STANDARD_ID, RegionCatalog.INDUSTRIAL_ID,
                            RegionCatalog.ABANDONED_ID),
                    LandmarkDefinition.Style.MASSIVE_STORAGE_COMPLEX,
                    Set.of(Connector.Direction.EAST, Connector.Direction.WEST)),
            create(
                    "generator_room",
                    4,
                    32,
                    48,
                    16,
                    4,
                    32,
                    0,
                    0,
                    Set.of(RegionCatalog.INDUSTRIAL_ID, RegionCatalog.ANCIENT_ID,
                            RegionCatalog.CORRUPTED_ID),
                    LandmarkDefinition.Style.GENERATOR_ROOM,
                    Set.of(Connector.Direction.NORTH, Connector.Direction.EAST,
                            Connector.Direction.SOUTH, Connector.Direction.WEST)),
            create(
                    "flooded_atrium",
                    4,
                    48,
                    48,
                    16,
                    2,
                    20,
                    -1,
                    0,
                    Set.of(RegionCatalog.FLOODED_ID),
                    LandmarkDefinition.Style.FLOODED_ATRIUM,
                    Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH)),
            create(
                    "abandoned_station",
                    4,
                    48,
                    32,
                    16,
                    1,
                    20,
                    -1,
                    1,
                    Set.of(RegionCatalog.ABANDONED_ID),
                    LandmarkDefinition.Style.ABANDONED_STATION,
                    Set.of(Connector.Direction.EAST, Connector.Direction.WEST)),
            create(
                    "ancient_chamber",
                    3,
                    32,
                    32,
                    16,
                    4,
                    32,
                    -1,
                    1,
                    Set.of(RegionCatalog.ANCIENT_ID),
                    LandmarkDefinition.Style.ANCIENT_CHAMBER,
                    Set.of(Connector.Direction.NORTH, Connector.Direction.EAST,
                            Connector.Direction.SOUTH, Connector.Direction.WEST)),
            create(
                    "corrupted_nexus",
                    2,
                    48,
                    48,
                    32,
                    12,
                    32,
                    0,
                    0,
                    Set.of(RegionCatalog.CORRUPTED_ID),
                    LandmarkDefinition.Style.CORRUPTED_NEXUS,
                    Set.of(Connector.Direction.NORTH, Connector.Direction.EAST,
                            Connector.Direction.SOUTH, Connector.Direction.WEST)));

    private LandmarkCatalog() {
    }

    public static List<LandmarkDefinition> definitions() {
        return DEFINITIONS;
    }

    public static LandmarkDefinition definition(ResourceLocation id) {
        Objects.requireNonNull(id, "id");
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    public static Optional<Instance> select(RandomState randomState, GenerationGrid.Cell originCell) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(originCell, "originCell");
        if (!isSectorOrigin(originCell) || isOriginSector(originCell)) {
            return Optional.empty();
        }
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        return select(
                originCell,
                factory.at(
                        Math.toIntExact(originCell.x()),
                        0,
                        Math.toIntExact(originCell.z())),
                floor -> DepthCatalog.profile(randomState, originCell, floor),
                (profile, floor) -> RegionCatalog.select(
                        randomState,
                        originCell,
                        profile.depth(),
                        floor),
                availableConnections(randomState, originCell));
    }

    public static Optional<Instance> select(long worldSeed, GenerationGrid.Cell originCell) {
        Objects.requireNonNull(originCell, "originCell");
        if (!isSectorOrigin(originCell) || isOriginSector(originCell)) {
            return Optional.empty();
        }
        return select(
                originCell,
                RandomSource.create(GenerationSeeds.landmarkSeed(worldSeed, originCell, 0, 0)),
                floor -> DepthCatalog.profile(worldSeed, originCell, floor),
                (profile, floor) -> RegionCatalog.select(
                        worldSeed,
                        originCell,
                        profile.depth(),
                        floor),
                availableConnections(worldSeed, originCell));
    }

    public static List<Instance> intersecting(RandomState randomState, ChunkPos chunkPos) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(chunkPos, "chunkPos");
        GenerationGrid.Cell center = GenerationGrid.cellForChunk(chunkPos.x, chunkPos.z);
        long sectorX = Math.floorDiv(center.x(), SECTOR_SIZE_CELLS);
        long sectorZ = Math.floorDiv(center.z(), SECTOR_SIZE_CELLS);
        GenerationGrid.Chunk target = new GenerationGrid.Chunk(chunkPos.x, chunkPos.z);
        java.util.ArrayList<Instance> instances = new java.util.ArrayList<>();
        for (long x = sectorX - 1; x <= sectorX + 1; x++) {
            for (long z = sectorZ - 1; z <= sectorZ + 1; z++) {
                GenerationGrid.Cell origin = new GenerationGrid.Cell(
                        x * SECTOR_SIZE_CELLS,
                        z * SECTOR_SIZE_CELLS);
                select(randomState, origin)
                        .filter(instance -> instance.piece().intersects(target))
                        .ifPresent(instances::add);
            }
        }
        return List.copyOf(instances);
    }

    public static List<Instance> intersecting(RandomState randomState, GenerationGrid.Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return intersecting(
                randomState,
                new ChunkPos(Math.toIntExact(chunk.x()), Math.toIntExact(chunk.z())));
    }

    public static List<Instance> intersecting(long worldSeed, GenerationGrid.Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        GenerationGrid.Cell center = chunk.cell();
        long sectorX = Math.floorDiv(center.x(), SECTOR_SIZE_CELLS);
        long sectorZ = Math.floorDiv(center.z(), SECTOR_SIZE_CELLS);
        java.util.ArrayList<Instance> instances = new java.util.ArrayList<>();
        for (long x = sectorX - 1; x <= sectorX + 1; x++) {
            for (long z = sectorZ - 1; z <= sectorZ + 1; z++) {
                GenerationGrid.Cell origin = new GenerationGrid.Cell(
                        x * SECTOR_SIZE_CELLS,
                        z * SECTOR_SIZE_CELLS);
                select(worldSeed, origin)
                        .filter(instance -> instance.piece().intersects(chunk))
                        .ifPresent(instances::add);
            }
        }
        return List.copyOf(instances);
    }

    public static boolean overlaps(Instance instance, GenerationGrid.Bounds bounds) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(bounds, "bounds");
        return instance.piece().bounds().intersects(bounds);
    }

    /** Materialize only the landmark intersection with the target chunk. */
    public static void place(ChunkAccess chunk, Instance instance) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(instance, "instance");
        var bounds = instance.piece().bounds();
        int minChunkX = chunk.getPos().getMinBlockX();
        int minChunkZ = chunk.getPos().getMinBlockZ();
        long minX = Math.max(bounds.minBlockX(), (long) minChunkX);
        long maxX = Math.min(bounds.maxBlockXExclusive(), (long) minChunkX + GenerationGrid.CHUNK_SIZE_BLOCKS);
        long minZ = Math.max(bounds.minBlockZ(), (long) minChunkZ);
        long maxZ = Math.min(bounds.maxBlockZExclusive(), (long) minChunkZ + GenerationGrid.CHUNK_SIZE_BLOCKS);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (long worldZ = minZ; worldZ < maxZ; worldZ++) {
            for (long worldX = minX; worldX < maxX; worldX++) {
                for (int worldY = bounds.minY(); worldY < bounds.maxYExclusive(); worldY++) {
                    BlockState state = blockStateAt(
                            instance,
                            Math.toIntExact(worldX),
                            worldY,
                            Math.toIntExact(worldZ));
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

    public static BlockState blockStateAt(Instance instance, int worldX, int worldY, int worldZ) {
        Objects.requireNonNull(instance, "instance");
        if (!instance.contains(worldX, worldY, worldZ)) {
            return Blocks.AIR.defaultBlockState();
        }
        int localX = Math.toIntExact(worldX - instance.piece().origin().x());
        int localY = worldY - instance.piece().origin().y();
        int localZ = Math.toIntExact(worldZ - instance.piece().origin().z());
        int width = instance.definition().piece().width();
        int depth = instance.definition().piece().depth();
        int height = instance.definition().piece().height();
        BlockState base = baseState(
                instance.definition().style(),
                localX,
                localY,
                localZ,
                width,
                depth,
                height,
                instance.openConnections());
        if (base.isAir()) {
            return instance.region().decorationState(
                    base,
                    localX,
                    localY,
                    localZ,
                    height,
                    worldX,
                    worldY,
                    worldZ);
        }
        return instance.region().paletteState(base, localY, height, localX, localZ);
    }

    private static Optional<Instance> select(
            GenerationGrid.Cell originCell,
            RandomSource random,
            Function<Integer, DepthCatalog.Profile> profileForFloor,
            BiFunction<DepthCatalog.Profile, Integer, RegionDefinition> regionForFloor,
            Set<Connector.Direction> availableConnections) {
        if (random.nextInt(100) >= LANDMARK_CHANCE_PERCENT) {
            return Optional.empty();
        }
        java.util.ArrayList<Option> options = new java.util.ArrayList<>();
        for (int floor = VerticalCatalog.MIN_FLOOR; floor <= VerticalCatalog.MAX_FLOOR; floor++) {
            DepthCatalog.Profile profile = profileForFloor.apply(floor);
            RegionDefinition region = regionForFloor.apply(profile, floor);
            for (LandmarkDefinition definition : DEFINITIONS) {
                if (definition.weight() > 0
                        && definition.eligible(profile.depth(), floor, region.id())
                        && definition.connectionsSatisfied(availableConnections)) {
                    options.add(new Option(definition, floor, profile.depth(), region));
                }
            }
        }
        if (options.isEmpty()) {
            return Optional.empty();
        }
        Option option = weightedChoice(random, options);
        StructurePiece pieceDefinition = option.definition().piece();
        PlacedStructurePiece piece = pieceDefinition.placedAt(
                new StructurePiece.BlockPoint(
                        GenerationGrid.blockOriginX(originCell),
                        VerticalCatalog.floorY(option.floorIndex()),
                        GenerationGrid.blockOriginZ(originCell)),
                StructurePiece.Rotation.NONE,
                StructurePiece.Mirror.NONE);
        if (!GenerationConstraints.LABRINTH.contains(piece.bounds())) {
            return Optional.empty();
        }
        return Optional.of(new Instance(
                option.definition(),
                originCell,
                option.floorIndex(),
                option.depth(),
                option.region(),
                piece,
                toOpenConnections(availableConnections)));
    }

    private static Option weightedChoice(RandomSource random, List<Option> options) {
        int totalWeight = options.stream()
                .mapToInt(option -> option.definition().weight())
                .sum();
        int choice = random.nextInt(totalWeight);
        for (Option option : options) {
            choice -= option.definition().weight();
            if (choice < 0) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }

    private static BlockState baseState(
            LandmarkDefinition.Style style,
            int localX,
            int localY,
            int localZ,
            int width,
            int depth,
            int height,
            Set<Connector.Direction> openConnections) {
        boolean boundary = localX == 0 || localX == width - 1 || localZ == 0 || localZ == depth - 1;
        if (localY == 0) {
            return floor(style);
        }
        if (localY == height - 1) {
            return ceiling(style);
        }
        if (boundary && isOpenFaceCell(
                localX,
                localY,
                localZ,
                width,
                depth,
                openConnections)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (boundary) {
            return wall(style);
        }
        if (style == LandmarkDefinition.Style.CENTRAL_STAIRWELL) {
            return detail(style, localX, localY, localZ, width, depth, height);
        }
        if (isLight(style, localX, localY, localZ, width, depth, height)) {
            return light(style);
        }
        return detail(style, localX, localY, localZ, width, depth, height);
    }

    private static boolean isOpenFaceCell(
            int localX,
            int localY,
            int localZ,
            int width,
            int depth,
            Set<Connector.Direction> openConnections) {
        if (localY < 1 || localY > 4) {
            return false;
        }
        for (Connector.Direction direction : openConnections) {
            int center = direction == Connector.Direction.NORTH
                    || direction == Connector.Direction.SOUTH
                    ? width / 2
                    : depth / 2;
            int across = direction == Connector.Direction.NORTH
                    || direction == Connector.Direction.SOUTH ? localX : localZ;
            boolean atFace = switch (direction) {
                case NORTH -> localZ == 0;
                case EAST -> localX == width - 1;
                case SOUTH -> localZ == depth - 1;
                case WEST -> localX == 0;
                case UP, DOWN -> false;
            };
            if (atFace && Math.abs(across - center) <= 2) {
                return true;
            }
        }
        return false;
    }

    private static BlockState detail(
            LandmarkDefinition.Style style,
            int x,
            int y,
            int z,
            int width,
            int depth,
            int height) {
        int centerX = width / 2;
        int centerZ = depth / 2;
        return switch (style) {
            case CENTRAL_STAIRWELL -> centralStairState(x, y, z, width, depth, height);
            case MASSIVE_STORAGE_COMPLEX -> y == 2
                    && (x == 4 || x == width - 5)
                    && z % 8 == 0
                    ? Blocks.IRON_BLOCK.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            case GENERATOR_ROOM -> x == centerX && z == centerZ && y <= 3
                    ? Blocks.REDSTONE_BLOCK.defaultBlockState()
                    : (x == centerX && z == centerZ && y == 4
                            ? Blocks.GLOWSTONE.defaultBlockState()
                            : Blocks.AIR.defaultBlockState());
            case FLOODED_ATRIUM -> y <= 2
                    ? Blocks.WATER.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            case ABANDONED_STATION -> y == 1
                    && Math.floorMod(x * 31L + z * 13L, 67L) == 0
                    ? Blocks.GRAVEL.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            case ANCIENT_CHAMBER -> x % 16 == 4 && z % 16 == 4 && y < height - 2
                    ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            case CORRUPTED_NEXUS -> (x == centerX || z == centerZ) && y % 4 == 1
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
            case GRAND_HALL -> (x == 8 || x == width - 9) && (z == 8 || z == depth - 9) && y < height - 2
                    ? Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
        };
    }

    private static BlockState centralStairState(
            int x,
            int y,
            int z,
            int width,
            int depth,
            int height) {
        if (y <= 0 || y >= height - 1) {
            return Blocks.AIR.defaultBlockState();
        }
        int[] position = centralStairPosition(y - 1, width, depth);
        if (position[0] != x || position[1] != z) {
            return Blocks.AIR.defaultBlockState();
        }
        return Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, centralStairFacing(y, width, depth))
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static int[] centralStairPosition(int step, int width, int depth) {
        int centerX = width / 2;
        int centerZ = depth / 2;
        int side = Math.max(2, Math.min(7, Math.min(width, depth) / 4));
        int normalized = Math.floorMod(step, side * 4);
        if (normalized <= side) {
            return new int[] {centerX, centerZ - normalized};
        }
        if (normalized <= side * 2) {
            return new int[] {centerX + normalized - side, centerZ - side};
        }
        if (normalized <= side * 3) {
            return new int[] {centerX + side, centerZ - side + normalized - side * 2};
        }
        return new int[] {centerX + side - (normalized - side * 3), centerZ};
    }

    private static Direction centralStairFacing(int y, int width, int depth) {
        int[] current = centralStairPosition(Math.max(0, y - 1), width, depth);
        int[] next = centralStairPosition(y, width, depth);
        int deltaX = Integer.compare(next[0], current[0]);
        int deltaZ = Integer.compare(next[1], current[1]);
        if (deltaX > 0) {
            return Direction.EAST;
        }
        if (deltaX < 0) {
            return Direction.WEST;
        }
        if (deltaZ > 0) {
            return Direction.SOUTH;
        }
        return Direction.NORTH;
    }

    private static boolean isLight(
            LandmarkDefinition.Style style,
            int x,
            int y,
            int z,
            int width,
            int depth,
            int height) {
        if (y != height - 2) {
            return false;
        }
        int spacing = style == LandmarkDefinition.Style.CORRUPTED_NEXUS ? 8 : 12;
        return x > 1 && x < width - 2 && z > 1 && z < depth - 2
                && (x % spacing == spacing / 2 || z % spacing == spacing / 2);
    }

    private static BlockState floor(LandmarkDefinition.Style style) {
        return switch (style) {
            case FLOODED_ATRIUM -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            case MASSIVE_STORAGE_COMPLEX, GENERATOR_ROOM -> Blocks.IRON_BLOCK.defaultBlockState();
            case ANCIENT_CHAMBER -> Blocks.STONE_BRICKS.defaultBlockState();
            case CORRUPTED_NEXUS -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            default -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        };
    }

    private static BlockState wall(LandmarkDefinition.Style style) {
        return switch (style) {
            case FLOODED_ATRIUM -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            case ABANDONED_STATION -> Blocks.CRACKED_DEEPSLATE_BRICKS.defaultBlockState();
            case ANCIENT_CHAMBER -> Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
            case CORRUPTED_NEXUS -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            default -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        };
    }

    private static BlockState ceiling(LandmarkDefinition.Style style) {
        return switch (style) {
            case FLOODED_ATRIUM -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            case ANCIENT_CHAMBER -> Blocks.STONE_BRICKS.defaultBlockState();
            case CORRUPTED_NEXUS -> Blocks.OBSIDIAN.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };
    }

    private static BlockState light(LandmarkDefinition.Style style) {
        return switch (style) {
            case ABANDONED_STATION -> Blocks.SOUL_LANTERN.defaultBlockState();
            case GENERATOR_ROOM -> Blocks.GLOWSTONE.defaultBlockState();
            case CORRUPTED_NEXUS -> Blocks.END_ROD.defaultBlockState();
            default -> Blocks.SEA_LANTERN.defaultBlockState();
        };
    }

    private static LandmarkDefinition create(
            String path,
            int weight,
            int width,
            int depth,
            int height,
            int minDepth,
            int maxDepth,
            int minFloor,
            int maxFloor,
            Set<ResourceLocation> regions,
            LandmarkDefinition.Style style,
            Set<Connector.Direction> requiredConnections) {
        // Landmarks own a complete generation cell. This keeps every side
        // connector on the same 64-block boundary used by ordinary content;
        // a smaller shell would leave its east/south exits stranded inside
        // the owning cell.
        width = GenerationGrid.CELL_SIZE_BLOCKS;
        depth = GenerationGrid.CELL_SIZE_BLOCKS;
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("labrinth", "landmark/" + path);
        StructurePiece piece = StructurePiece.builder(
                        id,
                        ResourceLocation.fromNamespaceAndPath("labrinth", "generated/landmark/" + path),
                        StructurePiece.Kind.LANDMARK,
                        width,
                        height,
                        depth)
                .weight(weight)
                .rarity(StructurePiece.Rarity.VERY_RARE)
                .rotations(EnumSet.of(StructurePiece.Rotation.NONE))
                .mirror(StructurePiece.Mirror.NONE)
                .depthRange(minDepth, maxDepth)
                .allowedRegions(regions)
                .connectors(connectors(width, depth))
                .placementConditions(new StructurePiece.PlacementConditions(2, true))
                .build();
        return new LandmarkDefinition(
                id,
                weight,
                SECTOR_SIZE_CELLS,
                1,
                piece,
                regions,
                minDepth,
                maxDepth,
                minFloor,
                maxFloor,
                requiredConnections,
                style);
    }

    private static List<Connector> connectors(int width, int depth) {
        return List.of(
                connector(Connector.Direction.NORTH, new Connector.Position(width / 2, 1, 0)),
                connector(Connector.Direction.EAST, new Connector.Position(width, 1, depth / 2)),
                connector(Connector.Direction.SOUTH, new Connector.Position(width / 2, 1, depth)),
                connector(Connector.Direction.WEST, new Connector.Position(0, 1, depth / 2)));
    }

    private static Connector connector(Connector.Direction direction, Connector.Position position) {
        return new Connector(
                position,
                direction,
                Connector.Type.STANDARD,
                5,
                4,
                StructurePiece.Rotation.NONE,
                true);
    }

    private static Set<Connector.Direction> toOpenConnections(
            Set<Connector.Direction> availableConnections) {
        return Set.copyOf(availableConnections);
    }

    private static boolean isSectorOrigin(GenerationGrid.Cell cell) {
        return Math.floorMod(cell.x(), SECTOR_SIZE_CELLS) == 0
                && Math.floorMod(cell.z(), SECTOR_SIZE_CELLS) == 0;
    }

    private static boolean isOriginSector(GenerationGrid.Cell cell) {
        return cell.x() == 0 && cell.z() == 0;
    }

    private static Set<Connector.Direction> availableConnections(
            long worldSeed,
            GenerationGrid.Cell cell) {
        GenerationNeighbors neighbors = GenerationNeighbors.forCell(worldSeed, cell);
        EnumSet<Connector.Direction> available = EnumSet.noneOf(Connector.Direction.class);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            if (neighbors.hasConnection(direction)) {
                available.add(toConnectorDirection(direction));
            }
        }
        return Set.copyOf(available);
    }

    private static Set<Connector.Direction> availableConnections(
            RandomState randomState,
            GenerationGrid.Cell cell) {
        GenerationNeighbors neighbors = GenerationNeighbors.forCell(randomState, cell);
        EnumSet<Connector.Direction> available = EnumSet.noneOf(Connector.Direction.class);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            if (neighbors.hasConnection(direction)) {
                available.add(toConnectorDirection(direction));
            }
        }
        return Set.copyOf(available);
    }

    private static Connector.Direction toConnectorDirection(GenerationGrid.Direction direction) {
        return switch (direction) {
            case NORTH -> Connector.Direction.NORTH;
            case EAST -> Connector.Direction.EAST;
            case SOUTH -> Connector.Direction.SOUTH;
            case WEST -> Connector.Direction.WEST;
        };
    }

    private record Option(
            LandmarkDefinition definition,
            int floorIndex,
            int depth,
            RegionDefinition region) {
    }

    public record Instance(
            LandmarkDefinition definition,
            GenerationGrid.Cell originCell,
            int floorIndex,
            int depth,
            RegionDefinition region,
            PlacedStructurePiece piece,
            Set<Connector.Direction> openConnections) {
        public Instance {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(originCell, "originCell");
            Objects.requireNonNull(region, "region");
            Objects.requireNonNull(openConnections, "openConnections");
            openConnections = Set.copyOf(openConnections);
            if (!definition.eligible(depth, floorIndex, region.id())) {
                throw new IllegalArgumentException("landmark instance is outside its definition gates");
            }
            Objects.requireNonNull(piece, "piece");
            if (!piece.definition().id().equals(definition.id())) {
                throw new IllegalArgumentException("landmark instance piece does not match definition");
            }
        }

        public boolean contains(int worldX, int worldY, int worldZ) {
            var bounds = piece.bounds();
            return worldX >= bounds.minBlockX()
                    && worldX < bounds.maxBlockXExclusive()
                    && worldY >= bounds.minY()
                    && worldY < bounds.maxYExclusive()
                    && worldZ >= bounds.minBlockZ()
                    && worldZ < bounds.maxBlockZExclusive();
        }
    }
}
