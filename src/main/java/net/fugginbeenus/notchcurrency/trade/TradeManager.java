package net.fugginbeenus.notchcurrency.trade;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class TradeManager {

    private static final Map<UUID, TradeSession> sessions = new HashMap<>();
    private static final int TIMEOUT_TICKS = 20 * 60 * 5; // 5 minutes
    private static final double MAX_DISTANCE = 10.0;      // blocks
    private static boolean INITIALIZED = false;

    private TradeManager() {}

    public static void init() {
        if (INITIALIZED) return;
        INITIALIZED = true;

        // Tick life-cycle (timeout / distance cancel)
        ServerTickEvents.START_SERVER_TICK.register(s -> tickSessions());

        // Client -> server: money + ready toggle
        Net.registerServerReceiver(NotchPackets.TRADE_UPDATE, (srv, player, buf) -> {
            int money = buf.readVarInt();
            boolean ready = buf.readBoolean();
            srv.execute(() -> {
                TradeSession sess = get(player.getUUID());
                if (sess != null) {
                    sess.updateOffer(player, money, ready);
                    if (sess.aReady && sess.bReady) {
                        sess.tryComplete();
                    }
                }
            });
        });

        // Client -> server: cancel (ESC/close)
        Net.registerServerReceiver(NotchPackets.TRADE_CANCEL, (srv, player, buf) -> {
            String reason = buf.readUtf(64);
            srv.execute(() -> {
                TradeSession sess = get(player.getUUID());
                if (sess != null) sess.cancel(reason);
            });
        });
    }

    public static void invite(ServerPlayer from, ServerPlayer to) {
        if (from == to) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("You cannot trade yourself.").withStyle(ChatFormatting.RED));
            return;
        }
        if (get(from.getUUID()) != null || get(to.getUUID()) != null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("Either you or the target is already trading.").withStyle(ChatFormatting.RED));
            return;
        }
        TradeSession sess = new TradeSession(from, to);
        sessions.put(from.getUUID(), sess);
        sessions.put(to.getUUID(), sess);

        Component accept = Component.literal("[ACCEPT]").setStyle(
                Style.EMPTY.withColor(ChatFormatting.GREEN)
                        .withBold(true)
                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand("/trade accept " + from.getName().getString()))
        );
        Component decline = Component.literal("[DECLINE]").setStyle(
                Style.EMPTY.withColor(ChatFormatting.RED)
                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand("/trade decline " + from.getName().getString()))
        );
        net.fugginbeenus.notchcurrency.compat.Msg.chat(to, Component.literal(from.getName().getString() + " wants to trade: ")
                .append(accept).append(Component.literal(" ")).append(decline));
        net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("Trade invite sent to " + to.getName().getString()).withStyle(ChatFormatting.GRAY));
    }

    public static void accept(ServerPlayer target, String inviterName) {
        TradeSession sess = get(target.getUUID());
        if (sess == null || !sess.involves(inviterName)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(target, Component.literal("No pending trade with " + inviterName + ".").withStyle(ChatFormatting.RED));
            return;
        }
        sess.openScreens();
    }

    public static void decline(ServerPlayer target, String inviterName) {
        TradeSession sess = get(target.getUUID());
        if (sess != null && sess.involves(inviterName)) {
            sess.cancel("Declined");
        }
    }

    private static void tickSessions() {
        if (sessions.isEmpty()) return;
        for (TradeSession s : new HashSet<>(sessions.values())) {
            if (s.closed) continue;

            s.ticks++;
            if (s.ticks > TIMEOUT_TICKS) {
                s.cancel("Timed out");
                continue;
            }
            if (!s.sameWorld() || s.distance() > MAX_DISTANCE) {
                s.cancel("Too far apart");
            }
        }
        sessions.values().removeIf(TradeSession::isClosed);
        sessions.entrySet().removeIf(e -> e.getValue().isClosed());
    }

    static TradeSession get(UUID any) { return sessions.get(any); }
    static void remove(TradeSession s) {
        sessions.remove(s.a.getUUID());
        sessions.remove(s.b.getUUID());
    }

    /* ---------------- Trade Session ---------------- */

    public static final class TradeSession {
        final ServerPlayer a, b;
        int aMoney = 0, bMoney = 0;
        boolean aReady = false, bReady = false;
        int ticks = 0;
        boolean closed = false;

        TradeScreenHandler aHandler, bHandler;

        TradeSession(ServerPlayer a, ServerPlayer b) {
            this.a = a;
            this.b = b;
        }

        boolean isClosed() { return closed; }
        boolean involves(String name) {
            return a.getName().getString().equals(name) || b.getName().getString().equals(name);
        }

        boolean sameWorld() { return a.level() == b.level(); }
        double distance() { return a.position().distanceTo(b.position()); }

        void openScreens() {
            // A’s view (self on left)
            a.openMenu(new SimpleMenuProvider((containerId, inv, p) -> {
                TradeScreenHandler h = new TradeScreenHandler(containerId, inv, p, this, true);
                this.aHandler = h;
                return h;
            }, Component.literal("Trade")));

            // B’s view (their self is left on their screen)
            b.openMenu(new SimpleMenuProvider((containerId, inv, p) -> {
                TradeScreenHandler h = new TradeScreenHandler(containerId, inv, p, this, false);
                this.bHandler = h;
                return h;
            }, Component.literal("Trade")));

            if (aHandler != null) aHandler.sendAllDataToRemote();
            if (bHandler != null) bHandler.sendAllDataToRemote();
        }

        void updateOffer(ServerPlayer who, int money, boolean ready) {
            if (who == a) { aMoney = Math.max(0, money); aReady = ready; }
            else if (who == b) { bMoney = Math.max(0, money); bReady = ready; }

            if (aHandler != null) aHandler.sendAllDataToRemote();
            if (bHandler != null) bHandler.sendAllDataToRemote();
        }

        void cancel(String reason) {
            if (closed) return;
            closed = true;

            if (aHandler != null) aHandler.returnItems();
            if (bHandler != null) bHandler.returnItems();

            sendCancel(a, reason);
            sendCancel(b, reason);

            TradeManager.remove(this);
        }

        void tryComplete() {
            if (!(aReady && bReady)) return;
            if (aHandler == null || bHandler == null) return;

            long aBal = BalanceStore.get(a);
            long bBal = BalanceStore.get(b);
            if (aMoney > aBal || bMoney > bBal) {
                cancel("Insufficient funds");
                return;
            }

            // Swap items
            var aItems = aHandler.takeItemsForCompletion();
            var bItems = bHandler.takeItemsForCompletion();
            for (ItemStack s : aItems) if (!s.isEmpty()) b.getInventory().placeItemBackInInventory(s.copy());
            for (ItemStack s : bItems) if (!s.isEmpty()) a.getInventory().placeItemBackInInventory(s.copy());

            // Money transfers
            BalanceStore.subtract(a, aMoney, TransactionReason.TRADE, "trade with " + b.getName().getString());
            BalanceStore.add(b, aMoney, TransactionReason.TRADE, "trade with " + a.getName().getString());
            BalanceStore.subtract(b, bMoney, TransactionReason.TRADE, "trade with " + a.getName().getString());
            BalanceStore.add(a, bMoney, TransactionReason.TRADE, "trade with " + b.getName().getString());

            // Push fresh balances to HUD immediately
            NotchPackets.sendBalance(a, BalanceStore.get(a));
            NotchPackets.sendBalance(b, BalanceStore.get(b));

            // Notify both clients trade is complete
            sendDone(a);
            sendDone(b);

            closed = true;
            TradeManager.remove(this);
        }

        /* ---------- helpers ---------- */

        private void sendCancel(ServerPlayer p, String reason) {
            var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
            buf.writeUtf(reason);
            Net.sendToClient(p, NotchPackets.TRADE_CANCEL, buf);
        }

        private void sendDone(ServerPlayer p) {
            Net.sendToClient(p, NotchPackets.TRADE_COMPLETE, net.fugginbeenus.notchcurrency.compat.Net.emptyBuf());
        }
    }
}
