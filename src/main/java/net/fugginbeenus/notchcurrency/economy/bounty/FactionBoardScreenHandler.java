package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.block.BoardColor;
import net.fugginbeenus.notchcurrency.block.entity.FactionBoardBlockEntity;
import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.economy.FactionBank;
import net.fugginbeenus.notchcurrency.npc.faction.Faction;
import net.fugginbeenus.notchcurrency.npc.faction.FactionBoards;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FactionBoardScreenHandler extends BountyBoardScreenHandler {

    public static final int BTN_RELINK = 5;
    private static final double REACH_SQ = 64.0;

    @Nullable
    private final BlockPos board;
    private final SimpleContainer headerInv = new SimpleContainer(1);

    public FactionBoardScreenHandler(int containerId, Inventory inv) {
        this(containerId, inv, null);
    }

    public FactionBoardScreenHandler(int containerId, Inventory inv, @Nullable BlockPos board) {
        super(ModScreenHandlers.FACTION_BOARD, containerId, inv);
        this.board = board;
        this.addSlot(new ReadOnlySlot(headerInv, 0, -10000, -10000));
        refresh();
    }

    @Nullable
    public BlockPos board() {
        return board;
    }

    public ItemStack header() {
        return headerInv.getItem(0);
    }

    @Override
    protected void refresh() {
        if (board == null || !(playerInv.player instanceof ServerPlayer sp)) return;
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        props.set(P_TAKE_LIMIT, 0);
        props.set(P_CAN_POST, 0);
        long now = BountyManager.worldTime(server);
        Faction faction = FactionBoards.factionAt(sp.level(), board);
        if (faction == null) {
            for (int i = 0; i < boardInv.getContainerSize(); i++) boardInv.setItem(i, ItemStack.EMPTY);
            headerInv.setItem(0, ItemStack.EMPTY);
            props.set(P_PAGE, 0);
            props.set(P_TOTAL_PAGES, 1);
            return;
        }
        UUID me = sp.getUUID();
        boolean member = FactionBank.isMember(sp, faction);
        headerInv.setItem(0, header(sp, server, faction, member));
        props.set(P_CAN_POST, member && BountyManager.canPost(sp) ? 1 : 0);

        BountyState state = BountyState.get(server);
        List<Bounty> avail = new ArrayList<>();
        for (Bounty b : state.allOffers()) {
            if (!faction.id().equals(b.getPostFaction()) || b.isExpired(now)) continue;
            if (state.hasTaken(me, b.getId()) || state.hasCompletedOffer(me, b.getId())) continue;
            if (state.reservedBy(b.getId()) != null) continue;
            avail.add(b);
        }
        for (Bounty q : FactionBoards.quests(server, faction)) {
            if (state.getTaken(me, q.getId()) != null) continue;
            if (!q.isRepeatable() && state.hasCompletedOffer(me, q.getId())) continue;
            if (!q.getNeedsQuest().isBlank() && !QuestManager.hasDone(sp, q.getNeedsQuest())) continue;
            avail.add(q);
        }
        int totalPages = Math.max(1, (avail.size() + OFFER_SLOTS - 1) / OFFER_SLOTS);
        page = Math.max(0, Math.min(page, totalPages - 1));
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        long balance = FactionBank.enabled() ? FactionBank.balance(server, faction) : 0L;
        for (int i = 0; i < OFFER_SLOTS; i++) {
            int idx = page * OFFER_SLOTS + i;
            boardInv.setItem(i, idx >= avail.size() ? ItemStack.EMPTY
                    : avail.get(idx).isQuest() ? note(avail.get(idx), faction, me, balance)
                    : display(avail.get(idx), false, 0, avail.get(idx).getExpiresGameTime(), me));
        }

        int t = 0;
        for (TakenBounty tb : state.getTakenAll(me)) {
            Bounty q = tb.bounty();
            if (t >= TAKEN_SLOTS) break;
            if (faction.id().equals(q.getPostFaction())) {
                boardInv.setItem(OFFER_SLOTS + t++, display(q, true, tb.progress(), tb.expiresGameTime(), me));
                continue;
            }
            if (!q.isQuest() || !q.isOnBoard()) continue;
            ItemStack row = display(q, true, tb.progress(), 0L, me);
            CompoundTag tag = StackData.editData(row);
            tag.putBoolean("quest", true);
            tag.putBoolean("handin", q.needsHandIn());
            tag.putBoolean("ready", q.getType().usesItem()
                    ? BountyManager.countItem(sp, BuiltInRegistries.ITEM.get(q.getTarget())) >= q.getRequired()
                    : tb.progress() >= q.getRequired());
            StackData.commitData(row, tag);
            boardInv.setItem(OFFER_SLOTS + t++, row);
        }
        while (t < TAKEN_SLOTS) boardInv.setItem(OFFER_SLOTS + t++, ItemStack.EMPTY);
    }

    private static ItemStack note(Bounty q, Faction faction, UUID viewer, long balance) {
        ItemStack s = display(q, false, 0, 0L, viewer);
        CompoundTag tag = StackData.editData(s);
        tag.putString("poster", faction.displayName());
        tag.putBoolean("quest", true);
        long cost = FactionBoards.cost(q);
        tag.putBoolean("short", cost > 0 && cost > balance);
        StackData.commitData(s, tag);
        return s;
    }

    private static ItemStack header(ServerPlayer sp, MinecraftServer server, Faction faction, boolean member) {
        ItemStack head = new ItemStack(Items.PAPER);
        CompoundTag tag = StackData.editData(head);
        tag.putString("fname", faction.displayName());
        tag.putInt("cloth", BoardColor.of(faction.color()).argb());
        tag.putBoolean("member", member);
        tag.putBoolean("admin", Perms.isAdmin(sp, Perms.FACTION));
        boolean bank = FactionBank.enabled() && (member || FactionBank.canWithdraw(sp, faction));
        tag.putBoolean("bank", bank);
        tag.putLong("bal", bank ? FactionBank.balance(server, faction) : 0L);
        StackData.commitData(head, tag);
        return head;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player instanceof ServerPlayer sp) || board == null) return false;
        if (id == BTN_RELINK) {
            FactionBoards.relink(sp, board);
            return true;
        }
        if (id == 2) {
            FactionBoards.openPost(sp, board);
            return true;
        }
        return (id == 0 || id == 1) && super.clickMenuButton(player, id);
    }

    @Override
    public boolean stillValid(Player player) {
        if (board == null) return true;
        return player.level().getBlockEntity(board) instanceof FactionBoardBlockEntity
                && player.distanceToSqr(Vec3.atCenterOf(board)) <= REACH_SQ;
    }
}
