package com.zerog.neoessentials.webdashboard.security;

import java.util.List;

/**
 * Represents a Discord user linked to a Minecraft account
 * Used for Discord OAuth authentication
 */
public class DiscordUser {
    private final String discordId;
    private final String discordUsername;
    private final String minecraftUsername;
    private final String minecraftUuid;
    private final List<String> discordRoles;

    public DiscordUser(String discordId, String discordUsername, String minecraftUsername,
                       String minecraftUuid, List<String> discordRoles) {
        this.discordId = discordId;
        this.discordUsername = discordUsername;
        this.minecraftUsername = minecraftUsername;
        this.minecraftUuid = minecraftUuid;
        this.discordRoles = discordRoles;
    }

    // Getters
    public String getDiscordId() { return discordId; }
    public String getDiscordUsername() { return discordUsername; }
    public String getMinecraftUsername() { return minecraftUsername; }
    public String getMinecraftUuid() { return minecraftUuid; }
    public List<String> getDiscordRoles() { return discordRoles; }
    
    public boolean isLinked() {
        return discordId != null && !discordId.isEmpty() &&
               minecraftUuid != null && !minecraftUuid.isEmpty();
    }
}
