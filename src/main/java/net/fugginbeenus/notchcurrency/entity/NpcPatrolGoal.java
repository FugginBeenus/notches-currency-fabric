package net.fugginbeenus.notchcurrency.entity;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.util.math.BlockPos;

import java.util.EnumSet;
import java.util.List;

public class NpcPatrolGoal extends Goal {

    private static final double ARRIVE_DIST_SQ = 6.25; // within 2.5 blocks = arrived

    private final NotchNpcEntity npc;
    private int index;
    private int repathCountdown;
    private int waitCountdown; // linger at a reached waypoint before moving on

    public NpcPatrolGoal(NotchNpcEntity npc) {
        this.npc = npc;
        this.setControls(EnumSet.of(Control.MOVE));
    }

    @Override
    public boolean canStart() {
        return !npc.getWaypoints().isEmpty();
    }

    @Override
    public boolean shouldContinue() {
        return !npc.getWaypoints().isEmpty();
    }

    @Override
    public void start() {
        // Begin at the nearest waypoint so a fresh patrol doesn't sprint across the whole route.
        List<BlockPos> route = npc.getWaypoints();
        int nearest = 0;
        double best = Double.MAX_VALUE;
        for (int i = 0; i < route.size(); i++) {
            double d = route.get(i).getSquaredDistance(npc.getPos());
            if (d < best) {
                best = d;
                nearest = i;
            }
        }
        this.index = nearest;
        this.repathCountdown = 0;
        this.waitCountdown = 0;
    }

    @Override
    public void tick() {
        List<BlockPos> route = npc.getWaypoints();
        if (route.isEmpty()) return;
        if (index >= route.size()) index = 0;

        // Lingering at a waypoint: stand still until the dwell timer runs out.
        if (waitCountdown > 0) {
            waitCountdown--;
            return;
        }

        BlockPos target = route.get(index);
        if (target.getSquaredDistance(npc.getPos()) <= ARRIVE_DIST_SQ) {
            index = (index + 1) % route.size();
            target = route.get(index);
            repathCountdown = 0;
            // Dwell time is read live so the editor's setting applies mid-patrol.
            if (npc.getPatrolWaitTicks() > 0) {
                waitCountdown = this.getTickCount(npc.getPatrolWaitTicks());
                npc.getNavigation().stop();
                return;
            }
        }
        if (--repathCountdown <= 0) {
            repathCountdown = this.getTickCount(20);
            // Speed is read live so the editor's setting applies mid-patrol.
            npc.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5,
                    npc.getPatrolSpeed());
        }
    }

    @Override
    public void stop() {
        npc.getNavigation().stop();
    }
}
