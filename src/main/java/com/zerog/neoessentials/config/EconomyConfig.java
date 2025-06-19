package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Economy system configuration file for NeoEssentials.
 * This config contains all economy-related settings.
 */
public class EconomyConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // General economy settings section
    static {
        BUILDER.comment("Economy Settings").push("economy");
    }
    
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME_SINGULAR = BUILDER
        .comment("The name of the currency (singular form)")
        .define("currencyNameSingular", "coin");
    
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_NAME_PLURAL = BUILDER
        .comment("The name of the currency (plural form)")
        .define("currencyNamePlural", "coins");
    
    public static final ModConfigSpec.ConfigValue<String> CURRENCY_SYMBOL = BUILDER
        .comment("The symbol used when displaying currency amounts")
        .define("currencySymbol", "$");
    
    public static final ModConfigSpec.DoubleValue STARTING_BALANCE = BUILDER
        .comment("Starting balance for new players")
        .defineInRange("startingBalance", 100.0, 0.0, Double.MAX_VALUE);
    
    public static final ModConfigSpec.BooleanValue ENABLE_INTEREST = BUILDER
        .comment("Enable interest payments on player balances")
        .define("enableInterest", false);
    
    public static final ModConfigSpec.DoubleValue INTEREST_RATE = BUILDER
        .comment("Interest rate percentage (only used if interest is enabled)")
        .defineInRange("interestRate", 0.5, 0.0, 100.0);
    
    public static final ModConfigSpec.IntValue INTEREST_INTERVAL = BUILDER
        .comment("How often to apply interest (in minutes)")
        .defineInRange("interestInterval", 60, 1, Integer.MAX_VALUE);
        
    public static final ModConfigSpec.IntValue MAX_BALTOP_ENTRIES = BUILDER
        .comment("Maximum number of entries to show in the baltop command")
        .defineInRange("maxBaltopEntries", 10, 1, 100);
    
    static {
        BUILDER.pop(); // End economy section
        
        // Transaction settings section
        BUILDER.comment("Transaction Settings").push("transactions");
    }
    
    public static final ModConfigSpec.BooleanValue LOG_TRANSACTIONS = BUILDER
        .comment("Whether to log all economy transactions")
        .define("logTransactions", true);
    
    public static final ModConfigSpec.BooleanValue NOTIFY_ON_PAY = BUILDER
        .comment("Whether to notify players when they receive money")
        .define("notifyOnPay", true);
        
    public static final ModConfigSpec.BooleanValue ALLOW_NEGATIVE_BALANCE = BUILDER
        .comment("Whether to allow players to have a negative balance")
        .define("allowNegativeBalance", false);
    
    static {
        BUILDER.pop(); // End transactions section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
