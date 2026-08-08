package net.fugginbeenus.notchcurrency.economy.cosmetic;

import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * One entry in the cosmetic shop, as written in a datapack.
 *
 * <p>The icon and the reward are held as item ids rather than stacks. From 26.2 an ItemStack cannot
 * be built before item components are bound, and these are parsed during a resource reload, which
 * happens earlier than that. The two accessors build a stack when something actually needs one.
 */
public record CosmeticOffer(String id, String name, String iconItemId, long price, boolean oneTime,
                            boolean isCommand, String rewardItemId, int rewardCount, String command) {

    public ItemStack icon() {
        if (!iconItemId.isEmpty()) return new ItemStack(itemOf(iconItemId));
        ItemStack reward = itemReward();
        if (!reward.isEmpty()) {
            ItemStack single = reward.copy();
            single.setCount(1);
            return single;
        }
        return new ItemStack(Items.PLAYER_HEAD);
    }

    public ItemStack itemReward() {
        if (isCommand || rewardItemId.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(itemOf(rewardItemId), Math.max(1, rewardCount));
    }

    public static CosmeticOffer fromJson(String id, JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : id;
        long price = o.has("price") ? o.get("price").getAsLong() : 0L;
        boolean oneTime = !o.has("one_time") || o.get("one_time").getAsBoolean();

        JsonObject reward = o.has("reward") ? o.getAsJsonObject("reward") : new JsonObject();
        String type = reward.has("type") ? reward.get("type").getAsString() : "item";
        boolean isCommand = type.equalsIgnoreCase("command");

        String command = reward.has("command") ? reward.get("command").getAsString() : "";
        String rewardItemId = !isCommand && reward.has("item") ? reward.get("item").getAsString() : "";
        int rewardCount = reward.has("count") ? reward.get("count").getAsInt() : 1;

        // Icon: explicit, else the item reward, else a generic placeholder. Resolved in icon().
        String iconItemId = o.has("icon") ? o.get("icon").getAsString() : "";

        return new CosmeticOffer(id, name, iconItemId, Math.max(0, price), oneTime,
                isCommand, rewardItemId, rewardCount, command);
    }

    private static net.minecraft.world.item.Item itemOf(String idStr) {
        ResourceLocation id = ResourceLocation.tryParse(idStr);
        net.minecraft.world.item.Item item = id == null ? null : BuiltInRegistries.ITEM.get(id);
        return (item == null || item == Items.AIR) ? Items.PAPER : item;
    }
}
