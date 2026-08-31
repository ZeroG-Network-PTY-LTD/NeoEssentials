package com.zerog.neoessentials.crates;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** One entry in a crate's reward pool. */
public class CrateReward {
    public String id;
    public double weight;
    public ItemStack item = ItemStack.EMPTY;
    public List<String> commands = new ArrayList<>();
    public boolean broadcastRare = false;
    public String broadcastMessage = "";

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("weight", weight);
        json.add("item", CrateItemSerializer.serialize(item));
        if (!commands.isEmpty()) {
            JsonArray cmds = new JsonArray();
            commands.forEach(cmds::add);
            json.add("commands", cmds);
        }
        json.addProperty("broadcastRare", broadcastRare);
        if (broadcastMessage != null && !broadcastMessage.isEmpty()) {
            json.addProperty("broadcastMessage", broadcastMessage);
        }
        return json;
    }

    public static CrateReward fromJson(JsonObject json) {
        CrateReward reward = new CrateReward();
        reward.id = json.has("id") ? json.get("id").getAsString() : java.util.UUID.randomUUID().toString().substring(0, 8);
        reward.weight = json.has("weight") ? json.get("weight").getAsDouble() : 1.0;
        reward.item = json.has("item") ? CrateItemSerializer.deserialize(json.getAsJsonObject("item")) : ItemStack.EMPTY;
        if (json.has("commands")) {
            for (var el : json.getAsJsonArray("commands")) reward.commands.add(el.getAsString());
        }
        reward.broadcastRare = json.has("broadcastRare") && json.get("broadcastRare").getAsBoolean();
        reward.broadcastMessage = json.has("broadcastMessage") ? json.get("broadcastMessage").getAsString() : "";
        return reward;
    }
}
