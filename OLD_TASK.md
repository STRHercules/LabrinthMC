# TASK.md

# The Labrinth — Master Development Roadmap

This file defines the complete development roadmap for **The Labrinth**, a Minecraft `NeoForge` mod for `Minecraft 1.21.1`.

The goal of this project is to create an entire procedurally generated dimension composed primarily of interconnected corridors, hallways, rooms, chambers, vertical passages, landmarks, and themed regions.

The dimension itself is the maze.

This document is intended to be worked through incrementally. Mark tasks complete only after the implementation is functional, builds successfully, and has been validated according to `AGENTS.md`.

---

# Project Rules

Before beginning any section:

* [ ] Read `AGENTS.md`
* [ ] Review the current state of the codebase
* [ ] Review completed tasks in this file
* [ ] Do not redo completed work unless required by the current task
* [ ] Respect all read-only directories
* [ ] Keep world generation deterministic
* [ ] Avoid uncontrolled recursion
* [ ] Avoid unnecessary chunk loading
* [ ] Maintain dedicated-server compatibility
* [ ] Build the project before committing
* [ ] Increment the version in `build.gradle`
* [ ] Update `TRACELOG.md`
* [ ] Update `SUGGESTIONS.md`

---

# Phase 0 — Project Foundation

## 0.1 Project Validation

* [x] Confirm the project targets Minecraft `1.21.1`
* [x] Confirm the project uses `NeoForge`
* [x] Confirm Java version is compatible with the configured NeoForge toolchain
* [x] Confirm Gradle wrapper works
* [x] Confirm `gradlew build` succeeds before major development begins
* [x] Verify mod metadata loads correctly
* [x] Verify mod ID
* [x] Verify display name is `The Labrinth`
* [x] Verify package namespace is consistent
* [x] Remove unused template/example code if safe to do so
* [x] Confirm dedicated server startup succeeds

---

## 0.2 Base Package Organization

Create or validate a maintainable package structure.

Suggested organization:

```text
src/main/java/
└── <mod package>/
    ├── TheLabrinth.java
    ├── registry/
    ├── world/
    │   ├── dimension/
    │   ├── generation/
    │   ├── region/
    │   ├── room/
    │   ├── corridor/
    │   ├── landmark/
    │   ├── connector/
    │   └── placement/
    ├── block/
    ├── item/
    ├── entity/
    ├── loot/
    ├── event/
    ├── command/
    ├── config/
    ├── debug/
    └── client/
```

Tasks:

* [x] Create logical package structure
* [x] Separate client-only code from common/server code
* [x] Establish registry classes
* [x] Establish generation packages
* [x] Establish debug/development packages
* [x] Document important architectural boundaries

---

# Phase 1 — Dimension Foundation

## 1.1 Dimension Registration

Create the base Labrinth dimension.

* [x] Register the Labrinth dimension
* [x] Register dimension type
* [x] Register level stem if required
* [x] Create required data files
* [x] Confirm the dimension loads
* [x] Confirm `/execute in` can access it
* [x] Confirm teleportation into the dimension works
* [x] Confirm dimension survives save/reload
* [x] Confirm multiplayer/dedicated server loading works

---

## 1.2 Basic Dimension Properties

Define the initial behavior of the Labrinth dimension.

Decide and implement:

* [x] Coordinate scale
* [x] Minimum Y
* [x] Maximum Y
* [x] Logical height
* [x] Skylight behavior
* [x] Ceiling behavior
* [x] Ambient light
* [x] Bed behavior
* [x] Respawn anchor behavior
* [x] Piglin safety behavior if applicable
* [x] Natural spawning rules
* [x] Time behavior
* [x] Weather behavior
* [x] Fog behavior if handled at this stage

---

## 1.3 Initial Test Environment

Before procedural generation exists, create a safe test environment.

* [x] Generate a simple solid or empty test dimension
* [x] Create a predictable spawn platform/room
* [x] Verify player does not spawn inside blocks
* [x] Verify player cannot immediately fall into the void
* [x] Verify client rendering
* [x] Verify dedicated server behavior

---

# Phase 2 — Generation Architecture

## 2.1 Generation Design

Implement a formal generation architecture before creating large amounts of content.

Core systems should distinguish between:

```text
World
├── Region
├── Generation Cell / Sector
├── Structure Piece
│   ├── Room
│   ├── Corridor
│   ├── Junction
│   ├── Vertical Connector
│   └── Landmark
└── Decoration
```

Tasks:

* [x] Define generation coordinate system
* [x] Define generation unit size
* [x] Decide whether generation uses chunks, cells, sectors, or another deterministic grid
* [x] Define ownership rules for generated pieces
* [x] Define how pieces spanning multiple chunks are owned
* [x] Define how neighboring connections are calculated
* [x] Define how generation decisions are seeded
* [x] Ensure decisions do not depend on chunk load order

---

## 2.2 Seed Derivation

Create deterministic seed utilities.

Generation decisions should be derivable from combinations such as:

```text
World Seed
+ Dimension Salt
+ Cell Coordinates
+ Structure Type Salt
+ Local Position
```

Tasks:

* [x] Create deterministic seed utility
* [x] Create salts/constants for major generation systems
* [x] Ensure room selection is deterministic
* [x] Ensure corridor selection is deterministic
* [x] Ensure region selection is deterministic
* [x] Ensure landmark selection is deterministic
* [x] Verify repeated world reloads generate identical layouts
* [x] Verify generation order does not alter results

---

## 2.3 Generation Context

Create a shared context object for generation where useful.

Potential information:

* [x] World seed
* [x] Dimension seed/salt
* [x] Cell coordinates
* [x] Chunk coordinates
* [x] Local generation depth
* [x] Region
* [x] Neighbor information
* [x] Random source
* [x] Generation constraints

Avoid uncontrolled shared mutable state.

> Phase 2 validation covers repeatable architecture decisions and evaluation
> order. Placed-layout reload validation begins when Phase 3 adds executable
> structure pieces.

---

# Phase 3 — Modular Structure System

## 3.1 Base Structure Piece

Create a reusable abstraction for Labrinth generation pieces.

Pieces may include:

* Rooms
* Corridors
* Junctions
* Stairways
* Shafts
* Landmarks

Each piece should support appropriate metadata.

Potential fields:

* [x] ID
* [x] Structure/template
* [x] Width
* [x] Height
* [x] Depth
* [x] Bounding box
* [x] Weight
* [x] Rarity
* [x] Rotation rules
* [x] Mirror rules
* [x] Minimum depth
* [x] Maximum depth
* [x] Allowed regions
* [x] Connector definitions
* [x] Placement conditions
* [x] Loot configuration
* [x] Decoration rules

---

## 3.2 Connector System

Create a formal connection system.

Connector data should support:

* [x] Position
* [x] Direction
* [x] Connector type
* [x] Width
* [x] Height
* [x] Rotation
* [x] Compatibility rules
* [x] Optional required connection
* [x] Optional blocked connection

Example connector types may include:

```text
STANDARD
WIDE
DOOR
ARCH
STAIR_UP
STAIR_DOWN
SHAFT
VENT
LANDMARK
SPECIAL
```

Tasks:

* [x] Create connector representation
* [x] Create compatibility checks
* [x] Create rotation transformation
* [x] Create connection alignment
* [x] Validate matching connector dimensions
* [x] Reject invalid connections
* [x] Support capped/closed connectors

---

## 3.3 Bounding Box Validation

* [x] Implement structure bounding boxes
* [x] Detect invalid overlaps
* [x] Allow explicitly permitted overlaps
* [x] Prevent rooms generating through unrelated rooms
* [x] Prevent corridor collisions
* [x] Validate vertical overlap
* [x] Validate world height
* [x] Keep validation bounded and performant

---

# Phase 4 — Corridor Generation

## 4.1 Basic Corridor

Create the first functional corridor module.

* [x] Straight corridor
* [x] Floor
* [x] Walls
* [x] Ceiling
* [x] Lighting support
* [x] Forward connector
* [x] Rear connector
* [x] Rotation support
* [x] Deterministic placement

---

## 4.2 Corridor Variants

Create basic corridor shapes.

* [x] Short straight
* [x] Medium straight
* [x] Long straight
* [x] Left turn
* [x] Right turn
* [x] T-junction
* [x] Four-way junction
* [x] Dead end
* [x] Wide corridor
* [x] Narrow corridor

---

## 4.3 Corridor Selection

* [x] Weighted selection
* [x] Configurable weights
* [x] Avoid excessive repetition
* [x] Allow dead ends
* [x] Ensure dead-end probability is controlled
* [x] Ensure branching remains bounded
* [x] Prevent runaway recursive generation

---

# Phase 5 — Room Generation

## 5.1 Base Room System

Create modular room definitions.

* [x] Room registration
* [x] Room ID
* [x] Room weight
* [x] Room rarity
* [x] Room dimensions
* [x] Room connectors
* [x] Room rotation
* [x] Region restrictions
* [x] Depth restrictions
* [x] Placement conditions

---

## 5.2 Initial Room Set

Create enough rooms to validate diversity.

Minimum initial set:

* [x] Empty room
* [x] Small storage room
* [x] Large chamber
* [x] Utility room
* [x] Cross-room
* [x] Long rectangular room
* [x] Multi-exit room
* [x] Dead-end reward room
* [x] Decorative room
* [x] Rare test room

---

## 5.3 Room Interiors

Support room-specific content.

* [x] Decorations
* [x] Lighting
* [x] Furniture/props
* [x] Block variation
* [x] Loot containers
* [x] Spawn markers
* [x] Environmental effects
* [x] Special interactive elements

---

# Phase 6 — Continuous Labrinth Generation

## 6.1 Infinite/Expandable Layout

Create the system that allows the Labrinth to continue generating as players explore.

* [x] Generate pieces beyond initial spawn
* [x] Generate deterministically as chunks load
* [x] Avoid pre-generating the entire dimension
* [x] Avoid recursive chunk loading
* [x] Ensure unloaded areas require minimal retained state
* [x] Verify generation can continue thousands of blocks from origin

---

## 6.2 Connection Continuity

* [x] Ensure corridors align across chunk boundaries
* [x] Ensure room entrances align across chunk boundaries
* [x] Ensure walls do not block valid connections
* [x] Ensure neighboring chunks agree on connection state
* [x] Ensure generation order does not create mismatches
* [x] Test approaching the same area from different directions

---

## 6.3 Dead-End Handling

Dead ends are intentional but should not dominate generation.

* [x] Define dead-end frequency
* [x] Allow dead-end rooms
* [x] Allow dead-end corridors
* [x] Add optional reward logic
* [x] Ensure large areas do not accidentally become inaccessible
* [x] Ensure generation does not require every path to reconnect

---

# Phase 7 — Vertical Generation

## 7.1 Multiple Floors

* [x] Support generation above/below starting floor
* [x] Define floor height
* [x] Define vertical layer system
* [x] Prevent layer overlap
* [x] Allow different room pools by elevation

---

## 7.2 Stairways

* [x] Stair up piece
* [x] Stair down piece
* [x] Stair connectors
* [x] Multi-floor alignment
* [x] Bounding validation

---

## 7.3 Shafts

* [x] Vertical shaft
* [x] Ladder shaft
* [x] Drop shaft
* [x] Elevator-like shaft placeholder
* [x] Multi-floor shaft support
* [x] Safety handling

---

## 7.4 Vertical Restrictions

* [x] Respect dimension minimum Y
* [x] Respect dimension maximum Y
* [x] Prevent vertical generation outside valid bounds
* [x] Cap upward/downward branching where required

---

# Phase 8 — Region System

## 8.1 Region Architecture

Regions control the visual and generation identity of large portions of the Labrinth.

Create a region definition system supporting:

* [x] Region ID
* [x] Region weight
* [x] Room pool
* [x] Corridor pool
* [x] Block palette
* [x] Lighting rules
* [x] Decoration rules
* [x] Mob rules
* [x] Loot modifiers
* [x] Ambient properties
* [x] Generation conditions

---

## 8.2 Region Distribution

* [x] Deterministic region selection
* [x] Large contiguous region areas
* [x] Avoid changing region every chunk
* [x] Support gradual transitions
* [x] Support rare regions
* [x] Support depth-restricted regions
* [x] Support elevation-restricted regions

---

## 8.3 Initial Regions

Create initial themed environments.

### Default / Standard

* [x] Base Labrinth region

### Abandoned

* [x] Damaged walls
* [x] Broken lighting
* [x] Debris
* [x] Cracked surfaces
* [x] Derelict rooms

### Industrial

* [x] Utility corridors
* [x] Pipes
* [x] Machinery props
* [x] Storage areas
* [x] Maintenance rooms

### Flooded

* [x] Waterlogged sections
* [x] Leaks
* [x] Flooded rooms
* [x] Water-safe generation logic

### Overgrown

* [x] Moss
* [x] Vines
* [x] Vegetation
* [x] Organic room variants

### Ancient

* [x] Older architecture
* [x] Stone palette
* [x] Ruin-like rooms

### Corrupted

* [x] Unusual geometry
* [x] Altered palettes
* [x] Strange room selection
* [x] Rare environmental behavior

---

# Phase 9 — Depth / Progression System

## 9.1 Labrinth Depth

Create a logical concept of "depth."

Depth does not necessarily need to equal physical Y-level.

Potential inputs:

```text
Distance from origin
+ Generation branch depth
+ Region transitions
+ Landmark progression
+ Optional vertical distance
```

Tasks:

* [x] Define depth formula
* [x] Ensure depth is deterministic
* [x] Expose depth to generation context
* [x] Allow content to specify minimum depth
* [x] Allow content to specify maximum depth

---

## 9.2 Depth-Based Generation

Use depth to progressively alter content.

* [x] Increase rare room availability
* [x] Unlock new room pools
* [x] Unlock new corridor variants
* [x] Unlock new regions
* [x] Modify loot
* [x] Modify entity spawning
* [x] Modify hazards
* [x] Modify ambient effects
* [x] Allow very deep generation to become increasingly unusual

---

# Phase 10 — Landmark System

## 10.1 Landmark Architecture

Landmarks are large, rare, recognizable structures.

Create support for:

* [x] Landmark ID
* [x] Deterministic origin
* [x] Minimum spacing
* [x] Maximum frequency
* [x] Bounding size
* [x] Region restrictions
* [x] Depth restrictions
* [x] Connection requirements
* [x] Multi-chunk placement
* [x] Multi-floor placement

---

## 10.2 Landmark Ownership

Critical:

* [x] Assign one deterministic origin cell/chunk
* [x] Prevent neighboring chunks from independently spawning the same landmark
* [x] Ensure multi-chunk pieces generate once
* [x] Verify reload safety
* [x] Verify generation-order safety

---

## 10.3 Initial Landmarks

Create several test landmarks.

Potential examples:

* [x] Grand Hall
* [x] Central Stairwell
* [x] Massive Storage Complex
* [x] Generator Room
* [x] Flooded Atrium
* [x] Abandoned Station
* [x] Ancient Chamber
* [x] Corrupted Nexus

Names and designs may change during development.

---

# Phase 11 — Block Palette System

## 11.1 Palette Architecture

Create reusable block palettes.

Palettes may define:

* [ ] Floor blocks
* [ ] Wall blocks
* [ ] Ceiling blocks
* [ ] Trim blocks
* [ ] Accent blocks
* [ ] Damaged variants
* [ ] Decorative variants

---

## 11.2 Weighted Blocks

* [ ] Weighted palette entries
* [ ] Deterministic variation
* [ ] Avoid obvious repeating patterns
* [ ] Support region overrides
* [ ] Support room overrides

---

# Phase 12 — Custom Blocks

Create custom blocks only as required by the dimension.

Potential categories:

## Structural

* [ ] Labrinth wall
* [ ] Labrinth floor
* [ ] Labrinth ceiling
* [ ] Trim
* [ ] Panels
* [ ] Damaged variants

## Lighting

* [ ] Ceiling light
* [ ] Wall light
* [ ] Broken light
* [ ] Flickering light

## Environmental

* [ ] Pipes
* [ ] Grates
* [ ] Vents
* [ ] Utility panels
* [ ] Signs
* [ ] Decorative machinery

For each custom block:

* [ ] Register block
* [ ] Register item if appropriate
* [ ] Add model
* [ ] Add texture
* [ ] Add blockstate
* [ ] Add loot table
* [ ] Add tags
* [ ] Add translation
* [ ] Test placement/destruction

---

# Phase 13 — Lighting System

## 13.1 Generated Lighting

* [ ] Place lights during generation
* [ ] Support region-specific lighting
* [ ] Support room-specific lighting
* [ ] Avoid excessive block entities
* [ ] Avoid large lighting update spikes

---

## 13.2 Broken/Flickering Lighting

If implemented:

* [ ] Flicker behavior
* [ ] Performance-safe timing
* [ ] No expensive per-tick scanning
* [ ] Region-specific probability
* [ ] Disabled state
* [ ] Broken state

---

# Phase 14 — Decoration System

Create post-structure decoration where useful.

Potential decoration:

* [ ] Debris
* [ ] Crates
* [ ] Pipes
* [ ] Vents
* [ ] Shelving
* [ ] Machinery
* [ ] Furniture
* [ ] Signs
* [ ] Rubble
* [ ] Plants
* [ ] Water leaks
* [ ] Ceiling damage

Architecture:

* [ ] Region decoration pool
* [ ] Room decoration pool
* [ ] Weighted decoration
* [ ] Placement validation
* [ ] Deterministic decoration seed

---

# Phase 15 — Loot System

## 15.1 Loot Tables

* [ ] Basic loot table
* [ ] Storage room loot
* [ ] Rare room loot
* [ ] Landmark loot
* [ ] Region loot
* [ ] Depth-scaled loot

---

## 15.2 Loot Progression

* [ ] Common loot
* [ ] Uncommon loot
* [ ] Rare loot
* [ ] Very rare loot
* [ ] Deep Labrinth loot
* [ ] Landmark-exclusive loot

---

## 15.3 Loot Safety

* [ ] Prevent duplicate generation exploits where possible
* [ ] Ensure loot containers use proper loot tables
* [ ] Ensure container contents generate once
* [ ] Ensure reloads do not reroll existing containers

---

# Phase 16 — Custom Items & Resources

Potential items:

* [ ] Labrinth materials
* [ ] Artifacts
* [ ] Keys
* [ ] Navigation tools
* [ ] Region-specific resources
* [ ] Rare landmark rewards
* [ ] Utility items

For each item:

* [ ] Registration
* [ ] Translation
* [ ] Texture/model
* [ ] Creative tab integration if applicable
* [ ] Recipe if applicable
* [ ] Loot integration
* [ ] Gameplay behavior
* [ ] Dedicated-server validation

---

# Phase 17 — Entity Framework

## 17.1 Entity Registration

* [ ] Establish entity registration system
* [ ] Separate entity AI from client rendering
* [ ] Verify dedicated-server compatibility

---

## 17.2 Labrinth-Aware AI

Potential behaviors:

* [ ] Corridor wandering
* [ ] Room wandering
* [ ] Patrol routes
* [ ] Darkness preference
* [ ] Sound investigation
* [ ] Door interaction
* [ ] Region restrictions
* [ ] Depth restrictions
* [ ] Landmark guarding

---

## 17.3 Initial Entities

Do not add large numbers of mobs before the world generation system is stable.

Possible first entities:

* [ ] Passive/neutral Labrinth creature
* [ ] Basic hostile corridor creature
* [ ] Rare room creature
* [ ] Region-exclusive creature
* [ ] Landmark guardian

---

# Phase 18 — Spawn System

* [ ] Region-based spawns
* [ ] Room-based spawns
* [ ] Depth-based spawns
* [ ] Light-level considerations
* [ ] Landmark-specific spawns
* [ ] Spawn caps
* [ ] Performance review
* [ ] Prevent mobs from spawning inside walls
* [ ] Prevent excessive mob density in enclosed spaces

---

# Phase 19 — Hazards

Potential hazards:

* [ ] Collapsing sections
* [ ] Damaged floors
* [ ] Flooding
* [ ] Electrical hazards
* [ ] Darkness zones
* [ ] Toxic/environmental zones
* [ ] Traps
* [ ] Region-specific hazards

Hazards must:

* [ ] Be deterministic where generated
* [ ] Avoid excessive ticking
* [ ] Be identifiable/fair enough for survival gameplay
* [ ] Support multiplayer correctly

---

# Phase 20 — Ambient System

## 20.1 Ambient Sounds

* [ ] Region ambient loops
* [ ] Rare one-shot sounds
* [ ] Distant machinery
* [ ] Drips
* [ ] Metallic noises
* [ ] Environmental creaks
* [ ] Unexplained distant sounds

---

## 20.2 Ambient Events

Potential events:

* [ ] Lights briefly dim
* [ ] Lights flicker
* [ ] Distant door sound
* [ ] Sudden silence
* [ ] Mechanical startup/shutdown
* [ ] Environmental rumble

Requirements:

* [ ] Client-safe implementation
* [ ] No heavy world scanning
* [ ] Multiplayer compatibility
* [ ] Configurable frequency

---

# Phase 21 — Navigation

Getting lost should remain part of the experience, but navigation systems may exist.

Potential features:

* [ ] Coordinate-compatible navigation
* [ ] Custom compass behavior
* [ ] Landmark locator
* [ ] Breadcrumb item
* [ ] Chalk/marker block
* [ ] Player waypoint support
* [ ] Map behavior research
* [ ] Optional custom map solution

Do not make navigation so powerful that it removes the intended exploration challenge.

---

# Phase 22 — Entry & Exit

## 22.1 Dimension Entry

Determine intended entry method.

Possible methods:

* [ ] Portal
* [ ] Rare overworld structure
* [ ] Crafted gateway
* [ ] Key item
* [ ] Command-only during early development

---

## 22.2 Dimension Exit

Determine how players leave.

Potential methods:

* [ ] Return portal
* [ ] Generated exit rooms
* [ ] Recall item
* [ ] Landmark portal
* [ ] Death/respawn rules

Ensure players cannot become permanently trapped unless that is explicitly intended.

---

# Phase 23 — Spawn / Entrance Area

Create a controlled initial arrival experience.

* [ ] Safe initial room
* [ ] Guaranteed valid exits
* [ ] Prevent immediate hostile spawn
* [ ] Establish visual identity
* [ ] Optional starter loot
* [ ] Optional navigation hints
* [ ] Ensure multiplayer players can arrive safely

---

# Phase 24 — Datapack Support

Where practical, make content data-driven.

Potential datapack-controlled systems:

* [ ] Room definitions
* [ ] Room weights
* [ ] Region definitions
* [ ] Region weights
* [ ] Block palettes
* [ ] Landmark definitions
* [ ] Loot
* [ ] Depth requirements
* [ ] Generation conditions

---

## 24.1 Reload Safety

* [ ] Validate malformed data
* [ ] Log clear errors
* [ ] Avoid crashing entire worlds due to one malformed room definition where practical
* [ ] Define behavior for missing templates
* [ ] Define duplicate ID behavior

---

# Phase 25 — Configuration

Create config options where they provide meaningful player/server control.

Potential config:

* [ ] Region size
* [ ] Rare room frequency
* [ ] Landmark frequency
* [ ] Corridor branching
* [ ] Dead-end frequency
* [ ] Mob spawning
* [ ] Ambient events
* [ ] Hazard frequency
* [ ] Loot scaling
* [ ] Debug output

Avoid exposing configuration values that could easily create invalid generation unless validated.

---

# Phase 26 — Debugging Tools

World generation will require robust debugging utilities.

Potential tools:

* [ ] Debug command root
* [ ] Display current region
* [ ] Display Labrinth depth
* [ ] Display generation cell
* [ ] Display room ID
* [ ] Display landmark ID
* [ ] Display generation seed
* [ ] Force-generate test room
* [ ] Teleport to region
* [ ] Teleport to landmark
* [ ] Dump generation information
* [ ] Visualize connectors
* [ ] Visualize bounding boxes

Debug functionality should not ship enabled unnecessarily.

---

# Phase 27 — Generation Validation

Create systematic generation testing.

## Seed Testing

* [ ] Test at least 5 seeds
* [ ] Test negative coordinates
* [ ] Test large positive coordinates
* [ ] Test large negative coordinates
* [ ] Test near world origin
* [ ] Test very large travel distances

---

## Reload Testing

* [ ] Generate area
* [ ] Exit world
* [ ] Reload world
* [ ] Confirm geometry unchanged
* [ ] Confirm loot unchanged after generation
* [ ] Confirm landmarks unchanged
* [ ] Confirm regions unchanged

---

## Generation Order Testing

* [ ] Approach area from north
* [ ] Approach same area from south in clean copy
* [ ] Compare result
* [ ] Approach from east
* [ ] Approach from west
* [ ] Confirm matching layout

---

# Phase 28 — Performance Testing

## Chunk Generation

* [ ] Measure generation time
* [ ] Identify expensive generation stages
* [ ] Review block placement count
* [ ] Review structure validation cost
* [ ] Review allocation hotspots
* [ ] Review template loading behavior

---

## Runtime

* [ ] Verify no large per-tick scans
* [ ] Verify no unintended permanent chunk tickets
* [ ] Verify no growing global generation cache
* [ ] Verify no major memory leak
* [ ] Verify mob AI remains acceptable in enclosed spaces

---

## Stress Tests

* [ ] High-speed creative flight
* [ ] Teleport repeatedly across unexplored areas
* [ ] Multiple players exploring separate areas
* [ ] Chunk pregeneration test
* [ ] Server restart after heavy exploration

---

# Phase 29 — Multiplayer

* [ ] Dedicated server starts successfully
* [ ] Multiple players can enter dimension
* [ ] Generation remains deterministic
* [ ] Players exploring separate areas do not corrupt generation
* [ ] Loot behaves correctly
* [ ] Entity spawning behaves correctly
* [ ] Client-only effects do not crash server
* [ ] Dimension save data persists
* [ ] Rejoining works correctly

---

# Phase 30 — Compatibility

Test common systems and mods where reasonable.

Areas to validate:

* [ ] JEI/EMI-style item viewers if relevant
* [ ] JourneyMap
* [ ] Xaero's Minimap
* [ ] Xaero's World Map
* [ ] Waystone mods
* [ ] Shader compatibility
* [ ] Distant Horizons behavior
* [ ] Chunk pregeneration tools
* [ ] Performance mods
* [ ] Server management tools

Compatibility fixes should not compromise core generation safety.

---

# Phase 31 — Visual Identity

## Dimension Visuals

* [ ] Finalize primary block palette
* [ ] Finalize region palettes
* [ ] Improve corridor variation
* [ ] Improve room variety
* [ ] Improve lighting
* [ ] Improve ceiling variation
* [ ] Improve floor variation
* [ ] Add environmental storytelling

---

## Client Effects

Potential effects:

* [ ] Custom fog
* [ ] Region fog variation
* [ ] Custom sky behavior
* [ ] Darkness effects
* [ ] Ambient particles
* [ ] Subtle color grading where feasible

Keep client code isolated.

---

# Phase 32 — Environmental Storytelling

Add world details that imply history without requiring explicit exposition.

Potential content:

* [ ] Signs
* [ ] Abandoned supplies
* [ ] Damaged machinery
* [ ] Barricades
* [ ] Camps
* [ ] Strange markings
* [ ] Broken doors
* [ ] Failed experiments
* [ ] Notes/books if appropriate
* [ ] Region-specific environmental clues

---

# Phase 33 — Lore Framework

Optional.

* [ ] Define Labrinth origin concept
* [ ] Define whether lore is explicit or ambiguous
* [ ] Define major factions/entities if any
* [ ] Define landmark lore
* [ ] Define region lore
* [ ] Add discoverable clues
* [ ] Avoid requiring lore knowledge for normal gameplay

---

# Phase 34 — Rare & Strange Generation

Once normal generation is stable, introduce low-frequency anomalies.

Potential examples:

* [ ] Extremely long corridor
* [ ] Room with impossible-looking scale
* [ ] Unusually tall chamber
* [ ] Repeating room sequence
* [ ] Strange lighting
* [ ] Abnormal palette
* [ ] Sudden architectural transition
* [ ] Rare dead-silent region
* [ ] Hidden passage
* [ ] Secret wall
* [ ] Massive empty room

Rare generation should remain deterministic.

---

# Phase 35 — Special Rooms

Create unique room classes.

Potential categories:

* [ ] Loot room
* [ ] Trap room
* [ ] Safe room
* [ ] Puzzle room
* [ ] Entity nest
* [ ] Shrine
* [ ] Machine room
* [ ] Observation room
* [ ] Archive
* [ ] Laboratory
* [ ] Garden
* [ ] Flood control room
* [ ] Security room

---

# Phase 36 — Secret Generation

* [ ] Hidden doors
* [ ] Breakable walls
* [ ] Concealed corridors
* [ ] Secret rooms
* [ ] Rare caches
* [ ] Alternate landmark entrances
* [ ] Region-specific secrets

---

# Phase 37 — Gameplay Progression

If progression becomes part of the design:

* [ ] Define progression goals
* [ ] Define reasons to travel deeper
* [ ] Define resource progression
* [ ] Define loot progression
* [ ] Define enemy progression
* [ ] Define landmark progression
* [ ] Define rare material progression
* [ ] Avoid requiring a linear route through a nonlinear world

---

# Phase 38 — Boss / Major Encounter Framework

Optional and should occur only after entities and landmarks are stable.

* [ ] Boss arena landmark support
* [ ] Encounter trigger
* [ ] Multiplayer scaling
* [ ] Reward handling
* [ ] Reset/re-entry rules
* [ ] Save-state persistence

---

# Phase 39 — API / Extensibility

Long-term goal: allow additional Labrinth content without rewriting the core generator.

Potential extension points:

* [ ] Room registration API
* [ ] Corridor registration API
* [ ] Region registration API
* [ ] Landmark registration API
* [ ] Connector types
* [ ] Block palettes
* [ ] Loot hooks
* [ ] Generation conditions

---

# Phase 40 — Documentation

## README

* [ ] Keep README synchronized with implemented functionality
* [ ] Remove planned features that are abandoned
* [ ] Mark implemented features accurately
* [ ] Add screenshots when available
* [ ] Add installation instructions
* [ ] Add requirements
* [ ] Add compatibility notes

---

## Developer Documentation

* [ ] Document generation architecture
* [ ] Document seed system
* [ ] Document connector system
* [ ] Document region system
* [ ] Document room registration
* [ ] Document landmark system
* [ ] Document datapack format
* [ ] Document debugging commands

---

# Phase 41 — Error Handling & Logging

* [ ] Use consistent mod logger
* [ ] Remove excessive debug logging
* [ ] Log invalid generation data clearly
* [ ] Log missing structures clearly
* [ ] Avoid log spam during normal chunk generation
* [ ] Provide useful debug logging behind config/debug mode
* [ ] Handle recoverable generation failures gracefully

---

# Phase 42 — Save Compatibility

Before public releases:

* [ ] Define what generation changes may break old worlds
* [ ] Avoid changing existing generated chunks
* [ ] Ensure new content can generate in unexplored areas
* [ ] Document incompatible generation changes
* [ ] Consider generation version identifier
* [ ] Consider storing generation version with Labrinth save data

---

# Phase 43 — Release Preparation

## Code

* [ ] Remove temporary code
* [ ] Remove unused imports
* [ ] Remove dead code
* [ ] Review TODO comments
* [ ] Review warnings
* [ ] Review deprecated API usage
* [ ] Review client/server separation
* [ ] Review generation safety
* [ ] Review performance

---

## Assets

* [ ] Validate textures
* [ ] Validate models
* [ ] Validate sounds
* [ ] Validate translations
* [ ] Validate loot tables
* [ ] Validate tags
* [ ] Validate structure files
* [ ] Validate datapack resources

---

## Builds

* [ ] Clean build succeeds
* [ ] Client starts
* [ ] Dedicated server starts
* [ ] New world creates successfully
* [ ] Labrinth generates successfully
* [ ] Existing test world reloads successfully
* [ ] Release JAR tested independently

---

# Phase 44 — Initial Public Release

Minimum recommended release requirements:

* [ ] Functional Labrinth dimension
* [ ] Stable procedural generation
* [ ] Corridors
* [ ] Rooms
* [ ] Junctions
* [ ] Dead ends
* [ ] Multiple floors
* [ ] Deterministic generation
* [ ] At least 3 meaningful regions
* [ ] At least 1 landmark
* [ ] Loot
* [ ] Entry method
* [ ] Exit method
* [ ] Dedicated-server support
* [ ] Multiplayer-tested
* [ ] Configuration
* [ ] Stable save/reload
* [ ] No known infinite-generation bugs
* [ ] No known chunk-loading loops
* [ ] Acceptable generation performance
* [ ] Updated README
* [ ] Release changelog

---

# Phase 45 — Post-Release Expansion

After the first stable release:

* [ ] Additional room sets
* [ ] Additional corridor styles
* [ ] Additional regions
* [ ] Additional landmarks
* [ ] More vertical structures
* [ ] Rare anomalies
* [ ] Environmental hazards
* [ ] Entities
* [ ] More loot
* [ ] More custom blocks
* [ ] Navigation tools
* [ ] Lore
* [ ] Boss encounters
* [ ] Datapack expansion
* [ ] API expansion
* [ ] Compatibility improvements
* [ ] Performance improvements

---

# Continuous Requirements

These requirements apply throughout the entire project.

## Determinism

* [ ] Same seed + same coordinates = same generation
* [ ] Generation does not depend on exploration order
* [ ] Generation does not depend on client state

---

## Chunk Safety

* [ ] No uncontrolled chunk loading
* [ ] No chunk-generation recursion
* [ ] No circular generation dependencies
* [ ] Multi-chunk structures have deterministic ownership

---

## Performance

* [ ] No unnecessary per-tick world scans
* [ ] No unbounded searches
* [ ] No unbounded recursion
* [ ] No uncontrolled memory growth
* [ ] Generation work remains bounded

---

## Maintainability

* [ ] Core systems remain modular
* [ ] Large classes are split where appropriate
* [ ] Special cases do not overwhelm core generation logic
* [ ] Important algorithms are commented
* [ ] Reusable systems are preferred over duplicated code

---

## Dedicated Server

* [ ] Common code does not reference client-only classes
* [ ] Dedicated server builds
* [ ] Dedicated server starts
* [ ] World generation functions server-side without a client

---

# Milestone Checklist

## Milestone 1 — Dimension Exists

* [x] Dimension registered
* [x] Dimension accessible
* [x] Safe spawn
* [x] Build stable

---

## Milestone 2 — Basic Labrinth

* [ ] Corridors generate
* [ ] Rooms generate
* [ ] Connections work
* [ ] Generation crosses chunks
* [ ] Generation is deterministic

---

## Milestone 3 — True Procedural World

* [ ] Continuous generation
* [ ] Junctions
* [ ] Dead ends
* [ ] Multiple floors
* [ ] Chunk-order independence
* [ ] Stable save/reload

---

## Milestone 4 — Regional Variety

* [ ] Region system
* [ ] At least 3 regions
* [ ] Region palettes
* [ ] Region-specific room pools
* [ ] Region transitions

---

## Milestone 5 — Exploration Gameplay

* [ ] Loot
* [ ] Rare rooms
* [ ] Landmarks
* [ ] Depth progression
* [ ] Entry
* [ ] Exit

---

## Milestone 6 — Atmosphere

* [ ] Lighting variation
* [ ] Decorations
* [ ] Ambient sounds
* [ ] Ambient events
* [ ] Environmental storytelling

---

## Milestone 7 — Living Labrinth

* [ ] Entity framework
* [ ] Labrinth-specific mobs
* [ ] Region/depth spawning
* [ ] Hazards
* [ ] Landmark encounters

---

## Milestone 8 — Release Candidate

* [ ] Multiplayer stable
* [ ] Dedicated server stable
* [ ] Performance acceptable
* [ ] Compatibility tested
* [ ] README current
* [ ] No critical generation bugs
* [ ] Release build verified

---

# Definition of Done

The core project may be considered feature-complete when:

* [ ] The Labrinth exists as a standalone Minecraft dimension
* [ ] The dimension can generate effectively indefinitely
* [ ] The world consists primarily of interconnected interior architecture
* [ ] Corridors, rooms, junctions, and vertical connections generate reliably
* [ ] Generation remains deterministic
* [ ] Chunk generation order does not corrupt layout
* [ ] Multiple themed regions exist
* [ ] Rare rooms and landmarks exist
* [ ] Depth meaningfully affects exploration
* [ ] Exploration provides meaningful rewards
* [ ] The dimension has a functional entry and exit system
* [ ] Multiplayer functions correctly
* [ ] Dedicated servers function correctly
* [ ] Save/reload behavior is stable
* [ ] Performance is acceptable during normal exploration
* [ ] No known uncontrolled generation loops exist
* [ ] No known chunk-loading recursion exists
* [ ] The architecture can support future rooms, regions, landmarks, and content without major rewrites

---

# Goal Expansion — Reserved Compounds and Major Room Catalogue

The requested village, dungeon, room-family, and monster-outpost expansion is
implemented on top of the existing deterministic room/corridor/landmark
generator.

* [x] Shared origin-owned compound definitions and complete-footprint reservations
* [x] Explicit compound connectors and ordinary-content yield behavior
* [x] Enclosed village variants with beds, job-site props, loot, and villagers
* [x] Compact and multi-room dungeon variants with controlled encounters
* [x] Caves, jungle, spider, quarters, barracks, stockade, dining, library,
  treasury, frozen, massive, chapel, and jail room families
* [x] Zombie, skeleton, illager, piglin, and wither-skeleton outposts
* [x] Depth, region, rarity, floor, deterministic reload, and bounded debug lookup rules
* [x] Framework-free reservation, connector, and chunk-order self-check coverage

Custom blocks and custom loot tables remain later extension points; current
content uses vanilla blocks and vanilla loot tables. The operator locator
(`/labrinth locate all` or `/labrinth locate <theme> [radius]`) complements the
generator F3 summary and bounded catalog lookup without changing generation.

# Final Vision

The Labrinth should not feel like a dungeon placed inside Minecraft.

It should feel like Minecraft has loaded an entirely different kind of world.

A world without open horizons.

A world of corridors, doors, chambers, stairwells, shafts, forgotten machinery, distant sounds, strange landmarks, and paths that continue far beyond what the player can reasonably map.

There should always be another hallway.

There should always be another room.

And the player should never be entirely certain what exists beyond the next corner.
