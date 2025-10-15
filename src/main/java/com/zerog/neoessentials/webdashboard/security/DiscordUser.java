package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a Discord user with their linked Minecraft account and role IDs
 * 
 * Note: discordRoles contains role IDs (not names) for stable role mapping
 */
public class DiscordUser {
    private final String discordId;
    private final String discordUsername;
    private final String minecraftUsername;
    private final String minecraftUuid;
    private final List<String> discordRoles; // Role IDs, not names
    private final boolean isLinked;
    
    public DiscordUser(String discordId, String discordUsername, String minecraftUsername, 
                      String minecraftUuid, List<String> discordRoles) {
        this.discordId = discordId;
        this.discordUsername = discordUsername;
        this.minecraftUsername = minecraftUsername;
        this.minecraftUuid = minecraftUuid;
        this.discordRoles = discordRoles != null ? new ArrayList<>(discordRoles) : new ArrayList<>();
        this.isLinked = discordId != null && !discordId.isEmpty() && 
                       minecraftUsername != null && !minecraftUsername.isEmpty();
    }
    
    // Getters
    public String getDiscordId() {
        return discordId;
    }
    
    public String getDiscordUsername() {
        return discordUsername;
    }
    
    public String getMinecraftUsername() {
        return minecraftUsername;
    }
    
    public String getMinecraftUuid() {
        return minecraftUuid;
    }
    
    /**
     * Get Discord role IDs (not names)
     */
    public List<String> getDiscordRoles() {
        return new ArrayList<>(discordRoles);
    }
    
    public boolean isLinked() {
        return isLinked;
    }
    
    /**
     * Check if user has a specific Discord role ID
     * 
     * @param roleId Discord role ID (not name)
     * @return true if user has the role
     */
    public boolean hasRole(String roleId) {
        if (roleId == null || discordRoles == null) {
            return false;
        }
        // Direct comparison (role IDs are case-sensitive numeric strings)
        return discordRoles.contains(roleId);
    }
    
    /**
     * Check if user has any of the specified Discord role IDs
     * 
     * @param roleIds List of Discord role IDs (not names)
     * @return true if user has any of the roles
     */
    public boolean hasAnyRole(List<String> roleIds) {
        if (roleIds == null || roleIds.isEmpty() || discordRoles == null) {
            return false;
        }
        return roleIds.stream()
            .anyMatch(this::hasRole);
    }
    
    /**
     * Convert to JSON object
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("discordId", discordId);
        json.addProperty("discordUsername", discordUsername);
        json.addProperty("minecraftUsername", minecraftUsername);
        json.addProperty("minecraftUuid", minecraftUuid);
        json.addProperty("isLinked", isLinked);
        
        JsonArray rolesArray = new JsonArray();
        for (String role : discordRoles) {
            rolesArray.add(role);
        }
        json.add("discordRoles", rolesArray);
        
        return json;
    }
    
    @Override
    public String toString() {
        return String.format("DiscordUser{discord=%s, minecraft=%s, roles=%s, linked=%s}",
            discordUsername, minecraftUsername, discordRoles, isLinked);
    }
}
