package com.labrinthmc.labrinth.world.generation;

import net.minecraft.util.RandomSource;

/** Pure seed derivation for all deterministic Labrinth decisions. */
public final class GenerationSeeds {
    public static final long DIMENSION_SALT = 0x4C414252494E5448L;
    public static final long REGION_SALT = 0x524547494F4E5F31L;
    public static final long ROOM_SALT = 0x524F4F4D5F534545L;
    public static final long CORRIDOR_SALT = 0x434F525249444F52L;
    public static final long CONTENT_SALT = 0x434F4E54454E5431L;
    public static final long DEPTH_SALT = 0x44455054485F5F31L;
    public static final long VERTICAL_SALT = 0x564552544943414CL;
    public static final long LANDMARK_SALT = 0x4C414E444D41524BL;
    public static final long CONNECTION_SALT = 0x434F4E4E45435431L;
    public static final long CONTEXT_SALT = 0x434F4E5445585431L;
    private static final long CONNECTION_FLOOR_SALT = 0x464C4F4F525F3031L;

    private static final long COORDINATE_X_SALT = 0x632BE59BD9B4E019L;
    private static final long COORDINATE_Z_SALT = 0x9E3779B185EBCA87L;
    private static final long LOCAL_X_SALT = 0xD1B54A32D192ED03L;
    private static final long LOCAL_Z_SALT = 0xABC98388FB8FAC03L;
    private static final long MIX_INCREMENT = 0x9E3779B97F4A7C15L;

    private GenerationSeeds() {
    }

    public static long dimensionSeed(long worldSeed) {
        return mix64(worldSeed ^ DIMENSION_SALT);
    }

    /**
     * Derive a seed from world identity, the dimension salt, cell coordinates,
     * content type, and local position. Long overflow is intentional: the
     * full signed 64-bit value is the stable input to the mixing function.
     */
    public static long seedFor(
            long worldSeed,
            GenerationGrid.Cell cell,
            long structureSalt,
            long localX,
            long localZ) {
        long value = mix64(worldSeed ^ DIMENSION_SALT);
        value = mix64(value ^ cell.x() * COORDINATE_X_SALT);
        value = mix64(value ^ cell.z() * COORDINATE_Z_SALT);
        value = mix64(value ^ structureSalt);
        value = mix64(value ^ localX * LOCAL_X_SALT);
        return mix64(value ^ localZ * LOCAL_Z_SALT);
    }

    public static long regionSeed(long worldSeed, GenerationGrid.Cell cell) {
        return seedFor(worldSeed, cell, REGION_SALT, 0, 0);
    }

    public static long regionSeed(
            long worldSeed,
            GenerationGrid.Cell cell,
            int depth,
            int floorIndex) {
        return seedFor(worldSeed, cell, REGION_SALT, depth, floorIndex);
    }

    public static long roomSeed(long worldSeed, GenerationGrid.Cell cell, long localX, long localZ) {
        return seedFor(worldSeed, cell, ROOM_SALT, localX, localZ);
    }

    public static long corridorSeed(long worldSeed, GenerationGrid.Cell cell, long localX, long localZ) {
        return seedFor(worldSeed, cell, CORRIDOR_SALT, localX, localZ);
    }

    public static long contentSeed(long worldSeed, GenerationGrid.Cell cell, long localX, long localZ) {
        return seedFor(worldSeed, cell, CONTENT_SALT, localX, localZ);
    }

    public static long depthSeed(
            long worldSeed,
            GenerationGrid.Cell cell,
            int floorIndex) {
        return seedFor(worldSeed, cell, DEPTH_SALT, floorIndex, 0);
    }

    public static long verticalSeed(
            long worldSeed,
            GenerationGrid.Cell cell,
            int lowerFloor) {
        return seedFor(worldSeed, cell, VERTICAL_SALT, lowerFloor, 0);
    }

    public static long landmarkSeed(long worldSeed, GenerationGrid.Cell cell, long localX, long localZ) {
        return seedFor(worldSeed, cell, LANDMARK_SALT, localX, localZ);
    }

    public static long contextSeed(long worldSeed, GenerationGrid.Chunk chunk) {
        return seedFor(worldSeed, chunk.cell(), CONTEXT_SALT, chunk.x(), chunk.z());
    }

    /**
     * Canonicalize an undirected cell edge before hashing so both cells make
     * the same connection decision regardless of which one loads first.
     */
    public static long connectionSeed(
            long worldSeed,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction) {
        return connectionSeed(worldSeed, cell, direction, 0);
    }

    /**
     * Derive an undirected edge seed for one floor. Keeping the floor in the
     * edge seed prevents the same horizontal connection graph from repeating
     * on every level while retaining the canonical two-cell ownership rule.
     */
    public static long connectionSeed(
            long worldSeed,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction,
            int floorIndex) {
        GenerationGrid.Cell neighbor = cell.neighbor(direction);
        GenerationGrid.Cell first = cell;
        GenerationGrid.Cell second = neighbor;
        if (compare(first, second) > 0) {
            first = neighbor;
            second = cell;
        }

        long value = mix64(worldSeed ^ DIMENSION_SALT ^ CONNECTION_SALT);
        value = mix64(value ^ floorIndex * CONNECTION_FLOOR_SALT);
        value = mix64(value ^ first.x() * COORDINATE_X_SALT);
        value = mix64(value ^ first.z() * COORDINATE_Z_SALT);
        value = mix64(value ^ second.x() * COORDINATE_X_SALT);
        return mix64(value ^ second.z() * COORDINATE_Z_SALT);
    }

    public static int selectionIndex(long seed, int optionCount) {
        if (optionCount <= 0) {
            throw new IllegalArgumentException("optionCount must be positive");
        }
        return RandomSource.create(seed).nextInt(optionCount);
    }

    public static int regionIndex(long worldSeed, GenerationGrid.Cell cell, int optionCount) {
        return selectionIndex(regionSeed(worldSeed, cell), optionCount);
    }

    public static int roomIndex(long worldSeed, GenerationGrid.Cell cell, long localX, long localZ, int optionCount) {
        return selectionIndex(roomSeed(worldSeed, cell, localX, localZ), optionCount);
    }

    public static int corridorIndex(
            long worldSeed,
            GenerationGrid.Cell cell,
            long localX,
            long localZ,
            int optionCount) {
        return selectionIndex(corridorSeed(worldSeed, cell, localX, localZ), optionCount);
    }

    public static int landmarkIndex(
            long worldSeed,
            GenerationGrid.Cell cell,
            long localX,
            long localZ,
            int optionCount) {
        return selectionIndex(landmarkSeed(worldSeed, cell, localX, localZ), optionCount);
    }

    private static int compare(GenerationGrid.Cell first, GenerationGrid.Cell second) {
        int xComparison = Long.compare(first.x(), second.x());
        return xComparison != 0 ? xComparison : Long.compare(first.z(), second.z());
    }

    private static long mix64(long value) {
        value += MIX_INCREMENT;
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }
}
