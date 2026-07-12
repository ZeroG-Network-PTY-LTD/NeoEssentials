package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player bans and IP bans with persistent storage
 */
public class BanManager {
    // Scheduler for periodic expired-ban cleanup
    // Use daemon threads to prevent blocking JVM shutdown
    private final java.util.concurrent.ScheduledExecutorService banCleanupScheduler =
        java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "BanManager-Cleanup");
            t.setDaemon(true); // CRITICAL: Set as daemon to allow JVM shutdown
            return t;
        });
    private java.util.concurrent.ScheduledFuture<?> cleanupTaskFuture;

    // Static shutdown flag to persist across instances
    private static volatile boolean isShuttingDown = false;

    private static final Logger LOGGER = LoggerFactory.getLogger(BanManager.class);
    private static BanManager instance;
    private final Gson gson = new GsonBuilder().disableHtmlEscaping().create();
    private static final String PLAYER_COLLECTION = "player_bans";
    private static final String IP_COLLECTION = "ip_bans";
    private final com.zerog.neoessentials.storage.DataStore store;

    // In-memory cache for quick lookups (active bans only)
    private final Map<UUID, BanEntry> playerBans = new ConcurrentHashMap<>();
    private final Map<String, IPBanEntry> ipBans = new ConcurrentHashMap<>();
    // Archive of reversed/expired bans — every ban ever issued lives here once it's no
    // longer active, so getBanHistory()/getIPBanHistory() can show a player's/IP's full
    // punishment record (matching the "view a player's entire record, including unbans
    // and by whom" feature of ban-management plugins like BanManager), rather than the
    // previous behavior of a hard delete that erased all trace of a past ban on unban.
    // Both the active map and this archive persist to the SAME storage collection
    // (see PLAYER_COLLECTION/IP_COLLECTION), distinguished by BanEntry.active — one
    // record's worth of writes per mutation instead of rewriting a whole file.
    private final List<BanEntry> banHistory = new ArrayList<>();
    private final List<IPBanEntry> ipBanHistory = new ArrayList<>();

    public static class BanEntry {
        public String id;
        public String playerName;
        public UUID playerId;
        public String reason;
        public String bannedBy;
        public long banTime;
        public long expireTime; // 0 for permanent ban
        public String evidence; // optional — e.g. screenshot/log URL
        public boolean active = true;
        public String unbannedBy;
        public long unbannedAt; // 0 = not (yet) unbanned

        public BanEntry(String playerName, UUID playerId, String reason, String bannedBy) {
            this.id = UUID.randomUUID().toString();
            this.playerName = playerName;
            this.playerId = playerId;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.banTime = System.currentTimeMillis();
            this.expireTime = 0; // Default to permanent
        }

        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }

        public String getFormattedBanTime() {
            return formatTime(banTime);
        }

        public String getFormattedExpireTime() {
            return expireTime > 0 ? formatTime(expireTime) : "Never";
        }
    }

    public static class IPBanEntry {
        public String id;
        public String ipAddress;
        public String reason;
        public String bannedBy;
        public long banTime;
        public long expireTime; // 0 = permanent
        public String evidence;
        public boolean active = true;
        public String unbannedBy;
        public long unbannedAt;

        public IPBanEntry(String ipAddress, String reason, String bannedBy) {
            this.id = UUID.randomUUID().toString();
            this.ipAddress = ipAddress;
            this.reason = reason;
            this.bannedBy = bannedBy;
            this.banTime = System.currentTimeMillis();
            this.expireTime = 0;
        }

        public boolean isExpired() {
            return expireTime > 0 && System.currentTimeMillis() > expireTime;
        }

        public String getFormattedBanTime() {
            return formatTime(banTime);
        }
    }
    
    private BanManager() {
        this.store = com.zerog.neoessentials.storage.StorageManager.getInstance().getStore();
        migrateLegacyFilesIfNeeded();
        loadBans();

        // Start periodic expired-ban cleanup if enabled (but not if shutting down)
        if (!isShuttingDown) {
            int interval = com.zerog.neoessentials.config.ConfigManager.getInstance().getCheckExpiredBansInterval();
            if (interval > 0) {
                cleanupTaskFuture = banCleanupScheduler.scheduleAtFixedRate(
                    this::cleanupExpiredTempBans,
                    interval, interval, java.util.concurrent.TimeUnit.SECONDS
                );
                LOGGER.info("Scheduled expired temp ban cleanup every {} seconds.", interval);
            } else {
                LOGGER.info("Expired temp ban cleanup scheduler is disabled (interval <= 0).");
            }
        } else {
            LOGGER.debug("BanManager created during shutdown - scheduler not started");
        }
    }
    
    public static BanManager getInstance() {
        if (instance == null) {
            instance = new BanManager();
        }
        return instance;
    }

    /** Archives a naturally-expired ban into history so it still shows up in getBanHistory(). */
    private void archiveExpiredBan(BanEntry ban) {
        ban.active = false;
        ban.unbannedBy = null; // expired naturally, not reversed by staff
        ban.unbannedAt = ban.expireTime;
        banHistory.add(ban);
        store.put(PLAYER_COLLECTION, ban.id, banToJson(ban));
    }

    private void archiveExpiredIPBan(IPBanEntry ban) {
        ban.active = false;
        ban.unbannedBy = null;
        ban.unbannedAt = ban.expireTime;
        ipBanHistory.add(ban);
        store.put(IP_COLLECTION, ban.id, ipBanToJson(ban));
    }

    /**
     * Periodically called to remove expired temp bans if autoExpireTempBans is enabled.
     * This uses the same logic as getAllPlayerBans() but is safe for background use.
     */
    private void cleanupExpiredTempBans() {
        boolean autoExpire = com.zerog.neoessentials.config.ConfigManager.getInstance().isAutoExpireTempBansEnabled();

        // Clean up expired player bans
        boolean removedPlayerBans = false;
        Iterator<Map.Entry<UUID, BanEntry>> playerIterator = playerBans.entrySet().iterator();
        while (playerIterator.hasNext()) {
            Map.Entry<UUID, BanEntry> entry = playerIterator.next();
            if (entry.getValue().isExpired() && autoExpire) {
                archiveExpiredBan(entry.getValue());
                playerIterator.remove();
                removedPlayerBans = true;
            }
        }
        if (removedPlayerBans) {
            LOGGER.info("Expired temp player bans cleaned up by scheduler.");
        }

        // Clean up expired IP bans
        boolean removedIPBans = false;
        Iterator<Map.Entry<String, IPBanEntry>> ipIterator = ipBans.entrySet().iterator();
        while (ipIterator.hasNext()) {
            Map.Entry<String, IPBanEntry> entry = ipIterator.next();
            if (entry.getValue().isExpired() && autoExpire) {
                archiveExpiredIPBan(entry.getValue());
                ipIterator.remove();
                removedIPBans = true;
            }
        }
        if (removedIPBans) {
            LOGGER.info("Expired temp IP bans cleaned up by scheduler.");
        }
    }

    /**
     * Call this on server/plugin shutdown to stop the scheduler cleanly.
     */
    public void shutdownScheduler() {
        isShuttingDown = true;
        if (cleanupTaskFuture != null) cleanupTaskFuture.cancel(false);
        banCleanupScheduler.shutdown();
        try {
            if (!banCleanupScheduler.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                banCleanupScheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            banCleanupScheduler.shutdownNow();
        }
    }
    
    /**
     * Ban a player permanently
     */
    public boolean banPlayer(String playerName, UUID playerId, String reason, String bannedBy) {
        if (isPlayerBanned(playerId)) {
            return false; // Already banned
        }
        // Enforce config: check if permanent bans are enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isPermanentBansEnabled()) {
            LOGGER.warn("Permanent bans are disabled in config. Cannot ban player {} permanently.", playerName);
            return false;
        }
        // Enforce config: check maxBanReason
        int maxReason = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxBanReasonLength();
        if (reason != null && reason.length() > maxReason) {
            LOGGER.warn("Ban reason too long ({} > {}). Cannot ban player {}.", reason.length(), maxReason, playerName);
            return false;
        }
        BanEntry ban = new BanEntry(playerName, playerId, reason, bannedBy);
        playerBans.put(playerId, ban);
        store.put(PLAYER_COLLECTION, ban.id, banToJson(ban));
        // Build the fully-localized/formatted ban message once, and store THAT (not the
        // raw reason) as the vanilla ban-list entry's reason. Vanilla's own login-time ban
        // check (used when this player reconnects later) wraps whatever we put there in its
        // own client-locale "You are banned...\nReason: %s" template — we can't override that
        // wrapper without a mixin, but the substituted banMessageFormat text still comes
        // through verbatim as the "%s", so the admin's configured/localized message is what
        // the player actually sees instead of just the raw ban reason.
        String format = com.zerog.neoessentials.config.ConfigManager.getBanMessageFormat();
        String formattedMessage = format.replace("{reason}", reason != null ? reason : "N/A")
                            .replace("{bannedBy}", bannedBy != null ? bannedBy : "Console")
                            .replace("{duration}", "Permanent");
        // Also add to vanilla ban list so it is visible to /banlist and vanilla kick logic
        addToVanillaBanList(playerId, playerName, formattedMessage, bannedBy, null);
        // Kick player if online
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.connection.disconnect(Component.literal(formattedMessage));
            }
            // Broadcast to staff if enabled
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isBroadcastBansEnabled()) {
                String staffPerm = com.zerog.neoessentials.config.ConfigManager.getInstance().getStaffNotificationPermission();
                String staffMsg = "[NeoEssentials] Player " + playerName + " was permanently banned by " + bannedBy + (reason != null && !reason.isEmpty() ? " for: " + reason : "");
                for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
                    if (staff.hasPermissions(2) || PermissionAPI.hasPermission(staff.getUUID(), staffPerm)) {
                        staff.sendSystemMessage(Component.literal(staffMsg));
                    }
                }
            }
        }
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogBanActionsEnabled()) {
            LOGGER.info("Player {} ({}) banned by {} for: {}", playerName, playerId, bannedBy, reason);
        }
        return true;
    }
    
    /**
     * Ban a player temporarily
     */
    public boolean tempBanPlayer(String playerName, UUID playerId, String reason, String bannedBy, long durationMillis) {
        if (isPlayerBanned(playerId)) {
            return false; // Already banned
        }
        // Enforce config: check if temp bans are enabled
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isTempBansEnabled()) {
            LOGGER.warn("Temporary bans are disabled in config. Cannot temp-ban player {}.", playerName);
            return false;
        }
        // Enforce config: check maxBanReason
        int maxReason = com.zerog.neoessentials.config.ConfigManager.getInstance().getMaxBanReasonLength();
        if (reason != null && reason.length() > maxReason) {
            LOGGER.warn("Temp ban reason too long ({} > {}). Cannot temp-ban player {}.", reason.length(), maxReason, playerName);
            return false;
        }

        BanEntry ban = new BanEntry(playerName, playerId, reason, bannedBy);
        ban.expireTime = System.currentTimeMillis() + durationMillis;
        playerBans.put(playerId, ban);
        store.put(PLAYER_COLLECTION, ban.id, banToJson(ban));
        // Build the fully-localized/formatted ban message once (see banPlayer() for why this,
        // rather than the raw reason, is what gets stored as the vanilla ban entry's reason).
        String format = com.zerog.neoessentials.config.ConfigManager.getTempBanMessageFormat();
        String duration = formatDuration(durationMillis);
        String expiry = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(java.time.ZoneId.systemDefault())
            .format(java.time.Instant.ofEpochMilli(System.currentTimeMillis() + durationMillis));
        String formattedMessage = format.replace("{reason}", reason != null ? reason : "N/A")
                            .replace("{bannedBy}", bannedBy != null ? bannedBy : "Console")
                            .replace("{duration}", duration)
                            .replace("{expiry}", expiry);
        // Sync to vanilla ban list with expiry
        java.util.Date expireDate = new java.util.Date(System.currentTimeMillis() + durationMillis);
        addToVanillaBanList(playerId, playerName, formattedMessage, bannedBy, expireDate);
        // Kick player if online
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.connection.disconnect(Component.literal(formattedMessage));
            }
            // Broadcast to staff if enabled
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isBroadcastBansEnabled()) {
                String staffPerm = com.zerog.neoessentials.config.ConfigManager.getInstance().getStaffNotificationPermission();
                String staffMsg = "[NeoEssentials] Player " + playerName + " was temporarily banned by " + bannedBy + " for " + formatDuration(durationMillis) + (reason != null && !reason.isEmpty() ? " - Reason: " + reason : "");
                for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
                    if (staff.hasPermissions(2) || PermissionAPI.hasPermission(staff.getUUID(), staffPerm)) {
                        staff.sendSystemMessage(Component.literal(staffMsg));
                    }
                }
            }
        }

    if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogBanActionsEnabled()) {
        LOGGER.info("Player {} ({}) temporarily banned by {} for {} - Reason: {}", 
            playerName, playerId, bannedBy, formatDuration(durationMillis), reason);
    }
        return true;
    }
    
    /**
     * Ban an IP address
     */
    public boolean banIP(String ipAddress, String reason, String bannedBy) {
        if (isIPBanned(ipAddress)) {
            return false; // Already banned
        }
        // Enforce config: check if IP bans are enabled
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isIPBansDisabled()) {
            LOGGER.warn("IP bans are disabled in config. Cannot ban IP {}.", ipAddress);
            return false;
        }

        IPBanEntry ban = new IPBanEntry(ipAddress, reason, bannedBy);
        ipBans.put(ipAddress, ban);
        store.put(IP_COLLECTION, ban.id, ipBanToJson(ban));

        // Kick all players with this IP
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            List<ServerPlayer> playersToKick = new ArrayList<>();
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (getPlayerIP(player).equals(ipAddress)) {
                    playersToKick.add(player);
                }
            }
            for (ServerPlayer player : playersToKick) {
                String format = com.zerog.neoessentials.config.ConfigManager.getIPBanMessageFormat();
                String message = format.replace("{reason}", reason != null ? reason : "N/A")
                                       .replace("{bannedBy}", bannedBy != null ? bannedBy : "Console");
                player.connection.disconnect(Component.literal(message));
            }
        }

        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogBanActionsEnabled()) {
            LOGGER.info("IP {} banned by {} for: {}", ipAddress, bannedBy, reason);
        }
        return true;
    }

    /**
     * Temporarily ban an IP address (Essentials: Commandtempbanip)
     */
    public boolean tempBanIP(String ipAddress, String reason, String bannedBy, long durationMillis) {
        if (isIPBanned(ipAddress)) return false;
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isIPBansDisabled()) {
            LOGGER.warn("IP bans are disabled. Cannot temp-ban IP {}.", ipAddress);
            return false;
        }
        IPBanEntry ban = new IPBanEntry(ipAddress, reason, bannedBy);
        ban.expireTime = System.currentTimeMillis() + durationMillis;
        ipBans.put(ipAddress, ban);
        store.put(IP_COLLECTION, ban.id, ipBanToJson(ban));

        // Kick all players with this IP
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : new ArrayList<>(server.getPlayerList().getPlayers())) {
                if (getPlayerIP(player).equals(ipAddress)) {
                    String msg = "You have been temporarily IP banned for " + formatDuration(durationMillis)
                        + (reason != null && !reason.isEmpty() ? ": " + reason : "");
                    player.connection.disconnect(Component.literal(msg));
                }
            }
        }
        if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogBanActionsEnabled()) {
            LOGGER.info("IP {} temporarily banned by {} for {} - Reason: {}", ipAddress, bannedBy, formatDuration(durationMillis), reason);
        }
        return true;
    }

    /**
     * Unban a player from NeoEssentials ban list AND vanilla ban list.
     * @deprecated use {@link #unbanPlayer(UUID, String)} so the reversal is attributed —
     *             this overload archives the ban with an unknown ({@code null}) unbanner.
     */
    @Deprecated
    public boolean unbanPlayer(UUID playerId) {
        return unbanPlayer(playerId, null);
    }

    /**
     * Unban a player from NeoEssentials ban list AND vanilla ban list. The reversed ban is
     * archived (not deleted) with who performed the unban and when, so it still shows up in
     * {@link #getBanHistory(UUID)} — matching ban-management plugins' "view a player's
     * entire record, including unbans and by whom" behavior instead of erasing all trace.
     */
    public boolean unbanPlayer(UUID playerId, String unbannedBy) {
        BanEntry removed = playerBans.remove(playerId);
        boolean removedFromVanilla = removeFromVanillaBanList(playerId);
        if (removed != null || removedFromVanilla) {
            if (removed != null) {
                removed.active = false;
                removed.unbannedBy = unbannedBy;
                removed.unbannedAt = System.currentTimeMillis();
                banHistory.add(removed);
                store.put(PLAYER_COLLECTION, removed.id, banToJson(removed));
            }
            // Broadcast to staff if enabled
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null && com.zerog.neoessentials.config.ConfigManager.getInstance().isBroadcastBansEnabled()) {
                String staffPerm = com.zerog.neoessentials.config.ConfigManager.getInstance().getStaffNotificationPermission();
                String name = removed != null ? removed.playerName : playerId.toString();
                String staffMsg = "[NeoEssentials] Player " + name + " was unbanned.";
                for (ServerPlayer staff : server.getPlayerList().getPlayers()) {
                    if (staff.hasPermissions(2) || PermissionAPI.hasPermission(staff.getUUID(), staffPerm)) {
                        staff.sendSystemMessage(Component.literal(staffMsg));
                    }
                }
            }
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogBanActionsEnabled()) {
                String name = removed != null ? removed.playerName : playerId.toString();
                LOGGER.info("Player {} ({}) unbanned", name, playerId);
            }
            return true;
        }
        return false;
    }

    // ── Vanilla ban list helpers ───────────────────────────────────────────────

    /**
     * Add a player to the vanilla UserBanList so vanilla kick/join blocking works.
     *
     * @param displayReason the text shown to the player when they try to reconnect —
     *                       pass the fully-formatted/localized ban message (banMessageFormat
     *                       or tempBanMessageFormat with placeholders substituted), not the
     *                       raw ban reason. Vanilla's own login-time ban check wraps whatever
     *                       is stored here in its own client-locale "Reason: %s" template, so
     *                       this is the only way the configured message format actually
     *                       reaches a player banned while offline / reconnecting later.
     */
    private void addToVanillaBanList(UUID playerId, String playerName, String displayReason,
                                     String bannedBy, java.util.Date expires) {
        try {
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return;
            com.mojang.authlib.GameProfile profile = resolveProfile(server, playerId, playerName);
            if (profile == null) return;
            net.minecraft.server.players.UserBanList vanillaBans = server.getPlayerList().getBans();
            if (!vanillaBans.isBanned(profile)) {
                String src = bannedBy != null ? bannedBy : "NeoEssentials";
                net.minecraft.server.players.UserBanListEntry entry = new net.minecraft.server.players.UserBanListEntry(
                    profile, null, src, expires, displayReason != null ? displayReason : "Banned");
                vanillaBans.add(entry);
                LOGGER.debug("Added {} to vanilla ban list", playerName);
            }
        } catch (Exception e) {
            LOGGER.warn("Could not sync ban to vanilla ban list for {}: {}", playerName, e.getMessage());
        }
    }

    /** Remove a player from the vanilla UserBanList. Returns true if they were found. */
    private boolean removeFromVanillaBanList(UUID playerId) {
        try {
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            net.minecraft.server.players.UserBanList vanillaBans = server.getPlayerList().getBans();
            // Try to resolve profile for removal
            BanEntry ourBan = playerBans.get(playerId);
            String name = ourBan != null ? ourBan.playerName : null;
            com.mojang.authlib.GameProfile profile = resolveProfile(server, playerId, name);
            if (profile != null && vanillaBans.isBanned(profile)) {
                vanillaBans.remove(profile);
                LOGGER.debug("Removed {} from vanilla ban list", playerId);
                return true;
            }
        } catch (Exception e) {
            LOGGER.warn("Could not remove {} from vanilla ban list: {}", playerId, e.getMessage());
        }
        return false;
    }

    /** Resolve a GameProfile from UUID + optional name using profile cache. */
    private com.mojang.authlib.GameProfile resolveProfile(MinecraftServer server, UUID uuid, String name) {
        // Try online player first
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) return online.getGameProfile();
        // Try profile cache
        try {
            var cache = server.getProfileCache();
            if (cache != null) return cache.get(uuid).orElse(null);
        } catch (Exception ignored) {}
        // Fallback: construct a minimal profile
        if (name != null && !name.isBlank()) {
            return new com.mojang.authlib.GameProfile(uuid, name);
        }
        return null;
    }
    
    /**
     * Unban an IP address.
     * @deprecated use {@link #unbanIP(String, String)} so the reversal is attributed.
     */
    @Deprecated
    public boolean unbanIP(String ipAddress) {
        return unbanIP(ipAddress, null);
    }

    /**
     * Unban an IP address. The reversed ban is archived (not deleted) with who performed
     * the unban and when — see {@link #unbanPlayer(UUID, String)} for why.
     */
    public boolean unbanIP(String ipAddress, String unbannedBy) {
        IPBanEntry removed = ipBans.remove(ipAddress);
        if (removed != null) {
            removed.active = false;
            removed.unbannedBy = unbannedBy;
            removed.unbannedAt = System.currentTimeMillis();
            ipBanHistory.add(removed);
            store.put(IP_COLLECTION, removed.id, ipBanToJson(removed));
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isLogBanActionsEnabled()) {
                LOGGER.info("IP {} unbanned by {}", ipAddress, unbannedBy != null ? unbannedBy : "unknown");
            }
            return true;
        }
        return false;
    }
    
    /**
     * Check if a player is banned (checks NeoEssentials list first, then vanilla ban list).
     */
    public boolean isPlayerBanned(UUID playerId) {
        BanEntry ban = playerBans.get(playerId);
        if (ban != null) {
            if (ban.isExpired()) {
                if (com.zerog.neoessentials.config.ConfigManager.getInstance().isAutoExpireTempBansEnabled()) {
                    archiveExpiredBan(ban);
                    playerBans.remove(playerId);
                }
                return false;
            }
            return true;
        }
        // Fallback: check vanilla ban list (handles bans issued by /ban or operator tools)
        try {
            MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                com.mojang.authlib.GameProfile profile = resolveProfile(server, playerId, null);
                if (profile != null) {
                    net.minecraft.server.players.UserBanList vanillaBans = server.getPlayerList().getBans();
                    if (vanillaBans.isBanned(profile)) {
                        // Import this vanilla ban into our list so it appears in /banlist
                        net.minecraft.server.players.UserBanListEntry entry = vanillaBans.get(profile);
                        if (entry != null) {
                            String reason = entry.getReason();
                            String source = entry.getSource();
                            BanEntry imported = new BanEntry(profile.getName(), playerId, reason, source);
                            if (entry.getExpires() != null) {
                                imported.expireTime = entry.getExpires().getTime();
                            }
                            playerBans.put(playerId, imported);
                            store.put(PLAYER_COLLECTION, imported.id, banToJson(imported));
                            LOGGER.info("Imported vanilla ban for {} into NeoEssentials ban list", profile.getName());
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Could not check vanilla ban list for {}: {}", playerId, e.getMessage());
        }
        return false;
    }
    
    /**
     * Check if an IP is banned (also handles expiry of temp IP bans)
     */
    public boolean isIPBanned(String ipAddress) {
        IPBanEntry ban = ipBans.get(ipAddress);
        if (ban == null) return false;
        if (ban.isExpired()) {
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isAutoExpireTempBansEnabled()) {
                archiveExpiredIPBan(ban);
                ipBans.remove(ipAddress);
            }
            return false;
        }
        return true;
    }

    /**
     * Check if a player can join the server (not banned)
     */
    @SuppressWarnings("unused") // Public API method - may be used by other plugins/mods
    public boolean canPlayerJoin(ServerPlayer player) {
        return !isPlayerBanned(player.getUUID()) && !isIPBanned(getPlayerIP(player));
    }
    
    /**
     * Get ban entry for a player
     */
    @SuppressWarnings("unused") // Public API method - may be used by other plugins/mods
    public BanEntry getBanEntry(UUID playerId) {
        BanEntry ban = playerBans.get(playerId);
        if (ban != null && ban.isExpired()) {
            if (com.zerog.neoessentials.config.ConfigManager.getInstance().isAutoExpireTempBansEnabled()) {
                // Auto-remove expired ban
                archiveExpiredBan(ban);
                playerBans.remove(playerId);
            }
            return null;
        }
        return ban;
    }
    
    /**
     * Get IP ban entry
     */
    @SuppressWarnings("unused") // Public API method
    public IPBanEntry getIPBanEntry(String ipAddress) {
        return ipBans.get(ipAddress);
    }
    
    /**
     * Get all active player bans
     */
    public List<BanEntry> getAllPlayerBans() {
        List<BanEntry> activeBans = new ArrayList<>();
        Iterator<Map.Entry<UUID, BanEntry>> iterator = playerBans.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, BanEntry> entry = iterator.next();
            BanEntry ban = entry.getValue();
            if (ban.isExpired()) {
                if (com.zerog.neoessentials.config.ConfigManager.getInstance().isAutoExpireTempBansEnabled()) {
                    // Remove expired ban (archiveExpiredBan() persists the change itself)
                    archiveExpiredBan(ban);
                    iterator.remove();
                }
            } else {
                activeBans.add(ban);
            }
        }
        return activeBans;
    }
    
    /**
     * Get all IP bans
     */
    public List<IPBanEntry> getAllIPBans() {
        return new ArrayList<>(ipBans.values());
    }

    /**
     * Full ban history for a player — any currently-active ban plus every archived
     * (expired or reversed) past ban, newest first. Matches ban-management plugins'
     * per-player punishment record view.
     */
    public List<BanEntry> getBanHistory(UUID playerId) {
        List<BanEntry> history = new ArrayList<>();
        BanEntry active = playerBans.get(playerId);
        if (active != null) history.add(active);
        for (BanEntry archived : banHistory) {
            if (archived.playerId.equals(playerId)) history.add(archived);
        }
        history.sort((a, b) -> Long.compare(b.banTime, a.banTime));
        return history;
    }

    /** Full ban history for an IP address — see {@link #getBanHistory(UUID)}. */
    public List<IPBanEntry> getIPBanHistory(String ipAddress) {
        List<IPBanEntry> history = new ArrayList<>();
        IPBanEntry active = ipBans.get(ipAddress);
        if (active != null) history.add(active);
        for (IPBanEntry archived : ipBanHistory) {
            if (archived.ipAddress.equals(ipAddress)) history.add(archived);
        }
        history.sort((a, b) -> Long.compare(b.banTime, a.banTime));
        return history;
    }

    /** Every archived (no-longer-active) ban across all players — for dashboard history views. */
    public List<BanEntry> getAllBanHistory() {
        return new ArrayList<>(banHistory);
    }

    /** Every archived (no-longer-active) IP ban — for dashboard history views. */
    public List<IPBanEntry> getAllIPBanHistory() {
        return new ArrayList<>(ipBanHistory);
    }

    /**
     * Parse duration string (1d, 2h, 30m, 60s) to milliseconds
     */
    public static long parseDuration(String duration) {
        if (duration == null || duration.isEmpty()) {
            return 0;
        }
        
        duration = duration.toLowerCase().trim();
        long totalMillis = 0;
        
        try {
            // Split by common separators and handle multiple time units
            String[] parts = duration.split("[\\s,]+");
            
            for (String part : parts) {
                if (part.isEmpty()) continue;
                
                // Extract number and unit
                String numberStr = part.replaceAll("[^0-9]", "");
                String unit = part.replaceAll("[0-9]", "");
                
                if (numberStr.isEmpty()) continue;
                
                long value = Long.parseLong(numberStr);
                
                switch (unit) {
                    case "s":
                    case "sec":
                    case "second":
                    case "seconds":
                        totalMillis += value * 1000;
                        break;
                    case "m":
                    case "min":
                    case "minute":
                    case "minutes":
                        totalMillis += value * 60 * 1000;
                        break;
                    case "h":
                    case "hr":
                    case "hour":
                    case "hours":
                        totalMillis += value * 60 * 60 * 1000;
                        break;
                    case "d":
                    case "day":
                    case "days":
                        totalMillis += value * 24 * 60 * 60 * 1000;
                        break;
                    case "w":
                    case "week":
                    case "weeks":
                        totalMillis += value * 7 * 24 * 60 * 60 * 1000;
                        break;
                    case "mo":
                    case "month":
                    case "months":
                        totalMillis += value * 30L * 24 * 60 * 60 * 1000; // Approximate
                        break;
                    case "y":
                    case "year":
                    case "years":
                        totalMillis += value * 365L * 24 * 60 * 60 * 1000; // Approximate
                        break;
                    default:
                        // If no unit specified, assume minutes
                        totalMillis += value * 60 * 1000;
                        break;
                }
            }
        } catch (NumberFormatException e) {
            LOGGER.warn("Invalid duration format: {}", duration);
            return 0;
        }
        
        return totalMillis;
    }
    
    /**
     * Format duration from milliseconds to readable string
     */
    public static String formatDuration(long durationMillis) {
        if (durationMillis <= 0) return "0s";
        
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        seconds %= 60;
        minutes %= 60;
        hours %= 24;
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");

        return sb.toString().trim();
    }
    
    /**
     * Get player's IP address
     */
    private String getPlayerIP(ServerPlayer player) {
        try {
            // Use reflection to access protected connection field
            java.lang.reflect.Field connectionField = player.connection.getClass().getDeclaredField("connection");
            connectionField.setAccessible(true);
            Object connection = connectionField.get(player.connection);
            
            java.lang.reflect.Method getRemoteAddressMethod = connection.getClass().getMethod("getRemoteAddress");
            String fullAddress = getRemoteAddressMethod.invoke(connection).toString();
            
            // Extract IP from format "/127.0.0.1:port"
            return fullAddress.split(":")[0].substring(1);
        } catch (Exception e) {
            LOGGER.debug("Failed to get IP for player {}: {}", player.getName().getString(), e.getMessage());
            return "unknown";
        }
    }
    
    /**
     * Format timestamp to readable string
     */
    private static String formatTime(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
    
    /**
     * Load bans from the active {@link com.zerog.neoessentials.storage.DataStore}. Both
     * active and archived (history) entries live in the same collection, split by
     * BanEntry.active — see PLAYER_COLLECTION/IP_COLLECTION.
     */
    private void loadBans() {
        for (JsonObject obj : store.getAll(PLAYER_COLLECTION).values()) {
            BanEntry ban = banFromJson(obj);
            if (ban.active && !ban.isExpired()) {
                playerBans.put(ban.playerId, ban);
            } else {
                banHistory.add(ban);
            }
        }
        for (JsonObject obj : store.getAll(IP_COLLECTION).values()) {
            IPBanEntry ban = ipBanFromJson(obj);
            if (ban.active && !ban.isExpired()) {
                ipBans.put(ban.ipAddress, ban);
            } else {
                ipBanHistory.add(ban);
            }
        }
    }

    private BanEntry banFromJson(JsonObject banObj) {
        BanEntry ban = new BanEntry(
            banObj.get("playerName").getAsString(),
            UUID.fromString(banObj.get("playerId").getAsString()),
            banObj.get("reason").getAsString(),
            banObj.get("bannedBy").getAsString()
        );
        ban.banTime = banObj.get("banTime").getAsLong();
        ban.expireTime = banObj.has("expireTime") ? banObj.get("expireTime").getAsLong() : 0;
        ban.id = banObj.has("id") ? banObj.get("id").getAsString() : UUID.randomUUID().toString();
        ban.evidence = banObj.has("evidence") && !banObj.get("evidence").isJsonNull() ? banObj.get("evidence").getAsString() : null;
        ban.active = !banObj.has("active") || banObj.get("active").getAsBoolean();
        ban.unbannedBy = banObj.has("unbannedBy") && !banObj.get("unbannedBy").isJsonNull() ? banObj.get("unbannedBy").getAsString() : null;
        ban.unbannedAt = banObj.has("unbannedAt") ? banObj.get("unbannedAt").getAsLong() : 0;
        return ban;
    }

    private IPBanEntry ipBanFromJson(JsonObject banObj) {
        IPBanEntry ban = new IPBanEntry(
            banObj.get("ipAddress").getAsString(),
            banObj.get("reason").getAsString(),
            banObj.get("bannedBy").getAsString()
        );
        ban.banTime = banObj.get("banTime").getAsLong();
        ban.expireTime = banObj.has("expireTime") ? banObj.get("expireTime").getAsLong() : 0;
        ban.id = banObj.has("id") ? banObj.get("id").getAsString() : UUID.randomUUID().toString();
        ban.evidence = banObj.has("evidence") && !banObj.get("evidence").isJsonNull() ? banObj.get("evidence").getAsString() : null;
        ban.active = !banObj.has("active") || banObj.get("active").getAsBoolean();
        ban.unbannedBy = banObj.has("unbannedBy") && !banObj.get("unbannedBy").isJsonNull() ? banObj.get("unbannedBy").getAsString() : null;
        ban.unbannedAt = banObj.has("unbannedAt") ? banObj.get("unbannedAt").getAsLong() : 0;
        return ban;
    }

    private JsonObject banToJson(BanEntry ban) {
        JsonObject banObj = new JsonObject();
        banObj.addProperty("id", ban.id);
        banObj.addProperty("playerName", ban.playerName);
        banObj.addProperty("playerId", ban.playerId.toString());
        banObj.addProperty("reason", ban.reason);
        banObj.addProperty("bannedBy", ban.bannedBy);
        banObj.addProperty("banTime", ban.banTime);
        banObj.addProperty("expireTime", ban.expireTime);
        banObj.addProperty("evidence", ban.evidence);
        banObj.addProperty("active", ban.active);
        banObj.addProperty("unbannedBy", ban.unbannedBy);
        banObj.addProperty("unbannedAt", ban.unbannedAt);
        return banObj;
    }

    private JsonObject ipBanToJson(IPBanEntry ban) {
        JsonObject banObj = new JsonObject();
        banObj.addProperty("id", ban.id);
        banObj.addProperty("ipAddress", ban.ipAddress);
        banObj.addProperty("reason", ban.reason);
        banObj.addProperty("bannedBy", ban.bannedBy);
        banObj.addProperty("banTime", ban.banTime);
        banObj.addProperty("expireTime", ban.expireTime);
        banObj.addProperty("evidence", ban.evidence);
        banObj.addProperty("active", ban.active);
        banObj.addProperty("unbannedBy", ban.unbannedBy);
        banObj.addProperty("unbannedAt", ban.unbannedAt);
        return banObj;
    }

    /**
     * One-time import of the four legacy files (player_bans.json, ip_bans.json,
     * player_ban_history.json, ip_ban_history.json) into the active DataStore, if it's
     * still empty and storage.autoMigrate is enabled. Both the "active" and "history"
     * legacy files fold into the same collection now (PLAYER_COLLECTION/IP_COLLECTION),
     * distinguished by each record's own "active" field.
     */
    private void migrateLegacyFilesIfNeeded() {
        if (store.hasAnyData(PLAYER_COLLECTION) || store.hasAnyData(IP_COLLECTION)) return;
        if (!com.zerog.neoessentials.config.ConfigManager.getInstance().isStorageAutoMigrateEnabled()) return;

        int migrated = 0;
        migrated += migrateLegacyBanFile("player_bans.json", PLAYER_COLLECTION, true);
        migrated += migrateLegacyBanFile("player_ban_history.json", PLAYER_COLLECTION, false);
        migrated += migrateLegacyIPBanFile("ip_bans.json", IP_COLLECTION, true);
        migrated += migrateLegacyIPBanFile("ip_ban_history.json", IP_COLLECTION, false);

        if (migrated > 0) {
            LOGGER.info("BanManager: migrated {} ban record(s) from legacy files into the '{}' storage backend.",
                migrated, com.zerog.neoessentials.storage.StorageManager.getInstance().getActiveType());
        }
    }

    private int migrateLegacyBanFile(String filename, String collection, boolean defaultActive) {
        File file = new File(com.zerog.neoessentials.util.ResourceUtil.DATA_DIR + "moderation", filename);
        if (!file.exists()) return 0;

        int count = 0;
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = gson.fromJson(reader, JsonObject.class);
            if (root == null || !root.has("bans")) return 0;
            for (JsonElement element : root.getAsJsonArray("bans")) {
                JsonObject obj = element.getAsJsonObject().deepCopy();
                if (!obj.has("active")) obj.addProperty("active", defaultActive);
                String id = obj.has("id") ? obj.get("id").getAsString() : UUID.randomUUID().toString();
                store.put(collection, id, obj);
                count++;
            }
        } catch (IOException e) {
            LOGGER.error("Failed to migrate legacy {}: {}", filename, e.getMessage());
        }
        return count;
    }

    private int migrateLegacyIPBanFile(String filename, String collection, boolean defaultActive) {
        // Same shape as migrateLegacyBanFile — kept as a separate method name for clarity
        // at call sites even though the body is identical (both are just JSON pass-through).
        return migrateLegacyBanFile(filename, collection, defaultActive);
    }
}
