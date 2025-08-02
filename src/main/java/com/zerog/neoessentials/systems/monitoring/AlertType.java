package com.zerog.neoessentials.systems.monitoring;

/**
 * Alert types for monitoring system
 */
public enum AlertType {
    MEMORY_HIGH("Memory usage is high"),
    HIGH_MEMORY_USAGE("Memory usage is high"),
    CPU_HIGH("CPU usage is high"),
    DISK_FULL("Disk space is low"),
    THREAD_LEAK("Potential thread leak detected"),
    HIGH_THREAD_COUNT("Thread count is high"),
    GC_PRESSURE("Garbage collection pressure"),
    PERFORMANCE_DEGRADATION("Performance degradation detected"),
    SYSTEM_ERROR("System error occurred"),
    RESOURCE_EXHAUSTION("Resource exhaustion warning"),
    NETWORK_ISSUE("Network connectivity issue"),
    DATABASE_SLOW("Database performance issue"),
    SECURITY_BREACH("Security incident detected"),
    SERVICE_DOWN("Service is down"),
    CUSTOM("Custom alert");
    
    private final String description;
    
    AlertType(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
}
