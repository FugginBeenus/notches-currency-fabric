package net.fugginbeenus.notchcurrency.npc.action;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NpcText;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Runs a list of {@link DialogueAction}s. One implementation serves both dialogue choices and
 * {@link NpcTrigger} reactions, so an action behaves the same wherever it was set off.
 *
 * <p>The player is optional. A dialogue choice always has one; a trigger might not (an NPC dying to
 * lava, for instance). Actions that only make sense for a player are skipped in that case rather than
 * guessing a target.
 */
public final class NpcActionRunner {

    /** What the caller needs to know after the list has run. */
    public enum Outcome {
        /** Everything ran; carry on. */
        COMPLETED,
        /** A screen was opened and has taken over the player's view. */
        OPENED_SCREEN,
        /** An action failed in a way that should undo the interaction (couldn't afford a charge). */
        ABORTED
    }

    private NpcActionRunner() {}

    private static final org.slf4j.Logger LOGGER =
            org.slf4j.LoggerFactory.getLogger("NotchCurrency-NpcActions");

    public static Outcome run(@Nullable ServerPlayerEntity sp, NotchNpcEntity npc, List<DialogueAction> actions) {
        if (actions == null || actions.isEmpty()) return Outcome.COMPLETED;
        try {
            return runAll(sp, npc, actions);
        } catch (Exception e) {
            // These run from damage and death handlers as well as dialogue. A bad stored action must
            // never take the entity down with it.
            LOGGER.error("NPC action list failed on {}", npc.getUuid(), e);
            return Outcome.COMPLETED;
        }
    }

    private static Outcome runAll(@Nullable ServerPlayerEntity sp, NotchNpcEntity npc, List<DialogueAction> actions) {
        boolean openedScreen = false;
        for (DialogueAction a : actions) {
            switch (a.type()) {
                case NONE -> { }
                case SAY_LINE -> {
                    // Spoken to whoever set it off, the same way dialogue's chat mode does: private,
                    // so a busy street of NPCs doesn't fill everyone's chat.
                    if (sp != null) {
                        NpcText.say(sp, npc, a.value());
                        break;
                    }
                    // Nobody set it off: a schedule turning over, or a death with no killer. The line
                    // still has an audience, it's just whoever is standing close enough to hear it.
                    // Skipping these would quietly make "announce opening time" do nothing at all.
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
                        // Unknown screen id: skip.
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
                        sp.sendMessage(Text.literal("You can't afford that (" + a.amount() + " "
                                        + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ").")
                                .formatted(Formatting.RED), false);
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

    /** Earshot for a line nobody triggered. Wide enough to carry across a market square, short
     *  enough that a town of scheduled NPCs doesn't turn into a wall of chat. */
    private static final double EARSHOT = 16.0;

    private static void sayNearby(NotchNpcEntity npc, String line) {
        if (line == null || line.isBlank()) return;
        if (!(npc.getWorld() instanceof net.minecraft.server.world.ServerWorld world)) return;
        double r2 = EARSHOT * EARSHOT;
        for (ServerPlayerEntity near : world.getPlayers()) {
            if (near.squaredDistanceTo(npc) <= r2) {
                NpcText.say(near, npc, line);
            }
        }
    }

    private static void giveItem(ServerPlayerEntity sp, DialogueAction a) {
        Identifier id = Identifier.tryParse(a.value());
        if (id == null) return;
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR || a.amount() <= 0) return;
        int remaining = (int) Math.min(a.amount(), 64L * 9L);
        while (remaining > 0) {
            int give = Math.min(remaining, item.getMaxCount());
            sp.getInventory().offerOrDrop(new ItemStack(item, give));
            remaining -= give;
        }
    }

    /** Commands only run for NPCs whose owner is an operator (or server-owned NPCs): a stored
     *  command must never outlive its author's authority. */
    public static boolean ownerMayRunCommands(NotchNpcEntity npc, MinecraftServer server) {
        if (npc.getOwnerType() == NotchNpcEntity.OwnerType.SERVER) return true;
        UUID owner = npc.getOwner();
        if (owner == null) return false;
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(owner);
        if (online != null) return online.hasPermissionLevel(2);
        var profile = server.getUserCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty()
                : server.getUserCache().getByUuid(owner);
        return profile.isPresent() && server.getPlayerManager().isOperator(profile.get());
    }

    private static void runCommand(@Nullable ServerPlayerEntity sp, NotchNpcEntity npc,
                                   String command, boolean asPlayer) {
        if (command == null || command.isBlank()) return;
        MinecraftServer server = sp != null ? sp.getServer() : npc.getServer();
        if (server == null) return;
        if (!ownerMayRunCommands(npc, server)) return;
        String cmd = NpcText.substitute(command, sp, NpcText.npcName(npc));
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        // "As player" needs a player; without one the console source is the only sensible actor.
        var source = (asPlayer && sp != null) ? sp.getCommandSource() : server.getCommandSource();
        server.getCommandManager().executeWithPrefix(source, cmd);
    }
}
