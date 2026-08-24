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
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public final class CrateManager {

    private static final Random RNG = new Random();

    private static boolean enabled = true;
    private static long keyPrice = 500L;

    private CrateManager() {}

    private static final int[] RATTLE_TICKS = {0};
    private static final float[] THUD_PITCH = {0.52f};
    private static final float[] THUD_KNOCK_PITCH = {0.50f};
    private static final float[] THUD_VOLUME = {1.0f};
    private static final int LID_OPENS_TICK = 23;
    private static final int REWARD_TICK = 27;
    private static final int LID_SHUTS_TICK = 108;

    private static final int STEP_LID_OPENS = 100;
    private static final int STEP_REWARD = 101;
    private static final int STEP_LID_SHUTS = 102;

    private record Cue(ServerLevel world, BlockPos pos, long at, int step) {}

    private static final List<Cue> cues = new ArrayList<>();

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CrateManager::tick);
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
                stopped -> cues.clear());
    }

    private static void tick(MinecraftServer server) {
        if (cues.isEmpty()) return;
        Iterator<Cue> it = cues.iterator();
        while (it.hasNext()) {
            Cue c = it.next();
            if (c.world().getGameTime() < c.at()) continue;
            fire(c.world(), c.pos(), c.step());
            it.remove();
        }
    }

    private static void fire(ServerLevel world, BlockPos pos, int step) {
        if (step == STEP_LID_OPENS) {
            world.playSound(null, pos, SoundEvents.ENDER_CHEST_OPEN, SoundSource.BLOCKS, 1.0f, 1.2f);
        } else if (step == STEP_REWARD) {
            world.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.6f, 1.4f);
            world.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    24, 0.4, 0.4, 0.4, 0.1);
            world.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5,
                    12, 0.3, 0.4, 0.3, 0.05);
        } else if (step == STEP_LID_SHUTS) {
            world.playSound(null, pos, SoundEvents.ENDER_CHEST_CLOSE, SoundSource.BLOCKS, 0.9f, 1.1f);
        } else {
            int i = Math.max(0, Math.min(step, THUD_PITCH.length - 1));
            world.playSound(null, pos, SoundEvents.BARREL_CLOSE, SoundSource.BLOCKS,
                    THUD_VOLUME[i], THUD_PITCH[i]);
            world.playSound(null, pos, SoundEvents.WOOD_HIT, SoundSource.BLOCKS,
                    THUD_VOLUME[i] * 0.8f, THUD_KNOCK_PITCH[i]);
        }
    }

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.crate.enabled;
        keyPrice = Math.max(0L, cfg.crate.keyPrice);
    }

    public static void open(ServerPlayer player, String crateType, ServerLevel world, BlockPos pos) {
        if (!enabled) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Crates are disabled.").withStyle(ChatFormatting.RED));
            return;
        }
        CrateDef def = CrateRegistry.get(crateType);
        if (def == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("This crate isn't configured.").withStyle(ChatFormatting.RED));
            return;
        }
        int keys = countKeys(player);
        if (keys < def.keysRequired()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You need " + def.keysRequired() + " key"
                    + (def.keysRequired() == 1 ? "" : "s") + " to open the " + def.name()
                    + " (you have " + keys + ").").withStyle(ChatFormatting.RED));
            showOdds(player, crateType);
            return;
        }

        CrateDef.LootEntry loot = CrateRegistry.roll(def, RNG);
        if (loot == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("This crate is empty.").withStyle(ChatFormatting.GRAY));
            return;
        }
        if (loot.isItem() && !BuiltInRegistries.ITEM.containsKey(loot.itemId())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("This crate is misconfigured (unknown item "
                    + loot.itemId() + ") - check the server log.").withStyle(ChatFormatting.RED));
            return;
        }
        consumeKeys(player, def.keysRequired());

        Component rewardText;
        if (loot.isItem()) {
            int n = randRange(loot.min(), loot.max());
            ItemStack reward = new ItemStack(BuiltInRegistries.ITEM.get(loot.itemId()), n);
            rewardText = Component.literal(n + "x ").append(reward.getHoverName().copy().withStyle(ChatFormatting.WHITE));
            player.getInventory().placeItemBackInInventory(reward);
        } else {
            CurrencyApi.deposit(player, loot.coins(), TransactionReason.FAUCET, "crate: " + def.name());
            rewardText = NotchCurrency.coins(loot.coins());
        }

        net.fugginbeenus.notchcurrency.block.CrateBlock.animateOpen(world, pos);
        effects(world, pos);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("🎁 You opened the " + def.name() + " and won ").withStyle(ChatFormatting.GOLD)
                .append(rewardText)
                .append(Component.literal("!").withStyle(ChatFormatting.GOLD)));
    }

    public static void showOdds(ServerPlayer player, String crateType) {
        CrateDef def = CrateRegistry.get(crateType);
        if (def == null) return;
        int total = def.totalWeight();
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("═══ " + def.name() + " - Odds ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Cost: " + def.keysRequired() + " key" + (def.keysRequired() == 1 ? "" : "s"))
                .withStyle(ChatFormatting.GRAY));
        for (CrateDef.LootEntry e : def.loot()) {
            String pct = total > 0 ? String.format("%.1f%%", 100.0 * e.weight() / total) : "0%";
            Component name;
            if (e.isItem()) {
                String range = e.min() == e.max() ? String.valueOf(e.min()) : e.min() + "-" + e.max();
                name = Component.literal(range + "x ").append(new ItemStack(BuiltInRegistries.ITEM.get(e.itemId())).getHoverName().copy().withStyle(ChatFormatting.WHITE));
            } else {
                name = Component.literal(e.coins() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()).withStyle(ChatFormatting.YELLOW);
            }
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(name)
                    .append(Component.literal("  " + pct).withStyle(ChatFormatting.AQUA)));
        }
    }

    public static void buyKey(ServerPlayer player, int amount) {
        if (!enabled) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Crates are disabled.").withStyle(ChatFormatting.RED));
            return;
        }
        if (amount <= 0) return;
        long cost = keyPrice * amount;
        if (CurrencyApi.getBalance(player) < cost) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("You can't afford " + amount + " key" + (amount == 1 ? "" : "s") + " (")
                    .withStyle(ChatFormatting.RED).append(NotchCurrency.coins(cost)).append(Component.literal(").").withStyle(ChatFormatting.RED)));
            return;
        }
        CurrencyApi.withdraw(player, cost, TransactionReason.SINK, "crate keys x" + amount);
        giveKeys(player, amount);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Bought " + amount + " Crate Key" + (amount == 1 ? "" : "s") + " for ")
                .withStyle(ChatFormatting.GREEN).append(NotchCurrency.coins(cost)).append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
    }

    public static void giveKeys(ServerPlayer player, int amount) {
        ItemStack keys = new ItemStack(ModItems.CRATE_KEY, amount);
        player.getInventory().placeItemBackInInventory(keys);
    }

    public static long getKeyPrice() {
        return keyPrice;
    }

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
        BlockPos at = pos.immutable();
        long now = world.getGameTime();
        for (int i = 0; i < RATTLE_TICKS.length; i++) {
            cues.add(new Cue(world, at, now + RATTLE_TICKS[i], i));
        }
        cues.add(new Cue(world, at, now + LID_OPENS_TICK, STEP_LID_OPENS));
        cues.add(new Cue(world, at, now + REWARD_TICK, STEP_REWARD));
        cues.add(new Cue(world, at, now + LID_SHUTS_TICK, STEP_LID_SHUTS));
    }

    private static int randRange(int min, int max) {
        return max <= min ? min : RNG.nextInt(max - min + 1) + min;
    }
}
