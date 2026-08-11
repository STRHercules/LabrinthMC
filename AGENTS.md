# AGENTS.md

This repository contains both editable mod code and read-only reference sources used while developing **The Labrinth**.

These rules exist to prevent accidental modification of upstream Minecraft/NeoForge code, maintain deterministic and stable world generation, and keep all development changes clean, documented, buildable, and reviewable.

This file defines the contribution rules and development boundaries for **The Labrinth**, a Minecraft `NeoForge` mod for `1.21.1`.

Follow these guidelines for every task performed in this repository.

---

## Golden Rules

* You **MUST** consult `TASK.md` before beginning work for a detailed outline of the overall goal.
  * If `TASK.md` is blank or contains no applicable instructions, proceed using the task/prompt provided.
* You **MUST** review the current Labrinth implementation before assuming a feature does not exist.
* You **MUST** confirm the project builds successfully **before committing any changes**.
* Edit only files permitted by the **Directory Policy**.
* Treat Minecraft, NeoForge, Gradle, reference implementations, generated examples, and other upstream sources as **READ-ONLY**.
* Never modify upstream/reference code simply to make The Labrinth compile.
* Never blindly copy reference code into The Labrinth.
  * First determine what problem the reference solves.
  * Adapt the technique to Labrinth's architecture.
  * Verify the API against the actual Labrinth target before implementation.
  * Review the reference project's license before incorporating source or assets.
* Leave clear and concise comments explaining important implementation decisions alongside newly written or significantly modified code.
* Increase the version number in `build.gradle` with **every completed change**.
* Preserve deterministic world generation wherever possible.
  * The same world seed and coordinates should produce the same Labrinth layout unless a deliberate generation change requires otherwise.
* Avoid generation logic that performs unnecessary work every tick or repeatedly scans large areas of the world.
* Never knowingly introduce chunk-loading loops, recursive chunk generation, uncontrolled structure recursion, or cross-chunk generation behavior that can deadlock or infinitely generate terrain.
* Prefer modular systems that allow additional rooms, corridors, regions, landmarks, compounds, structures, and generation rules to be added without rewriting the core generator.
* Reuse proven Minecraft/worldgen patterns where they fit Labrinth instead of reinventing equivalent infrastructure.
* Do not silently remove existing functionality to implement a new feature.
* Do not commit temporary debugging code, test commands, generated dumps, logs, or development-only artifacts unless explicitly required.

---

## Directory Policy

```text
/ (root)
├─ References/                                                  # ALL CONTENT READ-ONLY
│  ├─ adventuredungeons-neoforge-1.21-1.3.1.jar                 # ARR/look-only dungeon design reference
│  ├─ claustrophobic_dungeons-1.0.1-forge-1.20.1.jar            # ARR/look-only; older-version dungeon reference
│  ├─ compatstructures-1.0.3.jar                                # Structure-library/donor candidate; license-check before reuse
│  ├─ Dungeons And Villages DeeperAndDarker 1.21.1.jar          # Underground dungeon/template donor candidate
│  ├─ Simple Structures Caves 1.21.10-11.jar                    # Cave-structure donor/reference; newer-version data warning
│  ├─ mostructures-1.21.x                                       # Large structure library; LGPL reuse caution
│  ├─ larion-world-generation-main                              # Cave/noise/density worldgen reference
│  ├─ lithostitched-1.21.2                                      # Data-driven worldgen and structure utility reference
│  ├─ Minecraft_Client_Source_1.21.1                            # Vanilla Minecraft 1.21.1 source reference
│  ├─ More Villages v0.2.3-alpha                                # Village/template donor candidate
│  ├─ DungeonCrawl-neoforge-1.21                                # Roguelike dungeon reference; GPL reuse caution
│  ├─ NeoForge-1.21.11                                          # NeoForge/API reference; verify against configured target
│  ├─ RepurposedStructures-1.21.5-MDG                           # Structure templates, pools, processors, variant patterns
│  ├─ StructureTutorialMod-1.21.11-Neoforge-Jigsaw              # Jigsaw/template/pool authoring reference
│  ├─ Underground-Village-Multiloader-1.21.1-dev                # MIT underground-village/template donor candidate
│  ├─ YUNGs-Better-Caves-1.21.1                                 # Cave/carver architecture reference
│  ├─ YUNGs-Better-Dungeons-1.21.1                              # Dungeon assembly and bounded structure reference
│  ├─ YUNGs-Better-Mineshafts-1.21.1                            # Piece graph, branching, bounds, and corridor reference
│  └─ YUNGs-Better-Strongholds-1.21.1                           # Large connected compound/landmark reference
├─ TheLabrinth/                                                 # Catalogue of verified builds/releases (READ-ONLY)
└─ src/                                                         # The Labrinth Source Code (EDITABLE)
```

Everything under `References/**` is **READ-ONLY**, including `.jar` files, extracted source trees, resources, Gradle files, generated data, assets, and documentation.

Reference JARs may be **inspected or unpacked for research** into a temporary working location, but the JAR itself must never be modified. Files may only be incorporated into Labrinth-owned source/resources when the applicable license explicitly permits that reuse and all required notices/conditions are satisfied.

If additional reference directories or JARs are added later, treat them as **READ-ONLY by default** unless explicitly identified otherwise in this file.

---

# Reference Source Policy

The `References/` directory exists to accelerate Labrinth development by providing proven implementations and examples for Minecraft world generation, structures, caves, jigsaw systems, and large procedural compounds.

References are **research inputs**, not alternate source trees.

## Source-of-Truth Order

When sources disagree, use this precedence:

1. The current user task/prompt.
2. `TASK.md`.
3. The current Labrinth implementation in `src/**`.
4. Labrinth architecture/documentation such as `README.md`, `architecture.md`, and related project docs.
5. Minecraft `1.21.1` behavior and APIs.
6. The actual NeoForge/Gradle versions configured by The Labrinth.
7. Read-only external reference projects.

A reference project must never override a Labrinth architectural requirement simply because its implementation is easier to copy.

## Version Safety

The Labrinth targets **Minecraft `1.21.1`**.

References are not assumed API- or data-compatible merely because their names contain `1.21`.

### Closest Target-Version References

Prefer these first when implementation details depend heavily on Minecraft `1.21.1` behavior:

* `Minecraft_Client_Source_1.21.1`
* `Underground-Village-Multiloader-1.21.1-dev`
* `Dungeons And Villages DeeperAndDarker 1.21.1.jar`
* `YUNGs-Better-Caves-1.21.1`
* `YUNGs-Better-Dungeons-1.21.1`
* `YUNGs-Better-Mineshafts-1.21.1`
* `YUNGs-Better-Strongholds-1.21.1`

### Newer or Ambiguous-Version References

Treat these primarily as conceptual, structural, or asset references until compatibility is verified:

* `lithostitched-1.21.2`
* `RepurposedStructures-1.21.5-MDG`
* `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`
* `NeoForge-1.21.11`
* `Simple Structures Caves 1.21.10-11.jar`
* `mostructures-1.21.x`
* `compatstructures-1.0.3.jar`
* `More Villages v0.2.3-alpha`
* `DungeonCrawl-neoforge-1.21`
* `adventuredungeons-neoforge-1.21-1.3.1.jar`

### Older-Version Reference

* `claustrophobic_dungeons-1.0.1-forge-1.20.1.jar` is **not** an API/data-format authority for `1.21.1`. Use it only for high-level layout/design study, subject to its reuse restrictions.

Do not copy method calls, registrations, codecs, constructors, resource formats, event hooks, mappings, or NBT/data assumptions from a non-`1.21.1` reference without checking the `1.21.1` equivalent.

## Licensing and Attribution

Before directly copying, adapting, redistributing, or incorporating source code or assets from a reference project:

1. Locate and read that reference project's `LICENSE`, `COPYING`, or equivalent terms.
2. Determine whether the intended reuse is permitted.
3. Preserve required copyright/license notices.
4. Avoid incorporating assets or code when the license is unclear.
5. Prefer implementing the underlying technique independently when direct reuse would create unnecessary licensing obligations.

Studying an algorithm, architecture, public API usage pattern, data layout, or world-generation strategy is encouraged. Direct source incorporation requires an explicit license check.

Do not assume that because a repository is public or open-source, all of its code and assets may be copied without conditions.

## Reference Reuse Classification

Before using a reference, classify the intended use:

### A. Donor Candidate

May be considered for direct structure/data/code adaptation **only after confirming the exact bundled/repository license and preserving required notices**.

Current donor candidates include:

* `Underground-Village-Multiloader-1.21.1-dev` — preferred underground village/building donor candidate
* `More Villages v0.2.3-alpha` — village/template donor candidate
* `Dungeons And Villages DeeperAndDarker 1.21.1.jar` — underground dungeon/room donor candidate; replace non-vanilla dependencies
* `Simple Structures Caves 1.21.10-11.jar` — cave-room/ruin donor candidate; convert/verify newer-version data
* `compatstructures-1.0.3.jar` — donor candidate only after verifying the exact JAR license and dependencies
* `StructureTutorialMod-1.21.11-Neoforge-Jigsaw` — permissive implementation/tutorial donor candidate after version translation
* `larion-world-generation-main` — permissive algorithm/reference candidate subject to its license notice requirements
* `lithostitched-1.21.2` — permissive utility/reference candidate after version translation

### B. Copyleft / Conditional Reference

These may be studied freely, but direct incorporation can create license obligations that must be deliberately accepted and documented:

* `DungeonCrawl-neoforge-1.21` — GPL-family caution
* `RepurposedStructures-1.21.5-MDG` — LGPL-family caution
* `YUNGs-Better-Caves-1.21.1`
* `YUNGs-Better-Dungeons-1.21.1`
* `YUNGs-Better-Mineshafts-1.21.1`
* `YUNGs-Better-Strongholds-1.21.1`
* `mostructures-1.21.x`

Prefer adapting **architecture and algorithms** from these rather than copying substantial source/assets unless the project's licensing plan explicitly accepts the resulting obligations.

### C. Look-Only / Inspiration-Only

Do **not** extract, copy, adapt, or redistribute code, NBT structures, textures, data files, or other assets from these unless separate explicit permission is obtained:

* `adventuredungeons-neoforge-1.21-1.3.1.jar`
* `claustrophobic_dungeons-1.0.1-forge-1.20.1.jar`

They may be inspected only for high-level ideas such as room categories, progression, scale, pacing, visual composition, and encounter concepts.

### D. Platform / Upstream Reference

* `Minecraft_Client_Source_1.21.1`
* `NeoForge-1.21.11`

Use these to understand platform behavior and APIs. They are not generic donor libraries.

If the license packaged with a local reference conflicts with this classification, **the packaged license wins**. Stop and update `REFERENCE_GUIDE.md` before direct reuse.

---

# When To Consult References

Agents should use the reference folders deliberately rather than searching all references for every task.

For a detailed project-by-project map, consult `REFERENCE_GUIDE.md`.

## Vanilla Minecraft / NeoForge

Consult:

* `Minecraft_Client_Source_1.21.1`
* `NeoForge-1.21.11`

when the task involves:

* Minecraft structure APIs
* `StructureTemplate`
* `StructurePiece`
* `Structure`
* `JigsawPlacement`
* template pools
* processors
* codecs
* registries
* chunk generator hooks
* biome/worldgen registration
* block/entity placement during worldgen
* loot integration
* server/world lifecycle behavior
* serialization
* version-specific method signatures

Use Minecraft `1.21.1` behavior as the authoritative vanilla reference.

Use the NeoForge reference to understand loader hooks and implementation patterns, but verify every relevant API against the NeoForge version actually configured by this repository.

## Jigsaw, Templates, Pools, and Data-Driven Structure Authoring

Consult:

* `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`
* `RepurposedStructures-1.21.5-MDG`
* `lithostitched-1.21.2`
* `Minecraft_Client_Source_1.21.1`

when implementing or changing:

* NBT structure templates
* jigsaw-style piece authoring
* template pools
* fallback/cap pieces
* processor lists
* reusable building modules
* datapack-defined structure content
* structure palette variation
* adding new room/building definitions without new Java classes
* extensible room/compound registration systems

Do not replace Labrinth's deterministic ownership and reservation systems with vanilla jigsaw placement unless the task explicitly requires an architectural change.

The desired direction is usually:

> use templates/pools/processors as an authoring and composition layer, while Labrinth retains authority over deterministic selection, reservation, collision rules, connectors, and chunk ownership.

## Dungeons and Multi-Room Compounds

Consult:

* `YUNGs-Better-Dungeons-1.21.1`
* `YUNGs-Better-Mineshafts-1.21.1`
* `YUNGs-Better-Strongholds-1.21.1`
* `Dungeons And Villages DeeperAndDarker 1.21.1.jar`
* `DungeonCrawl-neoforge-1.21`
* `compatstructures-1.0.3.jar`
* `Minecraft_Client_Source_1.21.1`

For additional **look-only** room/layout inspiration, agents may inspect `adventuredungeons-neoforge-1.21-1.3.1.jar` and `claustrophobic_dungeons-1.0.1-forge-1.20.1.jar`, but must not extract or reuse their assets without permission.

when implementing or changing:

* compact dungeons
* multi-room dungeons
* dungeon entrances/exits
* prisons
* outposts
* fortresses
* catacombs
* temples
* multi-floor compounds
* large landmarks
* branching piece graphs
* bounded structure assembly
* structure collision validation
* precomputing complete compound footprints

Prefer assembling and validating the logical compound **before block placement**, then reserving its complete footprint so ordinary Labrinth rooms and corridors yield to it.

Do not allow ordinary Labrinth generation to independently punch accidental openings into compounds.

## Corridors, Branching Networks, and Piece Graphs

Consult:

* `YUNGs-Better-Mineshafts-1.21.1`
* `YUNGs-Better-Strongholds-1.21.1`
* `Minecraft_Client_Source_1.21.1`

when implementing or changing:

* branch depth
* piece chain limits
* turns
* intersections
* vertical transitions
* endpoint rooms
* side rooms
* weighted branching
* bounded recursive/iterative structure expansion
* collision-aware piece placement

Reference implementations may use recursive `addChildren`-style generation. Labrinth must still enforce its own hard limits, deterministic seed derivation, chunk ownership, and chunk-loading restrictions.

## Caves, Caverns, and Organic Rooms

Consult:

* `larion-world-generation-main`
* `YUNGs-Better-Caves-1.21.1`
* `Simple Structures Caves 1.21.10-11.jar`
* `compatstructures-1.0.3.jar`
* `Minecraft_Client_Source_1.21.1`

when implementing or changing:

* enormous cave rooms
* cavern-shaped compounds
* organic chambers
* noise fields
* density functions
* cave deformation
* domain warping
* cave layers
* carver-style algorithms
* irregular stone volumes
* natural openings inside reserved Labrinth space

Do **not** blindly attach an Overworld cave generator to the Labrinth dimension.

The preferred Labrinth pattern is:

1. deterministically select a cave/cavern compound;
2. reserve a bounded volume;
3. run cave/noise logic only inside that owned volume;
4. create explicit Labrinth connectors;
5. keep ordinary Labrinth generation outside the reservation.

## Villages and Enclosed Settlements

Consult:

* `Underground-Village-Multiloader-1.21.1-dev`
* `More Villages v0.2.3-alpha`
* `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`
* `RepurposedStructures-1.21.5-MDG`
* `YUNGs-Better-Strongholds-1.21.1`
* `YUNGs-Better-Mineshafts-1.21.1`
* `mostructures-1.21.x`
* `Minecraft_Client_Source_1.21.1`

when implementing or expanding:

* enclosed Labrinth villages
* houses
* profession buildings
* civic buildings
* shops
* village streets/passages
* village central hubs
* settlement variants
* village template pools
* villager spawn markers
* workstation/bed placement

Use templates and pools to reduce Java authoring work, but keep the village itself an origin-owned Labrinth compound with explicit external entrances.

## Structure Asset Donors and Migration

Consult the donor-candidate references when the task can be accelerated by reusing an existing room/building shape instead of authoring it from scratch.

Useful donor categories include:

* underground houses and profession buildings
* meeting halls and town centers
* streets, crossroads, turns, and terminators
* libraries, temples, workshops, farms, pens, and stables
* dungeon rooms, treasure rooms, traps, stairs, and intersections
* cave shelters, ruins, camps, small dungeons, and decorative underground structures
* outposts, markets, mines, catacombs, and utility structures

Before importing an asset:

1. verify the exact local reference license;
2. record the source project and original path;
3. verify or convert the NBT/data format for Minecraft `1.21.1`;
4. replace blocks/entities/loot from unavailable dependency mods;
5. remove or translate foreign jigsaw markers into Labrinth connector metadata;
6. rename the asset into the `labrinth` namespace;
7. validate bounds and rotations;
8. ensure the Labrinth compound/owner system, not the donor generator, controls placement; and
9. preserve required attribution/license notices.

Do not import a complete donor settlement/dungeon generator when individual templates can be integrated into Labrinth's existing systems.

## Structure Processors and Regional Variants

Consult:

* `RepurposedStructures-1.21.5-MDG`
* `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`
* `Minecraft_Client_Source_1.21.1`

when implementing:

* block replacement
* decay
* cracked/damaged variants
* moss/overgrowth
* flooded variants
* palette swaps
* randomized props
* structure-specific decoration passes
* shared structures that need different regional appearances

Prefer reusing one authored template with processors/palettes over maintaining many nearly identical copies.

## Data-Driven / Datapack Expansion

Consult:

* `lithostitched-1.21.2`
* `RepurposedStructures-1.21.5-MDG`
* `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`
* `Minecraft_Client_Source_1.21.1`

when implementing:

* JSON-defined room metadata
* datapack registration
* appendable pools
* structure configuration
* codecs
* data-driven generation conditions
* third-party room/structure expansion
* Labrinth APIs for registering new content

Use reference projects to understand patterns, but design the final data model around Labrinth's existing concepts such as:

* ID
* template
* weight
* rarity
* depth
* allowed regions
* floor/elevation restrictions
* connector profiles
* bounds
* rotation
* processors
* loot
* decorations
* population rules
* placement conditions

---

## Reference Research Workflow

When a task would benefit from a reference implementation:

1. Read the relevant Labrinth code first.
2. Identify the specific missing behavior or reusable subsystem.
3. Consult `REFERENCE_GUIDE.md` to choose the smallest relevant set of reference projects.
4. Inspect the reference implementation.
5. Write down the useful concept in Labrinth terms.
6. Check the reference version against Minecraft `1.21.1`.
7. Check the reference license before directly incorporating code or assets.
8. Implement the solution in Labrinth-owned editable files.
9. Preserve Labrinth's determinism, ownership, connector, reservation, and chunk-safety rules.
10. Build and test.
11. Record the reference-inspired architectural decision in comments or documentation when non-obvious.

Avoid "cargo-cult" implementation where large reference classes are copied without understanding which parts are actually required.

---

## Allowed Edits

The following locations may be modified:

* `src/**`
* `TASK.md`

### Root Documentation

* `README.md`
* `CONTRIBUTING.md`
* `.gitignore`
* `.editorconfig`
* `SUGGESTIONS.md`
* `TRACELOG.md`
* `REFERENCE_GUIDE.md`

### Build Configuration

* `gradle.properties`
* `build.gradle`
* `settings.gradle`

Additional files may only be modified when explicitly authorized by the task or by an update to this policy.

---

## Forbidden Edits

Do **NOT** modify:

* Anything inside `References/**`
* Anything inside a top-level directory designated as `READ-ONLY`
* Minecraft decompiled source
* NeoForge source
* ModDevGradle source
* Verified/reference builds
* Generated upstream source
* External dependency source code
* `AGENTS.md`

If an apparent solution requires modifying a forbidden file, stop and find a solution within The Labrinth's editable source instead.

The prohibition on editing `AGENTS.md` applies to normal development tasks. It may only be changed when the user explicitly requests a policy update.

---

# Development Standards

## World Generation

World generation is the core feature of The Labrinth and must be treated as performance-sensitive infrastructure.

When modifying generation code:

* Preserve deterministic generation from the Minecraft world seed.
* Prefer coordinate- and seed-based decisions over global mutable state.
* Avoid generation that depends on chunk visitation order whenever possible.
* Avoid loading neighboring chunks merely to decide what should generate.
* Never create uncontrolled recursive generation.
* Place hard limits on any recursive, iterative, or search-based generation algorithm.
* Validate room and corridor connections before placement.
* Prevent structures from accidentally overlapping in invalid ways.
* Ensure generation gracefully handles chunk boundaries.
* Avoid expensive full-volume scans unless absolutely necessary.
* Cache reusable calculations when appropriate.
* Do not perform world generation calculations every game tick.
* Never rely on client-only state for server-side generation decisions.

Generation changes should be tested across multiple seeds whenever practical.

---

## Modular Generation

The Labrinth should remain expandable.

Whenever practical, generation content should be separated into systems such as:

```text
Labrinth Generation
├── Dimension
├── Generator
├── Regions
├── Rooms
├── Corridors
├── Junctions
├── Vertical Connections
├── Landmarks
├── Compounds
├── Templates
├── Processors
├── Decorations
├── Loot
└── Generation Rules
```

Avoid giant classes containing unrelated generation behavior.

Individual room or structure definitions should contain their own configuration where appropriate rather than adding extensive special-case logic to the core generator.

Prefer registries, data-driven definitions, interfaces, configuration objects, templates, pools, processors, or reusable generation components where they improve maintainability.

---

## Room & Corridor Connections

All modular generation pieces should have clearly defined connection behavior.

Where applicable, generated pieces should define information such as:

* Available connection points
* Connection direction
* Connection type/profile
* Rotation compatibility
* Width and height
* Bounding area
* Minimum required clearance
* Allowed neighboring structures
* Region restrictions
* Depth restrictions
* Floor/elevation restrictions
* Generation weight
* Rarity
* Placement conditions

Connections should be validated before committing placement.

Never assume another room, corridor, template, compound, or child piece successfully generated without verifying the result.

---

## Compound Reservations

Large structures such as villages, dungeons, outposts, huge caves, fortresses, and landmarks should normally be treated as **origin-owned reserved compounds**.

A compound should, where practical:

1. choose one deterministic owner/origin;
2. assemble or determine its logical layout;
3. calculate its complete bounds;
4. validate the complete candidate;
5. reserve the footprint;
6. expose only intentional external connectors;
7. allow ordinary Labrinth content to yield to the reservation; and
8. materialize only the portion intersecting the current chunk.

Do not let neighboring chunks independently roll the same compound.

Do not allow ordinary rooms or corridors to create unintended entrances into reserved compounds.

---

## Chunk Safety

The Labrinth may generate extremely dense enclosed environments, making chunk-generation mistakes especially dangerous.

Code must avoid:

* Infinite chunk generation
* Circular chunk dependencies
* Forcing distant chunks during normal generation
* Generating structures repeatedly when chunks reload
* Duplicating generation because neighboring chunks attempt the same placement
* Performing expensive searches across large numbers of unloaded chunks
* Server-only generation depending on client state
* Race conditions caused by unsafe shared mutable generation state

Any system that places content across multiple chunks should have a clearly defined ownership or origin rule.

For example:

> A landmark should have one deterministic origin chunk responsible for deciding whether the landmark exists.

Neighboring chunks should not independently attempt to generate the same landmark.

---

## Performance

Performance is a first-class requirement.

The Labrinth may generate significantly more enclosed geometry than normal Minecraft terrain, so avoid unnecessary computational overhead.

Prefer:

* Deterministic math
* Cached reusable data
* Bounded searches
* Efficient collections
* Precomputed templates
* Structure/template reuse
* Processor/palette reuse
* Lazy initialization where appropriate
* Spatial indexes or bounded reservation lookups when justified

Avoid:

* Repeated filesystem access during gameplay
* Large allocations inside frequent loops
* Recalculating invariant generation data
* Per-tick scans of large areas
* Unbounded recursion
* Loading chunks solely for validation
* Excessive block-by-block updates where bulk/template placement is possible

Performance optimizations must not make generation nondeterministic or unsafe.

---

## Client / Server Separation

World generation and gameplay logic must function correctly on a dedicated server.

Do not reference client-only Minecraft classes from common/server code.

Client-specific functionality should remain isolated within appropriate client packages, event handlers, or initialization paths.

The dedicated server build must never depend on:

* Rendering classes
* Client Minecraft instances
* Client GUI classes
* Client-only keybinds
* Client-only configuration state

---

## Comments

Add useful comments for:

* Complex generation algorithms
* Non-obvious coordinate transformations
* Rotation logic
* Seed derivation
* Chunk ownership logic
* Compound reservation logic
* Template-to-connector transformations
* Performance optimizations
* Workarounds for Minecraft/NeoForge behavior
* Important architectural decisions
* Non-obvious adaptations inspired by a reference implementation

Avoid comments that merely restate obvious code.

Good:

```java
// Derive the room RNG from the world seed and origin chunk so generation
// remains stable regardless of the order in which neighboring chunks load.
```

Good:

```java
// The dungeon graph is assembled before placement so its entire footprint can
// be reserved before ordinary Labrinth rooms/corridors are materialized.
```

Bad:

```java
// Create random
RandomSource random = ...
```

---

# What To Do If The Build Fails

If the project does not build:

1. Identify the actual compile, configuration, dependency, or runtime setup failure.
2. Determine whether the failure originates from editable Labrinth code or a read-only dependency/reference source.
3. Fix failures inside permitted files whenever possible.
4. Do **NOT** modify read-only sources to force a successful build.
5. Do **NOT** downgrade Labrinth architecture merely to match a copied reference implementation.
6. If a reference API is incompatible with `1.21.1`, find the `1.21.1` equivalent rather than changing the target version.
7. If the required fix would violate the Directory Policy, document the issue and suggest a text-only solution instead.

Examples include:

* Correcting a dependency declaration
* Fixing an import
* Updating an API call to match Minecraft/NeoForge `1.21.1`
* Adjusting Gradle configuration
* Adding a required relative source/configuration path
* Correcting Labrinth-owned code
* Reimplementing a newer reference pattern against the target API

Do not alter NeoForge, Minecraft, or reference sources to work around an error in the mod.

---

# What To Do When The Build Succeeds

After a successful build:

1. Review all changed files.
2. Check for accidental edits outside the permitted directories.
3. Confirm no file under `References/**` changed.
4. Review the implementation for:
   * Correctness
   * Performance
   * Readability
   * Maintainability
   * Deterministic generation
   * Chunk safety
   * Compound ownership/reservation safety where applicable
   * Dedicated-server compatibility
   * Version compatibility
5. Identify reasonable refactors or optimizations.
6. Identify feature expansions directly related to the completed task.
7. Record those ideas as a new entry in `SUGGESTIONS.md`.

Do not implement unrelated suggestions during the same task unless requested.

---

# SUGGESTIONS.md

Every completed task should include a new entry in `SUGGESTIONS.md`.

Suggestions may include:

* Refactors
* Performance improvements
* Generation improvements
* Additional room variants
* New corridor behavior
* Additional configuration
* Better debugging tools
* Datapack integration
* Template/pool expansion
* Improved extensibility
* Related gameplay features

Suggestions should be directly related to the work completed.

Do not use `SUGGESTIONS.md` as permission to expand the current task scope.

---

# TRACELOG.md

Every commit **MUST** include a corresponding entry in `TRACELOG.md`.

Each entry should include:

### Task

The original prompt or a concise description of the requested task.

### Changes

What was modified.

### Implementation

The major steps taken to complete the task.

### Rationale

Why the chosen implementation was used.

### Validation

How the change was verified.

### References

When external reference projects materially informed the implementation, identify them and briefly state what concept was adapted.

Do not paste large portions of third-party source into `TRACELOG.md`.

Example:

```markdown
## 0.1.4 - Procedural Corridor Connections

### Task
Add deterministic corridor connection generation.

### Changes
- Added corridor connection definitions.
- Added directional connector validation.
- Added seed-derived connection selection.

### Implementation
Corridor exits are derived from the world seed, chunk position, and
local structure position before placement.

### Rationale
Using coordinate-derived generation prevents corridor layouts from
changing based on chunk load order.

### Validation
- `gradlew build` completed successfully.
- Tested generation using multiple world seeds.
- Confirmed identical layouts after reloading the same seed.

### References
- `YUNGs-Better-Mineshafts-1.21.1`: reviewed bounded piece-chain and
  bounding-box placement patterns; implementation was adapted to
  Labrinth ownership and seed rules.
```

---

# Versioning

The version defined in `build.gradle` must be incremented with every completed change.

Do not reuse an existing version number for a new commit.

Unless otherwise specified, increment the smallest appropriate version component.

Example:

```text
0.1.0 → 0.1.1
```

Larger feature milestones may justify:

```text
0.1.9 → 0.2.0
```

Do not change the Minecraft or NeoForge target version unless explicitly instructed.

---

# Scope Control

Implement the requested task completely, but avoid unrelated changes.

Do not:

* Rewrite unrelated systems
* Reformat the entire project
* Rename unrelated classes
* Replace functioning implementations without justification
* Add unrelated dependencies
* Perform speculative architecture rewrites
* Remove features merely because they are currently unused
* Replace Labrinth systems wholesale with reference-project systems simply because they already exist

If a nearby improvement would be valuable but is not required for the task, add it to `SUGGESTIONS.md`.

---

# Commit Checklist

Before committing, confirm:

* [ ] `TASK.md` was reviewed
* [ ] Current Labrinth implementation was reviewed
* [ ] Requested task is complete
* [ ] Relevant references were consulted when doing so avoided reinvention
* [ ] Any directly reused third-party material was license-checked
* [ ] Reference API/version differences were accounted for
* [ ] Project builds with no errors
* [ ] Changes are limited to allowed directories
* [ ] No read-only/reference source was modified
* [ ] New or modified complex code contains useful comments
* [ ] World generation remains deterministic where applicable
* [ ] Generation logic contains no obvious unbounded recursion
* [ ] Generation does not unnecessarily force/load neighboring chunks
* [ ] Multi-chunk structures have deterministic ownership
* [ ] Reserved compounds expose only intentional external connectors
* [ ] Dedicated-server compatibility was considered
* [ ] Performance implications were reviewed
* [ ] `TRACELOG.md` contains a new entry with task, changes, implementation, rationale, validation, and references where applicable
* [ ] `SUGGESTIONS.md` contains a new relevant entry
* [ ] Documentation/configuration updated where relevant
* [ ] `build.gradle` version number incremented
* [ ] Final diff reviewed before commit
