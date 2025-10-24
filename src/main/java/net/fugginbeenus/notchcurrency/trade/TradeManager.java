package net.fugginbeenus.notchcurrency.trade;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

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
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_UPDATE, (srv, player, handler, buf, response) -> {
            int money = buf.readVarInt();
            boolean ready = buf.readBoolean();
            srv.execute(() -> {
                TradeSession sess = get(player.getUuid());
                if (sess != null) {
                    sess.updateOffer(player, money, ready);
                    if (sess.aReady && sess.bReady) {
                        sess.tryComplete();
                    }
                }
            });
        });

        // Client -> server: cancel (ESC/close)
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_CANCEL, (srv, player, handler, buf, response) -> {
            String reason = buf.readString(64);
            srv.execute(() -> {
                TradeSession sess = get(player.getUuid());
                if (sess != null) sess.cancel(reason);
            });
        });
    }

    public static void invite(ServerPlayerEntity from, ServerPlayerEntity to) {
        if (from == to) {
            from.sendMessage(Text.literal("You cannot trade yourself.").formatted(Formatting.RED), false);
            return;
        }
        if (get(from.getUuid()) != null || get(to.getUuid()) != null) {
            from.sendMessage(Text.literal("Either you or the target is already trading.").formatted(Formatting.RED), false);
            return;
        }
        TradeSession sess = new TradeSession(from, to);
        sessions.put(from.getUuid(), sess);
        sessions.put(to.getUuid(), sess);

        Text accept = Text.literal("[ACCEPT]").setStyle(
                Style.EMPTY.withColor(Formatting.GREEN)
                        .withBold(true)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade accept " + from.getName().getString()))
        );
        Text decline = Text.literal("[DECLINE]").setStyle(
                Style.EMPTY.withColor(Formatting.RED)
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/trade decline " + from.getName().getString()))
        );
        to.sendMessage(Text.literal(from.getName().getString() + " wants to trade: ")
                .append(accept).append(Text.literal(" ")).append(decline), false);
        from.sendMessage(Text.literal("Trade invite sent to " + to.getName().getString()).formatted(Formatting.GRAY), false);
    }

    public static void accept(ServerPlayerEntity target, String inviterName) {
        TradeSession sess = get(target.getUuid());
        if (sess == null || !sess.involves(inviterName)) {
            target.sendMessage(Text.literal("No pending trade with " + inviterName + ".").formatted(Formatting.RED), false);
            return;
        }
        sess.openScreens();
    }

    public static void decline(ServerPlayerEntity target, String inviterName) {
        TradeSession sess = get(target.getUuid());
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
        sessions.remove(s.a.getUuid());
        sessions.remove(s.b.getUuid());
    }

    /* ---------------- Trade Session ---------------- */

    public static final class TradeSession {
        final ServerPlayerEntity a, b;
        int aMoney = 0, bMoney = 0;
        boolean aReady = false, bReady = false;
        int ticks = 0;
        boolean closed = false;

        TradeScreenHandler aHandler, bHandler;

        TradeSession(ServerPlayerEntity a, ServerPlayerEntity b) {
            this.a = a;
            this.b = b;
        }

        boolean isClosed() { return closed; }
        boolean involves(String name) {
            return a.getName().getString().equals(name) || b.getName().getString().equals(name);
        }

        boolean sameWorld() { return a.getWorld() == b.getWorld(); }
        double distance() { return a.getPos().distanceTo(b.getPos()); }

        void openScreens() {
            // A’s view (self on left)
            a.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
                TradeScreenHandler h = new TradeScreenHandler(syncId, inv, p, this, true);
                this.aHandler = h;
                return h;
            }, Text.literal("Trade")));

            // B’s view (their self is left on their screen)
            b.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, inv, p) -> {
                TradeScreenHandler h = new TradeScreenHandler(syncId, inv, p, this, false);
                this.bHandler = h;
                return h;
            }, Text.literal("Trade")));

            if (aHandler != null) aHandler.syncState();
            if (bHandler != null) bHandler.syncState();
        }

        void updateOffer(ServerPlayerEntity who, int money, boolean ready) {
            if (who == a) { aMoney = Math.max(0, money); aReady = ready; }
            else if (who == b) { bMoney = Math.max(0, money); bReady = ready; }

            if (aHandler != null) aHandler.syncState();
            if (bHandler != null) bHandler.syncState();
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

            int aBal = BalanceStore.get(a);
            int bBal = BalanceStore.get(b);
            if (aMoney > aBal || bMoney > bBal) {
                cancel("Insufficient funds");
                return;
            }

            // Swap items
            var aItems = aHandler.takeItemsForCompletion();
            var bItems = bHandler.takeItemsForCompletion();
            for (ItemStack s : aItems) if (!s.isEmpty()) b.getInventory().offerOrDrop(s.copy());
            for (ItemStack s : bItems) if (!s.isEmpty()) a.getInventory().offerOrDrop(s.copy());

            // Money transfers
            BalanceStore.subtract(a, aMoney);
            BalanceStore.add(b, aMoney);
            BalanceStore.subtract(b, bMoney);
            BalanceStore.add(a, bMoney);

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

        private void sendCancel(ServerPlayerEntity p, String reason) {
            var buf = PacketByteBufs.create();
            buf.writeString(reason);
            ServerPlayNetworking.send(p, NotchPackets.TRADE_CANCEL, buf);
        }

        private void sendDone(ServerPlayerEntity p) {
            ServerPlayNetworking.send(p, NotchPackets.TRADE_COMPLETE, PacketByteBufs.empty());
        }
    }
}
