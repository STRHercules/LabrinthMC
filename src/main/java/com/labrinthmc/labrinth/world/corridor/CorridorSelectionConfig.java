package com.labrinthmc.labrinth.world.corridor;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Immutable server-side controls for deterministic corridor selection. */
public record CorridorSelectionConfig(
        Map<CorridorKind, Integer> weights,
        int deadEndChancePercent,
        int maxBranching) {
    public static final int DEFAULT_DEAD_END_CHANCE_PERCENT = 20;
    public static final Codec<CorridorSelectionConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT)
                    .optionalFieldOf("weights", defaultCodecWeights())
                    .forGetter(CorridorSelectionConfig::codecWeights),
            Codec.intRange(0, 100)
                    .optionalFieldOf("dead_end_chance_percent", DEFAULT_DEAD_END_CHANCE_PERCENT)
                    .forGetter(CorridorSelectionConfig::deadEndChancePercent),
            Codec.intRange(1, 4)
                    .optionalFieldOf("max_branching", 4)
                    .forGetter(CorridorSelectionConfig::maxBranching))
            .apply(instance, CorridorSelectionConfig::fromCodecValues));

    public CorridorSelectionConfig {
        Objects.requireNonNull(weights, "weights");
        EnumMap<CorridorKind, Integer> copy = new EnumMap<>(CorridorKind.class);
        for (CorridorKind kind : CorridorKind.values()) {
            int weight = weights.getOrDefault(kind, 0);
            if (weight < 0) {
                throw new IllegalArgumentException("corridor weights must not be negative");
            }
            copy.put(kind, weight);
        }
        long totalWeight = copy.values().stream().mapToLong(Integer::longValue).sum();
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("at least one corridor weight must be positive");
        }
        if (totalWeight > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("total corridor weight must fit in a positive integer");
        }
        if (deadEndChancePercent < 0 || deadEndChancePercent > 100) {
            throw new IllegalArgumentException("deadEndChancePercent must be between 0 and 100");
        }
        if (maxBranching < 1 || maxBranching > 4) {
            throw new IllegalArgumentException("maxBranching must be between 1 and 4");
        }
        weights = Collections.unmodifiableMap(copy);
    }

    public static CorridorSelectionConfig defaults() {
        EnumMap<CorridorKind, Integer> weights = new EnumMap<>(CorridorKind.class);
        weights.put(CorridorKind.SHORT_STRAIGHT, 3);
        weights.put(CorridorKind.MEDIUM_STRAIGHT, 4);
        weights.put(CorridorKind.LONG_STRAIGHT, 5);
        weights.put(CorridorKind.LEFT_TURN, 3);
        weights.put(CorridorKind.RIGHT_TURN, 3);
        weights.put(CorridorKind.T_JUNCTION, 2);
        weights.put(CorridorKind.FOUR_WAY, 1);
        weights.put(CorridorKind.DEAD_END, 2);
        weights.put(CorridorKind.WIDE_CORRIDOR, 2);
        weights.put(CorridorKind.NARROW_CORRIDOR, 2);
        return new CorridorSelectionConfig(weights, DEFAULT_DEAD_END_CHANCE_PERCENT, 4);
    }

    private static Map<String, Integer> defaultCodecWeights() {
        return codecWeights(defaults());
    }

    private static Map<String, Integer> codecWeights(CorridorSelectionConfig config) {
        Map<String, Integer> values = new LinkedHashMap<>();
        for (CorridorKind kind : CorridorKind.values()) {
            values.put(kind.name().toLowerCase(Locale.ROOT), config.weight(kind));
        }
        return values;
    }

    private static CorridorSelectionConfig fromCodecValues(
            Map<String, Integer> encodedWeights,
            int deadEndChancePercent,
            int maxBranching) {
        EnumMap<CorridorKind, Integer> weights = new EnumMap<>(CorridorKind.class);
        CorridorSelectionConfig defaults = defaults();
        for (CorridorKind kind : CorridorKind.values()) {
            weights.put(kind, defaults.weight(kind));
        }
        for (Map.Entry<String, Integer> entry : encodedWeights.entrySet()) {
            final CorridorKind kind;
            try {
                kind = CorridorKind.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("unknown corridor weight: " + entry.getKey(), exception);
            }
            weights.put(kind, entry.getValue());
        }
        return new CorridorSelectionConfig(weights, deadEndChancePercent, maxBranching);
    }

    public int weight(CorridorKind kind) {
        return weights.getOrDefault(Objects.requireNonNull(kind, "kind"), 0);
    }

    public CorridorSelectionConfig withWeight(CorridorKind kind, int weight) {
        Objects.requireNonNull(kind, "kind");
        EnumMap<CorridorKind, Integer> copy = new EnumMap<>(weights);
        copy.put(kind, weight);
        return new CorridorSelectionConfig(copy, deadEndChancePercent, maxBranching);
    }

    public CorridorSelectionConfig withDeadEndChancePercent(int chancePercent) {
        return new CorridorSelectionConfig(weights, chancePercent, maxBranching);
    }

    public CorridorSelectionConfig withMaxBranching(int branching) {
        return new CorridorSelectionConfig(weights, deadEndChancePercent, branching);
    }
}
