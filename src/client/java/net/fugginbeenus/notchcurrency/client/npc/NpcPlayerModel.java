package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.math.MathHelper;

/**
 * Player model with pose presets layered on top, then an IDLE ANIMATION layered on the pose
 * (statue/breathe/sway/lively — EasyNPC-style life for otherwise frozen NPCs). Poses are applied
 * AFTER vanilla setAngles because they need per-part absolute values; while the NPC is mid attack
 * swing, the swinging arm is left to vanilla so combat reads properly. Angle/offset values adapted
 * from EasyNPC's baked pose data. Overlay parts are re-synced at the end.
 */
public class NpcPlayerModel extends PlayerEntityModel<NotchNpcEntity> {

    private final boolean thinArms;

    public NpcPlayerModel(ModelPart root, boolean thinArms) {
        super(root, thinArms);
        this.thinArms = thinArms;
    }

    private static final float DEG = (float) (Math.PI / 180.0);

    @Override
    public void setAngles(NotchNpcEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        // Mid attack swing (the model field is the render-time value the swing animates with):
        // vanilla just animated the main arm — poses must not stomp it.
        boolean swinging = this.handSwingProgress > 0f;

        if (entity.getPoseAnim() == NotchNpcEntity.ANIM_STATUE) {
            removeVanillaArmBob(animationProgress);
        }
        applyPose(entity, animationProgress, swinging);
        applyIdleAnim(entity, animationProgress, swinging);
        applyAttackSwing(entity, animationProgress);
        syncOverlays();
    }

    /** Statue mode: cancel vanilla's built-in idle arm bob (the exact inverse of the terms
     *  BipedEntityModel adds every frame) so the NPC is genuinely frozen. */
    private void removeVanillaArmBob(float t) {
        float bobRoll = MathHelper.cos(t * 0.09f) * 0.05f + 0.05f;
        float bobPitch = MathHelper.sin(t * 0.067f) * 0.05f;
        this.rightArm.roll -= bobRoll;
        this.leftArm.roll += bobRoll;
        this.rightArm.pitch -= bobPitch;
        this.leftArm.pitch += bobPitch;
    }

    /** How long the custom attack swing plays, in ticks. */
    private static final float SWING_TICKS = 8f;

    /** A big readable attack swing: raise the right arm overhead and chop, with a shoulder twist.
     *  Driven primarily by the entity's own ATTACK_PULSE sync (vanilla's hand-swing packet proved
     *  unreliable for this entity; the render-time progress is kept as a fallback). Applied LAST so
     *  it wins over pose + idle in every pose. */
    private void applyAttackSwing(NotchNpcEntity entity, float t) {
        float p = this.handSwingProgress;
        if (p <= 0f) {
            float since = t - entity.clientSwingStartAge;
            if (since < 0f || since >= SWING_TICKS) return;
            p = since / SWING_TICKS;
        }
        float wind = MathHelper.sin(Math.min(1f, p) * (float) Math.PI); // up, strike, recover
        this.rightArm.pitch = -0.5f - 1.7f * wind;
        this.rightArm.yaw = -0.15f * wind;
        this.rightArm.roll = 0f;
        this.body.yaw = -0.2f * wind;
    }

    /** Vanilla resets angles every frame but NOT body/arm pivots — so the Sitting/Chilling pivot
     *  drops would linger forever after switching poses (torso and arms sunk into the legs). Reset
     *  everything to the biped defaults first; the sneak pose manages its own pivots in vanilla.
     *  body.roll and head.roll are also never reset by vanilla — zero them here or the idle-sway
     *  additions accumulate frame over frame and the torso literally spins around the neck. */
    private void resetPivots() {
        this.body.roll = 0f;
        this.head.roll = 0f;
        if (this.sneaking) return;
        float armY = thinArms ? 2.5f : 2.0f;
        this.head.setPivot(0f, 0f, 0f);
        this.body.setPivot(0f, 0f, 0f);
        this.rightArm.setPivot(-5f, armY, 0f);
        this.leftArm.setPivot(5f, armY, 0f);
        this.rightLeg.setPivot(-1.9f, 12f, 0f);
        this.leftLeg.setPivot(1.9f, 12f, 0f);
    }

    private void applyPose(NotchNpcEntity entity, float animationProgress, boolean swinging) {
        resetPivots();
        int pose = entity.getNpcPose();
        if (pose == NotchNpcEntity.POSE_WAVING) {
            // Raise the right arm UP AND OUT to the side (POSITIVE roll swings it away from the body so
            // it clears the head; negative rolled it across the chest). Gentle wave via sway.
            this.body.yaw = 0.12f;
            if (!swinging) {
                float sway = (float) Math.sin(animationProgress * 0.18f) * 0.22f;
                this.rightArm.pitch = 0f;
                this.rightArm.yaw = 0f;
                this.rightArm.roll = 2.2f + sway;
            }
            return;
        }
        if (pose == NotchNpcEntity.POSE_CUSTOM) {
            float[] a = entity.getCustomPoseAngles();
            if (a != null) {
                applyRot(this.head, a, 0);
                applyRot(this.body, a, 1);
                if (!swinging) applyRot(this.rightArm, a, 2);
                applyRot(this.leftArm, a, 3);
                applyRot(this.rightLeg, a, 4);
                applyRot(this.leftLeg, a, 5);
            }
            return;
        }
        if (pose == NotchNpcEntity.POSE_PRONE) {
            // The renderer tips the body face-down; keep limbs straight (arms a touch forward) so it
            // reads as a clean crawl instead of an idle-swaying flat body.
            this.head.pitch = 0f; this.head.yaw = 0f; this.head.roll = 0f;
            this.body.pitch = 0f; this.body.yaw = 0f; this.body.roll = 0f;
            if (!swinging) {
                this.rightArm.pitch = -0.35f; this.rightArm.yaw = 0f; this.rightArm.roll = 0.05f;
            }
            this.leftArm.pitch = -0.35f; this.leftArm.yaw = 0f; this.leftArm.roll = -0.05f;
            this.rightLeg.pitch = 0f; this.rightLeg.yaw = 0f; this.rightLeg.roll = 0f;
            this.leftLeg.pitch = 0f; this.leftLeg.yaw = 0f; this.leftLeg.roll = 0f;
            return;
        }
        if (pose != NotchNpcEntity.POSE_SITTING && pose != NotchNpcEntity.POSE_CHILLING) {
            return;
        }
        boolean chilling = (pose == NotchNpcEntity.POSE_CHILLING);
        float drop = chilling ? 12.5f : 10.5f;

        // Absolute pivots (not +=) so nothing accumulates across frames.
        this.head.pivotY = drop;
        this.head.pivotZ = chilling ? 2.0f : 0.0f;
        this.body.pivotY = drop;
        this.rightArm.pivotY = 2.0f + drop;
        this.leftArm.pivotY = 2.0f + drop;
        this.rightLeg.pivotY = 12.0f + 10.0f;
        this.leftLeg.pivotY = 12.0f + 10.0f;

        this.rightLeg.pitch = -1.5708f;
        this.leftLeg.pitch = -1.5708f;
        this.rightLeg.yaw = 0.26f;
        this.leftLeg.yaw = -0.26f;

        if (chilling) {
            this.body.pitch = -0.35f; // reclined
            if (!swinging) this.rightArm.pitch = 0.30f;
            this.leftArm.pitch = 0.30f;
            this.rightLeg.roll = -0.09f;
            this.leftLeg.roll = 0.09f;
        } else {
            if (!swinging) this.rightArm.pitch = -0.61f; // hands resting forward
            this.leftArm.pitch = -0.61f;
            this.rightLeg.roll = 0.0f;
            this.leftLeg.roll = 0.0f;
        }
    }

    /** The idle life layer. Statue is handled up-front (vanilla bob removed); Breathe IS the plain
     *  vanilla idle, so only Lively adds anything here: breathing chest, a gentle weight shift, and
     *  slow people-watching head glances — all additive on top of whatever pose is set. */
    private void applyIdleAnim(NotchNpcEntity entity, float t, boolean swinging) {
        if (entity.getPoseAnim() < NotchNpcEntity.ANIM_LIVELY) return;

        // Breathing chest with the arms drifting slightly out and back in.
        float breathe = MathHelper.sin(t * 0.045f);
        this.body.pitch += breathe * 0.012f;
        if (!swinging) this.rightArm.roll += 0.02f + breathe * 0.018f;
        this.leftArm.roll -= 0.02f + breathe * 0.018f;

        // A gentle weight shift.
        this.body.yaw += MathHelper.sin(t * 0.02f) * 0.035f;
        this.body.roll += MathHelper.sin(t * 0.02f + 0.4f) * 0.012f;

        // Slow head glances around, like it's people-watching.
        this.head.yaw += MathHelper.sin(t * 0.007f) * 0.3f;
        this.head.pitch += MathHelper.sin(t * 0.011f) * 0.06f;
        this.leftArm.pitch += MathHelper.sin(t * 0.013f) * 0.03f;
    }

    /** Apply one part's custom rotation (degrees; a zeroed part keeps a neutral stance). */
    private static void applyRot(ModelPart part, float[] angles, int idx) {
        part.pitch = angles[idx * 3] * DEG;
        part.yaw = angles[idx * 3 + 1] * DEG;
        part.roll = angles[idx * 3 + 2] * DEG;
    }

    /**
     * Toggle the skin's outer "second layer" (hat/jacket/sleeves/pants). These six parts are half the
     * player model's geometry and they're transparent, so they cost more per part than the base body —
     * but past a few blocks they're indistinguishable. The renderer hides them at range so a crowd of
     * NPCs costs roughly what a crowd of simpler mobs does.
     */
    public void setOverlaysVisible(boolean visible) {
        this.hat.visible = visible;
        this.jacket.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftSleeve.visible = visible;
        this.rightPants.visible = visible;
        this.leftPants.visible = visible;
    }

    /** Re-sync the skin overlay layers with the mutated parts. */
    private void syncOverlays() {
        this.hat.copyTransform(this.head);
        this.jacket.copyTransform(this.body);
        this.rightSleeve.copyTransform(this.rightArm);
        this.leftSleeve.copyTransform(this.leftArm);
        this.rightPants.copyTransform(this.rightLeg);
        this.leftPants.copyTransform(this.leftLeg);
    }
}
