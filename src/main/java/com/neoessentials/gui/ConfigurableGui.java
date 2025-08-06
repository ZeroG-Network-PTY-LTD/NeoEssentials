package com.neoessentials.gui;

import com.google.gson.JsonObject;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.SimpleContainer;
import net.minecraft.network.chat.Component;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configurable GUI container that handles JSON-based GUI layouts
 * and manages click events for NeoEssentials features.
 */
public class ConfigurableGui extends ChestMenu {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurableGui.class);
    
    private final JsonObject config;
    private final Player player;
    private final String guiType;
    
    public ConfigurableGui(MenuType<ChestMenu> menuType, int containerId, Inventory playerInventory, 
                          SimpleContainer container, Component title, JsonObject config) {
        super(menuType, containerId, playerInventory, container, container.getContainerSize() / 9);
        this.config = config;
        this.player = (Player) playerInventory.player;
        this.guiType = extractGuiType(config);
        
        LOGGER.debug("Created ConfigurableGui for player '{}' with type '{}'", 
                    player.getName().getString(), guiType);
    }
    
    /**
     * Extract GUI type from configuration
     */
    private String extractGuiType(JsonObject config) {
        // Look for GUI type in various config sections
        if (config.has("shop_gui")) return "shop_gui";
        if (config.has("stats_gui")) return "stats_gui";
        if (config.has("economy_gui")) return "economy_gui";
        if (config.has("kits_gui")) return "kits_gui";
        if (config.has("warps_gui")) return "warps_gui";
        if (config.has("admin_gui")) return "admin_gui";
        if (config.has("teleport_gui")) return "teleport_gui";
        if (config.has("main_config")) return "main_config";
        
        return "unknown";
    }
    
    @Override
    public void clicked(int slotIndex, int button, @Nonnull net.minecraft.world.inventory.ClickType clickType, @Nonnull Player player) {
        // Prevent normal item manipulation
        if (slotIndex < 0 || slotIndex >= getContainer().getContainerSize()) {
            return;
        }
        
        // Get the clicked item
        ItemStack clickedItem = getContainer().getItem(slotIndex);
        if (clickedItem.isEmpty()) {
            return;
        }
        
        // Handle click based on GUI configuration
        handleConfiguredClick(slotIndex, button, clickType, player, clickedItem);
    }
    
    /**
     * Handle click events based on JSON configuration
     */
    private void handleConfiguredClick(int slotIndex, int button, net.minecraft.world.inventory.ClickType clickType, 
                                     Player player, ItemStack clickedItem) {
        try {
            // Find the slot configuration for this index
            JsonObject slotConfig = findSlotConfig(slotIndex);
            if (slotConfig == null) {
                LOGGER.debug("No configuration found for slot {}", slotIndex);
                return;
            }
            
            // Get the action for this slot
            String action = slotConfig.has("action") ? slotConfig.get("action").getAsString() : "none";
            
            if ("none".equals(action)) {
                return;
            }
            
            // Execute the configured action
            executeAction(action, player, slotIndex, slotConfig);
            
            LOGGER.debug("Executed action '{}' for slot {} in GUI '{}'", action, slotIndex, guiType);
            
        } catch (Exception e) {
            LOGGER.error("Failed to handle configured click for slot {}", slotIndex, e);
        }
    }
    
    /**
     * Find slot configuration for given slot index
     */
    private JsonObject findSlotConfig(int slotIndex) {
        if (!config.has("layout") || !config.getAsJsonObject("layout").has("slots")) {
            return null;
        }
        
        JsonObject slots = config.getAsJsonObject("layout").getAsJsonObject("slots");
        
        // Check each slot configuration
        for (String slotKey : slots.keySet()) {
            if (isSlotInRange(slotIndex, slotKey)) {
                return slots.getAsJsonObject(slotKey);
            }
        }
        
        return null;
    }
    
    /**
     * Check if slot index is in the specified range
     */
    private boolean isSlotInRange(int slotIndex, String slotKey) {
        if (slotKey.contains("-")) {
            String[] parts = slotKey.split("-");
            if (parts.length == 2) {
                try {
                    int start = Integer.parseInt(parts[0]);
                    int end = Integer.parseInt(parts[1]);
                    return slotIndex >= start && slotIndex <= end;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
        } else {
            try {
                return slotIndex == Integer.parseInt(slotKey);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return false;
    }
    
    /**
     * Execute the configured action
     */
    private void executeAction(String action, Player player, int slotIndex, JsonObject slotConfig) {
        switch (action) {
            case "close_gui" -> {
                player.closeContainer();
                playActionSound(player, "close");
            }
            
            case "refresh_gui" -> {
                // Refresh GUI content
                player.closeContainer();
                // Reopen the same GUI type
                reopenGui(player);
                playActionSound(player, "refresh");
            }
            
            // Shop actions
            case "open_shop_category" -> {
                String category = slotConfig.has("category") ? 
                    slotConfig.get("category").getAsString() : "general";
                openShopCategory(player, category);
            }
            
            case "purchase_item" -> {
                String itemId = slotConfig.has("item_id") ? 
                    slotConfig.get("item_id").getAsString() : "";
                purchaseItem(player, itemId);
            }
            
            case "sell_item" -> {
                openSellInterface(player);
            }
            
            // Warp actions
            case "warp_to" -> {
                String warpName = action.contains(":") ? 
                    action.split(":")[1] : "spawn";
                executeWarp(player, warpName);
            }
            
            case "open_warp_category" -> {
                String category = slotConfig.has("category") ? 
                    slotConfig.get("category").getAsString() : "public";
                openWarpCategory(player, category);
            }
            
            // Kit actions
            case "claim_kit" -> {
                String kitName = action.contains(":") ? 
                    action.split(":")[1] : "";
                claimKit(player, kitName);
            }
            
            case "open_kit_category" -> {
                String category = slotConfig.has("category") ? 
                    slotConfig.get("category").getAsString() : "starter";
                openKitCategory(player, category);
            }
            
            // Teleport actions
            case "teleport_spawn" -> {
                teleportToSpawn(player);
            }
            
            case "teleport_home" -> {
                teleportToHome(player);
            }
            
            case "open_tpa_menu" -> {
                openTpaMenu(player);
            }
            
            // Admin actions
            case "open_player_management" -> {
                openPlayerManagement(player);
            }
            
            case "open_economy_management" -> {
                openEconomyManagement(player);
            }
            
            // Generic actions
            case "show_help" -> {
                showHelp(player, guiType);
            }
            
            case "show_info" -> {
                showInfo(player, slotConfig);
            }
            
            default -> {
                LOGGER.debug("Unknown action: {}", action);
                player.sendSystemMessage(Component.literal("§cAction not implemented: " + action));
            }
        }
    }
    
    /**
     * Reopen the current GUI type
     */
    private void reopenGui(Player player) {
        // This would integrate with ConfigurableGuiManager to reopen the same GUI
        LOGGER.debug("Reopening GUI type '{}' for player '{}'", guiType, player.getName().getString());
    }
    
    /**
     * Shop category actions
     */
    private void openShopCategory(Player player, String category) {
        player.sendSystemMessage(Component.literal("§6Opening shop category: " + category));
        // Implementation would open specific shop category
    }
    
    private void purchaseItem(Player player, String itemId) {
        player.sendSystemMessage(Component.literal("§aPurchasing item: " + itemId));
        // Implementation would handle item purchase
    }
    
    private void openSellInterface(Player player) {
        player.sendSystemMessage(Component.literal("§6Opening sell interface"));
        // Implementation would open sell interface
    }
    
    /**
     * Warp actions
     */
    private void executeWarp(Player player, String warpName) {
        player.sendSystemMessage(Component.literal("§3Warping to: " + warpName));
        // Implementation would execute actual warp
    }
    
    private void openWarpCategory(Player player, String category) {
        player.sendSystemMessage(Component.literal("§3Opening warp category: " + category));
        // Implementation would open warp category
    }
    
    /**
     * Kit actions
     */
    private void claimKit(Player player, String kitName) {
        player.sendSystemMessage(Component.literal("§6Claiming kit: " + kitName));
        // Implementation would handle kit claiming
    }
    
    private void openKitCategory(Player player, String category) {
        player.sendSystemMessage(Component.literal("§6Opening kit category: " + category));
        // Implementation would open kit category
    }
    
    /**
     * Teleport actions
     */
    private void teleportToSpawn(Player player) {
        player.sendSystemMessage(Component.literal("§aTeleporting to spawn"));
        // Implementation would teleport to spawn
    }
    
    private void teleportToHome(Player player) {
        player.sendSystemMessage(Component.literal("§aTeleporting to home"));
        // Implementation would teleport to home
    }
    
    private void openTpaMenu(Player player) {
        player.sendSystemMessage(Component.literal("§5Opening TPA menu"));
        // Implementation would open TPA menu
    }
    
    /**
     * Admin actions
     */
    private void openPlayerManagement(Player player) {
        player.sendSystemMessage(Component.literal("§4Opening player management"));
        // Implementation would open player management
    }
    
    private void openEconomyManagement(Player player) {
        player.sendSystemMessage(Component.literal("§2Opening economy management"));
        // Implementation would open economy management
    }
    
    /**
     * Utility actions
     */
    private void showHelp(Player player, String topic) {
        player.sendSystemMessage(Component.literal("§bShowing help for: " + topic));
        // Implementation would show help information
    }
    
    private void showInfo(Player player, JsonObject slotConfig) {
        player.sendSystemMessage(Component.literal("§fShowing information"));
        // Implementation would show detailed information
    }
    
    /**
     * Play action sound effect
     */
    private void playActionSound(Player player, String actionType) {
        // Implementation would play appropriate sound
        LOGGER.debug("Playing sound for action: {}", actionType);
    }
    
    @Override
    public boolean stillValid(@Nonnull Player player) {
        return super.stillValid(player);
    }
    
    /**
     * Get the GUI configuration
     */
    public JsonObject getConfig() {
        return config;
    }
    
    /**
     * Get the GUI type
     */
    public String getGuiType() {
        return guiType;
    }
    
    /**
     * Get the player using this GUI
     */
    public Player getPlayer() {
        return player;
    }
}
