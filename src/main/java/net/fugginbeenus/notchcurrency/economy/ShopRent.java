package net.fugginbeenus.notchcurrency.economy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.shop.PlayerShop;
import net.fugginbeenus.notchcurrency.shop.ShopState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class ShopRent {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-ShopRent");

    private static boolean enabled = false;
    private static long baseRent = 100L;
    private static long perListing = 0L;
    private static long intervalTicks = 1440L * 60L * 20L;
    private static int graceCycles = 3;
    private static boolean announce = true;

    private static long tickAccum = 0;

    private ShopRent() {}

    public static long rentFor(int listingCount) {
        return enabled ? baseRent + perListing * listingCount : 0L;
    }

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(ShopRent::tick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.ShopRent r = cfg.shopRent;
        enabled = r.enabled;
        baseRent = Math.max(0L, r.baseRent);
        perListing = Math.max(0L, r.perListing);
        intervalTicks = Math.max(1L, (long) r.intervalMinutes) * 60L * 20L;
        graceCycles = Math.max(0, r.graceCycles);
        announce = r.announce;
    }

    private static void tick(MinecraftServer server) {
        if (!enabled) return;
        if (++tickAccum < intervalTicks) return;
        tickAccum = 0;
        charge(server);
    }

    private static void charge(MinecraftServer server) {
        ShopState state = ShopState.get(server);
        boolean dirty = false;

        for (PlayerShop shop : state.getAllShops()) {
            if (!shop.isOpen()) continue; // closed shops owe nothing

            long rent = baseRent + perListing * shop.getListings().size();
            if (rent <= 0) continue;

            UUID owner = shop.getOwnerId();
            long available = shop.getPendingBalance() + BalanceStore.get(server, owner);

            if (available >= rent) {
                long fromPending = shop.payFromPending(rent);
                long fromOwner = rent - fromPending;
                if (fromOwner > 0) {
                    ServerPlayerEntity online = server.getPlayerManager().getPlayer(owner);
                    if (online != null) {
                        CurrencyApi.withdraw(online, fromOwner, TransactionReason.SINK, "shop rent: " + shop.getShopName());
                    } else {
                        BalanceStore.add(server, owner, -fromOwner, TransactionReason.SINK, "shop rent: " + shop.getShopName());
                    }
                }
                shop.setRentPaused(false);
                shop.setUnpaidRentCycles(0);
                notify(server, owner, Text.literal("Rent of ").formatted(Formatting.GRAY)
                        .append(NotchCurrency.coins(rent))
                        .append(Text.literal(" was paid for ").formatted(Formatting.GRAY))
                        .append(Text.literal(shop.getShopName()).formatted(Formatting.WHITE))
                        .append(Text.literal(".").formatted(Formatting.GRAY)));
            } else {
                shop.setRentPaused(true);
                int cycles = shop.getUnpaidRentCycles() + 1;
                shop.setUnpaidRentCycles(cycles);

                if (cycles > graceCycles) {
                    shop.setOpen(false);
                    shop.setRentPaused(false);
                    shop.setUnpaidRentCycles(0);
                    notify(server, owner, Text.literal("Your shop ")
                            .append(Text.literal(shop.getShopName()).formatted(Formatting.WHITE))
                            .append(Text.literal(" was closed for unpaid rent. Reopen it with /shop toggle.").formatted(Formatting.RED)));
                } else {
                    notify(server, owner, Text.literal("⚠ ")
                            .append(Text.literal(shop.getShopName()).formatted(Formatting.WHITE))
                            .append(Text.literal(" is frozen - couldn't pay rent of ").formatted(Formatting.YELLOW))
                            .append(NotchCurrency.coins(rent))
                            .append(Text.literal(" (" + cycles + "/" + (graceCycles + 1) + ").").formatted(Formatting.YELLOW)));
                }
            }
            dirty = true;
        }

        if (dirty) {
            state.markDirty();
            LOGGER.info("Charged shop rent.");
        }
    }

    private static void notify(MinecraftServer server, UUID owner, Text message) {
        if (!announce) return;
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(owner);
        if (p != null) p.sendMessage(message, false);
    }
}
