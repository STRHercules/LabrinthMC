package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.corridor.CorridorCatalog;
import com.labrinthmc.labrinth.world.corridor.CorridorSelectionConfig;
import com.labrinthmc.labrinth.world.room.RoomCatalog;
import com.labrinthmc.labrinth.world.room.RoomKind;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Deterministic room/corridor selection and chunk-local materialization. */
public final class LabrinthContentCatalog {
    public static final int ROOM_CHANCE_PERCENT = 36;
    public static final int DEFAULT_DEPTH = 0;
    public static final ResourceLocation DEFAULT_REGION = RoomCatalog.STANDARD_REGION;

    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "content_selection");
    private static final long SELECTION_LOCAL_X = 0x434F4E54454E54L;
    private static final long SELECTION_LOCAL_Z = 0x53454C454354L;

    private LabrinthContentCatalog() {
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig) {
        return select(
                randomState,
                cell,
                corridorConfig,
                DEFAULT_DEPTH,
                RegionCatalog.select(randomState, cell, DEFAULT_DEPTH, 0).id(),
                0);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region) {
        return select(randomState, cell, corridorConfig, depth, region, 0);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(corridorConfig, "corridorConfig");
        Objects.requireNonNull(region, "region");
        validateFloor(floorIndex);
        validateDepth(depth);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        RandomSource random = factory.at(
                Math.toIntExact(cell.x()),
                Math.addExact(depth, floorIndex),
                Math.toIntExact(cell.z()));
        if (isRoomCell(cell, random)) {
            return new Selection(
                    null,
                    RoomCatalog.select(randomState, cell, depth, region, floorIndex));
        }
        return new Selection(
                CorridorCatalog.select(randomState, cell, corridorConfig, depth, floorIndex, region),
                null);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig) {
        return select(
                worldSeed,
                cell,
                corridorConfig,
                DEFAULT_DEPTH,
                RegionCatalog.select(worldSeed, cell, DEFAULT_DEPTH, 0).id(),
                0);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region) {
        return select(worldSeed, cell, corridorConfig, depth, region, 0);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(corridorConfig, "corridorConfig");
        Objects.requireNonNull(region, "region");
        validateFloor(floorIndex);
        validateDepth(depth);
        RandomSource random = RandomSource.create(GenerationSeeds.contentSeed(
                worldSeed,
                cell,
                SELECTION_LOCAL_X + depth + floorIndex,
                SELECTION_LOCAL_Z + floorIndex));
        if (isRoomCell(cell, random)) {
            return new Selection(
                    null,
                    RoomCatalog.select(worldSeed, cell, depth, region, floorIndex));
        }
        return new Selection(
                CorridorCatalog.select(worldSeed, cell, corridorConfig, depth, floorIndex, region),
                null);
    }

    /**
     * Keep ordinary room variety where possible, but adapt incompatible
     * choices to the immutable edge graph so an open edge is never accidental.
     */
    private static Selection selectForConnections(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region,
            int floorIndex,
            Set<GenerationGrid.Direction> requiredDirections) {
        Selection selected = select(randomState, cell, corridorConfig, depth, region, floorIndex);
        if (supportsConnections(selected, cell, requiredDirections)) {
            return selected;
        }
        return new Selection(
                CorridorCatalog.selectForConnections(
                        randomState,
                        cell,
                        corridorConfig,
                        depth,
                        floorIndex,
                        region,
                        requiredDirections),
                null);
    }

    private static Selection selectForConnections(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region,
            int floorIndex,
            Set<GenerationGrid.Direction> requiredDirections) {
        Selection selected = select(worldSeed, cell, corridorConfig, depth, region, floorIndex);
        if (supportsConnections(selected, cell, requiredDirections)) {
            return selected;
        }
        return new Selection(
                CorridorCatalog.selectForConnections(
                        worldSeed,
                        cell,
                        corridorConfig,
                        depth,
                        floorIndex,
                        region,
                        requiredDirections),
                null);
    }

    private static boolean supportsConnections(
            Selection selection,
            GenerationGrid.Cell cell,
            Set<GenerationGrid.Direction> requiredDirections) {
        return selection.connectorDirections().containsAll(requiredDirections)
                && requiredDirections.stream().allMatch(direction ->
                        GenerationConnectionRules.hasBoundaryConnector(
                                selection.piece(),
                                cell,
                                direction));
    }

    /**
     * Select the current cell and its four direct neighbors only. This keeps
     * connection decisions bounded and makes the minimum-corner cell the sole
     * owner of the current piece.
     */
    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig) {
        return placementAtFixedDepth(randomState, cell, corridorConfig, DEFAULT_DEPTH, 0);
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region) {
        return placement(randomState, cell, corridorConfig, depth, region, 0);
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            int floorIndex) {
        RegionDefinition currentRegion = RegionCatalog.select(randomState, cell, depth, floorIndex);
        return placement(
                randomState,
                cell,
                corridorConfig,
                depth,
                floorIndex,
                currentRegion,
                neighbor -> DepthCatalog.depthAt(randomState, neighbor, floorIndex),
                neighbor -> RegionCatalog.select(
                        randomState,
                        neighbor,
                        DepthCatalog.depthAt(randomState, neighbor, floorIndex),
                        floorIndex));
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        RegionDefinition currentRegion = RegionCatalog.resolve(region);
        return placement(
                randomState,
                cell,
                corridorConfig,
                depth,
                floorIndex,
                currentRegion,
                ignored -> depth,
                ignored -> currentRegion);
    }

    private static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            int floorIndex,
            RegionDefinition currentRegion,
            Function<GenerationGrid.Cell, Integer> depthForCell,
            Function<GenerationGrid.Cell, RegionDefinition> regionForCell) {
        Set<GenerationGrid.Direction> requestedConnections =
                GenerationNeighbors.forCell(randomState, cell, floorIndex).connected();
        Selection selection = selectForConnections(
                randomState,
                cell,
                corridorConfig,
                depth,
                currentRegion.id(),
                floorIndex,
                requestedConnections);
        EnumSet<GenerationGrid.Direction> open = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : requestedConnections) {
            GenerationGrid.Cell neighborCell = cell.neighbor(direction);
            Optional<Connector> specialConnection = SpecialStructureCatalog.connectionAt(
                    randomState, cell, direction, floorIndex);
            if (specialConnection.isPresent()) {
                if (compatibleSpecialConnection(
                        selection.piece(), cell, direction, specialConnection.get())) {
                    open.add(direction);
                }
                continue;
            }
            RegionDefinition neighborRegion = regionForCell.apply(neighborCell);
            int neighborDepth = depthForCell.apply(neighborCell);
            Selection neighbor = selectForConnections(
                    randomState,
                    neighborCell,
                    corridorConfig,
                    neighborDepth,
                    neighborRegion.id(),
                    floorIndex,
                    GenerationNeighbors.forCell(randomState, neighborCell, floorIndex).connected());
            if (GenerationConnectionRules.compatible(
                    selection.piece(),
                    cell,
                    direction,
                    neighbor.piece(),
                    neighborCell)) {
                open.add(direction);
            }
        }
        return new Placement(selection, open, currentRegion, depth);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig) {
        return placementAtFixedDepth(worldSeed, cell, corridorConfig, DEFAULT_DEPTH, 0);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region) {
        return placement(worldSeed, cell, corridorConfig, depth, region, 0);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            int floorIndex) {
        RegionDefinition currentRegion = RegionCatalog.select(worldSeed, cell, depth, floorIndex);
        return placement(
                worldSeed,
                cell,
                corridorConfig,
                depth,
                floorIndex,
                currentRegion,
                neighbor -> DepthCatalog.depthAt(worldSeed, neighbor, floorIndex),
                neighbor -> RegionCatalog.select(
                        worldSeed,
                        neighbor,
                        DepthCatalog.depthAt(worldSeed, neighbor, floorIndex),
                        floorIndex));
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            ResourceLocation region,
            int floorIndex) {
        RegionDefinition currentRegion = RegionCatalog.resolve(region);
        return placement(
                worldSeed,
                cell,
                corridorConfig,
                depth,
                floorIndex,
                currentRegion,
                ignored -> depth,
                ignored -> currentRegion);
    }

    private static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            int floorIndex,
            RegionDefinition currentRegion,
            Function<GenerationGrid.Cell, Integer> depthForCell,
            Function<GenerationGrid.Cell, RegionDefinition> regionForCell) {
        Set<GenerationGrid.Direction> requestedConnections =
                GenerationNeighbors.forCell(worldSeed, cell, floorIndex).connected();
        Selection selection = selectForConnections(
                worldSeed,
                cell,
                corridorConfig,
                depth,
                currentRegion.id(),
                floorIndex,
                requestedConnections);
        EnumSet<GenerationGrid.Direction> open = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : requestedConnections) {
            GenerationGrid.Cell neighborCell = cell.neighbor(direction);
            Optional<Connector> specialConnection = SpecialStructureCatalog.connectionAt(
                    worldSeed, cell, direction, floorIndex);
            if (specialConnection.isPresent()) {
                if (compatibleSpecialConnection(
                        selection.piece(), cell, direction, specialConnection.get())) {
                    open.add(direction);
                }
                continue;
            }
            RegionDefinition neighborRegion = regionForCell.apply(neighborCell);
            int neighborDepth = depthForCell.apply(neighborCell);
            Selection neighbor = selectForConnections(
                    worldSeed,
                    neighborCell,
                    corridorConfig,
                    neighborDepth,
                    neighborRegion.id(),
                    floorIndex,
                    GenerationNeighbors.forCell(worldSeed, neighborCell, floorIndex).connected());
            if (GenerationConnectionRules.compatible(
                    selection.piece(),
                    cell,
                    direction,
                    neighbor.piece(),
                    neighborCell)) {
                open.add(direction);
            }
        }
        return new Placement(selection, open, currentRegion, depth);
    }

    /** Preserve the legacy no-depth API's symmetric fixed-depth contract. */
    private static Placement placementAtFixedDepth(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            int floorIndex) {
        RegionDefinition currentRegion = RegionCatalog.select(randomState, cell, depth, floorIndex);
        return placement(
                randomState,
                cell,
                corridorConfig,
                depth,
                floorIndex,
                currentRegion,
                ignored -> depth,
                neighbor -> RegionCatalog.select(randomState, neighbor, depth, floorIndex));
    }

    /** Preserve the legacy no-depth API's symmetric fixed-depth contract. */
    private static Placement placementAtFixedDepth(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig corridorConfig,
            int depth,
            int floorIndex) {
        RegionDefinition currentRegion = RegionCatalog.select(worldSeed, cell, depth, floorIndex);
        return placement(
                worldSeed,
                cell,
                corridorConfig,
                depth,
                floorIndex,
                currentRegion,
                ignored -> depth,
                neighbor -> RegionCatalog.select(worldSeed, neighbor, depth, floorIndex));
    }

    private static boolean compatibleSpecialConnection(
            PlacedStructurePiece ordinaryPiece,
            GenerationGrid.Cell ordinaryCell,
            GenerationGrid.Direction direction,
            Connector specialConnection) {
        Connector.Direction ordinaryDirection = switch (direction) {
            case NORTH -> Connector.Direction.NORTH;
            case EAST -> Connector.Direction.EAST;
            case SOUTH -> Connector.Direction.SOUTH;
            case WEST -> Connector.Direction.WEST;
        };
        return GenerationConnectionRules.hasBoundaryConnector(ordinaryPiece, ordinaryCell, direction)
                && ordinaryPiece.connectors().stream()
                .filter(connector -> connector.direction() == ordinaryDirection)
                .anyMatch(connector -> connector.position().equals(specialConnection.position())
                        && connector.compatibleWith(specialConnection));
    }

    public static void place(ChunkAccess chunk, Placement placement) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(placement, "placement");
        if (placement.selection().isRoom()) {
            RoomCatalog.place(chunk, new RoomCatalog.Placement(
                    placement.selection().room(),
                    placement.openDirections()),
                    placement.region());
        } else {
            CorridorCatalog.place(chunk, new CorridorCatalog.Placement(
                    placement.selection().corridor(),
                    placement.openDirections()),
                    placement.region());
        }
        placeDecorations(chunk, placement);
    }

    public static BlockState blockStateAt(
            Placement placement,
            int worldX,
            int worldY,
            int worldZ) {
        Objects.requireNonNull(placement, "placement");
        RegionDefinition region = placement.region();
        BlockState state;
        if (placement.selection().isRoom()) {
            state = RoomCatalog.blockStateAt(
                    new RoomCatalog.Placement(placement.selection().room(), placement.openDirections()),
                    worldX,
                    worldY,
                    worldZ,
                    region);
        } else {
            state = CorridorCatalog.blockStateAt(
                    new CorridorCatalog.Placement(placement.selection().corridor(), placement.openDirections()),
                    worldX,
                    worldY,
                    worldZ,
                    region);
        }
        var bounds = placement.piece().bounds();
        if (state.isAir()
                && worldX >= bounds.minBlockX() && worldX < bounds.maxBlockXExclusive()
                && worldZ >= bounds.minBlockZ() && worldZ < bounds.maxBlockZExclusive()
                && worldY >= bounds.minY() && worldY < bounds.maxYExclusive()
                && hasWalkableFloor(placement, worldX, bounds.minY(), worldZ)) {
            int localX = Math.toIntExact(worldX - placement.piece().origin().x());
            int localY = worldY - placement.piece().origin().y();
            int localZ = Math.toIntExact(worldZ - placement.piece().origin().z());
            return region.decorationState(
                    state,
                    localX,
                    localY,
                    localZ,
                    placement.piece().definition().height(),
                    worldX,
                    worldY,
                    worldZ);
        }
        return state;
    }

    /** Keep generic dressing on authored walkable geometry, never empty shell space. */
    private static boolean hasWalkableFloor(
            Placement placement,
            int worldX,
            int floorY,
            int worldZ) {
        BlockState floor = placement.selection().isRoom()
                ? RoomCatalog.blockStateAt(
                        new RoomCatalog.Placement(
                                placement.selection().room(),
                                placement.openDirections()),
                        worldX,
                        floorY,
                        worldZ,
                        placement.region())
                : CorridorCatalog.blockStateAt(
                        new CorridorCatalog.Placement(
                                placement.selection().corridor(),
                                placement.openDirections()),
                        worldX,
                        floorY,
                        worldZ,
                        placement.region());
        return !floor.isAir();
    }

    private static void placeDecorations(ChunkAccess chunk, Placement placement) {
        if (!placement.region().decorationRules().enabled()) {
            return;
        }
        var bounds = placement.piece().bounds();
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        long minX = Math.max(bounds.minBlockX(), (long) chunkMinX);
        long maxX = Math.min(bounds.maxBlockXExclusive(), (long) chunkMinX + GenerationGrid.CHUNK_SIZE_BLOCKS);
        long minZ = Math.max(bounds.minBlockZ(), (long) chunkMinZ);
        long maxZ = Math.min(bounds.maxBlockZExclusive(), (long) chunkMinZ + GenerationGrid.CHUNK_SIZE_BLOCKS);
        int minY = Math.max(bounds.minY(), Math.toIntExact(placement.piece().origin().y()) + 1);
        int maxY = Math.min(bounds.maxYExclusive(),
                Math.toIntExact(placement.piece().origin().y()) + placement.piece().definition().height() - 1);
        net.minecraft.core.BlockPos.MutableBlockPos blockPos = new net.minecraft.core.BlockPos.MutableBlockPos();
        for (long worldZ = minZ; worldZ < maxZ; worldZ++) {
            for (long worldX = minX; worldX < maxX; worldX++) {
                for (int worldY = minY; worldY < maxY; worldY++) {
                    BlockState state = blockStateAt(
                            placement,
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

    private static boolean isRoomCell(GenerationGrid.Cell cell, RandomSource random) {
        // Keep the origin as the known corridor anchor used by spawn setup.
        return (cell.x() != 0 || cell.z() != 0)
                && random.nextInt(100) < ROOM_CHANCE_PERCENT;
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

    public record Selection(
            CorridorCatalog.Selection corridor,
            RoomCatalog.Selection room) {
        public Selection {
            if ((corridor == null) == (room == null)) {
                throw new IllegalArgumentException("content selection must contain exactly one piece kind");
            }
        }

        public boolean isRoom() {
            return room != null;
        }

        public boolean isCorridor() {
            return corridor != null;
        }

        public PlacedStructurePiece piece() {
            return isRoom() ? room.piece() : corridor.piece();
        }

        public Set<GenerationGrid.Direction> connectorDirections() {
            return isRoom() ? room.connectorDirections() : corridor.connectorDirections();
        }

        public ResourceLocation id() {
            return piece().definition().id();
        }

        public RoomKind roomKind() {
            return isRoom() ? room.kind() : null;
        }
    }

    public record Placement(
            Selection selection,
            Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region,
            int depth) {
        public Placement(Selection selection, Set<GenerationGrid.Direction> openDirections) {
            this(selection, openDirections, RegionCatalog.standard(), DepthCatalog.MIN_DEPTH);
        }

        public Placement(
                Selection selection,
                Set<GenerationGrid.Direction> openDirections,
                RegionDefinition region) {
            this(selection, openDirections, region, DepthCatalog.MIN_DEPTH);
        }

        public Placement {
            Objects.requireNonNull(selection, "selection");
            openDirections = Set.copyOf(openDirections);
            Objects.requireNonNull(region, "region");
            validateDepth(depth);
        }

        public int lootRarityBonusPercent() {
            return DepthCatalog.profileForDepth(depth).lootBonusPercent()
                    + region.lootModifiers().rarityBonusPercent();
        }

        public int entitySpawnWeight() {
            if (!region.mobRules().naturalSpawnsAllowed()) {
                return 0;
            }
            long weight = (long) region.mobRules().spawnWeight()
                    * DepthCatalog.profileForDepth(depth).entitySpawnMultiplierPercent()
                    / 100L;
            return Math.toIntExact(Math.min(Integer.MAX_VALUE, weight));
        }

        public int hazardMultiplierPercent() {
            return DepthCatalog.profileForDepth(depth).hazardMultiplierPercent();
        }

        public int ambientIntensityPercent() {
            int regionContribution = Math.round(
                    region.ambientProperties().ambientLight() * 100.0F);
            return Math.min(
                    200,
                    DepthCatalog.profileForDepth(depth).ambientIntensityPercent()
                            + regionContribution);
        }

        public int unusualnessPercent() {
            return DepthCatalog.profileForDepth(depth).unusualnessPercent();
        }

        public PlacedStructurePiece piece() {
            return selection.piece();
        }

        public boolean isRoom() {
            return selection.isRoom();
        }

        public boolean isCorridor() {
            return selection.isCorridor();
        }
    }
}
