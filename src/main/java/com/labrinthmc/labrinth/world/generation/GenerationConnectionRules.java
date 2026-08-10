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

    /** Return whether the placed piece exposes a connector on this cell edge. */
    public static boolean hasBoundaryConnector(
            PlacedStructurePiece piece,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(cell, "cell");
        Objects.requireNonNull(direction, "direction");
        Connector candidate = connector(piece, direction);
        return candidate != null && isCellBoundaryConnector(candidate, cell, direction);
    }

    private static boolean isCellBoundaryConnector(
            Connector connector,
            GenerationGrid.Cell cell,
            GenerationGrid.Direction direction) {
        long cellX = GenerationGrid.blockOriginX(cell);
        long cellZ = GenerationGrid.blockOriginZ(cell);
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

    /**
     * Return the local block coordinate corresponding to the owning cell's
     * center after the placed piece rotation. Renderers use this for doorway
     * and junction apertures so block geometry follows the exact connector
     * coordinate instead of drifting on even-sized rotated pieces.
     */
    public static LocalCenter localCellCenter(PlacedStructurePiece piece) {
        Objects.requireNonNull(piece, "piece");
        int transformedWidth = StructurePiece.transformedWidth(
                piece.definition().width(), piece.definition().depth(), piece.rotation());
        int transformedDepth = StructurePiece.transformedDepth(
                piece.definition().width(), piece.definition().depth(), piece.rotation());
        long cellOriginX = piece.origin().x() - centeredOffset(transformedWidth);
        long cellOriginZ = piece.origin().z() - centeredOffset(transformedDepth);
        if (Math.floorMod(cellOriginX, GenerationGrid.CELL_SIZE_BLOCKS) != 0
                || Math.floorMod(cellOriginZ, GenerationGrid.CELL_SIZE_BLOCKS) != 0) {
            return new LocalCenter(
                    piece.definition().width() / 2,
                    piece.definition().depth() / 2);
        }

        long targetX = cellOriginX + GenerationGrid.CELL_SIZE_BLOCKS / 2;
        long targetZ = cellOriginZ + GenerationGrid.CELL_SIZE_BLOCKS / 2;
        long transformedX = targetX - piece.origin().x();
        long transformedZ = targetZ - piece.origin().z();
        long localX;
        long localZ;
        switch (piece.rotation()) {
            case NONE -> {
                localX = transformedX;
                localZ = transformedZ;
            }
            case CLOCKWISE_90 -> {
                localX = transformedZ;
                localZ = piece.definition().depth() - 1L - transformedX;
            }
            case CLOCKWISE_180 -> {
                localX = piece.definition().width() - 1L - transformedX;
                localZ = piece.definition().depth() - 1L - transformedZ;
            }
            case COUNTERCLOCKWISE_90 -> {
                localX = piece.definition().width() - 1L - transformedZ;
                localZ = transformedX;
            }
            default -> throw new IllegalStateException("unhandled piece rotation: "
                    + piece.rotation());
        }
        return new LocalCenter(Math.toIntExact(localX), Math.toIntExact(localZ));
    }

    private static int centeredOffset(int size) {
        return (GenerationGrid.CELL_SIZE_BLOCKS - size + 1) / 2;
    }

    public record LocalCenter(int x, int z) {
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
