## Admin

Most admin commands require **permission level 2** (op).

### Economy Administration

| Command | Description |
| --- | --- |
| `/eco give <player> <amount>` | Give a player coins (faucet) |
| `/eco take <player> <amount>` | Remove coins from a player (sink) |
| `/eco set <player> <amount>` | Set a player's balance |
| `/eco stats` | Supply, account count, created/destroyed totals, inflation warning |
| `/baltop` | Richest players (also available to everyone) |

There is also a **Ledger Board** block that shows the top balances in-world.

### NPC Administration

| Command | Description |
| --- | --- |
| `/npc spawn [preset]` | Spawn a Notch NPC where you stand |
| `/npc setrole <role> [args]` | Assign an economy role to the looked-at entity |
| `/npc clearrole` | Remove a role |
| `/npc info` / `/npc debug` | Inspect the looked-at NPC |

Admin economy roles (banker, auctioneer, admin shop, etc.) are op-gated; players may set the shop role
on their own NPCs.

### Admin Shops

| Command | Description |
| --- | --- |
| `/adminshop create\|delete\|list <shop>` | Manage admin shops |
| `/adminshop additem <shop> <buy> <sell> [dynamic]` | Add an item (hold it) |
| `/adminshop removeitem` / `info` | Edit an admin shop |
| `/npc setrole adminshop <shop>` | Bind an admin shop to the looked-at NPC |

### World Events

| Command | Description |
| --- | --- |
| `/balloon spawn [x y z]` | Spawn a balloon crate |
| `/balloon setArea <x y z radius>` | Set the spawn area |
| `/balloon setYRange <min> <max>` | Set altitude range |
| `/balloon setCount <n>` / `announce on\|off` | Count / announcements |
| `/cache spawn <radius>` / `spawn_at <x y z>` | Spawn a golden cache |
| `/cache announce <true\|false>` | Toggle cache announcements |

### Feature Admin

- **Raffle:** `/raffle draw`, `/raffle reset`, `/raffle setprize` (op).
- **Bounties:** `/bounty admin` opens the setup GUI; create/remove offers and place decrees.
- **Crates:** `/crate givekey <targets> <n>`.

### Configuration

Config file: `config/notchcurrency.json`. With **ModMenu** installed, use the in-game GUI
(**ModMenu → Notch Currency**) - see [Configuration](11.-Configuration) for the full list of
categories, config keys, and datapack paths.

**Balloon crates**

```json
{
  "balloon": {
    "centerX": 0, "centerY": 80, "centerZ": 0,
    "radius": 25, "minY": 110, "maxY": 150,
    "perDay": 3, "announce": true,
    "windowStart": 1000, "windowEnd": 2000
  }
}
```

**Golden cache**

```json
{
  "cache": {
    "announce": true, "cooldownMinutes": 60,
    "currencyStacksMin": 1, "currencyStacksMax": 3,
    "currencyPerStackMin": 100, "currencyPerStackMax": 250
  }
}
```

**Auction fees**

```json
{
  "auctionListingFeeFlat": 0,
  "auctionListingFeePercent": 0,
  "auctionListingFeeMax": 0,
  "auctionSaleTaxPercent": 0,
  "auctionSaleTaxMax": 0
}
```

### Audit Log & Webhook

Every balance change is written to a per-day audit file under `<world>/notchcurrency/ledger/`. An
optional **Discord webhook** mirrors admin-relevant and large transactions - enable it and paste a
webhook URL in the config.

### Recipes

**ATM / Bank**

```
I I I
I E I
I G I

I = Iron Ingot   E = Emerald   G = Gold Block
```

Other blocks and items are available in the **Notch Currency** creative tab.

### Troubleshooting

- **"My custom coin art isn't showing"** - enable the generated **NotchCurrencyCustom** resource pack
  in Options → Resource Packs, then reload/restart.
- **"A player's NPC won't let me edit it"** - only the owner or an op can edit an NPC. Ops can always
  override.
- **"An offer/auction paid nothing"** - coins and items owed to an offline player are held and
  delivered on their next login; check the mailbox / relog.
