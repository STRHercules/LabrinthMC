package com.labrinthmc.labrinth.world.connector;

import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import java.util.Objects;
import java.util.Optional;

/** Deterministic connector matching and origin calculation. */
public final class ConnectorAlignment {
    private ConnectorAlignment() {
    }

    /** Try the candidate's allowed transforms in a fixed order. */
    public static Optional<PlacedStructurePiece> align(
            PlacedStructurePiece anchor,
            int anchorConnectorIndex,
            StructurePiece candidate,
            int candidateConnectorIndex) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(candidate, "candidate");
        if (anchorConnectorIndex < 0 || anchorConnectorIndex >= anchor.connectors().size()
                || candidateConnectorIndex < 0 || candidateConnectorIndex >= candidate.connectors().size()) {
            return Optional.empty();
        }
        Connector anchorConnector = anchor.connector(anchorConnectorIndex);
        for (StructurePiece.Rotation rotation : StructurePiece.Rotation.values()) {
            for (StructurePiece.Mirror mirror : StructurePiece.Mirror.values()) {
                Optional<PlacedStructurePiece> aligned = align(
                        anchor,
                        anchorConnector,
                        candidate,
                        candidateConnectorIndex,
                        rotation,
                        mirror);
                if (aligned.isPresent()) {
                    return aligned;
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<PlacedStructurePiece> align(
            PlacedStructurePiece anchor,
            Connector anchorConnector,
            StructurePiece candidate,
            Connector candidateConnector) {
        int candidateIndex = candidate.connectors().indexOf(candidateConnector);
        int anchorIndex = anchor.connectors().indexOf(anchorConnector);
        return candidateIndex < 0 || anchorIndex < 0
                ? Optional.empty()
                : align(anchor, anchorIndex, candidate, candidateIndex);
    }

    public static Optional<PlacedStructurePiece> align(
            PlacedStructurePiece anchor,
            Connector anchorConnector,
            StructurePiece candidate,
            int candidateConnectorIndex,
            StructurePiece.Rotation rotation,
            StructurePiece.Mirror mirror) {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(anchorConnector, "anchorConnector");
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(rotation, "rotation");
        Objects.requireNonNull(mirror, "mirror");
        if (candidateConnectorIndex < 0 || candidateConnectorIndex >= candidate.connectors().size()) {
            return Optional.empty();
        }
        if (!candidate.allows(rotation, mirror)) {
            return Optional.empty();
        }

        // Transform around zero first. Subtracting that endpoint from the
        // anchor endpoint yields the unique origin for this candidate pose.
        PlacedStructurePiece atZero = candidate.placedAt(
                new StructurePiece.BlockPoint(0, 0, 0), rotation, mirror);
        Connector candidateConnector = atZero.connector(candidateConnectorIndex);
        if (!anchorConnector.compatibleWith(candidateConnector)) {
            return Optional.empty();
        }

        Connector.Position anchorPosition = anchorConnector.position();
        Connector.Position candidatePosition = candidateConnector.position();
        StructurePiece.BlockPoint origin = new StructurePiece.BlockPoint(
                Math.subtractExact(anchorPosition.x(), candidatePosition.x()),
                Math.subtractExact(anchorPosition.y(), candidatePosition.y()),
                Math.subtractExact(anchorPosition.z(), candidatePosition.z()));
        return Optional.of(candidate.placedAt(origin, rotation, mirror));
    }
}
