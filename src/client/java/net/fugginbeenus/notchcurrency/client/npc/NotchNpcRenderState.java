package net.fugginbeenus.notchcurrency.client.npc;

// Everything the NPC renderer and model need to draw one NPC, copied off the entity once a frame.
//
// From 1.21.11 a renderer never sees the entity: it fills a render state in one pass and draws from
// it in a later one, so this has to survive the gap. It rides on the vanilla AvatarRenderState in a
// field a mixin adds, the way EasyNPC does it; see NotchNpcStateHolder and EntityRenderStateMixin.
// Fabric's RenderStateDataKey was tried first and does not work: it sets during extract and reads
// back empty at drawing time.
//
// Carrying the data is only half of it. The drawing pass picks the renderer from the state, and
// hands anything that is an AvatarRenderState to the vanilla player renderer, so our own submit was
// never called. EntityRenderDispatcherMixin puts NPCs back on their own renderer.
//
// The accessors carry the same names as the entity's, so the animation code in NpcPlayerModel reads
// identically on every version and only its signature differs.
//
// The whole class is 1.21.11 and up; on older versions it compiles to nothing, which is legal.
//? if >=1.21.11 {
/*public class NotchNpcRenderState {

    // Nothing has been drawn yet when a state is fresh, so a blank one has to be harmless.
    public static final NotchNpcRenderState BLANK = new NotchNpcRenderState();

    public static NotchNpcRenderState of(net.minecraft.client.renderer.entity.state.EntityRenderState state) {
        NotchNpcRenderState npc = ((NotchNpcStateHolder) state).notchcurrency$getNpcState();
        return npc == null ? BLANK : npc;
    }

    // Hangs this NPC's data on the vanilla state, for the drawing pass to pick up.
    public static void attachTo(net.minecraft.client.renderer.entity.state.EntityRenderState state,
                                NotchNpcRenderState npc) {
        ((NotchNpcStateHolder) state).notchcurrency$setNpcState(npc);
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

    public String getSubtitle() { return subtitle; }

    public float npcScale() { return scaleX; }

    public float getScaleY() { return scaleY; }

    public float getScaleZ() { return scaleZ; }
}
*///?}
