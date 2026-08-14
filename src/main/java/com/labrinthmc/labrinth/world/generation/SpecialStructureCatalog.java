package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LadderBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.phys.AABB;

/**
 * Deterministic, origin-owned compound structures and their chunk-local
 * renderer/populator. The reservation is always the complete piece bounds;
 * authored sections never participate in ordinary cell selection.
 */
public final class SpecialStructureCatalog {
    public static final int SECTOR_SIZE_CELLS = 8;
    public static final int CHANCE_PERCENT = 28;

    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "special_structure_selection");
    private static final List<SpecialStructureDefinition> DEFINITIONS = createDefinitions();

    private SpecialStructureCatalog() {
    }

    public static List<SpecialStructureDefinition> definitions() {
        return DEFINITIONS;
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
                factory.at(Math.toIntExact(originCell.x()), 0, Math.toIntExact(originCell.z())),
                floor -> DepthCatalog.depthAt(randomState, originCell, floor),
                (cell, floor) -> RegionCatalog.select(randomState, cell,
                        DepthCatalog.depthAt(randomState, cell, floor), floor),
                cell -> GenerationNeighbors.forCell(randomState, cell));
    }

    public static Optional<Instance> select(long worldSeed, GenerationGrid.Cell originCell) {
        Objects.requireNonNull(originCell, "originCell");
        if (!isSectorOrigin(originCell) || isOriginSector(originCell)) {
            return Optional.empty();
        }
        return select(
                originCell,
                RandomSource.create(GenerationSeeds.contentSeed(worldSeed, originCell,
                        0x5350454349414CL, 0x535452554354L)),
                floor -> DepthCatalog.depthAt(worldSeed, originCell, floor),
                (cell, floor) -> RegionCatalog.select(worldSeed, cell,
                        DepthCatalog.depthAt(worldSeed, cell, floor), floor),
                cell -> GenerationNeighbors.forCell(worldSeed, cell));
    }

    public static List<Instance> intersecting(RandomState randomState, ChunkPos chunkPos) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(chunkPos, "chunkPos");
        GenerationGrid.Cell center = GenerationGrid.cellForChunk(chunkPos.x, chunkPos.z);
        GenerationGrid.Chunk target = new GenerationGrid.Chunk(chunkPos.x, chunkPos.z);
        return intersecting(
                center,
                target,
                origin -> select(randomState, origin));
    }

    public static List<Instance> intersecting(long worldSeed, GenerationGrid.Chunk chunk) {
        Objects.requireNonNull(chunk, "chunk");
        return intersecting(
                chunk.cell(),
                chunk,
                origin -> select(worldSeed, origin));
    }

    public static boolean overlaps(Instance instance, GenerationGrid.Bounds bounds) {
        Objects.requireNonNull(instance, "instance");
        Objects.requireNonNull(bounds, "bounds");
        return instance.piece().bounds().intersects(bounds);
    }

    /** Materialize only the target chunk's intersection with one compound. */
    public static void place(ChunkAccess chunk, Instance instance) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(instance, "instance");
        GenerationGrid.Bounds bounds = instance.piece().bounds();
        int chunkMinX = chunk.getPos().getMinBlockX();
        int chunkMinZ = chunk.getPos().getMinBlockZ();
        long minX = Math.max(bounds.minBlockX(), (long) chunkMinX);
        long maxX = Math.min(bounds.maxBlockXExclusive(), (long) chunkMinX + GenerationGrid.CHUNK_SIZE_BLOCKS);
        long minZ = Math.max(bounds.minBlockZ(), (long) chunkMinZ);
        long maxZ = Math.min(bounds.maxBlockZExclusive(), (long) chunkMinZ + GenerationGrid.CHUNK_SIZE_BLOCKS);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (long worldZ = minZ; worldZ < maxZ; worldZ++) {
            for (long worldX = minX; worldX < maxX; worldX++) {
                for (int worldY = bounds.minY(); worldY < bounds.maxYExclusive(); worldY++) {
                    BlockState state = blockStateAt(instance,
                            Math.toIntExact(worldX), worldY, Math.toIntExact(worldZ));
                    if (state.isAir()) {
                        continue;
                    }
                    BlockPos position = blockPos.set(Math.toIntExact(worldX), worldY,
                            Math.toIntExact(worldZ));
                    chunk.setBlockState(position, state, false);
                    if ((state.is(Blocks.CHEST) || state.is(Blocks.BARREL))
                            && instance.definition().lootTable().isPresent()) {
                        setLootTable(chunk, position, state, instance.definition().lootTable().get(),
                                lootSeed(instance, worldX, worldY, worldZ));
                    }
                    if (state.is(Blocks.SPAWNER)) {
                        setSpawnerData(chunk, position, instance.definition().population());
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
        return stateForLocal(instance, localX, localY, localZ);
    }

    static boolean caveVoidAt(Instance instance, int localX, int localY, int localZ) {
        Objects.requireNonNull(instance, "instance");
        return instance.definition().isCave()
                && isCaveVoid(instance, localX, localY, localZ);
    }

    static boolean entranceOpenAt(Instance instance, int localX, int localY, int localZ) {
        Objects.requireNonNull(instance, "instance");
        return isOpenEntrance(instance, localX, localY, localZ);
    }

    /**
     * Return the special-side endpoint for an ordinary cell edge. This lets
     * the normal piece selector open only declared compound entrances.
     */
    public static Optional<Connector> connectionAt(
            RandomState randomState,
            GenerationGrid.Cell outsideCell,
            GenerationGrid.Direction direction,
            int floorIndex) {
        return connectionAt(
                outsideCell,
                direction,
                floorIndex,
                origin -> select(randomState, origin));
    }

    public static Optional<Connector> connectionAt(
            long worldSeed,
            GenerationGrid.Cell outsideCell,
            GenerationGrid.Direction direction,
            int floorIndex) {
        return connectionAt(
                outsideCell,
                direction,
                floorIndex,
                origin -> select(worldSeed, origin));
    }

    /** Bounded development lookup for locating a selected structure without changing generation. */
    public static Optional<Instance> findNearest(
            long worldSeed,
            SpecialStructureDefinition.Theme theme,
            GenerationGrid.Cell center,
            int radius) {
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(center, "center");
        if (radius < 0 || radius > 64) {
            throw new IllegalArgumentException("debug lookup radius must be in 0..64");
        }
        Instance closest = null;
        long bestDistance = Long.MAX_VALUE;
        for (long x = center.x() - radius; x <= center.x() + radius; x++) {
            for (long z = center.z() - radius; z <= center.z() + radius; z++) {
                GenerationGrid.Cell origin = new GenerationGrid.Cell(x, z);
                if (!isSectorOrigin(origin)) {
                    continue;
                }
                Optional<Instance> candidate = select(worldSeed, origin)
                        .filter(instance -> instance.definition().theme() == theme);
                if (candidate.isEmpty()) {
                    continue;
                }
                long distance = Math.abs(x - center.x()) + Math.abs(z - center.z());
                if (distance < bestDistance) {
                    bestDistance = distance;
                    closest = candidate.get();
                }
            }
        }
        return Optional.ofNullable(closest);
    }

    /**
     * Scan only deterministic sector owners and report discoverability without
     * loading or changing any chunks. This is intentionally bounded so it can
     * back an operator command and offline tuning checks safely.
     */
    public static Statistics statistics(
            long worldSeed,
            GenerationGrid.Cell center,
            int radius) {
        Objects.requireNonNull(center, "center");
        if (radius < 0 || radius > 64) {
            throw new IllegalArgumentException("statistics radius must be in 0..64");
        }
        EnumMap<SpecialStructureDefinition.Theme, Integer> byTheme =
                new EnumMap<>(SpecialStructureDefinition.Theme.class);
        EnumMap<LabrinthDiscoveryTier, Integer> byTier =
                new EnumMap<>(LabrinthDiscoveryTier.class);
        for (SpecialStructureDefinition.Theme theme : SpecialStructureDefinition.Theme.values()) {
            byTheme.put(theme, 0);
        }
        for (LabrinthDiscoveryTier tier : LabrinthDiscoveryTier.values()) {
            byTier.put(tier, 0);
        }
        Map<ResourceLocation, Integer> byRegion = new java.util.HashMap<>();
        int candidateOrigins = 0;
        int selectedStructures = 0;
        long totalDistance = 0;
        for (long x = center.x() - radius; x <= center.x() + radius; x++) {
            for (long z = center.z() - radius; z <= center.z() + radius; z++) {
                GenerationGrid.Cell origin = new GenerationGrid.Cell(x, z);
                if (!isSectorOrigin(origin)) {
                    continue;
                }
                candidateOrigins++;
                Optional<Instance> selected = select(worldSeed, origin);
                if (selected.isEmpty()) {
                    continue;
                }
                Instance instance = selected.get();
                selectedStructures++;
                totalDistance += Math.abs(x - center.x()) + Math.abs(z - center.z());
                byTheme.merge(instance.definition().theme(), 1, Integer::sum);
                byTier.merge(instance.definition().tier(), 1, Integer::sum);
                byRegion.merge(instance.region().id(), 1, Integer::sum);
            }
        }
        return new Statistics(
                radius,
                candidateOrigins,
                selectedStructures,
                selectedStructures == 0 ? 0.0 : (double) totalDistance / selectedStructures,
                byTheme,
                byTier,
                byRegion);
    }

    /**
     * Populate deterministic positions from the chunk that owns each entity.
     * The structure decision is still origin-owned; distributing the fixed
     * positions across their chunks keeps WorldGenRegion writes in bounds for
     * large compounds and makes interior populations reachable.
     */
    public static void populate(WorldGenRegion region, long worldSeed) {
        ChunkPos center = region.getCenter();
        GenerationGrid.Chunk centerChunk = new GenerationGrid.Chunk(center.x, center.z);
        for (Instance instance : intersecting(worldSeed, centerChunk)) {
            if (instance.definition().population() == SpecialStructureDefinition.Population.NONE) {
                continue;
            }
            for (BlockPos position : populationPositions(instance)) {
                GenerationGrid.Chunk populationChunk = GenerationGrid.chunkForBlock(
                        position.getX(), position.getZ());
                if (!populationChunk.equals(centerChunk)
                        || populationAlreadyPresent(region.getLevel(), instance, position)) {
                    continue;
                }
                Entity entity = createEntity(instance.definition().population(), region.getLevel());
                if (entity == null) {
                    continue;
                }
                entity.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
                if (entity instanceof Mob mob) {
                    mob.setPersistenceRequired();
                    mob.finalizeSpawn(region, region.getCurrentDifficultyAt(position),
                            MobSpawnType.STRUCTURE, null);
                }
                region.addFreshEntity(entity);
            }
        }
    }

    private static boolean populationAlreadyPresent(
            ServerLevel level, Instance instance, BlockPos position) {
        AABB bounds = new AABB(
                position.getX(), position.getY(), position.getZ(),
                position.getX() + 1, position.getY() + 2, position.getZ() + 1)
                .inflate(0.1D);
        return level.getEntitiesOfClass(Entity.class, bounds).stream()
                .anyMatch(entity -> matchesPopulation(entity, instance.definition().population()));
    }

    private static boolean matchesPopulation(
            Entity entity,
            SpecialStructureDefinition.Population population) {
        return switch (population) {
            case VILLAGERS -> entity.getType() == EntityType.VILLAGER;
            case ZOMBIES -> entity.getType() == EntityType.ZOMBIE;
            case SKELETONS -> entity.getType() == EntityType.SKELETON;
            case PILLAGERS -> entity.getType() == EntityType.PILLAGER;
            case PIGLINS -> entity.getType() == EntityType.PIGLIN;
            case WITHER_SKELETONS -> entity.getType() == EntityType.WITHER_SKELETON;
            case SPIDERS -> entity.getType() == EntityType.SPIDER;
            case NONE -> false;
        };
    }

    private static Optional<Instance> select(
            GenerationGrid.Cell originCell,
            RandomSource random,
            Function<Integer, Integer> depthForFloor,
            java.util.function.BiFunction<GenerationGrid.Cell, Integer, RegionDefinition> regionForCell,
            Function<GenerationGrid.Cell, GenerationNeighbors> neighborsForCell) {
        if (random.nextInt(100) >= CHANCE_PERCENT) {
            return Optional.empty();
        }
        List<Option> options = new ArrayList<>();
        for (int floor = VerticalCatalog.MIN_FLOOR; floor <= VerticalCatalog.MAX_FLOOR; floor++) {
            int depth = depthForFloor.apply(floor);
            RegionDefinition region = regionForCell.apply(originCell, floor);
            for (SpecialStructureDefinition definition : DEFINITIONS) {
                if (definition.weight() <= 0 || !definition.eligible(depth, floor, region.id())) {
                    continue;
                }
                StructurePiece pieceDefinition = definition.piece();
                LabrinthTemplatePiece template = LabrinthTemplatePiece.procedural(pieceDefinition);
                if (!template.eligible(depth, region.id(), floor)) {
                    continue;
                }
                PlacedStructurePiece piece = pieceDefinition.placedAt(
                        new StructurePiece.BlockPoint(
                                GenerationGrid.blockOriginX(originCell),
                                VerticalCatalog.floorY(floor),
                                GenerationGrid.blockOriginZ(originCell)),
                        StructurePiece.Rotation.NONE,
                        StructurePiece.Mirror.NONE);
                if (!GenerationConstraints.LABRINTH.contains(piece.bounds())) {
                    continue;
                }
                Set<Connector> open = openConnectors(piece, originCell, neighborsForCell);
                if (open.size() >= definition.minimumExternalConnections()) {
                    options.add(new Option(definition, floor, depth, region, piece, open));
                }
            }
        }
        if (options.isEmpty()) {
            return Optional.empty();
        }
        Option selectedOption = LabrinthPiecePool.choose(
                random,
                options,
                candidate -> candidate.definition().distributionWeight(candidate.depth()));
        return Optional.of(new Instance(
                selectedOption.definition(),
                originCell,
                selectedOption.floor(),
                selectedOption.depth(),
                selectedOption.region(),
                selectedOption.piece(),
                selectedOption.openConnectors(),
                random.nextLong()));
    }

    private static List<Instance> intersecting(
            GenerationGrid.Cell center,
            GenerationGrid.Chunk target,
            Function<GenerationGrid.Cell, Optional<Instance>> selector) {
        long sectorX = Math.floorDiv(center.x(), SECTOR_SIZE_CELLS);
        long sectorZ = Math.floorDiv(center.z(), SECTOR_SIZE_CELLS);
        List<Instance> result = new ArrayList<>();
        // Definitions are currently <= two cells wide, so one sector ring is sufficient.
        for (long x = sectorX - 1; x <= sectorX + 1; x++) {
            for (long z = sectorZ - 1; z <= sectorZ + 1; z++) {
                GenerationGrid.Cell origin = new GenerationGrid.Cell(
                        x * SECTOR_SIZE_CELLS, z * SECTOR_SIZE_CELLS);
                selector.apply(origin)
                        .filter(instance -> instance.piece().intersects(target))
                        .ifPresent(result::add);
            }
        }
        return List.copyOf(result);
    }

    private static Optional<Connector> connectionAt(
            GenerationGrid.Cell outsideCell,
            GenerationGrid.Direction direction,
            int floorIndex,
            Function<GenerationGrid.Cell, Optional<Instance>> selector) {
        GenerationGrid.Cell center = outsideCell;
        long sectorX = Math.floorDiv(center.x(), SECTOR_SIZE_CELLS);
        long sectorZ = Math.floorDiv(center.z(), SECTOR_SIZE_CELLS);
        Connector.Direction specialDirection = toConnectorDirection(direction.opposite());
        for (long x = sectorX - 1; x <= sectorX + 1; x++) {
            for (long z = sectorZ - 1; z <= sectorZ + 1; z++) {
                GenerationGrid.Cell origin = new GenerationGrid.Cell(
                        x * SECTOR_SIZE_CELLS, z * SECTOR_SIZE_CELLS);
                Optional<Instance> selected = selector.apply(origin)
                        .filter(instance -> instance.floorIndex() == floorIndex);
                if (selected.isEmpty()) {
                    continue;
                }
                for (Connector connector : selected.get().piece().connectors()) {
                    if (!selected.get().openConnectors().contains(connector)
                            || connector.direction() != specialDirection
                            || !matchesBoundary(connector, outsideCell, direction)) {
                        continue;
                    }
                    return Optional.of(connector);
                }
            }
        }
        return Optional.empty();
    }

    private static boolean matchesBoundary(
            Connector connector,
            GenerationGrid.Cell outsideCell,
            GenerationGrid.Direction direction) {
        long cellX = GenerationGrid.blockOriginX(outsideCell);
        long cellZ = GenerationGrid.blockOriginZ(outsideCell);
        long centerX = cellX + GenerationGrid.CELL_SIZE_BLOCKS / 2;
        long centerZ = cellZ + GenerationGrid.CELL_SIZE_BLOCKS / 2;
        return switch (direction) {
            case NORTH -> connector.position().z() == cellZ
                    && connector.position().x() == centerX;
            case EAST -> connector.position().x() == cellX + GenerationGrid.CELL_SIZE_BLOCKS
                    && connector.position().z() == centerZ;
            case SOUTH -> connector.position().z() == cellZ + GenerationGrid.CELL_SIZE_BLOCKS
                    && connector.position().x() == centerX;
            case WEST -> connector.position().x() == cellX
                    && connector.position().z() == centerZ;
        };
    }

    private static Set<Connector> openConnectors(
            PlacedStructurePiece piece,
            GenerationGrid.Cell originCell,
            Function<GenerationGrid.Cell, GenerationNeighbors> neighborsForCell) {
        // Connector is a record, not an enum; use a linked set while retaining declaration order.
        java.util.LinkedHashSet<Connector> result = new java.util.LinkedHashSet<>();
        for (Connector connector : piece.connectors()) {
            GenerationGrid.Direction specialDirection = toGridDirection(connector.direction());
            GenerationGrid.Cell outsideCell = outsideCell(piece, connector);
            if (outsideCell != null
                    && neighborsForCell.apply(outsideCell).hasConnection(specialDirection.opposite())) {
                result.add(connector);
            }
        }
        return Set.copyOf(result);
    }

    private static GenerationGrid.Cell outsideCell(PlacedStructurePiece piece, Connector connector) {
        long x = connector.position().x();
        long z = connector.position().z();
        return switch (connector.direction()) {
            case NORTH -> GenerationGrid.cellForBlock(x, z - 1);
            case EAST -> GenerationGrid.cellForBlock(x + 1, z);
            case SOUTH -> GenerationGrid.cellForBlock(x, z + 1);
            case WEST -> GenerationGrid.cellForBlock(x - 1, z);
            case UP, DOWN -> null;
        };
    }

    private static BlockState stateForLocal(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition definition = instance.definition();
        int width = definition.piece().width();
        int height = definition.piece().height();
        int depth = definition.piece().depth();
        if (y == 0) {
            SpecialStructureDefinition.Section section = sectionAtFloor(instance, x, z);
            if (section != null && section.kind() == SpecialStructureDefinition.SectionKind.FARM) {
                return Blocks.FARMLAND.defaultBlockState();
            }
            return processStructural(instance, floor(definition.theme()), x, y, z);
        }
        if (y == height - 1) {
            return processStructural(instance, ceiling(definition.theme()), x, y, z);
        }
        if (definition.isCave()) {
            if (!isOpenEntrance(instance, x, y, z) && !isCaveVoid(instance, x, y, z)) {
                return caveWall(instance, x, y, z);
            }
            BlockState caveDetail = detail(instance, x, y, z);
            if (!caveDetail.isAir()) {
                return caveDetail;
            }
            return caveInterior(instance, x, y, z);
        }
        if (isOpenEntrance(instance, x, y, z)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (internalRouteAt(instance, x, y, z)) {
            return routeState(instance, x, y, z);
        }
        SpecialStructureDefinition.Section sectionFloor = sectionAtFloor(instance, x, z);
        if (sectionFloor != null && y == sectionFloor.y() - 1) {
            return processStructural(instance, floor(instance.definition().theme()), x, y, z);
        }
        SpecialStructureDefinition.Section sectionRoof = sectionAtRoof(instance, x, y, z);
        if (sectionRoof != null) {
            return processStructural(
                    instance,
                    sectionRoofState(instance.definition().theme(), sectionRoof.kind()),
                    x,
                    y,
                    z);
        }
        boolean boundary = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
        if (boundary) {
            return processStructural(instance, wall(definition.theme(), x, y, z), x, y, z);
        }
        for (SpecialStructureDefinition.Section section : definition.sections()) {
            if (inside(section, x, y, z) && sectionWall(section, x, y, z)) {
                return processStructural(
                        instance,
                        sectionWallState(definition.theme(), section.kind()),
                        x,
                        y,
                        z);
            }
        }
        return detail(instance, x, y, z);
    }

    private static SpecialStructureDefinition.Section sectionAtFloor(
            Instance instance,
            int x,
            int z) {
        for (SpecialStructureDefinition.Section section : instance.definition().sections()) {
            if (x >= section.x() && x < section.x() + section.width()
                    && z >= section.z() && z < section.z() + section.depth()) {
                return section;
            }
        }
        return null;
    }

    private static SpecialStructureDefinition.Section sectionAtRoof(
            Instance instance,
            int x,
            int y,
            int z) {
        for (SpecialStructureDefinition.Section section : instance.definition().sections()) {
            if (x >= section.x() && x < section.x() + section.width()
                    && z >= section.z() && z < section.z() + section.depth()
                    && y == section.y() + section.height() - 1) {
                return section;
            }
        }
        return null;
    }

    /**
     * Connect section centers and declared external doors with a bounded
     * Manhattan graph.  This is the small piece-graph layer the old section
     * rectangles lacked: every room in a compound now belongs to one route,
     * while the outer shell still admits only declared entrances.
     */
    static boolean internalRouteAt(Instance instance, int x, int y, int z) {
        List<SpecialStructureDefinition.Section> sections = instance.definition().sections();
        if (sections.isEmpty()) {
            return false;
        }
        for (int index = 0; index + 1 < sections.size(); index++) {
            if (routeBetween(
                    pointFor(sections.get(index)),
                    pointFor(sections.get(index + 1)),
                    x,
                    y,
                    z)) {
                return true;
            }
        }
        for (Connector connector : instance.openConnectors()) {
            RoutePoint door = new RoutePoint(
                    Math.toIntExact(connector.position().x() - instance.piece().origin().x()),
                    1,
                    Math.toIntExact(connector.position().z() - instance.piece().origin().z()));
            SpecialStructureDefinition.Section closest = sections.get(0);
            int closestDistance = Integer.MAX_VALUE;
            for (SpecialStructureDefinition.Section section : sections) {
                int distance = Math.abs(door.x() - sectionCenterX(section))
                        + Math.abs(door.z() - sectionCenterZ(section));
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closest = section;
                }
            }
            if (routeBetween(door, pointFor(closest), x, y, z)) {
                return true;
            }
        }
        return false;
    }

    private static BlockState routeState(Instance instance, int x, int y, int z) {
        RoutePoint verticalTarget = verticalTarget(instance, x, y, z);
        if (verticalTarget != null) {
            int ladderX = verticalTarget.x() - 2;
            if (x == ladderX && z == verticalTarget.z() && y > 1) {
                return Blocks.LADDER.defaultBlockState()
                        .setValue(LadderBlock.FACING, Direction.EAST);
            }
            if (x == ladderX - 1 && z == verticalTarget.z()) {
                return processStructural(
                        instance,
                        wall(instance.definition().theme(), x, y, z),
                        x,
                        y,
                        z);
            }
        }
        // Routes are evaluated before the detail pass. Preserve a ladder that
        // is an authored vertical connection instead of replacing it with
        // route air or carpet.
        BlockState functionalDetail = detail(instance, x, y, z);
        if (functionalDetail.is(Blocks.LADDER)) {
            return functionalDetail;
        }
        int routeFloor = routeFloorAt(instance, x, z);
        if (y == routeFloor) {
            return routeAccent(instance.definition().theme());
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static RoutePoint verticalTarget(Instance instance, int x, int y, int z) {
        List<SpecialStructureDefinition.Section> sections = instance.definition().sections();
        for (int index = 0; index + 1 < sections.size(); index++) {
            RoutePoint first = pointFor(sections.get(index));
            RoutePoint second = pointFor(sections.get(index + 1));
            if (first.y() == second.y() || !near(second.x(), x, 2)
                    || !near(second.z(), z, 2)) {
                continue;
            }
            int minY = Math.min(first.y(), second.y());
            int maxY = Math.max(first.y(), second.y());
            if (y >= minY && y <= maxY + 3) {
                return second;
            }
        }
        return null;
    }

    private static int routeFloorAt(Instance instance, int x, int z) {
        int best = 1;
        int bestDistance = Integer.MAX_VALUE;
        for (SpecialStructureDefinition.Section section : instance.definition().sections()) {
            int distance = Math.abs(x - sectionCenterX(section))
                    + Math.abs(z - sectionCenterZ(section));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = section.y();
            }
        }
        return best;
    }

    private static boolean routeBetween(RoutePoint first, RoutePoint second,
            int x, int y, int z) {
        int floor = Math.min(first.y(), second.y());
        if (y >= floor && y < floor + 5) {
            boolean firstLeg = near(z, first.z(), 2) && between(x, first.x(), second.x());
            boolean secondLeg = near(x, second.x(), 2) && between(z, first.z(), second.z());
            if (firstLeg || secondLeg) {
                return true;
            }
        }
        return first.y() != second.y()
                && near(x, second.x(), 2)
                && near(z, second.z(), 2)
                && y >= Math.min(first.y(), second.y())
                && y <= Math.max(first.y(), second.y()) + 3;
    }

    private static boolean between(int value, int first, int second) {
        return value >= Math.min(first, second) - 2
                && value <= Math.max(first, second) + 2;
    }

    private static boolean near(int value, int target, int radius) {
        return Math.abs(value - target) <= radius;
    }

    private static RoutePoint pointFor(SpecialStructureDefinition.Section section) {
        return new RoutePoint(sectionCenterX(section), section.y(), sectionCenterZ(section));
    }

    private static int sectionCenterX(SpecialStructureDefinition.Section section) {
        return section.x() + section.width() / 2;
    }

    private static int sectionCenterZ(SpecialStructureDefinition.Section section) {
        return section.z() + section.depth() / 2;
    }

    private static BlockState routeAccent(SpecialStructureDefinition.Theme theme) {
        return switch (theme) {
            case VILLAGE -> Blocks.OAK_SLAB.defaultBlockState();
            case COMPACT_DUNGEON, DUNGEON_COMPLEX, DUNGEON_MEGA ->
                    Blocks.RED_CARPET.defaultBlockState();
            case ZOMBIE_OUTPOST -> Blocks.BROWN_CARPET.defaultBlockState();
            case SKELETON_OUTPOST, ILLAGER_OUTPOST -> Blocks.GRAY_CARPET.defaultBlockState();
            case PIGLIN_OUTPOST, WITHER_SKELETON_OUTPOST -> Blocks.BLACK_CARPET.defaultBlockState();
            case MASSIVE_HALL -> Blocks.PURPLE_CARPET.defaultBlockState();
            default -> Blocks.GRAY_CARPET.defaultBlockState();
        };
    }

    private static BlockState caveWall(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition.Theme theme = instance.definition().theme();
        int variation = Math.floorMod(
                x * 13 + y * 7 + z * 17 + (int) instance.layoutSeed(),
                53);
        BlockState base = wall(theme, x, y, z);
        if (theme == SpecialStructureDefinition.Theme.CAVE_POCKET) {
            if (variation == 0) {
                return Blocks.IRON_ORE.defaultBlockState();
            }
            if (variation <= 2) {
                return Blocks.COAL_ORE.defaultBlockState();
            }
            if (variation == 3) {
                return Blocks.COPPER_ORE.defaultBlockState();
            }
        }
        if (theme == SpecialStructureDefinition.Theme.ANCIENT_CAVE && variation == 0) {
            return Blocks.GOLD_ORE.defaultBlockState();
        }
        return processStructural(instance, base, x, y, z);
    }

    private static BlockState processStructural(
            Instance instance,
            BlockState state,
            int x,
            int y,
            int z) {
        if (state.isAir()) {
            return state;
        }
        LabrinthProcessorSet processor = processorFor(instance);
        return processor.apply(
                state,
                y,
                instance.definition().piece().height(),
                x,
                z);
    }

    private static LabrinthProcessorSet processorFor(Instance instance) {
        SpecialStructureDefinition.Theme theme = instance.definition().theme();
        if (theme == SpecialStructureDefinition.Theme.PIGLIN_OUTPOST
                || theme == SpecialStructureDefinition.Theme.WITHER_SKELETON_OUTPOST) {
            return LabrinthProcessorSet.NETHER_INFLUENCED;
        }
        return LabrinthProcessorSet.forRegion(instance.region().id());
    }

    /**
     * Generate a cave as a filled bounded volume with an internal void. The
     * volume is reserved before this method is reached, so the irregular shape
     * cannot punch through unrelated Labrinth content or load another chunk.
     */
    private static boolean isCaveVoid(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition.Theme theme = instance.definition().theme();
        int width = instance.definition().piece().width();
        int height = instance.definition().piece().height();
        int depth = instance.definition().piece().depth();
        double centerX = (width - 1) * 0.5;
        double centerY = Math.max(1.0, (height - 1) * 0.5);
        double centerZ = (depth - 1) * 0.5;
        double radiusX = width * switch (theme) {
            case CAVE_POCKET -> 0.30;
            case FLOODED_CAVERN -> 0.44;
            case OVERGROWN_GROTTO -> 0.37;
            case ANCIENT_CAVE -> 0.35;
            default -> 0.40;
        };
        double radiusY = Math.max(2.0, (height - 2) * switch (theme) {
            case CAVE_POCKET -> 0.40;
            case ENORMOUS_CAVE -> 0.56;
            case FLOODED_CAVERN -> 0.42;
            default -> 0.48;
        });
        double radiusZ = depth * switch (theme) {
            case CAVE_POCKET -> 0.30;
            case FLOODED_CAVERN -> 0.34;
            case OVERGROWN_GROTTO -> 0.42;
            case ANCIENT_CAVE -> 0.35;
            default -> 0.40;
        };
        double phase = (instance.layoutSeed() & 0xFFFFL) / 65535.0 * Math.PI * 2.0;
        double noise = caveNoise(instance, x, y, z, phase);
        double warp = switch (theme) {
            case CORRUPTED_CAVE -> 0.14 * noise;
            case ANCIENT_CAVE -> 0.07 * Math.signum(noise);
            case CAVE_POCKET -> 0.04 * noise;
            default -> 0.09 * noise;
        };
        boolean main = ellipsoid(x, y, z, centerX, centerY, centerZ,
                radiusX, radiusY, radiusZ, warp);

        boolean side = false;
        if (theme != SpecialStructureDefinition.Theme.CAVE_POCKET
                && theme != SpecialStructureDefinition.Theme.FLOODED_CAVERN) {
            double sideOffset = width * 0.22;
            side = ellipsoid(x, y, z, centerX - sideOffset, centerY + 0.5,
                    centerZ + depth * 0.14, radiusX * 0.58, radiusY * 0.78,
                    radiusZ * 0.58, warp + 0.04 * noise)
                    || ellipsoid(x, y, z, centerX + sideOffset, centerY - 0.5,
                    centerZ - depth * 0.14, radiusX * 0.52, radiusY * 0.72,
                    radiusZ * 0.52, -warp - 0.03 * noise);
        }
        boolean chamber = theme == SpecialStructureDefinition.Theme.ANCIENT_CAVE
                && ((x / 8 + z / 8) & 1) == 0
                && ellipsoid(x, y, z, centerX, centerY + 1.0, centerZ,
                        radiusX * 0.50, radiusY * 0.62, radiusZ * 0.50, -0.02);
        return main || side || chamber || connectorTunnel(instance, x, y, z);
    }

    private static double caveNoise(Instance instance, int x, int y, int z, double phase) {
        long seed = instance.layoutSeed();
        double coarse = Math.sin((x + (seed & 127L)) * 0.13 + phase)
                + Math.cos((z - ((seed >>> 8) & 127L)) * 0.11 - phase * 0.7);
        double fine = Math.sin((x + z) * 0.29 + y * 0.17 + phase * 1.3)
                + Math.cos((x - z) * 0.23 - y * 0.09 - phase);
        return (coarse * 0.5 + fine * 0.25) / 2.0;
    }

    private static boolean ellipsoid(
            int x,
            int y,
            int z,
            double centerX,
            double centerY,
            double centerZ,
            double radiusX,
            double radiusY,
            double radiusZ,
            double warp) {
        double normalizedX = (x - centerX) / radiusX;
        double normalizedY = (y - centerY) / radiusY;
        double normalizedZ = (z - centerZ) / radiusZ;
        return normalizedX * normalizedX + normalizedY * normalizedY
                + normalizedZ * normalizedZ <= 1.0 + warp;
    }

    private static boolean connectorTunnel(Instance instance, int x, int y, int z) {
        if (y < 1 || y >= instance.definition().piece().height() - 1) {
            return false;
        }
        int centerX = instance.definition().piece().width() / 2;
        int centerZ = instance.definition().piece().depth() / 2;
        for (Connector connector : instance.openConnectors()) {
            int localX = Math.toIntExact(connector.position().x() - instance.piece().origin().x());
            int localZ = Math.toIntExact(connector.position().z() - instance.piece().origin().z());
            switch (connector.direction()) {
                case NORTH -> {
                    if (z <= centerZ && Math.abs(x - localX) <= 2) {
                        return true;
                    }
                }
                case SOUTH -> {
                    if (z >= centerZ && Math.abs(x - localX) <= 2) {
                        return true;
                    }
                }
                case WEST -> {
                    if (x <= centerX && Math.abs(z - localZ) <= 2) {
                        return true;
                    }
                }
                case EAST -> {
                    if (x >= centerX && Math.abs(z - localZ) <= 2) {
                        return true;
                    }
                }
                case UP, DOWN -> {
                    // Compound connectors are currently horizontal only.
                }
            }
        }
        return false;
    }

    private static BlockState caveInterior(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition.Theme theme = instance.definition().theme();
        if (theme != SpecialStructureDefinition.Theme.CAVE_POCKET
                && isCaveColumn(instance, x, z)
                && !connectorTunnel(instance, x, y, z)) {
            return processStructural(instance, wall(theme, x, y, z), x, y, z);
        }
        if (theme == SpecialStructureDefinition.Theme.FLOODED_CAVERN
                && y == 1
                && !isCaveShore(instance, x, z)
                && !connectorTunnel(instance, x, y, z)) {
            return Blocks.WATER.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.OVERGROWN_GROTTO
                && y == 1 && Math.floorMod(x * 7 + z * 11 + (int) instance.layoutSeed(), 19) == 0) {
            return Blocks.MOSS_CARPET.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.OVERGROWN_GROTTO
                && y >= 2 && y <= 5
                && Math.floorMod(x * 5 + z * 7 + y, 23) == 0) {
            return Blocks.VINE.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.ANCIENT_CAVE
                && y == 1 && Math.floorMod(x * 3 + z * 5, 17) == 0) {
            return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.CORRUPTED_CAVE
                && y == 1 && Math.floorMod(x * 5 + z * 3 + (int) instance.layoutSeed(), 23) == 0) {
            return Blocks.SOUL_SAND.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.CORRUPTED_CAVE
                && y == 1 && Math.floorMod(x * 11 + z * 7, 31) == 0) {
            return Blocks.MAGMA_BLOCK.defaultBlockState();
        }
        if (y >= instance.definition().piece().height() - 4
                && Math.floorMod(x * 7 + z * 11 + y, 31) == 0) {
            return Blocks.POINTED_DRIPSTONE.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static boolean isCaveShore(Instance instance, int x, int z) {
        int centerX = instance.definition().piece().width() / 2;
        int centerZ = instance.definition().piece().depth() / 2;
        int distance = Math.abs(x - centerX) + Math.abs(z - centerZ);
        return distance > instance.definition().piece().width() / 3;
    }

    private static boolean isCaveColumn(Instance instance, int x, int z) {
        int centerX = instance.definition().piece().width() / 2;
        int centerZ = instance.definition().piece().depth() / 2;
        long distanceSquared = (long) (x - centerX) * (x - centerX)
                + (long) (z - centerZ) * (z - centerZ);
        int roll = Math.floorMod(x * 31 + z * 17 + (int) instance.layoutSeed(), 97);
        return distanceSquared > 64
                && distanceSquared < (long) instance.definition().piece().width()
                        * instance.definition().piece().width() / 4
                && roll < (instance.definition().theme() == SpecialStructureDefinition.Theme.ENORMOUS_CAVE
                        ? 4 : 2);
    }

    private static boolean isOpenEntrance(Instance instance, int x, int y, int z) {
        if (y < 1 || y > 4) {
            return false;
        }
        for (Connector connector : instance.openConnectors()) {
            long localX = connector.position().x() - instance.piece().origin().x();
            long localZ = connector.position().z() - instance.piece().origin().z();
            if (connector.direction() == Connector.Direction.NORTH
                    || connector.direction() == Connector.Direction.SOUTH) {
                if (z == (connector.direction() == Connector.Direction.NORTH
                        ? 0 : instance.piece().definition().depth() - 1)
                        && Math.abs(x - localX) <= 2) {
                    return true;
                }
            } else if (x == (connector.direction() == Connector.Direction.WEST
                    ? 0 : instance.piece().definition().width() - 1)
                    && Math.abs(z - localZ) <= 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean inside(SpecialStructureDefinition.Section section, int x, int y, int z) {
        return x >= section.x() && x < section.x() + section.width()
                && y >= section.y() && y < section.y() + section.height()
                && z >= section.z() && z < section.z() + section.depth();
    }

    static boolean sectionWall(SpecialStructureDefinition.Section section, int x, int y, int z) {
        if (y < section.y() || y >= section.y() + section.height() - 1) {
            return false;
        }
        boolean edge = x == section.x() || x == section.x() + section.width() - 1
                || z == section.z() || z == section.z() + section.depth() - 1;
        if (!edge) {
            return false;
        }
        int cross = (x == section.x() || x == section.x() + section.width() - 1)
                ? z - section.z() : x - section.x();
        int length = (x == section.x() || x == section.x() + section.width() - 1)
                ? section.depth() : section.width();
        return !(y <= section.y() + 4 && Math.abs(cross - length / 2) <= 2);
    }

    private static BlockState detail(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition.Theme theme = instance.definition().theme();
        for (SpecialStructureDefinition.Section section : instance.definition().sections()) {
            if (x < section.x() || x >= section.x() + section.width()
                    || z < section.z() || z >= section.z() + section.depth()) {
                continue;
            }
            int centerX = section.x() + section.width() / 2;
            int centerZ = section.z() + section.depth() / 2;
            int level = y - section.y();
            if (level < 0 || level >= section.height() - 1) {
                continue;
            }
            switch (section.kind()) {
                case HOUSE -> {
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.RED_BED.defaultBlockState();
                    }
                    if (level == 0 && x == centerX - 3 && z == centerZ + 3) {
                        return Blocks.CHEST.defaultBlockState();
                    }
                    if (level == 1 && (x == section.x() + 3 || x == section.x() + section.width() - 4)
                            && z % 5 == 0) {
                        return Blocks.BOOKSHELF.defaultBlockState();
                    }
                }
                case WORKSHOP -> {
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.CRAFTING_TABLE.defaultBlockState();
                    }
                    if (level == 0 && x == centerX - 3 && z == centerZ + 3) {
                        return Blocks.SMITHING_TABLE.defaultBlockState();
                    }
                    if (level == 0 && x == centerX + 3 && z == centerZ - 3) {
                        return Blocks.GRINDSTONE.defaultBlockState();
                    }
                    if (level == 0 && x == centerX - 3 && z == centerZ - 3) {
                        return Blocks.CAULDRON.defaultBlockState();
                    }
                    if (level == 1 && Math.floorMod(x + z, 6) == 0) {
                        return Blocks.IRON_BARS.defaultBlockState();
                    }
                }
                case FARM -> {
                    if (level == 0 && Math.floorMod(x + z, 4) == 1) {
                        return Blocks.WHEAT.defaultBlockState();
                    }
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.COMPOSTER.defaultBlockState();
                    }
                }
                case DUNGEON_ROOM -> {
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.SPAWNER.defaultBlockState();
                    }
                    if (level <= 2 && (Math.abs(x - centerX) == 3 || Math.abs(z - centerZ) == 3)
                            && Math.floorMod(x + z, 3) == 0) {
                        return Blocks.IRON_BARS.defaultBlockState();
                    }
                }
                case CELL -> {
                    if (level <= 3 && (x == centerX || z == centerZ)
                            && Math.floorMod(x + z, 3) == 0) {
                        return Blocks.IRON_BARS.defaultBlockState();
                    }
                }
                case TREASURY -> {
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.CHEST.defaultBlockState();
                    }
                    if (level <= 2 && Math.abs(x - centerX) == 3 && Math.abs(z - centerZ) == 3) {
                        return Blocks.GOLD_BLOCK.defaultBlockState();
                    }
                }
                case WATCHTOWER -> {
                    if (x == centerX && z == centerZ && level >= 0 && level < section.height() - 2) {
                        return Blocks.LADDER.defaultBlockState()
                                .setValue(LadderBlock.FACING, Direction.EAST);
                    }
                    if (level == section.height() - 2
                            && Math.abs(x - centerX) <= 2 && Math.abs(z - centerZ) <= 2) {
                        return Blocks.TARGET.defaultBlockState();
                    }
                }
                case PLAZA, HALL -> {
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return light(theme);
                    }
                    if (level <= 3 && (x == section.x() + 4 || x == section.x() + section.width() - 5)
                            && (z == section.z() + 4 || z == section.z() + section.depth() - 5)) {
                        return wall(theme, x, y, z);
                    }
                }
                case STREET, BRIDGE -> {
                    if (level == 0 && Math.floorMod(x + z, 7) == 0) {
                        return light(theme);
                    }
                }
                case LIBRARY -> {
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.LECTERN.defaultBlockState();
                    }
                    if (level <= 2 && (x == section.x() + 3 || x == section.x() + section.width() - 4
                            || z == section.z() + 3 || z == section.z() + section.depth() - 4)
                            && Math.floorMod(x + z + level, 4) == 0) {
                        return Blocks.BOOKSHELF.defaultBlockState();
                    }
                }
                case BARRACKS -> {
                    if (level == 0
                            && (x == section.x() + 4 || x == section.x() + section.width() - 5)
                            && (z == section.z() + 5 || z == section.z() + section.depth() - 6)) {
                        return Blocks.RED_BED.defaultBlockState();
                    }
                    if (level == 0 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.BARREL.defaultBlockState();
                    }
                }
                case SHRINE -> {
                    if (level <= 2 && x == centerX + 3 && z == centerZ + 3) {
                        return Blocks.CRYING_OBSIDIAN.defaultBlockState();
                    }
                    if (level == 0 && Math.abs(x - centerX) == 3 && Math.abs(z - centerZ) == 3) {
                        return Blocks.CANDLE.defaultBlockState();
                    }
                }
                case STORAGE -> {
                    if (level <= 2 && (x == section.x() + 3 || x == section.x() + section.width() - 4)
                            && Math.floorMod(z, 4) == 0) {
                        return Blocks.BARREL.defaultBlockState();
                    }
                }
            }
        }
        if (theme == SpecialStructureDefinition.Theme.ZOMBIE_OUTPOST
                && y == 1 && Math.floorMod(x * 3 + z, 13) == 0) {
            return Blocks.OAK_FENCE.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.SKELETON_OUTPOST
                && y == 2 && Math.floorMod(x * 3 + z, 11) == 0) {
            return Blocks.IRON_BARS.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.ILLAGER_OUTPOST
                && y == 2 && Math.floorMod(x * 5 + z, 13) == 0) {
            return Blocks.DARK_OAK_FENCE.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.PIGLIN_OUTPOST
                && y == 1 && Math.floorMod(x * 5 + z * 3, 17) == 0) {
            return Blocks.GILDED_BLACKSTONE.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.WITHER_SKELETON_OUTPOST
                && y >= 2 && Math.floorMod(x * 7 + z * 5 + y, 19) == 0) {
            return Blocks.BASALT.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.ENORMOUS_CAVE
                && y > 2 && Math.floorMod(x * 7 + z * 11, 47) == 0) {
            return Blocks.POINTED_DRIPSTONE.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.VILLAGE
                && y == 1 && Math.floorMod(x + z, 19) == 0) {
            return Blocks.LANTERN.defaultBlockState();
        }
        return Blocks.AIR.defaultBlockState();
    }

    private static BlockState sectionRoofState(
            SpecialStructureDefinition.Theme theme,
            SpecialStructureDefinition.SectionKind kind) {
        if (kind == SpecialStructureDefinition.SectionKind.FARM
                || kind == SpecialStructureDefinition.SectionKind.BRIDGE
                || kind == SpecialStructureDefinition.SectionKind.STREET) {
            return Blocks.AIR.defaultBlockState();
        }
        return switch (theme) {
            case VILLAGE -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case ZOMBIE_OUTPOST -> Blocks.OAK_PLANKS.defaultBlockState();
            case ILLAGER_OUTPOST -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case PIGLIN_OUTPOST, WITHER_SKELETON_OUTPOST -> Blocks.BLACKSTONE.defaultBlockState();
            default -> Blocks.DEEPSLATE_TILES.defaultBlockState();
        };
    }

    private static BlockState sectionWallState(
            SpecialStructureDefinition.Theme theme,
            SpecialStructureDefinition.SectionKind kind) {
        if (theme == SpecialStructureDefinition.Theme.VILLAGE) {
            return kind == SpecialStructureDefinition.SectionKind.FARM
                    ? Blocks.OAK_LOG.defaultBlockState()
                    : Blocks.STONE_BRICKS.defaultBlockState();
        }
        if (kind == SpecialStructureDefinition.SectionKind.CELL) {
            return Blocks.IRON_BARS.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.ZOMBIE_OUTPOST) {
            return Blocks.OAK_PLANKS.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.ILLAGER_OUTPOST) {
            return Blocks.DARK_OAK_PLANKS.defaultBlockState();
        }
        if (theme == SpecialStructureDefinition.Theme.PIGLIN_OUTPOST
                || theme == SpecialStructureDefinition.Theme.WITHER_SKELETON_OUTPOST) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return wall(theme, 0, 1, 0);
    }

    private static BlockState floor(SpecialStructureDefinition.Theme theme) {
        return switch (theme) {
            case VILLAGE -> Blocks.STONE_BRICKS.defaultBlockState();
            case CAVE_POCKET, ENORMOUS_CAVE, OVERGROWN_GROTTO, ANCIENT_CAVE ->
                    Blocks.STONE.defaultBlockState();
            case FLOODED_CAVERN -> Blocks.PRISMARINE_BRICKS.defaultBlockState();
            case CORRUPTED_CAVE -> Blocks.DEEPSLATE.defaultBlockState();
            case MASSIVE_HALL -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
            case ZOMBIE_OUTPOST -> Blocks.MOSSY_COBBLESTONE.defaultBlockState();
            case PIGLIN_OUTPOST, WITHER_SKELETON_OUTPOST -> Blocks.BLACKSTONE.defaultBlockState();
            case SKELETON_OUTPOST, ILLAGER_OUTPOST -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            default -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        };
    }

    private static BlockState wall(
            SpecialStructureDefinition.Theme theme,
            int x,
            int y,
            int z) {
        return switch (theme) {
            case VILLAGE -> (y % 5 == 0 ? Blocks.MOSSY_STONE_BRICKS : Blocks.STONE_BRICKS).defaultBlockState();
            case CAVE_POCKET, ENORMOUS_CAVE -> Math.floorMod(x + z + y, 9) == 0
                    ? Blocks.TUFF.defaultBlockState() : Blocks.STONE.defaultBlockState();
            case FLOODED_CAVERN -> Math.floorMod(x + z + y, 7) == 0
                    ? Blocks.PRISMARINE.defaultBlockState() : Blocks.PRISMARINE_BRICKS.defaultBlockState();
            case OVERGROWN_GROTTO -> Math.floorMod(x + z + y, 5) == 0
                    ? Blocks.MOSSY_COBBLESTONE.defaultBlockState() : Blocks.MOSS_BLOCK.defaultBlockState();
            case ANCIENT_CAVE -> Math.floorMod(x + z + y, 11) == 0
                    ? Blocks.CHISELED_STONE_BRICKS.defaultBlockState() : Blocks.STONE.defaultBlockState();
            case CORRUPTED_CAVE -> Math.floorMod(x * 3 + z * 5 + y, 9) == 0
                    ? Blocks.CRYING_OBSIDIAN.defaultBlockState() : Blocks.DEEPSLATE.defaultBlockState();
            case ZOMBIE_OUTPOST -> Math.floorMod(x + z + y, 7) == 0
                    ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                    : Blocks.OAK_PLANKS.defaultBlockState();
            case PIGLIN_OUTPOST, WITHER_SKELETON_OUTPOST -> Blocks.BLACKSTONE.defaultBlockState();
            case SKELETON_OUTPOST -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case ILLAGER_OUTPOST -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            default -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        };
    }

    private static BlockState ceiling(SpecialStructureDefinition.Theme theme) {
        return switch (theme) {
            case VILLAGE -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case CAVE_POCKET, ENORMOUS_CAVE, OVERGROWN_GROTTO, ANCIENT_CAVE -> Blocks.STONE.defaultBlockState();
            case FLOODED_CAVERN -> Blocks.PRISMARINE.defaultBlockState();
            case CORRUPTED_CAVE -> Blocks.DEEPSLATE.defaultBlockState();
            case ZOMBIE_OUTPOST -> Blocks.OAK_PLANKS.defaultBlockState();
            case ILLAGER_OUTPOST -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            case PIGLIN_OUTPOST, WITHER_SKELETON_OUTPOST -> Blocks.BLACKSTONE.defaultBlockState();
            default -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        };
    }

    private static BlockState light(SpecialStructureDefinition.Theme theme) {
        return theme == SpecialStructureDefinition.Theme.WITHER_SKELETON_OUTPOST
                || theme == SpecialStructureDefinition.Theme.PIGLIN_OUTPOST
                ? Blocks.SOUL_LANTERN.defaultBlockState()
                : Blocks.LANTERN.defaultBlockState();
    }

    private static void setLootTable(ChunkAccess chunk, BlockPos position, BlockState state,
            ResourceLocation lootTable, long seed) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", state.is(Blocks.BARREL) ? "minecraft:barrel" : "minecraft:chest");
        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());
        tag.putString("LootTable", lootTable.toString());
        tag.putLong("LootTableSeed", seed);
        chunk.setBlockEntityNbt(tag);
    }

    private static void setSpawnerData(ChunkAccess chunk, BlockPos position,
            SpecialStructureDefinition.Population population) {
        String entityId = spawnerEntityId(population);
        if (entityId == null) {
            return;
        }
        // Minecraft 1.21.1 reads the entity id from SpawnData.entity. Leaving
        // this nested payload out creates a visually correct but inert spawner.
        CompoundTag entity = new CompoundTag();
        entity.putString("id", entityId);
        CompoundTag spawnData = new CompoundTag();
        spawnData.put("entity", entity);

        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:mob_spawner");
        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());
        tag.put("SpawnData", spawnData);
        chunk.setBlockEntityNbt(tag);
    }

    static String spawnerEntityId(SpecialStructureDefinition.Population population) {
        return switch (population) {
            case ZOMBIES -> "minecraft:zombie";
            case SKELETONS -> "minecraft:skeleton";
            case PILLAGERS -> "minecraft:pillager";
            case PIGLINS -> "minecraft:piglin";
            case WITHER_SKELETONS -> "minecraft:wither_skeleton";
            case SPIDERS -> "minecraft:spider";
            case VILLAGERS, NONE -> null;
        };
    }

    private static long lootSeed(Instance instance, long x, long y, long z) {
        long value = instance.originCell().x() * 0x9E3779B97F4A7C15L
                ^ instance.originCell().z() * 0xC2B2AE3D27D4EB4FL;
        return value ^ x * 31L ^ y * 17L ^ z * 13L;
    }

    private static List<BlockPos> populationPositions(Instance instance) {
        int count = switch (instance.definition().population()) {
            case VILLAGERS -> 6;
            case WITHER_SKELETONS, PILLAGERS -> 5;
            default -> 4;
        };
        List<BlockPos> positions = new ArrayList<>();
        List<RoutePoint> anchors = new ArrayList<>();
        for (SpecialStructureDefinition.Section section : instance.definition().sections()) {
            boolean useful = switch (instance.definition().population()) {
                case VILLAGERS -> section.kind() == SpecialStructureDefinition.SectionKind.PLAZA
                        || section.kind() == SpecialStructureDefinition.SectionKind.HOUSE
                        || section.kind() == SpecialStructureDefinition.SectionKind.WORKSHOP
                        || section.kind() == SpecialStructureDefinition.SectionKind.LIBRARY;
                default -> section.kind() == SpecialStructureDefinition.SectionKind.DUNGEON_ROOM
                        || section.kind() == SpecialStructureDefinition.SectionKind.WATCHTOWER
                        || section.kind() == SpecialStructureDefinition.SectionKind.SHRINE
                        || section.kind() == SpecialStructureDefinition.SectionKind.TREASURY
                        || section.kind() == SpecialStructureDefinition.SectionKind.BARRACKS;
            };
            if (useful) {
                anchors.add(pointFor(section));
            }
        }
        if (anchors.isEmpty()) {
            anchors.add(new RoutePoint(
                    instance.definition().piece().width() / 2,
                    1,
                    instance.definition().piece().depth() / 2));
        }
        int originX = Math.toIntExact(instance.piece().origin().x());
        int originZ = Math.toIntExact(instance.piece().origin().z());
        for (int index = 0; index < count; index++) {
            RoutePoint anchor = anchors.get(index % anchors.size());
            int side = index / anchors.size();
            int x = anchor.x() + (side % 2 == 0 ? -2 : 2);
            int z = anchor.z() + (side % 3 - 1) * 2;
            positions.add(new BlockPos(
                    originX + x,
                    Math.toIntExact(instance.piece().origin().y()) + anchor.y(),
                    originZ + z));
        }
        return positions;
    }

    private static Entity createEntity(SpecialStructureDefinition.Population population,
            ServerLevel level) {
        return switch (population) {
            case VILLAGERS -> EntityType.VILLAGER.create(level);
            case ZOMBIES -> EntityType.ZOMBIE.create(level);
            case SKELETONS -> EntityType.SKELETON.create(level);
            case PILLAGERS -> EntityType.PILLAGER.create(level);
            case PIGLINS -> EntityType.PIGLIN.create(level);
            case WITHER_SKELETONS -> EntityType.WITHER_SKELETON.create(level);
            case SPIDERS -> EntityType.SPIDER.create(level);
            case NONE -> null;
        };
    }

    private static GenerationGrid.Direction toGridDirection(Connector.Direction direction) {
        return switch (direction) {
            case NORTH -> GenerationGrid.Direction.NORTH;
            case EAST -> GenerationGrid.Direction.EAST;
            case SOUTH -> GenerationGrid.Direction.SOUTH;
            case WEST -> GenerationGrid.Direction.WEST;
            case UP, DOWN -> throw new IllegalArgumentException("compound structures use horizontal connectors");
        };
    }

    private static Connector.Direction toConnectorDirection(GenerationGrid.Direction direction) {
        return switch (direction) {
            case NORTH -> Connector.Direction.NORTH;
            case EAST -> Connector.Direction.EAST;
            case SOUTH -> Connector.Direction.SOUTH;
            case WEST -> Connector.Direction.WEST;
        };
    }

    private static boolean isSectorOrigin(GenerationGrid.Cell cell) {
        return Math.floorMod(cell.x(), SECTOR_SIZE_CELLS) == 0
                && Math.floorMod(cell.z(), SECTOR_SIZE_CELLS) == 0;
    }

    private static boolean isOriginSector(GenerationGrid.Cell cell) {
        return cell.x() == 0 && cell.z() == 0;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("labrinth", path);
    }

    private static ResourceLocation vanillaLoot(String path) {
        return ResourceLocation.withDefaultNamespace("chests/" + path);
    }

    private static ResourceLocation discoveryLoot(String path) {
        return id("chests/" + path);
    }

    private static List<SpecialStructureDefinition> createDefinitions() {
        Set<ResourceLocation> all = RegionCatalog.REGION_IDS;
        Set<ResourceLocation> settlementRegions = Set.of(
                RegionCatalog.ABANDONED_ID,
                RegionCatalog.INDUSTRIAL_ID,
                RegionCatalog.OVERGROWN_ID,
                RegionCatalog.ANCIENT_ID);
        Set<ResourceLocation> dangerousRegions = Set.of(
                RegionCatalog.ABANDONED_ID,
                RegionCatalog.ANCIENT_ID,
                RegionCatalog.CORRUPTED_ID);
        List<SpecialStructureDefinition> values = List.of(
                create("village/stone_settlement", SpecialStructureDefinition.Theme.VILLAGE,
                        2, StructurePiece.Rarity.RARE, 4, 28, -1, 1, settlementRegions,
                        128, 16, 128, 2, SpecialStructureDefinition.Population.VILLAGERS,
                        List.of(
                                section(8, 1, 8, 40, 7, 40, SpecialStructureDefinition.SectionKind.PLAZA),
                                section(8, 1, 56, 32, 6, 32, SpecialStructureDefinition.SectionKind.HOUSE),
                                section(56, 1, 8, 32, 6, 32, SpecialStructureDefinition.SectionKind.WORKSHOP),
                                section(88, 1, 56, 32, 6, 32, SpecialStructureDefinition.SectionKind.FARM)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST, Connector.Direction.WEST),
                        vanillaLoot("village/plains_house")),
                create("village/overgrown_settlement", SpecialStructureDefinition.Theme.VILLAGE,
                        1, StructurePiece.Rarity.VERY_RARE, 8, 32, 0, 1,
                        Set.of(RegionCatalog.OVERGROWN_ID, RegionCatalog.ABANDONED_ID),
                        128, 18, 128, 2, SpecialStructureDefinition.Population.VILLAGERS,
                        List.of(
                                section(8, 1, 8, 48, 8, 48, SpecialStructureDefinition.SectionKind.PLAZA),
                                section(72, 1, 8, 40, 7, 32, SpecialStructureDefinition.SectionKind.HOUSE),
                                section(8, 1, 72, 32, 7, 40, SpecialStructureDefinition.SectionKind.FARM),
                                section(72, 1, 72, 40, 7, 40, SpecialStructureDefinition.SectionKind.WORKSHOP)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("village/taiga_house")),
                create("village/vertical_settlement", SpecialStructureDefinition.Theme.VILLAGE,
                        1, StructurePiece.Rarity.VERY_RARE, 10, 32, 0, 1,
                        Set.of(RegionCatalog.OVERGROWN_ID, RegionCatalog.ANCIENT_ID),
                        128, 32, 128, 2, SpecialStructureDefinition.Population.VILLAGERS,
                        List.of(
                                section(8, 1, 8, 48, 8, 48, SpecialStructureDefinition.SectionKind.PLAZA),
                                section(72, 1, 8, 40, 9, 40, SpecialStructureDefinition.SectionKind.LIBRARY),
                                section(8, 17, 72, 40, 9, 40, SpecialStructureDefinition.SectionKind.BARRACKS),
                                section(72, 17, 72, 40, 9, 40, SpecialStructureDefinition.SectionKind.WORKSHOP)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST),
                        vanillaLoot("village/plains_house")),
                create("dungeon/compact", SpecialStructureDefinition.Theme.COMPACT_DUNGEON,
                        8, StructurePiece.Rarity.UNCOMMON, 2, 32, -1, 1, dangerousRegions,
                        64, 12, 64, 1, SpecialStructureDefinition.Population.ZOMBIES,
                        List.of(
                                section(6, 1, 6, 22, 7, 22, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(36, 1, 6, 22, 7, 22, SpecialStructureDefinition.SectionKind.CELL),
                                section(21, 1, 36, 22, 8, 22, SpecialStructureDefinition.SectionKind.TREASURY)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("simple_dungeon")),
                create("dungeon/complex", SpecialStructureDefinition.Theme.DUNGEON_COMPLEX,
                        3, StructurePiece.Rarity.RARE, 7, 32, -1, 1, dangerousRegions,
                        128, 24, 128, 2, SpecialStructureDefinition.Population.SKELETONS,
                        List.of(
                                section(8, 1, 8, 40, 8, 40, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(80, 1, 8, 40, 8, 40, SpecialStructureDefinition.SectionKind.CELL),
                                section(8, 1, 80, 40, 8, 40, SpecialStructureDefinition.SectionKind.WORKSHOP),
                                section(80, 1, 80, 40, 8, 40, SpecialStructureDefinition.SectionKind.TREASURY),
                                section(48, 1, 48, 32, 10, 32, SpecialStructureDefinition.SectionKind.SHRINE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST),
                        vanillaLoot("stronghold_corridor")),
                create("dungeon/mega", SpecialStructureDefinition.Theme.DUNGEON_MEGA,
                        1, StructurePiece.Rarity.VERY_RARE, 16, 32, -1, 1, dangerousRegions,
                        192, 28, 192, 2, SpecialStructureDefinition.Population.WITHER_SKELETONS,
                        List.of(
                                section(16, 1, 16, 48, 10, 48, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(80, 1, 16, 48, 10, 48, SpecialStructureDefinition.SectionKind.BARRACKS),
                                section(144, 1, 16, 32, 10, 48, SpecialStructureDefinition.SectionKind.CELL),
                                section(16, 1, 96, 48, 12, 48, SpecialStructureDefinition.SectionKind.LIBRARY),
                                section(80, 1, 80, 64, 14, 64, SpecialStructureDefinition.SectionKind.SHRINE),
                                section(144, 1, 96, 32, 12, 48, SpecialStructureDefinition.SectionKind.TREASURY),
                                section(80, 15, 144, 48, 10, 32, SpecialStructureDefinition.SectionKind.STORAGE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST, Connector.Direction.WEST),
                        discoveryLoot("dungeon_legendary")),
                create("cave/pocket", SpecialStructureDefinition.Theme.CAVE_POCKET,
                        10, StructurePiece.Rarity.UNCOMMON, 0, 32, -1, 1, all,
                        64, 12, 64, 1, SpecialStructureDefinition.Population.NONE,
                        List.of(section(16, 1, 16, 32, 7, 32, SpecialStructureDefinition.SectionKind.HALL)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        discoveryLoot("cave_common")),
                create("cave/enormous_chamber", SpecialStructureDefinition.Theme.ENORMOUS_CAVE,
                        2, StructurePiece.Rarity.RARE, 6, 32, -1, 1, all,
                        128, 18, 128, 1, SpecialStructureDefinition.Population.SPIDERS,
                        List.of(
                                section(18, 1, 18, 44, 8, 44, SpecialStructureDefinition.SectionKind.HALL),
                                section(66, 1, 18, 44, 8, 44, SpecialStructureDefinition.SectionKind.SHRINE),
                                section(42, 1, 66, 44, 8, 44, SpecialStructureDefinition.SectionKind.STORAGE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST, Connector.Direction.WEST),
                        vanillaLoot("abandoned_mineshaft")),
                create("cave/flooded_cavern", SpecialStructureDefinition.Theme.FLOODED_CAVERN,
                        3, StructurePiece.Rarity.RARE, 4, 32, -1, 1,
                        Set.of(RegionCatalog.FLOODED_ID),
                        128, 18, 128, 1, SpecialStructureDefinition.Population.NONE,
                        List.of(section(24, 1, 24, 80, 10, 80, SpecialStructureDefinition.SectionKind.BRIDGE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST),
                        discoveryLoot("cave_flooded")),
                create("cave/overgrown_grotto", SpecialStructureDefinition.Theme.OVERGROWN_GROTTO,
                        4, StructurePiece.Rarity.UNCOMMON, 2, 32, -1, 1,
                        Set.of(RegionCatalog.OVERGROWN_ID, RegionCatalog.ABANDONED_ID),
                        96, 16, 96, 1, SpecialStructureDefinition.Population.SPIDERS,
                        List.of(
                                section(16, 1, 16, 32, 8, 32, SpecialStructureDefinition.SectionKind.SHRINE),
                                section(48, 1, 48, 32, 7, 32, SpecialStructureDefinition.SectionKind.STORAGE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        discoveryLoot("cave_overgrown")),
                create("cave/ancient_ruin", SpecialStructureDefinition.Theme.ANCIENT_CAVE,
                        2, StructurePiece.Rarity.RARE, 8, 32, -1, 1,
                        Set.of(RegionCatalog.ANCIENT_ID),
                        128, 20, 128, 1, SpecialStructureDefinition.Population.SKELETONS,
                        List.of(
                                section(24, 1, 24, 40, 10, 40, SpecialStructureDefinition.SectionKind.SHRINE),
                                section(72, 1, 72, 32, 9, 32, SpecialStructureDefinition.SectionKind.LIBRARY)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.WEST),
                        discoveryLoot("cave_ancient")),
                create("cave/corrupted_chamber", SpecialStructureDefinition.Theme.CORRUPTED_CAVE,
                        2, StructurePiece.Rarity.RARE, 12, 32, -1, 1,
                        Set.of(RegionCatalog.CORRUPTED_ID),
                        128, 20, 128, 1, SpecialStructureDefinition.Population.SPIDERS,
                        List.of(
                                section(24, 1, 24, 48, 10, 48, SpecialStructureDefinition.SectionKind.SHRINE),
                                section(72, 1, 72, 32, 8, 32, SpecialStructureDefinition.SectionKind.TREASURY)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST),
                        discoveryLoot("cave_corrupted")),
                create("massive/hall", SpecialStructureDefinition.Theme.MASSIVE_HALL,
                        1, StructurePiece.Rarity.VERY_RARE, 10, 32, -1, 1, all,
                        128, 20, 128, 2, SpecialStructureDefinition.Population.NONE,
                        List.of(
                                section(8, 1, 8, 40, 14, 40, SpecialStructureDefinition.SectionKind.HALL),
                                section(80, 1, 8, 40, 14, 40, SpecialStructureDefinition.SectionKind.HALL),
                                section(8, 1, 80, 40, 14, 40, SpecialStructureDefinition.SectionKind.HALL),
                                section(80, 1, 80, 40, 14, 40, SpecialStructureDefinition.SectionKind.HALL),
                                section(48, 1, 48, 32, 16, 32, SpecialStructureDefinition.SectionKind.SHRINE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("stronghold_crossing")),
                create("outpost/zombie", SpecialStructureDefinition.Theme.ZOMBIE_OUTPOST,
                        4, StructurePiece.Rarity.UNCOMMON, 3, 32, -1, 1, all,
                        64, 12, 64, 1, SpecialStructureDefinition.Population.ZOMBIES,
                        List.of(
                                section(6, 1, 8, 32, 7, 32, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(42, 1, 24, 16, 7, 16, SpecialStructureDefinition.SectionKind.WATCHTOWER)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("simple_dungeon")),
                create("outpost/skeleton", SpecialStructureDefinition.Theme.SKELETON_OUTPOST,
                        3, StructurePiece.Rarity.UNCOMMON, 5, 32, -1, 1, all,
                        64, 14, 64, 1, SpecialStructureDefinition.Population.SKELETONS,
                        List.of(
                                section(6, 1, 8, 28, 9, 28, SpecialStructureDefinition.SectionKind.WATCHTOWER),
                                section(38, 1, 20, 18, 7, 24, SpecialStructureDefinition.SectionKind.STORAGE)),
                        Set.of(Connector.Direction.EAST, Connector.Direction.WEST),
                        vanillaLoot("pillager_outpost")),
                create("outpost/illager", SpecialStructureDefinition.Theme.ILLAGER_OUTPOST,
                        2, StructurePiece.Rarity.RARE, 8, 32, 0, 1, all,
                        64, 16, 64, 1, SpecialStructureDefinition.Population.PILLAGERS,
                        List.of(
                                section(6, 1, 6, 30, 10, 30, SpecialStructureDefinition.SectionKind.WATCHTOWER),
                                section(40, 1, 20, 18, 7, 24, SpecialStructureDefinition.SectionKind.HOUSE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("pillager_outpost")),
                create("outpost/piglin", SpecialStructureDefinition.Theme.PIGLIN_OUTPOST,
                        2, StructurePiece.Rarity.RARE, 10, 32, -1, 0, all,
                        64, 14, 64, 1, SpecialStructureDefinition.Population.PIGLINS,
                        List.of(
                                section(6, 1, 6, 28, 8, 28, SpecialStructureDefinition.SectionKind.TREASURY),
                                section(38, 1, 20, 20, 8, 24, SpecialStructureDefinition.SectionKind.BARRACKS)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("bastion_treasure")),
                create("outpost/wither_skeleton", SpecialStructureDefinition.Theme.WITHER_SKELETON_OUTPOST,
                        1, StructurePiece.Rarity.VERY_RARE, 14, 32, -1, 0,
                        Set.of(RegionCatalog.ANCIENT_ID, RegionCatalog.CORRUPTED_ID),
                        64, 16, 64, 1, SpecialStructureDefinition.Population.WITHER_SKELETONS,
                        List.of(
                                section(6, 1, 6, 28, 10, 28, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(38, 1, 20, 20, 10, 24, SpecialStructureDefinition.SectionKind.SHRINE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("nether_bridge")));
        return values;
    }

    private static SpecialStructureDefinition create(
            String path,
            SpecialStructureDefinition.Theme theme,
            int weight,
            StructurePiece.Rarity rarity,
            int minDepth,
            int maxDepth,
            int minFloor,
            int maxFloor,
            Set<ResourceLocation> regions,
            int width,
            int height,
            int depth,
            int minimumConnections,
            SpecialStructureDefinition.Population population,
            List<SpecialStructureDefinition.Section> sections,
            Set<Connector.Direction> directions,
            ResourceLocation lootTable) {
        ResourceLocation id = id(path);
        StructurePiece piece = StructurePiece.builder(
                        id,
                        id("generated/" + path),
                        StructurePiece.Kind.COMPOUND,
                        width,
                        height,
                        depth)
                .weight(weight)
                .rarity(rarity)
                .rotation(StructurePiece.Rotation.NONE)
                .mirror(StructurePiece.Mirror.NONE)
                .depthRange(minDepth, maxDepth)
                .allowedRegions(regions)
                .connectors(connectors(width, depth, directions))
                .placementConditions(new StructurePiece.PlacementConditions(2, true))
                .loot(StructurePiece.LootConfiguration.table(lootTable))
                .build();
        return new SpecialStructureDefinition(
                id,
                theme,
                weight,
                rarity,
                tierFor(theme, rarity),
                minDepth,
                maxDepth,
                minFloor,
                maxFloor,
                regions,
                piece,
                sections,
                minimumConnections,
                population,
                Optional.of(lootTable));
    }

    private static LabrinthDiscoveryTier tierFor(
            SpecialStructureDefinition.Theme theme,
            StructurePiece.Rarity rarity) {
        return switch (theme) {
            case CAVE_POCKET, FLOODED_CAVERN, OVERGROWN_GROTTO ->
                    LabrinthDiscoveryTier.UNCOMMON;
            case ANCIENT_CAVE, CORRUPTED_CAVE -> LabrinthDiscoveryTier.RARE;
            case DUNGEON_MEGA -> LabrinthDiscoveryTier.LEGENDARY;
            case MASSIVE_HALL -> LabrinthDiscoveryTier.MAJOR;
            case VILLAGE -> rarity == StructurePiece.Rarity.VERY_RARE
                    ? LabrinthDiscoveryTier.MAJOR : LabrinthDiscoveryTier.RARE;
            default -> LabrinthDiscoveryTier.fromRarity(rarity);
        };
    }

    private static List<Connector> connectors(
            int width,
            int depth,
            Set<Connector.Direction> directions) {
        List<Connector> connectors = new ArrayList<>();
        int[] offsetsX = width >= 128 ? new int[] {32, 96} : new int[] {width / 2};
        int[] offsetsZ = depth >= 128 ? new int[] {32, 96} : new int[] {depth / 2};
        for (Connector.Direction direction : directions) {
            if (direction == Connector.Direction.NORTH || direction == Connector.Direction.SOUTH) {
                for (int x : offsetsX) {
                    connectors.add(new Connector(
                            new Connector.Position(x, 1, direction == Connector.Direction.NORTH ? 0 : depth),
                            direction, Connector.Type.DOOR, 5, 4,
                            StructurePiece.Rotation.NONE, false));
                }
            } else {
                for (int z : offsetsZ) {
                    connectors.add(new Connector(
                            new Connector.Position(direction == Connector.Direction.WEST ? 0 : width, 1, z),
                            direction, Connector.Type.DOOR, 5, 4,
                            StructurePiece.Rotation.NONE, false));
                }
            }
        }
        return List.copyOf(connectors);
    }

    private static SpecialStructureDefinition.Section section(
            int x,
            int y,
            int z,
            int width,
            int height,
            int depth,
            SpecialStructureDefinition.SectionKind kind) {
        return new SpecialStructureDefinition.Section(x, y, z, width, height, depth, kind);
    }

    private record RoutePoint(int x, int y, int z) {
    }

    private record Option(
            SpecialStructureDefinition definition,
            int floor,
            int depth,
            RegionDefinition region,
            PlacedStructurePiece piece,
            Set<Connector> openConnectors) {
    }

    public record Instance(
            SpecialStructureDefinition definition,
            GenerationGrid.Cell originCell,
            int floorIndex,
            int depth,
            RegionDefinition region,
            PlacedStructurePiece piece,
            Set<Connector> openConnectors,
            long layoutSeed) {
        public Instance {
            Objects.requireNonNull(definition, "definition");
            Objects.requireNonNull(originCell, "originCell");
            Objects.requireNonNull(region, "region");
            Objects.requireNonNull(piece, "piece");
            openConnectors = Set.copyOf(openConnectors);
        }

        public boolean contains(int x, int y, int z) {
            GenerationGrid.Bounds bounds = piece.bounds();
            return x >= bounds.minBlockX() && x < bounds.maxBlockXExclusive()
                    && y >= bounds.minY() && y < bounds.maxYExclusive()
                    && z >= bounds.minBlockZ() && z < bounds.maxBlockZExclusive();
        }
    }

    public record Statistics(
            int radiusCells,
            int candidateOrigins,
            int selectedStructures,
            double averageCellDistance,
            Map<SpecialStructureDefinition.Theme, Integer> byTheme,
            Map<LabrinthDiscoveryTier, Integer> byTier,
            Map<ResourceLocation, Integer> byRegion) {
        public Statistics {
            if (radiusCells < 0 || candidateOrigins < 0 || selectedStructures < 0
                    || selectedStructures > candidateOrigins || averageCellDistance < 0.0) {
                throw new IllegalArgumentException("invalid discovery statistics");
            }
            byTheme = Map.copyOf(Objects.requireNonNull(byTheme, "byTheme"));
            byTier = Map.copyOf(Objects.requireNonNull(byTier, "byTier"));
            byRegion = Map.copyOf(Objects.requireNonNull(byRegion, "byRegion"));
        }

        public int count(SpecialStructureDefinition.Theme theme) {
            return byTheme.getOrDefault(theme, 0);
        }

        public int count(LabrinthDiscoveryTier tier) {
            return byTier.getOrDefault(tier, 0);
        }
    }
}
