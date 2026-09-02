package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.minecraft.nbt.CompoundTag;

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
        RUN_COMMAND_AS_PLAYER,
        HEAL_PLAYER,
        GIVE_EFFECT
    }

    public static boolean isAdminOnly(Type t) {
        return t == Type.PAY_COINS || t == Type.GIVE_ITEM || t == Type.GIVE_EFFECT
                || t == Type.RUN_COMMAND || t == Type.RUN_COMMAND_AS_PLAYER;
    }

    private Type type = Type.NONE;
    private String value = "";
    private long amount = 0;
    private String sound = "";
    private boolean hideText = false;

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

    public String sound() { return sound; }
    public void setSound(String id) { this.sound = id == null ? "" : id.trim(); }
    public boolean hideText() { return hideText; }
    public void setHideText(boolean hide) { this.hideText = hide; }
    public boolean hasSound() { return !sound.isEmpty(); }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Type", type.name());
        nbt.putString("Value", value);
        nbt.putLong("Amount", amount);
        if (!sound.isEmpty()) nbt.putString("Sound", sound);
        if (hideText) nbt.putBoolean("HideText", true);
        return nbt;
    }

    public static DialogueAction fromNbt(CompoundTag nbt) {
        DialogueAction a = new DialogueAction();
        try {
            a.type = Type.valueOf(nbt.getString("Type"));
        } catch (IllegalArgumentException e) {
            a.type = Type.NONE;
        }
        a.value = nbt.getString("Value");
        a.amount = nbt.getLong("Amount");
        a.sound = nbt.getString("Sound");
        a.hideText = nbt.getBoolean("HideText");
        return a;
    }
}
