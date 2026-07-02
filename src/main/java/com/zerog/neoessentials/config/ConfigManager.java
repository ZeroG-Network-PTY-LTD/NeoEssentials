package com.zerog.neoessentials.config;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
     * Returns true if the vanish system is enabled in the config
     * (moderation.vanishSettings.enableVanishSystem).
     * Defaults to true if not set.
     */
    public boolean isVanishSystemEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation") && config.getAsJsonObject("moderation").has("vanishSettings")) {
            JsonObject vanishSettings = config.getAsJsonObject("moderation").getAsJsonObject("vanishSettings");
            if (vanishSettings.has("enableVanishSystem")) {
                return vanishSettings.get("enableVanishSystem").getAsBoolean();
            }
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
     * Returns the max jail reason length from moderation.jailSettings.maxJailReason.
     * Defaults to 500 if not set or invalid.
     */
    public int getMaxJailReasonLength() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                if (jailSettings.has("maxJailReason")) {
                    try {
                        int val = jailSettings.get("maxJailReason").getAsInt();
                        if (val > 0) return val;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 500;
    }

    /**
     * Returns true if IP bans are disabled in moderation.banSettings.enableIPBans.
     * Defaults to false if not set.
     */
    public boolean isIPBansDisabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("banSettings")) {
                JsonObject banSettings = moderation.getAsJsonObject("banSettings");
                if (banSettings.has("enableIPBans")) {
                    return !banSettings.get("enableIPBans").getAsBoolean();
                }
            }
        }
        return false;
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
     * Check if vanilla OP status should act as a last-resort fallback when every
     * permission system (external adapter + internal manager) is unavailable or
     * returns {@code false}. (permissions.vanillaOpFallback)
     *
     * <p>Unlike {@link #isOpsBypassPermissionsEnabled()} which skips permission
     * checks entirely, this fires <em>after</em> all checks have run — so the
     * permission system is still consulted first in normal operation.  The fallback
     * is designed to prevent admin lockouts when configs are corrupted or an
     * external permission mod crashes.
     *
     * <p>Defaults to {@code true}.
     */
    public boolean isVanillaOpFallbackEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("vanillaOpFallback")) {
                return perms.get("vanillaOpFallback").getAsBoolean();
            }
        }
        return true; // Safe default — prevents lockouts
    }

    /**
     * Check if permission-change audit logging is enabled (permissions.auditLogging).
     * Defaults to {@code true}.
     */
    public boolean isPermissionAuditEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("permissions")) {
            JsonObject perms = config.getAsJsonObject("permissions");
            if (perms.has("auditLogging")) {
                return perms.get("auditLogging").getAsBoolean();
            }
        }
        return true;
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
                // Always merge from split files, never from config.json
                JsonObject merged = ConfigSplitter.mergeSplitConfigs();
                configCache.put(configName, merged);
                return merged;
            }

            // If configName is a section name (no .json extension), extract it from the
            // main config. This allows callers such as getConfig("chat") to work regardless
            // of whether split configs are enabled, without needing to know the file layout.
            if (!configName.endsWith(".json")) {
                // ReentrantReadWriteLock allows same thread to re-acquire the read lock
                JsonObject mainConfig = getConfig(MAIN_CONFIG);
                if (mainConfig != null && mainConfig.has(configName)
                        && mainConfig.get(configName).isJsonObject()) {
                    JsonObject section = mainConfig.getAsJsonObject(configName);
                    configCache.put(configName, section);
                    return section;
                }
                // Fallback: attempt to read configName.json directly (handles split-config files
                // whose section was not present in the cached MAIN_CONFIG, e.g. chat.json when
                // the merged config cache is stale or split configs were recently enabled).
                File splitFile = ResourceUtil.getConfigFile(configName + ".json");
                if (splitFile.exists()) {
                    try (FileReader splitReader = new FileReader(splitFile, StandardCharsets.UTF_8)) {
                        JsonObject fileObj = parseJsonWithComments(splitReader).getAsJsonObject();
                        // Split files wrap their content under the section key: {"chat": {...}}
                        if (fileObj.has(configName) && fileObj.get(configName).isJsonObject()) {
                            JsonObject section = fileObj.getAsJsonObject(configName);
                            configCache.put(configName, section);
                            LOGGER.debug("Config section '{}' loaded directly from {}.json (fallback)", configName, configName);
                            return section;
                        }
                        // File exists but uses a flat layout – return the whole object
                        configCache.put(configName, fileObj);
                        LOGGER.debug("Config '{}' loaded directly from {}.json (flat layout fallback)", configName, configName);
                        return fileObj;
                    } catch (IOException fallbackEx) {
                        LOGGER.warn("Could not read fallback config file {}.json: {}", configName, fallbackEx.getMessage());
                    }
                }
                // Section missing – return empty (do not cache so it retries after reload)
                LOGGER.debug("Config section '{}' not found in main config or {}.json, returning empty object", configName, configName);
                return new JsonObject();
            }

            File file = ResourceUtil.getConfigFile(configName);
            reader = new FileReader(file, StandardCharsets.UTF_8);
            JsonObject obj = parseJsonWithComments(reader).getAsJsonObject();
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

    /**
     * Parse a JSON file in "lenient" mode, which allows {@code //} single-line comments and
     * {@code /* ... *\/} block comments in the file.  Comments are ignored by the parser; the
     * resulting {@link JsonObject} contains only the actual data keys.
     *
     * <p>This is the sole entry-point for reading config files from disk or from JAR resources
     * so that admins can annotate their config files with comments without breaking the loader.
     *
     * <p><strong>Note:</strong> When a config is written back (e.g. after a version merge) it is
     * serialised through Gson's pretty-printer which does <em>not</em> emit comments.  Comments
     * are therefore intentionally only present in the default/template copy (first install or
     * JAR resource), not in files that have been through a round-trip write.
     */
    private static JsonObject parseJsonWithComments(java.io.Reader reader) throws IOException {
        com.google.gson.stream.JsonReader jsonReader = new com.google.gson.stream.JsonReader(reader);
        jsonReader.setLenient(true);
        return JsonParser.parseReader(jsonReader).getAsJsonObject();
    }

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
    public static final String TABLIST_CONFIG = "tablist.json";
    public static final String ANIMATIONS_CONFIG = "animations.json";

    // Config version tracking - increment when structure changes
    private static final String CONFIG_VERSION_KEY = "_configVersion";

    // Expected versions for each config file (must match the version in JAR resources)
    private static final java.util.Map<String, Integer> EXPECTED_CONFIG_VERSIONS = new java.util.HashMap<>() {{
        put(MAIN_CONFIG, 25);          // v25 — localization.preserveCustomTranslations setting
        put(ECONOMY_CONFIG, 3);        // v3  — removed _configVersion_comment
        put(PERMISSIONS_CONFIG, 7);    // v7  — removed _configVersion_comment
        put(KITS_CONFIG, 2);           // v2  — removed _configVersion_comment
        put(DISCORD_AUTH_CONFIG, 8);   // v8  — migrated to // comment style
        put(TABLIST_CONFIG, 5);        // v5  — migrated to // comment style
        put(ANIMATIONS_CONFIG, 2);     // v2  — migrated to // comment style
    }};

    /**
     * Returns the configured server language code (e.g. "fr_fr", "de_de").
     * Reads from localization.language in config.json.
     * Defaults to "en_us" if not set.
     */
    public static String getServerLanguage() {
        try {
            JsonObject config = getInstance().getConfig(MAIN_CONFIG);
            if (config.has("localization")) {
                JsonObject loc = config.getAsJsonObject("localization");
                if (loc.has("language")) {
                    String val = loc.get("language").getAsString();
                    if (val != null && !val.trim().isEmpty()) return val.trim().toLowerCase();
                }
            }
        } catch (Exception ignored) {}
        return "en_us";
    }

    /**
     * Sets the active server language (localization.language) and persists it to config.json.
     * Does not reload translations — call {@link com.zerog.neoessentials.util.MessageUtil#reloadTranslations()}
     * afterward to apply the change immediately.
     */
    public static void setServerLanguage(String languageCode) {
        ConfigManager instance = getInstance();
        JsonObject config = instance.getConfig(MAIN_CONFIG);
        JsonObject loc = config.has("localization") && config.get("localization").isJsonObject()
            ? config.getAsJsonObject("localization")
            : new JsonObject();
        loc.addProperty("language", languageCode.trim().toLowerCase());
        config.add("localization", loc);
        instance.saveConfig(MAIN_CONFIG, config);
    }

    /**
     * Returns whether custom-edited language files should be left completely untouched.
     * Reads from localization.preserveCustomTranslations in config.json. Defaults to false.
     * When true, the mod will not merge in new keys or auto-fix legacy placeholders on startup.
     */
    public static boolean isPreserveCustomTranslationsEnabled() {
        try {
            JsonObject config = getInstance().getConfig(MAIN_CONFIG);
            if (config.has("localization")) {
                JsonObject loc = config.getAsJsonObject("localization");
                if (loc.has("preserveCustomTranslations")) {
                    return loc.get("preserveCustomTranslations").getAsBoolean();
                }
            }
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Returns true if the web dashboard is enabled (webDashboard.enabled).
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
     * Returns true if the web dashboard should start automatically on server boot
     * (webDashboard.autoStart). If false, use /dashboard start to start it manually.
     * Defaults to true if not set.
     */
    public static boolean isWebDashboardAutoStartEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("autoStart")) {
                return dashboard.get("autoStart").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns the web dashboard HTTP port (webDashboard.port). Defaults to 8080.
     */
    public int getWebDashboardPort() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("port")) {
                return dashboard.get("port").getAsInt();
            }
        }
        return 8080;
    }

    /**
     * Returns the web dashboard WebSocket port (webDashboard.websocketPort). Defaults to 8081.
     */
    public int getWebDashboardWebSocketPort() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("websocketPort")) {
                return dashboard.get("websocketPort").getAsInt();
            }
        }
        return 8081;
    }

    /**
     * Returns the web dashboard bind address (webDashboard.bindAddress). Defaults to "0.0.0.0".
     */
    public String getWebDashboardBindAddress() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("bindAddress")) {
                String val = dashboard.get("bindAddress").getAsString();
                if (val != null && !val.trim().isEmpty()) return val.trim();
            }
        }
        return "0.0.0.0";
    }

    /**
     * Returns a friendly URL for accessing the web dashboard, derived from webDashboard.port.
     * Uses "localhost" since the actual reachable address depends on the server's public IP.
     */
    public String getWebDashboardUrl() {
        return "http://localhost:" + getWebDashboardPort();
    }

    /**
     * Returns true if per-IP rate limiting is enabled for the dashboard API
     * (webDashboard.securitySettings.enableRateLimiting). Defaults to true.
     */
    public boolean isDashboardRateLimitingEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("securitySettings")) {
                JsonObject security = dashboard.getAsJsonObject("securitySettings");
                if (security.has("enableRateLimiting")) {
                    return security.get("enableRateLimiting").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns the maximum dashboard API requests per IP per minute
     * (webDashboard.securitySettings.maxRequestsPerMinute). Defaults to 60.
     */
    public int getDashboardMaxRequestsPerMinute() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("securitySettings")) {
                JsonObject security = dashboard.getAsJsonObject("securitySettings");
                if (security.has("maxRequestsPerMinute")) {
                    return security.get("maxRequestsPerMinute").getAsInt();
                }
            }
        }
        return 60;
    }

    /**
     * Returns true if Bearer token authentication is required on dashboard API endpoints
     * (webDashboard.securitySettings.requireAuthentication). Defaults to true.
     */
    public boolean isDashboardAuthRequired() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("webDashboard")) {
            JsonObject dashboard = config.getAsJsonObject("webDashboard");
            if (dashboard.has("securitySettings")) {
                JsonObject security = dashboard.getAsJsonObject("securitySettings");
                if (security.has("requireAuthentication")) {
                    return security.get("requireAuthentication").getAsBoolean();
                }
            }
        }
        return true;
    }

    private ConfigManager() {
        // On first construction, ensure all required config files exist
        ensureDefaultConfigs();
    }

    /**
     * Ensure all required config files exist in the config directory, copying from JAR if missing.
     * If split configs are enabled, config.json is skipped for all operations except migration/backup.
     * Internal permissions.json is not generated if external permissions are enabled.
     */
    private void ensureDefaultConfigs() {
        String[] requiredConfigs = new String[] {
            MAIN_CONFIG, ECONOMY_CONFIG, PERMISSIONS_CONFIG, KITS_CONFIG, DISCORD_AUTH_CONFIG, TABLIST_CONFIG, ANIMATIONS_CONFIG
        };

        // Check if split configs are enabled
        boolean splitConfigsEnabled = ConfigSplitter.isSplittingEnabled();
        boolean externalPermsEnabled = false;
        try {
            externalPermsEnabled = isExternalPermissionsEnabled();
        } catch (Exception ignored) {}

        if (splitConfigsEnabled) {
            // Always ensure split configs are up to date
            LOGGER.info("Split configs enabled - ensuring all split config files are up to date");
            ConfigSplitter.ensureSplitConfigsUpToDate();

            // Only check other standalone configs (economy, permissions, kits, discord_auth)
            for (String configName : requiredConfigs) {
                if (configName.equals(MAIN_CONFIG)) {
                    continue; // Skip config.json when using split configs
                }
                if (configName.equals(PERMISSIONS_CONFIG) && externalPermsEnabled) {
                    // Skip internal permissions.json if external permissions are enabled
                    continue;
                }
                File configFile = ResourceUtil.getConfigFile(configName);
                if (!configFile.exists()) {
                    copyDefaultConfig(configName, configFile);
                } else {
                    checkAndUpdateConfigVersion(configName, configFile);
                }
            }
        } else {
            // Normal monolithic config mode
            for (String configName : requiredConfigs) {
                if (configName.equals(PERMISSIONS_CONFIG) && externalPermsEnabled) {
                    // Skip internal permissions.json if external permissions are enabled
                    continue;
                }
                File configFile = ResourceUtil.getConfigFile(configName);
                if (!configFile.exists()) {
                    copyDefaultConfig(configName, configFile);
                } else {
                    checkAndUpdateConfigVersion(configName, configFile);
                }
            }
        }
    }

    /**
     * Check if a config file needs updating based on version mismatch.
     * <p>
     * Strategy:
     *  - If the on-disk version is OLDER than expected → merge new/changed keys from the JAR
     *    template into the user's file (preserve all existing values) then bump _configVersion.
     *    A backup is still created before touching the file.
     *  - If the on-disk version is NEWER than expected → warn only, do not touch.
     *  - If equal → no-op.
     * <p>
     * This prevents blowing away user-set values (role IDs, client secrets, custom settings)
     * every time the config gains a new field.
     */
    private void checkAndUpdateConfigVersion(String configName, File configFile) {
        Integer expectedVersion = EXPECTED_CONFIG_VERSIONS.get(configName);
        if (expectedVersion == null) {
            return; // No version tracking for this config
        }

        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            JsonObject onDisk = parseJsonWithComments(reader).getAsJsonObject();

            int currentVersion = 0;
            if (onDisk.has(CONFIG_VERSION_KEY)) {
                currentVersion = onDisk.get(CONFIG_VERSION_KEY).getAsInt();
            }

            if (currentVersion < expectedVersion) {
                LOGGER.warn("Config file {} is outdated (version {} < {}). Merging new keys from JAR template (user values preserved)...",
                    configName, currentVersion, expectedVersion);

                // Load JAR template (may contain // comments — use lenient reader)
                JsonObject jarTemplate = null;
                try (InputStream in = ResourceUtil.getJarConfigResource(configName)) {
                    if (in != null) {
                        jarTemplate = parseJsonWithComments(
                            new java.io.InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    }
                } catch (Exception e) {
                    LOGGER.error("Could not load JAR template for {}: {}", configName, e.getMessage());
                }

                if (jarTemplate == null) {
                    LOGGER.warn("JAR template not found for {}. Skipping update.", configName);
                    return;
                }

                // Create backup before modifying
                createConfigBackup(configFile, currentVersion);

                // Deep-merge: add keys that exist in JAR but are missing on disk.
                // Never overwrite existing user values.
                boolean changed = mergeNewKeys(jarTemplate, onDisk);

                // Strip legacy comment keys (xxx_comment, _doc_*, _step*, etc.)
                // from the user's file as part of this upgrade.
                boolean stripped = stripLegacyCommentKeys(onDisk);
                if (stripped) {
                    changed = true;
                    LOGGER.info("Config file {}: removed legacy _comment/_doc keys (comment migration).", configName);
                }

                // Always bump the version so we don't re-run this on next start
                onDisk.addProperty(CONFIG_VERSION_KEY, expectedVersion);

                // Write merged result back
                try (java.io.FileWriter writer = new java.io.FileWriter(configFile, StandardCharsets.UTF_8)) {
                    new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create().toJson(onDisk, writer);
                }

                configCache.remove(configName);
                LOGGER.info("Config file {} merged to version {} ({} new key(s) added).",
                    configName, expectedVersion, changed ? "some" : "no");

                com.zerog.neoessentials.util.MessageUtil.ensureLanguageFileUpToDate();

            } else if (currentVersion > expectedVersion) {
                LOGGER.warn("Config file {} has a newer version ({}) than expected ({}). This may indicate a downgrade.",
                    configName, currentVersion, expectedVersion);
            } else {
                LOGGER.debug("Config file {} is up to date (version {})", configName, currentVersion);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to check/update version for config {}: {}", configName, e.getMessage(), e);
        }
    }

    /**
     * Deep-merge {@code source} into {@code target}: for every key in source that is missing
     * in target, add it. Recurse into nested objects. Never overwrite existing values.
     *
     * @return true if at least one key was added
     */
    private boolean mergeNewKeys(com.google.gson.JsonObject source, com.google.gson.JsonObject target) {
        boolean changed = false;
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : source.entrySet()) {
            String key = entry.getKey();
            com.google.gson.JsonElement sourceVal = entry.getValue();

            if (!target.has(key)) {
                // Missing entirely — add from template
                target.add(key, sourceVal.deepCopy());
                changed = true;
                LOGGER.debug("  + Added missing config key: {}", key);
            } else if (sourceVal.isJsonObject() && target.get(key).isJsonObject()) {
                // Both sides are objects — recurse
                changed |= mergeNewKeys(sourceVal.getAsJsonObject(), target.get(key).getAsJsonObject());
            }
            // If key exists and isn't an object, leave the user's value alone
        }
        return changed;
    }

    /**
     * Recursively remove legacy "comment" keys from a {@link JsonObject}.
     *
     * <p>A key is considered a legacy comment key if it:
     * <ul>
     *   <li>ends with {@code _comment}  (e.g. {@code currencySymbol_comment})</li>
     *   <li>ends with {@code -description} (e.g. {@code chat-format-description})</li>
     *   <li>starts with {@code _} <em>and</em> is not {@code _configVersion}
     *       (catches {@code _comment}, {@code _doc_*}, {@code _step*}, {@code _how_*},
     *        {@code _example}, {@code _role_*}, {@code _important}, etc.)</li>
     * </ul>
     *
     * @return {@code true} if at least one key was removed
     */
    private boolean stripLegacyCommentKeys(com.google.gson.JsonObject obj) {
        boolean changed = false;
        List<String> toRemove = new ArrayList<>();
        for (java.util.Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
            String key = entry.getKey();
            if (isLegacyCommentKey(key)) {
                toRemove.add(key);
            } else if (entry.getValue().isJsonObject()) {
                changed |= stripLegacyCommentKeys(entry.getValue().getAsJsonObject());
            }
        }
        for (String key : toRemove) {
            obj.remove(key);
            changed = true;
            LOGGER.debug("  - Removed legacy comment key: {}", key);
        }
        return changed;
    }

    private static boolean isLegacyCommentKey(String key) {
        if ("_configVersion".equals(key)) return false; // keep the version field
        // Pattern: ends with _comment, ends with -description, or starts with _ (doc/step/how/example...)
        return key.endsWith("_comment") || key.endsWith("-description") || key.startsWith("_");
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
     * Returns whether economy transactions should be logged to logs/neoessentials/transactions.log
     * (economy.json logTransactions). Defaults to true if not set.
     */
    public static boolean isLogTransactionsEnabled() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("logTransactions")) {
            try {
                return config.get("logTransactions").getAsBoolean();
            } catch (Exception ignored) {}
        }
        return true;
    }

    /**
     * Returns the max transactions kept per player for /history
     * (economy.json transactionHistoryLimit). Defaults to 20 if not set.
     */
    public static int getTransactionHistoryLimit() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("transactionHistoryLimit")) {
            try {
                int val = config.get("transactionHistoryLimit").getAsInt();
                if (val > 0) return val;
            } catch (Exception ignored) {}
        }
        return 20;
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
     * Returns the singular currency name from economy.json (currencyName).
     * Defaults to the currency symbol if not set.
     */
    public static String getCurrencyName() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("currencyName")) {
            try {
                String name = config.get("currencyName").getAsString();
                if (name != null && !name.isBlank()) return name;
            } catch (Exception ignored) {}
        }
        return getCurrencySymbol();
    }

    /**
     * Returns the plural currency name from economy.json (currencyNamePlural).
     * Defaults to the singular currency name if not set.
     */
    public static String getCurrencyNamePlural() {
        JsonObject config = getInstance().getConfig(ECONOMY_CONFIG);
        if (config.has("currencyNamePlural")) {
            try {
                String name = config.get("currencyNamePlural").getAsString();
                if (name != null && !name.isBlank()) return name;
            } catch (Exception ignored) {}
        }
        return getCurrencyName();
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
            JsonObject kits = config.getAsJsonObject("kits");
            if (kits.has("commandCosts") && kits.get("commandCosts").isJsonObject()) {
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
        if (config.has("kits") && config.get("kits").isJsonObject()) {
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
     * Returns the ban message format from moderation.banSettings.banMessageFormat
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
     * Returns the temp ban message format from moderation.banSettings.tempBanMessageFormat
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
     * Returns true if per-warp permission checks are enabled (teleportation.warpSettings.perWarpPermission).
     * Essentials: getPerWarpPermission() — checks neoessentials.warps.<name> per warp.
     * Defaults to false if not set.
     */
    public boolean isPerWarpPermissionEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("warpSettings")) {
                JsonObject warpSettings = tp.getAsJsonObject("warpSettings");
                if (warpSettings.has("perWarpPermission")) {
                    return warpSettings.get("perWarpPermission").getAsBoolean();
                }
            }
        }
        return false;
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
     * Returns the max economy amount a single validated input can be — reuses
     * economy.json's maxBalance (there's no separate security-side cap; a
     * transaction amount can never usefully exceed the max a balance can hold).
     */
    public BigDecimal getMaxEconomyAmount() {
        return BigDecimal.valueOf(getMaxBalance());
    }

    /**
     * Returns the minimum economy amount a validated input must be. Not
     * separately configurable — 0.01 is the smallest unit two-decimal
     * currency formatting can display.
     */
    public BigDecimal getMinEconomyAmount() {
        return BigDecimal.valueOf(0.01);
    }

    /**
     * Returns whether unsafe commands are allowed from security.allowUnsafeCommands.
     * Defaults to true (the dangerous-pattern/character check in
     * {@link com.zerog.neoessentials.util.InputValidator#validateCommand} is opt-in via
     * this flag, not opt-out) — set it to {@code false} explicitly to enable stricter
     * command scanning for powertool-bound commands.
     */
    public boolean isUnsafeCommandsAllowed() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("security")) {
            JsonObject security = config.getAsJsonObject("security");
            if (security.has("allowUnsafeCommands")) {
                return security.get("allowUnsafeCommands").getAsBoolean();
            }
        }
        return true;
    }

    /**
     * Returns max unsafe enchantment level from items.max-unsafe-enchantment-level.
     * Defaults to 10 if not set.
     */
    public int getMaxUnsafeEnchantmentLevel() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("items")) {
            JsonObject items = config.getAsJsonObject("items");
            if (items.has("max-unsafe-enchantment-level")) {
                try {
                    return items.get("max-unsafe-enchantment-level").getAsInt();
                } catch (Exception ignored) {}
            }
        }
        return 10;
    }

    /**
     * Check if jail system is enabled (moderation.jailSettings.enableJailSystem)
     */
    public static boolean isJailSystemEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jailSettings = moderation.getAsJsonObject("jailSettings");
                if (jailSettings.has("enableJailSystem")) {
                    return jailSettings.get("enableJailSystem").getAsBoolean();
                }
            }
        }
        return true; // Default to enabled
    }

    /**
     * Get max jails before permanent ban from moderation.jailSettings.maxJailsBeforePermBan
     * Defaults to 3 if not set
     */
    public static int getMaxJailsBeforePermBan() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jail = moderation.getAsJsonObject("jailSettings");
                if (jail.has("maxJailsBeforePermBan")) {
                    return jail.get("maxJailsBeforePermBan").getAsInt();
                }
            }
        }
        return 3;
    }

    /**
     * Get temp ban duration in minutes from moderation.jailSettings.tempBanDurationMinutes
     * Defaults to 1440 (24 hours) if not set
     */
    public static int getTempBanDurationMinutes() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("moderation")) {
            JsonObject moderation = config.getAsJsonObject("moderation");
            if (moderation.has("jailSettings")) {
                JsonObject jail = moderation.getAsJsonObject("jailSettings");
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
     * Check if debug mode is enabled from logging.enableDebugLogging
     * Defaults to false if not set
     */
    public static boolean isDebugModeEnabled() {
        JsonObject config = getInstance().getConfig(MAIN_CONFIG);
        if (config.has("logging")) {
            JsonObject logging = config.getAsJsonObject("logging");
            if (logging.has("enableDebugLogging")) {
                return logging.get("enableDebugLogging").getAsBoolean();
            }
        }
        return false;
    }

    /**
     * Returns the teleport delay (in seconds) for the /back command.
     * Reads from teleportation.backSettings.teleportDelay first, then falls back to
     * teleportation.generalSettings.teleportDelay.  Default: 3.
     */
    public int getBackTeleportDelay() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("backSettings")) {
                JsonObject bs = tp.getAsJsonObject("backSettings");
                if (bs.has("teleportDelay")) {
                    try {
                        int val = bs.get("teleportDelay").getAsInt();
                        if (val >= 0) return val;
                    } catch (Exception ignored) {}
                }
            }
            if (tp.has("generalSettings")) {
                JsonObject gs = tp.getAsJsonObject("generalSettings");
                if (gs.has("teleportDelay")) {
                    try {
                        int val = gs.get("teleportDelay").getAsInt();
                        if (val >= 0) return val;
                    } catch (Exception ignored) {}
                }
            }
        }
        return 3;
    }

    /**
     * Returns whether death locations should be saved for /back.
     * Reads from teleportation.backSettings.enableDeathBack.  Default: true.
     */
    public boolean isDeathBackEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("backSettings")) {
                JsonObject bs = tp.getAsJsonObject("backSettings");
                if (bs.has("enableDeathBack")) {
                    return bs.get("enableDeathBack").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Returns whether the previous location is saved before each teleport (for /back).
     * Reads from teleportation.backSettings.enableTeleportBack.  Default: true.
     */
    public boolean isTeleportBackEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("backSettings")) {
                JsonObject bs = tp.getAsJsonObject("backSettings");
                if (bs.has("enableTeleportBack")) {
                    return bs.get("enableTeleportBack").getAsBoolean();
                }
            }
        }
        return true;
    }

    /**
     * Save config changes. If split configs are enabled, only write to split files, never to config.json.
     */
    public void saveConfig(String configName, JsonObject config) {
        lock.writeLock().lock();
        try {
            if (ConfigSplitter.isSplittingEnabled() && configName.equals(MAIN_CONFIG)) {
                LOGGER.info("Split configs enabled - writing changes to split files instead of config.json");
                ConfigSplitter.saveMergedConfigToSplitFiles(config);
                configCache.put(configName, config);
                return;
            }
            File file = ResourceUtil.getConfigFile(configName);
            try (FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8)) {
                com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
                gson.toJson(config, writer);
            }
            configCache.put(configName, config);
        } catch (IOException e) {
            LOGGER.error("Failed to save config file {}: {}", configName, e.getMessage());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns true if back/death teleport safety is enabled in teleportation.backSettings config section.
     * Key: teleportation.backSettings.enableBackSafety (defaults to true when absent).
     */
    public boolean isBackTeleportSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("backSettings")) {
                JsonObject backSettings = tp.getAsJsonObject("backSettings");
                if (backSettings.has("enableBackSafety")) {
                    return backSettings.get("enableBackSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true for safety
    }

    /**
     * Returns true if spawn teleport safety is enabled in teleportation.spawnSettings config section.
     * (teleportation.spawnSettings.enableSpawnSafety)
     */
    public boolean isSpawnSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("spawnSettings")) {
                JsonObject spawnSettings = tp.getAsJsonObject("spawnSettings");
                if (spawnSettings.has("enableSpawnSafety")) {
                    return spawnSettings.get("enableSpawnSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true for safety
    }

    /**
     * Returns true if home teleport safety is enabled in teleportation.homeSettings config section.
     * Accepts both "enableHomeTeleportSafety" (canonical) and "enableHomeSafety" (alias, consistent with enableWarpSafety).
     * (teleportation.homeSettings.enableHomeTeleportSafety or teleportation.homeSettings.enableHomeSafety)
     */
    public boolean isHomeTeleportSafetyEnabled() {
        JsonObject config = getConfig(MAIN_CONFIG);
        if (config.has("teleportation")) {
            JsonObject tp = config.getAsJsonObject("teleportation");
            if (tp.has("homeSettings")) {
                JsonObject homeSettings = tp.getAsJsonObject("homeSettings");
                // Accept canonical key first
                if (homeSettings.has("enableHomeTeleportSafety")) {
                    return homeSettings.get("enableHomeTeleportSafety").getAsBoolean();
                }
                // Also accept alias key (enableHomeSafety, consistent with enableWarpSafety naming)
                if (homeSettings.has("enableHomeSafety")) {
                    return homeSettings.get("enableHomeSafety").getAsBoolean();
                }
            }
        }
        return true; // Default to true for safety
    }

    /**
     * Ensure split configs are present and up to date on startup
     */
    public static void ensureSplitConfigsOnStartup() {
        if (ConfigSplitter.isSplittingEnabled()) {
            ConfigSplitter.ensureSplitConfigsUpToDate();
        }
    }


    /**
     * Get the config directory, using ResourceUtil for centralized path management
     */
    private static File getConfigDirectory() {
        File configDir = new File(ResourceUtil.CONFIG_DIR);
        ResourceUtil.ensureDirectoryExists(ResourceUtil.CONFIG_DIR);
        return configDir;
    }

    /** Returns the backup-command string from commands.backupCommand, or null if not set. */
    public String getBackupCommand() {
        try {
            JsonObject config = getConfig(MAIN_CONFIG);
            if (config.has("commands") && config.getAsJsonObject("commands").has("backupCommand")) {
                String val = config.getAsJsonObject("commands").get("backupCommand").getAsString();
                return val.isBlank() ? null : val;
            }
        } catch (Exception ignored) {}
        return null;
    }
}
