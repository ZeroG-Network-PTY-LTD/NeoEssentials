package com.zerog.neoessentials.systems.notifications;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.Map;
import java.util.HashMap;

/**
 * Statistics for notification system
 */
public class NotificationStats {
    private final AtomicLong totalNotifications = new AtomicLong(0);
    private final AtomicLong sentNotifications = new AtomicLong(0);
    private final AtomicLong failedNotifications = new AtomicLong(0);
    private final AtomicLong readNotifications = new AtomicLong(0);
    private final LocalDateTime lastReset;
    private final LocalDateTime startTime;
    private int queueSize = 0;
    private int historySize = 0;
    
    public NotificationStats() {
        this.lastReset = LocalDateTime.now();
        this.startTime = LocalDateTime.now();
    }
    
    // Constructor for NotificationManager compatibility
    public NotificationStats(boolean enabled, int totalSent, int totalFailed, 
                           Map<NotificationType, Integer> channelStats, 
                           int queueSize, long uptime, LocalDateTime lastActivity) {
        this.totalNotifications.set(totalSent + totalFailed);
        this.sentNotifications.set(totalSent);
        this.failedNotifications.set(totalFailed);
        this.queueSize = queueSize;
        this.historySize = totalSent + totalFailed;
        this.lastReset = lastActivity;
        this.startTime = LocalDateTime.now().minusSeconds(uptime / 1000);
    }
    
    public void incrementTotal() { totalNotifications.incrementAndGet(); }
    public void incrementSent() { sentNotifications.incrementAndGet(); }
    public void incrementFailed() { failedNotifications.incrementAndGet(); }
    public void incrementRead() { readNotifications.incrementAndGet(); }
    
    public long getTotalNotifications() { return totalNotifications.get(); }
    public long getSentNotifications() { return sentNotifications.get(); }
    public long getFailedNotifications() { return failedNotifications.get(); }
    public long getReadNotifications() { return readNotifications.get(); }
    public LocalDateTime getLastReset() { return lastReset; }
    
    public double getSuccessRate() {
        long total = totalNotifications.get();
        return total > 0 ? (double) sentNotifications.get() / total : 0.0;
    }
    
    public double getReadRate() {
        long sent = sentNotifications.get();
        return sent > 0 ? (double) readNotifications.get() / sent : 0.0;
    }
    
    // Additional methods for compatibility
    public int getQueueSize() { return queueSize; }
    public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
    
    public int getHistorySize() { return historySize; }
    public void setHistorySize(int historySize) { this.historySize = historySize; }
    
    public long getUptime() {
        return Duration.between(startTime, LocalDateTime.now()).toSeconds() * 1000;
    }
}
