package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Player model with pose presets layered on top. Sitting/Chilling are applied AFTER vanilla
 * setAngles because they need per-part position drops (the whole body moves down ~10px) — the
 * vanilla riding flag can't do that and gets overwritten by the renderer anyway. Angle/offset
 * values adapted from EasyNPC's baked pose data. Overlay parts are re-synced afterwards.
 */
public class NpcPlayerModel extends PlayerEntityModel<NotchNpcEntity> {

    public NpcPlayerModel(ModelPart root, boolean thinArms) {
        super(root, thinArms);
    }

    private static final float DEG = (float) (Math.PI / 180.0);

    @Override
    public void setAngles(NotchNpcEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        super.setAngles(entity, limbAngle, limbDistance, animationProgress, headYaw, headPitch);

        int pose = entity.getNpcPose();
        if (pose == NotchNpcEntity.POSE_WAVING) {
            // Right arm raised in greeting, with a gentle sway.
            this.rightArm.pitch = -2.9f;
            this.rightArm.roll = 0.15f + (float) Math.sin(animationProgress * 0.15f) * 0.12f;
            syncOverlays();
            return;
        }
        if (pose == NotchNpcEntity.POSE_CUSTOM) {
            float[] a = entity.getCustomPoseAngles();
            if (a != null) {
                applyRot(this.head, a, 0);
                applyRot(this.body, a, 1);
                applyRot(this.rightArm, a, 2);
                applyRot(this.leftArm, a, 3);
                applyRot(this.rightLeg, a, 4);
                applyRot(this.leftLeg, a, 5);
            }
            syncOverlays();
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
            this.rightArm.pitch = 0.30f;
            this.leftArm.pitch = 0.30f;
            this.rightLeg.roll = -0.09f;
            this.leftLeg.roll = 0.09f;
        } else {
            this.rightArm.pitch = -0.61f; // hands resting forward
            this.leftArm.pitch = -0.61f;
            this.rightLeg.roll = 0.0f;
            this.leftLeg.roll = 0.0f;
        }

        syncOverlays();
    }

    /** Apply one part's custom rotation (degrees; a zeroed part keeps a neutral stance). */
    private static void applyRot(ModelPart part, float[] angles, int idx) {
        part.pitch = angles[idx * 3] * DEG;
        part.yaw = angles[idx * 3 + 1] * DEG;
        part.roll = angles[idx * 3 + 2] * DEG;
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
