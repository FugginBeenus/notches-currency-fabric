package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.block.MailboxBlock;
import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.mail.MailState;
import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MailboxBlockEntity extends BlockEntity {

    private static final String OWNER_KEY = "Owner";
    private static final String OWNER_NAME_KEY = "OwnerName";
    private static final int CHECK_EVERY_TICKS = 40;

    @Nullable
    private UUID owner;
    private String ownerName = "";
    private int sinceCheck = 0;

    public MailboxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MAILBOX, pos, state);
    }

    @Nullable
    public UUID owner() {
        return owner;
    }
    public String ownerName() {
        return ownerName;
    }
    public boolean isClaimed() {
        return owner != null;
    }
    public boolean isOwner(UUID player) {
        return owner != null && owner.equals(player);
    }

    public void claim(UUID player, String name) {
        this.owner = player;
        this.ownerName = name == null ? "" : name;
        setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, MailboxBlockEntity box) {
        if (box.owner == null || !(level instanceof ServerLevel server)) return;
        if (++box.sinceCheck < CHECK_EVERY_TICKS) return;
        box.sinceCheck = 0;

        boolean waiting = MailState.get(server.getServer()).count(box.owner) > 0;
        if (state.getValue(MailboxBlock.FLAG) != waiting) {
            level.setBlock(pos, state.setValue(MailboxBlock.FLAG, waiting),
                    net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
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
        if (owner != null) Nbt.putUuid(nbt, OWNER_KEY, owner);
        if (!ownerName.isEmpty()) nbt.putString(OWNER_NAME_KEY, ownerName);
        //? if >=1.21.11 {
        /*Nbt.copyInto(nbt, out);
        *///?}
    }

    @Override
    //? if >=1.21.11 {
    /*protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        CompoundTag nbt = Nbt.readAll(in);
    *///?} elif >=1.21 {
    /*protected void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(nbt, registries);
    *///?} else {
    public void load(CompoundTag nbt) {
        super.load(nbt);
    //?}
        owner = Nbt.hasUuid(nbt, OWNER_KEY) ? Nbt.getUuid(nbt, OWNER_KEY) : null;
        ownerName = nbt.getString(OWNER_NAME_KEY);
    }
}
