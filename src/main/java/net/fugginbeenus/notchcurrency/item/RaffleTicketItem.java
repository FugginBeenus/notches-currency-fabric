package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * A physical raffle ticket. It's a personal receipt: the buyer wins by identity, so the
 * ticket is proof/flavour rather than a bearer instrument. Its display is driven entirely
 * by NBT that the server restamps ({@code Status}) when the owner interacts with the raffle,
 * runs {@code /raffle}, or logs in — because items already sitting in inventories can't be
 * mutated retroactively at draw time, the status is resolved lazily on next contact.
 *
 * NBT: {@code Round} (long), {@code Entries} (int), {@code Owner} (UUID), {@code OwnerName}
 * (string), {@code Status} (ACTIVE/WINNER/LOSER), {@code Prize} (long, winner display only).
 */
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
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putLong(K_ROUND, round);
        nbt.putInt(K_ENTRIES, entries);
        nbt.putUuid(K_OWNER, owner);
        nbt.putString(K_OWNER_NAME, ownerName);
        nbt.putString(K_STATUS, STATUS_ACTIVE);
        return stack;
    }

    public static boolean isTicket(ItemStack stack) {
        return stack.isOf(ModItems.RAFFLE_TICKET) && stack.hasNbt();
    }

    public static long round(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0L : nbt.getLong(K_ROUND);
    }

    public static int entries(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? 0 : nbt.getInt(K_ENTRIES);
    }

    /** Update the entry count on an existing ticket (used when a player buys more entries). */
    public static void setEntries(ItemStack stack, int entries) {
        stack.getOrCreateNbt().putInt(K_ENTRIES, entries);
    }

    @Nullable
    public static UUID owner(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.containsUuid(K_OWNER) ? nbt.getUuid(K_OWNER) : null;
    }

    public static String status(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt == null ? STATUS_ACTIVE : nbt.getString(K_STATUS);
    }

    public static void setStatus(ItemStack stack, String status, long prize) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putString(K_STATUS, status);
        if (STATUS_WINNER.equals(status)) nbt.putLong(K_PRIZE, prize);
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
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (nbt == null) {
            super.appendTooltip(stack, world, tooltip, context);
            return;
        }

        long r = nbt.getLong(K_ROUND);
        int e = nbt.getInt(K_ENTRIES);
        String s = nbt.getString(K_STATUS);

        tooltip.add(Text.literal("Raffle #" + r).formatted(Formatting.AQUA));
        tooltip.add(Text.literal(e + (e == 1 ? " entry" : " entries")).formatted(Formatting.WHITE));

        switch (s) {
            case STATUS_WINNER -> {
                tooltip.add(Text.empty());
                tooltip.add(Text.literal("★ WINNING TICKET ★").formatted(Formatting.GOLD, Formatting.BOLD));
                tooltip.add(Text.literal("Prize: " + nbt.getLong(K_PRIZE) + " coins").formatted(Formatting.YELLOW));
                tooltip.add(Text.literal("Claim at the raffle, or /raffle claim.").formatted(Formatting.GRAY));
            }
            case STATUS_LOSER -> {
                tooltip.add(Text.empty());
                tooltip.add(Text.literal("This raffle is over.").formatted(Formatting.DARK_GRAY));
                tooltip.add(Text.literal("Turn in for a discount on new entries.").formatted(Formatting.GREEN));
            }
            default -> tooltip.add(Text.literal("Active — winner not yet drawn.").formatted(Formatting.DARK_GREEN));
        }

        if (nbt.contains(K_OWNER_NAME)) {
            tooltip.add(Text.literal("Holder: " + nbt.getString(K_OWNER_NAME)).formatted(Formatting.DARK_GRAY));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
