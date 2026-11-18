## Admin

Most admin commands require **permission level 2** (op). With LuckPerms you can hand out admin
rights without making anyone an op. See [Permissions](#permissions) below.

### Economy Administration

| Command | Description |
| --- | --- |
| `/eco give <player> <amount>` | Give a player coins (faucet) |
| `/eco take <player> <amount>` | Remove coins from a player (sink) |
| `/eco set <player> <amount>` | Set a player's balance |
| `/eco stats` | Supply, account count, created/destroyed totals, inflation warning |
| `/eco treasury` | Show how much the treasury holds |
| `/eco treasury pay <player> <amount>` | Pay a player out of the treasury |
| `/eco treasury set <amount>` | Set the treasury's balance |
| `/baltop` | Richest players (also available to everyone) |

There is also a **Ledger Board** block that shows the top balances in-world.

### Treasury

The treasury is a server bank. It is off by default. Turn on **Treasury enabled** in the config to use it.

While it is on, money that would otherwise vanish goes into the treasury instead: taxes, fees, rent,
loan interest, crate keys, the raffle house cut, the auction sale tax and purchases from admin shops.
Players pay the same as before. Only the place the money ends up changes.

Two more settings build on it:

- **Treasury is the house**: gambling bets go into the treasury, and wins are paid out of it while it
  has enough.
- **Pay rewards from treasury**: bounty rewards, interest, loans and other payouts come out of the
  treasury while it has enough. When it runs low, they are new money again.

The treasury is not a player. Wealth tax, savings interest and `/baltop` skip it, and `/eco stats`
shows its balance on its own line.

### NPC Administration

| Command | Description |
| --- | --- |
| `/npc spawn [preset]` | Spawn a Notch NPC where you stand |
| `/npc setrole <role> [args]` | Assign an economy role to the looked-at entity |
| `/npc clearrole` | Remove a role |
| `/npc info` / `/npc debug` | Inspect the looked-at NPC |

Admin economy roles (banker, auctioneer, etc.) are op-gated; players may set the shop role
on their own NPCs.

`/shop admin transfer <shop> <player>` gives a shop to a player straight away, with no offer to
accept. The shop's NPC moves with it, so the NPC has to be loaded. The old owner gets the shop's
unpaid earnings first. Players give NPCs away with the **Transfer** button instead (see
[NPCs](08.-NPCs)).

### Permissions

Notch Currency works with **LuckPerms**, or any other mod that uses the Fabric Permissions API. You
do not need to set anything up. Without a permissions mod, nothing changes: ops can do the admin
things, and everyone can use the player commands.

**Admin areas.** Each node covers that area's commands and its admin powers in game. Ops have all of
them unless you take one away.

| Node | Covers |
| --- | --- |
| `notchcurrency.admin.eco` | `/eco`, `/givnotches` and the treasury |
| `notchcurrency.admin.crate` | Balloon and cache commands, `/crate givekey`, balloon settings |
| `notchcurrency.admin.raffle` | The raffle admin commands and screen |
| `notchcurrency.admin.bounty` | `/bounty admin` and the bounty admin screen |
| `notchcurrency.admin.quest` | Designing quests, `/quests list` |
| `notchcurrency.admin.shop` | `/shop admin`, `/shop linknpc` and admin shops |
| `notchcurrency.admin.faction` | `/faction admin`, managing any faction |
| `notchcurrency.admin.npc` | `/npc`, `/npcfx`, editing anyone's NPC, uploading models, sounds and textures, the particle and animation designers |
| `notchcurrency.admin.npc.commands` | NPC dialogue and reactions that run commands. Anyone with this can run any command through an NPC, so give it only to people you would make op. |
| `notchcurrency.admin.hearts` | `/hearts` admin commands |
| `notchcurrency.admin.cosmetics` | The cosmetics shop admin mode |

Give `notchcurrency.admin.*` to hand out all of them at once.

**Player commands.** Everyone has these unless you take one away, for example to stop a group
using `/pay`: `notchcurrency.command.` followed by `ah`, `balance` (also `/bal`), `pay`, `baltop`,
`receipts`, `bounty`, `crate`, `faction`, `slots`, `coinflip`, `hearts`, `loan`, `npcmodels`,
`npctransfer`, `quests`, `raffle`, `shop` or `trade`.
`notchcurrency.bounty.post` lets a player post bounties at a Bounty Board, when player bounties
are on. Everyone has it too.

**NPCs that need a permission.** On the NPC editor's **Role** tab, admins can type a permission into
the box at the bottom and press **Set**. Only players who have it can use that NPC. The NPC's owner
and admins always can. Leave the box empty so anyone can.

### Factions

Turn on **Permanent membership** in the config to make faction membership stick. Players can't
leave, switch or found a new faction once they join one. Disbanding a faction still frees its
members. Ops are locked in too, but these commands can move anyone, themselves included:

| Command | Description |
| --- | --- |
| `/faction admin move <player> <faction>` | Move a player into a faction |
| `/faction admin remove <player>` | Take a player out of their faction |

**Faction banks.** Turn on **Faction banks** in the config to give every faction its own bank.

- Join fees go into the bank, instead of disappearing or going to the Treasury.
- Members see the balance and deposit at the faction's recruiter NPC: open it and press **Bank**.
- The founder and admins can withdraw there too.
- Bank money stays out of `/baltop`, wealth tax and savings interest.
- When a faction is disbanded, its bank pays out to the founder.
- If the setting is turned off later, the money stays saved and comes back when it is turned on again.

| Command | Who | Description |
| --- | --- | --- |
| `/faction bank` | members | Show your faction's balance |
| `/faction bank deposit <amount>` | members | Put coins in |
| `/faction bank withdraw <amount>` | founder, admins | Take coins out to your wallet |
| `/faction bank pay <player> <amount>` | founder, admins | Pay a player from the bank |
| `/faction admin bank <faction>` | admins | Show any faction's balance |
| `/faction admin bank <faction> add\|take <amount>` | admins | Add to or take from a bank (no wallet involved) |

**Faction Board.** A notice board for one faction's jobs. Craft one by putting any banner on top of a
Bounty Board. Its cloth takes the faction's color.

- A founder right-clicks a board with no faction to claim it for their faction.
- An admin right-clicks a board with no faction and picks one from a list. On a claimed board, admins
  press **Faction** at the bottom of its screen to change it.
- Anyone can look at the board. Only members take jobs or post.
- When the faction changes its color, its boards follow. When it is disbanded, they go back to white.

The board shows two kinds of jobs:

- **Faction quests** are quests set to **Show on: Faction Board** in the Quest Designer (see
  [Quests](13.-Quests)). They show on every Faction Board, or only on one faction's boards if the
  quest's **Who** names that faction.
- **Requests** are jobs that members post with **Post**, like player bounties. They need **Player
  bounties** on, and they show only on that faction's boards, never on a Bounty Board. The founder
  and admins can switch **I pay** to **Bank pays** to pay the reward from the bank.

With faction banks off, faction quests pay like any other quest. With faction banks on, the bank pays
their coins:

- Taking the job sets the coins aside. If the bank is short, the note says **BANK SHORT** and nobody
  can take it.
- Finishing the job pays the set-aside coins. Item rewards work as usual.
- **Give up** on the board drops the job and puts the coins back. Leaving the faction does the same.
- Disbanding the faction cancels its jobs and requests. The refunds go back first, then the bank pays
  the founder.

### Admin Shops

Admin shops have no commands of their own. Make one in game: give an NPC the **Shop** role, open its
**Shop Manager** and press **Admin**. You need the `notchcurrency.admin.shop` permission, which ops
have. See [Shops](04.-Shops#admin-shops) for what an admin shop does.

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

**Treasury and factions**

```json
{
  "treasury": {
    "enabled": false,
    "collectGambling": false,
    "fundRewards": false
  },
  "factions": {
    "permanentMembership": false,
    "factionBanks": false
  }
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
