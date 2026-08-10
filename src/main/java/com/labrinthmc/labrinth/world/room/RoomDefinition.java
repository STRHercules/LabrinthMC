package com.labrinthmc.labrinth.world.room;

import com.labrinthmc.labrinth.world.generation.StructurePiece;
import java.util.List;
import java.util.Objects;
import net.minecraft.resources.ResourceLocation;

/** Immutable room metadata plus the interior style used by the room renderer. */
public record RoomDefinition(
        RoomKind kind,
        StructurePiece piece,
        InteriorStyle interiorStyle,
        List<SpawnMarker> spawnMarkers) {
    public RoomDefinition {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(interiorStyle, "interiorStyle");
        if (piece.kind() != StructurePiece.Kind.ROOM) {
            throw new IllegalArgumentException("room definitions require ROOM structure pieces");
        }
        spawnMarkers = List.copyOf(spawnMarkers);
        for (SpawnMarker marker : spawnMarkers) {
            if (marker.x() < 0 || marker.x() >= piece.width()
                    || marker.y() < 0 || marker.y() >= piece.height()
                    || marker.z() < 0 || marker.z() >= piece.depth()) {
                throw new IllegalArgumentException("room spawn marker is outside its piece bounds: " + marker);
            }
        }
    }

    public ResourceLocation id() {
        return piece.id();
    }

    public boolean eligible(int depth, ResourceLocation region) {
        Objects.requireNonNull(region, "region");
        return depth >= piece.minDepth()
                && depth <= piece.maxDepth()
                && (piece.allowedRegions().isEmpty() || piece.allowedRegions().contains(region));
    }

    public record SpawnMarker(ResourceLocation id, int x, int y, int z) {
        public SpawnMarker {
            Objects.requireNonNull(id, "id");
        }
    }

    public enum InteriorStyle {
        EMPTY,
        STORAGE,
        CHAMBER,
        UTILITY,
        CROSS,
        LONG,
        MULTI_EXIT,
        REWARD,
        DECORATIVE,
        RARE
    }
}
