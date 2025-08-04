package com.zerog.neoessentials.analytics;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comprehensive analytics report containing all tracked metrics
 */
public class AnalyticsReport {
    private final String reportType;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final LocalDateTime generatedTime;
    
    private Map<String, Long> commandUsageStats;
    private Map<UUID, PlayerSession> activePlayerSessions;
    private Map<String, PerformanceMetric> performanceMetrics;
    private Map<String, FeatureUsageStats> featureUsageStats;
    private List<AnalyticsEvent> eventSummary;
    
    public AnalyticsReport(String reportType, LocalDateTime startTime, LocalDateTime endTime) {
        this.reportType = reportType;
        this.startTime = startTime;
        this.endTime = endTime;
        this.generatedTime = LocalDateTime.now();
    }
    
    /**
     * Generate a formatted string representation of the report
     */
    public String generateFormattedReport() {
        StringBuilder report = new StringBuilder();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                  NEOESSENTIALS ANALYTICS REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append(String.format("Report Type: %s\n", reportType));
        report.append(String.format("Period: %s to %s\n", 
            startTime.format(formatter), endTime.format(formatter)));
        report.append(String.format("Generated: %s\n", generatedTime.format(formatter)));
        report.append("\n");
        
        // Command Usage Statistics
        if (commandUsageStats != null && !commandUsageStats.isEmpty()) {
            report.append("📊 COMMAND USAGE STATISTICS\n");
            report.append("───────────────────────────────────────────────────────────────\n");
            commandUsageStats.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> report.append(String.format("  %-30s %,8d uses\n", 
                    entry.getKey(), entry.getValue())));
            report.append("\n");
        }
        
        // Active Player Sessions
        if (activePlayerSessions != null && !activePlayerSessions.isEmpty()) {
            report.append("👥 ACTIVE PLAYER SESSIONS\n");
            report.append("───────────────────────────────────────────────────────────────\n");
            report.append(String.format("  Total Active Players: %d\n", activePlayerSessions.size()));
            activePlayerSessions.values().stream()
                .limit(10)
                .forEach(session -> report.append(String.format("  %-20s %3d min session, %d commands\n",
                    session.getPlayerName(), session.getSessionDurationMinutes(), session.getCommandsExecuted())));
            report.append("\n");
        }
        
        // Performance Metrics
        if (performanceMetrics != null && !performanceMetrics.isEmpty()) {
            report.append("⚡ PERFORMANCE METRICS\n");
            report.append("───────────────────────────────────────────────────────────────\n");
            performanceMetrics.values().stream()
                .sorted((a, b) -> Long.compare(b.getExecutionCount(), a.getExecutionCount()))
                .limit(10)
                .forEach(metric -> report.append(String.format("  %-25s %,6d calls, avg: %6.2fms\n",
                    metric.getName(), metric.getExecutionCount(), metric.getAverageExecutionTime())));
            report.append("\n");
        }
        
        // Feature Usage Statistics
        if (featureUsageStats != null && !featureUsageStats.isEmpty()) {
            report.append("🔧 FEATURE USAGE STATISTICS\n");
            report.append("───────────────────────────────────────────────────────────────\n");
            featureUsageStats.values().stream()
                .sorted((a, b) -> Long.compare(b.getTotalUsage(), a.getTotalUsage()))
                .limit(10)
                .forEach(feature -> report.append(String.format("  %-25s %,8d uses\n",
                    feature.getFeatureName(), feature.getTotalUsage())));
            report.append("\n");
        }
        
        // Event Summary
        if (eventSummary != null && !eventSummary.isEmpty()) {
            report.append("📋 EVENT SUMMARY\n");
            report.append("───────────────────────────────────────────────────────────────\n");
            report.append(String.format("  Total Events in Period: %,d\n", eventSummary.size()));
            
            // Count events by type
            Map<AnalyticsEvent.EventType, Long> eventCounts = eventSummary.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    AnalyticsEvent::getEventType,
                    java.util.stream.Collectors.counting()));
            
            eventCounts.forEach((type, count) -> 
                report.append(String.format("  %-20s %,8d events\n", type, count)));
            report.append("\n");
        }
        
        report.append("═══════════════════════════════════════════════════════════════\n");
        report.append("                     END OF REPORT\n");
        report.append("═══════════════════════════════════════════════════════════════\n");
        
        return report.toString();
    }
    
    // Setters
    public void setCommandUsageStats(Map<String, Long> commandUsageStats) {
        this.commandUsageStats = commandUsageStats;
    }
    
    public void setActivePlayerSessions(Map<UUID, PlayerSession> activePlayerSessions) {
        this.activePlayerSessions = activePlayerSessions;
    }
    
    public void setPerformanceMetrics(Map<String, PerformanceMetric> performanceMetrics) {
        this.performanceMetrics = performanceMetrics;
    }
    
    public void setFeatureUsageStats(Map<String, FeatureUsageStats> featureUsageStats) {
        this.featureUsageStats = featureUsageStats;
    }
    
    public void setEventSummary(List<AnalyticsEvent> eventSummary) {
        this.eventSummary = eventSummary;
    }
    
    // Getters
    public String getReportType() { return reportType; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public LocalDateTime getGeneratedTime() { return generatedTime; }
    public Map<String, Long> getCommandUsageStats() { return commandUsageStats; }
    public Map<UUID, PlayerSession> getActivePlayerSessions() { return activePlayerSessions; }
    public Map<String, PerformanceMetric> getPerformanceMetrics() { return performanceMetrics; }
    public Map<String, FeatureUsageStats> getFeatureUsageStats() { return featureUsageStats; }
    public List<AnalyticsEvent> getEventSummary() { return eventSummary; }
}
