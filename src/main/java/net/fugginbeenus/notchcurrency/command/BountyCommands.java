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
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * {@code /bounty}: players view the board, claim kill rewards, and turn in deliveries. Admins
 * post/remove bounties under {@code /bounty admin}. The board itself ({@link BountyManager}) is
 * also reachable from a BOUNTY-role NPC.
 */
public final class BountyCommands {

    private BountyCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("bounty")
                .executes(ctx -> open(ctx.getSource()))
                .then(CommandManager.literal("list").executes(ctx -> open(ctx.getSource())))
                .then(CommandManager.literal("take")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                    if (id == null) { ctx.getSource().sendError(Text.literal("Invalid bounty.")); return 0; }
                                    BountyManager.take(p, id);
                                    return 1;
                                })))
                .then(CommandManager.literal("claim")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            BountyManager.claim(p, null);
                            return 1;
                        })
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                    BountyManager.claim(p, id);
                                    return 1;
                                })))
                .then(CommandManager.literal("turnin")
                        .then(CommandManager.argument("id", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                    if (id == null) { ctx.getSource().sendError(Text.literal("Invalid bounty.")); return 0; }
                                    BountyManager.turnIn(p, id);
                                    return 1;
                                })))

                // ---- admin ----
                .then(CommandManager.literal("admin")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            BountyManager.openAdminScreen(p);
                            return 1;
                        })
                        .then(CommandManager.literal("create")
                                .then(CommandManager.literal("kill")
                                        .then(CommandManager.argument("entity", IdentifierArgumentType.identifier())
                                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                                        .then(CommandManager.argument("reward", LongArgumentType.longArg(0))
                                                                .executes(ctx -> create(ctx.getSource(), BountyType.KILL,
                                                                        IdentifierArgumentType.getIdentifier(ctx, "entity"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                        LongArgumentType.getLong(ctx, "reward"), true, ""))
                                                                .then(CommandManager.argument("repeatable", BoolArgumentType.bool())
                                                                        .executes(ctx -> create(ctx.getSource(), BountyType.KILL,
                                                                                IdentifierArgumentType.getIdentifier(ctx, "entity"),
                                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                                LongArgumentType.getLong(ctx, "reward"),
                                                                                BoolArgumentType.getBool(ctx, "repeatable"), ""))
                                                                        .then(CommandManager.argument("desc", StringArgumentType.greedyString())
                                                                                .executes(ctx -> create(ctx.getSource(), BountyType.KILL,
                                                                                        IdentifierArgumentType.getIdentifier(ctx, "entity"),
                                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                                        LongArgumentType.getLong(ctx, "reward"),
                                                                                        BoolArgumentType.getBool(ctx, "repeatable"),
                                                                                        StringArgumentType.getString(ctx, "desc")))))))))
                                .then(CommandManager.literal("fetch")
                                        .then(CommandManager.argument("item", IdentifierArgumentType.identifier())
                                                .then(CommandManager.argument("count", IntegerArgumentType.integer(1))
                                                        .then(CommandManager.argument("reward", LongArgumentType.longArg(0))
                                                                .executes(ctx -> create(ctx.getSource(), BountyType.FETCH,
                                                                        IdentifierArgumentType.getIdentifier(ctx, "item"),
                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                        LongArgumentType.getLong(ctx, "reward"), true, ""))
                                                                .then(CommandManager.argument("repeatable", BoolArgumentType.bool())
                                                                        .executes(ctx -> create(ctx.getSource(), BountyType.FETCH,
                                                                                IdentifierArgumentType.getIdentifier(ctx, "item"),
                                                                                IntegerArgumentType.getInteger(ctx, "count"),
                                                                                LongArgumentType.getLong(ctx, "reward"),
                                                                                BoolArgumentType.getBool(ctx, "repeatable"), ""))
                                                                        .then(CommandManager.argument("desc", StringArgumentType.greedyString())
                                                                                .executes(ctx -> create(ctx.getSource(), BountyType.FETCH,
                                                                                        IdentifierArgumentType.getIdentifier(ctx, "item"),
                                                                                        IntegerArgumentType.getInteger(ctx, "count"),
                                                                                        LongArgumentType.getLong(ctx, "reward"),
                                                                                        BoolArgumentType.getBool(ctx, "repeatable"),
                                                                                        StringArgumentType.getString(ctx, "desc"))))))))))
                        .then(CommandManager.literal("remove")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            UUID id = parseUuid(StringArgumentType.getString(ctx, "id"));
                                            boolean removed = id != null && BountyState.get(ctx.getSource().getServer()).removeOffer(id);
                                            ctx.getSource().sendFeedback(() -> Text.literal(removed ? "Bounty removed." : "No such bounty.")
                                                    .formatted(Formatting.YELLOW), true);
                                            return removed ? 1 : 0;
                                        })))
                        .then(CommandManager.literal("list")
                                .executes(ctx -> adminList(ctx.getSource()))))
        );
    }

    private static int open(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player.")); return 0; }
        BountyManager.openScreen(p);
        return 1;
    }

    private static int create(ServerCommandSource src, BountyType type, Identifier target,
                              int count, long reward, boolean repeatable, String desc) {
        boolean valid = type == BountyType.KILL ? Registries.ENTITY_TYPE.containsId(target)
                : Registries.ITEM.containsId(target);
        if (!valid) {
            src.sendError(Text.literal("Unknown " + (type == BountyType.KILL ? "entity" : "item") + ": " + target));
            return 0;
        }
        // Manual admin bounties are permanent (no expiry), coins-only, common rarity.
        Bounty b = new Bounty(UUID.randomUUID(), type, target, count, reward, ItemStack.EMPTY,
                BountyRarity.COMMON, repeatable, 0L, desc);
        BountyState.get(src.getServer()).addOffer(b);
        src.sendFeedback(() -> Text.literal("Posted bounty: ").formatted(Formatting.GREEN)
                .append(Text.literal(b.describe()).formatted(Formatting.WHITE))
                .append(Text.literal(" (" + reward + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + (repeatable ? ", repeatable" : "") + ").").formatted(Formatting.GRAY)), true);
        return 1;
    }

    private static int adminList(ServerCommandSource src) {
        BountyState state = BountyState.get(src.getServer());
        if (state.allOffers().isEmpty()) {
            src.sendFeedback(() -> Text.literal("No bounties posted.").formatted(Formatting.GRAY), false);
            return 0;
        }
        src.sendFeedback(() -> Text.literal("Bounties:").formatted(Formatting.GOLD), false);
        for (Bounty b : state.allOffers()) {
            MutableText line = Text.literal(" • ").formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(b.describe()).formatted(Formatting.WHITE))
                    .append(Text.literal(" (" + b.rewardSummary() + ") ").formatted(Formatting.GRAY))
                    .append(Text.literal("[X]").formatted(Formatting.RED).styled(s -> s
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/bounty admin remove " + b.getId()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Remove this bounty")))));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
