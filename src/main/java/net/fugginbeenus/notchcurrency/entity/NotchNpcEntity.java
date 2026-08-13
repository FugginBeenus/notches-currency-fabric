package net.fugginbeenus.notchcurrency.entity;

import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
// Only the types this class actually names in a signature. The utility calls live in compat/Geo;
// these cannot, because they appear in an implements clause and in override signatures.
// GeckoLib moves these at every major version. 5.x (MC 1.21.5 and up) put AnimatableManager under
// animatable.manager and PlayState under animation.object, and replaced AnimationState with
// AnimationTest. Only the names moved; the calls below are the same on both.
//? if >=1.21.5 {
/*import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animatable.manager.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.state.AnimationTest;
import software.bernie.geckolib.animation.object.PlayState;
*///?} elif >=1.21 {
/*import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
*///?} else {
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
//?}

import java.util.UUID;

public class NotchNpcEntity extends PathfinderMob implements GeoEntity {

    public enum OwnerType { PLAYER, SERVER }

    public enum Behavior { STATIONARY, WANDER, FOLLOW_OWNER, PATROL, GUARD }

    public enum DialogueMode { WINDOW, CHAT }

    private static final String IDLE_ANIM = "animation.notch_npc.idle";
    private static final String WALK_ANIM = "animation.notch_npc.walk";
    /** The flourishes, with how long one pass of each takes in ticks. */
    private static final String[] SPECIAL_ANIMS = {
            "animation.notch_npc.special_idle1",
            "animation.notch_npc.special_idle2",
            "animation.notch_npc.special_idle3",
    };
    private static final int[] SPECIAL_TICKS = {215, 341, 234};
    /** How often a lively NPC considers doing something other than standing there. */
    private static final int FLOURISH_EVERY = 600;
    private final AnimatableInstanceCache geoCache = net.fugginbeenus.notchcurrency.compat.Geo.cache(this);

    // Model + skin identifiers.
    public static final String MODEL_HUMANOID = "humanoid";
    public static final String MODEL_APPLY = "apply";
    public static final String SKIN_PRESET = "preset";
    public static final String SKIN_PLAYER = "player";
    public static final String SKIN_URL = "url";
    public static final String SKIN_VARIANT = "variant";

    // Synced appearance (so the client renderer reflects edits live).
    private static final EntityDataAccessor<String> MODEL =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_TYPE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SKIN_VALUE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Boolean> SLIM =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> TALK_BUBBLE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SCALE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE_Y =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SCALE_Z =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> NAME_OFFSET =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<String> BILLBOARD =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<String> SUBTITLE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.STRING);
    private static final EntityDataAccessor<Integer> NPC_POSE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.INT);

    public static final int POSE_STANDING = 0;
    public static final int POSE_SITTING = 1;
    public static final int POSE_SNEAKING = 2;
    public static final int POSE_SLEEPING = 3;
    public static final int POSE_CHILLING = 4; // reclined sit
    public static final int POSE_PRONE = 5;    // lying face-down (vanilla swim pose)
    public static final int POSE_WAVING = 6;   // arm raised in greeting
    public static final int POSE_CUSTOM = 7;   // per-part rotations from the pose editor

    private static final EntityDataAccessor<CompoundTag> CUSTOM_POSE =
            SynchedEntityData.defineId(NotchNpcEntity.class,
                    net.fugginbeenus.notchcurrency.compat.Sync.compound());

    private static final EntityDataAccessor<Integer> POSE_ANIM =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.INT);

    /**
     * A clip name to play instead of the built-in idle, or empty to work it out automatically.
     *
     * <p>A name rather than a number, because the whole point is that the mod does not know what a
     * resource pack will call the motions it adds.
     */
    private static final EntityDataAccessor<String> CUSTOM_CLIP =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.STRING);

    private static final EntityDataAccessor<Integer> ATTACK_PULSE =
            SynchedEntityData.defineId(NotchNpcEntity.class, EntityDataSerializers.INT);

    public float clientSwingStartAge = -1000f;
    private int lastSeenAttackPulse = -1;

    public static final int ANIM_STATUE = 0;  // truly frozen (vanilla's idle arm bob removed too)
    public static final int ANIM_BREATHE = 1; // the normal vanilla idle look (default)
    public static final int ANIM_LIVELY = 2;  // breathing chest + body sway + slow head glances
    public static final int ANIM_COUNT = 3;

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
    @Nullable private net.minecraft.core.BlockPos homePos = null;
    private final java.util.List<net.minecraft.core.BlockPos> waypoints = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.world.entity.ai.goal.Goal> behaviorGoals = new java.util.ArrayList<>();
    private final java.util.List<net.minecraft.world.entity.ai.goal.Goal> behaviorTargetGoals = new java.util.ArrayList<>();

    // Branching dialogue. Empty = interaction goes straight to the role.
    private net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree dialogue =
            new net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree();
    private DialogueMode dialogueMode = DialogueMode.WINDOW;
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
    private String factionId = "";
    private int actionSweepVersion = 0;
    private String voiceSound = "";
    private int voicePitch = 100;

    private net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule schedule =
            new net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule();
    private int scheduleActive = -1;

    // A running schedule steers the NPC through these rather than through the configured behaviour,
    // home and radius. Driving the saved fields directly would have the schedule quietly rewriting
    // what the owner set on the Moves tab, and switching the schedule off would leave that damage
    // behind. Kept out of NBT on purpose: they are derived, and they rebuild themselves on load.
    @Nullable private Behavior scheduleBehavior = null;
    private int poseBeforeSchedule = -1;
    @Nullable private net.minecraft.core.BlockPos scheduleHome = null;
    private int scheduleRadius = 8;
    private int regen = 0; // half-hearts healed every 5 seconds
    @Nullable private net.minecraft.world.entity.ai.goal.Goal doorGoal = null;

    // While a player is interacting, the NPC holds still and faces them (see TalkGoal). Refreshed on
    // each interaction; expires so it resumes wandering once the player leaves or a few seconds pass.
    @Nullable private java.util.UUID talkingTo = null;
    private int talkingTicks = 0;

    // Proximity bookkeeping. Both are created only for NPCs that actually use the trigger, so the
    // ordinary NPC carries two null references and nothing else.
    @Nullable private java.util.Set<java.util.UUID> proximityInside = null;
    @Nullable private java.util.Map<java.util.UUID, Integer> proximityFired = null;
    @Nullable private int[] lastFiredAge = null;
    private static final int PROXIMITY_SCAN_TICKS = 10;
    private static final int PROXIMITY_RECHARGE_TICKS = 200;

    // Moves-tab granularity.
    private String followPlayerName = ""; // blank = follow the owner
    private boolean avoidMonsters = false;
    private boolean watchPlayers = true; // the look-at-passers-by goal
    // No initializer: initGoals() runs from the super constructor and sets this before
    // field initializers would run (an "= null" here would wipe the reference).
    @Nullable private LookAtPlayerGoal lookGoal;

    // Display rule: when the NPC exists to be seen. Hidden = invisible + non-interactive.
    public static final int VIS_ALWAYS = 0, VIS_DAY = 1, VIS_NIGHT = 2;
    private int visibility = VIS_ALWAYS;
    private boolean manualInvisible = false; // the stats-screen Invisible toggle

    // Handler id for the CUSTOM role (registered by other mods via NotchNpcApi).
    private String customRoleId = "";

    public NotchNpcEntity(EntityType<? extends NotchNpcEntity> type, Level world) {
        super(type, world);
        this.setPersistenceRequired();
    }

    @Override
    //? if >=1.21 {
    /*protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(MODEL, MODEL_HUMANOID);
        builder.define(SKIN_TYPE, SKIN_PRESET);
        builder.define(SKIN_VALUE, "1");
        builder.define(SLIM, false);
        builder.define(TALK_BUBBLE, false);
        builder.define(SCALE, 1.0f);
        builder.define(SCALE_Y, 1.0f);
        builder.define(SCALE_Z, 1.0f);
        builder.define(NAME_OFFSET, 0.0f);
        builder.define(BILLBOARD, "");
        builder.define(SUBTITLE, "");
        builder.define(NPC_POSE, POSE_STANDING);
        builder.define(CUSTOM_POSE, new CompoundTag());
        builder.define(POSE_ANIM, ANIM_BREATHE); // alive-by-default
        builder.define(CUSTOM_CLIP, "");
        builder.define(ATTACK_PULSE, 0);
    }
    *///?} else {
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MODEL, MODEL_HUMANOID);
        this.entityData.define(SKIN_TYPE, SKIN_PRESET);
        this.entityData.define(SKIN_VALUE, "1");
        this.entityData.define(SLIM, false);
        this.entityData.define(TALK_BUBBLE, false);
        this.entityData.define(SCALE, 1.0f);
        this.entityData.define(SCALE_Y, 1.0f);
        this.entityData.define(SCALE_Z, 1.0f);
        this.entityData.define(NAME_OFFSET, 0.0f);
        this.entityData.define(BILLBOARD, "");
        this.entityData.define(SUBTITLE, "");
        this.entityData.define(NPC_POSE, POSE_STANDING);
        this.entityData.define(CUSTOM_POSE, new CompoundTag());
        this.entityData.define(POSE_ANIM, ANIM_BREATHE); // alive-by-default
        this.entityData.define(CUSTOM_CLIP, "");
        this.entityData.define(ATTACK_PULSE, 0);
    }
    //?}

    // 1.21.11 handed the server level to every combat hook rather than making them dig it back out.
    //? if >=1.21.11 {
    /*@Override
    public boolean doHurtTarget(net.minecraft.server.level.ServerLevel level,
                                net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(level, target);
    *///?} else {
    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hit = super.doHurtTarget(target);
    //?}
        if (hit && !this.level().isClientSide) {
            // Pulse the swing to clients (wraps safely: the client only watches for CHANGE).
            this.entityData.set(ATTACK_PULSE, this.entityData.get(ATTACK_PULSE) + 1);
        }
        return hit;
    }

    public String getCustomClip() { return this.entityData.get(CUSTOM_CLIP); }
    public void setCustomClip(String clip) {
        this.entityData.set(CUSTOM_CLIP, clip == null ? "" : clip.strip());
    }

    public int getPoseAnim() { return this.entityData.get(POSE_ANIM); }
    public void setPoseAnim(int anim) {
        this.entityData.set(POSE_ANIM, Math.max(0, Math.min(ANIM_COUNT - 1, anim)));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> data) {
        super.onSyncedDataUpdated(data);
        if (CUSTOM_POSE.equals(data)) {
            customPoseCache = unpackCustomPose(this.entityData.get(CUSTOM_POSE));
        }
        if (ATTACK_PULSE.equals(data)) {
            int pulse = this.entityData.get(ATTACK_PULSE);
            // First sync just sets the baseline; a CHANGE afterwards means a fresh melee hit.
            if (lastSeenAttackPulse >= 0 && pulse != lastSeenAttackPulse) {
                clientSwingStartAge = this.tickCount;
            }
            lastSeenAttackPulse = pulse;
        }
    }

    @Nullable
    private static float[] unpackCustomPose(CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) return null;
        float[] out = new float[18];
        for (int part = 0; part < 6; part++) {
            int[] rot = net.fugginbeenus.notchcurrency.compat.Nbt.intArray(nbt, Integer.toString(part));
            if (rot.length == 3) {
                out[part * 3] = rot[0];
                out[part * 3 + 1] = rot[1];
                out[part * 3 + 2] = rot[2];
            }
        }
        return out;
    }

    @Nullable
    public float[] getCustomPoseAngles() {
        return customPoseCache;
    }

    public void setCustomPosePart(int part, int x, int y, int z) {
        CompoundTag pose = this.entityData.get(CUSTOM_POSE).copy();
        if (part < 0) {
            pose = new CompoundTag();
        } else if (part < 6) {
            pose.putIntArray(Integer.toString(part), new int[]{clampDeg(x), clampDeg(y), clampDeg(z)});
        }
        this.entityData.set(CUSTOM_POSE, pose);
        customPoseCache = unpackCustomPose(pose); // keep the server-side copy fresh too
    }

    private static int clampDeg(int deg) {
        return Math.max(-180, Math.min(180, deg));
    }

    public int getNpcPose() { return this.entityData.get(NPC_POSE); }

    public void setNpcPose(int pose) {
        int clamped = Math.max(POSE_STANDING, Math.min(POSE_CUSTOM, pose));
        this.entityData.set(NPC_POSE, clamped);
        this.setPose(entityPoseFor(clamped));
    }

    private static net.minecraft.world.entity.Pose entityPoseFor(int npcPose) {
        return switch (npcPose) {
            case POSE_SNEAKING -> net.minecraft.world.entity.Pose.CROUCHING;
            case POSE_SLEEPING -> net.minecraft.world.entity.Pose.SLEEPING;
            case POSE_PRONE -> net.minecraft.world.entity.Pose.SWIMMING;
            default -> net.minecraft.world.entity.Pose.STANDING; // sitting/chilling are model-level
        };
    }

    //? if <1.21 {
    @Override
    public float getNameTagOffsetY() {
        float base = switch (getNpcPose()) {
            case POSE_SLEEPING, POSE_PRONE -> 0.5f;
            case POSE_SNEAKING -> 1.7f;
            case POSE_SITTING, POSE_CHILLING -> 1.35f;
            default -> 1.95f;
        };
        return base * getScaleY() + 0.4f; // height follows the vertical axis, not the width
    }
    //?}

    public String getModelId() { return this.entityData.get(MODEL); }
    public void setModelId(String id) { this.entityData.set(MODEL, (id == null || id.isEmpty()) ? MODEL_HUMANOID : id); }

    public String getSkinType() { return this.entityData.get(SKIN_TYPE); }
    public void setSkinType(String t) { this.entityData.set(SKIN_TYPE, (t == null || t.isEmpty()) ? SKIN_PRESET : t); }

    public String getSkinValue() { return this.entityData.get(SKIN_VALUE); }
    public void setSkinValue(String v) { this.entityData.set(SKIN_VALUE, v == null ? "" : v); }

    public boolean isSlim() { return this.entityData.get(SLIM); }
    public void setSlim(boolean slim) { this.entityData.set(SLIM, slim); }

    /** A bubble hanging over the NPC's head, to mark it as worth talking to. */
    public boolean showsTalkBubble() { return this.entityData.get(TALK_BUBBLE); }
    public void setTalkBubble(boolean show) { this.entityData.set(TALK_BUBBLE, show); }

    private static float clampNpcScale(float s) { return Math.max(0.3f, Math.min(3.0f, s)); }

    // The X axis of our own per-axis scale, which the renderers apply themselves. It doubles as
    // vanilla's entity scale, which is what makes a tall NPC's hitbox tall too. 1.21.11 made
    // getScale final and had it read the scale attribute, so there the value is pushed into that
    // attribute instead of overriding, and the hitbox still follows.
    public float npcScale() { return this.entityData.get(SCALE); }

    //? if <1.21.11 {
    @Override
    public float getScale() { return npcScale(); }
    //?}

    public void setScale(float scale) {
        float clamped = clampNpcScale(scale);
        this.entityData.set(SCALE, clamped);
        //? if >=1.21.11 {
        /*net.minecraft.world.entity.ai.attributes.AttributeInstance scaleAttr =
                this.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.SCALE);
        if (scaleAttr != null) scaleAttr.setBaseValue(clamped);
        *///?}
    }

    public float getScaleY() { return this.entityData.get(SCALE_Y); }
    public void setScaleY(float scale) { this.entityData.set(SCALE_Y, clampNpcScale(scale)); }

    public float getScaleZ() { return this.entityData.get(SCALE_Z); }
    public void setScaleZ(float scale) { this.entityData.set(SCALE_Z, clampNpcScale(scale)); }

    public static final int MAX_SUBTITLE_LENGTH = 32;

    public String getSubtitle() { return this.entityData.get(SUBTITLE); }

    public void setSubtitle(String text) {
        String out = text == null ? "" : text.replace('\n', ' ').trim();
        if (out.length() > MAX_SUBTITLE_LENGTH) out = out.substring(0, MAX_SUBTITLE_LENGTH);
        this.entityData.set(SUBTITLE, out);
    }

    public String getVoice() { return voiceSound; }

    public void setVoice(String soundId) {
        this.voiceSound = soundId == null ? "" : soundId;
    }

    public int getVoicePitchPercent() { return voicePitch; }

    public void setVoicePitchPercent(int percent) {
        this.voicePitch = Math.max(50, Math.min(200, percent));
    }

    public void playVoice() {
        if (voiceSound.isEmpty() || this.isSilent()) return;
        net.minecraft.resources.ResourceLocation id = net.minecraft.resources.ResourceLocation.tryParse(voiceSound);
        if (id == null) return;
        net.minecraft.sounds.SoundEvent sound = net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound == null) return;
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), sound,
                net.minecraft.sounds.SoundSource.NEUTRAL, 1.0f, voicePitch / 100.0f);
    }

    public static final int MAX_BILLBOARD_LINES = 4;
    public static final int MAX_BILLBOARD_LINE_LENGTH = 48;

    public String getBillboard() { return this.entityData.get(BILLBOARD); }

    public void setBillboard(String text) {
        if (text == null || text.isBlank()) {
            this.entityData.set(BILLBOARD, "");
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
        this.entityData.set(BILLBOARD, out.toString());
    }

    public float getNameOffset() { return this.entityData.get(NAME_OFFSET); }
    public void setNameOffset(float offset) {
        this.entityData.set(NAME_OFFSET, Math.max(-2.0f, Math.min(3.0f, offset)));
    }

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

    public void setHome(net.minecraft.core.BlockPos pos) {
        this.homePos = pos == null ? null : pos.immutable();
        applyBehaviorGoals();
    }

    public java.util.List<net.minecraft.core.BlockPos> getWaypoints() { return waypoints; }

    public boolean addWaypoint(net.minecraft.core.BlockPos pos) {
        if (waypoints.size() >= 16) return false;
        waypoints.add(pos.immutable());
        return true;
    }

    public void clearWaypoints() {
        waypoints.clear();
    }

    public boolean removeLastWaypoint() {
        if (waypoints.isEmpty()) return false;
        waypoints.remove(waypoints.size() - 1);
        return true;
    }

    public float getPatrolSpeed() { return patrolSpeed; }
    public void setPatrolSpeed(float speed) { this.patrolSpeed = Math.max(0.3f, Math.min(1.5f, speed)); }

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

    public boolean isRoleOpenNow() {
        if (!schedule.isActive() || !schedule.enforceHours()) return true;
        var entry = schedule.activeAt(this.level().getDayTime());
        return entry == null || entry.roleOpen();
    }

    public String closedLineNow() {
        var entry = schedule.isActive() ? schedule.activeAt(this.level().getDayTime()) : null;
        if (entry != null && !entry.closedLine().isBlank()) return entry.closedLine();
        return "Sorry, we're closed right now.";
    }

    public void fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger trigger,
                     @Nullable ServerPlayer player) {
        if (this.level().isClientSide || !actions.has(trigger)) return;
        int cooldown = trigger.cooldownTicks();
        if (cooldown > 0) {
            if (lastFiredAge == null) {
                lastFiredAge = new int[net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.values().length];
                // Far enough back that the first firing always passes, without underflowing the subtraction.
                java.util.Arrays.fill(lastFiredAge, -100000);
            }
            int slot = trigger.ordinal();
            if (this.tickCount - lastFiredAge[slot] < cooldown) return;
            lastFiredAge[slot] = this.tickCount;
        }
        net.fugginbeenus.notchcurrency.npc.action.NpcActionRunner.run(player, this, actions.get(trigger));
    }

    public String getFarewellText() { return farewellText; }
    public void setFarewellText(String text) { this.farewellText = text == null ? "" : text; }

    // ---- stats ----

    public boolean isProtectedNpc() { return protectedNpc; }
    public void setProtectedNpc(boolean p) { this.protectedNpc = p; }

    public boolean opensDoors() { return opensDoors; }

    public void setOpensDoors(boolean open) {
        this.opensDoors = open;
        applyDoorCapability();
    }

    private void applyDoorCapability() {
        if (this.getNavigation() instanceof net.minecraft.world.entity.ai.navigation.GroundPathNavigation nav) {
            nav.setCanOpenDoors(opensDoors);
            // Pass-through moved off the navigation and onto the evaluator that does the pathing.
            //? if >=1.21.11 {
            /*if (nav.getNodeEvaluator() != null) nav.getNodeEvaluator().setCanPassDoors(true);
            *///?} else {
            nav.setCanPassDoors(true);
            //?}
            if (nav.getNodeEvaluator() != null) nav.getNodeEvaluator().setCanOpenDoors(opensDoors);
        }
        if (opensDoors && doorGoal == null) {
            doorGoal = new net.minecraft.world.entity.ai.goal.OpenDoorGoal(this, true);
            this.goalSelector.addGoal(1, doorGoal);
        } else if (!opensDoors && doorGoal != null) {
            this.goalSelector.removeGoal(doorGoal);
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

    @Nullable
    public Player resolveFollowTarget() {
        if (!followPlayerName.isEmpty() && this.level().getServer() != null) {
            return this.level().getServer().getPlayerList().getPlayerByName(followPlayerName);
        }
        return owner != null ? this.level().getPlayerByUUID(owner) : null;
    }

    // False for everyone until a faction is set, which is what keeps factions inert by default.
    public boolean isAlly(@Nullable net.minecraft.world.entity.Entity other) {
        if (factionId.isEmpty() || other == null) return false;
        if (other instanceof NotchNpcEntity npc) return factionId.equals(npc.getFactionId());
        if (other instanceof ServerPlayer sp) {
            return factionId.equals(net.fugginbeenus.notchcurrency.npc.faction.FactionState
                    .get(sp.serverLevel()).factionIdOf(sp.getUUID()));
        }
        return false;
    }

    public boolean isRivalFaction(@Nullable net.minecraft.world.entity.Entity other) {
        if (factionId.isEmpty() || other == null) return false;
        String theirs = null;
        if (other instanceof NotchNpcEntity npc) {
            theirs = npc.getFactionId();
        } else if (other instanceof ServerPlayer sp) {
            theirs = net.fugginbeenus.notchcurrency.npc.faction.FactionState
                    .get(sp.serverLevel()).factionIdOf(sp.getUUID());
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
            this.goalSelector.removeGoal(lookGoal);
            if (watch) this.goalSelector.addGoal(6, lookGoal);
        }
    }

    public int getVisibility() { return visibility; }
    public void setVisibility(int vis) { this.visibility = Math.max(0, Math.min(2, vis)); }

    public boolean isManualInvisible() { return manualInvisible; }
    public void setManualInvisible(boolean inv) { this.manualInvisible = inv; }

    public String getCustomRoleId() { return customRoleId; }
    public void setCustomRoleId(String id) { this.customRoleId = id == null ? "" : id; }

    public boolean isRuleHidden() {
        if (visibility == VIS_DAY) return !this.level().isDay();
        if (visibility == VIS_NIGHT) return this.level().isDay();
        return false;
    }

    @Override
    //? if >=1.21 {
    /*public boolean canBeLeashed() {
        return leashable && super.canBeLeashed();
    }
    *///?} else {
    public boolean canBeLeashed(Player player) {
        return leashable && super.canBeLeashed(player);
    }
    //?}

    public int getRegen() { return regen; }
    public void setRegen(int r) { this.regen = Math.max(0, Math.min(10, r)); }

    public void setBaseStats(int maxHealth, int speedPct) {
        int hp = Math.max(2, Math.min(100, maxHealth));
        double speed = Math.max(0.1, Math.min(0.6, speedPct / 100.0));
        var health = this.getAttribute(Attributes.MAX_HEALTH);
        if (health != null) health.setBaseValue(hp);
        var move = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (move != null) move.setBaseValue(speed);
        this.setHealth(hp);
    }

    public java.util.List<String> debugSummary(ServerPlayer viewer) {
        java.util.List<String> out = new java.util.ArrayList<>();
        out.add("behavior=" + behavior + " radius=" + wanderRadius
                + " home=" + (homePos == null ? "none" : homePos.toShortString()));
        Player resolvedOwner = owner == null ? null : this.level().getPlayerByUUID(owner);
        out.add("owner=" + (owner == null ? "none" : ownerName)
                + " resolved=" + (resolvedOwner != null)
                + " distToYou=" + String.format("%.1f", Math.sqrt(this.distanceToSqr(viewer))));
        out.add("movementGoals=" + behaviorGoals.size()
                + " navIdle=" + this.getNavigation().isDone()
                + " despawnCounter=" + this.getNoActionTime());
        out.add("onGround=" + this.onGround() + " noGravity=" + this.isNoGravity()
                + " aiDisabled=" + this.isNoAi()
                + " speedAttr=" + this.getAttributeValue(Attributes.MOVEMENT_SPEED)
                + " followRange=" + this.getAttributeValue(Attributes.FOLLOW_RANGE));
        return out;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                // Follow range doubles as the pathfinding distance limit; the default 16 made a
                // following NPC freeze whenever its owner got 16-30 blocks away (teleport kicks
                // in at 30).
                .add(Attributes.FOLLOW_RANGE, 48.0)
                // Needed by MeleeAttackGoal (GUARD mode). Weapon bonuses stack on top.
                .add(Attributes.ATTACK_DAMAGE, 4.0);
    }

    @Override
    protected void registerGoals() {
        // Base goals every NPC has; movement goals are swapped in by applyBehaviorGoals().
        this.goalSelector.addGoal(0, new net.minecraft.world.entity.ai.goal.FloatGoal(this));
        // Top priority: while a player is interacting, hold still and face them (see startTalking).
        this.goalSelector.addGoal(0, new TalkGoal());
        this.lookGoal = new LookAtPlayerGoal(this, Player.class, 8.0f);
        this.goalSelector.addGoal(6, lookGoal);
    }

    public void startTalking(Player player) {
        this.talkingTo = player.getUUID();
        this.talkingTicks = 160; // ~8s; TalkGoal counts this down and releases when it (or the player) runs out
    }

    private class TalkGoal extends net.minecraft.world.entity.ai.goal.Goal {
        TalkGoal() {
            this.setFlags(java.util.EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Nullable
        private Player partner() {
            if (talkingTo == null || talkingTicks <= 0) return null;
            Player p = level().getPlayerByUUID(talkingTo);
            if (p == null || p.isRemoved() || distanceToSqr(p) > 100.0) return null; // ~10 blocks
            return p;
        }

        @Override
        public boolean canUse() {
            return partner() != null;
        }

        @Override
        public boolean canContinueToUse() {
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
            Player p = partner();
            if (p != null) {
                getLookControl().setLookAt(p, 30.0f, 30.0f);
            }
        }

        @Override
        public void stop() {
            talkingTo = null;
            talkingTicks = 0;
        }
    }

    private void applyBehaviorGoals() {
        for (net.minecraft.world.entity.ai.goal.Goal g : behaviorGoals) {
            this.goalSelector.removeGoal(g);
        }
        behaviorGoals.clear();
        for (net.minecraft.world.entity.ai.goal.Goal g : behaviorTargetGoals) {
            this.targetSelector.removeGoal(g);
        }
        behaviorTargetGoals.clear();
        this.setTarget(null); // drop any combat target when leaving GUARD

        if (avoidMonsters) {
            // Runs alongside any behavior: back away from hostiles that get close.
            net.minecraft.world.entity.ai.goal.Goal flee = new net.minecraft.world.entity.ai.goal.AvoidEntityGoal<>(
                    this, net.minecraft.world.entity.monster.Monster.class, 8.0f, 1.0, 1.25);
            this.goalSelector.addGoal(1, flee);
            behaviorGoals.add(flee);
        }

        switch (movementBehavior()) {
            case WANDER -> {
                // Short-range strolls every ~2s: livelier than the vanilla far-wander cadence and a
                // better fit for the home leash. canDespawn=false skips the despawn-counter gate.
                net.minecraft.world.entity.ai.goal.Goal wander =
                        new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.8, 40, false);
                this.goalSelector.addGoal(2, wander);
                behaviorGoals.add(wander);
                applyHomeLeash();
            }
            case FOLLOW_OWNER -> {
                net.minecraft.world.entity.ai.goal.Goal follow = new NpcFollowOwnerGoal(this, 1.15);
                this.goalSelector.addGoal(2, follow);
                behaviorGoals.add(follow);
                this.clearRestriction(); // no leash
            }
            case PATROL -> {
                net.minecraft.world.entity.ai.goal.Goal patrol = new NpcPatrolGoal(this);
                this.goalSelector.addGoal(2, patrol);
                behaviorGoals.add(patrol);
                this.clearRestriction(); // waypoints may be far from home
            }
            case GUARD -> {
                // Stroll the post while idle and stay leashed to home; the fighting itself is set up
                // below, since Guard is only one of the reasons an NPC might swing at something.
                net.minecraft.world.entity.ai.goal.Goal stroll =
                        new net.minecraft.world.entity.ai.goal.RandomStrollGoal(this, 0.6, 80, false);
                this.goalSelector.addGoal(5, stroll);
                behaviorGoals.add(stroll);
                applyHomeLeash();
            }
            case STATIONARY -> this.clearRestriction();
        }

        // One melee goal, however many reasons there are to fight: a second would fight itself for
        // the movement control.
        boolean fights = behavior == Behavior.GUARD || hostileToPlayers || fightsBack
                || attackMonsters || protectOwner || fightRivalFactions;
        if (fights) {
            net.minecraft.world.entity.ai.goal.Goal melee =
                    new net.minecraft.world.entity.ai.goal.MeleeAttackGoal(this, 1.1, true);
            this.goalSelector.addGoal(2, melee);
            behaviorGoals.add(melee);
        }
        if (behavior == Behavior.GUARD || attackMonsters) {
            // Hostile mobs, but never creepers (iron-golem rule: don't walk a blast into the shop).
            net.minecraft.world.entity.ai.goal.Goal targets = new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.monster.Monster.class, 10, true, false,
                    //? if >=1.21.11 {
                    /*(e, checkLevel) -> !(e instanceof net.minecraft.world.entity.monster.Creeper));
                    *///?} else {
                    e -> !(e instanceof net.minecraft.world.entity.monster.Creeper));
                    //?}
            this.targetSelector.addGoal(1, targets);
            behaviorTargetGoals.add(targets);
        }
        if (protectOwner) {
            net.minecraft.world.entity.ai.goal.Goal protect = new NpcProtectOwnerGoal(this);
            this.targetSelector.addGoal(1, protect);
            behaviorTargetGoals.add(protect);
        }
        if (hostileToPlayers) {
            // Hunt ANY player in range, including the owner (hostile means hostile). Vanilla
            // targeting already skips creative/spectator players. Its own faction is still spared:
            // a guard that turns on its own people is nobody's idea of a guard.
            net.minecraft.world.entity.ai.goal.Goal huntPlayers = new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.player.Player.class, 10, true, false,
                    //? if >=1.21.11 {
                    /*(e, checkLevel) -> !isAlly(e));
                    *///?} else {
                    e -> !isAlly(e));
                    //?}
            this.targetSelector.addGoal(2, huntPlayers);
            behaviorTargetGoals.add(huntPlayers);
        }
        if (fightRivalFactions && !factionId.isEmpty()) {
            // Only people actually flying another faction's colours: bystanders with no faction are
            // left alone, so a faction war doesn't sweep up everyone who never joined.
            net.minecraft.world.entity.ai.goal.Goal rivals = new net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal<>(
                    this, net.minecraft.world.entity.LivingEntity.class, 10, true, false,
                    //? if >=1.21.11 {
                    /*(e, checkLevel) -> isRivalFaction(e));
                    *///?} else {
                    this::isRivalFaction);
                    //?}
            this.targetSelector.addGoal(2, rivals);
            behaviorTargetGoals.add(rivals);
        }
        if (fightsBack) {
            net.minecraft.world.entity.ai.goal.Goal revenge = new net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal(this);
            this.targetSelector.addGoal(1, revenge);
            behaviorTargetGoals.add(revenge);
        }
        applyDoorCapability(); // re-assert door pathing/goal after the goal list is rebuilt
    }

    // Combat deliberately still reads the configured behaviour: a guard on a scheduled post is a guard.
    private Behavior movementBehavior() {
        return scheduleBehavior != null ? scheduleBehavior : behavior;
    }

    @Nullable
    private net.minecraft.core.BlockPos leashHome() {
        return scheduleBehavior != null ? scheduleHome : homePos;
    }

    private int leashRadius() {
        return scheduleBehavior != null ? scheduleRadius : wanderRadius;
    }

    private void applyHomeLeash() {
        net.minecraft.core.BlockPos home = leashHome();
        if (home != null) {
            this.restrictTo(home, Math.max(2, leashRadius()));
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide) {
            if (movementBehavior() == Behavior.STATIONARY) {
                // Reading the leash point rather than the configured home is what gives a scheduled
                // NPC its commute: the same walk-back that returns a guard to its post after a fight
                // is what carries a shopkeeper to the counter at opening time, and to bed at night.
                net.minecraft.core.BlockPos post = leashHome();
                if (this.getTarget() != null && this.getTarget().isAlive()) {
                    // In combat (hostile/fights-back): let the attack goal chase.
                } else if (post != null && this.distanceToSqr(
                        post.getX() + 0.5, post.getY(), post.getZ() + 0.5) > 2.25) {
                    // Combat over (or shoved): walk back to the post before locking down again.
                    this.getNavigation().moveTo(
                            post.getX() + 0.5, post.getY(), post.getZ() + 0.5, 1.0);
                } else {
                    this.getNavigation().stop();
                    this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                    holdScheduleFacing();
                }
            }
            // Re-assert the configured pose in case vanilla logic reset it.
            net.minecraft.world.entity.Pose want = entityPoseFor(getNpcPose());
            if (this.getPose() != want) {
                this.setPose(want);
            }
            // Health regeneration (half-hearts per 5 seconds).
            if (regen > 0 && this.tickCount % 100 == 0 && this.getHealth() < this.getMaxHealth()) {
                this.heal(regen * 0.5f);
            }
            // Keep the vanilla invisible flag in step with the toggle + day/night rule.
            if (this.tickCount % 20 == 0) {
                boolean hidden = manualInvisible || isRuleHidden();
                if (this.isInvisible() != hidden) this.setInvisible(hidden);
            }
            tickProximity();
            tickSchedule();
            tickScheduleSleep();
            net.fugginbeenus.notchcurrency.npc.action.NpcActionSweep.sweep(this);
        }
    }

    private void tickSchedule() {
        boolean runnable = schedule.isActive()
                && net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.dimensionSupports(this.level());
        if (!runnable) {
            // Switched off, emptied, or carried somewhere with no day: hand the NPC back to whatever
            // the Moves tab says instead of leaving it frozen in the last stance it was given.
            if (scheduleBehavior != null) releaseSchedule();
            return;
        }
        if (this.tickCount % 20 != 0) return;

        int idx = schedule.indexAt(this.level().getDayTime());
        if (idx == scheduleActive) return;

        // A first application after loading is not a transition. Firing entry actions here would mean
        // a shopkeeper announcing opening hours every time somebody walks into the chunk.
        boolean transition = scheduleActive != -1;
        scheduleActive = idx;
        applyScheduleEntry(schedule.get(idx), transition);
    }

    // Fields set together and goals rebuilt once. The public setters would overwrite the owner's own settings.
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

    // Posing it asleep is not sleeping: vanilla sleep() is what claims the bed and snaps it on.
    private void tickScheduleSleep() {
        var entry = currentScheduleEntry();
        boolean wantsBed = entry != null
                && entry.stance() == net.fugginbeenus.notchcurrency.npc.schedule.NpcStance.SLEEP
                && entry.anchor() != null;

        if (!wantsBed) {
            if (this.isSleeping()) {
                this.stopSleeping();
                restoreLookGoal();
            }
            return;
        }

        net.minecraft.core.BlockPos bed = bedHead(entry.anchor());
        if (this.isSleeping()) {
            // Somebody mined the bed out from under it: stand up rather than float there.
            if (!(this.level().getBlockState(bed).getBlock() instanceof net.minecraft.world.level.block.BedBlock)) {
                this.stopSleeping();
                restoreLookGoal();
                return;
            }
            this.getNavigation().stop();
            return;
        }

        // Still walking over. Climb in once close enough to reach it.
        if (!(this.level().getBlockState(bed).getBlock() instanceof net.minecraft.world.level.block.BedBlock)) return;
        double dx = bed.getX() + 0.5 - this.getX();
        double dz = bed.getZ() + 0.5 - this.getZ();
        if (dx * dx + dz * dz > 4.0 || Math.abs(bed.getY() - this.getY()) > 2.0) return;

        this.startSleeping(bed);
        this.getNavigation().stop();
        if (lookGoal != null) this.goalSelector.removeGoal(lookGoal);
    }

    // Head half of the bed, or the body lies a block down it and hangs off the end.
    private net.minecraft.core.BlockPos bedHead(net.minecraft.core.BlockPos pos) {
        net.minecraft.world.level.block.state.BlockState state = this.level().getBlockState(pos);
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock
                && state.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT) {
            return pos.relative(state.getValue(net.minecraft.world.level.block.BedBlock.FACING));
        }
        return pos;
    }

    // Remove before adding, or a nap leaves a second copy of the goal running.
    private void restoreLookGoal() {
        if (lookGoal == null) return;
        this.goalSelector.removeGoal(lookGoal);
        if (watchPlayers) this.goalSelector.addGoal(6, lookGoal);
    }

    // Runs before the goals, so talking, watching and combat all still win. This is the resting angle.
    private void holdScheduleFacing() {
        if (talkingTo != null || this.getTarget() != null || this.isSleeping()) return;
        var entry = currentScheduleEntry();
        if (entry == null || entry.stance() != net.fugginbeenus.notchcurrency.npc.schedule.NpcStance.STAND) return;
        float want = entry.facing();
        this.setYRot(want);
        this.setYBodyRot(want);
        this.setYHeadRot(want);
    }

    private void releaseSchedule() {
        scheduleBehavior = null;
        scheduleHome = null;
        scheduleActive = -1;
        holdPose(-1);
        applyBehaviorGoals();
    }

    private void holdPose(int wanted) {
        if (wanted >= 0) {
            if (poseBeforeSchedule < 0) poseBeforeSchedule = getNpcPose();
            if (getNpcPose() != wanted) setNpcPose(wanted);
        } else if (poseBeforeSchedule >= 0) {
            setNpcPose(poseBeforeSchedule);
            poseBeforeSchedule = -1;
        }
    }

    @Nullable
    public net.fugginbeenus.notchcurrency.npc.schedule.ScheduleEntry currentScheduleEntry() {
        if (!schedule.isActive()) return null;
        if (!net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.dimensionSupports(this.level())) return null;
        return schedule.activeAt(this.level().getDayTime());
    }

    private void tickProximity() {
        if (!actions.has(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_PROXIMITY)) return;
        if (this.tickCount % PROXIMITY_SCAN_TICKS != 0) return;
        if (manualInvisible || isRuleHidden()) return; // a hidden NPC shouldn't greet anyone

        if (proximityInside == null) {
            proximityInside = new java.util.HashSet<>();
            proximityFired = new java.util.HashMap<>();
        }
        double radius = actions.proximityRadius();
        double enterSq = radius * radius;
        double leaveSq = (radius + 2.0) * (radius + 2.0);

        for (net.minecraft.world.entity.player.Player generic : this.level().players()) {
            if (!(generic instanceof ServerPlayer player)) continue;
            if (player.isSpectator() || !player.isAlive()) continue;
            java.util.UUID id = player.getUUID();
            double distanceSq = player.distanceToSqr(this);
            if (!proximityInside.contains(id)) {
                if (distanceSq > enterSq) continue;
                proximityInside.add(id);
                Integer last = proximityFired.get(id);
                if (last == null || this.tickCount - last >= PROXIMITY_RECHARGE_TICKS) {
                    proximityFired.put(id, this.tickCount);
                    fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_PROXIMITY, player);
                }
            } else if (distanceSq > leaveSq) {
                proximityInside.remove(id);
            }
        }

        // Anyone who logged out or changed world counts as gone, and spent cooldowns are dropped so
        // neither collection grows with everyone who ever walked past.
        proximityInside.removeIf(id -> this.level().getPlayerByUUID(id) == null);
        proximityFired.entrySet().removeIf(e ->
                !proximityInside.contains(e.getKey()) && this.tickCount - e.getValue() > PROXIMITY_RECHARGE_TICKS);
    }

    @Override
    protected void doPush(net.minecraft.world.entity.Entity entity) {
        if (pushable) super.doPush(entity); // hold ground unless the Pushable ability is on
    }

    @Override
    public void checkDespawn() {
        // Never despawn. These are placed, persistent NPCs. Also keep the despawn counter at zero:
        // vanilla increments it every AI tick and only resets it here, and WanderAroundGoal refuses
        // to start once it passes 100 (which froze wandering ~5s after placement).
        this.noActionTime = 0;
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

    public boolean isOwnedBy(Player player) {
        return owner != null && owner.equals(player.getUUID());
    }

    public boolean canEdit(ServerPlayer player) {
        return isOwnedBy(player) || net.fugginbeenus.notchcurrency.compat.Perms.isOperator(player);
    }

    // ---- interaction ----

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.mobInteract(player, hand);
        }
        if (this.level().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer sp) {
            // Off duty per the day/night rule: only the owner/op can still reach it (to edit).
            if (isRuleHidden() && !canEdit(sp)) {
                return InteractionResult.PASS;
            }
            startTalking(player); // pause + face the player while they're dealing with us
            playVoice();           // a grunt as it turns to you, the way a villager answers
            // Before dialogue or the role screen takes over, so a greeting is read first.
            fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_INTERACT, sp);
            if (sp.isShiftKeyDown() && canEdit(sp)) {
                NotchNpcManager.openEditor(sp, this);
            } else if (!net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.open(sp, this)) {
                // No dialogue: go straight to the role.
                NotchNpcManager.dispatchRole(sp, this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ---- damage protection (owned NPCs are protected, like the old shopkeeper) ----

    // hurt() became final in 1.21.11 and split: the server side of it is hurtServer, which is only
    // ever called on the server and so drops the client guard below.
    //? if >=1.21.11 {
    /*@Override
    public boolean hurtServer(net.minecraft.server.level.ServerLevel level,
                              DamageSource source, float amount) {
    *///?} else {
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (this.level().isClientSide()) return false;
    //?}
        // Fires on being HIT, not on damage getting through. Protection is on by default, so an
        // "if it takes damage" reading would never run for an ordinary shopkeeper, and a shopkeeper
        // snapping at someone who punched it is the whole point.
        if (!this.isDeadOrDying() && amount > 0) {
            fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_HURT,
                    source.getEntity() instanceof ServerPlayer p ? p : null);
        }
        if (protectedNpc && (owner != null || ownerType == OwnerType.SERVER)) {
            // Only the void or /kill can remove a protected NPC.
            if (source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) {
                //? if >=1.21.11 {
                /*return super.hurtServer(level, source, amount);
                *///?} else {
                return super.hurt(source, amount);
                //?}
            }
            // The hit is cancelled, but Fights Back still needs to know who swung: record the
            // attacker so the RevengeGoal can retaliate even while the NPC itself is unhurtable.
            if (fightsBack && source.getEntity() instanceof net.minecraft.world.entity.LivingEntity attacker) {
                this.setLastHurtByMob(attacker);
            }
            return false;
        }
        //? if >=1.21.11 {
        /*return super.hurtServer(level, source, amount);
        *///?} else {
        return super.hurt(source, amount);
        //?}
    }

    @Override
    public void die(DamageSource source) {
        // Before super, while the NPC is still in the world and its actions can still reference it.
        // The killer may be nobody at all: lava and fall damage count.
        fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_DEATH,
                source.getEntity() instanceof ServerPlayer p ? p : null);
        super.die(source);
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean killedEntity(net.minecraft.server.level.ServerLevel world,
                                net.minecraft.world.entity.LivingEntity other,
                                DamageSource cause) {
        boolean result = super.killedEntity(world, other, cause);
    *///?} else {
    @Override
    public boolean killedEntity(net.minecraft.server.level.ServerLevel world,
                                 net.minecraft.world.entity.LivingEntity other) {
        boolean result = super.killedEntity(world, other);
    //?}
        // A player only comes along when the NPC killed a player, otherwise there's no one to talk to.
        fire(net.fugginbeenus.notchcurrency.npc.action.NpcTrigger.ON_KILL,
                other instanceof ServerPlayer p ? p : null);
        return result;
    }

    // ---- NBT ----

    // writeConfig and readConfig stay tag-based on every version: presets, share codes and the
    // pick-up item all go through them too. Nbt bridges the tag across 1.21.11's view API.
    //? if >=1.21.11 {
    /*@Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput out) {
        super.addAdditionalSaveData(out);
        CompoundTag nbt = new CompoundTag();
        writeConfig(nbt);
        net.fugginbeenus.notchcurrency.compat.Nbt.copyInto(nbt, out);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        readConfig(net.fugginbeenus.notchcurrency.compat.Nbt.readAll(in));
    }
    *///?} else {
    @Override
    public void addAdditionalSaveData(CompoundTag nbt) {
        super.addAdditionalSaveData(nbt);
        writeConfig(nbt);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag nbt) {
        super.readAdditionalSaveData(nbt);
        readConfig(nbt);
    }
    //?}

    // Excludes the custom name; the caller handles that.
    public void writeConfig(CompoundTag nbt) {
        nbt.putString("Role", role.name());
        if (roleTarget != null) net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "RoleTarget", roleTarget);
        nbt.putString("OwnerType", ownerType.name());
        if (owner != null) net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(nbt, "Owner", owner);
        nbt.putString("OwnerName", ownerName);
        nbt.putString("Model", getModelId());
        nbt.putString("SkinType", getSkinType());
        nbt.putString("SkinValue", getSkinValue());
        nbt.putBoolean("Slim", isSlim());
        nbt.putBoolean("TalkBubble", showsTalkBubble());
        nbt.putFloat("Scale", npcScale());
        nbt.putFloat("ScaleY", getScaleY());
        nbt.putFloat("ScaleZ", getScaleZ());
        nbt.putFloat("NameOffset", getNameOffset());
        nbt.putString("Billboard", getBillboard());
        nbt.putInt("NpcPose", getNpcPose());
        nbt.put("CustomPose", this.entityData.get(CUSTOM_POSE).copy());
        nbt.putInt("PoseAnim", getPoseAnim());
        if (!getCustomClip().isEmpty()) nbt.putString("CustomClip", getCustomClip());
        nbt.putString("Behavior", behavior.name());
        nbt.putInt("WanderRadius", wanderRadius);
        nbt.putFloat("PatrolSpeed", patrolSpeed);
        nbt.putInt("PatrolWait", patrolWaitTicks);
        if (homePos != null) {
            nbt.putIntArray("Home", new int[]{homePos.getX(), homePos.getY(), homePos.getZ()});
        }
        net.minecraft.nbt.ListTag wps = new net.minecraft.nbt.ListTag();
        for (net.minecraft.core.BlockPos wp : waypoints) {
            wps.add(new net.minecraft.nbt.IntArrayTag(new int[]{wp.getX(), wp.getY(), wp.getZ()}));
        }
        nbt.put("Waypoints", wps);
        nbt.put("Dialogue", dialogue.toNbt());
        nbt.putString("DialogueMode", dialogueMode.name());
        nbt.putString("Farewell", farewellText);
        if (!actions.isEmpty()) nbt.put("Actions", actions.toNbt());
        if (schedule.isEnabled() || !schedule.isEmpty()) nbt.put("Schedule", schedule.toNbt());
        if (poseBeforeSchedule >= 0) nbt.putInt("PoseBeforeSchedule", poseBeforeSchedule);
        if (!getSubtitle().isEmpty()) nbt.putString("Subtitle", getSubtitle());
        if (!voiceSound.isEmpty()) nbt.putString("Voice", voiceSound);
        if (voicePitch != 100) nbt.putInt("VoicePitch", voicePitch);
        // Stats: the vanilla flags are re-recorded here so they survive the pick-up item too.
        nbt.putBoolean("Protected", protectedNpc);
        nbt.putBoolean("StatSilent", this.isSilent());
        nbt.putBoolean("StatGlowing", this.isCurrentlyGlowing());
        nbt.putBoolean("StatNoGravity", this.isNoGravity());
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
        nbt.putInt("StatMaxHealth", (int) Math.round(this.getAttributeValue(Attributes.MAX_HEALTH)));
        nbt.putInt("StatSpeedPct", (int) Math.round(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 100));
        // Equipment: re-recorded so it survives the pick-up item too.
        CompoundTag equip = new CompoundTag();
        for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
            net.minecraft.world.item.ItemStack st = this.getItemBySlot(slot);
            if (!st.isEmpty()) equip.put(slot.getName(), net.fugginbeenus.notchcurrency.compat.StackData.writePortableStack(st));
        }
        nbt.put("Equip", equip);
    }

    public void readConfig(CompoundTag nbt) {
        try {
            role = NpcRole.valueOf(nbt.getString("Role"));
        } catch (IllegalArgumentException e) {
            role = NpcRole.NONE;
        }
        roleTarget = net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(nbt, "RoleTarget") ? net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "RoleTarget") : null;
        try {
            ownerType = OwnerType.valueOf(nbt.getString("OwnerType"));
        } catch (IllegalArgumentException e) {
            ownerType = OwnerType.PLAYER;
        }
        owner = net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(nbt, "Owner") ? net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(nbt, "Owner") : null;
        ownerName = nbt.getString("OwnerName");
        if (nbt.contains("Model")) setModelId(nbt.getString("Model"));
        if (nbt.contains("SkinType")) setSkinType(nbt.getString("SkinType"));
        if (nbt.contains("SkinValue")) setSkinValue(nbt.getString("SkinValue"));
        if (nbt.contains("Slim")) setSlim(nbt.getBoolean("Slim"));
        setTalkBubble(nbt.getBoolean("TalkBubble"));
        if (nbt.contains("Scale")) setScale(nbt.getFloat("Scale"));
        // Older NPCs only stored one scale: fall back to it so they stay the shape they were.
        setScaleY(nbt.contains("ScaleY") ? nbt.getFloat("ScaleY") : npcScale());
        setScaleZ(nbt.contains("ScaleZ") ? nbt.getFloat("ScaleZ") : npcScale());
        if (nbt.contains("NameOffset")) setNameOffset(nbt.getFloat("NameOffset"));
        if (nbt.contains("Billboard")) setBillboard(nbt.getString("Billboard"));
        if (nbt.contains("NpcPose")) setNpcPose(nbt.getInt("NpcPose"));
        if (nbt.contains("PoseAnim")) setPoseAnim(nbt.getInt("PoseAnim"));
        if (nbt.contains("CustomClip")) setCustomClip(nbt.getString("CustomClip"));
        if (nbt.contains("CustomPose")) {
            CompoundTag pose = nbt.getCompound("CustomPose");
            this.entityData.set(CUSTOM_POSE, pose);
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
        int[] home = net.fugginbeenus.notchcurrency.compat.Nbt.intArray(nbt, "Home");
        if (home.length == 3) homePos = new net.minecraft.core.BlockPos(home[0], home[1], home[2]);
        if (nbt.contains("Waypoints")) {
            waypoints.clear();
            net.minecraft.nbt.ListTag wps = nbt.getList("Waypoints", net.minecraft.nbt.Tag.TAG_INT_ARRAY);
            for (int i = 0; i < wps.size(); i++) {
                int[] wp = net.fugginbeenus.notchcurrency.compat.Nbt.intArray(wps, i);
                if (wp.length == 3) waypoints.add(new net.minecraft.core.BlockPos(wp[0], wp[1], wp[2]));
            }
        }
        if (nbt.contains("Dialogue")) {
            dialogue = net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(nbt.getCompound("Dialogue"));
        }
        if (nbt.contains("Farewell")) farewellText = nbt.getString("Farewell");
        actions = net.fugginbeenus.notchcurrency.npc.action.NpcActions.fromNbt(
                nbt.contains("Actions") ? nbt.getCompound("Actions") : null);
        poseBeforeSchedule = nbt.contains("PoseBeforeSchedule") ? nbt.getInt("PoseBeforeSchedule") : -1;
        setSubtitle(nbt.getString("Subtitle"));
        setVoice(nbt.getString("Voice"));
        setVoicePitchPercent(nbt.contains("VoicePitch") ? nbt.getInt("VoicePitch") : 100);
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
        if (nbt.contains("StatGlowing")) this.setGlowingTag(nbt.getBoolean("StatGlowing"));
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
            if (hp != (int) Math.round(this.getAttributeValue(Attributes.MAX_HEALTH))
                    || speedPct != (int) Math.round(this.getAttributeValue(Attributes.MOVEMENT_SPEED) * 100)) {
                setBaseStats(hp, speedPct);
            }
        }
        if (nbt.contains("Equip")) {
            CompoundTag equip = nbt.getCompound("Equip");
            for (net.minecraft.world.entity.EquipmentSlot slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                String slotKey = slot.getName();
                if (equip.contains(slotKey)) {
                    this.setItemSlot(slot, net.fugginbeenus.notchcurrency.compat.StackData.readPortableStack(equip.getCompound(slotKey)));
                    this.setDropChance(slot, 1.0f); // owner's items always drop if it dies
                }
            }
        }
        applyBehaviorGoals();
    }

    public CompoundTag writeToItem() {
        CompoundTag tag = new CompoundTag();
        writeConfig(tag);
        if (this.hasCustomName() && this.getCustomName() != null) {
            tag.putString("Name", this.getCustomName().getString());
        }
        return tag;
    }

    public void readFromItem(CompoundTag tag) {
        readConfig(tag);
        if (tag.contains("Name")) {
            this.setCustomName(Component.literal(tag.getString("Name")));
            this.setCustomNameVisible(true);
        }
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // 5.x dropped the animatable argument: the controller is handed to the animatable, not the
        // other way round.
        //? if >=1.21.5 {
        /*controllers.add(new AnimationController<NotchNpcEntity>("main", 4, this::idlePredicate));
        *///?} else {
        controllers.add(new AnimationController<>(this, "main", 4, this::idlePredicate));
        //?}
    }

    //? if >=1.21.5 {
    /*private PlayState idlePredicate(AnimationTest<NotchNpcEntity> state) {
    *///?} else {
    private <E extends NotchNpcEntity> PlayState idlePredicate(AnimationState<E> state) {
    //?}
        String clip = chooseAnimation();
        if (clip == null) return PlayState.STOP; // a statue holds still, including its hands
        state.setAndContinue(net.fugginbeenus.notchcurrency.compat.Geo.loop(clip));
        return PlayState.CONTINUE;
    }

    /**
     * Which clip suits what the NPC is doing, or null to stand perfectly still.
     *
     * <p>Driven by the same Statue / Breathe / Lively setting that drives the vanilla models, so one
     * choice in the editor means the same thing whichever model an NPC is wearing.
     *
     * <p>The flourishes are worked out from the tick count and the NPC's own id rather than kept in
     * a field. That way nothing has to be stored or sent, and two NPCs standing side by side do not
     * move in lockstep, which is what gives away that they are the same thing twice.
     */
    @Nullable
    private String chooseAnimation() {
        int mode = getPoseAnim();
        if (mode == ANIM_STATUE) return null;

        // Walking wins over anything it might have been doing standing still.
        if (this.walkAnimation.speed() > 0.02f) return WALK_ANIM;

        // A clip chosen by hand stands in for the idle, flourishes and all. It is checked against
        // what is actually loaded, so pulling the resource pack out leaves the NPC on the built-in
        // idle rather than stuck on a clip that no longer exists.
        String chosen = getCustomClip();
        if (!chosen.isEmpty() && net.fugginbeenus.notchcurrency.compat.Geo.hasClip(chosen)) {
            return chosen;
        }

        if (mode != ANIM_LIVELY) return IDLE_ANIM;

        int stagger = Math.floorMod(getUUID().hashCode(), FLOURISH_EVERY);
        int spot = Math.floorMod(this.tickCount + stagger, FLOURISH_EVERY);
        long window = Math.floorDiv((long) this.tickCount + stagger, FLOURISH_EVERY);

        // Stable for the whole window, so the choice does not change part way through a flourish.
        int roll = Math.floorMod(Long.hashCode(window * 31L + getUUID().hashCode()), SPECIAL_ANIMS.length);
        return spot < SPECIAL_TICKS[roll] ? SPECIAL_ANIMS[roll] : IDLE_ANIM;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
