package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public class LedgerBoardBlockEntity extends BlockEntity {

    public static final int ROWS = 6;
    private static final int REFRESH_TICKS = 40;

    private List<EconomyLeaderboard.Entry> rows = new ArrayList<>();
    private int cooldown = 0;

    public LedgerBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LEDGER_BOARD, pos, state);
    }

    public List<EconomyLeaderboard.Entry> rows() {
        return rows;
    }

    public static void serverTick(Level world, BlockPos pos, BlockState state, LedgerBoardBlockEntity be) {
        if (--be.cooldown > 0 || world.getServer() == null) return;
        be.cooldown = REFRESH_TICKS;
        List<EconomyLeaderboard.Entry> fresh = EconomyLeaderboard.topEntries(world.getServer(), ROWS);
        if (!fresh.equals(be.rows)) {
            be.rows = fresh;
            be.setChanged();
            if (world instanceof ServerLevel sw) {
                sw.getChunkSource().blockChanged(pos); // push the block-entity update to trackers
            }
        }
    }

    // ---- sync ----

    // 1.21.11 swapped the tag for a write view. The body below still fills a tag, which Nbt then
    // copies across key for key, so what lands on disk is the same either way.
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
        ListTag list = new ListTag();
        for (EconomyLeaderboard.Entry e : rows) {
            CompoundTag row = new CompoundTag();
            row.putString("n", e.name());
            row.putLong("b", e.balance());
            list.add(row);
        }
        nbt.put("Rows", list);
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
        List<EconomyLeaderboard.Entry> parsed = new ArrayList<>();
        ListTag list = nbt.getList("Rows", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag row = list.getCompound(i);
            parsed.add(new EconomyLeaderboard.Entry(row.getString("n"), row.getLong("b")));
        }
        rows = parsed;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    //? if >=1.21 {
    /*public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    *///?} else {
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    //?}
    }
}
