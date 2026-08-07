package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class AdminShopEntry {

    // ---- Global dynamic-pricing tuning (overridable via config later) ----
    public static double ELASTICITY = 0.004;  // price move per net unit of stock index
    public static double MIN_MULT = 0.25;      // price never below 25% of base
    public static double MAX_MULT = 4.0;       // price never above 400% of base
    public static double DECAY = 0.02;         // fraction of the index that recovers per decay tick

    private final UUID id;
    private ItemStack item;       // template; item.getCount() = units per transaction
    private long baseBuyPrice;    // coins to buy one unit FROM the shop (0 = not buyable)
    private long baseSellPrice;   // coins paid for selling one unit TO the shop (0 = not sellable)
    private boolean dynamic;      // whether prices float with the stock index
    private double stockIndex;    // net supply pressure (sell raises, buy lowers)

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

    public double multiplier() {
        if (!dynamic) return 1.0;
        double m = 1.0 - ELASTICITY * stockIndex;
        return Math.max(MIN_MULT, Math.min(MAX_MULT, m));
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
        if (!dynamic || stockIndex == 0.0) return;
        stockIndex *= (1.0 - DECAY);
        if (Math.abs(stockIndex) < 0.01) stockIndex = 0.0;
    }

    // ---- NBT ----

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "Id", id);
        nbt.put("Item", StackData.writeStack(item));
        nbt.putLong("Buy", baseBuyPrice);
        nbt.putLong("Sell", baseSellPrice);
        nbt.putBoolean("Dynamic", dynamic);
        nbt.putDouble("StockIndex", stockIndex);
        return nbt;
    }

    public static AdminShopEntry fromNbt(CompoundTag nbt) {
        AdminShopEntry e = new AdminShopEntry(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "Id"));
        e.item = StackData.readStack(nbt.getCompound("Item"));
        e.baseBuyPrice = nbt.getLong("Buy");
        e.baseSellPrice = nbt.getLong("Sell");
        e.dynamic = nbt.getBoolean("Dynamic");
        e.stockIndex = nbt.getDouble("StockIndex");
        return e;
    }
}
