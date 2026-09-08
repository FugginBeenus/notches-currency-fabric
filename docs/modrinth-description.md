<!-- Paste this into the Modrinth description box. Replace every IMAGE line with a real upload. -->

# Notch Currency

**Money that means something, and villagers worth talking to.**

Give your server a real economy: coins players earn and spend, shops they run themselves, and NPCs
you build by hand who hand out work, tell a story, and remember who did what.

No commands to memorise. Everything is a screen in game.

<!-- IMAGE: a busy town square. Two or three NPCs with floating name signs, a shop NPC mid-sale,
     the coin HUD visible in the corner. This is the hero shot, make it look lived in. -->

---

## Coins, and somewhere for them to go

A real balance with a bank, an ATM block, receipts, and a coin counter on your HUD.

You can even **rename the currency and draw your own coin**. Call them Crowns, Caps, Shells,
whatever. The art and the name follow it everywhere, and every player on the server gets it
automatically.

<!-- IMAGE: the currency creator screen with a custom coin, plus the HUD showing that coin. -->

An economy only works if money leaves as fast as it arrives, so there is plenty to spend it on:
an **auction house**, a **casino**, **crates**, **loans**, **raffles**, an **enchanter**, and
**cosmetics**. Server owners can add rent, tax and fees on top.

---

## Shops players actually run

Anyone can open a shop. Set a price in coins, in items, or both at once.

<!-- IMAGE: a player shop's browse screen with a few listings and prices. -->

Servers get the same shop with endless stock, so there is one system to learn, not two. Shops can
restock on a timer, cap how much one person buys, drift their prices with demand, and buy things
*from* players instead of only selling.

---

## NPCs you build yourself

One item spawns a blank NPC. Everything after that is a screen.

Choose how they look, from a vanilla skin to **your own Blockbench model**, or disguise them as any
mob in the game. Tint them, fade them to a ghost, resize them. Give them a name in colour and a
floating sign above their head.

<!-- IMAGE: the NPC editor open on the Look tab, live preview visible. -->

Then decide what they do. Stand still, wander, follow you, patrol a route, or guard the place.
Give them a **daily schedule** so the shopkeeper opens up in the morning and walks home at dusk.

<!-- IMAGE: three or four visibly different NPCs side by side, showing the range. -->

---

## Give them something to say

**Quick Lines** for a bit of flavour. Type a few lines and they pick one at random.

**Dialogue Studio** for a real conversation: pages, buttons, and choices that only appear when they
should. A page can check what the player has done, so an NPC greets a stranger differently from
someone who has been running errands for them all week.

<!-- IMAGE: the Dialogue Studio with a few pages listed and choices on screen. -->

**Reactions** for everything else. An NPC can respond when it is talked to, when someone walks up,
when it is hit, when it kills something, even when another NPC wanders past. Every response can
check a condition first, so one NPC handles the whole arc without a single tree.

---

## Quests

Write a job once, and any NPC can hand it out.

- **Kill** something
- **Collect** something
- **Talk to** someone
- **Go to** a place
- **Deliver** items to someone

Pay in coins, items, or both. Chain them together so finishing one starts the next. Gate one behind
another. Make it repeatable or once in a lifetime.

<!-- IMAGE: the quest editor, ideally on a Deliver quest so several fields are filled in. -->

**Faction quests** are for guilds: only members can take them, and anyone from your faction nearby
shares the progress, so clearing a cave together counts for everyone.

Players get a tracker on screen and a `/quests` log. Servers can restyle the tracker to match.

<!-- IMAGE: the on-screen quest tracker with a quest part way through. -->

---

## Animate them, in game

Build a loop of poses on a timeline. No modelling software, no files, nothing to install.

<!-- IMAGE: the animation editor. Show the dope sheet at the bottom and the mannequin mid-pose. -->

Drop keyframes anywhere in time, drag a limb on the mannequin to pose it, and let the game slide
between them. Every keyframe can ease in and out, which is the difference between a wave that looks
like a person and one that looks like a robot arm.

A ghost of the previous pose sits behind the model so you can see what changed, and **Mirror** flips
left and right, which halves the work on a walk cycle.

Then hand it to any NPC: loop it forever as an idle, or play it once when something happens.

<!-- IMAGE (optional but worth it): a short GIF of an NPC playing an animation in the world. -->

---

## Good to know

- **Fabric**, for **1.20.1, 1.21.1, 1.21.11, 26.1.2 and 26.2**
- Needs **Fabric API** and **GeckoLib**
- **ModMenu** optional, for the settings screen
- **Waystones** optional, to charge for teleports
- Nearly everything is configurable, with a searchable settings screen in game
- Single player works fine, but this is built for servers

---

## Links

- [Wiki](https://github.com/FugginBeenus/notches-currency-fabric/wiki) - every screen explained
- [Discord](https://discord.gg/fMpb6retYA) - questions, bugs, ideas
- [Source](https://github.com/FugginBeenus/notches-currency-fabric)
