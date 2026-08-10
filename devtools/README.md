# devtools

Nothing in here is compiled or shipped. It sits outside `src/`, so Gradle never sees it.

## ShareCodeHarness

Checks that an NPC share code written by one Minecraft version still means the same thing when
another version reads it. Worth re-running whenever a new version is added to the tree, or whenever
anything in `writeConfig` / `readConfig` changes.

It builds one deliberately awkward NPC, writes its code out on each version, then reads every code
back on every version and reports each key that went missing or came back different. The check is
strict: it re-encodes the rebuilt NPC and compares the code byte for byte, falling back to a
key-by-key diff to say what moved.

### Running it

Put the class back where it can compile, and call it:

```bash
cp devtools/ShareCodeHarness.java src/main/java/net/fugginbeenus/notchcurrency/npc/
```

Then add this as the first line of `NotchCurrency.onInitialize`:

```java
net.fugginbeenus.notchcurrency.npc.ShareCodeHarness.init();
```

Then:

```bash
./devtools/sharetest.sh export && ./devtools/sharetest.sh import
```

Export runs first on all five versions and leaves a `.code` and a `.dump` per version; import then
reads every code on every version and writes `report-<version>.txt` next to them. The script drives
`runClient` with quickPlay, because the harness hooks player join: equipment is written through the
item codec, which needs the registry manager, and that is not published until a world is in play.

Remember to take the class and the `init()` call back out before releasing.

### What it found, and what is expected now

Equipment used to break silently across the 1.21 line, because item stacks moved from tags to
components there. Fixed by `StackData.writePortableStack`, which writes the item id and count plainly
alongside the native form and only trusts the native form when it agrees about which item it is.

Enchantments turned out to have their own three eras, narrower than the item split: `1.20.1`,
`1.21.1`, and `1.21.11` upwards, with 1.21.1 able to read the newest group but not the reverse. They
now travel by name and level in the portable block, so they cross too.

The rest of what a piece of gear looks like goes the same way: damage, custom name, dye and armour
trim. Each is written plainly next to the item id, and put back on the other side. Anything the
reading version does not recognise, such as a trim pattern it has never heard of, is skipped rather
than guessed at. Whatever is left over beyond these, custom model data say, still rides only in the
native block and so crosses within an era but not between.

A clean run is **25 of 25 pairs**, with the mainhand keeping `sharpness=3`, the helmet keeping its
damage, name and trim, the boots keeping their dye, and the bread keeping its stack of three. The comparison
deliberately ignores anything under `Equip.<slot>.Native`, which is the stack in whatever shape the
reading version uses and is meant to be rewritten on the way through, so the report prints the
mainhand and offhand in full instead: that is where you check enchantments actually survived.

The fixture covers every key `writeConfig` writes, including the ones it only writes when non-empty
(`Actions`, `Waypoints`, `PoseBeforeSchedule`, and the action lists nested in dialogue choices and
schedule entries). `PoseBeforeSchedule` has no setter, so export puts it straight into the tag;
reading it still goes through `readConfig`, which is the half that has to survive.

Two traps in the runner, both already handled, worth not reintroducing:
- The output directory must not contain spaces. `JAVA_TOOL_OPTIONS` splits `-D` values on
  whitespace, and this repo lives under a path with spaces, so it defaults to `/tmp`.
- `local a=$1 b=$2 c=$b` does not work: every right hand side is expanded before any assignment
  happens, so `c` comes out blank.
