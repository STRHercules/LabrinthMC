package com.labrinthmc.labrinth.world.generation;

import java.util.List;
import java.util.Objects;
import java.util.function.ToIntFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;

/** Small deterministic weighted-pool helper shared by generated content. */
public final class LabrinthPiecePool {
    private LabrinthPiecePool() {
    }

    public static <T> T choose(
            RandomSource random,
            List<T> candidates,
            ToIntFunction<T> weight) {
        Objects.requireNonNull(random, "random");
        Objects.requireNonNull(candidates, "candidates");
        Objects.requireNonNull(weight, "weight");
        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("pool must contain at least one candidate");
        }
        int totalWeight = 0;
        for (T candidate : candidates) {
            int candidateWeight = weight.applyAsInt(candidate);
            if (candidateWeight < 0) {
                throw new IllegalArgumentException("pool weights must not be negative");
            }
            totalWeight = Math.addExact(totalWeight, candidateWeight);
        }
        if (totalWeight <= 0) {
            throw new IllegalArgumentException("pool must contain a positive weight");
        }
        int choice = random.nextInt(totalWeight);
        for (T candidate : candidates) {
            choice -= weight.applyAsInt(candidate);
            if (choice < 0) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    /** Immutable named pool for rooms, modules, or authored templates. */
    public record Pool<T>(
            ResourceLocation id,
            List<T> entries,
            ToIntFunction<T> weight) {
        public Pool {
            Objects.requireNonNull(id, "id");
            entries = List.copyOf(Objects.requireNonNull(entries, "entries"));
            Objects.requireNonNull(weight, "weight");
            if (entries.isEmpty()) {
                throw new IllegalArgumentException("named pool must contain entries");
            }
        }

        public T choose(RandomSource random) {
            return LabrinthPiecePool.choose(random, entries, weight);
        }
    }

    public static <T> Pool<T> named(
            ResourceLocation id,
            List<T> entries,
            ToIntFunction<T> weight) {
        return new Pool<>(id, entries, weight);
    }
}
