package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.CurrencyText;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.mail.MailItem;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * A wrapped parcel: what a piece of mail looks like once it is in your hands.
 *
 * <p>Making mail an item rather than a row in a screen is what lets the mailbox be a plain grid of
 * slots. A parcel says who sent it and what is in it on its own tooltip, so the screen does not
 * need a column explaining the thing next to it, and it can be carried off, stored in a chest or
 * handed to somebody else before it is opened.
 *
 * <p>It stacks to one. That is not only flavour: a stack of two could be split by a right click,
 * and half a parcel is not a thing.
 */
public class ParcelItem extends Item {

    private static final String K_SENDER = "Sender";
    private static final String K_NOTE = "Note";
    private static final String K_COINS = "Coins";
    private static final String K_COUNT = "N";
    private static final String K_ITEM = "I";

    public ParcelItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    // ---- wrapping and unwrapping ----

    /** Wraps a waiting entry up as something the player can pick out of a slot. */
    public static ItemStack of(MailItem mail) {
        ItemStack stack = new ItemStack(ModItems.PARCEL);
        StackData.putString(stack, K_SENDER, mail.sender());
        if (!mail.note().isEmpty()) StackData.putString(stack, K_NOTE, mail.note());
        if (mail.coins() > 0L) StackData.putLong(stack, K_COINS, mail.coins());
        StackData.putInt(stack, K_COUNT, mail.contents().size());
        for (int i = 0; i < mail.contents().size(); i++) {
            StackData.putCompound(stack, K_ITEM + i,
                    StackData.writePortableStack(mail.contents().get(i)));
        }
        return stack;
    }

    public static boolean isParcel(ItemStack stack) {
        return stack.is(ModItems.PARCEL) && StackData.hasData(stack);
    }

    public static String sender(ItemStack stack) {
        return StackData.has(stack, K_SENDER) ? StackData.getString(stack, K_SENDER) : "";
    }

    public static String note(ItemStack stack) {
        return StackData.has(stack, K_NOTE) ? StackData.getString(stack, K_NOTE) : "";
    }

    public static long coins(ItemStack stack) {
        return StackData.has(stack, K_COINS) ? StackData.getLong(stack, K_COINS) : 0L;
    }

    public static List<ItemStack> contents(ItemStack stack) {
        List<ItemStack> out = new ArrayList<>();
        int count = StackData.has(stack, K_COUNT) ? StackData.getInt(stack, K_COUNT) : 0;
        for (int i = 0; i < count; i++) {
            if (!StackData.has(stack, K_ITEM + i)) continue;
            CompoundTag nbt = StackData.getCompound(stack, K_ITEM + i);
            ItemStack inside = StackData.readPortableStack(nbt);
            if (!inside.isEmpty()) out.add(inside);
        }
        return out;
    }

    /** Rewrites what is still inside, for a parcel an inventory could only half empty. */
    private static void setContents(ItemStack stack, List<ItemStack> left) {
        int old = StackData.has(stack, K_COUNT) ? StackData.getInt(stack, K_COUNT) : 0;
        for (int i = 0; i < old; i++) StackData.remove(stack, K_ITEM + i);
        StackData.putInt(stack, K_COUNT, left.size());
        for (int i = 0; i < left.size(); i++) {
            StackData.putCompound(stack, K_ITEM + i, StackData.writePortableStack(left.get(i)));
        }
    }

    // ---- opening ----

    //? if >=1.21.11 {
    /*@Override
    public net.minecraft.world.InteractionResult use(Level world, Player user, InteractionHand hand) {
    *///?} else {
    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(Level world, Player user,
                                                                      InteractionHand hand) {
    //?}
        ItemStack held = user.getItemInHand(hand);
        if (!world.isClientSide && user instanceof ServerPlayer sp && isParcel(held)) {
            unwrap(sp, held);
        }
        //? if >=1.21.11 {
        /*return net.minecraft.world.InteractionResult.SUCCESS;
        *///?} else {
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(held, world.isClientSide);
        //?}
    }

    /**
     * Tips the parcel out.
     *
     * <p>Coins first, because money always fits and goods might not. What will not fit stays in the
     * parcel, so a full inventory costs the player a second right click rather than the contents.
     */
    public static void unwrap(ServerPlayer player, ItemStack parcel) {
        String from = sender(parcel);
        long coins = coins(parcel);
        if (coins > 0L) {
            BalanceStore.add(player, coins, TransactionReason.AUCTION, "opened a parcel");
            NotchPackets.sendBalance(player, BalanceStore.get(player));
            StackData.remove(parcel, K_COINS);
            Msg.chat(player, Component.literal("Opened a parcel: ")
                    .append(Component.literal(coins + " " + CurrencyText.word())
                            .withStyle(ChatFormatting.GOLD))
                    .append(Component.literal(from.isEmpty() ? "." : " from " + from + ".")
                            .withStyle(ChatFormatting.GREEN)));
        }

        List<ItemStack> left = new ArrayList<>();
        int given = 0;
        for (ItemStack inside : contents(parcel)) {
            ItemStack giving = inside.copy();
            player.getInventory().add(giving);
            if (giving.isEmpty()) given++;
            else left.add(giving);
        }
        setContents(parcel, left);

        if (given > 0) {
            Msg.chat(player, Component.literal("Opened a parcel: ")
                    .append(Component.literal(given + (given == 1 ? " item" : " items"))
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(from.isEmpty() ? "." : " from " + from + ".")
                            .withStyle(ChatFormatting.GREEN)));
        }

        if (!left.isEmpty()) {
            Msg.chat(player, Component.literal("Your inventory is full. The rest is still wrapped up.")
                    .withStyle(ChatFormatting.RED));
            player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            player.inventoryMenu.broadcastChanges();
            return;
        }

        // Nothing left inside, so the wrapping goes too.
        parcel.shrink(1);
        player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.0F);
        player.inventoryMenu.broadcastChanges();
    }

    // ---- display ----

    @Override
    public Component getName(ItemStack stack) {
        String from = sender(stack);
        if (from.isEmpty()) return super.getName(stack);
        return Component.literal("Parcel from " + from).withStyle(ChatFormatting.YELLOW);
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
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip,
                                TooltipFlag context) {
    //?}
        if (isParcel(stack)) {
            String note = note(stack);
            if (!note.isEmpty()) {
                tooltip.add(Component.literal("\"" + note + "\"")
                        .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
            }

            long coins = coins(stack);
            if (coins > 0L) {
                tooltip.add(Component.literal(coins + " " + CurrencyText.word())
                        .withStyle(ChatFormatting.GOLD));
            }

            List<ItemStack> inside = contents(stack);
            for (ItemStack item : inside) {
                tooltip.add(Component.literal(item.getCount() + "x ")
                        .withStyle(ChatFormatting.DARK_GRAY)
                        .append(item.getHoverName().copy().withStyle(ChatFormatting.WHITE)));
            }
            if (inside.isEmpty() && coins <= 0L) {
                tooltip.add(Component.literal("Empty.").withStyle(ChatFormatting.DARK_GRAY));
            }

            tooltip.add(Component.literal("Right-click to open.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Nothing wrapped up in here.")
                    .withStyle(ChatFormatting.DARK_GRAY));
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
