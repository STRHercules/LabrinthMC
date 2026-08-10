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
                .toList();
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
