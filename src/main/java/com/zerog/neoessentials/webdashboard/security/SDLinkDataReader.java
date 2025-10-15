package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Reader for Simple Discord Link data files
 * Parses verifiedaccounts.json (JSONL format) to get Minecraft UUID <-> Discord ID mappings
 */
public class SDLinkDataReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SDLinkDataReader.class);
    private static final Gson GSON = new Gson();
    
    // Cache of verified accounts: Minecraft UUID -> Discord ID
    private final Map<UUID, String> uuidToDiscordId = new HashMap<>();
    
    // Cache of verified accounts: Discord ID -> Minecraft UUID
    private final Map<String, UUID> discordIdToUuid = new HashMap<>();
    
    // Cache of verified accounts: Minecraft username -> Verified account data
    private final Map<String, VerifiedAccount> usernameToAccount = new HashMap<>();
    
    private final Path verifiedAccountsPath;
    private long lastModified = 0;
    
    /**
     * Create a new SDLink data reader
     * @param serverDirectory Path to the Minecraft server root directory
     */
    public SDLinkDataReader(Path serverDirectory) {
        this.verifiedAccountsPath = serverDirectory.resolve("sdlinkstorage").resolve("verifiedaccounts.json");
        LOGGER.debug("SDLink data reader initialized for: {}", verifiedAccountsPath);
    }
    
    /**
     * Load or reload verified accounts from the file
     * Only reloads if the file has been modified since last load
     */
    public synchronized void loadVerifiedAccounts() {
        if (!Files.exists(verifiedAccountsPath)) {
            LOGGER.warn("SDLink verifiedaccounts.json not found at: {}", verifiedAccountsPath);
            return;
        }
        
        try {
            // Check if file has been modified
            long currentModified = Files.getLastModifiedTime(verifiedAccountsPath).toMillis();
            if (currentModified == lastModified && !uuidToDiscordId.isEmpty()) {
                LOGGER.debug("Verified accounts already loaded and file not modified, skipping reload");
                return;
            }
            
            LOGGER.info("Loading verified accounts from: {}", verifiedAccountsPath);
            
            // Clear existing caches
            uuidToDiscordId.clear();
            discordIdToUuid.clear();
            usernameToAccount.clear();
            
            int lineCount = 0;
            int accountCount = 0;
            
            // Read JSONL file (one JSON object per line)
            try (BufferedReader reader = Files.newBufferedReader(verifiedAccountsPath)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    lineCount++;
                    line = line.trim();
                    
                    // Skip empty lines
                    if (line.isEmpty()) {
                        continue;
                    }
                    
                    try {
                        JsonObject json = GSON.fromJson(line, JsonObject.class);
                        
                        // First line is schema version
                        if (json.has("schemaVersion")) {
                            LOGGER.debug("Schema version: {}", json.get("schemaVersion").getAsString());
                            continue;
                        }
                        
                        // Parse verified account
                        VerifiedAccount account = parseVerifiedAccount(json);
                        if (account != null && account.isValid()) {
                            uuidToDiscordId.put(account.uuid, account.discordId);
                            discordIdToUuid.put(account.discordId, account.uuid);
                            usernameToAccount.put(account.inGameName.toLowerCase(), account);
                            accountCount++;
                        }
                        
                    } catch (JsonSyntaxException e) {
                        LOGGER.warn("Invalid JSON on line {}: {}", lineCount, e.getMessage());
                    }
                }
            }
            
            lastModified = currentModified;
            LOGGER.info("Loaded {} verified accounts from {} lines", accountCount, lineCount);
            
        } catch (IOException e) {
            LOGGER.error("Failed to load verified accounts: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Parse a verified account from JSON
     * Note: SDLink uses "discordID" with capital "ID" in the JSON file
     */
    private VerifiedAccount parseVerifiedAccount(JsonObject json) {
        try {
            String uuidStr = json.get("uuid").getAsString();
            String username = json.get("username").getAsString();
            String inGameName = json.get("inGameName").getAsString();
            // SDLink uses "discordID" with capital ID, not "discordId"
            String discordId = json.get("discordID").getAsString();
            boolean isOffline = json.has("isOffline") && json.get("isOffline").getAsBoolean();
            
            UUID uuid = UUID.fromString(uuidStr);
            
            return new VerifiedAccount(uuid, username, inGameName, discordId, isOffline);
            
        } catch (Exception e) {
            LOGGER.warn("Failed to parse verified account on line, skipping: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get Discord ID for a Minecraft UUID
     */
    public String getDiscordId(UUID minecraftUuid) {
        loadVerifiedAccounts(); // Auto-reload if file changed
        return uuidToDiscordId.get(minecraftUuid);
    }
    
    /**
     * Get Minecraft UUID for a Discord ID
     */
    public UUID getMinecraftUuid(String discordId) {
        loadVerifiedAccounts(); // Auto-reload if file changed
        return discordIdToUuid.get(discordId);
    }
    
    /**
     * Get verified account by Minecraft username
     */
    public VerifiedAccount getAccountByUsername(String minecraftUsername) {
        loadVerifiedAccounts(); // Auto-reload if file changed
        return usernameToAccount.get(minecraftUsername.toLowerCase());
    }
    
    /**
     * Get verified account by Minecraft UUID
     */
    public VerifiedAccount getAccountByUuid(UUID minecraftUuid) {
        loadVerifiedAccounts(); // Auto-reload if file changed
        String discordId = uuidToDiscordId.get(minecraftUuid);
        if (discordId == null) {
            return null;
        }
        
        // Find account by discord ID
        for (VerifiedAccount account : usernameToAccount.values()) {
            if (account.discordId.equals(discordId)) {
                return account;
            }
        }
        return null;
    }
    
    /**
     * Check if a Minecraft UUID is linked to Discord
     */
    public boolean isLinked(UUID minecraftUuid) {
        return getDiscordId(minecraftUuid) != null;
    }
    
    /**
     * Check if a Minecraft username is linked to Discord
     */
    public boolean isLinked(String minecraftUsername) {
        return getAccountByUsername(minecraftUsername) != null;
    }
    
    /**
     * Get count of verified accounts
     */
    public int getVerifiedAccountCount() {
        loadVerifiedAccounts();
        return uuidToDiscordId.size();
    }
    
    /**
     * Represents a verified account from SDLink
     */
    public static class VerifiedAccount {
        public final UUID uuid;
        public final String username;
        public final String inGameName;
        public final String discordId;
        public final boolean isOffline;
        
        public VerifiedAccount(UUID uuid, String username, String inGameName, String discordId, boolean isOffline) {
            this.uuid = uuid;
            this.username = username;
            this.inGameName = inGameName;
            this.discordId = discordId;
            this.isOffline = isOffline;
        }
        
        public boolean isValid() {
            return uuid != null && discordId != null && !discordId.isEmpty() && 
                   inGameName != null && !inGameName.isEmpty();
        }
        
        @Override
        public String toString() {
            return String.format("VerifiedAccount{minecraft=%s, discord=%s, offline=%s}", 
                inGameName, discordId, isOffline);
        }
    }
}
