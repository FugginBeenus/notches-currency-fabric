package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.block.BoardColor;
import net.fugginbeenus.notchcurrency.block.FactionBoardBlock;
import net.fugginbeenus.notchcurrency.block.entity.FactionBoardBlockEntity;
import net.fugginbeenus.notchcurrency.compat.Colors;
import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.economy.FactionBank;
import net.fugginbeenus.notchcurrency.economy.bounty.Bounty;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyState;
import net.fugginbeenus.notchcurrency.economy.bounty.TakenBounty;
import net.fugginbeenus.notchcurrency.economy.bounty.FactionBoardScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.QuestManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FactionBoards {

    public static final int TAKE = 0, COLLECT = 1, TURN_IN = 2, TAKE_DOWN = 3, GIVE_UP = 4;
    private static final double REACH_SQ = 64.0;

    private FactionBoards() {}

    @Nullable
    private static FactionBoardBlockEntity board(Level level, BlockPos lower) {
        return level.getBlockEntity(lower) instanceof FactionBoardBlockEntity be ? be : null;
    }

    @Nullable
    public static Faction factionOf(MinecraftServer server, FactionBoardBlockEntity be) {
        return be.factionId().isEmpty() ? null : FactionState.get(server).get(be.factionId());
    }

    @Nullable
    public static Faction factionAt(Level level, BlockPos lower) {
        MinecraftServer server = level.getServer();
        FactionBoardBlockEntity be = board(level, lower);
        return server == null || be == null ? null : factionOf(server, be);
    }

    public static List<Bounty> quests(MinecraftServer server, Faction faction) {
        List<Bounty> out = new ArrayList<>();
        for (Bounty q : QuestManager.allQuests(server)) {
            if (q.isOnBoard() && (q.getNeedsFaction().isBlank() || q.getNeedsFaction().equalsIgnoreCase(faction.id()))) {
                out.add(q);
            }
        }
        return out;
    }

    public static void use(ServerPlayer sp, BlockPos lower) {
        MinecraftServer server = sp.level().getServer();
        FactionBoardBlockEntity be = board(sp.level(), lower);
        if (server == null || be == null) return;
        refresh(sp.level(), lower, be);
        boolean admin = Perms.isAdmin(sp, Perms.FACTION);
        Faction faction = factionOf(server, be);
        if (faction != null) {
            sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new FactionBoardScreenHandler(id, inv, lower),
                    Component.literal("Faction Board")));
            return;
        }
        if (admin) {
            sendPicker(sp, lower, "");
            return;
        }
        Faction mine = FactionState.get(server).foundedBy(sp.getUUID());
        if (mine != null) {
            link(sp, lower, mine.id());
            return;
        }
        Msg.chat(sp, Component.literal("This Faction Board has no faction yet. A faction founder can claim it.")
                .withStyle(ChatFormatting.GRAY));
    }

    public static void link(ServerPlayer sp, BlockPos lower, String factionId) {
        Level level = sp.level();
        MinecraftServer server = level.getServer();
        FactionBoardBlockEntity be = board(level, lower);
        if (server == null || be == null || sp.distanceToSqr(Vec3.atCenterOf(lower)) > REACH_SQ) return;
        boolean admin = Perms.isAdmin(sp, Perms.FACTION);
        if (factionId.isEmpty()) {
            if (!admin) return;
            be.setFactionId("");
            paint(level, lower, BoardColor.NONE);
            Msg.chat(sp, Component.literal("This board has no faction now.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        Faction faction = FactionState.get(server).get(factionId);
        if (faction == null) {
            Msg.chat(sp, Component.literal("No faction by that name.").withStyle(ChatFormatting.RED));
            return;
        }
        if (!admin) {
            if (!faction.isFoundedBy(sp.getUUID())) {
                Msg.chat(sp, Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED));
                return;
            }
            if (!be.factionId().isEmpty()) {
                Msg.chat(sp, Component.literal("This board already belongs to a faction.").withStyle(ChatFormatting.RED));
                return;
            }
        }
        be.setFactionId(faction.id());
        paint(level, lower, BoardColor.of(faction.color()));
        Msg.chat(sp, Component.literal("This board now belongs to ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
    }

    public static void openPost(ServerPlayer sp, BlockPos lower) {
        Faction faction = factionAt(sp.level(), lower);
        if (faction == null || !FactionBank.isMember(sp, faction) || !BountyManager.canPost(sp)) return;
        boolean bank = FactionBank.enabled() && FactionBank.canWithdraw(sp, faction);
        sp.openMenu(new SimpleMenuProvider((id, inv, p) -> new net.fugginbeenus.notchcurrency.economy.bounty.BountyPostScreenHandler(
                id, inv, lower, faction.id(), bank), Component.literal("Post a Request")));
    }

    public static void relink(ServerPlayer sp, BlockPos lower) {
        FactionBoardBlockEntity be = board(sp.level(), lower);
        if (be == null || !Perms.isAdmin(sp, Perms.FACTION)) return;
        sendPicker(sp, lower, be.factionId());
    }

    public static void act(ServerPlayer sp, BlockPos lower, UUID questId, int action) {
        MinecraftServer server = sp.level().getServer();
        Faction faction = factionAt(sp.level(), lower);
        if (server == null || faction == null) return;
        Bounty request = BountyState.get(server).getOffer(questId);
        if (request != null && faction.id().equals(request.getPostFaction())) {
            if (action == TAKE_DOWN) {
                BountyManager.cancelPost(sp, questId);
                return;
            }
            if (!FactionBank.isMember(sp, faction)) return;
            if (action == TAKE) BountyManager.take(sp, questId);
            else if (action == COLLECT) BountyManager.claim(sp, questId);
            else if (action == TURN_IN) BountyManager.turnIn(sp, questId);
            return;
        }
        if (!FactionBank.isMember(sp, faction)) {
            Msg.chat(sp, Component.literal("Only members of ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                    .append(Component.literal(" can take jobs here.").withStyle(ChatFormatting.RED)));
            return;
        }
        if (action == TAKE) {
            for (Bounty q : quests(server, faction)) {
                if (q.getId().equals(questId)) take(sp, server, faction, q);
            }
        } else if (action == GIVE_UP) {
            giveUp(sp, server, questId);
        } else if (action == TURN_IN) {
            for (Bounty q : QuestManager.allQuests(server)) {
                if (q.getId().equals(questId) && q.isOnBoard()) QuestManager.turnIn(sp, q.getQuestKey());
            }
        }
    }

    public static long cost(Bounty q) {
        return FactionBank.enabled() ? q.getRewardCoins() : 0L;
    }

    private static void take(ServerPlayer sp, MinecraftServer server, Faction faction, Bounty q) {
        long cost = cost(q);
        if (cost > 0 && FactionBank.balance(server, faction) < cost) {
            Msg.chat(sp, Component.literal("The bank of ").withStyle(ChatFormatting.RED)
                    .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                    .append(Component.literal(" can't pay this job right now.").withStyle(ChatFormatting.RED)));
            return;
        }
        BountyState state = BountyState.get(server);
        if (state.getTaken(sp.getUUID(), q.getId()) != null) return;
        QuestManager.give(sp, q.getQuestKey(), "the Faction Board");
        TakenBounty tb = state.getTaken(sp.getUUID(), q.getId());
        if (tb == null) return;
        boolean held = cost > 0 && FactionBank.hold(server, faction, cost, "held for " + sp.getName().getString() + ": " + q.describe());
        tb.setBoard(faction.id(), held ? cost : 0L);
        state.setDirty();
        if (held) {
            Msg.chat(sp, Component.literal("The bank set aside ").withStyle(ChatFormatting.GRAY)
                    .append(net.fugginbeenus.notchcurrency.core.NotchCurrency.coins(cost))
                    .append(Component.literal(" for this job.").withStyle(ChatFormatting.GRAY)));
        }
    }

    private static void giveUp(ServerPlayer sp, MinecraftServer server, UUID questId) {
        BountyState state = BountyState.get(server);
        TakenBounty tb = state.getTaken(sp.getUUID(), questId);
        if (tb == null || !tb.bounty().isOnBoard()) return;
        long back = release(server, tb, "given up by " + sp.getName().getString());
        state.removeTaken(sp.getUUID(), questId);
        Component line = Component.literal("You gave up: ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(tb.bounty().describe()).withStyle(ChatFormatting.WHITE));
        if (back > 0) {
            line = line.copy().append(Component.literal(" The coins went back to the bank.").withStyle(ChatFormatting.YELLOW));
        }
        Msg.chat(sp, line);
        BountyManager.syncTracker(sp);
    }

    private static long release(MinecraftServer server, TakenBounty tb, String detail) {
        long back = tb.held();
        FactionBank.giveBack(server, tb.boardFaction(), back, detail);
        tb.setBoard(tb.boardFaction(), 0L);
        return back;
    }

    public static void dropJobs(MinecraftServer server, UUID player, String factionId) {
        if (factionId == null || factionId.isEmpty()) return;
        BountyState state = BountyState.get(server);
        int dropped = 0;
        for (TakenBounty tb : state.getTakenAll(player)) {
            if (!factionId.equals(tb.boardFaction())) continue;
            release(server, tb, "job dropped");
            state.removeTaken(player, tb.bounty().getId());
            dropped++;
        }
        ServerPlayer online = server.getPlayerList().getPlayer(player);
        if (dropped > 0 && online != null) {
            Msg.chat(online, Component.literal(dropped == 1 ? "Your Faction Board job was dropped."
                    : "Your " + dropped + " Faction Board jobs were dropped.").withStyle(ChatFormatting.YELLOW));
            BountyManager.syncTracker(online);
        }
    }

    public static void dropAllJobs(MinecraftServer server, String factionId) {
        for (UUID player : BountyState.get(server).playersWithTaken()) dropJobs(server, player, factionId);
    }

    public static void refresh(Level level, BlockPos lower, FactionBoardBlockEntity be) {
        MinecraftServer server = level.getServer();
        if (server == null) return;
        Faction faction = factionOf(server, be);
        if (faction == null && !be.factionId().isEmpty()) be.setFactionId("");
        paint(level, lower, faction == null ? BoardColor.NONE : BoardColor.of(faction.color()));
    }

    private static void paint(Level level, BlockPos lower, BoardColor color) {
        for (BlockPos part : new BlockPos[]{lower, lower.above()}) {
            BlockState state = level.getBlockState(part);
            if (state.getBlock() instanceof FactionBoardBlock && state.getValue(FactionBoardBlock.COLOR) != color) {
                level.setBlock(part, state.setValue(FactionBoardBlock.COLOR, color), Block.UPDATE_CLIENTS);
            }
        }
    }

    private static void sendPicker(ServerPlayer sp, BlockPos lower, String currentId) {
        FactionState state = FactionState.get(sp.level().getServer());
        List<Faction> all = state.all();
        FriendlyByteBuf buf = Net.buf();
        buf.writeBlockPos(lower);
        buf.writeUtf(currentId);
        buf.writeVarInt(all.size());
        for (Faction f : all) {
            buf.writeUtf(f.id());
            buf.writeUtf(f.displayName());
            buf.writeUtf(Colors.name(f.color()));
            buf.writeVarInt(state.memberCount(f.id()));
        }
        Net.sendToClient(sp, NotchPackets.FACTION_BOARD_PICKER, buf);
    }
}
