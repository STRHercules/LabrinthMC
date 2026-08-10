package com.labrinthmc.labrinth.world.region;

import com.labrinthmc.labrinth.world.generation.GenerationGrid;
import com.labrinthmc.labrinth.world.generation.GenerationSeeds;
import com.labrinthmc.labrinth.world.room.RoomKind;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;

/** Deterministic coarse region field and immutable initial region catalog. */
public final class RegionCatalog {
    public static final ResourceLocation STANDARD_ID = id("standard");
    public static final ResourceLocation ABANDONED_ID = id("abandoned");
    public static final ResourceLocation INDUSTRIAL_ID = id("industrial");
    public static final ResourceLocation FLOODED_ID = id("flooded");
    public static final ResourceLocation OVERGROWN_ID = id("overgrown");
    public static final ResourceLocation ANCIENT_ID = id("ancient");
    public static final ResourceLocation CORRUPTED_ID = id("corrupted");
    public static final Set<ResourceLocation> REGION_IDS = Set.of(
            STANDARD_ID,
            ABANDONED_ID,
            INDUSTRIAL_ID,
            FLOODED_ID,
            OVERGROWN_ID,
            ANCIENT_ID,
            CORRUPTED_ID);

    private static final int MACRO_CELL_SIZE = 8;
    private static final int TRANSITION_BAND_CELLS = 2;
    private static final ResourceLocation RANDOM_FACTORY_ID = id("region_selection");
    private static final List<RegionDefinition> DEFINITIONS = List.of(
            createStandard(),
            abandoned(),
            industrial(),
            flooded(),
            overgrown(),
            ancient(),
            corrupted());
    private static final Map<ResourceLocation, RegionDefinition> BY_ID =
            DEFINITIONS.stream().collect(java.util.stream.Collectors.toUnmodifiableMap(
                    RegionDefinition::id,
                    Function.identity()));

    private RegionCatalog() {
    }

    public static List<RegionDefinition> definitions() {
        return DEFINITIONS;
    }

    public static RegionDefinition definition(ResourceLocation id) {
        return BY_ID.get(Objects.requireNonNull(id, "id"));
    }

    public static RegionDefinition resolve(ResourceLocation id) {
        RegionDefinition definition = definition(id);
        return definition == null ? standard() : definition;
    }

    public static RegionDefinition standard() {
        return find(STANDARD_ID);
    }

    /** Regions can make an area feel deeper without changing physical Y. */
    public static int depthContribution(RegionDefinition region) {
        Objects.requireNonNull(region, "region");
        if (ANCIENT_ID.equals(region.id())) {
            return 2;
        }
        if (CORRUPTED_ID.equals(region.id())) {
            return 4;
        }
        if (INDUSTRIAL_ID.equals(region.id())
                || FLOODED_ID.equals(region.id())
                || OVERGROWN_ID.equals(region.id())) {
            return 1;
        }
        return 0;
    }

    public static RegionDefinition select(
            RandomState randomState,
            GenerationGrid.Cell cell) {
        return select(randomState, cell, 0, 0);
    }

    public static RegionDefinition select(
            RandomState randomState,
            GenerationGrid.Cell cell,
            int depth,
            int floorIndex) {
        Objects.requireNonNull(randomState, "randomState");
        Objects.requireNonNull(cell, "cell");
        validateDepth(depth);
        if (!com.labrinthmc.labrinth.world.generation.VerticalCatalog.isValidFloor(floorIndex)) {
            throw new IllegalArgumentException("unsupported Labrinth floor: " + floorIndex);
        }
        PositionalRandomFactory factory = randomState.getOrCreateRandomFactory(RANDOM_FACTORY_ID);
        return select(
                cell,
                depth,
                floorIndex,
                candidate -> factory.at(
                        Math.toIntExact(candidate.x()),
                        Math.addExact(depth, floorIndex),
                        Math.toIntExact(candidate.z())));
    }

    public static RegionDefinition select(
            long worldSeed,
            GenerationGrid.Cell cell,
            int depth,
            int floorIndex) {
        Objects.requireNonNull(cell, "cell");
        validateDepth(depth);
        if (!com.labrinthmc.labrinth.world.generation.VerticalCatalog.isValidFloor(floorIndex)) {
            throw new IllegalArgumentException("unsupported Labrinth floor: " + floorIndex);
        }
        return select(
                cell,
                depth,
                floorIndex,
                candidate -> RandomSource.create(
                        GenerationSeeds.regionSeed(worldSeed, candidate, depth, floorIndex)));
    }

    public static RegionDefinition select(long worldSeed, GenerationGrid.Cell cell) {
        return select(worldSeed, cell, 0, 0);
    }

    private static RegionDefinition select(
            GenerationGrid.Cell cell,
            int depth,
            int floorIndex,
            Function<GenerationGrid.Cell, RandomSource> randomForCell) {
        if (cell.x() == 0 && cell.z() == 0) {
            return standard();
        }
        GenerationGrid.Cell macro = macroCell(cell);
        RegionDefinition primary = weightedChoice(
                eligible(depth, floorIndex),
                randomForCell.apply(macro));
        if (macro.x() == 0 && macro.z() == 0) {
            // Keep the origin sector predictable and safe; themed regions begin
            // after the standard core and transition at the macro boundary.
            primary = standard();
        }
        long localX = Math.floorMod(cell.x(), (long) MACRO_CELL_SIZE);
        long localZ = Math.floorMod(cell.z(), (long) MACRO_CELL_SIZE);
        int edgeDistance = (int) Math.min(
                Math.min(localX, MACRO_CELL_SIZE - 1L - localX),
                Math.min(localZ, MACRO_CELL_SIZE - 1L - localZ));
        if (edgeDistance >= TRANSITION_BAND_CELLS) {
            return primary;
        }

        GenerationGrid.Cell neighborMacro = transitionNeighbor(macro, localX, localZ);
        RegionDefinition secondary = weightedChoice(
                eligible(depth, floorIndex),
                randomForCell.apply(neighborMacro));
        if (primary.id().equals(secondary.id())) {
            return primary;
        }
        int transitionChance = (TRANSITION_BAND_CELLS - edgeDistance) * 25;
        return randomForCell.apply(cell).nextInt(100) < transitionChance
                ? secondary
                : primary;
    }

    private static RegionDefinition weightedChoice(
            List<RegionDefinition> candidates,
            RandomSource random) {
        if (candidates.isEmpty()) {
            return standard();
        }
        int totalWeight = candidates.stream().mapToInt(RegionDefinition::weight).sum();
        if (totalWeight <= 0) {
            return standard();
        }
        int choice = random.nextInt(totalWeight);
        for (RegionDefinition candidate : candidates) {
            choice -= candidate.weight();
            if (choice < 0) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private static List<RegionDefinition> eligible(int depth, int floorIndex) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.weight() > 0)
                .filter(definition -> definition.eligible(depth, floorIndex))
                .toList();
    }

    private static GenerationGrid.Cell macroCell(GenerationGrid.Cell cell) {
        return new GenerationGrid.Cell(
                Math.floorDiv(cell.x(), MACRO_CELL_SIZE),
                Math.floorDiv(cell.z(), MACRO_CELL_SIZE));
    }

    private static GenerationGrid.Cell transitionNeighbor(
            GenerationGrid.Cell macro,
            long localX,
            long localZ) {
        long distanceX = Math.min(localX, MACRO_CELL_SIZE - 1L - localX);
        long distanceZ = Math.min(localZ, MACRO_CELL_SIZE - 1L - localZ);
        if (distanceX <= distanceZ) {
            return new GenerationGrid.Cell(
                    macro.x() + (localX < MACRO_CELL_SIZE / 2 ? -1 : 1),
                    macro.z());
        }
        return new GenerationGrid.Cell(
                macro.x(),
                macro.z() + (localZ < MACRO_CELL_SIZE / 2 ? -1 : 1));
    }

    private static void validateDepth(int depth) {
        if (depth < 0 || depth > 32) {
            throw new IllegalArgumentException("region depth must be in 0..32");
        }
    }

    private static RegionDefinition find(ResourceLocation id) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("missing region: " + id));
    }

    private static RegionDefinition createStandard() {
        return definition(
                STANDARD_ID,
                45,
                allRooms(),
                allCorridors(),
                palette("polished_deepslate", "deepslate_bricks",
                        "polished_deepslate", "sea_lantern", "chiseled_deepslate"),
                new RegionDefinition.LightingRules(true, 0),
                decorations(false, false, false, false, false, false, 0,
                        "gravel", "iron_block", "moss_block"),
                new RegionDefinition.MobRules(false, 0, Set.of()),
                loot(0, "standard"),
                ambient(0.10F, false),
                conditions(0, 32, -1, 1));
    }

    private static RegionDefinition abandoned() {
        return definition(
                ABANDONED_ID,
                18,
                allRooms(),
                allCorridors(),
                palette("cracked_deepslate_bricks", "cracked_deepslate_bricks",
                        "deepslate_tiles", "soul_lantern", "mossy_cobblestone"),
                new RegionDefinition.LightingRules(true, 55),
                decorations(true, false, false, true, false, false, 18,
                        "gravel", "chain", "moss_block"),
                new RegionDefinition.MobRules(true, 2, tags("abandoned")),
                loot(10, "abandoned"),
                ambient(0.06F, true),
                conditions(0, 32, -1, 1));
    }

    private static RegionDefinition industrial() {
        return definition(
                INDUSTRIAL_ID,
                14,
                rooms(RoomKind.SMALL_STORAGE, RoomKind.UTILITY, RoomKind.LARGE_CHAMBER),
                corridors("short_straight", "medium_straight", "long_straight", "wide", "narrow"),
                palette("polished_deepslate", "deepslate_bricks",
                        "iron_block", "glowstone", "iron_block"),
                new RegionDefinition.LightingRules(true, 10),
                decorations(false, true, false, false, false, false, 22,
                        "gravel", "iron_block", "iron_bars"),
                new RegionDefinition.MobRules(true, 3, tags("industrial")),
                loot(15, "industrial"),
                ambient(0.08F, false),
                conditions(0, 32, -1, 1));
    }

    private static RegionDefinition flooded() {
        return definition(
                FLOODED_ID,
                8,
                rooms(RoomKind.EMPTY, RoomKind.LARGE_CHAMBER, RoomKind.UTILITY, RoomKind.DECORATIVE),
                corridors("short_straight", "medium_straight", "narrow", "dead_end"),
                palette("dark_prismarine", "prismarine_bricks",
                        "prismarine_bricks", "sea_lantern", "prismarine"),
                new RegionDefinition.LightingRules(true, 25),
                decorations(false, false, false, false, true, false, 20,
                        "gravel", "prismarine", "seagrass"),
                new RegionDefinition.MobRules(true, 2, tags("flooded")),
                loot(8, "flooded"),
                ambient(0.12F, true),
                conditions(0, 32, -1, 0));
    }

    private static RegionDefinition overgrown() {
        return definition(
                OVERGROWN_ID,
                8,
                rooms(RoomKind.EMPTY, RoomKind.DECORATIVE, RoomKind.LONG_RECTANGULAR,
                        RoomKind.LARGE_CHAMBER),
                corridors("long_straight", "left_turn", "right_turn", "dead_end"),
                palette("moss_block", "mossy_cobblestone",
                        "mossy_cobblestone", "lantern", "moss_block"),
                new RegionDefinition.LightingRules(true, 15),
                decorations(false, false, true, true, false, false, 24,
                        "coarse_dirt", "mossy_cobblestone", "moss_block"),
                new RegionDefinition.MobRules(true, 2, tags("overgrown")),
                loot(5, "overgrown"),
                ambient(0.11F, true),
                conditions(0, 32, 0, 1));
    }

    private static RegionDefinition ancient() {
        return definition(
                ANCIENT_ID,
                5,
                rooms(RoomKind.LARGE_CHAMBER, RoomKind.DECORATIVE,
                        RoomKind.DEAD_END_REWARD, RoomKind.RARE_TEST),
                corridors("long_straight", "t_junction", "four_way", "dead_end"),
                palette("stone_bricks", "cracked_stone_bricks",
                        "stone_bricks", "lantern", "chiseled_stone_bricks"),
                new RegionDefinition.LightingRules(true, 20),
                decorations(true, false, false, false, false, true, 12,
                        "cobblestone", "stone_bricks", "moss_block"),
                new RegionDefinition.MobRules(true, 3, tags("ancient")),
                loot(25, "ancient"),
                ambient(0.07F, true),
                conditions(4, 32, -1, 1));
    }

    private static RegionDefinition corrupted() {
        return definition(
                CORRUPTED_ID,
                2,
                rooms(RoomKind.CROSS_ROOM, RoomKind.MULTI_EXIT,
                        RoomKind.RARE_TEST, RoomKind.DEAD_END_REWARD),
                corridors("left_turn", "right_turn", "t_junction", "four_way", "narrow"),
                palette("purple_concrete", "crying_obsidian",
                        "obsidian", "end_rod", "magenta_concrete"),
                new RegionDefinition.LightingRules(true, 65),
                decorations(false, false, false, false, false, true, 20,
                        "obsidian", "crying_obsidian", "purple_concrete"),
                new RegionDefinition.MobRules(true, 5, tags("corrupted")),
                loot(40, "corrupted"),
                ambient(0.04F, true),
                conditions(12, 32, 0, 1));
    }

    private static RegionDefinition definition(
            ResourceLocation id,
            int weight,
            Set<ResourceLocation> roomPool,
            Set<ResourceLocation> corridorPool,
            RegionDefinition.Palette palette,
            RegionDefinition.LightingRules lighting,
            RegionDefinition.DecorationRules decorations,
            RegionDefinition.MobRules mobs,
            RegionDefinition.LootModifiers loot,
            RegionDefinition.AmbientProperties ambient,
            RegionDefinition.GenerationConditions conditions) {
        return new RegionDefinition(
                id,
                weight,
                roomPool,
                corridorPool,
                palette,
                lighting,
                decorations,
                mobs,
                loot,
                ambient,
                conditions);
    }

    private static RegionDefinition.Palette palette(
            String floor,
            String wall,
            String ceiling,
            String light,
            String accent) {
        return new RegionDefinition.Palette(
                block(floor),
                block(wall),
                block(ceiling),
                block(light),
                block(accent));
    }

    private static RegionDefinition.DecorationRules decorations(
            boolean debris,
            boolean pipes,
            boolean vegetation,
            boolean vines,
            boolean waterlogged,
            boolean alternateGeometry,
            int density,
            String debrisBlock,
            String pipeBlock,
            String vegetationBlock) {
        return new RegionDefinition.DecorationRules(
                debris,
                pipes,
                vegetation,
                vines,
                waterlogged,
                alternateGeometry,
                density,
                block(debrisBlock),
                block(pipeBlock),
                block(vegetationBlock));
    }

    private static RegionDefinition.LootModifiers loot(int rarityBonus, String tag) {
        return new RegionDefinition.LootModifiers(
                rarityBonus,
                Optional.of(id("loot/" + tag)));
    }

    private static RegionDefinition.AmbientProperties ambient(float light, boolean fogged) {
        return new RegionDefinition.AmbientProperties(light, fogged, Optional.empty());
    }

    private static RegionDefinition.GenerationConditions conditions(
            int minDepth,
            int maxDepth,
            int minFloor,
            int maxFloor) {
        return new RegionDefinition.GenerationConditions(minDepth, maxDepth, minFloor, maxFloor);
    }

    private static Set<ResourceLocation> rooms(RoomKind... kinds) {
        return Arrays.stream(kinds)
                .map(kind -> id("room/" + kind.name().toLowerCase(Locale.ROOT)))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<ResourceLocation> allRooms() {
        return rooms(RoomKind.values());
    }

    private static Set<ResourceLocation> corridors(String... paths) {
        return Arrays.stream(paths)
                .map(path -> id("corridor/" + path))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Set<ResourceLocation> allCorridors() {
        return corridors(
                "short_straight",
                "medium_straight",
                "long_straight",
                "left_turn",
                "right_turn",
                "t_junction",
                "four_way",
                "dead_end",
                "wide",
                "narrow");
    }

    private static Set<ResourceLocation> tags(String tag) {
        return Set.of(id("mob/" + tag));
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("labrinth", path);
    }

    private static ResourceLocation block(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }
}
