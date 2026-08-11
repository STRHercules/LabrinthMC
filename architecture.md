# The Labrinth Architecture

> There is no end. There is only another hallway.

This document consolidates the technical direction stated in `README.md` and
the design schematics in `Pictures/`. It is a design contract for the
procedural dimension, not a claim that every planned system is already
implemented. The visual design document is marked `v0.1.0`; that label is the
design-document revision and is separate from the mod version.

## 1. Scope and current status

The Labrinth is a Minecraft `1.21.1` NeoForge mod. Its central feature is the
`labrinth:labrinth` dimension: a world made primarily from interconnected
interior spaces rather than open terrain, biomes, mountains, or oceans. The
dimension is the maze. It is intended to expand as players explore, without
requiring the entire layout to be authored in advance.

The current implementation includes the fixed-time, enclosed dimension, the
deterministic generation architecture, chunk-local rooms and corridors,
multi-floor pieces, regions, depth, landmarks, and origin-owned compound
structures. The architecture below distinguishes shipped contracts from
future content systems such as custom blocks and datapack authoring.

| Area | Current repository state | Architectural target |
| --- | --- | --- |
| Platform | Minecraft `1.21.1`, NeoForge, mod ID `labrinth` | Keep the same platform contract |
| Dimension | `labrinth:labrinth` is registered and loadable | An effectively unlimited interior dimension |
| Test world | `minecraft:the_void` with the executable Labrinth generator and a walkable origin | Just-in-time modular cells and sectors |
| Generation | Deterministic 64-block cells, chunk ownership, neighbor edges, seed derivation, chunk-local materializers, and bounded compound reservations | Seed-derived, chunk-safe piece graph |
| Verticality | Three generated floors with stairs, ladders, and shafts | More authored vertical structures |
| Regions | Abandoned, Industrial, Flooded, Overgrown, Ancient, and Corrupted region fields | More region-specific authored content |
| Content | Rooms, corridors, landmarks, enclosed villages, two dungeon scales, large compounds, loot metadata, and owner-chunk population | Custom blocks, datapack-friendly authoring, hazards, and broader entity systems |

### 1.1 Implemented dimension contract

The current data-driven dimension type is the source of truth for the Phase 1
foundation:

| Property | Value |
| --- | --- |
| Dimension ID | `labrinth:labrinth` |
| Coordinate scale | `1.0` |
| Effects | `minecraft:overworld` |
| Fixed time | `6000` |
| Ambient light | `0.1` |
| Minimum Y | `0` |
| Maximum height | `256` |
| Logical height | `256` |
| Has ceiling | `true` |
| Has skylight | `false` |
| Natural | `false` |
| Bed works | `false` |
| Respawn anchor works | `false` |
| Has raids | `false` |
| Piglin safe | `false` |
| Ultrawarm | `false` |
| Infiniburn tag | `#minecraft:infiniburn_overworld` |
| Monster spawn block-light limit | `0` |
| Monster spawn light level | `0` |

The Phase 1 level stem uses:

```text
Generator: minecraft:flat
Biome: minecraft:the_void
Features: disabled
Lakes: disabled
Structure overrides: none

Y=0: bedrock, height 1
Y=1..2: stone, height 2
Y=3: polished_deepslate, height 1
```

The flat platform is a validation environment only. It must remain separate
from the future procedural generator so that dimension-property behavior can
be tested without coupling it to room placement.

## 2. Generation model

The target architecture is a deterministic graph of reusable pieces. The
logical ownership hierarchy is:

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

### 2.1 Cells, sectors, and expansion

The Labrinth expands from a start sector/spawn through modular sectors or
cells. A generated cell is a modular unit; unexplored neighboring cells are
future expansion. Connections are doors or passages between units.

The implemented horizontal coordinate system uses vanilla Minecraft blocks as
the source coordinates and vanilla chunks as placement boundaries:

```text
CHUNK_SIZE_BLOCKS = 16
CHUNKS_PER_CELL   = 4
CELL_SIZE_BLOCKS  = 64
```

Each deterministic generation cell is therefore `64 x 64` blocks, or four by
four chunks. The cell coordinate is derived with mathematical floor division:

```text
chunkX = floorDiv(blockX, 16)
chunkZ = floorDiv(blockZ, 16)
cellX  = floorDiv(chunkX, 4)
cellZ  = floorDiv(chunkZ, 4)
```

Cell origins are `cellX * 64` and `cellZ * 64`; chunk origins are `chunkX * 16`
and `chunkZ * 16`. Negative coordinates use floor division so boundaries do
not shift or overlap below the origin. The horizontal cell grid is independent
of vertical placement; Y remains vanilla block Y and is checked against
generation constraints.

Piece bounds use half-open coordinates: minimum X/Z/Y are included and maximum
X/Z/Y values are excluded. Horizontal and vertical bounds must have positive
size. This makes chunk intersection and height validation unambiguous.

For a piece with bounds, the chunk containing its minimum horizontal corner is
the immutable placement owner. Only that chunk makes the existence decision;
other intersecting chunks may materialize the already-decided piece without
making another random choice. This is the ownership rule for pieces that span
multiple chunks.

The design requires:

- deterministic generation: the same seed produces the same Labrinth;
- modular pieces that snap together on a consistent grid;
- chunk-safe expansion generated just in time as players explore;
- effectively unlimited size in all directions and depths;
- no pre-generation of the entire dimension;
- minimal retained state for unloaded areas;
- no recursive or circular chunk generation;
- generation that can continue thousands of blocks from the origin; and
- no dependence on chunk visitation order, client state, or exploration order.

Every multi-chunk piece needs one deterministic owner, normally an origin cell
or origin chunk. Neighboring chunks must not independently place the same
landmark or multi-chunk piece.

### 2.2 Seed derivation

All layout decisions must be derivable from stable inputs. The intended seed
inputs are:

```text
World Seed
+ Dimension Salt
+ Cell Coordinates
+ Structure Type Salt
+ Local Position
```

The generation context may carry:

- world seed;
- dimension seed or salt;
- cell coordinates;
- chunk coordinates;
- local generation depth;
- region;
- neighboring connector information;
- a random source derived from the stable inputs; and
- generation constraints.

The context must avoid uncontrolled shared mutable state. Re-loading a world,
generating neighboring chunks in a different order, or using a different
client must not change an already determined layout.

The implemented seed system derives a 64-bit value by repeatedly mixing these
inputs in order:

```text
world seed
+ dimension salt
+ cell X coordinate salt
+ cell Z coordinate salt
+ structure-type salt
+ local X coordinate salt
+ local Z coordinate salt
```

The named public salts are:

| Salt | Hex value |
| --- | --- |
| `DIMENSION_SALT` | `0x4C414252494E5448` |
| `REGION_SALT` | `0x524547494F4E5F31` |
| `ROOM_SALT` | `0x524F4F4D5F534545` |
| `CORRIDOR_SALT` | `0x434F525249444F52` |
| `LANDMARK_SALT` | `0x4C414E444D41524B` |
| `CONNECTION_SALT` | `0x434F4E4E45435431` |
| `CONTEXT_SALT` | `0x434F4E5445585431` |

Region, room, corridor, landmark, connection, and per-chunk context decisions
use independent salts. Selection uses a seeded `RandomSource` and rejects a
non-positive option count. Long overflow during mixing is intentional and is
part of the stable signed 64-bit input.

### 2.3 Neighbor edges and generation context

Cells expose four cardinal directions: `NORTH`, `EAST`, `SOUTH`, and `WEST`.
An undirected edge is canonicalized by sorting the two cell coordinates before
hashing, so both cells derive the same connection result regardless of which
one loads first. The current neighbor state records the connected cardinal
directions as an immutable set. A direction is connected when the low bit of
its canonical connection seed is zero; the opposite side uses the same edge
seed.

The immutable generation context carries:

```text
worldSeed
dimensionSeed
cell
chunk
depth
region
neighbors
random
constraints
```

The default context region is `labrinth:standard`. The current default
constraints are:

```text
minY = 0
maxYExclusive = 256
maxDepth = 32
```

Depth must be in the inclusive range `0..32`, and Y must be in the half-open
range `0..256`. The context requires that its cell owns the target chunk and
that the seed-derived neighbor set and random source are non-null. Its random
source is derived from the world seed, context salt, and target chunk; shared
random sources must not be reused across cells.

### 2.4 Piece placement flow

The intended placement flow is:

1. Derive the owning cell/sector and its seed from the world seed and stable
   coordinates.
2. Resolve the region and logical depth for the cell.
3. Select a compatible modular piece using weighted, depth-aware pools.
4. Transform the piece using its rotation and connector rules.
5. Validate connector alignment, bounding boxes, height, and collisions.
6. Commit the piece only after validation succeeds.
7. Populate decoration, lighting, loot, hazards, and entities from deterministic
   secondary decisions.
8. Seal or cap unused connectors according to the piece and neighboring-cell
   decision.

Placement validation must be bounded and must not load distant chunks merely to
answer whether a piece can be placed.

## 3. Structure pieces and connectors

### 3.1 Core piece vocabulary

| Piece | Technical role |
| --- | --- |
| Corridor | Connects spaces and guides movement. Variants control length, sightlines, and branching. |
| Room | A bounded space with a purpose, contents, exits, and optional rarity. |
| Junction | A route-choice node that increases network complexity; common forms are T and cross junctions. |
| Stair | Moves between floors and elevations; stair-up and stair-down variants are distinct. |
| Vertical shaft | Extends deep up and down and acts as a vertical-navigation hub. |
| Landmark | A distinct, memorable, usually rare structure and point of interest. |
| Secret passage | A concealed path behind walls, floors, or mechanisms. |
| Decoration | Post-structure content such as debris, pipes, machinery, furniture, signs, plants, leaks, or ceiling damage. |

Rooms may contain their own layout, decorations, loot, blocks, lighting,
traps, creatures, environmental effects, sounds, structures, and interactive
elements. Landmarks may be enormous rooms, multi-floor structures, safe areas,
loot locations, boss arenas, story locations, transportation hubs, resource
centers, or entrances to unusual regions.

### 3.2 Piece metadata

Each reusable piece should be able to define the following metadata where it
applies:

- ID;
- structure or template;
- width, height, and depth;
- bounding box;
- generation weight and rarity;
- rotation and mirror rules;
- minimum and maximum depth;
- allowed regions;
- connector definitions;
- placement conditions;
- loot configuration; and
- decoration rules.

### 3.3 Connector contract

A connector carries:

- position;
- direction;
- connector type;
- width;
- height;
- rotation;
- compatibility rules;
- whether a matching connection is required; and
- whether a connection is blocked or capped.

The visual connector vocabulary is:

```text
STRAIGHT
TURN
T-JUNCTION
CROSS
STAIR UP
STAIR DOWN
SHAFT
DEAD END
```

The broader generation roadmap also reserves connector types such as
`STANDARD`, `WIDE`, `DOOR`, `ARCH`, `STAIR_UP`, `STAIR_DOWN`, `SHAFT`, `VENT`,
`LANDMARK`, and `SPECIAL`. A connector is valid only when orientation, type,
dimensions, and rotation are compatible. The region/depth design specifically
calls out compatible doorway profiles such as `1x3`, `T`, `Cross`, and
`Vertical`.

### 3.4 Bounding and collision rules

Before placement, the generator must:

- compute the piece bounding box;
- reject invalid overlaps;
- allow only explicitly permitted overlaps;
- prevent rooms from generating through unrelated rooms;
- prevent corridor collisions;
- validate vertical overlap;
- validate the dimension's minimum and maximum Y bounds;
- confirm that every required exit can connect or be intentionally capped; and
- keep all checks bounded and performant.

The design document's quality barrier is “broken alignment”: connectors must
not be misaligned, and visual openings must seal correctly. “Unusable layout”
and “no clear exits” are also rejection conditions because they can create
confusing flow, soft-locks, or dead ends without a readable purpose.

## 4. Floors, corridors, and vertical construction

### 4.1 Floor modules

| Module | Exact design intent |
| --- | --- |
| Standard Hall | Clean, durable stone for primary circulation and combat spaces. |
| Utility Floor | Grates, vents, and hatches; connects to systems and services. |
| Ruined Floor | Damaged and uneven; breaks sightlines and adds risk. |
| Flooded Floor Edge | Shallow water edges; slippery surfaces and visibility trade-offs. |
| Ancient Stone Floor | Older, denser stone with relic motifs and embedded details. |
| Corrupted Floor | Tainted surfaces that may spawn hazards or amplify threats. |

### 4.2 Corridor standards

| Variant | Design rule | Visual width guide |
| --- | --- | --- |
| Straight corridor | Primary flow; maintain clear sightlines. | `3–5` |
| Narrow passage | Restricts movement and line of sight; use sparingly. | `2–3` |
| Wide corridor | High-traffic area or arena; supports multiple routes. | `5–7` |
| Left turn | A `90°` turn that breaks line of sight and slows pursuers. | — |
| T-junction | Offers choices and alternate progression. | — |
| Cross junction | Major hub point; use landmarks and lighting. | — |
| Dead end | Misleads, hides secrets, or creates an ambush zone. | — |

The standard corridor width is **3 blocks**, with a stated minimum of **2**
and maximum of **7**. The construction guide also requires sightline breaks
every **10–15 blocks**, a rhythm of **open → tight → open**, and connectors
that can link vertical systems and secrets.

### 4.3 Walls, ceilings, and supports

- Wall segments vary texture and damage to avoid repetition.
- Support pillars provide structural support and space definition and break up
  lines of sight.
- Overhead beams add depth and vertical layering; chains and lanterns may hang
  from them.
- Ceiling variants may be flat, arched, vaulted, or reinforced to manage
  headroom.
- Vents and pipes provide atmosphere and navigation cues and may emit steam or
  sound.
- Hidden walls conceal doors, levers, or passages and use subtle hints.

### 4.4 Stairs and shafts

| Piece | Intended behavior |
| --- | --- |
| Stair up | Quick vertical transition; use in pairs. |
| Stair down | Safe descent; watch for drop landings. |
| Spiral stair | Compact footprint, ideal for towers. |
| Drop shaft | Risky drop; add water, hay, or nets for control. |
| Ladder shaft | Reliable vertical access; may be exposed or concealed. |
| Vertical lift | Mechanical lift that moves players and items. |

Vertical generation must support floors above and below the starting floor,
explicit floor heights, a layer system, non-overlapping layers, elevation-
specific room pools, multi-floor alignment, bounding validation, and hard
minimum/maximum Y restrictions. Upward and downward branching must be capped
where required.

### 4.5 Lighting and navigation construction

- Torches and lanterns provide base illumination and belong at turns, doors,
  and key landmarks.
- Emergency lights provide stronger cues in danger zones and boss arenas.
- Signage gives directional hints or warnings; language should remain minimal.
- Markers are visual anchors repeated across levels for orientation.
- Light should suggest routes, highlight options, and hide secrets rather than
  illuminate every space uniformly.

### 4.6 Construction principles

1. **Modular snapping:** reusable modules snap to a consistent grid so spaces
   orient and align.
2. **Multiple floors:** design vertically with offsets and overlaps; encourage
   up, down, and side routes.
3. **Clear connectors:** every area should connect to at least two other paths,
   vertically or horizontally, unless it is intentionally a dead end.
4. **Hidden routes:** secrets, fake walls, vents, and shafts reward curiosity
   and diversify paths.
5. **Safe anchors:** safe rooms, respawn anchors, and resource checkpoints occur
   at sensible intervals.
6. **Readable landmarks:** distinctive features help players orient, remember
   paths, and plan future exploration.
7. **Variety and rhythm:** mix layouts, heights, lighting, and themes so travel
   remains fresh and unpredictable.
8. **Performance awareness:** use efficient blocks, limit unnecessary entities,
   and plan lighting to keep performance stable.

## 5. Rooms and content

### 5.1 Room catalogue

| Room type | Exact purpose |
| --- | --- |
| Utility room | Supports the player with functional features. |
| Storage room | Contains containers, supplies, and materials. |
| Small chamber | Compact room for a short encounter or transition. |
| Large chamber | Open space for events, combat, or set pieces. |
| Cross room | Four-direction connector for route variety and flow. |
| Multi-exit hub | Central node with `3+` exits and route choices. |
| Hidden room | Concealed behind walls, floors, or triggers. |
| Challenge room | Tests skill, combat, or survival ability. |
| Puzzle room | Focuses on logic, sequence, or pattern solving. |
| Loot room | High-value rewards protected by risk or puzzles. |
| Safe room | Secure space for rest, planning, or recovery. |
| Landmark room | Memorable set piece that anchors the Labrinth. |

The README also describes empty rooms, long rectangular rooms, dead-end reward
rooms, decorative rooms, and rare test rooms as part of the initial room set.

### 5.2 Room content layers

Every room is assembled in layers:

1. **Structural shell:** room shape, walls, roof, and openings.
2. **Floor and walls:** block palette, patterns, trim, and detail.
3. **Lighting:** torches, lanterns, soul lights, and candles.
4. **Navigation elements:** doorways, stairs, shafts, and connectors.
5. **Decoration:** pillars, statues, banners, chains, and debris.
6. **Containers:** chests, barrels, shulker boxes, and urns.
7. **Entities:** spawners, mobs, and friendly NPCs.
8. **Hazards:** traps, lava, voids, and collapsing blocks.
9. **Special interaction:** levers, buttons, runes, and puzzle blocks.

### 5.3 Room contents

Rooms may place:

- loot chests containing equipment, materials, coins, and rare items;
- crafting and utility blocks such as crafting tables, anvils, smithing,
  brewing, and stone masonry;
- arrow shooters, spike traps, fall pits, flame vents, and collapsing floors;
- ruined altars, diaries, skulls, broken gear, and ancient carvings;
- hostile mobs, elite variants, mini-bosses, and ambient wildlife; and
- obelisks, fountains, giant statues, and beacons that guide and orient.

### 5.4 Rarity and purpose

| Type | Appearance frequency | Purpose | Distinguishing features |
| --- | --- | --- | --- |
| Common | Very high (`60–70%`) | Navigation, resources, common loot, minor threats. | Simple layouts, common loot, basic traps, standard connectors. |
| Uncommon | High (`20–25%`) | Stronger challenges and useful rewards. | More complex layouts, more rare loot, unique mechanics. |
| Rare | Low (`7–10%`) | Major rewards and significant choices. | Advanced puzzles, elite enemies, multiple exits, high-value loot. |
| Landmark room | Very low (`1–3%`) | Story anchors, orientation, and milestones. | Grand scale, unique visuals, lore significance, often multi-exit hubs. |

Room rarity is weighted. The region and logical depth may change the available
pool and the effective rarity without changing deterministic outcomes.

### 5.5 Room layout rules

- Every room has an obvious entry and at least one clear way forward; multiple
  exits are preferred.
- Every room serves a meaningful function: combat, puzzle, resource, rest, or
  story.
- Unique shapes and landmarks provide readable silhouettes.
- Rooms align to the grid and support standard connector types.
- Optional secrets may add hidden rooms, alternate paths, and bonus rewards.
- Higher risk or complexity must yield better rewards and progression value.

### 5.6 Challenge chamber example

The design example combines the following elements:

- a high-value chest beyond the gauntlet;
- a spike trap activated by a pressure plate;
- a disguised wall that opens after a lever sequence;
- an objective to defeat spawners or solve the puzzle;
- skeleton spawners that increase pressure;
- drip particles and echoing ambience;
- broken statues and banners that tell a story; and
- a rare loot chest plus a progression key as the reward.

### 5.7 Room population by purpose

| Purpose | Recommended content | Player-facing purpose |
| --- | --- | --- |
| Utility / support | Crafting tables, anvils, cauldrons, brewing stands, repair kits, smithing, beds, respawn anchors, storage, and sorting systems. | Prepare and sustain. |
| Exploration | Basic loot and supplies, clues, signs, journals, parkour or light puzzles, safe/low-risk threats, and secret paths or shortcuts. | Encourage discovery. |
| Danger / combat | Spawners, enemy waves, traps, environmental hazards, elite mobs, mini-bosses, combat arenas, and high-risk/high-reward loot. | Test skill and risk tolerance. |
| Mystery / lore | Lore books, inscriptions, ruined altars, artifacts, statues, murals, banners, ambient effects, sounds, cryptic puzzles, and clues. | Reveal the story. |
| Progression / reward | Rare loot, unique items, keys, runes, progression items, complex puzzles, trials, multi-step objectives, and area/path unlocks. | Advance the journey. |

## 6. Regions, depth, and progression

### 6.1 Region definitions

Regions are large portions of the continuous structure. They influence visual
identity and generation through region ID, weight, room and corridor pools,
block palette, lighting rules, decoration rules, mob rules, loot modifiers,
ambient properties, and generation conditions. Region borders should be
gradual where practical rather than changing every chunk.

| Region | Visual and generation identity |
| --- | --- |
| Abandoned | Cracked stone, broken halls, forgotten corridors, common ruins, traps, and basic enemies. |
| Industrial | Pistons, gears, idle machines, redstone mechanics, conveyors, factory remnants, pipes, and maintenance spaces. |
| Flooded | Waterlogged passages, submerged chambers, dripping hazards, currents, and drowned threats. |
| Overgrown | Roots, vines, nature reclaiming stone, mushrooms, spores, and living ambushes. |
| Ancient | Monumental architecture from a lost era, puzzles, relics, and guardian constructs. |
| Corrupted | Reality frays and twists; corrupted blocks, void energy, and warped monstrosities. |

The README presents the same six themes as possible regions and emphasizes
gradual transitions. Region-specific palettes are:

| Region | Palette direction |
| --- | --- |
| Abandoned | Cold, muted, and forgotten. |
| Industrial | Rusted metal, grates, and mechanical. |
| Flooded | Wet stone, corrosion, and mineral stains. |
| Overgrown | Nature reclaims. |
| Ancient | Timeless stone, ritual, and relics. |
| Corrupted | Void-tainted stone and unstable blocks. |

### 6.2 Logical depth

Depth is a logical progression value and does not have to equal physical Y.
The intended inputs are:

```text
Distance from origin
+ Generation branch depth
+ Region transitions
+ Landmark progression
+ Optional vertical distance
```

The visual depth bands are:

| Band | Design behavior |
| --- | --- |
| Core / shallow | Safe starting area, familiar layouts, low danger, and low reward. |
| Inner depths | Denser systems, more variations, stronger enemies, and better rewards. |
| Mid depths | Complex networks, deadly traps, valuable resources, and stronger foes. |
| Outer depths | Stranger places, unpredictable routes, rare materials, and very high risk. |
| Abyss | Unknown extremes, greatest danger, relics, and endgame rewards. |

The architecture principle is: **deeper areas become stranger, harder, rarer,
and more rewarding**. Depth can progressively unlock rare rooms, corridor
variants, regions, loot, entities, hazards, and ambient effects.

### 6.3 Generation rules

The visual design specifies these rules:

1. **Modular cells and sectors:** the Labrinth is built from modular cells
   grouped into sectors; cells snap via defined connectors.
2. **Deterministic and seed-based:** the layout is generated from the world
   seed; the same seed means the same Labrinth.
3. **Room rarity tiers:** room weights are `Common`, `Uncommon`, `Rare`, and
   `Epic`; effective rarity increases with depth.
4. **Branching frequency:** branching density increases with depth, creating
   more path choices and dead ends.
5. **Connector compatibility:** cells connect only through compatible doorway
   profiles such as `1x3`, `T`, `Cross`, and `Vertical`.
6. **Vertical links:** stairs, shafts, lifts, and drops appear according to
   depth and region rules.
7. **Landmark spacing:** major landmarks are spaced far apart and aligned to
   act as navigation anchors.
8. **Secret passages:** hidden paths appear with a probability based on region,
   depth, and surrounding density.

### 6.4 Room pools by depth

The design matrix tracks these pools across `Shallow (Core)`, `Medium (Inner +
Mid)`, `Deep (Outer)`, and `Abyss`:

- Corridors / Junctions
- Puzzle Rooms
- Loot Rooms
- Challenge Rooms
- Trap Halls
- Vertical Shafts
- Boss / Elite Arenas
- Environment Hazards
- Corrupted / Void Rooms
- Rare / Secret Rooms

The visual key assigns the four depth columns progressively stronger rarity
colors: `Common`, `Uncommon`, `Rare`, and `Epic`.

## 7. Multi-floor sector architecture

The multi-floor schematic describes a sector as a puzzle of space and
verticality: what lies above may unlock what lies below. The illustrative
sector contains **3 levels** and uses:

| Floor | Physical depth shown | Content emphasis |
| --- | ---: | --- |
| Upper floor | `+16` | Overlook walkways, ranged advantage, hidden chambers, and shortcut bridges. |
| Main floor | `±0` | Central hub, multiple intersections, key progression, and resource rooms. |
| Lower floor | `-16` | Deep chambers, dangerous traps, secret passages, and ancient remnants. |

The schematic labels these example spaces and connections:

- Overlook Balcony
- Archer's Nest
- Hidden Alcove
- Collapsed Bridge
- The Heart Chamber
- Puzzle Room
- Treasure Vault
- Grand Stairwell
- Secret Passage
- Ancient Ruins
- Trap Chamber
- Dark Sanctum

The vertical key distinguishes upper, main, and lower floor materials. The
symbol guide distinguishes `Door / Opening`, `Hidden Door`, `Breakable Wall`,
`Bridge`, and `One-way Gate`.

An illustrative sector data card shows:

```text
Sector ID: LBR-7A3F
Biome: Stone Depths
Difficulty: High
Estimated size: 128 x 128 x 64
Levels: 3 (shown)
Seed: 872361982
```

These values are schematic example data, not current runtime configuration.

The bird's-eye schematic further groups space into depth rings:

```text
Ring 1: The Core
Ring 2: Inner Depths
Ring 3: Mid Depths
Ring 4: Outer Depths
Ring 5+: The Abyss
```

## 8. Landmarks and special encounters

### 8.1 Landmark categories

| Landmark | Role |
| --- | --- |
| Grand Hall | Majestic, high-ceilinged hall; regional anchor and orientation point. |
| Archive | Repository of knowledge and relics; records, clues, and key story information. |
| Forge | Industrial chamber with heat and machinery; may unlock crafting or upgrades. |
| Void Gate | Door to the unknown; connects regions or unlocks new paths. |
| Chamber of Echoes | Haunted or resonant space that reveals memories, whispers, or forgotten events. |
| Central Stairwell | Vertical spine that knits the Labrinth together across elevations. |

The large schematic also identifies The Forge, The Archive, The Core, The
Chamber of Echoes, and The Void Gate as major recognizable structures.

### 8.2 Landmark purposes

Landmarks may provide:

- orientation and a mental map;
- safe anchors such as respawn points, shops, or rest opportunities;
- valuable rewards, unique items, and resources;
- progression gates requiring keys, knowledge, or actions;
- story spaces that reveal the Labrinth's forgotten past; and
- memorable navigation points that players can reference and revisit.

### 8.3 Special rooms

| Special room | Function |
| --- | --- |
| Shrine room | May grant boons, upgrades, or blessings at a cost or requirement. |
| Elite arena | Challenging combat with elite enemies or mini-bosses. |
| Puzzle vault | Logic, sequence, or spatial puzzles guarded by mechanisms. |
| Safe room | Secure place to rest, regroup, and manage inventory. |
| Hidden cache | Concealed stash of loot behind tricks or secrets. |
| Observation room | Vantage point for observing patterns, patrols, or the environment. |

### 8.4 Landmark placement rules

1. **Spacing:** landmarks have a minimum path-length distance between them.
2. **Rarity:** one landmark per region target; types rotate to maintain variety.
3. **Depth restrictions:** some landmarks appear only in specific depth bands.
4. **Region restrictions:** biome/region themes determine eligible landmark
   types.
5. **Multi-floor support:** landmarks may span floors and connect vertically.
6. **Deterministic origin:** each landmark has one deterministic anchor cell.

Landmark placement must also enforce minimum spacing, maximum frequency,
bounding size, connection requirements, and multi-chunk ownership.

### 8.5 Origin-owned compound structures

Villages, dungeon complexes, enormous caves, the massive hall, and monster
outposts use `SpecialStructureCatalog` rather than separate reservation
systems. Candidate origins are aligned to deterministic eight-cell sectors;
one origin selects the definition, floor, region, depth, and open external
connectors. The selected `COMPOUND` piece owns its complete half-open bounds,
including internal courtyards and air, so ordinary rooms, corridors, vertical
pieces, and overlapping landmarks yield before materialization.

Compound connectors are placed on ordinary cell-center boundaries and are
opened only when the canonical neighbor edge is connected. The normal content
selector compares its boundary connector to that declared compound endpoint;
every other compound face remains a wall. Intersecting chunks re-derive the
same instance and render only their local intersection without loading a
neighboring chunk. The owner chunk alone adds the deterministic population,
while block-entity loot metadata is written with a stable position-derived
seed so reloads do not duplicate entities or reroll an already-created chest.

`findNearest` is a bounded development lookup, and the chunk-generator debug
screen reports a selected compound and its open entrance count. Neither path
changes normal selection or generation.

### 8.6 Encounter elements

Special rooms may contain:

- mini-bosses with unique attacks and loot tables;
- trap sets using environmental hazards, pressure plates, firing devices, or
  timed events;
- logic, pattern, movement, or resource puzzles;
- patrols with sight, sound, or route logic;
- ambient threats such as darkness, corruption, or decay;
- story objects such as scrolls, relics, terminals, and memory nodes; and
- loot guardians made from elite mobs or reward-protecting mechanisms.

### 8.7 Example: The Archive

The landmark example includes:

- shelves and stacks containing lore books, clues, and map fragments;
- a record vault with a locked, high-value archive chest;
- a hidden lever behind a bookshelf panel that opens a concealed passage;
- a safe study alcove for reading, planning, and managing gear; and
- a rare loot chest containing high-tier loot with story or progression
  requirements.

## 9. Loot, hazards, and entities

### 9.1 Loot tiers

| Tier | Examples |
| --- | --- |
| Common | Stone, iron, food, and basic materials. |
| Uncommon | Tools, enchanted books, potions, and utility items. |
| Rare | Rare gear, keys, upgrade items, and unique materials. |
| Landmark / relic | Artifacts, relics, dimensional essence, and legendary items. |

Loot may be influenced by room type, region, distance traveled, structure
rarity, danger level, and dimension progression. The README's intended rewards
include unique items, equipment, building materials, artifacts, and resources
connected directly to the Labrinth.

### 9.2 Reward by depth

| Depth | Risk and reward |
| --- | --- |
| Core | Low risk, basic loot, learning, and orientation. |
| Inner depths | Moderate risk, better loot, and better resources. |
| Mid depths | High risk, rare loot, and stronger entities. |
| Outer depths | Very high risk, elite threats, and powerful loot. |
| Abyss | Extreme risk, relics, and endgame loot. |

Risk, hazard frequency, and enemy strength scale with depth.

### 9.3 Entity categories

| Category | Behavior |
| --- | --- |
| Patrol creature | Wanders corridors in patterns and alerts others when disturbed. |
| Stalker | Stealthy hunter that tracks players and strikes from shadows. |
| Guardian | Defends key areas and objectives; high durability and area control. |
| Swarm | Appears in groups and overwhelms through numbers and speed. |
| Neutral dweller | May trade, warn, or assist; can turn hostile if provoked or harmed. |
| Elite variant | Stronger form with unique abilities and better loot. |

The README also expects support for corridor wandering, room wandering, patrol
routes, darkness preference, sound investigation, door interaction, alternate
routes, region restrictions, depth restrictions, and landmark guarding.

### 9.4 Hazard types

- **Pressure traps:** triggered by weight or step patterns.
- **Dart traps:** ranged darts, arrows, or spikes fired on a trigger.
- **Flooding:** rising or rushing water limits movement.
- **Collapsing floors:** unstable tiles fall after a delay or overload.
- **Darkness zones:** visibility is reduced and mobs gain an advantage.
- **Corrupted anomalies:** apply debuffs, spawn enemies, or drain resources.
- **Fire / machinery:** burning jets, lava, cogs, blades, or redstone traps.

The README also names environmental hazards, unusual fog, localized darkness,
electrical failures, and strange phenomena as possible exploration systems.

### 9.5 Population calculation

The design model is:

```text
Room Type
  + Region & Depth
  + Modifiers
  = Population Result
```

The inputs and outputs are:

```text
Room Type: Combat, Puzzle, Utility, Loot, Landmark, Safe
Region & Depth: Core, Inner Depths, Mid Depths, Outer Depths, Abyss
Modifiers: Clearance Level, Player Count, Progression Flags,
           Difficulty Settings, Time / Events

Population Result: Enemy Types, Enemy Count, Hazard Intensity,
                   Loot Tier, Special Events
```

The example population table is:

| Room type | Loot chance | Enemy level | Hazard intensity | Rarity |
| --- | --- | --- | --- | --- |
| Utility room | Low | Low | Low | Common |
| Loot room | Medium | Low–Medium | Low–Medium | Common |
| Challenge room | High | Medium–High | Medium–High | Uncommon |
| Safe room | Low | None | None | Uncommon |
| Puzzle room | Medium | Low | Low | Rare |
| Landmark room | Very High | High–Extreme | High–Extreme | Very Rare |

### 9.6 Elite arena example

The reference arena combines an elite spawner, timed dart emitters, cover-
providing pillars, lava channels, a higher-tier reward chest, pressure plates,
a delayed collapsing floor, and a darkness core that reduces visibility and
empowers enemies. Completing the arena unlocks the exit.

Balance rules are:

- reward is proportional to risk;
- safe rooms stay rare because recovery is valuable and limited;
- elite danger is telegraphed with clear cues and buildup;
- hazards are readable so players can learn, adapt, and prevail; and
- exploration rewards curiosity, knowledge, and persistence.

## 10. Atmosphere, navigation, and secrets

### 10.1 Atmosphere

The dimension should feel strange even when nothing is attacking. Potential
systems are flickering lights, distant noises, ambient machinery, echoing
footsteps, doors opening or closing, environmental particles, unusual fog,
localized darkness, electrical failures, strange music, rare ambient events,
and areas that become unnaturally quiet. Sounds are not guaranteed to indicate
nearby danger.

### 10.2 Navigation tools

| Tool | Exact behavior |
| --- | --- |
| Map fragments | Found throughout the Labrinth; combine them to reveal adjacent sectors and landmarks. |
| Compasses | Point toward the nearest landmark anchor or safe room; unreliable near anomalous areas or vaults. |
| Echo crystals | Pulse near secrets, alternate routes, or puzzle elements; recharge slowly. |
| Chalk / wall marks | Diegetic markings left by explorers or factions; indicate danger, direction, or hidden mechanisms. |
| Torch / lantern trails | Visual breadcrumbs that preserve progress and help players find safe paths back. |
| Safe-room anchors | Tie into compasses and maps and provide reliable reorientation and respawning points. |

The README also names waypoints, maps, signs, torches, block markers,
coordinates, breadcrumb trails, and player-built safe rooms as valid player
navigation strategies.

### 10.3 Player guidance

- Gradual lighting changes signal transitions and depth.
- Distinct landmarks act as reorientation beacons.
- Unique silhouettes and shapes should be visible from a distance.
- Repeated architectural motifs build familiarity.
- Region palettes signal new regions through color and materials.

These cues should orient players without removing the feeling of being lost.

### 10.4 Secret design

| Secret form | Implementation cue |
| --- | --- |
| Hidden door | Blends with surroundings; triggered by pressure plates, levers, or items. |
| Breakable wall | Weak or cracked blocks; visual and audio irregularities provide a hint. |
| Concealed shaft | Drop shaft, climb, or crawl space hidden behind false blocks. |
| False dead end | Appears blocked but offers an alternate solution. |
| Puzzle lock | Requires logic, sequencing, or observation. |
| Alternate route | Parallel path that provides a shortcut, safer option, or new secret. |

Secret discovery rules are:

- hint through cracks, gaps, or slight block variations;
- use lighting to suggest points of interest;
- use environmental sound cues such as drips, wind, and mechanisms;
- leverage suspicious symmetry and repetition;
- use misaligned blocks, doors, or textures;
- tell stories through notes, ruins, and environmental details; and
- always leave at least one clue for players.

### 10.5 Safe rooms and marking

Safe rooms should provide essentials but not excess resources, prevent hostile
spawns, have a clear visual identity, expose or hint at multiple exits, and
use memorable visuals. The marking system includes sector signs, directional
arrows, rune markers, color-coded lights, numbered doors, and mechanical labels
on plates or levers.

### 10.6 Exploration loop

The documented loop is:

```text
ENTER THE LABRINTH  -> Spawn in the Core; gather bearings.
EXPLORE             -> Move through corridors and rooms.
MAP                 -> Reveal layout, mark landmarks, collect map fragments.
DISCOVER            -> Find secrets, loot, lore, and resources.
SURVIVE             -> Manage hazards and traps.
GO DEEPER           -> Push farther for better rewards and challenges.
RETURN OR GET LOST  -> Exit via known routes or risk becoming another legend.
```

The loop intentionally supports getting lost, rewarding curiosity, preserving
clues, making secrets logically discoverable, using loops to build navigation
mastery, balancing challenge with clarity, evolving the maze over time, and
respecting player time with return paths and shortcuts.

## 11. Content data and authoring pipeline

### 11.1 Room authoring pipeline

Every modular room or structure follows the nine-step authoring flow:

1. **Concept:** define purpose, mood, and gameplay role.
2. **Block palette:** choose materials, accents, and the detail kit.
3. **Structure shell:** build base shape, height, and primary flow.
4. **Connectors:** add connector points and orientation.
5. **Lighting:** place lights, lanterns, and visibility cues.
6. **Room contents:** add props, set dressing, and interactables.
7. **Hazards/entities:** place hazards, spawners, and encounters.
8. **Validation:** run rules and performance validation.
9. **Integration:** publish to the registry and enter generation.

### 11.2 Canonical content data shape

The visual design gives this example data model:

```json
{
  "id": "labrinth:room.treasure_vault",
  "type": "room",
  "weight": 12,
  "allowed_regions": ["ancient", "flooded", "overgrown"],
  "min_depth": -64,
  "max_depth": 128,
  "connectors": [
    {"type": "door", "dir": "north"},
    {"type": "door", "dir": "south"}
  ],
  "loot_table": "labrinth:chests/treasure_vault",
  "conditions": {
    "min_clearance": 3,
    "requires_lighting": true
  }
}
```

The field reference is:

| Field | Meaning |
| --- | --- |
| `id` | Unique identifier. |
| `type` | `corridor`, `room`, `junction`, `stair`, `shaft`, or `landmark`. |
| `weight` | Spawn-probability weight. |
| `allowed_regions` | Regions where the piece may appear. |
| `min_depth`, `max_depth` | Logical/vertical placement range. |
| `connectors` | Connector endpoints with direction. |
| `loot_table` | Loot-table resource. |
| `conditions` | Placement constraints. |

### 11.3 Modular piece checklist

| Type | Connectors | Dimensions | Theme | Loot | Hazards | Compatibility |
| --- | --- | --- | --- | --- | --- | --- |
| Corridor | At least 2 endpoints or T/X type | Short, medium, and long length variants | Matches region palette | None or minimal | Optional traps or ambushes | Connects to valid corridor/junction types |
| Room | `1+` doors minimum | Fits height and size constraints | Themed interior | Defined loot table or weight | Spawner/trap density balanced | All exits align and seal correctly |
| Junction | `3+` doors for T/X type | Symmetry and clearance checked | Neutral or mixed theme | Low to moderate rewards | Optional ambush zones | Supports all corridor connections |
| Stair | Up or down endpoints | Rise/run within limits | Stone palette | None or minor | Fall risk considered | Matches shaft rules and level |
| Shaft | Top and bottom links | Height conforms to limits | Shaft styling applied | None | Fall/mob exposure checked | Vertical connectors only |
| Landmark | Multiple entrances allowed | Large scale within region bounds | Unique landmark style | High-value/unique rewards | Boss/elite encounter or risk | Does not break region balance |

The checklist uses three states: required, conditional/optional, and not
allowed. Any piece failing a required compatibility rule is rejected or
returned for revision.

## 12. Validation, testing, and release gates

### 12.1 Validation rules

1. **Deterministic seed behavior:** same seed and options produce the same
   world; generation is 100% reproducible.
2. **Connection compatibility:** every connector finds a valid match or is
   intentionally capped; orientation, type, and size align.
3. **Overlap and collision checking:** pieces do not overlap or clip into
   existing geometry; terrain and structure collisions are blocked.
4. **Height limits:** region minimum/maximum depths are respected; stairs,
   shafts, and rooms remain within bounds.
5. **Chunk-safe generation:** generation and population do not cross chunk
   boundaries unpredictably.
6. **Performance awareness:** avoid excessive block states, entities, or tile
   entities; keep generation time predictable and efficient.

### 12.2 Testing matrix

| Test | Goal | Method | Pass criteria |
| --- | --- | --- | --- |
| Build test | Verify piece integrity. | Place an isolated piece in a test world. | No gaps or floaters; all exits functional. |
| Seed test | Check determinism and variation. | Generate with the same seed multiple times. | Identical results for the same seed; different seeds remain distinct. |
| Reload test | Validate save/load stability. | Save, reload, and re-enter. | No corruption or connection breaks. |
| Negative-coordinate test | Ensure behavior below zero. | Generate at large negative coordinates. | No overflow; structures align. |
| Multiplayer test | Verify sync and consistency. | Multiple clients explore together. | No desync, missing blocks, or connection errors. |
| Performance test | Measure generation performance. | Profile generation and traversal. | Target TPS is met; no long spikes. |

The roadmap additionally requires clean builds, client startup, dedicated
server startup, new-world creation, successful Labrinth generation, existing
test-world reload, and independent release-JAR testing.

### 12.3 Quality barriers

Reject or revise pieces with:

- unusable layout: confusing flow, tight dead ends, or unclear routes;
- no clear exits: a player can become trapped or soft-locked;
- broken alignment: connectors are misaligned or do not visually seal;
- reward imbalance: loot value does not match risk or placement;
- excessive block cost: the piece exceeds the generation budget; or
- poor theming: the piece breaks immersion or conflicts with its region.

### 12.4 Release readiness snapshot from the design reference

The page-8 readiness strip marks these areas as follows:

| Area | Status in the visual reference |
| --- | --- |
| Generation stability | Ready |
| Region coverage | Ready |
| Landmark set | In progress |
| Loot pass | In progress |
| Hazard pass | In progress |
| Server compatibility | Ready |
| Documentation | Ready |

These are design-document status labels, not an automated report for the
current checkout.

## 13. Performance, server, and extensibility boundaries

The README's technical goals are:

- effectively unlimited procedural generation;
- modular room and corridor generation;
- weighted room rarity;
- multi-floor generation;
- vertical connectivity;
- region-based generation;
- structure validation that prevents impossible connections;
- deterministic generation based on the world seed;
- efficient chunk generation;
- server compatibility;
- datapack-friendly content where practical; and
- extensible APIs for adding rooms and structures.

Performance is a first-class constraint because the dimension can contain more
generated blocks and enclosed geometry than traditional terrain. Generation
must avoid large per-tick scans, unbounded searches, unbounded recursion,
uncontrolled memory growth, unnecessary chunk loading, and excessive block
entity or lighting work.

Common/server code must not depend on client rendering, GUI, keybind, or other
client-only classes. The dimension and its generator must work on a dedicated
server and in multiplayer with low desynchronization risk.

The intended project boundaries remain modular:

```text
com.labrinthmc.labrinth
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

New rooms, regions, landmarks, blocks, entities, loot, and generation rules
should be addable without rewriting the entire dimension generator.

## 14. Reference schematics and visual vocabulary

The pictures are concept references. Their labels are included here so that
future implementation work uses the same vocabulary.

### 14.1 Bird's-eye schematic

`schematic_1.png` defines this legend:

- corridor;
- room;
- landmark;
- stairs;
- shaft up/down;
- secret passage; and
- safe room.

It shows The Forge, The Archive, The Core, The Chamber of Echoes, and The
Void Gate as major landmarks around the core and labels depth rings 1, 2, 3,
4, and `5+`. Its notes are **Endless by design**, **Layout shifts beyond Ring
5**, **Expect the unknown**, **Map your path**, and **Trust no corridor**.

The illustrative sector card reads:

```text
Sector ID: LAB-∞
Dimension: The Labrinth
Biome: Stone / Echo
Hazard level: High
Population: Unknown
Civilization: None
Discovered by: ???
```

### 14.2 Region schematic

`schematic_2.png` shows six contiguous visual regions and the following map
legend:

```text
Region gateway
Primary path
Secondary path
One-way route
Points of interest
Submerged route
Vertical connection
```

Its region captions are:

- **Abandoned:** crumbling halls and shattered remnants; echoes of what once
  was;
- **Industrial:** endless machinery and corroded infrastructure; the Labrinth
  that works;
- **Flooded:** submerged depths and hidden currents; the Labrinth that drowns;
- **Overgrown:** nature reclaims what was built; the Labrinth that grows;
- **Ancient:** timeless chambers and forgotten rites; the Labrinth that
  remembers; and
- **Corrupted:** reality fractures and whispers unmake all; the Labrinth that
  hungers.

The schematic metadata is `Scale: 1:00`, `Mapping survey: 7th cycle`,
`Cartographer: Unknown`, and `Status: Incomplete`.

### 14.3 Multi-floor schematic

`schematic_3.png` names the sector's upper, main, and lower floors, the
`+16`, `±0`, and `-16` depth offsets, and the vertical connection vocabulary
used in Section 7. Its summary is **Procedural, persistent, perilous** and
states that no two sectors are ever the same and orientation is the greatest
challenge.

### 14.4 Visual design-document pages

The eight-page `v0.1.0` design document provides the detailed construction,
room, region, landmark, population, navigation, and validation rules captured
above:

| Page | Subject | File |
| ---: | --- | --- |
| 1 / 8 | Dimension architecture overview | [`ChatGPT Image Aug 9, 2026, 03_25_45 PM (1).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_45 PM (1).png>) |
| 2 / 8 | Floor, corridor, and vertical construction guide | [`ChatGPT Image Aug 9, 2026, 03_25_46 PM (3).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_46 PM (3).png>) |
| 3 / 8 | Room design and content planning | [`ChatGPT Image Aug 9, 2026, 03_25_46 PM (4).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_46 PM (4).png>) |
| 4 / 8 | Regions, depth, and generation rules | [`ChatGPT Image Aug 9, 2026, 03_25_45 PM (2).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_45 PM (2).png>) |
| 5 / 8 | Landmarks, special rooms, and encounter design | [`ChatGPT Image Aug 9, 2026, 03_26_03 PM (2).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_26_03 PM (2).png>) |
| 6 / 8 | Loot, hazards, and entity population | [`ChatGPT Image Aug 9, 2026, 03_26_03 PM (1).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_26_03 PM (1).png>) |
| 7 / 8 | Navigation, secrets, and player guidance | [`ChatGPT Image Aug 9, 2026, 03_26_03 PM (3).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_26_03 PM (3).png>) |
| 8 / 8 | Content pipeline, build workflow, and validation | [`ChatGPT Image Aug 9, 2026, 03_26_03 PM (4).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_26_03 PM (4).png>) |

The three files beginning `03_25_21 PM` and the corresponding three files
beginning `03_25_39 PM` are duplicate brochure/overview exports. The named
files are retained in `Pictures/`; their repeated content does not introduce
additional technical requirements.

### 14.5 Complete visual reference index

The remaining poster, overview, and duplicate files in `Pictures/` are part of
the same reference set:

| Role | File |
| --- | --- |
| Product banner | [`banner.png`](<Pictures/banner.png>) |
| Region overview poster | [`biomes.png`](<Pictures/biomes.png>) |
| Dimension feature poster | [`info.png`](<Pictures/info.png>) |
| Main product poster | [`mainposter.png`](<Pictures/mainposter.png>) |
| Bird's-eye schematic | [`schematic_1.png`](<Pictures/schematic_1.png>) |
| Region schematic | [`schematic_2.png`](<Pictures/schematic_2.png>) |
| Multi-floor sector schematic | [`schematic_3.png`](<Pictures/schematic_3.png>) |
| Overview brochure export 1 | [`ChatGPT Image Aug 9, 2026, 03_25_21 PM (1).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_21 PM (1).png>) |
| Overview brochure export 2 | [`ChatGPT Image Aug 9, 2026, 03_25_21 PM (2).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_21 PM (2).png>) |
| Overview brochure export 3 | [`ChatGPT Image Aug 9, 2026, 03_25_21 PM (3).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_21 PM (3).png>) |
| Duplicate overview export 1 | [`ChatGPT Image Aug 9, 2026, 03_25_39 PM (1).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_39 PM (1).png>) |
| Duplicate overview export 2 | [`ChatGPT Image Aug 9, 2026, 03_25_39 PM (2).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_39 PM (2).png>) |
| Duplicate overview export 3 | [`ChatGPT Image Aug 9, 2026, 03_25_39 PM (3).png`](<Pictures/ChatGPT Image Aug 9, 2026, 03_25_39 PM (3).png>) |

The eight page-specific files are listed in Section 14.4. Together, the two
tables cover every PNG currently stored in `Pictures/`.

## 15. Design pillars

The combined README and visual references reduce to these engineering and
player-experience pillars:

1. **Exploration:** curiosity drives every step.
2. **Discovery:** new sights, secrets, and stories reward travel.
3. **Orientation:** clear cues guide choices without revealing a complete map.
4. **Survival:** risk, reward, and resource management matter.
5. **Mystery:** unknown paths hide ancient truths.
6. **Modularity:** reusable pieces create endless possibilities.
7. **Determinism:** the same rules and seed produce the same world.

The final system should be expandable with new regions and depths, modular
through reusable well-defined pieces, deterministic for a stable experience,
performant during exploration, and extensible for future content and systems.
