package com.zerog.neoessentials.config.dedicated;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration for Economy System customization
 * Provides comprehensive settings for the financial system
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class EconomyConfig {
    
    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    public static final ModConfigSpec SPEC;
    
    // Main Economy Settings
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue STARTING_BALANCE;
    public static final ModConfigSpec.DoubleValue MAX_BALANCE;
    public static final ModConfigSpec.DoubleValue MIN_BALANCE;
    public static final ModConfigSpec.BooleanValue ALLOW_NEGATIVE_BALANCE;
    
    // Currency Display
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_SYMBOL;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME_SINGULAR;
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME_PLURAL;
    public static final ModConfigSpec.BooleanValue SYMBOL_BEFORE_AMOUNT;
    public static final ModConfigSpec.IntValue DECIMAL_PLACES;
    
    // Transaction Settings
    public static final ModConfigSpec.DoubleValue MIN_TRANSACTION_AMOUNT;
    public static final ModConfigSpec.DoubleValue MAX_TRANSACTION_AMOUNT;
    public static final ModConfigSpec.BooleanValue ENABLE_TRANSACTION_FEES;
    public static final ModConfigSpec.DoubleValue TRANSACTION_FEE_PERCENTAGE;
    public static final ModConfigSpec.DoubleValue TRANSACTION_FEE_FLAT;
    public static final ModConfigSpec.DoubleValue TRANSACTION_FEE_MAX;
    
    // Banking System
    public static final ModConfigSpec.BooleanValue ENABLE_INTEREST;
    public static final ModConfigSpec.DoubleValue INTEREST_RATE;
    public static final ModConfigSpec.IntValue INTEREST_INTERVAL_HOURS;
    public static final ModConfigSpec.DoubleValue MIN_BALANCE_FOR_INTEREST;
    public static final ModConfigSpec.DoubleValue MAX_INTEREST_PER_PERIOD;
    
    // Shop Integration
    public static final ModConfigSpec.BooleanValue ENABLE_SHOP_TAX;
    public static final ModConfigSpec.DoubleValue SHOP_TAX_PERCENTAGE;
    public static final ModConfigSpec.DoubleValue SHOP_TAX_FLAT;
    public static final ModConfigSpec.BooleanValue SHOP_TAX_TO_SERVER;
    public static final ModConfigSpec.ConfigValue<String> SERVER_ECONOMY_ACCOUNT;
    
    // Pay Command Settings
    public static final ModConfigSpec.DoubleValue MIN_PAY_AMOUNT;
    public static final ModConfigSpec.DoubleValue MAX_PAY_AMOUNT;
    public static final ModConfigSpec.IntValue PAY_COOLDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue PAY_REQUIRES_CONFIRMATION;
    public static final ModConfigSpec.DoubleValue PAY_CONFIRMATION_THRESHOLD;
    
    // Baltop Settings
    public static final ModConfigSpec.IntValue BALTOP_DEFAULT_SIZE;
    public static final ModConfigSpec.IntValue BALTOP_MAX_SIZE;
    public static final ModConfigSpec.BooleanValue BALTOP_UPDATE_REALTIME;
    public static final ModConfigSpec.IntValue BALTOP_CACHE_MINUTES;
    public static final ModConfigSpec.BooleanValue BALTOP_HIDE_VANISHED;
    
    // Logging and Analytics
    public static final ModConfigSpec.BooleanValue ENABLE_TRANSACTION_LOGGING;
    public static final ModConfigSpec.IntValue TRANSACTION_HISTORY_DAYS;
    public static final ModConfigSpec.BooleanValue LOG_ADMIN_TRANSACTIONS;
    public static final ModConfigSpec.BooleanValue LOG_AUTOMATED_TRANSACTIONS;
    public static final ModConfigSpec.BooleanValue ENABLE_ANALYTICS;
    
    // Admin Economy Tools
    public static final ModConfigSpec.DoubleValue ADMIN_MAX_GIVE_AMOUNT;
    public static final ModConfigSpec.DoubleValue ADMIN_MAX_TAKE_AMOUNT;
    public static final ModConfigSpec.BooleanValue ADMIN_BYPASS_LIMITS;
    public static final ModConfigSpec.BooleanValue ADMIN_BYPASS_FEES;
    public static final ModConfigSpec.BooleanValue LOG_ADMIN_ECO_COMMANDS;
    
    // Integration Settings
    public static final ModConfigSpec.BooleanValue INTEGRATE_WITH_WARPS;
    public static final ModConfigSpec.BooleanValue INTEGRATE_WITH_KITS;
    public static final ModConfigSpec.BooleanValue INTEGRATE_WITH_COMMANDS;
    public static final ModConfigSpec.BooleanValue INTEGRATE_WITH_SHOPS;
    
    static {
        BUILDER.comment("Economy Configuration")
               .comment("Comprehensive settings for the NeoEssentials economy system");
        
        BUILDER.push("main");
        ENABLED = BUILDER
            .comment("Enable the economy system")
            .define("enabled", true);
        STARTING_BALANCE = BUILDER
            .comment("Starting balance for new players")
            .defineInRange("starting_balance", 1000.0, 0.0, 1000000.0);
        MAX_BALANCE = BUILDER
            .comment("Maximum balance a player can have (0 = unlimited)")
            .defineInRange("max_balance", 0.0, 0.0, Double.MAX_VALUE);
        MIN_BALANCE = BUILDER
            .comment("Minimum balance a player can have")
            .defineInRange("min_balance", 0.0, -1000000.0, 1000000.0);
        ALLOW_NEGATIVE_BALANCE = BUILDER
            .comment("Allow players to have negative balances")
            .define("allow_negative_balance", false);
        BUILDER.pop();
        
        BUILDER.push("currency");
        CURRENCY_SYMBOL = BUILDER
            .comment("Symbol used to represent currency")
            .define("currency_symbol", "$");
        CURRENCY_NAME_SINGULAR = BUILDER
            .comment("Name of currency (singular form)")
            .define("currency_name_singular", "coin");
        CURRENCY_NAME_PLURAL = BUILDER
            .comment("Name of currency (plural form)")
            .define("currency_name_plural", "coins");
        SYMBOL_BEFORE_AMOUNT = BUILDER
            .comment("Show currency symbol before amount ($100 vs 100$)")
            .define("symbol_before_amount", true);
        DECIMAL_PLACES = BUILDER
            .comment("Number of decimal places to show for currency")
            .defineInRange("decimal_places", 2, 0, 10);
        BUILDER.pop();
        
        BUILDER.push("transactions");
        MIN_TRANSACTION_AMOUNT = BUILDER
            .comment("Minimum amount for transactions")
            .defineInRange("min_transaction_amount", 0.01, 0.0, 1000.0);
        MAX_TRANSACTION_AMOUNT = BUILDER
            .comment("Maximum amount for transactions (0 = unlimited)")
            .defineInRange("max_transaction_amount", 0.0, 0.0, Double.MAX_VALUE);
        ENABLE_TRANSACTION_FEES = BUILDER
            .comment("Enable fees for transactions")
            .define("enable_transaction_fees", false);
        TRANSACTION_FEE_PERCENTAGE = BUILDER
            .comment("Transaction fee as percentage (5.0 = 5%)")
            .defineInRange("transaction_fee_percentage", 0.0, 0.0, 50.0);
        TRANSACTION_FEE_FLAT = BUILDER
            .comment("Flat transaction fee amount")
            .defineInRange("transaction_fee_flat", 0.0, 0.0, 100.0);
        TRANSACTION_FEE_MAX = BUILDER
            .comment("Maximum transaction fee (0 = unlimited)")
            .defineInRange("transaction_fee_max", 0.0, 0.0, 1000.0);
        BUILDER.pop();
        
        BUILDER.push("banking");
        ENABLE_INTEREST = BUILDER
            .comment("Enable interest system for player balances")
            .define("enable_interest", false);
        INTEREST_RATE = BUILDER
            .comment("Interest rate percentage per period (1.0 = 1%)")
            .defineInRange("interest_rate", 0.1, 0.0, 10.0);
        INTEREST_INTERVAL_HOURS = BUILDER
            .comment("How often to apply interest (in hours)")
            .defineInRange("interest_interval_hours", 24, 1, 168);
        MIN_BALANCE_FOR_INTEREST = BUILDER
            .comment("Minimum balance required to earn interest")
            .defineInRange("min_balance_for_interest", 1000.0, 0.0, 100000.0);
        MAX_INTEREST_PER_PERIOD = BUILDER
            .comment("Maximum interest earned per period (0 = unlimited)")
            .defineInRange("max_interest_per_period", 0.0, 0.0, 10000.0);
        BUILDER.pop();
        
        BUILDER.push("shop");
        ENABLE_SHOP_TAX = BUILDER
            .comment("Enable tax on shop purchases")
            .define("enable_shop_tax", false);
        SHOP_TAX_PERCENTAGE = BUILDER
            .comment("Shop tax percentage (5.0 = 5%)")
            .defineInRange("shop_tax_percentage", 0.0, 0.0, 25.0);
        SHOP_TAX_FLAT = BUILDER
            .comment("Flat shop tax amount")
            .defineInRange("shop_tax_flat", 0.0, 0.0, 100.0);
        SHOP_TAX_TO_SERVER = BUILDER
            .comment("Send shop tax to server economy account")
            .define("shop_tax_to_server", true);
        SERVER_ECONOMY_ACCOUNT = BUILDER
            .comment("Server economy account UUID (auto-generated if empty)")
            .define("server_economy_account", "");
        BUILDER.pop();
        
        BUILDER.push("pay");
        MIN_PAY_AMOUNT = BUILDER
            .comment("Minimum amount for /pay command")
            .defineInRange("min_pay_amount", 0.01, 0.0, 1000.0);
        MAX_PAY_AMOUNT = BUILDER
            .comment("Maximum amount for /pay command (0 = unlimited)")
            .defineInRange("max_pay_amount", 0.0, 0.0, Double.MAX_VALUE);
        PAY_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown for /pay command in seconds")
            .defineInRange("pay_cooldown_seconds", 0, 0, 3600);
        PAY_REQUIRES_CONFIRMATION = BUILDER
            .comment("Require confirmation for large payments")
            .define("pay_requires_confirmation", true);
        PAY_CONFIRMATION_THRESHOLD = BUILDER
            .comment("Amount threshold for payment confirmation")
            .defineInRange("pay_confirmation_threshold", 1000.0, 0.0, 100000.0);
        BUILDER.pop();
        
        BUILDER.push("baltop");
        BALTOP_DEFAULT_SIZE = BUILDER
            .comment("Default number of players shown in /baltop")
            .defineInRange("baltop_default_size", 10, 1, 50);
        BALTOP_MAX_SIZE = BUILDER
            .comment("Maximum number of players that can be shown")
            .defineInRange("baltop_max_size", 25, 1, 100);
        BALTOP_UPDATE_REALTIME = BUILDER
            .comment("Update baltop in real-time (may impact performance)")
            .define("baltop_update_realtime", false);
        BALTOP_CACHE_MINUTES = BUILDER
            .comment("How long to cache baltop results (minutes)")
            .defineInRange("baltop_cache_minutes", 5, 1, 60);
        BALTOP_HIDE_VANISHED = BUILDER
            .comment("Hide vanished players from baltop")
            .define("baltop_hide_vanished", true);
        BUILDER.pop();
        
        BUILDER.push("logging");
        ENABLE_TRANSACTION_LOGGING = BUILDER
            .comment("Enable transaction logging and history")
            .define("enable_transaction_logging", true);
        TRANSACTION_HISTORY_DAYS = BUILDER
            .comment("How many days to keep transaction history")
            .defineInRange("transaction_history_days", 30, 1, 365);
        LOG_ADMIN_TRANSACTIONS = BUILDER
            .comment("Log admin economy commands")
            .define("log_admin_transactions", true);
        LOG_AUTOMATED_TRANSACTIONS = BUILDER
            .comment("Log automated transactions (interest, fees, etc.)")
            .define("log_automated_transactions", true);
        ENABLE_ANALYTICS = BUILDER
            .comment("Enable economy analytics and reporting")
            .define("enable_analytics", true);
        BUILDER.pop();
        
        BUILDER.push("admin");
        ADMIN_MAX_GIVE_AMOUNT = BUILDER
            .comment("Maximum amount admins can give with /eco give")
            .defineInRange("admin_max_give_amount", 1000000.0, 0.0, Double.MAX_VALUE);
        ADMIN_MAX_TAKE_AMOUNT = BUILDER
            .comment("Maximum amount admins can take with /eco take")
            .defineInRange("admin_max_take_amount", 1000000.0, 0.0, Double.MAX_VALUE);
        ADMIN_BYPASS_LIMITS = BUILDER
            .comment("Allow admins to bypass economy limits")
            .define("admin_bypass_limits", true);
        ADMIN_BYPASS_FEES = BUILDER
            .comment("Allow admins to bypass transaction fees")
            .define("admin_bypass_fees", true);
        LOG_ADMIN_ECO_COMMANDS = BUILDER
            .comment("Log all admin economy commands")
            .define("log_admin_eco_commands", true);
        BUILDER.pop();
        
        BUILDER.push("integration");
        INTEGRATE_WITH_WARPS = BUILDER
            .comment("Enable economy integration with warp system")
            .define("integrate_with_warps", true);
        INTEGRATE_WITH_KITS = BUILDER
            .comment("Enable economy integration with kit system")
            .define("integrate_with_kits", true);
        INTEGRATE_WITH_COMMANDS = BUILDER
            .comment("Enable economy integration with command costs")
            .define("integrate_with_commands", true);
        INTEGRATE_WITH_SHOPS = BUILDER
            .comment("Enable economy integration with shop system")
            .define("integrate_with_shops", true);
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}
