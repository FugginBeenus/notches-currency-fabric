package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-persistent store of standing trade offers plus a per-player item mailbox. The mailbox holds
 * items owed to a player who was offline when a trade resolved (payment to the creator, or a
 * returned offer on cancel); they are handed over on the player's next login.
 */
public class TradeOfferState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_trade_offers";

    private final Map<UUID, TradeOffer> offers = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> mailbox = new java.util.HashMap<>();

    public static TradeOfferState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return StateData.getOrCreate(mgr, TradeOfferState::new, TradeOfferState::fromNbt, DATA_KEY);
    }

    // ---- offers ----

    public void add(TradeOffer offer) {
        offers.put(offer.id(), offer);
        markDirty();
    }

    @Nullable
    public TradeOffer get(UUID id) {
        return offers.get(id);
    }

    public void remove(UUID id) {
        if (offers.remove(id) != null) markDirty();
    }

    /** Offers the given player may accept (target match or open), excluding their own. */
    public List<TradeOffer> incomingFor(UUID playerUuid, String playerName) {
        List<TradeOffer> out = new ArrayList<>();
        for (TradeOffer o : offers.values()) {
            if (!o.creatorUuid().equals(playerUuid) && o.acceptableBy(playerName)) out.add(o);
        }
        return out;
    }

    /** Offers the given player created. */
    public List<TradeOffer> outgoingBy(UUID playerUuid) {
        List<TradeOffer> out = new ArrayList<>();
        for (TradeOffer o : offers.values()) {
            if (o.creatorUuid().equals(playerUuid)) out.add(o);
        }
        return out;
    }

    public int countBy(UUID playerUuid) {
        int n = 0;
        for (TradeOffer o : offers.values()) if (o.creatorUuid().equals(playerUuid)) n++;
        return n;
    }

    // ---- mailbox ----

    public void addMail(UUID player, ItemStack stack) {
        if (stack.isEmpty()) return;
        mailbox.computeIfAbsent(player, k -> new ArrayList<>()).add(stack.copy());
        markDirty();
    }

    public boolean hasMail(UUID player) {
        List<ItemStack> m = mailbox.get(player);
        return m != null && !m.isEmpty();
    }

    /** Remove and return everything in a player's mailbox. */
    public List<ItemStack> claimMail(UUID player) {
        List<ItemStack> m = mailbox.remove(player);
        if (m != null) markDirty();
        return m == null ? List.of() : m;
    }

    /** Put items back in the mailbox (e.g. the player's inventory filled up mid-delivery). */
    public void returnMail(UUID player, List<ItemStack> leftover) {
        if (leftover.isEmpty()) return;
        mailbox.computeIfAbsent(player, k -> new ArrayList<>()).addAll(leftover);
        markDirty();
    }

    // ---- NBT ----

    private static TradeOfferState fromNbt(NbtCompound nbt) {
        TradeOfferState state = new TradeOfferState();
        NbtList offerList = nbt.getList("Offers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < offerList.size(); i++) {
            TradeOffer o = TradeOffer.fromNbt(offerList.getCompound(i));
            state.offers.put(o.id(), o);
        }
        NbtList mail = nbt.getList("Mailbox", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < mail.size(); i++) {
            NbtCompound entry = mail.getCompound(i);
            UUID id = entry.getUuid("Player");
            List<ItemStack> items = new ArrayList<>();
            NbtList stacks = entry.getList("Items", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < stacks.size(); j++) {
                items.add(StackData.readStack(stacks.getCompound(j)));
            }
            state.mailbox.put(id, items);
        }
        return state;
    }

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtList offerList = new NbtList();
        for (TradeOffer o : offers.values()) offerList.add(o.toNbt());
        nbt.put("Offers", offerList);

        NbtList mail = new NbtList();
        for (Map.Entry<UUID, List<ItemStack>> e : mailbox.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", e.getKey());
            NbtList stacks = new NbtList();
            for (ItemStack st : e.getValue()) stacks.add(StackData.writeStack(st));
            entry.put("Items", stacks);
            mail.add(entry);
        }
        nbt.put("Mailbox", mail);
        return nbt;
    }
}
