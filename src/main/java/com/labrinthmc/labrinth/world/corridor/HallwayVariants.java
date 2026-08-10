package com.labrinthmc.labrinth.world.corridor;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.GenerationConnectionRules;
import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.chunk.ChunkAccess;

/**
 * Shape-aware hallway definitions.  All variants keep the same five-block
 * doorway profile, so changing the shell size never breaks a cell boundary.
 * Ramp pieces use level landings at both ends; their stair run is internal to
 * the cell and therefore remains compatible with either neighboring floor.
 */
final class HallwayVariants {
    private static final int CELL_SIZE = GenerationGrid.CELL_SIZE_BLOCKS;
    private static final int APERTURE_WIDTH = 5;
    private static final int APERTURE_HEIGHT = 4;
    private static final int STANDARD_WIDTH = 7;
    private static final int GRAND_WIDTH = 11;
    private static final int STANDARD_HEIGHT = 6;
    private static final int GRAND_HEIGHT = 10;

    private static final Map<CorridorKind, StructurePiece> DEFINITIONS = createDefinitions();
    private static final Map<StructurePiece, Geometry> GEOMETRIES = createGeometries();

    private HallwayVariants() {
    }

    static StructurePiece definition(CorridorKind kind) {
        StructurePiece definition = DEFINITIONS.get(Objects.requireNonNull(kind, "kind"));
        if (definition == null) {
            throw new IllegalArgumentException("not an expanded hallway variant: " + kind);
        }
        return definition;
    }

    static boolean supports(StructurePiece definition) {
        return GEOMETRIES.containsKey(definition);
    }

    static void place(
            ChunkAccess chunk,
            PlacedStructurePiece placed,
            Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        Objects.requireNonNull(chunk, "chunk");
        Objects.requireNonNull(placed, "placed");
        Objects.requireNonNull(openDirections, "openDirections");
        Objects.requireNonNull(region, "region");
        var bounds = placed.bounds();
        ChunkPos chunkPos = chunk.getPos();
        long minX = Math.max(bounds.minBlockX(), (long) chunkPos.getMinBlockX());
        long maxX = Math.min(bounds.maxBlockXExclusive(),
                (long) chunkPos.getMinBlockX() + GenerationGrid.CHUNK_SIZE_BLOCKS);
        long minZ = Math.max(bounds.minBlockZ(), (long) chunkPos.getMinBlockZ());
        long maxZ = Math.min(bounds.maxBlockZExclusive(),
                (long) chunkPos.getMinBlockZ() + GenerationGrid.CHUNK_SIZE_BLOCKS);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
        for (long worldZ = minZ; worldZ < maxZ; worldZ++) {
            for (long worldX = minX; worldX < maxX; worldX++) {
                for (int worldY = bounds.minY(); worldY < bounds.maxYExclusive(); worldY++) {
                    BlockState state = blockStateAt(
                            placed,
                            Math.toIntExact(worldX),
                            worldY,
                            Math.toIntExact(worldZ),
                            openDirections,
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

    static BlockState blockStateAt(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ,
            Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        Objects.requireNonNull(placed, "placed");
        Objects.requireNonNull(openDirections, "openDirections");
        Objects.requireNonNull(region, "region");
        Geometry geometry = geometryFor(placed.definition());
        var bounds = placed.bounds();
        if (worldX < bounds.minBlockX() || worldX >= bounds.maxBlockXExclusive()
                || worldZ < bounds.minBlockZ() || worldZ >= bounds.maxBlockZExclusive()
                || worldY < bounds.minY() || worldY >= bounds.maxYExclusive()) {
            return Blocks.AIR.defaultBlockState();
        }
        LocalPoint local = inverseTransform(placed, worldX, worldY, worldZ);
        if (local == null || !geometry.pathAt(local.x(), local.z(),
                GenerationConnectionRules.localCellCenter(placed).x(),
                GenerationConnectionRules.localCellCenter(placed).z())) {
            return Blocks.AIR.defaultBlockState();
        }

        int floorY = geometry.floorY(local.z());
        int ceilingY = floorY + geometry.clearHeight();
        BlockState state;
        if (local.y() == floorY) {
            GenerationConnectionRules.LocalCenter center =
                    GenerationConnectionRules.localCellCenter(placed);
            state = geometry.staircase() && geometry.isStep(
                    local.x(), local.z(), center.x(), center.z())
                    ? stairState(
                            geometry,
                            local.z(),
                            placed.rotation())
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else if (local.y() < floorY) {
            // Give raised ramp sections a solid, stepped foundation instead of
            // leaving visible voids below the narrow stair run.
            state = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else if (local.y() == ceilingY) {
            state = isLight(geometry, local.x(), local.z(),
                    GenerationConnectionRules.localCellCenter(placed))
                    ? Blocks.SEA_LANTERN.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else {
            // Keep the shell closed above a ramp's lower ceiling section.
            // Those upper blocks are interior headroom, but an open boundary
            // there would recreate the grand-to-standard vertical gap.
            state = hasWall(
                    placed,
                    geometry,
                    local.x(),
                    local.z(),
                    local.y(),
                    placed.rotation(),
                    openDirections,
                    GenerationConnectionRules.localCellCenter(placed))
                    ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
        }
        return region.paletteState(
                state,
                local.y(),
                geometry.height(),
                local.x(),
                local.z());
    }

    private static Map<CorridorKind, StructurePiece> createDefinitions() {
        EnumMap<CorridorKind, StructurePiece> values = new EnumMap<>(CorridorKind.class);
        put(values, CorridorKind.CURVED_LEFT, "curved_left", CELL_SIZE, STANDARD_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST));
        put(values, CorridorKind.CURVED_RIGHT, "curved_right", CELL_SIZE, STANDARD_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.WEST));
        put(values, CorridorKind.S_CURVE, "s_curve", STANDARD_WIDTH, STANDARD_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.U_TURN, "u_turn", CELL_SIZE, STANDARD_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST,
                GenerationGrid.Direction.WEST));
        put(values, CorridorKind.INCLINE, "incline", STANDARD_WIDTH, 9, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.DECLINE, "decline", STANDARD_WIDTH, 9, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.STAIRCASE_UP, "staircase_up", STANDARD_WIDTH, 9, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.STAIRCASE_DOWN, "staircase_down", STANDARD_WIDTH, 9, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.GRAND_STRAIGHT, "grand_straight", GRAND_WIDTH, GRAND_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.GRAND_CURVED_LEFT, "grand_curved_left", CELL_SIZE, GRAND_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST));
        put(values, CorridorKind.GRAND_CURVED_RIGHT, "grand_curved_right", CELL_SIZE, GRAND_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.WEST));
        put(values, CorridorKind.GRAND_S_CURVE, "grand_s_curve", GRAND_WIDTH, GRAND_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.GRAND_U_TURN, "grand_u_turn", CELL_SIZE, GRAND_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST,
                GenerationGrid.Direction.WEST));
        put(values, CorridorKind.GRAND_T_JUNCTION, "grand_t_junction", CELL_SIZE, GRAND_HEIGHT, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST,
                GenerationGrid.Direction.WEST));
        put(values, CorridorKind.GRAND_FOUR_WAY, "grand_four_way", CELL_SIZE, GRAND_HEIGHT,
                EnumSet.allOf(GenerationGrid.Direction.class));
        put(values, CorridorKind.GRAND_INCLINE, "grand_incline", GRAND_WIDTH, 14, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.GRAND_DECLINE, "grand_decline", GRAND_WIDTH, 14, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.GRAND_STAIRCASE_UP, "grand_staircase_up", GRAND_WIDTH, 14, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        put(values, CorridorKind.GRAND_STAIRCASE_DOWN, "grand_staircase_down", GRAND_WIDTH, 14, Set.of(
                GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
        return Map.copyOf(values);
    }

    private static void put(
            Map<CorridorKind, StructurePiece> definitions,
            CorridorKind kind,
            String path,
            int width,
            int height,
            Set<GenerationGrid.Direction> directions) {
        int doorwayY = 1 - Math.min(0, rampDelta(kind));
        definitions.put(kind, StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "corridor/" + path),
                        ResourceLocation.fromNamespaceAndPath("labrinth", "generated/" + path),
                        kind == CorridorKind.GRAND_T_JUNCTION || kind == CorridorKind.GRAND_FOUR_WAY
                                ? StructurePiece.Kind.JUNCTION
                                : StructurePiece.Kind.CORRIDOR,
                        width,
                        height,
                        CELL_SIZE)
                .weight(1)
                .rotations(EnumSet.allOf(StructurePiece.Rotation.class))
                .mirror(StructurePiece.Mirror.NONE)
                .connectors(directions.stream()
                        .sorted()
                        .map(direction -> connector(direction, width, doorwayY))
                        .toList())
                .build());
    }

    private static Connector connector(
            GenerationGrid.Direction direction,
            int width,
            int doorwayY) {
        return switch (direction) {
            case NORTH -> new Connector(
                    new Connector.Position(width / 2, doorwayY, 0),
                    Connector.Direction.NORTH,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
            case EAST -> new Connector(
                    new Connector.Position(width, doorwayY, CELL_SIZE / 2),
                    Connector.Direction.EAST,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
            case SOUTH -> new Connector(
                    new Connector.Position(width / 2, doorwayY, CELL_SIZE),
                    Connector.Direction.SOUTH,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
            case WEST -> new Connector(
                    new Connector.Position(0, doorwayY, CELL_SIZE / 2),
                    Connector.Direction.WEST,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    APERTURE_HEIGHT,
                    StructurePiece.Rotation.NONE,
                    true);
        };
    }

    private static Map<StructurePiece, Geometry> createGeometries() {
        IdentityHashMap<StructurePiece, Geometry> values = new IdentityHashMap<>();
        for (Map.Entry<CorridorKind, StructurePiece> entry : DEFINITIONS.entrySet()) {
            CorridorKind kind = entry.getKey();
            int rampDelta = rampDelta(kind);
            boolean staircase = rampDelta != 0;
            Shape shape = kind.name().contains("S_CURVE")
                    ? Shape.S_CURVE
                    : kind.name().contains("CURVED") || kind == CorridorKind.U_TURN
                            || kind == CorridorKind.GRAND_U_TURN
                            ? Shape.CURVED
                            : kind.name().contains("INCLINE") || kind.name().contains("DECLINE")
                                    || kind.name().contains("STAIRCASE")
                                    ? Shape.RAMP
                                    : kind == CorridorKind.GRAND_T_JUNCTION
                                            || kind == CorridorKind.GRAND_FOUR_WAY
                                            ? Shape.ROUTED
                                            : Shape.RECTANGLE;
            int pathWidth = kind.name().startsWith("GRAND_") ? GRAND_WIDTH : STANDARD_WIDTH;
            values.put(entry.getValue(), new Geometry(
                    shape,
                    entry.getValue().width(),
                    entry.getValue().depth(),
                    pathWidth,
                    baseDirections(entry.getValue()),
                    entry.getValue().height(),
                    rampDelta,
                    staircase));
        }
        return Map.copyOf(values);
    }

    private static int rampDelta(CorridorKind kind) {
        return switch (kind) {
            case INCLINE -> 2;
            case STAIRCASE_UP -> 3;
            case GRAND_INCLINE -> 4;
            case GRAND_STAIRCASE_UP -> 4;
            case DECLINE -> -2;
            case STAIRCASE_DOWN -> -3;
            case GRAND_DECLINE -> -4;
            case GRAND_STAIRCASE_DOWN -> -4;
            default -> 0;
        };
    }

    /** Declines dip below their shared endpoint floor while staying in-bounds. */
    static int originYOffset(CorridorKind kind) {
        int delta = rampDelta(kind);
        return Math.min(0, delta);
    }

    private static Set<GenerationGrid.Direction> baseDirections(StructurePiece definition) {
        EnumSet<GenerationGrid.Direction> directions = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (Connector connector : definition.connectors()) {
            directions.add(toGridDirection(connector.direction()));
        }
        return Set.copyOf(directions);
    }

    private static BlockState stairState(
            Geometry geometry,
            int localZ,
            StructurePiece.Rotation rotation) {
        int currentFloor = geometry.floorY(localZ);
        Direction localFacing;
        // A bottom-half stair belongs to the higher floor row. Its lower slab
        // then meets the lower row, while FACING points toward the high side.
        // Check the descending edge first when a ramp reaches a one-row peak.
        if (localZ + 1 < geometry.depth()
                && currentFloor > geometry.floorY(localZ + 1)) {
            localFacing = Direction.NORTH;
        } else if (localZ > 0
                && currentFloor > geometry.floorY(localZ - 1)) {
            localFacing = Direction.SOUTH;
        } else {
            throw new IllegalStateException("hallway stair is not on a height transition");
        }
        return Blocks.POLISHED_DEEPSLATE_STAIRS.defaultBlockState()
                .setValue(StairBlock.FACING, rotateDirection(localFacing, rotation))
                .setValue(StairBlock.HALF, Half.BOTTOM);
    }

    private static Direction rotateDirection(
            Direction direction,
            StructurePiece.Rotation rotation) {
        return switch (rotation) {
            case NONE -> direction;
            case CLOCKWISE_90 -> switch (direction) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                default -> direction;
            };
            case CLOCKWISE_180 -> direction.getOpposite();
            case COUNTERCLOCKWISE_90 -> switch (direction) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                default -> direction;
            };
        };
    }

    private static boolean isLight(
            Geometry geometry,
            int localX,
            int localZ,
            GenerationConnectionRules.LocalCenter center) {
        if (geometry.shape() == Shape.RECTANGLE) {
            return localX == center.x() && localZ % 8 == 4;
        }
        return (localX == center.x() && localZ % 8 == 4)
                || (localZ == center.z() && localX % 8 == 4);
    }

    private static boolean hasWall(
            PlacedStructurePiece placed,
            Geometry geometry,
            int localX,
            int localZ,
            int localY,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> openDirections,
            GenerationConnectionRules.LocalCenter center) {
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            int neighborX = localX + directionX(direction);
            int neighborZ = localZ + directionZ(direction);
            if (!geometry.pathAt(neighborX, neighborZ, center.x(), center.z())
                    && !isOpenFaceCell(
                            placed,
                            geometry,
                            localX,
                            localZ,
                            localY,
                            direction,
                            rotation,
                            openDirections,
                            center)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOpenFaceCell(
            PlacedStructurePiece placed,
            Geometry geometry,
            int localX,
            int localZ,
            int localY,
            GenerationGrid.Direction localDirection,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> openDirections,
            GenerationConnectionRules.LocalCenter center) {
        int doorwayFloor = switch (localDirection) {
            case NORTH -> geometry.floorY(0);
            case SOUTH -> geometry.floorY(geometry.depth() - 1);
            case EAST, WEST -> geometry.floorY(center.z());
        };
        if (localY < doorwayFloor + 1 || localY >= doorwayFloor + 1 + APERTURE_HEIGHT
                || !geometry.connectors().contains(localDirection)
                || !openDirections.contains(localDirection.rotated(rotation))) {
            return false;
        }
        boolean atFace = switch (localDirection) {
            case NORTH -> localZ == 0;
            case EAST -> localX == geometry.width() - 1;
            case SOUTH -> localZ == geometry.depth() - 1;
            case WEST -> localX == 0;
        };
        if (!atFace) {
            return false;
        }
        int across = localDirection == GenerationGrid.Direction.NORTH
                || localDirection == GenerationGrid.Direction.SOUTH ? localX : localZ;
        int apertureCenter = localDirection == GenerationGrid.Direction.NORTH
                || localDirection == GenerationGrid.Direction.SOUTH ? center.x() : center.z();
        return Math.abs(across - apertureCenter) <= APERTURE_WIDTH / 2;
    }

    private static int directionX(GenerationGrid.Direction direction) {
        return switch (direction) {
            case EAST -> 1;
            case WEST -> -1;
            default -> 0;
        };
    }

    private static int directionZ(GenerationGrid.Direction direction) {
        return switch (direction) {
            case SOUTH -> 1;
            case NORTH -> -1;
            default -> 0;
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
        long transformedWidth = StructurePiece.transformedWidth(width, depth, placed.rotation());
        long transformedDepth = StructurePiece.transformedDepth(width, depth, placed.rotation());
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
            default -> throw new IllegalStateException("unhandled hallway rotation: " + placed.rotation());
        }
        return new LocalPoint(
                Math.toIntExact(localX),
                worldY - placed.origin().y(),
                Math.toIntExact(localZ));
    }

    private static GenerationGrid.Direction toGridDirection(Connector.Direction direction) {
        return switch (direction) {
            case NORTH -> GenerationGrid.Direction.NORTH;
            case EAST -> GenerationGrid.Direction.EAST;
            case SOUTH -> GenerationGrid.Direction.SOUTH;
            case WEST -> GenerationGrid.Direction.WEST;
            case UP, DOWN -> throw new IllegalArgumentException("expanded hallways are horizontal pieces");
        };
    }

    private static Geometry geometryFor(StructurePiece definition) {
        Geometry geometry = GEOMETRIES.get(definition);
        if (geometry == null) {
            throw new IllegalArgumentException("placed piece is not an expanded hallway variant");
        }
        return geometry;
    }

    private enum Shape {
        RECTANGLE,
        ROUTED,
        CURVED,
        S_CURVE,
        RAMP
    }

    private record Geometry(
            Shape shape,
            int width,
            int depth,
            int pathWidth,
            Set<GenerationGrid.Direction> connectors,
            int height,
            int rampDelta,
            boolean staircase) {
        private Geometry {
            connectors = Set.copyOf(connectors);
            if (height <= 1 || height - 1 - Math.abs(rampDelta) < 1) {
                throw new IllegalArgumentException("hallway ramp does not leave headroom");
            }
        }

        private boolean pathAt(int x, int z, int centerX, int centerZ) {
            if (x < 0 || x >= width || z < 0 || z >= depth) {
                return false;
            }
            int halfWidth = pathWidth / 2;
            return switch (shape) {
                case RECTANGLE -> true;
                case ROUTED -> routedPath(x, z, centerX, centerZ, halfWidth);
                case CURVED -> curvedPath(x, z, centerX, centerZ, halfWidth);
                case S_CURVE -> {
                    double progress = depth <= 1 ? 0.0 : z / (double) (depth - 1);
                    int bend = (int) Math.round(Math.sin(progress * Math.PI * 2.0)
                            * Math.max(2, width / 5));
                    yield Math.abs(x - centerX - bend) <= halfWidth;
                }
                case RAMP -> Math.abs(x - centerX) <= halfWidth;
            };
        }

        private int floorY(int localZ) {
            if (rampDelta == 0 || depth <= 5) {
                return 0;
            }
            int span = depth - 5;
            int progress = Math.max(0, Math.min(span, localZ - 2));
            double t = progress / (double) span;
            double peak = rampDelta < 0 ? 0.35 : 0.65;
            double triangle = t <= peak ? t / peak : (1.0 - t) / (1.0 - peak);
            int offset = (int) Math.round(Math.abs(rampDelta) * triangle);
            // A decline is translated down at placement time. Its endpoint
            // floor therefore remains at the shared Y while its center dips.
            return rampDelta < 0 ? Math.abs(rampDelta) - offset : offset;
        }

        private int clearHeight() {
            return height - 1 - Math.abs(rampDelta);
        }

        private boolean isStep(int x, int z, int centerX, int centerZ) {
            if (!pathAt(x, z, centerX, centerZ)) {
                return false;
            }
            int currentFloor = floorY(z);
            return (z > 0 && currentFloor > floorY(z - 1))
                    || (z + 1 < depth && currentFloor > floorY(z + 1));
        }

        private boolean routedPath(int x, int z, int centerX, int centerZ, int halfWidth) {
            return (connectors.contains(GenerationGrid.Direction.NORTH)
                            && x >= centerX - halfWidth && x <= centerX + halfWidth && z <= centerZ)
                    || (connectors.contains(GenerationGrid.Direction.EAST)
                            && z >= centerZ - halfWidth && z <= centerZ + halfWidth && x >= centerX)
                    || (connectors.contains(GenerationGrid.Direction.SOUTH)
                            && x >= centerX - halfWidth && x <= centerX + halfWidth && z >= centerZ)
                    || (connectors.contains(GenerationGrid.Direction.WEST)
                            && z >= centerZ - halfWidth && z <= centerZ + halfWidth && x <= centerX);
        }

        private boolean curvedPath(int x, int z, int centerX, int centerZ, int halfWidth) {
            if (routedPath(x, z, centerX, centerZ, halfWidth)) {
                return true;
            }
            int dx = x - centerX;
            int dz = z - centerZ;
            boolean corner = (connectors.contains(GenerationGrid.Direction.NORTH) && dz <= 0)
                    || (connectors.contains(GenerationGrid.Direction.SOUTH) && dz >= 0);
            boolean horizontal = (connectors.contains(GenerationGrid.Direction.EAST) && dx >= 0)
                    || (connectors.contains(GenerationGrid.Direction.WEST) && dx <= 0);
            int radius = pathWidth + 1;
            return corner && horizontal && dx * dx + dz * dz <= radius * radius;
        }
    }

    private record LocalPoint(int x, int y, int z) {
    }
}
