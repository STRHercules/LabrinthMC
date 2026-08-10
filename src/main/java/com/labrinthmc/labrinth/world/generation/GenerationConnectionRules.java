package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import java.util.Objects;

/** Shared boundary and aperture rules for connections between generated cells. */
public final class GenerationConnectionRules {
    private GenerationConnectionRules() {
    }

    /**
     * A connection is decided from the two immutable cell selections. It must
     * land on the shared cell boundary and match the full connector profile;
     * no neighboring chunk or world lookup is needed.
     */
    public static boolean compatible(
            PlacedStructurePiece current,
            GenerationGrid.Cell currentCell,
            GenerationGrid.Direction direction,
            PlacedStructurePiece neighbor,
            GenerationGrid.Cell neighborCell) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(currentCell, "currentCell");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(neighbor, "neighbor");
        Objects.requireNonNull(neighborCell, "neighborCell");

        Connector currentConnector = connector(current, direction);
        Connector neighborConnector = connector(neighbor, direction.opposite());
        return currentConnector != null
                && neighborConnector != null
                && isCellBoundaryConnector(currentConnector, currentCell, direction)
                && isCellBoundaryConnector(neighborConnector, neighborCell, direction.opposite())
                && currentConnector.position().equals(neighborConnector.position())
                && currentConnector.compatibleWith(neighborConnector);
    }

    private static boolean isCellBoundaryConnector(
            Connector connector,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction) {
        long cellX = GenerationGrid.blockOriginX(cell);
        long cellZ = GenerationGrid.blockOriginZ(cell);
        return switch (direction) {
            case NORTH -> connector.position().z() == cellZ;
            case EAST -> connector.position().x() == cellX + GenerationGrid.CELL_SIZE_BLOCKS;
            case SOUTH -> connector.position().z() == cellZ + GenerationGrid.CELL_SIZE_BLOCKS;
            case WEST -> connector.position().x() == cellX;
        };
    }

    private static Connector connector(
            PlacedStructurePiece piece,
            GenerationGrid.Direction direction) {
        Connector.Direction connectorDirection = switch (direction) {
            case NORTH -> Connector.Direction.NORTH;
            case EAST -> Connector.Direction.EAST;
            case SOUTH -> Connector.Direction.SOUTH;
            case WEST -> Connector.Direction.WEST;
        };
        return piece.connectors().stream()
                .filter(candidate -> candidate.direction() == connectorDirection)
                .findFirst()
                .orElse(null);
    }
}
