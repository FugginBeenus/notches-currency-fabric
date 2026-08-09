package net.fugginbeenus.notchcurrency.client.npc;

/**
 * A place on a vanilla render state to keep one NPC's drawing data.
 *
 * <p>From 1.21.11 a renderer fills a render state during one pass and draws from it in a later one,
 * so the NPC's pose, model choice and skin have to survive the gap. Two ways of carrying them across
 * were tried and neither held: Fabric's RenderStateDataKey sets during extract and reads back empty
 * at drawing time, and giving the state its own subclass makes the renderer vanish from the
 * dispatcher's map altogether.
 *
 * <p>So the data goes in a real field, added to the vanilla class by a mixin, which is how EasyNPC
 * solves the same problem. A field cannot be cleared behind our back or lost to a lookup.
 *
 * <p>The mixin implements this on every render state; the cast is always safe.
 */
public interface NotchNpcStateHolder {

    //? if >=1.21.11 {
    /*NotchNpcRenderState notchcurrency$getNpcState();

    void notchcurrency$setNpcState(NotchNpcRenderState state);
    *///?}
}
