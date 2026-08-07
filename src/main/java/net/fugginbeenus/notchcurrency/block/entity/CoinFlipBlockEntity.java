package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CoinFlipBlockEntity extends BlockEntity {

    private long flipStartTick = -1;
    private int revealTicks = 0;

    public CoinFlipBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COIN_FLIP, pos, state);
    }

    public long flipStartTick() { return flipStartTick; }
    public int revealTicks() { return revealTicks; }

    public void startFlip(long worldTime, int reveal) {
        this.flipStartTick = worldTime;
        this.revealTicks = reveal;
        setChanged();
        if (level instanceof ServerLevel sw) {
            sw.getChunkSource().blockChanged(worldPosition);
        }
    }

    @Override
    //? if >=1.21 {
    /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
    *///?} else {
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
    //?}
        nbt.putLong("FlipStart", flipStartTick);
        nbt.putInt("Reveal", revealTicks);
    }

    @Override
    //? if >=1.21 {
    /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
    *///?} else {
    public void load(CompoundTag nbt) {
        super.load(nbt);
    //?}
        flipStartTick = nbt.getLong("FlipStart");
        revealTicks = nbt.getInt("Reveal");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    //? if >=1.21 {
    /*public CompoundTag toInitialChunkDataNbt(net.minecraft.core.HolderLookup.Provider registries) {
        return save(registries);
    *///?} else {
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    //?}
    }
}
