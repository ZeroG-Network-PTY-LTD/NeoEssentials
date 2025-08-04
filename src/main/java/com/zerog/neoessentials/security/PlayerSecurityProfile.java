package com.zerog.neoessentials.security;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Player security profile for tracking player-specific security metrics
 */
public class PlayerSecurityProfile {
    private final UUID playerId;
    private final AtomicLong loginCount = new AtomicLong(0);
    private final AtomicLong lastLoginTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong firstSeenTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger suspiciousActivityCount = new AtomicInteger(0);
    private final AtomicInteger commandViolations = new AtomicInteger(0);
    private final AtomicInteger suspicionLevel = new AtomicInteger(0);
    private final List<String> loginAttempts = new CopyOnWriteArrayList<>();
    private final List<String> commandHistory = new CopyOnWriteArrayList<>();
    private final AtomicInteger recentCommandCount = new AtomicInteger(0);
    private boolean flagged = false;
    private String flagReason = "";
    private String lastKnownIp = "";

    public PlayerSecurityProfile(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public long getLoginCount() {
        return loginCount.get();
    }

    public void incrementLoginCount() {
        loginCount.incrementAndGet();
        lastLoginTime.set(System.currentTimeMillis());
    }

    public long getLastLoginTime() {
        return lastLoginTime.get();
    }

    public LocalDateTime getFirstSeen() {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(firstSeenTime.get()), 
            ZoneId.systemDefault()
        );
    }

    public LocalDateTime getLastSeen() {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastLoginTime.get()), 
            ZoneId.systemDefault()
        );
    }

    public String getLastKnownIp() {
        return lastKnownIp;
    }

    public void setLastKnownIp(String ip) {
        this.lastKnownIp = ip;
    }

    public int getSuspicionLevel() {
        return suspicionLevel.get();
    }

    public void setSuspicionLevel(int level) {
        suspicionLevel.set(Math.max(0, Math.min(10, level)));
    }

    public List<String> getLoginAttempts() {
        return new ArrayList<>(loginAttempts);
    }

    public void addLoginAttempt(String details) {
        loginAttempts.add(details);
        if (loginAttempts.size() > 100) {
            loginAttempts.remove(0);
        }
    }

    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }

    public void addCommand(String command) {
        commandHistory.add(command);
        if (commandHistory.size() > 1000) {
            commandHistory.remove(0);
        }
        recentCommandCount.incrementAndGet();
    }

    public int getRecentCommandCount() {
        return recentCommandCount.get();
    }

    public int getSuspiciousActivityCount() {
        return suspiciousActivityCount.get();
    }

    public void incrementSuspiciousActivity() {
        suspiciousActivityCount.incrementAndGet();
        suspicionLevel.incrementAndGet();
    }

    public int getCommandViolations() {
        return commandViolations.get();
    }

    public void incrementCommandViolations() {
        commandViolations.incrementAndGet();
        suspicionLevel.addAndGet(2);
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged, String reason) {
        this.flagged = flagged;
        this.flagReason = reason != null ? reason : "";
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void resetCounters() {
        suspiciousActivityCount.set(0);
        commandViolations.set(0);
        suspicionLevel.set(0);
        recentCommandCount.set(0);
        flagged = false;
        flagReason = "";
        loginAttempts.clear();
        commandHistory.clear();
    }
}
