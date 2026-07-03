package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.auction.AuctionCategories;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionListing;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

/** Extracted from the mod initializer; registered from NotchCurrency.onInitialize(). */
public final class CrateCommands {

    private CrateCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /balloon (spawn + admin settings) =====
        dispatcher.register(
                CommandManager.literal("balloon")
                        .requires(src -> src.hasPermissionLevel(2))

                        // /balloon spawn [pos]
                        .then(CommandManager.literal("spawn")
                                // no args -> at player
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    var world = src.getWorld();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) {
                                        src.sendError(Text.literal(
                                                "Run as a player or use: /balloon spawn <x> <y> <z>"));
                                        return 0;
                                    }
                                    BlockPos base = player.getBlockPos().up(12);
                                    BalloonEntity b = new BalloonEntity(world,
                                            base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                                    world.spawnEntity(b);
                                    src.sendFeedback(() -> Text.literal(
                                            "Spawned test balloon at " + base.getX() + " " + base.getY() + " " + base.getZ()
                                    ), false);
                                    return 1;
                                })
                                // with explicit block pos
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getWorld();
                                            BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                                            BalloonEntity b = new BalloonEntity(world,
                                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                            world.spawnEntity(b);
                                            src.sendFeedback(() -> Text.literal(
                                                    "Spawned test balloon at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                                            ), false);
                                            return 1;
                                        })
                                )
                        )

                        // /balloon setArea <x> <y> <z> <radius>
                        .then(CommandManager.literal("setArea")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> {
                                                                    var src = ctx.getSource();
                                                                    var world = src.getWorld();
                                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                    int r = IntegerArgumentType.getInteger(ctx, "radius");
                                                                    DailyCrateManager.setArea((ServerWorld) world,
                                                                            new BlockPos(x, y, z), r);
                                                                    src.sendFeedback(() -> Text.literal(
                                                                            "Balloon area set to (" + x + "," + y + "," + z + ") r=" + r
                                                                    ), false);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )

                        // /balloon setYRange <minY> <maxY>
                        .then(CommandManager.literal("setYRange")
                                .then(CommandManager.argument("minY", IntegerArgumentType.integer(5))
                                        .then(CommandManager.argument("maxY", IntegerArgumentType.integer(6))
                                                .executes(ctx -> {
                                                    var src = ctx.getSource();
                                                    var world = src.getWorld();
                                                    int minY = IntegerArgumentType.getInteger(ctx, "minY");
                                                    int maxY = IntegerArgumentType.getInteger(ctx, "maxY");
                                                    DailyCrateManager.setYRange((ServerWorld) world, minY, maxY);
                                                    src.sendFeedback(() -> Text.literal(
                                                            "Balloon Y range set to [" + minY + ".." + maxY + "]"
                                                    ), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /balloon setCount <perDay>
                        .then(CommandManager.literal("setCount")
                                .then(CommandManager.argument("perDay", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getWorld();
                                            int per = IntegerArgumentType.getInteger(ctx, "perDay");
                                            DailyCrateManager.setCount((ServerWorld) world, per);
                                            src.sendFeedback(() -> Text.literal(
                                                    "Balloons per day set to " + per
                                            ), false);
                                            return 1;
                                        })
                                )
                        )

                        // /balloon announce on|off
                        .then(CommandManager.literal("announce")
                                .then(CommandManager.literal("on").executes(ctx -> {
                                    var src = ctx.getSource();
                                    DailyCrateManager.setAnnouncements((ServerWorld) src.getWorld(), true);
                                    src.sendFeedback(() -> Text.literal("Balloon announcements: ON"), false);
                                    return 1;
                                }))
                                .then(CommandManager.literal("off").executes(ctx -> {
                                    var src = ctx.getSource();
                                    DailyCrateManager.setAnnouncements((ServerWorld) src.getWorld(), false);
                                    src.sendFeedback(() -> Text.literal("Balloon announcements: OFF"), false);
                                    return 1;
                                }))
                        )
        );

        // ===== /cache =====
        dispatcher.register(
                CommandManager.literal("cache")
                        .requires(src -> src.hasPermissionLevel(2))
                        // /cache spawn [radius]
                        .then(CommandManager.literal("spawn")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(8, 256))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            int r = IntegerArgumentType.getInteger(ctx, "radius");
                                            var world = p.getServerWorld();
                                            var placed = GoldenCacheManager.spawnNear(world, p.getBlockPos(), r);
                                            if (placed != null) {
                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("Spawned Golden Cache at " + placed.toShortString()),
                                                        false);
                                                return 1;
                                            } else {
                                                ctx.getSource().sendError(Text.literal("Failed to place cache."));
                                                return 0;
                                            }
                                        })
                                )
                                .executes(ctx -> {
                                    // default radius 64
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var world = p.getServerWorld();
                                    var placed = GoldenCacheManager.spawnNear(world, p.getBlockPos(), 64);
                                    if (placed != null) {
                                        ctx.getSource().sendFeedback(
                                                () -> Text.literal("Spawned Golden Cache at " + placed.toShortString()), false);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendError(Text.literal("Failed to place cache."));
                                        return 0;
                                    }
                                })
                        )
                        // /cache spawn_at <x> <y> <z>
                        .then(CommandManager.literal("spawn_at")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            var src = ctx.getSource();
                                                            var world = src.getWorld();
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            var placed = GoldenCacheManager.spawnAt(
                                                                    (ServerWorld) world, x, y, z);
                                                            if (placed != null) {
                                                                src.sendFeedback(
                                                                        () -> Text.literal(
                                                                                "Spawned Golden Cache at " + placed.toShortString()
                                                                        ), false);
                                                                return 1;
                                                            } else {
                                                                src.sendError(Text.literal("Failed to place cache."));
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                                )
                        )
                        // /cache announce <true|false>
                        .then(CommandManager.literal("announce")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            GoldenCacheManager.setAnnouncements(enabled);
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Golden Cache announcements: " +
                                                            (enabled ? "ON" : "OFF")), false);
                                            return 1;
                                        })
                                )
                        )
        );

    }
}
