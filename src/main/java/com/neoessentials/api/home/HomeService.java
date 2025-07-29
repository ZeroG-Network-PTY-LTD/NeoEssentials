package com.neoessentials.api.home;

import com.neoessentials.api.home.data.HomeData;
import com.neoessentials.api.home.data.HomeDataManager;
import com.neoessentials.api.home.data.impl.JsonHomeDataManager;
import com.neoessentials.config.EssentialsConfig;
import com.neoessentials.language.LanguageManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Home management service
 * Similar to EssentialsX home manager
 */
public class HomeService {
    private static final Pattern VALID_HOME_NAME = Pattern.compile("^[a-zA-Z0-9_]{1,16}$");
    
    private final HomeDataManager dataManager;
    private final EssentialsConfig config;
    private final LanguageManager languageManager;
    
    public HomeService(Path dataDirectory, EssentialsConfig config, LanguageManager languageManager) {
        this.dataManager = new JsonHomeDataManager(dataDirectory);
        this.config = config;
        this.languageManager = languageManager;
    }
    
    /**
     * Set a home for a player
     */
    public CompletableFuture<SetHomeResult> setHome(ServerPlayer player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate home name
            if (!isValidHomeName(homeName)) {
                return SetHomeResult.INVALID_NAME;
            }
            
            UUID playerUUID = player.getUUID();
            
            // Check home count limit
            try {
                int currentCount = dataManager.getHomeCount(playerUUID).get();
                int maxHomes = getMaxHomes(player);
                
                boolean homeExists = dataManager.hasHome(playerUUID, homeName).get();
                
                if (!homeExists && currentCount >= maxHomes) {
                    return SetHomeResult.MAX_HOMES_REACHED;
                }
                
                // Create home data
                HomeData home = new HomeData(
                    homeName,
                    player.level().dimension(),
                    player.position().x,
                    player.position().y,
                    player.position().z,
                    player.getYRot(),
                    player.getXRot(),
                    System.currentTimeMillis()
                );
                
                // Save home
                dataManager.saveHome(playerUUID, homeName, home).get();
                return SetHomeResult.SUCCESS;
                
            } catch (Exception e) {
                return SetHomeResult.ERROR;
            }
        });
    }
    
    /**
     * Teleport player to a home
     */
    public CompletableFuture<TeleportHomeResult> teleportToHome(ServerPlayer player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            // Validate server-side player
            if (player.level().isClientSide()) {
                return TeleportHomeResult.ERROR;
            }
            
            try {
                UUID playerUUID = player.getUUID();
                HomeData home = dataManager.loadHome(playerUUID, homeName).get();
                
                if (home == null) {
                    return TeleportHomeResult.NOT_FOUND;
                }
                
                // Get the target level
                ServerLevel targetLevel = player.getServer().getLevel(home.getDimension());
                if (targetLevel == null) {
                    return TeleportHomeResult.DIMENSION_NOT_FOUND;
                }
                
                // Schedule teleportation on main thread (server-side safe)
                player.getServer().execute(() -> {
                    player.teleportTo(targetLevel, home.getX(), home.getY(), home.getZ(), 
                                    home.getYaw(), home.getPitch());
                });
                
                return TeleportHomeResult.SUCCESS;
                
            } catch (Exception e) {
                return TeleportHomeResult.ERROR;
            }
        });
    }
    
    /**
     * Delete a home
     */
    public CompletableFuture<DeleteHomeResult> deleteHome(ServerPlayer player, String homeName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                UUID playerUUID = player.getUUID();
                boolean deleted = dataManager.deleteHome(playerUUID, homeName).get();
                
                return deleted ? DeleteHomeResult.SUCCESS : DeleteHomeResult.NOT_FOUND;
                
            } catch (Exception e) {
                return DeleteHomeResult.ERROR;
            }
        });
    }
    
    /**
     * Get list of player's homes
     */
    public CompletableFuture<List<String>> getPlayerHomes(ServerPlayer player) {
        return dataManager.getHomeNames(player.getUUID());
    }
    
    /**
     * Get home count for player
     */
    public CompletableFuture<Integer> getHomeCount(ServerPlayer player) {
        return dataManager.getHomeCount(player.getUUID());
    }
    
    /**
     * Check if home name is valid
     */
    public boolean isValidHomeName(String name) {
        return name != null && VALID_HOME_NAME.matcher(name).matches();
    }
    
    /**
     * Get maximum number of homes for a player
     */
    public int getMaxHomes(ServerPlayer player) {
        // TODO: Implement permission-based home limits
        return config.getTeleportConfig().getMaxHomes();
    }
    
    /**
     * Send appropriate message to player based on result
     */
    public void sendSetHomeMessage(ServerPlayer player, SetHomeResult result, String homeName) {
        Component message;
        
        switch (result) {
            case SUCCESS:
                if ("home".equals(homeName)) {
                    message = languageManager.getMessage(player, "home.set");
                } else {
                    message = languageManager.getMessage(player, "home.set_named", homeName);
                }
                break;
            case INVALID_NAME:
                message = languageManager.getMessage(player, "home.invalid_name");
                break;
            case MAX_HOMES_REACHED:
                message = languageManager.getMessage(player, "home.max_homes_reached", 
                                                   String.valueOf(getMaxHomes(player)));
                break;
            default:
                message = languageManager.getMessage(player, "error.command_failed");
                break;
        }
        
        player.sendSystemMessage(message);
    }
    
    public void sendTeleportMessage(ServerPlayer player, TeleportHomeResult result, String homeName) {
        Component message;
        
        switch (result) {
            case SUCCESS:
                if ("home".equals(homeName)) {
                    message = languageManager.getMessage(player, "home.teleporting");
                } else {
                    message = languageManager.getMessage(player, "home.teleporting_named", homeName);
                }
                break;
            case NOT_FOUND:
                message = languageManager.getMessage(player, "home.not_found", homeName);
                break;
            case DIMENSION_NOT_FOUND:
                message = languageManager.getMessage(player, "error.command_failed");
                break;
            default:
                message = languageManager.getMessage(player, "error.command_failed");
                break;
        }
        
        player.sendSystemMessage(message);
    }
    
    public void sendDeleteMessage(ServerPlayer player, DeleteHomeResult result, String homeName) {
        Component message;
        
        switch (result) {
            case SUCCESS:
                message = languageManager.getMessage(player, "home.deleted", homeName);
                break;
            case NOT_FOUND:
                message = languageManager.getMessage(player, "home.not_found", homeName);
                break;
            default:
                message = languageManager.getMessage(player, "error.command_failed");
                break;
        }
        
        player.sendSystemMessage(message);
    }
    
    /**
     * Clear cache for player (on logout)
     */
    public void clearPlayerCache(UUID playerUUID) {
        if (dataManager instanceof JsonHomeDataManager) {
            ((JsonHomeDataManager) dataManager).clearPlayerCache(playerUUID);
        }
    }
    
    /**
     * Save all cached data
     */
    public CompletableFuture<Void> saveAll() {
        if (dataManager instanceof JsonHomeDataManager) {
            return ((JsonHomeDataManager) dataManager).saveAll();
        }
        return CompletableFuture.completedFuture(null);
    }
    
    // Result enums
    public enum SetHomeResult {
        SUCCESS, INVALID_NAME, MAX_HOMES_REACHED, ERROR
    }
    
    public enum TeleportHomeResult {
        SUCCESS, NOT_FOUND, DIMENSION_NOT_FOUND, ERROR
    }
    
    public enum DeleteHomeResult {
        SUCCESS, NOT_FOUND, ERROR
    }
}
