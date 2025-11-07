package com.zerog.neoessentials.webdashboard.security;

import com.google.gson.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Reads verified accounts from SDLink's verifiedaccounts.json file
 */
public class SDLinkDataReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SDLinkDataReader.class);
    private final Path serverDirectory;
    
    public SDLinkDataReader(Path serverDirectory) {
        this.serverDirectory = serverDirectory;
    }
    
    /**
     * Get Discord ID for a Minecraft UUID from SDLink's verified accounts file
     */
    public String getDiscordId(UUID minecraftUuid) {
        try {
            File dataFile = serverDirectory.resolve("config/sdlink/verifiedaccounts.json").toFile();
            if (!dataFile.exists()) {
                return null;
            }
            
            try (FileReader reader = new FileReader(dataFile)) {
                JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
                
                // SDLink stores verified accounts as Discord ID -> Minecraft UUID mapping
                // We need to reverse search for our UUID
                for (String discordId : data.keySet()) {
                    JsonElement element = data.get(discordId);
                    if (element.isJsonObject()) {
                        JsonObject account = element.getAsJsonObject();
                        if (account.has("minecraftUuid")) {
                            String storedUuid = account.get("minecraftUuid").getAsString();
                            if (storedUuid.equals(minecraftUuid.toString())) {
                                return discordId;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error reading SDLink data: {}", e.getMessage());
        }
        
        return null;
    }
}
