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

A clean run is **25 of 25 pairs**. The comparison deliberately ignores anything under
`Equip.<slot>.Native`, which is the stack in whatever shape the reading version uses and is meant to
be rewritten on the way through.

Four keys are still not covered, because `writeConfig` omits them when empty and the fixture leaves
them so: `Actions`, `Waypoints`, `PoseBeforeSchedule`, and the nested action lists inside dialogue
choices and schedule entries.
