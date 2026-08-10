package com.labrinthmc.labrinth.world.generation;

/** Bounded inputs shared by generation decisions and placement validation. */
public record GenerationConstraints(int minY, int maxYExclusive, int maxDepth) {
    public static final GenerationConstraints LABRINTH = new GenerationConstraints(-32, 256, 32);

    public GenerationConstraints {
        if (minY >= maxYExclusive) {
            throw new IllegalArgumentException("generation height must be positive");
        }
        if (maxDepth < 0) {
            throw new IllegalArgumentException("maxDepth must not be negative");
        }
    }

    public boolean allowsDepth(int depth) {
        return depth >= 0 && depth <= maxDepth;
    }

    public boolean containsY(int y) {
        return y >= minY && y < maxYExclusive;
    }

    public boolean contains(GenerationGrid.Bounds bounds) {
        return bounds.minY() >= minY && bounds.maxYExclusive() <= maxYExclusive;
    }
}
