package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.corridor.CorridorKind;
import com.labrinthmc.labrinth.world.region.RegionCatalog;
import com.labrinthmc.labrinth.world.region.RegionDefinition;
import com.labrinthmc.labrinth.world.room.RoomDefinition;
import java.util.Objects;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Deterministic logical depth and the small progression curve derived from it. */
public final class DepthCatalog {
    public static final int MIN_DEPTH = 0;
    public static final int MAX_DEPTH = GenerationConstraints.LABRINTH.maxDepth();

    private static final int DISTANCE_CELLS_PER_DEPTH = 8;
    private static final int MAX_LANDMARK_PROGRESSION = 4;
    private static final ResourceLocation RANDOM_FACTORY_ID =
            ResourceLocation.fromNamespaceAndPath("labrinth", "depth_profile");

    private DepthCatalog() {
    }

    public static Profile profile(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int floorIndex) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        validateFloor(floorIndex);
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        return profile(
                cell,
                floorIndex,
                factory.at(
                        Math.toIntExact(cell.x()),
                        floorIndex,
                        Math.toIntExact(cell.z())),
                provisionalDepth -> RegionCatalog.select(
                        randomState,
                        cell,
                        provisionalDepth,
                        floorIndex));
    }

    public static Profile profile(long worldSeed, GenerationGrid.Cell cell, int floorIndex) {
        Objects.requireNonNull(cell, "cell");
        validateFloor(floorIndex);
        return profile(
                cell,
                floorIndex,
                RandomSource.create(GenerationSeeds.depthSeed(worldSeed, cell, floorIndex)),
                provisionalDepth -> RegionCatalog.select(
                        worldSeed,
                        cell,
                        provisionalDepth,
                        floorIndex));
    }

    public static Profile profileForDepth(int depth) {
        validateDepth(depth);
        return new Profile(
                depth,
                depth,
                0,
                0,
                0,
                0,
                Math.min(200, depth * 8),
                Math.min(100, depth * 4),
                Math.min(260, 100 + depth * 5),
                Math.min(200, depth * 6),
                Math.min(200, 100 + depth * 3),
                Math.min(100, depth * 4));
    }

    public static int depthAt(RandomState randomState, GenerationGrid.Cell cell, int floorIndex) {
        return profile(randomState, cell, floorIndex).depth();
    }

    public static int depthAt(long worldSeed, GenerationGrid.Cell cell, int floorIndex) {
        return profile(worldSeed, cell, floorIndex).depth();
    }

    /** Gate rare room tiers until the depth has earned them. */
    public static boolean roomAllowed(RoomDefinition definition, int depth) {
        Objects.requireNonNull(definition, "definition");
        validateDepth(depth);
        int minimumDepth = switch (definition.piece().rarity()) {
            case COMMON -> 0;
            case UNCOMMON -> 2;
            case RARE -> 4;
            case VERY_RARE -> 8;
        };
        return depth >= minimumDepth;
    }

    /** Increase rare-room weight without changing the stable base catalog. */
    public static int roomWeight(RoomDefinition definition, int depth) {
        Objects.requireNonNull(definition, "definition");
        Profile profile = profileForDepth(depth);
        int tierMultiplier = switch (definition.piece().rarity()) {
            case COMMON -> 0;
            case UNCOMMON -> 1;
            case RARE -> 2;
            case VERY_RARE -> 3;
        };
        long weight = definition.piece().weight()
                + (long) definition.piece().weight()
                        * profile.rareRoomBonusPercent()
                        * tierMultiplier
                        / 100L;
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, weight));
    }

    /** Progressively favor broader, branched, and unusual corridor variants. */
    public static int corridorWeight(CorridorKind kind, int baseWeight, int depth) {
        Objects.requireNonNull(kind, "kind");
        validateDepth(depth);
        if (baseWeight <= 0) {
            return 0;
        }
        int boostPerDepth = switch (kind) {
            case T_JUNCTION, FOUR_WAY -> 6;
            case LEFT_TURN, RIGHT_TURN -> 4;
            case WIDE_CORRIDOR, NARROW_CORRIDOR -> 3;
            case DEAD_END -> 1;
            default -> 0;
        };
        long weight = baseWeight + (long) baseWeight * boostPerDepth * depth / 100L;
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, weight));
    }

    private static Profile profile(
            GenerationGrid.Cell cell,
            int floorIndex,
            RandomSource random,
            Function<Integer, RegionDefinition> regionAt) {
        int distanceContribution = distanceContribution(cell);
        int branchContribution = random.nextInt(3);
        int landmarkProgression = Math.min(
                MAX_LANDMARK_PROGRESSION,
                distanceContribution / DISTANCE_CELLS_PER_DEPTH);
        int verticalContribution = Math.min(4, Math.abs(floorIndex));
        int provisionalDepth = clamp(
                distanceContribution
                        + branchContribution
                        + landmarkProgression
                        + verticalContribution);
        RegionDefinition region = regionAt.apply(provisionalDepth);
        int regionContribution = RegionCatalog.depthContribution(region);
        int depth = clamp(provisionalDepth + regionContribution);
        return new Profile(
                depth,
                distanceContribution,
                branchContribution,
                regionContribution,
                landmarkProgression,
                verticalContribution,
                Math.min(200, depth * 8),
                Math.min(100, depth * 4),
                Math.min(260, 100 + depth * 5),
                Math.min(200, depth * 6),
                Math.min(200, 100 + depth * 3),
                Math.min(100, depth * 4));
    }

    private static int distanceContribution(GenerationGrid.Cell cell) {
        long absoluteX = cell.x() == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(cell.x());
        long absoluteZ = cell.z() == Long.MIN_VALUE ? Long.MAX_VALUE : Math.abs(cell.z());
        long maximumDistance = Math.max(absoluteX, absoluteZ);
        return (int) Math.min(MAX_DEPTH, maximumDistance / DISTANCE_CELLS_PER_DEPTH);
    }

    private static int clamp(int depth) {
        return Math.max(MIN_DEPTH, Math.min(MAX_DEPTH, depth));
    }

    private static void validateDepth(int depth) {
        if (depth < MIN_DEPTH || depth > MAX_DEPTH) {
            throw new IllegalArgumentException("depth must be in " + MIN_DEPTH + ".." + MAX_DEPTH);
        }
    }

    private static void validateFloor(int floorIndex) {
        if (!VerticalCatalog.isValidFloor(floorIndex)) {
            throw new IllegalArgumentException("unsupported Labrinth floor: " + floorIndex);
        }
    }

    public record Profile(
            int depth,
            int distanceContribution,
            int branchContribution,
            int regionContribution,
            int landmarkProgression,
            int verticalContribution,
            int rareRoomBonusPercent,
            int lootBonusPercent,
            int entitySpawnMultiplierPercent,
            int hazardMultiplierPercent,
            int ambientIntensityPercent,
            int unusualnessPercent) {
        public Profile {
            validateDepth(depth);
            if (distanceContribution < 0
                    || branchContribution < 0
                    || regionContribution < 0
                    || landmarkProgression < 0
                    || verticalContribution < 0) {
                throw new IllegalArgumentException("depth contributions must not be negative");
            }
            if (rareRoomBonusPercent < 0
                    || lootBonusPercent < 0
                    || entitySpawnMultiplierPercent < 100
                    || hazardMultiplierPercent < 0
                    || ambientIntensityPercent < 100
                    || unusualnessPercent < 0) {
                throw new IllegalArgumentException("invalid depth progression modifiers");
            }
        }
    }
}
