package com.labrinthmc.labrinth.world.generation;

import com.labrinthmc.labrinth.world.connector.Connector;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/**
 * Immutable reusable definition for one room, corridor, or other generated
 * structure. Placement state is kept in {@link PlacedStructurePiece} so the
 * same definition can be reused at multiple deterministic locations.
 */
public final class StructurePiece {
    public enum Kind {
        ROOM,
        CORRIDOR,
        JUNCTION,
        STAIRWAY,
        SHAFT,
        LANDMARK,
        SECRET_PASSAGE
    }

    public enum Rarity {
        COMMON,
        UNCOMMON,
        RARE,
        VERY_RARE
    }

    public enum Rotation {
        NONE(0),
        CLOCKWISE_90(1),
        CLOCKWISE_180(2),
        COUNTERCLOCKWISE_90(3);

        private final int quarterTurns;

        Rotation(int quarterTurns) {
            this.quarterTurns = quarterTurns;
        }

        public Rotation add(Rotation other) {
            int turns = (quarterTurns + other.quarterTurns) & 3;
            return fromQuarterTurns(turns);
        }

        /** Reflect a visual rotation when a piece is mirrored across X/Z. */
        public Rotation mirrored(Mirror mirror) {
            return mirror == Mirror.NONE || this == NONE || this == CLOCKWISE_180
                    ? this
                    : this == CLOCKWISE_90 ? COUNTERCLOCKWISE_90 : CLOCKWISE_90;
        }

        private static Rotation fromQuarterTurns(int quarterTurns) {
            return switch (quarterTurns) {
                case 0 -> NONE;
                case 1 -> CLOCKWISE_90;
                case 2 -> CLOCKWISE_180;
                case 3 -> COUNTERCLOCKWISE_90;
                default -> throw new IllegalArgumentException("quarter turns must be normalized");
            };
        }
    }

    /**
     * LEFT_RIGHT reflects local X and FRONT_BACK reflects local Z. Coordinates
     * are block-grid anchors, so a boundary point at x == width remains on a
     * boundary after reflection.
     */
    public enum Mirror {
        NONE,
        LEFT_RIGHT,
        FRONT_BACK
    }

    /** A world or local block-grid point used for piece origins and connectors. */
    public record BlockPoint(long x, int y, long z) {
        public BlockPoint add(long deltaX, int deltaY, long deltaZ) {
            return new BlockPoint(
                    Math.addExact(x, deltaX),
                    Math.addExact(y, deltaY),
                    Math.addExact(z, deltaZ));
        }
    }

    /** Metadata for placement systems that need more than depth and region. */
    public record PlacementConditions(int minClearance, boolean requiresLighting) {
        public PlacementConditions {
            if (minClearance < 0) {
                throw new IllegalArgumentException("minClearance must not be negative");
            }
        }

        public static PlacementConditions none() {
            return new PlacementConditions(0, false);
        }
    }

    /** Optional loot-table metadata; actual loot generation belongs to Phase 15. */
    public record LootConfiguration(Optional<ResourceLocation> table, boolean generateOnce) {
        public LootConfiguration {
            Objects.requireNonNull(table, "table");
        }

        public static LootConfiguration none() {
            return new LootConfiguration(Optional.empty(), true);
        }

        public static LootConfiguration table(ResourceLocation table) {
            return new LootConfiguration(Optional.of(Objects.requireNonNull(table, "table")), true);
        }
    }

    /** Ordered, immutable decoration-rule identifiers for later population. */
    public record DecorationRules(List<ResourceLocation> ruleIds) {
        public DecorationRules {
            Objects.requireNonNull(ruleIds, "ruleIds");
            ruleIds = List.copyOf(ruleIds);
        }

        public static DecorationRules none() {
            return new DecorationRules(List.of());
        }
    }

    private final ResourceLocation id;
    private final ResourceLocation template;
    private final Kind kind;
    private final int width;
    private final int height;
    private final int depth;
    private final int weight;
    private final Rarity rarity;
    private final Set<Rotation> allowedRotations;
    private final Set<Mirror> allowedMirrors;
    private final int minDepth;
    private final int maxDepth;
    private final Set<ResourceLocation> allowedRegions;
    private final List<Connector> connectors;
    private final PlacementConditions placementConditions;
    private final LootConfiguration lootConfiguration;
    private final DecorationRules decorationRules;
    private final Set<ResourceLocation> permittedOverlapIds;

    private StructurePiece(Builder builder) {
        id = Objects.requireNonNull(builder.id, "id");
        template = Objects.requireNonNull(builder.template, "template");
        kind = Objects.requireNonNull(builder.kind, "kind");
        width = positive(builder.width, "width");
        height = positive(builder.height, "height");
        depth = positive(builder.depth, "depth");
        if (builder.weight < 0) {
            throw new IllegalArgumentException("weight must not be negative");
        }
        weight = builder.weight;
        rarity = Objects.requireNonNull(builder.rarity, "rarity");
        if (builder.allowedRotations.isEmpty()) {
            throw new IllegalArgumentException("at least one rotation is required");
        }
        if (builder.allowedMirrors.isEmpty()) {
            throw new IllegalArgumentException("at least one mirror rule is required");
        }
        allowedRotations = immutableEnumSet(builder.allowedRotations);
        allowedMirrors = immutableEnumSet(builder.allowedMirrors);
        if (builder.minDepth > builder.maxDepth) {
            throw new IllegalArgumentException("minDepth must not exceed maxDepth");
        }
        minDepth = builder.minDepth;
        maxDepth = builder.maxDepth;
        allowedRegions = immutableSet(builder.allowedRegions);
        connectors = List.copyOf(builder.connectors);
        for (Connector connector : connectors) {
            validateConnector(connector);
        }
        placementConditions = Objects.requireNonNull(builder.placementConditions, "placementConditions");
        lootConfiguration = Objects.requireNonNull(builder.lootConfiguration, "lootConfiguration");
        decorationRules = Objects.requireNonNull(builder.decorationRules, "decorationRules");
        permittedOverlapIds = immutableSet(builder.permittedOverlapIds);
    }

    public static Builder builder(
            ResourceLocation id,
            ResourceLocation template,
            Kind kind,
            int width,
            int height,
            int depth) {
        return new Builder(id, template, kind, width, height, depth);
    }

    public ResourceLocation id() {
        return id;
    }

    public ResourceLocation template() {
        return template;
    }

    public Kind kind() {
        return kind;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public int depth() {
        return depth;
    }

    public int weight() {
        return weight;
    }

    public Rarity rarity() {
        return rarity;
    }

    public Set<Rotation> allowedRotations() {
        return allowedRotations;
    }

    public Set<Mirror> allowedMirrors() {
        return allowedMirrors;
    }

    public int minDepth() {
        return minDepth;
    }

    public int maxDepth() {
        return maxDepth;
    }

    public Set<ResourceLocation> allowedRegions() {
        return allowedRegions;
    }

    public List<Connector> connectors() {
        return connectors;
    }

    public PlacementConditions placementConditions() {
        return placementConditions;
    }

    public LootConfiguration lootConfiguration() {
        return lootConfiguration;
    }

    public DecorationRules decorationRules() {
        return decorationRules;
    }

    public Set<ResourceLocation> permittedOverlapIds() {
        return permittedOverlapIds;
    }

    public boolean allows(Rotation rotation, Mirror mirror) {
        return allowedRotations.contains(rotation) && allowedMirrors.contains(mirror);
    }

    public boolean canPlace(GenerationContext context) {
        Objects.requireNonNull(context, "context");
        return context.depth() >= minDepth
                && context.depth() <= maxDepth
                && (allowedRegions.isEmpty() || allowedRegions.contains(context.region()));
    }

    public boolean permitsOverlapWith(ResourceLocation otherId) {
        return permittedOverlapIds.contains(otherId);
    }

    public PlacedStructurePiece placedAt(BlockPoint origin, Rotation rotation, Mirror mirror) {
        return new PlacedStructurePiece(this, origin, rotation, mirror);
    }

    public static int transformedWidth(int width, int depth, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? depth
                : width;
    }

    public static int transformedDepth(int width, int depth, Rotation rotation) {
        return rotation == Rotation.CLOCKWISE_90 || rotation == Rotation.COUNTERCLOCKWISE_90
                ? width
                : depth;
    }

    private void validateConnector(Connector connector) {
        Connector.Position position = connector.position();
        if (position.x() < 0 || position.x() > width
                || position.y() < 0 || position.y() > height
                || position.z() < 0 || position.z() > depth) {
            throw new IllegalArgumentException("connector position is outside piece bounds: " + connector);
        }
        if (connector.height() > height) {
            throw new IllegalArgumentException("connector height exceeds piece height: " + connector);
        }
        long horizontalLimit = connector.direction() == Connector.Direction.NORTH
                || connector.direction() == Connector.Direction.SOUTH ? width : depth;
        if (connector.width() > horizontalLimit) {
            throw new IllegalArgumentException("connector width exceeds piece face: " + connector);
        }
        boolean onFace = switch (connector.direction()) {
            case NORTH -> position.z() == 0;
            case EAST -> position.x() == width;
            case SOUTH -> position.z() == depth;
            case WEST -> position.x() == 0;
            case UP -> position.y() == height;
            case DOWN -> position.y() == 0;
        };
        if (!onFace) {
            throw new IllegalArgumentException("connector must lie on its facing boundary: " + connector);
        }
        if (connector.required() && !connector.isOpen()) {
            throw new IllegalArgumentException("required connectors must remain open: " + connector);
        }
    }

    private static int positive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static <E extends Enum<E>> Set<E> immutableEnumSet(Collection<E> values) {
        EnumSet<E> copy = EnumSet.copyOf(values);
        return Collections.unmodifiableSet(copy);
    }

    private static <E> Set<E> immutableSet(Collection<E> values) {
        return Collections.unmodifiableSet(Set.copyOf(values));
    }

    public static final class Builder {
        private final ResourceLocation id;
        private final ResourceLocation template;
        private final Kind kind;
        private final int width;
        private final int height;
        private final int depth;
        private int weight = 1;
        private Rarity rarity = Rarity.COMMON;
        private Set<Rotation> allowedRotations = EnumSet.allOf(Rotation.class);
        private Set<Mirror> allowedMirrors = EnumSet.of(Mirror.NONE);
        private int minDepth = 0;
        private int maxDepth = Integer.MAX_VALUE;
        private Set<ResourceLocation> allowedRegions = Set.of();
        private List<Connector> connectors = List.of();
        private PlacementConditions placementConditions = PlacementConditions.none();
        private LootConfiguration lootConfiguration = LootConfiguration.none();
        private DecorationRules decorationRules = DecorationRules.none();
        private Set<ResourceLocation> permittedOverlapIds = Set.of();

        private Builder(
                ResourceLocation id,
                ResourceLocation template,
                Kind kind,
                int width,
                int height,
                int depth) {
            this.id = id;
            this.template = template;
            this.kind = kind;
            this.width = width;
            this.height = height;
            this.depth = depth;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder rarity(Rarity rarity) {
            this.rarity = rarity;
            return this;
        }

        public Builder rotations(Set<Rotation> rotations) {
            this.allowedRotations = EnumSet.copyOf(rotations);
            return this;
        }

        public Builder rotation(Rotation rotation) {
            this.allowedRotations = EnumSet.of(rotation);
            return this;
        }

        public Builder mirrors(Set<Mirror> mirrors) {
            this.allowedMirrors = EnumSet.copyOf(mirrors);
            return this;
        }

        public Builder mirror(Mirror mirror) {
            this.allowedMirrors = EnumSet.of(mirror);
            return this;
        }

        public Builder depthRange(int minDepth, int maxDepth) {
            this.minDepth = minDepth;
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder allowedRegions(Collection<ResourceLocation> regions) {
            this.allowedRegions = Set.copyOf(regions);
            return this;
        }

        public Builder connectors(Collection<Connector> connectors) {
            this.connectors = List.copyOf(connectors);
            return this;
        }

        public Builder placementConditions(PlacementConditions conditions) {
            this.placementConditions = conditions;
            return this;
        }

        public Builder loot(LootConfiguration lootConfiguration) {
            this.lootConfiguration = lootConfiguration;
            return this;
        }

        public Builder decorations(DecorationRules decorationRules) {
            this.decorationRules = decorationRules;
            return this;
        }

        public Builder permitOverlapWith(ResourceLocation pieceId) {
            var updated = new HashSet<>(permittedOverlapIds);
            updated.add(Objects.requireNonNull(pieceId, "pieceId"));
            permittedOverlapIds = updated;
            return this;
        }

        public StructurePiece build() {
            return new StructurePiece(this);
        }
    }
}
