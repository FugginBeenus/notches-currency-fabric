package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.block.CoinFace;
import net.fugginbeenus.notchcurrency.block.CoinFlipBlock;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * The coin flip: a true 50/50. Bet, pick heads or tails, win a configurable payout. When played at a
 * {@link CoinFlipBlock} the block "flips" for a short delay (the {@code FLIPPING} blockstate) and the
 * result is revealed once the spin lands — driven by a tick queue so the reveal art has time to play.
 * Played by command it resolves instantly.
 */
public final class CoinFlipManager {

    private static final Random RNG = new Random();

    private static int payoutPercent = 195;
    private static int revealTicks = 30;

    /** Which coin-flip block a player last opened, so the FLIP packet knows where to animate. */
    private static final Map<UUID, BlockPos> pendingBlock = new HashMap<>();

    /** Bets awaiting their delayed reveal (block flips only). */
    private static final List<Pending> queue = new ArrayList<>();

    private record Pending(ServerWorld world, BlockPos pos, UUID player,
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

    /** Right-clicked a coin-flip block: remember it and open the betting screen. */
    public static void openScreen(ServerPlayerEntity sp, BlockPos pos) {
        if (!GamblingManager.isEnabled()) {
            sp.sendMessage(Text.literal("Gambling is disabled on this server.").formatted(Formatting.RED), false);
            return;
        }
        pendingBlock.put(sp.getUuid(), pos.toImmutable());
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new CoinFlipScreenHandler(syncId, inv),
                Text.literal("Coin Flip")));
    }

    public static void notifyBusy(ServerPlayerEntity sp) {
        sp.sendMessage(Text.literal("The coin is still in the air — wait for it to land.").formatted(Formatting.YELLOW), false);
    }

    /** Player hit FLIP in the coin-flip screen: play at the block they opened (delayed reveal). */
    public static void flipFromScreen(ServerPlayerEntity sp, boolean guessHeads, long bet) {
        BlockPos pos = pendingBlock.get(sp.getUuid());
        resolve(sp, guessHeads, bet, pos);
    }

    /** {@code /coinflip} — no block, resolves instantly. */
    public static void flipCommand(ServerPlayerEntity sp, boolean guessHeads, long bet) {
        resolve(sp, guessHeads, bet, null);
    }

    // ---- core ----

    private static void resolve(ServerPlayerEntity sp, boolean guessHeads, long bet, BlockPos pos) {
        if (!GamblingManager.isEnabled()) {
            sp.sendMessage(Text.literal("Gambling is disabled on this server.").formatted(Formatting.RED), false);
            return;
        }
        if (!GamblingManager.betInRange(bet)) {
            sp.sendMessage(Text.literal("Bet must be between " + GamblingManager.getMinBet()
                    + " and " + GamblingManager.getMaxBet() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").formatted(Formatting.RED), false);
            return;
        }
        ServerWorld world = sp.getServerWorld();
        if (pos != null) {
            BlockState st = world.getBlockState(pos);
            if (!(st.getBlock() instanceof CoinFlipBlock)) {
                pos = null; // block gone — fall back to an instant, block-less flip
            } else if (st.get(CoinFlipBlock.FLIPPING)) {
                notifyBusy(sp);
                return;
            }
        }
        if (!CurrencyApi.withdraw(sp, bet, TransactionReason.SINK, "Coin flip bet")) {
            sp.sendMessage(Text.literal("You don't have " + bet + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to bet.").formatted(Formatting.RED), false);
            return;
        }

        boolean landedHeads = RNG.nextBoolean();
        boolean won = (guessHeads == landedHeads);

        if (pos != null && revealTicks > 0) {
            // Start the spin, close the screen so the player watches the block, reveal on delay.
            BlockState st = world.getBlockState(pos);
            world.setBlockState(pos, st.with(CoinFlipBlock.FLIPPING, true), Block.NOTIFY_LISTENERS);
            if (world.getBlockEntity(pos) instanceof net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity be) {
                be.startFlip(world.getTime(), revealTicks); // drives the pop + spin animation
            }
            world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS, 0.8f, 1.4f);
            sp.closeHandledScreen();
            queue.add(new Pending(world, pos.toImmutable(), sp.getUuid(), bet, won, landedHeads,
                    world.getTime() + revealTicks));
        } else {
            applyReveal(world, pos, sp.getUuid(), bet, won, landedHeads);
        }
    }

    private static void tick(MinecraftServer server) {
        if (queue.isEmpty()) return;
        Iterator<Pending> it = queue.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.world().getTime() >= p.revealAt()) {
                applyReveal(p.world(), p.pos(), p.player(), p.bet(), p.won(), p.landedHeads());
                it.remove();
            }
        }
    }

    /** Land the coin: settle the block face, pay out, and tell the player. */
    private static void applyReveal(ServerWorld world, BlockPos pos, UUID player,
                                    long bet, boolean won, boolean landedHeads) {
        if (pos != null) {
            BlockState st = world.getBlockState(pos);
            if (st.getBlock() instanceof CoinFlipBlock) {
                world.setBlockState(pos, st
                        .with(CoinFlipBlock.FLIPPING, false)
                        .with(CoinFlipBlock.FACE, landedHeads ? CoinFace.HEADS : CoinFace.TAILS),
                        Block.NOTIFY_LISTENERS);
            }
            Vec3d c = Vec3d.ofCenter(pos).add(0, 0.6, 0);
            if (won) {
                world.playSound(null, pos, SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.BLOCKS, 0.8f, 1.2f);
                world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, c.x, c.y, c.z, 16, 0.3, 0.3, 0.3, 0.0);
                world.spawnParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 10, 0.25, 0.25, 0.25, 0.02);
            } else {
                world.playSound(null, pos, SoundEvents.ENTITY_VILLAGER_NO, SoundCategory.BLOCKS, 0.9f, 0.9f);
                world.spawnParticles(ParticleTypes.SMOKE, c.x, c.y, c.z, 10, 0.2, 0.2, 0.2, 0.01);
            }
        }

        long payout = won ? Math.round(bet * (payoutPercent / 100.0)) : 0L;
        if (payout > 0) {
            CurrencyApi.deposit(world.getServer(), player, payout, TransactionReason.FAUCET, "Coin flip win");
        }

        ServerPlayerEntity sp = world.getServer().getPlayerManager().getPlayer(player);
        if (sp != null) {
            String side = landedHeads ? "HEADS" : "TAILS";
            if (won) {
                sp.sendMessage(Text.literal("The coin landed " + side + " — you won " + payout + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "!")
                        .formatted(Formatting.GREEN), false);
            } else {
                sp.sendMessage(Text.literal("The coin landed " + side + " — you lost " + bet + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                        .formatted(Formatting.RED), false);
            }
        }
    }
}
