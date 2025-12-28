package net.fugginbeenus.notchcurrency.entity;

import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.Arm;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * Custom Shopkeeper NPC entity.
 * Replaces EasyNPC dependency with our own lightweight humanoid entity.
 */
public class ShopkeeperEntity extends LivingEntity {

    // Synced data for skin
    private static final TrackedData<String> SKIN_TYPE = DataTracker.registerData(ShopkeeperEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_VALUE = DataTracker.registerData(ShopkeeperEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Optional<UUID>> OWNER_UUID = DataTracker.registerData(ShopkeeperEntity.class, TrackedDataHandlerRegistry.OPTIONAL_UUID);
    private static final TrackedData<Boolean> SLIM_ARMS = DataTracker.registerData(ShopkeeperEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    // Skin types
    public static final String SKIN_PRESET = "preset";    // Uses preset texture (1-12)
    public static final String SKIN_URL = "url";          // Custom URL skin
    public static final String SKIN_PLAYER = "player";    // Player username skin

    // Armor slots (not used, but required by LivingEntity)
    private final DefaultedList<ItemStack> armorItems = DefaultedList.ofSize(4, ItemStack.EMPTY);

    public ShopkeeperEntity(EntityType<? extends ShopkeeperEntity> entityType, World world) {
        super(entityType, world);
        this.setNoGravity(false);
        this.setInvulnerable(false); // Can be killed, but we handle drops ourselves
    }

    public ShopkeeperEntity(World world, double x, double y, double z) {
        this(ModEntities.SHOPKEEPER, world);
        this.setPosition(x, y, z);
    }

    public static DefaultAttributeContainer.Builder createShopkeeperAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.0) // Stationary
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0); // Can't be pushed
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(SKIN_TYPE, SKIN_PRESET);
        this.dataTracker.startTracking(SKIN_VALUE, "1"); // Default to preset 1
        this.dataTracker.startTracking(OWNER_UUID, Optional.empty());
        this.dataTracker.startTracking(SLIM_ARMS, false);
    }

    // ========== SKIN MANAGEMENT ==========

    public String getSkinType() {
        return this.dataTracker.get(SKIN_TYPE);
    }

    public String getSkinValue() {
        return this.dataTracker.get(SKIN_VALUE);
    }

    public void setPresetSkin(int presetNumber) {
        this.dataTracker.set(SKIN_TYPE, SKIN_PRESET);
        this.dataTracker.set(SKIN_VALUE, String.valueOf(Math.max(1, Math.min(12, presetNumber))));
    }

    public void setUrlSkin(String url) {
        this.dataTracker.set(SKIN_TYPE, SKIN_URL);
        this.dataTracker.set(SKIN_VALUE, url);
    }

    public void setPlayerSkin(String username) {
        this.dataTracker.set(SKIN_TYPE, SKIN_PLAYER);
        this.dataTracker.set(SKIN_VALUE, username);
    }

    public boolean hasSlimArms() {
        return this.dataTracker.get(SLIM_ARMS);
    }

    public void setSlimArms(boolean slim) {
        this.dataTracker.set(SLIM_ARMS, slim);
    }

    // ========== OWNER MANAGEMENT ==========

    public Optional<UUID> getOwnerUuid() {
        return this.dataTracker.get(OWNER_UUID);
    }

    public void setOwnerUuid(UUID uuid) {
        this.dataTracker.set(OWNER_UUID, Optional.ofNullable(uuid));
    }

    public boolean isOwnedBy(PlayerEntity player) {
        return getOwnerUuid().map(uuid -> uuid.equals(player.getUuid())).orElse(false);
    }

    // ========== BEHAVIOR ==========

    @Override
    public void tick() {
        // Store previous values for interpolation (LivingEntity does this but we override)
        this.prevBodyYaw = this.bodyYaw;
        this.prevHeadYaw = this.headYaw;

        super.tick();

        // Prevent horizontal movement but allow gravity
        this.setVelocity(0, this.getVelocity().y, 0);

        // Body follows head with delay for natural movement
        float headYaw = this.getHeadYaw();
        float bodyYaw = this.bodyYaw;
        float diff = headYaw - bodyYaw;

        // Normalize difference
        while (diff > 180) diff -= 360;
        while (diff < -180) diff += 360;

        // Body turns to follow head if difference is large enough
        if (Math.abs(diff) > 30) {
            // Smoothly rotate body toward head
            this.bodyYaw = bodyYaw + diff * 0.1f;
        }

        // Sync yaw with body yaw for renderer
        this.setYaw(this.bodyYaw);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushAway(net.minecraft.entity.Entity entity) {
        // Don't get pushed
    }

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) {
            return false;
        }

        // If claimed (has owner), only allow damage from void/kill command
        if (getOwnerUuid().isPresent()) {
            // Allow /kill command and void damage
            if (source == this.getDamageSources().outOfWorld() ||
                    source == this.getDamageSources().genericKill()) {
                return super.damage(source, amount);
            }
            // Block all other damage - shopkeeper is protected
            return false;
        }

        // Unclaimed shopkeepers can be killed normally
        return super.damage(source, amount);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        // Always invulnerable to environmental damage
        if (source == this.getDamageSources().inWall() ||
                source == this.getDamageSources().drown() ||
                source == this.getDamageSources().fall() ||
                source == this.getDamageSources().cactus() ||
                source == this.getDamageSources().sweetBerryBush() ||
                source == this.getDamageSources().freeze()) {
            return true;
        }

        // If claimed, invulnerable to almost everything
        if (getOwnerUuid().isPresent()) {
            // Only void and /kill can hurt claimed shopkeepers
            if (source == this.getDamageSources().outOfWorld() ||
                    source == this.getDamageSources().genericKill()) {
                return false;
            }
            return true;
        }

        return super.isInvulnerableTo(source);
    }

    // ========== NBT ==========

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("SkinType", getSkinType());
        nbt.putString("SkinValue", getSkinValue());
        nbt.putBoolean("SlimArms", hasSlimArms());
        getOwnerUuid().ifPresent(uuid -> nbt.putUuid("OwnerUUID", uuid));
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (nbt.contains("SkinType")) {
            this.dataTracker.set(SKIN_TYPE, nbt.getString("SkinType"));
        }
        if (nbt.contains("SkinValue")) {
            this.dataTracker.set(SKIN_VALUE, nbt.getString("SkinValue"));
        }
        if (nbt.contains("SlimArms")) {
            this.dataTracker.set(SLIM_ARMS, nbt.getBoolean("SlimArms"));
        }
        if (nbt.containsUuid("OwnerUUID")) {
            setOwnerUuid(nbt.getUuid("OwnerUUID"));
        }
    }

    // ========== REQUIRED OVERRIDES ==========

    @Override
    public Iterable<ItemStack> getArmorItems() {
        return this.armorItems;
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return switch (slot.getType()) {
            case ARMOR -> this.armorItems.get(slot.getEntitySlotId());
            default -> ItemStack.EMPTY;
        };
    }

    @Override
    public void equipStack(EquipmentSlot slot, ItemStack stack) {
        if (slot.getType() == EquipmentSlot.Type.ARMOR) {
            this.armorItems.set(slot.getEntitySlotId(), stack);
        }
    }

    @Override
    public Arm getMainArm() {
        return Arm.RIGHT;
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this);
    }
}