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

    public int npcTint = -1;
    public float npcAlpha = 1.0f;

    private final boolean thinArms;
    //? if >=1.21.11 {
    /*private float attackTime;
    private boolean crouching;
    *///?}

    public NpcPlayerModel(ModelPart root, boolean thinArms) {
        super(root, thinArms);
        this.thinArms = thinArms;
    }

    //? if <1.21 {
    @Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack ps,
                               com.mojang.blaze3d.vertex.VertexConsumer vc,
                               int light, int overlay, float r, float g, float b, float a) {
        if (npcTint != -1) {
            r *= ((npcTint >> 16) & 0xFF) / 255.0f;
            g *= ((npcTint >> 8) & 0xFF) / 255.0f;
            b *= (npcTint & 0xFF) / 255.0f;
        }
        super.renderToBuffer(ps, vc, light, overlay, r, g, b, a * npcAlpha);
    }
    //?} elif <1.21.11 {
    /*@Override
    public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack ps,
                               com.mojang.blaze3d.vertex.VertexConsumer vc,
                               int light, int overlay, int color) {
        int a = Math.round(((color >>> 24) & 0xFF) * Math.max(0.0f, Math.min(1.0f, npcAlpha)));
        int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        if (npcTint != -1) {
            r = r * ((npcTint >> 16) & 0xFF) / 255;
            g = g * ((npcTint >> 8) & 0xFF) / 255;
            b = b * (npcTint & 0xFF) / 255;
        }
        super.renderToBuffer(ps, vc, light, overlay, (a << 24) | (r << 16) | (g << 8) | b);
    }
    *///?}

    private static final float DEG = (float) (Math.PI / 180.0);
    //? if >=1.21.11 {
    /*@Override
    public void setupAnim(net.minecraft.client.renderer.entity.state.AvatarRenderState state) {
        super.setupAnim(state);
        NotchNpcRenderState entity = NotchNpcRenderState.of(state);
        float animationProgress = state.ageInTicks;
        this.attackTime = state.attackTime;
        this.crouching = state.isCrouching;
    *///?} else {
    @Override
    public void setupAnim(NotchNpcEntity entity, float limbAngle, float limbDistance,
                          float animationProgress, float yHeadRot, float headPitch) {
        super.setupAnim(entity, limbAngle, limbDistance, animationProgress, yHeadRot, headPitch);
    //?}
        boolean swinging = this.attackTime > 0f;

        if (entity.getPoseAnim() == NotchNpcEntity.ANIM_STATUE) {
            removeVanillaArmBob(animationProgress);
        }
        applyPose(entity, animationProgress, swinging);
        applyIdleAnim(entity, animationProgress, swinging);
        applyAttackSwing(entity, animationProgress);
        float[] framed = sampleAnimation(entity.getPlayingAnimation(), entity.getAnimationStart(),
                entity.getIdleAnimation(), entity.getNpcAge(), animationProgress);
        if (framed != null) {
            applyFrame(this.head, framed, 0);
            applyFrame(this.body, framed, 1);
            applyFrame(this.rightArm, framed, 2);
            applyFrame(this.leftArm, framed, 3);
            applyFrame(this.rightLeg, framed, 4);
            applyFrame(this.leftLeg, framed, 5);
        }
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
        float breathe = Mth.sin(t * 0.045f);
        this.body.xRot += breathe * 0.012f;
        if (!swinging) this.rightArm.zRot += 0.02f + breathe * 0.018f;
        this.leftArm.zRot -= 0.02f + breathe * 0.018f;
        this.body.yRot += Mth.sin(t * 0.02f) * 0.035f;
        this.body.zRot += Mth.sin(t * 0.02f + 0.4f) * 0.012f;
        this.head.yRot += Mth.sin(t * 0.007f) * 0.3f;
        this.head.xRot += Mth.sin(t * 0.011f) * 0.06f;
        this.leftArm.xRot += Mth.sin(t * 0.013f) * 0.03f;
    }

    private static final float[] BASE_Y = {0f, 0f, 2f, 2f, 12f, 12f};
    private static final float[] BASE_X = {0f, 0f, -5f, 5f, -1.9f, 1.9f};

    private static void applyFrame(ModelPart part, float[] f, int idx) {
        applyRot(part, f, idx);
        int o = net.fugginbeenus.notchcurrency.npc.anim.NpcAnimation.SLOTS + idx * 3;
        if (f.length > o + 2) {
            part.x = BASE_X[idx] + f[o];
            part.y = BASE_Y[idx] + f[o + 1];
            part.z = f[o + 2];
        }
    }

    private static void applyRot(ModelPart part, float[] angles, int idx) {
        part.xRot = angles[idx * 3] * DEG;
        part.yRot = angles[idx * 3 + 1] * DEG;
        part.zRot = angles[idx * 3 + 2] * DEG;
    }

    private boolean overlaysHidden;

    public void setOverlaysVisible(boolean visible) {
        if (!visible) {
            this.hat.visible = false;
            this.jacket.visible = false;
            this.rightSleeve.visible = false;
            this.leftSleeve.visible = false;
            this.rightPants.visible = false;
            this.leftPants.visible = false;
            this.overlaysHidden = true;
        } else if (this.overlaysHidden) {
            this.hat.visible = true;
            this.jacket.visible = true;
            this.rightSleeve.visible = true;
            this.leftSleeve.visible = true;
            this.rightPants.visible = true;
            this.leftPants.visible = true;
            this.overlaysHidden = false;
        }
    }

    private void syncOverlays() {
        //? if <1.21.11 {
        this.hat.copyFrom(this.head);
        this.jacket.copyFrom(this.body);
        this.rightSleeve.copyFrom(this.rightArm);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftPants.copyFrom(this.leftLeg);
        //?}
    }

    private static float[] sampleAnimation(String playing, int start, String idle,
                                           int npcAge, float ageInTicks) {
        if (playing != null && !playing.isBlank()) {
            net.fugginbeenus.notchcurrency.npc.anim.NpcAnimation once = AnimationLibrary.get(playing);
            if (once != null) {
                float elapsed = npcAge - start;
                if (elapsed >= 0 && (once.loop() || elapsed < once.totalTicks())) {
                    return once.sample(elapsed);
                }
            }
        }
        if (idle == null || idle.isBlank()) return null;
        net.fugginbeenus.notchcurrency.npc.anim.NpcAnimation anim = AnimationLibrary.get(idle);
        return anim == null ? null : anim.sample(ageInTicks);
    }
}
