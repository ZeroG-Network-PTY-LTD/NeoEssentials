package com.zerog.neoessentials.logging;

/**
 * Subsystems that can independently have their normal/debug logging toggled via
 * {@code logging.categories.<configKey>} in config.json. See {@link NeoLog}.
 */
public enum LogCategory {
    CHAT("chat"),
    ECONOMY("economy"),
    PERMISSIONS("permissions"),
    TELEPORTATION("teleportation"),
    MODERATION("moderation"),
    AUCTION_HOUSE("auctionHouse"),
    KITS("kits"),
    WEB_DASHBOARD("webDashboard"),
    DISCORD("discord"),
    CONFIG("config"),
    COMMANDS("commands"),
    GENERAL("general");

    private final String configKey;

    LogCategory(String configKey) {
        this.configKey = configKey;
    }

    /** The key under {@code logging.categories} in config.json for this category. */
    public String configKey() {
        return configKey;
    }
}
