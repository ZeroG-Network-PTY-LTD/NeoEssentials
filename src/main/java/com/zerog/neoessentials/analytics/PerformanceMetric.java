package com.zerog.neoessentials.analytics;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Tracks performance metrics for commands and operations
 */
public class PerformanceMetric {
    private final String name;
    private final AtomicLong executionCount = new AtomicLong(0);
    private final AtomicLong totalExecutionTime = new AtomicLong(0);
    private volatile long minExecutionTime = Long.MAX_VALUE;
    private volatile long maxExecutionTime = 0;
    
    public PerformanceMetric(String name) {
        this.name = name;
    }
    
    public synchronized void recordExecution(long executionTimeMs) {
        executionCount.incrementAndGet();
        totalExecutionTime.addAndGet(executionTimeMs);
        
        if (executionTimeMs < minExecutionTime) {
            minExecutionTime = executionTimeMs;
        }
        if (executionTimeMs > maxExecutionTime) {
            maxExecutionTime = executionTimeMs;
        }
    }
    
    public double getAverageExecutionTime() {
        long count = executionCount.get();
        return count > 0 ? (double) totalExecutionTime.get() / count : 0.0;
    }
    
    // Getters
    public String getName() { return name; }
    public long getExecutionCount() { return executionCount.get(); }
    public long getTotalExecutionTime() { return totalExecutionTime.get(); }
    public long getMinExecutionTime() { return minExecutionTime == Long.MAX_VALUE ? 0 : minExecutionTime; }
    public long getMaxExecutionTime() { return maxExecutionTime; }
    
    @Override
    public String toString() {
        return String.format("PerformanceMetric{name='%s', count=%d, avg=%.2fms, min=%dms, max=%dms}", 
            name, getExecutionCount(), getAverageExecutionTime(), getMinExecutionTime(), getMaxExecutionTime());
    }
}
