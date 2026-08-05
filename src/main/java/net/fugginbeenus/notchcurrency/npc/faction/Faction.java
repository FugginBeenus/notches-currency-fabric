package net.fugginbeenus.notchcurrency.npc.faction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * One faction. Lives in {@link FactionState}, which is the only thing that owns it — a Recruiter NPC
 * merely stores this faction's {@link #id()} and points at it. That's deliberate: losing the NPC, to
 * a creeper or a careless pick-up, must never take the faction and its members with it.
 */
public class Faction {

    public static final int MAX_ID_LENGTH = 24;
    public static final int MAX_NAME_LENGTH = 32;

    private final String id;
    private String displayName;
    private Formatting color;
    /** Who founded it. Keeps management rights with a person, not with an entity that can die. */
    @Nullable private UUID founder;

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
        return nbt;
    }

    @Nullable
    public static Faction fromNbt(NbtCompound nbt) {
        String id = nbt.getString("Id");
        if (id == null || id.isBlank()) return null;
        Formatting color = Formatting.byName(nbt.getString("Color"));
        return new Faction(
                id,
                nbt.getString("Name").isBlank() ? id : nbt.getString("Name"),
                color == null || !color.isColor() ? Formatting.WHITE : color,
                nbt.containsUuid("Founder") ? nbt.getUuid("Founder") : null);
    }
}
