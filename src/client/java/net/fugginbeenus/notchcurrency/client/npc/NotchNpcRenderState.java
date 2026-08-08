package net.fugginbeenus.notchcurrency.client.npc;

// Everything the NPC renderer and model need to draw one NPC, copied off the entity once a frame.
//
// From 1.21.11 a renderer never sees the entity: it fills a render state and the drawing then works
// only from that. This rides along on the vanilla AvatarRenderState under a Fabric data key rather
// than subclassing it. Subclassing looks tidier and does not work: HumanoidArmorLayer requires its
// model to be a HumanoidModel of exactly the state type, and PlayerModel is bound to
// AvatarRenderState, so a subclass puts the armour layer out of reach.
//
// The accessors carry the same names as the entity's, so the animation code in NpcPlayerModel reads
// identically on every version and only its signature differs.
//
// The whole class is 1.21.11 and up; on older versions it compiles to nothing, which is legal.
//? if >=1.21.11 {
/*public class NotchNpcRenderState {

    public static final net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey<NotchNpcRenderState> KEY =
            net.fabricmc.fabric.api.client.rendering.v1.RenderStateDataKey.create();

    // Nothing has been drawn yet when a state is fresh, so a blank one has to be harmless.
    public static final NotchNpcRenderState BLANK = new NotchNpcRenderState();

    public static NotchNpcRenderState of(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        return state.getDataOrDefault(KEY, BLANK);
    }

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
    public String skinValue = "";

    // Which of the three drawing paths this NPC takes, decided while the entity is still in hand.
    public boolean invisible;
    public boolean useGeo;
    // A disguised NPC borrows another entity's renderer. Its state has to be extracted here, during
    // the pass that still knows the partial tick, and kept for the drawing pass to submit.
    @SuppressWarnings("rawtypes")
    public net.minecraft.client.renderer.entity.EntityRenderer proxyRenderer;
    public net.minecraft.client.renderer.entity.state.EntityRenderState proxyState;
    public boolean showDisguiseName;
    public net.minecraft.network.chat.Component displayName;

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
