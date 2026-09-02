package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class QuestCommands {

    private QuestCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("quests")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) {
                        ctx.getSource().sendFailure(Component.literal("Run as a player."));
                        return 0;
                    }
                    net.fugginbeenus.notchcurrency.compat.Net.sendToClient(p,
                            net.fugginbeenus.notchcurrency.net.NotchPackets.QUEST_LOG,
                            net.fugginbeenus.notchcurrency.compat.Net.emptyBuf());
                    return 1;
                })
                .then(Commands.literal("list")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .executes(ctx -> {
                            var server = ctx.getSource().getServer();
                            var all = net.fugginbeenus.notchcurrency.economy.bounty.QuestManager.allQuests(server);
                            if (all.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No quests written yet.")
                                        .withStyle(ChatFormatting.GRAY), false);
                                return 1;
                            }
                            for (var q : all) {
                                ctx.getSource().sendSuccess(() -> Component.literal(q.getQuestKey() + ": ")
                                        .withStyle(ChatFormatting.GOLD)
                                        .append(Component.literal(q.describe()).withStyle(ChatFormatting.WHITE)), false);
                            }
                            return 1;
                        })));
    }
}
