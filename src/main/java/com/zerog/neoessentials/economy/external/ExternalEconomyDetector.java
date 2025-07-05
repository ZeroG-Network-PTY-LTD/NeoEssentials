package com.zerog.neoessentials.economy.external;

import com.zerog.neoessentials.NeoEssentials;
import net.neoforged.fml.ModList;

import java.util.Arrays;
import java.util.List;

/**
 * Detects external economy mods to prevent conflicts.
 * Automatically disables internal economy when external economy is found.
 */
public class ExternalEconomyDetector {
    
    private static final String LOGGER_PREFIX = "[EconomyDetector] ";
    
    // Known economy mod IDs to detect
    private static final List<String> KNOWN_ECONOMY_MODS = Arrays.asList(
            "economyapi",
            "vault",
            "treasury",
            "essentials",
            "simpleconomy",
            "craftconomy",
            "bosseconomy",
            "grandeconomy",
            "lighteconomy",
            "ultimateeconomy",
            "quickshop",
            "shopkeepers",
            "adminshop",
            "chestsign",
            "dynmap",
            "towny",
            "factions",
            "griefprevention"
    );
    
    // Known economy classes to detect (for runtime detection)
    private static final List<String> KNOWN_ECONOMY_CLASSES = Arrays.asList(
            "net.milkbowl.vault.economy.Economy",
            "me.lokka30.treasury.api.economy.EconomyProvider",
            "com.earth2me.essentials.api.Economy",
            "org.bukkit.plugin.RegisteredServiceProvider"
    );
    
    private volatile boolean lastDetectionResult = false;
    
    /**
     * Detects if an external economy mod is present
     */
    public boolean detectExternalEconomy() {
        try {
            // Check for mod-based detection first
            if (detectEconomyMods()) {
                lastDetectionResult = true;
                return true;
            }
            
            // Check for class-based detection
            if (detectEconomyClasses()) {
                lastDetectionResult = true;
                return true;
            }
            
            lastDetectionResult = false;
            return false;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn(LOGGER_PREFIX + "Error during economy detection", e);
            return lastDetectionResult; // Return last known state on error
        }
    }
    
    /**
     * Detects economy mods by checking the mod list
     */
    private boolean detectEconomyMods() {
        try {
            ModList modList = ModList.get();
            
            for (String modId : KNOWN_ECONOMY_MODS) {
                if (modList.isLoaded(modId)) {
                    NeoEssentials.LOGGER.info(LOGGER_PREFIX + "Detected economy mod: " + modId);
                    return true;
                }
            }
            
            return false;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.debug(LOGGER_PREFIX + "Error checking mod list for economy mods", e);
            return false;
        }
    }
    
    /**
     * Detects economy systems by checking for known classes
     */
    private boolean detectEconomyClasses() {
        for (String className : KNOWN_ECONOMY_CLASSES) {
            try {
                Class.forName(className);
                NeoEssentials.LOGGER.info(LOGGER_PREFIX + "Detected economy class: " + className);
                return true;
            } catch (ClassNotFoundException ignored) {
                // Class not found, continue checking
            } catch (Exception e) {
                NeoEssentials.LOGGER.debug(LOGGER_PREFIX + "Error checking class: " + className, e);
            }
        }
        
        return false;
    }
    
    /**
     * Gets a detailed report of detected economy systems
     */
    public EconomyDetectionReport getDetectionReport() {
        EconomyDetectionReport report = new EconomyDetectionReport();
        
        // Check each mod individually
        try {
            ModList modList = ModList.get();
            
            for (String modId : KNOWN_ECONOMY_MODS) {
                if (modList.isLoaded(modId)) {
                    String version = modList.getModContainerById(modId)
                            .map(container -> container.getModInfo().getVersion().toString())
                            .orElse("Unknown");
                    report.addDetectedMod(modId, version);
                }
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.debug(LOGGER_PREFIX + "Error generating detection report", e);
        }
        
        // Check each class individually
        for (String className : KNOWN_ECONOMY_CLASSES) {
            try {
                Class.forName(className);
                report.addDetectedClass(className);
            } catch (ClassNotFoundException ignored) {
                // Expected when class is not present
            } catch (Exception e) {
                NeoEssentials.LOGGER.debug(LOGGER_PREFIX + "Error checking class for report: " + className, e);
            }
        }
        
        return report;
    }
    
    /**
     * Checks if a specific mod is detected
     */
    public boolean isModDetected(String modId) {
        try {
            return ModList.get().isLoaded(modId);
        } catch (Exception e) {
            NeoEssentials.LOGGER.debug(LOGGER_PREFIX + "Error checking specific mod: " + modId, e);
            return false;
        }
    }
    
    /**
     * Gets the last detection result without performing a new check
     */
    public boolean getLastDetectionResult() {
        return lastDetectionResult;
    }
}
