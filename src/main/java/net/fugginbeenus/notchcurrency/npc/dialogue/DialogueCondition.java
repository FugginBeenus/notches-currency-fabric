package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * A requirement gating a dialogue choice. All conditions on a choice must pass; failing choices
 * render locked (greyed) or hidden, per the choice's setting. Economy-native: coin and item checks
 * out of the box.
 */
public class DialogueCondition {

    public enum Type {
        /** No requirement — always passes. An editor placeholder; stripped on save. */
        NONE,
        /** Player balance ≥ {@code amount}. */
        HAS_COINS,
        /** Player carries ≥ {@code amount} of item {@code value} (an item id). */
        HAS_ITEM,
        /** Player owns this NPC. */
        IS_OWNER,
        /** Player is an operator. */
        IS_OP
    }

    private Type type = Type.HAS_COINS;
    private String value = "";
    private long amount = 0;

    public DialogueCondition() {}

    public DialogueCondition(Type type, String value, long amount) {
        this.type = type == null ? Type.HAS_COINS : type;
        this.value = value == null ? "" : value;
        this.amount = amount;
    }

    public Type type() { return type; }
    public String value() { return value; }
    public long amount() { return amount; }

    public void setType(Type t) { this.type = t == null ? Type.HAS_COINS : t; }
    public void setValue(String v) { this.value = v == null ? "" : v; }
    public void setAmount(long a) { this.amount = a; }

    public boolean test(ServerPlayerEntity sp, NotchNpcEntity npc) {
        return switch (type) {
            case NONE -> true;
            case HAS_COINS -> CurrencyApi.getBalance(sp) >= amount;
            case HAS_ITEM -> countItem(sp) >= amount;
            case IS_OWNER -> npc.isOwnedBy(sp);
            case IS_OP -> sp.hasPermissionLevel(2);
        };
    }

    private long countItem(ServerPlayerEntity sp) {
        Item item = Registries.ITEM.get(Identifier.tryParse(value) == null
                ? new Identifier("minecraft", "air") : Identifier.tryParse(value));
        if (item == net.minecraft.item.Items.AIR) return 0;
        long count = 0;
        for (int i = 0; i < sp.getInventory().size(); i++) {
            ItemStack st = sp.getInventory().getStack(i);
            if (st.isOf(item)) count += st.getCount();
        }
        return count;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Type", type.name());
        nbt.putString("Value", value);
        nbt.putLong("Amount", amount);
        return nbt;
    }

    public static DialogueCondition fromNbt(NbtCompound nbt) {
        DialogueCondition c = new DialogueCondition();
        try {
            c.type = Type.valueOf(nbt.getString("Type"));
        } catch (IllegalArgumentException e) {
            c.type = Type.HAS_COINS;
        }
        c.value = nbt.getString("Value");
        c.amount = nbt.getLong("Amount");
        return c;
    }
}
