package com.zerog.neoessentials.systems.security;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Security audit logger interface
 */
public class AuditLogger {
    private boolean enabled = true;
    private Path logPath;
    private boolean initialized = false;
    
    /**
     * Initialize the audit logger
     */
    public void initialize() {
        try {
            logPath = Paths.get("logs", "audit.log");
            Files.createDirectories(logPath.getParent());
            initialized = true;
        } catch (IOException e) {
            System.err.println("Failed to initialize audit logger: " + e.getMessage());
            initialized = false;
        }
    }
    
    public void logSecurityEvent(String event, String user, String details) {
        if (enabled && initialized) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            String logEntry = String.format("[%s] [AUDIT] %s by %s: %s%n", timestamp, event, user, details);
            
            try {
                Files.write(logPath, logEntry.getBytes(), 
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Failed to write audit log: " + e.getMessage());
            }
        }
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Log a security event
     */
    public void logEvent(SecurityEvent event) {
        if (enabled && initialized) {
            String logEntry = String.format("[%s] [AUDIT] %s by %s: %s%n", 
                event.getTimestamp(), 
                event.getType(), 
                event.getUser(), 
                event.getAction());
            
            try {
                Files.write(logPath, logEntry.getBytes(), 
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND);
            } catch (IOException e) {
                System.err.println("Failed to write audit log: " + e.getMessage());
            }
        }
    }
    
    /**
     * Flush log buffers
     */
    public void flush() {
        // In this simple implementation, flush is not needed since we write directly
        // In a more complex implementation, this would flush any buffered writes
    }
    
    /**
     * Rotate log files
     */
    public void rotate() {
        if (!initialized) return;
        
        try {
            // Simple rotation: rename current log and create new one
            Path backupPath = Paths.get(logPath.toString() + "." + System.currentTimeMillis());
            Files.move(logPath, backupPath);
            Files.createFile(logPath);
        } catch (IOException e) {
            System.err.println("Failed to rotate audit log: " + e.getMessage());
        }
    }
}
