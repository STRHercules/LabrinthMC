/**
 * Deterministic Labrinth generation algorithms and shared generation context.
 *
 * <p>The horizontal grid uses vanilla chunks as placement boundaries and
 * groups four-by-four chunks into a 64-by-64 block generation cell. A piece's
 * minimum-corner chunk owns its existence decision, including pieces that
 * cross chunk boundaries. Neighbor edges are hashed from a canonical pair of
 * cells so load order cannot change the connection result.
 *
 * <p>Generation decisions should derive from world seed and coordinates and
 * remain bounded; they must not perform per-tick scans or client lookups.
 */
package com.labrinthmc.labrinth.world.generation;
