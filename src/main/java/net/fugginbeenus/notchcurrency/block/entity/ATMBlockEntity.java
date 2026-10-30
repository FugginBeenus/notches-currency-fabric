package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ATMBlockEntity extends BlockEntity {

    public ATMBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATM, pos, state);
    }
}
