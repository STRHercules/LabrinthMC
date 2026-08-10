package com.labrinthmc.labrinth.world.generation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Immutable cardinal neighbor connection state for one generation cell. */
public record GenerationNeighbors(Set<GenerationGrid.Direction> connected) {
    /** Optional loop edges are intentionally common so the maze stays compact. */
    public static final int OPTIONAL_EDGE_CHANCE_PERCENT = 78;
    private static final int FLOOR_RANDOM_STRIDE = 257;
    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "connection_edges");

    public GenerationNeighbors {
        Objects.requireNonNull(connected, "connected");
        EnumSet<GenerationGrid.Direction> copy = connected.isEmpty()
                ? EnumSet.noneOf(GenerationGrid.Direction.class)
                : EnumSet.copyOf(connected);
        connected = Collections.unmodifiableSet(copy);
    }

    public static GenerationNeighbors forCell(long worldSeed, GenerationGrid.Cell cell) {
        return forCell(worldSeed, cell, 0);
    }

    public static GenerationNeighbors forCell(
            long worldSeed,
            GenerationGrid.Cell cell,
            int floorIndex) {
        EnumSet<GenerationGrid.Direction> connections = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            if (isConnected(worldSeed, cell, direction, floorIndex)) {
                connections.add(direction);
            }
        }
        return new GenerationNeighbors(connections);
    }

    /**
     * Live-generator equivalent of {@link #forCell(long, GenerationGrid.Cell)}.
     * The positional factory keeps the decision world-seeded without exposing
     * the seed through common generator APIs.
     */
    public static GenerationNeighbors forCell(
            RandomState randomState,
            GenerationGrid.Cell cell) {
        return forCell(randomState, cell, 0);
    }

    public static GenerationNeighbors forCell(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int floorIndex) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        EnumSet<GenerationGrid.Direction> connections = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            if (isConnected(factory, cell, direction, floorIndex)) {
                connections.add(direction);
            }
        }
        return new GenerationNeighbors(connections);
    }

    public boolean hasConnection(GenerationGrid.Direction direction) {
        return connected.contains(direction);
    }

    private static boolean isConnected(
            long worldSeed,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction,
            int floorIndex) {
        long edgeSeed = GenerationSeeds.connectionSeed(worldSeed, cell, direction, floorIndex);
        return isTreeEdge(cell, direction, floorIndex)
                || Long.remainderUnsigned(edgeSeed, 100L) < OPTIONAL_EDGE_CHANCE_PERCENT;
    }

    private static boolean isConnected(
            PositionalRandomFactory factory,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction,
            int floorIndex) {
        if (isTreeEdge(cell, direction, floorIndex)) {
            return true;
        }
        Edge edge = canonicalEdge(cell, direction);
        RandomSource random = factory.at(
                Math.toIntExact(edge.first().x()),
                Math.addExact(
                        Math.toIntExact(edge.first().z()),
                        Math.multiplyExact(floorIndex, FLOOR_RANDOM_STRIDE)),
                edge.directionFromFirst().ordinal());
        return random.nextInt(100) < OPTIONAL_EDGE_CHANCE_PERCENT;
    }

    /**
     * Independent coin flips produce disconnected islands. Keep one
     * deterministic parent edge for every non-origin cell, then add the
     * seed-derived optional edges to retain loops and variation.
     */
    private static boolean isTreeEdge(
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction,
            int floorIndex) {
        GenerationGrid.Cell neighbor = cell.neighbor(direction);
        GenerationGrid.Cell cellParent = parent(cell, floorIndex);
        GenerationGrid.Cell neighborParent = parent(neighbor, floorIndex);
        return (cellParent != null && cellParent.equals(neighbor))
                || (neighborParent != null && neighborParent.equals(cell));
    }

    private static GenerationGrid.Cell parent(GenerationGrid.Cell cell, int floorIndex) {
        // Pick the axis toward the origin per cell and floor. This keeps the
        // deterministic route to the core while avoiding a repeated X-then-Z
        // backbone on every vertical level.
        boolean preferX = ((cell.x() * 31L + cell.z() * 17L + floorIndex * 13L) & 1L) == 0L;
        if (preferX && cell.x() != 0) {
            return new GenerationGrid.Cell(cell.x() - Long.signum(cell.x()), cell.z());
        }
        if (cell.z() != 0) {
            return new GenerationGrid.Cell(cell.x(), cell.z() - Long.signum(cell.z()));
        }
        if (cell.x() != 0) {
            return new GenerationGrid.Cell(cell.x() - Long.signum(cell.x()), cell.z());
        }
        return null;
    }

    private static Edge canonicalEdge(
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction) {
        GenerationGrid.Cell neighbor = cell.neighbor(direction);
        if (compare(cell, neighbor) <= 0) {
            return new Edge(cell, neighbor, direction);
        }
        return new Edge(neighbor, cell, direction.opposite());
    }

    private static int compare(GenerationGrid.Cell first, GenerationGrid.Cell second) {
        int xComparison = Long.compare(first.x(), second.x());
        return xComparison != 0 ? xComparison : Long.compare(first.z(), second.z());
    }

    private record Edge(
            GenerationGrid.Cell first,
            GenerationGrid.Cell second,
            GenerationGrid.Direction directionFromFirst) {
    }
}
