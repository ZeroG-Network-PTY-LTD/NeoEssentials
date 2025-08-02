package com.zerog.neoessentials.systems.monitoring;

import java.time.LocalDateTime;

/**
 * Represents a performance alert in the monitoring system
 */
public class PerformanceAlert {
    private final AlertType type;
    private final AlertSeverity severity;
    private final String message;
    private final LocalDateTime timestamp;
    private final double value;
    private final double threshold;
    private boolean processed = false;
    
    public PerformanceAlert(AlertType type, AlertSeverity severity, String message, double value, double threshold) {
        this.type = type;
        this.severity = severity;
        this.message = message;
        this.value = value;
        this.threshold = threshold;
        this.timestamp = LocalDateTime.now();
    }
    
    public AlertType getType() { return type; }
    public AlertSeverity getSeverity() { return severity; }
    public String getMessage() { return message; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public double getValue() { return value; }
    public double getThreshold() { return threshold; }
    
    // Missing methods
    public double getMetricValue() { return value; }
    
    public boolean isResolved(SystemSnapshot snapshot) {
        // Simple resolution logic - check if current value is below threshold
        switch (type) {
            case HIGH_MEMORY_USAGE, MEMORY_HIGH:
                return snapshot.memoryUsagePercent < threshold;
            case HIGH_THREAD_COUNT:
                return snapshot.threadCount < threshold;
            default:
                return false;
        }
    }
    
    public boolean isProcessed() {
        return processed;
    }
    
    public void markProcessed() {
        this.processed = true;
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s: %s (%.2f > %.2f)", 
            severity, type, message, value, threshold);
    }
}
