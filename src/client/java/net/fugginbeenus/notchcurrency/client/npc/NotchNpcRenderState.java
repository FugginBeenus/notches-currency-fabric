package net.fugginbeenus.notchcurrency.client.npc;
//? if >=1.21.11 {
/*public class NotchNpcRenderState {

    public static final NotchNpcRenderState BLANK = new NotchNpcRenderState();

    public static NotchNpcRenderState of(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        NotchNpcRenderState npc = ((NotchNpcStateHolder) state).notchcurrency$getNpcState();
        return npc == null ? BLANK : npc;
    }

    public static void attachTo(net.minecraft.client.renderer.entity.state.EntityRenderState state,
                                NotchNpcRenderState npc) {
        ((NotchNpcStateHolder) state).notchcurrency$setNpcState(npc);
    }

    public String modelId = "";
    public int poseAnim;
    public int npcPose;
    public float[] customPoseAngles;
    public float clientSwingStartAge = -1000f;
    public boolean slim;
    public float nameOffset;
    public float bodyOffset;
    public String subtitle = "";
    public float scaleX = 1f, scaleY = 1f, scaleZ = 1f;
    public String[] billboard = new String[0];
    public net.minecraft.resources.Identifier texture;
    public String skinValue = "";
    public boolean invisible;
    public boolean useGeo;
    @SuppressWarnings("rawtypes")
    public net.minecraft.client.renderer.entity.EntityRenderer proxyRenderer;
    public net.minecraft.client.renderer.entity.state.EntityRenderState proxyState;
    public boolean showLabel;
    public boolean talkBubble;
    public float ageInTicks;
    public float bodyHeight;
    public net.minecraft.network.chat.Component displayName;
    public int getPoseAnim() { return poseAnim; }
    public int getNpcPose() { return npcPose; }
    public float[] getCustomPoseAngles() { return customPoseAngles; }
    public boolean isSlim() { return slim; }
    public float getNameOffset() { return nameOffset; }
    public float getBodyOffset() { return bodyOffset; }
    public String getSubtitle() { return subtitle; }
    public float npcScale() { return scaleX; }
    public float getScaleY() { return scaleY; }
    public float getScaleZ() { return scaleZ; }
}
*///?}
