# Custom NPC models

A design for letting a player build a model in Blockbench, animate it, and have it walk around
in game as a real NPC.

Status: **built.** All five stages. Written and built 2026-08-13.

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
  New NPC Model

  Name        [ Town Guard                ]
              saves as: town_guard
  Model       [ solmannen.geo.json        ]   <- files found in _import
  Texture     [ solmannen.png             ]
  Animations  [ solmannen.animation.json  ]
  Idle        [ animation.solmannen.idle  ]   <- clips found in that file
  Walk        [ animation.solmannen.walk  ]
  Flourish    [ (none)                    ]

  [ Import folder ] [ Refresh ]   [ Cancel ] [ Create ]
```

The three clip fields come straight from the roles the animation system already uses. The mod parses
the chosen animation file and lists its `animations` keys, so the player picks from what their file
genuinely contains rather than typing a name that has to match exactly. Clips named `...idle` and
`...walk` are picked out on their own, so a tidily named export needs a name typed and Create
pressed.

No separate id field: the folder name comes from the model name and is shown under it. Every picker
walks backwards on a right click, for a long list.

**No preview before Create.** A model cannot render until it has been written and the resources
reloaded, so there is nothing to preview yet. It is previewed in the model picker afterwards, where
that already works.

Scale and hitbox are not on the screen either. Per-NPC scale is already in the editor's Move, Rotate
and Size panel, and `npc.json` still accepts a hitbox for anyone who wants one.

### The manage screen

A second screen lists what is installed, one row per model with its name, author and clip count, and
a Remove on each. It scrolls past six. New model and Import folder live there too, so everything to
do with models is in one place.

Remove asks twice. The folder goes and there is no getting it back. An NPC still wearing a removed
model falls back to the built-in one, the same as a model that was never installed.

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
| Bundle within size limits | "That model is larger than 4 MB." |

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

1. On Create, the client writes the bundle locally. Share sends it up.
2. The server stores it in the world folder and holds the hash.
3. On join, the server sends its bundle list as id plus hash. The client compares against what it
   has and asks for anything missing or stale.
4. Transfers arrive chunked, are written to the cache, and trigger **one** reload at the end.

### Nothing is pushed at a player who is already playing

Applying a model means reloading resources, and that is a visible hitch in somebody's game. An
operator pressing Share must not be able to reach into everyone else's session and cause one,
because a thing you can do once you can do repeatedly, and that is a way to make a server unplayable
from inside the rules.

So a shared model reaches other players at one of three moments, all of them theirs:

- their next join, where a reload is expected anyway
- `/npcmodels sync`, which fetches for whoever ran it and nobody else
- clicking **[Get it now]** on the line they are shown when they talk to an NPC wearing a model they
  do not have

Until then the NPC falls back to its ordinary look, which is correct rather than broken. The hint
exists because silently looking wrong is worse than saying so.

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

## What was proved, and what bit

**A mid-session reload does re-bake GeckoLib's caches.** Confirmed by `/npc modelspike` on 26.2
(GeckoLib 5.5) and on 1.20.1 (GeckoLib 4), so it holds on both generations. This was the assumption
the whole design rested on.

Three things bit on the way, all now fixed:

1. **Animations never ticked on 1.21.11 and up.** The renderer called GeckoLib's submit pass but
   never its extract pass, and extract is where animations are ticked: submit is handed a render
   state and no entity, so there is nothing in it to read a tick count or a walk speed from. The
   model drew fine and simply held still. This was true of the built-in model too, and predated
   custom models entirely.
2. **Two nameplates.** Running GeckoLib's extract also runs the living renderer's, which fills in
   the vanilla nameplate, and this path already draws its own. The vanilla one is cleared after.
3. **Three GeckoLib package layouts, not two.** See the table above.

Still unmeasured:

- **Reload cost** with a realistic bundle count. The loader stamps the source folders and skips the
  reload when nothing moved, so the common case costs nothing, but a big first load is untested.
- **Memory with many bundles.** Each baked model is held for the session. Ten is fine. A hundred
  needs checking.
- **Hitbox from the manifest.** Per-NPC scale already drives the hitbox, but an arbitrary
  width/height pair is a separate mechanism and may need an attribute rather than a scale.

---

## Staging

Each stage is useful on its own and leaves the mod shippable.

### Stage 0: the spike — DONE

Hand-write a second GeckoLib model into the generated pack, reload mid-session, confirm GeckoLib
picks it up. Answered yes on both GeckoLib generations. `/npc modelspike` is still in the tree and
can be deleted.

### Stage 1: format and loading — DONE

Bundle format, scanner, validator, pack writer. Bundles placed by hand appear in the model picker.
Works in singleplayer. Proves the concept end to end with no UI work.

Touches: a new loader class, `CurrencyPackGenerator`, `NotchNpcModelPickerScreen`.

### Stage 2: per-NPC resolution — DONE

`NotchNpcGeoModel` resolves from the bundle id. `npc:` prefix routing. `modelId` on the render state.
Clip roles read from the manifest. Manifest scale and hitbox applied. The stage where custom NPCs
actually look different from each other.

Touches: `NotchNpcGeoModel`, `NotchNpcRenderState`, `NotchNpcRenderer`, `NotchNpcEntity`.

### Stage 3: the creation screen — DONE

Drop folder, Open Folder button, file and clip pickers, validation, Create. Plus a manage screen
listing what is installed, with Remove on each row. No preview before Create: a model cannot render
until it is written and resources reload, so it is previewed in the picker afterwards instead.

Built as `NpcModelCreateScreen` and `NpcModelManageScreen`.

### Stage 4: server sharing — DONE

Share button per model, bundle list on join, chunked transfer, sandboxed write, batched reload,
op gate.

The defence against a hostile server ended up simpler than the design assumed. Rather than checking
a filename from the wire for `..` and separators, **only four filenames are accepted at all**, and a
name is never joined onto a path. There is no arrangement of characters that escapes the folder,
because the name never builds the destination.

Built as `NpcModelBlob` (the format and the whitelist), `NpcModelStream` (reassembly, bounded),
`NpcModelServerStore` (the world folder copy), `NpcModelShare` (the server side) and
`NpcModelDownloads` (the client side).

---

## What stays out of scope

Worth naming so the feature does not sprawl:

- **Behaviour stays the mod's.** A bundle supplies looks and motion. Roles, dialogue, schedules,
  factions and trading remain mod features configured in the editor.
- **No per-bone collision.** One hitbox per NPC, as now.
- **No scripting in bundles.** Data only. Nothing in a bundle should ever be executed.
- **No in-game modelling.** Blockbench is the tool. The mod imports what it makes.
