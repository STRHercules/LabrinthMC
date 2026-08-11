package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable metadata for one origin-owned compound structure.
 *
 * <p>The whole piece is the reservation. Sections are only an authored
 * internal layout and never compete with ordinary cell content.
 */
public record SpecialStructureDefinition(
        ResourceLocation id,
        Theme theme,
        int weight,
        StructurePiece.Rarity rarity,
        int minDepth,
        int maxDepth,
        int minFloor,
        int maxFloor,
        Set<ResourceLocation> allowedRegions,
        StructurePiece piece,
        List<Section> sections,
        int minimumExternalConnections,
        Population population,
        Optional<ResourceLocation> lootTable) {
    public SpecialStructureDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(theme, "theme");
        Objects.requireNonNull(rarity, "rarity");
        Objects.requireNonNull(allowedRegions, "allowedRegions");
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(sections, "sections");
        Objects.requireNonNull(population, "population");
        Objects.requireNonNull(lootTable, "lootTable");
        if (weight < 0 || minDepth < 0 || maxDepth < minDepth
                || minFloor > maxFloor || minimumExternalConnections < 0) {
            throw new IllegalArgumentException("invalid special-structure metadata");
        }
        if (piece.kind() != StructurePiece.Kind.COMPOUND || !piece.id().equals(id)) {
            throw new IllegalArgumentException("compound definitions require a matching COMPOUND piece");
        }
        if (!VerticalCatalog.isValidFloor(minFloor)
                || !VerticalCatalog.isValidFloor(maxFloor)) {
            throw new IllegalArgumentException("special structure floor range is invalid");
        }
        if (piece.minDepth() > minDepth || piece.maxDepth() < maxDepth
                || (!piece.allowedRegions().isEmpty()
                && !piece.allowedRegions().containsAll(allowedRegions))) {
            throw new IllegalArgumentException("piece restrictions must cover definition restrictions");
        }
        allowedRegions = Set.copyOf(allowedRegions);
        sections = List.copyOf(sections);
        for (Section section : sections) {
            if (section.x() < 0 || section.z() < 0
                    || section.x() + section.width() > piece.width()
                    || section.z() + section.depth() > piece.depth()
                    || section.y() < 0
                    || section.y() + section.height() > piece.height()) {
                throw new IllegalArgumentException("compound section exceeds its reservation: " + section);
            }
        }
    }

    public boolean eligible(int depth, int floor, ResourceLocation region) {
        Objects.requireNonNull(region, "region");
        return depth >= minDepth && depth <= maxDepth
                && floor >= minFloor && floor <= maxFloor
                && (allowedRegions.isEmpty() || allowedRegions.contains(region));
    }

    public enum Theme {
        VILLAGE,
        COMPACT_DUNGEON,
        DUNGEON_COMPLEX,
        ENORMOUS_CAVE,
        MASSIVE_HALL,
        ZOMBIE_OUTPOST,
        SKELETON_OUTPOST,
        ILLAGER_OUTPOST,
        PIGLIN_OUTPOST,
        WITHER_SKELETON_OUTPOST
    }

    public enum Population {
        NONE,
        VILLAGERS,
        ZOMBIES,
        SKELETONS,
        PILLAGERS,
        PIGLINS,
        WITHER_SKELETONS,
        SPIDERS
    }

    public enum SectionKind {
        PLAZA,
        HOUSE,
        WORKSHOP,
        FARM,
        HALL,
        DUNGEON_ROOM,
        CELL,
        TREASURY,
        WATCHTOWER
    }

    public record Section(
            int x,
            int y,
            int z,
            int width,
            int height,
            int depth,
            SectionKind kind) {
        public Section {
            Objects.requireNonNull(kind, "kind");
            if (x < 0 || y < 0 || z < 0 || width <= 0 || height <= 0 || depth <= 0) {
                throw new IllegalArgumentException("compound sections must be positive and local");
            }
        }
    }
}
