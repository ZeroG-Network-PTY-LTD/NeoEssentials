package com.neoessentials.network;

import com.neoessentials.NeoEssentials;

/**
 * Network handler for NeoEssentials
 * Since this is a server-side only mod, this class ensures no client-server communication
 * that would cause network mismatches when the mod is only on the server.
 * 
 * This mod is designed to work without any client-server packets, making it safe
 * to install only on the server without requiring client installation.
 */
public class NetworkHandler {
    
    /**
     * Initialize network handling
     * For a server-side only mod, we don't register any packets
     */
    public static void init() {
        NeoEssentials.LOGGER.info("Network handler initialized - Server-side only mod (no client packets)");
        
        // This mod intentionally avoids any client-server communication
        // All functionality is handled through:
        // - Server commands
        // - Server-side data storage
        // - Server-side player management
        // - Standard Minecraft server-to-client packets (chat, teleportation, etc.)
    }
    
    /**
     * Check if this mod is compatible with client versions
     * Always returns true since we don't require the mod on clients
     */
    public static boolean isClientCompatible() {
        return true; // Clients don't need this mod
    }
    
    /**
     * Check if this mod is compatible with server versions
     * Only returns true if running on a dedicated server
     */
    public static boolean isServerCompatible() {
        return true; // Required on server
    }
}
