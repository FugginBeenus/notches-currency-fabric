package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModItems;
//? if <1.21 {
import net.minecraft.client.item.TooltipContext;
//?}
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RaffleTicketItem extends Item {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_WINNER = "WINNER";
    public static final String STATUS_LOSER  = "LOSER";

    private static final String K_ROUND = "Round";
    private static final String K_ENTRIES = "Entries";
    private static final String K_OWNER = "Owner";
    private static final String K_OWNER_NAME = "OwnerName";
    private static final String K_STATUS = "Status";
    private static final String K_PRIZE = "Prize";

    public RaffleTicketItem(Settings settings) {
        super(settings.maxCount(1)); // one item per purchase; never merge entry counts
    }

    // ---- factory & accessors (server-side helpers) ----

    public static ItemStack create(long round, int entries, UUID owner, String ownerName) {
        ItemStack stack = new ItemStack(ModItems.RAFFLE_TICKET);
        StackData.putLong(stack, K_ROUND, round);
        StackData.putInt(stack, K_ENTRIES, entries);
        StackData.putUuid(stack, K_OWNER, owner);
        StackData.putString(stack, K_OWNER_NAME, ownerName);
        StackData.putString(stack, K_STATUS, STATUS_ACTIVE);
        return stack;
    }

    public static boolean isTicket(ItemStack stack) {
        return stack.isOf(ModItems.RAFFLE_TICKET) && StackData.hasData(stack);
    }

    public static long round(ItemStack stack) {
        return StackData.getLong(stack, K_ROUND);
    }

    public static int entries(ItemStack stack) {
        return StackData.getInt(stack, K_ENTRIES);
    }

    public static void setEntries(ItemStack stack, int entries) {
        StackData.putInt(stack, K_ENTRIES, entries);
    }

    @Nullable
    public static UUID owner(ItemStack stack) {
        return StackData.getUuid(stack, K_OWNER);
    }

    public static String status(ItemStack stack) {
        return StackData.hasData(stack) ? StackData.getString(stack, K_STATUS) : STATUS_ACTIVE;
    }

    public static void setStatus(ItemStack stack, String status, long prize) {
        StackData.putString(stack, K_STATUS, status);
        if (STATUS_WINNER.equals(status)) StackData.putLong(stack, K_PRIZE, prize);
    }

    // ---- display ----

    @Override
    public Text getName(ItemStack stack) {
        return switch (status(stack)) {
            case STATUS_WINNER -> Text.literal("Winning Raffle Ticket").formatted(Formatting.GOLD);
            case STATUS_LOSER -> Text.literal("Expired Losing Ticket").formatted(Formatting.GRAY);
            default -> super.getName(stack);
        };
    }

    @Override
    //? if >=1.21 {
    /*public void appendTooltip(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
    *///?} else {
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
    //?}
        if (!StackData.hasData(stack)) {
            //? if >=1.21 {
            /*super.appendTooltip(stack, context, tooltip, type);
            *///?} else {
            super.appendTooltip(stack, world, tooltip, context);
            //?}
            return;
        }

        long r = StackData.getLong(stack, K_ROUND);
        int e = StackData.getInt(stack, K_ENTRIES);
        String s = StackData.getString(stack, K_STATUS);

        tooltip.add(Text.literal("Raffle #" + r).formatted(Formatting.AQUA));
        tooltip.add(Text.literal(e + (e == 1 ? " entry" : " entries")).formatted(Formatting.WHITE));

        switch (s) {
            case STATUS_WINNER -> {
                tooltip.add(Text.empty());
                tooltip.add(Text.literal("★ WINNING TICKET ★").formatted(Formatting.GOLD, Formatting.BOLD));
                tooltip.add(Text.literal("Prize: " + StackData.getLong(stack, K_PRIZE) + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()).formatted(Formatting.YELLOW));
                tooltip.add(Text.literal("Claim at the raffle, or /raffle claim.").formatted(Formatting.GRAY));
            }
            case STATUS_LOSER -> {
                tooltip.add(Text.empty());
                tooltip.add(Text.literal("This raffle is over.").formatted(Formatting.DARK_GRAY));
                tooltip.add(Text.literal("Turn in for a discount on new entries.").formatted(Formatting.GREEN));
            }
            default -> tooltip.add(Text.literal("Active - winner not yet drawn.").formatted(Formatting.DARK_GREEN));
        }

        if (StackData.has(stack, K_OWNER_NAME)) {
            tooltip.add(Text.literal("Holder: " + StackData.getString(stack, K_OWNER_NAME)).formatted(Formatting.DARK_GRAY));
        }
        //? if >=1.21 {
        /*super.appendTooltip(stack, context, tooltip, type);
        *///?} else {
        super.appendTooltip(stack, world, tooltip, context);
        //?}
    }
}
