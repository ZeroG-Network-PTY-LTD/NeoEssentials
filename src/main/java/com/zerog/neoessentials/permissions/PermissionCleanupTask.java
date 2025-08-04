package com.zerog.neoessentials.permissions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Scheduled task to clean up expired temporary permissions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class PermissionCleanupTask extends TimerTask {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionCleanupTask.class);
    private static Timer timer;
    private static final long CLEANUP_INTERVAL = 300000; // 5 minutes
    
    /**
     * Start the permission cleanup task
     */
    public static void start() {
        if (timer != null) {
            stop(); // Stop existing timer if running
        }
        
        timer = new Timer("PermissionCleanup", true);
        timer.scheduleAtFixedRate(new PermissionCleanupTask(), CLEANUP_INTERVAL, CLEANUP_INTERVAL);
        
        LOGGER.info("Started permission cleanup task (interval: {}ms)", CLEANUP_INTERVAL);
    }
    
    /**
     * Stop the permission cleanup task
     */
    public static void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
            LOGGER.info("Stopped permission cleanup task");
        }
    }
    
    @Override
    public void run() {
        try {
            CustomPermissionsManager manager = CustomPermissionsManager.getInstance();
            manager.cleanupExpiredPermissions();
        } catch (Exception e) {
            LOGGER.error("Error during permission cleanup", e);
        }
    }
}
