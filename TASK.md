# TASK.md

# The Labrinth - Reference Integration and Endless Discovery Generation Overhaul

This is the active development task for **The Labrinth**, a Minecraft `NeoForge` mod targeting **Minecraft 1.21.1**.

The purpose of this task is to turn the current procedural foundation into the experience the project is actually supposed to deliver:

> **An ever-sprawling interior world where the maze itself continues indefinitely and meaningful discoveries keep appearing deeper inside it.**

The Labrinth must not feel like endless ordinary hallways with a catalogue of special content that technically exists somewhere in code.

Caves, villages, dungeons, outposts, major rooms, landmarks, hostile territories, ruins, and other discoveries must **actually generate, be reachable, survive chunk reloads, and appear often enough to matter during real exploration**.

This task supersedes previous "implemented" checkboxes for villages, dungeons, caves, outposts, and other compound content until those systems have been re-audited and proven in a fresh world.

Do not mark a feature complete merely because:

* it has a registry entry;
* it can be selected by a catalogue;
* a locator predicts a coordinate;
* a framework-free self-check passes;
* a structure class exists;
* a compound definition exists; or
* a debug command reports that it should exist.

For this task, **working means physically materialized, reachable, coherent, persistent, and playable in a freshly generated Labrinth world**.

---

# 0. REQUIRED REPOSITORY REVIEW

Before changing code:

* [ ] Read `AGENTS.md`.
* [ ] Read `README.md`.
* [ ] Read `architecture.md`.
* [ ] Read `GOAL.md`.
* [ ] Read `REFERENCE_GUIDE.md`.
* [ ] Read the latest relevant entries in `TRACELOG.md`.
* [ ] Read the latest relevant entries in `SUGGESTIONS.md`.
* [ ] Review `IDEAS.md`.
* [ ] Review the current `TASK.md` history if present.
* [ ] Inspect the actual generation, room, corridor, landmark, compound, connector, region, depth, population, loot, and debug implementations.
* [ ] Run the existing generation self-checks.
* [ ] Build the project before major changes.

Do not assume the documentation and current runtime behavior are identical.

The latest runtime implementation must be treated as something to **verify**, not merely trust.

---

# 1. PRIMARY PLAYER-FACING GOAL

The finished Labrinth should feel like a world that can be explored for an extremely long time without reducing itself to repeated hallways and rectangular rooms.

The player should encounter a hierarchy of discoveries.

## Frequent discoveries

Examples:

* unusual room geometry;
* cave pockets;
* ruined shelters;
* kitchens;
* baths;
* quarters;
* workshops;
* small shrines;
* storage rooms;
* collapsed sections;
* small hostile rooms;
* abandoned camps;
* environmental story spaces;
* tiny structures embedded in larger rooms.

## Uncommon discoveries

Examples:

* large caves;
* dining halls;
* libraries;
* stockades;
* barracks;
* churches;
* markets;
* jail blocks;
* small villages;
* compact dungeons;
* faction outposts;
* mines;
* catacomb sections;
* ruined civic spaces.

## Rare discoveries

Examples:

* large enclosed settlements;
* multi-room dungeon complexes;
* multi-floor prisons;
* major hostile strongholds;
* enormous caverns;
* grand libraries;
* massive halls;
* ancient temples;
* treasure compounds;
* large faction territories;
* ruined underground districts.

## Exceptional / landmark discoveries

Examples:

* fortress-scale compounds;
* rare multi-floor dungeons;
* enormous underground settlements;
* monumental chapels;
* ancient citadels;
* major boss or trial spaces;
* unusual portal-related structures;
* region-defining landmarks.

The deeper and farther the player explores, the **content vocabulary should broaden**.

Do not simply increase mob health or loot numbers.

Depth should increasingly unlock:

* new room families;
* more complex compound grammars;
* stranger cave types;
* harder outpost variants;
* larger settlements;
* rarer architecture;
* unusual region combinations;
* better rewards;
* stronger encounters;
* more vertical structures;
* more secrets;
* more dangerous or surreal variants.

The player should repeatedly have the experience:

> "What the hell is this place?"

---

# 2. NON-NEGOTIABLE GENERATION CONTRACT

The existing Labrinth world-generation architecture remains authoritative.

Preserve:

* deterministic world-seed generation;
* 64-block generation-cell ownership;
* canonical neighbor-edge agreement;
* chunk-local materialization;
* origin-owned multi-chunk structures;
* complete compound reservations;
* explicit external connectors;
* region and logical-depth selection;
* bounded algorithms;
* chunk-order independence;
* no recursive chunk generation;
* no unnecessary neighboring chunk loads;
* dedicated-server compatibility.

External reference mods are used to **improve or populate Labrinth systems**, not to replace the Labrinth generator.

A large discovery should conceptually follow:

```text
World seed + Labrinth coordinates
        ↓
Determine region / depth / discovery eligibility
        ↓
Choose one deterministic owner
        ↓
Assemble the complete logical discovery
        ↓
Calculate complete bounds
        ↓
Validate collisions / floor / region / depth
        ↓
Reserve the full occupied volume
        ↓
Declare intentional Labrinth entrances / exits
        ↓
Generate ordinary Labrinth around the reservation
        ↓
Materialize only the current chunk's intersecting content
        ↓
Populate owned entities / loot exactly once
```

Never solve missing structures by pasting them over an already generated maze.

The maze must appear to have grown **around** major discoveries.

---

# 3. PHASE ONE - ESTABLISH THE TRUTH ABOUT CURRENT GENERATION

Before importing large amounts of new content, determine what is actually broken.

The current project documentation describes villages, two dungeon scales, enormous caves, large compounds, and multiple hostile outposts as implemented.

Treat all of those as **UNVERIFIED** until this phase is complete.

## 3.1 Build a Runtime Discovery Audit

For every current special-content family, record:

```text
registered?
eligible?
selected?
locator agrees?
owner determined?
reservation created?
materialized?
entrance materialized?
reachable from Labrinth?
interior coherent?
entities populated?
loot populated?
survives reload?
same under different chunk order?
```

Audit at minimum:

* villages;
* compact dungeons;
* dungeon complexes;
* enormous caves;
* massive halls;
* zombie outposts;
* skeleton outposts;
* illager outposts;
* piglin outposts;
* wither-skeleton outposts;
* each registered landmark family.

## 3.2 Fresh-World Validation

Use multiple fresh worlds.

Do not validate only an existing save because previously generated chunks are not rewritten.

For representative discoveries:

1. use the current locator;
2. travel or teleport near the destination without manually placing the structure;
3. allow the relevant chunks to generate;
4. confirm the predicted structure physically exists;
5. inspect its intended entrance;
6. confirm surrounding Labrinth geometry connects correctly;
7. enter and traverse it;
8. leave the area;
9. reload the chunks;
10. return and verify the structure, entities, and loot state remain correct.

## 3.3 Fix Root Causes Before Content Expansion

If any category is not materializing reliably, identify the failure layer:

```text
selection
reservation
bounds
floor selection
region gate
depth gate
owner calculation
chunk intersection
materializer
connector routing
template placement
population
loot initialization
version/data mismatch
```

Do not hide broken selection with artificially huge spawn rates.

Do not proceed to mass donor migration until the core discovery pipeline can reliably materialize a test structure.

---

# 4. PHASE TWO - REFERENCE LIBRARY AUDIT AND EXTRACTION PLAN

The entire current `References/` library must be considered during this task.

Everything remains READ-ONLY.

Reference JARs may be unpacked into a temporary research directory where allowed by `AGENTS.md`, but never modified in place.

Before direct source or asset reuse, verify the exact local license.

Create `REFERENCE_IMPORTS.md` if any third-party structure, code, algorithm implementation, or asset is directly incorporated.

For each direct import record:

```text
source reference
source version
original path
license
Labrinth destination
changes made
DataVersion conversion
foreign block substitutions
foreign entity substitutions
connector conversion
processor/palette conversion
attribution requirements
```

## 4.1 Reference Integration Matrix

### `Minecraft_Client_Source_1.21.1`

Use as the primary API and data-format authority for:

* `StructureTemplate`;
* jigsaw/template pools;
* processors;
* bounding boxes;
* rotations;
* village structures;
* strongholds;
* mineshafts;
* ancient cities;
* trial chambers;
* structure serialization;
* chunk interaction.

### `NeoForge-1.21.11`

Use for loader, registry, datagen, event, and worldgen integration patterns.

It is not automatically API-compatible with the project's target version.

Translate everything back to the actual configured Labrinth NeoForge/Minecraft 1.21.1 API.

### `StructureTutorialMod-1.21.11-Neoforge-Jigsaw`

Use heavily for:

* NBT structure authoring;
* template loading;
* jigsaw markers;
* fallback pools;
* weighted template pools;
* processors;
* structure markers;
* modular building composition.

Use it to help implement Labrinth-owned template infrastructure.

### `RepurposedStructures-1.21.5-MDG`

Use heavily for:

* large structure libraries;
* pool organization;
* processor lists;
* palette variation;
* theme variants;
* data-driven structure content;
* maintaining many structure types without massive Java registries.

### `lithostitched-1.21.2`

Use for:

* data-driven worldgen architecture;
* registries/codecs;
* pool manipulation concepts;
* reusable worldgen utilities;
* spatial/bounding lookup patterns;
* future datapack extensibility.

Do not add complex spatial indexing unless current profiling justifies it.

### `larion-world-generation-main`

Use for:

* density functions;
* noise composition;
* domain warping;
* irregular volumes;
* organic cave shaping;
* large natural chamber geometry.

Use its concepts to improve bounded Labrinth cave volumes, not to replace the dimension generator.

### `YUNGs-Better-Caves-1.21.1`

Use for:

* cave/carver architecture;
* cavern layers;
* noise ranges;
* separating sampling from carving;
* configurable cave shapes;
* Minecraft 1.21.1 cave implementation patterns.

### `Simple Structures Caves 1.21.10-11.jar`

After license and version validation, inspect for potential donor structures such as:

* cave shelters;
* miner camps;
* cave shrines;
* small ruins;
* tiny dungeons;
* rest areas;
* abandoned cave structures;
* environmental storytelling pieces.

Convert any permitted donor structure to Minecraft 1.21.1 before use.

### `YUNGs-Better-Dungeons-1.21.1`

Use as a primary architectural reference for:

* assembling a complete dungeon graph before placement;
* room/tunnel families;
* bounded dungeon assembly;
* child-piece expansion;
* separation of layout from materialization.

### `YUNGs-Better-Mineshafts-1.21.1`

Use as a primary reference for:

* bounded branching;
* turns;
* intersections;
* stairs;
* side rooms;
* terminal pieces;
* branch depth;
* maximum chain length;
* bounding-box validation;
* piece placement failure handling.

### `YUNGs-Better-Strongholds-1.21.1`

Use for:

* rare large compounds;
* multi-level landmarks;
* hubs;
* fortress-scale layouts;
* libraries;
* prisons;
* chapels;
* multi-floor structure composition;
* major navigable interior networks.

### `Dungeons And Villages DeeperAndDarker 1.21.1.jar`

Treat as a high-priority underground dungeon donor candidate after exact license verification.

Inspect for:

* chambers;
* hallways;
* intersections;
* stair rooms;
* treasure rooms;
* trap rooms;
* large rooms;
* terminal rooms;
* multi-floor pieces.

Do not add Deeper and Darker as a required Labrinth dependency merely to preserve donor blocks.

Replace foreign content with vanilla or Labrinth-owned equivalents.

### `DungeonCrawl-neoforge-1.21`

Use primarily for architecture and pacing unless direct GPL reuse is intentionally accepted.

Study:

* multi-floor roguelike pacing;
* room-category ratios;
* branch termination;
* dungeon density;
* reward-room frequency;
* stair frequency;
* repeated room variation;
* large-dungeon progression.

### `Underground-Village-Multiloader-1.21.1-dev`

Treat as the first village donor candidate after local license verification.

Inspect for:

* town centers;
* meeting points;
* streets;
* corners;
* crossroads;
* terminators;
* houses;
* butcher shops;
* smiths;
* fletchers;
* shepherds;
* armorers;
* fishers;
* tanneries;
* cartographers;
* libraries;
* masons;
* weaponsmiths;
* temples;
* stables;
* farms;
* animal pens.

The building library is more valuable than its original world-placement system.

### `More Villages v0.2.3-alpha`

Use as a secondary village donor candidate.

Use it to broaden the visual vocabulary beyond one donor project.

Inspect:

* residential buildings;
* profession buildings;
* civic structures;
* farms;
* decorative settlement pieces;
* roads/paths;
* alternate palettes.

### `compatstructures-1.0.3.jar`

Treat as conditional until the exact local license is verified.

Inspect for:

* mines;
* catacombs;
* cave structures;
* puzzle rooms;
* treasure chambers;
* utility rooms;
* unusual underground structures;
* structure integration examples.

Do not add unrelated dependency mods merely to preserve donor blocks.

### `mostructures-1.21.x`

Treat as conditional/copyleft according to the current reference policy.

Use primarily for:

* outpost concepts;
* hostile compounds;
* markets;
* fortified rooms;
* castles;
* barns;
* landmark silhouettes;
* small dungeon concepts.

Direct reuse requires explicit license compatibility review.

### `adventuredungeons-neoforge-1.21-1.3.1.jar`

LOOK-ONLY unless explicit permission says otherwise.

Study only:

* dungeon themes;
* trial pacing;
* underground camps;
* treasure placement;
* dungeon silhouettes;
* room-category ideas;
* labyrinth-style encounter design.

Do not extract or adapt restricted assets.

### `claustrophobic_dungeons-1.0.1-forge-1.20.1.jar`

LOOK-ONLY.

It also targets Minecraft 1.20.1.

Study only:

* oppressive corridor density;
* narrow proportions;
* secrets;
* traps;
* hidden rooms;
* claustrophobic dungeon pacing;
* composition ideas visible through play/design.

Do not use it as a 1.21.1 API or NBT authority.

---

# 5. PHASE THREE - BUILD THE REUSABLE CONTENT PIPELINE

Before copying dozens of structures into Labrinth, make sure imported/authored content has a clean home.

Inspect current equivalents first.

Do not duplicate systems that already exist.

Where missing, implement or extend the following concepts.

## 5.1 `LabrinthTemplatePiece`

A room, building, dungeon module, outpost module, or cave structure should be able to use an authored `.nbt` template.

Support metadata such as:

```text
id
template
bounds
weight
rarity
minDepth
maxDepth
allowedRegions
allowedFloors
connectors
rotationRules
processorSet
lootProfile
populationProfile
placementConditions
tags / categories
```

## 5.2 Connector Normalization

Imported jigsaw/structure markers must not automatically become arbitrary world connections.

Translate useful donor markers into Labrinth connector metadata.

Distinguish:

```text
INTERNAL STRUCTURE CONNECTION
```

from:

```text
EXTERNAL LABRINTH CONNECTION
```

The surrounding maze may connect only through explicit external connectors.

## 5.3 `LabrinthProcessorSet`

Support reusable transformation layers where practical:

* standard;
* abandoned;
* damaged;
* overgrown;
* flooded;
* ancient;
* corrupted;
* frozen;
* faction-controlled;
* Nether-influenced.

Prefer transforming one good structure over storing many near-identical copies.

## 5.4 `LabrinthPiecePool`

Structure content should be grouped into reusable weighted pools.

Examples:

```text
village/residential
village/profession
village/civic
village/streets
village/markets
dungeon/entrances
dungeon/travel
dungeon/intersections
dungeon/combat
dungeon/treasure
dungeon/traps
dungeon/vertical
dungeon/terminal
cave/shelters
cave/ruins
cave/decorations
outpost/guard
outpost/barracks
outpost/storage
outpost/defense
landmark/modules
```

## 5.5 Import Validation

Add development validation for imported templates.

Check:

* structure DataVersion;
* unsupported blocks;
* unsupported block entities;
* unsupported entities;
* donor namespaces;
* foreign loot tables;
* foreign processors;
* foreign jigsaw pool references;
* bounds;
* connector alignment;
* rotation;
* world-height fit.

Do not silently place `air` where a missing mod block was expected.

Foreign dependencies must be deliberately mapped.

---

# 6. PHASE FOUR - CAVE GENERATION OVERHAUL

Current cave content must become actual explorable cave spaces, not scattered cave decoration or rectangular rooms with stone.

Use:

* `larion-world-generation-main`;
* `YUNGs-Better-Caves-1.21.1`;
* `Minecraft_Client_Source_1.21.1`;
* `Simple Structures Caves 1.21.10-11.jar`;
* `compatstructures-1.0.3.jar`.

## 6.1 Bounded Cave Volumes

Create or improve a Labrinth-owned bounded noise-volume system.

A cave compound should:

1. reserve a complete 3D volume;
2. derive its own deterministic cave seed;
3. generate irregular walls, ceiling, floor, ledges, columns, pits, and chambers;
4. remain entirely inside its reservation;
5. expose explicit Labrinth connectors;
6. preserve walkable routes between intended entrances;
7. allow deterministic cave structures/decorations inside the same reservation.

## 6.2 Cave Scales

Support at minimum:

* small cave pocket;
* medium cavern;
* large cave chamber;
* rare enormous cavern.

Large/rare caves should meaningfully interrupt the normal corridor rhythm.

## 6.3 Cave Families

Support distinct cave families such as:

* ordinary stone cavern;
* ore-bearing cave;
* flooded cavern;
* overgrown grotto;
* spider cavern;
* ancient ruined cavern;
* corrupted cavern;
* collapsed cave;
* underground lake chamber.

Do not make these simple block-palette swaps.

Their silhouettes, traversal, decoration density, hazards, and contained structures should differ.

## 6.4 Cave Discoveries

Populate appropriate cave compounds using legally reusable donor structures where possible:

* shelters;
* abandoned mining stops;
* ruins;
* camps;
* shrines;
* tiny dungeons;
* caches;
* environmental story structures.

These should be placed deterministically **inside the owned cave volume**.

---

# 7. PHASE FIVE - DUNGEON GENERATION OVERHAUL

Use:

* `YUNGs-Better-Dungeons-1.21.1`;
* `YUNGs-Better-Mineshafts-1.21.1`;
* `YUNGs-Better-Strongholds-1.21.1`;
* `Dungeons And Villages DeeperAndDarker 1.21.1.jar`;
* `DungeonCrawl-neoforge-1.21`;
* `compatstructures-1.0.3.jar`;
* vanilla Minecraft structure source;
* `adventuredungeons...` and `claustrophobic_dungeons...` for look-only design study.

## 7.1 Dungeon Grammar

Dungeon complexes should be assembled logically before placement.

A generic grammar may contain:

```text
ENTRANCE
    ↓
TRAVEL
    ↓
BRANCH / INTERSECTION
    ↓
COMBAT / UTILITY / PUZZLE / PRISON / STORAGE
    ↓
OPTIONAL SECRET
    ↓
VERTICAL TRANSITION
    ↓
DEEPER BRANCH
    ↓
TREASURE / ELITE / TERMINAL
    ↓
EXIT or intentional dead-end reward
```

Do not produce random room soup.

## 7.2 Hard Generation Budgets

Every dungeon assembly must have explicit limits such as:

```text
maxPieces
maxDepth
maxBranches
maxHorizontalRadius
maxVerticalRange
maxFailedPlacementAttempts
```

No unbounded recursive generation.

## 7.3 Dungeon Scales

Support:

### Compact

* one to three encounter spaces;
* short side dungeon;
* minor reward.

### Standard

* several rooms;
* branches;
* one or more special rooms;
* possible vertical change.

### Major Complex

* many internal pieces;
* distinct phases;
* prison/barracks/storage/treasury/etc.;
* multiple branches;
* possible multiple floors;
* clear entrance and exit logic.

### Rare Mega Dungeon

Use sparingly.

May include:

* many rooms;
* multiple floors;
* hub spaces;
* large chambers;
* secrets;
* elite rooms;
* final reward spaces.

This should be a major discovery, not normal background generation.

## 7.4 Donor Room Migration

Where licensing permits, migrate useful Deeper and Darker dungeon templates into Labrinth-owned pools.

Replace:

* Deeper and Darker blocks;
* foreign loot;
* foreign entities;
* foreign processor references.

Preserve useful geometry.

Do not preserve foreign dependencies simply for donor assets.

## 7.5 Dungeon Quality Rules

A dungeon must:

* have an intentional threshold from the Labrinth;
* not be pierced by unrelated corridors;
* not expose jail cells or treasury walls as accidental entrances;
* be traversable;
* have coherent progression;
* contain meaningful encounter/reward variation;
* survive reload unchanged;
* materialize correctly across chunks.

---

# 8. PHASE SIX - VILLAGE AND SETTLEMENT OVERHAUL

Use:

* `Underground-Village-Multiloader-1.21.1-dev`;
* `More Villages v0.2.3-alpha`;
* `StructureTutorialMod`;
* `RepurposedStructures`;
* `YUNGs-Better-Strongholds`;
* `YUNGs-Better-Mineshafts`;
* vanilla village source.

The Labrinth village must feel like a settlement that **grew inside the maze**.

Do not paste an outdoor village underground.

## 8.1 Village Component Pools

Create reusable pools such as:

```text
town centers
streets
crossroads
terminators
small houses
large houses
profession buildings
libraries
temples
markets
workshops
farms
stables
animal pens
storage
guard posts
communal dining
utility spaces
```

Use legal donor templates first where they are appropriate.

Author new structures only where donor coverage is inadequate or the Labrinth needs a stronger identity.

## 8.2 Settlement Shapes

Support more than one settlement topology.

Examples:

### Chamber Village

One enormous enclosed chamber containing multiple buildings.

### Street Village

A network of internal streets and buildings occupying several cells.

### Multi-Chamber Settlement

Several major chambers connected by settlement-owned passages.

### Vertical Settlement

Rare settlement using more than one floor.

## 8.3 Functional Population

Validate:

* villagers spawn once;
* sensible villager counts;
* beds;
* job-site blocks;
* professions;
* trading;
* pathfinding between important settlement areas;
* no population duplication after reload;
* no duplicate population when multiple players approach from different directions.

## 8.4 Village Variants

Build the system so later variants can include:

* normal villager settlement;
* ruined village;
* abandoned village;
* undead settlement;
* illager occupation;
* witch settlement;
* Piglin settlement;
* Ancient-region village;
* Overgrown settlement;
* Flooded settlement.

Do not implement every possible faction immediately if it would destabilize the core system.

Prioritize a robust framework and several high-quality examples.

---

# 9. PHASE SEVEN - HOSTILE OUTPOSTS AND OCCUPIED TERRITORIES

Use:

* `mostructures-1.21.x`;
* `compatstructures-1.0.3.jar`;
* `YUNGs-Better-Dungeons`;
* `YUNGs-Better-Strongholds`;
* vanilla hostile structures;
* look-only dungeon references for encounter pacing.

Outposts must become more than one room with a different mob list.

Initial factions remain:

* Zombie;
* Skeleton;
* Illager;
* Piglin;
* Wither Skeleton.

Each faction should have its own architectural grammar.

## Zombie

Favor:

* ruined living spaces;
* barricades;
* infected quarters;
* storage remnants;
* broken routes;
* dense close-range combat.

## Skeleton

Favor:

* long sightlines;
* firing platforms;
* defensive lanes;
* arrow supplies;
* elevated positions;
* choke points.

## Illager

Favor:

* organized fortification;
* guard rooms;
* barracks;
* banners;
* storage;
* patrol loops;
* prison/holding spaces.

## Piglin

Favor:

* Nether architectural accents;
* defended storage;
* gold-related visual language;
* enclosed bastion-like sections;
* faction-appropriate mob behavior.

## Wither Skeleton

Favor:

* fortress-like rooms;
* difficult choke points;
* dark large chambers;
* stronger rewards;
* rare placement;
* dangerous deep-region weighting.

Support small outposts and rarer multi-room faction compounds.

---

# 10. PHASE EIGHT - MAJOR ROOM AND MICRO-DISCOVERY EXPANSION

Use the donor library aggressively where legally allowed.

Expand beyond the current room catalogue.

Priority categories include:

* ballrooms;
* kitchens;
* baths;
* butcher rooms;
* workshops;
* mines;
* catacombs;
* crypts;
* ruined shelters;
* abandoned camps;
* laboratories/utility spaces where region-appropriate;
* storage vaults;
* large dining halls;
* prisons;
* guard stations;
* shrines;
* temples;
* hidden rooms;
* ore caves;
* treasury variants;
* libraries;
* civic spaces;
* ruined markets;
* giant support halls;
* bridges;
* balconies;
* vertical rooms;
* unusual dead ends;
* secret passage destinations.

A content category is not complete because one rectangular version exists.

Use:

* multiple silhouettes;
* multiple connector patterns;
* multiple scales;
* processor variants;
* region weighting;
* depth weighting;
* damage/occupation states;
* contained structures;
* decoration variation.

---

# 11. PHASE NINE - ENDLESS DISCOVERY ECOLOGY

This is one of the most important parts of the task.

The Labrinth must not merely *support* special content.

It must distribute discoveries in a way that keeps exploration compelling.

## 11.1 Discovery Tiers

Introduce or formalize discovery categories such as:

```text
MICRO
COMMON
UNCOMMON
RARE
MAJOR
LEGENDARY
```

These are not required to be literal enum names if the current rarity model already supports equivalent behavior.

Use the existing rarity/depth/region architecture where possible.

## 11.2 Prevent Long Content Deserts

Do not allow normal generation to accidentally create enormous distances containing only interchangeable hallways and generic rooms unless a deliberately sparse region calls for it.

Add deterministic sampling/debug statistics that can answer:

* expected distance between micro discoveries;
* expected distance between uncommon rooms;
* expected distance between compounds;
* expected distance between villages;
* expected distance between dungeons;
* expected distance between major landmarks;
* content distribution by region;
* content distribution by logical depth;
* content distribution by floor.

Tune generation based on measured fresh-world output instead of intuition alone.

## 11.3 Depth Unlocks Variety

As logical depth increases, progressively introduce:

* new room pools;
* new dungeon room families;
* larger dungeons;
* rarer cave types;
* rare settlement types;
* stronger outposts;
* stranger processors;
* deeper-region landmarks;
* more unusual layouts.

Do not make the deepest areas simply use the same buildings with stronger mobs.

## 11.4 Region Identity

Use processors, palettes, room pools, structures, and population to make:

* Abandoned;
* Industrial;
* Flooded;
* Overgrown;
* Ancient;
* Corrupted

feel meaningfully different.

The same donor structure may be transformed for multiple regions only when the transformed result still feels intentional.

---

# 12. PHASE TEN - LOOT, POPULATION, AND ENCOUNTER RELIABILITY

This task is primarily about generation, but discoveries are not meaningful if they are empty or broken.

## 12.1 Loot

Add authored Labrinth loot tables where practical for:

* villages;
* compact dungeons;
* major dungeons;
* outposts;
* treasuries;
* cave caches;
* rare landmarks.

Risk and rarity should influence rewards.

Avoid hardcoded chest inventories when loot tables fit.

## 12.2 Population

All structure-owned population must be safe against:

* chunk reload;
* server restart;
* multiple players approaching;
* different chunk order;
* intersecting-chunk materialization.

## 12.3 Intelligent Hostile Placement

Use room/compound metadata to place hostile content deliberately.

Do not scatter spawners or mobs without considering:

* combat space;
* sightlines;
* entrances;
* chokepoints;
* room purpose;
* faction;
* depth;
* region.

---

# 13. PHASE ELEVEN - DEBUG AND GENERATION INSPECTION TOOLS

The existing locator is useful but is not enough by itself.

Extend debugging so generation can be proven quickly.

Useful commands/tools may include:

```text
/labrinth locate <type>
/labrinth locate all
/labrinth inspect
/labrinth validate <type>
/labrinth stats <radius>
```

Exact command names may differ to fit the existing command architecture.

Development tooling should be able to report:

* selected owner;
* compound type;
* variant;
* floor;
* logical depth;
* region;
* bounds;
* external connectors;
* intersecting chunks;
* materialization status where safely observable;
* template/pool selection;
* population owner.

Debug tools must never change normal deterministic generation.

Do not force-load huge areas merely to inspect them.

---

# 14. PHASE TWELVE - AUTOMATED AND MANUAL VALIDATION

This task is not complete until the generated world has been physically verified.

## 14.1 Add Focused Game / Integration Tests Where Practical

Prioritize tests that:

* run against a fresh generated Labrinth;
* use the same locator logic as runtime;
* materialize the reported destination;
* confirm the intended entrance exists;
* confirm the compound shell exists;
* confirm ordinary content yields;
* reload chunks;
* verify the structure remains consistent.

Add representative coverage for:

* cave compound;
* village;
* compact dungeon;
* major dungeon;
* each outpost faction;
* major landmark.

## 14.2 Connector Integrity

Verify:

* no village walls pierced by unrelated corridors;
* no dungeon walls pierced;
* no accidental entrance into cells/prisons;
* no treasury wall penetration;
* no ordinary room overlap;
* unused connectors seal correctly;
* intended external connectors align;
* multi-floor exits/entrances align.

## 14.3 Coordinates

Test:

* positive X/Z;
* negative X;
* negative Z;
* negative X and Z;
* large distance;
* shallow depth;
* deep logical depth;
* all active floors;
* near world-height limits.

## 14.4 Chunk Order

For representative multi-chunk discoveries:

* approach from different directions;
* generate different intersecting chunks first;
* reload;
* restart the server;
* compare results.

Same seed + same coordinates must produce the same discovery.

## 14.5 Dedicated Server

Run the dedicated server.

Test multiple players approaching the same compound.

Verify:

* no duplicate structures;
* no duplicate villagers;
* no duplicate hostile populations;
* no repeated loot initialization;
* no client-only dependencies.

---

# 15. PERFORMANCE REQUIREMENTS

The Labrinth contains significantly more enclosed geometry than normal Minecraft terrain.

Do not trade discoverability for runaway generation cost.

Avoid:

* scanning large numbers of loaded/unloaded chunks;
* forcing neighbors for validation;
* unbounded flood fills;
* unbounded recursive piece graphs;
* repeatedly parsing structure files during generation;
* recalculating complete compounds independently per intersecting chunk;
* excessive block entities;
* enormous persistent mob populations;
* per-tick worldgen searches.

Prefer:

* preloaded/cached templates;
* bounded layout assembly;
* deterministic seeds;
* cheap owner reconstruction;
* chunk intersection tests;
* reusable processors;
* data-driven pools;
* compact immutable metadata.

Profile major generation changes when practical.

---

# 16. IMPLEMENTATION ORDER

Follow this order unless repository inspection proves a different dependency is required.

## Step 1

Audit the current runtime and prove which existing discovery systems actually materialize.

## Step 2

Fix any remaining seed, owner, reservation, materialization, or connector mismatch.

## Step 3

Inventory all 19 reference entries and identify:

```text
architecture references
permitted donor candidates
conditional/copyleft references
look-only references
version-mismatched data
```

## Step 4

Implement/finish template import, connector normalization, processors, and reusable piece pools.

## Step 5

Create one imported or authored test template and prove it works through Labrinth ownership across chunk boundaries.

## Step 6

Overhaul cave compounds using bounded noise and cave-contained discovery pools.

## Step 7

Overhaul dungeon generation using complete logical assembly and reusable dungeon piece pools.

## Step 8

Overhaul enclosed villages using donor building/street pools under Labrinth compound ownership.

## Step 9

Overhaul faction outposts into distinct hostile compounds.

## Step 10

Expand major rooms and micro-discovery content.

## Step 11

Tune region/depth/rarity distribution so discovery remains compelling at long distances.

## Step 12

Add/finish loot, population, and encounter reliability.

## Step 13

Add generation statistics and focused automated tests.

## Step 14

Run fresh-world visual traversal.

## Step 15

Profile, build, run dedicated server, update docs, version, trace, and suggestions.

Do not skip the visual traversal step.

---

# 17. COMPLETION GATES

A category may only be marked complete when it passes all applicable gates.

## Registered

The content definition exists.

## Selectable

The deterministic generator can legitimately choose it.

## Locatable

Debug lookup predicts the same location/type used by live generation.

## Reserved

Its complete bounds are owned and protected before normal content placement.

## Materialized

The structure physically appears in newly generated chunks.

## Connected

The intended Labrinth entrance is physically reachable and correctly aligned.

## Traversable

The player can actually move through the intended structure.

## Populated

Expected villagers, mobs, containers, or markers appear exactly once.

## Persistent

Reloading chunks/world/server does not duplicate or remove the discovery.

## Chunk-Order Safe

Approaching from another direction does not change it.

## Player-Ready

The result looks and plays like an intentional Labrinth discovery rather than test geometry.

Only then mark it complete.

---

# 18. DEFINITION OF DONE

This task is complete when all of the following are true:

* [ ] The project builds successfully.
* [ ] Dedicated-server startup succeeds.
* [ ] Existing ordinary rooms/corridors/vertical generation still works.
* [ ] The world remains deterministic and chunk-order independent.
* [ ] No reference file under `References/**` was modified.
* [ ] Every directly reused donor asset has an explicit license/source record.
* [ ] Imported structure data is valid for Minecraft 1.21.1.
* [ ] Foreign dependency blocks/entities are deliberately replaced or supported.
* [ ] Caves physically generate as bounded, natural-looking explorable compounds.
* [ ] Cave compounds can contain deterministic structures/points of interest.
* [ ] Villages physically generate and are reachable through intentional Labrinth entrances.
* [ ] Village buildings use a reusable pool rather than one hard-coded settlement.
* [ ] Villagers, beds, job sites, and population persistence are validated.
* [ ] Compact dungeons physically generate.
* [ ] Multi-room dungeons physically generate.
* [ ] At least one dungeon can span multiple chunks without penetration or duplication.
* [ ] Dungeon entrances/exits are deliberate and traversable.
* [ ] Zombie outposts physically generate.
* [ ] Skeleton outposts physically generate.
* [ ] Illager outposts physically generate.
* [ ] Piglin outposts physically generate.
* [ ] Wither-skeleton outposts physically generate.
* [ ] Outpost factions have meaningfully different layouts/encounter design.
* [ ] Major-room variety has expanded through reusable templates/pools/processors.
* [ ] Region and logical depth materially influence discoverable content.
* [ ] Fresh-world generation statistics show special content is actually discoverable.
* [ ] Long exploration does not collapse into only generic corridors/rooms.
* [ ] Representative discoveries survive chunk reload and server restart.
* [ ] Representative discoveries remain identical under different chunk-generation order.
* [ ] Automated discovery/materialization tests exist where practical.
* [ ] Fresh-world manual visual traversal has been completed.
* [ ] `README.md` reflects actual verified behavior.
* [ ] `architecture.md` is updated if generation contracts changed.
* [ ] `REFERENCE_IMPORTS.md` exists if donor assets/code were directly incorporated.
* [ ] `TRACELOG.md` documents the implementation.
* [ ] `SUGGESTIONS.md` contains related future improvements.
* [ ] `build.gradle` version is incremented.

---

# 19. FINAL IMPLEMENTATION REPORT

When the task is complete, report:

1. Current-generation failures discovered.
2. Root causes fixed.
3. Reference projects inspected.
4. Donor assets incorporated.
5. Licenses and attribution recorded.
6. Template/pool/processor infrastructure created or extended.
7. Cave-generation changes.
8. Dungeon-generation changes.
9. Village-generation changes.
10. Outpost-generation changes.
11. Major room/discovery additions.
12. Region/depth distribution changes.
13. Loot/population changes.
14. Debugging/statistics tools added.
15. Automated tests added.
16. Fresh-world tests performed.
17. Dedicated-server result.
18. Performance observations.
19. Build result.
20. Remaining known limitations.

Do not merely describe how this work could be implemented.

**Implement it in the repository and prove it in a freshly generated Labrinth.**

---

# Final Design Standard

The purpose of this work is not to make The Labrinth a collection of imported Minecraft structures.

The purpose is to use proven open/reference work to rapidly build a much richer **Labrinth-owned procedural ecosystem**.

A player should be able to travel for thousands upon thousands of blocks and continue finding:

```text
another hallway
another branch
another floor
another ruin
another cave
another settlement
another dungeon
another outpost
another impossible room
another landmark
another thing they have not seen before
```

The system should be designed so adding future content increasingly means:

> **Add another discovery to the Labrinth's existing pools.**

not:

> **Write another world generator.**
