## 0.10.11 Wall-side traversal QA

- Add a NeoForge game test that traverses both incline/decline directions in
  all four rotations and verifies the player reaches each floor without a
  one-block drop at the transition.
- Add a visual smoke pass for the inner-wall stairwell route at the lower
  landing, both repeated turns, stacked boundary, and upper landing.

## 0.10.10 Vertical traversal follow-up

- Add a NeoForge game test that walks the shared stair landing when a lower
  stair piece is present but the next boundary is intentionally absent.
- Add visual smoke coverage for rotated hallway stair rows and ladder support
  after chunk reload.

## 0.10.9 Stairwell traversal follow-up

- Add a NeoForge game test that walks the complete 32-step inner-wall route in
  both directions and checks each step's collision and facing.
- Add a visual stairwell smoke pass for the lower entry, repeated inner turn,
  upper landing, and all four chunk-boundary approaches.

## 0.10.8 Geometry follow-up

- Add a NeoForge game test that walks every expanded hallway silhouette across
  all rotations and verifies the five-by-four doorway air volume after chunk
  reload.
- Add a bounded traversal smoke test for the centered ramp stair row and the
  retained seven-by-seven vertical stair path at both floor boundaries.
- Replace the current fixed cell-size decoration coordinates with authored
  room-local decoration rules if future room variants need denser interiors.

## 0.10.6 Vertical passage follow-up

- Add a NeoForge game test that samples every live vertical kind at both floor
  boundaries and confirms the lower entry, continuous stair path, upper air
  opening, and horizontal wall passages are walkable after chunk reload.
- Implement drop shafts and elevator placeholders only with explicit landing,
  cap, and room/corridor passage contracts; keep them out of live selection
  until those contracts exist.
- Add a small in-world vertical debug marker for stair start/end cells during
  development, disabled in normal generation.

## 0.10.5 Connection geometry follow-up

- Add a NeoForge game test that samples every connected room, routed corridor,
  and straight corridor at all four doorway layers after rotation and chunk
  reload, checking both the canonical endpoint and the generated air volume.
- Add a bounded stair traversal smoke test that walks from the lower floor to
  the upper floor through the now-open seven-by-seven stairwell footprint.
- Keep glowstone replacement centralized in the region palette if future
  authored light blocks are introduced, and verify they remain full-cube
  structural lights.

## 0.10.4 Connector and vertical geometry follow-up

- Add a NeoForge game test that samples every connected room/corridor boundary
  at all four doorway layers and verifies there is no solid seam or air gap.
- Add a bounded stair traversal smoke test that walks the lower step, each
  switchback turn, the upper landing, and the centered floor opening.
- Keep the ceiling-light fallback covered when additional region lighting
  blocks or outage rules are introduced.

## 0.10.3 Connected layout follow-up

- Add a NeoForge game test or dedicated-server block-population smoke check
  that walks every generated doorway across chunk boundaries and confirms the
  floor, aperture, and neighbor cell remain aligned after reload.
- Add a small visual debug mode that marks cell ownership and connection edges
  in-world, making future layout regressions faster to diagnose without
  changing normal generation output.
- Tune region-specific authored landmark detail after a fresh-world
  exploration pass; keep generic decoration sparse and floor-aware.

## 0.10.2 README visual documentation follow-up

- Add a lightweight image gallery or generated thumbnail index if the README
  becomes difficult to scan as the visual documentation grows.
- Keep the README image set synchronized with `Pictures/` when new design
  documents or player-facing artwork are added.

## 0.10.1 Phase selector follow-up

- Add focused selector tests with deliberately depth-restricted room and
  corridor fixtures when the catalogs become data-driven.
- Keep region eligibility validation centralized if additional generation
  catalogs begin accepting explicit region IDs.

## 0.10.0 Depth and Landmark follow-up

- Add a NeoForge game test or dedicated-server block-population smoke check
  that visits each landmark style across chunk boundaries and confirms the
  landmark shell wins over rooms, corridors, and vertical openings.
- Connect the depth-aware loot, entity, hazard, and ambient modifiers to their
  runtime systems as those later phases ship, preserving the placement metadata
  as the shared progression input.
- Tune landmark connection requirements, sector frequency, and procedural
  interior details after an in-game exploration pass without changing the
  canonical sector-owner rule.

## 0.6.0 Region System follow-up

- Add a NeoForge game test or dedicated-server block-population smoke check for
  each themed palette, decoration rule, transition band, and vertical layer.
- Connect the region mob, loot, and ambient metadata to their runtime systems
  when those Phase 10, 15, and atmosphere systems are implemented.
- Tune macro-cell weights and transition width after an in-game exploration
  pass, keeping region selection seed-derived and chunk-order independent.

## 0.5.0 Continuous and Vertical Generation follow-up

- Add a NeoForge game test or dedicated-server block-population smoke check
  that walks the three floor layers, stair openings, ladder shafts, drop
  landings, and chunk boundaries in-game.
- Replace the elevator placeholder with a real transport interaction when
  player-facing vertical travel is implemented.
- Move floor-aware room/corridor pools and the current dead-end frequency into
  region/depth configuration once those later systems are implemented.

## 0.4.0 Room Generation follow-up

- Add a NeoForge game test or dedicated-server block-population smoke check for
  every room style across all rotations and chunk boundaries.
- Connect the room loot metadata to real loot tables when Phase 15 begins,
  including one-time container population and reload behavior.
- Move room chance and room pools into region/depth configuration once those
  later systems are implemented, keeping the Phase 5 default deterministic.

## 0.3.0 Corridor Generation follow-up

- Add a Phase 5 room-definition registry that reuses the corridor catalog's
  immutable piece and connector contracts.
- Add a NeoForge game test or equivalent block-population smoke check that
  inspects every corridor shape across both rotations and chunk boundaries.
- Tune connector-profile compatibility and visual shell variation after an
  in-game pass over generated junctions and capped dead ends.

## 0.2.6 Medium Straight follow-up

- Add the long straight definition with the same reusable shell when its Phase
  4.2 item is implemented.
- Introduce the corridor definition registry/pool when the remaining straight
  variants and Phase 4.3 selection are started.
- Add a NeoForge block-population test for the medium piece across both axes
  and its centered chunk boundaries.

## 0.2.5 Short Straight follow-up

- Add medium and long straight definitions with the same reusable shell when
  their Phase 4.2 items are implemented.
- Introduce the corridor definition registry/pool when Phase 4.3 selection is
  started, keeping variant selection separate from piece definitions.
- Add a NeoForge block-population test for the short piece across both axes and
  its centered chunk boundaries.

## 0.2.4 Straight Corridor follow-up

- Add a small registry or pool for corridor definitions when the first short,
  medium, and long variants are introduced.
- Add a game-test or dedicated-server block-shape smoke check that inspects a
  generated chunk across both corridor orientations and a chunk boundary.
- Add connector capping during the connection-continuity phase so isolated
  straight pieces cannot leave open ends into ungenerated space.

## Foundation follow-ups

- Use the repository-local wrapper in CI so every validation path runs the
  same pinned Gradle distribution.
- Add the Phase 0.2 package, registry, and generation boundaries after this
  minimal common entry point remains stable.
- Add a small automated metadata smoke check to catch mod ID, display-name, and
  dependency-range drift before dedicated-server startup.

## 0.1.1 Wrapper follow-up

- Consider adding a Gradle distribution checksum once the project standardizes
  its CI and release environment.

## 0.1.2 Package follow-up

- Add specialized registries only when the first real content type requires
  them, keeping the registry surface proportional to shipped content.
- Add a small architecture validation check when generation packages gain
  executable code, especially for client/server dependency boundaries.

## 0.2.0 Generation follow-up

- Replace the phase-one flat platform with a bounded, seed-derived generation
  cell system before adding rooms or corridors.
- Add an automated game test for dimension discovery, spawn height, and
  save/reload once executable generation code exists.

## 0.2.1 Generation follow-up

- Add Phase 3 structure-piece metadata and bounded overlap validation on top of
  the completed cell ownership and seed contracts.
- Replace the flat test platform with the first chunk-owned generated piece
  once room and corridor content is ready.

## 0.2.2 Architecture documentation follow-up

- Keep `architecture.md` synchronized with executable generation contracts as
  cells, pieces, connectors, and data-driven content are implemented.
- Add schema-level validation for the documented room and modular-piece data
  model before content is published to generation registries.

## 0.2.3 Modular structure follow-up

- Add a small registry/pool for `StructurePiece` definitions when the first
  generated corridor and room variants are introduced.
- Add an isolated template/block-population test once Phase 4 can materialize
  a placed piece in a test world.
