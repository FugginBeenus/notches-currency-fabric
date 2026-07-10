package net.fugginbeenus.notchcurrency.entity;

import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The unified Notch NPC entity — a GeckoLib-animated humanoid that carries its own identity (name,
 * owner, role) and, in later phases, appearance/behavior/dialogue. Phase 1 keeps it stationary with a
 * look-at goal and a single idle animation; the role stored on the entity decides what interacting
 * with it does (shop, bank, auction, …). Only the owner (or an op) can edit it.
 */
public class NotchNpcEntity extends PathAwareEntity implements GeoEntity {

    public enum OwnerType { PLAYER, SERVER }

    /** Movement preset. STATIONARY holds position; WANDER roams around a home point within a radius;
     *  FOLLOW_OWNER tags along behind the owner (teleporting to catch up if left far behind);
     *  PATROL walks its waypoint route in a loop; GUARD holds its post and attacks hostile mobs. */
    public enum Behavior { STATIONARY, WANDER, FOLLOW_OWNER, PATROL, GUARD }

    /** How dialogue plays. WINDOW opens the conversation screen; CHAT says a quick line in chat
     *  (a random page from the tree) and then opens the role directly — the lightweight style. */
    public enum DialogueMode { WINDOW, CHAT }

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.notch_npc.idle");
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    // Model + skin identifiers.
    public static final String MODEL_HUMANOID = "humanoid";
    public static final String MODEL_APPLY = "apply";
    public static final String SKIN_PRESET = "preset";
    public static final String SKIN_PLAYER = "player";
    public static final String SKIN_URL = "url";
    public static final String SKIN_VARIANT = "variant";

    // Synced appearance (so the client renderer reflects edits live).
    private static final TrackedData<String> MODEL =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_TYPE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<String> SKIN_VALUE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.STRING);
    private static final TrackedData<Boolean> SLIM =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /** Pose preset: 0 standing, 1 sitting, 2 sneaking, 3 sleeping. */
    private static final TrackedData<Integer> NPC_POSE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.INTEGER);

    public static final int POSE_STANDING = 0;
    public static final int POSE_SITTING = 1;
    public static final int POSE_SNEAKING = 2;
    public static final int POSE_SLEEPING = 3;
    public static final int POSE_CHILLING = 4; // reclined sit
    public static final int POSE_PRONE = 5;    // lying face-down (vanilla swim pose)
    public static final int POSE_WAVING = 6;   // arm raised in greeting
    public static final int POSE_CUSTOM = 7;   // per-part rotations from the pose editor

    /** Per-part custom rotations (degrees), keyed "0".."5" (head, body, r-arm, l-arm, r-leg, l-leg)
     *  as int arrays [x,y,z]. Synced so the client model can apply them. */
    private static final TrackedData<NbtCompound> CUSTOM_POSE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.NBT_COMPOUND);

    /** Idle animation layered on top of whatever pose is active (synced for the client model). */
    private static final TrackedData<Integer> POSE_ANIM =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Bumped on every landed melee hit — the client model plays its attack swing off this, since
     *  vanilla's hand-swing animation packet proved unreliable for this entity. */
    private static final TrackedData<Integer> ATTACK_PULSE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.INTEGER);

    /** Client-side: the age at which the latest attack swing started (model animates 8 ticks). */
    public float clientSwingStartAge = -1000f;
    private int lastSeenAttackPulse = -1;

    public static final int ANIM_STATUE = 0;  // truly frozen (vanilla's idle arm bob removed too)
    public static final int ANIM_BREATHE = 1; // the normal vanilla idle look (default)
    public static final int ANIM_LIVELY = 2;  // breathing chest + body sway + slow head glances
    public static final int ANIM_COUNT = 3;

    /** Client-side cache of CUSTOM_POSE as 18 floats (6 parts × pitch/yaw/roll, degrees). */
    @Nullable private float[] customPoseCache = null;

    // Config (persisted in NBT; also packed into the NPC item on pick-up).
    private NpcRole role = NpcRole.NONE;
    @Nullable private UUID roleTarget = null;
    private OwnerType ownerType = OwnerType.PLAYER;
    @Nullable private UUID owner = null;
    private String ownerName = "";

    // Behavior (movement preset + home leash). Home is set where the NPC is placed.
    private Behavior behavior = Behavior.STATIONARY;
    private int wanderRadius = 8;
    private float patrolSpeed = 0.9f; // stroll 0.6 / walk 0.9 / jog 1.2
    private int patrolWaitTicks = 0;  // pause at each waypoint (game ticks, 0 = none)
    @Nullable private net.minecraft.util.math.BlockPos homePos = null;
    private final java.util.List<net.minecraft.util.math.BlockPos> waypoints = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.entity.ai.goal.Goal> behaviorGoals = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.entity.ai.goal.Goal> behaviorTargetGoals = new java.util.ArrayList<>();

    // Branching dialogue. Empty = interaction goes straight to the role.
    private net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree dialogue =
            new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree();
    private DialogueMode dialogueMode = DialogueMode.WINDOW;

    // Stats: protection toggle (silent/glowing/gravity/nameplate ride on vanilla entity flags).
    private boolean protectedNpc = true;
    private boolean opensDoors = false;
    private boolean leashable = false;
    private boolean pushable = false; // NPCs hold their ground by default (not shoved around)
    private boolean hostileToPlayers = false; // actively hunts non-owner players
    private boolean fightsBack = false;       // revenge-targets whatever hurts it
    private int regen = 0; // half-hearts healed every 5 seconds
    @Nullable private net.minecraft.entity.ai.goal.Goal doorGoal = null;

    // Moves-tab granularity.
    private String followPlayerName = ""; // blank = follow the owner
    private boolean avoidMonsters = false;
    private boolean watchPlayers = true; // the look-at-passers-by goal
    // No initializer: initGoals() runs from the super constructor and sets this before
    // field initializers would run (an "= null" here would wipe the reference).
    @Nullable private LookAtEntityGoal lookGoal;

    // Display rule: when the NPC exists to be seen. Hidden = invisible + non-interactive.
    public static final int VIS_ALWAYS = 0, VIS_DAY = 1, VIS_NIGHT = 2;
    private int visibility = VIS_ALWAYS;
    private boolean manualInvisible = false; // the stats-screen Invisible toggle

    // Handler id for the CUSTOM role (registered by other mods via NotchNpcApi).
    private String customRoleId = "";

    public NotchNpcEntity(EntityType<? extends NotchNpcEntity> type, World world) {
        super(type, world);
        this.setPersistent();
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MODEL, MODEL_HUMANOID);
        this.dataTracker.startTracking(SKIN_TYPE, SKIN_PRESET);
        this.dataTracker.startTracking(SKIN_VALUE, "1");
        this.dataTracker.startTracking(SLIM, false);
        this.dataTracker.startTracking(SCALE, 1.0f);
        this.dataTracker.startTracking(NPC_POSE, POSE_STANDING);
        this.dataTracker.startTracking(CUSTOM_POSE, new NbtCompound());
        this.dataTracker.startTracking(POSE_ANIM, ANIM_BREATHE); // alive-by-default
        this.dataTracker.startTracking(ATTACK_PULSE, 0);
    }

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && !this.getWorld().isClient) {
            // Pulse the swing to clients (wraps safely — the client only watches for CHANGE).
            this.dataTracker.set(ATTACK_PULSE, this.dataTracker.get(ATTACK_PULSE) + 1);
        }
        return hit;
    }

    public int getPoseAnim() { return this.dataTracker.get(POSE_ANIM); }
    public void setPoseAnim(int anim) {
        this.dataTracker.set(POSE_ANIM, Math.max(0, Math.min(ANIM_COUNT - 1, anim)));
    }

    @Override
    public void onTrackedDataSet(TrackedData<?> data) {
        super.onTrackedDataSet(data);
        if (CUSTOM_POSE.equals(data)) {
            customPoseCache = unpackCustomPose(this.dataTracker.get(CUSTOM_POSE));
        }
        if (ATTACK_PULSE.equals(data)) {
            int pulse = this.dataTracker.get(ATTACK_PULSE);
            // First sync just sets the baseline; a CHANGE afterwards means a fresh melee hit.
            if (lastSeenAttackPulse >= 0 && pulse != lastSeenAttackPulse) {
                clientSwingStartAge = this.age;
            }
            lastSeenAttackPulse = pulse;
        }
    }

    @Nullable
    private static float[] unpackCustomPose(NbtCompound nbt) {
        if (nbt == null || nbt.isEmpty()) return null;
        float[] out = new float[18];
        for (int part = 0; part < 6; part++) {
            int[] rot = nbt.getIntArray(Integer.toString(part));
            if (rot.length == 3) {
                out[part * 3] = rot[0];
                out[part * 3 + 1] = rot[1];
                out[part * 3 + 2] = rot[2];
            }
        }
        return out;
    }

    /** The custom-pose rotations (degrees, 6 parts × pitch/yaw/roll), or null when unset. */
    @Nullable
    public float[] getCustomPoseAngles() {
        return customPoseCache;
    }

    /** Set one part's custom rotation (degrees); {@code part == -1} clears the whole pose. */
    public void setCustomPosePart(int part, int x, int y, int z) {
        NbtCompound pose = this.dataTracker.get(CUSTOM_POSE).copy();
        if (part < 0) {
            pose = new NbtCompound();
        } else if (part < 6) {
            pose.putIntArray(Integer.toString(part), new int[]{clampDeg(x), clampDeg(y), clampDeg(z)});
        }
        this.dataTracker.set(CUSTOM_POSE, pose);
        customPoseCache = unpackCustomPose(pose); // keep the server-side copy fresh too
    }

    private static int clampDeg(int deg) {
        return Math.max(-180, Math.min(180, deg));
    }

    public int getNpcPose() { return this.dataTracker.get(NPC_POSE); }

    public void setNpcPose(int pose) {
        int clamped = Math.max(POSE_STANDING, Math.min(POSE_CUSTOM, pose));
        this.dataTracker.set(NPC_POSE, clamped);
        this.setPose(entityPoseFor(clamped));
    }

    private static net.minecraft.entity.EntityPose entityPoseFor(int npcPose) {
        return switch (npcPose) {
            case POSE_SNEAKING -> net.minecraft.entity.EntityPose.CROUCHING;
            case POSE_SLEEPING -> net.minecraft.entity.EntityPose.SLEEPING;
            case POSE_PRONE -> net.minecraft.entity.EntityPose.SWIMMING;
            default -> net.minecraft.entity.EntityPose.STANDING; // sitting/chilling are model-level
        };
    }

    /** Keep the nameplate above the visible body: pose changes the height and the model is scaled. */
    @Override
    public float getNameLabelHeight() {
        float base = switch (getNpcPose()) {
            case POSE_SLEEPING, POSE_PRONE -> 0.5f;
            case POSE_SNEAKING -> 1.7f;
            case POSE_SITTING, POSE_CHILLING -> 1.35f;
            default -> 1.95f;
        };
        return base * getScale() + 0.4f;
    }

    public String getModelId() { return this.dataTracker.get(MODEL); }
    public void setModelId(String id) { this.dataTracker.set(MODEL, (id == null || id.isEmpty()) ? MODEL_HUMANOID : id); }

    public String getSkinType() { return this.dataTracker.get(SKIN_TYPE); }
    public void setSkinType(String t) { this.dataTracker.set(SKIN_TYPE, (t == null || t.isEmpty()) ? SKIN_PRESET : t); }

    public String getSkinValue() { return this.dataTracker.get(SKIN_VALUE); }
    public void setSkinValue(String v) { this.dataTracker.set(SKIN_VALUE, v == null ? "" : v); }

    public boolean isSlim() { return this.dataTracker.get(SLIM); }
    public void setSlim(boolean slim) { this.dataTracker.set(SLIM, slim); }

    public float getScale() { return this.dataTracker.get(SCALE); }
    public void setScale(float scale) { this.dataTracker.set(SCALE, Math.max(0.3f, Math.min(3.0f, scale))); }

    /** Apply a full appearance in one call (used by the editor packet). */
    public void setAppearance(String model, String skinType, String skinValue, boolean slim, float scale) {
        setModelId(model);
        setSkinType(skinType);
        setSkinValue(skinValue);
        setSlim(slim);
        setScale(scale);
    }

    // ---- behavior ----

    public Behavior getBehavior() { return behavior; }

    public void setBehavior(Behavior b) {
        this.behavior = b == null ? Behavior.STATIONARY : b;
        applyBehaviorGoals();
    }

    public int getWanderRadius() { return wanderRadius; }

    public void setWanderRadius(int radius) {
        this.wanderRadius = Math.max(4, Math.min(64, radius));
        applyBehaviorGoals();
    }

    /** Set the home/leash point (called on placement). */
    public void setHome(net.minecraft.util.math.BlockPos pos) {
        this.homePos = pos == null ? null : pos.toImmutable();
        applyBehaviorGoals();
    }

    /** Patrol route (looped in order). The patrol goal reads this live. */
    public java.util.List<net.minecraft.util.math.BlockPos> getWaypoints() { return waypoints; }

    /** Add a patrol waypoint (capped at 16). Returns false when full. */
    public boolean addWaypoint(net.minecraft.util.math.BlockPos pos) {
        if (waypoints.size() >= 16) return false;
        waypoints.add(pos.toImmutable());
        return true;
    }

    public void clearWaypoints() {
        waypoints.clear();
    }

    /** Remove the most recently added waypoint (route tool undo). Returns false if the route is empty. */
    public boolean removeLastWaypoint() {
        if (waypoints.isEmpty()) return false;
        waypoints.remove(waypoints.size() - 1);
        return true;
    }

    public float getPatrolSpeed() { return patrolSpeed; }
    public void setPatrolSpeed(float speed) { this.patrolSpeed = Math.max(0.3f, Math.min(1.5f, speed)); }

    /** How long the NPC lingers at each waypoint before moving on (game ticks, 0 = no pause). */
    public int getPatrolWaitTicks() { return patrolWaitTicks; }
    public void setPatrolWaitTicks(int ticks) { this.patrolWaitTicks = Math.max(0, Math.min(600, ticks)); }

    // ---- dialogue ----

    public net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree getDialogue() { return dialogue; }

    public void setDialogue(net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree tree) {
        this.dialogue = tree == null ? new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree() : tree;
    }

    public DialogueMode getDialogueMode() { return dialogueMode; }
    public void setDialogueMode(DialogueMode mode) { this.dialogueMode = mode == null ? DialogueMode.WINDOW : mode; }

    // ---- stats ----

    public boolean isProtectedNpc() { return protectedNpc; }
    public void setProtectedNpc(boolean p) { this.protectedNpc = p; }

    public boolean opensDoors() { return opensDoors; }

    /** Toggle door use: adds/removes the open-door goal and the pathfinding permissions. */
    public void setOpensDoors(boolean open) {
        this.opensDoors = open;
        applyDoorCapability();
    }

    /** (Re)apply the door pathfinding flags + open-door goal from {@link #opensDoors}. Called whenever
     *  the toggle changes AND whenever behavior goals are rebuilt, so a behavior swap never drops it.
     *  NOTE: doors only open while the NPC is actually pathing through one — a Stationary NPC won't. */
    private void applyDoorCapability() {
        if (this.getNavigation() instanceof net.minecraft.entity.ai.pathing.MobNavigation nav) {
            nav.setCanPathThroughDoors(opensDoors);
            nav.setCanEnterOpenDoors(true);
            if (nav.getNodeMaker() != null) nav.getNodeMaker().setCanOpenDoors(opensDoors);
        }
        if (opensDoors && doorGoal == null) {
            doorGoal = new net.minecraft.entity.ai.goal.LongDoorInteractGoal(this, true);
            this.goalSelector.add(1, doorGoal);
        } else if (!opensDoors && doorGoal != null) {
            this.goalSelector.remove(doorGoal);
            doorGoal = null;
        }
    }

    public boolean isLeashable() { return leashable; }
    public void setLeashable(boolean l) { this.leashable = l; }

    public boolean isNpcPushable() { return pushable; }
    public void setNpcPushable(boolean p) { this.pushable = p; }

    public boolean isHostileToPlayers() { return hostileToPlayers; }
    public void setHostileToPlayers(boolean h) {
        if (this.hostileToPlayers != h) {
            this.hostileToPlayers = h;
            applyBehaviorGoals(); // combat goals ride the behavior goal lists
        }
    }

    public boolean fightsBack() { return fightsBack; }
    public void setFightsBack(boolean f) {
        if (this.fightsBack != f) {
            this.fightsBack = f;
            applyBehaviorGoals();
        }
    }

    @Override
    public boolean isPushable() {
        return pushable;
    }

    public String getFollowPlayerName() { return followPlayerName; }
    public void setFollowPlayerName(String name) {
        this.followPlayerName = name == null ? "" : name.trim();
        if (this.followPlayerName.length() > 16) this.followPlayerName = this.followPlayerName.substring(0, 16);
    }

    /** Who FOLLOW mode walks after: the named player when set, otherwise the owner. */
    @Nullable
    public PlayerEntity resolveFollowTarget() {
        if (!followPlayerName.isEmpty() && this.getServer() != null) {
            return this.getServer().getPlayerManager().getPlayer(followPlayerName);
        }
        return owner != null ? this.getWorld().getPlayerByUuid(owner) : null;
    }

    public boolean avoidsMonsters() { return avoidMonsters; }
    public void setAvoidMonsters(boolean avoid) {
        if (this.avoidMonsters != avoid) {
            this.avoidMonsters = avoid;
            applyBehaviorGoals();
        }
    }

    public boolean watchesPlayers() { return watchPlayers; }
    public void setWatchPlayers(boolean watch) {
        this.watchPlayers = watch;
        if (lookGoal != null) {
            this.goalSelector.remove(lookGoal);
            if (watch) this.goalSelector.add(6, lookGoal);
        }
    }

    public int getVisibility() { return visibility; }
    public void setVisibility(int vis) { this.visibility = Math.max(0, Math.min(2, vis)); }

    public boolean isManualInvisible() { return manualInvisible; }
    public void setManualInvisible(boolean inv) { this.manualInvisible = inv; }

    public String getCustomRoleId() { return customRoleId; }
    public void setCustomRoleId(String id) { this.customRoleId = id == null ? "" : id; }

    /** True while the day/night visibility rule says this NPC should be hidden right now. */
    public boolean isRuleHidden() {
        if (visibility == VIS_DAY) return !this.getWorld().isDay();
        if (visibility == VIS_NIGHT) return this.getWorld().isDay();
        return false;
    }

    @Override
    public boolean canBeLeashedBy(PlayerEntity player) {
        return leashable && super.canBeLeashedBy(player);
    }

    public int getRegen() { return regen; }
    public void setRegen(int r) { this.regen = Math.max(0, Math.min(10, r)); }

    /** Apply the slider attributes (max health, walk speed) and heal to the new cap. */
    public void setBaseStats(int maxHealth, int speedPct) {
        int hp = Math.max(2, Math.min(100, maxHealth));
        double speed = Math.max(0.1, Math.min(0.6, speedPct / 100.0));
        var health = this.getAttributeInstance(EntityAttributes.GENERIC_MAX_HEALTH);
        if (health != null) health.setBaseValue(hp);
        var move = this.getAttributeInstance(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        if (move != null) move.setBaseValue(speed);
        this.setHealth(hp);
    }

    /** One-line-per-fact diagnostic dump for {@code /npc debug}. */
    public java.util.List<String> debugSummary(ServerPlayerEntity viewer) {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("behavior=" + behavior + " radius=" + wanderRadius
                + " home=" + (homePos == null ? "none" : homePos.toShortString()));
        PlayerEntity resolvedOwner = owner == null ? null : this.getWorld().getPlayerByUuid(owner);
        out.add("owner=" + (owner == null ? "none" : ownerName)
                + " resolved=" + (resolvedOwner != null)
                + " distToYou=" + String.format("%.1f", Math.sqrt(this.squaredDistanceTo(viewer))));
        out.add("movementGoals=" + behaviorGoals.size()
                + " navIdle=" + this.getNavigation().isIdle()
                + " despawnCounter=" + this.getDespawnCounter());
        out.add("onGround=" + this.isOnGround() + " noGravity=" + this.hasNoGravity()
                + " aiDisabled=" + this.isAiDisabled()
                + " speedAttr=" + this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED)
                + " followRange=" + this.getAttributeValue(EntityAttributes.GENERIC_FOLLOW_RANGE));
        return out;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return PathAwareEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0)
                // Follow range doubles as the pathfinding distance limit; the default 16 made a
                // following NPC freeze whenever its owner got 16-30 blocks away (teleport kicks
                // in at 30).
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 48.0)
                // Needed by MeleeAttackGoal (GUARD mode). Weapon bonuses stack on top.
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void initGoals() {
        // Base goals every NPC has; movement goals are swapped in by applyBehaviorGoals().
        this.goalSelector.add(0, new net.minecraft.entity.ai.goal.SwimGoal(this));
        this.lookGoal = new LookAtEntityGoal(this, PlayerEntity.class, 8.0f);
        this.goalSelector.add(6, lookGoal);
    }

    /** Swap the movement/combat goals to match the current behavior preset. */
    private void applyBehaviorGoals() {
        for (net.minecraft.entity.ai.goal.Goal g : behaviorGoals) {
            this.goalSelector.remove(g);
        }
        behaviorGoals.clear();
        for (net.minecraft.entity.ai.goal.Goal g : behaviorTargetGoals) {
            this.targetSelector.remove(g);
        }
        behaviorTargetGoals.clear();
        this.setTarget(null); // drop any combat target when leaving GUARD

        if (avoidMonsters) {
            // Runs alongside any behavior: back away from hostiles that get close.
            net.minecraft.entity.ai.goal.Goal flee = new net.minecraft.entity.ai.goal.FleeEntityGoal<>(
                    this, net.minecraft.entity.mob.HostileEntity.class, 8.0f, 1.0, 1.25);
            this.goalSelector.add(1, flee);
            behaviorGoals.add(flee);
        }

        switch (behavior) {
            case WANDER -> {
                // Short-range strolls every ~2s — livelier than the vanilla far-wander cadence and a
                // better fit for the home leash. canDespawn=false skips the despawn-counter gate.
                net.minecraft.entity.ai.goal.Goal wander =
                        new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.8, 40, false);
                this.goalSelector.add(2, wander);
                behaviorGoals.add(wander);
                applyHomeLeash();
            }
            case FOLLOW_OWNER -> {
                net.minecraft.entity.ai.goal.Goal follow = new NpcFollowOwnerGoal(this, 1.15);
                this.goalSelector.add(2, follow);
                behaviorGoals.add(follow);
                this.clearPositionTarget(); // no leash
            }
            case PATROL -> {
                net.minecraft.entity.ai.goal.Goal patrol = new NpcPatrolGoal(this);
                this.goalSelector.add(2, patrol);
                behaviorGoals.add(patrol);
                this.clearPositionTarget(); // waypoints may be far from home
            }
            case GUARD -> {
                // Fight what comes close, stroll the post while idle, stay leashed to home.
                net.minecraft.entity.ai.goal.Goal melee =
                        new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.1, true);
                this.goalSelector.add(2, melee);
                behaviorGoals.add(melee);
                net.minecraft.entity.ai.goal.Goal stroll =
                        new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.6, 80, false);
                this.goalSelector.add(5, stroll);
                behaviorGoals.add(stroll);
                // Target hostile mobs, but never creepers (iron-golem rule — don't trigger blasts).
                net.minecraft.entity.ai.goal.Goal targets = new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                        this, net.minecraft.entity.mob.HostileEntity.class, 10, true, false,
                        e -> !(e instanceof net.minecraft.entity.mob.CreeperEntity));
                this.targetSelector.add(1, targets);
                behaviorTargetGoals.add(targets);
                applyHomeLeash();
            }
            case STATIONARY -> this.clearPositionTarget();
        }

        // Hostile actions ride along with any behavior (GUARD already brings its own melee goal).
        if ((hostileToPlayers || fightsBack) && behavior != Behavior.GUARD) {
            net.minecraft.entity.ai.goal.Goal melee =
                    new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.1, true);
            this.goalSelector.add(2, melee);
            behaviorGoals.add(melee);
        }
        if (hostileToPlayers) {
            // Hunt ANY player in range — including the owner (hostile means hostile). Vanilla
            // targeting already skips creative/spectator players.
            net.minecraft.entity.ai.goal.Goal huntPlayers = new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                    this, net.minecraft.entity.player.PlayerEntity.class, 10, true, false, null);
            this.targetSelector.add(2, huntPlayers);
            behaviorTargetGoals.add(huntPlayers);
        }
        if (fightsBack) {
            net.minecraft.entity.ai.goal.Goal revenge = new net.minecraft.entity.ai.goal.RevengeGoal(this);
            this.targetSelector.add(1, revenge);
            behaviorTargetGoals.add(revenge);
        }
        applyDoorCapability(); // re-assert door pathing/goal after the goal list is rebuilt
    }

    private void applyHomeLeash() {
        if (homePos != null) {
            this.setPositionTarget(homePos, Math.max(2, wanderRadius));
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (!this.getWorld().isClient) {
            if (behavior == Behavior.STATIONARY) {
                if (this.getTarget() != null && this.getTarget().isAlive()) {
                    // In combat (hostile/fights-back): let the attack goal chase.
                } else if (homePos != null && this.squaredDistanceTo(
                        homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5) > 2.25) {
                    // Combat over (or shoved): walk back to the post before locking down again.
                    this.getNavigation().startMovingTo(
                            homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, 1.0);
                } else {
                    this.getNavigation().stop();
                    this.setVelocity(0, this.getVelocity().y, 0);
                }
            }
            // Re-assert the configured pose in case vanilla logic reset it.
            net.minecraft.entity.EntityPose want = entityPoseFor(getNpcPose());
            if (this.getPose() != want) {
                this.setPose(want);
            }
            // Health regeneration (half-hearts per 5 seconds).
            if (regen > 0 && this.age % 100 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(regen * 0.5f);
            }
            // Keep the vanilla invisible flag in step with the toggle + day/night rule.
            if (this.age % 20 == 0) {
                boolean hidden = manualInvisible || isRuleHidden();
                if (this.isInvisible() != hidden) this.setInvisible(hidden);
            }
        }
    }

    @Override
    protected void pushAway(net.minecraft.entity.Entity entity) {
        if (pushable) super.pushAway(entity); // hold ground unless the Pushable ability is on
    }

    @Override
    public void checkDespawn() {
        // Never despawn — these are placed, persistent NPCs. Also keep the despawn counter at zero:
        // vanilla increments it every AI tick and only resets it here, and WanderAroundGoal refuses
        // to start once it passes 100 (which froze wandering ~5s after placement).
        this.despawnCounter = 0;
    }

    // ---- config accessors ----

    public NpcRole getRole() { return role; }
    public void setRole(NpcRole role) { this.role = role == null ? NpcRole.NONE : role; }

    @Nullable public UUID getRoleTarget() { return roleTarget; }
    public void setRoleTarget(@Nullable UUID target) { this.roleTarget = target; }

    public OwnerType getOwnerType() { return ownerType; }
    public void setOwnerType(OwnerType t) { this.ownerType = t == null ? OwnerType.PLAYER : t; }

    @Nullable public UUID getOwner() { return owner; }
    public String getOwnerName() { return ownerName; }

    public void setOwner(@Nullable UUID uuid, String name) {
        this.owner = uuid;
        this.ownerName = name == null ? "" : name;
        this.ownerType = uuid == null ? OwnerType.SERVER : OwnerType.PLAYER;
    }

    public boolean isOwnedBy(PlayerEntity player) {
        return owner != null && owner.equals(player.getUuid());
    }

    /** Owner or an operator may open the editor and change anything. */
    public boolean canEdit(ServerPlayerEntity player) {
        return isOwnedBy(player) || player.hasPermissionLevel(2);
    }

    // ---- interaction ----

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (hand != Hand.MAIN_HAND) {
            return super.interactMob(player, hand);
        }
        if (this.getWorld().isClient) {
            return ActionResult.SUCCESS;
        }
        if (player instanceof ServerPlayerEntity sp) {
            // Off duty per the day/night rule: only the owner/op can still reach it (to edit).
            if (isRuleHidden() && !canEdit(sp)) {
                return ActionResult.PASS;
            }
            if (sp.isSneaking() && canEdit(sp)) {
                NotchNpcManager.openEditor(sp, this);
            } else if (!net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.open(sp, this)) {
                // No dialogue — go straight to the role.
                NotchNpcManager.dispatchRole(sp, this);
            }
        }
        return ActionResult.SUCCESS;
    }

    // ---- damage protection (owned NPCs are protected, like the old shopkeeper) ----

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) return false;
        if (protectedNpc && (owner != null || ownerType == OwnerType.SERVER)) {
            // Only the void or /kill can remove a protected NPC.
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return super.damage(source, amount);
            }
            // The hit is cancelled, but Fights Back still needs to know who swung — record the
            // attacker so the RevengeGoal can retaliate even while the NPC itself is unhurtable.
            if (fightsBack && source.getAttacker() instanceof net.minecraft.entity.LivingEntity attacker) {
                this.setAttacker(attacker);
            }
            return false;
        }
        return super.damage(source, amount);
    }

    // ---- NBT ----

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        writeConfig(nbt);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        readConfig(nbt);
    }

    /** Shared serializer used by both entity NBT and the "pick up" item. Excludes the custom name,
     *  which the caller handles (entity NBT stores it already; the item stores it separately). */
    public void writeConfig(NbtCompound nbt) {
        nbt.putString("Role", role.name());
        if (roleTarget != null) nbt.putUuid("RoleTarget", roleTarget);
        nbt.putString("OwnerType", ownerType.name());
        if (owner != null) nbt.putUuid("Owner", owner);
        nbt.putString("OwnerName", ownerName);
        nbt.putString("Model", getModelId());
        nbt.putString("SkinType", getSkinType());
        nbt.putString("SkinValue", getSkinValue());
        nbt.putBoolean("Slim", isSlim());
        nbt.putFloat("Scale", getScale());
        nbt.putInt("NpcPose", getNpcPose());
        nbt.put("CustomPose", this.dataTracker.get(CUSTOM_POSE).copy());
        nbt.putInt("PoseAnim", getPoseAnim());
        nbt.putString("Behavior", behavior.name());
        nbt.putInt("WanderRadius", wanderRadius);
        nbt.putFloat("PatrolSpeed", patrolSpeed);
        nbt.putInt("PatrolWait", patrolWaitTicks);
        if (homePos != null) {
            nbt.putIntArray("Home", new int[]{homePos.getX(), homePos.getY(), homePos.getZ()});
        }
        net.minecraft.nbt.NbtList wps = new net.minecraft.nbt.NbtList();
        for (net.minecraft.util.math.BlockPos wp : waypoints) {
            wps.add(new net.minecraft.nbt.NbtIntArray(new int[]{wp.getX(), wp.getY(), wp.getZ()}));
        }
        nbt.put("Waypoints", wps);
        nbt.put("Dialogue", dialogue.toNbt());
        nbt.putString("DialogueMode", dialogueMode.name());
        // Stats — the vanilla flags are re-recorded here so they survive the pick-up item too.
        nbt.putBoolean("Protected", protectedNpc);
        nbt.putBoolean("StatSilent", this.isSilent());
        nbt.putBoolean("StatGlowing", this.isGlowing());
        nbt.putBoolean("StatNoGravity", this.hasNoGravity());
        nbt.putBoolean("StatNameVisible", this.isCustomNameVisible());
        nbt.putBoolean("StatDoors", opensDoors);
        nbt.putBoolean("StatLeashable", leashable);
        nbt.putBoolean("StatPushable", pushable);
        nbt.putBoolean("StatHostilePlayers", hostileToPlayers);
        nbt.putBoolean("StatFightsBack", fightsBack);
        nbt.putBoolean("StatInvisible", manualInvisible);
        nbt.putInt("StatRegen", regen);
        nbt.putInt("Visibility", visibility);
        nbt.putString("CustomRole", customRoleId);
        nbt.putString("FollowPlayer", followPlayerName);
        nbt.putBoolean("AvoidMonsters", avoidMonsters);
        nbt.putBoolean("WatchPlayers", watchPlayers);
        // Attribute bases — recorded so they survive the pick-up item (entity NBT has them anyway).
        nbt.putInt("StatMaxHealth", (int) Math.round(this.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH)));
        nbt.putInt("StatSpeedPct", (int) Math.round(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 100));
        // Equipment — re-recorded so it survives the pick-up item too.
        NbtCompound equip = new NbtCompound();
        for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
            net.minecraft.item.ItemStack st = this.getEquippedStack(slot);
            if (!st.isEmpty()) equip.put(slot.getName(), st.writeNbt(new NbtCompound()));
        }
        nbt.put("Equip", equip);
    }

    public void readConfig(NbtCompound nbt) {
        try {
            role = NpcRole.valueOf(nbt.getString("Role"));
        } catch (IllegalArgumentException e) {
            role = NpcRole.NONE;
        }
        roleTarget = nbt.containsUuid("RoleTarget") ? nbt.getUuid("RoleTarget") : null;
        try {
            ownerType = OwnerType.valueOf(nbt.getString("OwnerType"));
        } catch (IllegalArgumentException e) {
            ownerType = OwnerType.PLAYER;
        }
        owner = nbt.containsUuid("Owner") ? nbt.getUuid("Owner") : null;
        ownerName = nbt.getString("OwnerName");
        if (nbt.contains("Model")) setModelId(nbt.getString("Model"));
        if (nbt.contains("SkinType")) setSkinType(nbt.getString("SkinType"));
        if (nbt.contains("SkinValue")) setSkinValue(nbt.getString("SkinValue"));
        if (nbt.contains("Slim")) setSlim(nbt.getBoolean("Slim"));
        if (nbt.contains("Scale")) setScale(nbt.getFloat("Scale"));
        if (nbt.contains("NpcPose")) setNpcPose(nbt.getInt("NpcPose"));
        if (nbt.contains("PoseAnim")) setPoseAnim(nbt.getInt("PoseAnim"));
        if (nbt.contains("CustomPose")) {
            NbtCompound pose = nbt.getCompound("CustomPose");
            this.dataTracker.set(CUSTOM_POSE, pose);
            customPoseCache = unpackCustomPose(pose);
        }
        if (nbt.contains("Behavior")) {
            try {
                behavior = Behavior.valueOf(nbt.getString("Behavior"));
            } catch (IllegalArgumentException e) {
                behavior = Behavior.STATIONARY;
            }
        }
        if (nbt.contains("WanderRadius")) wanderRadius = Math.max(4, Math.min(64, nbt.getInt("WanderRadius")));
        if (nbt.contains("PatrolSpeed")) setPatrolSpeed(nbt.getFloat("PatrolSpeed"));
        if (nbt.contains("PatrolWait")) setPatrolWaitTicks(nbt.getInt("PatrolWait"));
        int[] home = nbt.getIntArray("Home");
        if (home.length == 3) homePos = new net.minecraft.util.math.BlockPos(home[0], home[1], home[2]);
        if (nbt.contains("Waypoints")) {
            waypoints.clear();
            net.minecraft.nbt.NbtList wps = nbt.getList("Waypoints", net.minecraft.nbt.NbtElement.INT_ARRAY_TYPE);
            for (int i = 0; i < wps.size(); i++) {
                int[] wp = wps.getIntArray(i);
                if (wp.length == 3) waypoints.add(new net.minecraft.util.math.BlockPos(wp[0], wp[1], wp[2]));
            }
        }
        if (nbt.contains("Dialogue")) {
            dialogue = net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(nbt.getCompound("Dialogue"));
        }
        if (nbt.contains("DialogueMode")) {
            try {
                dialogueMode = DialogueMode.valueOf(nbt.getString("DialogueMode"));
            } catch (IllegalArgumentException e) {
                dialogueMode = DialogueMode.WINDOW;
            }
        }
        if (nbt.contains("Protected")) protectedNpc = nbt.getBoolean("Protected");
        if (nbt.contains("StatSilent")) this.setSilent(nbt.getBoolean("StatSilent"));
        if (nbt.contains("StatGlowing")) this.setGlowing(nbt.getBoolean("StatGlowing"));
        if (nbt.contains("StatNoGravity")) this.setNoGravity(nbt.getBoolean("StatNoGravity"));
        if (nbt.contains("StatNameVisible")) this.setCustomNameVisible(nbt.getBoolean("StatNameVisible"));
        if (nbt.contains("StatDoors")) setOpensDoors(nbt.getBoolean("StatDoors"));
        if (nbt.contains("StatLeashable")) leashable = nbt.getBoolean("StatLeashable");
        if (nbt.contains("StatPushable")) pushable = nbt.getBoolean("StatPushable");
        if (nbt.contains("StatHostilePlayers")) hostileToPlayers = nbt.getBoolean("StatHostilePlayers");
        if (nbt.contains("StatFightsBack")) fightsBack = nbt.getBoolean("StatFightsBack");
        if (nbt.contains("StatInvisible")) {
            manualInvisible = nbt.getBoolean("StatInvisible");
            this.setInvisible(manualInvisible);
        }
        if (nbt.contains("StatRegen")) setRegen(nbt.getInt("StatRegen"));
        if (nbt.contains("Visibility")) setVisibility(nbt.getInt("Visibility"));
        if (nbt.contains("CustomRole")) setCustomRoleId(nbt.getString("CustomRole"));
        if (nbt.contains("FollowPlayer")) setFollowPlayerName(nbt.getString("FollowPlayer"));
        if (nbt.contains("AvoidMonsters")) avoidMonsters = nbt.getBoolean("AvoidMonsters");
        if (nbt.contains("WatchPlayers")) setWatchPlayers(nbt.getBoolean("WatchPlayers"));
        if (nbt.contains("StatMaxHealth")) {
            int hp = nbt.getInt("StatMaxHealth");
            int speedPct = nbt.contains("StatSpeedPct") ? nbt.getInt("StatSpeedPct") : 30;
            // Skip when they already match (world reload path — vanilla restored the attributes
            // before us, and re-applying would heal a damaged NPC to full).
            if (hp != (int) Math.round(this.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH))
                    || speedPct != (int) Math.round(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 100)) {
                setBaseStats(hp, speedPct);
            }
        }
        if (nbt.contains("Equip")) {
            NbtCompound equip = nbt.getCompound("Equip");
            for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
                if (equip.contains(slot.getName())) {
                    this.equipStack(slot, net.minecraft.item.ItemStack.fromNbt(equip.getCompound(slot.getName())));
                    this.setEquipmentDropChance(slot, 1.0f); // owner's items always drop if it dies
                }
            }
        }
        applyBehaviorGoals();
    }

    /** Pack this NPC's full config (including its display name) into a compound for the NPC item. */
    public NbtCompound writeToItem() {
        NbtCompound tag = new NbtCompound();
        writeConfig(tag);
        if (this.hasCustomName() && this.getCustomName() != null) {
            tag.putString("Name", this.getCustomName().getString());
        }
        return tag;
    }

    /** Restore config from an NPC item's packed compound (owner/role/name). */
    public void readFromItem(NbtCompound tag) {
        readConfig(tag);
        if (tag.contains("Name")) {
            this.setCustomName(Text.literal(tag.getString("Name")));
            this.setCustomNameVisible(true);
        }
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "main", 4, this::idlePredicate));
    }

    private <E extends NotchNpcEntity> PlayState idlePredicate(AnimationState<E> state) {
        state.setAndContinue(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
