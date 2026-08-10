package com.labrinthmc.labrinth.world.corridor;

/**
 * The deterministic hallway and grand-corridor choices available to the
 * selector.  The compact entries are hallway-scale pieces; {@code GRAND_*}
 * entries use the same connection vocabulary with larger shells and rooms.
 */
public enum CorridorKind {
    SHORT_STRAIGHT,
    MEDIUM_STRAIGHT,
    LONG_STRAIGHT,
    LEFT_TURN,
    RIGHT_TURN,
    T_JUNCTION,
    FOUR_WAY,
    DEAD_END,
    WIDE_CORRIDOR,
    NARROW_CORRIDOR,
    CURVED_LEFT,
    CURVED_RIGHT,
    S_CURVE,
    U_TURN,
    INCLINE,
    DECLINE,
    STAIRCASE_UP,
    STAIRCASE_DOWN,
    GRAND_STRAIGHT,
    GRAND_CURVED_LEFT,
    GRAND_CURVED_RIGHT,
    GRAND_S_CURVE,
    GRAND_U_TURN,
    GRAND_T_JUNCTION,
    GRAND_FOUR_WAY,
    GRAND_INCLINE,
    GRAND_DECLINE,
    GRAND_STAIRCASE_UP,
    GRAND_STAIRCASE_DOWN
}
