package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages SocialSpy feature for moderators/admins.
 * Thread-safe implementation with permission checking.
 *
 * <p>The broadcast format is fully configurable — priority order:
 * <ol>
 *   <li>Primary  : {@code chat.messaging.socialspyFormat} in {@code config.json}</li>
 *   <li>Fallback : {@code neoessentials.socialspy.format} lang key</li>
 * </ol>
 *
 * <p>Supported named placeholders in the format:
 * <ul>
 *   <li>{@code {sender}}               — raw username of the sender</li>
 *   <li>{@code {receiver}}             — raw username of the recipient</li>
 *   <li>{@code {message}}              — the private message text</li>
 *   <li>{@code {sender_displayname}}   — display name of the sender (via PlaceholderAPI)</li>
 *   <li>{@code {receiver_displayname}} — display name of the recipient</li>
 *   <li>Any {@code {neoessentials_*}} placeholder registered with PlaceholderAPI</li>
 * </ul>
 * Color codes using {@code &} are supported.
 */
public class SocialSpyManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SocialSpyManager.class);

    // Thread-safe Set for concurrent access
    private static final Set<String> socialSpyPlayers = ConcurrentHashMap.newKeySet();

    public static void toggleSocialSpy(ServerPlayer player) {
        String name = player.getName().getString().toLowerCase();
        if (socialSpyPlayers.contains(name)) {
            socialSpyPlayers.remove(name);
        } else {
            socialSpyPlayers.add(name);
        }
    }

    public static boolean hasSocialSpy(ServerPlayer player) {
        return socialSpyPlayers.contains(player.getName().getString().toLowerCase());
    }

    // ── Format resolution ────────────────────────────────────────────────────

    /**
     * Returns the SocialSpy broadcast format template with named placeholders.
     * Checks {@code chat.messaging.socialspyFormat} in config first, then falls back
     * to the {@code neoessentials.socialspy.format} lang key (converted to use named vars).
     */
    private static String getSocialSpyFormat() {
        try {
            com.google.gson.JsonObject chatConfig =
                com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig != null && chatConfig.has("messaging")) {
                com.google.gson.JsonObject messaging = chatConfig.getAsJsonObject("messaging");
                if (messaging != null && messaging.has("socialspyFormat")) {
                    String fmt = messaging.get("socialspyFormat").getAsString();
                    if (fmt != null && !fmt.isBlank()) {
                        if (MessageUtil.isDebugMode()) {
                            LOGGER.debug("[SocialSpy] Using config-based format: '{}'", fmt);
                        }
                        return fmt;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("[SocialSpy] Could not read format from config, using lang fallback: {}", e.getMessage());
        }

        // Convert the positional lang key ({0}, {1}, {2}) → named vars by supplying
        // the named placeholder tokens as positional args so MessageFormat substitutes them.
        return MessageUtil.localize("neoessentials.socialspy.format",
            "{sender}", "{receiver}", "{message}");
    }

    /**
     * Resolves a player's display name via PlaceholderAPI, falling back
     * safely to the raw Minecraft username on any error.
     */
    private static String resolveDisplayName(ServerPlayer player) {
        try {
            String display = com.zerog.neoessentials.api.PlaceholderAPI
                .setPlaceholders(player, "{neoessentials_displayname}");
            // If the placeholder wasn't resolved (still contains braces) use raw name
            return (display != null && !display.contains("{")) ? display : player.getName().getString();
        } catch (Exception e) {
            LOGGER.debug("[SocialSpy] Failed to resolve displayname for {}: {}", player.getName().getString(), e.getMessage());
            return player.getName().getString();
        }
    }

    // ── Broadcast ────────────────────────────────────────────────────────────

    public static void broadcast(ServerPlayer sender, ServerPlayer target, String message) {
        // Exempt check — senders with this permission are not monitored
        if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                sender.getUUID(), "neoessentials.chat.socialspy.exempt")) {
            return;
        }

        // Build the resolved format string once (shared for all spy recipients)
        String format = getSocialSpyFormat();

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("sender",               sender.getName().getString());
        vars.put("receiver",             target.getName().getString());
        vars.put("message",              message);
        vars.put("sender_displayname",   resolveDisplayName(sender));
        vars.put("receiver_displayname", resolveDisplayName(target));

        String formatted = MessageUtil.resolveTemplate(sender, format, vars);

        if (MessageUtil.isDebugMode()) {
            LOGGER.info("[SocialSpy] format='{}' → resolved='{}'", format, formatted);
        }

        // Broadcast to all players with SocialSpy active
        for (ServerPlayer spy : sender.getServer().getPlayerList().getPlayers()) {
            if (!hasSocialSpy(spy) || spy.equals(sender) || spy.equals(target)) continue;

            if (com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(
                    spy.getUUID(), "neoessentials.chat.socialspy")) {
                spy.sendSystemMessage(MessageUtil.coloredText(formatted));
            } else {
                // Permission revoked — auto-remove from spy list
                socialSpyPlayers.remove(spy.getName().getString().toLowerCase());
            }
        }
    }
}
