package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

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
        markDirty();
        if (world instanceof ServerWorld sw) {
            sw.getChunkManager().markForUpdate(pos);
        }
    }

    @Override
    //? if >=1.21 {
    /*protected void writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
    *///?} else {
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
    //?}
        nbt.putLong("FlipStart", flipStartTick);
        nbt.putInt("Reveal", revealTicks);
    }

    @Override
    //? if >=1.21 {
    /*public void readNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
    *///?} else {
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    //?}
        flipStartTick = nbt.getLong("FlipStart");
        revealTicks = nbt.getInt("Reveal");
    }

    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    //? if >=1.21 {
    /*public NbtCompound toInitialChunkDataNbt(net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        return createNbt(registries);
    *///?} else {
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    //?}
    }
}
