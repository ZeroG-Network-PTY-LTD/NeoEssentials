package com.zerog.neoessentials.crates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A crate's full definition — display name, key item, animation style, and reward pool. */
public class CrateDefinition {
    public String id;
    public String displayName;
    /** Cosmetic only — which vanilla block type a physical instance of this crate uses. */
    public String block = "minecraft:chest";
    public CrateAnimation animation = CrateAnimation.SEQUENTIAL;
    /** The item shown/given as this crate's key (see {@link CrateManager#buildKeyItem}). */
    public ItemStack keyItem = new ItemStack(net.minecraft.world.item.Items.TRIPWIRE_HOOK);
    public List<CrateReward> rewards = new ArrayList<>();

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("displayName", displayName);
        json.addProperty("block", block);
        json.addProperty("animation", animation.name().toLowerCase());
        json.add("keyItem", CrateItemSerializer.serialize(keyItem));
        JsonArray rewardsArray = new JsonArray();
        for (CrateReward reward : rewards) rewardsArray.add(reward.toJson());
        json.add("rewards", rewardsArray);
        return json;
    }

    public static CrateDefinition fromJson(String id, JsonObject json) {
        CrateDefinition def = new CrateDefinition();
        def.id = id;
        def.displayName = json.has("displayName") ? json.get("displayName").getAsString() : id;
        def.block = json.has("block") ? json.get("block").getAsString() : "minecraft:chest";
        def.animation = CrateAnimation.parse(json.has("animation") ? json.get("animation").getAsString() : null);
        if (json.has("keyItem")) {
            ItemStack key = CrateItemSerializer.deserialize(json.getAsJsonObject("keyItem"));
            if (!key.isEmpty()) def.keyItem = key;
        }
        if (json.has("rewards")) {
            for (var el : json.getAsJsonArray("rewards")) {
                def.rewards.add(CrateReward.fromJson(el.getAsJsonObject()));
            }
        }
        return def;
    }
}
