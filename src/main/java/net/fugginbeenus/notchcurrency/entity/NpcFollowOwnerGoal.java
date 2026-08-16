package net.fugginbeenus.notchcurrency.entity;

import java.util.EnumSet;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

public class NpcFollowOwnerGoal extends Goal {

    private static final double START_DIST_SQ = 9.0;
    private static final double STOP_DIST_SQ = 5.0;
    private static final double TELEPORT_DIST_SQ = 900.0;

    private final NotchNpcEntity npc;
    private final double speed;
    private Player owner;
    private int updateCountdown;

    public NpcFollowOwnerGoal(NotchNpcEntity npc, double speed) {
        this.npc = npc;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {

        Player p = npc.resolveFollowTarget();
        if (p == null || p.isSpectator() || !p.isAlive()) return false;
        if (npc.distanceToSqr(p) < START_DIST_SQ) return false;
        this.owner = p;
        return true;
    }

    @Override
    public boolean canContinueToUse() {

        return owner != null && owner.isAlive() && !owner.isSpectator()
                && !npc.getNavigation().isDone()
                && npc.distanceToSqr(owner) > STOP_DIST_SQ;
    }

    @Override
    public void start() {
        this.updateCountdown = 0;
        npc.getNavigation().moveTo(owner, speed);
    }

    @Override
    public void tick() {
        npc.getLookControl().setLookAt(owner, 10.0f, npc.getMaxHeadXRot());
        if (npc.distanceToSqr(owner) > TELEPORT_DIST_SQ) {
            npc.moveTo(owner.getX(), owner.getY(), owner.getZ(), npc.getYRot(), npc.getXRot());
            npc.getNavigation().stop();
            return;
        }

        if (--this.updateCountdown <= 0) {
            this.updateCountdown = this.adjustedTickDelay(10);
            npc.getNavigation().moveTo(owner, speed);
        }
    }

    @Override
    public void stop() {
        owner = null;
        npc.getNavigation().stop();
    }
}
