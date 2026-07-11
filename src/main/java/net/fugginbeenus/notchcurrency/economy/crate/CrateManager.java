package net.fugginbeenus.notchcurrency.economy.crate;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Random;

/**
 * Runs crates: opening a crate consumes coin-bought keys and rolls a weighted reward (items or
 * a coin FAUCET) from the datapack loot table, with transparent odds. Buying keys is a coin SINK.
 */
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

    public static void open(ServerPlayerEntity player, String crateType, ServerWorld world, BlockPos pos) {
        if (!enabled) {
            player.sendMessage(Text.literal("Crates are disabled.").formatted(Formatting.RED), false);
            return;
        }
        CrateDef def = CrateRegistry.get(crateType);
        if (def == null) {
            player.sendMessage(Text.literal("This crate isn't configured.").formatted(Formatting.RED), false);
            return;
        }
        int keys = countKeys(player);
        if (keys < def.keysRequired()) {
            player.sendMessage(Text.literal("You need " + def.keysRequired() + " key"
                    + (def.keysRequired() == 1 ? "" : "s") + " to open the " + def.name()
                    + " (you have " + keys + ").").formatted(Formatting.RED), false);
            showOdds(player, crateType);
            return;
        }

        CrateDef.LootEntry loot = CrateRegistry.roll(def, RNG);
        if (loot == null) {
            player.sendMessage(Text.literal("This crate is empty.").formatted(Formatting.GRAY), false);
            return;
        }
        // Guard BEFORE keys are consumed: a bad datapack entry must never pay out "Air".
        if (loot.isItem() && !Registries.ITEM.containsId(loot.itemId())) {
            player.sendMessage(Text.literal("This crate is misconfigured (unknown item "
                    + loot.itemId() + ") — check the server log.").formatted(Formatting.RED), false);
            return;
        }
        consumeKeys(player, def.keysRequired());

        Text rewardText;
        if (loot.isItem()) {
            int n = randRange(loot.min(), loot.max());
            ItemStack reward = new ItemStack(Registries.ITEM.get(loot.itemId()), n);
            // Name BEFORE insertion — offerOrDrop empties the stack, and an empty stack names "Air".
            rewardText = Text.literal(n + "x ").append(reward.getName().copy().formatted(Formatting.WHITE));
            player.getInventory().offerOrDrop(reward);
        } else {
            CurrencyApi.deposit(player, loot.coins(), TransactionReason.FAUCET, "crate: " + def.name());
            rewardText = NotchCurrency.coins(loot.coins());
        }

        net.fugginbeenus.notchcurrency.block.CrateBlock.animateOpen(world, pos); // pop the lid
        effects(world, pos);
        player.sendMessage(Text.literal("🎁 You opened the " + def.name() + " and won ").formatted(Formatting.GOLD)
                .append(rewardText)
                .append(Text.literal("!").formatted(Formatting.GOLD)), false);
    }

    public static void showOdds(ServerPlayerEntity player, String crateType) {
        CrateDef def = CrateRegistry.get(crateType);
        if (def == null) return;
        int total = def.totalWeight();
        player.sendMessage(Text.literal("═══ " + def.name() + " — Odds ═══").formatted(Formatting.GOLD, Formatting.BOLD), false);
        player.sendMessage(Text.literal("Cost: " + def.keysRequired() + " key" + (def.keysRequired() == 1 ? "" : "s"))
                .formatted(Formatting.GRAY), false);
        for (CrateDef.LootEntry e : def.loot()) {
            String pct = total > 0 ? String.format("%.1f%%", 100.0 * e.weight() / total) : "0%";
            Text name;
            if (e.isItem()) {
                String range = e.min() == e.max() ? String.valueOf(e.min()) : e.min() + "-" + e.max();
                name = Text.literal(range + "x ").append(new ItemStack(Registries.ITEM.get(e.itemId())).getName().copy().formatted(Formatting.WHITE));
            } else {
                name = Text.literal(e.coins() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()).formatted(Formatting.YELLOW);
            }
            player.sendMessage(Text.literal(" • ").formatted(Formatting.DARK_GRAY)
                    .append(name)
                    .append(Text.literal("  " + pct).formatted(Formatting.AQUA)), false);
        }
    }

    // ---- keys ----

    public static void buyKey(ServerPlayerEntity player, int amount) {
        if (!enabled) {
            player.sendMessage(Text.literal("Crates are disabled.").formatted(Formatting.RED), false);
            return;
        }
        if (amount <= 0) return;
        long cost = keyPrice * amount;
        if (CurrencyApi.getBalance(player) < cost) {
            player.sendMessage(Text.literal("You can't afford " + amount + " key" + (amount == 1 ? "" : "s") + " (")
                    .formatted(Formatting.RED).append(NotchCurrency.coins(cost)).append(Text.literal(").").formatted(Formatting.RED)), false);
            return;
        }
        CurrencyApi.withdraw(player, cost, TransactionReason.SINK, "crate keys x" + amount);
        giveKeys(player, amount);
        player.sendMessage(Text.literal("Bought " + amount + " Crate Key" + (amount == 1 ? "" : "s") + " for ")
                .formatted(Formatting.GREEN).append(NotchCurrency.coins(cost)).append(Text.literal(".").formatted(Formatting.GREEN)), false);
    }

    public static void giveKeys(ServerPlayerEntity player, int amount) {
        ItemStack keys = new ItemStack(ModItems.CRATE_KEY, amount);
        player.getInventory().offerOrDrop(keys);
    }

    public static long getKeyPrice() {
        return keyPrice;
    }

    // ---- helpers ----

    private static int countKeys(ServerPlayerEntity player) {
        int n = 0;
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            if (inv.getStack(i).isOf(ModItems.CRATE_KEY)) n += inv.getStack(i).getCount();
        }
        return n;
    }

    private static void consumeKeys(ServerPlayerEntity player, int amount) {
        Item key = ModItems.CRATE_KEY;
        PlayerInventory inv = player.getInventory();
        int remaining = amount;
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (s.isOf(key)) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
            }
        }
    }

    private static void effects(ServerWorld world, BlockPos pos) {
        world.playSound(null, pos, SoundEvents.BLOCK_ENDER_CHEST_OPEN, SoundCategory.BLOCKS, 1.0f, 1.2f);
        world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.6f, 1.4f);
        world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                24, 0.4, 0.4, 0.4, 0.1);
        world.spawnParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                12, 0.3, 0.4, 0.3, 0.05);
    }

    private static int randRange(int min, int max) {
        return max <= min ? min : RNG.nextInt(max - min + 1) + min;
    }
}
