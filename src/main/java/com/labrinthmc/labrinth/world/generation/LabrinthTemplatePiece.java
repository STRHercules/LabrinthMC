package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Adapter for authored template content without giving a template control of
 * Labrinth ownership. Current procedural pieces use the same contract, so an
 * NBT template can be added later without creating another placement system.
 */
public record LabrinthTemplatePiece(
        StructurePiece definition,
        ResourceLocation processorSet,
        ResourceLocation connectorProfile,
        ResourceLocation populationProfile,
        Set<ResourceLocation> tags) {
    public LabrinthTemplatePiece {
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(processorSet, "processorSet");
        Objects.requireNonNull(connectorProfile, "connectorProfile");
        Objects.requireNonNull(populationProfile, "populationProfile");
        tags = Set.copyOf(Objects.requireNonNull(tags, "tags"));
        if (definition.template().getNamespace().isBlank()
                || definition.template().getPath().isBlank()) {
            throw new IllegalArgumentException("template identifiers must be complete");
        }
        validateConnectors(definition);
    }

    public static LabrinthTemplatePiece procedural(StructurePiece definition) {
        Objects.requireNonNull(definition, "definition");
        return new LabrinthTemplatePiece(
                definition,
                ResourceLocation.fromNamespaceAndPath("labrinth", "processor/standard"),
                ResourceLocation.fromNamespaceAndPath("labrinth", "connector/standard_door"),
                ResourceLocation.fromNamespaceAndPath("labrinth", "population/none"),
                Set.of(ResourceLocation.fromNamespaceAndPath(
                        "labrinth", definition.kind().name().toLowerCase())));
    }

    public PlacedStructurePiece placeAt(
            StructurePiece.BlockPoint origin,
            StructurePiece.Rotation rotation,
            StructurePiece.Mirror mirror) {
        return definition.placedAt(origin, rotation, mirror);
    }

    public List<Connector> externalConnectors() {
        // Connector scope is owned by the surrounding compound definition;
        // the normalized piece contract deliberately does not infer it from
        // a donor marker's required/open state.
        return definition.connectors();
    }

    public boolean eligible(int depth, ResourceLocation region, int floor) {
        return definition.minDepth() <= depth
                && definition.maxDepth() >= depth
                && (definition.allowedRegions().isEmpty()
                || definition.allowedRegions().contains(region));
    }

    private static void validateConnectors(StructurePiece definition) {
        for (Connector connector : definition.connectors()) {
            if (connector.width() <= 0 || connector.height() <= 0) {
                throw new IllegalArgumentException("template connector dimensions must be positive");
            }
        }
    }
}
