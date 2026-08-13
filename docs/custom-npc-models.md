# Custom NPC models

A design for letting a player build a model in Blockbench, animate it, and have it walk around
in game as a real NPC.

Status: **proposal**. Nothing here is built. Written 2026-08-13.

---

## The short version

Most of the scaffolding already exists. NPCs already carry a per-NPC model id, already carry a
per-NPC scale that drives their hitbox, and the mod already knows how to write a resource pack to
disk, switch it on, and reload it. What is missing is a bundle format, a screen to make one, and the
wiring that lets the GeckoLib model vary per NPC instead of being one fixed model.

Two decisions shape everything else:

1. **Feed GeckoLib rather than fight it.** GeckoLib scans the resource manager on reload. If the mod
   writes a bundle's files into the pack it already generates, GeckoLib picks them up through its
   normal path. No cache injection, no custom loaders, no reflection.
2. **The server owns the bundles, clients hold copies.** A model made by one player should work for
   everyone on the server without anyone else installing anything.

---

## What already exists

Worth stating plainly, because it changes the size of the job.

| Piece | Where | What it gives us |
|---|---|---|
| Per-NPC model id | `NotchNpcEntity.getModelId()` | A synced string already routed by prefix |
| Per-NPC scale | `NotchNpcEntity.setScale()` | Already drives the vanilla hitbox |
| Render state plumbing | `NotchNpcRenderState` | Carries pose, scale and texture per NPC on 1.21.11+ |
| Pack generation | `CurrencyPackGenerator` | Writes a pack, enables it, calls `reloadResourcePacks()` |
| Server to client assets | `CurrencyPackGenerator` | Already lands server-sent textures on clients |
| Clip roles | `NotchNpcEntity.chooseAnimation()` | Already picks idle / walk / flourish by NPC state |
| Live preview widget | `NpcPreviewWidget` | Already renders an NPC in a screen |

The model id is already a prefix scheme, which is the extension point:

- `humanoid` - the vanilla biped path
- `apply` - the GeckoLib path, one fixed model, textures vary by skin value
- `entity:<id>` - disguised as a vanilla entity

A fourth prefix, `npc:<bundle_id>`, slots in without disturbing the other three.

---

## Where the files live

The question this design has to answer well: a model made by one player has to work for every other
player who can see the NPC. Minecraft renders on the client, so every viewer needs the assets
locally. That is the game's architecture, not a mod limitation.

So one bundle exists in three places, with one of them the authority:

| Where | Path | Role |
|---|---|---|
| Author's client | `config/notchcurrency/npc_models/<id>/` | Where it is made, and a working copy |
| Server | `<world>/notchcurrency/npc_models/<id>/` | **The authority.** Travels with the world |
| Other clients | `config/notchcurrency/npc_models/<id>/` | Cache, filled by the server on join |

Server side sits in the world folder rather than config, because an NPC referencing a bundle is
world data. Back up the world and the models come with it.

### The drop folder

Raw Blockbench exports go somewhere separate from finished bundles:

```
config/notchcurrency/npc_models/_import/     <- drop exports here
config/notchcurrency/npc_models/town_guard/  <- finished bundle, made by the screen
```

The creation screen has an **Open Folder** button using `Util.getPlatform().openPath()`, the same
call vanilla uses for the resource pack folder. Click it, drag the Blockbench exports in, come back,
hit Refresh.

Deliberately **not** a native file dialog. LWJGL's tinyfd is on the classpath, but its file picker
has to run on the main thread and is a known deadlock risk on macOS. A drop folder is boring,
predictable and works identically everywhere.

---

## The creation screen

The point of the screen is that nobody hand-writes JSON. Every field is a picker over what actually
exists, so there is nothing to mistype.

```
  Create NPC Model
  
  Name     [ Town Guard                    ]
  Id       [ town_guard                    ]   (from the name, editable)
  
  Model    [ guard.geo.json            v ]     <- files found in _import
  Texture  [ guard.png                 v ]
  Anims    [ guard.animation.json      v ]
  
  Idle     [ animation.guard.idle      v ]     <- clips found in that file
  Walk     [ animation.guard.walk      v ]
  Special  [ animation.guard.wave      v ]     (optional)
  
  Scale    [ 1.0 ]    Height [ 1.95 ]  Width [ 0.6 ]
  
  [ Open Folder ]  [ Refresh ]        [ Preview ]  [ Create ]
```

The three clip fields come straight from the roles the animation system already uses. The mod parses
the chosen animation file and lists its `animations` keys, so the player picks from what their file
genuinely contains rather than typing a name that has to match exactly.

**Preview** uses the existing `NpcPreviewWidget` so the model can be turned around and checked before
it is saved anywhere.

### Validation, in plain English

This is most of what makes it feel robust. Every check happens on Create, with a message that says
what to fix:

| Check | Message |
|---|---|
| Geo parses and has `minecraft:geometry` | "That is not a GeckoLib model. In Blockbench, use File, Export, GeckoLib Animated Model." |
| Texture is a real PNG | "That texture could not be read as a PNG." |
| PNG size matches the geo's `texture_width` / `texture_height` | "This model expects a 64 by 64 texture, but that image is 128 by 128." |
| Chosen clips exist in the animation file | "That animation file has no clip called ..." |
| Id is unique and filename-safe | "A model called town_guard already exists." |
| Bundle within size limits | "That model is larger than the 2 MB limit." |

The texture size check is worth the effort on its own. A mismatched texture is the most common
reason a Blockbench model looks scrambled in game, and the error is otherwise baffling.

---

## The bundle

Written by the screen, not by hand, but plain enough to hand-edit or share.

```
town_guard/
  npc.json
  model.geo.json
  animation.json
  texture.png
```

```json
{
  "format": 1,
  "id": "town_guard",
  "name": "Town Guard",
  "author": "Joey",
  "scale": 1.0,
  "hitbox": { "width": 0.6, "height": 1.95 },
  "clips": {
    "idle": "animation.guard.idle",
    "walk": "animation.guard.walk",
    "special": ["animation.guard.wave"]
  }
}
```

`clips` maps the bundle's own animation names onto the roles the NPC already understands. Anything
missing falls back: no walk clip means the NPC plays its idle while moving rather than breaking.

This also generalises what is there now. The built-in model's `animation.notch_npc.idle` and friends
become the default role mapping rather than hardcoded names, so one code path serves both.

---

## Loading

On client start, on join, and on Create:

1. Scan for folders containing `npc.json`. Reject a bad bundle, never the whole load.
2. Copy into the generated pack under **both** GeckoLib layouts, because the two generations scan
   different paths and the mod supports both:

```
assets/notchcurrency/geo/npc_<id>.geo.json                 GeckoLib 4
assets/notchcurrency/animations/npc_<id>.animation.json    GeckoLib 4
assets/notchcurrency/geckolib/models/npc_<id>.geo.json     GeckoLib 5
assets/notchcurrency/geckolib/animations/npc_<id>.json     GeckoLib 5
assets/notchcurrency/textures/entity/npc_<id>.png          both
```

3. Enable the pack and reload **once**, after all bundles are written. Never once per bundle.

The mod already ships both layouts for `notch_npc`, so this follows an existing pattern.

### Three GeckoLib package layouts, not two

Learned the hard way while building clip enumeration:

| Versions | GeckoLib | Cache class |
|---|---|---|
| 1.20.1, 1.21.1 | 4.x | `software.bernie.geckolib.cache.GeckoLibCache` |
| 1.21.11 | 5.4 | `software.bernie.geckolib.cache.GeckoLibResources` |
| 26.1.2, 26.2 | 5.5 | `com.geckolib.cache.GeckoLibResources` |

1.21.11 is on GeckoLib 5 but still under the old package. Any new code touching these caches needs
all three branches.

---

## Per-NPC resolution

`NotchNpcGeoModel` currently returns fixed constants. It becomes a lookup on the NPC's bundle id.

- **GeckoLib 4** hands `getModelResource` the animatable directly. Read `getModelId()` off it.
- **GeckoLib 5** hands it a `GeoRenderState`. Add a `modelId` field to `NotchNpcRenderState` and fill
  it in `extractRenderState`. This is the same door `getTextureResource` already uses for skin
  values, so it is a known-good pattern rather than new ground.

`NotchNpcRenderer` routes on the prefix:

```java
state.useGeo = MODEL_APPLY.equals(model) || model.startsWith("npc:");
```

Everything downstream (nameplate, talk bubble, billboard) already works on the GeckoLib path and
needs no changes.

### When a bundle is missing

The same shape as the missing-clip handling already built: fall back to the built-in model, and say
so in the editor rather than pretending it is fine. An NPC whose bundle was deleted should still be
editable and re-assignable, never a crash and never an invisible entity.

---

## Sharing it with the server

The step that makes a model made by one player work for everybody.

1. On Create, the client writes the bundle locally and offers to upload it.
2. The server stores it in the world folder and holds the hash.
3. On join, the server sends its bundle list as id plus hash. The client compares against what it
   has and asks for anything missing or stale.
4. Transfers arrive chunked, are written to the cache, and trigger **one** reload at the end.

`CurrencyPackGenerator` already does the write, enable and reload half of this for currency
textures, so the risky part has a working precedent in the codebase.

### What this has to get right

This writes server-supplied files to a player's disk, so it carries all the security weight:

- **Creating and uploading is op-only by default**, with a config flag to open it up. It writes to
  the server and pushes to every client, which is not a thing any player should be able to do
  unprompted. Consistent with how the mod already gates value-minting dialogue actions.
- Sandbox to the bundle folder and nowhere else.
- Bundles arrive as an id plus named blobs, never as a path from the wire. Reject any name with
  `..`, a separator, or a non-safe character.
- Cap per-file size, file count per bundle, and total bundles.
- Validate that the JSON parses and the PNG really is a PNG **before** writing.
- Hash bundles so an unchanged one is not re-sent every join.

---

## What needs proving

Honest list of what cannot be verified without running the game:

1. **Does a mid-session reload re-bake GeckoLib's caches?** `CurrencyPackGenerator` proves textures
   and lang reload fine. GeckoLib registers its own reload listener, so it should re-scan, but this
   is the assumption the whole design rests on.
2. **Reload cost.** A resource reload causes a visible hitch. Fine on join, annoying if frequent.
   Needs measuring with a realistic bundle count.
3. **Memory with many bundles.** Each baked model is held for the session. Ten is certainly fine, a
   hundred needs checking.
4. **Hitbox from the manifest.** Per-NPC scale already drives the hitbox, but an arbitrary
   width/height pair is a separate mechanism and may need an attribute rather than a scale.

Item 1 is a half-hour spike and it decides whether the rest is worth designing further.

---

## Staging

Each stage is useful on its own and leaves the mod shippable.

### Stage 0: the spike

Hand-write a second GeckoLib model into the generated pack, reload mid-session, confirm GeckoLib
picks it up. No format, no UI. Answers the question the design rests on.

### Stage 1: format and loading

Bundle format, scanner, validator, pack writer. Bundles placed by hand appear in the model picker.
Works in singleplayer. Proves the concept end to end with no UI work.

Touches: a new loader class, `CurrencyPackGenerator`, `NotchNpcModelPickerScreen`.

### Stage 2: per-NPC resolution

`NotchNpcGeoModel` resolves from the bundle id. `npc:` prefix routing. `modelId` on the render state.
Clip roles read from the manifest. Manifest scale and hitbox applied. The stage where custom NPCs
actually look different from each other.

Touches: `NotchNpcGeoModel`, `NotchNpcRenderState`, `NotchNpcRenderer`, `NotchNpcEntity`.

### Stage 3: the creation screen

Drop folder, Open Folder button, file and clip pickers, validation, preview, Create. This is the
stage that turns it from a modder feature into a player feature.

Touches: a new screen, the loader.

### Stage 4: server sharing

Upload on create, bundle list on join, chunked transfer, sandboxed write, batched reload, op gate.
The stage that makes it feel like magic, and the one carrying the security weight.

Touches: `NotchPackets`, `ServerPacketHandlers`, `CurrencyPackGenerator`, a new transfer class.

---

## What stays out of scope

Worth naming so the feature does not sprawl:

- **Behaviour stays the mod's.** A bundle supplies looks and motion. Roles, dialogue, schedules,
  factions and trading remain mod features configured in the editor.
- **No per-bone collision.** One hitbox per NPC, as now.
- **No scripting in bundles.** Data only. Nothing in a bundle should ever be executed.
- **No in-game modelling.** Blockbench is the tool. The mod imports what it makes.
