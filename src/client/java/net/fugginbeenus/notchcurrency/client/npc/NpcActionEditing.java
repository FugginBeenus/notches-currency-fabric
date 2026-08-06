package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.client.MinecraftClient;

/**
 * The rules for editing a list of NPC actions, in one place.
 *
 * <p>Reactions and schedule entries offer the same vocabulary and have to enforce the same admin
 * gate, so the palette, the cycle order and the "which fields does this kind need" questions live
 * here rather than once per screen. Two copies of an admin check is one copy too many: the day a new
 * action type is added, the screen nobody remembered becomes the way around it.
 *
 * <p>Client-side convenience only. The server re-validates everything on save and is the actual gate.
 */
public final class NpcActionEditing {

    private NpcActionEditing() {}

    /** The kinds worth offering, in cycle order. */
    public static final DialogueAction.Type[] PALETTE = {
            DialogueAction.Type.SAY_LINE,
            DialogueAction.Type.PAY_COINS,
            DialogueAction.Type.CHARGE_COINS,
            DialogueAction.Type.GIVE_ITEM,
            DialogueAction.Type.RUN_COMMAND,
            DialogueAction.Type.RUN_COMMAND_AS_PLAYER,
    };

    public static boolean adminActionsAllowed() {
        return MinecraftClient.getInstance().player != null
                && MinecraftClient.getInstance().player.hasPermissionLevel(2);
    }

    public static boolean needsValue(DialogueAction.Type t) {
        return t == DialogueAction.Type.SAY_LINE || t == DialogueAction.Type.GIVE_ITEM
                || t == DialogueAction.Type.RUN_COMMAND || t == DialogueAction.Type.RUN_COMMAND_AS_PLAYER;
    }

    public static boolean needsAmount(DialogueAction.Type t) {
        return t == DialogueAction.Type.PAY_COINS || t == DialogueAction.Type.CHARGE_COINS
                || t == DialogueAction.Type.GIVE_ITEM;
    }

    public static String actionName(DialogueAction.Type t) {
        return switch (t) {
            case SAY_LINE -> "Say a line";
            case PAY_COINS -> "Pay player coins";
            case CHARGE_COINS -> "Charge coins";
            case GIVE_ITEM -> "Give item";
            case RUN_COMMAND -> "Server command";
            case RUN_COMMAND_AS_PLAYER -> "Player command";
            default -> "None";
        };
    }

    /** What this kind of action wants typed into the value box. */
    public static String valueHint(DialogueAction.Type t) {
        return switch (t) {
            case SAY_LINE -> "what it says";
            case GIVE_ITEM -> "item id, e.g. minecraft:bread";
            case RUN_COMMAND, RUN_COMMAND_AS_PLAYER -> "command, without the slash";
            default -> "";
        };
    }

    /** Step an action to the next kind, skipping admin-only kinds for players who can't use them. */
    public static void cycleType(DialogueAction a) {
        int at = 0;
        for (int i = 0; i < PALETTE.length; i++) {
            if (PALETTE[i] == a.type()) {
                at = i;
                break;
            }
        }
        for (int step = 1; step <= PALETTE.length; step++) {
            DialogueAction.Type next = PALETTE[(at + step) % PALETTE.length];
            if (DialogueAction.isAdminOnly(next) && !adminActionsAllowed()) continue;
            a.setType(next);
            return;
        }
    }

    /** A one-line summary for a list row. */
    public static String describe(DialogueAction a) {
        String name = actionName(a.type());
        if (needsAmount(a.type()) && needsValue(a.type())) {
            return name + ": " + a.amount() + "x " + shorten(a.value());
        }
        if (needsAmount(a.type())) return name + ": " + a.amount();
        if (needsValue(a.type())) return name + ": " + shorten(a.value());
        return name;
    }

    private static String shorten(String s) {
        if (s == null || s.isBlank()) return "(empty)";
        return s.length() <= 22 ? s : s.substring(0, 21) + "...";
    }
}
