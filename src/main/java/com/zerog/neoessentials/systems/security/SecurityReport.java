package com.zerog.neoessentials.systems.security;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Security report generation
 */
public class SecurityReport {
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime generatedAt;
    private final Map<String, Object> metrics;
    private final List<SecurityEvent> events;
    private final List<String> recommendations;
    
    public SecurityReport(LocalDateTime startTime, LocalDateTime endTime, 
                         Map<String, Object> metrics, List<SecurityEvent> events, 
                         List<String> recommendations) {
        this.startTime = startTime;
        this.endTime = endTime;
        this.generatedAt = LocalDateTime.now();
        this.metrics = metrics;
        this.events = events;
        this.recommendations = recommendations;
    }
    
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public Map<String, Object> getMetrics() { return metrics; }
    public List<SecurityEvent> getEvents() { return events; }
    public List<String> getRecommendations() { return recommendations; }
    
    /**
     * Get total number of events in this report
     */
    public int getTotalEvents() {
        return events != null ? events.size() : 0;
    }
}
