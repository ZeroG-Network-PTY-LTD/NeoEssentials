package com.zerog.neoessentials.systems.monitoring;

import java.util.List;
import java.util.Map;
import java.util.Collections;

/**
 * Represents optimization results from the monitoring system
 */
public class OptimizationResult {
    private final boolean successful;
    private final String summary;
    private final List<String> actions;
    private final Map<String, Object> metrics;
    private final long executionTime;
    private final int optimizationsApplied;
    private final double performanceImprovement;
    private final long memoryFreed;
    
    public OptimizationResult(boolean successful, String summary, List<String> actions, 
                            Map<String, Object> metrics, long executionTime) {
        this.successful = successful;
        this.summary = summary;
        this.actions = actions;
        this.metrics = metrics;
        this.executionTime = executionTime;
        this.optimizationsApplied = actions != null ? actions.size() : 0;
        this.performanceImprovement = 0.0;
        this.memoryFreed = 0L;
    }
    
    // Alternative constructor for RealTimeServerMonitor
    public OptimizationResult(int optimizationsApplied, String summary, 
                            double performanceImprovement, long memoryFreed) {
        this.successful = optimizationsApplied > 0;
        this.summary = summary;
        this.actions = Collections.singletonList(summary);
        this.metrics = Collections.emptyMap();
        this.executionTime = 0L;
        this.optimizationsApplied = optimizationsApplied;
        this.performanceImprovement = performanceImprovement;
        this.memoryFreed = memoryFreed;
    }
    
    public boolean isSuccessful() { return successful; }
    public String getSummary() { return summary; }
    public List<String> getActions() { return actions; }
    public Map<String, Object> getMetrics() { return metrics; }
    public long getExecutionTime() { return executionTime; }
    
    // Missing methods
    public boolean hasOptimizations() { return optimizationsApplied > 0; }
    public int getOptimizationsApplied() { return optimizationsApplied; }
    public double getPerformanceImprovement() { return performanceImprovement; }
    public long getMemoryFreed() { return memoryFreed; }
}
