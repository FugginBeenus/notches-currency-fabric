package net.fugginbeenus.notchcurrency.client.npc;

// Everything the NPC renderer and model need to draw one NPC, copied off the entity once a frame.
//
// From 1.21.11 a renderer never sees the entity: it fills one of these and the drawing then works
// only from it. The accessors below deliberately carry the same names as the ones on the entity, so
// the animation code in NpcPlayerModel and the body of the renderer read identically on every
// version and only their method signatures differ.
//
// The whole class is 1.21.11 and up, since the render-state architecture does not exist before it.
// On older versions this file compiles to nothing at all, which is legal and is how the two eras
// share one source tree.
//? if >=1.21.11 {
/*public class NotchNpcRenderState extends net.minecraft.client.renderer.entity.state.AvatarRenderState {

    public int poseAnim;
    public int npcPose;
    public float[] customPoseAngles;
    public float clientSwingStartAge = -1000f;
    public boolean slim;
    public float nameOffset;
    public String subtitle = "";
    public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    public String[] billboard = new String[0];
    public net.minecraft.resources.Identifier texture;

    public int getPoseAnim() { return poseAnim; }

    public int getNpcPose() { return npcPose; }

    public float[] getCustomPoseAngles() { return customPoseAngles; }

    public boolean isSlim() { return slim; }

    public float getNameOffset() { return nameOffset; }

    public String getSubtitle() { return subtitle; }

    public float npcScale() { return scaleX; }

    public float getScaleY() { return scaleY; }

    public float getScaleZ() { return scaleZ; }
}
*///?}
