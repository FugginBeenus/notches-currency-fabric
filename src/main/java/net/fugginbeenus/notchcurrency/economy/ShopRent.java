package net.fugginbeenus.notchcurrency.economy;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.shop.PlayerShop;
import net.fugginbeenus.notchcurrency.shop.ShopState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
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
                    ServerPlayer online = server.getPlayerList().getPlayer(owner);
                    if (online != null) {
                        CurrencyApi.withdraw(online, fromOwner, TransactionReason.SINK, "shop rent: " + shop.getShopName());
                    } else {
                        BalanceStore.add(server, owner, -fromOwner, TransactionReason.SINK, "shop rent: " + shop.getShopName());
                    }
                }
                shop.setRentPaused(false);
                shop.setUnpaidRentCycles(0);
                notify(server, owner, Component.literal("Rent of ").withStyle(ChatFormatting.GRAY)
                        .append(NotchCurrency.coins(rent))
                        .append(Component.literal(" was paid for ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(shop.getShopName()).withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(".").withStyle(ChatFormatting.GRAY)));
            } else {
                shop.setRentPaused(true);
                int cycles = shop.getUnpaidRentCycles() + 1;
                shop.setUnpaidRentCycles(cycles);

                if (cycles > graceCycles) {
                    shop.setOpen(false);
                    shop.setRentPaused(false);
                    shop.setUnpaidRentCycles(0);
                    notify(server, owner, Component.literal("Your shop ")
                            .append(Component.literal(shop.getShopName()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" was closed for unpaid rent. Reopen it with /shop toggle.").withStyle(ChatFormatting.RED)));
                } else {
                    notify(server, owner, Component.literal("⚠ ")
                            .append(Component.literal(shop.getShopName()).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" is frozen - couldn't pay rent of ").withStyle(ChatFormatting.YELLOW))
                            .append(NotchCurrency.coins(rent))
                            .append(Component.literal(" (" + cycles + "/" + (graceCycles + 1) + ").").withStyle(ChatFormatting.YELLOW)));
                }
            }
            dirty = true;
        }

        if (dirty) {
            state.setDirty();
            LOGGER.info("Charged shop rent.");
        }
    }

    private static void notify(MinecraftServer server, UUID owner, Component message) {
        if (!announce) return;
        ServerPlayer p = server.getPlayerList().getPlayer(owner);
        if (p != null) p.displayClientMessage(message, false);
    }
}
