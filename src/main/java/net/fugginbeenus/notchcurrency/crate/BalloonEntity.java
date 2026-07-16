package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameterSet;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.entity.ItemEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class BalloonEntity extends Entity {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");

    // Loot table: data/notchcurrency/loot_tables/balloon_crate.json
    public static final Identifier LOOT = NotchCurrency.id("balloon_crate");

    // ---- Motion tuning ----
    private static final double BOB_AMPLITUDE = 0.30;
    private static final double BOB_PERIOD_TICKS = 14.0; // bigger = slower
    private static final double DRIFT_SPEED = 0.004;
    private static final long DESPAWN_TICKS = 20L * 60L * 5L; // 5 minutes

    private int animTicks = 0;  // For bobbing animation only
    private long spawnWorldTime = -1;  // World time when spawned (persists through chunk unloads)

    public BalloonEntity(EntityType<? extends BalloonEntity> type, World world) {
        super(type, world);
        this.noClip = false;
        this.setNoGravity(true);
    }

    public BalloonEntity(World world, double x, double y, double z) {
        this(ModEntities.BALLOON, world);
        this.setPos(x, y, z);
        this.setVelocity(Vec3d.ZERO);
    }

    // NBT
    @Override protected void initDataTracker() {}

    @Override
    protected void readCustomDataFromNbt(net.minecraft.nbt.NbtCompound nbt) {
        // If SpawnWorldTime is missing (old balloon), it will be 0, and we'll re-init in tick()
        this.spawnWorldTime = nbt.contains("SpawnWorldTime") ? nbt.getLong("SpawnWorldTime") : -1;
        this.animTicks = nbt.getInt("AnimTicks");
    }

    @Override
    protected void writeCustomDataToNbt(net.minecraft.nbt.NbtCompound nbt) {
        nbt.putLong("SpawnWorldTime", this.spawnWorldTime);
        nbt.putInt("AnimTicks", this.animTicks);
    }

    // Targetable
    @Override public boolean isAttackable() { return true; }
    @Override public boolean isPushable()   { return false; }
    @Override public void    pushAwayFrom(Entity entity) {} // keep public in 1.20.1
    @Override public boolean isCollidable() { return true; }
    @Override public boolean canHit()       { return true; }

    @Override
    public void tick() {
        super.tick();
        if (this.getWorld().isClient) return;

        // Initialize spawn time on first tick
        if (spawnWorldTime < 0) {
            spawnWorldTime = getWorld().getTime();
        }

        // Check despawn based on world time (works even if chunk was unloaded)
        long currentWorldTime = getWorld().getTime();
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
        setPosition(getX() + dx, getY() + bob, getZ() + dz);

        if (animTicks % 10 == 0) {
            ((ServerWorld) getWorld()).spawnParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.4, getZ(), 1, 0.1, 0.1, 0.1, 0.0
            );
        }
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (getWorld().isClient) return true;

        final Entity src = source.getSource();
        final boolean fromProjectile  = src instanceof net.minecraft.entity.projectile.ProjectileEntity;
        final boolean fromPlayerMelee = source.getAttacker() != null && source.getAttacker().isPlayer();

        if (!fromProjectile && !fromPlayerMelee) {
            return false; // ignore e.g. cactus/environmental
        }

        popAndRainLoot();
        return true;
    }

    private void popAndRainLoot() {
        ServerWorld sw = (ServerWorld) getWorld();

        // 1) Sounds + burst FX
        sw.playSound(null, getBlockPos(), SoundEvents.ENTITY_FIREWORK_ROCKET_BLAST, SoundCategory.AMBIENT, 0.9f, 1.5f);
        sw.playSound(null, getBlockPos(), SoundEvents.BLOCK_BUBBLE_COLUMN_UPWARDS_INSIDE, SoundCategory.AMBIENT, 0.4f, 1.8f);

        for (int i = 0; i < 20; i++) {
            double spread = 0.3 + sw.random.nextDouble() * 0.3;
            double ax = (sw.random.nextDouble() - 0.5) * spread;
            double ay = (sw.random.nextDouble()) * 0.4;
            double az = (sw.random.nextDouble() - 0.5) * spread;
            sw.spawnParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 1, ax, ay, az, 0.02);
        }

        // Barrel "shatter" visual (block break event)
        sw.syncWorldEvent(2001, getBlockPos(), Block.getRawIdFromState(Blocks.BARREL.getDefaultState()));
        sw.playSound(null, getBlockPos(), SoundEvents.BLOCK_WOOD_BREAK, SoundCategory.BLOCKS, 0.8f, 1.1f);

        // 2) Roll loot table
        //? if >=1.21 {
        /*LootTable table = sw.getServer().getReloadableRegistries().getLootTable(
                net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.LOOT_TABLE, LOOT));
        *///?} else {
        LootTable table = sw.getServer().getLootManager().getLootTable(LOOT);
        //?}
        LootContextParameterSet ctx = new LootContextParameterSet.Builder(sw)
                .add(LootContextParameters.ORIGIN, this.getPos())
                .add(LootContextParameters.THIS_ENTITY, this)
                .build(LootContextTypes.GIFT);

        java.util.List<ItemStack> loot = table.generateLoot(ctx);

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

            item.setVelocity(vx, vy, vz);
            item.setPickupDelay(20); // brief delay so it visibly falls before pickup
            sw.spawnEntity(item);
        }

        // 4) Remove the balloon entity
        discard();
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}