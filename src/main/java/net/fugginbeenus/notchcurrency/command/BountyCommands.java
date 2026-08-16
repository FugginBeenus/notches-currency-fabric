package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.economy.bounty.Bounty;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyRarity;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyState;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class BountyCommands {

    private BountyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("bounty")
                .executes(ctx -> open(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> open(ctx.getSource())))
                .then(Commands.literal("take")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                    if (id == null) { ctx.getSource().sendFailure(Component.literal("Invalid bounty.")); return 0; }
                                    BountyManager.take(p, id);
                                    return 1;
                                })))
                .then(Commands.literal("claim")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            BountyManager.claim(p, null);
                            return 1;
                        })
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                    BountyManager.claim(p, id);
                                    return 1;
                                })))
                .then(Commands.literal("turnin")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                    if (id == null) { ctx.getSource().sendFailure(Component.literal("Invalid bounty.")); return 0; }
                                    BountyManager.turnIn(p, id);
                                    return 1;
                                })))

                .then(Commands.literal("admin")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            BountyManager.openAdminScreen(p);
                            return 1;
                        })
                        .then(Commands.literal("create")
                                .then(Commands.literal("kill")
                                        .then(Commands.argument("entity", ResourceLocationArgument.id())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("reward", LongArgumentType.longArg(0))
                                                                .executes(ctx -> create(ctx.getSource(), BountyType.KILL,
                                                                        ResourceLocationArgument.getId(ctx, "entity"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                        LongArgumentType.getLong(ctx, "reward"), true, ""))
                                                                .then(Commands.argument("repeatable", BoolArgumentType.bool())
                                                                        .executes(ctx -> create(ctx.getSource(), BountyType.KILL,
                                                                                ResourceLocationArgument.getId(ctx, "entity"),
                                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                                LongArgumentType.getLong(ctx, "reward"),
                                                                                BoolArgumentType.getBool(ctx, "repeatable"), ""))
                                                                        .then(Commands.argument("desc", StringArgumentType.greedyString())
                                                                                .executes(ctx -> create(ctx.getSource(), BountyType.KILL,
                                                                                        ResourceLocationArgument.getId(ctx, "entity"),
                                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                                        LongArgumentType.getLong(ctx, "reward"),
                                                                                        BoolArgumentType.getBool(ctx, "repeatable"),
                                                                                        StringArgumentType.getString(ctx, "desc")))))))))
                                .then(Commands.literal("fetch")
                                        .then(Commands.argument("item", ResourceLocationArgument.id())
                                                .then(Commands.argument("count", IntegerArgumentType.integer(1))
                                                        .then(Commands.argument("reward", LongArgumentType.longArg(0))
                                                                .executes(ctx -> create(ctx.getSource(), BountyType.FETCH,
                                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                        LongArgumentType.getLong(ctx, "reward"), true, ""))
                                                                .then(Commands.argument("repeatable", BoolArgumentType.bool())
                                                                        .executes(ctx -> create(ctx.getSource(), BountyType.FETCH,
                                                                                ResourceLocationArgument.getId(ctx, "item"),
                                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                                LongArgumentType.getLong(ctx, "reward"),
                                                                                BoolArgumentType.getBool(ctx, "repeatable"), ""))
                                                                        .then(Commands.argument("desc", StringArgumentType.greedyString())
                                                                                .executes(ctx -> create(ctx.getSource(), BountyType.FETCH,
                                                                                        ResourceLocationArgument.getId(ctx, "item"),
                                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                                        LongArgumentType.getLong(ctx, "reward"),
                                                                                        BoolArgumentType.getBool(ctx, "repeatable"),
                                                                                        StringArgumentType.getString(ctx, "desc"))))))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                            boolean removed = id != null && BountyState.get(ctx.getSource().getServer()).removeOffer(id);
                                            ctx.getSource().sendSuccess(() -> Component.literal(removed ? "Bounty removed." : "No such bounty.")
                                                    .withStyle(ChatFormatting.YELLOW), true);
                                            return removed ? 1 : 0;
                                        })))
                        .then(Commands.literal("list")
                                .executes(ctx -> adminList(ctx.getSource()))))
        );
    }

    private static int open(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        BountyManager.openScreen(p);
        return 1;
    }

    private static int create(CommandSourceStack src, BountyType type, ResourceLocation target,
                              int count, long reward, boolean repeatable, String desc) {
        boolean valid = type == BountyType.KILL ? BuiltInRegistries.ENTITY_TYPE.containsKey(target)
                : BuiltInRegistries.ITEM.containsKey(target);
        if (!valid) {
            src.sendFailure(Component.literal("Unknown " + (type == BountyType.KILL ? "entity" : "item") + ": " + target));
            return 0;
        }

        Bounty b = new Bounty(UUID.randomUUID(), type, target, count, reward, ItemStack.EMPTY,
                BountyRarity.COMMON, repeatable, 0L, desc);
        BountyState.get(src.getServer()).addOffer(b);
        src.sendSuccess(() -> Component.literal("Posted bounty: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(b.describe()).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" (" + reward + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + (repeatable ? ", repeatable" : "") + ").").withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }

    private static int adminList(CommandSourceStack src) {
        BountyState state = BountyState.get(src.getServer());
        if (state.allOffers().isEmpty()) {
            src.sendSuccess(() -> Component.literal("No bounties posted.").withStyle(ChatFormatting.GRAY), false);
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Bounties:").withStyle(ChatFormatting.GOLD), false);
        for (Bounty b : state.allOffers()) {
            MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(Component.literal(b.describe()).withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" (" + b.rewardSummary() + ") ").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("[X]").withStyle(ChatFormatting.RED).withStyle(s -> s
                            .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand("/bounty admin remove " + b.getId()))
                            .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Remove this bounty")))));
            src.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
