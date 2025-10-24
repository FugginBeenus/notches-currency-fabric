package net.fugginbeenus.notchcurrency.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NotchCurrency implements ModInitializer {

    public static final String MOD_ID = "notchcurrency";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        // Registries
        ModBlocks.register();
        ModItems.register();
        ModScreenHandlers.register();

        // Trade manager (register tick + packet handlers once)
        TradeManager.init();

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            registerCommands(dispatcher);

            // /trade command tree
            dispatcher.register(
                    CommandManager.literal("trade")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity from = ctx.getSource().getPlayer();
                                        ServerPlayerEntity to   = EntityArgumentType.getPlayer(ctx, "player");
                                        TradeManager.invite(from, to);
                                        return 1;
                                    })
                            )
                            .then(CommandManager.literal("accept")
                                    .then(CommandManager.argument("inviter", StringArgumentType.word())
                                            .executes(ctx -> {
                                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                String inviter = StringArgumentType.getString(ctx, "inviter");
                                                TradeManager.accept(p, inviter);
                                                return 1;
                                            })
                                    )
                            )
                            .then(CommandManager.literal("decline")
                                    .then(CommandManager.argument("inviter", StringArgumentType.word())
                                            .executes(ctx -> {
                                                ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                String inviter = StringArgumentType.getString(ctx, "inviter");
                                                TradeManager.decline(p, inviter);
                                                return 1;
                                            })
                                    )
                            )
            );
        });

        // Keep HUD in sync on join + respawn (covers SP & MP)
        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            if (newP instanceof ServerPlayerEntity) {
                ServerPlayerEntity sp = (ServerPlayerEntity) newP;
                NotchPackets.sendBalance(sp, BalanceStore.get(sp));
            }
        });

// Server handles client’s explicit balance request (SP/MP safe)
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.BALANCE_REQUEST, (server, player, handler, buf, response) -> {
            server.execute(() -> {
                if (player instanceof ServerPlayerEntity) {
                    ServerPlayerEntity sp = (ServerPlayerEntity) player;
                    NotchPackets.sendBalance(sp, BalanceStore.get(sp));
                }
            });
        });
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("givnotches")
                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                            player.giveItemStack(new ItemStack(ModItems.NOTCH_COIN, amount));
                            player.sendMessage(Text.literal("Given " + amount + " Notch Coins!"), false);
                            return 1;
                        })
                )
        );
    }
}
