package com.zerog.neoessentials.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Centralized registry for tracking and managing all NeoEssentials system managers.
 * Provides diagnostic capabilities to verify proper initialization and track manager lifecycle.
 * 
 * <p>This registry helps ensure all critical systems are properly initialized and provides
 * visibility into the mod's internal state for debugging and monitoring purposes.</p>
 * 
 * @author ZeroG Network
 * @version 1.0.0
 * @since Build #496
 */
public class ManagerRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManagerRegistry.class);
    private static volatile ManagerRegistry instance;
    
    private final Map<String, ManagerInfo> managers = new ConcurrentHashMap<>();
    private final Map<String, Long> initializationTimes = new ConcurrentHashMap<>();
    
    private ManagerRegistry() {
        LOGGER.debug("ManagerRegistry initialized");
    }
    
    /**
     * Get the singleton instance of ManagerRegistry.
     * 
     * @return The ManagerRegistry instance
     */
    public static ManagerRegistry getInstance() {
        if (instance == null) {
            synchronized (ManagerRegistry.class) {
                if (instance == null) {
                    instance = new ManagerRegistry();
                }
            }
        }
        return instance;
    }
    
    /**
     * Register a manager with the registry.
     * 
     * @param name The unique name/identifier for the manager
     * @param category The category this manager belongs to (e.g., "economy", "chat", "moderation")
     * @param managerClass The class of the manager being registered
     * @param initSupplier Optional supplier that returns the manager instance (for lazy init verification)
     */
    public void registerManager(String name, String category, Class<?> managerClass, Supplier<?> initSupplier) {
        if (name == null || name.trim().isEmpty()) {
            LOGGER.warn("Attempted to register manager with null or empty name");
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        try {
            // Verify the manager can be initialized
            Object instance = null;
            if (initSupplier != null) {
                instance = initSupplier.get();
            }
            
            ManagerInfo info = new ManagerInfo(
                name,
                category,
                managerClass,
                instance != null,
                System.currentTimeMillis()
            );
            
            managers.put(name, info);
            long duration = System.currentTimeMillis() - startTime;
            initializationTimes.put(name, duration);
            
            LOGGER.debug("Registered manager: {} ({}) [{}ms]", name, category, duration);
            
        } catch (Exception e) {
            LOGGER.error("Failed to register manager '{}': {}", name, e.getMessage(), e);
            
            // Register as failed
            ManagerInfo info = new ManagerInfo(
                name,
                category,
                managerClass,
                false,
                System.currentTimeMillis()
            );
            info.markAsFailed(e.getMessage());
            managers.put(name, info);
        }
    }
    
    /**
     * Register a manager with simple initialization (no supplier).
     * 
     * @param name The unique name/identifier for the manager
     * @param category The category this manager belongs to
     * @param managerClass The class of the manager being registered
     */
    public void registerManager(String name, String category, Class<?> managerClass) {
        registerManager(name, category, managerClass, null);
    }
    
    /**
     * Mark a manager as successfully initialized.
     * 
     * @param name The manager name
     */
    public void markInitialized(String name) {
        ManagerInfo info = managers.get(name);
        if (info != null) {
            info.markAsInitialized();
            LOGGER.debug("Manager '{}' marked as initialized", name);
        }
    }
    
    /**
     * Mark a manager as failed to initialize.
     * 
     * @param name The manager name
     * @param reason The reason for failure
     */
    public void markFailed(String name, String reason) {
        ManagerInfo info = managers.get(name);
        if (info != null) {
            info.markAsFailed(reason);
            LOGGER.error("Manager '{}' marked as failed: {}", name, reason);
        }
    }
    
    /**
     * Check if a manager is registered.
     * 
     * @param name The manager name
     * @return true if the manager is registered
     */
    public boolean isRegistered(String name) {
        return managers.containsKey(name);
    }
    
    /**
     * Check if a manager is initialized.
     * 
     * @param name The manager name
     * @return true if the manager is initialized
     */
    public boolean isInitialized(String name) {
        ManagerInfo info = managers.get(name);
        return info != null && info.isInitialized();
    }
    
    /**
     * Get the total number of registered managers.
     * 
     * @return The count of registered managers
     */
    public int getManagerCount() {
        return managers.size();
    }
    
    /**
     * Get the number of initialized managers.
     * 
     * @return The count of initialized managers
     */
    public int getInitializedCount() {
        return (int) managers.values().stream()
            .filter(ManagerInfo::isInitialized)
            .count();
    }
    
    /**
     * Get the number of failed managers.
     * 
     * @return The count of failed managers
     */
    public int getFailedCount() {
        return (int) managers.values().stream()
            .filter(ManagerInfo::isFailed)
            .count();
    }
    
    /**
     * Get all registered managers grouped by category.
     * 
     * @return Map of category to list of manager names
     */
    public Map<String, List<String>> getManagersByCategory() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        
        for (ManagerInfo info : managers.values()) {
            result.computeIfAbsent(info.category(), k -> new ArrayList<>())
                .add(info.name());
        }
        
        // Sort categories and manager names
        Map<String, List<String>> sorted = new TreeMap<>(result);
        sorted.values().forEach(Collections::sort);
        
        return sorted;
    }
    
    /**
     * Get detailed information about all managers.
     * 
     * @return List of all manager information objects
     */
    public List<ManagerInfo> getAllManagers() {
        return new ArrayList<>(managers.values());
    }
    
    /**
     * Get failed managers with their error messages.
     * 
     * @return Map of manager name to failure reason
     */
    public Map<String, String> getFailedManagers() {
        Map<String, String> failed = new LinkedHashMap<>();
        
        for (ManagerInfo info : managers.values()) {
            if (info.isFailed()) {
                failed.put(info.name(), info.failureReason());
            }
        }
        
        return failed;
    }
    
    /**
     * Generate a diagnostic report of all managers.
     * 
     * @return A formatted string containing the diagnostic report
     */
    public String generateDiagnosticReport() {
        StringBuilder report = new StringBuilder();
        report.append("\n╔════════════════════════════════════════════════════════════════╗\n");
        report.append("║           NEOESSENTIALS MANAGER REGISTRY REPORT            ║\n");
        report.append("╚════════════════════════════════════════════════════════════════╝\n\n");
        
        report.append("Summary:\n");
        report.append(String.format("  Total Managers: %d\n", getManagerCount()));
        report.append(String.format("  Initialized: %d (%.1f%%)\n", 
            getInitializedCount(), 
            (getInitializedCount() * 100.0 / Math.max(1, getManagerCount()))));
        report.append(String.format("  Failed: %d\n\n", getFailedCount()));
        
        // Group by category
        Map<String, List<String>> byCategory = getManagersByCategory();
        
        for (Map.Entry<String, List<String>> entry : byCategory.entrySet()) {
            String category = entry.getKey();
            List<String> managerNames = entry.getValue();
            
            report.append(String.format("Category: %s (%d managers)\n", 
                category.toUpperCase(), managerNames.size()));
            
            for (String name : managerNames) {
                ManagerInfo info = managers.get(name);
                String status = info.isInitialized() ? "✓" : (info.isFailed() ? "✗" : "○");
                Long initTime = initializationTimes.get(name);
                String timeStr = initTime != null ? String.format(" [%dms]", initTime) : "";
                
                report.append(String.format("  %s %s%s", status, name, timeStr));
                
                if (info.isFailed()) {
                    report.append(String.format(" - FAILED: %s", info.failureReason()));
                }
                report.append("\n");
            }
            report.append("\n");
        }
        
        // Failed managers section
        Map<String, String> failed = getFailedManagers();
        if (!failed.isEmpty()) {
            report.append("⚠ FAILED MANAGERS:\n");
            for (Map.Entry<String, String> entry : failed.entrySet()) {
                report.append(String.format("  ✗ %s: %s\n", entry.getKey(), entry.getValue()));
            }
            report.append("\n");
        }
        
        report.append("Legend: ✓ = Initialized | ✗ = Failed | ○ = Registered\n");
        report.append("════════════════════════════════════════════════════════════════\n");
        
        return report.toString();
    }
    
    /**
     * Clear all registered managers.
     * Should only be used during shutdown or testing.
     */
    public void clear() {
        managers.clear();
        initializationTimes.clear();
        LOGGER.info("Cleared all registered managers");
    }
    
    /**
     * Record class containing information about a registered manager.
     */
    public static class ManagerInfo {
        private final String name;
        private final String category;
        private final Class<?> managerClass;
        private boolean initialized;
        private boolean failed;
        private String failureReason;
        private final long registrationTime;
        
        public ManagerInfo(String name, String category, Class<?> managerClass, 
                          boolean initialized, long registrationTime) {
            this.name = name;
            this.category = category;
            this.managerClass = managerClass;
            this.initialized = initialized;
            this.failed = false;
            this.failureReason = null;
            this.registrationTime = registrationTime;
        }
        
        public String name() { return name; }
        public String category() { return category; }
        public Class<?> managerClass() { return managerClass; }
        public boolean isInitialized() { return initialized; }
        public boolean isFailed() { return failed; }
        public String failureReason() { return failureReason; }
        public long registrationTime() { return registrationTime; }
        
        void markAsInitialized() {
            this.initialized = true;
            this.failed = false;
        }
        
        void markAsFailed(String reason) {
            this.initialized = false;
            this.failed = true;
            this.failureReason = reason;
        }
    }
}
