package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import java.util.List;
import java.util.Objects;

/** Immutable transformed instance of a reusable {@link StructurePiece}. */
public final class PlacedStructurePiece {
    private final StructurePiece definition;
    private final StructurePiece.BlockPoint origin;
    private final StructurePiece.Rotation rotation;
    private final StructurePiece.Mirror mirror;
    private final GenerationGrid.Bounds bounds;
    private final List<Connector> connectors;

    public PlacedStructurePiece(
            StructurePiece definition,
            StructurePiece.BlockPoint origin,
            StructurePiece.Rotation rotation,
            StructurePiece.Mirror mirror) {
        this.definition = Objects.requireNonNull(definition, "definition");
        this.origin = Objects.requireNonNull(origin, "origin");
        this.rotation = Objects.requireNonNull(rotation, "rotation");
        this.mirror = Objects.requireNonNull(mirror, "mirror");
        if (!definition.allows(rotation, mirror)) {
            throw new IllegalArgumentException("piece does not allow " + rotation + " / " + mirror);
        }
        int transformedWidth = StructurePiece.transformedWidth(definition.width(), definition.depth(), rotation);
        int transformedDepth = StructurePiece.transformedDepth(definition.width(), definition.depth(), rotation);
        bounds = new GenerationGrid.Bounds(
                origin.x(),
                origin.z(),
                Math.addExact(origin.x(), transformedWidth),
                Math.addExact(origin.z(), transformedDepth),
                origin.y(),
                Math.addExact(origin.y(), definition.height()));
        connectors = definition.connectors().stream()
                .map(connector -> connector.transformed(
                        definition.width(), definition.depth(), origin, rotation, mirror))
                .map(connector -> normalizeCellCenter(
                        connector,
                        definition,
                        origin,
                        rotation))
                .toList();
    }

    /**
     * Keep canonical horizontal endpoints on the integer center of their
     * owning cell. The raw transform correctly preserves piece boundaries,
     * but odd-width pieces can otherwise move their cross-axis endpoint by
     * one block when mirrored or rotated 180 degrees.
     */
    private static Connector normalizeCellCenter(
            Connector connector,
            StructurePiece definition,
            StructurePiece.BlockPoint origin,
            StructurePiece.Rotation rotation) {
        if (connector.direction() == Connector.Direction.UP
                || connector.direction() == Connector.Direction.DOWN) {
            return connector;
        }
        int transformedWidth = StructurePiece.transformedWidth(
                definition.width(), definition.depth(), rotation);
        int transformedDepth = StructurePiece.transformedDepth(
                definition.width(), definition.depth(), rotation);
        long cellOriginX = origin.x() - centeredOffset(transformedWidth);
        long cellOriginZ = origin.z() - centeredOffset(transformedDepth);
        if (Math.floorMod(cellOriginX, GenerationGrid.CELL_SIZE_BLOCKS) != 0
                || Math.floorMod(cellOriginZ, GenerationGrid.CELL_SIZE_BLOCKS) != 0) {
            return connector;
        }

        Connector.Position position = connector.position();
        long normalizedX = position.x();
        long normalizedZ = position.z();
        switch (connector.direction()) {
            case NORTH, SOUTH -> normalizedX = cellOriginX + GenerationGrid.CELL_SIZE_BLOCKS / 2;
            case EAST, WEST -> normalizedZ = cellOriginZ + GenerationGrid.CELL_SIZE_BLOCKS / 2;
            default -> throw new IllegalStateException("unhandled horizontal connector: "
                    + connector.direction());
        }
        if (normalizedX == position.x() && normalizedZ == position.z()) {
            return connector;
        }
        return connector.withPosition(new Connector.Position(
                normalizedX,
                position.y(),
                normalizedZ));
    }

    private static int centeredOffset(int size) {
        return (GenerationGrid.CELL_SIZE_BLOCKS - size + 1) / 2;
    }

    public StructurePiece definition() {
        return definition;
    }

    public StructurePiece.BlockPoint origin() {
        return origin;
    }

    public StructurePiece.Rotation rotation() {
        return rotation;
    }

    public StructurePiece.Mirror mirror() {
        return mirror;
    }

    public GenerationGrid.Bounds bounds() {
        return bounds;
    }

    public List<Connector> connectors() {
        return connectors;
    }

    public Connector connector(int index) {
        return connectors.get(index);
    }

    public GenerationGrid.Chunk ownerChunk() {
        return GenerationGrid.ownerChunk(bounds);
    }

    public GenerationGrid.Cell ownerCell() {
        return GenerationGrid.ownerCell(bounds);
    }

    public boolean intersects(GenerationGrid.Chunk chunk) {
        return bounds.intersects(chunk);
    }
}
