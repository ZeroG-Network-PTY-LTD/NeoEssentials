package com.zerog.neoessentials.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.NeoEssentials;

import java.util.HashMap;
import java.util.Map;

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

    // Fallback messages for critical keys when translation fails
    private static final Map<String, String> FALLBACK_MESSAGES = new HashMap<>();
    
    static {
        // Economy messages
        FALLBACK_MESSAGES.put("neoessentials.economy.financial_status_header", "&6====== Financial Status ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.cash_on_hand", "&eCash on Hand: &a%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_account_balance", "&eBank Account: &a%s &7(Account: %s)");
        FALLBACK_MESSAGES.put("neoessentials.economy.total_wealth", "&eTotal Wealth: &a%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.no_bank_account", "&7No bank account found");
        FALLBACK_MESSAGES.put("neoessentials.economy.balance_check_error", "&cError checking balance: %s");
        
        // Command messages
        FALLBACK_MESSAGES.put("commands.tphistory.no_history", "&7You have no teleport history yet");
        FALLBACK_MESSAGES.put("commands.tphistory.header", "&6====== Teleport History ======");
        FALLBACK_MESSAGES.put("commands.tphistory.footer", "&6==============================");
        FALLBACK_MESSAGES.put("commands.tphistory.entry", "&e%s. &f%s &7(%s) &8- %s");
        
        // Teleport bookmark messages
        FALLBACK_MESSAGES.put("commands.tpbookmark.not_found", "&cBookmark '%s' not found!");
        FALLBACK_MESSAGES.put("commands.tpbookmark.removed", "&aBookmark '%s' removed successfully");
        
        // Error messages
        FALLBACK_MESSAGES.put("neoessentials.commands.error.no_permission", "&cYou don't have permission to use this command!");
        FALLBACK_MESSAGES.put("neoessentials.commands.error.player_not_found", "&cPlayer not found: %s");
        FALLBACK_MESSAGES.put("neoessentials.commands.error.invalid_arguments", "&cInvalid arguments. Use: %s");
        FALLBACK_MESSAGES.put("neoessentials.commands.error.command_failed", "&cCommand failed: %s");
        FALLBACK_MESSAGES.put("neoessentials.commands.error.not_player", "&cThis command can only be used by players!");
        FALLBACK_MESSAGES.put("neoessentials.commands.error.target_offline", "&cTarget player is offline!");
        
        // Teleport messages
        FALLBACK_MESSAGES.put("neoessentials.teleport.teleporting_to_player", "&aTeleporting to %s.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.player_teleporting_to_you", "&a%s is teleporting to you.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.you_denied_request", "&c%s denied your teleport request.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.requester_offline", "&cThe player who sent the teleport request is no longer online.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.no_previous_location", "&cYou have no previous location to return to.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.dimension_error", "&cError: Could not find the dimension you were previously in.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.cooldown_active", "&cYou must wait %s seconds before teleporting again.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.request_sent", "&aTeleport request sent to %s!");
        FALLBACK_MESSAGES.put("neoessentials.teleport.request_received", "&e%s wants to teleport to you. Use /tpaccept or /tpdeny.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.request_received_here", "&e%s wants you to teleport to them. Use /tpaccept or /tpdeny.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.request_denied", "&cTeleport request denied.");
        FALLBACK_MESSAGES.put("neoessentials.teleport.back_success", "&aTeleported to your previous location!");
        
        // Home messages
        FALLBACK_MESSAGES.put("neoessentials.home.teleported_default", "&aTeleported to home!");
        FALLBACK_MESSAGES.put("neoessentials.home.teleported", "&aTeleported to home '%s'!");
        FALLBACK_MESSAGES.put("neoessentials.home.set_default", "&aHome set successfully!");
        FALLBACK_MESSAGES.put("neoessentials.home.set_success", "&aHome '%s' set successfully!");
        FALLBACK_MESSAGES.put("neoessentials.home.deleted", "&aHome '%s' deleted successfully!");
        FALLBACK_MESSAGES.put("neoessentials.home.list_empty", "&7You have no homes set. Use /sethome to set one!");
        FALLBACK_MESSAGES.put("neoessentials.home.list_header", "&6Your homes:");
        FALLBACK_MESSAGES.put("neoessentials.home.list_format", "&e- %s");
        
        // Spawn messages
        FALLBACK_MESSAGES.put("neoessentials.spawn.teleported", "&aTeleported to spawn!");
        FALLBACK_MESSAGES.put("neoessentials.spawn.set_success", "&aSpawn set successfully!");
        
        // Economy additional messages
        FALLBACK_MESSAGES.put("neoessentials.economy.financial_status_header_other", "&6====== %s's Financial Status ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.no_bank_account_other", "&eBank Account: &7None");
        FALLBACK_MESSAGES.put("neoessentials.economy.balance", "&aYour balance: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.balance_other", "&a%s's balance: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.deposit_success", "&aDeposited %s into your bank account");
        FALLBACK_MESSAGES.put("neoessentials.economy.withdraw_success", "&aWithdrew %s from your bank account");
        FALLBACK_MESSAGES.put("neoessentials.economy.insufficient_bank_funds", "&cInsufficient bank funds! Need %s but account has %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.deposit_prompt", "&eEnter the amount to deposit:");
        FALLBACK_MESSAGES.put("neoessentials.economy.withdraw_prompt", "&eEnter the amount to withdraw:");
        FALLBACK_MESSAGES.put("neoessentials.economy.transfer_coming_soon", "&7Bank transfers coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_coming_soon", "&7Account settings coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.account_created", "&aCreated %s account with number %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.account_creation_failed", "&cFailed to create account: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.money_sent", "&aSent %s to %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.money_received", "&aReceived %s from %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.cannot_send_to_self", "&cYou cannot send money to yourself!");
        FALLBACK_MESSAGES.put("neoessentials.economy.transfer_failed", "&cMoney transfer failed!");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_recipient_prompt", "&eEnter the recipient's name for %s:");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_amount_prompt", "&eEnter the amount to send:");
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_history_coming_soon", "&7Transaction history coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_coming_soon", "&7Loan system coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.exchange_coming_soon", "&7Currency exchange coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.stats_coming_soon", "&7Financial statistics coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.economy_settings_coming_soon", "&7Economy settings coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_header", "&6====== Send Money ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_instructions", "&7Use the pay command to send money:");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_usage", "&e/pay <player> <amount>");
        FALLBACK_MESSAGES.put("neoessentials.economy.transfer_error", "&cTransfer error: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.gui_unavailable", "&cEconomy GUI is currently unavailable");
        FALLBACK_MESSAGES.put("neoessentials.economy.using_chat_interface", "&7Using chat-based interface instead");
        
        // World management messages
        FALLBACK_MESSAGES.put("commands.worldinfo.header", "&6====== World Information ======");
        FALLBACK_MESSAGES.put("commands.worldinfo.footer", "&6================================");
        FALLBACK_MESSAGES.put("commands.worldinfo.dimension_not_found", "&cDimension '%s' not found!");
        FALLBACK_MESSAGES.put("commands.worldinfo.error", "&cError retrieving world information: %s");
        
        // Dimension messages
        FALLBACK_MESSAGES.put("commands.dimensions.header", "&6====== Available Dimensions ======");
        FALLBACK_MESSAGES.put("commands.dimensions.footer", "&6===================================");
        FALLBACK_MESSAGES.put("commands.dimensions.entry", "&e%d. &f%s &7(%s) &8- %s");
        FALLBACK_MESSAGES.put("commands.dimensions.no_dimensions", "&cNo dimensions found!");
        FALLBACK_MESSAGES.put("commands.dimensions.teleport_success", "&aTeleported to dimension: &e%s");
        FALLBACK_MESSAGES.put("commands.dimensions.teleport_failed", "&cFailed to teleport to dimension: %s");
        FALLBACK_MESSAGES.put("commands.dimensions.invalid_dimension", "&cInvalid dimension: %s");
    }

    /**
     * Gets a translated component with the specified translation key.
     * Uses fallback messages if translation fails.
     * 
     * @param key The translation key
     * @param args Optional arguments for string formatting
     * @return Translated component or fallback
     */
    public static MutableComponent getTranslated(String key, Object... args) {
        try {
            // For mod-specific keys, use fallback directly as Component.translatable 
            // doesn't work reliably for mod translations
            if (key.startsWith("neoessentials.") || key.startsWith("commands.")) {
                return getFallbackComponent(key, args);
            }
            
            // For vanilla keys, try Minecraft's translation system
            MutableComponent translated = Component.translatable(key, args);
            
            // Check if the translation actually worked (rough heuristic)
            String plainText = translated.getString();
            if (plainText.equals(key) || plainText.startsWith(key)) {
                // Translation likely failed, use fallback
                return getFallbackComponent(key, args);
            }
            
            return translated;
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to translate key '{}': {}", key, e.getMessage());
            return getFallbackComponent(key, args);
        }
    }
    
    /**
     * Gets a fallback component when translation fails.
     */
    private static MutableComponent getFallbackComponent(String key, Object... args) {
        String fallback = FALLBACK_MESSAGES.get(key);
        if (fallback != null) {
            try {
                String formatted = String.format(fallback, args);
                return Component.literal(formatText(formatted));
            } catch (Exception e) {
                NeoEssentials.LOGGER.warn("Failed to format fallback for key '{}': {}", key, e.getMessage());
                return Component.literal(fallback);
            }
        } else {
            // No fallback available, show a helpful error
            NeoEssentials.LOGGER.warn("No fallback message for key: {}", key);
            return Component.literal("§c[MISSING: " + key + "]");
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

    /**
     * Sends a translated message to a player.
     * 
     * @param player The player to send the message to
     * @param key The translation key
     * @param args Optional arguments for string formatting
     */
    public static void sendMessage(ServerPlayer player, String key, Object... args) {
        sendTranslatedColored(player, key, args);
    }

    /**
     * Sends a translated message to a command source.
     * 
     * @param source The command source
     * @param key The translation key
     * @param args Optional arguments for string formatting
     */
    public static void sendMessage(CommandSourceStack source, String key, Object... args) {
        try {
            MutableComponent message = getTranslatedColored(key, args);
            source.sendSuccess(() -> message, false);
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to send message to source with key '{}': {}", key, e.getMessage());
            source.sendFailure(Component.literal("[ERROR: " + key + "]"));
        }
    }

    /**
     * Sends an error message to a command source.
     * 
     * @param source The command source
     * @param key The translation key
     * @param args Optional arguments for string formatting
     */
    public static void sendErrorMessage(CommandSourceStack source, String key, Object... args) {
        try {
            MutableComponent message = getTranslatedColored(key, args);
            source.sendFailure(message);
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to send error message to source with key '{}': {}", key, e.getMessage());
            source.sendFailure(Component.literal("[ERROR: " + key + "]"));
        }
    }

    /**
     * Sends an error message to a player.
     * 
     * @param player The player
     * @param key The translation key
     * @param args Optional arguments for string formatting
     */
    public static void sendErrorMessage(ServerPlayer player, String key, Object... args) {
        try {
            MutableComponent message = getTranslatedColored(key, args);
            player.sendSystemMessage(message);
        } catch (Exception e) {
            NeoEssentials.LOGGER.warn("Failed to send error message to player '{}' with key '{}': {}", 
                player.getDisplayName().getString(), key, e.getMessage());
            player.sendSystemMessage(Component.literal("[ERROR: " + key + "]"));
        }
    }

    /**
     * Sends a component message directly to a player.
     * 
     * @param player The player to send the message to
     * @param component The component to send
     */
    public static void sendComponent(ServerPlayer player, Component component) {
        player.sendSystemMessage(component);
    }

    /**
     * Sends a component message directly to a command source.
     * 
     * @param source The command source
     * @param component The component to send
     */
    public static void sendComponent(CommandSourceStack source, Component component) {
        source.sendSuccess(() -> component, false);
    }

    /**
     * Formats text with color codes.
     * 
     * @param text The text to format
     * @return The formatted text with color codes applied
     */
    public static String formatText(String text) {
        if (text == null) {
            return "";
        }
        return ColorUtils.processColorCodes(text);
    }

    /**
     * Sends a success message to a player.
     * 
     * @param player The player to send the message to
     * @param message The message to send
     */
    public static void sendSuccessMessage(ServerPlayer player, String message) {
        player.sendSystemMessage(Component.literal("§a" + message));
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
