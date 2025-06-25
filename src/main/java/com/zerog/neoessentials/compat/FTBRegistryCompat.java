package com.zerog.neoessentials.compat;

import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.HashSet;
import java.util.Set;

/**
 * Compatibility class to help with Registry synchronization issues with FTB Library
 * This helps identify and log known problematic registry entries that can cause
 * "unknown registry key" errors when connecting to servers with different mod versions
 */
public class FTBRegistryCompat {
    
    private static final Set<String> knownProblematicEntries = new HashSet<>();
    private static final String FTB_LIBRARY_MOD_ID = "ftblibrary";
    private static final String FTB_RANKS_MOD_ID = "ftbranks";
    private static final String ARCHITECTURY_MOD_ID = "architectury";
    
    static {
        // Known registry entries from FTB Library that can cause issues
        knownProblematicEntries.add("ftblibrary:icon_item");
        knownProblematicEntries.add("ftblibrary:fluid_container");
        knownProblematicEntries.add("ftblibrary:icon");
        knownProblematicEntries.add("ftblibrary:config_group");
        // Add more as they are discovered
    }
    
    /**
     * Register this compatibility class to the event bus
     */
    public static void init() {
        NeoEssentials.LOGGER.info("Initializing FTB compatibility monitoring system");
        
        if (ModList.get().isLoaded(FTB_LIBRARY_MOD_ID)) {
            String ftbLibraryVersion = getModVersion(FTB_LIBRARY_MOD_ID);
            NeoEssentials.LOGGER.info("FTB Library detected (version: {}) - registering compatibility handlers for custom registries", ftbLibraryVersion);
            
            if (ModList.get().isLoaded(FTB_RANKS_MOD_ID)) {
                String ftbRanksVersion = getModVersion(FTB_RANKS_MOD_ID);
                NeoEssentials.LOGGER.info("FTB Ranks detected (version: {})", ftbRanksVersion);
            }
            
            if (ModList.get().isLoaded(ARCHITECTURY_MOD_ID)) {
                String architecturyVersion = getModVersion(ARCHITECTURY_MOD_ID);
                NeoEssentials.LOGGER.info("Architectury detected (version: {})", architecturyVersion);
            }
            
            // Register this class to listen for events
            NeoForge.EVENT_BUS.register(FTBRegistryCompat.class);
        } else {
            NeoEssentials.LOGGER.debug("FTB Library not detected - skipping registry compatibility handling");
        }
    }
    
    /**
     * Get the version of a mod from the ModList
     * 
     * @param modId The mod ID to check
     * @return The version string, or "unknown" if not found
     */
    private static String getModVersion(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }
    
    /**
     * Log known problematic registry entries when server starts
     * This helps server admins identify potential issues with FTB Library
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!ModList.get().isLoaded(FTB_LIBRARY_MOD_ID)) {
            return;
        }
        
        NeoEssentials.LOGGER.info("FTB Library integration active - monitoring for registry sync issues");
        NeoEssentials.LOGGER.info("Known potentially problematic registry entries to watch for:");
        for (String entry : knownProblematicEntries) {
            NeoEssentials.LOGGER.info("  - {}", entry);
        }
        
        // Add a notice about how to resolve registry issues
        NeoEssentials.LOGGER.info("If you encounter 'unknown registry key' errors when clients connect,");
        NeoEssentials.LOGGER.info("make sure all clients have the exact same version of:");
        NeoEssentials.LOGGER.info("  - FTB Library (server: {})", getModVersion(FTB_LIBRARY_MOD_ID));
        if (ModList.get().isLoaded(FTB_RANKS_MOD_ID)) {
            NeoEssentials.LOGGER.info("  - FTB Ranks (server: {})", getModVersion(FTB_RANKS_MOD_ID));
        }
        if (ModList.get().isLoaded(ARCHITECTURY_MOD_ID)) {
            NeoEssentials.LOGGER.info("  - Architectury (server: {})", getModVersion(ARCHITECTURY_MOD_ID));
        }
        NeoEssentials.LOGGER.info("Version mismatches are the most common cause of registry sync errors.");
    }
}
