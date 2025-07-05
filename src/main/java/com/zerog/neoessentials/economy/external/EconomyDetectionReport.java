package com.zerog.neoessentials.economy.external;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains detailed information about detected external economy systems.
 */
public class EconomyDetectionReport {
    
    private final Map<String, String> detectedMods; // mod ID -> version
    private final List<String> detectedClasses;
    private final long generatedAt;
    
    public EconomyDetectionReport() {
        this.detectedMods = new HashMap<>();
        this.detectedClasses = new ArrayList<>();
        this.generatedAt = System.currentTimeMillis();
    }
    
    /**
     * Adds a detected mod to the report
     */
    public void addDetectedMod(String modId, String version) {
        detectedMods.put(modId, version);
    }
    
    /**
     * Adds a detected class to the report
     */
    public void addDetectedClass(String className) {
        if (!detectedClasses.contains(className)) {
            detectedClasses.add(className);
        }
    }
    
    /**
     * Checks if any external economy was detected
     */
    public boolean hasExternalEconomy() {
        return !detectedMods.isEmpty() || !detectedClasses.isEmpty();
    }
    
    /**
     * Gets the number of detected economy systems
     */
    public int getDetectedCount() {
        return detectedMods.size() + detectedClasses.size();
    }
    
    /**
     * Gets a formatted report string
     */
    public String getFormattedReport() {
        StringBuilder report = new StringBuilder();
        report.append("Economy Detection Report (Generated: ").append(new java.util.Date(generatedAt)).append(")\n");
        
        if (!hasExternalEconomy()) {
            report.append("No external economy systems detected.\n");
            return report.toString();
        }
        
        report.append("External economy systems detected:\n");
        
        if (!detectedMods.isEmpty()) {
            report.append("\nDetected Mods:\n");
            for (Map.Entry<String, String> entry : detectedMods.entrySet()) {
                report.append("  - ").append(entry.getKey()).append(" (Version: ").append(entry.getValue()).append(")\n");
            }
        }
        
        if (!detectedClasses.isEmpty()) {
            report.append("\nDetected Classes:\n");
            for (String className : detectedClasses) {
                report.append("  - ").append(className).append("\n");
            }
        }
        
        return report.toString();
    }
    
    // Getters
    public Map<String, String> getDetectedMods() {
        return new HashMap<>(detectedMods);
    }
    
    public List<String> getDetectedClasses() {
        return new ArrayList<>(detectedClasses);
    }
    
    public long getGeneratedAt() {
        return generatedAt;
    }
    
    @Override
    public String toString() {
        return getFormattedReport();
    }
}
