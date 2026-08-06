package net.fugginbeenus.notchcurrency.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.TrackTargetGoal;
import net.minecraft.entity.player.PlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public class NpcProtectOwnerGoal extends TrackTargetGoal {

    private final NotchNpcEntity npc;
    @Nullable private LivingEntity candidate;
    private int handledSwing;

    public NpcProtectOwnerGoal(NotchNpcEntity npc) {
        super(npc, false);
        this.npc = npc;
        this.setControls(EnumSet.of(Control.TARGET));
    }

    @Override
    public boolean canStart() {
        if (!npc.protectsOwner()) return false;
        PlayerEntity owner = npc.resolveFollowTarget();
        if (owner == null) return false;

        // Someone hitting the owner is the urgent case, so it wins.
        LivingEntity pick = owner.getAttacker();
        if (!worthAttacking(pick, owner)) {
            LivingEntity target = owner.getAttacking();
            int swing = owner.getLastAttackTime();
            pick = (swing != handledSwing && worthAttacking(target, owner)) ? target : null;
            if (pick != null) handledSwing = swing;
        }
        if (pick == null) return false;

        this.candidate = pick;
        return canTrack(pick, TargetPredicate.DEFAULT);
    }

    private boolean worthAttacking(@Nullable LivingEntity e, PlayerEntity owner) {
        if (e == null || !e.isAlive() || e == npc || e == owner) return false;
        if (e instanceof PlayerEntity p && (p.isCreative() || p.isSpectator())) return false;
        return !npc.isAlly(e);
    }

    @Override
    public void start() {
        this.mob.setTarget(candidate);
        super.start();
    }
}
