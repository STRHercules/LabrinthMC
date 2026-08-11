package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/**
 * Deterministic, origin-owned compound structures and their chunk-local
 * renderer/populator. The reservation is always the complete piece bounds;
 * authored sections never participate in ordinary cell selection.
 */
public final class SpecialStructureCatalog {
    public static final int SECTOR_SIZE_CELLS = 8;
    public static final int CHANCE_PERCENT = 22;

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
                    if (state.is(Blocks.CHEST) && instance.definition().lootTable().isPresent()) {
                        setLootTable(chunk, position, instance.definition().lootTable().get(),
                                lootSeed(instance, worldX, worldY, worldZ));
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

    /** Populate only from the origin chunk; saved entities prevent reload duplication. */
    public static void populate(WorldGenRegion region, long worldSeed) {
        ChunkPos center = region.getCenter();
        GenerationGrid.Chunk centerChunk = new GenerationGrid.Chunk(center.x, center.z);
        for (Instance instance : intersecting(worldSeed, centerChunk)) {
            if (!instance.piece().ownerChunk().equals(centerChunk)
                    || instance.definition().population() == SpecialStructureDefinition.Population.NONE) {
                continue;
            }
            for (BlockPos position : populationPositions(instance)) {
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
        Option option = weightedChoice(random, options);
        return Optional.of(new Instance(
                option.definition(),
                originCell,
                option.floor(),
                option.depth(),
                option.region(),
                option.piece(),
                option.openConnectors()));
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

    private static Option weightedChoice(RandomSource random, List<Option> options) {
        int totalWeight = options.stream().mapToInt(option -> option.definition().weight()).sum();
        int choice = random.nextInt(totalWeight);
        for (Option option : options) {
            choice -= option.definition().weight();
            if (choice < 0) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }

    private static BlockState stateForLocal(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition definition = instance.definition();
        int width = definition.piece().width();
        int height = definition.piece().height();
        int depth = definition.piece().depth();
        if (y == 0) {
            return floor(definition.theme());
        }
        if (y == height - 1) {
            return ceiling(definition.theme());
        }
        if (isOpenEntrance(instance, x, y, z)) {
            return Blocks.AIR.defaultBlockState();
        }
        boolean boundary = x == 0 || x == width - 1 || z == 0 || z == depth - 1;
        if (boundary) {
            return wall(definition.theme(), x, y, z);
        }
        for (SpecialStructureDefinition.Section section : definition.sections()) {
            if (inside(section, x, y, z) && sectionWall(section, x, y, z)) {
                return sectionWallState(definition.theme(), section.kind());
            }
        }
        return detail(instance, x, y, z);
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

    private static boolean sectionWall(SpecialStructureDefinition.Section section, int x, int y, int z) {
        if (y <= section.y() || y >= section.y() + section.height() - 1) {
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
        return !(y <= section.y() + 4 && Math.abs(cross - length / 2) <= 1);
    }

    private static BlockState detail(Instance instance, int x, int y, int z) {
        SpecialStructureDefinition.Theme theme = instance.definition().theme();
        for (SpecialStructureDefinition.Section section : instance.definition().sections()) {
            if (!inside(section, x, y, z) || y != section.y()) {
                continue;
            }
            int centerX = section.x() + section.width() / 2;
            int centerZ = section.z() + section.depth() / 2;
            switch (section.kind()) {
                case HOUSE -> {
                    if (x == centerX && z == centerZ) return Blocks.RED_BED.defaultBlockState();
                    if (x == centerX + 2 && z == centerZ) return Blocks.CHEST.defaultBlockState();
                }
                case WORKSHOP -> {
                    if (x == centerX && z == centerZ) return Blocks.CRAFTING_TABLE.defaultBlockState();
                    if (x == centerX + 2 && z == centerZ) return Blocks.SMITHING_TABLE.defaultBlockState();
                }
                case FARM -> {
                    if (Math.floorMod(x + z, 4) == 0) return Blocks.FARMLAND.defaultBlockState();
                    if (Math.floorMod(x + z, 4) == 1) return Blocks.WHEAT.defaultBlockState();
                }
                case DUNGEON_ROOM -> {
                    if (x == centerX && z == centerZ) return Blocks.SPAWNER.defaultBlockState();
                    if (Math.abs(x - centerX) == 3 && z == centerZ) return Blocks.IRON_BARS.defaultBlockState();
                }
                case CELL -> {
                    if (x == centerX && z == centerZ) return Blocks.IRON_BARS.defaultBlockState();
                }
                case TREASURY -> {
                    if (x == centerX && z == centerZ) return Blocks.CHEST.defaultBlockState();
                    if (Math.abs(x - centerX) == 3 && Math.abs(z - centerZ) == 3) {
                        return Blocks.GOLD_BLOCK.defaultBlockState();
                    }
                }
                case WATCHTOWER -> {
                    if (x == centerX && z == centerZ) return Blocks.LADDER.defaultBlockState();
                }
                case PLAZA, HALL -> {
                    if (y == section.y() + 1 && x == centerX && z == centerZ) {
                        return light(theme);
                    }
                }
            }
        }
        if ((theme == SpecialStructureDefinition.Theme.SKELETON_OUTPOST
                || theme == SpecialStructureDefinition.Theme.ILLAGER_OUTPOST)
                && y == 2 && Math.floorMod(x * 3 + z, 17) == 0) {
            return Blocks.IRON_BARS.defaultBlockState();
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
        return wall(theme, 0, 1, 0);
    }

    private static BlockState floor(SpecialStructureDefinition.Theme theme) {
        return switch (theme) {
            case VILLAGE -> Blocks.STONE_BRICKS.defaultBlockState();
            case ENORMOUS_CAVE -> Blocks.STONE.defaultBlockState();
            case MASSIVE_HALL -> Blocks.POLISHED_DEEPSLATE.defaultBlockState();
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
            case ENORMOUS_CAVE -> Math.floorMod(x + z + y, 9) == 0
                    ? Blocks.TUFF.defaultBlockState() : Blocks.STONE.defaultBlockState();
            case PIGLIN_OUTPOST, WITHER_SKELETON_OUTPOST -> Blocks.BLACKSTONE.defaultBlockState();
            case SKELETON_OUTPOST -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
            case ILLAGER_OUTPOST -> Blocks.DARK_OAK_PLANKS.defaultBlockState();
            default -> Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        };
    }

    private static BlockState ceiling(SpecialStructureDefinition.Theme theme) {
        return switch (theme) {
            case VILLAGE -> Blocks.DEEPSLATE_TILES.defaultBlockState();
            case ENORMOUS_CAVE -> Blocks.STONE.defaultBlockState();
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

    private static void setLootTable(ChunkAccess chunk, BlockPos position,
            ResourceLocation lootTable, long seed) {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:chest");
        tag.putInt("x", position.getX());
        tag.putInt("y", position.getY());
        tag.putInt("z", position.getZ());
        tag.putString("LootTable", lootTable.toString());
        tag.putLong("LootTableSeed", seed);
        chunk.setBlockEntityNbt(tag);
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
        int y = instance.piece().origin().y() + 1;
        List<BlockPos> positions = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            int x = Math.min(13, 4 + (index % 3) * 4);
            int z = Math.min(13, 4 + (index / 3) * 6);
            positions.add(new BlockPos(
                    Math.toIntExact(instance.piece().origin().x()) + x,
                    y,
                    Math.toIntExact(instance.piece().origin().z()) + z));
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
                create("dungeon/compact", SpecialStructureDefinition.Theme.COMPACT_DUNGEON,
                        8, StructurePiece.Rarity.UNCOMMON, 2, 32, -1, 1, dangerousRegions,
                        64, 12, 64, 1, SpecialStructureDefinition.Population.ZOMBIES,
                        List.of(
                                section(8, 1, 8, 48, 7, 48, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("simple_dungeon")),
                create("dungeon/complex", SpecialStructureDefinition.Theme.DUNGEON_COMPLEX,
                        3, StructurePiece.Rarity.RARE, 7, 32, -1, 1, dangerousRegions,
                        128, 24, 128, 2, SpecialStructureDefinition.Population.SKELETONS,
                        List.of(
                                section(8, 1, 8, 48, 8, 48, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(72, 1, 8, 48, 8, 48, SpecialStructureDefinition.SectionKind.CELL),
                                section(8, 1, 72, 48, 8, 48, SpecialStructureDefinition.SectionKind.WORKSHOP),
                                section(72, 1, 72, 48, 8, 48, SpecialStructureDefinition.SectionKind.TREASURY)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST),
                        vanillaLoot("stronghold_corridor")),
                create("cave/enormous_chamber", SpecialStructureDefinition.Theme.ENORMOUS_CAVE,
                        2, StructurePiece.Rarity.RARE, 6, 32, -1, 1, all,
                        128, 18, 128, 1, SpecialStructureDefinition.Population.SPIDERS,
                        List.of(
                                section(24, 1, 24, 80, 8, 80, SpecialStructureDefinition.SectionKind.HALL)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH,
                                Connector.Direction.EAST, Connector.Direction.WEST),
                        vanillaLoot("abandoned_mineshaft")),
                create("massive/hall", SpecialStructureDefinition.Theme.MASSIVE_HALL,
                        1, StructurePiece.Rarity.VERY_RARE, 10, 32, -1, 1, all,
                        128, 20, 128, 2, SpecialStructureDefinition.Population.NONE,
                        List.of(
                                section(8, 1, 8, 48, 14, 48, SpecialStructureDefinition.SectionKind.HALL),
                                section(72, 1, 8, 48, 14, 48, SpecialStructureDefinition.SectionKind.HALL),
                                section(8, 1, 72, 48, 14, 48, SpecialStructureDefinition.SectionKind.HALL),
                                section(72, 1, 72, 48, 14, 48, SpecialStructureDefinition.SectionKind.HALL)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("stronghold_crossing")),
                create("outpost/zombie", SpecialStructureDefinition.Theme.ZOMBIE_OUTPOST,
                        4, StructurePiece.Rarity.UNCOMMON, 3, 32, -1, 1, all,
                        64, 12, 64, 1, SpecialStructureDefinition.Population.ZOMBIES,
                        List.of(
                                section(8, 1, 8, 48, 7, 48, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM),
                                section(24, 1, 24, 16, 6, 16, SpecialStructureDefinition.SectionKind.WATCHTOWER)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("simple_dungeon")),
                create("outpost/skeleton", SpecialStructureDefinition.Theme.SKELETON_OUTPOST,
                        3, StructurePiece.Rarity.UNCOMMON, 5, 32, -1, 1, all,
                        64, 14, 64, 1, SpecialStructureDefinition.Population.SKELETONS,
                        List.of(
                                section(8, 1, 8, 48, 9, 48, SpecialStructureDefinition.SectionKind.WATCHTOWER)),
                        Set.of(Connector.Direction.EAST, Connector.Direction.WEST),
                        vanillaLoot("pillager_outpost")),
                create("outpost/illager", SpecialStructureDefinition.Theme.ILLAGER_OUTPOST,
                        2, StructurePiece.Rarity.RARE, 8, 32, 0, 1, all,
                        64, 16, 64, 1, SpecialStructureDefinition.Population.PILLAGERS,
                        List.of(
                                section(8, 1, 8, 48, 10, 48, SpecialStructureDefinition.SectionKind.WATCHTOWER),
                                section(20, 1, 20, 24, 6, 24, SpecialStructureDefinition.SectionKind.HOUSE)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("pillager_outpost")),
                create("outpost/piglin", SpecialStructureDefinition.Theme.PIGLIN_OUTPOST,
                        2, StructurePiece.Rarity.RARE, 10, 32, -1, 0, all,
                        64, 14, 64, 1, SpecialStructureDefinition.Population.PIGLINS,
                        List.of(
                                section(8, 1, 8, 48, 8, 48, SpecialStructureDefinition.SectionKind.TREASURY)),
                        Set.of(Connector.Direction.NORTH, Connector.Direction.SOUTH),
                        vanillaLoot("bastion_treasure")),
                create("outpost/wither_skeleton", SpecialStructureDefinition.Theme.WITHER_SKELETON_OUTPOST,
                        1, StructurePiece.Rarity.VERY_RARE, 14, 32, -1, 0,
                        Set.of(RegionCatalog.ANCIENT_ID, RegionCatalog.CORRUPTED_ID),
                        64, 16, 64, 1, SpecialStructureDefinition.Population.WITHER_SKELETONS,
                        List.of(
                                section(8, 1, 8, 48, 10, 48, SpecialStructureDefinition.SectionKind.DUNGEON_ROOM)),
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
            Set<Connector> openConnectors) {
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
}
