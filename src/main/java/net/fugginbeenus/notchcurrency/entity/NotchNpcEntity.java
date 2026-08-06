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
//? if >=1.21 {
/*import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.animation.PlayState;
*///?} else {
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
//?}
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

/**
 * The unified Notch NPC entity: a GeckoLib-animated humanoid that carries its own identity (name,
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
     *  (a random page from the tree) and then opens the role directly: the lightweight style. */
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
    /** Width. Kept under the original name so existing NPCs keep their size on load. */
    private static final TrackedData<Float> SCALE =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SCALE_Y =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.FLOAT);
    private static final TrackedData<Float> SCALE_Z =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /** Nudges the floating name up or down: models vary enough that one height never fits all. */
    private static final TrackedData<Float> NAME_OFFSET =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.FLOAT);
    /** Free-floating sign above the NPC, newline-separated. Synced because the client draws it, and
     *  because placeholders like %balance% have to resolve per viewer. */
    private static final TrackedData<String> BILLBOARD =
            DataTracker.registerData(NotchNpcEntity.class, TrackedDataHandlerRegistry.STRING);
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

    /** Bumped on every landed melee hit: the client model plays its attack swing off this, since
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
    /** What this NPC does when something happens to it (talked to, hurt, killed, approached). */
    private net.fugginbeenus.notchcurrency.npc.action.NpcActions actions =
            new net.fugginbeenus.notchcurrency.npc.action.NpcActions();
    private String farewellText = "";

    // Stats: protection toggle (silent/glowing/gravity/nameplate ride on vanilla entity flags).
    private boolean protectedNpc = true;
    private boolean opensDoors = false;
    private boolean leashable = false;
    private boolean pushable = false; // NPCs hold their ground by default (not shoved around)
    private boolean hostileToPlayers = false; // actively hunts non-owner players
    private boolean fightsBack = false;       // revenge-targets whatever hurts it
    private boolean protectOwner = false;     // fights whoever its owner is fighting
    private boolean attackMonsters = false;   // hunts hostiles without needing the Guard behavior
    private boolean fightRivalFactions = false; // takes on anyone flying a different faction's colours
    /** Which faction it belongs to: an id pointing at the record in FactionState, nothing more.
     *  The faction itself is never stored here, so losing this NPC never costs anyone their faction. */
    private String factionId = "";
    /** Which round of action rules this NPC has already been brought in line with. */
    private int actionSweepVersion = 0;

    /** The NPC's day. Empty and disabled costs nothing: see {@link #tickSchedule()}. */
    private net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule schedule =
            new net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule();
    /** Which entry is currently applied, or -1 for "nothing applied yet". Never saved: the whole
     *  point of the schedule is that this can be re-derived from the clock at any moment. */
    private int scheduleActive = -1;

    // A running schedule steers the NPC through these rather than through the configured behaviour,
    // home and radius. Driving the saved fields directly would have the schedule quietly rewriting
    // what the owner set on the Moves tab, and switching the schedule off would leave that damage
    // behind. Kept out of NBT on purpose: they are derived, and they rebuild themselves on load.
    @Nullable private Behavior scheduleBehavior = null;
    /** The pose the owner chose, parked here while a Sleep entry borrows the real one. -1 when
     *  the schedule is not holding it. Saved, so an NPC that unloads mid-nap still wakes up in
     *  the pose its owner picked rather than snapping to standing. */
    private int poseBeforeSchedule = -1;
    @Nullable private net.minecraft.util.math.BlockPos scheduleHome = null;
    private int scheduleRadius = 8;
    private int regen = 0; // half-hearts healed every 5 seconds
    @Nullable private net.minecraft.entity.ai.goal.Goal doorGoal = null;

    // While a player is interacting, the NPC holds still and faces them (see TalkGoal). Refreshed on
    // each interaction; expires so it resumes wandering once the player leaves or a few seconds pass.
    @Nullable private java.util.UUID talkingTo = null;
    private int talkingTicks = 0;

    // Proximity bookkeeping. Both are created only for NPCs that actually use the trigger, so the
    // ordinary NPC carries two null references and nothing else.
    @Nullable private java.util.Set<java.util.UUID> proximityInside = null;
    @Nullable private java.util.Map<java.util.UUID, Integer> proximityFired = null;
    /** When each trigger last fired, for the ones that need pacing. Only allocated once one fires. */
    @Nullable private int[] lastFiredAge = null;
    private static final int PROXIMITY_SCAN_TICKS = 10;
    /** How long before the same player can set it off again, so pacing around isn't a replay button. */
    private static final int PROXIMITY_RECHARGE_TICKS = 200;

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
    //? if >=1.21 {
    /*protected void initDataTracker(net.minecraft.entity.data.DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(MODEL, MODEL_HUMANOID);
        builder.add(SKIN_TYPE, SKIN_PRESET);
        builder.add(SKIN_VALUE, "1");
        builder.add(SLIM, false);
        builder.add(SCALE, 1.0f);
        builder.add(SCALE_Y, 1.0f);
        builder.add(SCALE_Z, 1.0f);
        builder.add(NAME_OFFSET, 0.0f);
        builder.add(BILLBOARD, "");
        builder.add(NPC_POSE, POSE_STANDING);
        builder.add(CUSTOM_POSE, new NbtCompound());
        builder.add(POSE_ANIM, ANIM_BREATHE); // alive-by-default
        builder.add(ATTACK_PULSE, 0);
    }
    *///?} else {
    protected void initDataTracker() {
        super.initDataTracker();
        this.dataTracker.startTracking(MODEL, MODEL_HUMANOID);
        this.dataTracker.startTracking(SKIN_TYPE, SKIN_PRESET);
        this.dataTracker.startTracking(SKIN_VALUE, "1");
        this.dataTracker.startTracking(SLIM, false);
        this.dataTracker.startTracking(SCALE, 1.0f);
        this.dataTracker.startTracking(SCALE_Y, 1.0f);
        this.dataTracker.startTracking(SCALE_Z, 1.0f);
        this.dataTracker.startTracking(NAME_OFFSET, 0.0f);
        this.dataTracker.startTracking(BILLBOARD, "");
        this.dataTracker.startTracking(NPC_POSE, POSE_STANDING);
        this.dataTracker.startTracking(CUSTOM_POSE, new NbtCompound());
        this.dataTracker.startTracking(POSE_ANIM, ANIM_BREATHE); // alive-by-default
        this.dataTracker.startTracking(ATTACK_PULSE, 0);
    }
    //?}

    @Override
    public boolean tryAttack(net.minecraft.entity.Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && !this.getWorld().isClient) {
            // Pulse the swing to clients (wraps safely: the client only watches for CHANGE).
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

    /** Keep the nameplate above the visible body: pose changes the height and the model is scaled.
     *  1.21 replaced getNameLabelHeight with the entity-attachments system, so on 1.21 the nameplate
     *  sits at the default height and doesn't follow the pose (cosmetic-only; revisit if wanted). */
    //? if <1.21 {
    @Override
    public float getNameLabelHeight() {
        float base = switch (getNpcPose()) {
            case POSE_SLEEPING, POSE_PRONE -> 0.5f;
            case POSE_SNEAKING -> 1.7f;
            case POSE_SITTING, POSE_CHILLING -> 1.35f;
            default -> 1.95f;
        };
        return base * getScaleY() + 0.4f; // height follows the vertical axis, not the width
    }
    //?}

    public String getModelId() { return this.dataTracker.get(MODEL); }
    public void setModelId(String id) { this.dataTracker.set(MODEL, (id == null || id.isEmpty()) ? MODEL_HUMANOID : id); }

    public String getSkinType() { return this.dataTracker.get(SKIN_TYPE); }
    public void setSkinType(String t) { this.dataTracker.set(SKIN_TYPE, (t == null || t.isEmpty()) ? SKIN_PRESET : t); }

    public String getSkinValue() { return this.dataTracker.get(SKIN_VALUE); }
    public void setSkinValue(String v) { this.dataTracker.set(SKIN_VALUE, v == null ? "" : v); }

    public boolean isSlim() { return this.dataTracker.get(SLIM); }
    public void setSlim(boolean slim) { this.dataTracker.set(SLIM, slim); }

    private static float clampNpcScale(float s) { return Math.max(0.3f, Math.min(3.0f, s)); }

    public float getScale() { return this.dataTracker.get(SCALE); }
    public void setScale(float scale) { this.dataTracker.set(SCALE, clampNpcScale(scale)); }

    public float getScaleY() { return this.dataTracker.get(SCALE_Y); }
    public void setScaleY(float scale) { this.dataTracker.set(SCALE_Y, clampNpcScale(scale)); }

    public float getScaleZ() { return this.dataTracker.get(SCALE_Z); }
    public void setScaleZ(float scale) { this.dataTracker.set(SCALE_Z, clampNpcScale(scale)); }

    public static final int MAX_BILLBOARD_LINES = 4;
    public static final int MAX_BILLBOARD_LINE_LENGTH = 48;

    /** The floating sign's lines, newline-separated; empty means no sign. */
    public String getBillboard() { return this.dataTracker.get(BILLBOARD); }

    public void setBillboard(String text) {
        if (text == null || text.isBlank()) {
            this.dataTracker.set(BILLBOARD, "");
            return;
        }
        StringBuilder out = new StringBuilder();
        int lines = 0;
        for (String line : text.split("\\n", -1)) {
            if (lines >= MAX_BILLBOARD_LINES) break;
            String trimmed = line.length() > MAX_BILLBOARD_LINE_LENGTH
                    ? line.substring(0, MAX_BILLBOARD_LINE_LENGTH) : line;
            if (lines > 0) out.append('\n');
            out.append(trimmed);
            lines++;
        }
        this.dataTracker.set(BILLBOARD, out.toString());
    }

    /** How far to nudge the floating name, in blocks. */
    public float getNameOffset() { return this.dataTracker.get(NAME_OFFSET); }
    public void setNameOffset(float offset) {
        this.dataTracker.set(NAME_OFFSET, Math.max(-2.0f, Math.min(3.0f, offset)));
    }

    /** Apply a full appearance in one call (used by the editor packet). */
    public void setAppearance(String model, String skinType, String skinValue, boolean slim,
                              float scaleX, float scaleY, float scaleZ, float nameOffset) {
        setModelId(model);
        setSkinType(skinType);
        setSkinValue(skinValue);
        setSlim(slim);
        setScale(scaleX);
        setScaleY(scaleY);
        setScaleZ(scaleZ);
        setNameOffset(nameOffset);
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

    public net.fugginbeenus.notchcurrency.npc.action.NpcActions getActions() { return actions; }

    public void setActions(net.fugginbeenus.notchcurrency.npc.action.NpcActions a) {
        this.actions = a == null ? new net.fugginbeenus.notchcurrency.npc.action.NpcActions() : a;
    }

    public net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule getSchedule() { return schedule; }

    public void setSchedule(net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule s) {
        this.schedule = s == null ? new net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule() : s;
        this.scheduleActive = -1; // re-derive on the next check rather than trusting the old index
    }

    /**
     * Whether the NPC's role can be used right now.
     *
     * <p>Answers yes unless the owner has both built a schedule and asked for its opening hours to be
     * kept, which is the toggle that lets a schedule be pure choreography for anyone who wants the
     * shop open around the clock.
     */
    public boolean isRoleOpenNow() {
        if (!schedule.isActive() || !schedule.enforceHours()) return true;
        var entry = schedule.activeAt(this.getWorld().getTimeOfDay());
        return entry == null || entry.roleOpen();
    }

    /** What to say when someone tries to use the role outside its hours. */
    public String closedLineNow() {
        var entry = schedule.isActive() ? schedule.activeAt(this.getWorld().getTimeOfDay()) : null;
        if (entry != null && !entry.closedLine().isBlank()) return entry.closedLine();
        return "Sorry, we're closed right now.";
    }

    /**
     * Run whatever is wired to a trigger. Server-side only, and cheap to call when nothing is set up:
     * every hook site calls this unconditionally, so the empty case has to cost next to nothing.
     *
     * @param player whoever set it off, or null when nobody did (an NPC drowning, say)
     */
    public void fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger trigger,
                     @Nullable ServerPlayerEntity player) {
        if (this.getWorld().isClient || !actions.has(trigger)) return;
        int cooldown = trigger.cooldownTicks();
        if (cooldown > 0) {
            if (lastFiredAge == null) {
                lastFiredAge = new int[net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.values().length];
                // Far enough back that the first firing always passes, without underflowing the subtraction.
                java.util.Arrays.fill(lastFiredAge, -100000);
            }
            int slot = trigger.ordinal();
            if (this.age - lastFiredAge[slot] < cooldown) return;
            lastFiredAge[slot] = this.age;
        }
        net.fugginbeenus.notchcurrency.npc.action.NpcActionRunner.run(player, this, actions.get(trigger));
    }

    /** Optional goodbye line said in chat when a screen this NPC opened is closed ("" = none). */
    public String getFarewellText() { return farewellText; }
    public void setFarewellText(String text) { this.farewellText = text == null ? "" : text; }

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
     *  NOTE: doors only open while the NPC is actually pathing through one. A Stationary NPC won't. */
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

    /**
     * Whether something counts as this NPC's own side. An NPC with no faction has no allies, so this
     * is false for everyone until a faction is actually set. That's what keeps factions inert for
     * anyone who never touches them.
     */
    public boolean isAlly(@Nullable net.minecraft.entity.Entity other) {
        if (factionId.isEmpty() || other == null) return false;
        if (other instanceof NotchNpcEntity npc) return factionId.equals(npc.getFactionId());
        if (other instanceof ServerPlayerEntity sp) {
            return factionId.equals(net.fugginbeenus.notchcurrency.npc.faction.FactionState
                    .get(sp.getServerWorld()).factionIdOf(sp.getUuid()));
        }
        return false;
    }

    /** True when the other side belongs to a DIFFERENT faction, not merely "isn't an ally". */
    public boolean isRivalFaction(@Nullable net.minecraft.entity.Entity other) {
        if (factionId.isEmpty() || other == null) return false;
        String theirs = null;
        if (other instanceof NotchNpcEntity npc) {
            theirs = npc.getFactionId();
        } else if (other instanceof ServerPlayerEntity sp) {
            theirs = net.fugginbeenus.notchcurrency.npc.faction.FactionState
                    .get(sp.getServerWorld()).factionIdOf(sp.getUuid());
        }
        return theirs != null && !theirs.isEmpty() && !theirs.equals(factionId);
    }

    public int getActionSweepVersion() { return actionSweepVersion; }
    public void setActionSweepVersion(int version) { this.actionSweepVersion = version; }

    public String getFactionId() { return factionId; }
    public void setFactionId(String id) {
        String next = id == null ? "" : id;
        if (!this.factionId.equals(next)) {
            this.factionId = next;
            applyBehaviorGoals(); // targeting rules change with allegiance
        }
    }

    public boolean protectsOwner() { return protectOwner; }
    public void setProtectOwner(boolean protect) {
        if (this.protectOwner != protect) {
            this.protectOwner = protect;
            applyBehaviorGoals();
        }
    }

    public boolean fightsRivalFactions() { return fightRivalFactions; }
    public void setFightRivalFactions(boolean fight) {
        if (this.fightRivalFactions != fight) {
            this.fightRivalFactions = fight;
            applyBehaviorGoals();
        }
    }

    public boolean attacksMonsters() { return attackMonsters; }
    public void setAttackMonsters(boolean attack) {
        if (this.attackMonsters != attack) {
            this.attackMonsters = attack;
            applyBehaviorGoals();
        }
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
    //? if >=1.21 {
    /*public boolean canBeLeashed() {
        return leashable && super.canBeLeashed();
    }
    *///?} else {
    public boolean canBeLeashedBy(PlayerEntity player) {
        return leashable && super.canBeLeashedBy(player);
    }
    //?}

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
        // Top priority: while a player is interacting, hold still and face them (see startTalking).
        this.goalSelector.add(0, new TalkGoal());
        this.lookGoal = new LookAtEntityGoal(this, PlayerEntity.class, 8.0f);
        this.goalSelector.add(6, lookGoal);
    }

    /** A player just interacted: stop wandering and face them for a few seconds (refreshed each time). */
    public void startTalking(PlayerEntity player) {
        this.talkingTo = player.getUuid();
        this.talkingTicks = 160; // ~8s; TalkGoal counts this down and releases when it (or the player) runs out
    }

    /**
     * While {@link #talkingTo} is set, this goal grabs the MOVE and LOOK controls (that alone
     * suspends the wander/look goals), then keeps the NPC stopped and turned toward the player.
     * Releases once the timer expires or the player walks off (or logs out).
     */
    private class TalkGoal extends net.minecraft.entity.ai.goal.Goal {
        TalkGoal() {
            this.setControls(java.util.EnumSet.of(Control.MOVE, Control.LOOK));
        }

        @Nullable
        private PlayerEntity partner() {
            if (talkingTo == null || talkingTicks <= 0) return null;
            PlayerEntity p = getWorld().getPlayerByUuid(talkingTo);
            if (p == null || p.isRemoved() || squaredDistanceTo(p) > 100.0) return null; // ~10 blocks
            return p;
        }

        @Override
        public boolean canStart() {
            return partner() != null;
        }

        @Override
        public boolean shouldContinue() {
            return partner() != null;
        }

        @Override
        public void start() {
            getNavigation().stop();
        }

        @Override
        public void tick() {
            talkingTicks--;
            getNavigation().stop();
            PlayerEntity p = partner();
            if (p != null) {
                getLookControl().lookAt(p, 30.0f, 30.0f);
            }
        }

        @Override
        public void stop() {
            talkingTo = null;
            talkingTicks = 0;
        }
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

        switch (movementBehavior()) {
            case WANDER -> {
                // Short-range strolls every ~2s: livelier than the vanilla far-wander cadence and a
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
                // Stroll the post while idle and stay leashed to home; the fighting itself is set up
                // below, since Guard is only one of the reasons an NPC might swing at something.
                net.minecraft.entity.ai.goal.Goal stroll =
                        new net.minecraft.entity.ai.goal.WanderAroundGoal(this, 0.6, 80, false);
                this.goalSelector.add(5, stroll);
                behaviorGoals.add(stroll);
                applyHomeLeash();
            }
            case STATIONARY -> this.clearPositionTarget();
        }

        // One melee goal, however many reasons there are to fight: a second would fight itself for
        // the movement control.
        boolean fights = behavior == Behavior.GUARD || hostileToPlayers || fightsBack
                || attackMonsters || protectOwner || fightRivalFactions;
        if (fights) {
            net.minecraft.entity.ai.goal.Goal melee =
                    new net.minecraft.entity.ai.goal.MeleeAttackGoal(this, 1.1, true);
            this.goalSelector.add(2, melee);
            behaviorGoals.add(melee);
        }
        if (behavior == Behavior.GUARD || attackMonsters) {
            // Hostile mobs, but never creepers (iron-golem rule: don't walk a blast into the shop).
            net.minecraft.entity.ai.goal.Goal targets = new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                    this, net.minecraft.entity.mob.HostileEntity.class, 10, true, false,
                    e -> !(e instanceof net.minecraft.entity.mob.CreeperEntity));
            this.targetSelector.add(1, targets);
            behaviorTargetGoals.add(targets);
        }
        if (protectOwner) {
            net.minecraft.entity.ai.goal.Goal protect = new NpcProtectOwnerGoal(this);
            this.targetSelector.add(1, protect);
            behaviorTargetGoals.add(protect);
        }
        if (hostileToPlayers) {
            // Hunt ANY player in range, including the owner (hostile means hostile). Vanilla
            // targeting already skips creative/spectator players. Its own faction is still spared:
            // a guard that turns on its own people is nobody's idea of a guard.
            net.minecraft.entity.ai.goal.Goal huntPlayers = new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                    this, net.minecraft.entity.player.PlayerEntity.class, 10, true, false,
                    e -> !isAlly(e));
            this.targetSelector.add(2, huntPlayers);
            behaviorTargetGoals.add(huntPlayers);
        }
        if (fightRivalFactions && !factionId.isEmpty()) {
            // Only people actually flying another faction's colours: bystanders with no faction are
            // left alone, so a faction war doesn't sweep up everyone who never joined.
            net.minecraft.entity.ai.goal.Goal rivals = new net.minecraft.entity.ai.goal.ActiveTargetGoal<>(
                    this, net.minecraft.entity.LivingEntity.class, 10, true, false,
                    this::isRivalFaction);
            this.targetSelector.add(2, rivals);
            behaviorTargetGoals.add(rivals);
        }
        if (fightsBack) {
            net.minecraft.entity.ai.goal.Goal revenge = new net.minecraft.entity.ai.goal.RevengeGoal(this);
            this.targetSelector.add(1, revenge);
            behaviorTargetGoals.add(revenge);
        }
        applyDoorCapability(); // re-assert door pathing/goal after the goal list is rebuilt
    }

    /**
     * Where the NPC is actually being moved to right now: the schedule when one is running, the
     * Moves tab otherwise. Combat deliberately keeps reading the configured behaviour instead, since
     * a guard standing a scheduled post is still a guard.
     */
    private Behavior movementBehavior() {
        return scheduleBehavior != null ? scheduleBehavior : behavior;
    }

    @Nullable
    private net.minecraft.util.math.BlockPos leashHome() {
        return scheduleBehavior != null ? scheduleHome : homePos;
    }

    private int leashRadius() {
        return scheduleBehavior != null ? scheduleRadius : wanderRadius;
    }

    private void applyHomeLeash() {
        net.minecraft.util.math.BlockPos home = leashHome();
        if (home != null) {
            this.setPositionTarget(home, Math.max(2, leashRadius()));
        }
    }

    @Override
    public void tickMovement() {
        super.tickMovement();
        if (!this.getWorld().isClient) {
            if (movementBehavior() == Behavior.STATIONARY) {
                // Reading the leash point rather than the configured home is what gives a scheduled
                // NPC its commute: the same walk-back that returns a guard to its post after a fight
                // is what carries a shopkeeper to the counter at opening time, and to bed at night.
                net.minecraft.util.math.BlockPos post = leashHome();
                if (this.getTarget() != null && this.getTarget().isAlive()) {
                    // In combat (hostile/fights-back): let the attack goal chase.
                } else if (post != null && this.squaredDistanceTo(
                        post.getX() + 0.5, post.getY(), post.getZ() + 0.5) > 2.25) {
                    // Combat over (or shoved): walk back to the post before locking down again.
                    this.getNavigation().startMovingTo(
                            post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
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
            tickProximity();
            tickSchedule();
            tickScheduleSleep();
            net.fugginbeenus.notchcurrency.npc.action.NpcActionSweep.sweep(this);
        }
    }

    /**
     * Advance the NPC's day.
     *
     * <p>Called every tick, and built so that costs almost nothing. An NPC with no schedule leaves on
     * the first line. One with a schedule looks at the clock once a second, and while it is still
     * inside the same entry (which is nearly always) it leaves on the third. Only a real transition,
     * a handful of times a day, does any work. A town of scheduled NPCs is idle between those.
     *
     * <p>Nothing here remembers where the NPC "was up to". The active entry is derived from the time
     * of day every check, which is why an NPC can be unloaded for a week and still pick up correctly:
     * there is no progress to lose.
     */
    private void tickSchedule() {
        boolean runnable = schedule.isActive()
                && net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.dimensionSupports(this.getWorld());
        if (!runnable) {
            // Switched off, emptied, or carried somewhere with no day: hand the NPC back to whatever
            // the Moves tab says instead of leaving it frozen in the last stance it was given.
            if (scheduleBehavior != null) releaseSchedule();
            return;
        }
        if (this.age % 20 != 0) return;

        int idx = schedule.indexAt(this.getWorld().getTimeOfDay());
        if (idx == scheduleActive) return;

        // A first application after loading is not a transition. Firing entry actions here would mean
        // a shopkeeper announcing opening hours every time somebody walks into the chunk.
        boolean transition = scheduleActive != -1;
        scheduleActive = idx;
        applyScheduleEntry(schedule.get(idx), transition);
    }

    /**
     * Put the NPC into an entry's stance by driving the behaviour it already has.
     *
     * <p>The override fields are set together and the goal list rebuilt once at the end. Going
     * through the public setters would rebuild it three times and, worse, would write the entry's
     * spot and radius over the owner's own settings.
     */
    private void applyScheduleEntry(@Nullable net.fugginbeenus.notchcurrency.npc.schedule.ScheduleEntry entry,
                                    boolean fireActions) {
        if (entry == null) {
            releaseSchedule();
            return;
        }
        scheduleHome = entry.anchor();
        switch (entry.stance()) {
            case WANDER -> {
                scheduleBehavior = Behavior.WANDER;
                scheduleRadius = entry.radius();
            }
            case PATROL -> {
                scheduleBehavior = Behavior.PATROL;
                scheduleHome = null; // waypoints may run well outside any leash
            }
            // Both mean "be at that block". The stationary walk-back in tickMovement does the
            // travelling, so there is no second pathing system to keep in step with the first.
            case STAND, SLEEP -> {
                scheduleBehavior = Behavior.STATIONARY;
                scheduleRadius = 2;
            }
        }
        holdPose(entry.stance() == net.fugginbeenus.notchcurrency.npc.schedule.NpcStance.SLEEP
                ? POSE_SLEEPING : -1);
        applyBehaviorGoals();

        if (fireActions && !entry.onBegin().isEmpty()) {
            net.fugginbeenus.notchcurrency.npc.action.NpcActionRunner.run(null, this, entry.onBegin());
        }
    }

    /**
     * Get an NPC on a Sleep entry actually into its bed, the way a villager does.
     *
     * <p>Posing it asleep is not enough on its own. Lying down is one thing; being <em>in</em> the bed
     * is another, and that comes from vanilla's own sleep call: it claims the bed, snaps the body onto
     * it, and orients it to the headboard. Without that an NPC stands beside the bed doing a lying-down
     * impression, which is what it was doing.
     *
     * <p>It also has to stop looking around. The watch-players goal keeps turning the head, which on a
     * sleeping body reads as broken, so that goal is stood down for as long as the NPC is asleep.
     */
    private void tickScheduleSleep() {
        var entry = currentScheduleEntry();
        boolean wantsBed = entry != null
                && entry.stance() == net.fugginbeenus.notchcurrency.npc.schedule.NpcStance.SLEEP
                && entry.anchor() != null;

        if (!wantsBed) {
            if (this.isSleeping()) {
                this.wakeUp();
                restoreLookGoal();
            }
            return;
        }

        net.minecraft.util.math.BlockPos bed = bedHead(entry.anchor());
        if (this.isSleeping()) {
            // Somebody mined the bed out from under it: stand up rather than float there.
            if (!(this.getWorld().getBlockState(bed).getBlock() instanceof net.minecraft.block.BedBlock)) {
                this.wakeUp();
                restoreLookGoal();
                return;
            }
            this.getNavigation().stop();
            return;
        }

        // Still walking over. Climb in once close enough to reach it.
        if (!(this.getWorld().getBlockState(bed).getBlock() instanceof net.minecraft.block.BedBlock)) return;
        double dx = bed.getX() + 0.5 - this.getX();
        double dz = bed.getZ() + 0.5 - this.getZ();
        if (dx * dx + dz * dz > 4.0 || Math.abs(bed.getY() - this.getY()) > 2.0) return;

        this.sleep(bed);
        this.getNavigation().stop();
        if (lookGoal != null) this.goalSelector.remove(lookGoal);
    }

    /**
     * Resolve a bed anchor to the head half of the bed.
     *
     * <p>Anchors are normalised when they're set, but schedules built before that, and any hand-edited
     * config, can still point at the foot. Doing it again here costs a block lookup and saves the NPC
     * lying a block down the bed with half of it hanging off the end.
     */
    private net.minecraft.util.math.BlockPos bedHead(net.minecraft.util.math.BlockPos pos) {
        net.minecraft.block.BlockState state = this.getWorld().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.block.BedBlock
                && state.get(net.minecraft.block.BedBlock.PART) == net.minecraft.block.enums.BedPart.FOOT) {
            return pos.offset(state.get(net.minecraft.block.BedBlock.FACING));
        }
        return pos;
    }

    /** Put the watch-players goal back after a nap. Removing first is what keeps a second copy of it
     *  from stacking up, the same way the toggle itself does it. */
    private void restoreLookGoal() {
        if (lookGoal == null) return;
        this.goalSelector.remove(lookGoal);
        if (watchPlayers) this.goalSelector.add(6, lookGoal);
    }

    /** Drop the schedule's grip and let the configured behaviour take back over. */
    private void releaseSchedule() {
        scheduleBehavior = null;
        scheduleHome = null;
        scheduleActive = -1;
        holdPose(-1);
        applyBehaviorGoals();
    }

    /** Borrow the pose for a stance that needs one, or hand it back. Pass -1 to release. */
    private void holdPose(int wanted) {
        if (wanted >= 0) {
            if (poseBeforeSchedule < 0) poseBeforeSchedule = getNpcPose();
            if (getNpcPose() != wanted) setNpcPose(wanted);
        } else if (poseBeforeSchedule >= 0) {
            setNpcPose(poseBeforeSchedule);
            poseBeforeSchedule = -1;
        }
    }

    /** The entry governing right now, or null when no schedule is running. Read by the goal and by
     *  the opening-hours check, both of which want the same answer. */
    @Nullable
    public net.fugginbeenus.notchcurrency.npc.schedule.ScheduleEntry currentScheduleEntry() {
        if (!schedule.isActive()) return null;
        if (!net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.dimensionSupports(this.getWorld())) return null;
        return schedule.activeAt(this.getWorld().getTimeOfDay());
    }

    /**
     * Fire the proximity trigger as players walk up. Server-side, called every tick, so the very first
     * line is the check that costs nothing for the NPCs (nearly all of them) that don't use it.
     *
     * <p>Fires on the way IN only, and a player has to leave properly before it can happen again: the
     * boundary they leave by is wider than the one they arrive at, so standing right on the edge can't
     * make it stutter.
     */
    private void tickProximity() {
        if (!actions.has(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_PROXIMITY)) return;
        if (this.age % PROXIMITY_SCAN_TICKS != 0) return;
        if (manualInvisible || isRuleHidden()) return; // a hidden NPC shouldn't greet anyone

        if (proximityInside == null) {
            proximityInside = new java.util.HashSet<>();
            proximityFired = new java.util.HashMap<>();
        }
        double radius = actions.proximityRadius();
        double enterSq = radius * radius;
        double leaveSq = (radius + 2.0) * (radius + 2.0);

        for (net.minecraft.entity.player.PlayerEntity generic : this.getWorld().getPlayers()) {
            if (!(generic instanceof ServerPlayerEntity player)) continue;
            if (player.isSpectator() || !player.isAlive()) continue;
            java.util.UUID id = player.getUuid();
            double distanceSq = player.squaredDistanceTo(this);
            if (!proximityInside.contains(id)) {
                if (distanceSq > enterSq) continue;
                proximityInside.add(id);
                Integer last = proximityFired.get(id);
                if (last == null || this.age - last >= PROXIMITY_RECHARGE_TICKS) {
                    proximityFired.put(id, this.age);
                    fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_PROXIMITY, player);
                }
            } else if (distanceSq > leaveSq) {
                proximityInside.remove(id);
            }
        }

        // Anyone who logged out or changed world counts as gone, and spent cooldowns are dropped so
        // neither collection grows with everyone who ever walked past.
        proximityInside.removeIf(id -> this.getWorld().getPlayerByUuid(id) == null);
        proximityFired.entrySet().removeIf(e ->
                !proximityInside.contains(e.getKey()) && this.age - e.getValue() > PROXIMITY_RECHARGE_TICKS);
    }

    @Override
    protected void pushAway(net.minecraft.entity.Entity entity) {
        if (pushable) super.pushAway(entity); // hold ground unless the Pushable ability is on
    }

    @Override
    public void checkDespawn() {
        // Never despawn. These are placed, persistent NPCs. Also keep the despawn counter at zero:
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
            startTalking(player); // pause + face the player while they're dealing with us
            // Before dialogue or the role screen takes over, so a greeting is read first.
            fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_INTERACT, sp);
            if (sp.isSneaking() && canEdit(sp)) {
                NotchNpcManager.openEditor(sp, this);
            } else if (!net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.open(sp, this)) {
                // No dialogue: go straight to the role.
                NotchNpcManager.dispatchRole(sp, this);
            }
        }
        return ActionResult.SUCCESS;
    }

    // ---- damage protection (owned NPCs are protected, like the old shopkeeper) ----

    @Override
    public boolean damage(DamageSource source, float amount) {
        if (this.getWorld().isClient()) return false;
        // Fires on being HIT, not on damage getting through. Protection is on by default, so an
        // "if it takes damage" reading would never run for an ordinary shopkeeper, and a shopkeeper
        // snapping at someone who punched it is the whole point.
        if (!this.isDead() && amount > 0) {
            fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_HURT,
                    source.getAttacker() instanceof ServerPlayerEntity p ? p : null);
        }
        if (protectedNpc && (owner != null || ownerType == OwnerType.SERVER)) {
            // Only the void or /kill can remove a protected NPC.
            if (source.isIn(net.minecraft.registry.tag.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                return super.damage(source, amount);
            }
            // The hit is cancelled, but Fights Back still needs to know who swung: record the
            // attacker so the RevengeGoal can retaliate even while the NPC itself is unhurtable.
            if (fightsBack && source.getAttacker() instanceof net.minecraft.entity.LivingEntity attacker) {
                this.setAttacker(attacker);
            }
            return false;
        }
        return super.damage(source, amount);
    }

    @Override
    public void onDeath(DamageSource source) {
        // Before super, while the NPC is still in the world and its actions can still reference it.
        // The killer may be nobody at all: lava and fall damage count.
        fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_DEATH,
                source.getAttacker() instanceof ServerPlayerEntity p ? p : null);
        super.onDeath(source);
    }

    @Override
    public boolean onKilledOther(net.minecraft.server.world.ServerWorld world,
                                 net.minecraft.entity.LivingEntity other) {
        boolean result = super.onKilledOther(world, other);
        // A player only comes along when the NPC killed a player, otherwise there's no one to talk to.
        fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_KILL,
                other instanceof ServerPlayerEntity p ? p : null);
        return result;
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
        nbt.putFloat("ScaleY", getScaleY());
        nbt.putFloat("ScaleZ", getScaleZ());
        nbt.putFloat("NameOffset", getNameOffset());
        nbt.putString("Billboard", getBillboard());
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
        nbt.putString("Farewell", farewellText);
        if (!actions.isEmpty()) nbt.put("Actions", actions.toNbt());
        if (schedule.isEnabled() || !schedule.isEmpty()) nbt.put("Schedule", schedule.toNbt());
        if (poseBeforeSchedule >= 0) nbt.putInt("PoseBeforeSchedule", poseBeforeSchedule);
        // Stats: the vanilla flags are re-recorded here so they survive the pick-up item too.
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
        nbt.putBoolean("ProtectOwner", protectOwner);
        nbt.putBoolean("AttackMonsters", attackMonsters);
        nbt.putString("Faction", factionId);
        nbt.putInt("ActionSweep", actionSweepVersion);
        nbt.putBoolean("FightRivalFactions", fightRivalFactions);
        // Attribute bases: recorded so they survive the pick-up item (entity NBT has them anyway).
        nbt.putInt("StatMaxHealth", (int) Math.round(this.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH)));
        nbt.putInt("StatSpeedPct", (int) Math.round(this.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED) * 100));
        // Equipment: re-recorded so it survives the pick-up item too.
        NbtCompound equip = new NbtCompound();
        for (net.minecraft.entity.EquipmentSlot slot : net.minecraft.entity.EquipmentSlot.values()) {
            net.minecraft.item.ItemStack st = this.getEquippedStack(slot);
            if (!st.isEmpty()) equip.put(slot.getName(), net.fugginbeenus.notchcurrency.compat.StackData.writeStack(st));
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
        // Older NPCs only stored one scale: fall back to it so they stay the shape they were.
        setScaleY(nbt.contains("ScaleY") ? nbt.getFloat("ScaleY") : getScale());
        setScaleZ(nbt.contains("ScaleZ") ? nbt.getFloat("ScaleZ") : getScale());
        if (nbt.contains("NameOffset")) setNameOffset(nbt.getFloat("NameOffset"));
        if (nbt.contains("Billboard")) setBillboard(nbt.getString("Billboard"));
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
        if (nbt.contains("Farewell")) farewellText = nbt.getString("Farewell");
        actions = net.fugginbeenus.notchcurrency.npc.action.NpcActions.fromNbt(
                nbt.contains("Actions") ? nbt.getCompound("Actions") : null);
        poseBeforeSchedule = nbt.contains("PoseBeforeSchedule") ? nbt.getInt("PoseBeforeSchedule") : -1;
        setSchedule(nbt.contains("Schedule")
                ? net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.fromNbt(nbt.getCompound("Schedule"))
                : new net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule());
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
        if (nbt.contains("ProtectOwner")) protectOwner = nbt.getBoolean("ProtectOwner");
        if (nbt.contains("AttackMonsters")) attackMonsters = nbt.getBoolean("AttackMonsters");
        if (nbt.contains("Faction")) factionId = nbt.getString("Faction");
        actionSweepVersion = nbt.getInt("ActionSweep"); // absent = 0 = never swept
        if (nbt.contains("FightRivalFactions")) fightRivalFactions = nbt.getBoolean("FightRivalFactions");
        if (nbt.contains("WatchPlayers")) setWatchPlayers(nbt.getBoolean("WatchPlayers"));
        if (nbt.contains("StatMaxHealth")) {
            int hp = nbt.getInt("StatMaxHealth");
            int speedPct = nbt.contains("StatSpeedPct") ? nbt.getInt("StatSpeedPct") : 30;
            // Skip when they already match (world reload path: vanilla restored the attributes
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
                    this.equipStack(slot, net.fugginbeenus.notchcurrency.compat.StackData.readStack(equip.getCompound(slot.getName())));
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
