package com.zerog.neoessentials.crates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** A crate's full definition — display name, key item, animation style, and reward pool. */
public class CrateDefinition {
    public String id;
    /** The crate's visual identity — hologram line and GUI titles, which can render
     *  {@code {animation:NAME}}/gradients/rainbow live or per-render. Not used directly for
     *  chat messages or the key item's name; see {@link #getChatDisplayName()} and
     *  {@link #getCrateKeyDisplayName()}. */
    public String displayName;
    /** Optional plain/static name for chat and command-feedback messages (no_keys,
     *  reward-added, the "won a rare reward" broadcast, etc.) — chat can't animate, so reusing
     *  an animated {@link #displayName} there just flashes whatever single frame happened to be
     *  current, on every message. Falls back to {@link #displayName} if not set. */
    public String chatDisplayName;
    /** Optional override for the physical key item's display name. Falls back to
     *  {@code "&6" + displayName + " Key"} if not set. */
    public String crateKeyDisplayName;
    /** Cosmetic only — which vanilla block type a physical instance of this crate uses. */
    public String block = "minecraft:chest";
    public CrateAnimation animation = CrateAnimation.SEQUENTIAL;
    /** The item shown/given as this crate's key (see {@link CrateManager#buildKeyItem}). */
    public ItemStack keyItem = new ItemStack(net.minecraft.world.item.Items.TRIPWIRE_HOOK);
    public List<CrateReward> rewards = new ArrayList<>();

    /** @return {@link #chatDisplayName} if set, otherwise {@link #displayName}. */
    public String getChatDisplayName() {
        return (chatDisplayName != null && !chatDisplayName.isEmpty()) ? chatDisplayName : displayName;
    }

    /** @return {@link #crateKeyDisplayName} if set, otherwise {@code "&6" + displayName + " Key"}. */
    public String getCrateKeyDisplayName() {
        return (crateKeyDisplayName != null && !crateKeyDisplayName.isEmpty())
            ? crateKeyDisplayName : "&6" + displayName + " Key";
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("displayName", displayName);
        if (chatDisplayName != null) json.addProperty("chatDisplayName", chatDisplayName);
        if (crateKeyDisplayName != null) json.addProperty("crateKeyDisplayName", crateKeyDisplayName);
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
        def.chatDisplayName = json.has("chatDisplayName") ? json.get("chatDisplayName").getAsString() : null;
        def.crateKeyDisplayName = json.has("crateKeyDisplayName") ? json.get("crateKeyDisplayName").getAsString() : null;
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
