package com.zerog.neoessentials.auctionhouse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

/**
 * JSON-backed configuration for the Auction House feature.
 * The file is stored at  config/auctionhouse.json  (created with defaults on first run).
 */
public class AuctionConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionConfig.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final File CONFIG_FILE = new File(Paths.get("", "config").toFile(), "auctionhouse.json");

    private static AuctionConfig instance;

    // ── Fields written to / read from JSON ────────────────────────────────────
    private int maxItemsPerPlayer          = 10;
    private long auctionSecondsDuration    = 604_800L; // 7 days
    private int maxPages                   = 50;

    // ── Singleton access ───────────────────────────────────────────────────────

    /** Returns the current config (loads from disk on first call). */
    public static AuctionConfig get() {
        if (instance == null) load();
        return instance;
    }

    /** Reload config from disk (creates default file if absent). */
    public static synchronized void load() {
        if (CONFIG_FILE.exists()) {
            try (Reader r = new InputStreamReader(new FileInputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
                AuctionConfig loaded = GSON.fromJson(r, AuctionConfig.class);
                instance = (loaded != null) ? loaded : new AuctionConfig();
            } catch (IOException e) {
                LOGGER.error("[AuctionHouse] Failed to read config, using defaults", e);
                instance = new AuctionConfig();
            }
        } else {
            instance = new AuctionConfig();
            save(); // write defaults
        }
    }

    /** Persist the current config instance to disk. */
    public static void save() {
        CONFIG_FILE.getParentFile().mkdirs();
        try (Writer w = new OutputStreamWriter(new FileOutputStream(CONFIG_FILE), StandardCharsets.UTF_8)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            LOGGER.error("[AuctionHouse] Failed to save config", e);
        }
    }

    // Invalidate cached instance so next get() reloads
    public static void invalidate() { instance = null; }

    // ── Getters ───────────────────────────────────────────────────────────────
    public int  getMaxItemsPerPlayer()       { return maxItemsPerPlayer; }
    public long getAuctionSecondsDuration()  { return auctionSecondsDuration; }
    public int  getMaxPages()                { return maxPages; }
}

