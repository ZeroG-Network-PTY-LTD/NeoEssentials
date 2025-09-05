package com.zerog.neoessentials.player;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Player notes management system for administrative records
 * Allows admins to maintain notes and records about players
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class PlayerNotesManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerNotesManager.class);
    private static PlayerNotesManager instance;
    
    private final PlayerDataManager playerDataManager;
    
    private PlayerNotesManager() {
        this.playerDataManager = PlayerDataManager.getInstance();
    }
    
    public static PlayerNotesManager getInstance() {
        if (instance == null) {
            instance = new PlayerNotesManager();
        }
        return instance;
    }
    
    /**
     * Add a note to a player's record
     */
    public String addNote(UUID targetPlayerUUID, UUID authorUUID, String authorName, 
                         String content, String category, String severity) {
        String noteId = generateNoteId();
        AdminNote note = new AdminNote(noteId, authorUUID, authorName, content, category, severity);
        
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        playerData.addAdminNote(noteId, note);
        playerDataManager.updatePlayerData(playerData);
        
        LOGGER.info("Added note '{}' to player {} by {}", noteId, targetPlayerUUID, authorName);
        return noteId;
    }
    
    /**
     * Add a simple note to a player's record
     */
    public String addNote(UUID targetPlayerUUID, UUID authorUUID, String authorName, String content) {
        return addNote(targetPlayerUUID, authorUUID, authorName, content, "general", "info");
    }
    
    /**
     * Add a note with expiration
     */
    public String addNote(UUID targetPlayerUUID, UUID authorUUID, String authorName, 
                         String content, String category, String severity, int expirationDays) {
        String noteId = addNote(targetPlayerUUID, authorUUID, authorName, content, category, severity);
        
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        AdminNote note = playerData.getAdminNote(noteId);
        if (note != null) {
            note.setExpirationDays(expirationDays);
            playerDataManager.updatePlayerData(playerData);
        }
        
        return noteId;
    }
    
    /**
     * Remove a note from a player's record
     */
    public boolean removeNote(UUID targetPlayerUUID, String noteId) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        AdminNote removedNote = playerData.getAdminNote(noteId);
        
        if (removedNote != null) {
            playerData.removeAdminNote(noteId);
            playerDataManager.updatePlayerData(playerData);
            
            LOGGER.info("Removed note '{}' from player {}", noteId, targetPlayerUUID);
            return true;
        }
        
        return false;
    }
    
    /**
     * Get a specific note
     */
    public AdminNote getNote(UUID targetPlayerUUID, String noteId) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        return playerData.getAdminNote(noteId);
    }
    
    /**
     * Get all notes for a player
     */
    public List<AdminNote> getAllNotes(UUID targetPlayerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        return new ArrayList<>(playerData.getAdminNotes().values());
    }
    
    /**
     * Get all active (non-expired) notes for a player
     */
    public List<AdminNote> getActiveNotes(UUID targetPlayerUUID) {
        return getAllNotes(targetPlayerUUID).stream()
            .filter(note -> !note.isExpired())
            .sorted(Comparator.comparing(AdminNote::getTimestamp).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * Get notes by category
     */
    public List<AdminNote> getNotesByCategory(UUID targetPlayerUUID, String category) {
        return getActiveNotes(targetPlayerUUID).stream()
            .filter(note -> note.getCategory().equalsIgnoreCase(category))
            .collect(Collectors.toList());
    }
    
    /**
     * Get notes by severity
     */
    public List<AdminNote> getNotesBySeverity(UUID targetPlayerUUID, String severity) {
        return getActiveNotes(targetPlayerUUID).stream()
            .filter(note -> note.getSeverity().equalsIgnoreCase(severity))
            .collect(Collectors.toList());
    }
    
    /**
     * Get notes by author
     */
    public List<AdminNote> getNotesByAuthor(UUID targetPlayerUUID, UUID authorUUID) {
        return getActiveNotes(targetPlayerUUID).stream()
            .filter(note -> note.getAuthorUUID().equals(authorUUID))
            .collect(Collectors.toList());
    }
    
    /**
     * Search notes by content
     */
    public List<AdminNote> searchNotes(UUID targetPlayerUUID, String searchTerm) {
        String lowerSearchTerm = searchTerm.toLowerCase();
        return getActiveNotes(targetPlayerUUID).stream()
            .filter(note -> note.getContent().toLowerCase().contains(lowerSearchTerm))
            .collect(Collectors.toList());
    }
    
    /**
     * Update note content
     */
    public boolean updateNoteContent(UUID targetPlayerUUID, String noteId, String newContent) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        AdminNote note = playerData.getAdminNote(noteId);
        
        if (note != null) {
            note.setContent(newContent);
            playerDataManager.updatePlayerData(playerData);
            
            LOGGER.info("Updated note '{}' for player {}", noteId, targetPlayerUUID);
            return true;
        }
        
        return false;
    }
    
    /**
     * Update note category
     */
    public boolean updateNoteCategory(UUID targetPlayerUUID, String noteId, String newCategory) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        AdminNote note = playerData.getAdminNote(noteId);
        
        if (note != null) {
            note.setCategory(newCategory);
            playerDataManager.updatePlayerData(playerData);
            return true;
        }
        
        return false;
    }
    
    /**
     * Update note severity
     */
    public boolean updateNoteSeverity(UUID targetPlayerUUID, String noteId, String newSeverity) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        AdminNote note = playerData.getAdminNote(noteId);
        
        if (note != null) {
            note.setSeverity(newSeverity);
            playerDataManager.updatePlayerData(playerData);
            return true;
        }
        
        return false;
    }
    
    /**
     * Set note privacy
     */
    public boolean setNotePrivacy(UUID targetPlayerUUID, String noteId, boolean isPrivate) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        AdminNote note = playerData.getAdminNote(noteId);
        
        if (note != null) {
            note.setPrivate(isPrivate);
            playerDataManager.updatePlayerData(playerData);
            return true;
        }
        
        return false;
    }
    
    /**
     * Clean up expired notes for a player
     */
    public int cleanupExpiredNotes(UUID targetPlayerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(targetPlayerUUID);
        Map<String, AdminNote> notes = playerData.getAdminNotes();
        
        List<String> expiredNoteIds = notes.values().stream()
            .filter(AdminNote::isExpired)
            .map(AdminNote::getNoteId)
            .collect(Collectors.toList());
        
        for (String noteId : expiredNoteIds) {
            playerData.removeAdminNote(noteId);
        }
        
        if (!expiredNoteIds.isEmpty()) {
            playerDataManager.updatePlayerData(playerData);
            LOGGER.info("Cleaned up {} expired notes for player {}", expiredNoteIds.size(), targetPlayerUUID);
        }
        
        return expiredNoteIds.size();
    }
    
    /**
     * Get note statistics for a player
     */
    public NoteStatistics getNoteStatistics(UUID targetPlayerUUID) {
        List<AdminNote> allNotes = getAllNotes(targetPlayerUUID);
        List<AdminNote> activeNotes = getActiveNotes(targetPlayerUUID);
        
        Map<String, Integer> categoryCount = activeNotes.stream()
            .collect(Collectors.groupingBy(
                AdminNote::getCategory,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        Map<String, Integer> severityCount = activeNotes.stream()
            .collect(Collectors.groupingBy(
                AdminNote::getSeverity,
                Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
            ));
        
        return new NoteStatistics(
            targetPlayerUUID,
            allNotes.size(),
            activeNotes.size(),
            allNotes.size() - activeNotes.size(),
            categoryCount,
            severityCount
        );
    }
    
    /**
     * Generate a unique note ID
     */
    private String generateNoteId() {
        return "note_" + System.currentTimeMillis() + "_" + 
               Integer.toHexString(new Random().nextInt(0xFFFF));
    }
    
    /**
     * Get available note categories
     */
    public List<String> getAvailableCategories() {
        return Arrays.asList(
            "general", "behavior", "moderation", "performance", 
            "technical", "positive", "warning", "violation"
        );
    }
    
    /**
     * Get available note severities
     */
    public List<String> getAvailableSeverities() {
        return Arrays.asList("info", "warning", "critical", "positive");
    }
    
    /**
     * Note statistics container
     */
    public static class NoteStatistics {
        private final UUID playerUUID;
        private final int totalNotes;
        private final int activeNotes;
        private final int expiredNotes;
        private final Map<String, Integer> categoryBreakdown;
        private final Map<String, Integer> severityBreakdown;
        
        public NoteStatistics(UUID playerUUID, int totalNotes, int activeNotes, int expiredNotes,
                             Map<String, Integer> categoryBreakdown, Map<String, Integer> severityBreakdown) {
            this.playerUUID = playerUUID;
            this.totalNotes = totalNotes;
            this.activeNotes = activeNotes;
            this.expiredNotes = expiredNotes;
            this.categoryBreakdown = categoryBreakdown;
            this.severityBreakdown = severityBreakdown;
        }
        
        // Getters
        public UUID getPlayerUUID() { return playerUUID; }
        public int getTotalNotes() { return totalNotes; }
        public int getActiveNotes() { return activeNotes; }
        public int getExpiredNotes() { return expiredNotes; }
        public Map<String, Integer> getCategoryBreakdown() { return categoryBreakdown; }
        public Map<String, Integer> getSeverityBreakdown() { return severityBreakdown; }
        
        public boolean hasWarnings() {
            return severityBreakdown.getOrDefault("warning", 0) > 0 ||
                   severityBreakdown.getOrDefault("critical", 0) > 0;
        }
        
        public int getWarningCount() {
            return severityBreakdown.getOrDefault("warning", 0) +
                   severityBreakdown.getOrDefault("critical", 0);
        }
    }
}
