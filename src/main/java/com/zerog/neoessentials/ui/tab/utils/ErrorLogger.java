package com.zerog.neoessentials.ui.tab.utils;

import com.zerog.neoessentials.NeoEssentials;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Error logging system for the TabManager
 * Logs errors both to console and a tab-errors.log file
 */
public class ErrorLogger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final File logFile;
    private boolean consoleLogging = true;
    private boolean fileLogging = true;
    
    /**
     * Creates a new error logger
     */
    public ErrorLogger() {
        // Create log file in config/neoessentials folder
        this.logFile = new File("config/neoessentials/tab-errors.log");
        try {
            if (!logFile.exists()) {
                File parent = logFile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                logFile.createNewFile();
            }
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to create tab-errors.log file", e);
        }
    }
    
    /**
     * Enable or disable logging to console
     */
    public void setConsoleLogging(boolean enabled) {
        this.consoleLogging = enabled;
    }
    
    /**
     * Enable or disable logging to file
     */
    public void setFileLogging(boolean enabled) {
        this.fileLogging = enabled;
    }
    
    /**
     * Logs an error to console and/or file
     * 
     * @param message Error message
     */
    public void logError(String message) {
        logError(message, null);
    }
    
    /**
     * Logs an error with exception to console and/or file
     * 
     * @param message Error message
     * @param error Exception that occurred
     */
    public void logError(String message, Throwable error) {
        String timestamp = DATE_FORMAT.format(new Date());
        String logMessage = "[" + timestamp + "] " + message;
        
        // Log to console
        if (consoleLogging) {
            if (error != null) {
                NeoEssentials.LOGGER.error(logMessage, error);
            } else {
                NeoEssentials.LOGGER.error(logMessage);
            }
        }
        
        // Log to file
        if (fileLogging) {
            try (FileWriter fw = new FileWriter(logFile, true);
                 PrintWriter pw = new PrintWriter(fw)) {
                
                pw.println(logMessage);
                if (error != null) {
                    error.printStackTrace(pw);
                    pw.println(); // Extra line for readability
                }
                
            } catch (Exception e) {
                // If we can't write to the log file, at least try console
                NeoEssentials.LOGGER.error("Failed to write to tab-errors.log", e);
            }
        }
    }
}
