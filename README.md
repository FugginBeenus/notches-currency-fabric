# Notch Currency Mod

[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)](https://www.minecraft.net/)
[![Fabric](https://img.shields.io/badge/Fabric-0.92.6-blue.svg)](https://fabricmc.net/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![GitHub Release](https://img.shields.io/github/v/release/FugginBeenus/notches-currency-fabric)](https://github.com/FugginBeenus/notches-currency-fabric/releases)

A server-side economy mod for Fabric 1.20.1. It started as a simple currency + ATM system and has
grown into a full economy suite: player shops, an auction house, a deeply customizable NPC system,
and a wide range of money sinks and faucets designed to keep a server's economy balanced.

> Heavily inspired by the Origin Realms economy system. The core concept isn't mine, but all the code and assets are written from scratch. I'm still fairly new to modding so development may be slow — open to collaboration if anyone's interested.

## Features

### Currency & Banking
- Secure, server-side validated virtual balance with persistent storage and an audit log
- ATM / Bank blocks for depositing physical coins and withdrawing your balance
- Always-visible balance HUD with a smooth animated counter and a custom coin glyph (chat, HUD, item)
- Transaction receipts (`/receipts`) — your recent history: what moved, what for, when

### Player Shops
- Code-drawn browse, manage, and listing-editor screens, fully paginated (up to 27 listings)
- Flexible pricing: sell for coins, for a bartered item, or both at once
- Live earnings collection, name/greeting/open controls, and optional shop rent as a sink
- Color the shop title with `&`-codes in the name (`&6Golden Goods`) — same codes as dialogue
- Each shop is run by an NPC with the Shop role; add dialogue and shoppers get the conversation
  first with a "Browse the shop" option, plus an optional goodbye line when they close the screen

### Auction House
- Buy-now and timed auctions with bidding, offline-safe outbid refunds, and a winnings mailbox
- GUI-first flow: browse, list, bid, and cancel in-screen
- Scaling fees: listing fee (flat + percent, capped) and sale tax, both configurable

### Deep NPC System
A single blank-slate Notch NPC item spawns a fully customizable NPC (built on GeckoLib):
- Appearance: vanilla humanoid, GeckoLib models, or disguise as any entity; preset/player/URL skins
- Behaviors: stationary, wander, follow, patrol, or guard
- Poses (presets + a live custom-pose editor) and a visual branching dialogue studio
- Stats & abilities (health/speed/regen sliders, protection, doors, leashing, day/night visibility)
- Ownership-gated editing, a "pick up" that repacks the config into the item, and saveable presets
- A public API (`NotchNpcApi`) for other mods to spawn NPCs and register custom roles

#### Dialogue
Two ways to give an NPC a voice (editor → Talk tab):
- **Quick Lines** — type up to 8 lines; the NPC says one at random in chat each time it's talked to.
  Perfect for greeters and flavor shopkeepers.
- **Dialogue Studio** — full branching conversations: pages, choice buttons, per-choice actions
  (open its shop/screen, pay/charge coins, give items, run commands) and requirements (has coins,
  has item, is owner, is op — locked choices grey out or hide). Preview plays the conversation
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
Enchanter, or Cosmetics — plus admin server-shops.

### Sinks & Faucets
- Enchanter (repair / buy enchants / extract to book), raffle, gambling (slots + coin flip),
  crates & keys, bounty board, loans, and a cosmetics shop
- Sinks: wealth tax, shop rent, auction fees, and an optional Waystone teleport fee
- Villager currency trades: when villagers roll new trades, some have a rare chance to be priced
  in coins instead of emeralds (chance and exchange rate configurable)
- A custom-currency maker that generates a resource pack from your coin texture + name

### Admin & Config
- `/eco give|take|set|stats`, a `/baltop` leaderboard, and a physical Ledger Board block
- Optional Discord webhook for admin-relevant transactions
- A full in-game settings screen (ModMenu) with live search — nearly everything is configurable

## Commands (quick reference)
- `/pay <player> <amount>` — send coins
- `/receipts` — your transaction history
- `/baltop` — richest players
- `/trade <player>` — live trade; `/trade offer` / `/trade offers` — offline trade offers
- `/ah`, `/shop`, `/raffle`, `/bounty`, `/crate`, `/loan`, `/slots`, `/coinflip` — feature entry points
- `/npc setrole <role>`, `/npc spawn [preset]`, `/eco …` — op tools

## Dependencies
- Fabric API (required)
- GeckoLib (required — the NPC system is built on it)
- ModMenu (optional — opens the config screen)
- Waystones (optional — enables the teleport fee)

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

- **FugginBeenus** — initial work and maintenance
- **Player Trade Mod** — UI inspiration
- **Origin Realms** — economy system inspiration
- **EasyNPC** — NPC system inspiration

## Contributing

Bug reports and feature requests are welcome via GitHub Issues. PRs are open if you want to contribute code.
