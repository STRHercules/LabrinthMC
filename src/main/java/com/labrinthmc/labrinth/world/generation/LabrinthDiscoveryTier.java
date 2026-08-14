package com.labrinthmc.labrinth.world.generation;

import java.util.Objects;

/** Player-facing discovery vocabulary used by distribution tools and locators. */
public enum LabrinthDiscoveryTier {
    MICRO,
    COMMON,
    UNCOMMON,
    RARE,
    MAJOR,
    LEGENDARY;

    /**
     * Stable depth weighting used by debug distribution reports. Selection
     * still remains owned by each definition's explicit depth range; this
     * value only describes how far a tier is expected to be discoverable.
     */
    public int preferredMinimumDepth() {
        return switch (this) {
            case MICRO, COMMON -> 0;
            case UNCOMMON -> 2;
            case RARE -> 6;
            case MAJOR -> 10;
            case LEGENDARY -> 14;
        };
    }

    public static LabrinthDiscoveryTier fromRarity(StructurePiece.Rarity rarity) {
        return switch (Objects.requireNonNull(rarity, "rarity")) {
            case COMMON -> COMMON;
            case UNCOMMON -> UNCOMMON;
            case RARE -> RARE;
            case VERY_RARE -> MAJOR;
        };
    }
}
