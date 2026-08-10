package com.labrinthmc.labrinth.world.generation;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/** Immutable cardinal neighbor connection state for one generation cell. */
public record GenerationNeighbors(Set<GenerationGrid.Direction> connected) {
    public GenerationNeighbors {
        Objects.requireNonNull(connected, "connected");
        EnumSet<GenerationGrid.Direction> copy = connected.isEmpty()
                ? EnumSet.noneOf(GenerationGrid.Direction.class)
                : EnumSet.copyOf(connected);
        connected = Collections.unmodifiableSet(copy);
    }

    public static GenerationNeighbors forCell(long worldSeed, GenerationGrid.Cell cell) {
        EnumSet<GenerationGrid.Direction> connections = EnumSet.noneOf(GenerationGrid.Direction.class);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            if ((GenerationSeeds.connectionSeed(worldSeed, cell, direction) & 1L) == 0L) {
                connections.add(direction);
            }
        }
        return new GenerationNeighbors(connections);
    }

    public boolean hasConnection(GenerationGrid.Direction direction) {
        return connected.contains(direction);
    }
}
