package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public final class BountyManager {

    private static final Random RNG = new Random();
    private static final int REFRESH_CHECK_TICKS = 600;

    private static boolean enabled = true;
    private static int activeCount = 5;
    private static int takeLimit = 3;
    private static long durationTicks = 30L * 60L * 20L;
    private static int rewardMultPercent = 100;
    private static long maxCoinReward = 250;

    private static long tickAccum = 0;

    private BountyManager() {}

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide()) return;
            if (source.getEntity() instanceof ServerPlayer player && player != entity) {
                onKill(player, BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()));
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

    private static void sweepTick(MinecraftServer server) {
        if (!enabled) return;
        if (++tickAccum < REFRESH_CHECK_TICKS) return;
        tickAccum = 0;

        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        long now = overworld.getGameTime();
        BountyState state = BountyState.get(server);

        if (activeCount > 0) {
            List<UUID> expired = new ArrayList<>();
            int active = 0;
            for (Bounty b : state.allOffers()) {
                if (b.getExpiresGameTime() <= 0) continue;
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

        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            int dropped = state.cleanupExpired(p.getUUID(), now);
            if (dropped > 0) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(dropped + " of your bounties expired.").withStyle(ChatFormatting.GRAY));
            }
        }
    }

    public static void take(ServerPlayer player, UUID offerId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        Bounty offer = state.getOffer(offerId);
        if (offer == null || offer.isExpired(now)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("That bounty is no longer available.").withStyle(ChatFormatting.RED));
            return;
        }
        if (state.hasTaken(player.getUUID(), offerId)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You've already taken that bounty.").withStyle(ChatFormatting.RED));
            return;
        }
        if (state.takeCount(player.getUUID()) >= takeLimit) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You can only carry " + takeLimit + " bounties at once - finish or wait one out.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long deadline = durationTicks <= 0 ? 0 : now + durationTicks;
        state.take(player.getUUID(), new TakenBounty(offer, deadline, 0));
        long mins = durationTicks / 20L / 60L;
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Took bounty: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(offer.describe()).withStyle(offer.getRarity().color()))
                .append(Component.literal(" - finish within " + mins + "m.").withStyle(ChatFormatting.GREEN)));
        syncTracker(player);
    }

    private static void onKill(ServerPlayer player, ResourceLocation typeId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        for (TakenBounty tb : state.getTakenAll(player.getUUID())) {
            Bounty b = tb.bounty();
            if (b.getType() != BountyType.KILL || !b.getTarget().equals(typeId)) continue;
            if (tb.isExpired(now) || tb.progress() >= b.getRequired()) continue;

            int next = tb.addProgress(1);
            state.setDirty();
            syncTracker(player);
            if (next >= b.getRequired()) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("✔ Bounty complete: ").withStyle(ChatFormatting.GREEN)
                        .append(Component.literal(b.describe()).withStyle(b.getRarity().color()))
                        .append(Component.literal(" - collect it at the board!").withStyle(ChatFormatting.GREEN)));
            }
        }
    }

    public static void claim(ServerPlayer player, @Nullable UUID only) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        long totalCoins = 0L;
        int count = 0;
        for (TakenBounty tb : state.getTakenAll(player.getUUID())) {
            Bounty b = tb.bounty();
            if (b.getType() != BountyType.KILL) continue;
            if (only != null && !b.getId().equals(only)) continue;
            if (tb.isExpired(now) || tb.progress() < b.getRequired()) continue;

            giveReward(player, b);
            totalCoins += b.getRewardCoins();
            count++;
            state.removeTaken(player.getUUID(), b.getId());
            state.markOfferCompleted(player.getUUID(), b.getId());
        }

        if (count > 0) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Collected " + count + " bount" + (count == 1 ? "y" : "ies") + "!")
                    .withStyle(ChatFormatting.GREEN)
                    .append(totalCoins > 0 ? Component.literal(" (+").withStyle(ChatFormatting.GREEN)
                            .append(NotchCurrency.coins(totalCoins)).append(Component.literal(")").withStyle(ChatFormatting.GREEN))
                            : Component.empty()));
            syncTracker(player);
        } else {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Nothing ready to collect.").withStyle(ChatFormatting.GRAY));
        }
    }

    public static void turnIn(ServerPlayer player, UUID offerId) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        TakenBounty tb = state.getTaken(player.getUUID(), offerId);
        if (tb == null || tb.isExpired(now) || tb.bounty().getType() != BountyType.FETCH) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("That delivery isn't in your taken bounties.").withStyle(ChatFormatting.RED));
            return;
        }
        Bounty b = tb.bounty();
        Item item = BuiltInRegistries.ITEM.get(b.getTarget());
        int have = countItem(player, item);
        if (have < b.getRequired()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You need " + b.getRequired() + " ").withStyle(ChatFormatting.RED)
                    .append(b.targetName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" - you have " + have + ".").withStyle(ChatFormatting.RED)));
            return;
        }

        removeItem(player, item, b.getRequired());
        giveReward(player, b);
        state.removeTaken(player.getUUID(), offerId);
        state.markOfferCompleted(player.getUUID(), offerId);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Delivered " + b.getRequired() + " ").withStyle(ChatFormatting.GREEN)
                .append(b.targetName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(" - reward: " + b.rewardSummary() + "!").withStyle(ChatFormatting.GREEN)));
        syncTracker(player);
    }

    public static void syncTracker(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        var taken = state.getTakenAll(player.getUUID());
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        int count = 0;
        for (TakenBounty tb : taken) {
            if (!tb.isExpired(now)) count++;
        }
        buf.writeVarInt(count);
        for (TakenBounty tb : taken) {
            if (tb.isExpired(now)) continue;
            Bounty b = tb.bounty();
            buf.writeUtf(b.describe());
            buf.writeBoolean(b.getType() == BountyType.KILL);
            buf.writeUtf(b.getType() == BountyType.FETCH ? b.getTarget().toString() : "");
            buf.writeVarInt(tb.progress());
            buf.writeVarInt(b.getRequired());
            buf.writeLong(tb.expiresGameTime());
            buf.writeUtf(b.getRarity().name());
        }
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(player,
                net.fugginbeenus.notchcurrency.net.NotchPackets.BOUNTY_TRACKER, buf);
    }

    private static void giveReward(ServerPlayer player, Bounty b) {
        if (b.getRewardCoins() > 0) {
            CurrencyApi.deposit(player, b.getRewardCoins(), TransactionReason.FAUCET, "bounty: " + b.describe());
        }
        if (!b.getRewardItem().isEmpty()) {
            player.getInventory().placeItemBackInInventory(b.getRewardItem().copy());
        }
    }

    public static void openScreen(ServerPlayer sp) {
        if (sp.level().getServer() != null) ensurePopulated(sp.level().getServer());
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new BountyBoardScreenHandler(containerId, inv),
                Component.literal("Bounty Board")));
    }

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

    public static void openAdminScreen(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new BountyAdminScreenHandler(containerId, inv),
                Component.literal("Bounty Setup")));
    }

    public static int getActiveCount() {
        return activeCount;
    }

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

    static long worldTime(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        return ow == null ? 0L : ow.getGameTime();
    }

    static boolean isEnabled() {
        return enabled;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static void removeItem(ServerPlayer player, Item item, int amount) {
        Inventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }
}
