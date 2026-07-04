package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conditional Formatting Manager - Phase 4
 * Provides conditional chat formatting based on:
 * - Time of day (<if:time=morning>Good morning!</if>)
 * - Player stats (<if:health<50>Low health!</if>)
 * - Events (<if:afk>AFK</if>)
 */
public class ConditionalFormatter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalFormatter.class);

    // Patterns for conditional tags
    private static final Pattern IF_TIME_PATTERN = Pattern.compile("<if:time=([a-z]+)>(.*?)</if>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_STAT_PATTERN = Pattern.compile("<if:([a-z]+)([<>=!]+)([0-9.]+)>(.*?)</if>", Pattern.CASE_INSENSITIVE);
    private static final Pattern IF_CONDITION_PATTERN = Pattern.compile("<if:([a-z]+)>(.*?)</if>", Pattern.CASE_INSENSITIVE);

    /**
     * Process conditional formatting based on player state.
     */
    public static String processConditionals(ServerPlayer player, String text) {
        if (!isConditionalFormattingEnabled()) {
            return text;
        }

        try {
            // Process time-based conditionals
            if (isTimeConditionalsAllowed()) text = processTimeConditionals(text);

            // Process stat-based conditionals
            if (isStatConditionalsAllowed()) text = processStatConditionals(player, text);

            // Process state-based conditionals
            if (isStateConditionalsAllowed()) text = processStateConditionals(player, text);

            return text;

        } catch (Exception e) {
            LOGGER.error("Error processing conditional formatting: {}", e.getMessage(), e);
            return text;
        }
    }

    /**
     * Process time-based conditionals.
     */
    private static String processTimeConditionals(String text) {
        Matcher matcher = IF_TIME_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String timeCondition = matcher.group(1).toLowerCase();
            String content = matcher.group(2);

            boolean shouldShow = checkTimeCondition(timeCondition);
            String replacement = shouldShow ? content : "";

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Check if time condition is met.
     */
    private static boolean checkTimeCondition(String condition) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        return switch (condition) {
            case "morning" -> hour >= 6 && hour < 12;
            case "afternoon" -> hour >= 12 && hour < 18;
            case "evening" -> hour >= 18 && hour < 22;
            case "night" -> hour >= 22 || hour < 6;
            case "day" -> hour >= 6 && hour < 18;
            case "weekday" -> now.getDayOfWeek().getValue() <= 5;
            case "weekend" -> now.getDayOfWeek().getValue() >= 6;
            default -> false;
        };
    }

    /**
     * Process stat-based conditionals.
     */
    private static String processStatConditionals(ServerPlayer player, String text) {
        Matcher matcher = IF_STAT_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String stat = matcher.group(1).toLowerCase();
            String operator = matcher.group(2);
            double threshold = Double.parseDouble(matcher.group(3));
            String content = matcher.group(4);

            boolean shouldShow = checkStatCondition(player, stat, operator, threshold);
            String replacement = shouldShow ? content : "";

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Check if stat condition is met.
     */
    private static boolean checkStatCondition(ServerPlayer player, String stat, String operator, double threshold) {
        double value = switch (stat) {
            case "health" -> player.getHealth();
            case "level" -> player.experienceLevel;
            case "food" -> player.getFoodData().getFoodLevel();
            case "armor" -> player.getArmorValue();
            case "xp" -> player.totalExperience;
            default -> 0;
        };

        return switch (operator) {
            case "<" -> value < threshold;
            case ">" -> value > threshold;
            case "<=" -> value <= threshold;
            case ">=" -> value >= threshold;
            case "=", "==" -> Math.abs(value - threshold) < 0.001;
            case "!=" -> Math.abs(value - threshold) >= 0.001;
            default -> false;
        };
    }

    /**
     * Process state-based conditionals.
     */
    private static String processStateConditionals(ServerPlayer player, String text) {
        Matcher matcher = IF_CONDITION_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String condition = matcher.group(1).toLowerCase();
            String content = matcher.group(2);

            boolean shouldShow = checkStateCondition(player, condition);
            String replacement = shouldShow ? content : "";

            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Check if state condition is met.
     */
    @SuppressWarnings("resource") // player.level() is not AutoCloseable, false positive
    private static boolean checkStateCondition(ServerPlayer player, String condition) {
        return switch (condition) {
            case "afk" -> isPlayerAfk(player);
            case "vanished" -> isPlayerVanished(player);
            case "flying" -> player.getAbilities().flying;
            case "creative" -> player.isCreative();
            case "survival" -> player.gameMode.getGameModeForPlayer() == net.minecraft.world.level.GameType.SURVIVAL;
            case "spectator" -> player.isSpectator();
            case "op" -> com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 2);
            case "sneaking" -> player.isCrouching();
            case "sprinting" -> player.isSprinting();
            case "swimming" -> player.isSwimming();
            case "onfire" -> player.isOnFire();
            case "wet" -> player.isInWaterOrRain();
            case "underground" -> player.getY() < 62;
            case "nether" -> player.level().dimension().identifier().getPath().equals("the_nether");
            case "end" -> player.level().dimension().identifier().getPath().equals("the_end");
            case "overworld" -> player.level().dimension().identifier().getPath().equals("overworld");
            default -> false;
        };
    }

    // Helper methods

    private static boolean isPlayerAfk(ServerPlayer player) {
        try {
            var afkManager = com.zerog.neoessentials.chat.AfkManager.getInstance();
            return afkManager.isAfk(player);
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isPlayerVanished(ServerPlayer player) {
        try {
            var vanishManager = com.zerog.neoessentials.moderation.VanishManager.getInstance();
            return vanishManager.isPlayerVanished(player.getUUID());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isConditionalFormattingEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("conditionalFormatting")) {
                return chatConfig.getAsJsonObject("conditionalFormatting").get("enabled").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private static boolean isTimeConditionalsAllowed() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            var cf = chatConfig.getAsJsonObject("conditionalFormatting");
            if (cf != null && cf.has("allowTimeConditionals")) {
                return cf.get("allowTimeConditionals").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private static boolean isStatConditionalsAllowed() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            var cf = chatConfig.getAsJsonObject("conditionalFormatting");
            if (cf != null && cf.has("allowStatConditionals")) {
                return cf.get("allowStatConditionals").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private static boolean isStateConditionalsAllowed() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            var cf = chatConfig.getAsJsonObject("conditionalFormatting");
            if (cf != null && cf.has("allowStateConditionals")) {
                return cf.get("allowStateConditionals").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }
}
