package com.zerog.neoessentials.items;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Helper class for managing item spawning with permission checks and blacklist validation.
 */
public class ItemSpawnHelper {
    @SuppressWarnings("unused") // Reserved for future logging features
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemSpawnHelper.class);
    
    /**
     * Check if a player can spawn a specific item.
     * Validates against blacklist and permission requirements.
     * 
     * @param player The player attempting to spawn
     * @param item The item to spawn
     * @return SpawnResult with success status and error message if failed
     */
    public static SpawnResult canSpawnItem(ServerPlayer player, Item item) {
        // Get item ID
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdString = itemId.toString();
        
        // Check blacklist first
        List<String> blacklist = ConfigManager.getItemSpawnBlacklist();
        if (blacklist.contains(itemIdString)) {
            return SpawnResult.failure("Item '" + itemIdString + "' is blacklisted and cannot be spawned");
        }
        
        // Check permission-based spawning
        if (ConfigManager.isPermissionBasedItemSpawn()) {
            UUID playerUuid = player.getUUID();
            
            // Check general item spawn permission
            if (!PermissionAPI.hasPermission(playerUuid, "neoessentials.item.spawn")) {
                return SpawnResult.failure("You don't have permission to spawn items");
            }
            
            // Check item-specific permission (format: neoessentials.item.spawn.<namespace>.<item>)
            String specificPerm = "neoessentials.item.spawn." + itemIdString.replace(":", ".");
            if (!PermissionAPI.hasPermission(playerUuid, specificPerm)) {
                // Also check wildcard for namespace (neoessentials.item.spawn.minecraft.*)
                String namespacePerm = "neoessentials.item.spawn." + itemId.getNamespace() + ".*";
                if (!PermissionAPI.hasPermission(playerUuid, namespacePerm)) {
                    return SpawnResult.failure("You don't have permission to spawn '" + itemIdString + "'");
                }
            }
        }
        
        return SpawnResult.success();
    }
    
    /**
     * Check if a player can spawn a specific item stack.
     * 
     * @param player The player attempting to spawn
     * @param stack The item stack to spawn
     * @return SpawnResult with success status and error message if failed
     */
    public static SpawnResult canSpawnItem(ServerPlayer player, ItemStack stack) {
        if (stack.isEmpty()) {
            return SpawnResult.failure("Cannot spawn empty item stack");
        }
        return canSpawnItem(player, stack.getItem());
    }
    
    /**
     * Check if an item is blacklisted.
     * 
     * @param item The item to check
     * @return true if blacklisted
     */
    public static boolean isBlacklisted(Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdString = itemId.toString();
        List<String> blacklist = ConfigManager.getItemSpawnBlacklist();
        return blacklist.contains(itemIdString);
    }
    
    /**
     * Check if an item is blacklisted by ID string.
     * 
     * @param itemId The item ID (e.g., "minecraft:bedrock")
     * @return true if blacklisted
     */
    public static boolean isBlacklisted(String itemId) {
        List<String> blacklist = ConfigManager.getItemSpawnBlacklist();
        return blacklist.contains(itemId);
    }
    
    /**
     * Result of a spawn permission check.
     */
    public static class SpawnResult {
        private final boolean success;
        private final String errorMessage;
        
        private SpawnResult(boolean success, String errorMessage) {
            this.success = success;
            this.errorMessage = errorMessage;
        }
        
        public static SpawnResult success() {
            return new SpawnResult(true, null);
        }
        
        public static SpawnResult failure(String message) {
            return new SpawnResult(false, message);
        }
        
        public boolean isSuccess() {
            return success;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
