package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.minecraft.nbt.NbtCompound;

/**
 * One thing a dialogue choice does when clicked. Choices carry a list of these, run in order by
 * {@link NpcDialogueManager}. Economy-native by design: coins move through the CurrencyApi with
 * proper SINK/FAUCET tagging.
 */
public class DialogueAction {

    public enum Type {
        /** No-op (placeholder). */
        NONE,
        /** Say {@code value} to the player who set this off, as a chat line under the NPC's name.
         *  Supports the same placeholders and {@code &} colours as dialogue pages. */
        SAY_LINE,
        /** Open the NPC's assigned role feature (shop, bank, auction, ...). Ends the dialogue. */
        OPEN_ROLE,
        /** Open a SPECIFIC economy screen regardless of the NPC's role: {@code value} is an
         *  {@code NpcRole} name (BANKER, AUCTIONEER, MAILBOX, RAFFLE, BOUNTY, DEALER). Ends the dialogue. */
        OPEN_SCREEN,
        /** Give the player {@code amount} coins (a FAUCET: use sparingly). */
        PAY_COINS,
        /** Charge the player {@code amount} coins (a SINK). Fails the choice if they can't afford it. */
        CHARGE_COINS,
        /** Give the player {@code amount} × item {@code value} (an item id). */
        GIVE_ITEM,
        /** Run {@code value} as a server command ({@code %player%}/{@code %npc%} substituted). */
        RUN_COMMAND,
        /** Run {@code value} as the clicking player (their permissions). */
        RUN_COMMAND_AS_PLAYER
    }

    /**
     * Actions only an operator may author. Two of these mint value out of nothing: an ordinary
     * player who could set up an NPC that pays coins or hands out items would have an infinite
     * money printer, which is exactly what an economy mod must not allow. The command ones are
     * dangerous for the usual reason.
     *
     * <p>The single source of truth for both dialogue choices and trigger reactions, and for both
     * the editing screens and the server-side save checks.
     */
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
