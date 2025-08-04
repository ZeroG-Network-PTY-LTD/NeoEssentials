package com.zerog.neoessentials.events;

import com.zerog.neoessentials.managers.*;
import com.zerog.neoessentials.storage.PlayerDataManager;
import com.zerog.neoessentials.storage.StorageManager;
import com.zerog.neoessentials.util.LocationUtil;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.utils.PlaceholderManager;
import com.zerog.neoessentials.features.TablistScoreboardManager;
import com.zerog.neoessentials.commands.essentials.GodCommand;
import com.zerog.neoessentials.commands.essentials.VanishCommand;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Central event handler for NeoEssentials
 * Manages player events, server events, and feature integration
 * 
 * @author ZeroG
 * @since 2.0.0
 */
@EventBusSubscriber(modid = "neoessentials")
public class NeoEssentialsEventHandler {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NeoEssentialsEventHandler.class);
    
    /**
     * Handle server startup - initialize all systems
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("NeoEssentials server startup - initializing systems...");
        
        try {
            // Initialize storage system
            StorageManager.getInstance();
            
            // Initialize all managers
            PlayerDataManager.getInstance();
            EconomyManager.getInstance();
            HomeManager.getInstance();
            WarpManager.getInstance();
            KitManager.getInstance();
            MessagingManager.getInstance();
            SpawnManager.getInstance();
            ModerationManager.getInstance();
            
            // Initialize placeholder system
            PlaceholderManager.getInstance();
            
            // Initialize tablist and scoreboard system
            TablistScoreboardManager.getInstance();
            
            LOGGER.info("NeoEssentials successfully initialized all systems");
            
        } catch (Exception e) {
            LOGGER.error("Failed to initialize NeoEssentials systems", e);
        }
    }
    
    /**
     * Handle server shutdown - cleanup and save data
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        LOGGER.info("NeoEssentials server shutdown - saving data...");
        
        try {
            // Create backup before shutdown
            StorageManager storageManager = StorageManager.getInstance();
            storageManager.createBackup().join(); // Wait for backup to complete
            
            // Shutdown storage manager
            storageManager.shutdown();
            
            LOGGER.info("NeoEssentials successfully saved all data");
            
        } catch (Exception e) {
            LOGGER.error("Error during NeoEssentials shutdown", e);
        }
    }
    
    /**
     * Handle player joining the server
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        LOGGER.info("Player {} joined the server", player.getName().getString());
        
        try {
            // Load player data
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            playerDataManager.loadPlayerData(player.getUUID());
            
            // Update last seen
            PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
            playerData.lastSeen = System.currentTimeMillis();
            
            // Check for pending mail (would be implemented when needed)
            // MessagingManager messagingManager = MessagingManager.getInstance();
            // messagingManager.checkPendingMail(player);
            
            // Welcome message with placeholders
            PlaceholderManager placeholderManager = PlaceholderManager.getInstance();
            String welcomeMessage = placeholderManager.processPlaceholders(player, 
                "§6Welcome back, %player%! §eBalance: %balance% | Homes: %homes_count%");
            MessageUtil.sendMessage(player, welcomeMessage);
            
        } catch (Exception e) {
            LOGGER.error("Error handling player join for {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Handle player leaving the server
     */
    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        LOGGER.info("Player {} left the server", player.getName().getString());
        
        try {
            // Clean up god mode status
            GodCommand.removePlayer(player.getUUID());
            
            // Clean up vanish status if exists
            VanishCommand.removePlayer(player.getUUID());
            
            // Update last seen time
            PlayerDataManager playerDataManager = PlayerDataManager.getInstance();
            PlayerDataManager.PlayerData playerData = playerDataManager.getPlayerData(player.getUUID());
            playerData.lastSeen = System.currentTimeMillis();
            
            // Save player data
            playerDataManager.savePlayerData(player.getUUID());
            
        } catch (Exception e) {
            LOGGER.error("Error handling player leave for {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Handle player death
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        try {
            // Record death location for potential /back command
            LocationUtil.Location deathLocation = LocationUtil.fromServerPlayer(player);
            
            LOGGER.info("Player {} died at {}", player.getName().getString(), 
                LocationUtil.formatLocation(deathLocation));
            
        } catch (Exception e) {
            LOGGER.error("Error handling player death for {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Handle player respawn
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        try {
            // Check if player should respawn at a custom spawn
            SpawnManager spawnManager = SpawnManager.getInstance();
            LocationUtil.Location spawnLocation = spawnManager.getSpawnLocation(player.level().dimension().location().getPath());
            
            if (spawnLocation != null) {
                LOGGER.info("Player {} respawning at custom spawn", player.getName().getString());
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling player respawn for {}", player.getName().getString(), e);
        }
    }
    
    /**
     * Handle block break events (for protection checks)
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        
        try {
            // Check if player is jailed (jailed players can't break blocks)
            ModerationManager moderationManager = ModerationManager.getInstance();
            if (moderationManager.isPlayerJailed(player.getUUID())) {
                event.setCanceled(true);
                MessageUtil.sendMessage(player, "§cYou cannot break blocks while jailed!");
                return;
            }
            
            // Additional protection checks could go here
            // - Home protection
            // - Warp protection
            // - Spawn protection
            
        } catch (Exception e) {
            LOGGER.error("Error handling block break event", e);
        }
    }
    
    /**
     * Handle block place events (for protection checks)
     */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        
        try {
            // Check if player is jailed (jailed players can't place blocks)
            ModerationManager moderationManager = ModerationManager.getInstance();
            if (moderationManager.isPlayerJailed(player.getUUID())) {
                event.setCanceled(true);
                MessageUtil.sendMessage(player, "§cYou cannot place blocks while jailed!");
                return;
            }
            
        } catch (Exception e) {
            LOGGER.error("Error handling block place event", e);
        }
    }
}
