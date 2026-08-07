package net.fugginbeenus.notchcurrency.economy.bounty;

import net.minecraft.nbt.CompoundTag;

public class TakenBounty {

    private final Bounty bounty;         // copy of the offer (its id identifies this taken bounty)
    private final long expiresGameTime;  // personal deadline
    private int progress;                // KILL progress

    public TakenBounty(Bounty bounty, long expiresGameTime, int progress) {
        this.bounty = bounty;
        this.expiresGameTime = expiresGameTime;
        this.progress = progress;
    }

    public Bounty bounty() { return bounty; }
    public long expiresGameTime() { return expiresGameTime; }
    public int progress() { return progress; }

    public int addProgress(int amount) {
        progress = Math.min(bounty.getRequired(), progress + amount);
        return progress;
    }

    public boolean isKillComplete() {
        return bounty.getType() == BountyType.KILL && progress >= bounty.getRequired();
    }

    public boolean isExpired(long now) {
        return expiresGameTime > 0 && now >= expiresGameTime;
    }

    public CompoundTag toNbt() {
        CompoundTag o = new CompoundTag();
        o.put("Bounty", bounty.toNbt());
        o.putLong("Expires", expiresGameTime);
        o.putInt("Progress", progress);
        return o;
    }

    public static TakenBounty fromNbt(CompoundTag o) {
        return new TakenBounty(Bounty.fromNbt(o.getCompound("Bounty")), o.getLong("Expires"), o.getInt("Progress"));
    }
}
