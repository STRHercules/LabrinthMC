package com.labrinthmc.labrinth.world.placement;

import com.labrinthmc.labrinth.world.generation.GenerationConstraints;
import com.labrinthmc.labrinth.world.generation.GenerationContext;
import com.labrinthmc.labrinth.world.generation.PlacedStructurePiece;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/** Bounded, side-independent validation for a candidate structure piece. */
public final class StructurePlacementValidator {
    public static final int DEFAULT_MAX_COLLISION_CHECKS = 256;

    private StructurePlacementValidator() {
    }

    public enum Reason {
        ACCEPTED,
        OUT_OF_WORLD_HEIGHT,
        CONTEXT_REJECTED,
        OVERLAP,
        CHECK_LIMIT_EXCEEDED
    }

    public record Result(
            boolean accepted,
            Reason reason,
            Optional<PlacedStructurePiece> conflictingPiece,
            int examinedPieces) {
        public Result {
            Objects.requireNonNull(reason, "reason");
            Objects.requireNonNull(conflictingPiece, "conflictingPiece");
            if (examinedPieces < 0) {
                throw new IllegalArgumentException("examinedPieces must not be negative");
            }
        }

        public static Result accepted(int examinedPieces) {
            return new Result(true, Reason.ACCEPTED, Optional.empty(), examinedPieces);
        }

        public static Result rejected(Reason reason, PlacedStructurePiece conflict, int examinedPieces) {
            return new Result(false, reason, Optional.ofNullable(conflict), examinedPieces);
        }
    }

    public static Result validate(
            PlacedStructurePiece candidate,
            Collection<PlacedStructurePiece> existing,
            GenerationConstraints constraints) {
        return validate(candidate, existing, constraints, DEFAULT_MAX_COLLISION_CHECKS);
    }

    public static Result validate(
            PlacedStructurePiece candidate,
            Collection<PlacedStructurePiece> existing,
            GenerationContext context) {
        Objects.requireNonNull(context, "context");
        if (!candidate.definition().canPlace(context)) {
            return Result.rejected(Reason.CONTEXT_REJECTED, null, 0);
        }
        return validate(candidate, existing, context.constraints(), DEFAULT_MAX_COLLISION_CHECKS);
    }

    public static Result validate(
            PlacedStructurePiece candidate,
            Collection<PlacedStructurePiece> existing,
            GenerationConstraints constraints,
            int maxCollisionChecks) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(existing, "existing");
        Objects.requireNonNull(constraints, "constraints");
        if (maxCollisionChecks <= 0) {
            throw new IllegalArgumentException("maxCollisionChecks must be positive");
        }
        if (!constraints.contains(candidate.bounds())) {
            return Result.rejected(Reason.OUT_OF_WORLD_HEIGHT, null, 0);
        }

        int examined = 0;
        for (PlacedStructurePiece other : existing) {
            if (other == candidate) {
                continue;
            }
            if (++examined > maxCollisionChecks) {
                // Deliberately conservative: a bounded scan rejects an
                // uncertain candidate instead of loading or scanning farther.
                return Result.rejected(Reason.CHECK_LIMIT_EXCEEDED, null, examined);
            }
            if (!candidate.bounds().intersects(other.bounds())) {
                continue;
            }
            if (candidate.definition().permitsOverlapWith(other.definition().id())
                    || other.definition().permitsOverlapWith(candidate.definition().id())) {
                continue;
            }
            return Result.rejected(Reason.OVERLAP, other, examined);
        }
        return Result.accepted(examined);
    }
}
