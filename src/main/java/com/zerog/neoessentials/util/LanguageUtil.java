package com.zerog.neoessentials.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.NeoEssentials;

/**
 * Language manager for multi-language support in NeoEssentials.
 * <p>
 * This utility class handles translation of all user-facing messages
 * using Minecraft's built-in translation system with lang files.
 * </p>
 * 
 * @author ZeroG
 * @since 1.0.2.99
 */
public class LanguageUtil {

    /**
     * Gets a translated component with the specified translation key.
     * 
     * @param key The translation key
     * @param args Optional arguments for string formatting
     * @return Translated component
     */
    public static MutableComponent getTranslated(String key, Object... args) {
        try {
            return Component.translatable(key, args);
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to translate key '{}': {}", key, e.getMessage());
            return Component.literal("[MISSING TRANSLATION: " + key + "]");
        }
    }

    /**
     * Gets a translated component with color formatting applied.
     * 
     * @param key The translation key
     * @param args Optional arguments for string formatting
     * @return Translated component with color formatting
     */
    public static MutableComponent getTranslatedColored(String key, Object... args) {
        MutableComponent component = getTranslated(key, args);
        return ColorUtils.applyColorCodes(component);
    }

    /**
     * Sends a translated message to a player.
     * 
     * @param player The player to send the message to
     * @param key The translation key
     * @param args Optional arguments for string formatting
     */
    public static void sendTranslated(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(getTranslated(key, args));
    }

    /**
     * Sends a translated message with color formatting to a player.
     * 
     * @param player The player to send the message to
     * @param key The translation key
     * @param args Optional arguments for string formatting
     */
    public static void sendTranslatedColored(ServerPlayer player, String key, Object... args) {
        player.sendSystemMessage(getTranslatedColored(key, args));
    }

    // Common error messages
    public static MutableComponent noPermission() {
        return getTranslated("neoessentials.commands.error.no_permission");
    }

    public static MutableComponent playerNotFound(String playerName) {
        return getTranslated("neoessentials.commands.error.player_not_found", playerName);
    }

    public static MutableComponent invalidArguments(String usage) {
        return getTranslated("neoessentials.commands.error.invalid_arguments", usage);
    }

    public static MutableComponent commandFailed(String reason) {
        return getTranslated("neoessentials.commands.error.command_failed", reason);
    }

    public static MutableComponent notPlayer() {
        return getTranslated("neoessentials.commands.error.not_player");
    }

    public static MutableComponent targetOffline() {
        return getTranslated("neoessentials.commands.error.target_offline");
    }

    public static MutableComponent invalidAmount(String amount) {
        return getTranslated("neoessentials.commands.error.invalid_amount", amount);
    }

    public static MutableComponent insufficientFunds(String needed, String current) {
        return getTranslated("neoessentials.commands.error.insufficient_funds", needed, current);
    }

    // Admin permission messages
    public static MutableComponent adminRequired() {
        return getTranslated("neoessentials.permissions.admin_required");
    }

    public static MutableComponent opRequired() {
        return getTranslated("neoessentials.permissions.op_required");
    }

    public static MutableComponent insufficientPermissions() {
        return getTranslated("neoessentials.permissions.insufficient");
    }

    // Success messages
    public static MutableComponent genericSuccess() {
        return getTranslated("neoessentials.commands.success.generic");
    }

    public static MutableComponent playerAction(String playerName) {
        return getTranslated("neoessentials.commands.success.player_action", playerName);
    }

    public static MutableComponent amountTransferred(String amount, String target) {
        return getTranslated("neoessentials.commands.success.amount_transferred", amount, target);
    }
}
