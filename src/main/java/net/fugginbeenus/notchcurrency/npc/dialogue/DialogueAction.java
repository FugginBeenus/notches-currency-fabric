package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.minecraft.nbt.NbtCompound;

public class DialogueAction {

    public enum Type {
        NONE,
        SAY_LINE,
        OPEN_ROLE,
        OPEN_SCREEN,
        PAY_COINS,
        CHARGE_COINS,
        GIVE_ITEM,
        RUN_COMMAND,
        RUN_COMMAND_AS_PLAYER
    }

    public static boolean isAdminOnly(Type t) {
        return t == Type.PAY_COINS || t == Type.GIVE_ITEM
                || t == Type.RUN_COMMAND || t == Type.RUN_COMMAND_AS_PLAYER;
    }

    private Type type = Type.NONE;
    private String value = "";
    private long amount = 0;

    public DialogueAction() {}

    public DialogueAction(Type type, String value, long amount) {
        this.type = type == null ? Type.NONE : type;
        this.value = value == null ? "" : value;
        this.amount = amount;
    }

    public Type type() { return type; }
    public String value() { return value; }
    public long amount() { return amount; }

    public void setType(Type t) { this.type = t == null ? Type.NONE : t; }
    public void setValue(String v) { this.value = v == null ? "" : v; }
    public void setAmount(long a) { this.amount = a; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Type", type.name());
        nbt.putString("Value", value);
        nbt.putLong("Amount", amount);
        return nbt;
    }

    public static DialogueAction fromNbt(NbtCompound nbt) {
        DialogueAction a = new DialogueAction();
        try {
            a.type = Type.valueOf(nbt.getString("Type"));
        } catch (IllegalArgumentException e) {
            a.type = Type.NONE;
        }
        a.value = nbt.getString("Value");
        a.amount = nbt.getLong("Amount");
        return a;
    }
}
