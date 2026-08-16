package net.fugginbeenus.notchcurrency.entity;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.player.Player;

public class NpcProtectOwnerGoal extends TargetGoal {

    private final NotchNpcEntity npc;
    @Nullable private LivingEntity candidate;
    private int handledSwing;

    public NpcProtectOwnerGoal(NotchNpcEntity npc) {
        super(npc, false);
        this.npc = npc;
        this.setFlags(EnumSet.of(Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        if (!npc.protectsOwner()) return false;
        Player owner = npc.resolveFollowTarget();
        if (owner == null) return false;

        LivingEntity pick = owner.getLastHurtByMob();
        if (!worthAttacking(pick, owner)) {
            LivingEntity target = owner.getLastHurtMob();
            int swing = owner.getLastHurtMobTimestamp();
            pick = (swing != handledSwing && worthAttacking(target, owner)) ? target : null;
            if (pick != null) handledSwing = swing;
        }
        if (pick == null) return false;

        this.candidate = pick;
        return canAttack(pick, TargetingConditions.DEFAULT);
    }

    private boolean worthAttacking(@Nullable LivingEntity e, Player owner) {
        if (e == null || !e.isAlive() || e == npc || e == owner) return false;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return false;
        return !npc.isAlly(e);
    }

    @Override
    public void start() {
        this.mob.setTarget(candidate);
        super.start();
    }
}
