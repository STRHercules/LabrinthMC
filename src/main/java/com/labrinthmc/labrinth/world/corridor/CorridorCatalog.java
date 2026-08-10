package com.labrinthmc.labrinth.world.corridor;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.GenerationConnectionRules;
import com.labrinthmc.labrinth.world.generation.DepthCatalog;
import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.GenerationSeeds;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import com.labrinthmc.labrinth.world.generation.VerticalCatalog;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Deterministic Phase 4 catalog, selector, connector matcher, and dispatcher. */
public final class CorridorCatalog {
    public static final CorridorSelectionConfig DEFAULT_CONFIG = CorridorSelectionConfig.defaults();

    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "corridor_selection");
    private static final long SELECTION_LOCAL_X = 0x53454C454354L;
    private static final long SELECTION_LOCAL_Z = 0x504945434553L;
    private static final List<Option> OPTIONS = Arrays.stream(CorridorKind.values())
            .map(kind -> option(kind, definitionFor(kind)))
            .toList();

    private CorridorCatalog() {
    }

    public static List<StructurePiece> definitions() {
        return OPTIONS.stream().map(Option::definition).toList();
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config) {
        return select(randomState, cell, config, 0, RegionCatalog.STANDARD_ID);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int floorIndex) {
        return select(randomState, cell, config, floorIndex, RegionCatalog.STANDARD_ID);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int floorIndex,
            ResourceLocation region) {
        return select(randomState, cell, config, 0, floorIndex, region);
    }

    public static Selection select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int depth,
            int floorIndex,
            ResourceLocation region) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(region, "region");
        validateFloor(floorIndex);
        validateDepth(depth);
        validateRegion(region, depth, floorIndex);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        return select(
                factory,
                cell,
                config,
                region,
                depth,
                Math.toIntExact(cell.x()),
                floorIndex,
                Math.toIntExact(cell.z()));
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config) {
        return select(worldSeed, cell, config, 0, RegionCatalog.STANDARD_ID);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int floorIndex) {
        return select(worldSeed, cell, config, floorIndex, RegionCatalog.STANDARD_ID);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int floorIndex,
            ResourceLocation region) {
        return select(worldSeed, cell, config, 0, floorIndex, region);
    }

    public static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int depth,
            int floorIndex,
            ResourceLocation region) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(region, "region");
        validateFloor(floorIndex);
        validateDepth(depth);
        validateRegion(region, depth, floorIndex);
        return select(
                worldSeed,
                cell,
                config,
                region,
                depth,
                Math.toIntExact(cell.x()),
                floorIndex,
                Math.toIntExact(cell.z()));
    }

    /**
     * Select a corridor pose that can honor the already-decided cell edges.
     * Ordinary weighted selection is intentionally separate; this path is the
     * connection adapter used when a room or corridor shape cannot satisfy the
     * shared edge graph.
     */
    public static Selection selectForConnections(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int depth,
            int floorIndex,
            ResourceLocation region,
            Set<GenerationGrid.Direction> requiredDirections) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(requiredDirections, "requiredDirections");
        validateFloor(floorIndex);
        validateDepth(depth);
        validateRegion(region, depth, floorIndex);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        RandomSource random = factory.at(
                Math.toIntExact(cell.x()),
                Math.addExact(depth, floorIndex),
                Math.toIntExact(cell.z()));
        return selectForConnections(
                cell,
                config,
                depth,
                floorIndex,
                region,
                requiredDirections,
                random);
    }

    /** Seed-only equivalent used by framework-free validation and tooling. */
    public static Selection selectForConnections(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int depth,
            int floorIndex,
            ResourceLocation region,
            Set<GenerationGrid.Direction> requiredDirections) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(region, "region");
        Objects.requireNonNull(requiredDirections, "requiredDirections");
        validateFloor(floorIndex);
        validateDepth(depth);
        validateRegion(region, depth, floorIndex);
        return selectForConnections(
                cell,
                config,
                depth,
                floorIndex,
                region,
                requiredDirections,
                RandomSource.create(GenerationSeeds.corridorSeed(
                        worldSeed,
                        cell,
                        SELECTION_LOCAL_X + depth,
                        SELECTION_LOCAL_Z + floorIndex)));
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config) {
        return placement(randomState, cell, config, 0);
    }

    public static Placement placement(
            RandomState randomState,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int floorIndex) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(config, "config");
        validateFloor(floorIndex);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        Selection selection = select(
                factory,
                cell,
                config,
                RegionCatalog.STANDARD_ID,
                0,
                Math.toIntExact(cell.x()),
                floorIndex,
                Math.toIntExact(cell.z()));
        EnumSet<GenerationGrid.Direction> open = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : selection.connectorDirections()) {
            Selection neighbor = select(
                    factory,
                    cell.neighbor(direction),
                    config,
                    RegionCatalog.STANDARD_ID,
                    0,
                    Math.toIntExact(cell.x() + directionOffsetX(direction)),
                    floorIndex,
                    Math.toIntExact(cell.z() + directionOffsetZ(direction)));
            if (GenerationConnectionRules.compatible(
                    selection.piece(),
                    cell,
                    direction,
                    neighbor.piece(),
                    cell.neighbor(direction))) {
                open.add(direction);
            }
        }
        return new Placement(selection, open);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config) {
        return placement(worldSeed, cell, config, 0);
    }

    public static Placement placement(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int floorIndex) {
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(config, "config");
        validateFloor(floorIndex);
        Selection selection = select(worldSeed, cell, config, floorIndex);
        EnumSet<GenerationGrid.Direction> open = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : selection.connectorDirections()) {
            Selection neighbor = select(worldSeed, cell.neighbor(direction), config, floorIndex);
            if (GenerationConnectionRules.compatible(
                    selection.piece(),
                    cell,
                    direction,
                    neighbor.piece(),
                    cell.neighbor(direction))) {
                open.add(direction);
            }
        }
        return new Placement(selection, open);
    }

    public static void place(ChunkAccess chunk, Placement placement) {
        place(chunk, placement, RegionCatalog.standard());
    }

    public static void place(
            ChunkAccess chunk,
            Placement placement,
            RegionDefinition region) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(placement, "placement");
        Objects.requireNonNull(region, "region");
        PlacedStructurePiece piece = placement.piece();
        if (StraightCorridor.supports(piece.definition())) {
            StraightCorridor.place(chunk, piece, placement.openDirections(), region);
        } else if (CorridorVariants.supports(piece.definition())) {
            CorridorVariants.place(chunk, piece, placement.openDirections(), region);
        } else if (HallwayVariants.supports(piece.definition())) {
            HallwayVariants.place(chunk, piece, placement.openDirections(), region);
        } else {
            throw new IllegalArgumentException("unknown corridor definition: " + piece.definition().id());
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
        PlacedStructurePiece piece = placement.piece();
        if (StraightCorridor.supports(piece.definition())) {
            return StraightCorridor.blockStateAt(
                    piece,
                    worldX,
                    worldY,
                    worldZ,
                    placement.openDirections(),
                    region);
        }
        if (CorridorVariants.supports(piece.definition())) {
            return CorridorVariants.blockStateAt(
                    piece,
                    worldX,
                    worldY,
                    worldZ,
                    placement.openDirections(),
                    region);
        }
        if (HallwayVariants.supports(piece.definition())) {
            return HallwayVariants.blockStateAt(
                    piece,
                    worldX,
                    worldY,
                    worldZ,
                    placement.openDirections(),
                    region);
        }
        throw new IllegalArgumentException("unknown corridor definition: " + piece.definition().id());
    }

    private static StructurePiece definitionFor(CorridorKind kind) {
        return switch (kind) {
            case SHORT_STRAIGHT -> StraightCorridor.shortDefinition();
            case MEDIUM_STRAIGHT -> StraightCorridor.mediumDefinition();
            case LONG_STRAIGHT -> StraightCorridor.longDefinition();
            case LEFT_TURN, RIGHT_TURN, T_JUNCTION, FOUR_WAY, DEAD_END,
                    WIDE_CORRIDOR, NARROW_CORRIDOR -> CorridorVariants.definition(kind);
            default -> HallwayVariants.definition(kind);
        };
    }

    private static Selection select(
            PositionalRandomFactory factory,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            ResourceLocation region,
            int depth,
            int cellX,
            int floorIndex,
            int cellZ) {
        RandomSource random = factory.at(cellX, floorIndex, cellZ);
        Option west = baseChoice(factory.at(cellX - 1, floorIndex, cellZ), config, region, depth);
        Option north = baseChoice(factory.at(cellX, floorIndex, cellZ - 1), config, region, depth);
        return select(cell, config, depth, floorIndex, random, west, north, region);
    }

    private static Selection select(
            long worldSeed,
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            ResourceLocation region,
            int depth,
            int cellX,
            int floorIndex,
            int cellZ) {
        RandomSource random = seededRandom(worldSeed, cell, floorIndex);
        Option west = baseChoice(
                seededRandom(worldSeed, cell.neighbor(GenerationGrid.Direction.WEST), floorIndex),
                config,
                region,
                depth);
        Option north = baseChoice(
                seededRandom(worldSeed, cell.neighbor(GenerationGrid.Direction.NORTH), floorIndex),
                config,
                region,
                depth);
        return select(cell, config, depth, floorIndex, random, west, north, region);
    }

    private static Selection select(
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int depth,
            int floorIndex,
            RandomSource random,
            Option west,
            Option north,
            ResourceLocation region) {
        Option option;
        if (cell.x() == 0 && cell.z() == 0
                && (RegionCatalog.STANDARD_ID.equals(region)
                        || RegionCatalog.resolve(region).allowsCorridor(
                                optionFor(CorridorKind.LONG_STRAIGHT).definition().id()))) {
            // Preserve a walkable anchor even when a caller disables the long weight.
            option = optionFor(CorridorKind.LONG_STRAIGHT);
        } else {
            List<Option> candidates = candidates(random, config, region, depth);
            List<Option> varied = candidates.stream()
                    .filter(candidate -> candidate.kind() != west.kind() && candidate.kind() != north.kind())
                    .toList();
            option = weightedChoice(
                    random,
                    varied.isEmpty() ? candidates : varied,
                    config,
                    depth);
        }
        StructurePiece.Rotation rotation = StructurePiece.Rotation.values()[random.nextInt(4)];
        PlacedStructurePiece piece = option.definition().placedAt(
                originFor(option, cell, rotation, floorIndex),
                rotation,
                StructurePiece.Mirror.NONE);
        EnumSet<GenerationGrid.Direction> directions = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : option.baseDirections()) {
            directions.add(direction.rotated(rotation));
        }
        return new Selection(option.kind(), option.definition(), rotation, piece, directions);
    }

    private static Selection selectForConnections(
            GenerationGrid.Cell cell,
            CorridorSelectionConfig config,
            int depth,
            int floorIndex,
            ResourceLocation region,
            Set<GenerationGrid.Direction> requiredDirections,
            RandomSource random) {
        Set<GenerationGrid.Direction> required = Set.copyOf(requiredDirections);
        List<Option> options = connectionCandidates(config, region, depth, true);
        List<ConnectionPose> exact = connectionPoses(options, required, true, cell, floorIndex);
        List<ConnectionPose> choices = exact;
        if (choices.isEmpty()) {
            choices = connectionPoses(options, required, false, cell, floorIndex);
        }
        if (choices.isEmpty()) {
            // A region may intentionally omit a turn or junction from its
            // visual pool. Structural continuity wins; retain that region's
            // palette while borrowing the smallest compatible shape globally.
            options = connectionCandidates(config, region, depth, false);
            choices = connectionPoses(options, required, false, cell, floorIndex);
        }
        if (choices.isEmpty()) {
            throw new IllegalArgumentException(
                    "no corridor shape can satisfy required connections: " + required);
        }
        ConnectionPose chosen = weightedConnectionChoice(random, choices, config, depth);
        PlacedStructurePiece piece = chosen.option().definition().placedAt(
                originFor(chosen.option(), cell, chosen.rotation(), floorIndex),
                chosen.rotation(),
                StructurePiece.Mirror.NONE);
        return new Selection(
                chosen.option().kind(),
                chosen.option().definition(),
                chosen.rotation(),
                piece,
                chosen.directions());
    }

    private static List<Option> connectionCandidates(
            CorridorSelectionConfig config,
            ResourceLocation region,
            int depth,
            boolean enforceRegion) {
        RegionDefinition regionDefinition = RegionCatalog.resolve(region);
        return OPTIONS.stream()
                .filter(option -> config.weight(option.kind()) > 0)
                .filter(option -> DepthCatalog.corridorWeight(
                        option.kind(),
                        config.weight(option.kind()),
                        depth) > 0)
                .filter(option -> depth >= option.definition().minDepth()
                        && depth <= option.definition().maxDepth())
                .filter(option -> option.baseDirections().size() <= config.maxBranching())
                .filter(option -> !enforceRegion
                        || regionDefinition.allowsCorridor(option.definition().id()))
                .toList();
    }

    private static List<ConnectionPose> connectionPoses(
            List<Option> options,
            Set<GenerationGrid.Direction> required,
            boolean exact,
            GenerationGrid.Cell cell,
            int floorIndex) {
        List<ConnectionPose> poses = new ArrayList<>();
        for (Option option : options) {
            for (StructurePiece.Rotation rotation : StructurePiece.Rotation.values()) {
                if (!option.definition().allowedRotations().contains(rotation)) {
                    continue;
                }
                EnumSet<GenerationGrid.Direction> directions = EnumSet.noneOf(GenerationGrid.Direction.class);
                for (GenerationGrid.Direction direction : option.baseDirections()) {
                    directions.add(direction.rotated(rotation));
                }
                boolean matches = exact
                        ? directions.equals(required)
                        : directions.containsAll(required);
                PlacedStructurePiece placed = option.definition().placedAt(
                        originFor(option, cell, rotation, floorIndex),
                        rotation,
                        StructurePiece.Mirror.NONE);
                if (matches && required.stream().allMatch(direction ->
                        GenerationConnectionRules.hasBoundaryConnector(placed, cell, direction))) {
                    poses.add(new ConnectionPose(option, rotation, Set.copyOf(directions)));
                }
            }
        }
        return poses;
    }

    private static ConnectionPose weightedConnectionChoice(
            RandomSource random,
            List<ConnectionPose> choices,
            CorridorSelectionConfig config,
            int depth) {
        int totalWeight = choices.stream()
                .mapToInt(choice -> DepthCatalog.corridorWeight(
                        choice.option().kind(),
                        choice.option().weight(config),
                        depth))
                .sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("connection choices have no positive weights");
        }
        int choice = random.nextInt(totalWeight);
        for (ConnectionPose candidate : choices) {
            choice -= DepthCatalog.corridorWeight(
                    candidate.option().kind(),
                    candidate.option().weight(config),
                    depth);
            if (choice < 0) {
                return candidate;
            }
        }
        return choices.get(choices.size() - 1);
    }

    private static Option baseChoice(
            RandomSource random,
            CorridorSelectionConfig config,
            ResourceLocation region,
            int depth) {
        return weightedChoice(random, candidates(random, config, region, depth), config, depth);
    }

    private static List<Option> candidates(
            RandomSource random,
            CorridorSelectionConfig config,
            ResourceLocation region,
            int depth) {
        RegionDefinition regionDefinition = RegionCatalog.resolve(region);
        boolean allowDeadEnd = random.nextInt(100) < config.deadEndChancePercent();
        List<Option> candidates = OPTIONS.stream()
                .filter(option -> config.weight(option.kind()) > 0)
                .filter(option -> DepthCatalog.corridorWeight(option.kind(), config.weight(option.kind()), depth) > 0)
                .filter(option -> depth >= option.definition().minDepth()
                        && depth <= option.definition().maxDepth())
                .filter(option -> regionDefinition.allowsCorridor(option.definition().id()))
                .filter(option -> option.baseDirections().size() <= config.maxBranching())
                .filter(option -> allowDeadEnd || option.kind() != CorridorKind.DEAD_END)
                .toList();
        if (!candidates.isEmpty()) {
            return candidates;
        }
        throw new IllegalArgumentException(
                "corridor configuration has no positive-weight choice within its branching/dead-end limits");
    }

    private static Option weightedChoice(
            RandomSource random,
            List<Option> options,
            CorridorSelectionConfig config,
            int depth) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("corridor choice list must not be empty");
        }
        int totalWeight = options.stream()
                .mapToInt(option -> DepthCatalog.corridorWeight(
                        option.kind(),
                        option.weight(config),
                        depth))
                .sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("corridor choice list has no positive weights");
        }
        int choice = random.nextInt(totalWeight);
        for (Option option : options) {
            choice -= DepthCatalog.corridorWeight(option.kind(), option.weight(config), depth);
            if (choice < 0) {
                return option;
            }
        }
        throw new IllegalStateException("weighted corridor choice did not resolve");
    }

    private static StructurePiece.BlockPoint originFor(
            Option option,
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation,
            int floorIndex) {
        return switch (option.kind()) {
            case SHORT_STRAIGHT -> withFloor(StraightCorridor.shortOriginFor(cell, rotation), floorIndex);
            case MEDIUM_STRAIGHT -> withFloor(StraightCorridor.mediumOriginFor(cell, rotation), floorIndex);
            case LONG_STRAIGHT -> withFloor(StraightCorridor.longOriginFor(cell, rotation), floorIndex);
            default -> offsetFloor(
                    centeredOrigin(
                            cell,
                            rotation,
                            option.definition().width(),
                            option.definition().depth(),
                            floorIndex),
                    option.kind());
        };
    }

    private static StructurePiece.BlockPoint offsetFloor(
            StructurePiece.BlockPoint origin,
            CorridorKind kind) {
        return origin.add(0, HallwayVariants.originYOffset(kind), 0);
    }

    private static StructurePiece.BlockPoint centeredOrigin(
            GenerationGrid.Cell cell,
            StructurePiece.Rotation rotation,
            int width,
            int depth,
            int floorIndex) {
        long cellX = GenerationGrid.blockOriginX(cell);
        long cellZ = GenerationGrid.blockOriginZ(cell);
        // Use the cell's integer center (x/z == 32 for a 64-block cell). This
        // keeps odd-width corridor centerlines on the same boundary coordinate
        // as square rooms after rotation.
        int crossAxisOffset = (GenerationGrid.CELL_SIZE_BLOCKS - width + 1) / 2;
        int axisOffset = (GenerationGrid.CELL_SIZE_BLOCKS - depth + 1) / 2;
        return switch (rotation) {
            case NONE, CLOCKWISE_180 -> new StructurePiece.BlockPoint(
                    cellX + crossAxisOffset,
                    VerticalCatalog.floorY(floorIndex),
                    cellZ + axisOffset);
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> new StructurePiece.BlockPoint(
                    cellX + axisOffset,
                    VerticalCatalog.floorY(floorIndex),
                    cellZ + crossAxisOffset);
        };
    }

    private static StructurePiece.BlockPoint withFloor(
            StructurePiece.BlockPoint origin,
            int floorIndex) {
        return new StructurePiece.BlockPoint(origin.x(), VerticalCatalog.floorY(floorIndex), origin.z());
    }

    private static Option option(CorridorKind kind, StructurePiece definition) {
        return new Option(kind, definition, baseDirections(definition));
    }

    private static Option optionFor(CorridorKind kind) {
        return OPTIONS.stream()
                .filter(option -> option.kind() == kind)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("missing corridor option: " + kind));
    }

    private static Set<GenerationGrid.Direction> baseDirections(StructurePiece definition) {
        EnumSet<GenerationGrid.Direction> directions = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (Connector connector : definition.connectors()) {
            directions.add(fromConnectorDirection(connector.direction()));
        }
        return Set.copyOf(directions);
    }

    private static RandomSource seededRandom(
            long worldSeed,
            GenerationGrid.Cell cell,
            int floorIndex) {
        return RandomSource.create(GenerationSeeds.corridorSeed(
                worldSeed,
                cell,
                SELECTION_LOCAL_X + floorIndex,
                SELECTION_LOCAL_Z));
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

    private static int directionOffsetX(GenerationGrid.Direction direction) {
        return switch (direction) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    private static int directionOffsetZ(GenerationGrid.Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
        };
    }

    private static GenerationGrid.Direction fromConnectorDirection(Connector.Direction direction) {
        return switch (direction) {
            case NORTH -> GenerationGrid.Direction.NORTH;
            case EAST -> GenerationGrid.Direction.EAST;
            case SOUTH -> GenerationGrid.Direction.SOUTH;
            case WEST -> GenerationGrid.Direction.WEST;
            case UP, DOWN -> throw new IllegalArgumentException("corridors are horizontal only");
        };
    }

    public record Selection(
            CorridorKind kind,
            StructurePiece definition,
            StructurePiece.Rotation rotation,
            PlacedStructurePiece piece,
            Set<GenerationGrid.Direction> connectorDirections) {
        public Selection {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(rotation, "rotation");
            Objects.requireNonNull(piece, "piece");
            connectorDirections = Set.copyOf(connectorDirections);
        }

        public boolean hasConnector(GenerationGrid.Direction direction) {
            return connectorDirections.contains(direction);
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

        public CorridorKind kind() {
            return selection.kind();
        }
    }

    private record Option(
            CorridorKind kind,
            StructurePiece definition,
            Set<GenerationGrid.Direction> baseDirections) {
        private Option {
            baseDirections = Set.copyOf(baseDirections);
        }

        private int weight(CorridorSelectionConfig config) {
            return config.weight(kind);
        }
    }

    private record ConnectionPose(
            Option option,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> directions) {
    }
}
