package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class QuestManager {

    private QuestManager() {}

    public static void define(MinecraftServer server, Bounty quest) {
        BountyState state = BountyState.get(server);
        state.removeOffer(quest.getId());
        state.addOffer(quest);
    }

    public static boolean delete(MinecraftServer server, String key) {
        Bounty q = find(server, key);
        if (q == null) return false;
        BountyState.get(server).removeOffer(q.getId());
        return true;
    }

    public static Bounty find(MinecraftServer server, String key) {
        if (key == null || key.isBlank()) return null;
        for (Bounty b : BountyState.get(server).allOffers()) {
            if (b.isQuest() && b.getQuestKey().equalsIgnoreCase(key.trim())) return b;
        }
        return null;
    }

    public static List<Bounty> allQuests(MinecraftServer server) {
        List<Bounty> out = new ArrayList<>();
        for (Bounty b : BountyState.get(server).allOffers()) {
            if (b.isQuest()) out.add(b);
        }
        return out;
    }

    public static boolean hasTaken(ServerPlayer player, String key) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        Bounty q = find(server, key);
        return q != null && BountyState.get(server).getTaken(player.getUUID(), q.getId()) != null;
    }

    public static boolean hasDone(ServerPlayer player, String key) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        Bounty q = find(server, key);
        return q != null && BountyState.get(server).hasCompletedOffer(player.getUUID(), q.getId());
    }

    public static boolean canStart(ServerPlayer player, String key) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        Bounty q = find(server, key);
        if (q == null) return false;
        if (!q.getNeedsQuest().isBlank() && !hasDone(player, q.getNeedsQuest())) return false;
        BountyState state = BountyState.get(server);
        if (state.getTaken(player.getUUID(), q.getId()) != null) return false;
        return q.isRepeatable() || !state.hasCompletedOffer(player.getUUID(), q.getId());
    }

    public static boolean isReady(ServerPlayer player, String key) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        Bounty q = find(server, key);
        if (q == null) return false;
        TakenBounty tb = BountyState.get(server).getTaken(player.getUUID(), q.getId());
        return tb != null && tb.progress() >= q.getRequired();
    }

    public static void give(ServerPlayer player, String key) {
        give(player, key, "");
    }

    public static void give(ServerPlayer player, String key, String giver) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        Bounty q = find(server, key);
        if (q == null) {
            Msg.chat(player, Component.literal("That quest does not exist.").withStyle(ChatFormatting.RED));
            return;
        }
        BountyState state = BountyState.get(server);
        if (state.getTaken(player.getUUID(), q.getId()) != null) return;
        if (state.hasCompletedOffer(player.getUUID(), q.getId()) && !q.isRepeatable()) return;
        if (!q.getNeedsQuest().isBlank() && !hasDone(player, q.getNeedsQuest())) return;
        if (q.isFactionOnly() && !factionOk(player, q)) {
            if (q.getNeedsFaction().isBlank()) {
                Msg.chat(player, Component.literal("You need to be in a faction for that.")
                        .withStyle(ChatFormatting.RED));
            } else {
                var want = net.fugginbeenus.notchcurrency.npc.faction.FactionState.get(server)
                        .get(q.getNeedsFaction());
                Msg.chat(player, Component.literal("That is for ").withStyle(ChatFormatting.RED)
                        .append(want == null
                                ? Component.literal(q.getNeedsFaction()).withStyle(ChatFormatting.WHITE)
                                : Component.literal(want.displayName()).withStyle(want.color()))
                        .append(Component.literal(".").withStyle(ChatFormatting.RED)));
            }
            return;
        }

        TakenBounty fresh = new TakenBounty(q, 0L, 0);
        fresh.setGiver(giver == null ? "" : giver);
        Long blocked = JUST_SETTLED.get(java.util.UUID.nameUUIDFromBytes(
                settleKey(player, key).getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        long nowTick = player.level().getGameTime();
        if (blocked != null && nowTick - blocked < 20L) return;

        state.take(player.getUUID(), fresh);
        Msg.chat(player, Component.literal("New quest: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(q.describe()).withStyle(ChatFormatting.WHITE)));
        BountyManager.syncTracker(player);
    }

    public static final double SHARE_RANGE = 48.0;

    private static final java.util.Map<java.util.UUID, Long> JUST_SETTLED = new java.util.HashMap<>();

    private static String settleKey(ServerPlayer p, String key) {
        return p.getUUID() + "|" + (key == null ? "" : key.toLowerCase());
    }

    private static boolean factionOk(ServerPlayer player, Bounty q) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        String mine = net.fugginbeenus.notchcurrency.npc.faction.FactionState.get(server)
                .factionIdOf(player.getUUID());
        if (mine == null || mine.isBlank()) return false;
        return q.getNeedsFaction().isBlank() || mine.equalsIgnoreCase(q.getNeedsFaction());
    }

    static void shareProgress(ServerPlayer source, Bounty q, String what) {
        if (!q.isFactionOnly()) return;
        MinecraftServer server = source.level().getServer();
        if (server == null) return;
        String mine = net.fugginbeenus.notchcurrency.npc.faction.FactionState.get(server)
                .factionIdOf(source.getUUID());
        if (mine == null || mine.isBlank()) return;
        BountyState state = BountyState.get(server);
        for (ServerPlayer mate : server.getPlayerList().getPlayers()) {
            if (mate == source) continue;
            if (mate.level() != source.level()) continue;
            if (mate.distanceToSqr(source) > SHARE_RANGE * SHARE_RANGE) continue;
            String theirs = net.fugginbeenus.notchcurrency.npc.faction.FactionState.get(server)
                    .factionIdOf(mate.getUUID());
            if (theirs == null || !theirs.equalsIgnoreCase(mine)) continue;
            TakenBounty tb = state.getTaken(mate.getUUID(), q.getId());
            if (tb == null || tb.progress() >= q.getRequired()) continue;
            tb.addProgress(1);
            state.setDirty();
            BountyManager.syncTracker(mate);
            if (tb.progress() >= q.getRequired()) {
                announce(mate, q, what + " with " + source.getName().getString());
                settle(mate, q, tb);
            }
        }
    }

    public static void turnIn(ServerPlayer player, String key) {
        turnIn(player, key, null);
    }

    public static void turnIn(ServerPlayer player, String key,
                              net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        Bounty q = find(server, key);
        if (q == null) return;
        BountyState state = BountyState.get(server);
        TakenBounty tb = state.getTaken(player.getUUID(), q.getId());
        if (tb == null) {
            Msg.chat(player, Component.literal("You are not on that quest.").withStyle(ChatFormatting.RED));
            return;
        }
        if (q.getType().usesItem()) {
            Item item = BuiltInRegistries.ITEM.get(q.getTarget());
            int have = BountyManager.countItem(player, item);
            if (have < q.getRequired()) {
                Msg.chat(player, Component.literal("You need " + q.getRequired() + " ").withStyle(ChatFormatting.RED)
                        .append(q.targetName().copy().withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(" - you have " + have + ".").withStyle(ChatFormatting.RED)));
                return;
            }
            BountyManager.removeItem(player, item, q.getRequired());
        } else if (tb.progress() < q.getRequired()) {
            Msg.chat(player, Component.literal("Not finished yet: ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(q.describe()).withStyle(ChatFormatting.WHITE)));
            return;
        }

        payOut(player, q, state);
        if (npc != null) {
            npc.fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_QUEST_DONE, player);
        }
    }

    private static void payOut(ServerPlayer player, Bounty q, BountyState state) {
        BountyManager.giveReward(player, q);
        state.removeTaken(player.getUUID(), q.getId());
        state.markOfferCompleted(player.getUUID(), q.getId());
        JUST_SETTLED.put(java.util.UUID.nameUUIDFromBytes(
                settleKey(player, q.getQuestKey()).getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                player.level().getGameTime());
        Msg.chat(player, Component.literal("Quest complete: ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(q.describe()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" - reward: " + q.rewardSummary()).withStyle(ChatFormatting.GREEN)));
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                net.minecraft.sounds.SoundEvents.PLAYER_LEVELUP,
                net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.4f);
        BountyManager.syncTracker(player);
        if (!q.getNextQuest().isBlank()) give(player, q.getNextQuest(), "");
    }

    public static void onTalkedTo(ServerPlayer player,
                                 net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
        String npcName = net.fugginbeenus.notchcurrency.npc.NpcText.npcName(npc);
        boolean hasDialogue = !npc.getDialogue().isEmpty();
        MinecraftServer server = player.level().getServer();
        if (server == null || npcName == null || npcName.isBlank()) return;
        BountyState state = BountyState.get(server);
        for (TakenBounty tb : state.getTakenAll(player.getUUID())) {
            Bounty b = tb.bounty();
            if (!b.isQuest()) continue;

            if (!hasDialogue && b.needsHandIn() && npcName.equalsIgnoreCase(tb.giver())) {
                boolean ready = b.getType().usesItem()
                        ? BountyManager.countItem(player,
                                BuiltInRegistries.ITEM.get(b.getTarget())) >= b.getRequired()
                        : tb.progress() >= b.getRequired();
                if (ready) {
                    turnIn(player, b.getQuestKey());
                    continue;
                }
            }

            if (tb.progress() >= b.getRequired()) continue;
            if (!b.getTargetText().equalsIgnoreCase(npcName)) continue;

            if (b.getType() == BountyType.TALK_TO) {
                tb.addProgress(b.getRequired());
                state.setDirty();
                announce(player, b, "Talked to " + npcName);
                shareProgress(player, b, "Talked to " + npcName);
                settle(player, b, tb);
            } else if (b.getType() == BountyType.DELIVER) {
                Item item = BuiltInRegistries.ITEM.get(b.getTarget());
                int have = BountyManager.countItem(player, item);
                if (have < b.getRequired()) {
                    Msg.chat(player, Component.literal(npcName + " wants " + b.getRequired() + " ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(b.targetName().copy().withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" - you have " + have + ".").withStyle(ChatFormatting.YELLOW)));
                    continue;
                }
                BountyManager.removeItem(player, item, b.getRequired());
                tb.addProgress(b.getRequired());
                state.setDirty();
                announce(player, b, "Delivered to " + npcName);
                shareProgress(player, b, "Delivered to " + npcName);
                settle(player, b, tb);
            }
        }
    }

    private static void announce(ServerPlayer player, Bounty b, String what) {
        Msg.chat(player, Component.literal(what + ": ").withStyle(ChatFormatting.GOLD)
                .append(Component.literal(b.describe()).withStyle(ChatFormatting.WHITE)));
        BountyManager.syncTracker(player);
    }

    static void settle(ServerPlayer player, Bounty b, TakenBounty tb) {
        if (b.needsHandIn()) {
            String who = tb.giver();
            Msg.chat(player, Component.literal(who.isBlank()
                    ? "Take it back to be paid."
                    : "Take it back to " + who + " to be paid.").withStyle(ChatFormatting.GRAY));
            return;
        }
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        payOut(player, b, state);
    }

    public static void tickVisits(MinecraftServer server) {
        BountyState state = BountyState.get(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            for (TakenBounty tb : state.getTakenAll(player.getUUID())) {
                BountyType kind = tb.bounty().getType();
                if (kind == BountyType.FETCH || kind == BountyType.DELIVER) {
                    BountyManager.syncTracker(player);
                    break;
                }
            }
            for (TakenBounty tb : state.getTakenAll(player.getUUID())) {
                Bounty b = tb.bounty();
                if (!b.isQuest() || b.getType() != BountyType.VISIT) continue;
                if (tb.progress() >= b.getRequired()) continue;
                if (!nearSpot(player, b.getTargetText())) continue;
                tb.addProgress(b.getRequired());
                state.setDirty();
                announce(player, b, "Arrived");
                shareProgress(player, b, "Arrived");
                settle(player, b, tb);
            }
        }
    }

    private static boolean nearSpot(ServerPlayer player, String spec) {
        String[] parts = spec == null ? new String[0] : spec.trim().split("\\s+");
        if (parts.length < 3) return false;
        try {
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double z = Double.parseDouble(parts[2]);
            double r = parts.length >= 4 ? Double.parseDouble(parts[3]) : 6.0;
            return player.distanceToSqr(x + 0.5, y, z + 0.5) <= r * r;
        } catch (NumberFormatException badSpot) {
            return false;
        }
    }

    public static UUID keyId(String key) {
        return Bounty.idForKey(key);
    }
}
