## 0.10.13 - Creative Darkness Inspection Toggle

### Task
Add a config toggle so creative players can temporarily see distant Labrinth
generation while visually testing the dense layout.

### Changes
- Added the common `darkness_mode` config option, enabled by default.
- Added a dedicated-server-safe creative inspection effect that refreshes
  hidden Night Vision only when darkness mode is disabled.
- Added client-only fog handling that replaces the black inspection fog and
  restores the configured render-distance fog plane for creative inspection.
- Incremented the mod version from `0.10.12` to `0.10.13`.

### Implementation
The dimension JSON remains permanently midnight and no-sky. The common player
tick handler checks the config, dimension, and creative mode before refreshing
the short hidden effect. Client viewport events are isolated in the client
package and only change fog for a creative camera inside the Labrinth while the
toggle is disabled.

### Rationale
Dimension type JSON is loaded as world data and cannot safely swap its visual
effects from a runtime common config. A bounded creative-only aid keeps the
survival atmosphere unchanged while making high-speed generation inspection
practical on both integrated and dedicated servers.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- Dedicated-server startup and manual creative flyover remain the final smoke
  checks for config loading, visibility, and distant geometry.
- `git diff --check` passed.

## 0.10.12 - Dense Floor-Varied Labyrinth

### Task
Increase hallway and room density, add more interconnection, prevent floors
from repeating the same layout, and keep the dimension permanently dark.

### Changes
- Increased optional connection edges to 78 percent so cells form a tighter
  network with more loops and branches.
- Increased room selection to 36 percent and made landmark openings use the
  floor-specific edge graph.
- Added floor-derived edge seeds and floor-varied parent routes so the three
  generated levels do not share one mirrored backbone.
- Set the dimension to midnight, zero ambient light, a black-fog biome, and the
  vanilla no-sky effects profile.
- Incremented the mod version from `0.10.11` to `0.10.12`.

### Implementation
The existing 64-block cell ownership and bounded chunk rematerialization stay
unchanged. Optional edges now use a percentage threshold, while the mandatory
route toward the origin chooses its axis per cell and floor. All horizontal
content and landmark connection checks pass the floor index into that graph.

### Rationale
Increasing the shared edge graph adds loops and junctions without duplicating
pieces or introducing cross-chunk searches. Seeding edges by floor changes the
actual connectivity pattern on each level while preserving deterministic
reloads. The dimension uses the vanilla no-sky rendering profile with a
black-fog biome, avoiding client-only rendering code.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `gradlew.bat runServer --console=plain --no-daemon` reached `Done` with
  The Labrinth `0.10.12` loaded successfully.
- `git diff --check` passed.
- Manual fresh-world visual QA remains required for density, floor silhouette
  variance, midnight lighting, and sky rendering.

## 0.10.11 - Wall-Hugging Stair Corrections

### Task
Correct the east/west incline and decline stair height and keep vertical
stairwell stairs against the inner walls with safe floor landings.

### Changes
- Moved hallway transition stairs onto the higher floor row for every rotation.
- Replaced the centered vertical route with a repeated inner-wall perimeter.
- Moved the upper landing to the wall-side route endpoint.
- Strengthened deterministic stair-path validation for inner-wall placement and
  stacked floor-boundary adjacency.
- Incremented the mod version from `0.10.10` to `0.10.11`.

### Implementation
Hallway geometry now recognizes a transition from either adjacent row and
materializes the bottom-half stair on the row with the greater floor height.
Its facing is derived from the lower neighboring side and then rotated with
the placed piece. The seven-by-seven vertical path uses only inner-perimeter
coordinates, repeats the same sixteen-step loop twice, and ends beside the
wall-side upper landing so stacked pieces continue in the same direction.

### Rationale
Placing a transition stair on the preceding lower row leaves the opposite
orientation one block low. The old vertical path passed through the stairwell
center, making turns and floor exits unsafe. Both fixes live in the shared
deterministic materializers without adding neighbor scans or changing piece
ownership.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `git diff --check` passed.
- Manual fresh-world visual traversal remains required for rotated ramps,
  stacked stairwell pieces, and collision/landing QA.

## 0.10.10 - Consistent Stair Geometry

### Task
Correct incline/decline stair placement, keep stacked stairwell pieces
continuous, and attach ladder shafts to their enclosing walls.

### Changes
- Materialized full-width stair rows at hallway height transitions and aligned
  their facing with the rising side of each transition.
- Replaced the stairwell route with a repeated inner loop whose floor-boundary
  endpoints are adjacent and added a dedicated upper landing.
- Rotated ladder states toward the north wall so every ladder has a solid wall
  behind it instead of facing into the shaft.
- Incremented the mod version from `0.10.9` to `0.10.10`.

### Implementation
Hallway ramps retain their deterministic level profile, but every walkable
cell in a transition row now receives the same vanilla stair state. Vertical
stair pieces share a 16-step inner loop repeated for the full 32-block floor
spacing; the final step is adjacent to the next piece's canonical first step.
An isolated stair piece keeps one upper landing block, while a stacked piece
overwrites that block with its next stair. Ladder blocks remain one cell inside
the north wall and use the south-facing attachment state.

### Rationale
The previous hallway materializer replaced only the center cell at each height
change, leaving broad solid risers and visible floor gaps. The previous
stairwell path did not meet its own next-floor entry and the ladder state faced
away from its support wall. These changes fix the shared materialization rules
without adding neighbor scans or changing deterministic cell ownership.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `git diff --check` passed.
- Manual fresh-world visual traversal remains required for ramp silhouettes,
  stacked floor transitions, and ladder attachment after chunk reload.

## 0.10.9 - Inner-wall Stairwell Path

### Task
Keep stairwell stairs out of the shell wall and make the complete 32-step
stairwell route continuously traversable.

### Changes
- Replaced the outer-boundary second half of the extended stair path with a
  repeated inner-perimeter route.
- Kept only the lower entry stair on the shell boundary; all remaining stairs
  occupy inner-track cells, leaving the surrounding wall intact.
- Added deterministic path validation for bounds, adjacency, and wall-cell
  exclusion.
- Incremented the mod version from `0.10.8` to `0.10.9`.

### Implementation
The seven-by-seven stairwell now uses a 32-position path whose first position
is the lower landing and whose subsequent positions remain inside coordinates
1 through 5. The renderer still derives each stair's facing from its next
adjacent path position, while the path validator prevents future edits from
placing a step in the shell or skipping a block.

### Rationale
The previous extension used the outer x/z boundary to gain the additional
vertical distance. Those coordinates are owned by the stairwell wall, so the
stairs visually and physically intersected the shell. Reusing the inner
perimeter preserves the compact footprint and gives the full spacing without
introducing a new stairwell type or boundary exception.

### Validation
- `gradlew.bat build` passed.
- The generation self-check passed with the deterministic path validation.
- `git diff --check` passed.
- Manual fresh-world traversal remains required for collision and visual QA.

## 0.10.8 - Connected Hallways and Expanded Floor Spacing

### Task
Remove the unwanted stairwell variants, repair grand-to-standard and
grand-to-room connections, constrain incline/decline stairs to a single row,
and give rooms more vertical space between Labrinth floors.

### Changes
- Removed the four experimental stairwell footprint/type variants and restored
  the original five vertical definitions.
- Kept every horizontal connector on the shared five-by-four doorway profile,
  including grand hallways and variable-height rooms; grand shells now cap the
  wall above that aperture instead of exposing a tall gap.
- Made all incline/decline and staircase hallway variants use one centered stair
  column, with rotation-aware facing and level endpoint landings.
- Increased floor spacing from 16 to 32 blocks and expanded the dimension lower
  bound to Y -32 while retaining the Y 256 exclusive top bound.
- Extended the retained seven-by-seven vertical stair path to all 32 blocks
  between floors and corrected its bottom-stair facing.
- Incremented the mod version from `0.10.7` to `0.10.8`.

### Implementation
Hallway materialization derives its doorway opening from the connector floor
and applies the four-block aperture only at an actually open boundary. Ramp
profiles use a signed delta for stair orientation; declines are translated
down within their piece bounds so both endpoint connectors remain at the
neighboring floor Y. Room shells continue to use their own width and height,
while the floor catalog and dimension metadata now share the 32-block spacing.

### Rationale
The connection contract must be identical at both sides of a boundary even
when one shell is grand or one ramp dips internally. Keeping the existing
cell-owned deterministic placement and changing only the shared aperture,
stair-column, and vertical-boundary calculations fixes the visible seams
without neighbor scans or a second generation graph.

### Validation
- `gradlew.bat generationSelfCheck` passed.
- `gradlew.bat compileJava` passed.
- The self-check verifies the retained five-definition vertical catalog, 32-block
  floor coordinates, shared ramp and grand connector profiles, expanded
  corridor coverage, and variable room bounds.
- `git diff --check` passed.
- Manual fresh-world visual smoke remains required for grand/standard seams,
  ramp silhouettes, and traversal across the expanded floor spacing.

## 0.10.7 - Expanded Hallway, Room, and Stairwell Variants

### Task
Add many curved, corner, incline, decline, staircase, grand-corridor,
stairwell, and variable-size room variants that remain procedurally connected.

### Changes
- Added nineteen weighted hallway and grand-corridor definitions, including
  curved turns, S-curves, U-turns, ramps, integrated staircase runs, and
  grand-width junctions.
- Added eight room definitions with width variants from 32 to 64 blocks and
  heights from 6 to 14 blocks; room shells now render from their own bounds.
- Added four deterministic stairwell variants covering narrow, standard, wide,
  and grand footprints with straight, L-shaped, switchback, and spiral paths.
- Expanded region pools and depth weighting so the new content can generate in
  themed areas without weakening the shared doorway profile.
- Incremented the mod version from `0.10.6` to `0.10.7`.

### Implementation
The existing cell-owned selection path now enumerates every corridor kind from
the enum and dispatches expanded shapes through a shape-aware materializer.
Curved and routed pieces retain the five-by-four horizontal aperture; ramp
pieces use level landings at both cell edges so their internal stair geometry
does not create mismatched neighboring Y coordinates. Room origins center the
variable footprint while keeping one full-length axis for every retained
boundary connector. Vertical selections use deterministic width/type metadata
and generate bounded paths from the lower floor to the upper opening.

### Rationale
Keeping one shared connector contract and the existing deterministic cell
ownership rule allows variety to grow without adding neighbor scans or a second
generation graph. Variable rooms and stairwells are parameterized at the shell
renderer instead of duplicating a fixed 64-block implementation.

### Validation
- `gradlew.bat generationSelfCheck` passed.
- `gradlew.bat compileJava` passed.
- The self-check now verifies expanded corridor coverage, rotated sized-room
  boundary alignment, room width/height diversity, and stairwell width/type
  diversity.
- `git diff --check` passed.
- Manual fresh-world visual smoke remains required for curved/ramp silhouettes,
  grand rooms, and traversal of each stairwell path.

## 0.10.6 - Traversable Perimeter Stairwells

### Task
Remove unexplained wheat-bottom and chain-filled vertical placeholders,
start stairways at the stairwell perimeter, extend them through the complete
floor boundary, and open stairwell walls where the same cell's room or
corridor provides a real approach.

### Changes
- Disabled the unfinished drop-shaft and elevator-placeholder variants from
  live random vertical selection while retaining their definitions for later
  authored implementations.
- Replaced the center-start stair path with a perimeter-following path whose
  first step is on the outer stairwell edge and whose final step reaches the
  upper boundary.
- Passed the deterministic same-cell horizontal placement into vertical
  materialization and removed only wall blocks adjacent to walkable horizontal
  air at the lower-floor doorway height.
- Added self-check coverage proving live selection excludes unfinished
  placeholder shafts.
- Incremented the mod version from `0.10.5` to `0.10.6`.

### Implementation
Vertical selection now exposes only `STAIR_UP`, `STAIR_DOWN`, and
`LADDER_SHAFT` in generated worlds; drop and elevator pieces remain in the
catalog but are not placed until their landing and passage contracts are
implemented. Stair positions are deterministic and contiguous from local
`(3, 0)` through the perimeter path to the upper endpoint. During chunk
materialization, the generator reuses the cell-owned horizontal placement and
probes its authored block function at each side of the seven-block shaft. A
wall block becomes air only when the adjacent horizontal position is air and
has a generated floor, so unrelated shell walls remain intact.

### Rationale
The wheat and chain shafts were placeholder visuals, not complete player
connections, so allowing them into live generation created unexplained holes
and sealed vertical features. A perimeter-first stair path gives the player a
reachable lower entry and a continuous rise. Reusing the horizontal cell
decision keeps wall openings deterministic, chunk-safe, and aligned with the
architecture's no-world-scan generation rule.

### Validation
- `gradlew.bat build --console=plain --no-daemon` passed.
- Generation architecture self-check passed as part of the build.
- Test task passed as part of the build.
- `git diff --check` passed before handoff.
- No files under `References/` or `TheLabrinth/` were modified.
- Manual fresh-world visual smoke remains required for perimeter stair entry,
  upper-floor traversal, and room/corridor-to-stairwell passages.

## 0.10.5 - Canonical Connection Apertures and Open Stairwells

### Task
Repair the remaining room and hallway offsets and gaps, replace non-solid
lantern lighting that punched through shells, correct backwards stairs, and
reopen stairwell tops while preserving the architecture-defined cell grid.

### Changes
- Normalized placed horizontal connector endpoints to the owning 64-block
  cell center and rejected off-center boundary endpoints.
- Derived rotated room and corridor aperture/path centers from the canonical
  world center instead of hardcoded local coordinates.
- Replaced generated lantern-family light states with solid glowstone blocks.
- Oriented switchback stairs toward their ascending path and left the full
  seven-by-seven upper stairwell footprint open.
- Added all-rotation regression coverage for odd-width straight-corridor
  boundary endpoints.
- Incremented the mod version from `0.10.4` to `0.10.5`.

### Implementation
`PlacedStructurePiece` corrects the cross-axis coordinate after rotation while
leaving non-canonical spawn-margin pieces untouched. Shared connection rules
now require the exact cell boundary and integer cell-center coordinate. Room
and routed-corridor renderers inverse-map that same canonical center before
materializing their apertures and path centerlines. Region palette application
converts sea lanterns, lanterns, soul lanterns, end rods, and existing light
blocks to glowstone so lighting cannot remove a full structural block.
Vertical stair blocks use the next switchback direction as their vanilla stair
facing, and the upper boundary pass writes air across the complete shaft
footprint so the opening reaches the floor module above.

### Rationale
The previous pass aligned connector metadata but could still render the
opening one block away after an even-sized piece rotated. Keeping endpoint
validation and block geometry on one canonical coordinate removes that split
contract without neighbor lookups or chunk-order state. Solid light blocks
preserve the shell, and an open upper footprint gives the traversable stair a
real connection into the next room or corridor.

### Validation
- `gradlew.bat build --console=plain --no-daemon` passed.
- Generation architecture self-check passed as part of the build.
- Test task passed as part of the build.
- `git diff --check` passed before handoff.
- No files under `References/` or `TheLabrinth/` were modified.
- Manual fresh-world visual smoke remains required for doorway seams, stair
  traversal, open stairwell transitions, and ceiling/floor lighting.

## 0.10.4 - Connector and Vertical Geometry Refinement

### Task
Resolve the remaining in-game generation defects: lateral and vertical
connector drift, hallway-to-room gaps, unfinished stair tops, incorrect stair
facing, and ceiling holes caused by light failures.

### Changes
- Restricted room wall openings to the declared four-block connector height.
- Kept failed ceiling lights as regional ceiling blocks instead of air.
- Centered vertical pieces on the same integer cell coordinate as horizontal
  room and corridor connectors.
- Reworked the switchback stair path so both stair selections use the same
  ascending geometry and vanilla-compatible facing.
- Added a centered three-by-three upper stair opening and restored the upper
  floor around it with the active region palette.
- Extended the framework self-check with room aperture and vertical-center
  assertions.
- Incremented the mod version from `0.10.3` to `0.10.4`.

### Implementation
Horizontal room openings now evaluate the local Y coordinate against the
connector profile before removing boundary blocks, so their floor, ceiling,
and doorway height agree with corridor shells. Vertical stair placement uses a
single coordinate-derived path, derives each stair's facing from the next rise,
and treats the upper boundary as a small opening surrounded by generated floor.
Palette lighting failures preserve the ceiling at the piece's top layer.

### Rationale
The prior geometry contracts agreed on connector endpoints but not always on
the blocks materialized around those endpoints. Aligning the aperture height,
integer center, and upper landing removes the visible seams without adding
neighbor-chunk lookups or order-dependent generation. Preserving a solid
ceiling during a lighting outage prevents visual decoration state from
destroying structural geometry.

### Validation
- `gradlew.bat build --console=plain --no-daemon` passed.
- Generation architecture self-check passed as part of the build.
- `git diff --check` passed before handoff.
- No files under `References/` or `TheLabrinth/` were modified.
- Manual fresh-world visual smoke remains required for stair traversal,
  doorway seams, and ceiling-light behavior.

## 0.10.3 - Connected Layout and Traversable Dressing

### Task
Repair the in-game generation defects reported from the first exploration
pass: disconnected corridors, misplaced decoration, and unusable stacked
stairways, while preserving the deterministic architecture in
`ARCHITECTURE.md`.

### Changes
- Added a deterministic parent-edge backbone to the cell connection graph,
  with seed-derived optional edges retained for loops.
- Made room and corridor selection honor every required shared edge and fixed
  rotated connector centers on the 64-block cell center.
- Aligned corridor centerlines, room openings, landmark openings, and landmark
  connectors to the same boundary coordinate.
- Restricted generic decoration to sparse, low-profile dressing over generated
  floor cells instead of empty corridor shell space.
- Replaced vertical center-column stairs and landmark center-column stairs with
  bounded switchback paths.
- Added self-check coverage for the connection graph, exact placement edges,
  and the updated corridor footprint alignment.

### Implementation
Connection ownership remains coordinate-derived and bounded to the current
cell and direct neighbors. Required edges first select a compatible corridor
pose, then validate transformed connector profiles and shared boundary
positions before exposing the doorway. Landmark shells use the same edge graph
and remain sector-owned. Decoration is applied only after the authored piece
has confirmed a floor at the candidate position.

### Rationale
Independent edge coin flips and independently rotated even-sized pieces made
valid-looking layouts become disconnected at runtime. The shared parent edge
ensures reachability, while the common integer center removes the one-block
rotation drift that prevented connector matching. Sparse floor-aware dressing
keeps visual themes without turning unused bounds into terrain.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.
- Manual fresh-world in-game smoke remains the final visual handoff check.

## 0.10.2 - README Visual Documentation

### Task
Insert the available pictures into the README where they best explain the
project, and remove decorative emoji from the README.

### Changes
- Added relative Markdown image embeds for every picture in `Pictures/`.
- Placed visual references beside the introduction, generation, room, region,
  exploration, landmark, loot, navigation, architecture, and validation
  sections.
- Removed emoji from README headings and normalized image paths to forward
  slashes for portable Markdown rendering.
- Incremented the mod version from `0.10.1` to `0.10.2`.

### Implementation
Each image is embedded once using a descriptive alt label and a relative path
under `Pictures/`. The existing README prose and section order remain intact;
only visual placement, heading labels, required project metadata, and task
documentation were updated.

### Rationale
Contextual image placement makes the diagrams useful while reading the related
concept instead of separating them into an unscoped gallery. Removing emoji
keeps the README's visual language consistent with its technical documentation.

### Validation
- Confirmed all files in `Pictures/` have exactly one README image embed.
- Confirmed README headings contain no decorative emoji.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.

## 0.10.1 - Phase 9/10 Selector Enforcement

### Task
Continue Phase 9 and Phase 10 until depth and region restrictions are enforced
by every shared room and corridor selection path.

### Changes
- Enforced declared minimum and maximum depth on corridor definitions.
- Added explicit depth validation to room selection.
- Rejected explicit room/corridor region IDs that are not eligible for the
  requested depth and floor.
- Incremented the mod version from `0.10.0` to `0.10.1`.

### Implementation
The shared selectors validate the same depth/floor/region contract used by the
normal chunk generator before creating a random stream. Candidate filtering
then applies each corridor piece's own depth range, preventing direct callers
from bypassing the progression gates.

### Rationale
This closes the remaining Phase 9 contract gap without adding a second
generation path or changing the sector-owned Phase 10 landmark decisions.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.10.1`, logged 10 room, 7 region, and 8 landmark definitions, and reached
  `Done` on the dedicated server. The exact development server process was
  stopped and its generated `run/` directory was removed.
- `git diff --check` passed; no files under `References/` or `TheLabrinth/`
  were modified.

## 0.10.0 - Phases 9 and 10 Depth and Landmarks

### Task
Complete Phases 9 and 10 while preserving the deterministic, chunk-safe
generation architecture described by `architecture.md`.

### Changes
- Added a bounded logical depth profile derived from distance, branch
  variation, region contribution, landmark progression, and floor offset.
- Threaded depth through generation context, room/corridor selection, region
  resolution, and content-placement progression metadata.
- Added depth-aware rare-room gating, corridor weighting, region eligibility,
  and loot/entity/hazard/ambient/unusualness modifiers.
- Added eight landmark definitions: Grand Hall, Central Stairwell, Massive
  Storage Complex, Generator Room, Flooded Atrium, Abandoned Station, Ancient
  Chamber, and Corrupted Nexus.
- Added canonical sector-origin selection, spacing/frequency limits, region,
  depth, floor, and connection requirements, multi-chunk procedural
  materialization, and landmark precedence in chunk generation.
- Expanded the architecture self-check and completed the Phase 9/10 checklist.

### Implementation
Depth is computed from the owning 64-block cell and active floor using only
seed-derived or coordinate-derived inputs, then resolved before room, corridor,
or region selection. Landmark candidates are considered only at deterministic
32-cell sector origins. One origin cell selects at most one candidate; any
intersecting chunk rematerializes that same piece from its half-open bounds.
Ordinary content and vertical pieces are filtered against those bounds before
the landmark is written, so neighboring chunks never make duplicate decisions
and cannot overwrite the landmark shell.

### Rationale
The implementation follows `architecture.md`'s minimum-corner ownership,
finite neighbor evaluation, half-open bounds, deterministic seed derivation,
region-before-piece selection, and no-recursive-generation requirements. The
logical depth model changes progression without confusing physical Y with
exploration distance, while sector ownership makes large landmarks safe across
chunk load order and reloads.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed,
  including depth repeatability, progression modifiers, landmark metadata,
  spacing, multi-chunk bounds, owner-cell selection, and reload/order checks.
- `gradlew.bat compileJava compileTestJava --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.10.0`, logged 10 room, 7 region, and 8 landmark definitions, and reached
  `Done` on the dedicated server. The exact development server process was
  stopped and its generated `run/` directory was removed.
- `git diff --check` passed; no files under `References/` or `TheLabrinth/`
  were modified.

## 0.6.0 - Phase 8 Region System

### Task
Complete Phase 8 — Region System while preserving the deterministic,
chunk-safe contracts in `architecture.md`.

### Changes
- Added immutable region definitions for Standard, Abandoned, Industrial,
  Flooded, Overgrown, Ancient, and rare Corrupted regions.
- Added region IDs, weights, room/corridor pools, lazy block palettes,
  lighting outages, bounded decoration rules, mob/loot/ambient metadata, and
  depth/elevation conditions.
- Added a seed-derived coarse region field with a standard origin core,
  contiguous macro areas, bounded transition bands, rare-region weights, and
  floor/depth eligibility filtering.
- Threaded the selected region through horizontal and vertical materializers so
  palettes, lighting, decorations, and content pools are applied per owner cell
  without changing chunk ownership or floor geometry.
- Expanded the architecture self-check, completed the Phase 8 checklist, and
  incremented the mod version from `0.5.0` to `0.6.0`.

### Implementation
Region selection derives a macro-cell choice from the world seed, cell,
logical depth, and floor. Eight-cell macro areas remain stable in their
interiors; only a two-cell boundary band consults the neighboring macro field
for a gradual transition. The origin cell is pinned to the standard core so
spawn setup stays compatible with the existing corridor anchor.

Each content owner resolves its own region before room or corridor selection.
Neighbor checks resolve the neighboring owner region independently, preserving
connector symmetry without loading or generating neighboring chunks. Horizontal
decoration is limited to interior air candidates and only runs for regions with
enabled rules. Palette block IDs remain lazy until runtime materialization, so
catalog and self-check code does not require a bootstrapped game registry.

### Rationale
The implementation follows `architecture.md`'s 64-block cell ownership,
half-open bounds, deterministic seed inputs, finite neighbor evaluation,
region-before-piece selection, and no-recursive-generation requirements. A
coarse cell field prevents region changes every chunk while keeping region
selection coordinate-derived and safe across reload and chunk-generation order.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed,
  including region metadata, deterministic selection, contiguous macro areas,
  restricted regions, region pools, and owner-region propagation.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.6.0`, logged 10 room definitions and 7 region definitions, and reached
  `Done` on the dedicated server; the exact development server process was
  stopped and its generated `run/` directory was removed.
- `git diff --check` passed, and no files under `References/` or `TheLabrinth/`
  were modified.

## 0.5.0 - Continuous and Vertical Generation

### Task
Complete Phases 6 and 7 — continuous Labrinth expansion and vertical
generation — while preserving the deterministic, chunk-safe contracts in
`architecture.md`.

### Changes
- Extended the mixed content generator across every chunk and every active
  floor without pre-generating the dimension or retaining mutable layout state.
- Added explicit far-distance continuity and dead-end checks, including the
  configured 20 percent corridor dead-end frequency and the reward-room loot
  path.
- Added a deterministic vertical catalog with stair-up, stair-down, ladder,
  drop, and elevator-placeholder pieces, aligned UP/DOWN/SHAFT connectors,
  safe shaft materialization, and explicit upper-floor air openings.
- Added floor-aware room and corridor selection, changed the dimension bounds
  to Y -16 through 255 with 16-block layer spacing, completed the Phase 6/7
  checklist, and incremented the mod version from `0.4.0` to `0.5.0`.

### Implementation
Horizontal pieces continue to be selected from the current cell and four
direct neighbors only. Each floor reuses that same bounded lookup with a
floor-derived random stream, so approaching a cell from different chunks or
directions cannot change its room, corridor, rotation, or connector state.
Vertical pieces are owned by one cell on one adjacent-floor boundary; only two
boundaries are enabled (`-1 -> 0` and `0 -> 1`), and the spawn cell receives
deterministic stair links to both sides. The chunk-local vertical renderer
writes its own footprint, including air at the upper endpoint, after
horizontal materialization so floor blocks cannot seal a valid link.

### Rationale
The implementation follows `architecture.md`'s 64-block cell ownership,
half-open bounds, deterministic seed derivation, finite neighbor evaluation,
and no-recursive-generation requirements. Keeping the lower floor inside a
Y -16 minimum preserves the existing top bound while giving the starting
floor one valid layer below and above it. Vertical branching is bounded by
one decision per cell and floor boundary, with no forced neighbor-chunk loads.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed,
  including far-cell expansion, symmetric openings, dead-end metadata, floor
  placement, vertical connector alignment, and height-bound checks.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.5.0` and reached `Done` with the revised dimension height and generator
  codec; the development server was then stopped by its exact process and its
  generated `run/` directory was removed.
- `git diff --check` passed, and no files under `References/` or `TheLabrinth/`
  were modified.

## 0.4.0 - Phase 5 Room Generation

### Task
Complete Phase 5 — Room Generation while preserving the deterministic,
chunk-safe contracts in `architecture.md`.

### Changes
- Added a registered ten-room catalog with IDs, weights, rarity, dimensions,
  rotations, connectors, region/depth gates, placement conditions, loot
  references, decoration rules, and spawn-marker metadata.
- Added bounded room shells with style-specific floors, lighting, props,
  block variation, visible containers, environmental details, and interactive
  markers.
- Added deterministic mixed room/corridor selection and shared connector
  compatibility rules, then integrated both content types into the live chunk
  generator.
- Expanded the architecture self-check, completed the Phase 5 checklist, and
  incremented the mod version from `0.3.0` to `0.4.0`.

### Implementation
Each non-origin 64-block cell uses an independent content seed to choose a
room or corridor. Room metadata is immutable and selected by weighted rarity;
the origin remains a full-cell corridor anchor. The mixed catalog evaluates
only the current cell and its four direct neighbors, opens only aligned,
compatible boundary connectors, and passes the finite result to a chunk-local
materializer. Room shells use full-cell bounds so their standard exits remain
grid-aligned; unmatched exits are capped. Spawn markers are deterministic
room-local metadata rendered as harmless soul-torch markers, while loot table
references remain metadata until the planned loot phase.

### Rationale
Keeping room definitions in a common-side catalog reuses the Phase 3
`StructurePiece` contract without introducing a speculative NeoForge registry.
The shared connection matcher prevents room/corridor seams from drifting apart,
and the finite neighbor lookup preserves `architecture.md`'s no-recursion,
no-neighbor-chunk-load, and chunk-order-independent generation requirements.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.4.0` and reached `Done` without dimension or generator codec startup
  errors; the development server was then stopped and its generated `run/`
  directory removed.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.

## 0.2.3 - Modular Structure System

### Task
Complete Phase 3 — Modular Structure System.

### Changes
- Added immutable reusable structure-piece definitions with template IDs,
  dimensions, weights, rarity, rotation/mirror rules, depth and region gates,
  connector metadata, placement conditions, loot, decorations, and explicit
  overlap permissions.
- Added transformed placed pieces with half-open multi-chunk bounds and
  world-space connector projections.
- Added connector types, directions, dimensions, profile rotation, required,
  open/capped/blocked states, compatibility rules, and deterministic endpoint
  alignment.
- Added bounded overlap and world-height validation, including vertical
  intersection checks and conservative collision-scan limits.
- Expanded the framework-free self-check to cover Phase 3 behavior and bumped
  the mod version from `0.2.2` to `0.2.3`.

### Implementation
`StructurePiece` stores reusable metadata while `PlacedStructurePiece` owns
origin, transform, bounds, and transformed connectors. Connector alignment
tries the finite rotation/mirror set in a stable order and derives the
candidate origin by matching connector endpoints. Placement validation uses
the existing half-open `GenerationGrid.Bounds` contract, allows only named
overlaps, rejects out-of-height pieces, and stops after a fixed collision-check
budget.

### Rationale
The phase needs a reusable contract before corridor and room content can be
generated. Keeping definitions immutable and placement state separate makes
the same content safe to reuse across cells, while explicit compatibility and
bounded validation prevent invalid connections, cross-piece clipping, and
unbounded generation searches.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.

## 0.3.0 - Phase 4 Corridor Generation

### Task
Finish Phase 4 — Corridor Generation, including all Phase 4.2 variants and
the Phase 4.3 selection rules, while following `architecture.md`.

### Changes
- Added distinct long, turn, T-junction, four-way, dead-end, wide, and narrow
  corridor definitions alongside the existing short and medium straights.
- Added shared chunk-local materializers for routed junctions and width-specific
  corridor shells, with transformed connector apertures and capped unmatched
  ends.
- Added a deterministic corridor catalog with weighted selection, configurable
  codec-backed weights, repetition avoidance, controlled dead-end chance, and a
  maximum branching limit.
- Integrated catalog placement into the Labrinth chunk generator without
  recursive generation or neighboring-chunk loads.
- Completed the Phase 4 checklist, expanded the architecture self-check, and
  incremented the mod version from `0.2.6` to `0.3.0`.

### Implementation
Each cell selects one immutable catalog definition from the world-seeded
positional random state. Routed turn and junction pieces use full-cell bounds
so their exits can reach cell boundaries; short and medium pieces remain
centered bounded variants whose internal ends are capped. Neighbor selections
are evaluated directly and only compatible opposite connectors whose transformed
world endpoints meet on the shared boundary become open. The finite result is
passed to a chunk-local renderer so every other connector is capped, and the
renderer scans only the target chunk intersection. Piece origins remain inside
their owning 64-block cell except for the bounded origin spawn margin, and
intersecting chunks rematerialize the same decision from the same seed. The
generator accepts an optional `corridor_config` codec field while retaining the
validated default configuration when it is omitted.

### Rationale
This preserves the architecture's cell ownership, half-open bounds,
deterministic seed, connector-compatibility, and no-neighbor-loading rules.
Keeping selection finite and separate from reusable piece definitions allows
Phase 5 rooms to reuse the same metadata and placement contracts without
introducing recursive corridor growth.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `gradlew.bat runServer --console=plain --no-daemon` reached `Done` with the
  `0.3.0` mod and no dimension or generator codec startup errors.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.

## 0.2.2 - Architecture Reference

### Task
Create `architecture.md` from the technical requirements in `README.md` and
the design images in `Pictures/`.

### Changes
- Added a consolidated architecture reference covering the current dimension
  contract and the planned modular generation system.
- Transcribed the visual design's piece types, connector rules, floor and
  corridor standards, room model, depth bands, regions, landmarks, hazards,
  navigation, content pipeline, and validation matrix.
- Documented the implemented Phase 2 contracts: 64-block cells, four-by-four
  chunk grouping, half-open bounds, minimum-corner chunk ownership, canonical
  neighbor edges, seed salts, and immutable generation context constraints.
- Incremented the mod version from `0.2.1` to `0.2.2`.

### Implementation
`architecture.md` separates the shipped Phase 1/Phase 2 foundation from
planned procedural rooms, corridors, and content systems. It records the
exact current dimension JSON values, the visual design document's eight pages,
the three schematic references, and the data model and testing contracts
needed for future content.

### Rationale
The README describes the product direction at a high level, while the images
contain the concrete construction, content, progression, and validation rules.
Keeping both in one versioned reference reduces ambiguity when executable
generation and data-driven content are added.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` completed successfully.
- `git diff --check` passed.
- Confirmed no files under `References/` or `TheLabrinth/` were modified.

## 0.2.1 - Deterministic Generation Architecture

### Task
Complete Phase 2 - Generation Architecture.

### Changes
- Added a 64-by-64-block generation-cell grid built on vanilla chunk
  boundaries.
- Added deterministic piece ownership, neighbor connection derivation, seed
  salts, selection helpers, generation constraints, and immutable context.
- Added a framework-free generation architecture self-check and wired it into
  the Gradle `check` task.
- Incremented the mod version from `0.2.0` to `0.2.1`.

### Implementation
Cells group four-by-four chunks. Bounds use half-open block coordinates, and
the chunk containing a piece's minimum horizontal corner owns its existence
decision. Other intersecting chunks can materialize the same deterministic
piece without making another decision. Neighbor edges use a canonical ordered
cell pair, while room, corridor, region, landmark, and context seeds use
independent salts and coordinates.

### Rationale
The architecture gives later structure generators stable coordinates and
bounded ownership rules without retaining mutable global generation state or
depending on chunk visitation order. The context carries the information
needed by future generators while keeping repeatable selection decisions tied
to explicit seed inputs.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- The self-check covered negative-coordinate floor division, cell boundaries,
  multi-chunk ownership, repeatable selections, neighbor symmetry,
  evaluation-order independence, and context random streams. Since rooms and
  corridors are not implemented until later phases, layout validation currently
  covers the deterministic architecture contract rather than placed structure
  content.

## 0.2.0 - Dimension Foundation

### Task
Complete Phase 1 — Dimension Foundation.

### Changes
- Added the data-driven `labrinth:labrinth` dimension and its custom dimension type.
- Added a deterministic flat test environment with a four-block spawn platform.
- Defined enclosed-dimension properties: no skylight, a ceiling, fixed time,
  no precipitation, no natural biome spawns, and standard coordinate scale.
- Incremented the mod version from `0.1.2` to `0.2.0`.

### Implementation
The dimension level stem is supplied by
`data/labrinth/dimension/labrinth.json`, while
`data/labrinth/dimension_type/labrinth.json` owns the dimension rules. The
flat generator uses `minecraft:the_void` with features and lakes disabled,
then places bedrock, stone, and polished deepslate from Y 0 through Y 3.

### Rationale
Native datapack registration is the smallest stable foundation for a custom
dimension and keeps world properties independent from the procedural generator
that will be introduced in Phase 2. The platform prevents the initial test
world from depending on void-spawn behavior or chunk-order-sensitive setup.

### Validation
- `gradlew.bat build --console=plain --no-daemon` completed successfully.
- A dedicated server loaded the mod, `/execute in labrinth:labrinth` returned a
  dimension time query, and the server saved `labrinth:labrinth` on shutdown.
- A forced test chunk generated the polished-deepslate layer; the force-load
  marker and layer survived a server restart and save/reload check.
- `runClient` reached the Minecraft title screen and loaded the render
  resources without mod errors.
- A singleplayer client world rendered the platform and successfully ran
  `/execute in labrinth:labrinth run tp @s 0 5 0`; the integrated server saved
  the Labrinth dimension when the test world closed.

## 0.1.2 - Base Package Organization

### Task
Complete Phase 0.2 — Base Package Organization without beginning Phase 1.

### Changes
- Added documented common/server package boundaries for registries, world
  systems, content, events, commands, configuration, debugging, and client code.
- Added common `ModBlocks` and `ModItems` deferred registries.
- Wired both registries to the existing common mod event bus.
- Incremented the mod version from `0.1.1` to `0.1.2`.

### Implementation
Package-level documentation makes the ownership boundaries visible without
adding speculative gameplay classes. Empty block and item registries provide
the smallest real registry seam and are registered from `TheLabrinth` without
importing client-only code. Generation subpackages are reserved for bounded,
seed-derived systems introduced by later tasks.

### Rationale
The project needs stable ownership boundaries before dimension and generation
features are added. Keeping the registries common-safe and the client package
isolated prevents later content work from coupling server startup to rendering.

### Validation
- `gradlew.bat build --console=plain --no-daemon` completed successfully.
- `gradlew.bat runServer` loaded `The Labrinth 0.1.2 (labrinth)` and reached
  `Done (6.138s)` on a dedicated server.
- No client-only imports were added to common code; the client package contains
  only package-boundary documentation.
- The generated `run/` directory was removed after the smoke test.

## 0.1.1 - Repository-local Gradle Wrapper

### Task
Add a repository-local Gradle wrapper so Labrinth builds do not depend on a
neighboring project, then finish validation for Phase 0.1.

### Changes
- Added the Gradle 8.10.2 wrapper scripts and bootstrap files.
- Incremented the mod version from `0.1.0` to `0.1.1`.
- Completed the 0.1 checklist in `TASK.md`.
- Preserved the wrapper JAR in Git despite the general JAR ignore rule.

### Implementation
The wrapper uses the existing Gradle 8.10.2 distribution configuration and
the Java 21 toolchain already required by NeoForge 1.21.1. No project code or
read-only reference source was changed.

### Rationale
A repository-local wrapper makes the build reproducible from a fresh checkout
and removes the previous dependency on another repository's wrapper.

### Validation
- `gradlew.bat --version` reported Gradle `8.10.2` and Java `21.0.11`.
- `gradlew.bat build --console=plain --no-daemon` completed successfully.
- Processed metadata reported `The Labrinth 0.1.1 (labrinth)` with Minecraft
  `1.21.1` and NeoForge `21.1.219` dependencies.
- `gradlew.bat runServer` reached `Done (4.558s)` and loaded the mod on a
  dedicated server. The non-interactive dev process was then stopped by its
  exact PID after startup; the intentional termination returned `-1`.

## 0.1.0 - Project Validation

### Task
Complete the first incomplete section under Phase 0 — Project Foundation:
validate and establish the minimum NeoForge 1.21.1 project foundation.

### Changes
- Added the NeoForge ModDev build configuration for Minecraft 1.21.1.
- Added the `labrinth` mod metadata with display name `The Labrinth`.
- Added the common-only `com.labrinthmc.labrinth.TheLabrinth` entry point.
- Added the initial resource pack metadata and ignored generated Gradle/server output.

### Implementation
The build uses NeoForge `21.1.219` and a Java 21 toolchain. Metadata is expanded
from Gradle properties during resource processing so the mod ID, display name,
version, and dependency ranges remain aligned with the build. The entry point
contains no client-only imports, allowing the same class to load on a dedicated
server.

### Rationale
The repository contained documentation and reference material but no editable
mod source or Gradle project. A single common entry point and the smallest
working build configuration establish the validation baseline without starting
the later package, registry, or generation phases.

### Validation
- Java `21.0.11` was detected.
- The processed metadata contained `labrinth`, `The Labrinth`, version `0.1.0`,
  Minecraft `1.21.1`, and NeoForge `21.1.219` dependency ranges.
- `ModernCompanions\gradlew.bat -p R:\Users\Zach\Documents\GitHub\LabrinthMC build --console=plain --no-daemon` completed successfully.
- The dedicated `runServer` task discovered The Labrinth, logged the common
  entry point, reached `Done (7.240s)`, and stopped cleanly.
- No template/example source existed to remove.
- A repository-local Gradle wrapper is still absent because the current
  directory policy does not permit adding wrapper files.
## 0.2.4 - First Straight Corridor

### Task
Implement the next roadmap step, Phase 4.1 Basic Corridor.

### Changes
- Added the reusable `StraightCorridor` piece with deterministic dimensions,
  complete floor, deepslate-brick walls, polished-deepslate ceiling, and
  periodic sea-lantern lighting.
- Added forward and rear standard connectors with rotation-aware placement.
- Added a common-safe `LabrinthChunkGenerator` and registered its codec so the
  Labrinth dimension materializes corridor blocks during chunk generation.
- Replaced the flat test level stem with the fixed void biome plus the
  corridor generator, preserving a walkable origin spawn.
- Incremented the mod version from `0.2.3` to `0.2.4`.

### Implementation
Each 64-block generation cell derives a straight corridor axis from the
world-seeded positional random state. The target chunk renders only the
intersecting portion of the immutable placed piece, with a bounded one-cell
lookaround for the origin spawn corridor's small negative margin. No neighbor
chunks are loaded or mutated. The corridor definition and transformed
connectors remain reusable for later alignment and connection-continuity
work.

### Rationale
The first materialized piece needs to validate the Phase 3 structure contract
against real chunk generation without introducing rooms, templates, custom
blocks, or recursive expansion prematurely. Vanilla block states provide a
stable visual shell and light source while the custom generator keeps the
dimension enclosed and dedicated-server safe.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed, including the
  self-check and test task.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.2.4` and reached `Done` without chunk-generator registry or dimension
  codec errors.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.

## 0.2.5 - Short Straight Corridor

### Task
Implement the next roadmap step, Phase 4.2 Short straight.

### Changes
- Added a distinct reusable `labrinth:corridor/short_straight` definition.
- Reused the deterministic straight-corridor shell for a centered 32-block
  half-cell footprint with forward and rear connectors.
- Extended the architecture self-check to cover short-piece metadata, bounds,
  connector depth, and rotation.
- Incremented the mod version from `0.2.4` to `0.2.5`.

### Implementation
The short definition remains a separate immutable `StructurePiece` with its own
ID, template ID, depth, and connector endpoints. The shared chunk-local
materializer accepts the standard and short definitions without loading or
mutating neighboring chunks. Non-origin short pieces are centered on their
owning 64-block cell; the origin cell retains its bounded spawn margin. Variant
selection remains deferred to Phase 4.3 so this phase adds content without
introducing an unplanned selection policy.

### Rationale
This follows `architecture.md` by keeping corridor variants as reusable,
metadata-bearing pieces with half-open bounds, deterministic cell placement,
and a single chunk-safe materialization path. Separating definition from
selection preserves the planned registry/pool boundary for the next phase.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.2.5` and reached `Done` without chunk-generator registry or dimension
  codec errors.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.

## 0.2.6 - Medium Straight Corridor

### Task
Implement the next roadmap step, Phase 4.2 Medium straight.

### Changes
- Added a distinct reusable `labrinth:corridor/medium_straight` definition.
- Reused the deterministic straight-corridor shell for a centered 48-block
  three-quarter-cell footprint with forward and rear connectors.
- Extended the architecture self-check to cover medium-piece metadata, bounds,
  connector depth, and rotation.
- Incremented the mod version from `0.2.5` to `0.2.6`.

### Implementation
The medium definition is a separate immutable `StructurePiece` with its own ID,
template ID, depth, and connector endpoints. The shared chunk-local
materializer now accepts the standard, short, and medium definitions without
loading or mutating neighboring chunks. Non-origin medium pieces are centered
on their owning 64-block cell; the origin cell retains its bounded spawn
margin. Variant selection remains deferred to Phase 4.3.

### Rationale
This follows `architecture.md` by keeping each corridor length as reusable
metadata-bearing content with half-open bounds, deterministic grid placement,
and one chunk-safe materialization path. Selection and registry work stay out
of this shape-definition step and remain available for the planned next phase.

### Validation
- `gradlew.bat generationSelfCheck --console=plain --no-daemon` passed.
- `gradlew.bat build --console=plain --no-daemon` passed.
- `gradlew.bat runServer --console=plain --no-daemon` loaded The Labrinth
  `0.2.6` and reached `Done` without chunk-generator registry or dimension
  codec errors.
- `git diff --check` passed.
- No files under `References/` or `TheLabrinth/` were modified.
