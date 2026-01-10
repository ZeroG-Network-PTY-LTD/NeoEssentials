package com.zerog.neoessentials.chat;

import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ChatManager handles all chat-related configuration, toggles, and logic for NeoEssentials.
 * Responsibilities:
 *   - Loads and provides access to chat config options (join/quit messages, AFK, muting, etc.)
 *   - Provides enable/disable toggles for each chat command
 *   - Exposes permission and mute/ignore logic for chat features
 *   - Can be extended for advanced formatting, localization, and plugin integration
 * Suggestions for future improvements:
 *   - Localize all user-facing messages (see en_us.json)
 *   - Add advanced formatting (hover/click events, color codes)
 *   - Integrate with external chat plugins (e.g., DiscordSRV)
 *   - Add runtime config reload support
 */
public class ChatManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChatManager.class);
    
    // Set of muted commands (from config) - thread-safe
    private final Set<String> mutedCommands;
    // Set of permissions required for chat features (from config) - thread-safe
    private final Set<String> playerChatPermissions;

    /**
     * Returns the set of permissions required to chat (from config).
     */
    public Set<String> getPlayerChatPermissions() {
        return playerChatPermissions;
    }
    // Config toggles and options
    private final boolean sleepIgnoresAfkPlayers;
    private final boolean sleepIgnoresVanishedPlayers;
    private final String afkListName;
    private final boolean broadcastAfkMessage;
    private final boolean deathMessages;
    private final String vanishingItemsPolicy;
    private final String bindingItemsPolicy;
    private final boolean sendInfoAfterDeath;
    private final boolean allowSilentJoinQuit;
    private final String customJoinMessage;
    private final String customQuitMessage;
    private final String customNewUsernameMessage;
    private final boolean useCustomServerFullMessage;
    private final int hideJoinQuitMessagesAbove;
    // Chat format: can be a string (default) or a map for per-group/world - thread-safe
    private final String defaultChatFormat;
    private final java.util.Map<String, String> chatFormatMap;

    // Chat command toggles (from config)
    private final JsonObject commandsConfig;

    /**
     * Constructs a ChatManager from chat and commands config sections.
     * @param chatConfig The chat section of config.json
     * @param commandsConfig The commands section of config.json
     */
    public ChatManager(JsonObject chatConfig, JsonObject commandsConfig) {
        this.mutedCommands = toSet(chatConfig, "muteCommands");
        this.playerChatPermissions = toSet(chatConfig, "playerChatPermissions");
        this.sleepIgnoresAfkPlayers = chatConfig.has("sleepIgnoresAfkPlayers") && chatConfig.get("sleepIgnoresAfkPlayers").getAsBoolean();
        this.sleepIgnoresVanishedPlayers = chatConfig.has("sleepIgnoresVanishedPlayers") && chatConfig.get("sleepIgnoresVanishedPlayers").getAsBoolean();
        this.afkListName = chatConfig.has("afkListName") ? chatConfig.get("afkListName").getAsString() : "none";
        this.broadcastAfkMessage = chatConfig.has("broadcastAfkMessage") && chatConfig.get("broadcastAfkMessage").getAsBoolean();
        this.deathMessages = chatConfig.has("deathMessages") && chatConfig.get("deathMessages").getAsBoolean();
        this.vanishingItemsPolicy = chatConfig.has("vanishingItemsPolicy") ? chatConfig.get("vanishingItemsPolicy").getAsString() : "keep";
        this.bindingItemsPolicy = chatConfig.has("bindingItemsPolicy") ? chatConfig.get("bindingItemsPolicy").getAsString() : "keep";
        this.sendInfoAfterDeath = chatConfig.has("sendInfoAfterDeath") && chatConfig.get("sendInfoAfterDeath").getAsBoolean();
        this.allowSilentJoinQuit = chatConfig.has("allowSilentJoinQuit") && chatConfig.get("allowSilentJoinQuit").getAsBoolean();
        this.customJoinMessage = chatConfig.has("customJoinMessage") ? chatConfig.get("customJoinMessage").getAsString() : "none";
        this.customQuitMessage = chatConfig.has("customQuitMessage") ? chatConfig.get("customQuitMessage").getAsString() : "none";
        this.customNewUsernameMessage = chatConfig.has("customNewUsernameMessage") ? chatConfig.get("customNewUsernameMessage").getAsString() : "none";
        this.useCustomServerFullMessage = chatConfig.has("useCustomServerFullMessage") && chatConfig.get("useCustomServerFullMessage").getAsBoolean();
        this.hideJoinQuitMessagesAbove = chatConfig.has("hideJoinQuitMessagesAbove") ? chatConfig.get("hideJoinQuitMessagesAbove").getAsInt() : -1;
        // Support chat-format as string or object
        if (chatConfig.has("chat-format")) {
            if (chatConfig.get("chat-format").isJsonObject()) {
                JsonObject obj = chatConfig.getAsJsonObject("chat-format");
                java.util.Map<String, String> map = new ConcurrentHashMap<>();
                String def = null;
                for (String key : obj.keySet()) {
                    if (key.equalsIgnoreCase("default")) {
                        def = obj.get(key).getAsString();
                    } else {
                        map.put(key, obj.get(key).getAsString());
                    }
                }
                this.defaultChatFormat = def != null ? def : "{neoessentials_displayname}: {MESSAGE}";
                this.chatFormatMap = map;
                LOGGER.info("Loaded chat-format (object): default=[{}], map size={}", this.defaultChatFormat, map.size());
            } else {
                this.defaultChatFormat = chatConfig.get("chat-format").getAsString();
                this.chatFormatMap = java.util.Collections.emptyMap();
                LOGGER.info("Loaded chat-format (string): [{}]", this.defaultChatFormat);
            }
        } else {
            this.defaultChatFormat = "{neoessentials_displayname}: {MESSAGE}";
            this.chatFormatMap = java.util.Collections.emptyMap();
            LOGGER.info("No chat-format in config, using default: [{}]", this.defaultChatFormat);
        }
        this.commandsConfig = commandsConfig;
    }

    /**
     * Converts a config array to a thread-safe Set<String>.
     */
    private Set<String> toSet(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return Collections.emptySet();
        Set<String> set = ConcurrentHashMap.newKeySet();
        obj.getAsJsonArray(key).forEach(e -> set.add(e.getAsString()));
        return set;
    }

    /**
     * Checks if a command is muted for chat.
     */
    public boolean isCommandMuted(String command) {
        return mutedCommands.contains(command);
    }

    /**
     * Checks if a player has a specific chat permission.
     */
    @SuppressWarnings("unused") // May be used by external systems
    public boolean hasChatPermission(String permission) {
        return playerChatPermissions.contains(permission);
    }

    // Accessors for chat config options
    @SuppressWarnings("unused") // May be used by external systems
    public boolean shouldSleepIgnoreAfk() { return sleepIgnoresAfkPlayers; }
    @SuppressWarnings("unused") // May be used by external systems
    public boolean shouldSleepIgnoreVanished() { return sleepIgnoresVanishedPlayers; }
    @SuppressWarnings("unused") // May be used by external systems
    public String getAfkListName() { return afkListName; }
    @SuppressWarnings("unused") // May be used by external systems
    public boolean shouldBroadcastAfk() { return broadcastAfkMessage; }
    @SuppressWarnings("unused") // May be used by external systems
    public boolean showDeathMessages() { return deathMessages; }
    @SuppressWarnings("unused") // May be used by external systems
    public String getVanishingItemsPolicy() { return vanishingItemsPolicy; }
    @SuppressWarnings("unused") // May be used by external systems
    public String getBindingItemsPolicy() { return bindingItemsPolicy; }
    @SuppressWarnings("unused") // May be used by external systems
    public boolean shouldSendInfoAfterDeath() { return sendInfoAfterDeath; }
    @SuppressWarnings("unused") // May be used by external systems
    public boolean allowSilentJoinQuit() { return allowSilentJoinQuit; }
    @SuppressWarnings("unused") // May be used by external systems
    public String getCustomJoinMessage() { return customJoinMessage; }
    @SuppressWarnings("unused") // May be used by external systems
    public String getCustomQuitMessage() { return customQuitMessage; }
    @SuppressWarnings("unused") // May be used by external systems
    public String getCustomNewUsernameMessage() { return customNewUsernameMessage; }
    @SuppressWarnings("unused") // May be used by external systems
    public boolean useCustomServerFullMessage() { return useCustomServerFullMessage; }
    @SuppressWarnings("unused") // May be used by external systems
    public int getHideJoinQuitMessagesAbove() { return hideJoinQuitMessagesAbove; }

    /**
     * Returns the chat format for a given group and/or world.
     * Priority: templates (if enabled) > group+world > group > world > default
     * @param group Player's primary group (null if unknown)
     * @param world Player's world (null if unknown)
     */
    public String getChatFormat(String group, String world) {
        // Phase 3: Check if format templates are enabled
        try {
            com.google.gson.JsonObject chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("formatTemplates")) {
                com.google.gson.JsonObject templates = chatConfig.getAsJsonObject("formatTemplates");
                if (templates.has("enabled") && templates.get("enabled").getAsBoolean()) {
                    String activeTemplate = templates.has("activeTemplate") ?
                        templates.get("activeTemplate").getAsString() : "default";

                    if (templates.has("templates")) {
                        com.google.gson.JsonObject templateMap = templates.getAsJsonObject("templates");
                        if (templateMap.has(activeTemplate)) {
                            return templateMap.get(activeTemplate).getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignore, fall through to normal format selection
        }

        // Normal format selection
        // Try group+world
        if (group != null && world != null) {
            String key = "group:" + group.toLowerCase() + ":world:" + world.toLowerCase();
            if (chatFormatMap.containsKey(key)) return chatFormatMap.get(key);
        }
        // Try group
        if (group != null) {
            String key = "group:" + group.toLowerCase();
            if (chatFormatMap.containsKey(key)) return chatFormatMap.get(key);
        }
        // Try world
        if (world != null) {
            String key = "world:" + world.toLowerCase();
            if (chatFormatMap.containsKey(key)) return chatFormatMap.get(key);
        }
        // Fallback
        return defaultChatFormat;
    }

    /**
     * Returns the default chat format (for config migration/compatibility).
     */
    @SuppressWarnings("unused") // May be used by external systems
    public String getDefaultChatFormat() {
        return defaultChatFormat;
    }

    // Chat command enable/disable checks
    @SuppressWarnings("BooleanMethodIsAlwaysInverted") // Used correctly with ! operator
    public boolean isAfkEnabled() { return isCommandEnabled("afk"); }
    public boolean isIgnoreEnabled() { return isCommandEnabled("ignore"); }
    public boolean isMsgEnabled() { return isCommandEnabled("msg"); }
    public boolean isMsgToggleEnabled() { return isCommandEnabled("msgtoggle"); }
    public boolean isMuteEnabled() { return isCommandEnabled("mute"); }
    public boolean isMuteListEnabled() { return isCommandEnabled("mutelist"); }
    public boolean isReplyEnabled() { return isCommandEnabled("reply"); }
    public boolean isSocialSpyEnabled() { return isCommandEnabled("socialspy"); }
    public boolean isUnignoreEnabled() { return isCommandEnabled("unignore"); }
    public boolean isUnmuteEnabled() { return isCommandEnabled("unmute"); }

    /**
     * Checks if a chat command is enabled in config.
     */
    private boolean isCommandEnabled(String command) {
        return commandsConfig != null && commandsConfig.has(command) && commandsConfig.get(command).getAsBoolean();
    }

    // Add more chat logic and improvements here as needed
}
