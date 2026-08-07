package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TradeOfferState extends SavedData {

    private static final String DATA_KEY = "notchcurrency_trade_offers";

    private final Map<UUID, TradeOffer> offers = new LinkedHashMap<>();
    private final Map<UUID, List<ItemStack>> mailbox = new java.util.HashMap<>();

    public static TradeOfferState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, TradeOfferState::new, TradeOfferState::fromNbt, DATA_KEY);
    }

    // ---- offers ----

    public void add(TradeOffer offer) {
        offers.put(offer.id(), offer);
        setDirty();
    }

    @Nullable
    public TradeOffer get(UUID id) {
        return offers.get(id);
    }

    public void remove(UUID id) {
        if (offers.remove(id) != null) setDirty();
    }

    public List<TradeOffer> incomingFor(UUID playerUuid, String playerName) {
        List<TradeOffer> out = new ArrayList<>();
        for (TradeOffer o : offers.values()) {
            if (!o.creatorUuid().equals(playerUuid) && o.acceptableBy(playerName)) out.add(o);
        }
        return out;
    }

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
        setDirty();
    }

    public boolean hasMail(UUID player) {
        List<ItemStack> m = mailbox.get(player);
        return m != null && !m.isEmpty();
    }

    public List<ItemStack> claimMail(UUID player) {
        List<ItemStack> m = mailbox.remove(player);
        if (m != null) setDirty();
        return m == null ? List.of() : m;
    }

    public void returnMail(UUID player, List<ItemStack> leftover) {
        if (leftover.isEmpty()) return;
        mailbox.computeIfAbsent(player, k -> new ArrayList<>()).addAll(leftover);
        setDirty();
    }

    // ---- NBT ----

    private static TradeOfferState fromNbt(CompoundTag nbt) {
        TradeOfferState state = new TradeOfferState();
        ListTag offerList = nbt.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < offerList.size(); i++) {
            TradeOffer o = TradeOffer.fromNbt(offerList.getCompound(i));
            state.offers.put(o.id(), o);
        }
        ListTag mail = nbt.getList("Mailbox", Tag.TAG_COMPOUND);
        for (int i = 0; i < mail.size(); i++) {
            CompoundTag entry = mail.getCompound(i);
            UUID id = entry.getUUID("Player");
            List<ItemStack> items = new ArrayList<>();
            ListTag stacks = entry.getList("Items", Tag.TAG_COMPOUND);
            for (int j = 0; j < stacks.size(); j++) {
                items.add(StackData.readStack(stacks.getCompound(j)));
            }
            state.mailbox.put(id, items);
        }
        return state;
    }

    @Override
    //? if >=1.21 {
    /*public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?} else {
    public CompoundTag save(CompoundTag nbt) {
    //?}
        ListTag offerList = new ListTag();
        for (TradeOffer o : offers.values()) offerList.add(o.toNbt());
        nbt.put("Offers", offerList);

        ListTag mail = new ListTag();
        for (Map.Entry<UUID, List<ItemStack>> e : mailbox.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Player", e.getKey());
            ListTag stacks = new ListTag();
            for (ItemStack st : e.getValue()) stacks.add(StackData.writeStack(st));
            entry.put("Items", stacks);
            mail.add(entry);
        }
        nbt.put("Mailbox", mail);
        return nbt;
    }
}
