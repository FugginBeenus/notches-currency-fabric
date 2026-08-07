package net.fugginbeenus.notchcurrency.economy.crate;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import java.util.Random;

public final class CrateManager {

    private static final Random RNG = new Random();

    private static boolean enabled = true;
    private static long keyPrice = 500L;

    private CrateManager() {}

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.crate.enabled;
        keyPrice = Math.max(0L, cfg.crate.keyPrice);
    }

    // ---- opening ----

    public static void open(ServerPlayer player, String crateType, ServerLevel world, BlockPos pos) {
        if (!enabled) {
            player.displayClientMessage(Component.literal("Crates are disabled.").withStyle(ChatFormatting.RED), false);
            return;
        }
        CrateDef def = CrateRegistry.get(crateType);
        if (def == null) {
            player.displayClientMessage(Component.literal("This crate isn't configured.").withStyle(ChatFormatting.RED), false);
            return;
        }
        int keys = countKeys(player);
        if (keys < def.keysRequired()) {
            player.displayClientMessage(Component.literal("You need " + def.keysRequired() + " key"
                    + (def.keysRequired() == 1 ? "" : "s") + " to open the " + def.name()
                    + " (you have " + keys + ").").withStyle(ChatFormatting.RED), false);
            showOdds(player, crateType);
            return;
        }

        CrateDef.LootEntry loot = CrateRegistry.roll(def, RNG);
        if (loot == null) {
            player.displayClientMessage(Component.literal("This crate is empty.").withStyle(ChatFormatting.GRAY), false);
            return;
        }
        // Guard BEFORE keys are consumed: a bad datapack entry must never pay out "Air".
        if (loot.isItem() && !BuiltInRegistries.ITEM.containsKey(loot.itemId())) {
            player.displayClientMessage(Component.literal("This crate is misconfigured (unknown item "
                    + loot.itemId() + ") - check the server log.").withStyle(ChatFormatting.RED), false);
            return;
        }
        consumeKeys(player, def.keysRequired());

        Component rewardText;
        if (loot.isItem()) {
            int n = randRange(loot.min(), loot.max());
            ItemStack reward = new ItemStack(BuiltInRegistries.ITEM.get(loot.itemId()), n);
            // Name BEFORE insertion: offerOrDrop empties the stack, and an empty stack names "Air".
            rewardText = Component.literal(n + "x ").append(reward.getHoverName().copy().withStyle(ChatFormatting.WHITE));
            player.getInventory().placeItemBackInInventory(reward);
        } else {
            CurrencyApi.deposit(player, loot.coins(), TransactionReason.FAUCET, "crate: " + def.name());
            rewardText = NotchCurrency.coins(loot.coins());
        }

        net.fugginbeenus.notchcurrency.block.CrateBlock.animateOpen(world, pos); // pop the lid
        effects(world, pos);
        player.displayClientMessage(Component.literal("🎁 You opened the " + def.name() + " and won ").withStyle(ChatFormatting.GOLD)
                .append(rewardText)
                .append(Component.literal("!").withStyle(ChatFormatting.GOLD)), false);
    }

    public static void showOdds(ServerPlayer player, String crateType) {
        CrateDef def = CrateRegistry.get(crateType);
        if (def == null) return;
        int total = def.totalWeight();
        player.displayClientMessage(Component.literal("═══ " + def.name() + " - Odds ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        player.displayClientMessage(Component.literal("Cost: " + def.keysRequired() + " key" + (def.keysRequired() == 1 ? "" : "s"))
                .withStyle(ChatFormatting.GRAY), false);
        for (CrateDef.LootEntry e : def.loot()) {
            String pct = total > 0 ? String.format("%.1f%%", 100.0 * e.weight() / total) : "0%";
            Component name;
            if (e.isItem()) {
                String range = e.min() == e.max() ? String.valueOf(e.min()) : e.min() + "-" + e.max();
                name = Component.literal(range + "x ").append(new ItemStack(BuiltInRegistries.ITEM.get(e.itemId())).getHoverName().copy().withStyle(ChatFormatting.WHITE));
            } else {
                name = Component.literal(e.coins() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()).withStyle(ChatFormatting.YELLOW);
            }
            player.displayClientMessage(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(name)
                    .append(Component.literal("  " + pct).withStyle(ChatFormatting.AQUA)), false);
        }
    }

    // ---- keys ----

    public static void buyKey(ServerPlayer player, int amount) {
        if (!enabled) {
            player.displayClientMessage(Component.literal("Crates are disabled.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (amount <= 0) return;
        long cost = keyPrice * amount;
        if (CurrencyApi.getBalance(player) < cost) {
            player.displayClientMessage(Component.literal("You can't afford " + amount + " key" + (amount == 1 ? "" : "s") + " (")
                    .withStyle(ChatFormatting.RED).append(NotchCurrency.coins(cost)).append(Component.literal(").").withStyle(ChatFormatting.RED)), false);
            return;
        }
        CurrencyApi.withdraw(player, cost, TransactionReason.SINK, "crate keys x" + amount);
        giveKeys(player, amount);
        player.displayClientMessage(Component.literal("Bought " + amount + " Crate Key" + (amount == 1 ? "" : "s") + " for ")
                .withStyle(ChatFormatting.GREEN).append(NotchCurrency.coins(cost)).append(Component.literal(".").withStyle(ChatFormatting.GREEN)), false);
    }

    public static void giveKeys(ServerPlayer player, int amount) {
        ItemStack keys = new ItemStack(ModItems.CRATE_KEY, amount);
        player.getInventory().placeItemBackInInventory(keys);
    }

    public static long getKeyPrice() {
        return keyPrice;
    }

    // ---- helpers ----

    private static int countKeys(ServerPlayer player) {
        int n = 0;
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(ModItems.CRATE_KEY)) n += inv.getItem(i).getCount();
        }
        return n;
    }

    private static void consumeKeys(ServerPlayer player, int amount) {
        Item key = ModItems.CRATE_KEY;
        Inventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(key)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    private static void effects(ServerLevel world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
        world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                24, 0.4, 0.4, 0.4, 0.1);
        world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                12, 0.3, 0.4, 0.3, 0.05);
    }

    private static int randRange(int min, int max) {
        return max <= min ? min : RNG.nextInt(max - min + 1) + min;
    }
}
