package com.labrinthmc.labrinth.world.landmark;

import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.generation.StructurePiece;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Immutable metadata for one deterministic, multi-chunk landmark type. */
public record LandmarkDefinition(
        ResourceLocation id,
        int weight,
        int minSpacingCells,
        int maxFrequencyPerSector,
        StructurePiece piece,
        Set<ResourceLocation> allowedRegions,
        int minDepth,
        int maxDepth,
        int minFloor,
        int maxFloor,
        Set<Connector.Direction> requiredConnections,
        Style style) {
    public LandmarkDefinition {
        Objects.requireNonNull(id, "id");
        if (weight < 0) {
            throw new IllegalArgumentException("landmark weight must not be negative");
        }
        if (minSpacingCells < 1 || maxFrequencyPerSector < 1) {
            throw new IllegalArgumentException("landmark spacing and frequency must be positive");
        }
        Objects.requireNonNull(piece, "piece");
        if (piece.kind() != StructurePiece.Kind.LANDMARK) {
            throw new IllegalArgumentException("landmark pieces must use LANDMARK kind");
        }
        allowedRegions = Set.copyOf(Objects.requireNonNull(allowedRegions, "allowedRegions"));
        if (minDepth < 0 || maxDepth < minDepth) {
            throw new IllegalArgumentException("invalid landmark depth range");
        }
        if (!com.labrinthmc.labrinth.world.generation.VerticalCatalog.isValidFloor(minFloor)
                || !com.labrinthmc.labrinth.world.generation.VerticalCatalog.isValidFloor(maxFloor)
                || maxFloor < minFloor) {
            throw new IllegalArgumentException("invalid landmark floor range");
        }
        requiredConnections = Set.copyOf(Objects.requireNonNull(requiredConnections, "requiredConnections"));
        Objects.requireNonNull(style, "style");
        if (!piece.id().equals(id)) {
            throw new IllegalArgumentException("landmark ID must match its structure piece");
        }
        if (!piece.allowedRegions().isEmpty() && !piece.allowedRegions().containsAll(allowedRegions)) {
            throw new IllegalArgumentException("piece region restrictions must cover landmark regions");
        }
        if (piece.minDepth() > minDepth || piece.maxDepth() < maxDepth) {
            throw new IllegalArgumentException("piece depth range must cover landmark range");
        }
        for (Connector.Direction direction : requiredConnections) {
            boolean present = piece.connectors().stream()
                    .anyMatch(connector -> connector.direction() == direction);
            if (!present) {
                throw new IllegalArgumentException("missing required landmark connection: " + direction);
            }
        }
    }

    public boolean eligible(int depth, int floorIndex, ResourceLocation region) {
        Objects.requireNonNull(region, "region");
        return depth >= minDepth
                && depth <= maxDepth
                && floorIndex >= minFloor
                && floorIndex <= maxFloor
                && (allowedRegions.isEmpty() || allowedRegions.contains(region));
    }

    public boolean connectionsSatisfied(Set<Connector.Direction> availableConnections) {
        Objects.requireNonNull(availableConnections, "availableConnections");
        return availableConnections.containsAll(requiredConnections);
    }

    public enum Style {
        GRAND_HALL,
        CENTRAL_STAIRWELL,
        MASSIVE_STORAGE_COMPLEX,
        GENERATOR_ROOM,
        FLOODED_ATRIUM,
        ABANDONED_STATION,
        ANCIENT_CHAMBER,
        CORRUPTED_NEXUS
    }
}
