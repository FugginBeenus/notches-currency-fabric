# Custom NPC models

A design for letting a player build a model in Blockbench, animate it, and have it walk around
in game as a real NPC.

Status: **proposal**. Nothing here is built. Written 2026-08-13.

---

## The short version

Most of the scaffolding already exists. NPCs already carry a per-NPC model id, already carry a
per-NPC scale that drives their hitbox, and the mod already knows how to write a resource pack to
disk, switch it on, and reload it. What is missing is a file format, a loader, and the wiring that
lets the GeckoLib model vary per NPC instead of being one fixed model.

The central design decision: **feed GeckoLib rather than fight it.** GeckoLib scans the resource
manager on reload. If the mod writes a bundle's files into the pack it already generates, GeckoLib
picks them up through its normal path. No cache injection, no custom loaders, no reflection.

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
| Clip enumeration | `Geo.clipNames()` | Reads GeckoLib's live clip cache |

The model id is already a prefix scheme, which is the extension point:

- `humanoid` - the vanilla biped path
- `apply` - the GeckoLib path, one fixed model, textures vary by skin value
- `entity:<id>` - disguised as a vanilla entity

A fourth prefix, `npc:<bundle_id>`, slots in without disturbing the other three.

---

## The bundle

A folder, not a zip. Blockbench exports loose files and a beginner should not have to zip anything.

```
config/notchcurrency/npc_models/
  town_guard/
    npc.json           the manifest
    model.geo.json     Blockbench: GeckoLib Animated Model
    animation.json     Blockbench: animations
    texture.png
```

`npc.json`:

```json
{
  "format": 1,
  "id": "town_guard",
  "name": "Town Guard",
  "author": "Joey",
  "scale": 1.0,
  "hitbox": { "width": 0.6, "height": 1.95 },
  "defaultClip": "animation.town_guard.idle"
}
```

Everything except `id` has a sensible default. A bundle with only the three files and an `id`
should work.

### What the author has to get right

- Export as **GeckoLib Animated Model**, not Java Block/Entity. Different exporter, different JSON.
- Flag transparent parts in Blockbench, or they render opaque.
- Clip names live in the animation file and are theirs to choose. The mod reads them, it does not
  impose names.

---

## Loading

On client start and on `/npc reload`:

1. Scan `config/notchcurrency/npc_models/` for folders containing `npc.json`.
2. Validate (see below). Reject the bundle, not the whole load, on a bad one.
3. Copy into the generated pack under **both** GeckoLib layouts, because the two generations scan
   different paths and the mod supports both:

```
assets/notchcurrency/geo/npc_<id>.geo.json                 GeckoLib 4
assets/notchcurrency/animations/npc_<id>.animation.json    GeckoLib 4
assets/notchcurrency/geckolib/models/npc_<id>.geo.json     GeckoLib 5
assets/notchcurrency/geckolib/animations/npc_<id>.json     GeckoLib 5
assets/notchcurrency/textures/entity/npc_<id>.png          both
```

4. Enable the pack and reload once, after all bundles are written. Never once per bundle.

The mod already ships both layouts for `notch_npc`, so this is following an existing pattern rather
than inventing one.

### Three GeckoLib package layouts, not two

Already learned the hard way while building clip enumeration:

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
- **GeckoLib 5** hands it a `GeoRenderState`. Add a `modelId` field to `NotchNpcRenderState` and
  fill it in `extractRenderState`. This is the same door `getTextureResource` already uses for skin
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

## Multiplayer

The constraint that cannot be engineered away: **Minecraft renders on the client.** Every player who
sees the NPC needs the model, texture and animation locally. This is the game's architecture, not a
mod limitation, and it is why delivery matters at all.

Three options, in increasing order of how good they feel:

1. Every player installs the bundle by hand. Works today with zero code. Miserable.
2. Server resource pack URL. Vanilla feature, but it replaces the player's other packs and needs
   hosting.
3. **The mod ships bundles over its own channel.** The server holds the bundles, tells clients what
   it has on join, and sends any the client is missing. The client writes them into the generated
   pack and reloads.

Option 3 is what `CurrencyPackGenerator` already does for currency textures. The precedent is in the
codebase and working.

### What option 3 has to get right

This writes server-supplied files to a player's disk, so it is the part to be careful with:

- Sandbox to `config/notchcurrency/npc_models/` and nowhere else.
- Reject any path containing `..` or a leading separator. Bundles arrive as a name plus bytes, never
  as a path from the wire.
- Cap bundle size, file count, and total bundles. A malicious server should not be able to fill a
  disk.
- Validate that JSON parses and the PNG is a PNG before writing.
- Hash bundles so an unchanged one is not re-sent every join.
- Batch the reload. One reload after all transfers, not one per bundle.

---

## What needs proving

Honest list of what I cannot verify without running the game:

1. **Does a mid-session reload re-bake GeckoLib's caches?** `CurrencyPackGenerator` proves textures
   and lang reload fine. GeckoLib registers its own reload listener, so it should re-scan, but this
   is the assumption the whole design rests on and it should be tested first, before any format work.
2. **Reload cost.** A resource reload causes a visible hitch. Acceptable on join, annoying if it
   happens often. Needs measuring with a realistic bundle count.
3. **Memory with many bundles.** Each baked model is held for the session. Ten is certainly fine.
   A hundred needs checking.
4. **Hitbox from the manifest.** Per-NPC scale already drives the hitbox, but an arbitrary
   width/height pair is a separate mechanism and may need an attribute rather than a scale.

Item 1 is a half-hour spike and it decides whether the rest is worth designing further. It should be
done before anything else.

---

## Staging

Each stage is useful on its own and leaves the mod shippable.

### Stage 0: the spike

Hand-write a second GeckoLib model into the generated pack, reload mid-session, confirm GeckoLib
picks it up. No format, no UI. This answers the question the design rests on.

### Stage 1: format and local loading

Bundle format, scanner, validator, pack writer. Custom bundles appear in the model picker. Works in
singleplayer and for any client that has the folder. Proves the concept end to end.

Touches: a new loader class, `CurrencyPackGenerator`, `NotchNpcModelPickerScreen`.

### Stage 2: per-NPC resolution

`NotchNpcGeoModel` resolves from the bundle id. `npc:` prefix routing. `modelId` on the render state.
Manifest scale and hitbox applied. This is the stage where custom NPCs actually look different from
each other.

Touches: `NotchNpcGeoModel`, `NotchNpcRenderState`, `NotchNpcRenderer`, `NotchNpcEntity`.

### Stage 3: server delivery

Bundle manifest on join, chunked transfer of missing bundles, sandboxed write, batched reload. The
stage that makes it feel like magic, and the one carrying all the security weight.

Touches: `NotchPackets`, `ServerPacketHandlers`, `CurrencyPackGenerator`, a new transfer class.

---

## What stays out of scope

Worth naming so the feature does not sprawl:

- **Behaviour stays the mod's.** A bundle supplies looks and motion. Roles, dialogue, schedules,
  factions and trading remain mod features configured in the editor.
- **No per-bone collision.** One hitbox per NPC, as now.
- **No scripting in bundles.** Data only. Nothing in a bundle should ever be executed.
