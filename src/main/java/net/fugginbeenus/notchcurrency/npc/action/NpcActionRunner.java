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
            return runAll(sp, npc, pickOneLine(sp, npc, actions, orderedLines));
        } catch (Exception e) {
            LOGGER.error("NPC action list failed on {}", npc.getUUID(), e);
            return Outcome.COMPLETED;
        }
    }

    private static List<DialogueAction> pickOneLine(@Nullable ServerPlayer sp, NotchNpcEntity npc,
                                                    List<DialogueAction> actions, boolean ordered) {
        List<DialogueAction> lines = new java.util.ArrayList<>();
        for (DialogueAction a : actions) {
            if (a.type() != DialogueAction.Type.SAY_LINE) continue;
            if (a.hasOnlyIf() && (sp == null || !a.onlyIf().test(sp, npc))) continue;
            lines.add(a);
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
            if (a.hasOnlyIf() && (sp == null || !a.onlyIf().test(sp, npc))) continue;
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
                case HEAL_PLAYER -> {
                    if (sp == null || a.amount() <= 0) break;
                    float hearts = Math.min(20L, a.amount()) * 2.0f;
                    sp.heal(hearts);
                }
                case GIVE_EFFECT -> {
                    if (sp == null || a.value().isBlank()) break;
                    applyEffect(sp, a);
                }
                case TELEPORT -> {
                    if (sp == null || a.value().isBlank()) break;
                    teleport(sp, a.value());
                }
                case GIVE_QUEST -> {
                    if (sp == null || a.value().isBlank()) break;
                    net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.give(sp, a.value());
                }
                case TURN_IN_QUEST -> {
                    if (sp == null || a.value().isBlank()) break;
                    net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.turnIn(sp, a.value(), npc);
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

    private static void applyEffect(ServerPlayer sp, DialogueAction a) {
        String raw = a.value().trim();
        int level = 1;
        int space = raw.lastIndexOf(' ');
        if (space > 0) {
            try {
                level = Math.max(1, Math.min(3, Integer.parseInt(raw.substring(space + 1).trim())));
                raw = raw.substring(0, space).trim();
            } catch (NumberFormatException notALevel) {
                level = 1;
            }
        }
        net.minecraft.resources.ResourceLocation id =
                net.minecraft.resources.ResourceLocation.tryParse(raw);
        if (id == null) return;
        var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getOptional(id);
        if (effect.isEmpty()) return;
        int seconds = (int) Math.max(1L, Math.min(300L, a.amount() <= 0 ? 30L : a.amount()));
        sp.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                //? if >=1.21 {
                /*net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.get()),
                *///?} else {
                effect.get(),
                //?}
                seconds * 20, level - 1));
    }

    private static void teleport(ServerPlayer sp, String spec) {
        String[] parts = spec.trim().split("\\s+");
        int at = 0;
        net.minecraft.server.level.ServerLevel target = sp.serverLevel();
        if (parts.length == 4) {
            net.minecraft.resources.ResourceLocation dim =
                    net.minecraft.resources.ResourceLocation.tryParse(parts[0]);
            if (dim == null) return;
            net.minecraft.server.level.ServerLevel found = sp.serverLevel().getServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION, dim));
            if (found == null) return;
            target = found;
            at = 1;
        } else if (parts.length != 3) {
            return;
        }
        double x, y, z;
        try {
            x = Double.parseDouble(parts[at]) + 0.5;
            y = Double.parseDouble(parts[at + 1]);
            z = Double.parseDouble(parts[at + 2]) + 0.5;
        } catch (NumberFormatException badNumber) {
            return;
        }
        net.fugginbeenus.notchcurrency.compat.Teleport.move(sp, target, x, safeY(target, x, y, z), z);
    }

    private static double safeY(net.minecraft.server.level.ServerLevel level,
                                double x, double y, double z) {
        int bx = net.minecraft.util.Mth.floor(x), bz = net.minecraft.util.Mth.floor(z);
        int from = net.minecraft.util.Mth.floor(y);
        level.getChunk(bx >> 4, bz >> 4);
        if (isClear(level, bx, from, bz)) return from;
        for (int step = 1; step <= 48; step++) {
            if (isClear(level, bx, from - step, bz)) return from - step;
            if (isClear(level, bx, from + step, bz)) return from + step;
        }
        return level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz);
    }

    private static boolean isClear(net.minecraft.server.level.ServerLevel level, int x, int y, int z) {
        net.minecraft.core.BlockPos feet = new net.minecraft.core.BlockPos(x, y, z);
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()
                && !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty();
    }
}
