package com.zerog.neoessentials.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Database configuration for NeoEssentials.
 * This config contains all database connection settings.
 */
public class DatabaseTomlConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    
    // Database settings section
    static {
        BUILDER.comment("Database Settings").push("database");
    }
    
    public static final ModConfigSpec.ConfigValue<String> DB_TYPE = BUILDER
        .comment("Database type (sqlite, mysql, postgres)")
        .define("databaseType", "sqlite");
    
    public static final ModConfigSpec.ConfigValue<String> DB_HOST = BUILDER
        .comment("Database host (for MySQL/PostgreSQL)")
        .define("databaseHost", "localhost");
    
    public static final ModConfigSpec.IntValue DB_PORT = BUILDER
        .comment("Database port (for MySQL/PostgreSQL)")
        .defineInRange("databasePort", 3306, 1, 65535);
    
    public static final ModConfigSpec.ConfigValue<String> DB_NAME = BUILDER
        .comment("Database name")
        .define("databaseName", "neoessentials");
    
    public static final ModConfigSpec.ConfigValue<String> DB_USER = BUILDER
        .comment("Database username (for MySQL/PostgreSQL)")
        .define("databaseUser", "root");
    
    public static final ModConfigSpec.ConfigValue<String> DB_PASS = BUILDER
        .comment("Database password (for MySQL/PostgreSQL)")
        .define("databasePass", "");
    
    public static final ModConfigSpec.ConfigValue<String> DB_PREFIX = BUILDER
        .comment("Table prefix for all database tables")
        .define("tablePrefix", "ne_");
    
    public static final ModConfigSpec.BooleanValue USE_CONNECTION_POOL = BUILDER
        .comment("Whether to use a connection pool (recommended for MySQL/PostgreSQL)")
        .define("useConnectionPool", true);
    
    public static final ModConfigSpec.IntValue CONNECTION_POOL_SIZE = BUILDER
        .comment("Connection pool size")
        .defineInRange("connectionPoolSize", 10, 1, 100);
    
    public static final ModConfigSpec.ConfigValue<String> SQLITE_FILE = BUILDER
        .comment("SQLite database file path (relative to server directory)")
        .define("sqliteFile", "neoessentials/database.db");
    
    static {
        BUILDER.pop(); // End database section
    }
    
    public static final ModConfigSpec SPEC = BUILDER.build();
}
