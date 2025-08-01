package com.zerog.neoessentials.systems.security;

import com.zerog.neoessentials.systems.notifications.AlertNotificationSystem;
import com.zerog.neoessentials.systems.status.SystemStatusMonitor;
import com.zerog.neoessentials.systems.analytics.DataAnalyticsSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.nio.file.*;
import java.nio.file.StandardOpenOption;

/**
 * Enterprise Security Monitoring System for NeoEssentials
 * 
 * Provides comprehensive real-time security monitoring, threat detection,
 * and automated security alerting integrated with the AlertNotificationSystem.
 * 
 * Features:
 * - Real-time security event monitoring
 * - Advanced threat detection algorithms
 * - Automated security alerting and incident response
 * - Security metrics collection and analysis
 * - Integration with enterprise monitoring infrastructure
 * - Persistent security audit logging
 * - Anomaly detection and behavior analysis
 * - Security dashboard and reporting
 * 
 * @author ZeroG Enterprise Security Team
 * @since 2.2.0
 */
public class SecurityMonitoringSystem {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityMonitoringSystem.class);
    
    // Singleton instance
    private static SecurityMonitoringSystem instance;
    private static final Object LOCK = new Object();
    
    // Core system components
    private final AlertNotificationSystem alertSystem;
    private final SystemStatusMonitor statusMonitor;
    private final DataAnalyticsSystem analyticsSystem;
    
    // Security monitoring state
    private final AtomicBoolean isMonitoring = new AtomicBoolean(false);
    private ScheduledExecutorService securityExecutor;
    private final ScheduledExecutorService alertExecutor;
    
    // Security metrics and tracking
    private final AtomicLong totalSecurityEvents = new AtomicLong(0);
    private final AtomicLong criticalThreats = new AtomicLong(0);
    private final AtomicLong warningEvents = new AtomicLong(0);
    private final AtomicLong resolvedIncidents = new AtomicLong(0);
    
    // Security event tracking
    private final Map<String, SecurityEventData> activeThreats = new ConcurrentHashMap<>();
    private final Map<String, Long> userActivityMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> failedLoginAttempts = new ConcurrentHashMap<>();
    private final List<SecurityIncident> securityHistory = new CopyOnWriteArrayList<>();
    
    // Configuration settings
    private volatile int maxFailedLogins = 5;
    private volatile long suspiciousActivityThreshold = 10; // events per minute
    private volatile long monitoringInterval = 15000; // 15 seconds
    private volatile boolean realTimeAlerting = true;
    private volatile String securityLogPath = "neoessentials/security.log";
    
    // Threat detection patterns
    private final Set<String> suspiciousCommands = new ConcurrentHashMap<String, Boolean>().keySet();
    private final Set<String> whitelistedIPs = new ConcurrentHashMap<String, Boolean>().keySet();
    private final Map<String, ThreatLevel> threatLevels = new ConcurrentHashMap<>();
    
    /**
     * Private constructor for singleton pattern
     */
    private SecurityMonitoringSystem() {
        this.alertSystem = AlertNotificationSystem.getInstance();
        this.statusMonitor = SystemStatusMonitor.getInstance();
        this.analyticsSystem = DataAnalyticsSystem.getInstance();
        this.alertExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "SecurityAlert-" + System.currentTimeMillis());
            t.setDaemon(true);
            return t;
        });
        
        initializeSuspiciousCommands();
        initializeSecurityConfiguration();
        
        LOGGER.info("Enterprise Security Monitoring System initialized");
    }
    
    /**
     * Get singleton instance of SecurityMonitoringSystem
     */
    public static SecurityMonitoringSystem getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new SecurityMonitoringSystem();
                }
            }
        }
        return instance;
    }
    
    /**
     * Start security monitoring with real-time threat detection
     */
    public void startSecurityMonitoring() {
        if (isMonitoring.compareAndSet(false, true)) {
            securityExecutor = Executors.newScheduledThreadPool(3, r -> {
                Thread t = new Thread(r, "SecurityMonitor-" + System.currentTimeMillis());
                t.setDaemon(true);
                return t;
            });
            
            // Start continuous security monitoring
            securityExecutor.scheduleAtFixedRate(this::performSecurityScan, 
                0, monitoringInterval, TimeUnit.MILLISECONDS);
            
            // Start threat analysis
            securityExecutor.scheduleAtFixedRate(this::analyzeThreatPatterns, 
                30000, 60000, TimeUnit.MILLISECONDS);
            
            // Start security audit logging
            securityExecutor.scheduleAtFixedRate(this::performSecurityAudit, 
                5000, 30000, TimeUnit.MILLISECONDS);
            
            logSecurityEvent("SECURITY_MONITORING_STARTED", "Security monitoring activated", ThreatLevel.INFO);
            
            if (realTimeAlerting) {
                alertSystem.sendAlert(createSecurityAlert(ThreatLevel.INFO, "Security System Started", 
                    "Enterprise Security Monitoring System activated with real-time threat detection"));
            }
            
            LOGGER.info("Security monitoring started with {}-second intervals", monitoringInterval / 1000);
        }
    }
    
    /**
     * Stop security monitoring
     */
    public void stopSecurityMonitoring() {
        if (isMonitoring.compareAndSet(true, false)) {
            if (securityExecutor != null && !securityExecutor.isShutdown()) {
                securityExecutor.shutdown();
                try {
                    if (!securityExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                        securityExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    securityExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            logSecurityEvent("SECURITY_MONITORING_STOPPED", "Security monitoring deactivated", ThreatLevel.INFO);
            
            if (realTimeAlerting) {
                alertSystem.sendAlert(createSecurityAlert(ThreatLevel.INFO, "Security System Stopped", 
                    "Enterprise Security Monitoring System deactivated"));
            }
            
            LOGGER.info("Security monitoring stopped");
        }
    }
    
    /**
     * Perform comprehensive security scan
     */
    private void performSecurityScan() {
        try {
            // Check for suspicious user activity
            checkSuspiciousActivity();
            
            // Monitor failed login attempts
            monitorFailedLogins();
            
            // Analyze command execution patterns
            analyzeCommandPatterns();
            
            // Check system integrity
            checkSystemIntegrity();
            
            // Monitor resource access patterns
            monitorResourceAccess();
            
            // Update security metrics
            updateSecurityMetrics();
            
        } catch (Exception e) {
            LOGGER.error("Error during security scan", e);
            logSecurityEvent("SECURITY_SCAN_ERROR", "Error during security scan: " + e.getMessage(), ThreatLevel.HIGH);
        }
    }
    
    /**
     * Check for suspicious user activity patterns
     */
    private void checkSuspiciousActivity() {
        long currentTime = System.currentTimeMillis();
        
        for (Map.Entry<String, Long> entry : userActivityMap.entrySet()) {
            String user = entry.getKey();
            long lastActivity = entry.getValue();
            
            // Check for rapid command execution
            if (currentTime - lastActivity < 1000) { // Less than 1 second between commands
                SecurityIncident incident = new SecurityIncident(
                    "RAPID_COMMAND_EXECUTION",
                    "User " + user + " executing commands rapidly",
                    user,
                    ThreatLevel.MEDIUM,
                    currentTime
                );
                
                processSecurityIncident(incident);
            }
        }
    }
    
    /**
     * Monitor failed login attempts for brute force detection
     */
    private void monitorFailedLogins() {
        for (Map.Entry<String, Integer> entry : failedLoginAttempts.entrySet()) {
            String user = entry.getKey();
            int attempts = entry.getValue();
            
            if (attempts >= maxFailedLogins) {
                SecurityIncident incident = new SecurityIncident(
                    "BRUTE_FORCE_ATTEMPT",
                    "User " + user + " exceeded failed login threshold (" + attempts + " attempts)",
                    user,
                    ThreatLevel.HIGH,
                    System.currentTimeMillis()
                );
                
                processSecurityIncident(incident);
                
                // Reset counter after processing
                failedLoginAttempts.put(user, 0);
            }
        }
    }
    
    /**
     * Analyze command execution patterns for anomalies
     */
    private void analyzeCommandPatterns() {
        // This would integrate with DataAnalyticsSystem to analyze command patterns
        try {
            // For now, we'll use a simplified approach since the exact method may vary
            // In a real implementation, this would connect to the analytics system
            
            // Look for suspicious command usage patterns
            for (String command : suspiciousCommands) {
                // Placeholder for command analysis - would integrate with analytics system
                // when the exact API is available
                LOGGER.debug("Monitoring command pattern for: {}", command);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not analyze command patterns", e);
        }
    }
    
    /**
     * Check system integrity for potential security issues
     */
    private void checkSystemIntegrity() {
        try {
            // Monitor system health for security implications
            SystemStatusMonitor.ComprehensiveSystemStatus systemStatus = statusMonitor.getSystemStatus();
            SystemStatusMonitor.ResourceStatus resourceStatus = systemStatus.getResourceStatus();
            
            double systemHealth = systemStatus.getHealthScore();
            
            if (systemHealth < 50.0) {
                SecurityIncident incident = new SecurityIncident(
                    "SYSTEM_INTEGRITY_CONCERN",
                    "System health critically low - potential security implications",
                    "SYSTEM",
                    ThreatLevel.HIGH,
                    System.currentTimeMillis()
                );
                
                processSecurityIncident(incident);
            }
            
            // Check for unusual resource consumption
            double memoryPercent = resourceStatus.getMemoryUsagePercent();
            
            if (memoryPercent > 95.0) {
                SecurityIncident incident = new SecurityIncident(
                    "RESOURCE_EXHAUSTION_ATTACK",
                    "Extremely high memory usage - potential DoS attack",
                    "SYSTEM",
                    ThreatLevel.CRITICAL,
                    System.currentTimeMillis()
                );
                
                processSecurityIncident(incident);
            }
            
        } catch (Exception e) {
            LOGGER.warn("Could not check system integrity", e);
        }
    }
    
    /**
     * Monitor resource access patterns
     */
    private void monitorResourceAccess() {
        // Monitor file system access, network connections, etc.
        // This is a placeholder for more advanced monitoring
        checkFileSystemAccess();
        checkNetworkActivity();
    }
    
    /**
     * Check file system access patterns
     */
    private void checkFileSystemAccess() {
        try {
            Path securityLogPath = Paths.get(this.securityLogPath);
            if (Files.exists(securityLogPath)) {
                long size = Files.size(securityLogPath);
                if (size > 50 * 1024 * 1024) { // 50MB
                    SecurityIncident incident = new SecurityIncident(
                        "LARGE_SECURITY_LOG",
                        "Security log file has grown very large - potential security event storm",
                        "SYSTEM",
                        ThreatLevel.MEDIUM,
                        System.currentTimeMillis()
                    );
                    
                    processSecurityIncident(incident);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Could not check file system access", e);
        }
    }
    
    /**
     * Check network activity patterns
     */
    private void checkNetworkActivity() {
        // Monitor network connections for suspicious activity
        // This is a placeholder for network monitoring capabilities
    }
    
    /**
     * Analyze threat patterns and update threat intelligence
     */
    private void analyzeThreatPatterns() {
        try {
            // Analyze recent security incidents for patterns
            long recentTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // Last 24 hours
            
            Map<String, Integer> threatCounts = new HashMap<>();
            Map<String, Integer> userThreatCounts = new HashMap<>();
            
            for (SecurityIncident incident : securityHistory) {
                if (incident.getTimestamp() > recentTime) {
                    threatCounts.merge(incident.getThreatType(), 1, Integer::sum);
                    userThreatCounts.merge(incident.getUser(), 1, Integer::sum);
                }
            }
            
            // Check for threat escalation patterns
            for (Map.Entry<String, Integer> entry : threatCounts.entrySet()) {
                String threatType = entry.getKey();
                int count = entry.getValue();
                
                if (count > 10) { // More than 10 incidents of same type in 24 hours
                    SecurityIncident incident = new SecurityIncident(
                        "THREAT_PATTERN_DETECTED",
                        "High frequency of " + threatType + " incidents detected (" + count + " in 24h)",
                        "SYSTEM",
                        ThreatLevel.HIGH,
                        System.currentTimeMillis()
                    );
                    
                    processSecurityIncident(incident);
                }
            }
            
            // Check for problematic users
            for (Map.Entry<String, Integer> entry : userThreatCounts.entrySet()) {
                String user = entry.getKey();
                int count = entry.getValue();
                
                if (count > 5) { // More than 5 security incidents per user in 24 hours
                    SecurityIncident incident = new SecurityIncident(
                        "USER_THREAT_PATTERN",
                        "User " + user + " involved in multiple security incidents (" + count + " in 24h)",
                        user,
                        ThreatLevel.HIGH,
                        System.currentTimeMillis()
                    );
                    
                    processSecurityIncident(incident);
                }
            }
            
        } catch (Exception e) {
            LOGGER.error("Error analyzing threat patterns", e);
        }
    }
    
    /**
     * Perform security audit and generate reports
     */
    private void performSecurityAudit() {
        try {
            // Log current security status
            SecurityAuditReport report = generateSecurityAuditReport();
            logSecurityAudit(report);
            
            // Clean up old security data
            cleanupOldSecurityData();
            
        } catch (Exception e) {
            LOGGER.error("Error during security audit", e);
        }
    }
    
    /**
     * Process a security incident
     */
    private void processSecurityIncident(SecurityIncident incident) {
        // Add to active threats if critical
        if (incident.getThreatLevel() == ThreatLevel.CRITICAL || incident.getThreatLevel() == ThreatLevel.HIGH) {
            activeThreats.put(incident.getId(), new SecurityEventData(incident));
        }
        
        // Add to security history
        securityHistory.add(incident);
        
        // Update metrics
        totalSecurityEvents.incrementAndGet();
        if (incident.getThreatLevel() == ThreatLevel.CRITICAL || incident.getThreatLevel() == ThreatLevel.HIGH) {
            criticalThreats.incrementAndGet();
        } else if (incident.getThreatLevel() == ThreatLevel.MEDIUM) {
            warningEvents.incrementAndGet();
        }
        
        // Log security event
        logSecurityEvent(incident.getThreatType(), incident.getDescription(), incident.getThreatLevel());
        
        // Send real-time alert if enabled
        if (realTimeAlerting) {
            ThreatLevel alertLevel = incident.getThreatLevel();
            alertSystem.sendAlert(createSecurityAlert(alertLevel, "Security Incident", 
                "Security incident detected: " + incident.getDescription()));
        }
        
        // Trigger automated response if critical
        if (incident.getThreatLevel() == ThreatLevel.CRITICAL) {
            triggerAutomatedResponse(incident);
        }
        
        LOGGER.warn("Security incident processed: {} - {}", incident.getThreatType(), incident.getDescription());
    }
    
    /**
     * Trigger automated security response
     */
    private void triggerAutomatedResponse(SecurityIncident incident) {
        alertExecutor.execute(() -> {
            try {
                // Immediate critical alert
                alertSystem.sendAlert(createSecurityAlert(ThreatLevel.CRITICAL, "Critical Security Threat", 
                    "CRITICAL: " + incident.getDescription() + " - Automated response initiated"));
                
                // Log immediate response
                logSecurityEvent("AUTOMATED_RESPONSE", 
                    "Automated response triggered for: " + incident.getThreatType(), 
                    ThreatLevel.CRITICAL);
                
                // Additional automated actions could be implemented here
                // e.g., temporary user suspension, IP blocking, etc.
                
            } catch (Exception e) {
                LOGGER.error("Error in automated security response", e);
            }
        });
    }
    
    /**
     * Update security metrics
     */
    private void updateSecurityMetrics() {
        // This integrates with the status monitor to provide security metrics
        Map<String, Object> securityMetrics = new HashMap<>();
        securityMetrics.put("totalSecurityEvents", totalSecurityEvents.get());
        securityMetrics.put("activeThreatCount", activeThreats.size());
        securityMetrics.put("criticalThreats", criticalThreats.get());
        securityMetrics.put("warningEvents", warningEvents.get());
        securityMetrics.put("resolvedIncidents", resolvedIncidents.get());
        securityMetrics.put("monitoringActive", isMonitoring.get());
        securityMetrics.put("lastScanTime", System.currentTimeMillis());
        
        // Update status monitor with security metrics
        statusMonitor.updateComponentStatus("SecurityMonitoring", 
            SystemStatusMonitor.ComponentState.ACTIVE, 
            "Security monitoring active - " + activeThreats.size() + " active threats");
    }
    
    /**
     * Generate comprehensive security audit report
     */
    private SecurityAuditReport generateSecurityAuditReport() {
        SecurityAuditReport report = new SecurityAuditReport();
        report.setTimestamp(System.currentTimeMillis());
        report.setTotalEvents(totalSecurityEvents.get());
        report.setActiveThreats(activeThreats.size());
        report.setCriticalThreats(criticalThreats.get());
        report.setWarningEvents(warningEvents.get());
        report.setResolvedIncidents(resolvedIncidents.get());
        report.setMonitoringStatus(isMonitoring.get());
        
        // Recent incident summary
        long recentTime = System.currentTimeMillis() - (60 * 60 * 1000); // Last hour
        long recentIncidents = securityHistory.stream()
            .filter(incident -> incident.getTimestamp() > recentTime)
            .count();
        report.setRecentIncidents(recentIncidents);
        
        return report;
    }
    
    /**
     * Log security event to file and console
     */
    private void logSecurityEvent(String eventType, String description, ThreatLevel level) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logEntry = String.format("[%s] [%s] %s: %s%n", timestamp, level, eventType, description);
            
            // Write to security log file
            Path logPath = Paths.get(securityLogPath);
            Files.createDirectories(logPath.getParent());
            Files.write(logPath, logEntry.getBytes(), 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // Log to console based on threat level
            switch (level) {
                case CRITICAL:
                case HIGH:
                    LOGGER.error("SECURITY: {} - {}", eventType, description);
                    break;
                case MEDIUM:
                    LOGGER.warn("SECURITY: {} - {}", eventType, description);
                    break;
                case LOW:
                case INFO:
                    LOGGER.info("SECURITY: {} - {}", eventType, description);
                    break;
            }
            
        } catch (Exception e) {
            LOGGER.error("Failed to log security event", e);
        }
    }
    
    /**
     * Log security audit report
     */
    private void logSecurityAudit(SecurityAuditReport report) {
        try {
            String auditEntry = String.format(
                "SECURITY AUDIT REPORT - %s%n" +
                "Total Events: %d | Active Threats: %d | Critical: %d | Warnings: %d%n" +
                "Recent Incidents (1h): %d | Monitoring: %s%n",
                LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
                report.getTotalEvents(), report.getActiveThreats(), report.getCriticalThreats(),
                report.getWarningEvents(), report.getRecentIncidents(), report.isMonitoringStatus()
            );
            
            Path logPath = Paths.get(securityLogPath);
            Files.write(logPath, auditEntry.getBytes(), 
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
        } catch (Exception e) {
            LOGGER.error("Failed to log security audit", e);
        }
    }
    
    /**
     * Clean up old security data
     */
    private void cleanupOldSecurityData() {
        long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 7 days
        
        // Remove old incidents from history
        securityHistory.removeIf(incident -> incident.getTimestamp() < cutoffTime);
        
        // Remove resolved threats from active threats
        activeThreats.entrySet().removeIf(entry -> 
            entry.getValue().isResolved() || entry.getValue().getTimestamp() < cutoffTime);
        
        // Clean up old user activity data
        userActivityMap.entrySet().removeIf(entry -> 
            System.currentTimeMillis() - entry.getValue() > (24 * 60 * 60 * 1000)); // 24 hours
    }
    
    /**
     * Initialize suspicious command patterns
     */
    private void initializeSuspiciousCommands() {
        suspiciousCommands.add("op");
        suspiciousCommands.add("deop");
        suspiciousCommands.add("ban");
        suspiciousCommands.add("kick");
        suspiciousCommands.add("stop");
        suspiciousCommands.add("reload");
        suspiciousCommands.add("whitelist");
        suspiciousCommands.add("pardon");
    }
    
    /**
     * Initialize security configuration
     */
    private void initializeSecurityConfiguration() {
        threatLevels.put("BRUTE_FORCE_ATTEMPT", ThreatLevel.HIGH);
        threatLevels.put("RAPID_COMMAND_EXECUTION", ThreatLevel.MEDIUM);
        threatLevels.put("SUSPICIOUS_COMMAND_USAGE", ThreatLevel.MEDIUM);
        threatLevels.put("SYSTEM_INTEGRITY_CONCERN", ThreatLevel.HIGH);
        threatLevels.put("RESOURCE_EXHAUSTION_ATTACK", ThreatLevel.CRITICAL);
    }
    
    // Configuration getters and setters
    public boolean isMonitoring() { return isMonitoring.get(); }
    public long getTotalSecurityEvents() { return totalSecurityEvents.get(); }
    public long getCriticalThreats() { return criticalThreats.get(); }
    public long getWarningEvents() { return warningEvents.get(); }
    public int getActiveThreats() { return activeThreats.size(); }
    public long getResolvedIncidents() { return resolvedIncidents.get(); }
    
    public void setMaxFailedLogins(int maxFailedLogins) { this.maxFailedLogins = maxFailedLogins; }
    public void setSuspiciousActivityThreshold(long threshold) { this.suspiciousActivityThreshold = threshold; }
    public void setMonitoringInterval(long interval) { this.monitoringInterval = interval; }
    public void setRealTimeAlerting(boolean enabled) { this.realTimeAlerting = enabled; }
    
    /**
     * Get security statistics for monitoring integration
     */
    public Map<String, Object> getSecurityStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("monitoring", isMonitoring.get());
        stats.put("totalEvents", totalSecurityEvents.get());
        stats.put("activeThreats", activeThreats.size());
        stats.put("criticalThreats", criticalThreats.get());
        stats.put("warningEvents", warningEvents.get());
        stats.put("resolvedIncidents", resolvedIncidents.get());
        stats.put("monitoringInterval", monitoringInterval);
        stats.put("realTimeAlerting", realTimeAlerting);
        stats.put("lastUpdate", System.currentTimeMillis());
        return stats;
    }
    
    /**
     * Get current security configuration
     */
    public Map<String, Object> getSecurityConfiguration() {
        Map<String, Object> config = new HashMap<>();
        config.put("maxFailedLogins", maxFailedLogins);
        config.put("suspiciousActivityThreshold", suspiciousActivityThreshold);
        config.put("monitoringInterval", monitoringInterval);
        config.put("realTimeAlerting", realTimeAlerting);
        config.put("securityLogPath", securityLogPath);
        config.put("activeThreatsCount", activeThreats.size());
        return config;
    }
    
    /**
     * Create a security alert for the notification system
     */
    private AlertNotificationSystem.StatusAlert createSecurityAlert(ThreatLevel level, String title, String message) {
        AlertNotificationSystem.AlertLevel alertLevel;
        switch (level) {
            case CRITICAL: alertLevel = AlertNotificationSystem.AlertLevel.CRITICAL; break;
            case HIGH: alertLevel = AlertNotificationSystem.AlertLevel.ERROR; break;
            case MEDIUM: alertLevel = AlertNotificationSystem.AlertLevel.WARNING; break;
            case LOW: alertLevel = AlertNotificationSystem.AlertLevel.INFO; break;
            case INFO: 
            default: alertLevel = AlertNotificationSystem.AlertLevel.INFO; break;
        }
        
        return new AlertNotificationSystem.StatusAlert(
            alertLevel,
            title,
            message,
            "SecurityMonitoring",
            java.time.LocalDateTime.now()
        );
    }
    
    // Inner classes for security data structures
    private static class SecurityEventData {
        private final SecurityIncident incident;
        private final long timestamp;
        private boolean resolved;
        
        public SecurityEventData(SecurityIncident incident) {
            this.incident = incident;
            this.timestamp = System.currentTimeMillis();
            this.resolved = false;
        }
        
        public boolean isResolved() { return resolved; }
        public void setResolved(boolean resolved) { this.resolved = resolved; }
        public long getTimestamp() { return timestamp; }
        public SecurityIncident getIncident() { return incident; }
    }
    
    private static class SecurityIncident {
        private final String id;
        private final String threatType;
        private final String description;
        private final String user;
        private final ThreatLevel threatLevel;
        private final long timestamp;
        
        public SecurityIncident(String threatType, String description, String user, ThreatLevel threatLevel, long timestamp) {
            this.id = generateIncidentId();
            this.threatType = threatType;
            this.description = description;
            this.user = user;
            this.threatLevel = threatLevel;
            this.timestamp = timestamp;
        }
        
        private String generateIncidentId() {
            return "SEC-" + System.currentTimeMillis() + "-" + (int)(Math.random() * 1000);
        }
        
        // Getters
        public String getId() { return id; }
        public String getThreatType() { return threatType; }
        public String getDescription() { return description; }
        public String getUser() { return user; }
        public ThreatLevel getThreatLevel() { return threatLevel; }
        public long getTimestamp() { return timestamp; }
    }
    
    private static class SecurityAuditReport {
        private long timestamp;
        private long totalEvents;
        private int activeThreats;
        private long criticalThreats;
        private long warningEvents;
        private long resolvedIncidents;
        private boolean monitoringStatus;
        private long recentIncidents;
        
        // Getters and setters
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        
        public long getTotalEvents() { return totalEvents; }
        public void setTotalEvents(long totalEvents) { this.totalEvents = totalEvents; }
        
        public int getActiveThreats() { return activeThreats; }
        public void setActiveThreats(int activeThreats) { this.activeThreats = activeThreats; }
        
        public long getCriticalThreats() { return criticalThreats; }
        public void setCriticalThreats(long criticalThreats) { this.criticalThreats = criticalThreats; }
        
        public long getWarningEvents() { return warningEvents; }
        public void setWarningEvents(long warningEvents) { this.warningEvents = warningEvents; }
        
        public long getResolvedIncidents() { return resolvedIncidents; }
        public void setResolvedIncidents(long resolvedIncidents) { this.resolvedIncidents = resolvedIncidents; }
        
        public boolean isMonitoringStatus() { return monitoringStatus; }
        public void setMonitoringStatus(boolean monitoringStatus) { this.monitoringStatus = monitoringStatus; }
        
        public long getRecentIncidents() { return recentIncidents; }
        public void setRecentIncidents(long recentIncidents) { this.recentIncidents = recentIncidents; }
    }
    
    public enum ThreatLevel {
        INFO, LOW, MEDIUM, HIGH, CRITICAL
    }
}
