package net.fugginbeenus.notchcurrency.block.entity;

import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.fugginbeenus.notchcurrency.registry.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

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

    public static void serverTick(World world, BlockPos pos, BlockState state, LedgerBoardBlockEntity be) {
        if (--be.cooldown > 0 || world.getServer() == null) return;
        be.cooldown = REFRESH_TICKS;
        List<EconomyLeaderboard.Entry> fresh = EconomyLeaderboard.topEntries(world.getServer(), ROWS);
        if (!fresh.equals(be.rows)) {
            be.rows = fresh;
            be.markDirty();
            if (world instanceof ServerWorld sw) {
                sw.getChunkManager().markForUpdate(pos); // push the block-entity update to trackers
            }
        }
    }

    // ---- sync ----

    @Override
    //? if >=1.21 {
    /*protected void writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(nbt, registries);
    *///?} else {
    public void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
    //?}
        NbtList list = new NbtList();
        for (EconomyLeaderboard.Entry e : rows) {
            NbtCompound row = new NbtCompound();
            row.putString("n", e.name());
            row.putLong("b", e.balance());
            list.add(row);
        }
        nbt.put("Rows", list);
    }

    @Override
    //? if >=1.21 {
    /*public void readNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);
    *///?} else {
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
    //?}
        List<EconomyLeaderboard.Entry> parsed = new ArrayList<>();
        NbtList list = nbt.getList("Rows", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound row = list.getCompound(i);
            parsed.add(new EconomyLeaderboard.Entry(row.getString("n"), row.getLong("b")));
        }
        rows = parsed;
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
