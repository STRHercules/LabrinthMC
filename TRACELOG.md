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
