package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class DialogueCondition {

    public enum Type {
        NONE,
        HAS_COINS,
        HAS_ITEM,
        IS_OWNER,
        IS_OP,
        IS_FACTION,
        IS_DAY,
        IS_NIGHT,
        HAS_XP_LEVEL,
        QUEST_TAKEN,
        QUEST_DONE,
        QUEST_NOT_DONE,
        QUEST_READY
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

    public boolean test(ServerPlayer sp, NotchNpcEntity npc) {
        return switch (type) {
            case NONE -> true;
            case HAS_COINS -> CurrencyApi.getBalance(sp) >= amount;
            case HAS_ITEM -> countItem(sp) >= amount;
            case IS_OWNER -> npc.isOwnedBy(sp);
            case IS_OP -> net.fugginbeenus.notchcurrency.compat.Perms.isOperator(sp);
            case IS_FACTION -> matchesFaction(sp, npc);
            case IS_DAY -> sp.serverLevel().isDay();
            case IS_NIGHT -> !sp.serverLevel().isDay();
            case HAS_XP_LEVEL -> sp.experienceLevel >= amount;
            case QUEST_TAKEN -> net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.hasTaken(sp, value);
            case QUEST_DONE -> net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.hasDone(sp, value);
            case QUEST_NOT_DONE -> !net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.hasDone(sp, value);
            case QUEST_READY -> net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.isReady(sp, value);
        };
    }

    private boolean matchesFaction(ServerPlayer sp, NotchNpcEntity npc) {
        String wanted = value == null || value.isBlank() ? npc.getFactionId() : value.trim();
        if (wanted.isEmpty()) return false;
        String theirs = net.fugginbeenus.notchcurrency.npc.faction.FactionState
                .get(sp.serverLevel()).factionIdOf(sp.getUUID());
        return wanted.equals(theirs);
    }

    private long countItem(ServerPlayer sp) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.tryParse(value) == null
                ? Reg.id("minecraft", "air") : ResourceLocation.tryParse(value));
        if (item == net.minecraft.world.item.Items.AIR) return 0;
        long count = 0;
        for (int i = 0; i < sp.getInventory().getContainerSize(); i++) {
            ItemStack st = sp.getInventory().getItem(i);
            if (st.is(item)) count += st.getCount();
        }
        return count;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", type.name());
        nbt.putString("Value", value);
        nbt.putLong("Amount", amount);
        return nbt;
    }

    public static DialogueCondition fromNbt(CompoundTag nbt) {
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
