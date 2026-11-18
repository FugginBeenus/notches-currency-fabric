package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.npc.faction.FactionBoards;
import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class FactionBoardBlockEntity extends BlockEntity {

    private static final int CHECK_TICKS = 100;

    private String factionId = "";
    private int cooldown = 0;

    public FactionBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FACTION_BOARD, pos, state);
    }

    public String factionId() {
        return factionId;
    }

    public void setFactionId(String id) {
        factionId = id == null ? "" : id;
        setChanged();
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, FactionBoardBlockEntity be) {
        if (--be.cooldown > 0) return;
        be.cooldown = CHECK_TICKS;
        FactionBoards.refresh(world, pos, be);
    }

    @Override
    //? if >=1.21.11 {
    /*protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        CompoundTag nbt = new CompoundTag();
    *///?} elif >=1.21 {
    /*protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(nbt, registries);
    *///?} else {
    public void saveAdditional(CompoundTag nbt) {
        super.saveAdditional(nbt);
    //?}
        nbt.putString("Faction", factionId);
        //? if >=1.21.11 {
        /*net.fugginbeenus.notchcurrency.compat.Nbt.copyInto(nbt, out);
        *///?}
    }

    @Override
    //? if >=1.21.11 {
    /*protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        CompoundTag nbt = net.fugginbeenus.notchcurrency.compat.Nbt.readAll(in);
    *///?} elif >=1.21 {
    /*public void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
    *///?} else {
    public void load(CompoundTag nbt) {
        super.load(nbt);
    //?}
        factionId = nbt.getString("Faction");
    }
}
