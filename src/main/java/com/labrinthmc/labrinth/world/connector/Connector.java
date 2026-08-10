package com.labrinthmc.labrinth.world.connector;

import com.labrinthmc.labrinth.world.generation.StructurePiece;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/** Immutable local or transformed endpoint for a modular structure piece. */
public record Connector(
        Position position,
        Direction direction,
        Type type,
        int width,
        int height,
        StructurePiece.Rotation rotation,
        boolean required,
        State state,
        Set<Type> compatibleTypes) {
    public Connector {
        Objects.requireNonNull(position, "position");
        Objects.requireNonNull(direction, "direction");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(rotation, "rotation");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(compatibleTypes, "compatibleTypes");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("connector dimensions must be positive");
        }
        compatibleTypes = Collections.unmodifiableSet(Set.copyOf(compatibleTypes));
    }

    public Connector(
            Position position,
            Direction direction,
            Type type,
            int width,
            int height,
            StructurePiece.Rotation rotation,
            boolean required) {
        this(position, direction, type, width, height, rotation, required, State.OPEN, Set.of());
    }

    public Connector(
            Position position,
            Direction direction,
            Type type,
            int width,
            int height,
            StructurePiece.Rotation rotation,
            boolean required,
            boolean blocked) {
        this(position, direction, type, width, height, rotation, required,
                blocked ? State.BLOCKED : State.OPEN, Set.of());
    }

    public boolean isOpen() {
        return state == State.OPEN;
    }

    public boolean isCapped() {
        return state == State.CAPPED;
    }

    public boolean isBlocked() {
        return state == State.BLOCKED;
    }

    public Connector capped() {
        return new Connector(position, direction, type, width, height, rotation, required, State.CAPPED,
                compatibleTypes);
    }

    public Connector blocked() {
        return new Connector(position, direction, type, width, height, rotation, required, State.BLOCKED,
                compatibleTypes);
    }

    public Connector withCompatibleTypes(Set<Type> types) {
        return new Connector(position, direction, type, width, height, rotation, required, state, types);
    }

    /**
     * A connector can join only an open connector facing the other piece, with
     * equal aperture dimensions, matching profile rotation, and compatible
     * connector types. Explicit type sets must agree from both sides.
     */
    public boolean compatibleWith(Connector other) {
        Objects.requireNonNull(other, "other");
        return isOpen()
                && other.isOpen()
                && direction.opposite() == other.direction
                && width == other.width
                && height == other.height
                && rotation == other.rotation
                && acceptsType(other)
                && other.acceptsType(this);
    }

    public boolean acceptsType(Connector other) {
        return compatibleTypes.isEmpty()
                ? type.compatibleWith(other.type)
                : compatibleTypes.contains(other.type);
    }

    /** Transform a local connector into its placed world-space representation. */
    public Connector transformed(
            int pieceWidth,
            int pieceDepth,
            StructurePiece.BlockPoint origin,
            StructurePiece.Rotation pieceRotation,
            StructurePiece.Mirror mirror) {
        Position transformedPosition = transformPosition(position, pieceWidth, pieceDepth, pieceRotation, mirror);
        return new Connector(
                new Position(
                        Math.addExact(origin.x(), transformedPosition.x()),
                        Math.addExact(origin.y(), transformedPosition.y()),
                        Math.addExact(origin.z(), transformedPosition.z())),
                direction.mirrored(mirror).rotated(pieceRotation),
                type,
                width,
                height,
                rotation.mirrored(mirror).add(pieceRotation),
                required,
                state,
                compatibleTypes);
    }

    private static Position transformPosition(
            Position position,
            int pieceWidth,
            int pieceDepth,
            StructurePiece.Rotation pieceRotation,
            StructurePiece.Mirror mirror) {
        long x = position.x();
        long z = position.z();
        if (mirror == StructurePiece.Mirror.LEFT_RIGHT) {
            x = pieceWidth - x;
        } else if (mirror == StructurePiece.Mirror.FRONT_BACK) {
            z = pieceDepth - z;
        }
        long rotatedX;
        long rotatedZ;
        switch (pieceRotation) {
            case NONE -> {
                rotatedX = x;
                rotatedZ = z;
            }
            case CLOCKWISE_90 -> {
                rotatedX = pieceDepth - z;
                rotatedZ = x;
            }
            case CLOCKWISE_180 -> {
                rotatedX = pieceWidth - x;
                rotatedZ = pieceDepth - z;
            }
            case COUNTERCLOCKWISE_90 -> {
                rotatedX = z;
                rotatedZ = pieceWidth - x;
            }
            default -> throw new IllegalStateException("unhandled piece rotation: " + pieceRotation);
        }
        return new Position(rotatedX, position.y(), rotatedZ);
    }

    public record Position(long x, int y, long z) {
    }

    public enum State {
        OPEN,
        CAPPED,
        BLOCKED
    }

    public enum Type {
        STANDARD,
        WIDE,
        DOOR,
        ARCH,
        STAIR_UP,
        STAIR_DOWN,
        SHAFT,
        VENT,
        LANDMARK,
        SPECIAL;

        public boolean compatibleWith(Type other) {
            return switch (this) {
                case STANDARD -> other == STANDARD || other == DOOR || other == ARCH;
                case WIDE -> other == WIDE || other == LANDMARK;
                case DOOR -> other == DOOR || other == STANDARD || other == ARCH;
                case ARCH -> other == ARCH || other == STANDARD || other == DOOR;
                case STAIR_UP -> other == STAIR_DOWN;
                case STAIR_DOWN -> other == STAIR_UP;
                case SHAFT -> other == SHAFT;
                case VENT -> other == VENT;
                case LANDMARK -> other == LANDMARK || other == WIDE;
                case SPECIAL -> other == SPECIAL;
            };
        }
    }

    public enum Direction {
        NORTH,
        EAST,
        SOUTH,
        WEST,
        UP,
        DOWN;

        public Direction opposite() {
            return switch (this) {
                case NORTH -> SOUTH;
                case EAST -> WEST;
                case SOUTH -> NORTH;
                case WEST -> EAST;
                case UP -> DOWN;
                case DOWN -> UP;
            };
        }

        public Direction rotated(StructurePiece.Rotation rotation) {
            if (this == UP || this == DOWN) {
                return this;
            }
            return switch (rotation) {
                case NONE -> this;
                case CLOCKWISE_90 -> switch (this) {
                    case NORTH -> EAST;
                    case EAST -> SOUTH;
                    case SOUTH -> WEST;
                    case WEST -> NORTH;
                    default -> this;
                };
                case CLOCKWISE_180 -> opposite();
                case COUNTERCLOCKWISE_90 -> switch (this) {
                    case NORTH -> WEST;
                    case WEST -> SOUTH;
                    case SOUTH -> EAST;
                    case EAST -> NORTH;
                    default -> this;
                };
            };
        }

        public Direction mirrored(StructurePiece.Mirror mirror) {
            return switch (mirror) {
                case NONE -> this;
                case LEFT_RIGHT -> switch (this) {
                    case EAST -> WEST;
                    case WEST -> EAST;
                    default -> this;
                };
                case FRONT_BACK -> switch (this) {
                    case NORTH -> SOUTH;
                    case SOUTH -> NORTH;
                    default -> this;
                };
            };
        }
    }
}
