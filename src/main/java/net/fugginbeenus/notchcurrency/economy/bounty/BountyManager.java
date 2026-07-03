package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Runs the bounty board (Bountiful-style, take-first): auto-generates a rotating set of offers
 * from datapack {@link BountyPools}, lets players <b>take</b> offers (up to a limit) which start a
 * personal timer + kill tracking, then pays claims / item turn-ins back at the board (a coin+item
 * FAUCET). Admin-posted offers are permanent and coexist with the generated ones.
 */
public final class BountyManager {

    private static final Random RNG = new Random();
    private static final int REFRESH_CHECK_TICKS = 600; // expiry/top-up sweep every 30s

    private static boolean enabled = true;
    private static int activeCount = 5;
    private static int takeLimit = 3;
    private static long durationTicks = 30L * 60L * 20L; // 30 min
    private static int rewardMultPercent = 100;
    private static long maxCoinReward = 250;

    private static long tickAccum = 0;

    private BountyManager() {}

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getWorld().isClient()) return;
            if (source.getAttacker() instanceof ServerPlayerEntity player && player != entity) {
                onKill(player, Registries.ENTITY_TYPE.getId(entity.getType()));
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(BountyManager::sweepTick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Bounty b = cfg.bounty;
        enabled = b.enabled;
        activeCount = Math.max(0, b.activeCount);
        takeLimit = Math.max(1, b.takeLimit);
        durationTicks = Math.max(1L, (long) b.durationMinutes) * 60L * 20L;
        rewardMultPercent = Math.max(0, b.rewardMultiplierPercent);
        maxCoinReward = Math.max(0, b.maxCoinReward);
    }

    public static int getTakeLimit() {
        return takeLimit;
    }

    // ---- rotation + expired-taken cleanup ----

    private static void sweepTick(MinecraftServer server) {
        if (!enabled) return;
        if (++tickAccum < REFRESH_CHECK_TICKS) return;
        tickAccum = 0;

        ServerWorld overworld = server.getOverworld();
        if (overworld == null) return;
        long now = overworld.getTime();
        BountyState state = BountyState.get(server);

        // Rotate offers: drop expired generated ones, top up to activeCount.
        if (activeCount > 0) {
            List<UUID> expired = new ArrayList<>();
            int active = 0;
            for (Bounty b : state.allOffers()) {
                if (b.getExpiresGameTime() <= 0) continue; // permanent
                if (b.isExpired(now)) expired.add(b.getId());
                else active++;
            }
            for (UUID id : expired) state.removeOffer(id);
            java.util.Set<String> categories = state.activeCategories();
            for (int i = active; i < activeCount; i++) {
                Bounty b = BountyGenerator.generate(now, durationTicks, RNG, categories, rewardMultPercent, maxCoinReward);
                if (b == null) break;
                state.addOffer(b);
            }
        }

        // Drop expired taken bounties and tell online players.
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            int dropped = state.cleanupExpired(p.getUuid(), now);
            if (dropped > 0) {
                p.sendMessage(Text.literal(dropped + " of your bounties expired.").formatted(Formatting.GRAY), false);
            }
        }
    }

    // ---- taking ----

    public static void take(ServerPlayerEntity player, UUID offerId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        Bounty offer = state.getOffer(offerId);
        if (offer == null || offer.isExpired(now)) {
            player.sendMessage(Text.literal("That bounty is no longer available.").formatted(Formatting.RED), false);
            return;
        }
        if (state.hasTaken(player.getUuid(), offerId)) {
            player.sendMessage(Text.literal("You've already taken that bounty.").formatted(Formatting.RED), false);
            return;
        }
        if (state.takeCount(player.getUuid()) >= takeLimit) {
            player.sendMessage(Text.literal("You can only carry " + takeLimit + " bounties at once — finish or wait one out.")
                    .formatted(Formatting.RED), false);
            return;
        }

        long deadline = durationTicks <= 0 ? 0 : now + durationTicks;
        state.take(player.getUuid(), new TakenBounty(offer, deadline, 0));
        long mins = durationTicks / 20L / 60L;
        player.sendMessage(Text.literal("Took bounty: ").formatted(Formatting.GREEN)
                .append(Text.literal(offer.describe()).formatted(offer.getRarity().color()))
                .append(Text.literal(" — finish within " + mins + "m.").formatted(Formatting.GREEN)), false);
    }

    // ---- kill tracking (only toward taken bounties) ----

    private static void onKill(ServerPlayerEntity player, Identifier typeId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        for (TakenBounty tb : state.getTakenAll(player.getUuid())) {
            Bounty b = tb.bounty();
            if (b.getType() != BountyType.KILL || !b.getTarget().equals(typeId)) continue;
            if (tb.isExpired(now) || tb.progress() >= b.getRequired()) continue;

            int next = tb.addProgress(1);
            state.markDirty();
            if (next >= b.getRequired()) {
                player.sendMessage(Text.literal("✔ Bounty complete: ").formatted(Formatting.GREEN)
                        .append(Text.literal(b.describe()).formatted(b.getRarity().color()))
                        .append(Text.literal(" — collect it at the board!").formatted(Formatting.GREEN)), false);
            }
        }
    }

    // ---- collecting ----

    /** Collect every completed KILL bounty (or just {@code only}) from the player's taken list. */
    public static void claim(ServerPlayerEntity player, @Nullable UUID only) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        long totalCoins = 0L;
        int count = 0;
        for (TakenBounty tb : state.getTakenAll(player.getUuid())) {
            Bounty b = tb.bounty();
            if (b.getType() != BountyType.KILL) continue;
            if (only != null && !b.getId().equals(only)) continue;
            if (tb.isExpired(now) || tb.progress() < b.getRequired()) continue;

            giveReward(player, b);
            totalCoins += b.getRewardCoins();
            count++;
            state.removeTaken(player.getUuid(), b.getId());
            state.markOfferCompleted(player.getUuid(), b.getId()); // hide it from their board until it rotates
        }

        if (count > 0) {
            player.sendMessage(Text.literal("Collected " + count + " bount" + (count == 1 ? "y" : "ies") + "!")
                    .formatted(Formatting.GREEN)
                    .append(totalCoins > 0 ? Text.literal(" (+").formatted(Formatting.GREEN)
                            .append(NotchCurrency.coins(totalCoins)).append(Text.literal(")").formatted(Formatting.GREEN))
                            : Text.empty()), false);
        } else {
            player.sendMessage(Text.literal("Nothing ready to collect.").formatted(Formatting.GRAY), false);
        }
    }

    public static void turnIn(ServerPlayerEntity player, UUID offerId) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        TakenBounty tb = state.getTaken(player.getUuid(), offerId);
        if (tb == null || tb.isExpired(now) || tb.bounty().getType() != BountyType.FETCH) {
            player.sendMessage(Text.literal("That delivery isn't in your taken bounties.").formatted(Formatting.RED), false);
            return;
        }
        Bounty b = tb.bounty();
        Item item = Registries.ITEM.get(b.getTarget());
        int have = countItem(player, item);
        if (have < b.getRequired()) {
            player.sendMessage(Text.literal("You need " + b.getRequired() + " ").formatted(Formatting.RED)
                    .append(b.targetName().copy().formatted(Formatting.WHITE))
                    .append(Text.literal(" — you have " + have + ".").formatted(Formatting.RED)), false);
            return;
        }

        removeItem(player, item, b.getRequired());
        giveReward(player, b);
        state.removeTaken(player.getUuid(), offerId);
        state.markOfferCompleted(player.getUuid(), offerId); // hide it from their board until it rotates
        player.sendMessage(Text.literal("Delivered " + b.getRequired() + " ").formatted(Formatting.GREEN)
                .append(b.targetName().copy().formatted(Formatting.WHITE))
                .append(Text.literal(" — reward: " + b.rewardSummary() + "!").formatted(Formatting.GREEN)), false);
    }

    private static void giveReward(ServerPlayerEntity player, Bounty b) {
        if (b.getRewardCoins() > 0) {
            CurrencyApi.deposit(player, b.getRewardCoins(), TransactionReason.FAUCET, "bounty: " + b.describe());
        }
        if (!b.getRewardItem().isEmpty()) {
            player.getInventory().offerOrDrop(b.getRewardItem().copy());
        }
    }

    // ---- opening the board ----

    public static void openScreen(ServerPlayerEntity sp) {
        if (sp.getServer() != null) ensurePopulated(sp.getServer());
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new BountyBoardScreenHandler(syncId, inv),
                Text.literal("Bounty Board")));
    }

    /** Top the board up to the target count immediately (on board placement / first open). */
    public static void ensurePopulated(MinecraftServer server) {
        if (!enabled || activeCount <= 0) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);
        int active = 0;
        for (Bounty b : state.allOffers()) {
            if (b.getExpiresGameTime() > 0 && !b.isExpired(now)) active++;
        }
        if (active >= activeCount) return;
        java.util.Set<String> categories = state.activeCategories();
        for (int i = active; i < activeCount; i++) {
            Bounty b = BountyGenerator.generate(now, durationTicks, RNG, categories, rewardMultPercent, maxCoinReward);
            if (b == null) break;
            state.addOffer(b);
        }
    }

    public static void openAdminScreen(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new BountyAdminScreenHandler(syncId, inv),
                Text.literal("Bounty Setup")));
    }

    public static int getActiveCount() {
        return activeCount;
    }

    /** Clear all generated offers and immediately roll a fresh set (respecting decrees). */
    public static void regenerate(MinecraftServer server) {
        BountyState state = BountyState.get(server);
        long now = worldTime(server);
        List<UUID> gen = new ArrayList<>();
        for (Bounty b : state.allOffers()) {
            if (b.getExpiresGameTime() > 0) gen.add(b.getId());
        }
        for (UUID id : gen) state.removeOffer(id);

        java.util.Set<String> categories = state.activeCategories();
        for (int i = 0; i < activeCount; i++) {
            Bounty b = BountyGenerator.generate(now, durationTicks, RNG, categories, rewardMultPercent, maxCoinReward);
            if (b == null) break;
            state.addOffer(b);
        }
    }

    // ---- helpers ----

    static long worldTime(MinecraftServer server) {
        ServerWorld ow = server.getOverworld();
        return ow == null ? 0L : ow.getTime();
    }

    static boolean isEnabled() {
        return enabled;
    }

    private static int countItem(ServerPlayerEntity player, Item item) {
        int n = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) n += s.getCount();
        }
        return n;
    }

    private static void removeItem(ServerPlayerEntity player, Item item, int amount) {
        PlayerInventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(item)) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
            }
        }
    }
}
