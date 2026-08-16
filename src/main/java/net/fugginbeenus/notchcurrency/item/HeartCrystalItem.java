package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.core.HeartState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

//? if <1.21.11 {
import net.minecraft.world.InteractionResultHolder;
//?}
public class HeartCrystalItem extends Item {

    public HeartCrystalItem(Properties settings) {
        super(settings.stacksTo(16));
    }

    //? if >=1.21.11 {
    /*@Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
    *///?} else {
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
    //?}
        ItemStack stack = user.getItemInHand(hand);
        if (!world.isClientSide() && user instanceof ServerPlayer sp) {
            consume(sp, stack);
        }
        //? if >=1.21.11 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResultHolder.success(stack);
        //?}
    }

    private static void consume(ServerPlayer sp, ItemStack stack) {
        HeartState state = HeartState.get(HeartState.serverOf(sp));
        if (!state.absorb(sp.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(
                            "Your heart will not hold another. That is all of them.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        HeartState.applyTo(sp);
        sp.heal(HeartState.HEALTH_PER_CRYSTAL);

        if (!sp.getAbilities().instabuild) stack.shrink(1);

        int now = state.count(sp.getUUID());
        int left = HeartState.MAX_CRYSTALS - now;
        Component tail = left == 0
                ? Component.literal(" That is all of them.").withStyle(ChatFormatting.GRAY)
                : Component.literal(" " + left + " more will fit.").withStyle(ChatFormatting.GRAY);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(
                        "The crystal sinks in. " + now + (now == 1 ? " extra heart." : " extra hearts."))
                .withStyle(ChatFormatting.LIGHT_PURPLE).append(tail));

        ServerLevel world = sp.serverLevel();
        world.playSound(null, sp.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0f, 0.7f);
        world.playSound(null, sp.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5f, 1.6f);
        world.sendParticles(ParticleTypes.HEART, sp.getX(), sp.getY() + 1.2, sp.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
    }

    //? if >=1.21.11 {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines,
                                net.minecraft.world.item.TooltipFlag type) {
        List<Component> tooltip = new java.util.ArrayList<>();
    *///?} elif >=1.21 {
    /*@Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                List<Component> tooltip, net.minecraft.world.item.TooltipFlag type) {
    *///?} else {
    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    //?}
        tooltip.add(Component.literal("Use to gain a heart.")
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.literal("Up to " + (HeartState.MAX_CRYSTALS / 10) + " extra rows. See /hearts.")
                .withStyle(ChatFormatting.DARK_GRAY));

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
