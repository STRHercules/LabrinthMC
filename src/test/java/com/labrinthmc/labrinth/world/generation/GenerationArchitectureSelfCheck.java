package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.corridor.CorridorCatalog;
import com.labrinthmc.labrinth.world.corridor.CorridorKind;
import com.labrinthmc.labrinth.world.corridor.CorridorSelectionConfig;
import com.labrinthmc.labrinth.world.corridor.StraightCorridor;
import com.labrinthmc.labrinth.world.connector.Connector;
import com.labrinthmc.labrinth.world.connector.ConnectorAlignment;
import com.labrinthmc.labrinth.world.landmark.LandmarkCatalog;
import com.labrinthmc.labrinth.world.landmark.LandmarkDefinition;
import com.labrinthmc.labrinth.world.placement.StructurePlacementValidator;
import com.labrinthmc.labrinth.world.room.RoomCatalog;
import com.labrinthmc.labrinth.world.room.RoomDefinition;
import com.labrinthmc.labrinth.world.room.RoomKind;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Small framework-free executable check for the generation architecture. */
public final class GenerationArchitectureSelfCheck {
    private GenerationArchitectureSelfCheck() {
    }

    public static void main(String[] args) {
        checkCoordinateMapping();
        checkOwnershipAndBounds();
        checkSeedStabilityAndSelection();
        checkNeighborSymmetry();
        checkGenerationOrderIndependence();
        checkContextRepeatability();
        checkStructurePieceSystem();
        checkStraightCorridorSystem();
        checkCorridorCatalogSystem();
        checkRoomCatalogSystem();
        checkContentCatalogSystem();
        checkContinuousExpansionSystem();
        checkVerticalCatalogSystem();
        checkRegionCatalogSystem();
        checkDepthAndLandmarkSystems();
        System.out.println("Generation architecture self-check passed.");
    }

    private static void checkCoordinateMapping() {
        require(GenerationGrid.chunkForBlock(-1, -17).equals(new GenerationGrid.Chunk(-1, -2)),
                "negative block coordinates use floor division");
        require(GenerationGrid.cellForChunk(-1, -4).equals(new GenerationGrid.Cell(-1, -1)),
                "negative chunk coordinates use floor division");
        require(GenerationGrid.cellForBlock(63, 63).equals(new GenerationGrid.Cell(0, 0)),
                "cell maximum remains in the original cell");
        require(GenerationGrid.cellForBlock(64, 64).equals(new GenerationGrid.Cell(1, 1)),
                "cell boundary advances at 64 blocks");
    }

    private static void checkOwnershipAndBounds() {
        GenerationGrid.Bounds bounds = new GenerationGrid.Bounds(-4, -4, 68, 68, 0, 16);
        GenerationGrid.Chunk owner = GenerationGrid.ownerChunk(bounds);
        require(owner.equals(new GenerationGrid.Chunk(-1, -1)), "piece owner is its minimum-corner chunk");
        require(GenerationGrid.ownsDecision(bounds, owner), "owner chunk makes the placement decision");
        require(GenerationGrid.intersects(bounds, new GenerationGrid.Chunk(4, 4)),
                "spanning piece reaches the final intersecting chunk");
        require(!GenerationGrid.intersects(bounds, new GenerationGrid.Chunk(5, 5)),
                "spanning piece does not reach the next chunk");
    }

    private static void checkSeedStabilityAndSelection() {
        long worldSeed = 0x1234_5678_9ABC_DEF0L;
        GenerationGrid.Cell cell = new GenerationGrid.Cell(-12, 7);
        long roomSeed = GenerationSeeds.roomSeed(worldSeed, cell, 9, -3);
        require(roomSeed == GenerationSeeds.roomSeed(worldSeed, cell, 9, -3), "room seed is repeatable");
        require(GenerationSeeds.regionIndex(worldSeed, cell, 7)
                        == GenerationSeeds.regionIndex(worldSeed, cell, 7),
                "region selection is repeatable");
        require(GenerationSeeds.roomIndex(worldSeed, cell, 9, -3, 11)
                        == GenerationSeeds.roomIndex(worldSeed, cell, 9, -3, 11),
                "room selection is repeatable");
        require(GenerationSeeds.corridorIndex(worldSeed, cell, 1, 2, 5)
                        == GenerationSeeds.corridorIndex(worldSeed, cell, 1, 2, 5),
                "corridor selection is repeatable");
        require(GenerationSeeds.contentSeed(worldSeed, cell, 1, 2)
                        == GenerationSeeds.contentSeed(worldSeed, cell, 1, 2),
                "mixed content seed is repeatable");
        require(GenerationSeeds.landmarkIndex(worldSeed, cell, 0, 0, 3)
                        == GenerationSeeds.landmarkIndex(worldSeed, cell, 0, 0, 3),
                "landmark selection is repeatable");
        require(roomSeed != GenerationSeeds.roomSeed(worldSeed + 1, cell, 9, -3),
                "world seed participates in derivation");
    }

    private static void checkNeighborSymmetry() {
        long worldSeed = 987654321L;
        GenerationGrid.Cell cell = new GenerationGrid.Cell(4, -2);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            GenerationNeighbors first = GenerationNeighbors.forCell(worldSeed, cell);
            GenerationNeighbors second = GenerationNeighbors.forCell(worldSeed, cell.neighbor(direction));
            require(first.hasConnection(direction)
                            == second.hasConnection(direction.opposite()),
                    "neighbor edge is symmetric for " + direction);
        }
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                GenerationGrid.Cell candidate = new GenerationGrid.Cell(x, z);
                if (x != 0 || z != 0) {
                    require(!GenerationNeighbors.forCell(worldSeed, candidate).connected().isEmpty(),
                            "every non-origin cell has a deterministic route toward the core");
                }
            }
        }
    }

    private static void checkGenerationOrderIndependence() {
        long worldSeed = 0xCAFEBABE12345678L;
        GenerationGrid.Cell[] cells = {
                new GenerationGrid.Cell(-2, -1),
                new GenerationGrid.Cell(0, 0),
                new GenerationGrid.Cell(3, 4),
                new GenerationGrid.Cell(8, -5)
        };
        long[] forward = new long[cells.length];
        long[] reverse = new long[cells.length];
        for (int i = 0; i < cells.length; i++) {
            forward[i] = decisionFingerprint(worldSeed, cells[i]);
        }
        for (int i = cells.length - 1; i >= 0; i--) {
            reverse[i] = decisionFingerprint(worldSeed, cells[i]);
        }
        require(Arrays.equals(forward, reverse),
                "generation decisions do not depend on evaluation order");
    }

    private static long decisionFingerprint(long worldSeed, GenerationGrid.Cell cell) {
        long fingerprint = GenerationSeeds.regionSeed(worldSeed, cell);
        fingerprint = Long.rotateLeft(fingerprint ^ GenerationSeeds.roomSeed(worldSeed, cell, 1, 2), 11);
        fingerprint = Long.rotateLeft(
                fingerprint ^ GenerationSeeds.corridorSeed(worldSeed, cell, -3, 4), 17);
        fingerprint = Long.rotateLeft(
                fingerprint ^ GenerationSeeds.contentSeed(worldSeed, cell, 5, 6), 19);
        fingerprint = Long.rotateLeft(
                fingerprint ^ GenerationSeeds.landmarkSeed(worldSeed, cell, 0, 0), 23);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            fingerprint = Long.rotateLeft(
                    fingerprint ^ GenerationSeeds.connectionSeed(worldSeed, cell, direction), 7);
        }
        return fingerprint;
    }

    private static void checkContextRepeatability() {
        GenerationGrid.Chunk chunk = new GenerationGrid.Chunk(-5, 8);
        GenerationContext first = GenerationContext.create(
                42L,
                chunk,
                3,
                ResourceLocation.fromNamespaceAndPath("labrinth", "standard"),
                GenerationConstraints.LABRINTH);
        GenerationContext second = GenerationContext.create(
                42L,
                chunk,
                3,
                ResourceLocation.fromNamespaceAndPath("labrinth", "standard"),
                GenerationConstraints.LABRINTH);
        require(first.dimensionSeed() == second.dimensionSeed(), "dimension seed is repeatable");
        require(first.neighbors().equals(second.neighbors()), "neighbor context is repeatable");
        for (int i = 0; i < 8; i++) {
            require(first.random().nextInt(1_000_000) == second.random().nextInt(1_000_000),
                    "context random source is repeatable");
        }
    }

    private static void checkStructurePieceSystem() {
        ResourceLocation standardRegion = ResourceLocation.fromNamespaceAndPath("labrinth", "standard");
        ResourceLocation anchorId = ResourceLocation.fromNamespaceAndPath("labrinth", "anchor");
        ResourceLocation candidateId = ResourceLocation.fromNamespaceAndPath("labrinth", "candidate");
        Connector anchorConnector = new Connector(
                new Connector.Position(2, 1, 4),
                Connector.Direction.SOUTH,
                Connector.Type.DOOR,
                2,
                3,
                StructurePiece.Rotation.NONE,
                true);
        Connector candidateConnector = new Connector(
                new Connector.Position(2, 1, 0),
                Connector.Direction.NORTH,
                Connector.Type.DOOR,
                2,
                3,
                StructurePiece.Rotation.NONE,
                true);
        StructurePiece anchorDefinition = StructurePiece.builder(
                        anchorId,
                        ResourceLocation.fromNamespaceAndPath("labrinth", "templates/anchor"),
                        StructurePiece.Kind.ROOM,
                        4,
                        4,
                        4)
                .weight(7)
                .rarity(StructurePiece.Rarity.UNCOMMON)
                .connectors(List.of(anchorConnector))
                .build();
        StructurePiece candidateDefinition = StructurePiece.builder(
                        candidateId,
                        ResourceLocation.fromNamespaceAndPath("labrinth", "templates/candidate"),
                        StructurePiece.Kind.CORRIDOR,
                        4,
                        4,
                        4)
                .connectors(List.of(candidateConnector))
                .build();

        PlacedStructurePiece anchor = anchorDefinition.placedAt(
                new StructurePiece.BlockPoint(10, 4, 20),
                StructurePiece.Rotation.NONE,
                StructurePiece.Mirror.NONE);
        require(anchor.bounds().equals(new GenerationGrid.Bounds(10, 20, 14, 24, 4, 8)),
                "piece bounds use transformed half-open dimensions");
        require(anchor.ownerChunk().equals(new GenerationGrid.Chunk(0, 1)),
                "placed piece retains deterministic chunk ownership");

        Optional<PlacedStructurePiece> aligned = ConnectorAlignment.align(anchor, 0, candidateDefinition, 0);
        require(aligned.isPresent(), "compatible connectors align");
        PlacedStructurePiece candidate = aligned.orElseThrow();
        require(candidate.connector(0).position().equals(anchor.connector(0).position()),
                "aligned connectors share an endpoint");
        require(candidate.connector(0).direction() == Connector.Direction.NORTH,
                "aligned candidate keeps its opposing direction");
        require(StructurePlacementValidator.validate(candidate, List.of(anchor), GenerationConstraints.LABRINTH)
                        .accepted(),
                "face-to-face pieces do not collide");

        Connector rotatedSource = new Connector(
                new Connector.Position(1, 1, 0),
                Connector.Direction.NORTH,
                Connector.Type.STANDARD,
                2,
                3,
                StructurePiece.Rotation.NONE,
                false);
        StructurePiece transformDefinition = StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "transform"),
                        ResourceLocation.fromNamespaceAndPath("labrinth", "templates/transform"),
                        StructurePiece.Kind.JUNCTION,
                        4,
                        4,
                        6)
                .mirrors(Set.of(StructurePiece.Mirror.NONE, StructurePiece.Mirror.LEFT_RIGHT))
                .connectors(List.of(rotatedSource))
                .build();
        PlacedStructurePiece rotated = transformDefinition.placedAt(
                new StructurePiece.BlockPoint(100, 8, 200),
                StructurePiece.Rotation.CLOCKWISE_90,
                StructurePiece.Mirror.NONE);
        require(rotated.bounds().equals(new GenerationGrid.Bounds(100, 200, 106, 204, 8, 12)),
                "quarter-turn swaps horizontal piece dimensions");
        require(rotated.connector(0).position().equals(new Connector.Position(106, 9, 201)),
                "quarter-turn transforms connector position");
        require(rotated.connector(0).direction() == Connector.Direction.EAST,
                "quarter-turn transforms connector direction");
        PlacedStructurePiece mirrored = transformDefinition.placedAt(
                new StructurePiece.BlockPoint(0, 0, 0),
                StructurePiece.Rotation.NONE,
                StructurePiece.Mirror.LEFT_RIGHT);
        require(mirrored.connector(0).position().equals(new Connector.Position(3, 1, 0)),
                "mirror transforms connector position");

        require(!anchor.connector(0).compatibleWith(new Connector(
                        new Connector.Position(2, 1, 0),
                        Connector.Direction.NORTH,
                        Connector.Type.DOOR,
                        1,
                        3,
                        StructurePiece.Rotation.NONE,
                        false)),
                "connector width mismatch is rejected");
        require(!anchor.connector(0).compatibleWith(new Connector(
                        new Connector.Position(2, 1, 0),
                        Connector.Direction.NORTH,
                        Connector.Type.SHAFT,
                        2,
                        3,
                        StructurePiece.Rotation.NONE,
                        false)),
                "connector type mismatch is rejected");
        require(!anchor.connector(0).compatibleWith(candidateConnector.capped()),
                "capped connectors cannot connect");
        require(!anchor.connector(0).compatibleWith(candidateConnector.blocked()),
                "blocked connectors cannot connect");

        StructurePiece gatedDefinition = StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "gated"),
                        ResourceLocation.fromNamespaceAndPath("labrinth", "templates/gated"),
                        StructurePiece.Kind.LANDMARK,
                        4,
                        4,
                        4)
                .weight(12)
                .rarity(StructurePiece.Rarity.RARE)
                .rotation(StructurePiece.Rotation.CLOCKWISE_90)
                .mirrors(Set.of(StructurePiece.Mirror.FRONT_BACK))
                .depthRange(2, 4)
                .allowedRegions(Set.of(standardRegion))
                .placementConditions(new StructurePiece.PlacementConditions(2, true))
                .loot(StructurePiece.LootConfiguration.table(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "chests/gated")))
                .decorations(new StructurePiece.DecorationRules(List.of(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "debris"))))
                .build();
        require(gatedDefinition.weight() == 12
                        && gatedDefinition.rarity() == StructurePiece.Rarity.RARE
                        && gatedDefinition.placementConditions().requiresLighting()
                        && gatedDefinition.lootConfiguration().table().isPresent()
                        && gatedDefinition.decorationRules().ruleIds().size() == 1,
                "piece metadata is retained");
        GenerationContext allowedContext = GenerationContext.create(
                42L,
                new GenerationGrid.Chunk(0, 0),
                3,
                standardRegion,
                GenerationConstraints.LABRINTH);
        require(gatedDefinition.canPlace(allowedContext), "depth and region allow a piece");
        require(!gatedDefinition.canPlace(GenerationContext.create(42L, new GenerationGrid.Chunk(0, 0), 1,
                        standardRegion, GenerationConstraints.LABRINTH)),
                "minimum depth is enforced");
        GenerationContext wrongRegionContext = GenerationContext.create(
                        42L,
                        new GenerationGrid.Chunk(0, 0),
                        3,
                        ResourceLocation.fromNamespaceAndPath("labrinth", "abandoned"),
                        GenerationConstraints.LABRINTH);
        require(!gatedDefinition.canPlace(wrongRegionContext),
                "allowed regions are enforced");
        require(StructurePlacementValidator.validate(
                        gatedDefinition.placedAt(
                                new StructurePiece.BlockPoint(0, 2, 0),
                                StructurePiece.Rotation.CLOCKWISE_90,
                                StructurePiece.Mirror.FRONT_BACK),
                        List.of(),
                        allowedContext).accepted(),
                "validator applies depth and region placement gates");
        require(StructurePlacementValidator.validate(
                        gatedDefinition.placedAt(
                                new StructurePiece.BlockPoint(0, 2, 0),
                                StructurePiece.Rotation.CLOCKWISE_90,
                                StructurePiece.Mirror.FRONT_BACK),
                        List.of(),
                        wrongRegionContext).reason() == StructurePlacementValidator.Reason.CONTEXT_REJECTED,
                "validator rejects an unavailable region");

        PlacedStructurePiece overlap = candidateDefinition.placedAt(
                new StructurePiece.BlockPoint(10, 4, 20),
                StructurePiece.Rotation.NONE,
                StructurePiece.Mirror.NONE);
        StructurePlacementValidator.Result overlapResult = StructurePlacementValidator.validate(
                overlap, List.of(anchor), GenerationConstraints.LABRINTH);
        require(!overlapResult.accepted() && overlapResult.reason()
                        == StructurePlacementValidator.Reason.OVERLAP,
                "unpermitted three-dimensional overlap is rejected");
        StructurePiece overlapAllowedDefinition = StructurePiece.builder(
                        ResourceLocation.fromNamespaceAndPath("labrinth", "overlap_allowed"),
                        ResourceLocation.fromNamespaceAndPath("labrinth", "templates/overlap_allowed"),
                        StructurePiece.Kind.ROOM,
                        4,
                        4,
                        4)
                .permitOverlapWith(anchorId)
                .build();
        require(StructurePlacementValidator.validate(
                        overlapAllowedDefinition.placedAt(
                                new StructurePiece.BlockPoint(10, 4, 20),
                                StructurePiece.Rotation.NONE,
                                StructurePiece.Mirror.NONE),
                        List.of(anchor),
                        GenerationConstraints.LABRINTH).accepted(),
                "explicit overlap permission is honored");
        require(StructurePlacementValidator.validate(
                        candidateDefinition.placedAt(
                                new StructurePiece.BlockPoint(10, 20, 20),
                                StructurePiece.Rotation.NONE,
                                StructurePiece.Mirror.NONE),
                        List.of(anchor),
                        GenerationConstraints.LABRINTH).accepted(),
                "vertical separation avoids collision");
        require(StructurePlacementValidator.validate(
                        candidateDefinition.placedAt(
                                new StructurePiece.BlockPoint(0, 255, 0),
                                StructurePiece.Rotation.NONE,
                                StructurePiece.Mirror.NONE),
                        List.of(),
                        GenerationConstraints.LABRINTH).reason()
                        == StructurePlacementValidator.Reason.OUT_OF_WORLD_HEIGHT,
                "world height limits are enforced");

        List<PlacedStructurePiece> manyPieces = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            manyPieces.add(candidateDefinition.placedAt(
                    new StructurePiece.BlockPoint(1_000L + i * 8L, 0, 1_000),
                    StructurePiece.Rotation.NONE,
                    StructurePiece.Mirror.NONE));
        }
        StructurePlacementValidator.Result boundedResult = StructurePlacementValidator.validate(
                candidate,
                manyPieces,
                GenerationConstraints.LABRINTH,
                3);
        require(!boundedResult.accepted()
                        && boundedResult.reason() == StructurePlacementValidator.Reason.CHECK_LIMIT_EXCEEDED,
                "collision validation remains bounded");
    }

    private static void checkStraightCorridorSystem() {
        StructurePiece definition = StraightCorridor.definition();
        require(definition.kind() == StructurePiece.Kind.CORRIDOR,
                "straight corridor is a corridor piece");
        require(definition.width() == StraightCorridor.WIDTH
                        && definition.height() == StraightCorridor.HEIGHT
                        && definition.depth() == StraightCorridor.LENGTH,
                "straight corridor dimensions match the construction contract");
        require(definition.connectors().size() == 2,
                "straight corridor has forward and rear connectors");
        require(definition.connectors().get(0).direction() == Connector.Direction.NORTH
                        && definition.connectors().get(1).direction() == Connector.Direction.SOUTH,
                "straight corridor connectors face opposite ends");

        GenerationGrid.Cell cell = new GenerationGrid.Cell(2, -3);
        PlacedStructurePiece straight = StraightCorridor.placedAt(
                cell,
                StructurePiece.Rotation.NONE);
        require(straight.bounds().equals(new GenerationGrid.Bounds(
                        157,
                        -192,
                        164,
                        -128,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "straight corridor occupies one cell axis");

        PlacedStructurePiece rotated = StraightCorridor.placedAt(
                cell,
                StructurePiece.Rotation.CLOCKWISE_90);
        require(rotated.bounds().equals(new GenerationGrid.Bounds(
                        128,
                        -163,
                        192,
                        -156,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "rotated corridor swaps its horizontal footprint");
        require(rotated.connector(0).direction() == Connector.Direction.EAST
                        && rotated.connector(1).direction() == Connector.Direction.WEST,
                "rotated corridor connectors follow its axis");
        for (StructurePiece.Rotation rotation : StructurePiece.Rotation.values()) {
            PlacedStructurePiece aligned = StraightCorridor.longPlacedAt(cell, rotation);
            Set<GenerationGrid.Direction> axis = rotation == StructurePiece.Rotation.NONE
                    || rotation == StructurePiece.Rotation.CLOCKWISE_180
                    ? Set.of(GenerationGrid.Direction.NORTH, GenerationGrid.Direction.SOUTH)
                    : Set.of(GenerationGrid.Direction.EAST, GenerationGrid.Direction.WEST);
            for (GenerationGrid.Direction direction : axis) {
                require(GenerationConnectionRules.hasBoundaryConnector(aligned, cell, direction),
                        "rotated odd-width corridor endpoint stays on the canonical boundary: "
                                + rotation + " " + direction);
            }
        }

        StructurePiece shortDefinition = StraightCorridor.shortDefinition();
        require(shortDefinition != definition
                        && shortDefinition.kind() == StructurePiece.Kind.CORRIDOR
                        && shortDefinition.width() == StraightCorridor.WIDTH
                        && shortDefinition.height() == StraightCorridor.HEIGHT
                        && shortDefinition.depth() == StraightCorridor.SHORT_LENGTH,
                "short straight is a distinct half-cell corridor definition");
        require(shortDefinition.connectors().size() == 2
                        && shortDefinition.connectors().get(1).position().z()
                        == StraightCorridor.SHORT_LENGTH,
                "short straight connectors terminate at its own depth");
        PlacedStructurePiece shortStraight = StraightCorridor.shortPlacedAt(
                cell,
                StructurePiece.Rotation.NONE);
        require(shortStraight.bounds().equals(new GenerationGrid.Bounds(
                        157,
                        -176,
                        164,
                        -144,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "short straight is centered on the cell axis");
        PlacedStructurePiece rotatedShort = StraightCorridor.shortPlacedAt(
                cell,
                StructurePiece.Rotation.CLOCKWISE_90);
        require(rotatedShort.bounds().equals(new GenerationGrid.Bounds(
                        144,
                        -163,
                        176,
                        -156,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "rotated short straight swaps its centered footprint");

        StructurePiece mediumDefinition = StraightCorridor.mediumDefinition();
        require(mediumDefinition != definition
                        && mediumDefinition != shortDefinition
                        && mediumDefinition.kind() == StructurePiece.Kind.CORRIDOR
                        && mediumDefinition.width() == StraightCorridor.WIDTH
                        && mediumDefinition.height() == StraightCorridor.HEIGHT
                        && mediumDefinition.depth() == StraightCorridor.MEDIUM_LENGTH,
                "medium straight is a distinct three-quarter-cell corridor definition");
        require(mediumDefinition.connectors().size() == 2
                        && mediumDefinition.connectors().get(1).position().z()
                        == StraightCorridor.MEDIUM_LENGTH,
                "medium straight connectors terminate at its own depth");
        PlacedStructurePiece mediumStraight = StraightCorridor.mediumPlacedAt(
                cell,
                StructurePiece.Rotation.NONE);
        require(mediumStraight.bounds().equals(new GenerationGrid.Bounds(
                        157,
                        -184,
                        164,
                        -136,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "medium straight is centered on the cell axis");
        PlacedStructurePiece rotatedMedium = StraightCorridor.mediumPlacedAt(
                cell,
                StructurePiece.Rotation.CLOCKWISE_90);
        require(rotatedMedium.bounds().equals(new GenerationGrid.Bounds(
                        136,
                        -163,
                        184,
                        -156,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "rotated medium straight swaps its centered footprint");

        StructurePiece longDefinition = StraightCorridor.longDefinition();
        require(longDefinition != definition
                        && longDefinition != shortDefinition
                        && longDefinition != mediumDefinition
                        && longDefinition.kind() == StructurePiece.Kind.CORRIDOR
                        && longDefinition.width() == StraightCorridor.WIDTH
                        && longDefinition.height() == StraightCorridor.HEIGHT
                        && longDefinition.depth() == StraightCorridor.LONG_LENGTH,
                "long straight is a distinct full-cell corridor definition");
        require(longDefinition.connectors().size() == 2
                        && longDefinition.connectors().get(1).position().z()
                        == StraightCorridor.LONG_LENGTH,
                "long straight connectors terminate at its own depth");
        PlacedStructurePiece longStraight = StraightCorridor.longPlacedAt(
                cell,
                StructurePiece.Rotation.NONE);
        require(longStraight.bounds().equals(new GenerationGrid.Bounds(
                        157,
                        -192,
                        164,
                        -128,
                        StraightCorridor.FLOOR_Y,
                        StraightCorridor.FLOOR_Y + StraightCorridor.HEIGHT)),
                "long straight occupies one complete generation cell");

        require(StraightCorridor.rotationForSeed(42L, cell)
                        == StraightCorridor.rotationForSeed(42L, cell),
                "straight corridor axis selection is deterministic");
        PlacedStructurePiece originCorridor = StraightCorridor.placedAt(
                new GenerationGrid.Cell(0, 0),
                StructurePiece.Rotation.NONE);
        require(originCorridor.bounds().minBlockX() == -3
                        && originCorridor.bounds().minBlockZ() == -3,
                "origin corridor leaves a bounded margin around spawn");

    }

    private static void checkCorridorCatalogSystem() {
        List<StructurePiece> definitions = CorridorCatalog.definitions();
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        for (StructurePiece definition : definitions) {
            require(ids.add(definition.id()), "corridor catalog IDs are unique");
            require(definition.width() > 0
                            && definition.height() > 0
                            && definition.depth() > 0,
                    "corridor catalog pieces have positive dimensions");
            require(definition.connectors().size() <= 4,
                    "corridor catalog branching remains bounded");
        }
        require(definitions.size() == CorridorKind.values().length,
                "corridor catalog covers every Phase 4 variant");

        long worldSeed = 0x13579BDF2468ACE0L;
        GenerationGrid.Cell cell = new GenerationGrid.Cell(3, -4);
        for (CorridorKind kind : CorridorKind.values()) {
            CorridorCatalog.Selection selected = CorridorCatalog.select(
                    worldSeed,
                    cell,
                    onlyKindConfig(kind));
            int expectedWidth = switch (kind) {
                case WIDE_CORRIDOR -> 9;
                case NARROW_CORRIDOR -> 5;
                case GRAND_STRAIGHT, GRAND_S_CURVE, GRAND_INCLINE, GRAND_DECLINE,
                        GRAND_STAIRCASE_UP, GRAND_STAIRCASE_DOWN -> 11;
                case GRAND_CURVED_LEFT, GRAND_CURVED_RIGHT, GRAND_U_TURN,
                        GRAND_T_JUNCTION, GRAND_FOUR_WAY -> GenerationGrid.CELL_SIZE_BLOCKS;
                case SHORT_STRAIGHT, MEDIUM_STRAIGHT, LONG_STRAIGHT, DEAD_END,
                        S_CURVE, INCLINE, DECLINE,
                        STAIRCASE_UP, STAIRCASE_DOWN -> 7;
                case CURVED_LEFT, CURVED_RIGHT, U_TURN -> GenerationGrid.CELL_SIZE_BLOCKS;
                default -> GenerationGrid.CELL_SIZE_BLOCKS;
            };
            int expectedDepth = switch (kind) {
                case SHORT_STRAIGHT -> StraightCorridor.SHORT_LENGTH;
                case MEDIUM_STRAIGHT -> StraightCorridor.MEDIUM_LENGTH;
                default -> GenerationGrid.CELL_SIZE_BLOCKS;
            };
            int expectedConnectors = switch (kind) {
                case LEFT_TURN, RIGHT_TURN, CURVED_LEFT, CURVED_RIGHT -> 2;
                case T_JUNCTION, U_TURN, GRAND_T_JUNCTION, GRAND_U_TURN -> 3;
                case FOUR_WAY, GRAND_FOUR_WAY -> 4;
                case DEAD_END -> 1;
                default -> 2;
            };
            require(selected.kind() == kind
                            && selected.piece().definition().width() == expectedWidth
                            && selected.piece().definition().depth() == expectedDepth
                            && selected.piece().connectors().size() == expectedConnectors,
                    "corridor catalog materializes the " + kind + " variant");
            if (kind.name().contains("INCLINE")
                    || kind.name().contains("DECLINE")
                    || kind.name().contains("STAIRCASE")) {
                require(selected.piece().connectors().stream()
                                .allMatch(connector -> connector.width() == 5
                                        && connector.height() == 4
                                        && connector.position().y() == VerticalCatalog.floorY(0) + 1),
                        "ramp " + kind + " retains the shared doorway profile");
            }
            if (kind.name().startsWith("GRAND_")) {
                require(selected.piece().connectors().stream()
                                .allMatch(connector -> connector.width() == 5
                                        && connector.height() == 4),
                        "grand " + kind + " retains the shared doorway aperture");
            }
            if (kind != CorridorKind.SHORT_STRAIGHT
                    && kind != CorridorKind.MEDIUM_STRAIGHT) {
                for (GenerationGrid.Direction direction : selected.connectorDirections()) {
                            require(GenerationConnectionRules.hasBoundaryConnector(
                                    selected.piece(), cell, direction),
                            "corridor " + kind + " keeps its " + direction
                                    + " connector on the cell boundary");
                }
            }
        }
        CorridorSelectionConfig defaults = CorridorCatalog.DEFAULT_CONFIG;
        CorridorCatalog.Selection first = CorridorCatalog.select(worldSeed, cell, defaults);
        CorridorCatalog.Selection second = CorridorCatalog.select(worldSeed, cell, defaults);
        require(first.kind() == second.kind()
                        && first.rotation() == second.rotation()
                        && first.piece().bounds().equals(second.piece().bounds()),
                "corridor selection is deterministic");

        CorridorCatalog.Placement placement = CorridorCatalog.placement(worldSeed, cell, defaults);
        for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
            CorridorCatalog.Placement neighbor = CorridorCatalog.placement(
                    worldSeed,
                    cell.neighbor(direction),
                    defaults);
            require(placement.openDirections().contains(direction)
                            == neighbor.openDirections().contains(direction.opposite()),
                    "corridor openings are symmetric for " + direction);
        }

        CorridorSelectionConfig fourWayOnly = onlyKindConfig(CorridorKind.FOUR_WAY);
        require(CorridorCatalog.select(worldSeed, new GenerationGrid.Cell(5, 5), fourWayOnly).kind()
                        == CorridorKind.FOUR_WAY,
                "corridor weights are configurable");
        boolean foundAlignedJunctionConnection = false;
        for (int x = 5; x <= 8 && !foundAlignedJunctionConnection; x++) {
            for (int z = 5; z <= 8 && !foundAlignedJunctionConnection; z++) {
                GenerationGrid.Cell candidateCell = new GenerationGrid.Cell(x, z);
                CorridorCatalog.Selection candidate = CorridorCatalog.select(
                        worldSeed,
                        candidateCell,
                        fourWayOnly);
                for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
                    CorridorCatalog.Selection neighbor = CorridorCatalog.select(
                            worldSeed,
                            candidateCell.neighbor(direction),
                            fourWayOnly);
                    if (candidate.rotation() == neighbor.rotation()) {
                        require(CorridorCatalog.placement(
                                        worldSeed,
                                        candidateCell,
                                        fourWayOnly)
                                .openDirections().contains(direction),
                                "full-cell junction connectors align at " + direction);
                        foundAlignedJunctionConnection = true;
                        break;
                    }
                }
            }
        }
        require(foundAlignedJunctionConnection,
                "catalog exposes an aligned full-cell junction connection");

        CorridorSelectionConfig noDeadEnds = defaults.withDeadEndChancePercent(0);
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                if (x == 0 && z == 0) {
                    continue;
                }
                require(CorridorCatalog.select(worldSeed, new GenerationGrid.Cell(x, z), noDeadEnds).kind()
                                != CorridorKind.DEAD_END,
                        "dead-end chance zero suppresses dead ends");
            }
        }

        CorridorSelectionConfig limitedBranching = defaults.withMaxBranching(2);
        for (int x = 1; x <= 3; x++) {
            for (int z = 1; z <= 3; z++) {
                require(CorridorCatalog.select(worldSeed, new GenerationGrid.Cell(x, z), limitedBranching)
                                .piece().connectors().size() <= 2,
                        "configured branching limit is enforced");
            }
        }

        CorridorCatalog.Selection origin = CorridorCatalog.select(
                worldSeed,
                new GenerationGrid.Cell(0, 0),
                fourWayOnly);
        require(origin.piece().bounds().minBlockX() == -3
                        && origin.piece().bounds().minBlockZ() == -3,
                "catalog preserves the origin spawn anchor");
    }

    private static void checkRoomCatalogSystem() {
        List<RoomDefinition> definitions = RoomCatalog.definitions();
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        boolean foundSpawnMarker = false;
        GenerationGrid.Cell cell = new GenerationGrid.Cell(-6, 9);
        Set<Integer> roomWidths = new HashSet<>();
        Set<Integer> roomHeights = new HashSet<>();
        require(definitions.size() == RoomKind.values().length,
                "room catalog covers every Phase 5 room kind");
        for (RoomDefinition definition : definitions) {
            require(ids.add(definition.id()), "room catalog IDs are unique");
            require(RoomCatalog.definition(definition.kind()) == definition
                            && RoomCatalog.definition(definition.id()) == definition,
                    "room registration maps both kind and ID");
            require(RoomCatalog.supports(definition.piece()), "room catalog owns its structure piece");
            require(definition.piece().kind() == StructurePiece.Kind.ROOM
                            && definition.piece().width() >= 32
                            && definition.piece().height() >= 6
                            && definition.piece().depth() >= 32,
                    "rooms use bounded variable-size shells");
            roomWidths.add(definition.piece().width());
            roomHeights.add(definition.piece().height());
            require(definition.piece().connectors().size() <= 4
                            && definition.piece().allowedRotations().size() == 4
                            && definition.piece().allowedRegions().contains(RoomCatalog.STANDARD_REGION),
                    "room metadata contains bounded connectors and rotation/region rules");
            require(definition.piece().connectors().stream().allMatch(connector ->
                            connector.position().y() == 1
                                    && connector.width() == RoomCatalog.APERTURE_WIDTH
                                    && connector.height() == RoomCatalog.APERTURE_HEIGHT),
                    "room connectors share the hallway aperture profile");
            foundSpawnMarker |= !definition.spawnMarkers().isEmpty();
            for (StructurePiece.Rotation rotation : definition.piece().allowedRotations()) {
                int transformedWidth = StructurePiece.transformedWidth(
                        definition.piece().width(), definition.piece().depth(), rotation);
                int transformedDepth = StructurePiece.transformedDepth(
                        definition.piece().width(), definition.piece().depth(), rotation);
                PlacedStructurePiece placed = definition.piece().placedAt(
                        new StructurePiece.BlockPoint(
                                GenerationGrid.blockOriginX(cell)
                                        + (GenerationGrid.CELL_SIZE_BLOCKS - transformedWidth) / 2,
                                RoomCatalog.FLOOR_Y,
                                GenerationGrid.blockOriginZ(cell)
                                        + (GenerationGrid.CELL_SIZE_BLOCKS - transformedDepth) / 2),
                        rotation,
                        StructurePiece.Mirror.NONE);
                for (Connector connector : placed.connectors()) {
                    require(GenerationConnectionRules.hasBoundaryConnector(
                                    placed,
                                    cell,
                                    gridDirection(connector.direction())),
                            "sized room connector remains aligned after rotation");
                }
            }
            for (RoomDefinition.SpawnMarker marker : definition.spawnMarkers()) {
                require(marker.x() >= 0 && marker.x() < definition.piece().width()
                                && marker.y() >= 0 && marker.y() < definition.piece().height()
                                && marker.z() >= 0 && marker.z() < definition.piece().depth(),
                        "room spawn markers stay inside their piece");
            }
            require(definition.eligible(0, RoomCatalog.STANDARD_REGION)
                            && !definition.eligible(-1, RoomCatalog.STANDARD_REGION)
                            && !definition.eligible(33, RoomCatalog.STANDARD_REGION)
                            && !definition.eligible(0, ResourceLocation.fromNamespaceAndPath(
                                    "labrinth", "unsupported")),
                    "room depth and region restrictions are enforced");
        }
        require(foundSpawnMarker, "room catalog includes deterministic spawn markers");
        require(roomWidths.size() >= 3 && roomHeights.size() >= 4,
                "room catalog exposes width and height diversity");

        long worldSeed = 0x0F0E0D0C0B0A0908L;
        RoomCatalog.Selection first = RoomCatalog.select(worldSeed, cell);
        RoomCatalog.Selection second = RoomCatalog.select(worldSeed, cell);
        require(first.kind() == second.kind()
                        && first.rotation() == second.rotation()
                        && first.piece().bounds().equals(second.piece().bounds()),
                "room selection is deterministic");

        RoomCatalog.Placement closedRoom = RoomCatalog.placement(worldSeed, cell);
        int closedWidth = StructurePiece.transformedWidth(
                closedRoom.piece().definition().width(),
                closedRoom.piece().definition().depth(),
                closedRoom.piece().rotation());
        int closedDepth = StructurePiece.transformedDepth(
                closedRoom.piece().definition().width(),
                closedRoom.piece().definition().depth(),
                closedRoom.piece().rotation());
        require(closedRoom.openDirections().isEmpty()
                        && closedRoom.piece().bounds().minBlockX()
                        == GenerationGrid.blockOriginX(cell)
                                + (GenerationGrid.CELL_SIZE_BLOCKS - closedWidth) / 2
                        && closedRoom.piece().bounds().minBlockZ()
                        == GenerationGrid.blockOriginZ(cell)
                                + (GenerationGrid.CELL_SIZE_BLOCKS - closedDepth) / 2,
                "room placement caps unused connectors within its owning cell");
    }

    private static void checkContentCatalogSystem() {
        long worldSeed = 0x2468ACE013579BDFL;
        CorridorSelectionConfig corridorConfig = CorridorCatalog.DEFAULT_CONFIG;
        GenerationGrid.Cell cell = new GenerationGrid.Cell(4, -3);
        LabrinthContentCatalog.Selection first = LabrinthContentCatalog.select(
                worldSeed,
                cell,
                corridorConfig);
        LabrinthContentCatalog.Selection second = LabrinthContentCatalog.select(
                worldSeed,
                cell,
                corridorConfig);
        require(first.id().equals(second.id())
                        && first.piece().bounds().equals(second.piece().bounds()),
                "mixed room/corridor selection is deterministic");

        boolean foundRoom = false;
        boolean foundCorridor = false;
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                LabrinthContentCatalog.Selection selection = LabrinthContentCatalog.select(
                        worldSeed,
                        new GenerationGrid.Cell(x, z),
                        corridorConfig);
                foundRoom |= selection.isRoom();
                foundCorridor |= selection.isCorridor();
            }
        }
        require(foundRoom && foundCorridor,
                "live content catalog selects both rooms and corridors");

        RoomDefinition crossRoom = RoomCatalog.definition(RoomKind.CROSS_ROOM);
        for (StructurePiece.Rotation rotation : StructurePiece.Rotation.values()) {
            PlacedStructurePiece rotatedRoom = crossRoom.piece().placedAt(
                    new StructurePiece.BlockPoint(0, RoomCatalog.FLOOR_Y, 0),
                    rotation,
                    StructurePiece.Mirror.NONE);
            GenerationConnectionRules.LocalCenter localCenter =
                    GenerationConnectionRules.localCellCenter(rotatedRoom);
            int expectedX = rotation == StructurePiece.Rotation.NONE
                    || rotation == StructurePiece.Rotation.CLOCKWISE_90 ? 32 : 31;
            int expectedZ = rotation == StructurePiece.Rotation.NONE
                    || rotation == StructurePiece.Rotation.COUNTERCLOCKWISE_90 ? 32 : 31;
            require(localCenter.x() == expectedX && localCenter.z() == expectedZ,
                    "rotated room geometry maps back to the canonical cell center");
            for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
                require(GenerationConnectionRules.hasBoundaryConnector(
                                rotatedRoom,
                                new GenerationGrid.Cell(0, 0),
                                direction),
                        "rotated room connector remains centered on " + direction);
            }
        }

        LabrinthContentCatalog.Selection origin = LabrinthContentCatalog.select(
                worldSeed,
                new GenerationGrid.Cell(0, 0),
                corridorConfig);
        require(origin.isCorridor()
                        && origin.piece().bounds().minBlockX() == -3
                        && origin.piece().bounds().minBlockZ() == -3,
                "mixed content catalog preserves the origin corridor anchor");

        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                GenerationGrid.Cell currentCell = new GenerationGrid.Cell(x, z);
                LabrinthContentCatalog.Placement placement = LabrinthContentCatalog.placement(
                        worldSeed,
                        currentCell,
                        corridorConfig);
                require(placement.openDirections().equals(
                                GenerationNeighbors.forCell(worldSeed, currentCell).connected()),
                        "content placement honors every shared edge decision at "
                                + currentCell + ": expected "
                                + GenerationNeighbors.forCell(worldSeed, currentCell).connected()
                                + ", actual " + placement.openDirections());
                for (GenerationGrid.Direction direction : placement.openDirections()) {
                    require(GenerationConnectionRules.hasBoundaryConnector(
                                    placement.selection().piece(),
                                    currentCell,
                                    direction),
                            "content edge is on the owning cell boundary");
                }
                for (GenerationGrid.Direction direction : GenerationGrid.Direction.values()) {
                    LabrinthContentCatalog.Placement neighbor = LabrinthContentCatalog.placement(
                            worldSeed,
                            currentCell.neighbor(direction),
                            corridorConfig);
                    require(placement.openDirections().contains(direction)
                                    == neighbor.openDirections().contains(direction.opposite()),
                            "mixed content openings are symmetric for " + direction
                                    + " at " + currentCell
                                    + " current=" + placement.selection().id()
                                    + " neighbor=" + neighbor.selection().id()
                                    + " currentConnectors=" + placement.selection().piece().connectors()
                                    + " neighborConnectors=" + neighbor.selection().piece().connectors());
                }
            }
        }
    }

    private static void checkContinuousExpansionSystem() {
        long worldSeed = 0x1020304050607080L;
        CorridorSelectionConfig config = CorridorCatalog.DEFAULT_CONFIG;
        GenerationGrid.Cell farCell = new GenerationGrid.Cell(1_024, -2_048);
        LabrinthContentCatalog.Selection first = LabrinthContentCatalog.select(
                worldSeed,
                farCell,
                config);
        LabrinthContentCatalog.Selection second = LabrinthContentCatalog.select(
                worldSeed,
                farCell,
                config);
        require(first.id().equals(second.id())
                        && first.piece().bounds().equals(second.piece().bounds()),
                "content continues deterministically thousands of blocks from origin");
        require(GenerationConstraints.LABRINTH.contains(first.piece().bounds()),
                "far content remains inside the vertical generation contract");

        int connectedCells = 0;
        int openEdges = 0;
        for (int x = 24; x < 32; x++) {
            for (int z = -32; z < -24; z++) {
                GenerationGrid.Cell cell = new GenerationGrid.Cell(x, z);
                LabrinthContentCatalog.Placement placement = LabrinthContentCatalog.placement(
                        worldSeed,
                        cell,
                        config);
                if (!placement.openDirections().isEmpty()) {
                    connectedCells++;
                }
                openEdges += placement.openDirections().size();
                for (GenerationGrid.Direction direction : placement.openDirections()) {
                    LabrinthContentCatalog.Placement neighbor = LabrinthContentCatalog.placement(
                            worldSeed,
                            cell.neighbor(direction),
                            config);
                    require(neighbor.openDirections().contains(direction.opposite()),
                            "far content agrees on the same boundary connection");
                }
            }
        }
        require(connectedCells >= 8 && openEdges >= 8,
                "bounded continuous expansion does not strand a sampled area");

        require(CorridorSelectionConfig.DEFAULT_DEAD_END_CHANCE_PERCENT == 20,
                "dead-end frequency has an explicit deterministic default");
        require(CorridorCatalog.definitions().stream()
                        .anyMatch(piece -> piece.id().getPath().endsWith("dead_end")),
                "corridor catalog retains an intentional dead-end piece");
        RoomDefinition reward = RoomCatalog.definition(RoomKind.DEAD_END_REWARD);
        require(reward.piece().connectors().size() == 1
                        && reward.piece().lootConfiguration().table().isPresent(),
                "dead-end reward rooms provide a bounded optional reward path");
    }

    private static void checkVerticalCatalogSystem() {
        long worldSeed = 0x0A0B0C0D0E0F1011L;
        GenerationGrid.Cell originCell = new GenerationGrid.Cell(0, 0);
        GenerationGrid.Cell farCell = new GenerationGrid.Cell(1_024, -2_048);
        require(VerticalCatalog.definitions().size() == 5,
                "vertical catalog retains the five supported stair and shaft definitions");
        for (VerticalCatalog.VerticalKind kind : List.of(
                VerticalCatalog.VerticalKind.STAIR_UP,
                VerticalCatalog.VerticalKind.STAIR_DOWN,
                VerticalCatalog.VerticalKind.LADDER_SHAFT,
                VerticalCatalog.VerticalKind.DROP_SHAFT,
                VerticalCatalog.VerticalKind.ELEVATOR_PLACEHOLDER)) {
            StructurePiece definition = VerticalCatalog.definition(kind);
            require(definition != null
                            && definition.width() == VerticalCatalog.WIDTH
                            && definition.height() == VerticalCatalog.HEIGHT
                            && definition.depth() == VerticalCatalog.DEPTH
                            && definition.connectors().size() == 2,
                    "vertical definition has bounded dimensions and two endpoints");
            require(definition.connectors().stream()
                            .anyMatch(connector -> connector.direction() == Connector.Direction.DOWN
                                    && connector.position().y() == 0)
                            && definition.connectors().stream()
                            .anyMatch(connector -> connector.direction() == Connector.Direction.UP
                                    && connector.position().y() == VerticalCatalog.HEIGHT),
                    "vertical definition endpoints align to both floor faces");
        }
        Set<VerticalCatalog.VerticalKind> liveKinds = Set.of(
                VerticalCatalog.VerticalKind.NONE,
                VerticalCatalog.VerticalKind.STAIR_UP,
                VerticalCatalog.VerticalKind.STAIR_DOWN,
                VerticalCatalog.VerticalKind.LADDER_SHAFT,
                VerticalCatalog.VerticalKind.DROP_SHAFT,
                VerticalCatalog.VerticalKind.ELEVATOR_PLACEHOLDER);
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                VerticalCatalog.Selection selected = VerticalCatalog.select(
                        worldSeed,
                        new GenerationGrid.Cell(x, z),
                        0);
                require(liveKinds.contains(selected.kind()),
                        "live vertical selection excludes unfinished placeholder shafts");
            }
        }

        VerticalCatalog.Selection lowerOrigin = VerticalCatalog.select(
                worldSeed,
                originCell,
                VerticalCatalog.MIN_FLOOR);
        VerticalCatalog.Selection upperOrigin = VerticalCatalog.select(
                worldSeed,
                originCell,
                0);
        require(lowerOrigin.kind() == VerticalCatalog.VerticalKind.STAIR_DOWN
                        && upperOrigin.kind() == VerticalCatalog.VerticalKind.STAIR_UP,
                "spawn cell provides deterministic links above and below the starting floor");
        require(GenerationConstraints.LABRINTH.contains(lowerOrigin.piece().bounds())
                        && GenerationConstraints.LABRINTH.contains(upperOrigin.piece().bounds()),
                "vertical links respect minimum and maximum Y");
        long expectedVerticalX = GenerationGrid.blockOriginX(originCell)
                + (GenerationGrid.CELL_SIZE_BLOCKS - VerticalCatalog.WIDTH + 1) / 2;
        long expectedVerticalZ = GenerationGrid.blockOriginZ(originCell)
                + (GenerationGrid.CELL_SIZE_BLOCKS - VerticalCatalog.DEPTH + 1) / 2;
        require(lowerOrigin.piece().origin().x() == expectedVerticalX
                        && lowerOrigin.piece().origin().z() == expectedVerticalZ
                        && lowerOrigin.piece().connector(0).position().x()
                                == GenerationGrid.blockOriginX(originCell) + GenerationGrid.CELL_SIZE_BLOCKS / 2
                        && lowerOrigin.piece().connector(0).position().z()
                                == GenerationGrid.blockOriginZ(originCell) + GenerationGrid.CELL_SIZE_BLOCKS / 2,
                "vertical pieces share the canonical cell center with horizontal connectors");
        require(!lowerOrigin.piece().bounds().intersects(upperOrigin.piece().bounds()),
                "adjacent vertical boundaries meet without overlapping");
        require(VerticalCatalog.contains(
                        lowerOrigin,
                        Math.toIntExact(lowerOrigin.piece().origin().x()),
                        lowerOrigin.upperY(),
                        Math.toIntExact(lowerOrigin.piece().origin().z())),
                "vertical selection owns the upper-floor opening");
        require(VerticalCatalog.FLOOR_SPACING == 32
                        && VerticalCatalog.floorY(-1) == -28
                        && VerticalCatalog.floorY(0) == 4
                        && VerticalCatalog.floorY(1) == 36,
                "floor layers leave room for taller rooms and corridors");

        for (int floorIndex = VerticalCatalog.MIN_FLOOR;
                floorIndex <= VerticalCatalog.MAX_FLOOR;
                floorIndex++) {
            LabrinthContentCatalog.Selection content = LabrinthContentCatalog.select(
                    worldSeed,
                    farCell,
                    CorridorCatalog.DEFAULT_CONFIG,
                    LabrinthContentCatalog.DEFAULT_DEPTH,
                    LabrinthContentCatalog.DEFAULT_REGION,
                    floorIndex);
            require(content.piece().origin().y() == VerticalCatalog.floorY(floorIndex)
                            && GenerationConstraints.LABRINTH.contains(content.piece().bounds()),
                    "floor-aware content stays on its selected layer");
        }
        require(GenerationSeeds.verticalSeed(worldSeed, farCell, -1)
                        != GenerationSeeds.verticalSeed(worldSeed, farCell, 0),
                "vertical boundary decisions use independent floor seeds");
        require(!VerticalCatalog.isValidBoundary(1),
                "vertical branching is capped at the configured top floor");
    }

    private static void checkRegionCatalogSystem() {
        List<RegionDefinition> definitions = RegionCatalog.definitions();
        Set<ResourceLocation> ids = new java.util.HashSet<>();
        require(definitions.size() == 7, "initial region catalog contains all Phase 8 regions");
        for (RegionDefinition definition : definitions) {
            require(ids.add(definition.id()), "region IDs are unique");
            require(definition.weight() > 0
                            && !definition.roomPool().isEmpty()
                            && !definition.corridorPool().isEmpty(),
                    "region metadata has a positive weight and non-empty content pools");
            require(definition.palette().floorId() != null
                            && definition.palette().wallId() != null
                            && definition.palette().ceilingId() != null
                            && definition.palette().lightId() != null
                            && definition.palette().accentId() != null,
                    "region palette is complete");
            require(definition.lightingRules() != null
                            && definition.decorationRules() != null
                            && definition.mobRules() != null
                            && definition.lootModifiers() != null
                            && definition.ambientProperties() != null
                            && definition.generationConditions() != null,
                    "region behavior metadata is complete");
        }
        long worldSeed = 0x55AA33CC77EE11DDL;
        require(RegionCatalog.resolve(ResourceLocation.fromNamespaceAndPath(
                        "labrinth", "missing")) == RegionCatalog.standard(),
                "unknown region IDs fall back to the standard region");
        require(RegionCatalog.select(worldSeed, new GenerationGrid.Cell(0, 0), 0, 0)
                        == RegionCatalog.standard(),
                "the origin sector remains the standard core");

        RegionDefinition ancient = RegionCatalog.definition(RegionCatalog.ANCIENT_ID);
        RegionDefinition flooded = RegionCatalog.definition(RegionCatalog.FLOODED_ID);
        RegionDefinition overgrown = RegionCatalog.definition(RegionCatalog.OVERGROWN_ID);
        RegionDefinition corrupted = RegionCatalog.definition(RegionCatalog.CORRUPTED_ID);
        require(!ancient.eligible(3, 0) && ancient.eligible(4, 0),
                "ancient region honors its minimum depth");
        require(flooded.eligible(0, 0) && !flooded.eligible(0, 1),
                "flooded region honors its elevation range");
        require(!overgrown.eligible(0, -1) && overgrown.eligible(0, 0),
                "overgrown region honors its elevation range");
        require(!corrupted.eligible(11, 0) && corrupted.eligible(12, 0),
                "corrupted region honors its depth restriction");

        GenerationGrid.Cell cell = new GenerationGrid.Cell(-19, 27);
        RegionDefinition first = RegionCatalog.select(worldSeed, cell, 0, 0);
        RegionDefinition second = RegionCatalog.select(worldSeed, cell, 0, 0);
        require(first.id().equals(second.id())
                        && GenerationSeeds.regionSeed(worldSeed, cell, 0, 0)
                        == GenerationSeeds.regionSeed(worldSeed, cell, 0, 0),
                "region selection and its seed are deterministic");

        Set<ResourceLocation> interiorIds = new java.util.HashSet<>();
        for (int x = 2; x < 6; x++) {
            for (int z = 2; z < 6; z++) {
                interiorIds.add(RegionCatalog.select(
                        worldSeed,
                        new GenerationGrid.Cell(16 + x, 16 + z),
                        0,
                        0).id());
            }
        }
        require(interiorIds.size() == 1,
                "coarse region fields keep an interior macro-area contiguous");

        Set<ResourceLocation> sampledIds = new java.util.HashSet<>();
        for (int x = -24; x < 24; x++) {
            for (int z = -24; z < 24; z++) {
                sampledIds.add(RegionCatalog.select(
                        worldSeed,
                        new GenerationGrid.Cell(x, z),
                        0,
                        0).id());
            }
        }
        require(sampledIds.size() >= 3,
                "weighted region distribution exposes multiple initial regions");

        RegionDefinition industrial = RegionCatalog.definition(RegionCatalog.INDUSTRIAL_ID);
        RoomCatalog.Selection industrialRoom = RoomCatalog.select(
                worldSeed,
                cell,
                LabrinthContentCatalog.DEFAULT_DEPTH,
                RegionCatalog.INDUSTRIAL_ID,
                0);
        CorridorCatalog.Selection industrialCorridor = CorridorCatalog.select(
                worldSeed,
                cell,
                CorridorCatalog.DEFAULT_CONFIG,
                0,
                RegionCatalog.INDUSTRIAL_ID);
        require(industrial.allowsRoom(industrialRoom.piece().definition().id())
                        && industrial.allowsCorridor(industrialCorridor.piece().definition().id()),
                "region-specific room and corridor pools constrain selection");

        LabrinthContentCatalog.Placement placement = LabrinthContentCatalog.placement(
                worldSeed,
                cell,
                CorridorCatalog.DEFAULT_CONFIG,
                LabrinthContentCatalog.DEFAULT_DEPTH,
                0);
        require(placement.region().id().equals(RegionCatalog.select(
                        worldSeed,
                        cell,
                        LabrinthContentCatalog.DEFAULT_DEPTH,
                        0).id()),
                "content placement carries its owning cell region");
    }

    private static void checkDepthAndLandmarkSystems() {
        long worldSeed = 0x13579BDF2468ACE0L;
        GenerationGrid.Cell origin = new GenerationGrid.Cell(0, 0);
        GenerationGrid.Cell distant = new GenerationGrid.Cell(256, -192);
        DepthCatalog.Profile originProfile = DepthCatalog.profile(worldSeed, origin, 0);
        DepthCatalog.Profile repeatProfile = DepthCatalog.profile(worldSeed, origin, 0);
        DepthCatalog.Profile distantProfile = DepthCatalog.profile(worldSeed, distant, 0);
        DepthCatalog.Profile lowerProfile = DepthCatalog.profile(worldSeed, origin, -1);
        require(originProfile.equals(repeatProfile), "depth profile is deterministic");
        require(originProfile.depth() >= DepthCatalog.MIN_DEPTH
                        && originProfile.depth() <= DepthCatalog.MAX_DEPTH
                        && distantProfile.depth() >= originProfile.depth(),
                "depth remains bounded and progresses away from the origin");
        require(distantProfile.distanceContribution() > originProfile.distanceContribution(),
                "distance contributes to logical depth");
        require(lowerProfile.verticalContribution() > originProfile.verticalContribution(),
                "vertical distance contributes to logical depth");

        GenerationGrid.Chunk contextChunk = new GenerationGrid.Chunk(36, 36);
        GenerationGrid.Cell contextCell = contextChunk.cell();
        GenerationContext derivedContext = GenerationContext.create(worldSeed, contextChunk);
        int expectedDepth = DepthCatalog.depthAt(worldSeed, contextCell, 0);
        require(derivedContext.depth() == expectedDepth
                        && derivedContext.region().equals(RegionCatalog.select(
                                worldSeed,
                                contextCell,
                                expectedDepth,
                                0).id()),
                "generation context exposes the owning depth and region");

        RoomDefinition rareRoom = RoomCatalog.definition(RoomKind.RARE_TEST);
        require(!DepthCatalog.roomAllowed(rareRoom, 0)
                        && DepthCatalog.roomAllowed(rareRoom, 8)
                        && DepthCatalog.roomWeight(rareRoom, 8) > DepthCatalog.roomWeight(rareRoom, 0),
                "depth gates and progressively weights rare rooms");
        require(DepthCatalog.corridorWeight(CorridorKind.T_JUNCTION, 10, 8)
                        > DepthCatalog.corridorWeight(CorridorKind.T_JUNCTION, 10, 0),
                "depth increases branching corridor weight");
        LabrinthContentCatalog.Placement shallow = LabrinthContentCatalog.placement(
                worldSeed,
                origin,
                CorridorCatalog.DEFAULT_CONFIG,
                0,
                RegionCatalog.STANDARD_ID,
                0);
        LabrinthContentCatalog.Placement deep = LabrinthContentCatalog.placement(
                worldSeed,
                distant,
                CorridorCatalog.DEFAULT_CONFIG,
                distantProfile.depth(),
                RegionCatalog.ABANDONED_ID,
                0);
        require(deep.depth() == distantProfile.depth()
                        && deep.lootRarityBonusPercent() > shallow.lootRarityBonusPercent()
                        && deep.entitySpawnWeight() > shallow.entitySpawnWeight()
                        && deep.hazardMultiplierPercent() > shallow.hazardMultiplierPercent()
                        && deep.ambientIntensityPercent() > shallow.ambientIntensityPercent()
                        && deep.unusualnessPercent() > shallow.unusualnessPercent(),
                "depth modifiers reach content placement metadata");
        GenerationGrid.Cell depthAwareCell = new GenerationGrid.Cell(7, 7);
        GenerationGrid.Cell depthAwareNeighbor = depthAwareCell.neighbor(GenerationGrid.Direction.EAST);
        LabrinthContentCatalog.Placement depthAwarePlacement = LabrinthContentCatalog.placement(
                worldSeed,
                depthAwareCell,
                CorridorCatalog.DEFAULT_CONFIG,
                DepthCatalog.depthAt(worldSeed, depthAwareCell, 0),
                0);
        LabrinthContentCatalog.Placement depthAwareNeighborPlacement = LabrinthContentCatalog.placement(
                worldSeed,
                depthAwareNeighbor,
                CorridorCatalog.DEFAULT_CONFIG,
                DepthCatalog.depthAt(worldSeed, depthAwareNeighbor, 0),
                0);
        require(depthAwarePlacement.openDirections().contains(GenerationGrid.Direction.EAST)
                        == depthAwareNeighborPlacement.openDirections().contains(GenerationGrid.Direction.WEST),
                "depth-aware neighboring placement remains connector-symmetric");

        List<LandmarkDefinition> definitions = LandmarkCatalog.definitions();
        Set<ResourceLocation> definitionIds = new HashSet<>();
        require(definitions.size() == 8, "initial landmark catalog contains all Phase 10 examples");
        for (LandmarkDefinition definition : definitions) {
            require(definitionIds.add(definition.id())
                            && definition.weight() > 0
                            && definition.minSpacingCells() == LandmarkCatalog.SECTOR_SIZE_CELLS
                            && definition.maxFrequencyPerSector() == 1
                            && !definition.requiredConnections().isEmpty()
                            && definition.piece().kind() == StructurePiece.Kind.LANDMARK
                            && GenerationConstraints.LABRINTH.contains(
                                    definition.piece().placedAt(
                                            new StructurePiece.BlockPoint(0,
                                                    VerticalCatalog.floorY(definition.minFloor()),
                                                    0),
                                            StructurePiece.Rotation.NONE,
                                            StructurePiece.Mirror.NONE).bounds()),
                    "landmark metadata is bounded and unique");
            require(definition.piece().allowedRegions().containsAll(definition.allowedRegions()),
                    "landmark region restrictions are enforced by the piece contract");
        }

        require(LandmarkCatalog.select(worldSeed, new GenerationGrid.Cell(1, 0)).isEmpty()
                        && LandmarkCatalog.select(worldSeed, origin).isEmpty(),
                "landmarks only select from non-core sector origins");

        List<LandmarkCatalog.Instance> selected = new ArrayList<>();
        Map<String, Integer> sectorsWithLandmarks = new HashMap<>();
        Map<ResourceLocation, List<GenerationGrid.Cell>> originsByDefinition = new HashMap<>();
        for (int sectorX = -24; sectorX <= 24; sectorX++) {
            for (int sectorZ = -24; sectorZ <= 24; sectorZ++) {
                GenerationGrid.Cell sectorOrigin = new GenerationGrid.Cell(
                        (long) sectorX * LandmarkCatalog.SECTOR_SIZE_CELLS,
                        (long) sectorZ * LandmarkCatalog.SECTOR_SIZE_CELLS);
                Optional<LandmarkCatalog.Instance> candidate = LandmarkCatalog.select(
                        worldSeed,
                        sectorOrigin);
                if (candidate.isEmpty()) {
                    continue;
                }
                LandmarkCatalog.Instance instance = candidate.get();
                Optional<LandmarkCatalog.Instance> repeat = LandmarkCatalog.select(
                        worldSeed,
                        sectorOrigin);
                require(repeat.isPresent()
                                && landmarkFingerprint(instance) == landmarkFingerprint(repeat.get()),
                        "landmark selection is reload-stable");
                require(instance.originCell().equals(sectorOrigin)
                                && instance.piece().ownerCell().equals(sectorOrigin)
                                && instance.definition().eligible(
                                        instance.depth(),
                                        instance.floorIndex(),
                                        instance.region().id())
                                && GenerationConstraints.LABRINTH.contains(instance.piece().bounds()),
                        "landmark origin ownership and progression gates are stable");
                require(instance.piece().bounds().maxBlockXExclusive()
                                - instance.piece().bounds().minBlockX() > GenerationGrid.CHUNK_SIZE_BLOCKS
                                || instance.piece().bounds().maxBlockZExclusive()
                                - instance.piece().bounds().minBlockZ() > GenerationGrid.CHUNK_SIZE_BLOCKS,
                        "landmark definitions span multiple chunks");
                selected.add(instance);
                String sectorKey = sectorX + ":" + sectorZ;
                sectorsWithLandmarks.merge(sectorKey, 1, Integer::sum);
                originsByDefinition.computeIfAbsent(
                                instance.definition().id(),
                                ignored -> new ArrayList<>())
                        .add(sectorOrigin);
            }
        }
        require(!selected.isEmpty(), "bounded seed scan finds a landmark");
        require(sectorsWithLandmarks.values().stream().allMatch(count -> count <= 1),
                "landmark frequency is capped at one per sector");
        for (LandmarkDefinition definition : definitions) {
            List<GenerationGrid.Cell> origins = originsByDefinition.getOrDefault(
                    definition.id(),
                    List.of());
            for (int first = 0; first < origins.size(); first++) {
                for (int second = first + 1; second < origins.size(); second++) {
                    GenerationGrid.Cell a = origins.get(first);
                    GenerationGrid.Cell b = origins.get(second);
                    long distance = Math.max(Math.abs(a.x() - b.x()), Math.abs(a.z() - b.z()));
                    require(distance >= definition.minSpacingCells(),
                            "landmark spacing is respected");
                }
            }
        }

        LandmarkCatalog.Instance first = selected.get(0);
        GenerationGrid.Chunk intersectingChunk = first.piece().ownerChunk();
        List<LandmarkCatalog.Instance> firstPass = LandmarkCatalog.intersecting(
                worldSeed,
                intersectingChunk);
        List<LandmarkCatalog.Instance> reloadPass = LandmarkCatalog.intersecting(
                worldSeed,
                intersectingChunk);
        require(firstPass.stream().mapToLong(GenerationArchitectureSelfCheck::landmarkFingerprint)
                        .boxed().toList()
                        .equals(reloadPass.stream()
                                .mapToLong(GenerationArchitectureSelfCheck::landmarkFingerprint)
                                .boxed().toList()),
                "intersecting chunk landmark materialization is order-independent");
        require(firstPass.stream().anyMatch(instance ->
                        instance.originCell().equals(first.originCell())),
                "owner chunk rematerializes the sector-owned landmark");
    }

    private static long landmarkFingerprint(LandmarkCatalog.Instance instance) {
        var bounds = instance.piece().bounds();
        long fingerprint = instance.definition().id().hashCode();
        fingerprint = Long.rotateLeft(fingerprint ^ instance.originCell().x(), 11);
        fingerprint = Long.rotateLeft(fingerprint ^ instance.originCell().z(), 17);
        fingerprint = Long.rotateLeft(fingerprint ^ instance.floorIndex(), 23);
        fingerprint = Long.rotateLeft(fingerprint ^ instance.depth(), 29);
        fingerprint = Long.rotateLeft(fingerprint ^ bounds.minBlockX(), 31);
        fingerprint = Long.rotateLeft(fingerprint ^ bounds.minBlockZ(), 37);
        return Long.rotateLeft(fingerprint ^ bounds.maxYExclusive(), 41);
    }

    private static GenerationGrid.Direction gridDirection(Connector.Direction direction) {
        return switch (direction) {
            case NORTH -> GenerationGrid.Direction.NORTH;
            case EAST -> GenerationGrid.Direction.EAST;
            case SOUTH -> GenerationGrid.Direction.SOUTH;
            case WEST -> GenerationGrid.Direction.WEST;
            case UP, DOWN -> throw new IllegalArgumentException("expected horizontal room connector");
        };
    }

    private static CorridorSelectionConfig onlyKindConfig(CorridorKind selectedKind) {
        java.util.EnumMap<CorridorKind, Integer> weights = new java.util.EnumMap<>(CorridorKind.class);
        for (CorridorKind kind : CorridorKind.values()) {
            weights.put(kind, kind == selectedKind ? 1 : 0);
        }
        return new CorridorSelectionConfig(weights, 100, 4);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
