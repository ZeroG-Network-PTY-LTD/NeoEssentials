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
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_coming_soon", "&7Loan management features are coming soon in a future update!");
        FALLBACK_MESSAGES.put("neoessentials.economy.exchange_coming_soon", "&7Currency exchange coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.stats_coming_soon", "&7Financial statistics coming soon!");
        FALLBACK_MESSAGES.put("neoessentials.economy.economy_settings_coming_soon", "&7Economy settings are coming soon in a future update!");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_header", "&6====== Send Money ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_instructions", "&7Use the pay command to send money:");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_usage", "&e/pay <player> <amount>");
        FALLBACK_MESSAGES.put("neoessentials.economy.transfer_error", "&cTransfer error: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.gui_unavailable", "&cEconomy GUI is currently unavailable");
        FALLBACK_MESSAGES.put("neoessentials.economy.using_chat_interface", "&7Using chat-based interface instead");
        
        // Additional economy GUI messages
        FALLBACK_MESSAGES.put("neoessentials.economy.available_actions_header", "&6====== Available Actions ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.action_send_money", "&e• Send Money: &7/pay <player> <amount>");
        FALLBACK_MESSAGES.put("neoessentials.economy.action_transaction_history", "&e• Transaction History: &7/economygui transactions");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_header", "&6====== Send Money ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_instructions", "&7Use /pay <player> <amount> to send money to another player");
        FALLBACK_MESSAGES.put("neoessentials.economy.send_money_usage", "&7Example: /pay Steve 50");
        FALLBACK_MESSAGES.put("neoessentials.economy.invalid_amount", "&cInvalid amount: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.insufficient_funds", "&cInsufficient funds! You need %s but only have %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.transfer_error", "&cTransfer error: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_feature_placeholder", "&6====== Bank Account ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_coming_soon", "&7Banking features are coming soon in a future update!");
        FALLBACK_MESSAGES.put("neoessentials.commands.error.server_not_available", "&cServer not available! Please try again later.");

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
        
        // Transaction history messages
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_history_empty", "&7You have no transaction history yet.");
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_history_header", "&6====== Transaction History ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_history_footer", "&6================================");
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_entry", "&e%s. &f%s &7- %s %s &8(Balance: %s)");
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_history_info", "&7Showing last %s transactions. Use /economygui for more options.");
        
        // Financial stats messages
        FALLBACK_MESSAGES.put("neoessentials.economy.financial_stats_header", "&6====== Financial Statistics ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.financial_stats_basic", "&7Your financial overview:");
        FALLBACK_MESSAGES.put("neoessentials.economy.financial_stats_footer", "&6==================================");
        FALLBACK_MESSAGES.put("neoessentials.economy.current_balance", "&eCurrent Balance: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.total_transactions", "&eTotal Transactions: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.total_spent", "&eTotal Spent: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.total_received", "&eTotal Received: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.no_transaction_history", "&7You have no transaction history yet.");
        FALLBACK_MESSAGES.put("neoessentials.economy.stats_error", "&cError retrieving financial stats: %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.transaction_history_error", "&cError retrieving transaction history: %s");
        
        // Additional action messages
        FALLBACK_MESSAGES.put("neoessentials.economy.action_financial_stats", "&e• Financial Stats: &7/financialstats");
        FALLBACK_MESSAGES.put("neoessentials.economy.action_bank_account", "&e• Bank Account: &7/bankaccount (coming soon)");
        FALLBACK_MESSAGES.put("neoessentials.economy.action_loans", "&e• Loan Management: &7/loans (coming soon)");
        FALLBACK_MESSAGES.put("neoessentials.economy.action_settings", "&e• Economy Settings: &7/economysettings");
        FALLBACK_MESSAGES.put("neoessentials.economy.quick_commands_info", "&7Quick access: &e/pay &7| &e/econ &7| &e/transactions &7| &e/financialstats");
        
        // Banking messages
        FALLBACK_MESSAGES.put("neoessentials.economy.current_cash_balance", "&eCash Balance: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_account_info", "&7Manage your savings and banking features:");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_features_preview", "&7Available banking features:");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_feature_savings", "&e• Savings Account: &7Secure storage with interest");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_feature_interest", "&e• Interest Rates: &7Earn money over time");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_feature_loans", "&e• Loans: &7Borrow money with flexible terms");
        FALLBACK_MESSAGES.put("neoessentials.economy.bank_error", "&cError accessing bank account: %s");
        
        // Currency exchange messages
        FALLBACK_MESSAGES.put("neoessentials.economy.currency_exchange_header", "&6====== Currency Exchange ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.currency_exchange_info", "&7Exchange between different currencies:");
        FALLBACK_MESSAGES.put("neoessentials.economy.available_currencies", "&7Available currencies:");
        FALLBACK_MESSAGES.put("neoessentials.economy.currency_primary", "&e• %s");
        FALLBACK_MESSAGES.put("neoessentials.economy.exchange_error", "&cError accessing currency exchange: %s");
        
        // Economy settings messages
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_header", "&6====== Economy Settings ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_currency_info", "&7Currency Configuration:");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_currency_symbol", "&eCurrency Symbol: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_currency_name", "&eCurrency Name: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_stats_header", "&7Economy Statistics:");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_total_currency", "&eTotal Currency in Circulation: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_active_players", "&eActive Players: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_system_status", "&7System Status:");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_status_operational", "&a✓ Economy System Operational");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_features_header", "&7Available Features:");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_feature_transfers", "&a✓ Money Transfers");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_feature_history", "&a✓ Transaction History");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_feature_stats", "&a✓ Financial Statistics");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_feature_banking_planned", "&e⚠ Banking System (Planned)");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_footer", "&6==============================");
        FALLBACK_MESSAGES.put("neoessentials.economy.settings_error", "&cError accessing economy settings: %s");
        
        // Loan management messages
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_header", "&6====== Loan Management ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_preview_header", "&7Planned Loan Features:");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_feature_basic", "&e• Basic Loans: &7Borrow money with competitive rates");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_feature_interest", "&e• Interest System: &7Fair interest rates based on risk");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_feature_collateral", "&e• Collateral Options: &7Secure loans with items or property");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_feature_repayment", "&e• Flexible Repayment: &7Multiple payment schedules available");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_eligibility_header", "&7Current Loan Eligibility:");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_eligibility_qualified", "&a✓ You qualify for loans");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_eligibility_insufficient", "&c✗ Insufficient balance for loan qualification");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_max_amount", "&eMaximum Loan Amount: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_minimum_balance", "&eMinimum Required Balance: &f%s");
        FALLBACK_MESSAGES.put("neoessentials.economy.loans_error", "&cError accessing loan management: %s");
        
        // Economy help messages
        FALLBACK_MESSAGES.put("neoessentials.economy.help_header", "&6====== Economy Help Guide ======");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_basic_commands", "&7Basic Economy Commands:");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_pay", "&e/pay <player> <amount> &7- Send money to another player");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_econ", "&e/econ &7- Open main economy interface");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_sendmoney", "&e/sendmoney &7- Open money transfer interface");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_advanced_commands", "&7Advanced Features:");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_transactions", "&e/transactions &7- View your transaction history");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_financialstats", "&e/financialstats &7- View financial statistics");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_bankaccount", "&e/bankaccount &7- Access banking features (coming soon)");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_loans", "&e/loans &7- Loan management system (coming soon)");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_currencyexchange", "&e/currencyexchange &7- Currency exchange (coming soon)");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_command_economysettings", "&e/economysettings &7- View economy configuration");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_examples_header", "&7Usage Examples:");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_example_pay", "&e/pay Steve 50 &7- Send $50.00 to Steve");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_example_check_balance", "&e/econ &7- Check your current balance");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_example_view_stats", "&e/financialstats &7- View spending and earning statistics");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_tips_header", "&7Tips & Information:");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_tip_safety", "&7• Double-check player names before sending money");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_tip_commands", "&7• Use &e/economyhelp &7to view this guide anytime");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_tip_support", "&7• Contact server staff if you need assistance");
        FALLBACK_MESSAGES.put("neoessentials.economy.help_footer", "&6===============================");
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
