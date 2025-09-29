package com.zerog.neoessentials.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.FileReader;

public class GlobalConfig {
    private static boolean economyEnabled = true;
    private static boolean loaded = false;
    private static JsonObject itemsConfig = null;

    public static void load(File configFile) {
        loaded = true;
        try (FileReader reader = new FileReader(configFile)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root.has("modules")) {
                JsonObject modules = root.getAsJsonObject("modules");
                if (modules.has("economyEnabled")) {
                    economyEnabled = modules.get("economyEnabled").getAsBoolean();
                }
            }
            if (root.has("items")) {
                itemsConfig = root.getAsJsonObject("items");
            }
        } catch (Exception e) {
            // Default to enabled if config missing or error
            economyEnabled = true;
            itemsConfig = null;
        }
    }

    public static boolean isEconomyEnabled() {
        if (!loaded) load(new File("config/neoessentials/config.json"));
        return economyEnabled;
    }

    public static JsonObject getItemsConfig() {
        if (!loaded) load(new File("config/neoessentials/config.json"));
        return itemsConfig;
    }

    // Helper methods for common item config options
    public static boolean isPermissionBasedItemSpawn() {
        JsonObject items = getItemsConfig();
        return items != null && items.has("permission-based-item-spawn") && items.get("permission-based-item-spawn").getAsBoolean();
    }
    public static boolean isDropItemsIfFull() {
        JsonObject items = getItemsConfig();
        return items != null && items.has("drop-items-if-full") && items.get("drop-items-if-full").getAsBoolean();
    }
    public static int getOversizedStackSize() {
        JsonObject items = getItemsConfig();
        return items != null && items.has("oversized-stacksize") ? items.get("oversized-stacksize").getAsInt() : 64;
    }
    public static int getDefaultStackSize() {
        JsonObject items = getItemsConfig();
        return items != null && items.has("default-stack-size") ? items.get("default-stack-size").getAsInt() : -1;
    }
    public static boolean isUnsafeEnchantmentsAllowed() {
        JsonObject items = getItemsConfig();
        return items != null && items.has("unsafe-enchantments") && items.get("unsafe-enchantments").getAsBoolean();
    }
    public static java.util.List<String> getItemSpawnBlacklist() {
        java.util.List<String> list = new java.util.ArrayList<>();
        JsonObject items = getItemsConfig();
        if (items != null && items.has("item-spawn-blacklist")) {
            for (var el : items.getAsJsonArray("item-spawn-blacklist")) {
                list.add(el.getAsString());
            }
        }
        return list;
    }
}