package net.fugginbeenus.notchcurrency.economy.adminshop;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public class AdminShop {

    private final UUID id;
    private String name;
    private final List<AdminShopEntry> entries = new ArrayList<>();

    public AdminShop(String name) {
        this.id = UUID.randomUUID();
        this.name = name;
    }

    private AdminShop(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<AdminShopEntry> getEntries() { return Collections.unmodifiableList(entries); }

    public void addEntry(AdminShopEntry entry) { entries.add(entry); }

    public boolean removeEntry(UUID entryId) {
        return entries.removeIf(e -> e.getId().equals(entryId));
    }

    @Nullable
    public AdminShopEntry getEntry(UUID entryId) {
        for (AdminShopEntry e : entries) {
            if (e.getId().equals(entryId)) return e;
        }
        return null;
    }

    public void decayAll() {
        for (AdminShopEntry e : entries) e.decay();
    }

    // ---- NBT ----

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "Id", id);
        nbt.putString("Name", name);
        ListTag list = new ListTag();
        for (AdminShopEntry e : entries) list.add(e.toNbt());
        nbt.put("Entries", list);
        return nbt;
    }

    public static AdminShop fromNbt(CompoundTag nbt) {
        AdminShop shop = new AdminShop(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "Id"), nbt.getString("Name"));
        ListTag list = nbt.getList("Entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            shop.entries.add(AdminShopEntry.fromNbt(list.getCompound(i)));
        }
        return shop;
    }
}
