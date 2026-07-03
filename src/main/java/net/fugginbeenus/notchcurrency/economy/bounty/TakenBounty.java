package net.fugginbeenus.notchcurrency.economy.bounty;

import net.minecraft.nbt.NbtCompound;

/**
 * A bounty a player has <em>taken</em> from the board (Bountiful-style): a personal copy of the
 * offer with its own completion deadline and (for KILL bounties) accumulated progress. Kills only
 * count toward taken bounties, and the reward is collected by returning to the board.
 */
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

    public NbtCompound toNbt() {
        NbtCompound o = new NbtCompound();
        o.put("Bounty", bounty.toNbt());
        o.putLong("Expires", expiresGameTime);
        o.putInt("Progress", progress);
        return o;
    }

    public static TakenBounty fromNbt(NbtCompound o) {
        return new TakenBounty(Bounty.fromNbt(o.getCompound("Bounty")), o.getLong("Expires"), o.getInt("Progress"));
    }
}
