package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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

    public RaffleTicketItem(Properties settings) {
        super(settings.stacksTo(1)); // one item per purchase; never merge entry counts
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
        return stack.is(ModItems.RAFFLE_TICKET) && StackData.hasData(stack);
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
    public Component getName(ItemStack stack) {
        return switch (status(stack)) {
            case STATUS_WINNER -> Component.literal("Winning Raffle Ticket").withStyle(ChatFormatting.GOLD);
            case STATUS_LOSER -> Component.literal("Expired Losing Ticket").withStyle(ChatFormatting.GRAY);
            default -> super.getName(stack);
        };
    }

    @Override
    // 1.21.11 feeds the lines to a consumer rather than filling a list. The body below still builds
    // a list, which is handed over in one go, ahead of whatever the superclass adds.
    //? if >=1.21.11 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines,
                                net.minecraft.world.item.TooltipFlag type) {
        List<Component> tooltip = new java.util.ArrayList<>();
    *///?} elif >=1.21 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag type) {
    *///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    //?}
        if (!StackData.hasData(stack)) {
            //? if >=1.21.11 {
            /*tooltip.forEach(lines);
            super.appendHoverText(stack, context, display, lines, type);
            *///?} elif >=1.21 {
            /*super.appendHoverText(stack, context, tooltip, type);
            *///?} else {
            super.appendHoverText(stack, world, tooltip, context);
            //?}
            return;
        }

        long r = StackData.getLong(stack, K_ROUND);
        int e = StackData.getInt(stack, K_ENTRIES);
        String s = StackData.getString(stack, K_STATUS);

        tooltip.add(Component.literal("Raffle #" + r).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal(e + (e == 1 ? " entry" : " entries")).withStyle(ChatFormatting.WHITE));

        switch (s) {
            case STATUS_WINNER -> {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("★ WINNING TICKET ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                tooltip.add(Component.literal("Prize: " + StackData.getLong(stack, K_PRIZE) + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()).withStyle(ChatFormatting.YELLOW));
                tooltip.add(Component.literal("Claim at the raffle, or /raffle claim.").withStyle(ChatFormatting.GRAY));
            }
            case STATUS_LOSER -> {
                tooltip.add(Component.empty());
                tooltip.add(Component.literal("This raffle is over.").withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.literal("Turn in for a discount on new entries.").withStyle(ChatFormatting.GREEN));
            }
            default -> tooltip.add(Component.literal("Active - winner not yet drawn.").withStyle(ChatFormatting.DARK_GREEN));
        }

        if (StackData.has(stack, K_OWNER_NAME)) {
            tooltip.add(Component.literal("Holder: " + StackData.getString(stack, K_OWNER_NAME)).withStyle(ChatFormatting.DARK_GRAY));
        }
        //? if >=1.21.11 {
        /*tooltip.forEach(lines);
        super.appendHoverText(stack, context, display, lines, type);
        *///?} elif >=1.21 {
        /*super.appendHoverText(stack, context, tooltip, type);
        *///?} else {
        super.appendHoverText(stack, world, tooltip, context);
        //?}
    }
}
