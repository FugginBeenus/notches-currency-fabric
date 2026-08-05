# Changelog

## 0.7.1

No behaviour changes. Every em dash is gone from the mod's writing, in-game text included, so the
punctuation reads the way a person writes rather than the way a machine does.

Most lines just changed punctuation. A few were reworded where no punctuation mark read well:

- A recruiter's join button says **Join for 100 Coins** instead of "Join - 100 Coins".
- Turning in a bounty you're short on says **You need 5 Zombie (you have 2)**.
- Config labels group with brackets, so **Auction listing fee (max)** instead of "Auction listing
  fee - max".

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
