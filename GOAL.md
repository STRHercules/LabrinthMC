# The Labrinth: Villages, Dungeons, Large Structures, and Major Room Expansion

Read the repository and existing project documentation before modifying anything.

At minimum, review:

* `AGENTS.md`
* `README.md`
* `architecture.md`
* `TASK.md`
* the existing generation, room, landmark, connector, region, depth, placement, and population systems
* existing debug/testing utilities

Do not redesign systems that already exist unless the current architecture genuinely cannot support this work.

This task must extend the existing Labrinth generation architecture rather than creating a second unrelated world-generation system.

The Labrinth already uses deterministic generation cells, chunk-local materialization, weighted rooms, regions, depth, connectors, collision validation, multi-floor generation, and origin-owned landmarks. Reuse and extend those systems.

---

# Primary Goal

Significantly expand the kinds of spaces that can generate inside the Labrinth by implementing:

1. **Bespoke enclosed Labrinth villages / settlements**
2. **Dungeon generation**
3. **A much larger room catalogue**
4. **Monster-controlled outposts**
5. Any reusable generation infrastructure necessary to support these structures cleanly

These additions must feel like parts of the Labrinth rather than vanilla structures pasted into the dimension.

---

# CRITICAL GENERATION RULE: STRUCTURES OWN THEIR SPACE

Large structures such as villages, dungeon complexes, large caves, massive rooms, and other multi-cell structures must be accounted for by the procedural layout BEFORE ordinary corridors and rooms are allowed to occupy their footprint.

Do **not**:

* generate the normal Labrinth and carve a dungeon into it afterward;
* paste structures over existing rooms;
* allow random hallways to penetrate a dungeon wall;
* allow ordinary rooms to partially overlap a special structure;
* allow corridors to connect anywhere except an explicitly defined entrance or exit;
* depend on chunk exploration order to determine whether a structure exists.

Instead, extend the existing deterministic landmark / bounding ownership concepts where appropriate.

A special structure should conceptually work like:

```text
Determine deterministic structure origin
        ↓
Determine full reserved bounding volume
        ↓
Determine structure rotation / variant / region / depth eligibility
        ↓
Determine intentional entrances and exits
        ↓
Reserve structure footprint
        ↓
Generate surrounding Labrinth around that reservation
        ↓
Connect normal Labrinth paths ONLY to declared structure connectors
        ↓
Materialize each intersecting chunk from the same deterministic decision
```

Ordinary Labrinth pieces must yield to reserved structure bounds.

The result should look as though the maze was constructed **around** the village or dungeon.

There must be no accidental doors, openings, tunnels, room mergers, or hallway penetrations.

---

# 1. REUSABLE LARGE-STRUCTURE / RESERVED-FOOTPRINT SUPPORT

Before implementing one-off hacks for villages or dungeons, inspect the existing landmark system.

If the existing landmark abstraction already supports everything required, extend it cleanly.

If it does not, create the smallest reusable abstraction necessary for structures that:

* occupy multiple rooms/cells/chunks;
* may span multiple floors;
* reserve a deterministic bounding volume;
* have one deterministic owner/origin;
* expose explicit connectors;
* require surrounding generation to yield;
* have rarity;
* have region restrictions;
* have depth restrictions;
* have elevation/floor restrictions;
* can contain internal sub-rooms;
* can contain entities;
* can contain loot;
* can define their own internal layout;
* can have multiple variants.

Do not create separate duplicate implementations for:

```text
VillageReservation
DungeonReservation
CaveReservation
MassiveRoomReservation
etc.
```

Prefer one reusable special-structure/compound-piece mechanism if that naturally fits the architecture.

Keep the core generator modular.

---

# 2. LABRINTH VILLAGES

Implement villages as **bespoke enclosed settlements designed specifically for the Labrinth**.

Do not attempt to place a normal outdoor plains/desert/etc. vanilla village underground.

The settlement should look like a community that exists **inside the maze**.

Think of the Labrinth itself as the outside world.

A village may occupy:

* one enormous chamber;
* several connected chambers;
* multiple Labrinth cells;
* multiple floors where appropriate;
* a combination of internal streets/passages and enclosed buildings.

## Village Structure

Create a modular settlement system rather than one identical village.

Potential village components include:

* central square / meeting area;
* small homes;
* larger homes;
* workshops;
* profession buildings;
* storage rooms;
* communal dining areas;
* farms or indoor growing areas;
* wells or water sources where appropriate;
* utility areas;
* small markets;
* guard posts;
* internal alleys;
* stairways;
* balconies;
* connecting halls;
* settlement walls or structural boundaries.

Use vanilla blocks initially where practical.

Do not block this feature on future Labrinth custom blocks.

## Villager Functionality

Villages should contain actual villagers and should preserve normal useful villager gameplay where practical:

* villagers;
* beds;
* job-site blocks;
* professions;
* trading;
* villager pathfinding within the settlement;
* sensible population counts.

Avoid spawning large uncontrolled numbers of entities.

Entity creation must be deterministic or otherwise safe against duplicate generation when chunks reload.

Do not repeatedly spawn the same villagers every time a chunk materializes.

## Village Entrances

Settlements need **definitive entrances/exits**.

The outside Labrinth may connect to the settlement only through explicitly declared connectors.

Examples:

```text
Main gate
Side entrance
Maintenance passage
Vertical entrance
Rare hidden entrance
```

Do not create openings simply because an unrelated corridor happens to touch the settlement bounding box.

Prefer at least two usable connections for larger settlements where generation permits so they can become meaningful navigation locations rather than giant dead ends.

## Village Rarity

Villages should be significant discoveries.

They should be substantially rarer than ordinary rooms.

Use the existing depth, region, rarity, landmark-spacing, and deterministic selection systems.

Do not hardcode coordinate checks or use random runtime chance unrelated to the existing seed architecture.

Allow multiple settlement variants to be added later.

---

# 3. DUNGEON SYSTEM

Add Labrinth dungeons.

These should be inspired by the gameplay function of Minecraft dungeons while being architecturally integrated into the Labrinth.

Use **multiple dungeon scales controlled by rarity**.

## Tier A: Compact Dungeons

These should be the more common form.

They may be:

* one room;
* a small combat chamber;
* a two-room encounter;
* a small nest;
* a short side dungeon.

Typical contents:

* monster spawner or controlled encounter;
* 1-2 loot containers;
* environmental detail;
* minor hazards;
* combat space.

They should still have controlled entrances.

A compact dungeon must not simply overwrite whichever normal room was selected.

It should be a legitimate registered room/special-room type with its own connectors and placement requirements.

---

# Tier B: Dungeon Complexes

Create rarer, larger dungeons that may contain multiple internal rooms.

Potential internal pieces:

* entrance chamber;
* monster rooms;
* guard rooms;
* hallways;
* jail cells;
* armories;
* storage;
* barracks;
* puzzle spaces;
* traps;
* treasury;
* mini-boss rooms;
* hidden rooms;
* alternate routes;
* final reward room.

These may occupy several cells or chunks and may occasionally span multiple floors.

They must use the reserved-footprint system.

Normal Labrinth corridors must generate **around the complex**.

Only explicitly declared dungeon connectors may connect the dungeon to the Labrinth.

---

# Dungeon Entrances and Exits

This is extremely important.

A dungeon should visually read as:

```text
Labrinth corridor
      ↓
intentional dungeon doorway / gate
      ↓
Dungeon
      ↓
intentional exit
      ↓
Labrinth continues
```

NOT:

```text
Hallway → random dungeon wall
Hallway → jail cell
Room → half of dungeon
Dungeon → accidental neighboring room
```

The surrounding maze must respect the dungeon's occupied volume.

Large dungeons should generally have a clear entrance and exit, although selected variants may intentionally be dead-end challenge/reward structures.

---

# Dungeon Rarity / Difficulty

Use weighted generation and Labrinth logical depth.

As the player progresses deeper:

* dungeon variants can become more dangerous;
* larger complexes can become more likely where appropriate;
* stronger mobs can appear;
* encounter density may increase;
* loot quality can improve;
* rare rooms can become available;
* unusual dungeon themes can unlock.

Do not make every dungeon scale identically.

Allow different dungeon definitions to specify their own:

```text
weight
rarity
minimum depth
maximum depth
allowed regions
allowed floors/elevations
dimensions
required connectors
enemy profile
loot profile
hazard profile
```

---

# Dungeon Rewards

Use a **balanced reward mix**.

Dungeon rewards should include combinations of:

* useful survival resources;
* food;
* building materials;
* tools;
* weapons;
* armor;
* enchanted items/books where appropriate;
* utility items;
* potions;
* rarer resources;
* occasional high-value treasure.

Risk should correspond to reward.

A compact skeleton dungeon should not reward like a giant rare dungeon complex.

Avoid hardcoding chest inventories if the existing loot-table infrastructure can support the content.

Where the custom Labrinth loot system is not yet complete, structure the implementation so proper loot tables can replace temporary/default loot cleanly later.

---

# 4. MAJOR ROOM CATALOGUE EXPANSION

Expand the registered room catalogue significantly.

Do not implement these as superficial palette swaps of one rectangular room.

Where practical, give each category:

* distinctive dimensions;
* distinctive geometry;
* appropriate connector counts;
* decoration rules;
* region/depth eligibility;
* rarity;
* internal content;
* optional variants.

Reuse existing room metadata and registration systems.

Implement the following room families.

---

## Caves

Large natural-looking stone chambers embedded inside the constructed Labrinth.

Possible characteristics:

* irregular stone walls;
* large open ceilings;
* rock formations;
* ledges;
* pits;
* occasional water;
* natural ore exposure where appropriate;
* paths crossing the chamber;
* multiple entrances.

These still require a bounded procedural footprint.

Do not use uncontrolled vanilla cave carving that destroys neighboring Labrinth pieces.

Create Labrinth-owned cave chambers.

Include multiple sizes, including rare enormous caves.

---

## Jungle Rooms

Large overgrown enclosed environments.

Potential content:

* dirt/grass areas;
* vines;
* leaves;
* trees where clearance permits;
* moss;
* water;
* ruined architecture beneath vegetation;
* elevated paths;
* dense vegetation pockets.

These should naturally favor the Overgrown region but may appear elsewhere at lower weights if appropriate.

---

## Spider Nests

Hostile organic-feeling rooms.

Potential content:

* cobwebs;
* spider / cave spider encounters;
* dark recesses;
* broken structures;
* egg/nest-like environmental clusters using available blocks;
* loot caught within the nest;
* narrow side pockets.

Support compact nests and rarer large nests.

---

## Quarters

Former living quarters.

Create several variants such as:

* small worker quarters;
* shared sleeping rooms;
* officer quarters;
* ruined quarters.

Possible contents:

* beds;
* storage;
* tables;
* personal belongings;
* minor loot.

---

## Barracks

Larger military-style sleeping and equipment spaces.

Possible contents:

* rows of beds;
* weapon/storage areas;
* tables;
* armor stands where appropriate;
* patrol or hostile occupation variants.

---

## Stockades

Contain fenced/caged holding areas.

Potential layouts:

* central holding pen;
* multiple pens;
* guard area;
* storage;
* execution/processing area where thematically appropriate.

---

## Dining Halls

Large communal spaces.

Potential contents:

* long tables;
* benches;
* kitchen/service area;
* storage;
* ruined feast variants;
* abandoned variants;
* occupied variants.

---

## Libraries

Create library/archive-style rooms beyond any existing basic archive implementation.

Variants may include:

* small reading room;
* traditional library;
* multi-level library;
* ruined library;
* enormous rare library.

Potential contents:

* bookshelves;
* reading tables;
* lecterns;
* hidden passages;
* knowledge-themed loot;
* rare enchanted-book rewards where appropriate.

---

## Treasury Rooms

Abandoned treasure rooms.

These should be uncommon or rare.

Potential contents:

* multiple containers;
* vault-like architecture;
* guarded treasure;
* traps;
* decorative valuables;
* hidden caches.

Loot must scale with rarity and danger.

Do not turn these into free high-tier loot rooms.

---

## Frozen Rooms

Cold/frozen anomalous chambers.

Potential materials:

* ice;
* packed ice;
* snow;
* frozen water;
* icicle-like geometry where feasible;
* frost-covered architecture.

Consider region/depth restrictions rather than making these extremely common everywhere.

---

## Massive Rooms

Very large open chambers intended to dramatically interrupt the normal corridor rhythm.

These may include:

* enormous empty halls;
* giant support columns;
* multi-level traversal;
* bridges;
* stair systems;
* balconies;
* multiple exits;
* dramatic ceiling height.

Treat sufficiently large variants as landmark-scale / reserved structures rather than trying to force them through normal small-room placement.

---

## Church / Chapel

Religious or ceremonial architecture.

Potential contents:

* pew-like seating;
* altar;
* candles;
* columns;
* raised platform;
* side chambers;
* crypt access;
* ruined variants;
* abandoned variants.

Keep the exact lore ambiguous unless existing project lore defines otherwise.

---

## Jail Cells

Detention/prison layouts.

Potential variants:

* short cell block;
* long cell block;
* two-sided cell corridor;
* guard station;
* interrogation room;
* prison complex.

Do not allow external Labrinth corridors to accidentally connect directly into individual cells.

Only intended prison entrances should connect externally.

---

# 5. MONSTER OUTPOSTS

Implement monster-controlled outposts as combat-oriented special rooms or structures.

Initial factions:

* Zombie
* Skeleton
* Illager
* Piglin
* Wither Skeleton

Each should feel meaningfully different instead of being the same room with another mob substituted.

---

## Zombie Outpost

Possible characteristics:

* crude barricades;
* ruined living areas;
* food/storage remnants;
* dense zombie population;
* occasional armored zombie;
* dark, damaged environment.

---

## Skeleton Outpost

Possible characteristics:

* long sightlines;
* firing positions;
* elevated platforms;
* defensive barriers;
* skeleton patrols;
* arrow-related supplies.

---

## Illager Outpost

Possible characteristics:

* organized fortification;
* banners;
* guard posts;
* barracks;
* storage;
* patrol routes;
* pillagers / vindicators where appropriate.

The Labrinth dimension currently does not need raids to function for this feature.

Treat this as a contained hostile occupation structure.

---

## Piglin Outpost

Create an enclosed Piglin-controlled area using appropriate Nether-themed architectural accents.

Account for the Labrinth dimension's existing Piglin behavior/settings.

Do not make assumptions that break normal Piglin mechanics.

---

## Wither Skeleton Outpost

This should generally be one of the rarer and more dangerous outpost variants.

Possible characteristics:

* dark fortress-like architecture;
* guarded chokepoints;
* larger combat spaces;
* stronger loot;
* Nether fortress influence without simply pasting an entire vanilla fortress into the Labrinth.

---

# 6. ROOM / STRUCTURE VARIANTS

Do not create one template for every category and consider the category finished.

Create enough deterministic variation that repeated discoveries do not immediately feel identical.

Variation may come from:

* dimensions;
* orientation;
* connector count;
* internal layout;
* decoration;
* block variation;
* damage state;
* lighting;
* population;
* loot;
* region;
* depth.

Prefer reusable definitions and procedural modifiers over enormous amounts of duplicated code.

---

# 7. REGIONS

Integrate the new content with the existing Labrinth region system.

Examples of natural weighting:

```text
Overgrown
    jungle rooms
    caves
    spider nests

Ancient
    churches
    libraries
    treasury rooms
    prisons
    large stone chambers

Abandoned
    quarters
    dining halls
    barracks
    jails
    ruined villages

Industrial
    worker quarters
    barracks
    stockades
    utility-oriented settlements

Flooded
    flooded caves
    damaged quarters
    flooded jail/dining variants

Corrupted
    unusual variants
    dangerous outposts
    strange dungeon variants
```

These are weighting guidelines, not absolute restrictions unless a room specifically requires them.

Preserve gradual region variation.

---

# 8. DEPTH

Respect the existing logical depth system.

Use depth to influence:

* room availability;
* structure rarity;
* village rarity;
* dungeon size;
* dungeon difficulty;
* outpost difficulty;
* mob population;
* loot;
* unusual variants.

Do not simply equate depth with Y-coordinate.

Reuse the project’s existing logical depth value.

---

# 9. CONNECTOR CONTRACT

Every new room/structure must use the existing connector system.

Validate:

* connector orientation;
* type;
* width;
* height;
* rotation;
* compatibility;
* required connections;
* capped connections.

Large structures should expose a small, deliberate collection of **external connectors**.

Their internal corridors are part of the structure itself and should not become arbitrary connection opportunities for the surrounding Labrinth.

For compound structures, distinguish between:

```text
INTERNAL CONNECTION
```

and:

```text
EXTERNAL LABRINTH CONNECTION
```

if the current architecture needs that distinction.

Do not expose every internal doorway as a world-generation connector.

---

# 10. COLLISION AND BOUNDING

No new structure may bypass existing bounding validation.

Before committing a piece:

* calculate its complete transformed bounds;
* validate world height;
* validate floor/elevation;
* validate region;
* validate depth;
* validate reserved-structure collisions;
* validate required external connections;
* validate all multi-chunk ownership rules.

Compound structures must reserve the bounds of their **entire compound**, not independently allow each internal building to compete with normal Labrinth generation.

For example:

```text
Village Compound Bounds
├── House
├── Workshop
├── Farm
├── Street
└── Plaza
```

The Labrinth generator sees the outer compound bounds as occupied even where an internal courtyard happens to contain air.

Otherwise a normal hallway could incorrectly generate through the courtyard or between village buildings.

---

# 11. CHUNK SAFETY AND DETERMINISM

These requirements are non-negotiable.

Same seed + same coordinates must produce the same result.

Generation must not depend on:

* which direction the player approaches from;
* which chunk loads first;
* client state;
* player count;
* runtime random state.

For multi-chunk structures:

* one deterministic origin owns the structure;
* intersecting chunks rematerialize their portion;
* intersecting chunks do not independently decide that the structure exists;
* no duplicate structures;
* no duplicated entity population;
* no duplicated loot initialization.

Do not force-load neighboring chunks to complete structures.

Do not recursively request chunk generation.

Do not perform unbounded world searches.

---

# 12. PERFORMANCE

Remember that the Labrinth already contains considerably more enclosed geometry than normal terrain.

Avoid:

* per-tick scans;
* enormous block searches;
* uncontrolled flood fills;
* recursively exploring neighboring chunks;
* excessive block entities;
* enormous entity populations;
* recalculating full compounds independently from every intersecting chunk.

Large structure layouts should be derivable cheaply from their deterministic owner and seed.

---

# 13. SERVER COMPATIBILITY

Everything in this task must work server-side.

Do not introduce client-only dependencies into common generation code.

Maintain dedicated-server compatibility.

---

# 14. DEBUGGING / DEVELOPMENT TOOLS

Add or extend useful debugging support so these structures can actually be validated without wandering for hours hoping to encounter one.

Where compatible with the existing debug architecture, provide development methods/commands for locating or forcing/test-placing:

* a Labrinth village;
* compact dungeon;
* large dungeon;
* each major room family;
* each monster outpost;
* large reserved structures generally.

Debug functionality must not alter normal deterministic generation.

Prefer existing project debug patterns instead of inventing an unrelated framework.

---

# 15. VALIDATION

Explicitly test the most failure-prone cases.

## Structure integrity

Verify:

* village walls are not pierced by corridors;
* dungeon walls are not pierced by corridors;
* jail cells do not become accidental entrances;
* treasury walls do not open into neighboring rooms;
* ordinary rooms do not overlap villages;
* ordinary rooms do not overlap dungeon complexes;
* vertical structures do not cut through reserved structures unexpectedly;
* every intended external entrance aligns correctly;
* unused connectors seal correctly.

## Determinism

Test:

* same seed;
* same coordinates;
* reload;
* approaching from different directions;
* different chunk generation orders.

The resulting structures must be identical.

## Coordinates

Test:

* positive coordinates;
* negative X;
* negative Z;
* negative X/Z;
* large distances from origin.

## Multi-floor

Test large structures near:

* lower valid floors;
* normal floors;
* upper floors;
* maximum height constraints.

Reject placements that cannot fit rather than clipping them.

## Multiplayer / dedicated server

Confirm generation and population work on a dedicated server and do not create duplicated structures/entities when multiple players approach from different directions.

---

# 16. IMPLEMENTATION ORDER

Use a deliberate implementation sequence.

Recommended order:

### Step 1

Audit the current generator, landmark ownership, bounding system, connector system, room registry, region/depth selection, loot markers, entity markers, and debug tools.

### Step 2

Implement or extend reusable reserved-footprint / compound-structure support.

### Step 3

Implement a small test compound and prove that normal rooms and corridors route around it correctly.

Do not proceed with dozens of content types while this foundational behavior is broken.

### Step 4

Implement compact dungeons.

### Step 5

Implement one larger multi-room dungeon and validate reservation + connector behavior.

### Step 6

Implement one working enclosed village variant.

### Step 7

Expand villages into modular variants/components.

### Step 8

Add the new room catalogue.

### Step 9

Add monster outposts.

### Step 10

Add region/depth/rarity tuning.

### Step 11

Run generation validation and fix collisions, accidental openings, duplicate population, and chunk-order issues.

### Step 12

Build and run required project validation.

---

# 17. IMPORTANT DESIGN PRIORITY

I care more about the **generation architecture being correct** than about having 50 mediocre room templates immediately.

If necessary, implement fewer high-quality variants initially while creating a registration system that makes additional variants trivial to add afterward.

Do not satisfy this task by creating dozens of nearly identical rectangular rooms while ignoring the compound-generation problem.

The key result I want is:

> Villages, dungeons, huge chambers, and other special structures feel like places the Labrinth discovered and grew around rather than structures pasted on top of the maze.

---

# 18. DOCUMENTATION

Update the applicable project documentation after implementation.

Follow `AGENTS.md`.

At minimum, as appropriate:

* update `TASK.md`;
* update the implemented-state portion of `README.md`;
* update `architecture.md` if a new compound/reservation architecture is introduced;
* update `TRACELOG.md`;
* update `SUGGESTIONS.md`;
* increment the version in `build.gradle` according to the project rules.

Do not mark functionality complete unless it actually works.

---

# 19. BUILD / DEFINITION OF DONE

Before considering the task complete:

* run the project build;
* resolve compilation errors;
* preserve Minecraft 1.21.1 NeoForge compatibility;
* verify existing generation still functions;
* verify existing rooms/corridors/landmarks were not broken;
* test dedicated-server compatibility where the project workflow supports it;
* test representative villages, dungeons, rooms, and outposts;
* verify deterministic generation;
* verify chunk-order independence;
* verify no accidental structure penetration.

When finished, give me a concise implementation report containing:

1. **Architecture changes**
2. **New classes/files**
3. **Existing classes/files modified**
4. **Village implementation**
5. **Dungeon implementation**
6. **New room types and variants**
7. **Monster outposts**
8. **Generation/rarity/depth rules**
9. **Debugging tools added**
10. **Tests performed**
11. **Build result**
12. **Known limitations or recommended next steps**

Do not merely describe how this could be implemented. Implement the feature in the repository.
