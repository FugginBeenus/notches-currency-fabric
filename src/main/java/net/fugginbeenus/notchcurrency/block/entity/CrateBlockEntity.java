package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.block.CrateBlock;
import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.animatable.GeoBlockEntity;

//? if >=1.21.5 {
/*import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.animation.object.PlayState;
*///?} elif >=1.21 {
/*import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
*///?} else {
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
//?}

public class CrateBlockEntity extends BlockEntity implements GeoBlockEntity {

    public static final String OPEN_CLIP = "open";

    private final AnimatableInstanceCache geoCache =
            net.fugginbeenus.notchcurrency.compat.Geo.blockCache(this);

    public CrateBlockEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type,
                            BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public String crateTexture() {
        return getBlockState().getBlock() instanceof CrateBlock crate ? crate.crateType() : "common";
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        //? if >=1.21.5 {
        /*controllers.add(new AnimationController<CrateBlockEntity>("lid", 0, this::lidPredicate));
        *///?} else {
        controllers.add(new AnimationController<>(this, "lid", 0, this::lidPredicate));
        //?}
    }

    //? if >=1.21.5 {
    /*private PlayState lidPredicate(AnimationTest<CrateBlockEntity> state) {
    *///?} else {
    private <E extends CrateBlockEntity> PlayState lidPredicate(AnimationState<E> state) {
    //?}
        if (!getBlockState().hasProperty(CrateBlock.OPEN) || !getBlockState().getValue(CrateBlock.OPEN)) {
            //? if >=1.21.5 {
            /*state.controller().reset();
            *///?} else {
            state.getController().forceAnimationReset();
            //?}
            return PlayState.STOP;
        }
        state.setAndContinue(net.fugginbeenus.notchcurrency.compat.Geo.play(OPEN_CLIP));
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
