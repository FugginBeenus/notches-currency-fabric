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
loan payments, NPC charges, crate keys, the raffle house cut, the auction sale tax and purchases from
admin shops.
Players pay the same as before. Only the place the money ends up changes.

Two more settings build on it:

- **Treasury is the house**: gambling bets go into the treasury, and wins are paid out of it while it
  has enough.
- **Pay rewards from treasury**: bounty rewards, interest, loans and other payouts come out of the
  treasury while it has enough. When it runs low, they are new money again.
- **Loans need treasury money** (in the Loans settings): loans come only out of the treasury. If the
  treasury has too little, the loan is refused, and the player is told how much the bank can lend.

The treasury is not a player. Wealth tax, savings interest and `/baltop` skip it, and `/eco stats`
shows its balance on its own line.

### NPC Administration

| Command | Description |
| --- | --- |
| `/npc spawn [preset]` | Spawn a Notch NPC where you stand |
| `/npc setrole <role> [args]` | Assign an economy role to the looked-at entity |
| `/npc clearrole` | Remove a role |
| `/npc info` / `/npc debug` | Inspect the looked-at NPC |

The `/npc` commands are for admins. NPC owners pick a role on the editor's **Role** tab. The
**Roles owners can pick** setting limits which roles they can pick (see [NPC editing](#npc-editing)).

`/shop admin transfer <shop> <player>` gives a shop to a player straight away, with no offer to
accept. The shop's NPC moves with it, so the NPC has to be loaded. The old owner gets the shop's
unpaid earnings first. Players give NPCs away with the **Transfer** button instead (see
[NPCs](08.-NPCs)).

### NPC editing

The **NPC owners** group in the config controls what NPC owners can change in the NPC editor. Every
setting is on by default, so owners can change everything until you turn one off.

| Section | Setting | LuckPerms node |
| --- | --- | --- |
| NPC look | **Change the name** | `notchcurrency.npc.edit.name` |
| NPC look | **Change skin and model** (preset, player and web-link skins, models, mob disguises) | `notchcurrency.npc.edit.skin` |
| NPC look | **Change size and hitbox** | `notchcurrency.npc.edit.size` |
| NPC look | **Change effects** (tint, fade, glowing, invisible) | `notchcurrency.npc.edit.effects` |
| NPC look | **Change the name plate and sign** (name plate, name height, talk bubble, floating text) | `notchcurrency.npc.edit.plate` |
| NPC moves | **Change movement** (wander, follow, patrol, guard, schedules) | `notchcurrency.npc.edit.movement` |
| NPC moves | **Change fighting** (protect owner, fight monsters, **Fight players**, fight back, fight rivals) | `notchcurrency.npc.edit.fighting` |
| NPC role | **Roles owners can pick**: **All roles**, **Shop only** or **None** | `notchcurrency.npc.role.<role>` |
| NPC talk | **Change dialogue** (pages, quick lines, window or chat, goodbye line) | `notchcurrency.npc.edit.dialogue` |
| NPC talk | **Change sounds and voice** | `notchcurrency.npc.edit.sounds` |
| NPC pose | **Change poses and animations** | `notchcurrency.npc.edit.poses` |
| NPC pose | **Move and turn the NPC** (the Move and Rotate tool) | `notchcurrency.npc.edit.move` |
| NPC manage | **Change stats and abilities** | `notchcurrency.npc.edit.stats` |
| NPC manage | **Change equipment** | `notchcurrency.npc.edit.equipment` |
| NPC manage | **Change reactions** | `notchcurrency.npc.edit.reactions` |
| NPC manage | **Use presets and share codes** | `notchcurrency.npc.edit.presets` |

- **Shop only** lets owners pick **Shop** or **Basic**. A role node looks like
  `notchcurrency.npc.role.dealer`.
- A LuckPerms node set for a group wins over the setting. For example, turn a setting off for
  everyone, then give its node to a VIP group.
- Admins skip every lock. **Pick Up**, **Transfer** and **Delete** always work for the owner.
- Loading a preset or a share code changes the whole NPC, so it needs every setting on.
- In the editor, a locked part shows a lock. A tab shows a lock only when every part on it is locked.
  The server also refuses the change.
- A shop owner runs the shop with a plain right-click, so a shop keeps working with every part locked.

**For player shops:** turn off everything except the two you want players to keep, for example
**Change the name** and **Change skin and model**, and set **Roles owners can pick** to **Shop
only**. Give players NPCs from an admin shop (see [Shops](04.-Shops#ready-made-shop-npcs)), or turn
on **NPC crafting**.

**NPC crafting** has 3 choices: **Off** (the default), **Single player only** (this also covers LAN
worlds) and **Everywhere**. A change takes effect after `/reload` or a restart.

**Max NPCs per player** (in the **NPCs** section) limits how many NPCs one player can own at a time.
0, the default, means no limit.

- Only NPCs that stand in the world count. A packed NPC in an inventory does not count until it is
  placed.
- A player at the limit cannot place another NPC, and a Transfer to that player is refused.
- Admins have no limit, and `notchcurrency.npc.nolimit` gives a group no limit.
- Players who are already over the limit keep their NPCs. They cannot place more until they are
  under it.

**Who can do what**

| | Admins | NPC owners | Other players |
| --- | --- | --- | --- |
| Get an NPC item | Creative tab or `/npc spawn` | Buy one, or craft it when NPC crafting is on | The same as owners |
| Edit an NPC | Any NPC | Their own NPCs, except locked parts | No |
| Pick a role | Any role | The roles that **Roles owners can pick** allows | No |
| Pay or charge coins, give items or effects, teleport, run commands | Yes | No | No |
| Quests, Animations and Particles libraries | Yes | No | No |
| Choose who may use an NPC | Yes | No | No |
| Pick Up, Transfer, Delete | Any NPC | Their own NPCs | No |
| Shops | Run any shop, make admin shops | Run their own shop | Buy from shops |

Admins are ops, or players with `notchcurrency.admin.npc`. In single player, you are an admin only
when cheats are on.

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

**NPC editor locks.** `notchcurrency.npc.edit.<part>` and `notchcurrency.npc.role.<role>` lock parts of
the NPC editor for NPC owners. See [NPC editing](#npc-editing).

**NPCs that need a permission.** On the NPC editor's **Role** tab, admins can type a permission into
the box at the bottom and press **Set**. Only players who have it can use that NPC. The NPC's owner
and admins always can. Leave the box empty so anyone can.

### Factions

Turn on **Permanent membership** in the config to make faction membership stick. Players can't
leave, switch or found a new faction once they join one. Disbanding a faction still frees its
members. Ops are locked in too, but these commands can move anyone, themselves included:

| Command | Description |
| --- | --- |
| `/faction admin move <player> <faction>` | Move a player into a faction, or into a House like `red_kingdom.miners` |
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

**Houses.** A House is a small group inside a faction, like Miners in Red Kingdom. Turn on **Houses**
in the config to allow them. It is off by default. **Max Houses per faction** sets how many one
faction can have (5).

- Only the founder and admins make a House, end it, and pick its leader. Make one on the Recruiter:
  press **Houses**, then **New House**. Or use `/faction house create <name>`.
- A House has a name, a join fee, open or closed, an optional leader, and its own bank. It has no
  color of its own. It uses the faction's color.
- Two factions can both have a House called Miners. In commands a House is written
  `faction.house`, like `red_kingdom.miners`. Press Tab to fill it in.
- Players join a House on the Recruiter's **Houses** page. Joining a House also joins its faction. A
  player who is new to the faction pays the faction fee and the House fee. Each fee goes to its own
  bank.
- A player is in one House at most. Leaving a House keeps them in the faction. Leaving the faction
  takes them out of their House too.
- **Permanent membership** still locks players to their faction, but they can move between its
  Houses.
- The leader presses **Edit** on the Houses page to change the House fee and open or closed. The
  leader can also take money out of the House bank. The founder and admins can do all of this for
  every House.
- An NPC can recruit for one House. Pick the House in the NPC's faction list. Its Recruiter screen
  then shows that House, with **Join**, **Leave House**, **Bank** and **Settings**.
- Turning **Houses** off only stops new Houses. Houses that exist keep working.

The whole faction still counts as one team. NPC friends and rivals, quests and dialogue checks that
name the faction, shared quest progress, the faction bank and the faction's boards treat every House
member as a faction member. A quest's **Who** or a dialogue check can name a House instead. Then only
that House counts.

**House boards.** A Faction Board set to a House is a House board. It keeps the faction's cloth, gets
a light banner, and its title says "of" and the faction's name.

- The House leader right-clicks a board with no faction to claim it for their House. The founder
  right-clicks one and picks the faction or one of its Houses.
- A House board shows the House's jobs and the faction's jobs. Only members of that House take jobs
  there. Requests posted on the faction's board show on its House boards too.
- The faction bank pays faction jobs. The House bank pays House jobs. Jobs with no **Who** are paid by
  the bank of the board they were taken from. On a House board, **Bank pays** uses the House bank.
- When a player moves to another House, the jobs their old House pays for end and the coins go back.
  Faction jobs stay.

**Ending a House.** Its members stay in the faction. Its jobs and requests end, with refunds. Its bank
money moves to the faction bank. Its boards go blank, and its NPCs go back to the faction.
Disbanding a faction ends all its Houses first. Then the faction bank pays the founder, as before.

| Command | Who | Description |
| --- | --- | --- |
| `/faction house list` | anyone | Show your faction's Houses, or every faction's if you have none |
| `/faction house join <house>` | anyone | Join a House |
| `/faction house leave` | House members | Leave your House and stay in the faction |
| `/faction house bank` | House members | Show your House's balance |
| `/faction house bank deposit <amount>` | House members | Put coins in your House's bank |
| `/faction house bank withdraw <amount>` | leader, founder | Take coins out of your House's bank |
| `/faction house create <name>` | founder | Make a House in your faction |
| `/faction house leader <house> <player>` | founder, admins | Pick the leader. Use `none` for no leader |
| `/faction house disband <house>` | founder, admins | End a House |
| `/faction admin house create <faction> <name>` | admins | Make a House in any faction |

`/faction list` shows each faction's Houses under it. `/faction admin bank` works on a House too.

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

**Notch NPC** (only when **NPC crafting** is on)

```
  E
E A E
  E

E = Emerald   A = Armor Stand
```

Other blocks and items are available in the **Notch Currency** creative tab.

### Troubleshooting

- **"My custom coin art isn't showing"** - enable the generated **NotchCurrencyCustom** resource pack
  in Options → Resource Packs, then reload/restart.
- **"A player's NPC won't let me edit it"** - only the owner or an op can edit an NPC. Ops can always
  override.
- **"An offer/auction paid nothing"** - coins and items owed to an offline player are held and
  delivered on their next login; check the mailbox / relog.
