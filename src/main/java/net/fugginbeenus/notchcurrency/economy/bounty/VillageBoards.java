package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.block.BountyBoardBlock;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class VillageBoards {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Villages");
    private static final long LIVED_IN_TICKS = 12_000L;
    private static final int GIVE_UP_TICKS = 1_200;
    private static final int REACH = 9;
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final List<int[]> SPOTS = spots();
    private static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, NotchCurrency.id("village_board_ground"));
    private static final Map<Bell, Integer> PENDING = new LinkedHashMap<>();

    private record Bell(ResourceKey<Level> level, long pos) {}

    private VillageBoards() {}

    public static void init() {
        //? if >=26.1 {
        /*net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                (level, chunk, newlyGenerated) -> onChunkLoad(level, chunk));
        *///?} else {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register(
                VillageBoards::onChunkLoad);
        //?}
        ServerTickEvents.END_SERVER_TICK.register(VillageBoards::tick);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { synchronized (PENDING) { PENDING.clear(); } });
    }

    private static boolean enabled() {
        var bounty = NotchConfigIO.get().bounty;
        return bounty.enabled && bounty.villageBoards;
    }

    private static void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        if (!enabled() || chunk.getInhabitedTime() > LIVED_IN_TICKS) return;
        for (BlockPos pos : chunk.getBlockEntitiesPos()) {
            if (!chunk.getBlockState(pos).is(Blocks.BELL)) continue;
            synchronized (PENDING) {
                PENDING.putIfAbsent(new Bell(level.dimension(), pos.asLong()), level.getServer().getTickCount());
            }
        }
    }

    private static void tick(MinecraftServer server) {
        if (server.getTickCount() % 20 != 0) return;
        List<Bell> due;
        synchronized (PENDING) {
            if (PENDING.isEmpty()) return;
            due = new ArrayList<>(PENDING.keySet());
        }
        for (Bell bell : due) {
            boolean done = true;
            ServerLevel level = server.getLevel(bell.level());
            BlockPos pos = BlockPos.of(bell.pos());
            if (level != null && enabled() && level.hasChunkAt(pos)) {
                if (areaLoaded(level, pos)) {
                    try {
                        visit(level, pos);
                    } catch (RuntimeException ex) {
                        LOGGER.warn("Could not put a bounty board by the bell at {}", pos, ex);
                    }
                } else {
                    Integer since;
                    synchronized (PENDING) { since = PENDING.get(bell); }
                    done = since == null || server.getTickCount() - since > GIVE_UP_TICKS;
                }
            }
            if (done) {
                synchronized (PENDING) { PENDING.remove(bell); }
            }
        }
    }

    private static boolean areaLoaded(ServerLevel level, BlockPos bell) {
        int r = REACH + 1;
        for (int cx = (bell.getX() - r) >> 4; cx <= (bell.getX() + r) >> 4; cx++) {
            for (int cz = (bell.getZ() - r) >> 4; cz <= (bell.getZ() + r) >> 4; cz++) {
                if (!level.hasChunkAt(new BlockPos(cx << 4, bell.getY(), cz << 4))) return false;
            }
        }
        return true;
    }

    private static void visit(ServerLevel level, BlockPos bell) {
        if (!level.getBlockState(bell).is(Blocks.BELL)) return;
        StructureStart village = villageAt(level, bell);
        if (village == null) return;
        if (!VillageBoardState.get(level).claim(chunkKey(village))) return;
        if (boardNear(level, bell)) return;
        BlockPos spot = findSpot(level, bell);
        if (spot == null) return;
        BlockState lower = ModBlocks.BOUNTY_BOARD.defaultBlockState()
                .setValue(BountyBoardBlock.FACING, facingToward(spot, bell))
                .setValue(BountyBoardBlock.HALF, DoubleBlockHalf.LOWER);
        level.setBlock(spot, lower, Block.UPDATE_CLIENTS);
        level.setBlock(spot.above(), lower.setValue(BountyBoardBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        BountyManager.ensurePopulated(level.getServer());
        LOGGER.debug("Put a bounty board in a village at {} {} {}", spot.getX(), spot.getY(), spot.getZ());
    }

    @Nullable
    private static StructureStart villageAt(ServerLevel level, BlockPos pos) {
        StructureStart tagged = level.structureManager().getStructureWithPieceAt(pos, StructureTags.VILLAGE);
        if (tagged.isValid()) return tagged;
        Registry<Structure> structures = level.registryAccess().registryOrThrow(Registries.STRUCTURE);
        for (Structure structure : level.structureManager().getAllStructuresAt(pos).keySet()) {
            var id = structures.getKey(structure);
            if (id == null || !id.getPath().contains("village")) continue;
            StructureStart start = pieceAt(level, pos, structure);
            if (start.isValid()) return start;
        }
        return null;
    }

    private static StructureStart pieceAt(ServerLevel level, BlockPos pos, Structure structure) {
        //? if >=26.3 {
        /*return level.structureManager().getStructureWithPieceAt(pos.getX(), pos.getY(), pos.getZ(), structure);
        *///?} else {
        return level.structureManager().getStructureWithPieceAt(pos, structure);
        //?}
    }

    private static long chunkKey(StructureStart start) {
        //? if >=26.1 {
        /*return start.getChunkPos().pack();
        *///?} else {
        return start.getChunkPos().toLong();
        //?}
    }

    private static boolean boardNear(ServerLevel level, BlockPos bell) {
        for (BlockPos pos : BlockPos.betweenClosed(bell.offset(-REACH, -4, -REACH), bell.offset(REACH, 4, REACH))) {
            if (level.getBlockState(pos).getBlock() instanceof BountyBoardBlock) return true;
        }
        return false;
    }

    @Nullable
    private static BlockPos findSpot(ServerLevel level, BlockPos bell) {
        for (int[] offset : SPOTS) {
            int x = bell.getX() + offset[0];
            int z = bell.getZ() + offset[1];
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (y < bell.getY() - 4 || y > bell.getY() + 1) continue;
            if (level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) > y) continue;
            BlockPos spot = new BlockPos(x, y, z);
            if (fits(level, spot, facingToward(spot, bell))) return spot;
        }
        return null;
    }

    private static boolean fits(ServerLevel level, BlockPos spot, Direction facing) {
        BlockPos floor = spot.below();
        BlockState ground = level.getBlockState(floor);
        if (!ground.is(GROUND) || !ground.isFaceSturdy(level, floor, Direction.UP)) return false;
        BlockPos under = floor.below();
        if (!level.getBlockState(under).isFaceSturdy(level, under, Direction.UP)) return false;
        for (Direction side : SIDES) {
            if (!level.getFluidState(floor.relative(side)).isEmpty()) return false;
        }
        for (BlockPos part : new BlockPos[]{spot, spot.above()}) {
            BlockState here = level.getBlockState(part);
            if (!here.canBeReplaced() || !here.getFluidState().isEmpty()) return false;
            BlockPos front = part.relative(facing);
            if (!level.getBlockState(front).getCollisionShape(level, front).isEmpty()) return false;
            for (Direction side : SIDES) {
                BlockState next = level.getBlockState(part.relative(side));
                if (next.is(BlockTags.DOORS) || next.is(BlockTags.FENCE_GATES) || next.is(Blocks.BELL)) return false;
            }
        }
        return true;
    }

    private static Direction facingToward(BlockPos from, BlockPos to) {
        int dx = to.getX() - from.getX();
        int dz = to.getZ() - from.getZ();
        if (Math.abs(dx) >= Math.abs(dz)) return dx > 0 ? Direction.EAST : Direction.WEST;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    private static List<int[]> spots() {
        List<int[]> out = new ArrayList<>();
        for (int r : new int[]{3, 4, 2, 5, 6, 7, 8}) {
            List<int[]> ring = new ArrayList<>();
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) ring.add(new int[]{dx, dz});
                }
            }
            ring.sort(Comparator.comparingInt(o -> Math.min(Math.abs(o[0]), Math.abs(o[1]))));
            out.addAll(ring);
        }
        return out;
    }
}
