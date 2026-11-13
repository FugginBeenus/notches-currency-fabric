package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.compat.Give;
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
    private static boolean playerBounties = false;
    private static int postLimit = 3;
    private static long postTicks = 24L * 72000L;
    private static boolean pvpBounties = false;

    private static long tickAccum = 0;

    private BountyManager() {}

    public static void init() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.level().isClientSide()) return;
            if (source.getEntity() instanceof ServerPlayer player && player != entity) {
                onKill(player, entity);
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(BountyManager::sweepTick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Bounty b = cfg.bounty;
        enabled = b.enabled;
        activeCount = Math.max(0, b.activeCount);
        takeLimit = Math.max(1, Math.min(BountyBoardScreenHandler.TAKEN_SLOTS, b.takeLimit));
        durationTicks = Math.max(1L, (long) b.durationMinutes) * 60L * 20L;
        rewardMultPercent = Math.max(0, b.rewardMultiplierPercent);
        maxCoinReward = Math.max(0, b.maxCoinReward);
        playerBounties = b.playerBounties;
        postLimit = Math.max(1, b.playerPostLimit);
        postTicks = Math.max(1L, (long) b.playerPostHours) * 72000L;
        pvpBounties = b.pvpBounties;
    }

    public static int getTakeLimit() {
        return takeLimit;
    }

    private static void sweepTick(MinecraftServer server) {
        if (server.getTickCount() % 20 == 0) QuestManager.tickVisits(server);
        if (++tickAccum < REFRESH_CHECK_TICKS) return;
        tickAccum = 0;

        ServerLevel overworld = server.overworld();
        if (overworld == null) return;
        long now = overworld.getGameTime();
        BountyState state = BountyState.get(server);
        sweepPosted(server, state, now);
        if (!enabled) return;

        if (activeCount > 0) {
            List<UUID> expired = new ArrayList<>();
            int active = 0;
            for (Bounty b : state.allOffers()) {
                if (b.getExpiresGameTime() <= 0 || b.isPosted()) continue;
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
        if (offer.isPosted() && player.getUUID().equals(offer.getPoster())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You can't take your own bounty.").withStyle(ChatFormatting.RED));
            return;
        }
        if (player.getUUID().equals(offer.getTargetPlayer())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You can't take the bounty on your own head.").withStyle(ChatFormatting.RED));
            return;
        }
        if (offer.isPosted() && state.reservedBy(offerId) != null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Someone else is already working on that one.").withStyle(ChatFormatting.RED));
            return;
        }
        if (state.takeCount(player.getUUID()) >= takeLimit) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You can only carry " + takeLimit + " bounties at once - finish or wait one out.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        long deadline = durationTicks <= 0 ? 0 : now + durationTicks;
        state.take(player.getUUID(), new TakenBounty(offer, deadline, 0));
        if (offer.isPosted()) state.reserve(offerId, player.getUUID());
        long mins = durationTicks / 20L / 60L;
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Took bounty: ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(offer.describe()).withStyle(offer.getRarity().color()))
                .append(Component.literal(" - finish within " + mins + "m.").withStyle(ChatFormatting.GREEN)));
        syncTracker(player);
    }

    private static void onKill(ServerPlayer player, net.minecraft.world.entity.LivingEntity victim) {
        ResourceLocation typeId = BuiltInRegistries.ENTITY_TYPE.getKey(victim.getType());
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        long now = worldTime(server);

        for (TakenBounty tb : state.getTakenAll(player.getUUID())) {
            Bounty b = tb.bounty();
            if (b.getType() != BountyType.KILL) continue;
            if (b.getTargetPlayer() != null ? !b.getTargetPlayer().equals(victim.getUUID()) : !b.getTarget().equals(typeId)) continue;
            if (tb.isExpired(now) || tb.progress() >= b.getRequired()) continue;

            int next = tb.addProgress(1);
            state.setDirty();
            syncTracker(player);
            if (b.isQuest()) QuestManager.shareProgress(player, b, "Killed together");
            if (next >= b.getRequired()) {
                if (b.isQuest()) {
                    QuestManager.settle(player, b, tb);
                } else {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("✔ Bounty complete: ").withStyle(ChatFormatting.GREEN)
                            .append(Component.literal(b.describe()).withStyle(b.getRarity().color()))
                            .append(Component.literal(" - collect it at the board!").withStyle(ChatFormatting.GREEN)));
                }
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
            if (b.isPosted()) finishPosted(server, state, player, b, List.of());
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

        List<ItemStack> handed = List.of();
        if (b.isPosted()) {
            handed = takeItems(player, item, b.getRequired());
            if (server.getPlayerList().getPlayer(b.getPoster()) == null && !net.fugginbeenus.notchcurrency.mail.MailManager.post(
                    server, b.getPoster(), net.fugginbeenus.notchcurrency.mail.MailItem.parcel("Bounty Board",
                            player.getName().getString() + " filled your bounty: " + b.describe(), handed))) {
                for (ItemStack st : handed) giveOrDrop(player, st);
                net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("The poster's mailbox is full. Try again later.").withStyle(ChatFormatting.RED));
                return;
            }
        } else {
            removeItem(player, item, b.getRequired());
        }
        giveReward(player, b);
        state.removeTaken(player.getUUID(), offerId);
        state.markOfferCompleted(player.getUUID(), offerId);
        if (b.isPosted()) finishPosted(server, state, player, b, handed);
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
            int shown = tb.progress();
            if (b.getType() == BountyType.FETCH || b.getType() == BountyType.DELIVER) {
                shown = Math.min(b.getRequired(),
                        countItem(player, BuiltInRegistries.ITEM.get(b.getTarget())));
            }
            buf.writeUtf(b.describe());
            buf.writeBoolean(b.getType() == BountyType.KILL);
            buf.writeUtf(b.getType() == BountyType.FETCH ? b.getTarget().toString() : "");
            buf.writeVarInt(shown);
            buf.writeVarInt(b.getRequired());
            buf.writeLong(tb.expiresGameTime());
            buf.writeUtf(b.getRarity().name());
            buf.writeBoolean(b.isQuest());
            buf.writeUtf(b.isQuest() ? b.getQuestKey() : "");
            buf.writeBoolean(b.isQuest() && b.needsHandIn());
            buf.writeUtf(tb.giver());
        }
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(player,
                net.fugginbeenus.notchcurrency.net.NotchPackets.BOUNTY_TRACKER, buf);
    }

    static void giveReward(ServerPlayer player, Bounty b) {
        if (b.getRewardCoins() > 0) {
            CurrencyApi.deposit(player, b.getRewardCoins(),
                    b.isPosted() ? TransactionReason.BOUNTY : TransactionReason.FAUCET, "bounty: " + b.describe());
        }
        if (!b.getRewardItem().isEmpty()) {
            Give.placeBack(player, b.getRewardItem().copy());
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
            if (b.getExpiresGameTime() > 0 && !b.isPosted() && !b.isExpired(now)) active++;
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
            if (b.getExpiresGameTime() > 0 && !b.isPosted()) gen.add(b.getId());
        }
        for (UUID id : gen) state.removeOffer(id);

        java.util.Set<String> categories = state.activeCategories();
        for (int i = 0; i < activeCount; i++) {
            Bounty b = BountyGenerator.generate(now, durationTicks, RNG, categories, rewardMultPercent, maxCoinReward);
            if (b == null) break;
            state.addOffer(b);
        }
    }

    public static boolean canPost(ServerPlayer sp) {
        return playerBounties && net.fugginbeenus.notchcurrency.compat.Perms.has(sp, "notchcurrency.bounty.post", true);
    }

    public static void openPost(ServerPlayer sp) {
        if (!canPost(sp)) return;
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new BountyPostScreenHandler(containerId, inv),
                Component.literal("Post a Bounty")));
    }

    public static boolean pvpAllowed(ServerPlayer sp) {
        return pvpBounties && canPost(sp);
    }

    public static boolean isHuntable(ResourceLocation id) {
        return id != null && BuiltInRegistries.ENTITY_TYPE.containsKey(id)
                && BuiltInRegistries.ENTITY_TYPE.get(id).getCategory() == net.minecraft.world.entity.MobCategory.MONSTER;
    }

    public static void post(ServerPlayer sp, BountyPostScreenHandler menu, int mode, String wanted, int amount, long coins) {
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        if (!canPost(sp)) {
            refuse(sp, "Player bounties are turned off.");
            return;
        }
        BountyState state = BountyState.get(server);
        if (state.postedCount(sp.getUUID()) >= postLimit) {
            refuse(sp, "You already have " + postLimit + " bounties up.");
            return;
        }
        ServerPlayer prey = null;
        if (mode == 2) {
            if (!pvpAllowed(sp)) {
                refuse(sp, "Bounties on players are turned off.");
                return;
            }
            prey = server.getPlayerList().getPlayerByName(wanted == null ? "" : wanted.trim());
            if (prey == null) {
                refuse(sp, "You can only put a bounty on a player who is online.");
                return;
            }
            if (prey == sp) {
                refuse(sp, "You can't put a bounty on yourself.");
                return;
            }
            amount = 1;
        }
        if (amount < 1 || amount > 999) {
            refuse(sp, "Pick an amount from 1 to 999.");
            return;
        }
        BountyType type = mode == 1 ? BountyType.FETCH : BountyType.KILL;
        ResourceLocation target;
        if (prey != null) {
            target = net.fugginbeenus.notchcurrency.compat.Reg.id("minecraft", "player");
        } else if (type == BountyType.KILL) {
            target = ResourceLocation.tryParse(wanted);
            if (!isHuntable(target)) {
                refuse(sp, "Pick a monster to hunt.");
                return;
            }
        } else {
            if (menu.sample().isEmpty()) {
                refuse(sp, "Put one of the item you want in the slot.");
                return;
            }
            target = BuiltInRegistries.ITEM.getKey(menu.sample().getItem());
        }
        long pay = Math.max(0L, coins);
        ItemStack reward = menu.reward().copy();
        if (pay <= 0 && reward.isEmpty()) {
            refuse(sp, "Add a reward: " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ", an item, or both.");
            return;
        }
        if (pay > 0 && net.fugginbeenus.notchcurrency.core.BalanceStore.get(sp) < pay) {
            refuse(sp, "You don't have " + pay + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".");
            return;
        }
        if (pay > 0) {
            net.fugginbeenus.notchcurrency.core.BalanceStore.subtract(sp, pay, TransactionReason.BOUNTY, "bounty posted");
            net.fugginbeenus.notchcurrency.net.NotchPackets.sendBalance(sp, net.fugginbeenus.notchcurrency.core.BalanceStore.get(sp));
        }
        menu.clearReward();
        Bounty b = new Bounty(UUID.randomUUID(), type, target, amount, pay, reward, BountyRarity.COMMON, false,
                worldTime(server) + postTicks, "").postedBy(sp.getUUID(), sp.getName().getString());
        if (prey != null) b.onPlayer(prey.getUUID(), prey.getName().getString());
        state.addOffer(b);
        if (prey != null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(prey, Component.literal(sp.getName().getString()
                    + " put a bounty on your head: " + b.rewardSummary() + ".").withStyle(ChatFormatting.RED));
        }
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Bounty posted: " + b.describe()
                + ". Your reward is held until someone finishes it.").withStyle(ChatFormatting.GREEN));
        openScreen(sp);
    }

    public static void cancelPost(ServerPlayer sp, UUID offerId) {
        MinecraftServer server = sp.level().getServer();
        if (server == null) return;
        BountyState state = BountyState.get(server);
        Bounty b = state.getOffer(offerId);
        if (b == null || !sp.getUUID().equals(b.getPoster())) {
            refuse(sp, "That isn't one of your bounties.");
            return;
        }
        if (state.reservedBy(offerId) != null) {
            refuse(sp, "Someone is working on it, so it can't come down now.");
            return;
        }
        refund(sp, b);
        state.removeOffer(offerId);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Bounty taken down. Your reward came back.")
                .withStyle(ChatFormatting.GREEN));
    }

    private static void sweepPosted(MinecraftServer server, BountyState state, long now) {
        List<Bounty> over = new ArrayList<>();
        for (Bounty b : state.allOffers()) {
            if (b.isPosted() && b.isExpired(now) && state.reservedBy(b.getId()) == null) over.add(b);
        }
        for (Bounty b : over) {
            ServerPlayer poster = server.getPlayerList().getPlayer(b.getPoster());
            if (poster != null) {
                refund(poster, b);
                net.fugginbeenus.notchcurrency.compat.Msg.chat(poster, Component.literal("Nobody finished your bounty, so its reward came back: "
                        + b.describe() + ".").withStyle(ChatFormatting.GRAY));
            } else {
                String note = "Nobody finished: " + b.describe();
                net.fugginbeenus.notchcurrency.mail.MailItem mail = b.getRewardItem().isEmpty()
                        ? net.fugginbeenus.notchcurrency.mail.MailItem.payout("Bounty Board", note, b.getRewardCoins())
                        : net.fugginbeenus.notchcurrency.mail.MailItem.parcel("Bounty Board", note,
                                List.of(b.getRewardItem().copy()), b.getRewardCoins());
                if (!net.fugginbeenus.notchcurrency.mail.MailManager.post(server, b.getPoster(), mail)) continue;
            }
            state.removeOffer(b.getId());
        }
    }

    private static void finishPosted(MinecraftServer server, BountyState state, ServerPlayer taker, Bounty b, List<ItemStack> items) {
        state.removeOffer(b.getId());
        ServerPlayer poster = server.getPlayerList().getPlayer(b.getPoster());
        if (poster == null) return;
        for (ItemStack st : items) giveOrDrop(poster, st);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(poster, Component.literal(taker.getName().getString()
                + " finished your bounty: " + b.describe() + ".").withStyle(ChatFormatting.GREEN));
    }

    private static void refund(ServerPlayer sp, Bounty b) {
        if (b.getRewardCoins() > 0) {
            CurrencyApi.deposit(sp, b.getRewardCoins(), TransactionReason.BOUNTY, "bounty refund");
            net.fugginbeenus.notchcurrency.net.NotchPackets.sendBalance(sp, net.fugginbeenus.notchcurrency.core.BalanceStore.get(sp));
        }
        if (!b.getRewardItem().isEmpty()) giveOrDrop(sp, b.getRewardItem().copy());
    }

    private static void refuse(ServerPlayer sp, String text) {
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(text).withStyle(ChatFormatting.RED));
    }

    static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack chunk = stack.split(Math.min(stack.getCount(), stack.getMaxStackSize()));
            if (!player.getInventory().add(chunk) && !chunk.isEmpty()) Give.drop(player, chunk);
        }
    }

    static List<ItemStack> takeItems(ServerPlayer player, Item item, int amount) {
        List<ItemStack> out = new ArrayList<>();
        Inventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) {
                out.add(s.split(Math.min(remaining, s.getCount())));
                remaining -= out.get(out.size() - 1).getCount();
            }
        }
        return out;
    }

    static long worldTime(MinecraftServer server) {
        ServerLevel ow = server.overworld();
        return ow == null ? 0L : ow.getGameTime();
    }

    static boolean isEnabled() {
        return enabled;
    }

    static int countItem(ServerPlayer player, Item item) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    static void removeItem(ServerPlayer player, Item item, int amount) {
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
