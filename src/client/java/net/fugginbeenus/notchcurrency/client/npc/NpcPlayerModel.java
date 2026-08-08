package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
//? if >=1.21.11 {
/*import net.minecraft.client.model.player.PlayerModel;
*///?} else {
import net.minecraft.client.model.PlayerModel;
//?}
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

//? if >=1.21.11 {
/*public class NpcPlayerModel extends PlayerModel {
*///?} else {
public class NpcPlayerModel extends PlayerModel<NotchNpcEntity> {
//?}

    private final boolean thinArms;

    // Both used to be inherited from HumanoidModel and are on the render state now. Keeping the
    // names as fields means the animation code below reads the same on every version.
    //? if >=1.21.11 {
    /*private float attackTime;
    private boolean crouching;
    *///?}

    public NpcPlayerModel(ModelPart root, boolean thinArms) {
        super(root, thinArms);
        this.thinArms = thinArms;
    }

    private static final float DEG = (float) (Math.PI / 180.0);

    // A model is handed a render state rather than the entity from 1.21.11. Unpacking it under the
    // old names keeps everything below this line identical across versions.
    //? if >=1.21.11 {
    /*@Override
    public void setupAnim(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        super.setupAnim(state);
        NotchNpcRenderState entity = (NotchNpcRenderState) state;
        float animationProgress = state.ageInTicks;
        this.attackTime = state.attackTime;
        this.crouching = state.isCrouching;
    *///?} else {
    @Override
    public void setupAnim(NotchNpcEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float yHeadRot, float headPitch) {
        super.setupAnim(entity, limbAngle, limbDistance, animationProgress, yHeadRot, headPitch);
    //?}

        // Mid attack swing (the model field is the render-time value the swing animates with):
        // vanilla just animated the main arm: poses must not stomp it.
        boolean swinging = this.attackTime > 0f;

        if (entity.getPoseAnim() == NotchNpcEntity.ANIM_STATUE) {
            removeVanillaArmBob(animationProgress);
        }
        applyPose(entity, animationProgress, swinging);
        applyIdleAnim(entity, animationProgress, swinging);
        applyAttackSwing(entity, animationProgress);
        syncOverlays();
    }

    private void removeVanillaArmBob(float t) {
        float bobRoll = Mth.cos(t * 0.09f) * 0.05f + 0.05f;
        float bobPitch = Mth.sin(t * 0.067f) * 0.05f;
        this.rightArm.zRot -= bobRoll;
        this.leftArm.zRot += bobRoll;
        this.rightArm.xRot -= bobPitch;
        this.leftArm.xRot += bobPitch;
    }

    private static final float SWING_TICKS = 8f;

    //? if >=1.21.11 {
    /*private void applyAttackSwing(NotchNpcRenderState entity, float t) {
    *///?} else {
    private void applyAttackSwing(NotchNpcEntity entity, float t) {
    //?}
        float p = this.attackTime;
        if (p <= 0f) {
            float since = t - entity.clientSwingStartAge;
            if (since < 0f || since >= SWING_TICKS) return;
            p = since / SWING_TICKS;
        }
        float wind = Mth.sin(Math.min(1f, p) * (float) Math.PI); // up, strike, recover
        this.rightArm.xRot = -0.5f - 1.7f * wind;
        this.rightArm.yRot = -0.15f * wind;
        this.rightArm.zRot = 0f;
        this.body.yRot = -0.2f * wind;
    }

    private void resetPivots() {
        this.body.zRot = 0f;
        this.head.zRot = 0f;
        if (this.crouching) return;
        float armY = thinArms ? 2.5f : 2.0f;
        this.head.setPos(0f, 0f, 0f);
        this.body.setPos(0f, 0f, 0f);
        this.rightArm.setPos(-5f, armY, 0f);
        this.leftArm.setPos(5f, armY, 0f);
        this.rightLeg.setPos(-1.9f, 12f, 0f);
        this.leftLeg.setPos(1.9f, 12f, 0f);
    }

    //? if >=1.21.11 {
    /*private void applyPose(NotchNpcRenderState entity, float animationProgress, boolean swinging) {
    *///?} else {
    private void applyPose(NotchNpcEntity entity, float animationProgress, boolean swinging) {
    //?}
        resetPivots();
        int pose = entity.getNpcPose();
        if (pose == NotchNpcEntity.POSE_WAVING) {
            // Raise the right arm UP AND OUT to the side (POSITIVE roll swings it away from the body so
            // it clears the head; negative rolled it across the chest). Gentle wave via sway.
            this.body.yRot = 0.12f;
            if (!swinging) {
                float sway = (float) Math.sin(animationProgress * 0.18f) * 0.22f;
                this.rightArm.xRot = 0f;
                this.rightArm.yRot = 0f;
                this.rightArm.zRot = 2.2f + sway;
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
            this.head.xRot = 0f; this.head.yRot = 0f; this.head.zRot = 0f;
            this.body.xRot = 0f; this.body.yRot = 0f; this.body.zRot = 0f;
            if (!swinging) {
                this.rightArm.xRot = -0.35f; this.rightArm.yRot = 0f; this.rightArm.zRot = 0.05f;
            }
            this.leftArm.xRot = -0.35f; this.leftArm.yRot = 0f; this.leftArm.zRot = -0.05f;
            this.rightLeg.xRot = 0f; this.rightLeg.yRot = 0f; this.rightLeg.zRot = 0f;
            this.leftLeg.xRot = 0f; this.leftLeg.yRot = 0f; this.leftLeg.zRot = 0f;
            return;
        }
        if (pose != NotchNpcEntity.POSE_SITTING && pose != NotchNpcEntity.POSE_CHILLING) {
            return;
        }
        boolean chilling = (pose == NotchNpcEntity.POSE_CHILLING);
        float drop = chilling ? 12.5f : 10.5f;

        // Absolute pivots (not +=) so nothing accumulates across frames.
        this.head.y = drop;
        this.head.z = chilling ? 2.0f : 0.0f;
        this.body.y = drop;
        this.rightArm.y = 2.0f + drop;
        this.leftArm.y = 2.0f + drop;
        this.rightLeg.y = 12.0f + 10.0f;
        this.leftLeg.y = 12.0f + 10.0f;

        this.rightLeg.xRot = -1.5708f;
        this.leftLeg.xRot = -1.5708f;
        this.rightLeg.yRot = 0.26f;
        this.leftLeg.yRot = -0.26f;

        if (chilling) {
            this.body.xRot = -0.35f; // reclined
            if (!swinging) this.rightArm.xRot = 0.30f;
            this.leftArm.xRot = 0.30f;
            this.rightLeg.zRot = -0.09f;
            this.leftLeg.zRot = 0.09f;
        } else {
            if (!swinging) this.rightArm.xRot = -0.61f; // hands resting forward
            this.leftArm.xRot = -0.61f;
            this.rightLeg.zRot = 0.0f;
            this.leftLeg.zRot = 0.0f;
        }
    }

    //? if >=1.21.11 {
    /*private void applyIdleAnim(NotchNpcRenderState entity, float t, boolean swinging) {
    *///?} else {
    private void applyIdleAnim(NotchNpcEntity entity, float t, boolean swinging) {
    //?}
        if (entity.getPoseAnim() < NotchNpcEntity.ANIM_LIVELY) return;

        // Breathing chest with the arms drifting slightly out and back in.
        float breathe = Mth.sin(t * 0.045f);
        this.body.xRot += breathe * 0.012f;
        if (!swinging) this.rightArm.zRot += 0.02f + breathe * 0.018f;
        this.leftArm.zRot -= 0.02f + breathe * 0.018f;

        // A gentle weight shift.
        this.body.yRot += Mth.sin(t * 0.02f) * 0.035f;
        this.body.zRot += Mth.sin(t * 0.02f + 0.4f) * 0.012f;

        // Slow head glances around, like it's people-watching.
        this.head.yRot += Mth.sin(t * 0.007f) * 0.3f;
        this.head.xRot += Mth.sin(t * 0.011f) * 0.06f;
        this.leftArm.xRot += Mth.sin(t * 0.013f) * 0.03f;
    }

    private static void applyRot(ModelPart part, float[] angles, int idx) {
        part.xRot = angles[idx * 3] * DEG;
        part.yRot = angles[idx * 3 + 1] * DEG;
        part.zRot = angles[idx * 3 + 2] * DEG;
    }

    public void setOverlaysVisible(boolean visible) {
        this.hat.visible = visible;
        this.jacket.visible = visible;
        this.rightSleeve.visible = visible;
        this.leftSleeve.visible = visible;
        this.rightPants.visible = visible;
        this.leftPants.visible = visible;
    }

    private void syncOverlays() {
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
    }
}
