package net.fugginbeenus.notchcurrency.npc.faction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One faction. Lives in {@link FactionState}, which is the only thing that owns it: a Recruiter NPC
 * merely stores this faction's {@link #id()} and points at it. That's deliberate: losing the NPC, to
 * a creeper or a careless pick-up, must never take the faction and its members with it.
 */
public class Faction {

    public static final int MAX_ID_LENGTH = 24;
    public static final int MAX_NAME_LENGTH = 32;

    public static final int MAX_MOTTO_LENGTH = 64;

    private final String id;
    private String displayName;
    private Formatting color;
    /** Who founded it. Keeps management rights with a person, not with an entity that can die. */
    @Nullable private UUID founder;
    /** One line of pitch, shown to anyone weighing up whether to sign on. */
    private String motto = "";
    /** What it costs to join, in coins. Zero is free. */
    private int joinFee = 0;
    /** Closed factions still show themselves off; they just don't take walk-ins. */
    private boolean openToJoin = true;

    public Faction(String id, String displayName, Formatting color, @Nullable UUID founder) {
        this.id = id;
        this.displayName = displayName;
        this.color = color == null ? Formatting.WHITE : color;
        this.founder = founder;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public Formatting color() { return color; }
    @Nullable public UUID founder() { return founder; }
    public String motto() { return motto; }
    public int joinFee() { return joinFee; }
    public boolean isOpenToJoin() { return openToJoin; }

    public void setMotto(String motto) {
        String next = motto == null ? "" : motto.trim();
        this.motto = next.length() > MAX_MOTTO_LENGTH ? next.substring(0, MAX_MOTTO_LENGTH) : next;
    }

    public void setJoinFee(int fee) { this.joinFee = Math.max(0, Math.min(1_000_000, fee)); }

    public void setOpenToJoin(boolean open) { this.openToJoin = open; }

    public void setDisplayName(String name) {
        if (name != null && !name.isBlank()) {
            this.displayName = name.length() > MAX_NAME_LENGTH ? name.substring(0, MAX_NAME_LENGTH) : name;
        }
    }

    public void setColor(Formatting color) {
        if (color != null && color.isColor()) this.color = color;
    }

    public void setFounder(@Nullable UUID founder) { this.founder = founder; }

    public boolean isFoundedBy(UUID player) {
        return founder != null && founder.equals(player);
    }

    /**
     * Turn a typed name into an id: lowercase, spaces to underscores, nothing exotic. Returns null if
     * nothing usable survives, so a name of pure punctuation can't become a faction with a blank id.
     */
    @Nullable
    public static String toId(String name) {
        if (name == null) return null;
        String id = name.trim().toLowerCase().replace(' ', '_').replaceAll("[^a-z0-9_-]", "");
        while (id.startsWith("_") || id.startsWith("-")) id = id.substring(1);
        if (id.isEmpty()) return null;
        return id.length() > MAX_ID_LENGTH ? id.substring(0, MAX_ID_LENGTH) : id;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Id", id);
        nbt.putString("Name", displayName);
        nbt.putString("Color", color.getName());
        if (founder != null) nbt.putUuid("Founder", founder);
        nbt.putString("Motto", motto);
        nbt.putInt("JoinFee", joinFee);
        nbt.putBoolean("Open", openToJoin);
        return nbt;
    }

    @Nullable
    public static Faction fromNbt(NbtCompound nbt) {
        String id = nbt.getString("Id");
        if (id == null || id.isBlank()) return null;
        Formatting color = Formatting.byName(nbt.getString("Color"));
        Faction f = new Faction(
                id,
                nbt.getString("Name").isBlank() ? id : nbt.getString("Name"),
                color == null || !color.isColor() ? Formatting.WHITE : color,
                nbt.containsUuid("Founder") ? nbt.getUuid("Founder") : null);
        f.setMotto(nbt.getString("Motto"));
        f.setJoinFee(nbt.getInt("JoinFee"));
        // Factions saved before this setting existed were all open, which is the sane default anyway.
        f.setOpenToJoin(!nbt.contains("Open") || nbt.getBoolean("Open"));
        return f;
    }
}
