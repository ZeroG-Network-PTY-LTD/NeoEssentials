package com.zerog.neoessentials.systems.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import com.zerog.neoessentials.models.security.SecurityViolation;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Enterprise Security Manager for NeoEssentials
 * Provides comprehensive security features including audit logging, 
 * intrusion detection, permission management, and security analytics
 * 
 * Features:
 * - Real-time audit logging with detailed event tracking
 * - Security event analysis and threat detection
 * - User session management and authentication
 * - Permission system with role-based access control
 * - Security metrics and reporting
 * - Automated security policy enforcement
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class SecurityManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityManager.class);
    
    // Singleton instance
    private static SecurityManager instance;
    
    // Core components
    private final AuditLogger auditLogger;
    private final ThreatDetector threatDetector;
    private final SessionManager sessionManager;
    private final PermissionManager permissionManager;
    private final SecurityAnalytics analytics;
    
    // Configuration
    private final Path securityDir;
    private final Path auditLogFile;
    private final Path configFile;
    private SecurityConfig config;
    
    // Runtime data
    private final Map<String, SecurityEvent> recentEvents = new ConcurrentHashMap<>();
    private final List<SecurityAlert> activeAlerts = new CopyOnWriteArrayList<>();
    private final Map<String, SecurityMetrics> userMetrics = new ConcurrentHashMap<>();
    
    // Background services
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(3);
    private boolean isRunning = false;
    
    // JSON serialization
    private final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create();
    
    public SecurityManager() {
        this.securityDir = Paths.get("config", "neoessentials", "security");
        this.auditLogFile = securityDir.resolve("audit.log");
        this.configFile = securityDir.resolve("security-config.json");
        
        // Initialize components
        this.auditLogger = new AuditLogger();
        this.threatDetector = new ThreatDetector();
        this.sessionManager = new SessionManager();
        this.permissionManager = new PermissionManager();
        this.analytics = new SecurityAnalytics();
        
        // Create directories if they don't exist
        try {
            Files.createDirectories(securityDir);
        } catch (IOException e) {
            LOGGER.error("Failed to create security directory", e);
        }
        
        loadConfiguration();
    }
    
    public static SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }
    
    /**
     * Initialize and start the security manager
     */
    public void initialize() {
        if (isRunning) {
            LOGGER.warn("Security Manager is already running");
            return;
        }
        
        LOGGER.info("Initializing Enterprise Security Manager...");
        
        // Load configuration
        loadConfiguration();
        
        // Initialize components
        auditLogger.initialize();
        threatDetector.initialize();
        sessionManager.initialize();
        permissionManager.initialize();
        analytics.initialize();
        
        // Start background services
        startBackgroundServices();
        
        isRunning = true;
        
        // Log security initialization
        auditLogger.logEvent(new SecurityEvent(
            SecurityEventType.SYSTEM_STARTUP,
            "SYSTEM",
            "Security Manager initialized successfully",
            SecurityLevel.INFO,
            Map.of(
                "components", "AuditLogger, ThreatDetector, SessionManager, PermissionManager, Analytics",
                "startup_time", LocalDateTime.now().toString()
            )
        ));
        
        LOGGER.info("Enterprise Security Manager initialized successfully");
    }
    
    /**
     * Shutdown the security manager
     */
    public void shutdown() {
        if (!isRunning) {
            return;
        }
        
        LOGGER.info("Shutting down Security Manager...");
        
        // Log shutdown event
        auditLogger.logEvent(new SecurityEvent(
            SecurityEventType.SYSTEM_SHUTDOWN,
            "SYSTEM",
            "Security Manager shutting down",
            SecurityLevel.INFO,
            Map.of("shutdown_time", LocalDateTime.now().toString())
        ));
        
        // Shutdown background services
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Save final state
        saveConfiguration();
        auditLogger.flush();
        
        isRunning = false;
        LOGGER.info("Security Manager shutdown complete");
    }
    
    /**
     * Log a security event with full audit trail
     */
    public void logSecurityEvent(SecurityEventType type, String user, String action, 
                                SecurityLevel level, Map<String, Object> details) {
        SecurityEvent event = new SecurityEvent(type, user, action, level, details);
        
        // Log to audit trail
        auditLogger.logEvent(event);
        
        // Store for real-time analysis
        recentEvents.put(event.getId(), event);
        
        // Analyze for threats
        threatDetector.analyzeEvent(event);
        
        // Update analytics
        analytics.processEvent(event);
        
        // Update user metrics
        updateUserMetrics(user, event);
        
        // Clean up old events (keep last 1000)
        if (recentEvents.size() > 1000) {
            Iterator<String> iterator = recentEvents.keySet().iterator();
            for (int i = 0; i < 100 && iterator.hasNext(); i++) {
                iterator.next();
                iterator.remove();
            }
        }
    }
    
    /**
     * Get current security status overview
     */
    public SecurityStatus getSecurityStatus() {
        return new SecurityStatus(
            isRunning,
            activeAlerts.size(),
            recentEvents.size(),
            sessionManager.getActiveSessionCount(),
            threatDetector.getThreatLevel(),
            analytics.getSecurityScore(),
            LocalDateTime.now()
        );
    }
    
    /**
     * Get security metrics for a specific user
     */
    public SecurityMetrics getUserSecurityMetrics(String username) {
        return userMetrics.getOrDefault(username, new SecurityMetrics(username));
    }
    
    /**
     * Get all active security alerts
     */
    public List<SecurityAlert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts);
    }
    
    /**
     * Get recent security events with optional filtering
     */
    public List<SecurityEvent> getRecentEvents(SecurityEventType type, int limit) {
        return recentEvents.values().stream()
            .filter(event -> type == null || event.getType() == type)
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(limit)
            .toList();
    }
    
    /**
     * Generate comprehensive security report
     */
    public SecurityReport generateSecurityReport(LocalDateTime startTime, LocalDateTime endTime) {
        return analytics.generateReport(startTime, endTime);
    }
    
    /**
     * Check if a user has specific permission
     */
    public boolean hasPermission(String username, String permission) {
        return permissionManager.hasPermission(username, permission);
    }
    
    /**
     * Grant permission to user
     */
    public void grantPermission(String grantedBy, String username, String permission) {
        permissionManager.grantPermission(username, permission);
        
        logSecurityEvent(
            SecurityEventType.PERMISSION_GRANTED,
            grantedBy,
            "Granted permission " + permission + " to " + username,
            SecurityLevel.INFO,
            Map.of(
                "target_user", username,
                "permission", permission,
                "granted_by", grantedBy
            )
        );
    }
    
    /**
     * Revoke permission from user
     */
    public void revokePermission(String revokedBy, String username, String permission) {
        permissionManager.revokePermission(username, permission);
        
        logSecurityEvent(
            SecurityEventType.PERMISSION_REVOKED,
            revokedBy,
            "Revoked permission " + permission + " from " + username,
            SecurityLevel.WARNING,
            Map.of(
                "target_user", username,
                "permission", permission,
                "revoked_by", revokedBy
            )
        );
    }
    
    /**
     * Start background security services
     */
    private void startBackgroundServices() {
        // Security monitoring task (every 30 seconds)
        scheduler.scheduleAtFixedRate(this::performSecurityScan, 30, 30, TimeUnit.SECONDS);
        
        // Audit log rotation task (every hour)
        scheduler.scheduleAtFixedRate(auditLogger::rotate, 1, 1, TimeUnit.HOURS);
        
        // Threat analysis task (every 2 minutes)
        scheduler.scheduleAtFixedRate(threatDetector::performAnalysis, 2, 2, TimeUnit.MINUTES);
        
        // Session cleanup task (every 5 minutes)
        scheduler.scheduleAtFixedRate(sessionManager::cleanupExpiredSessions, 5, 5, TimeUnit.MINUTES);
        
        // Security report generation task (every 6 hours)
        scheduler.scheduleAtFixedRate(this::generatePeriodicReport, 6, 6, TimeUnit.HOURS);
    }
    
    /**
     * Perform comprehensive security scan
     */
    private void performSecurityScan() {
        try {
            // Check for security policy violations
            List<SecurityViolation> violations = threatDetector.checkSecurityPolicies();
            
            // Process violations
            for (SecurityViolation violation : violations) {
                SecurityAlert alert = new SecurityAlert(
                    violation.getType(),
                    violation.getSeverity(),
                    violation.getDescription(),
                    violation.getDetails(),
                    LocalDateTime.now()
                );
                
                activeAlerts.add(alert);
                
                logSecurityEvent(
                    SecurityEventType.SECURITY_VIOLATION,
                    violation.getUser(),
                    violation.getDescription(),
                    SecurityLevel.WARNING,
                    violation.getDetails()
                );
            }
            
            // Clean up old alerts (keep last 50)
            if (activeAlerts.size() > 50) {
                activeAlerts.subList(0, activeAlerts.size() - 50).clear();
            }
            
        } catch (Exception e) {
            LOGGER.error("Error during security scan", e);
        }
    }
    
    /**
     * Generate periodic security report
     */
    private void generatePeriodicReport() {
        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(6);
            
            SecurityReport report = generateSecurityReport(startTime, endTime);
            
            // Save report to file
            Path reportFile = securityDir.resolve("reports")
                .resolve("security-report-" + endTime.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json");
            
            Files.createDirectories(reportFile.getParent());
            Files.writeString(reportFile, gson.toJson(report), StandardCharsets.UTF_8);
            
            logSecurityEvent(
                SecurityEventType.REPORT_GENERATED,
                "SYSTEM",
                "Periodic security report generated",
                SecurityLevel.INFO,
                Map.of(
                    "report_file", reportFile.toString(),
                    "period_start", startTime.toString(),
                    "period_end", endTime.toString(),
                    "events_analyzed", report.getTotalEvents()
                )
            );
            
        } catch (Exception e) {
            LOGGER.error("Error generating periodic security report", e);
        }
    }
    
    /**
     * Update security metrics for a user
     */
    private void updateUserMetrics(String username, SecurityEvent event) {
        SecurityMetrics metrics = userMetrics.computeIfAbsent(username, SecurityMetrics::new);
        metrics.recordEvent(event);
    }
    
    /**
     * Load security configuration
     */
    private void loadConfiguration() {
        try {
            if (Files.exists(configFile)) {
                String json = Files.readString(configFile, StandardCharsets.UTF_8);
                config = gson.fromJson(json, SecurityConfig.class);
            } else {
                config = createDefaultConfiguration();
                saveConfiguration();
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load security configuration, using defaults", e);
            config = createDefaultConfiguration();
        }
    }
    
    /**
     * Save security configuration
     */
    private void saveConfiguration() {
        try {
            String json = gson.toJson(config);
            Files.writeString(configFile, json, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            LOGGER.error("Failed to save security configuration", e);
        }
    }
    
    /**
     * Create default security configuration
     */
    private SecurityConfig createDefaultConfiguration() {
        SecurityConfig defaultConfig = new SecurityConfig();
        defaultConfig.setAuditLogEnabled(true);
        defaultConfig.setMaxAuditLogSize(100 * 1024 * 1024); // 100MB
        defaultConfig.setSessionTimeout(3600); // 1 hour
        defaultConfig.setMaxFailedLogins(5);
        defaultConfig.setThreatDetectionEnabled(true);
        defaultConfig.setAutoBlockSuspiciousIPs(true);
        defaultConfig.setSecurityReportsEnabled(true);
        return defaultConfig;
    }
    
    // Getters for access to components
    public AuditLogger getAuditLogger() { return auditLogger; }
    public ThreatDetector getThreatDetector() { return threatDetector; }
    public SessionManager getSessionManager() { return sessionManager; }
    public PermissionManager getPermissionManager() { return permissionManager; }
    public SecurityAnalytics getAnalytics() { return analytics; }
    public SecurityConfig getConfig() { return config; }
    public boolean isRunning() { return isRunning; }
}
