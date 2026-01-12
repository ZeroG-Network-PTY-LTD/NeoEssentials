package com.zerog.neoessentials.webdashboard.cloud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages cloud provider OAuth tokens and authentication status
 * Supports Google Drive, Dropbox, and other cloud storage providers
 * <p>
 * Future implementation will include:
 * - OAuth 2.0 token management
 * - Token refresh logic
 * - Provider-specific API integration
 * - File upload/download to cloud storage
 */
public class CloudProviderManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CloudProviderManager.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File TOKENS_FILE = new File("config/neoessentials/cloud_tokens.json");

    private static CloudProviderManager INSTANCE;
    private final Map<String, CloudProviderToken> tokens = new HashMap<>();

    private CloudProviderManager() {
        loadTokens();
    }

    public static CloudProviderManager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new CloudProviderManager();
        }
        return INSTANCE;
    }

    /**
     * Check if a cloud provider is linked (has valid OAuth token)
     */
    public boolean isProviderLinked(String providerName) {
        CloudProviderToken token = tokens.get(providerName.toLowerCase());
        if (token == null) {
            return false;
        }

        // Check if token is expired
        if (token.isExpired()) {
            LOGGER.debug("Token for {} is expired", providerName);
            return false;
        }

        return true;
    }

    /**
     * Get OAuth token for a provider
     * This method is reserved for future cloud API integration
     */
    @SuppressWarnings("unused")
    public String getAccessToken(String providerName) {
        CloudProviderToken token = tokens.get(providerName.toLowerCase());
        if (token == null || token.isExpired()) {
            return null;
        }
        return token.getAccessToken();
    }

    /**
     * Store OAuth token for a provider
     */
    public void storeToken(String providerName, String accessToken, String refreshToken, long expiresIn) {
        CloudProviderToken token = new CloudProviderToken(
            providerName,
            accessToken,
            refreshToken,
            System.currentTimeMillis() + (expiresIn * 1000)
        );

        tokens.put(providerName.toLowerCase(), token);
        saveTokens();

        LOGGER.info("Stored OAuth token for provider: {}", providerName);
    }

    /**
     * Remove OAuth token for a provider (unlink)
     */
    public void removeToken(String providerName) {
        tokens.remove(providerName.toLowerCase());
        saveTokens();

        LOGGER.info("Removed OAuth token for provider: {}", providerName);
    }

    /**
     * Load tokens from file
     */
    private void loadTokens() {
        if (!TOKENS_FILE.exists()) {
            LOGGER.debug("Cloud tokens file does not exist, starting with empty tokens");
            return;
        }

        try (FileReader reader = new FileReader(TOKENS_FILE, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            if (root != null && root.has("tokens")) {
                JsonObject tokensObj = root.getAsJsonObject("tokens");

                for (String providerName : tokensObj.keySet()) {
                    JsonObject tokenObj = tokensObj.getAsJsonObject(providerName);

                    CloudProviderToken token = new CloudProviderToken(
                        tokenObj.get("provider").getAsString(),
                        tokenObj.get("accessToken").getAsString(),
                        tokenObj.has("refreshToken") ? tokenObj.get("refreshToken").getAsString() : null,
                        tokenObj.get("expiresAt").getAsLong()
                    );

                    tokens.put(providerName.toLowerCase(), token);
                }
            }

            LOGGER.info("Loaded {} cloud provider token(s)", tokens.size());
        } catch (Exception e) {
            LOGGER.error("Failed to load cloud tokens", e);
        }
    }

    /**
     * Save tokens to file
     */
    private void saveTokens() {
        try {
            // Ensure parent directory exists
            File parentDir = TOKENS_FILE.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                if (!parentDir.mkdirs()) {
                    LOGGER.error("Failed to create directory for cloud tokens");
                    return;
                }
            }

            JsonObject root = new JsonObject();
            JsonObject tokensObj = new JsonObject();

            for (Map.Entry<String, CloudProviderToken> entry : tokens.entrySet()) {
                CloudProviderToken token = entry.getValue();
                JsonObject tokenObj = new JsonObject();

                tokenObj.addProperty("provider", token.getProviderName());
                tokenObj.addProperty("accessToken", token.getAccessToken());
                if (token.getRefreshToken() != null) {
                    tokenObj.addProperty("refreshToken", token.getRefreshToken());
                }
                tokenObj.addProperty("expiresAt", token.getExpiresAt());

                tokensObj.add(entry.getKey(), tokenObj);
            }

            root.add("tokens", tokensObj);
            root.addProperty("_version", 1);

            try (FileWriter writer = new FileWriter(TOKENS_FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            LOGGER.debug("Saved {} cloud provider token(s)", tokens.size());
        } catch (IOException e) {
            LOGGER.error("Failed to save cloud tokens", e);
        }
    }

    /**
     * Inner class representing a cloud provider OAuth token
     */
    private static class CloudProviderToken {
        private final String providerName;
        private final String accessToken;
        private final String refreshToken;
        private final long expiresAt;

        public CloudProviderToken(String providerName, String accessToken, String refreshToken, long expiresAt) {
            this.providerName = providerName;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresAt = expiresAt;
        }

        public String getProviderName() {
            return providerName;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() >= expiresAt;
        }
    }
}

