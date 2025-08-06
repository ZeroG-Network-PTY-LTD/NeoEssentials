package com.zerog.neoessentials.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.math.BigDecimal;
import java.util.*;

/**
 * Enhanced Shop Configuration Manager
 * Handles config-based pricing, sell system, and shop settings
 */
public class ShopConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopConfigManager.class);
    private static ShopConfigManager instance;
    
    private JsonObject shopConfig;
    private JsonObject shopSettings;
    private JsonObject shopCategories;
    
    // Shop settings cache
    private boolean sellSystemEnabled = true;
    private double sellRateMultiplier = 0.75;
    private int maxItemsPerPurchase = 64;
    private boolean quantitySelectionEnabled = true;
    private boolean shoppingCartEnabled = true;
    private boolean confirmationDialogsEnabled = true;
    private String currencySymbol = "$";
    private Map<String, String> soundEffects = new HashMap<>();
    
    private ShopConfigManager() {
        loadConfig();
    }
    
    public static ShopConfigManager getInstance() {
        if (instance == null) {
            instance = new ShopConfigManager();
        }
        return instance;
    }
    
    /**
     * Load shop configuration from file
     */
    private void loadConfig() {
        try {
            File configFile = new File("config/shop_config.json");
            if (!configFile.exists()) {
                LOGGER.warn("Shop config file not found, using defaults");
                createDefaultConfig(configFile);
                return;
            }
            
            try (FileReader reader = new FileReader(configFile)) {
                shopConfig = JsonParser.parseReader(reader).getAsJsonObject();
                
                // Load shop settings
                if (shopConfig.has("shop_settings")) {
                    shopSettings = shopConfig.getAsJsonObject("shop_settings");
                    loadShopSettings();
                }
                
                // Load shop categories
                if (shopConfig.has("shop_categories")) {
                    shopCategories = shopConfig.getAsJsonObject("shop_categories");
                }
                
                LOGGER.info("Shop configuration loaded successfully");
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to load shop configuration", e);
            shopConfig = new JsonObject();
            shopSettings = new JsonObject();
            shopCategories = new JsonObject();
        }
    }
    
    /**
     * Load shop settings into cache
     */
    private void loadShopSettings() {
        if (shopSettings.has("enable_sell_system")) {
            sellSystemEnabled = shopSettings.get("enable_sell_system").getAsBoolean();
        }
        if (shopSettings.has("sell_rate_multiplier")) {
            sellRateMultiplier = shopSettings.get("sell_rate_multiplier").getAsDouble();
        }
        if (shopSettings.has("max_items_per_purchase")) {
            maxItemsPerPurchase = shopSettings.get("max_items_per_purchase").getAsInt();
        }
        if (shopSettings.has("enable_quantity_selection")) {
            quantitySelectionEnabled = shopSettings.get("enable_quantity_selection").getAsBoolean();
        }
        if (shopSettings.has("enable_shopping_cart")) {
            shoppingCartEnabled = shopSettings.get("enable_shopping_cart").getAsBoolean();
        }
        if (shopSettings.has("enable_confirmation_dialogs")) {
            confirmationDialogsEnabled = shopSettings.get("enable_confirmation_dialogs").getAsBoolean();
        }
        if (shopSettings.has("currency_symbol")) {
            currencySymbol = shopSettings.get("currency_symbol").getAsString();
        }
        
        // Load sound effects
        if (shopSettings.has("sound_effects")) {
            JsonObject sounds = shopSettings.getAsJsonObject("sound_effects");
            soundEffects.clear();
            sounds.entrySet().forEach(entry -> 
                soundEffects.put(entry.getKey(), entry.getValue().getAsString()));
        }
    }
    
    /**
     * Create default configuration file
     */
    private void createDefaultConfig(File configFile) {
        // This would create the default config - for now we'll use the existing one
        LOGGER.info("Using embedded default shop configuration");
    }
    
    /**
     * Get buy price for an item
     */
    public BigDecimal getBuyPrice(String itemId) {
        try {
            for (Map.Entry<String, JsonElement> categoryEntry : shopCategories.entrySet()) {
                JsonObject category = categoryEntry.getValue().getAsJsonObject();
                if (category.has("items")) {
                    JsonObject items = category.getAsJsonObject("items");
                    if (items.has(itemId)) {
                        JsonObject item = items.getAsJsonObject(itemId);
                        if (item.has("buy_price")) {
                            return new BigDecimal(item.get("buy_price").getAsString());
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting buy price for item: " + itemId, e);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get sell price for an item
     */
    public BigDecimal getSellPrice(String itemId) {
        if (!sellSystemEnabled) {
            return BigDecimal.ZERO;
        }
        
        try {
            for (Map.Entry<String, JsonElement> categoryEntry : shopCategories.entrySet()) {
                JsonObject category = categoryEntry.getValue().getAsJsonObject();
                if (category.has("items")) {
                    JsonObject items = category.getAsJsonObject("items");
                    if (items.has(itemId)) {
                        JsonObject item = items.getAsJsonObject(itemId);
                        if (item.has("sell_price")) {
                            return new BigDecimal(item.get("sell_price").getAsString());
                        } else if (item.has("buy_price")) {
                            // Calculate sell price from buy price if not specified
                            BigDecimal buyPrice = new BigDecimal(item.get("buy_price").getAsString());
                            return buyPrice.multiply(new BigDecimal(sellRateMultiplier));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting sell price for item: " + itemId, e);
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get item display name from config
     */
    public String getItemDisplayName(String itemId) {
        try {
            for (Map.Entry<String, JsonElement> categoryEntry : shopCategories.entrySet()) {
                JsonObject category = categoryEntry.getValue().getAsJsonObject();
                if (category.has("items")) {
                    JsonObject items = category.getAsJsonObject("items");
                    if (items.has(itemId)) {
                        JsonObject item = items.getAsJsonObject(itemId);
                        if (item.has("display_name")) {
                            return item.get("display_name").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting display name for item: " + itemId, e);
        }
        return itemId; // Fallback to item ID
    }
    
    /**
     * Get item description from config
     */
    public List<String> getItemDescription(String itemId) {
        List<String> description = new ArrayList<>();
        try {
            for (Map.Entry<String, JsonElement> categoryEntry : shopCategories.entrySet()) {
                JsonObject category = categoryEntry.getValue().getAsJsonObject();
                if (category.has("items")) {
                    JsonObject items = category.getAsJsonObject("items");
                    if (items.has(itemId)) {
                        JsonObject item = items.getAsJsonObject(itemId);
                        if (item.has("description")) {
                            item.getAsJsonArray("description").forEach(element ->
                                description.add(element.getAsString()));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting description for item: " + itemId, e);
        }
        return description;
    }
    
    /**
     * Get max stack size for an item
     */
    public int getMaxStackSize(String itemId) {
        try {
            for (Map.Entry<String, JsonElement> categoryEntry : shopCategories.entrySet()) {
                JsonObject category = categoryEntry.getValue().getAsJsonObject();
                if (category.has("items")) {
                    JsonObject items = category.getAsJsonObject("items");
                    if (items.has(itemId)) {
                        JsonObject item = items.getAsJsonObject(itemId);
                        if (item.has("max_stack")) {
                            return item.get("max_stack").getAsInt();
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting max stack for item: " + itemId, e);
        }
        return 64; // Default Minecraft stack size
    }
    
    /**
     * Get all items in a category
     */
    public Set<String> getCategoryItems(String category) {
        Set<String> items = new HashSet<>();
        try {
            if (shopCategories.has(category)) {
                JsonObject categoryObj = shopCategories.getAsJsonObject(category);
                if (categoryObj.has("items")) {
                    JsonObject itemsObj = categoryObj.getAsJsonObject("items");
                    items.addAll(itemsObj.keySet());
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting items for category: " + category, e);
        }
        return items;
    }
    
    /**
     * Get category display name
     */
    public String getCategoryDisplayName(String category) {
        try {
            if (shopCategories.has(category)) {
                JsonObject categoryObj = shopCategories.getAsJsonObject(category);
                if (categoryObj.has("display_name")) {
                    return categoryObj.get("display_name").getAsString();
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting display name for category: " + category, e);
        }
        return category.substring(0, 1).toUpperCase() + category.substring(1);
    }
    
    /**
     * Get category icon item
     */
    public Item getCategoryIcon(String category) {
        try {
            if (shopCategories.has(category)) {
                JsonObject categoryObj = shopCategories.getAsJsonObject(category);
                if (categoryObj.has("icon")) {
                    String iconId = categoryObj.get("icon").getAsString();
                    ResourceLocation resourceLocation = ResourceLocation.parse(iconId);
                    return BuiltInRegistries.ITEM.get(resourceLocation);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error getting icon for category: " + category, e);
        }
        return null;
    }
    
    /**
     * Check if an item can be sold
     */
    public boolean canSellItem(String itemId) {
        return sellSystemEnabled && getSellPrice(itemId).compareTo(BigDecimal.ZERO) > 0;
    }
    
    // Getters for shop settings
    public boolean isSellSystemEnabled() { return sellSystemEnabled; }
    public double getSellRateMultiplier() { return sellRateMultiplier; }
    public int getMaxItemsPerPurchase() { return maxItemsPerPurchase; }
    public boolean isQuantitySelectionEnabled() { return quantitySelectionEnabled; }
    public boolean isShoppingCartEnabled() { return shoppingCartEnabled; }
    public boolean isConfirmationDialogsEnabled() { return confirmationDialogsEnabled; }
    public String getCurrencySymbol() { return currencySymbol; }
    public String getSoundEffect(String action) { return soundEffects.getOrDefault(action, ""); }
    
    /**
     * Get all available categories
     */
    public Set<String> getAvailableCategories() {
        return shopCategories.keySet();
    }
    
    /**
     * Reload configuration from file
     */
    public void reloadConfig() {
        loadConfig();
        LOGGER.info("Shop configuration reloaded");
    }
    
    /**
     * Convert item stack to item ID string
     */
    public String getItemId(ItemStack itemStack) {
        ResourceLocation resourceLocation = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        return resourceLocation != null ? resourceLocation.toString() : "";
    }
    
    /**
     * Convert item ID string to ItemStack
     */
    public ItemStack getItemStack(String itemId) {
        try {
            ResourceLocation resourceLocation = ResourceLocation.parse(itemId);
            Item item = BuiltInRegistries.ITEM.get(resourceLocation);
            return item != null ? new ItemStack(item) : ItemStack.EMPTY;
        } catch (Exception e) {
            LOGGER.error("Error creating ItemStack for: " + itemId, e);
            return ItemStack.EMPTY;
        }
    }
}
