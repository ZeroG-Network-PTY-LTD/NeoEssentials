package com.zerog.neoessentials.security;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Security Manager for NeoEssentials
 * Handles security monitoring, IP blocking, and threat detection
 */
public class SecurityManager {
    private static SecurityManager instance;
    private boolean running = false;
    private final Map<String, IpSecurityProfile> ipProfiles = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerSecurityProfile> playerProfiles = new ConcurrentHashMap<>();
    private final List<SecurityEvent> securityEvents = new CopyOnWriteArrayList<>();
    private final Set<String> blockedIps = ConcurrentHashMap.newKeySet();

    private SecurityManager() {}

    public static SecurityManager getInstance() {
        if (instance == null) {
            instance = new SecurityManager();
        }
        return instance;
    }

    public void initialize() {
        running = true;
        // Initialize security monitoring
    }

    public boolean isRunning() {
        return running;
    }

    public Map<String, Object> getSecurityStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("blocked_ips", blockedIps.size());
        stats.put("security_events", securityEvents.size());
        stats.put("monitored_players", playerProfiles.size());
        stats.put("system_running", running);
        return stats;
    }

    public List<SecurityEvent> getRecentEvents(int limit) {
        List<SecurityEvent> recent = new ArrayList<>(securityEvents);
        if (recent.size() > limit) {
            return recent.subList(recent.size() - limit, recent.size());
        }
        return recent;
    }

    public PlayerSecurityProfile getPlayerProfile(UUID playerId) {
        return playerProfiles.computeIfAbsent(playerId, k -> new PlayerSecurityProfile(playerId));
    }

    public IpSecurityProfile getIpProfile(String ipAddress) {
        return ipProfiles.computeIfAbsent(ipAddress, k -> new IpSecurityProfile(ipAddress));
    }

    public boolean isIpBlocked(String ipAddress) {
        return blockedIps.contains(ipAddress);
    }

    public void blockIpAddress(String ipAddress, String reason) {
        blockedIps.add(ipAddress);
        SecurityEvent event = new SecurityEvent(
            SecurityEventType.IP_BLOCKED,
            "IP blocked: " + ipAddress + " - " + reason,
            SecurityLevel.WARNING,
            System.currentTimeMillis()
        );
        securityEvents.add(event);
    }

    public void unblockIpAddress(String ipAddress) {
        blockedIps.remove(ipAddress);
        SecurityEvent event = new SecurityEvent(
            SecurityEventType.IP_UNBLOCKED,
            "IP unblocked: " + ipAddress,
            SecurityLevel.INFO,
            System.currentTimeMillis()
        );
        securityEvents.add(event);
    }

    public void logSecurityEvent(SecurityEventType type, String message, SecurityLevel level) {
        SecurityEvent event = new SecurityEvent(type, message, level, System.currentTimeMillis());
        securityEvents.add(event);
        
        // Keep only last 1000 events
        if (securityEvents.size() > 1000) {
            securityEvents.remove(0);
        }
    }
}
