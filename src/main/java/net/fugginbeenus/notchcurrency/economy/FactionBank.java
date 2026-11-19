package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.npc.faction.Faction;
import net.fugginbeenus.notchcurrency.npc.faction.FactionManager;
import net.fugginbeenus.notchcurrency.npc.faction.FactionState;
import net.fugginbeenus.notchcurrency.npc.faction.FactionText;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class FactionBank {

    private static final long MARK = 0x4E4346414354L;

    private FactionBank() {}

    public static boolean enabled() {
        return NotchConfigIO.get().factions.factionBanks;
    }

    public static UUID account(String factionId) {
        UUID hash = UUID.nameUUIDFromBytes(("notchcurrency:faction:" + factionId).getBytes(StandardCharsets.UTF_8));
        return new UUID((MARK << 16) | (hash.getMostSignificantBits() & 0x0FFFL), hash.getLeastSignificantBits());
    }

    public static boolean isAccount(@Nullable UUID id) {
        return id != null && (id.getMostSignificantBits() >>> 16) == MARK;
    }

    @Nullable
    public static String nameOf(MinecraftServer server, UUID id) {
        FactionState state = FactionState.get(server);
        for (Faction faction : state.all()) {
            if (account(faction.id()).equals(id)) return faction.displayName();
        }
        for (Faction house : state.allHouses()) {
            if (account(house.id()).equals(id)) return FactionText.plain(server, house);
        }
        return null;
    }

    public static long balance(MinecraftServer server, Faction faction) {
        return BalanceStore.get(server, account(faction.id()));
    }

    public static boolean isMember(ServerPlayer sp, Faction faction) {
        return FactionState.get(sp.level().getServer()).isIn(sp.getUUID(), faction.id());
    }

    public static boolean canWithdraw(ServerPlayer sp, Faction faction) {
        return FactionManager.canManage(sp, faction);
    }

    public static boolean deposit(ServerPlayer sp, Faction faction, long amount) {
        if (!ready(sp, amount)) return false;
        if (!isMember(sp, faction)) {
            tell(sp, "Only members can put money in.");
            return false;
        }
        if (!CurrencyApi.withdraw(sp, amount, TransactionReason.FACTION, "faction bank: " + faction.displayName())) {
            Msg.chat(sp, Component.literal("You don't have ").withStyle(ChatFormatting.RED)
                    .append(NotchCurrency.coins(amount)).append(Component.literal(".").withStyle(ChatFormatting.RED)));
            return false;
        }
        BalanceStore.add(sp.level().getServer(), account(faction.id()), amount, TransactionReason.FACTION,
                "deposit by " + sp.getName().getString());
        Msg.chat(sp, Component.literal("You put ").withStyle(ChatFormatting.GREEN)
                .append(NotchCurrency.coins(amount))
                .append(Component.literal(" in the bank of ").withStyle(ChatFormatting.GREEN))
                .append(FactionText.name(sp.level().getServer(), faction))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
        return true;
    }

    public static boolean withdraw(ServerPlayer sp, Faction faction, long amount) {
        if (!ready(sp, amount)) return false;
        if (!canWithdraw(sp, faction)) {
            tell(sp, faction.isHouse() ? "Only the leader, the founder or an admin can take money out."
                    : "Only the founder or an admin can take money out.");
            return false;
        }
        MinecraftServer server = sp.level().getServer();
        if (!take(server, faction, amount, "withdrawn by " + sp.getName().getString())) {
            notEnough(sp, server, faction);
            return false;
        }
        CurrencyApi.deposit(sp, amount, TransactionReason.FACTION, "faction bank: " + faction.displayName());
        Msg.chat(sp, Component.literal("You took ").withStyle(ChatFormatting.GREEN)
                .append(NotchCurrency.coins(amount))
                .append(Component.literal(" from the bank of ").withStyle(ChatFormatting.GREEN))
                .append(FactionText.name(sp.level().getServer(), faction))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
        return true;
    }

    public static boolean pay(ServerPlayer sp, Faction faction, ServerPlayer target, long amount) {
        if (!ready(sp, amount)) return false;
        if (!canWithdraw(sp, faction)) {
            tell(sp, faction.isHouse() ? "Only the leader, the founder or an admin can pay from the bank."
                    : "Only the founder or an admin can pay from the bank.");
            return false;
        }
        MinecraftServer server = sp.level().getServer();
        if (!take(server, faction, amount, "paid to " + target.getName().getString())) {
            notEnough(sp, server, faction);
            return false;
        }
        CurrencyApi.deposit(target, amount, TransactionReason.FACTION, "paid by " + faction.displayName());
        Msg.chat(sp, Component.literal("Paid ").withStyle(ChatFormatting.GREEN)
                .append(NotchCurrency.coins(amount))
                .append(Component.literal(" to " + target.getName().getString() + " from the bank.")
                        .withStyle(ChatFormatting.GREEN)));
        if (target != sp) {
            Msg.chat(target, Component.literal(faction.displayName()).withStyle(faction.color())
                    .append(Component.literal(" paid you ").withStyle(ChatFormatting.GREEN))
                    .append(NotchCurrency.coins(amount))
                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
        }
        return true;
    }

    public static void addDues(MinecraftServer server, Faction faction, long fee, String from) {
        BalanceStore.add(server, account(faction.id()), fee, TransactionReason.FACTION, "dues from " + from);
    }

    public static boolean adjust(MinecraftServer server, Faction faction, long delta) {
        if (delta < 0 && balance(server, faction) < -delta) return false;
        BalanceStore.add(server, account(faction.id()), delta, TransactionReason.ADMIN, "admin");
        return true;
    }

    public static void payOutOnDisband(MinecraftServer server, Faction faction, ServerPlayer disbander) {
        long left = balance(server, faction);
        if (left <= 0) return;
        UUID to = faction.founder() != null ? faction.founder() : disbander.getUUID();
        BalanceStore.add(server, account(faction.id()), -left, TransactionReason.FACTION, "faction disbanded");
        CurrencyApi.deposit(server, to, left, TransactionReason.FACTION, "bank of " + faction.displayName());
        ServerPlayer online = server.getPlayerList().getPlayer(to);
        if (online != null) {
            Msg.chat(online, Component.literal(faction.displayName()).withStyle(faction.color())
                    .append(Component.literal(" was disbanded. Its bank paid you ").withStyle(ChatFormatting.YELLOW))
                    .append(NotchCurrency.coins(left))
                    .append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
        }
    }

    public static long moveAll(MinecraftServer server, Faction from, Faction to, String detail) {
        long left = balance(server, from);
        if (left <= 0) return 0L;
        BalanceStore.add(server, account(from.id()), -left, TransactionReason.FACTION, detail);
        BalanceStore.add(server, account(to.id()), left, TransactionReason.FACTION, detail);
        return left;
    }

    public static boolean hold(MinecraftServer server, Faction faction, long amount, String detail) {
        return amount > 0 && take(server, faction, amount, detail);
    }

    public static void giveBack(MinecraftServer server, String factionId, long amount, String detail) {
        if (amount <= 0 || factionId == null || factionId.isEmpty()) return;
        BalanceStore.add(server, account(factionId), amount, TransactionReason.FACTION, detail);
    }

    private static boolean take(MinecraftServer server, Faction faction, long amount, String detail) {
        if (balance(server, faction) < amount) return false;
        BalanceStore.add(server, account(faction.id()), -amount, TransactionReason.FACTION, detail);
        return true;
    }

    private static boolean ready(ServerPlayer sp, long amount) {
        if (!enabled()) {
            tell(sp, "Faction banks are off on this server.");
            return false;
        }
        if (amount <= 0) {
            tell(sp, "Enter an amount above 0.");
            return false;
        }
        return true;
    }

    private static void notEnough(ServerPlayer sp, MinecraftServer server, Faction faction) {
        Msg.chat(sp, Component.literal("The bank only has ").withStyle(ChatFormatting.RED)
                .append(NotchCurrency.coins(balance(server, faction)))
                .append(Component.literal(".").withStyle(ChatFormatting.RED)));
    }

    private static void tell(ServerPlayer sp, String text) {
        Msg.chat(sp, Component.literal(text).withStyle(ChatFormatting.RED));
    }
}
