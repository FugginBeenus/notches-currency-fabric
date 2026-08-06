package net.fugginbeenus.notchcurrency.economy.cosmetic;

import com.google.gson.JsonObject;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public record CosmeticOffer(String id, String name, ItemStack icon, long price, boolean oneTime,
                            boolean isCommand, ItemStack itemReward, String command) {

    public static CosmeticOffer fromJson(String id, JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : id;
        long price = o.has("price") ? o.get("price").getAsLong() : 0L;
        boolean oneTime = !o.has("one_time") || o.get("one_time").getAsBoolean();

        JsonObject reward = o.has("reward") ? o.getAsJsonObject("reward") : new JsonObject();
        String type = reward.has("type") ? reward.get("type").getAsString() : "item";
        boolean isCommand = type.equalsIgnoreCase("command");

        String command = reward.has("command") ? reward.get("command").getAsString() : "";
        ItemStack itemReward = ItemStack.EMPTY;
        if (!isCommand && reward.has("item")) {
            int count = reward.has("count") ? reward.get("count").getAsInt() : 1;
            itemReward = new ItemStack(itemOf(reward.get("item").getAsString()), Math.max(1, count));
        }

        // Icon: explicit, else the item reward, else a generic placeholder.
        ItemStack icon;
        if (o.has("icon")) {
            icon = new ItemStack(itemOf(o.get("icon").getAsString()));
        } else if (!itemReward.isEmpty()) {
            icon = itemReward.copy();
            icon.setCount(1);
        } else {
            icon = new ItemStack(Items.PLAYER_HEAD);
        }

        return new CosmeticOffer(id, name, icon, Math.max(0, price), oneTime, isCommand, itemReward, command);
    }

    private static net.minecraft.item.Item itemOf(String idStr) {
        Identifier id = Identifier.tryParse(idStr);
        net.minecraft.item.Item item = id == null ? null : Registries.ITEM.get(id);
        return (item == null || item == Items.AIR) ? Items.PAPER : item;
    }
}
