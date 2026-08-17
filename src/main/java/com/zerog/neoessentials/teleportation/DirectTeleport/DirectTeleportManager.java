package com.zerog.neoessentials.teleportation.DirectTeleport;

import com.zerog.neoessentials.teleportation.TeleportLocation;
import com.google.gson.JsonObject;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import com.zerog.neoessentials.teleportation.TeleportUtil;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/**
 * Manager for direct teleportation commands (admin teleports)
 */
public class DirectTeleportManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectTeleportManager.class);
    
    // Singleton pattern
    private static class SingletonHolder {
        private static final DirectTeleportManager INSTANCE = new DirectTeleportManager();
    }
    
    public static DirectTeleportManager getInstance() {
        return SingletonHolder.INSTANCE;
    }
    
    // Configuration
    private int teleportDelay = 0; // No delay for admin teleports
    private boolean bypassSafetyChecks = true; // Admin teleports can bypass safety

    private DirectTeleportManager() {
        // Load config values
        try {
            com.zerog.neoessentials.config.ConfigManager configManager = com.zerog.neoessentials.config.ConfigManager.getInstance();
            boolean bypass = true;
            if (configManager != null) {
                JsonObject config = configManager.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                if (config.has("teleportation")) {
                    JsonObject tp = config.getAsJsonObject("teleportation");
                    if (tp.has("generalSettings")) {
                        JsonObject generalSettings = tp.getAsJsonObject("generalSettings");
                        if (generalSettings.has("enableTeleportSafety")) {
                            // If safety is enabled, do NOT bypass safety checks
                            bypass = !generalSettings.get("enableTeleportSafety").getAsBoolean();
                        }
                    }
                }
            }
            this.bypassSafetyChecks = bypass;
        } catch (Exception e) {
            LOGGER.warn("Failed to load direct teleport safety config, defaulting to bypass: {}", e.getMessage());
        }
        // Private constructor for singleton
    }
    
    /**
     * Teleport a player to another player (/tp <player> <target>)
     */
    public CompletableFuture<TeleportUtil.TeleportResult> teleportPlayerToPlayer(ServerPlayer executor, 
                                                                                ServerPlayer player, 
                                                                                ServerPlayer target) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportPlayerToPlayer request: executor={} player={} target={}",
            executor.getName().getString(), player.getName().getString(), target.getName().getString());

        // Save current location for /back command (for the player being teleported)
        com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);

        TeleportLocation targetLocation = new TeleportLocation(target);
        
        return TeleportUtil.teleportPlayer(player, targetLocation, teleportDelay * 20, !bypassSafetyChecks)
            .thenApply(result -> {
                if (result.isSuccess()) {
                    // Notify the executor (admin)
                    if (!executor.getUUID().equals(player.getUUID())) {
                        executor.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.teleported_player", 
                                                                       player.getName().getString(), 
                                                                       target.getName().getString()));
                    }
                    
                    // Notify the teleported player
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.teleported_to", 
                                                                 target.getName().getString()));
                    
                    // Notify the target (optional)
                    if (!target.getUUID().equals(executor.getUUID())) {
                        target.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.admin.player_teleported_to_you", 
                                                                  player.getName().getString()));
                    }
                    
                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Admin {} teleported {} to {}", 
                               executor.getName().getString(), 
                               player.getName().getString(), 
                               target.getName().getString());
                } else {
                    executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.failed", 
                                                                 player.getName().getString(), 
                                                                 result.getMessage()));
                    
                    LOGGER.warn("Failed admin teleport by {}: {} to {} - {}", 
                               executor.getName().getString(), 
                               player.getName().getString(), 
                               target.getName().getString(), 
                               result.getMessage());
                }
                
                return result;
            });
    }
    
    /**
     * Teleport a player to coordinates (/tp <player> <x> <y> <z>)
     */
    public CompletableFuture<TeleportUtil.TeleportResult> teleportPlayerToCoordinates(ServerPlayer executor,
                                                                                     ServerPlayer player,
                                                                                     double x, double y, double z) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportPlayerToCoordinates request: executor={} player={} target=({},{},{})",
            executor.getName().getString(), player.getName().getString(), x, y, z);

        // Save current location for /back command (for the player being teleported)
        com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);

        TeleportLocation targetLocation = new TeleportLocation(
            com.zerog.neoessentials.util.LevelCompat.of(player).dimension().location().toString(), x, y, z, 0f, 0f, 
            executor.getName().getString());
        
        return TeleportUtil.teleportPlayer(player, targetLocation, teleportDelay * 20, !bypassSafetyChecks)
            .thenApply(result -> {
                if (result.isSuccess()) {
                    // Notify the executor (admin)
                    if (!executor.getUUID().equals(player.getUUID())) {
                        executor.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.teleported_player_coords", 
                                                                       player.getName().getString(), x, y, z));
                    }
                    
                    // Notify the teleported player
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.teleported_to_coords", x, y, z));
                    
                    NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Admin {} teleported {} to coordinates {}, {}, {}", 
                               executor.getName().getString(), 
                               player.getName().getString(), 
                               x, y, z);
                } else {
                    executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.failed_coords", 
                                                                 player.getName().getString(), 
                                                                 result.getMessage()));
                    
                    LOGGER.warn("Failed admin coordinate teleport by {}: {} to {}, {}, {} - {}", 
                               executor.getName().getString(), 
                               player.getName().getString(), 
                               x, y, z, 
                               result.getMessage());
                }
                
                return result;
            });
    }
    
    /**
     * Teleport a player to the executor (/tphere <player>)
     */
    public CompletableFuture<TeleportUtil.TeleportResult> teleportPlayerHere(ServerPlayer executor, 
                                                                            ServerPlayer player) {
        return teleportPlayerToPlayer(executor, player, executor);
    }
    
    /**
     * Teleport all players to a location (/tpall)
     */
    public void teleportAllPlayers(ServerPlayer executor, TeleportLocation targetLocation) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportAllPlayers request: executor={} target={}",
            executor.getName().getString(), targetLocation.getLocationString());
        Collection<ServerPlayer> players = executor.getServer().getPlayerList().getPlayers();
        int totalPlayers = players.size();
        int excludingSelf = executor != null ? totalPlayers - 1 : totalPlayers;
        
        if (excludingSelf == 0) {
            executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.tpall.no_players"));
            return;
        }
        
        executor.sendSystemMessage(MessageUtil.info("commands.neoessentials.teleport.admin.tpall.starting", excludingSelf));
        
        int[] successCount = {0};
        int[] failureCount = {0};
        
        @SuppressWarnings("unchecked")
        CompletableFuture<Void>[] futures = players.stream()
            .filter(player -> !player.getUUID().equals(executor.getUUID())) // Exclude executor
            .map(player -> {
                // Save current location for /back command
                com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(player);

                return TeleportUtil.teleportPlayer(player, targetLocation, teleportDelay * 20, !bypassSafetyChecks)
                    .thenAccept(result -> {
                        if (result.isSuccess()) {
                            successCount[0]++;
                            player.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.tpall.teleported"));
                        } else {
                            failureCount[0]++;
                            LOGGER.warn("Failed to teleport {} during tpall: {}", 
                                       player.getName().getString(), result.getMessage());
                        }
                    });
            })
            .toArray(CompletableFuture[]::new);
        
        // Wait for all teleports to complete
        CompletableFuture.allOf(futures).thenRun(() -> {
            executor.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.tpall.completed", 
                                                           successCount[0], failureCount[0]));
            
            NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Admin {} completed tpall: {} successful, {} failed", 
                       executor.getName().getString(), successCount[0], failureCount[0]);
        });
    }
    
    /**
     * Teleport all players to the executor (/tpall)
     */
    public void teleportAllPlayersHere(ServerPlayer executor) {
        TeleportLocation executorLocation = new TeleportLocation(executor);
        teleportAllPlayers(executor, executorLocation);
    }
    
    /**
     * Teleport all players to coordinates (/tpall <x> <y> <z>)
     */
    public void teleportAllPlayersToCoordinates(ServerPlayer executor, double x, double y, double z) {
        TeleportLocation targetLocation = new TeleportLocation(
            com.zerog.neoessentials.util.LevelCompat.of(executor).dimension().location().toString(), x, y, z, 0f, 0f, 
            executor.getName().getString());
        teleportAllPlayers(executor, targetLocation);
    }
    
    /**
     * Teleport all players to another player (/tpall <target>)
     */
    public void teleportAllPlayersToTarget(ServerPlayer executor, ServerPlayer target) {
        TeleportLocation targetLocation = new TeleportLocation(target);
        teleportAllPlayers(executor, targetLocation);
    }
    
    // Configuration getters/setters
    public int getTeleportDelay() {
        return teleportDelay;
    }
    
    public void setTeleportDelay(int delay) {
        this.teleportDelay = Math.max(0, delay);
    }
    
    public boolean isBypassSafetyChecks() {
        return bypassSafetyChecks;
    }
    
    public void setBypassSafetyChecks(boolean bypass) {
        this.bypassSafetyChecks = bypass;
    }

    /**
     * Teleport to offline player's last location (/tpo <offline_player>)
     */
    public boolean teleportToOfflinePlayer(ServerPlayer executor, String playerName) {
        NeoLog.debug(LOGGER, LogCategory.TELEPORTATION, "teleportToOfflinePlayer request: executor={} target={}",
            executor.getName().getString(), playerName);
        try {
            // Get server for UUID lookup
            net.minecraft.server.MinecraftServer server = executor.getServer();
            
            // Try to get UUID from cache (offline players)
            com.mojang.authlib.GameProfile profile = server.getProfileCache().get(playerName).orElse(null);
            if (profile == null) {
                executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.offline_player_not_found", playerName));
                return false;
            }
            
            // Get player data from NeoEssentialsManager
            com.zerog.neoessentials.NeoEssentialsManager manager = com.zerog.neoessentials.NeoEssentialsManager.getInstance();
            com.zerog.neoessentials.NeoEssentialsManager.PlayerData playerData = manager.getPlayerData(profile.getId());
            
            String lastLocationString = playerData.getLastLocation();
            if (lastLocationString == null || lastLocationString.isEmpty()) {
                executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.offline_player_not_found", playerName));
                return false;
            }
            
            // Parse the location string and create TeleportLocation
            TeleportLocation offlineLocation = TeleportLocation.fromLocationString(lastLocationString);
            if (offlineLocation == null) {
                executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.offline_failed", lastLocationString));
                return false;
            }
            
            // Save current location for /back command
            com.zerog.neoessentials.teleportation.Misc.MiscTeleportManager.getInstance().saveBackLocation(executor);

            // Perform teleportation
            TeleportUtil.teleportPlayer(executor, offlineLocation, teleportDelay * 20, !bypassSafetyChecks)
                .thenAccept(result -> {
                    if (result.isSuccess()) {
                        executor.sendSystemMessage(MessageUtil.success("commands.neoessentials.teleport.admin.offline_teleported", playerName));
                        NeoLog.info(LOGGER, LogCategory.TELEPORTATION, "Admin {} teleported to offline player {}'s location", 
                                   executor.getName().getString(), playerName);
                    } else {
                        executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.offline_failed", result.getMessage()));
                        LOGGER.warn("Failed to teleport {} to offline player {}: {}", 
                                   executor.getName().getString(), playerName, result.getMessage());
                    }
                });
            
            return true;
            
        } catch (Exception e) {
            executor.sendSystemMessage(MessageUtil.error("commands.neoessentials.teleport.admin.offline_failed", e.getMessage()));
            LOGGER.error("Error during offline teleport to {}: {}", playerName, e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Get statistics
     */
    public String getStatistics() {
        return String.format("DirectTeleport Statistics: delay=%ds, bypassSafety=%s", 
                           teleportDelay, bypassSafetyChecks);
    }
}

