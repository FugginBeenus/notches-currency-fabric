package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class AdminShopEntry {

    public static double ELASTICITY = 0.004;
    public static double MIN_MULT = 0.25;
    public static double MAX_MULT = 4.0;
    public static double DECAY = 0.02;
    private final UUID id;
    private ItemStack item;
    private long baseBuyPrice;
    private long baseSellPrice;
    private boolean dynamic;
    private double stockIndex;
    private net.fugginbeenus.notchcurrency.shop.Restock.Mode resetMode =
            net.fugginbeenus.notchcurrency.shop.Restock.Mode.OFF;
    private int buyLimit = 0;
    private int sellLimit = 0;
    private long lastPeriod = 0L;
    private final java.util.Map<UUID, Integer> boughtBy = new java.util.HashMap<>();
    private final java.util.Map<UUID, Integer> soldBy = new java.util.HashMap<>();

    public AdminShopEntry(ItemStack item, long baseBuyPrice, long baseSellPrice, boolean dynamic) {
        this.id = UUID.randomUUID();
        this.item = item.copy();
        this.baseBuyPrice = Math.max(0, baseBuyPrice);
        this.baseSellPrice = Math.max(0, baseSellPrice);
        this.dynamic = dynamic;
        this.stockIndex = 0.0;
    }

    private AdminShopEntry(UUID id) {
        this.id = id;
        this.item = ItemStack.EMPTY;
    }

    public UUID getId() { return id; }
    public ItemStack getItem() { return item; }
    public int getUnit() { return Math.max(1, item.getCount()); }
    public long getBaseBuyPrice() { return baseBuyPrice; }
    public long getBaseSellPrice() { return baseSellPrice; }
    public boolean isDynamic() { return dynamic; }
    public boolean isBuyable() { return baseBuyPrice > 0; }
    public boolean isSellable() { return baseSellPrice > 0; }

    public void setBaseBuyPrice(long p) { this.baseBuyPrice = Math.max(0, p); }
    public void setBaseSellPrice(long p) { this.baseSellPrice = Math.max(0, p); }
    public void setDynamic(boolean d) { this.dynamic = d; }

    public net.fugginbeenus.notchcurrency.shop.Restock.Mode getResetMode() { return resetMode; }
    public void setResetMode(net.fugginbeenus.notchcurrency.shop.Restock.Mode mode) {
        this.resetMode = mode == null ? net.fugginbeenus.notchcurrency.shop.Restock.Mode.OFF : mode;
    }
    public int getBuyLimit() { return buyLimit; }
    public void setBuyLimit(int limit) { this.buyLimit = Math.max(0, limit); }
    public int getSellLimit() { return sellLimit; }
    public void setSellLimit(int limit) { this.sellLimit = Math.max(0, limit); }

    public synchronized void maybeReset(net.minecraft.server.level.ServerLevel level) {
        if (resetMode == net.fugginbeenus.notchcurrency.shop.Restock.Mode.OFF) return;
        long now = net.fugginbeenus.notchcurrency.shop.Restock.periodOf(resetMode, level);
        if (now == lastPeriod) return;
        lastPeriod = now;
        boughtBy.clear();
        soldBy.clear();
    }

    public synchronized int remainingBuy(UUID player) {
        if (buyLimit <= 0) return Integer.MAX_VALUE;
        return Math.max(0, buyLimit - boughtBy.getOrDefault(player, 0));
    }

    public synchronized int remainingSell(UUID player) {
        if (sellLimit <= 0) return Integer.MAX_VALUE;
        return Math.max(0, sellLimit - soldBy.getOrDefault(player, 0));
    }

    public synchronized void recordPlayerBuy(UUID player, int units) {
        if (buyLimit > 0 && units > 0) boughtBy.merge(player, units, Integer::sum);
    }

    public synchronized void recordPlayerSell(UUID player, int units) {
        if (sellLimit > 0 && units > 0) soldBy.merge(player, units, Integer::sum);
    }

    private static void writeCounts(CompoundTag nbt, String key, java.util.Map<UUID, Integer> map) {
        if (map.isEmpty()) return;
        net.minecraft.nbt.ListTag list = new net.minecraft.nbt.ListTag();
        for (java.util.Map.Entry<UUID, Integer> e : map.entrySet()) {
            CompoundTag row = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(row, "P", e.getKey());
            row.putInt("N", e.getValue());
            list.add(row);
        }
        nbt.put(key, list);
    }

    private static void readCounts(CompoundTag nbt, String key, java.util.Map<UUID, Integer> map) {
        if (!nbt.contains(key, net.minecraft.nbt.Tag.TAG_LIST)) return;
        net.minecraft.nbt.ListTag list = nbt.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(row, "P")) continue;
            map.put(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(row, "P"), row.getInt("N"));
        }
    }

    public double multiplier() {
        if (!dynamic) return 1.0;
        return net.fugginbeenus.notchcurrency.shop.DynamicPrice.multiplier(stockIndex);
    }

    public long currentBuyPrice() {
        return Math.max(1, Math.round(baseBuyPrice * multiplier()));
    }

    public long currentSellPrice() {
        return Math.max(0, Math.round(baseSellPrice * multiplier()));
    }

    public void recordBuy(int units) {
        if (dynamic) stockIndex -= units;
    }

    public void recordSell(int units) {
        if (dynamic) stockIndex += units;
    }

    public void decay() {
        if (!dynamic) return;
        stockIndex = net.fugginbeenus.notchcurrency.shop.DynamicPrice.decayed(stockIndex);
    }

    public double stockIndex() { return stockIndex; }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "Id", id);
        nbt.put("Item", StackData.writeStack(item));
        nbt.putLong("Buy", baseBuyPrice);
        nbt.putLong("Sell", baseSellPrice);
        nbt.putBoolean("Dynamic", dynamic);
        nbt.putDouble("StockIndex", stockIndex);
        if (resetMode != net.fugginbeenus.notchcurrency.shop.Restock.Mode.OFF) {
            nbt.putString("ResetMode", resetMode.name());
            nbt.putLong("ResetPeriod", lastPeriod);
        }
        if (buyLimit > 0) nbt.putInt("BuyLimit", buyLimit);
        if (sellLimit > 0) nbt.putInt("SellLimit", sellLimit);
        writeCounts(nbt, "BoughtBy", boughtBy);
        writeCounts(nbt, "SoldBy", soldBy);
        return nbt;
    }

    public static AdminShopEntry fromNbt(CompoundTag nbt) {
        AdminShopEntry e = new AdminShopEntry(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "Id"));
        e.item = StackData.readStack(nbt.getCompound("Item"));
        e.baseBuyPrice = nbt.getLong("Buy");
        e.baseSellPrice = nbt.getLong("Sell");
        e.dynamic = nbt.getBoolean("Dynamic");
        e.stockIndex = nbt.getDouble("StockIndex");
        e.resetMode = net.fugginbeenus.notchcurrency.shop.Restock.Mode.byName(nbt.getString("ResetMode"));
        e.lastPeriod = nbt.getLong("ResetPeriod");
        e.buyLimit = nbt.getInt("BuyLimit");
        e.sellLimit = nbt.getInt("SellLimit");
        readCounts(nbt, "BoughtBy", e.boughtBy);
        readCounts(nbt, "SoldBy", e.soldBy);
        return e;
    }
}
