# The Labrinth Reference Guide

This document tells development agents **which read-only reference project to inspect for a given Labrinth problem, what concepts are worth taking from it, and what should remain Labrinth-owned**.

It is a development acceleration guide, not permission to copy third-party code indiscriminately.

All projects listed here live under `References/` and are **READ-ONLY**.

---

# Core Rule

The Labrinth already owns the world-generation contract.

External references should help implement **subsystems**, not replace Labrinth's generator.

The Labrinth remains authoritative for:

* world seed determinism
* 64-block cell ownership
* region/depth decisions
* chunk-safe materialization
* neighbor-edge agreement
* connector validation
* reserved compound ownership
* complete-footprint reservations
* collision rules
* intentional compound entrances/exits
* no-neighbor-chunk-loading decisions
* bounded generation
* dedicated-server compatibility

The normal integration pattern should be:

```text
REFERENCE IMPLEMENTATION
        ↓
extract useful idea / algorithm / data pattern
        ↓
translate into Labrinth concepts
        ↓
validate against Minecraft 1.21.1
        ↓
implement inside Labrinth-owned code
        ↓
preserve Labrinth ownership + determinism + chunk safety
```

## Donor-First Rule for Content Work

When the requested task is primarily **content authoring** rather than generator architecture, first check whether an allowed donor already contains a suitable structure.

For example, before hand-building another:

* village house
* profession building
* market
* meeting hall
* dungeon chamber
* stair room
* cave shelter
* mine room
* outpost
* crypt/catacomb room

inspect the donor-candidate references and determine whether an existing template can be legally and technically migrated.

The preferred content workflow is:

```text
existing permitted donor structure
→ record source + license
→ copy into Labrinth-owned namespace
→ convert to 1.21.1 if required
→ replace unavailable mod blocks/entities
→ normalize connectors/markers
→ apply Labrinth processors/palette
→ register in Labrinth pool
→ Labrinth controls reservation and placement
```

This is intentionally different from copying another mod's complete generator.

---

# Reference Selection Cheat Sheet

| Need | Primary Reference | Secondary / Donor Reference |
| --- | --- | --- |
| Vanilla `1.21.1` structure API | `Minecraft_Client_Source_1.21.1` | `NeoForge-1.21.11` |
| Jigsaw/template authoring | `StructureTutorialMod-1.21.11-Neoforge-Jigsaw` | `Minecraft_Client_Source_1.21.1` |
| Template pools/processors at scale | `RepurposedStructures-1.21.5-MDG` | `StructureTutorialMod-1.21.11-Neoforge-Jigsaw` |
| Data-driven worldgen extensions | `lithostitched-1.21.2` | `RepurposedStructures-1.21.5-MDG` |
| Cave/noise math | `larion-world-generation-main` | `YUNGs-Better-Caves-1.21.1` |
| Cave-room structure assets | `Simple Structures Caves 1.21.10-11.jar` | `compatstructures-1.0.3.jar` |
| Minecraft carver architecture | `YUNGs-Better-Caves-1.21.1` | `Minecraft_Client_Source_1.21.1` |
| Dungeon assembly | `YUNGs-Better-Dungeons-1.21.1` | `YUNGs-Better-Mineshafts-1.21.1` |
| Underground dungeon room donors | `Dungeons And Villages DeeperAndDarker 1.21.1.jar` | `compatstructures-1.0.3.jar` |
| Roguelike dungeon architecture | `DungeonCrawl-neoforge-1.21` | `YUNGs-Better-Dungeons-1.21.1` |
| Branching piece graphs | `YUNGs-Better-Mineshafts-1.21.1` | `YUNGs-Better-Strongholds-1.21.1` |
| Large landmarks/fortresses | `YUNGs-Better-Strongholds-1.21.1` | `mostructures-1.21.x` |
| Enclosed underground villages | `Underground-Village-Multiloader-1.21.1-dev` | `More Villages v0.2.3-alpha` |
| Village building donor library | `Underground-Village-Multiloader-1.21.1-dev` | `More Villages v0.2.3-alpha` |
| Outposts/markets/general structures | `mostructures-1.21.x` | `compatstructures-1.0.3.jar` |
| Regional structure variants | `RepurposedStructures-1.21.5-MDG` | `Minecraft_Client_Source_1.21.1` |
| Spatial/bounding utilities | `lithostitched-1.21.2` | YUNG structure references |
| Layout inspiration only | `adventuredungeons-neoforge-1.21-1.3.1.jar` | `claustrophobic_dungeons-1.0.1-forge-1.20.1.jar` |

## Reuse Legend

* **Donor candidate**: direct asset/code adaptation may be possible after verifying the exact local license and satisfying its conditions.
* **Conditional/copyleft**: best used for concepts unless the project deliberately accepts the license obligations attached to direct reuse.
* **Look-only**: inspect for ideas only. Do not extract/copy/adapt assets or code without separate permission.
* **Version-mismatched**: architecture/assets may still be useful, but all APIs and data formats must be translated/validated against Minecraft `1.21.1`.

---

# 1. `Minecraft_Client_Source_1.21.1`

## Use It For

This is the primary source of truth for how **vanilla Minecraft `1.21.1`** implements worldgen and structures.

Inspect it for:

* `Structure`
* `StructurePiece`
* `StructurePiecesBuilder`
* `StructureTemplate`
* `StructureTemplateManager`
* `StructurePlaceSettings`
* `StructureProcessor`
* `StructureProcessorList`
* jigsaw structures
* template pools
* bounding boxes
* rotation/mirroring
* loot placement
* structure block entities
* worldgen random behavior
* generation steps
* chunk interactions
* codecs/serialization
* vanilla villages
* strongholds
* mineshafts
* trial chambers
* ancient cities
* woodland mansions
* nether fortresses
* ruined portals

## Take From It

Take:

* correct `1.21.1` APIs
* vanilla serialization patterns
* structure placement contracts
* bounding-box behavior
* template placement semantics
* processor behavior
* rotation/mirror transformations
* vanilla jigsaw data formats
* safe examples of how vanilla structures interact with chunks

## Do Not Take

Do not:

* modify the source
* assume vanilla ownership rules automatically satisfy Labrinth's compound rules
* convert Labrinth into a vanilla structure-placement world
* use vanilla behavior that forces exploration-order dependencies into Labrinth

---

# 2. `NeoForge-1.21.11`

## Use It For

Inspect when a task involves:

* NeoForge registration
* events
* data generation
* biome/worldgen modifiers
* loader integration
* server-side hooks
* registries
* serialization integration
* Gradle/ModDevGradle patterns

## Take From It

Take:

* loader-specific architecture
* event/registry patterns
* NeoForge integration concepts
* examples of how Minecraft worldgen hooks are exposed through NeoForge

## Warning

The folder name indicates a version newer than Labrinth's Minecraft `1.21.1` target.

Never assume its signatures are directly compatible.

For every API copied conceptually from this reference:

1. check Labrinth's configured dependency version;
2. check the Minecraft `1.21.1` implementation;
3. use the target-compatible method/signature.

---

# 3. `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`

## Best Use

This should be the first reference for building Labrinth's **template-driven room/building system**.

## Take From It

Study and adapt concepts for:

* saving authored structures as `.nbt`
* loading structure templates
* jigsaw blocks as authored connection markers
* template pools
* weighted pool entries
* fallback pools
* terminating/capping pieces
* structure processors
* processor lists
* loot markers
* entity markers
* template rotation
* modular building composition
* JSON-driven structure content

## Labrinth Feature It Should Inform

### `LabrinthTemplatePiece`

Goal:

Allow a Labrinth room, corridor, building, or compound piece to reference an authored NBT structure instead of hard-coding all blocks in Java.

Suggested Labrinth metadata:

```text
TemplatePiece
├── id
├── template
├── bounds
├── weight
├── rarity
├── minDepth
├── maxDepth
├── allowedRegions
├── allowedFloors
├── connectors
├── rotationRules
├── processorSet
├── lootRules
├── populationRules
└── placementConditions
```

### Connector Marker Import

Consider supporting authored markers that are translated into Labrinth connector definitions at load time.

Possible workflow:

```text
build room in dev world
→ place marker/jigsaw blocks at intended doors
→ save NBT
→ load template metadata
→ convert markers to Labrinth connectors
→ Labrinth owns placement/validation
```

## Do Not Take

Do not hand control of the overall dimension to vanilla jigsaw placement.

Labrinth should use jigsaw/template concepts as **content authoring tools**, while Labrinth still decides:

* whether the piece exists
* who owns it
* where it may go
* whether its bounds are valid
* which external connectors may open
* how chunk materialization occurs

---

# 4. `RepurposedStructures-1.21.5-MDG`

## Best Use

Use this as the reference for managing **a very large library of structure variants without giant Java registries**.

## Take From It

Study:

* structure NBT organization
* large template libraries
* reusable pool organization
* processor lists
* block palette transformations
* theme-specific variants
* data-driven structure configuration
* pool additions/extension patterns
* keeping structure content separate from structure orchestration
* compatibility/data-pack style organization

## Labrinth Feature It Should Inform

### `LabrinthPiecePool`

Goal:

Make room/building/compound content appendable without modifying the central generator.

Conceptual example:

```text
labrinth:rooms/quarters
├── quarters_small_01
├── quarters_small_02
├── quarters_large_01
└── quarters_ruined_01
```

Each entry can define:

```text
template
weight
rarity
region restrictions
depth restrictions
floor restrictions
processor set
connector profile
```

### Regional Processor Variants

A single authored building should be reusable across regions where practical.

Example:

```text
barracks_01.nbt
      ↓
STANDARD processor
ABANDONED processor
OVERGROWN processor
ANCIENT processor
CORRUPTED processor
```

This is preferable to creating five nearly identical NBT files.

## Version Warning

This reference targets a newer Minecraft version than Labrinth.

Use its **organization and architecture**, then validate all implementation details against `1.21.1`.

---

# 5. `lithostitched-1.21.2`

## Best Use

Use for **data-driven worldgen infrastructure and reusable utilities**, especially where vanilla worldgen is awkward to extend cleanly.

## Take From It

Study:

* data-driven worldgen extension patterns
* codecs/registries
* template-pool manipulation concepts
* reusable worldgen utility architecture
* structure utility code
* bounding/spatial data structures
* efficient lookup strategies
* extensibility without huge hard-coded registries

## Specific Labrinth Areas

### Datapack Support

Use it as a design reference for Labrinth's eventual:

```text
data/labrinth/rooms/
data/labrinth/corridors/
data/labrinth/compounds/
data/labrinth/processors/
data/labrinth/pools/
```

### Spatial Reservation Lookup

Inspect spatial/bounding structures such as octree-style organization if the current compound reservation lookup becomes a performance bottleneck.

Do not add complexity preemptively.

Only use a spatial index if profiling or scaling shows the current bounded lookup needs it.

## Version Warning

This targets `1.21.2`.

Do not paste APIs directly into `1.21.1` code without translation.

---

# 6. `YUNGs-Better-Caves-1.21.1`

## Best Use

Use this for **Minecraft-style cave/carver architecture**.

## Take From It

Study:

* cave carver abstraction
* cavern carvers
* carver settings
* noise ranges
* vertical cave layers
* cavern layers
* configuration-driven cave behavior
* separating noise sampling from carving
* NeoForge carver registration patterns for `1.21.1`

## Labrinth Feature It Should Inform

### `LabrinthCaveCompound`

The Labrinth does not need global Overworld cave carving.

Instead:

```text
select cave compound
→ reserve complete 3D bounds
→ derive deterministic cave seed
→ run localized cave/noise sampler inside reservation
→ shape walls/floor/ceiling
→ open only declared Labrinth connectors
→ ordinary rooms/corridors yield to cave bounds
```

Useful for:

* enormous caves
* jungle grottos
* spider caverns
* collapsed areas
* underground lakes
* ancient natural chambers
* corrupted organic spaces

## Important

Carver code often assumes it is operating on normal terrain generation.

Adapt the math and architecture.

Do not allow a cave algorithm to carve through unrelated Labrinth structures outside its owned reservation.

---

# 7. `larion-world-generation-main`

## Best Use

Use this for **organic worldgen mathematics**, especially cave and terrain density techniques.

## Take From It

Study:

* density functions
* noise composition
* domain warping
* irregular volume shaping
* large-scale natural forms
* cave deformation
* layered noise
* mixing several noise fields
* avoiding visibly geometric cave shapes

## Labrinth Feature It Should Inform

### `LabrinthNoiseVolume`

A generic bounded sampler that can answer:

```text
inside solid wall?
inside cave air?
floor material?
ceiling material?
decorative pocket?
water?
```

for positions inside a reserved organic compound.

This can power far more than caves:

* giant roots
* fungal caverns
* corrupted flesh-like regions
* collapsed voids
* underground lakes
* geodes
* massive stone rooms
* eroded ruins

## Important

Use Larion to improve **shape quality**.

Do not import its entire terrain generator into Labrinth.

---

# 8. `YUNGs-Better-Dungeons-1.21.1`

## Best Use

This is the primary reference for **multi-room dungeon assembly**.

## Take From It

Study:

* building a complete `StructurePiece` graph before block placement
* dungeon-specific piece classes
* tunnels
* chambers
* nest/room variants
* entrance handling
* processors
* structure bounds
* child-piece expansion
* separating logical assembly from materialization

## Labrinth Feature It Should Inform

### `LabrinthCompoundAssembler`

Ideal high-level flow:

```text
choose dungeon origin
        ↓
derive deterministic RNG
        ↓
assemble logical dungeon graph
        ↓
calculate all piece bounds
        ↓
validate collisions / limits / Y range
        ↓
reserve complete footprint
        ↓
declare intentional external doors
        ↓
ordinary Labrinth generation yields
        ↓
chunks materialize intersecting pieces
```

## Dungeon Families

A shared assembler should support:

* compact dungeons
* complex dungeons
* crypts
* prisons
* spider nests
* undead strongholds
* chapels
* treasure vaults
* boss compounds

## Critical Labrinth Rule

No ordinary hallway may accidentally break into a dungeon.

External connections must come from the dungeon's declared connector set.

---

# 9. `YUNGs-Better-Mineshafts-1.21.1`

## Best Use

This is the strongest reference for **bounded branching piece graphs**.

## Take From It

Study:

* chain length
* maximum branch depth
* candidate piece selection
* bounding-box testing before commit
* intersections
* turns
* stairs
* side rooms
* terminal rooms
* branch-ending logic
* special endpoint selection
* piece-specific connection behavior

## Labrinth Feature It Should Inform

### Generic Compound Grammar

Instead of random room soup, a compound can have progression rules.

Example:

```text
ENTRANCE
   ↓
TRAVEL
   ↓
BRANCH
 ┌─┴─────┐
SIDE    MAIN
ROOM    PATH
          ↓
       VERTICAL
          ↓
       TERMINAL
```

That same grammar can drive:

* dungeon
* settlement
* prison
* barracks
* catacomb
* ruined mine
* fortress
* archive complex

### Hard Limits

Every expansion process should carry explicit budgets such as:

```text
maxPieces
maxDepth
maxBranches
maxHorizontalRadius
maxVerticalRange
maxFailedPlacementAttempts
```

Labrinth must not use unbounded recursive structure expansion.

---

# 10. `YUNGs-Better-Strongholds-1.21.1`

## Best Use

Use for **large connected landmarks and fortress-scale compounds**.

## Take From It

Study:

* large multi-piece topology
* hub rooms
* corridor-to-room transitions
* verticality
* memorable landmark composition
* multi-level networks
* large-scale bounds management
* themed piece families
* ensuring large compounds still have navigable internal structure

## Labrinth Feature It Should Inform

Use these patterns for things such as:

* ancient fortress
* citadel
* giant chapel
* grand archive
* massive prison
* large settlement
* faction headquarters
* major story landmark
* rare multi-floor dungeon

The goal is not to make every Labrinth cell a stronghold.

Use this reference for **rare compounds whose internal layout is itself a meaningful exploration space**.

---


# 11. `Underground-Village-Multiloader-1.21.1-dev`

## Classification

**High-priority donor candidate.** The local project/license should be verified before each direct import, but this reference is intended to be the first place to look for ordinary enclosed-village content.

## Best Use

Use for **underground village composition and ready-made village structure families**.

## Take From It

Prioritize its structure/template content for:

* town centers
* fountains
* meeting points
* underground streets
* straight street segments
* corners
* crossroads
* turns
* terminators
* small, medium, and large houses
* butcher shops
* toolsmiths
* fletchers
* shepherds
* armorers
* fishers
* tanneries
* cartographers
* libraries
* masons
* weaponsmiths
* temples
* stables
* farms
* animal pens

## Labrinth Use

This should usually be the **first donor searched for enclosed villages**.

Do not import its entire placement system as the owner of a Labrinth village. Instead:

```text
Underground Village template
→ migrate/rename into labrinth namespace
→ normalize jigsaw/door markers into Labrinth connectors
→ optionally apply Labrinth palette/processors
→ add to residential/profession/civic/street pool
→ LabrinthCompoundAssembler owns the complete settlement
```

The most valuable part is the **building and street library**, not its dimension-placement rules.

---

# 12. `More Villages v0.2.3-alpha`

## Classification

**Village donor candidate.** Verify the exact local license before direct import.

## Best Use

Use as a secondary source of **village building variety and settlement styles**.

## Take From It

Inspect for:

* houses
* profession buildings
* central/civic structures
* farms
* decorative village pieces
* road/path modules
* alternate settlement palettes

## Labrinth Use

Use this to prevent the enclosed village system from being visually dominated by a single donor project.

Prefer mixing compatible templates into Labrinth-owned pools rather than reproducing a donor village wholesale.

---

# 13. `Dungeons And Villages DeeperAndDarker 1.21.1.jar`

## Classification

**High-priority underground dungeon donor candidate.** It targets the correct Minecraft generation, but its templates may depend on Deeper and Darker content.

## Best Use

Use for **large underground dungeon room libraries and multi-level dungeon content**.

## Take From It

Inspect for reusable shapes such as:

* standard chambers
* intersections
* hallway rooms
* stair/vertical rooms
* treasure rooms
* trap rooms
* large chambers
* terminal rooms
* multi-floor transition pieces

## Dependency Migration

Do not introduce Deeper and Darker as a Labrinth dependency merely to preserve donor blocks.

Instead map foreign blocks to:

```text
vanilla equivalent
or
Labrinth custom block/palette
```

Preserve useful geometry while making the final structure self-contained.

## Labrinth Use

These pieces are candidates for:

* ancient dungeon compounds
* corrupted wings
* prison complexes
* deep ruins
* sculk-like regions using Labrinth-owned palettes
* boss/treasure compounds

---

# 14. `Simple Structures Caves 1.21.10-11.jar`

## Classification

**Cave-structure donor candidate with a version mismatch.** Treat the structure geometry as potentially useful, but do not assume its NBT/data formats are immediately compatible with `1.21.1`.

## Best Use

Use for **small and medium cave-contained structures**, especially environmental filler that makes large caverns feel inhabited.

## Take From It

Look for:

* cave shelters
* abandoned stops
* miner camps
* small ruins
* cave shrines
* tiny dungeons
* decorative structures
* rest areas
* environmental storytelling rooms

## Labrinth Use

These are ideal as low-cost content for `LabrinthCaveCompound` and giant room interiors.

Preferred migration:

```text
extract permitted template
→ inspect DataVersion / palette / block states
→ convert to 1.21.1-compatible structure data
→ replace unsupported blocks
→ strip donor placement configuration
→ place through LabrinthTemplatePiece
```

---

# 15. `compatstructures-1.0.3.jar`

## Classification

**Conditional donor/reference candidate.** Verify the exact license packaged in this JAR before copying assets. Many of its structures are compatibility-oriented and may depend on other mods.

## Best Use

Search it when Labrinth needs an existing shape for:

* mines
* catacombs
* underground utility spaces
* puzzle rooms
* treasure chambers
* cave structures
* unusual structure-mod integration examples

## Dependency Rule

A donor structure requiring another mod does **not** justify adding that mod as a Labrinth dependency unless the task explicitly calls for it.

Prefer geometry/pattern migration plus block substitution.

---

# 16. `mostructures-1.21.x`

## Classification

**Copyleft/conditional structure reference.** Verify the exact local license before direct asset reuse. Prefer concepts and layout study when licensing obligations are undesirable.

## Best Use

Use for a broad catalog of larger authored structures, especially:

* markets
* outposts
* pillager/hostile structures
* barns
* castles
* small dungeons
* landmark-scale buildings

## Labrinth Use

Good source material for:

* hostile outposts
* abandoned markets
* fortified chambers
* faction compounds
* large landmark silhouettes

Do not allow its surface placement assumptions to dictate Labrinth generation.

---

# 17. `DungeonCrawl-neoforge-1.21`

## Classification

**GPL/conditional reference.** Use primarily for architecture and room taxonomy unless Labrinth's licensing plan explicitly accepts direct reuse obligations.

## Best Use

This is a strong reference for **large roguelike underground dungeons**.

## Take From It

Study:

* floor progression
* room density
* corridor-to-room ratios
* stairs between floors
* dead ends
* loot rooms
* combat rooms
* large chambers
* repeated room families without obvious repetition
* dungeon pacing over several floors

## Labrinth Use

Use it to answer questions like:

* How many rooms should a large dungeon contain?
* How frequently should branches terminate?
* When should vertical transitions appear?
* How can repeated room types still feel varied?
* What room categories make a multi-floor dungeon feel complete?

Prefer independently implementing those concepts using Labrinth templates and compound assembly.

---

# 18. Look-Only Dungeon References

These references are intentionally retained because they are useful for **design study**, but they are not donor libraries.

## `adventuredungeons-neoforge-1.21-1.3.1.jar`

Use only for high-level inspiration such as:

* dungeon themes
* room-category ideas
* encounter pacing
* labyrinth concepts
* underground camp concepts
* treasure/trial placement ideas

Do **not** extract or adapt its NBT/assets/code without explicit permission if the packaged license does not allow reuse.

## `claustrophobic_dungeons-1.0.1-forge-1.20.1.jar`

Use only for high-level inspiration such as:

* very narrow dungeon proportions
* corridor density
* traps
* secrets
* hidden rooms
* claustrophobic pacing
* jigsaw composition concepts visible through behavior/design

It also targets Minecraft `1.20.1`, so it is not a valid `1.21.1` API or data-format authority.

Do **not** extract or adapt its NBT/assets/code without explicit permission.

---

# Recommended Shared Labrinth Systems

The reference projects collectively point toward several reusable systems that would reduce future development cost.

## 1. `LabrinthTemplatePiece`

Purpose:

Place authored `.nbt` structures through the existing Labrinth piece system.

Benefits:

* room creation becomes building/content work instead of Java work
* village buildings become easy to add
* dungeon room libraries can grow quickly
* artists/builders can create content without touching generator code

Primary references:

* StructureTutorialMod
* Minecraft source
* Repurposed Structures

Primary **content donors** after license/version validation:

* Underground Village
* More Villages
* Deeper and Darker dungeon pack
* Simple Structures Caves

---

## 2. `LabrinthProcessorSet`

Purpose:

Transform templates at placement time.

Examples:

* cracked
* mossy
* ruined
* flooded
* overgrown
* ancient
* corrupted
* faction-controlled

Benefits:

* fewer duplicate structure files
* stronger regional identity
* faster content expansion

Primary references:

* Repurposed Structures
* StructureTutorialMod
* Minecraft structure processors

---

## 3. `LabrinthPiecePool`

Purpose:

Define weighted content pools outside giant Java switch statements.

Possible content:

```text
rooms
corridors
village houses
village civic buildings
dungeon chambers
dungeon tunnels
outpost modules
landmark modules
```

Primary references:

* Repurposed Structures
* StructureTutorialMod
* Lithostitched

---

## 4. `LabrinthCompoundAssembler`

Purpose:

Create a complete logical multi-piece structure before ordinary Labrinth content is allowed to occupy its space.

Responsibilities:

* deterministic origin
* seeded selection
* graph assembly
* piece limits
* bounding validation
* full reservation
* external connector declaration
* chunk-local materialization

Primary references:

* YUNG's Better Dungeons
* YUNG's Better Mineshafts
* YUNG's Better Strongholds
* Dungeon Crawl for roguelike pacing/room taxonomy

Primary **room donors** after license/dependency validation:

* Dungeons And Villages DeeperAndDarker
* compatstructures

---

## 5. `LabrinthNoiseVolume`

Purpose:

Generate bounded organic spaces without introducing global cave carving.

Responsibilities:

* deterministic local seed
* noise/density sampling
* volume masking
* floor/ceiling shaping
* optional liquids
* optional decoration masks
* strict reservation bounds

Primary references:

* Larion
* YUNG's Better Caves
* Minecraft carvers

Primary **cave structure donors** after license/version validation:

* Simple Structures Caves
* compatstructures

---

# Recommended Development Order

If these systems are not already implemented, the most useful order is:

```text
1. Template placement layer
2. Processor/palette layer
3. Data-driven piece pools
4. Generic compound assembler
5. Bounded cave/noise volume system
6. Migrate existing authored room families onto reusable templates where worthwhile
7. Expand villages/dungeons/landmarks using content rather than new generator code
```

This order maximizes future content velocity.

---

# Reference Usage Examples

## Task: Add ten new village houses

Consult:

1. `StructureTutorialMod`
2. `RepurposedStructures`
3. vanilla village source

Preferred solution:

1. search `Underground-Village-Multiloader-1.21.1-dev` and `More Villages v0.2.3-alpha` for legally reusable candidates;
2. migrate suitable templates into the Labrinth namespace;
3. author only the missing building types that donors do not cover;
4. assign Labrinth connector metadata;
5. add them to a village residential/profession/civic pool;
6. do not create ten new Java placement classes.

---

## Task: Add a three-floor prison dungeon

Consult:

1. `YUNGs-Better-Dungeons`
2. `YUNGs-Better-Mineshafts`
3. `YUNGs-Better-Strongholds`

Preferred solution:

* define prison piece families;
* assemble a bounded graph;
* reserve the entire compound;
* create explicit entrances;
* materialize by chunk.

---

## Task: Make caves less rectangular

Consult:

1. Larion
2. YUNG's Better Caves

Preferred solution:

* keep the cave compound reservation;
* replace/augment the internal rectangular shaping with deterministic bounded noise;
* never carve outside the reservation.

---

## Task: Make Ancient and Overgrown variants of the same chapel

Consult:

1. Repurposed Structures
2. StructureTutorialMod
3. vanilla processors

Preferred solution:

* one chapel template;
* separate processor sets/palettes;
* avoid duplicate Java logic.

---

## Task: Allow datapacks to add new Labrinth rooms

Consult:

1. Lithostitched
2. Repurposed Structures
3. StructureTutorialMod
4. vanilla codec/registry source

Preferred solution:

Expose a Labrinth-owned data definition such as:

```json
{
  "template": "labrinth:rooms/example",
  "weight": 8,
  "rarity": "common",
  "min_depth": 0,
  "max_depth": 12,
  "regions": ["labrinth:abandoned"],
  "connector_profile": "labrinth:door_3x3",
  "processors": "labrinth:abandoned_stone"
}
```

The exact schema should be designed around Labrinth, not copied from another mod.

---

## Task: Populate a giant cave with abandoned structures

Consult:

1. `Simple Structures Caves 1.21.10-11.jar`
2. `compatstructures-1.0.3.jar`
3. Larion / YUNG's Better Caves for the cave itself

Preferred solution:

* keep the cave volume owned by `LabrinthCaveCompound`;
* migrate permitted small structure templates into a cave-decoration pool;
* convert all structure data to `1.21.1`;
* replace unsupported blocks;
* position structures deterministically inside the reserved cave bounds.

---

## Task: Add a new multi-floor dungeon without building 50 rooms

Consult:

1. `Dungeons And Villages DeeperAndDarker 1.21.1.jar`
2. `YUNGs-Better-Dungeons-1.21.1`
3. `YUNGs-Better-Mineshafts-1.21.1`
4. `DungeonCrawl-neoforge-1.21` for taxonomy/pacing only unless GPL reuse is intentionally accepted

Preferred solution:

* migrate legally reusable donor rooms where possible;
* normalize palettes and connectors;
* let `LabrinthCompoundAssembler` create the dungeon graph;
* reserve the complete footprint before ordinary Labrinth generation;
* author new rooms only where donor coverage is insufficient.

---

# What Not To Do

Do not:

* copy an entire external generator into Labrinth
* attach generic Overworld cave carving to the whole dimension
* let jigsaw generation bypass Labrinth reservations
* let reference code choose ownership differently from Labrinth
* trust newer API signatures without checking `1.21.1`
* use recursive piece expansion without hard limits
* duplicate hundreds of structures when processors can create variants
* add a Java class for every new room when a reusable template can represent it
* modify anything under `References/**`
* assume public source means unrestricted redistribution
* assume a `.jar` being locally available grants asset reuse rights
* extract/reuse ARR or look-only structure assets without explicit permission
* add a third-party mod dependency merely to preserve blocks from a donor NBT
* trust `1.20.1`, `1.21.2`, `1.21.5`, `1.21.10`, or `1.21.11` structure/API data as automatically compatible with `1.21.1`

---

# Success Condition

The reference library is being used correctly when a future development task can say:

> "We need another village family, dungeon style, giant cave type, or landmark."

and the answer is mostly:

> "Add content to an existing Labrinth framework."

rather than:

> "Write another bespoke world-generation engine."
