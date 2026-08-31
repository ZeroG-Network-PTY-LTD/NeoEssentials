package com.zerog.neoessentials.crates;

import com.google.gson.JsonObject;
import com.zerog.neoessentials.auctionhouse.AuctionComponentSerializer;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.util.ResourceLocationHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Full-fidelity {@code ItemStack} <-> JSON round-trip for crate reward/key items — same
 * {@code {item, count, components}} shape as {@code Kit.toJson()}/{@code fromJson()}, reusing
 * {@link AuctionComponentSerializer} for the actual DataComponentMap serialization rather than
 * reinventing it.
 */
public final class CrateItemSerializer {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrateItemSerializer.class);

    private CrateItemSerializer() {}

    public static JsonObject serialize(ItemStack item) {
        JsonObject json = new JsonObject();
        if (item == null || item.isEmpty()) return json;

        Identifier itemKey = BuiltInRegistries.ITEM.getKey(item.getItem());
        json.addProperty("item", itemKey.toString());
        json.addProperty("count", item.getCount());
        try {
            json.add("components", AuctionComponentSerializer.serialize(item.getComponents()));
        } catch (Exception e) {
            NeoLog.debug(LOGGER, LogCategory.CRATES, "Failed to serialize item components for crate reward — item saved without them", e);
        }
        return json;
    }

    public static ItemStack deserialize(JsonObject json) {
        if (json == null || !json.has("item")) return ItemStack.EMPTY;
        try {
            Identifier itemId = ResourceLocationHelper.parse(json.get("item").getAsString());
            Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
            if (item == null) return ItemStack.EMPTY;

            int count = json.has("count") ? json.get("count").getAsInt() : 1;
            ItemStack stack = new ItemStack(item, count);

            if (json.has("components")) {
                try {
                    var components = AuctionComponentSerializer.deserialize(json.get("components"));
                    stack.applyComponents(components);
                } catch (Exception e) {
                    NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to apply saved components to crate item", e);
                }
            }
            return stack;
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CRATES, "Failed to deserialize crate item JSON", e);
            return ItemStack.EMPTY;
        }
    }
}
