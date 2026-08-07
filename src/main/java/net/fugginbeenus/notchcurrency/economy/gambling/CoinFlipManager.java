package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.block.CoinFace;
import net.fugginbeenus.notchcurrency.block.CoinFlipBlock;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class CoinFlipManager {

    private static final Random RNG = new Random();

    private static int payoutPercent = 195;
    private static int revealTicks = 30;

    private static final Map<UUID, BlockPos> pendingBlock = new HashMap<>();

    private static final List<Pending> queue = new ArrayList<>();

    private record Pending(ServerLevel world, BlockPos pos, UUID player,
                           long bet, boolean won, boolean landedHeads, long revealAt) {}

    private CoinFlipManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(CoinFlipManager::tick);
    }

    public static void applyConfig(NotchConfig cfg) {
        payoutPercent = Math.max(100, Math.min(300, cfg.gambling.coinFlipPayoutPercent));
        revealTicks = Math.max(0, Math.min(200, cfg.gambling.coinFlipRevealTicks));
    }

    public static int getPayoutPercent() { return payoutPercent; }

    // ---- entry points ----

    public static void openScreen(ServerPlayer sp, BlockPos pos) {
        if (!GamblingManager.isEnabled()) {
            sp.displayClientMessage(Component.literal("Gambling is disabled on this server.").withStyle(ChatFormatting.RED), false);
            return;
        }
        pendingBlock.put(sp.getUUID(), pos.immutable());
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new CoinFlipScreenHandler(containerId, inv),
                Component.literal("Coin Flip")));
    }

    public static void notifyBusy(ServerPlayer sp) {
        sp.displayClientMessage(Component.literal("The coin is still in the air - wait for it to land.").withStyle(ChatFormatting.YELLOW), false);
    }

    public static void flipFromScreen(ServerPlayer sp, boolean guessHeads, long bet) {
        BlockPos pos = pendingBlock.get(sp.getUUID());
        resolve(sp, guessHeads, bet, pos);
    }

    public static void flipCommand(ServerPlayer sp, boolean guessHeads, long bet) {
        resolve(sp, guessHeads, bet, null);
    }

    // ---- core ----

    private static void resolve(ServerPlayer sp, boolean guessHeads, long bet, BlockPos pos) {
        if (!GamblingManager.isEnabled()) {
            sp.displayClientMessage(Component.literal("Gambling is disabled on this server.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (!GamblingManager.betInRange(bet)) {
            sp.displayClientMessage(Component.literal("Bet must be between " + GamblingManager.getMinBet()
                    + " and " + GamblingManager.getMaxBet() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.RED), false);
            return;
        }
        ServerLevel world = sp.serverLevel();
        if (pos != null) {
            BlockState st = world.getBlockState(pos);
            if (!(st.getBlock() instanceof CoinFlipBlock)) {
                pos = null; // block gone: fall back to an instant, block-less flip
            } else if (st.getValue(CoinFlipBlock.FLIPPING)) {
                notifyBusy(sp);
                return;
            }
        }
        if (!CurrencyApi.withdraw(sp, bet, TransactionReason.SINK, "Coin flip bet")) {
            sp.displayClientMessage(Component.literal("You don't have " + bet + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to bet.").withStyle(ChatFormatting.RED), false);
            return;
        }

        boolean landedHeads = RNG.nextBoolean();
        boolean won = (guessHeads == landedHeads);

        if (pos != null && revealTicks > 0) {
            // Start the spin, close the screen so the player watches the block, reveal on delay.
            BlockState st = world.getBlockState(pos);
            world.setBlock(pos, st.setValue(CoinFlipBlock.FLIPPING, true), Block.UPDATE_CLIENTS);
            if (world.getBlockEntity(pos) instanceof net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity be) {
                be.startFlip(world.getGameTime(), revealTicks); // drives the pop + spin animation
            }
            world.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8f, 1.4f);
            sp.closeContainer();
            queue.add(new Pending(world, pos.immutable(), sp.getUUID(), bet, won, landedHeads,
                    world.getGameTime() + revealTicks));
        } else {
            applyReveal(world, pos, sp.getUUID(), bet, won, landedHeads);
        }
    }

    private static void tick(MinecraftServer server) {
        if (queue.isEmpty()) return;
        Iterator<Pending> it = queue.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.world().getGameTime() >= p.revealAt()) {
                applyReveal(p.world(), p.pos(), p.player(), p.bet(), p.won(), p.landedHeads());
                it.remove();
            }
        }
    }

    private static void applyReveal(ServerLevel world, BlockPos pos, UUID player,
                                    long bet, boolean won, boolean landedHeads) {
        if (pos != null) {
            BlockState st = world.getBlockState(pos);
            if (st.getBlock() instanceof CoinFlipBlock) {
                world.setBlock(pos, st
                        .setValue(CoinFlipBlock.FLIPPING, false)
                        .setValue(CoinFlipBlock.FACE, landedHeads ? CoinFace.HEADS : CoinFace.TAILS),
                        Block.UPDATE_CLIENTS);
            }
            Vec3 c = Vec3.atCenterOf(pos).add(0, 0.6, 0);
            if (won) {
                world.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8f, 1.2f);
                world.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 16, 0.3, 0.3, 0.3, 0.0);
                world.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 10, 0.25, 0.25, 0.25, 0.02);
            } else {
                world.playSound(null, pos, SoundEvents.VILLAGER_NO, SoundSource.BLOCKS, 0.9f, 0.9f);
                world.sendParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 10, 0.2, 0.2, 0.2, 0.01);
            }
        }

        long payout = won ? Math.round(bet * (payoutPercent / 100.0)) : 0L;
        if (payout > 0) {
            CurrencyApi.deposit(world.getServer(), player, payout, TransactionReason.FAUCET, "Coin flip win");
        }

        ServerPlayer sp = world.getServer().getPlayerList().getPlayer(player);
        if (sp != null) {
            String side = landedHeads ? "HEADS" : "TAILS";
            if (won) {
                sp.displayClientMessage(Component.literal("The coin landed " + side + " - you won " + payout + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "!")
                        .withStyle(ChatFormatting.GREEN), false);
            } else {
                sp.displayClientMessage(Component.literal("The coin landed " + side + " - you lost " + bet + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                        .withStyle(ChatFormatting.RED), false);
            }
        }
    }
}
