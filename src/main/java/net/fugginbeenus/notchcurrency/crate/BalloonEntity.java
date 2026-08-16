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
    public static final ResourceLocation LOOT = NotchCurrency.id("balloon_crate");
    private static final double BOB_AMPLITUDE = 0.30;
    private static final double BOB_PERIOD_TICKS = 14.0;
    private static final double DRIFT_SPEED = 0.004;
    private static final long DESPAWN_TICKS = 20L * 60L * 5L;
    private int animTicks = 0;
    private long spawnWorldTime = -1;

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

    //? if >=1.21 {
    /*@Override protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {}
    *///?} else {
    @Override protected void defineSynchedData() {}
    //?}

    @Override
    //? if >=1.21.11 {
    /*protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        net.minecraft.nbt.CompoundTag nbt = net.fugginbeenus.notchcurrency.compat.Nbt.readAll(in);
    *///?} else {
    protected void readAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
    //?}
        this.spawnWorldTime = nbt.contains("SpawnWorldTime") ? nbt.getLong("SpawnWorldTime") : -1;
        this.animTicks = nbt.getInt("AnimTicks");
    }

    @Override
    //? if >=1.21.11 {
    /*protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
        net.minecraft.nbt.CompoundTag nbt = new net.minecraft.nbt.CompoundTag();
    *///?} else {
    protected void addAdditionalSaveData(net.minecraft.nbt.CompoundTag nbt) {
    //?}
        nbt.putLong("SpawnWorldTime", this.spawnWorldTime);
        nbt.putInt("AnimTicks", this.animTicks);
        //? if >=1.21.11 {
        /*net.fugginbeenus.notchcurrency.compat.Nbt.copyInto(nbt, out);
        *///?}
    }

    @Override public boolean isAttackable() { return true; }
    @Override public boolean isPushable()   { return false; }
    @Override public void    push(Entity entity) {} // keep public in 1.20.1
    //? if >=1.21.11 {
    /*@Override public boolean canBeCollidedWith(Entity by) { return true; }
    *///?} else {
    @Override public boolean canBeCollidedWith() { return true; }
    //?}
    @Override public boolean isPickable()       { return true; }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) return;
        if (spawnWorldTime < 0) {
            spawnWorldTime = level().getGameTime();
        }
        long currentWorldTime = level().getGameTime();
        long age = currentWorldTime - spawnWorldTime;
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

    //? if >=1.21.11 {
    /*@Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              DamageSource source, float amount) {
    *///?} else {
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide) return true;
    //?}

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
        sw.playSound(null, blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.AMBIENT, 0.9f, 1.5f);
        sw.playSound(null, blockPosition(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, SoundSource.AMBIENT, 0.4f, 1.8f);

        for (int i = 0; i < 20; i++) {
            double spread = 0.3 + sw.random.nextDouble() * 0.3;
            double ax = (sw.random.nextDouble() - 0.5) * spread;
            double ay = (sw.random.nextDouble()) * 0.4;
            double az = (sw.random.nextDouble() - 0.5) * spread;
            sw.sendParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 1, ax, ay, az, 0.02);
        }
        sw.levelEvent(2001, blockPosition(), Block.getId(Blocks.BARREL.defaultBlockState()));
        sw.playSound(null, blockPosition(), SoundEvents.WOOD_BREAK, SoundSource.BLOCKS, 0.8f, 1.1f);

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

        if (loot.isEmpty()) {
            loot.add(new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN, 5));
        }

        final double cx = getX();
        final double cy = getY();
        final double cz = getZ();

        for (ItemStack s : loot) {
            if (s.isEmpty()) continue;
            var item = new ItemEntity(sw, cx, cy, cz, s.copy());

            double vx = (sw.random.nextDouble() - 0.5) * 0.2;
            double vy = -0.25 - sw.random.nextDouble() * 0.15;
            double vz = (sw.random.nextDouble() - 0.5) * 0.2;

            item.setDeltaMovement(vx, vy, vz);
            item.setPickUpDelay(20);
            sw.addFreshEntity(item);
        }

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