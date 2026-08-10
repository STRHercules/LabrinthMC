package com.labrinthmc.labrinth.world.generation;

/**
 * Canonical horizontal coordinate system for Labrinth generation.
 *
 * <p>Minecraft blocks remain the source coordinate system. Chunks are the
 * placement boundary, and four-by-four chunk cells are the deterministic
 * decision boundary used by later rooms, corridors, and regions. Y remains
 * vanilla block Y and is validated separately by {@link GenerationConstraints}.
 */
public final class GenerationGrid {
    public static final int CHUNK_SIZE_BLOCKS = 16;
    public static final int CHUNKS_PER_CELL = 4;
    public static final int CELL_SIZE_BLOCKS = CHUNK_SIZE_BLOCKS * CHUNKS_PER_CELL;

    private GenerationGrid() {
    }

    public static Chunk chunkForBlock(long blockX, long blockZ) {
        return new Chunk(
                Math.floorDiv(blockX, CHUNK_SIZE_BLOCKS),
                Math.floorDiv(blockZ, CHUNK_SIZE_BLOCKS));
    }

    public static Cell cellForBlock(long blockX, long blockZ) {
        return cellForChunk(
                Math.floorDiv(blockX, CHUNK_SIZE_BLOCKS),
                Math.floorDiv(blockZ, CHUNK_SIZE_BLOCKS));
    }

    public static Cell cellForChunk(long chunkX, long chunkZ) {
        return new Cell(
                Math.floorDiv(chunkX, CHUNKS_PER_CELL),
                Math.floorDiv(chunkZ, CHUNKS_PER_CELL));
    }

    public static long blockOriginX(Cell cell) {
        return Math.multiplyExact(cell.x(), (long) CELL_SIZE_BLOCKS);
    }

    public static long blockOriginZ(Cell cell) {
        return Math.multiplyExact(cell.z(), (long) CELL_SIZE_BLOCKS);
    }

    public static long blockOriginX(Chunk chunk) {
        return Math.multiplyExact(chunk.x(), (long) CHUNK_SIZE_BLOCKS);
    }

    public static long blockOriginZ(Chunk chunk) {
        return Math.multiplyExact(chunk.z(), (long) CHUNK_SIZE_BLOCKS);
    }

    /**
     * The minimum block corner is the immutable placement owner for a piece.
     * Only that chunk decides whether the piece exists; intersecting chunks
     * may later materialize its already-decided bounds without making a new
     * random choice.
     */
    public static Chunk ownerChunk(Bounds bounds) {
        return chunkForBlock(bounds.minBlockX(), bounds.minBlockZ());
    }

    public static Cell ownerCell(Bounds bounds) {
        return cellForBlock(bounds.minBlockX(), bounds.minBlockZ());
    }

    public static boolean ownsDecision(Bounds bounds, Chunk chunk) {
        return ownerChunk(bounds).equals(chunk);
    }

    public static boolean intersects(Bounds bounds, Chunk chunk) {
        return bounds.intersects(chunk);
    }

    public enum Direction {
        NORTH(0, -1),
        EAST(1, 0),
        SOUTH(0, 1),
        WEST(-1, 0);

        private final int xOffset;
        private final int zOffset;

        Direction(int xOffset, int zOffset) {
            this.xOffset = xOffset;
            this.zOffset = zOffset;
        }

        public Cell offset(Cell cell) {
            return new Cell(cell.x() + xOffset, cell.z() + zOffset);
        }

        public Direction opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case SOUTH -> NORTH;
                case WEST -> EAST;
            };
        }

        public Direction rotated(StructurePiece.Rotation rotation) {
            return switch (rotation) {
                case NONE -> this;
                case CLOCKWISE_90 -> switch (this) {
                    case NORTH -> EAST;
                    case EAST -> SOUTH;
                    case SOUTH -> WEST;
                    case WEST -> NORTH;
                };
                case CLOCKWISE_180 -> opposite();
                case COUNTERCLOCKWISE_90 -> switch (this) {
                    case NORTH -> WEST;
                    case WEST -> SOUTH;
                    case SOUTH -> EAST;
                    case EAST -> NORTH;
                };
            };
        }
    }

    public record Cell(long x, long z) {
        public Cell neighbor(Direction direction) {
            return direction.offset(this);
        }
    }

    public record Chunk(long x, long z) {
        public Cell cell() {
            return cellForChunk(x, z);
        }
    }

    /**
     * Half-open block bounds make chunk intersection and Y validation
     * unambiguous: minimum coordinates are included, maximum coordinates are
     * excluded.
     */
    public record Bounds(
            long minBlockX,
            long minBlockZ,
            long maxBlockXExclusive,
            long maxBlockZExclusive,
            int minY,
            int maxYExclusive) {
        public Bounds {
            if (minBlockX >= maxBlockXExclusive || minBlockZ >= maxBlockZExclusive) {
                throw new IllegalArgumentException("horizontal bounds must have positive size");
            }
            if (minY >= maxYExclusive) {
                throw new IllegalArgumentException("vertical bounds must have positive size");
            }
        }

        public boolean intersects(Chunk chunk) {
            long chunkMinX = blockOriginX(chunk);
            long chunkMinZ = blockOriginZ(chunk);
            long chunkMaxX = chunkMinX + CHUNK_SIZE_BLOCKS;
            long chunkMaxZ = chunkMinZ + CHUNK_SIZE_BLOCKS;
            return minBlockX < chunkMaxX
                    && maxBlockXExclusive > chunkMinX
                    && minBlockZ < chunkMaxZ
                    && maxBlockZExclusive > chunkMinZ;
        }

        /**
         * Half-open intersection includes vertical overlap as well as the
         * horizontal footprint. Pieces that only touch at a face are allowed
         * to connect without being treated as collisions.
         */
        public boolean intersects(Bounds other) {
            return minBlockX < other.maxBlockXExclusive
                    && maxBlockXExclusive > other.minBlockX
                    && minBlockZ < other.maxBlockZExclusive
                    && maxBlockZExclusive > other.minBlockZ
                    && minY < other.maxYExclusive
                    && maxYExclusive > other.minY;
        }
    }
}
