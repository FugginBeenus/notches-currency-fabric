package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.minecraft.client.Minecraft;

public final class NpcActionEditing {

    private NpcActionEditing() {}

    public static final DialogueAction.Type[] PALETTE = {
            DialogueAction.Type.SAY_LINE,
            DialogueAction.Type.PAY_COINS,
            DialogueAction.Type.CHARGE_COINS,
            DialogueAction.Type.GIVE_ITEM,
            DialogueAction.Type.HEAL_PLAYER,
            DialogueAction.Type.GIVE_EFFECT,
            DialogueAction.Type.TELEPORT,
            DialogueAction.Type.GIVE_QUEST,
            DialogueAction.Type.TURN_IN_QUEST,
            DialogueAction.Type.PLAY_ANIMATION,
            DialogueAction.Type.RUN_COMMAND,
            DialogueAction.Type.RUN_COMMAND_AS_PLAYER,
    };

    public static boolean adminActionsAllowed() {
        return net.fugginbeenus.notchcurrency.compat.PermsClient.isOperator();
    }

    public static boolean needsValue(DialogueAction.Type t) {
        return t == DialogueAction.Type.SAY_LINE || t == DialogueAction.Type.GIVE_ITEM
                || t == DialogueAction.Type.GIVE_EFFECT || t == DialogueAction.Type.TELEPORT
                || t == DialogueAction.Type.GIVE_QUEST || t == DialogueAction.Type.TURN_IN_QUEST
                || t == DialogueAction.Type.PLAY_ANIMATION
                || t == DialogueAction.Type.RUN_COMMAND || t == DialogueAction.Type.RUN_COMMAND_AS_PLAYER;
    }

    public static boolean needsAmount(DialogueAction.Type t) {
        return t == DialogueAction.Type.PAY_COINS || t == DialogueAction.Type.CHARGE_COINS
                || t == DialogueAction.Type.GIVE_ITEM || t == DialogueAction.Type.HEAL_PLAYER
                || t == DialogueAction.Type.GIVE_EFFECT;
    }

    public static boolean condNeedsValue(net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type t) {
        return t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.HAS_ITEM
                || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.IS_FACTION
                || condIsQuest(t);
    }

    public static boolean condNeedsAmount(net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type t) {
        return t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.HAS_COINS
                || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.HAS_ITEM
                || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.HAS_XP_LEVEL;
    }

    public static boolean condIsQuest(net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type t) {
        return t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.QUEST_TAKEN
                || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.QUEST_DONE
                || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.QUEST_NOT_DONE
                || t == net.fugginbeenus.notchcurrency.npc.dialogue.DialogueCondition.Type.QUEST_READY;
    }

    public static String actionName(DialogueAction.Type t) {
        return switch (t) {
            case SAY_LINE -> "Say a line";
            case PAY_COINS -> "Pay player coins";
            case CHARGE_COINS -> "Charge coins";
            case GIVE_ITEM -> "Give item";
            case RUN_COMMAND -> "Server command";
            case RUN_COMMAND_AS_PLAYER -> "Player command";
            case HEAL_PLAYER -> "Heal player";
            case GIVE_EFFECT -> "Give effect";
            case TELEPORT -> "Teleport player";
            case GIVE_QUEST -> "Give quest";
            case TURN_IN_QUEST -> "Turn in quest";
            case PLAY_ANIMATION -> "Play animation";
            default -> "None";
        };
    }

    public static String valueHint(DialogueAction.Type t) {
        return switch (t) {
            case SAY_LINE -> "what it says";
            case GIVE_ITEM -> "item id, e.g. minecraft:bread";
            case RUN_COMMAND, RUN_COMMAND_AS_PLAYER -> "command, without the slash";
            case GIVE_EFFECT -> "effect id, e.g. minecraft:regeneration";
            case TELEPORT -> "x y z, or world x y z";
            case GIVE_QUEST, TURN_IN_QUEST -> "quest name";
            case PLAY_ANIMATION -> "animation name";
            default -> "";
        };
    }

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

    public static String describe(DialogueAction a) {
        String name = actionName(a.type());
        if (a.type() == DialogueAction.Type.HEAL_PLAYER) return name + ": " + a.amount() + " hearts";
        if (a.type() == DialogueAction.Type.GIVE_EFFECT) {
            return name + ": " + shorten(a.value()) + " " + a.amount() + "s";
        }
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
