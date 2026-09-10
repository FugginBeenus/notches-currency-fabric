# Changelog

## 0.12.5: Sounds and particles

An NPC can now make noise, and it can glow. Both come with a way to bring in your own files and
share them with a whole server.

### Sounds

**Talk tab, then Sounds.** Six slots: a **voice**, an **ambient** sound on a timer, and sounds for
**hurt**, **death**, **step** and **angry**. Pitch shifts all of them at once, so one villager noise
becomes a whole cast.

The ambient sound has a gap and a mode. **Exact** plays on the dot, for a blacksmith at an anvil.
**Random** wanders, for chatter. The boxes fill in as you type, and a typo is refused in chat
instead of saving silently.

**Your own sounds.** Drop an `.ogg` in a folder, name it, done. It is `notchcurrency:yourname` from
then on. On a server an operator's import goes up at once and everyone else gets it on their next
join. Nothing is pushed to players already online, on purpose.

### Animations on a timer

**Pose tab, Now and then.** A second animation that plays once on a timer, exact or random, then
drops back to the idle loop. Set it and the anvil sound to the same gap and the swing lands with the
clang. They used to drift apart. They do not any more.

### Particles

**Manage tab, then Particles.** Build an effect from up to four layers, six shapes, spin and rise,
and give it to any NPC to wear all the time or fire from any reaction, dialogue choice or quest.

Every effect is drawn on your own game with a one tick lifetime, so a ring of glyphs holds together
and turns as one thing, and moving it costs the server nothing.

Minecraft's own particles are all there with plain names, sorted into groups, with a picture when
you hover. A few of them ignore where you put them, so there are **seven tame versions of ours**
that wear the same pictures and behave. And **eight slots for your own PNGs**, shared the same way
sounds are.

Four starter effects come with a fresh world: wizard, ghost, holy and cursed. Pull them apart.
On an older world, `/npcfx starters` adds them.

### Economy

A pass over the money side, looking for holes.

- **Shop buying is airtight now.** The server takes the quantity from the client, and it only
  checked that it was above zero. A hacked client could send a number so big the price wrapped
  around, and the shop handed over the items for nothing. Buying is capped at 256 per click, the
  price maths cannot wrap, and if the withdrawal fails the sale stops. The sell side already did
  all of this.
- **Loans have a debt ceiling.** An overdue loan grew 20% a cycle forever, so a player who left for
  two months came back owing millions. Interest and fees now stop at **3x the borrowing limit**.
  Config: **Debt ceiling**, 0 for the old behaviour.
- **Big payments ask first.** `/pay` for 10,000 or more shows a **[Confirm]** button. Click it, or
  run the same command again within 30 seconds. Config: **Confirm /pay above**, 0 to turn it off.
- **Starting balance.** A config option to hand every brand-new player a few coins on their first
  join. Off by default.
- **Savings interest.** A config option to pay every account a small percent of its balance on a
  timer, capped per payout. Off by default, it creates money.
- A balance can no longer wrap past the top of the number range.
- A listing with some stock left, but not enough for one sale, looked buyable and then did
  nothing when clicked. It now shows the red cross, and the tooltip says how many a sale needs.
  The owner's screen shows the same.

### Also

- A **fly ceiling** for NPCs with gravity off, on Stats & Abilities, next to the toggle.
- Three quiet guards keep particles from ever hurting a server: a 32 block range, a cap per shape,
  and a per player budget every tick.

## 0.12.0

The NPC becomes a storyteller. Quests, an animation maker, and a reactions system that can finally
tell one situation from another.

### Quests

A quest is a job an NPC hands out. **Manage tab -> Quests** to write one, then any NPC can give it
out by name, from a reaction or from a line of dialogue. Write it once, hand it out anywhere.

Five things a quest can ask for: **kill** something, **collect** something, **talk to** someone,
**go to** a place, or **deliver** items to someone. Every one of them pays coins, an item, or both.

Each quest says how it ends. **Pays now** settles the moment you do the thing, which is right for
"go say hi to the blacksmith". **Hand back** sends the player to an NPC, which is right for a story.
Whoever gave the quest accepts it back without any extra setup, so a hand-back quest works out of
the box.

Two ways to build a chain. **Needs** is a gate: the quest exists but is refused until another one is
finished. **Then** is a handoff: finishing one starts the next on the spot.

**Faction quests** are for guilds. Only members can take one, and faction mates within 48 blocks
share the progress, so three people clearing a cave all count the kills. Everyone gets their own
full reward.

`/quests` shows a player what they are on, with a hand-in button as a safety net if the giver has
gone. The tracker on screen wraps long text now, and servers can set its colours or turn its
progress bar off.

### The animation maker

**Manage tab -> Animations.** Build a loop of poses and any NPC can play it, either forever from the
Pose tab or once from a reaction, so a guard can wave when you talk to it and go back to standing.

It works the way an animation tool should. Keyframes sit **anywhere on the timeline**, not at fixed
spacing, so you can do a fast snap and a slow settle. A **dope sheet** shows a lane per body part, so
you can see which limb is keyed when and click straight to it. A keyframe **only holds the parts you
touch**; everything else slides between the keyframes either side, which is how Blockbench behaves
and the reason two limbs can move on different rhythms.

Every keyframe has **easing**, and new ones default to eased both ways, because nothing in life moves
at a constant speed and a linear wave reads as a robot.

A **mannequin** shows the frame you are editing, with a **ghost** of the previous pose behind it.
**Mirror** flips left and right, which halves the work on any walk cycle. And you can grab a limb on
the mannequin and drag it, rather than hunting for the right slider.

Blockbench models are untouched: they already animate through GeckoLib from their own file.

### Reactions that can tell situations apart

**Any action can carry a condition.** That is one small change with a large result: a single NPC can
now greet a stranger, nudge someone mid-quest, take the hand-in and thank them afterwards, all from
one screen with no dialogue tree.

Because both editors share the same actions, the Dialogue Studio got it for free.

New actions: **heal the player**, **give an effect**, **teleport** (same world or another, landing on
solid ground with room to stand), **give quest**, **turn in quest**, and **play animation**.

Two new triggers: **when an NPC is near**, for chatter between NPCs, and **when a quest is finished**.

An NPC that reacts to another NPC stops walking, turns to face them, and carries on afterwards. It
only fires with a player in range, so an empty town stays quiet.

### Dialogue pages can branch

A page can carry **two conditions**, and the NPC opens on the first page that matches. So one NPC has
a page per quest state instead of a button per state, and the greeting stops coming back once you
have taken the job.

More ways to gate a choice, too: faction, time of day, and experience level, alongside the quest
tests.

### One place for how an NPC looks

Seven settings that were scattered across three screens now live on one **Appearance** screen with
tabs. It also gained **tint**, **fade**, and a **hitbox** you can resize and see while you drag it.

### Shops

Admin shops are gone as a separate thing. They are **player shops with an admin flag**, which means
one shop system, one set of screens, and every player feature works on a server shop. Existing admin
shops convert themselves.

Along the way shops gained **restocking** on a game or real clock, a **cap on what one player can
buy**, prices that **drift with stock**, and the ability to **buy from players**, not only sell.

### Fixes

- **Tints and fades did nothing on 1.20.1 and 1.21.1.** The hook they used only exists from 1.21.11,
  so on the two older versions the code was skipped entirely and never told anyone.
- **Admins could not see admin-only actions on 1.21.11 and up.** The check asked whether the player
  was a server player, which is never true on the client, so it hid them from everyone.
- **1.20.1 crashed on any NPC** once animations landed, because the entity registers its fields in
  two places and the new ones went into one.
- The hitbox preview drew behind the model on older versions, the quest tracker sat under
  advancement toasts, and the new screens were washed out by the 1.21 menu blur.
- Item and mob fields **autocomplete as you type** now, so a typo cannot quietly save as Air.
- **NPCs showed up empty handed on 1.21.11 and up.** Armor and held items never reached the drawing
  step, so nothing an NPC wore or carried was visible.
- **Player skins never loaded.** On 1.21.11 the picture was built on the wrong thread; on 1.20.1 and
  1.21.1 the download hung up before the game could read it. A failed name also retried every frame
  until Mojang rate limited us, so it now waits a minute between tries.
- **A shop could not buy an item without also selling it.** It demanded a sell price first, so
  players had to spend coins to make a deal that pays coins. Buy-only rows read the right way round
  now instead of showing "free" and an empty stock count.

## 0.8.1

Small things, all aimed at the same problem: a street of NPCs reading as a row of identical
mannequins.

### Names and titles
- **Names take colour codes.** `&6Carol` is a gold name, using the same codes dialogue, signs and
  shop titles already understand. Nothing new to turn on, just type them in the name field.
- **A title under the name.** Blacksmith, Harbourmaster, Night Watch, whatever the NPC is for. It
  lives on the floating text screen with the sign, since one is text above the head and the other is
  text below the name. That screen is now called **Floating Text** and the Look tab button opens it.

### Voices
An NPC can be given a voice: a short sound when it is spoken to, and on every line it says. Twelve
vanilla voices to pick from, with a pitch control from 50% to 200% beside it.

The pitch is the part worth playing with. The same villager grunt at 70% and at 130% reads as two
completely different people, so a short list of voices and a slider gets you a whole cast. Cycling
through them plays each one, so you can hear what you are choosing. Talk tab, at the bottom.

### Schedules
- **A Stand entry can face a direction.** Pick a bearing and the NPC holds it once it settles, so a
  shopkeeper faces across the counter instead of whichever way it happened to arrive. It still turns
  to whoever talks to it and goes back afterwards.
- Marking a spot points the NPC back the way you came from, so marking a counter from the customer's
  side leaves the NPC looking at the customer.

If you built a Stand entry before this update, it has been quietly storing a direction all along and
never using it. It will start using it now, so a few NPCs may turn on their heels the first time you
load the world.

## 0.8.0

### Daily schedules
An NPC can now keep hours. Give it a list of times and what it should be doing at each, and it runs
its own day: asleep in a bed overnight, up and wandering before dawn, at the counter when the shop
opens, walking a round in the afternoon.

Moves tab, **Daily Schedule**. Each block of the day gets a start time, one of four things to do
(sleep in a bed, wander an area, stand at a spot, walk the patrol route), and a spot, which you mark
by right-clicking the world with the tool it hands you. The same tool you already use for patrol
routes, so there is nothing new to learn.

- **Opening hours, if you want them.** Turn *Keep opening hours* on and each block decides whether
  the NPC's role can be used, so a shop that is closed says so in words you write instead of opening
  at three in the morning. Leave it off and the schedule is pure choreography, with the shop open
  around the clock. Dialogue keeps working either way, so you can write closed-hours conversations.
- **Things that happen on the hour.** Any block can run actions as it begins: say a line, hand
  something over, charge or pay coins, run a command. This is how a shop restocks at opening and
  announces itself while it does.
- **It survives being left alone.** The schedule is worked out from the clock, so an NPC in an
  unloaded chunk costs nothing and cannot drift. Come back a week later and it is doing whatever it
  should be doing at that hour, with no catch-up and nothing to repair.
- Entries that still need a spot are marked, counted, and fixable in a couple of clicks with
  **Fix next**, which hands you the tool aimed at the right one.
- Schedules need a sunrise, so the screen says so plainly in the Nether and the End rather than
  letting you build a day that would never advance.

### Share an NPC as text
Presets have always saved NPCs to files, but those land in the server's config folder where no
player can reach them. An NPC could be copied anywhere inside one world and nowhere outside it.

A share code is just text. Copy it off an NPC, paste it onto another, and it carries the whole build:
look, pose, dialogue, reactions, gear. It travels however you already talk to people. The same text
is what a `.npc` file holds, so in single player you can hand over a file instead, and that route has
no size limit at all.

Codes coming in are treated as hostile input, since a code is the one thing in the mod written by a
stranger. A code that claims to expand into something enormous is refused rather than parsed, and
oversized pastes are turned away with a pointer to the file route rather than disconnecting whoever
sent them.

### Security
- Paying coins and giving items have been admin-only since 0.6.0, enforced by a sweep that skips any
  NPC already checked. That "already checked" mark travelled inside the NPC's config, so an
  operator's preset landing on an ordinary player's NPC kept its pay actions and was never looked at
  again, which is a coin faucet for the price of loading a preset. The mark is now stripped along
  with everything else world-specific, so a foreign config always gets checked on arrival.

### Fixes
- Sleeping NPCs lie in the bed properly, in the head half, instead of standing beside it or hanging
  off the end.
- A sleeping NPC stops turning to watch passers-by.
- A bed mined out from under a sleeping NPC wakes it rather than leaving it asleep on nothing.

## 0.7.1

No behaviour changes, and nothing moves or renames.

Em dashes are gone from the mod's writing. In-game text says exactly what it said before, with a
plain hyphen where the long dash used to be.

## 0.7.0

### Factions
A faction is a real thing on the server (name, colour, founder, members), and players join in person
rather than by command.

- **Recruiter NPC** (new role): shows the faction, with Join and Leave.
- Set an NPC to Recruiter and it offers to **found a faction** if you don't already run one: name,
  colour, motto, joining fee, open or closed. All editable later from the same NPC.
- The faction lives on the server and a recruiter only points at it, so **losing the NPC never costs
  you the faction**. Place another and point it at the same one. `/faction` works with no NPC at all.
- **NPCs take sides**: point one at a faction from the Role tab and it won't turn on its own people.
  *Fight rivals* takes on other factions while leaving the unaffiliated alone.
- Dialogue gains an **In faction** requirement.

### Floating signs
Up to four lines hovering above an NPC: price boards, titles, welcomes. Colours and the
`%player%`/`%npc%`/`%balance%` placeholders work, and the balance shown is the reader's own.

### NPC combat
- **Protect owner**: fights whoever its person is fighting, both directions.
- **Fight monsters** without needing the Guard behaviour, so followers are useful in a fight.
- All combat options grouped on the Moves tab instead of split across two screens.

### Fixes
- Mobs used as NPC models animate again. Walking and attacking were frozen.
- Modded mobs that never spawn naturally now appear in the model picker.
- The Basic role no longer says the NPC has no job.
- Per-axis NPC size, set from the Move & Rotate panel; the floating name can be nudged up or down.
- Coin and item payouts are cleared from NPCs whose owner isn't an operator. 0.6.0 only did that when
  an NPC was next saved, so older dialogue kept paying out.

## 0.6.0

### NPC Reactions
NPCs can now react to things that happen to them, not just to a dialogue choice being clicked. Editor →
Manage → **Reactions**, then pick a moment and say what should happen:

- **When talked to**: before any dialogue or shop opens
- **When a player comes near**: once as they arrive, re-arming when they leave (range is adjustable)
- **When hurt**: even if the NPC is protected from the damage
- **When killed**, and **when it kills something**

Each moment can run up to five actions: say a line, pay or charge coins, give an item, or run a command.
Lines support the same `%player%`/`%npc%`/`%balance%` placeholders and `&` colours as dialogue. Reactions
travel with the pick-up item and with presets, so a greeter you build once can be stamped anywhere.

"Say a line" is available as a dialogue choice action too.

### Security
- Actions that create value (**paying coins** and **giving items**) now require operator permission,
  the same as the command actions. Previously any NPC owner could write a dialogue choice that paid the
  player who clicked it, which is an unlimited money supply on a public server. Charging coins is
  unchanged and still available to every shop owner.

## 0.5.4

### Fixes
- Purchase messages now say how many items you actually bought. After the 0.5.3 fix the buyer and the
  seller were both told "1x" no matter how big the listing's stack was, and the listing's sold counter
  was under-counting to match.

### Performance
- With an animation pack installed (Fresh Animations, Fresh Moves), NPCs more than 28 blocks away now
  render on a model layer the pack doesn't touch, so a crowd stops paying for animation you can't see
  at that range. Nearby NPCs are animated by the pack exactly as before, and nothing changes for players
  without one.

## 0.5.3

### Fixes
- **Shops now sell the whole stack the listing shows.** A listing of 32 sculk sensors for 15 coins was
  charging the full 15 but handing over a single item, and only taking one off the stock. Both game
  versions were affected. Listings that sell single items are unchanged.
- Removing a listing with more than a stack of items in it returns all of them. It used to stop at 64.
- Shift-clicking a listing buys whole stacks of it instead of loose items.

### Performance
- Crowds of NPCs are much lighter to render: the skin's outer layer is dropped past 20 blocks and
  floating names past 32, so a village full of NPCs costs closer to what ordinary mobs do.

## 0.5.2

### Fixes
- The waystone teleport fee now shows the coin in front of the price, drawn from the actual coin item,
  so it picks up custom currency art. It was rendering with no coin at all in 0.5.1.

## 0.5.1

A tester-feedback patch on top of 0.5.0.

### Fixes & polish
- NPCs hold still and face you while you're interacting with them, instead of wandering off mid-conversation
- Statue-posed NPCs stay frozen even with animation packs (Fresh Animations / Fresh Moves) installed.
  Other poses still get animated by the pack, so NPCs keep their life
- Waystone teleport fees now appear in a hover tooltip on each destination (the dimensional fee on
  cross-dimension trips); this also fixes the fee not showing at all on 1.21
- The NPC model picker no longer hitches on large modpacks. Previews build as you scroll to them
  rather than all at once

## 0.5.0

The multi-version release: the mod now ships for **Minecraft 1.20.1 and 1.21.1** from one codebase,
plus a round of tester-requested features.

### Multi-version
- Full 1.21.1 port. Every feature works on both versions; download the jar matching your game
- One shared codebase (Stonecutter), so future fixes and features land on all supported versions

### NPCs
- **Equipment screen rework**: shop-style layout with a live NPC preview (gear shows on the model
  the moment it's equipped), plus hover hints on every slot
- **Trinkets integration** (optional): eight accessory slots on NPCs when the Trinkets mod is
  installed; contents persist with the NPC

### Integrations
- **Waystones**: each destination in the waystone selection menu now shows its teleport fee, with
  the dimensional fee on cross-dimension trips

### Fixes & polish
- The balance HUD ducks out of the way of long chat messages (1.21)
- On 1.21, NPC nameplates sit at the standard height (the pose-follow hook no longer exists there)

## 0.4.0

The "art + polish" release: every placeholder block and item now has real, hand-made art (some of
it animated), the Ledger Board shows the leaderboard live in-world, and a large round of tester
feedback is folded in.

### Blocks & in-world art
- Real models + textures for the **Slot Machine** (animated marquee lights), **Bounty Board**
  (two-tall notice board), **Crates** (Common/Rare/Epic arched chests whose lids swing open on a
  win), and the **Coin Flip table** (furniture-scale, with a 3D coin that pops up, tumbles, and
  lands on the result face, heads or tails)
- **Ledger Board** renders the live top-balances leaderboard directly on its face, Create
  display-board style (its own block entity + renderer; updates in real time)
- Cleaner Crate Key sprite; the NPC spawn item now uses the shopkeeper spawn egg

### Currency maker
- The custom coin name now flows into **every** message and GUI, not just the item
- On a server, the admin's coin art + name is **pushed to every player on join**: no
  hand-distributed resource pack

### NPCs & dialogue
- Any NPC can talk (Greeter role retired); role NPCs get a "Browse the shop"-style entry choice in
  their dialogue that owners can edit or remove
- Optional chat greeting → open GUI → goodbye line flow for shops
- Dialogue command actions are admin-only (hidden for non-ops, and only run when the NPC's owner is
  an operator)

### Economy
- **Villager currency trades**: villagers can rarely roll trades priced in coins instead of
  emeralds (configurable chance + rate)
- Positional **Bounty Tracker HUD** (toggle **B**), placeable via ModMenu

### UI & config
- Rebuilt the settings screen (ModMenu) with live search and inline editing
- Shop titles support `&`-color codes; the manage screen lists items as trade cards with a
  title-color swatch

### Fixes
- Hardened client-sent packet strings (length caps; URL-skins must be real web URLs)
- Removed the unused Cloth Config dependency

## 0.3.x and earlier
Currency, banking, player shops, auction house, the deep NPC system, and the full suite of economy
sinks and faucets. See the git history for details.
