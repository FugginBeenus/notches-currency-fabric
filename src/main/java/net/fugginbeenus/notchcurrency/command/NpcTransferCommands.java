package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.npc.NpcTransfer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;

import java.util.UUID;

public final class NpcTransferCommands {

    private NpcTransferCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("npctransfer").requires(net.fugginbeenus.notchcurrency.compat.Perms.command("npctransfer"))
                .then(Commands.literal("offer")
                        .then(Commands.argument("npc", StringArgumentType.word())
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> {
                                            UUID npc;
                                            try {
                                                npc = UUID.fromString(StringArgumentType.getString(ctx, "npc"));
                                            } catch (IllegalArgumentException e) {
                                                ctx.getSource().sendFailure(Component.literal("That isn't an NPC id."));
                                                return 0;
                                            }
                                            NpcTransfer.offer(ctx.getSource().getPlayerOrException(), npc,
                                                    EntityArgument.getPlayer(ctx, "player"));
                                            return 1;
                                        }))))
                .then(Commands.literal("accept")
                        .executes(ctx -> {
                            NpcTransfer.accept(ctx.getSource().getPlayerOrException());
                            return 1;
                        }))
                .then(Commands.literal("decline")
                        .executes(ctx -> {
                            NpcTransfer.decline(ctx.getSource().getPlayerOrException());
                            return 1;
                        })));
    }
}
