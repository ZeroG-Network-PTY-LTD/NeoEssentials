package com.zerog.neoessentials.chat;

import com.google.gson.JsonObject;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * ChatManager handles all chat-related configuration, toggles, and logic for NeoEssentials.
 *
 * Responsibilities:
 *   - Loads and provides access to chat config options (join/quit messages, AFK, muting, etc.)
 *   - Provides enable/disable toggles for each chat command
 *   - Exposes permission and mute/ignore logic for chat features
 *   - Can be extended for advanced formatting, localization, and plugin integration
 *
 * Suggestions for future improvements:
 *   - Localize all user-facing messages (see en_us.json)
 *   - Add advanced formatting (hover/click events, color codes)
 *   - Integrate with external chat plugins (e.g., DiscordSRV)
 *   - Add runtime config reload support
 */
public class ChatManager {
    // Set of muted commands (from config)
    private final Set<String> mutedCommands;
    // Set of permissions required for chat features (from config)
    private final Set<String> playerChatPermissions;
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
    private final String chatFormat;

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
        this.chatFormat = chatConfig.has("chat-format") ? chatConfig.get("chat-format").getAsString() : "{DISPLAYNAME}: {MESSAGE}";
        this.commandsConfig = commandsConfig;
    }

    /**
     * Converts a config array to a Set<String>.
     */
    private Set<String> toSet(JsonObject obj, String key) {
        if (!obj.has(key) || !obj.get(key).isJsonArray()) return Collections.emptySet();
        Set<String> set = new HashSet<>();
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
    public boolean hasChatPermission(String permission) {
        return playerChatPermissions.contains(permission);
    }

    // Accessors for chat config options
    public boolean shouldSleepIgnoreAfk() { return sleepIgnoresAfkPlayers; }
    public boolean shouldSleepIgnoreVanished() { return sleepIgnoresVanishedPlayers; }
    public String getAfkListName() { return afkListName; }
    public boolean shouldBroadcastAfk() { return broadcastAfkMessage; }
    public boolean showDeathMessages() { return deathMessages; }
    public String getVanishingItemsPolicy() { return vanishingItemsPolicy; }
    public String getBindingItemsPolicy() { return bindingItemsPolicy; }
    public boolean shouldSendInfoAfterDeath() { return sendInfoAfterDeath; }
    public boolean allowSilentJoinQuit() { return allowSilentJoinQuit; }
    public String getCustomJoinMessage() { return customJoinMessage; }
    public String getCustomQuitMessage() { return customQuitMessage; }
    public String getCustomNewUsernameMessage() { return customNewUsernameMessage; }
    public boolean useCustomServerFullMessage() { return useCustomServerFullMessage; }
    public int getHideJoinQuitMessagesAbove() { return hideJoinQuitMessagesAbove; }
    public String getChatFormat() { return chatFormat; }

    // Chat command enable/disable checks
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
