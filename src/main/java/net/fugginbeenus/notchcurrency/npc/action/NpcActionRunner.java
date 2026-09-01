package net.fugginbeenus.notchcurrency.npc.action;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NpcText;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class NpcActionRunner {

    public enum Outcome {
        COMPLETED,
        OPENED_SCREEN,
        ABORTED
    }

    private NpcActionRunner() {}

    private static void playLineSound(NotchNpcEntity npc, DialogueAction a) {
        if (!a.hasSound() || !(npc.level() instanceof net.minecraft.server.level.ServerLevel world)) return;
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.resources.ResourceLocation.tryParse(a.sound());
        if (id == null) return;
        net.minecraft.sounds.SoundEvent event = net.minecraft.sounds.SoundEvent.createVariableRangeEvent(id);
        world.playSound(null, npc.getX(), npc.getY(), npc.getZ(), event,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, 1.0f);
    }

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("NotchCurrency-NpcActions");

    private static final java.util.Map<java.util.UUID, Integer> LINE_TURN = new java.util.HashMap<>();
    private static final java.util.Random LINE_PICK = new java.util.Random();

    public static Outcome run(@Nullable ServerPlayer sp, NotchNpcEntity npc, List<DialogueAction> actions) {
        return run(sp, npc, actions, false);
    }

    public static Outcome run(@Nullable ServerPlayer sp, NotchNpcEntity npc, List<DialogueAction> actions,
                              boolean orderedLines) {
        if (actions == null || actions.isEmpty()) return Outcome.COMPLETED;
        try {
            return runAll(sp, npc, pickOneLine(npc, actions, orderedLines));
        } catch (Exception e) {
            LOGGER.error("NPC action list failed on {}", npc.getUUID(), e);
            return Outcome.COMPLETED;
        }
    }

    private static List<DialogueAction> pickOneLine(NotchNpcEntity npc, List<DialogueAction> actions,
                                                    boolean ordered) {
        List<DialogueAction> lines = new java.util.ArrayList<>();
        for (DialogueAction a : actions) {
            if (a.type() == DialogueAction.Type.SAY_LINE) lines.add(a);
        }
        if (lines.size() < 2) return actions;

        DialogueAction chosen;
        if (ordered) {
            int turn = LINE_TURN.merge(npc.getUUID(), 1, Integer::sum) - 1;
            chosen = lines.get(Math.floorMod(turn, lines.size()));
        } else {
            chosen = lines.get(LINE_PICK.nextInt(lines.size()));
        }

        List<DialogueAction> out = new java.util.ArrayList<>();
        for (DialogueAction a : actions) {
            if (a.type() == DialogueAction.Type.SAY_LINE && a != chosen) continue;
            out.add(a);
        }
        return out;
    }

    private static Outcome runAll(@Nullable ServerPlayer sp, NotchNpcEntity npc, List<DialogueAction> actions) {
        boolean openedScreen = false;
        for (DialogueAction a : actions) {
            switch (a.type()) {
                case NONE -> { }
                case SAY_LINE -> {
                    playLineSound(npc, a);
                    if (a.hideText() || a.value().isEmpty()) break;
                    if (sp != null) {
                        NpcText.say(sp, npc, a.value());
                        break;
                    }
                    sayNearby(npc, a.value());
                }
                case OPEN_ROLE -> {
                    if (sp == null) break;
                    NpcDialogueManager.openRole(sp, npc);
                    openedScreen = true;
                }
                case OPEN_SCREEN -> {
                    if (sp == null) break;
                    try {
                        var role = net.fugginbeenus.notchcurrency.economy.npc.NpcRole.valueOf(a.value());
                        NpcRoleDispatch.open(sp, role, null, npc);
                        NpcDialogueManager.watchForFarewell(sp, npc);
                        openedScreen = true;
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                case PAY_COINS -> {
                    if (sp != null && a.amount() > 0) {
                        CurrencyApi.deposit(sp, a.amount(), TransactionReason.FAUCET, "NPC dialogue reward");
                    }
                }
                case CHARGE_COINS -> {
                    if (sp == null || a.amount() <= 0) break;
                    if (!CurrencyApi.withdraw(sp, a.amount(), TransactionReason.SINK, "NPC dialogue fee")) {
                        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You can't afford that (" + a.amount() + " "
                                        + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ").")
                                .withStyle(ChatFormatting.RED));
                        return Outcome.ABORTED;
                    }
                }
                case GIVE_ITEM -> {
                    if (sp != null) giveItem(sp, a);
                }
                case RUN_COMMAND -> runCommand(sp, npc, a.value(), false);
                case RUN_COMMAND_AS_PLAYER -> {
                    if (sp != null) runCommand(sp, npc, a.value(), true);
                }
            }
        }
        return openedScreen ? Outcome.OPENED_SCREEN : Outcome.COMPLETED;
    }

    private static final double EARSHOT = 16.0;

    private static void sayNearby(NotchNpcEntity npc, String line) {
        if (line == null || line.isBlank()) return;
        if (!(npc.level() instanceof net.minecraft.server.level.ServerLevel world)) return;
        double r2 = EARSHOT * EARSHOT;
        npc.playVoice();
        for (ServerPlayer near : world.players()) {
            if (near.distanceToSqr(npc) <= r2) {
                NpcText.sendLine(near, npc, line);
            }
        }
    }

    private static void giveItem(ServerPlayer sp, DialogueAction a) {
        ResourceLocation id = ResourceLocation.tryParse(a.value());
        if (id == null) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == Items.AIR || a.amount() <= 0) return;
        int remaining = (int) Math.min(a.amount(), 64L * 9L);
        while (remaining > 0) {
            int give = Math.min(remaining, new ItemStack(item).getMaxStackSize());
            sp.getInventory().placeItemBackInInventory(new ItemStack(item, give));
            remaining -= give;
        }
    }

    public static boolean ownerMayRunCommands(NotchNpcEntity npc, MinecraftServer server) {
        if (npc.getOwnerType() == NotchNpcEntity.OwnerType.SERVER) return true;
        UUID owner = npc.getOwner();
        if (owner == null) return false;
        ServerPlayer online = server.getPlayerList().getPlayer(owner);
        if (online != null) return net.fugginbeenus.notchcurrency.compat.Perms.isOperator(online);
        return net.fugginbeenus.notchcurrency.compat.Profiles.isOp(server, owner);
    }

    private static void runCommand(@Nullable ServerPlayer sp, NotchNpcEntity npc,
                                   String command, boolean asPlayer) {
        if (command == null || command.isBlank()) return;
        MinecraftServer server = sp != null ? sp.level().getServer() : npc.level().getServer();
        if (server == null) return;
        if (!ownerMayRunCommands(npc, server)) return;
        String cmd = NpcText.substitute(command, sp, NpcText.npcName(npc));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        var source = (asPlayer && sp != null) ? sp.createCommandSourceStack() : server.createCommandSourceStack();
        server.getCommands().performPrefixedCommand(source, cmd);
    }
}
