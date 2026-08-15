# Notch Currency Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1%20%7C%201.21.1%20%7C%201.21.11%20%7C%2026.1.2%20%7C%2026.2-green.svg)](https://www.minecraft.net/)
[![Fabric API](https://img.shields.io/badge/Fabric_API-required-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/FugginBeenus/notches-currency-fabric)](https://github.com/FugginBeenus/notches-currency-fabric/releases)

A server-side economy mod for Fabric. It started as a simple currency + ATM system and has
grown into a full economy suite: player shops, an auction house, a deeply customizable NPC system,
and a wide range of money sinks and faucets designed to keep a server's economy balanced.

### Supported versions

Grab the jar matching your game version. Every version is built from the same source, so they all
have the same features.

| Minecraft | Jar |
|---|---|
| 1.20.1 | `notchcurrency-<version>+1.20.1.jar` |
| 1.21.1 | `notchcurrency-<version>+1.21.1.jar` |
| 1.21.11 | `notchcurrency-<version>+1.21.11.jar` |
| 26.1.2 | `notchcurrency-<version>+26.1.2.jar` |
| 26.2 | `notchcurrency-<version>+26.2.jar` |

Requires Fabric API, and GeckoLib for the animated NPC model.

> Heavily inspired by the Origin Realms economy system. The core concept isn't mine, but all the code and assets are written from scratch. I'm still fairly new to modding so development may be slow. Open to collaboration if anyone's interested.

## Features

### Currency & Banking
- Secure, server-side validated virtual balance with persistent storage and an audit log
- ATM / Bank blocks for depositing physical coins and withdrawing your balance
- Always-visible balance HUD with a smooth animated counter and a custom coin glyph (chat, HUD, item)
- Transaction receipts (`/receipts`): your recent history of what moved, what for, and when

### Player Shops
- Code-drawn browse, manage, and listing-editor screens, fully paginated (up to 27 listings)
- Flexible pricing: sell for coins, for a bartered item, or both at once
- Live earnings collection, name/greeting/open controls, and optional shop rent as a sink
- Color the shop title with `&`-codes in the name (`&6Golden Goods`), using the same codes as dialogue
- Each shop is run by an NPC with the Shop role; add dialogue and shoppers get the conversation
  first with a "Browse the shop" option, plus an optional goodbye line when they close the screen

### Auction House
- Buy-now and timed auctions with bidding, offline-safe outbid refunds, and a winnings mailbox
- GUI-first flow: browse, list, bid, and cancel in-screen
- Scaling fees: listing fee (flat + percent, capped) and sale tax, both configurable

### Mail
A mailbox block. Place one and it's yours: it puts its flag up when something is waiting, and
mounts on the floor or on a wall depending where you put it. Break it and the mail is still there
when you put a new one down.

Mail arrives as a **parcel item**, not a list in a screen. A parcel holds up to six stacks plus
coins, says who sent it and what's inside on its tooltip, and can be carried off, dropped in a
chest, handed to somebody, or opened whenever you like.

- **Inbox**: a grid of parcels. Drag one out, or Take all to unwrap the lot
- **Outbox**: pick a recipient from everyone with a mailbox, fill the slots, add coins and a note
- Auction winnings arrive as a parcel from the Auction House, so there's one place to look
- If the person you picked is online, a **Trade** button opens a live trade with them instead

### Deep NPC System
A single blank-slate Notch NPC item spawns a fully customizable NPC (built on GeckoLib):
- Appearance: vanilla humanoid, GeckoLib models, or disguise as any entity; preset/player/URL skins
- Behaviors: stationary, wander, follow, patrol, or guard
- Daily schedules: send an NPC to a spot at a time of day, with a route it walks to get there
- Poses (presets + a live custom-pose editor) and a visual branching dialogue studio
- Reactions: have an NPC respond when it's talked to, approached, hurt, killed, or kills something
- Factions: found one at a Recruiter NPC; guards tell friend from foe, dialogue gates on allegiance
- Floating signs: up to four lines hovering above an NPC, for shop boards and titles
- Stats & abilities (health/speed/regen sliders, protection, doors, leashing, day/night visibility)
- Ownership-gated editing, a "pick up" that repacks the config into the item, and saveable presets
- A public API (`NotchNpcApi`) for other mods to spawn NPCs and register custom roles

#### Bring your own model
Model something in Blockbench, animate it, and put it in the game. No JSON to write and no
resource pack to assemble by hand.

Export as a **GeckoLib Animated Model**, drop the files in the import folder (there's a button that
opens it), then Look tab → Choose Model → Manage models → New model. Every field is a picker over
something that exists: the model, texture and animation lists come from the folder, and the clip
list comes from reading the animation file you just chose, so there's never a name to type that has
to match exactly. Clips called `idle` and `walk` are found on their own.

If something's wrong it says so in plain words while you're still on the screen, including the one
that otherwise wastes an evening: *"This model expects a 64 by 64 texture, but that image is 128 by
128."*

**Sharing with a server**: an operator presses Share and the model goes up to the server, which
keeps it in the world folder, so copying the world copies the models. Players get it on their next
join, and a player who already has it downloads nothing. Nothing is pushed at anyone mid-session.

#### Dialogue
Two ways to give an NPC a voice (editor → Talk tab):
- **Quick Lines**: type up to 8 lines; the NPC says one at random in chat each time it's talked to.
  Perfect for greeters and flavor shopkeepers.
- **Dialogue Studio**: full branching conversations. Pages, choice buttons, per-choice actions
  (open its shop/screen, pay/charge coins, give items, run commands) and requirements (has coins,
  has item, is owner, is op; locked choices grey out or hide). Preview plays the conversation
  client-side without saving; nothing runs for real until you talk to the NPC.

**Text placeholders** (work in dialogue pages, choice labels, and quick lines):
| Placeholder | Becomes |
|---|---|
| `%player%` | the name of whoever is talking to the NPC |
| `%npc%` | the NPC's name |
| `%balance%` | the player's coin balance |
| `&` color codes | standard Minecraft colors, e.g. `&6gold`, `&lbold` |

Styles: **Window** opens the conversation screen; **Chat** prints one random page to chat and then
opens the NPC's job (shop, bank, …). Quick Lines auto-selects Chat.

### Economy Roles
Assign any NPC a role: Shop, Banker, Auctioneer, Mailbox, Raffle, Bounty, Dealer (casino),
Enchanter, Cosmetics, or Recruiter (factions), plus admin server-shops.

### Sinks & Faucets
- Enchanter (repair / buy enchants / extract to book), raffle, gambling (slots + coin flip),
  crates & keys, bounty board, loans, and a cosmetics shop
- Sinks: wealth tax, shop rent, auction fees, and an optional Waystone teleport fee
- Villager currency trades: when villagers roll new trades, some have a rare chance to be priced
  in coins instead of emeralds (chance and exchange rate configurable)
- A custom-currency maker: rename the coin and drop in your own art, and it flows everywhere: the
  item, HUD, chat glyph, and every message/GUI. On a server the admin's coin is pushed to every
  player automatically (no hand-distributed resource pack)

### Blocks
Every feature has a physical, hand-modeled block or table to build a casino/marketplace around:
- **ATM**: deposit coins / withdraw balance
- **Ledger Board**: a wide monument that renders the live balance leaderboard right on its face,
  Create-display-board style (updates in real time; no GUI needed)
- **Bounty Board**: a two-tall notice board that opens the rotating bounty list
- **Slot Machine**: animated marquee lights; bet and spin the reels
- **Coin Flip table**: a 3D coin sits on the felt, then pops up, tumbles, and lands on the result
- **Crates** (Common / Rare / Epic): arched treasure chests whose lids swing open on a win
- **Mailbox**: floor or wall mounted, flag up when something is waiting
- Plus the Golden Cache and balloon crates that spawn around the world

### World events and rare finds
- **Balloon crates** drift down over a configured area on a timer, and one balloon is measured out
  for each player from their own height, so nobody has to be at spawn to get a shot at the loot
- **Golden Caches** generate buried under oak trees, in about one chunk in three thousand, with a
  hard cap on how many can sit unopened at once, so flying around uncovering chunks is not a way to
  farm them. Both numbers are configurable
- **Heart Crystals** are the rarest thing in the mod. Eat one for a permanent extra heart, up to two
  extra rows. Dying costs one back, down to none, which a server can turn off with
  `/hearts loseondeath false`. They turn up in crates, balloons and caches at between three
  hundredths and three tenths of a percent, and admin NPCs can sell them

On-screen, taken bounties show in a positional **Bounty Tracker HUD** (toggle with **B**), freely
placeable via ModMenu so it dodges other mods' overlays.

### Admin & Config
- `/eco give|take|set|stats`, a `/baltop` leaderboard, and a physical Ledger Board block
- Optional Discord webhook for admin-relevant transactions
- A full in-game settings screen (ModMenu) with live search; nearly everything is configurable

## Commands (quick reference)
- `/pay <player> <amount>`: send coins
- `/receipts`: your transaction history
- `/baltop`: richest players
- `/trade <player>`: live trade; `/trade offer` / `/trade offers`: offline trade offers
- `/ah`, `/shop`, `/raffle`, `/bounty`, `/crate`, `/loan`, `/slots`, `/coinflip`: feature entry points
- `/shop relink <id>`: lost the NPC running one of your shops? Look at a new one and run this. The
  shop and everything in it was never deleted, it just had nobody standing behind it
- `/faction`: list, join, leave; found, rename or disband your own
- `/bal` / `/balance`: your balance in chat
- `/hearts`: your extra hearts, and whether this server takes one when you die
- `/npc setrole <role>`, `/npc spawn [preset]`, `/eco …`: op tools
- `/adminshop`, `/balloon`, `/cache spawn`, `/givnotches`, `/npcmodels sync`: more op tools

## Dependencies
- Fabric API (required)
- GeckoLib (required): the NPC system is built on it
- ModMenu (optional): opens the config screen
- Waystones (optional): enables the teleport fee, shown when you hover a destination in the waystone menu
- Trinkets (optional): adds accessory slots to NPCs on the equipment screen

## Development Setup

1. Import the project into IntelliJ IDEA
2. Run `./gradlew genSources`
3. Refresh the Gradle project
4. Run the `Minecraft Client` configuration

## Links

- [Modrinth](https://modrinth.com/project/notches-currency)
- [CurseForge](https://legacy.curseforge.com/minecraft/mc-mods/notches-currency)
- [Discord](https://discord.gg/fMpb6retYA)
- [Wiki](https://github.com/FugginBeenus/notches-currency-fabric/wiki)

## Credits

- **FugginBeenus**: initial work and maintenance
- **Player Trade Mod**: UI inspiration
- **Origin Realms**: economy system inspiration
- **EasyNPC**: NPC system inspiration

## Contributing

Bug reports and feature requests are welcome via GitHub Issues. PRs are open if you want to contribute code.
