package com.labrinthmc.labrinth.world.corridor;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.GenerationConnectionRules;
import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.EnumSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

/** Non-straight and width-specific Phase 4 corridor definitions and shell rendering. */
public final class CorridorVariants {
    private static final int HEIGHT = StraightCorridor.HEIGHT;
    private static final int PATH_WIDTH = StraightCorridor.WIDTH;
    private static final int APERTURE_WIDTH = PATH_WIDTH - 2;
    private static final int JUNCTION_SIZE = GenerationGrid.CELL_SIZE_BLOCKS;
    private static final int DEAD_END_LENGTH = GenerationGrid.CELL_SIZE_BLOCKS;
    private static final int WIDE_WIDTH = 9;
    private static final int NARROW_WIDTH = 5;
    private static final int LIGHT_SPACING = 8;

    private static final StructurePiece LEFT_TURN = createDefinition(
            "left_turn", StructurePiece.Kind.CORRIDOR, JUNCTION_SIZE, JUNCTION_SIZE,
            Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST));
    private static final StructurePiece RIGHT_TURN = createDefinition(
            "right_turn", StructurePiece.Kind.CORRIDOR, JUNCTION_SIZE, JUNCTION_SIZE,
            Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.WEST));
    private static final StructurePiece T_JUNCTION = createDefinition(
            "t_junction", StructurePiece.Kind.JUNCTION, JUNCTION_SIZE, JUNCTION_SIZE,
            Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.EAST,
                    GenerationGrid.Direction.WEST));
    private static final StructurePiece FOUR_WAY = createDefinition(
            "four_way", StructurePiece.Kind.JUNCTION, JUNCTION_SIZE, JUNCTION_SIZE,
            EnumSet.allOf(GenerationGrid.Direction.class));
    private static final StructurePiece DEAD_END = createDefinition(
            "dead_end", StructurePiece.Kind.CORRIDOR, PATH_WIDTH, DEAD_END_LENGTH,
            Set.of(GenerationGrid.Direction.NORTH));
    private static final StructurePiece WIDE = createDefinition(
            "wide", StructurePiece.Kind.CORRIDOR, WIDE_WIDTH, GenerationGrid.CELL_SIZE_BLOCKS,
            Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));
    private static final StructurePiece NARROW = createDefinition(
            "narrow", StructurePiece.Kind.CORRIDOR, NARROW_WIDTH, GenerationGrid.CELL_SIZE_BLOCKS,
            Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH));

    private static final Map<StructurePiece, Geometry> GEOMETRIES = createGeometries();

    private CorridorVariants() {
    }

    static StructurePiece definition(CorridorKind kind) {
        return switch (Objects.requireNonNull(kind, "kind")) {
            case LEFT_TURN -> LEFT_TURN;
            case RIGHT_TURN -> RIGHT_TURN;
            case T_JUNCTION -> T_JUNCTION;
            case FOUR_WAY -> FOUR_WAY;
            case DEAD_END -> DEAD_END;
            case WIDE_CORRIDOR -> WIDE;
            case NARROW_CORRIDOR -> NARROW;
            default -> throw new IllegalArgumentException("not a non-straight corridor variant: " + kind);
        };
    }

    static boolean supports(StructurePiece definition) {
        return GEOMETRIES.containsKey(definition);
    }

    static void place(
            ChunkAccess chunk,
            PlacedStructurePiece placed,
            Set<GenerationGrid.Direction> openDirections) {
        place(chunk, placed, openDirections, RegionCatalog.standard());
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
        ChunkPos chunkPos = chunk.getPos();
        int minChunkX = chunkPos.getMinBlockX();
        int minChunkZ = chunkPos.getMinBlockZ();
        int maxChunkX = minChunkX + GenerationGrid.CHUNK_SIZE_BLOCKS;
        int maxChunkZ = minChunkZ + GenerationGrid.CHUNK_SIZE_BLOCKS;
        var bounds = placed.bounds();
        long minX = Math.max(bounds.minBlockX(), (long) minChunkX);
        long maxX = Math.min(bounds.maxBlockXExclusive(), (long) maxChunkX);
        long minZ = Math.max(bounds.minBlockZ(), (long) minChunkZ);
        long maxZ = Math.min(bounds.maxBlockZExclusive(), (long) maxChunkZ);
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();

        // Evaluate only this chunk's intersection instead of scanning the full
        // 64-by-64 junction for each of its sixteen materializing chunks.
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
                    if (state.isAir()) {
                        continue;
                    }
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

    static BlockState blockStateAt(
            PlacedStructurePiece placed,
            int worldX,
            int worldY,
            int worldZ,
            Set<GenerationGrid.Direction> openDirections) {
        return blockStateAt(
                placed,
                worldX,
                worldY,
                worldZ,
                openDirections,
                RegionCatalog.standard());
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

        LocalPoint local = inverseTransform(placed, worldX, worldY, worldZ, geometry.width(), geometry.depth());
        return local == null
                ? Blocks.AIR.defaultBlockState()
                : blockStateForLocal(
                        placed,
                        geometry,
                        local.x(),
                        local.y(),
                        local.z(),
                        placed.rotation(),
                        openDirections,
                        region);
    }

    private static Map<StructurePiece, Geometry> createGeometries() {
        IdentityHashMap<StructurePiece, Geometry> geometries = new IdentityHashMap<>();
        geometries.put(LEFT_TURN, routedGeometry(LEFT_TURN));
        geometries.put(RIGHT_TURN, routedGeometry(RIGHT_TURN));
        geometries.put(T_JUNCTION, routedGeometry(T_JUNCTION));
        geometries.put(FOUR_WAY, routedGeometry(FOUR_WAY));
        geometries.put(DEAD_END, rectangleGeometry(DEAD_END));
        geometries.put(WIDE, rectangleGeometry(WIDE));
        geometries.put(NARROW, rectangleGeometry(NARROW));
        return Map.copyOf(geometries);
    }

    private static Geometry routedGeometry(StructurePiece definition) {
        return new Geometry(
                Shape.ROUTED,
                definition.width(),
                definition.depth(),
                PATH_WIDTH,
                baseDirections(definition),
                APERTURE_WIDTH);
    }

    private static Geometry rectangleGeometry(StructurePiece definition) {
        return new Geometry(
                Shape.RECTANGLE,
                definition.width(),
                definition.depth(),
                definition.width(),
                baseDirections(definition),
                APERTURE_WIDTH);
    }

    private static Set<GenerationGrid.Direction> baseDirections(StructurePiece definition) {
        EnumSet<GenerationGrid.Direction> directions = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (Connector connector : definition.connectors()) {
            directions.add(fromConnectorDirection(connector.direction()));
        }
        return Set.copyOf(directions);
    }

    private static StructurePiece createDefinition(
            String path,
            StructurePiece.Kind kind,
            int width,
            int depth,
            Set<GenerationGrid.Direction> directions) {
        return StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "corridor/" + path),
                        ResourceLocation.fromNamespaceAndPath("labrinth", "generated/" + path),
                        kind,
                        width,
                        HEIGHT,
                        depth)
                .weight(1)
                .rotations(EnumSet.allOf(StructurePiece.Rotation.class))
                .mirror(StructurePiece.Mirror.NONE)
                .connectors(directions.stream()
                        .sorted()
                        .map(direction -> connector(direction, width, depth))
                        .toList())
                .build();
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
                    HEIGHT - 2,
                    StructurePiece.Rotation.NONE,
                    true);
            case EAST -> new Connector(
                    new Connector.Position(width, 1, depth / 2),
                    Connector.Direction.EAST,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    HEIGHT - 2,
                    StructurePiece.Rotation.NONE,
                    true);
            case SOUTH -> new Connector(
                    new Connector.Position(width / 2, 1, depth),
                    Connector.Direction.SOUTH,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    HEIGHT - 2,
                    StructurePiece.Rotation.NONE,
                    true);
            case WEST -> new Connector(
                    new Connector.Position(0, 1, depth / 2),
                    Connector.Direction.WEST,
                    Connector.Type.STANDARD,
                    APERTURE_WIDTH,
                    HEIGHT - 2,
                    StructurePiece.Rotation.NONE,
                    true);
        };
    }

    private static BlockState blockStateForLocal(
            PlacedStructurePiece placed,
            Geometry geometry,
            int localX,
            int localY,
            int localZ,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> openDirections,
            RegionDefinition region) {
        GenerationConnectionRules.LocalCenter center =
                GenerationConnectionRules.localCellCenter(placed);
        if (!geometry.pathAt(localX, localZ, center.x(), center.z())) {
            return Blocks.AIR.defaultBlockState();
        }
        BlockState state;
        if (localY == 0) {
            state = Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else if (localY == HEIGHT - 1) {
            state = isLight(geometry, localX, localZ, center)
                    ? Blocks.SEA_LANTERN.defaultBlockState()
                    : Blocks.POLISHED_DEEPSLATE.defaultBlockState();
        } else if (localY == 1 && localX == center.x()
                && Math.floorMod(localZ, LIGHT_SPACING) == LIGHT_SPACING / 2) {
            state = Blocks.GRAY_CARPET.defaultBlockState();
        } else if (localY == HEIGHT - 2
                && Math.floorMod(localZ, LIGHT_SPACING * 2) == LIGHT_SPACING
                && localX > 0 && localX < geometry.width() - 1) {
            state = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        } else {
            state = hasWall(placed, geometry, localX, localZ, rotation, openDirections, center)
                    ? Blocks.DEEPSLATE_BRICKS.defaultBlockState()
                    : Blocks.AIR.defaultBlockState();
        }
        return region.paletteState(state, localY, HEIGHT, localX, localZ);
    }

    private static boolean isLight(
            Geometry geometry,
            int localX,
            int localZ,
            GenerationConnectionRules.LocalCenter center) {
        int centerX = center.x();
        int centerZ = center.z();
        if (geometry.shape() == Shape.RECTANGLE) {
            return localX == centerX && localZ % LIGHT_SPACING == LIGHT_SPACING / 2;
        }
        return (localX == centerX && localZ % LIGHT_SPACING == LIGHT_SPACING / 2)
                || (localZ == centerZ && localX % LIGHT_SPACING == LIGHT_SPACING / 2);
    }

    private static boolean hasWall(
            PlacedStructurePiece placed,
            Geometry geometry,
            int localX,
            int localZ,
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
            GenerationGrid.Direction localDirection,
            StructurePiece.Rotation rotation,
            Set<GenerationGrid.Direction> openDirections,
            GenerationConnectionRules.LocalCenter localCenter) {
        if (!geometry.connectors().contains(localDirection)
                || !openDirections.contains(localDirection.rotated(rotation))) {
            return false;
        }
        int center = localDirection == GenerationGrid.Direction.NORTH
                || localDirection == GenerationGrid.Direction.SOUTH
                ? localCenter.x()
                : localCenter.z();
        int across = localDirection == GenerationGrid.Direction.NORTH
                || localDirection == GenerationGrid.Direction.SOUTH ? localX : localZ;
        boolean atFace = switch (localDirection) {
            case NORTH -> localZ == 0;
            case EAST -> localX == geometry.width() - 1;
            case SOUTH -> localZ == geometry.depth() - 1;
            case WEST -> localX == 0;
        };
        return atFace && Math.abs(across - center) <= geometry.apertureWidth() / 2;
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
            int worldZ,
            int width,
            int depth) {
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
            default -> throw new IllegalStateException("unhandled corridor rotation: " + placed.rotation());
        }
        return new LocalPoint(
                Math.toIntExact(localX),
                worldY - placed.origin().y(),
                Math.toIntExact(localZ));
    }

    private static GenerationGrid.Direction fromConnectorDirection(Connector.Direction direction) {
        return switch (direction) {
            case NORTH -> GenerationGrid.Direction.NORTH;
            case EAST -> GenerationGrid.Direction.EAST;
            case SOUTH -> GenerationGrid.Direction.SOUTH;
            case WEST -> GenerationGrid.Direction.WEST;
            case UP, DOWN -> throw new IllegalArgumentException("corridor variants are horizontal only");
        };
    }

    private static Geometry geometryFor(StructurePiece definition) {
        Geometry geometry = GEOMETRIES.get(definition);
        if (geometry == null) {
            throw new IllegalArgumentException("placed piece is not a supported corridor variant");
        }
        return geometry;
    }

    private enum Shape {
        RECTANGLE,
        ROUTED
    }

    private record Geometry(
            Shape shape,
            int width,
            int depth,
            int pathWidth,
            Set<GenerationGrid.Direction> connectors,
            int apertureWidth) {
        private Geometry {
            connectors = Set.copyOf(connectors);
        }

        private boolean pathAt(int x, int z, int centerX, int centerZ) {
            if (x < 0 || x >= width || z < 0 || z >= depth) {
                return false;
            }
            if (shape == Shape.RECTANGLE) {
                return true;
            }
            int halfWidth = pathWidth / 2;
            return (connectors.contains(GenerationGrid.Direction.NORTH)
                            && x >= centerX - halfWidth && x <= centerX + halfWidth && z <= centerZ)
                    || (connectors.contains(GenerationGrid.Direction.EAST)
                            && z >= centerZ - halfWidth && z <= centerZ + halfWidth && x >= centerX)
                    || (connectors.contains(GenerationGrid.Direction.SOUTH)
                            && x >= centerX - halfWidth && x <= centerX + halfWidth && z >= centerZ)
                    || (connectors.contains(GenerationGrid.Direction.WEST)
                            && z >= centerZ - halfWidth && z <= centerZ + halfWidth && x <= centerX);
        }
    }

    private record LocalPoint(int x, int y, int z) {
    }
}
