package com.neoessentials.util;

import net.neoforged.fml.loading.FMLEnvironment;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side validation utilities for NeoEssentials
 * Ensures all operations are server-side safe and compatible
 */
public class ServerSideUtil {
    
    /**
     * Check if we're running on the logical server
     */
    public static boolean isServerSide() {
        return !FMLEnvironment.dist.isClient();
    }
    
    /**
     * Check if we're running on a dedicated server
     */
    public static boolean isDedicatedServer() {
        return FMLEnvironment.dist.isDedicatedServer();
    }
    
    /**
     * Validate that a player is on the server side
     */
    public static boolean isValidServerPlayer(ServerPlayer player) {
        return player != null && !player.level().isClientSide();
    }
    
    /**
     * Safely execute server-side code
     */
    public static void executeOnServer(Runnable serverCode) {
        if (isServerSide()) {
            serverCode.run();
        }
    }
    
    /**
     * Get server instance safely
     */
    public static MinecraftServer getServer(ServerPlayer player) {
        return player.getServer();
    }
    
    /**
     * Check if this environment supports server commands
     */
    public static boolean canExecuteServerCommands() {
        return isServerSide();
    }
    
    /**
     * Log server-side compatibility status
     */
    public static void logCompatibilityStatus() {
        if (isDedicatedServer()) {
            com.neoessentials.NeoEssentials.LOGGER.info("Running on dedicated server - Full functionality available");
        } else if (isServerSide()) {
            com.neoessentials.NeoEssentials.LOGGER.info("Running on integrated server - Full functionality available");
        } else {
            com.neoessentials.NeoEssentials.LOGGER.warn("Running on client side - This mod should only be on servers!");
        }
    }
}
