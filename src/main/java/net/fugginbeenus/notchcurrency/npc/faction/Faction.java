package net.fugginbeenus.notchcurrency.npc.faction;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;

public class Faction {

    public static final int MAX_ID_LENGTH = 24;
    public static final int MAX_NAME_LENGTH = 32;

    public static final int MAX_MOTTO_LENGTH = 64;

    private final String id;
    private String displayName;
    private ChatFormatting color;
    @Nullable private UUID founder;
    private String motto = "";
    private int joinFee = 0;
    private boolean openToJoin = true;

    public Faction(String id, String displayName, ChatFormatting color, @Nullable UUID founder) {
        this.id = id;
        this.displayName = displayName;
        this.color = color == null ? ChatFormatting.WHITE : color;
        this.founder = founder;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public ChatFormatting color() { return color; }
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

    public void setColor(ChatFormatting color) {
        if (color != null && color.isColor()) this.color = color;
    }

    public void setFounder(@Nullable UUID founder) { this.founder = founder; }

    public boolean isFoundedBy(UUID player) {
        return founder != null && founder.equals(player);
    }

    @Nullable
    public static String toId(String name) {
        if (name == null) return null;
        String id = name.trim().toLowerCase().replace(' ', '_').replaceAll("[^a-z0-9_-]", "");
        while (id.startsWith("_") || id.startsWith("-")) id = id.substring(1);
        if (id.isEmpty()) return null;
        return id.length() > MAX_ID_LENGTH ? id.substring(0, MAX_ID_LENGTH) : id;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putString("Id", id);
        nbt.putString("Name", displayName);
        nbt.putString("Color", color.getName());
        if (founder != null) nbt.putUUID("Founder", founder);
        nbt.putString("Motto", motto);
        nbt.putInt("JoinFee", joinFee);
        nbt.putBoolean("Open", openToJoin);
        return nbt;
    }

    @Nullable
    public static Faction fromNbt(CompoundTag nbt) {
        String id = nbt.getString("Id");
        if (id == null || id.isBlank()) return null;
        ChatFormatting color = ChatFormatting.getByName(nbt.getString("Color"));
        Faction f = new Faction(
                id,
                nbt.getString("Name").isBlank() ? id : nbt.getString("Name"),
                color == null || !color.isColor() ? ChatFormatting.WHITE : color,
                nbt.hasUUID("Founder") ? nbt.getUUID("Founder") : null);
        f.setMotto(nbt.getString("Motto"));
        f.setJoinFee(nbt.getInt("JoinFee"));
        // Factions saved before this setting existed were all open, which is the sane default anyway.
        f.setOpenToJoin(!nbt.contains("Open") || nbt.getBoolean("Open"));
        return f;
    }
}
