package com.zerog.neoessentials.security;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * IP security profile for tracking IP-specific security metrics
 */
public class IpSecurityProfile {
    private final String ipAddress;
    private final AtomicLong connectionCount = new AtomicLong(0);
    private final AtomicLong lastConnectionTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicLong firstSeenTime = new AtomicLong(System.currentTimeMillis());
    private final AtomicInteger failedLoginAttempts = new AtomicInteger(0);
    private final AtomicInteger suspiciousActivityCount = new AtomicInteger(0);
    private final AtomicInteger recentFailureCount = new AtomicInteger(0);
    private final AtomicInteger suspicionLevel = new AtomicInteger(0);
    private final List<String> loginAttempts = new CopyOnWriteArrayList<>();
    private final List<String> webRequests = new CopyOnWriteArrayList<>();
    private boolean blocked = false;
    private String blockReason = "";
    private long blockTime = 0;
    private boolean botLike = false;
    private String geolocation = null;

    public IpSecurityProfile(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public long getConnectionCount() {
        return connectionCount.get();
    }

    public void incrementConnectionCount() {
        connectionCount.incrementAndGet();
        lastConnectionTime.set(System.currentTimeMillis());
    }

    public long getLastConnectionTime() {
        return lastConnectionTime.get();
    }

    public LocalDateTime getFirstSeen() {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(firstSeenTime.get()), 
            ZoneId.systemDefault()
        );
    }

    public LocalDateTime getLastSeen() {
        return LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(lastConnectionTime.get()), 
            ZoneId.systemDefault()
        );
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts.get();
    }

    public void incrementFailedLoginAttempts() {
        failedLoginAttempts.incrementAndGet();
        recentFailureCount.incrementAndGet();
    }

    public void resetFailedLoginAttempts() {
        failedLoginAttempts.set(0);
        recentFailureCount.set(0);
    }

    public int getRecentFailureCount() {
        return recentFailureCount.get();
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

    public List<String> getWebRequests() {
        return new ArrayList<>(webRequests);
    }

    public void addWebRequest(String request) {
        webRequests.add(request);
        if (webRequests.size() > 500) {
            webRequests.remove(0);
        }
    }

    public boolean isBotLike() {
        return botLike;
    }

    public void setBotLike(boolean botLike) {
        this.botLike = botLike;
    }

    public String getGeolocation() {
        return geolocation;
    }

    public void setGeolocation(String geolocation) {
        this.geolocation = geolocation;
    }

    public int getSuspiciousActivityCount() {
        return suspiciousActivityCount.get();
    }

    public void incrementSuspiciousActivity() {
        suspiciousActivityCount.incrementAndGet();
        suspicionLevel.incrementAndGet();
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked, String reason) {
        this.blocked = blocked;
        this.blockReason = reason != null ? reason : "";
        this.blockTime = blocked ? System.currentTimeMillis() : 0;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public long getBlockTime() {
        return blockTime;
    }

    public void resetCounters() {
        failedLoginAttempts.set(0);
        suspiciousActivityCount.set(0);
        recentFailureCount.set(0);
        suspicionLevel.set(0);
        blocked = false;
        blockReason = "";
        blockTime = 0;
        loginAttempts.clear();
        webRequests.clear();
    }
}
