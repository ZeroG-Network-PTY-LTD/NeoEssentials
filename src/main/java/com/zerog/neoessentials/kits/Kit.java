package com.zerog.neoessentials.kits;

import net.minecraft.world.item.ItemStack;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import com.zerog.neoessentials.util.ResourceLocationHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Represents a kit containing items, metadata, and usage restrictions.
 * Kits can have cooldowns, permission requirements, and usage limits.
 */
public class Kit {
    private final String name;
    private final String displayName;
    private final String description;
    private final List<ItemStack> items;
    private final long cooldownMillis;
    private final String permission;
    private final int maxUses;
    private final boolean enabled;
    private final List<String> commands;

    /**
     * Creates a new Kit instance.
     *
     * @param name Unique identifier for the kit (lowercase, no spaces)
     * @param displayName Human-readable name for display
     * @param description Brief description of the kit
     * @param items List of ItemStacks in the kit
     * @param cooldownMillis Cooldown between uses in milliseconds
     * @param permission Required permission node (null for no requirement)
     * @param maxUses Maximum uses per player (-1 for unlimited)
     * @param enabled Whether the kit is currently enabled
     */
    public Kit(String name, String displayName, String description, List<ItemStack> items,
               long cooldownMillis, String permission, int maxUses, boolean enabled) {
        this(name, displayName, description, items, cooldownMillis, permission, maxUses, enabled, null);
    }

    /**
     * Creates a new Kit instance with console commands run on claim (see {@link #getCommands()}).
     */
    public Kit(String name, String displayName, String description, List<ItemStack> items,
               long cooldownMillis, String permission, int maxUses, boolean enabled, List<String> commands) {
        this.name = name.toLowerCase().replaceAll("[^a-z0-9_]", ""); // Sanitize name
        this.displayName = displayName != null ? displayName : name;
        this.description = description != null ? description : "";
        this.items = new ArrayList<>(items != null ? items : Collections.emptyList());
        this.cooldownMillis = Math.max(0, cooldownMillis);
        this.permission = permission;
        this.maxUses = maxUses;
        this.enabled = enabled;
        this.commands = new ArrayList<>(commands != null ? commands : Collections.emptyList());
    }

    // Getters
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
    public List<ItemStack> getItems() { return new ArrayList<>(items); }
    public long getCooldownMillis() { return cooldownMillis; }
    public String getPermission() { return permission; }
    public int getMaxUses() { return maxUses; }
    public boolean isEnabled() { return enabled; }

    /**
     * Console commands run (as the server, with {player} replaced by the claiming player's
     * name) each time this kit is successfully claimed — e.g. granting a permission, playing
     * a sound, or broadcasting a message alongside the items.
     */
    public List<String> getCommands() { return new ArrayList<>(commands); }

    @SuppressWarnings("unused") // Public API method - reserved for future use
    public Map<String, Object> getMetadata() { return new HashMap<>(); }

    /**
     * Gets cooldown duration in a human-readable format.
     */
    @SuppressWarnings("unused") // Public API method
    public String getCooldownDisplay() {
        if (cooldownMillis == 0) return "No cooldown";
        
        long seconds = TimeUnit.MILLISECONDS.toSeconds(cooldownMillis);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(cooldownMillis);
        long hours = TimeUnit.MILLISECONDS.toHours(cooldownMillis);
        
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }
    
    /**
     * Checks if the kit has any restrictions.
     */
    @SuppressWarnings("unused") // Public API method
    public boolean hasRestrictions() {
        return cooldownMillis > 0 || permission != null || maxUses > 0;
    }
    
    /**
     * Creates a copy of this kit with modified properties.
     */
    @SuppressWarnings("unused") // Public API method
    public Kit withEnabled(boolean enabled) {
        return new Kit(name, displayName, description, items, cooldownMillis,
                      permission, maxUses, enabled, commands);
    }

    @SuppressWarnings("unused") // Public API method
    public Kit withCooldown(long cooldownMillis) {
        return new Kit(name, displayName, description, items, cooldownMillis,
                      permission, maxUses, enabled, commands);
    }

    @SuppressWarnings("unused") // Public API method
    public Kit withPermission(String permission) {
        return new Kit(name, displayName, description, items, cooldownMillis,
                      permission, maxUses, enabled, commands);
    }
    
    /**
     * Converts the kit to JSON for storage.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("displayName", displayName);
        json.addProperty("description", description);
        json.addProperty("cooldownMillis", cooldownMillis);
        json.addProperty("permission", permission);
        json.addProperty("maxUses", maxUses);
        json.addProperty("enabled", enabled);
        
        // Serialize items
        JsonArray itemsArray = new JsonArray();
        for (ItemStack item : items) {
            if (!item.isEmpty()) {
                try {
                    // Guard against null registry key (defensive — modded environments may differ)
                    net.minecraft.resources.Identifier itemKey =
                            BuiltInRegistries.ITEM.getKey(item.getItem());
                    //noinspection ConstantConditions
                    if (itemKey == null) {
                        // Skip items with no registry key to avoid NPE
                        continue;
                    }

                    JsonObject itemJson = new JsonObject();
                    itemJson.addProperty("item", itemKey.toString());
                    itemJson.addProperty("count", item.getCount());

                    // Full DataComponentMap (enchantments, custom name, dyed color, potion
                    // contents, attribute modifiers, etc.) — not just CUSTOM_DATA. Since
                    // 1.20.5 most of what admins actually put on a kit item (enchant it,
                    // rename it in an anvil, dye leather armor) lives in typed components,
                    // not raw NBT, so a CUSTOM_DATA-only round-trip silently dropped it on
                    // every kit reload/restart. Reuses the same codec the Auction House
                    // already uses to solve this exact problem.
                    try {
                        JsonElement components = com.zerog.neoessentials.auctionhouse.AuctionComponentSerializer
                            .serialize(item.getComponents());
                        itemJson.add("components", components);
                    } catch (Exception e) {
                        // Fall back to the legacy CUSTOM_DATA-only field if the server registry
                        // isn't available yet (e.g. AuctionComponentSerializer not initialized).
                        if (item.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA)) {
                            var customData = item.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
                            if (customData != null) {
                                itemJson.addProperty("nbt", customData.copyTag().toString());
                            }
                        }
                    }

                    itemsArray.add(itemJson);
                } catch (Exception e) {
                    // Skip individual items that fail to serialize; don't abort the whole kit
                }
            }
        }
        json.add("items", itemsArray);

        if (!commands.isEmpty()) {
            JsonArray commandsArray = new JsonArray();
            for (String command : commands) commandsArray.add(command);
            json.add("commands", commandsArray);
        }

        return json;
    }
    
    /**
     * Creates a Kit from JSON data.
     */
    public static Kit fromJson(JsonObject json) {
        String name = json.get("name").getAsString();
        String displayName = json.has("displayName") ? json.get("displayName").getAsString() : name;
        String description = json.has("description") ? json.get("description").getAsString() : "";

        // Handle both "cooldown" (seconds) and "cooldownMillis" for backward compatibility
        long cooldownMillis = 0;
        if (json.has("cooldownMillis")) {
            cooldownMillis = json.get("cooldownMillis").getAsLong();
        } else if (json.has("cooldown")) {
            // Convert seconds to milliseconds
            long cooldownSeconds = json.get("cooldown").getAsLong();
            cooldownMillis = cooldownSeconds * 1000;
        }

        // Always set permission node to neoessentials.kits.<kitname> if not present
        String permission = json.has("permission") && !json.get("permission").getAsString().isEmpty()
                ? json.get("permission").getAsString()
                : ("neoessentials.kits." + name.toLowerCase());
        int maxUses = json.has("maxUses") ? json.get("maxUses").getAsInt() : -1;
        boolean enabled = !json.has("enabled") || json.get("enabled").getAsBoolean();
        
        // Deserialize items
        List<ItemStack> items = new ArrayList<>();
        if (json.has("items")) {
            JsonArray itemsArray = json.getAsJsonArray("items");
            for (JsonElement element : itemsArray) {
                JsonObject itemJson = element.getAsJsonObject();
                try {
                    String itemString = itemJson.get("item").getAsString();

                    // Use helper to create Identifier safely across versions
                    Identifier itemId = ResourceLocationHelper.parse(itemString);

                    // Use getOptional() for Minecraft 1.21.4+ compatibility
                    Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
                    if (item == null) {
                        // Skip unknown items
                        continue;
                    }
                    int count = itemJson.has("count") ? itemJson.get("count").getAsInt() : 1;
                    
                    ItemStack stack = new ItemStack(item, count);

                    // Preferred: full DataComponentMap (see toJson() — covers enchantments,
                    // custom name, dyed color, etc., not just raw NBT).
                    if (itemJson.has("components")) {
                        try {
                            var components = com.zerog.neoessentials.auctionhouse.AuctionComponentSerializer
                                .deserialize(itemJson.get("components"));
                            stack.applyComponents(components);
                        } catch (Exception e) {
                            // Skip invalid/unresolvable component data
                        }
                    } else if (itemJson.has("nbt")) {
                        // Legacy format (kits saved before this fix) — CUSTOM_DATA only.
                        try {
                            CompoundTag nbt = TagParser.parseCompoundFully(itemJson.get("nbt").getAsString());
                            stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                                     net.minecraft.world.item.component.CustomData.of(nbt));
                        } catch (Exception e) {
                            // Skip invalid NBT
                        }
                    }

                    items.add(stack);
                } catch (Throwable e) {
                    // Skip invalid items
                }
            }
        }
        
        // Commands run as console on claim (see getCommands()) — optional, absent in most kits.
        List<String> commands = new ArrayList<>();
        if (json.has("commands")) {
            for (JsonElement element : json.getAsJsonArray("commands")) {
                commands.add(element.getAsString());
            }
        }

        return new Kit(name, displayName, description, items, cooldownMillis,
                      permission, maxUses, enabled, commands);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Kit other)) return false;
        return Objects.equals(name, other.name);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
    
    @Override
    public String toString() {
        return String.format("Kit{name='%s', displayName='%s', items=%d, enabled=%s}", 
                           name, displayName, items.size(), enabled);
    }
}