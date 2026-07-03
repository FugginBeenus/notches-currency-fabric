package net.fugginbeenus.notchcurrency.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

/**
 * Follow-the-owner movement for a Notch NPC in FOLLOW_OWNER mode. Walks toward the owner when they
 * get more than a few blocks away, and quietly teleports to catch up if they're left far behind
 * (so the NPC can't be permanently lost). Only added to the goal selector while the mode is active.
 */
public class NpcFollowOwnerGoal extends Goal {

    private static final double START_DIST_SQ = 9.0;   // start walking beyond 3 blocks
    private static final double STOP_DIST_SQ = 5.0;    // stop once within ~2.2 blocks
    private static final double TELEPORT_DIST_SQ = 900.0; // catch up beyond 30 blocks

    private final NotchNpcEntity npc;
    private final double speed;
    private PlayerEntity owner;
    private int updateCountdown;

    public NpcFollowOwnerGoal(NotchNpcEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        // Follows the owner by default, or a specific player when one is named in the editor.
        PlayerEntity p = npc.resolveFollowTarget();
        if (p == null || p.isSpectator() || !p.isAlive()) return false;
        if (npc.squaredDistanceTo(p) < START_DIST_SQ) return false;
        this.owner = p;
        return true;
    }

    @Override
    public boolean shouldContinue() {
        // Wolf-style: also stop when the current path ends. canStart() re-fires right away while the
        // owner is still far, so the stop/restart loop keeps the follow going even between re-paths.
        return owner != null && owner.isAlive() && !owner.isSpectator()
                && !npc.getNavigation().isIdle()
                && npc.squaredDistanceTo(owner) > STOP_DIST_SQ;
    }

    @Override
    public void start() {
        this.updateCountdown = 0;
        npc.getNavigation().startMovingTo(owner, speed);
    }

    @Override
    public void tick() {
        npc.getLookControl().lookAt(owner, 10.0f, npc.getMaxLookPitchChange());
        if (npc.squaredDistanceTo(owner) > TELEPORT_DIST_SQ) {
            npc.refreshPositionAndAngles(owner.getX(), owner.getY(), owner.getZ(), npc.getYaw(), npc.getPitch());
            npc.getNavigation().stop();
            return;
        }
        // Countdown re-path (vanilla FollowOwnerGoal pattern). NOTE: goals are only fully ticked
        // every OTHER game tick, so an `age % N` check can permanently miss depending on entity-id
        // parity — which froze following after the first path. getTickCount() adjusts for that.
        if (--this.updateCountdown <= 0) {
            this.updateCountdown = this.getTickCount(10);
            npc.getNavigation().startMovingTo(owner, speed);
        }
    }

    @Override
    public void stop() {
        owner = null;
        npc.getNavigation().stop();
    }
}
