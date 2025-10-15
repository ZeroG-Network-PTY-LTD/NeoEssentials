package com.zerog.neoessentials.moderation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages server bans and whitelist entries.
 */
public class ModerationManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationManager.class);
    private static ModerationManager instance;
    
    private final Map<String, BanEntry> bans;
    private final Map<String, WhitelistEntry> whitelist;
    private final Path storageDirectory;
    private final Path bansFile;
    private final Path whitelistFile;
    private final Gson gson;
    private MinecraftServer server;
    private boolean whitelistEnabled;
    
    private static final String MODERATION_DIR = "neoessentials/moderation";
    private static final String BANS_FILE = "bans.json";
    private static final String WHITELIST_FILE = "whitelist.json";
    
    private ModerationManager() {
        this.bans = new ConcurrentHashMap<>();
        this.whitelist = new ConcurrentHashMap<>();
        this.storageDirectory = Paths.get(MODERATION_DIR);
        this.bansFile = storageDirectory.resolve(BANS_FILE);
        this.whitelistFile = storageDirectory.resolve(WHITELIST_FILE);
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.whitelistEnabled = false;
        
        try {
            Files.createDirectories(storageDirectory);
            loadBans();
            loadWhitelist();
        } catch (IOException e) {
            LOGGER.error("Failed to create moderation directory", e);
        }
    }
    
    public static ModerationManager getInstance() {
        if (instance == null) {
            instance = new ModerationManager();
        }
        return instance;
    }
    
    public void setServer(MinecraftServer server) {
        this.server = server;
    }
    
    // ===== BAN MANAGEMENT =====
    
    /**
     * Add a new ban entry
     */
    public BanEntry addBan(BanEntry.BanType type, String target, String playerName, 
                           String reason, String evidence, Instant expiresAt, String bannedBy) {
        BanEntry ban = new BanEntry();
        ban.setType(type);
        ban.setTarget(target);
        ban.setPlayerName(playerName);
        ban.setReason(reason);
        ban.setEvidence(evidence);
        ban.setExpiresAt(expiresAt);
        ban.setBannedBy(bannedBy);
        ban.setActive(true);
        
        bans.put(ban.getId(), ban);
        saveBans();
        
        // Kick player if online
        if (server != null && type != BanEntry.BanType.IP) {
            try {
                UUID uuid = UUID.fromString(target);
                ServerPlayer player = server.getPlayerList().getPlayer(uuid);
                if (player != null) {
                    String kickMessage = "You have been banned from this server.\nReason: " + reason;
                    if (!ban.isPermanent()) {
                        kickMessage += "\nExpires: " + expiresAt.toString();
                    }
                    player.connection.disconnect(Component.literal(kickMessage));
                }
            } catch (IllegalArgumentException e) {
                LOGGER.debug("Target is not a valid UUID: {}", target);
            }
        }
        
        LOGGER.info("Ban added: {} banned {} ({})", bannedBy, playerName != null ? playerName : target, reason);
        return ban;
    }
    
    /**
     * Remove a ban
     */
    public boolean removeBan(String banId) {
        BanEntry ban = bans.remove(banId);
        if (ban != null) {
            saveBans();
            LOGGER.info("Ban removed: {} ({})", ban.getPlayerName() != null ? ban.getPlayerName() : ban.getTarget(), ban.getId());
            return true;
        }
        return false;
    }
    
    /**
     * Get a ban by ID
     */
    public BanEntry getBan(String banId) {
        return bans.get(banId);
    }
    
    /**
     * Get all bans
     */
    public Collection<BanEntry> getAllBans() {
        return bans.values();
    }
    
    /**
     * Get active bans
     */
    public Collection<BanEntry> getActiveBans() {
        return bans.values().stream()
            .filter(BanEntry::isActive)
            .filter(ban -> !ban.isExpired())
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a player/IP is banned
     */
    public BanEntry checkBan(String uuid, String ip) {
        // Check for expired bans and deactivate them
        bans.values().stream()
            .filter(ban -> ban.isActive() && ban.isExpired())
            .forEach(ban -> {
                ban.setActive(false);
                saveBans();
            });
        
        // Check UUID bans
        if (uuid != null) {
            Optional<BanEntry> uuidBan = bans.values().stream()
                .filter(BanEntry::isActive)
                .filter(ban -> ban.getType() == BanEntry.BanType.UUID || ban.getType() == BanEntry.BanType.BOTH)
                .filter(ban -> ban.getTarget().equals(uuid))
                .filter(ban -> !ban.isExpired())
                .findFirst();
            
            if (uuidBan.isPresent()) {
                return uuidBan.get();
            }
        }
        
        // Check IP bans
        if (ip != null) {
            Optional<BanEntry> ipBan = bans.values().stream()
                .filter(BanEntry::isActive)
                .filter(ban -> ban.getType() == BanEntry.BanType.IP || ban.getType() == BanEntry.BanType.BOTH)
                .filter(ban -> ban.getTarget().equals(ip) || ban.getTarget().startsWith(ip.substring(0, ip.lastIndexOf('.'))))
                .filter(ban -> !ban.isExpired())
                .findFirst();
            
            if (ipBan.isPresent()) {
                return ipBan.get();
            }
        }
        
        return null;
    }
    
    /**
     * Get ban history for a player
     */
    public List<BanEntry> getBanHistory(String target) {
        return bans.values().stream()
            .filter(ban -> ban.getTarget().equals(target))
            .sorted(Comparator.comparing(BanEntry::getBannedAt).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Submit a ban appeal
     */
    public boolean submitAppeal(String banId, String appealText) {
        BanEntry ban = bans.get(banId);
        if (ban == null || !ban.isActive()) {
            return false;
        }
        
        BanEntry.BanAppeal appeal = new BanEntry.BanAppeal();
        appeal.setAppealText(appealText);
        ban.setAppeal(appeal);
        saveBans();
        
        LOGGER.info("Ban appeal submitted for ban {}", banId);
        return true;
    }
    
    /**
     * Review a ban appeal
     */
    public boolean reviewAppeal(String banId, BanEntry.BanAppeal.AppealStatus status, 
                                String reviewedBy, String reviewNotes) {
        BanEntry ban = bans.get(banId);
        if (ban == null || !ban.hasAppeal()) {
            return false;
        }
        
        BanEntry.BanAppeal appeal = ban.getAppeal();
        appeal.setStatus(status);
        appeal.setReviewedBy(reviewedBy);
        appeal.setReviewedAt(Instant.now());
        appeal.setReviewNotes(reviewNotes);
        
        // If approved, lift the ban
        if (status == BanEntry.BanAppeal.AppealStatus.APPROVED) {
            ban.setActive(false);
        }
        
        saveBans();
        LOGGER.info("Ban appeal {} for ban {}: {}", status, banId, reviewNotes);
        return true;
    }
    
    // ===== WHITELIST MANAGEMENT =====
    
    /**
     * Enable/disable whitelist
     */
    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
        LOGGER.info("Whitelist {}", enabled ? "enabled" : "disabled");
    }
    
    /**
     * Check if whitelist is enabled
     */
    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }
    
    /**
     * Add a whitelist entry
     */
    public WhitelistEntry addWhitelist(WhitelistEntry.WhitelistType type, String target, 
                                       String playerName, String addedBy, String notes) {
        WhitelistEntry entry = new WhitelistEntry();
        entry.setType(type);
        entry.setTarget(target);
        entry.setPlayerName(playerName);
        entry.setAddedBy(addedBy);
        entry.setNotes(notes);
        
        whitelist.put(entry.getId(), entry);
        saveWhitelist();
        
        LOGGER.info("Whitelist entry added: {} by {}", playerName != null ? playerName : target, addedBy);
        return entry;
    }
    
    /**
     * Remove a whitelist entry
     */
    public boolean removeWhitelist(String entryId) {
        WhitelistEntry entry = whitelist.remove(entryId);
        if (entry != null) {
            saveWhitelist();
            LOGGER.info("Whitelist entry removed: {}", entry.getPlayerName() != null ? entry.getPlayerName() : entry.getTarget());
            return true;
        }
        return false;
    }
    
    /**
     * Get all whitelist entries
     */
    public Collection<WhitelistEntry> getAllWhitelist() {
        return whitelist.values();
    }
    
    /**
     * Check if a player/IP is whitelisted
     */
    public boolean isWhitelisted(String uuid, String ip) {
        if (!whitelistEnabled) {
            return true; // Whitelist disabled, everyone allowed
        }
        
        // Check UUID whitelist
        if (uuid != null) {
            boolean uuidWhitelisted = whitelist.values().stream()
                .anyMatch(entry -> entry.getType() == WhitelistEntry.WhitelistType.UUID && 
                                 entry.getTarget().equals(uuid));
            if (uuidWhitelisted) {
                return true;
            }
        }
        
        // Check IP whitelist
        if (ip != null) {
            boolean ipWhitelisted = whitelist.values().stream()
                .anyMatch(entry -> entry.getType() == WhitelistEntry.WhitelistType.IP && 
                                 (entry.getTarget().equals(ip) || 
                                  ip.startsWith(entry.getTarget())));
            if (ipWhitelisted) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Bulk import whitelist entries
     */
    public int importWhitelist(List<WhitelistEntry> entries, String importedBy) {
        int imported = 0;
        for (WhitelistEntry entry : entries) {
            entry.setId(UUID.randomUUID().toString());
            entry.setAddedBy(importedBy);
            entry.setAddedAt(Instant.now());
            whitelist.put(entry.getId(), entry);
            imported++;
        }
        saveWhitelist();
        LOGGER.info("Imported {} whitelist entries", imported);
        return imported;
    }
    
    // ===== PERSISTENCE =====
    
    private void loadBans() {
        if (!Files.exists(bansFile)) {
            return;
        }
        
        try {
            String json = Files.readString(bansFile);
            Map<String, BanEntry> loaded = gson.fromJson(json, new TypeToken<Map<String, BanEntry>>(){}.getType());
            if (loaded != null) {
                bans.putAll(loaded);
                LOGGER.info("Loaded {} bans", loaded.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load bans", e);
        }
    }
    
    private void saveBans() {
        try {
            String json = gson.toJson(bans);
            Files.writeString(bansFile, json);
        } catch (Exception e) {
            LOGGER.error("Failed to save bans", e);
        }
    }
    
    private void loadWhitelist() {
        if (!Files.exists(whitelistFile)) {
            return;
        }
        
        try {
            String json = Files.readString(whitelistFile);
            Map<String, WhitelistEntry> loaded = gson.fromJson(json, new TypeToken<Map<String, WhitelistEntry>>(){}.getType());
            if (loaded != null) {
                whitelist.putAll(loaded);
                LOGGER.info("Loaded {} whitelist entries", loaded.size());
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load whitelist", e);
        }
    }
    
    private void saveWhitelist() {
        try {
            String json = gson.toJson(whitelist);
            Files.writeString(whitelistFile, json);
        } catch (Exception e) {
            LOGGER.error("Failed to save whitelist", e);
        }
    }
}
