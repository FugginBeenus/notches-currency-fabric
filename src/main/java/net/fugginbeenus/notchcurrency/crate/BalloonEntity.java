package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BalloonEntity extends Entity {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");

    // Loot table: data/notchcurrency/loot_tables/balloon_crate.json
    public static final ResourceLocation LOOT = NotchCurrency.id("balloon_crate");

    // ---- Motion tuning ----
    private static final double BOB_AMPLITUDE = 0.30;
    private static final double BOB_PERIOD_TICKS = 14.0; // bigger = slower
    private static final double DRIFT_SPEED = 0.004;
    private static final long DESPAWN_TICKS = 20L * 60L * 5L; // 5 minutes

    private int animTicks = 0;  // For bobbing animation only
    private long spawnWorldTime = -1;  // Level time when spawned (persists through chunk unloads)

    public BalloonEntity(EntityType<? extends BalloonEntity> type, Level world) {
        super(type, world);
        this.noPhysics = false;
        this.setNoGravity(true);
    }

    public BalloonEntity(Level world, double x, double y, double z) {
        this(ModEntities.BALLOON, world);
        this.setPosRaw(x, y, z);
        this.setDeltaMovement(Vec3.ZERO);
    }

    // NBT
    //? if >=1.21 {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    *///?} else {
    @Override protected void defineSynchedData() {}
    //?}

    @Override
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        // If SpawnWorldTime is missing (old balloon), it will be 0, and we'll re-init in tick()
        this.spawnWorldTime = nbt.contains("SpawnWorldTime") ? nbt.getLong("SpawnWorldTime") : -1;
        this.animTicks = nbt.getInt("AnimTicks");
    }

    @Override
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
        nbt.putLong("SpawnWorldTime", this.spawnWorldTime);
        nbt.putInt("AnimTicks", this.animTicks);
    }

    // Targetable
    @Override public boolean isAttackable() { return true; }
    @Override public boolean isPushable()   { return false; }
    @Override public void    push(Entity entity) {} // keep public in 1.20.1
    @Override public boolean canBeCollidedWith() { return true; }
    @Override public boolean isPickable()       { return true; }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;

        // Initialize spawn time on first tick
        if (spawnWorldTime < 0) {
            spawnWorldTime = level().getGameTime();
        }

        // Check despawn based on world time (works even if chunk was unloaded)
        long currentWorldTime = level().getGameTime();
        long age = currentWorldTime - spawnWorldTime;

        // Debug logging every 30 seconds (600 ticks)
        if (animTicks % 600 == 0) {
            LOGGER.debug("[Balloon] ID={} age={}/{} ticks ({}/{} seconds)",
                    getId(), age, DESPAWN_TICKS, age / 20, DESPAWN_TICKS / 20);
        }

        if (age > DESPAWN_TICKS) {
            LOGGER.debug("[Balloon] ID={} despawning after {} seconds", getId(), age / 20);
            discard();
            return;
        }

        animTicks++;

        // Bob + gentle drift (deterministic per id)
        double bob = Math.sin(animTicks / BOB_PERIOD_TICKS) * (BOB_AMPLITUDE / BOB_PERIOD_TICKS);
        double dx = ((getId() & 1) == 0 ? DRIFT_SPEED : -DRIFT_SPEED);
        double dz = ((getId() % 3) == 0 ? -DRIFT_SPEED : DRIFT_SPEED);
        setPos(getX() + dx, getY() + bob, getZ() + dz);

        if (animTicks % 10 == 0) {
            ((ServerLevel) level()).sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.4, getZ(), 1, 0.1, 0.1, 0.1, 0.0
            );
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return true;

        final Entity src = source.getDirectEntity();
        final boolean fromProjectile  = src instanceof net.minecraft.world.entity.projectile.Projectile;
        final boolean fromPlayerMelee = source.getEntity() != null && source.getEntity().isAlwaysTicking();

        if (!fromProjectile && !fromPlayerMelee) {
            return false; // ignore e.g. cactus/environmental
        }

        popAndRainLoot();
        return true;
    }

    private void popAndRainLoot() {
        ServerLevel sw = (ServerLevel) level();

        // 1) Sounds + burst FX
        sw.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 0.9f, 1.5f);
        sw.playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.AMBIENT, 0.4f, 1.8f);

        for (int i = 0; i < 20; i++) {
            double spread = 0.3 + sw.random.nextDouble() * 0.3;
            double ax = (sw.random.nextDouble() - 0.5) * spread;
            double ay = (sw.random.nextDouble()) * 0.4;
            double az = (sw.random.nextDouble() - 0.5) * spread;
            sw.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 1, ax, ay, az, 0.02);
        }

        // Barrel "shatter" visual (block break event)
        sw.levelEvent(2001, blockPosition(), Block.getId(Blocks.BARREL.defaultBlockState()));
        sw.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 1.1f);

        // 2) Roll loot table
        //? if >=1.21 {
        /*LootTable table = sw.getServer().reloadableRegistries().getLootTable(
                net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.LOOT_TABLE, LOOT));
        *///?} else {
        LootTable table = sw.getServer().getLootData().getLootTable(LOOT);
        //?}
        LootParams ctx = new LootParams.Builder(sw)
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.GIFT);

        java.util.List<ItemStack> loot = table.getRandomItems(ctx);

        // Fallback so you SEE something if the table is empty/missing
        if (loot.isEmpty()) {
            loot.add(new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN, 5));
        }

        // 3) "Loot rain" – spawn items with downward + slight outward velocity
        final double cx = getX();
        final double cy = getY();
        final double cz = getZ();

        for (ItemStack s : loot) {
            if (s.isEmpty()) continue;
            var item = new ItemEntity(sw, cx, cy, cz, s.copy());

            double vx = (sw.random.nextDouble() - 0.5) * 0.2; // small horizontal spread
            double vy = -0.25 - sw.random.nextDouble() * 0.15; // falling down
            double vz = (sw.random.nextDouble() - 0.5) * 0.2;

            item.setDeltaMovement(vx, vy, vz);
            item.setPickUpDelay(20); // brief delay so it visibly falls before pickup
            sw.addFreshEntity(item);
        }

        // 4) Remove the balloon entity
        discard();
    }

    @Override
    //? if >=1.21 {
    /*public Packet<ClientGamePacketListener> getAddEntityPacket(net.minecraft.server.level.ServerEntity entry) {
        return new ClientboundAddEntityPacket(this, entry);
    }
    *///?} else {
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return new ClientboundAddEntityPacket(this);
    }
    //?}
}