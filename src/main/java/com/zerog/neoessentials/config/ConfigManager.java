package com.zerog.neoessentials.config;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.zerog.neoessentials.util.ResourceUtil;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings({"unused", "InvertedCondition"}) // Public API class with many getters/setters
public class ConfigManager {
    /**
     * Returns true if kick actions should be logged (logKickActions in config).
     * Defaults to true if not set.
     */
    public static boolean isLogKickActionsEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("logKickActions")) {
                return kickSettings.get("logKickActions").getAsBoolean();
            }
        }
        return true;
    }
    /**
     * Returns the kickMessage from moderation.kickSettings.kickMessage
     * Defaults to 'You have been kicked from the server. Reason: {reason} Kicked by: {kicker}' if not set.
     */
    public static String getKickMessage() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("kickMessage")) {
                String val = kickSettings.get("kickMessage").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        return "You have been kicked from the server.\nReason: {reason}\nKicked by: {kicker}";
    }

    /**
     * Returns the kickAllMessage from moderation.kickSettings.kickAllMessage
     * Defaults to 'Server maintenance in progress. Please reconnect in a few minutes.' if not set.
     */
    public static String getKickAllMessage() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("kickAllMessage")) {
                String val = kickSettings.get("kickAllMessage").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        return "Server maintenance in progress. Please reconnect in a few minutes.";
    }
    /**
     * Returns true if staff should be notified when a player is kicked (notifyStaffOnKick in config).
     * Defaults to true if not set.
     */
    public static boolean isNotifyStaffOnKickEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("notifyStaffOnKick")) {
                return kickSettings.get("notifyStaffOnKick").getAsBoolean();
            }
        }
        return true;
    }
    /**
     * Returns the defaultKickReason from moderation.kickSettings.defaultKickReason
     * Defaults to 'Kicked by an operator' if not set or invalid.
     */
    @SuppressWarnings("unused") // Public API method
    public static String getDefaultKickReason() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("defaultKickReason")) {
                String val = kickSettings.get("defaultKickReason").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        return "Kicked by an operator";
    }
    /**
     * Returns the maxKickReason from moderation.kickSettings.maxKickReason
     * Defaults to 500 if not set or invalid.
     */
    @SuppressWarnings("unused") // Public API method
    public static int getMaxKickReasonLength() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("maxKickReason")) {
                try {
                    int val = kickSettings.get("maxKickReason").getAsInt();
                    if (val > 0) return val;
                } catch (Exception ignored) {}
            }
        }
        return 500;
    }

    /**
     * Returns true if kick actions should be broadcast to all players (broadcastKicks in config).
     * Defaults to false if not set.
     */
    public static boolean isBroadcastKicksEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("broadcastKicks")) {
                return kickSettings.get("broadcastKicks").getAsBoolean();
            }
        }
        return false;
    }
    /**
     * Returns true if the kick system is enabled (enableKickSystem in config).
     * Defaults to true if not set.
     */
    @SuppressWarnings("unused") // Public API method
    public static boolean isKickSystemEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("kickSettings")) {
            JsonObject kickSettings = config.getAsJsonObject("moderation").getAsJsonObject("kickSettings");
            if (kickSettings.has("enableKickSystem")) {
                return kickSettings.get("enableKickSystem").getAsBoolean();
            }
        }
        return true;
    }
    /**
     * Returns the freezeMessage from moderation.freezeSettings.freezeMessage
     * Falls back to localization key if not set.
     */
    public static String getFreezeMessage() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("freezeMessage")) {
                String val = freezeSettings.get("freezeMessage").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        // Fallback to localization key
        return "neoessentials.moderation.frozen_message";
    }

    /**
     * Returns the unfreezeMessage from moderation.freezeSettings.unfreezeMessage
     * Falls back to localization key if not set.
     */
    public static String getUnfreezeMessage() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("unfreezeMessage")) {
                String val = freezeSettings.get("unfreezeMessage").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        // Fallback to localization key
        return "neoessentials.moderation.unfrozen_message";
    }

    /**
     * Returns the freezeReminder from moderation.freezeSettings.freezeReminder
     * Falls back to localization key if not set.
     */
    public static String getFreezeReminder() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("freezeReminder")) {
                String val = freezeSettings.get("freezeReminder").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        // Fallback to localization key
        return "neoessentials.moderation.freeze_reminder";
    }
    /**
     * Returns the defaultFreezeReason from moderation.freezeSettings.defaultFreezeReason
     * Defaults to 'Frozen by an operator' if not set or invalid.
     */
    public static String getDefaultFreezeReason() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("defaultFreezeReason")) {
                String val = freezeSettings.get("defaultFreezeReason").getAsString();
                if (val != null && !val.trim().isEmpty()) return val;
            }
        }
        return "Frozen by an operator";
    }
    /**
     * Returns the maxFreezeReason from moderation.freezeSettings.maxFreezeReason
     * Defaults to 500 if not set or invalid.
     */
    public static int getMaxFreezeReasonLength() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("maxFreezeReason")) {
                try {
                    int val = freezeSettings.get("maxFreezeReason").getAsInt();
                    if (val > 0) return val;
                } catch (Exception ignored) {}
            }
        }
        return 500;
    }

    /**
     * Returns the freezeReminderInterval (in seconds) from moderation.freezeSettings.freezeReminderInterval
     * Defaults to 30 if not set or invalid.
     */
    @SuppressWarnings("unused") // Public API method
    public static int getFreezeReminderInterval() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("freezeReminderInterval")) {
                try {
                    int val = freezeSettings.get("freezeReminderInterval").getAsInt();
                    if (val >= 0) return val;
                } catch (Exception ignored) {}
            }
        }
        return 30;
    }

    /**
     * Returns true if freeze/unfreeze actions should be logged (logFreezeActions in config).
     * Defaults to true if not set.
     */
    @SuppressWarnings("unused") // Public API method
    public static boolean isLogFreezeActionsEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("logFreezeActions")) {
                return freezeSettings.get("logFreezeActions").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if frozen players should remain frozen when they log back in (freezeOnLogin in config).
     * Defaults to true if not set.
     */
    @SuppressWarnings("unused") // Public API method
    public static boolean isFreezeOnLoginEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("freezeOnLogin")) {
                return freezeSettings.get("freezeOnLogin").getAsBoolean();
            }
        }
        return true;
    }
    /**
     * Returns the list of allowed commands for frozen players from moderation.freezeSettings.allowedCommands.
     * Returns an empty list if not set.
     */
    public static java.util.List<String> getFreezeAllowedCommands() {
        java.util.List<String> allowed = new java.util.ArrayList<>();
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("allowedCommands") && freezeSettings.get("allowedCommands").isJsonArray()) {
                for (var el : freezeSettings.getAsJsonArray("allowedCommands")) {
                    if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                        allowed.add(el.getAsString().toLowerCase());
                    }
                }
            }
        }
        return allowed;
    }


    /**
     * Returns true if freeze system is enabled in moderation.freezeSettings config section.
     * (moderation.freezeSettings.enableFreezeSystem)
     * Defaults to true if not set.
     */
    public static boolean isFreezeSystemEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("enableFreezeSystem")) {
                return freezeSettings.get("enableFreezeSystem").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if frozen players should be prevented from using commands (preventCommands in config).
     * Defaults to true if not set.
     */
    public static boolean isFreezePreventCommandsEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("freezeSettings")) {
            JsonObject freezeSettings = config.getAsJsonObject("moderation").getAsJsonObject("freezeSettings");
            if (freezeSettings.has("preventCommands")) {
                return freezeSettings.get("preventCommands").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if vanished players should be prevented from interacting (preventInteraction in config).
     * Defaults to true if not set.
     */
    public static boolean isVanishPreventInteractionEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("preventInteraction")) {
                return vanishSettings.get("preventInteraction").getAsBoolean();
            }
        }
        return true;
    }
    /**
     * Returns true if vanish actions should be broadcast to staff (broadcastToStaffVanish in config).
     * Defaults to false if not set.
     */
    public static boolean isBroadcastToStaffVanishEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("broadcastToStaffVanish")) {
                return vanishSettings.get("broadcastToStaffVanish").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns true if vanish actions should be broadcast to all players (BroadcastToAllVanish in config).
     * Defaults to false if not set.
     */
    public static boolean isBroadcastToAllVanishEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("BroadcastToAllVanish")) {
                return vanishSettings.get("BroadcastToAllVanish").getAsBoolean();
            }
        }
        return false;
    }
    /**
     * Returns true if vanished players should be hidden from the tab list (hideFromTabList in config).
     * Defaults to true if not set.
     */
    public static boolean isHideFromTabListEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("hideFromTabList")) {
                return vanishSettings.get("hideFromTabList").getAsBoolean();
            }
        }
        return true;
    }
    /**
     * Returns true if vanish actions should be logged (moderation.vanishSettings.logVanishActions).
     * Defaults to true if not set.
     */
    public static boolean isLogVanishActionsEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("logVanishActions")) {
                return vanishSettings.get("logVanishActions").getAsBoolean();
            }
        }
        return true;
    }
    // Note: instance methods for vanish system/on-join exist and should be used via getInstance().
    /**
     * Returns true if staff should be vanished on join (vanishOnJoin in config).
     * Defaults to false if not set.
     */
    public boolean isVanishOnJoinEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("vanishOnJoin")) {
                return vanishSettings.get("vanishOnJoin").getAsBoolean();
            }
        }
        return false;
    }
    /**
     * Returns true if the vanish system is enabled in the config (enableVanishSystem).
     * Defaults to true if not set.
     */
    public boolean isVanishSystemEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("enableVanishSystem")) {
            return config.get("enableVanishSystem").getAsBoolean();
        }
        return true;
    }
        /**
         * Returns true if logJailActions is enabled in moderation.jailSettings config section.
         * (moderation.jailSettings.logJailActions)
         */
        public boolean isLogJailActionsEnabled() {
            JsonObject config = getConfig(MAIN_CONFIG);
            if (config.has("moderation")) {
                JsonObject moderation = config.getAsJsonObject("moderation");
                if (moderation.has("jailSettings")) {
                    JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                    if (jailSettings.has("logJailActions")) {
                        return jailSettings.get("logJailActions").getAsBoolean();
                    }
                }
            }
            return true;
        }
        /**
         * Returns true if preventJailEscape is enabled in moderation.jailSettings config section.
         * (moderation.jailSettings.preventJailEscape)
         */
        public boolean isPreventJailEscapeEnabled() {
            JsonObject config = getConfig(MAIN_CONFIG);
            if (config.has("moderation")) {
                JsonObject moderation = config.getAsJsonObject("moderation");
                if (moderation.has("jailSettings")) {
                    JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                    if (jailSettings.has("preventJailEscape")) {
                        return jailSettings.get("preventJailEscape").getAsBoolean();
                    }
                }
            }
            return false;
        }
        /**
         * Returns the jail message format from moderation.jailSettings.jailMessageFormat
         * Defaults to a standard message if not set.
         */
        public String getJailMessageFormat() {
            JsonObject config = getConfig(MAIN_CONFIG);
            if (config.has("moderation")) {
                JsonObject moderation = config.getAsJsonObject("moderation");
                if (moderation.has("jailSettings")) {
                    JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                    if (jailSettings.has("jailMessageFormat")) {
                        String val = jailSettings.get("jailMessageFormat").getAsString();
                        if (val != null && !val.trim().isEmpty()) return val;
                    }
                }
            }
            return "You cannot leave jail!";
        }
        /**
         * Returns the maxJailsBeforeTempBan from moderation.jailSettings.maxJailsBeforeTempBan
         * Defaults to 3 if not set.
         */
    public static int getMaxJailsBeforeTempBan() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                if (jailSettings.has("maxJailsBeforeTempBan")) {
                    return jailSettings.get("maxJailsBeforeTempBan").getAsInt();
                }
            }
        }
        return 3;
    }
    /**
     * Returns true if jailTeleportOnLogin is enabled in moderation.jailSettings.jailTeleportOnLogin
     * Defaults to true if not set.
     */
    public boolean isJailTeleportOnLoginEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                if (jailSettings.has("jailTeleportOnLogin")) {
                    return jailSettings.get("jailTeleportOnLogin").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns the staff notification permission node from moderation.generalSettings.staffNotificationPermission
     * Defaults to 'neoessentials.moderation.notify' if not set.
     */
    public String getStaffNotificationPermission() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("generalSettings")) {
                JsonObject general = moderation.getAsJsonObject("generalSettings");
                if (general.has("staffNotificationPermission")) {
                    String val = general.get("staffNotificationPermission").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val;
                }
            }
        }
        return "neoessentials.moderation.notify";
    }

    /**
     * Returns true if broadcastBans is enabled in moderation.banSettings config section.
     * (moderation.banSettings.broadcastBans)
     */
    public boolean isBroadcastBansEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("broadcastBans")) {
                    return banSettings.get("broadcastBans").getAsBoolean();
                }
            }
        }
        return false;
    }

    /**
     * Returns true if logBanActions is enabled in moderation.banSettings config section.
     * (moderation.banSettings.logBanActions)
     */
    public boolean isLogBanActionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("logBanActions")) {
                    return banSettings.get("logBanActions").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns the checkExpiredBansInterval (in seconds) from moderation.banSettings.checkExpiredBansInterval.
     * Defaults to 300 if not set or invalid. Values <= 0 disable the scheduler (returns 0). Minimum allowed is 5 seconds.
     */
    public int getCheckExpiredBansInterval() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("checkExpiredBansInterval")) {
                    try {
                        int val = banSettings.get("checkExpiredBansInterval").getAsInt();
                        if (val <= 0) return 0; // Disabled
                        return Math.max(val, 5); // Enforce minimum
                    } catch (Exception ignored) {}
                }
            }
        }
        return 300;
    }
    /**
     * Returns the defaultBanReason from moderation.banSettings.defaultBanReason
     * Defaults to 'Banned by an operator' if not set or invalid.
     */
    public String getDefaultBanReason() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("defaultBanReason")) {
                    String val = banSettings.get("defaultBanReason").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val;
                }
            }
        }
        return "Banned by an operator";
    }

    /**
     * Returns the maxBanReason from moderation.banSettings.maxBanReason
     * Defaults to 500 if not set or invalid.
     */
    public int getMaxBanReasonLength() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("maxBanReason")) {
                    try {
                        int val = banSettings.get("maxBanReason").getAsInt();
                        if (val > 0) return val;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 500;
    }

    /**
     * Returns true if IP bans are enabled in moderation.banSettings.enableIPBans
     * Defaults to true if not set.
     */
    public boolean isIPBansEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("enableIPBans")) {
                    return banSettings.get("enableIPBans").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns true if permanent bans are enabled in moderation.banSettings.enablePermanentBans
     * Defaults to true if not set.
     */
    public boolean isPermanentBansEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("enablePermanentBans")) {
                    return banSettings.get("enablePermanentBans").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns true if temporary bans are enabled in moderation.banSettings.enableTempBans
     * Defaults to true if not set.
     */
    public boolean isTempBansEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("enableTempBans")) {
                    return banSettings.get("enableTempBans").getAsBoolean();
                }
            }
        }
        return true;
    }
    /**
     * Returns true if autoExpireTempBans is enabled in moderation.banSettings.autoExpireTempBans
     * Defaults to true if not set.
     */
    public boolean isAutoExpireTempBansEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("autoExpireTempBans")) {
                    return banSettings.get("autoExpireTempBans").getAsBoolean();
                }
            }
        }
        return true;
    }
    /**
     * Returns true if enableParticleEffects is enabled in teleportation.generalSettings config section.
     * (teleportation.generalSettings.enableParticleEffects)
     */
    public boolean getEnableParticleEffects() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("generalSettings")) {
                JsonObject general = tp.getAsJsonObject("generalSettings");
                if (general.has("enableParticleEffects")) {
                    return general.get("enableParticleEffects").getAsBoolean();
                }
            }
        }
        return true; // Default to enabled if not set
    }
    /**
     * Returns the maxTeleportDistance from teleportation.generalSettings.maxTeleportDistance
     * Returns -1 for unlimited if not set or invalid.
     */
    public int getMaxTeleportDistance() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("generalSettings")) {
                JsonObject general = tp.getAsJsonObject("generalSettings");
                if (general.has("maxTeleportDistance")) {
                    try {
                        return general.get("maxTeleportDistance").getAsInt();
                    } catch (Exception ignored) {}
                }
            }
        }
        return -1;
    }

    /**
     * Returns true if allowTeleportInCombat is enabled in teleportation.generalSettings config section.
     * (teleportation.generalSettings.allowTeleportInCombat)
     */
    public boolean isAllowTeleportInCombatEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("generalSettings")) {
                JsonObject general = tp.getAsJsonObject("generalSettings");
                if (general.has("allowTeleportInCombat")) {
                    return general.get("allowTeleportInCombat").getAsBoolean();
                }
            }
        }
        return false;
    }
    /**
     * Returns true if logTeleportRequests is enabled in teleportation.teleportRequestSettings config section.
     * (teleportation.teleportRequestSettings.logTeleportRequests)
     */
    public boolean isLogTeleportRequestsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("logTeleportRequests")) {
                    return req.get("logTeleportRequests").getAsBoolean();
                }
            }
        }
        return false;
    }

    /**
     * Returns true if autoAcceptFromFriends is enabled in teleportation.teleportRequestSettings config section.
     * (teleportation.teleportRequestSettings.autoAcceptFromFriends)
     */
    public boolean isAutoAcceptTeleportFromFriendsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("autoAcceptFromFriends")) {
                    return req.get("autoAcceptFromFriends").getAsBoolean();
                }
            }
        }
        return false;
    }
    /**
     * Returns true if enableRequestNotifications is enabled in teleportation.teleportRequestSettings config section.
     * (teleportation.teleportRequestSettings.enableRequestNotifications)
     */
    public boolean isTeleportRequestNotificationsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("enableRequestNotifications")) {
                    return req.get("enableRequestNotifications").getAsBoolean();
                }
            }
        }
        return true;
    }
    /**
     * Returns true if allowMultipleRequests is enabled in teleportation.teleportRequestSettings config section.
     * (teleportation.teleportRequestSettings.allowMultipleRequests)
     */
    public boolean isAllowMultipleTeleportRequestsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("allowMultipleRequests")) {
                    return req.get("allowMultipleRequests").getAsBoolean();
                }
            }
        }
        return false;
    }

    /**
     * Returns the max number of pending teleport requests per player from teleportation.teleportRequestSettings.maxPendingRequests
     * Defaults to 5 if not set or invalid.
     */
    public int getMaxPendingTeleportRequests() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("maxPendingRequests")) {
                    try {
                        int val = req.get("maxPendingRequests").getAsInt();
                        if (val > 0) return val;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 5;
    }

    /**
     * Returns the teleport request timeout (in seconds) from teleportation.teleportRequestSettings.requestTimeout
     * Defaults to 60 if not set or invalid.
     */
    public int getTeleportRequestTimeoutSeconds() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("requestTimeout")) {
                    try {
                        int val = req.get("requestTimeout").getAsInt();
                        if (val > 0) return val;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 60;
    }

    /**
     * Returns cooldown in seconds between sending teleport requests from teleportation.teleportRequestSettings.cooldownBetweenRequests
     * Defaults to 10 if not set or invalid.
     */
    public int getCooldownBetweenTeleportRequestsSeconds() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("teleportRequestSettings")) {
                JsonObject req = tp.getAsJsonObject("teleportRequestSettings");
                if (req.has("cooldownBetweenRequests")) {
                    try {
                        int val = req.get("cooldownBetweenRequests").getAsInt();
                        if (val >= 0) return val;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 10;
    }

    /**
     * Returns true if logSpawnActions is enabled in teleportation.spawnSettings config section.
     * (teleportation.spawnSettings.logSpawnActions)
     */
    public boolean isLogSpawnActionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("spawnSettings")) {
                JsonObject spawnSettings = tp.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("logSpawnActions")) {
                    return spawnSettings.get("logSpawnActions").getAsBoolean();
                }
            }
        }
        return false;
    }


    /**
     * Returns true if cancelOnDamage is enabled in teleportation.generalSettings config section.
     * (teleportation.generalSettings.cancelOnDamage)
     */
    public boolean isCancelOnDamageEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("generalSettings")) {
                JsonObject general = tp.getAsJsonObject("generalSettings");
                if (general.has("cancelOnDamage")) {
                    return general.get("cancelOnDamage").getAsBoolean();
                }
            }
        }
        return false;
    }

    /**
     * Check if teleportation module is enabled (modules.teleportationEnabled)
     */
    public boolean isTeleportationEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("teleportationEnabled")) {
                return modules.get("teleportationEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }

    /**
     * Returns true if requireConfirmationForDelete is enabled in teleportation.homeSettings config section.
     * (teleportation.homeSettings.requireConfirmationForDelete)
     */
    public boolean isRequireConfirmationForDeleteEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("homeSettings")) {
                JsonObject homeSettings = tp.getAsJsonObject("homeSettings");
                if (homeSettings.has("requireConfirmationForDelete")) {
                    return homeSettings.get("requireConfirmationForDelete").getAsBoolean();
                }
            }
        }
        return false;
    }

    /**
     * Returns true if logHomeActions is enabled in teleportation.homeSettings config section.
     * (teleportation.homeSettings.logHomeActions)
     */
    public boolean isLogHomeActionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("homeSettings")) {
                JsonObject homeSettings = tp.getAsJsonObject("homeSettings");
                if (homeSettings.has("logHomeActions")) {
                    return homeSettings.get("logHomeActions").getAsBoolean();
                }
            }
        }
        return false;
    }
    /**
     * Returns true if newPlayerKit is enabled in kits config section.
     * (kits.newPlayerKit.enabled)
     */
    public boolean isNewPlayerKitEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("newPlayerKit")) {
                JsonObject npk = kits.getAsJsonObject("newPlayerKit");
                if (npk.has("enabled")) {
                    return npk.get("enabled").getAsBoolean();
                }
            }
        }
        return false;
    }

    /**
     * Returns the kit name for newPlayerKit (kits.newPlayerKit.kitName), or empty string if not set.
     */
    public String getNewPlayerKitName() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("newPlayerKit")) {
                JsonObject npk = kits.getAsJsonObject("newPlayerKit");
                if (npk.has("kitName")) {
                    return npk.get("kitName").getAsString();
                }
            }
        }
        return "";
    }
    /**
     * Gets the maximum number of kits a player can have active cooldowns for (kits.maxKitsPerPlayer).
     * Returns -1 for unlimited if not set or invalid.
     */
    public int getMaxKitsPerPlayer() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("maxKitsPerPlayer")) {
                try {
                    return kits.get("maxKitsPerPlayer").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }
    /**
     * Check if AFK system is enabled (afk.enabled)
     */
    public boolean isAfkEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("afk")) {
            JsonObject afk = config.getAsJsonObject("afk");
            if (afk.has("enabled")) {
                return afk.get("enabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }
    /**
     * Get the permission cache expiry in minutes (permissions.permissionCacheExpiryMinutes)
     * Returns 5 if not set or invalid.
     */
    public int getPermissionCacheExpiryMinutes() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("permissionCacheExpiryMinutes")) {
                try {
                    int val = perms.get("permissionCacheExpiryMinutes").getAsInt();
                    if (val > 0) return val;
                } catch (Exception ignored) {}
            }
        }
        return 5; // Default to 5 minutes if not set
    }
    /**
     * Check if permission caching is enabled (permissions.cachePermissions)
     */
    public boolean isPermissionCacheEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("cachePermissions")) {
                return perms.get("cachePermissions").getAsBoolean();
            }
        }
        return true; // Default to enabled for legacy behavior
    }
    /**
     * Check if ops should bypass all permissions (permissions.opsBypassPermissions)
     */
    public boolean isOpsBypassPermissionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("opsBypassPermissions")) {
                return perms.get("opsBypassPermissions").getAsBoolean();
            }
        }
        return true; // Default to true for legacy behavior
    }
    /**
     * Get the default group name from config.json (permissions.defaultGroup).
     * Returns "default" if not set or empty.
     */
    public String getDefaultGroup() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("defaultGroup")) {
                String group = perms.get("defaultGroup").getAsString();
                if (group != null && !group.trim().isEmpty()) {
                    return group.trim();
                }
            }
        }
        return "default";
    }

    /**
     * Check if a command is enabled in the config (commands section).
     * Returns true if the command is enabled or not explicitly disabled.
     */
    public boolean isCommandEnabled(String command) {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("commands")) {
            JsonObject commands = config.getAsJsonObject("commands");
            if (commands.has(command)) {
                return commands.get(command).getAsBoolean();
            }
        }
        return true; // Default to enabled if not specified
    }
    /**
     * Returns true if allowKitOverride is enabled in kits config section.
     * (kits.allowKitOverride)
     */
    public boolean isAllowKitOverrideEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("allowKitOverride")) {
                return kits.get("allowKitOverride").getAsBoolean();
            }
        }
        return false;
    }
    /**
     * Retrieve the config object for the given config file name.
     * Loads and caches the config if not already loaded.
     * Supports split configs - if config.json is requested and split configs are enabled,
     * returns merged view of all split config files.
     */
    public JsonObject getConfig(String configName) {
        lock.readLock().lock();
        FileReader reader = null;
        try {
            if (configCache.containsKey(configName)) {
                return configCache.get(configName);
            }

            // Special handling for config.json when split configs are enabled
            if (configName.equals(MAIN_CONFIG) && ConfigSplitter.isSplittingEnabled()) {
                JsonObject merged = ConfigSplitter.mergeSplitConfigs();
                configCache.put(configName, merged);
                return merged;
            }

            File file = ResourceUtil.getConfigFile(configName);
            reader = new FileReader(file, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseReader(reader).getAsJsonObject();
            configCache.put(configName, obj);
            return obj;
        } catch (IOException e) {
            LOGGER.error("Failed to read config file {}: {}", configName, e.getMessage());
            JsonObject empty = new JsonObject();
            configCache.put(configName, empty);
            return empty;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
            lock.readLock().unlock();
        }
    }
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
    // private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Thread-safe singleton
    private static class SingletonHolder {
        private static final ConfigManager INSTANCE = new ConfigManager();
    }

    public static ConfigManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    // Thread-safe configuration cache
    private final ConcurrentHashMap<String, JsonObject> configCache = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    // private volatile boolean loaded = false;

    // Configuration file names
    public static final String MAIN_CONFIG = "config.json";
    public static final String ECONOMY_CONFIG = "economy.json";
    public static final String PERMISSIONS_CONFIG = "permissions.json";
    public static final String KITS_CONFIG = "kits.json";
    public static final String DISCORD_AUTH_CONFIG = "discord_auth.json";

    // Config version tracking - increment when structure changes
    private static final String CONFIG_VERSION_KEY = "_configVersion";

    // Expected versions for each config file (must match the version in JAR resources)
    private static final java.util.Map<String, Integer> EXPECTED_CONFIG_VERSIONS = new java.util.HashMap<String, Integer>() {{
        put(MAIN_CONFIG, 13);
        put(ECONOMY_CONFIG, 2);
        put(PERMISSIONS_CONFIG, 5);
        put(KITS_CONFIG, 1);
        put(DISCORD_AUTH_CONFIG, 5);
    }};

    private ConfigManager() {
        // On first construction, ensure all required config files exist
        ensureDefaultConfigs();
    }

    /**
     * Ensure all required config files exist in the config directory, copying from JAR if missing.
     */
    private void ensureDefaultConfigs() {
        String[] requiredConfigs = new String[] {
            MAIN_CONFIG, ECONOMY_CONFIG, PERMISSIONS_CONFIG, KITS_CONFIG, DISCORD_AUTH_CONFIG
        };
        for (String configName : requiredConfigs) {
            File configFile = ResourceUtil.getConfigFile(configName);
            if (!configFile.exists()) {
                copyDefaultConfig(configName, configFile);
            } else {
                // Config exists - check if it needs updating
                checkAndUpdateConfigVersion(configName, configFile);
            }
        }
    }

    /**
     * Check if a config file needs updating based on version mismatch.
     * If the file version is older than expected, backup and replace with new version.
     */
    private void checkAndUpdateConfigVersion(String configName, File configFile) {
        Integer expectedVersion = EXPECTED_CONFIG_VERSIONS.get(configName);
        if (expectedVersion == null) {
            return; // No version tracking for this config
        }

        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject config = JsonParser.parseReader(reader).getAsJsonObject();

            int currentVersion = 0;
            if (config.has(CONFIG_VERSION_KEY)) {
                currentVersion = config.get(CONFIG_VERSION_KEY).getAsInt();
            }

            if (currentVersion < expectedVersion) {
                LOGGER.warn("Config file {} is outdated (version {} < {}). Backing up and updating...",
                    configName, currentVersion, expectedVersion);

                // Create backup of old config
                createConfigBackup(configFile, currentVersion);

                // Replace with new version from JAR
                copyDefaultConfig(configName, configFile);

                // Clear cache to force reload
                configCache.remove(configName);

                LOGGER.info("Config file {} has been updated to version {}", configName, expectedVersion);
            } else if (currentVersion > expectedVersion) {
                LOGGER.warn("Config file {} has a newer version ({}) than expected ({}). This may cause issues.",
                    configName, currentVersion, expectedVersion);
            } else {
                LOGGER.debug("Config file {} is up to date (version {})", configName, currentVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check version for config {}: {}", configName, e.getMessage());
        }
    }

    /**
     * Create a timestamped backup of a config file.
     */
    private void createConfigBackup(File configFile, int oldVersion) {
        try {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new java.util.Date());
            String backupName = configFile.getName().replace(".json",
                String.format("_v%d_backup_%s.json", oldVersion, timestamp));
            File backupFile = new File(configFile.getParentFile(), backupName);

            java.nio.file.Files.copy(configFile.toPath(), backupFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            LOGGER.info("Created backup of old config: {}", backupFile.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to create backup for {}: {}", configFile.getName(), e.getMessage());
        }
    }

    /**
     * Copy default config from JAR resources to the config directory.
     */
    private void copyDefaultConfig(String configName, File configFile) {
        try (InputStream in = ResourceUtil.getJarConfigResource(configName)) {
            if (in != null) {
                // Ensure parent directories exist
                File parentDir = configFile.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    if (!parentDir.mkdirs()) {
                        LOGGER.warn("Failed to create parent directories for {}", configFile.getAbsolutePath());
                    }
                }
                try (OutputStream out = new FileOutputStream(configFile)) {
                    byte[] buffer = new byte[8192];
                    int len;
                    while ((len = in.read(buffer)) > 0) {
                        out.write(buffer, 0, len);
                    }
                }
                LOGGER.info("Copied default config {} to {}", configName, configFile.getAbsolutePath());
            } else {
                LOGGER.warn("Default config resource not found in JAR: {}", configName);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to copy default config {}: {}", configName, e.getMessage());
        }
    }

    /**
     * Check if external permissions should be used (permissions.useExternalPermissions)
     */
    public boolean isExternalPermissionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("useExternalPermissions")) {
                return perms.get("useExternalPermissions").getAsBoolean();
            }
        }
        return false; // Default to false
    }

    /**
     * Check if XSS protection is enabled (security.enableXSSProtection)
     */
    public boolean isXSSProtectionEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("enableXSSProtection")) {
                return security.get("enableXSSProtection").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }

    /**
     * Check if input validation is enabled (security.enableInputValidation)
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isInputValidationEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("enableInputValidation")) {
                return security.get("enableInputValidation").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }

    /**
     * Check if command length enforcer is enabled (security.enableCommandLengthEnforcer).
     * This controls whether the CommandLengthEnforcer event handler validates player commands.
     * When disabled, players can use commands of any length (not recommended for security).
     * Defaults to true if not set.
     *
     * @return true if command length enforcement is enabled, false otherwise
     */
    public boolean isCommandLengthEnforcerEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("enableCommandLengthEnforcer")) {
                return security.get("enableCommandLengthEnforcer").getAsBoolean();
            }
        }
        return true; // Default to enabled for security
    }

    /**
     * Returns true if custom chat formatting is enabled (chat.enable-chat-formatting in config).
     * Defaults to true if not set.
     */
    public static boolean isChatFormattingEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("chat")) {
            JsonObject chat = config.getAsJsonObject("chat");
            if (chat.has("enable-chat-formatting")) {
                return chat.get("enable-chat-formatting").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if color codes (including hex) are enabled in config (chat.enable-color-codes).
     * Defaults to true if not set.
     */
    public static boolean isColorCodesEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("chat")) {
            JsonObject chat = config.getAsJsonObject("chat");
            if (chat.has("enable-color-codes")) {
                return chat.get("enable-color-codes").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if economy module is enabled (modules.economyEnabled).
     * Defaults to true if not set.
     */
    public static boolean isEconomyEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("economyEnabled")) {
                return modules.get("economyEnabled").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns the economy starting balance from economy.json (startingBalance).
     * Defaults to 100.0 if not set.
     */
    public static double getEconomyStartingBalance() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("startingBalance")) {
            try {
                return config.get("startingBalance").getAsDouble();
            } catch (Exception ignored) {}
        }
        return 100.0;
    }

    /**
     * Returns the currency symbol from economy.json (currencySymbol).
     * Defaults to "$" if not set.
     */
    public static String getCurrencySymbol() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("currencySymbol")) {
            String symbol = config.get("currencySymbol").getAsString();
            if (symbol != null && !symbol.isEmpty()) {
                return symbol;
            }
        }
        return "$";
    }

    /**
     * Returns the max balance from economy.json (maxBalance).
     * Defaults to 999999999.99 if not set.
     */
    public static double getMaxBalance() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("maxBalance")) {
            try {
                return config.get("maxBalance").getAsDouble();
            } catch (Exception ignored) {}
        }
        return 999999999.99;
    }

    /**
     * Returns the tax percentage from economy.json (taxPercentage).
     * Defaults to 0.0 if not set.
     */
    public static double getTaxPercentage() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("taxPercentage")) {
            try {
                return config.get("taxPercentage").getAsDouble();
            } catch (Exception ignored) {}
        }
        return 0.0;
    }

    /**
     * Alias for getTaxPercentage() for backwards compatibility.
     */
    public static double getEconomyTaxPercentage() {
        return getTaxPercentage();
    }

    /**
     * Returns true if negative balances are allowed from economy.json (allowNegativeBalances).
     * Defaults to false if not set.
     */
    public static boolean allowNegativeBalances() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("allowNegativeBalances")) {
            return config.get("allowNegativeBalances").getAsBoolean();
        }
        return false;
    }

    /**
     * Returns true if inactive account cleanup is enabled from economy.json (cleanupInactiveAccounts).
     * Defaults to true if not set.
     */
    public static boolean isCleanupInactiveAccountsEnabled() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("cleanupInactiveAccounts")) {
            return config.get("cleanupInactiveAccounts").getAsBoolean();
        }
        return true;
    }

    /**
     * Returns the inactive account cleanup days from economy.json (inactiveAccountCleanupDays).
     * Defaults to 30 if not set.
     */
    public static int getInactiveAccountCleanupDays() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("inactiveAccountCleanupDays")) {
            try {
                return config.get("inactiveAccountCleanupDays").getAsInt();
            } catch (Exception ignored) {}
        }
        return 30;
    }

    /**
     * Returns the max transfer amount from economy.json (maxTransferAmount).
     * Defaults to 10000.0 if not set.
     */
    public static double getMaxTransferAmount() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("maxTransferAmount")) {
            try {
                return config.get("maxTransferAmount").getAsDouble();
            } catch (Exception ignored) {}
        }
        return 10000.0;
    }

    /**
     * Returns the pay toggle default from economy.json (paytoggleDefault).
     * Defaults to true if not set.
     */
    public static boolean getPayToggleDefault() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("paytoggleDefault")) {
            return config.get("paytoggleDefault").getAsBoolean();
        }
        return true;
    }

    /**
     * Returns the cache maximum size from economy.json (cacheMaximumSize).
     * Defaults to 10000 if not set.
     */
    public static int getCacheMaximumSize() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("cacheMaximumSize")) {
            try {
                return config.get("cacheMaximumSize").getAsInt();
            } catch (Exception ignored) {}
        }
        return 10000;
    }

    /**
     * Returns the cache expire after access minutes from economy.json (cacheExpireAfterAccessMinutes).
     * Defaults to 60 if not set.
     */
    public static int getCacheExpireAfterAccessMinutes() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("cacheExpireAfterAccessMinutes")) {
            try {
                return config.get("cacheExpireAfterAccessMinutes").getAsInt();
            } catch (Exception ignored) {}
        }
        return 60;
    }

    /**
     * Returns the pay cooldown in seconds. This method is for backwards compatibility.
     * Returns 0 (no cooldown) by default as there is no specific config for this.
     */
    public static int getPayCooldownSeconds() {
        // No specific config for pay cooldown, return 0 (no cooldown)
        return 0;
    }

    /**
     * Clears the config cache, forcing all configs to be reloaded from disk on next access.
     * This is thread-safe and will acquire a write lock.
     */
    public void clearCache() {
        lock.writeLock().lock();
        try {
            configCache.clear();
            LOGGER.info("Configuration cache cleared - configs will be reloaded from disk");
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Loads all config files by clearing the cache and forcing a reload.
     * This ensures that any changes made to config files on disk are picked up.
     */
    public static void loadAll() {
        getInstance().clearCache();
        // Ensure all required configs exist
        getInstance().ensureDefaultConfigs();
    }

    /**
     * Returns true if chat module is enabled (modules.chatEnabled).
     * Defaults to true if not set.
     */
    public static boolean isChatEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("chatEnabled")) {
                return modules.get("chatEnabled").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if moderation module is enabled (modules.moderationEnabled).
     * Defaults to true if not set.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isModerationEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("moderationEnabled")) {
                return modules.get("moderationEnabled").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if web dashboard is enabled (webDashboard.enabled).
     * Defaults to true if not set.
     */
    public static boolean isWebDashboardEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("enabled")) {
                return dashboard.get("enabled").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if unsafe enchantments are allowed (items.unsafe-enchantments).
     * Defaults to true if not set.
     */
    public static boolean isUnsafeEnchantsAllowed() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("unsafe-enchantments")) {
                return items.get("unsafe-enchantments").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns the default stack size from items.default-stack-size.
     * Returns -1 (use vanilla) if not set.
     */
    public static int getDefaultStackSize() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("default-stack-size")) {
                try {
                    return items.get("default-stack-size").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return -1;
    }

    /**
     * Returns the oversized stack size from items.oversized-stacksize.
     * Defaults to 64 if not set.
     */
    public static int getOversizedStackSize() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("oversized-stacksize")) {
                try {
                    return items.get("oversized-stacksize").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 64;
    }

    /**
     * Returns the item spawn blacklist from items.item-spawn-blacklist.
     * Defaults to empty list if not set.
     */
    public static java.util.List<String> getItemSpawnBlacklist() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("item-spawn-blacklist") && items.get("item-spawn-blacklist").isJsonArray()) {
                java.util.List<String> list = new java.util.ArrayList<>();
                items.getAsJsonArray("item-spawn-blacklist").forEach(e -> list.add(e.getAsString()));
                return list;
            }
        }
        return java.util.Collections.emptyList();
    }

    /**
     * Returns true if permission-based item spawn is enabled (items.permission-based-item-spawn).
     * Defaults to false if not set.
     */
    public static boolean isPermissionBasedItemSpawn() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("permission-based-item-spawn")) {
                return items.get("permission-based-item-spawn").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns true if kits module is enabled (modules.kitsEnabled).
     * Defaults to true if not set.
     */
    public static boolean isKitModuleEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("kitsEnabled")) {
                return modules.get("kitsEnabled").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if kit system is enabled (kits config section exists and module enabled).
     * Defaults to true if not set.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isKitSystemEnabled() {
        return isKitModuleEnabled();
    }

    /**
     * Returns the cost for a kit command from kits.commandCosts.<commandName>.
     * Defaults to 0 if not set.
     */
    public static double getKitCommandCost(String commandName) {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("commandCosts")) {
                JsonObject costs = kits.getAsJsonObject("commandCosts");
                if (costs.has(commandName)) {
                    try {
                        return costs.get(commandName).getAsDouble();
                    } catch (Exception ignored) {}
                }
            }
        }
        return 0.0;
    }

    /**
     * Returns true if pastebin createkit is enabled (kits.pastebinCreatekit).
     * Defaults to false if not set.
     */
    public static boolean isPastebinCreatekitEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("pastebinCreatekit")) {
                return kits.get("pastebinCreatekit").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns true if used one-time kits should be skipped from kit list (kits.skipUsedOneTimeKitsFromKitList).
     * Defaults to false if not set.
     */
    public static boolean isSkipUsedOneTimeKitsFromKitList() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("skipUsedOneTimeKitsFromKitList")) {
                return kits.get("skipUsedOneTimeKitsFromKitList").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns true if kit auto-equip is enabled (kits.kitAutoEquip).
     * Defaults to false if not set.
     */
    public static boolean isKitAutoEquipEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("kitAutoEquip")) {
                return kits.get("kitAutoEquip").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns true if kit usage logging is enabled (kits.logKitUsage).
     * Defaults to true if not set.
     */
    public static boolean isLogKitUsageEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("kits")) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("logKitUsage")) {
                return kits.get("logKitUsage").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns true if jail location is required (moderation.jailSettings.requireJailLocation).
     * Defaults to true if not set.
     */
    public static boolean isRequireJailLocationEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                if (jailSettings.has("requireJailLocation")) {
                    return jailSettings.get("requireJailLocation").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns the ban message format from moderation.banSettings.banMessageFormat.
     * Defaults to standard message if not set.
     */
    public static String getBanMessageFormat() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("banMessageFormat")) {
                    String val = banSettings.get("banMessageFormat").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val;
                }
            }
        }
        return "You have been banned from this server.\nReason: {reason}\nBanned by: {bannedBy}\n{duration}";
    }

    /**
     * Returns the temp ban message format from moderation.banSettings.tempBanMessageFormat.
     * Defaults to standard message if not set.
     */
    public static String getTempBanMessageFormat() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("tempBanMessageFormat")) {
                    String val = banSettings.get("tempBanMessageFormat").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val;
                }
            }
        }
        return "You have been temporarily banned from this server.\nReason: {reason}\nBanned by: {bannedBy}\nExpires: {expiry}";
    }

    /**
     * Returns the IP ban message format from moderation.banSettings.ipBanMessageFormat.
     * Defaults to standard message if not set.
     */
    public static String getIPBanMessageFormat() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("ipBanMessageFormat")) {
                    String val = banSettings.get("ipBanMessageFormat").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val;
                }
            }
        }
        return "Your IP address has been banned from this server.\nReason: {reason}\nBanned by: {bannedBy}";
    }

    /**
     * Returns true if warp actions should be logged (teleportation.warpSettings.logWarpActions).
     * Defaults to true if not set.
     */
    public boolean isLogWarpActionsEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("warpSettings")) {
                JsonObject warpSettings = tp.getAsJsonObject("warpSettings");
                if (warpSettings.has("logWarpActions")) {
                    return warpSettings.get("logWarpActions").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns true if debug logging is enabled (logging.enableDebugLogging).
     * Defaults to false if not set.
     */
    public boolean isDebugLoggingEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("logging")) {
            JsonObject logging = config.getAsJsonObject("logging");
            if (logging.has("enableDebugLogging")) {
                return logging.get("enableDebugLogging").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Permission node to allow seeing vanished players. Used by event handlers.
     * Returns a reasonable default if not set.
     */
    public String getSeeVanishedPermission() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("generalSettings")) {
                JsonObject general = moderation.getAsJsonObject("generalSettings");
                if (general.has("seeVanishedPermission")) {
                    String val = general.get("seeVanishedPermission").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val;
                }
            }
        }
        return "neoessentials.moderation.seevanished";
    }

    /**
     * Returns the web dashboard port from webDashboard.port.
     * Defaults to 8080 if not set.
     */
    public int getWebDashboardPort() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("port")) {
                try {
                    return dashboard.get("port").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 8080;
    }

    /**
     * Returns the web dashboard bind address from webDashboard.bindAddress.
     * Defaults to "0.0.0.0" if not set.
     */
    public String getWebDashboardBindAddress() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("bindAddress")) {
                String addr = dashboard.get("bindAddress").getAsString();
                if (addr != null && !addr.trim().isEmpty()) return addr;
            }
        }
        return "0.0.0.0";
    }

    /**
     * Returns the web dashboard max threads from webDashboard.maxThreads.
     * Defaults to 10 if not set.
     */
    public int getWebDashboardMaxThreads() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("maxThreads")) {
                try {
                    return dashboard.get("maxThreads").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 10;
    }

    /**
     * Returns the web dashboard WebSocket port from webDashboard.webSocketPort.
     * Defaults to 8081 if not set.
     */
    public int getWebDashboardWebSocketPort() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("webSocketPort")) {
                try {
                    return dashboard.get("webSocketPort").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 8081;
    }

    /**
     * Returns the web dashboard hostname from webDashboard.hostname.
     * Defaults to "localhost" if not set.
     */
    public String getWebDashboardHostname() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("hostname")) {
                String hostname = dashboard.get("hostname").getAsString();
                if (hostname != null && !hostname.trim().isEmpty()) return hostname;
            }
        }
        return "localhost";
    }

    /**
     * Returns the custom web dashboard URL from webDashboard.customUrl.
     * Returns empty string if not set.
     */
    public String getWebDashboardCustomUrl() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("customUrl")) {
                String customUrl = dashboard.get("customUrl").getAsString();
                if (customUrl != null && !customUrl.trim().isEmpty()) return customUrl.trim();
            }
        }
        return "";
    }

    /**
     * Builds the dashboard URL based on configuration.
     * If customUrl is set, returns that. Otherwise builds URL from hostname and port.
     *
     * @return The dashboard URL to display to users
     */
    public String getWebDashboardUrl() {
        String customUrl = getWebDashboardCustomUrl();
        if (!customUrl.isEmpty()) {
            return customUrl;
        }

        String hostname = getWebDashboardHostname();
        int port = getWebDashboardPort();

        // Build URL with hostname and port
        return "http://" + hostname + ":" + port;
    }

    /**
     * Returns max command length from security.maxCommandLength.
     * Defaults to 256 if not set.
     */
    public int getMaxCommandLength() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("maxCommandLength")) {
                try {
                    return security.get("maxCommandLength").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 256;
    }

    /**
     * Returns max reason length from security.maxReasonLength.
     * Defaults to 500 if not set.
     */
    public int getMaxReasonLength() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("maxReasonLength")) {
                try {
                    return security.get("maxReasonLength").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 500;
    }

    /**
     * Returns max economy amount from security.maxEconomyAmount.
     * Defaults to 999999999.99 if not set.
     */
    public BigDecimal getMaxEconomyAmount() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("maxEconomyAmount")) {
                try {
                    return BigDecimal.valueOf(security.get("maxEconomyAmount").getAsDouble());
                } catch (Exception ignored) {}
            }
        }
        return BigDecimal.valueOf(999999999.99);
    }

    /**
     * Returns min economy amount from security.minEconomyAmount.
     * Defaults to 0.01 if not set.
     */
    public BigDecimal getMinEconomyAmount() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("minEconomyAmount")) {
                try {
                    return BigDecimal.valueOf(security.get("minEconomyAmount").getAsDouble());
                } catch (Exception ignored) {}
            }
        }
        return BigDecimal.valueOf(0.01);
    }

    /**
     * Returns whether unsafe commands are allowed from security.allowUnsafeCommands.
     * Defaults to false if not set.
     */
    public boolean isUnsafeCommandsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("allowUnsafeCommands")) {
                return security.get("allowUnsafeCommands").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns max unsafe enchantment level from security.maxUnsafeEnchantmentLevel.
     * Defaults to 10 if not set.
     */
    public int getMaxUnsafeEnchantmentLevel() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("maxUnsafeEnchantmentLevel")) {
                try {
                    return security.get("maxUnsafeEnchantmentLevel").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 10;
    }

    /**
     * Check if jail system is enabled (modules.jailEnabled)
     */
    public static boolean isJailSystemEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("jailEnabled")) {
                return modules.get("jailEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }

    /**
     * Get max jails before permanent ban from moderation.jail.maxJailsBeforePermBan
     * Defaults to 3 if not set
     */
    public static int getMaxJailsBeforePermBan() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jail")) {
                JsonObject jail = moderation.getAsJsonObject("jail");
                if (jail.has("maxJailsBeforePermBan")) {
                    return jail.get("maxJailsBeforePermBan").getAsInt();
                }
            }
        }
        return 3;
    }

    /**
     * Get temp ban duration in minutes from moderation.jail.tempBanDurationMinutes
     * Defaults to 1440 (24 hours) if not set
     */
    public static int getTempBanDurationMinutes() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jail")) {
                JsonObject jail = moderation.getAsJsonObject("jail");
                if (jail.has("tempBanDurationMinutes")) {
                    return jail.get("tempBanDurationMinutes").getAsInt();
                }
            }
        }
        return 1440; // Default 24 hours
    }

    /**
     * Check if permissions module is enabled (modules.permissionsEnabled)
     */
    public static boolean isPermissionsEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("modules")) {
            JsonObject modules = config.getAsJsonObject("modules");
            if (modules.has("permissionsEnabled")) {
                return modules.get("permissionsEnabled").getAsBoolean();
            }
        }
        return true; // Default to enabled
    }

    /**
     * Get list of protected areas from teleportation.protectedAreas
     * Returns empty list if not set
     */
    public static List<String> getProtectedAreas() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        List<String> areas = new ArrayList<>();
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("protectedAreas")) {
                teleportation.getAsJsonArray("protectedAreas").forEach(element -> 
                    areas.add(element.getAsString())
                );
            }
        }
        return areas;
    }

    /**
     * Check if cancel on movement is enabled from teleportation.generalSettings.cancelOnMovement
     * Defaults to true if not set
     */
    public static boolean isCancelOnMovementEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject general = teleportation.getAsJsonObject("generalSettings");
                if (general.has("cancelOnMovement")) {
                    return general.get("cancelOnMovement").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Check if sound effects are enabled from teleportation.generalSettings.enableSoundEffects
     * Defaults to true if not set
     */
    public static boolean getEnableSoundEffects() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject teleportation = config.getAsJsonObject("teleportation");
            if (teleportation.has("generalSettings")) {
                JsonObject general = teleportation.getAsJsonObject("generalSettings");
                if (general.has("enableSoundEffects")) {
                    return general.get("enableSoundEffects").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Check if debug mode is enabled from debug.enabled
     * Defaults to false if not set
     */
    public static boolean isDebugModeEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("debug")) {
            JsonObject debug = config.getAsJsonObject("debug");
            if (debug.has("enabled")) {
                return debug.get("enabled").getAsBoolean();
            }
        }
        return false;
    }

}

