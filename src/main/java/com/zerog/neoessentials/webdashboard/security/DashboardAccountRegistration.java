package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.JsonObject;

import java.util.UUID;

/**
 * Represents a registered dashboard account linked to a Minecraft account
 * Supports both standalone and Discord-linked registrations
 */
public class DashboardAccountRegistration {
    private final UUID minecraftUuid;
    private final String minecraftUsername;
    private final String dashboardUsername;
    private final String passwordHash;
    private final long registeredAt;

    // Discord link (optional)
    private String discordId;
    private String discordUsername;
    private long discordLinkedAt;

    public DashboardAccountRegistration(UUID minecraftUuid, String minecraftUsername,
                                       String dashboardUsername, String passwordHash, long registeredAt) {
        this.minecraftUuid = minecraftUuid;
        this.minecraftUsername = minecraftUsername;
        this.dashboardUsername = dashboardUsername;
        this.passwordHash = passwordHash;
        this.registeredAt = registeredAt;
    }

    // Getters
    public UUID getMinecraftUuid() { return minecraftUuid; }
    public String getMinecraftUsername() { return minecraftUsername; }
    public String getDashboardUsername() { return dashboardUsername; }
    public String getPasswordHash() { return passwordHash; }
    public long getRegisteredAt() { return registeredAt; }
    public String getDiscordId() { return discordId; }
    public String getDiscordUsername() { return discordUsername; }
    public long getDiscordLinkedAt() { return discordLinkedAt; }

    public boolean isDiscordLinked() {
        return discordId != null && !discordId.isEmpty();
    }

    // Setters for Discord link
    public void setDiscordId(String discordId) { this.discordId = discordId; }
    public void setDiscordUsername(String discordUsername) { this.discordUsername = discordUsername; }
    public void setDiscordLinkedAt(long discordLinkedAt) { this.discordLinkedAt = discordLinkedAt; }

    /**
     * Convert to JSON for storage
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("minecraftUuid", minecraftUuid.toString());
        json.addProperty("minecraftUsername", minecraftUsername);
        json.addProperty("dashboardUsername", dashboardUsername);
        json.addProperty("passwordHash", passwordHash);
        json.addProperty("registeredAt", registeredAt);

        if (discordId != null) {
            json.addProperty("discordId", discordId);
            json.addProperty("discordUsername", discordUsername);
            json.addProperty("discordLinkedAt", discordLinkedAt);
        }

        return json;
    }

    /**
     * Load from JSON
     */
    public static DashboardAccountRegistration fromJson(JsonObject json) {
        try {
            UUID minecraftUuid = UUID.fromString(json.get("minecraftUuid").getAsString());
            String minecraftUsername = json.get("minecraftUsername").getAsString();
            String dashboardUsername = json.get("dashboardUsername").getAsString();
            String passwordHash = json.get("passwordHash").getAsString();
            long registeredAt = json.get("registeredAt").getAsLong();

            DashboardAccountRegistration reg = new DashboardAccountRegistration(
                minecraftUuid, minecraftUsername, dashboardUsername, passwordHash, registeredAt
            );

            // Load Discord link if present
            if (json.has("discordId")) {
                reg.setDiscordId(json.get("discordId").getAsString());
                reg.setDiscordUsername(json.has("discordUsername") ?
                    json.get("discordUsername").getAsString() : null);
                reg.setDiscordLinkedAt(json.has("discordLinkedAt") ?
                    json.get("discordLinkedAt").getAsLong() : 0);
            }

            return reg;
        } catch (Exception e) {
            return null;
        }
    }
}
