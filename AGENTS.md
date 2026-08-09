# AGENTS.md

This repository contains both editable mod code and read-only reference sources used while developing **The Labrinth**.

These rules exist to prevent accidental modification of upstream Minecraft/NeoForge code, maintain deterministic and stable world generation, and keep all development changes clean, documented, buildable, and reviewable.

This file defines the contribution rules and development boundaries for **The Labrinth**, a Minecraft `NeoForge` mod for `1.21.1`.

Follow these guidelines for every task performed in this repository.

---

## Golden Rules

* You **MUST** consult `TASK.md` before beginning work for a detailed outline of the overall goal.

  * If `TASK.md` is blank or contains no applicable instructions, proceed using the task/prompt provided.
* You **MUST** confirm the project builds successfully **before committing any changes**.
* Edit only files permitted by the **Directory Policy**.
* Treat Minecraft, NeoForge, Gradle, reference implementations, generated examples, and other upstream sources as **READ-ONLY**.
* Never modify upstream/reference code simply to make The Labrinth compile.
* Leave clear and concise comments explaining important implementation decisions alongside newly written or significantly modified code.
* Increase the version number in `build.gradle` with **every completed change**.
* Preserve deterministic world generation wherever possible.

  * The same world seed and coordinates should produce the same Labrinth layout unless a deliberate generation change requires otherwise.
* Avoid generation logic that performs unnecessary work every tick or repeatedly scans large areas of the world.
* Never knowingly introduce chunk-loading loops, recursive chunk generation, uncontrolled structure recursion, or cross-chunk generation behavior that can deadlock or infinitely generate terrain.
* Prefer modular systems that allow additional rooms, corridors, regions, landmarks, and generation rules to be added without rewriting the core generator.
* Do not silently remove existing functionality to implement a new feature.
* Do not commit temporary debugging code, test commands, generated dumps, logs, or development-only artifacts unless explicitly required.

---

## Directory Policy

```text
/ (root)
├─ References/
|   ├─ NeoForge-1.21.x/                     # NeoForge Source Code for Minecraft 1.21.1 (READ-ONLY)
|   └─ Minecraft_Client_Source_1.21.1/      # Minecraft 1.21.1 Decompiled Source (READ-ONLY)
├─ TheLabrinth/                             # Catalogue of verified builds/releases (READ-ONLY)
└─ src/                                     # The Labrinth Source Code (EDITABLE)
```

If additional top-level reference directories are added later, treat them as **READ-ONLY by default** unless explicitly identified as editable in this file.

---

## Allowed Edits

The following locations may be modified:

* `src/**`

### Root Documentation

* `README.md`
* `CONTRIBUTING.md`
* `.gitignore`
* `.editorconfig`
* `SUGGESTIONS.md`
* `TRACELOG.md`

### Build Configuration

* `gradle.properties`
* `build.gradle`
* `settings.gradle`

Additional files may only be modified when explicitly authorized by the task or by an update to this policy.

---

## Forbidden Edits

Do **NOT** modify:

* Anything inside a top-level directory designated as `READ-ONLY`
* Minecraft decompiled source
* NeoForge source
* ModDevGradle source
* Verified/reference builds
* Generated upstream source
* External dependency source code
* `TASK.md`
* `AGENTS.md`

If an apparent solution requires modifying a forbidden file, stop and find a solution within The Labrinth's editable source instead.

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
├── Decorations
├── Loot
└── Generation Rules
```

Avoid giant classes containing unrelated generation behavior.

Individual room or structure definitions should contain their own configuration where appropriate rather than adding extensive special-case logic to the core generator.

Prefer registries, data-driven definitions, interfaces, configuration objects, or reusable generation components where they improve maintainability.

---

## Room & Corridor Connections

All modular generation pieces should have clearly defined connection behavior.

Where applicable, generated pieces should define information such as:

* Available connection points
* Connection direction
* Connection type
* Rotation compatibility
* Bounding area
* Minimum required clearance
* Allowed neighboring structures
* Region restrictions
* Depth restrictions
* Generation weight
* Rarity
* Placement conditions

Connections should be validated before committing placement.

Never assume another room successfully generated without verifying the result.

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
* Lazy initialization where appropriate

Avoid:

* Repeated filesystem access during gameplay
* Large allocations inside frequent loops
* Recalculating invariant generation data
* Per-tick scans of large areas
* Unbounded recursion
* Loading chunks solely for validation
* Excessive block-by-block updates where bulk placement is possible

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
* Performance optimizations
* Workarounds for Minecraft/NeoForge behavior
* Important architectural decisions

Avoid comments that merely restate obvious code.

Good:

```java
// Derive the room RNG from the world seed and origin chunk so generation
// remains stable regardless of the order in which neighboring chunks load.
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
5. If the required fix would violate the Directory Policy, document the issue and suggest a text-only solution instead.

Examples include:

* Correcting a dependency declaration
* Fixing an import
* Updating an API call to match NeoForge `1.21.1`
* Adjusting Gradle configuration
* Adding a required relative source/configuration path
* Correcting Labrinth-owned code

Do not alter NeoForge or Minecraft source to work around an error in the mod.

---

# What To Do When The Build Succeeds

After a successful build:

1. Review all changed files.
2. Check for accidental edits outside the permitted directories.
3. Review the implementation for:

   * Correctness
   * Performance
   * Readability
   * Maintainability
   * Deterministic generation
   * Chunk safety
   * Dedicated-server compatibility
4. Identify reasonable refactors or optimizations.
5. Identify feature expansions directly related to the completed task.
6. Record those ideas as a new entry in `SUGGESTIONS.md`.

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

If a nearby improvement would be valuable but is not required for the task, add it to `SUGGESTIONS.md`.

---

# Commit Checklist

Before committing, confirm:

* [ ] `TASK.md` was reviewed
* [ ] Requested task is complete
* [ ] Project builds with no errors
* [ ] Changes are limited to allowed directories
* [ ] No read-only/reference source was modified
* [ ] New or modified complex code contains useful comments
* [ ] World generation remains deterministic where applicable
* [ ] Generation logic contains no obvious unbounded recursion
* [ ] Generation does not unnecessarily force/load neighboring chunks
* [ ] Dedicated-server compatibility was considered
* [ ] Performance implications were reviewed
* [ ] `TRACELOG.md` contains a new entry with task, changes, implementation, rationale, and validation
* [ ] `SUGGESTIONS.md` contains a new relevant entry
* [ ] Documentation/configuration updated where relevant
* [ ] `build.gradle` version number incremented
* [ ] Final diff reviewed before commit
