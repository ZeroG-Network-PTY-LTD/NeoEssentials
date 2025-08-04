package com.zerog.neoessentials.analytics;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

/**
 * Represents a player session for analytics tracking
 */
public class PlayerSession {
    private final UUID playerUUID;
    private final String playerName;
    private final LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private long commandsExecuted = 0;
    
    public PlayerSession(UUID playerUUID, String playerName, LocalDateTime loginTime) {
        this.playerUUID = playerUUID;
        this.playerName = playerName;
        this.loginTime = loginTime;
    }
    
    public void setLogoutTime(LocalDateTime logoutTime) {
        this.logoutTime = logoutTime;
    }
    
    public void incrementCommandsExecuted() {
        this.commandsExecuted++;
    }
    
    public long getSessionDurationMinutes() {
        LocalDateTime endTime = logoutTime != null ? logoutTime : LocalDateTime.now();
        return ChronoUnit.MINUTES.between(loginTime, endTime);
    }
    
    public boolean isActive() {
        return logoutTime == null;
    }
    
    // Getters
    public UUID getPlayerUUID() { return playerUUID; }
    public String getPlayerName() { return playerName; }
    public LocalDateTime getLoginTime() { return loginTime; }
    public LocalDateTime getLogoutTime() { return logoutTime; }
    public long getCommandsExecuted() { return commandsExecuted; }
    
    @Override
    public String toString() {
        return String.format("PlayerSession{player='%s', login=%s, duration=%d min, commands=%d}", 
            playerName, loginTime, getSessionDurationMinutes(), commandsExecuted);
    }
}
