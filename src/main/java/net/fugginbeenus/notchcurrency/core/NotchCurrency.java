package net.fugginbeenus.notchcurrency.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
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

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> registerCommands(dispatcher));

        // Keep HUD in sync on join + respawn (covers SP & MP)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity sp = handler.player;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            if (newP instanceof ServerPlayerEntity sp) {
                NotchPackets.sendBalance(sp, BalanceStore.get(sp));
            }
        });

        // Server handles client’s explicit balance request (SP/MP safe)
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.BALANCE_REQUEST, (server, player, handler, buf, response) -> {
            server.execute(() -> {
                if (player instanceof ServerPlayerEntity sp) {
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
