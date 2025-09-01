package com.zerog.neoessentials.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Achievement system for tracking player milestones and accomplishments
 * Provides configurable achievements with progress tracking and rewards
 * 
 * @author ZeroG
 * @since 2.1.0
 */
public class AchievementSystem {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(AchievementSystem.class);
    private static AchievementSystem instance;
    
    private final Map<String, Achievement> achievements;
    private final PlayerDataManager playerDataManager;
    private final StatisticsManager statisticsManager;
    
    private AchievementSystem() {
        this.achievements = new ConcurrentHashMap<>();
        this.playerDataManager = PlayerDataManager.getInstance();
        this.statisticsManager = StatisticsManager.getInstance();
        
        // Initialize default achievements
        initializeDefaultAchievements();
    }
    
    public static AchievementSystem getInstance() {
        if (instance == null) {
            instance = new AchievementSystem();
        }
        return instance;
    }
    
    /**
     * Initialize default achievements
     */
    private void initializeDefaultAchievements() {
        // Welcome achievements
        registerAchievement(new Achievement("first_join", "Welcome!", 
            "Join the server for the first time", 1, AchievementCategory.GENERAL));
        
        registerAchievement(new Achievement("first_hour", "Getting Started", 
            "Play for 1 hour", 3600, AchievementCategory.PLAYTIME));
        
        registerAchievement(new Achievement("dedication", "Dedicated Player", 
            "Play for 10 hours", 36000, AchievementCategory.PLAYTIME));
        
        registerAchievement(new Achievement("veteran", "Veteran", 
            "Play for 100 hours", 360000, AchievementCategory.PLAYTIME));
        
        // Combat achievements
        registerAchievement(new Achievement("first_kill", "First Blood", 
            "Kill your first player", 1, AchievementCategory.COMBAT));
        
        registerAchievement(new Achievement("serial_killer", "Serial Killer", 
            "Kill 10 players", 10, AchievementCategory.COMBAT));
        
        registerAchievement(new Achievement("massacre", "Massacre", 
            "Kill 100 players", 100, AchievementCategory.COMBAT));
        
        registerAchievement(new Achievement("survivor", "Survivor", 
            "Maintain a K/D ratio above 2.0", 2, AchievementCategory.COMBAT));
        
        registerAchievement(new Achievement("mob_hunter", "Mob Hunter", 
            "Kill 100 mobs", 100, AchievementCategory.COMBAT));
        
        // Building achievements
        registerAchievement(new Achievement("first_block", "First Steps", 
            "Break your first block", 1, AchievementCategory.BUILDING));
        
        registerAchievement(new Achievement("demolition", "Demolition Expert", 
            "Break 1,000 blocks", 1000, AchievementCategory.BUILDING));
        
        registerAchievement(new Achievement("architect", "Architect", 
            "Place 10,000 blocks", 10000, AchievementCategory.BUILDING));
        
        registerAchievement(new Achievement("master_builder", "Master Builder", 
            "Place 100,000 blocks", 100000, AchievementCategory.BUILDING));
        
        // Movement achievements
        registerAchievement(new Achievement("wanderer", "Wanderer", 
            "Travel 1,000 blocks", 1000, AchievementCategory.EXPLORATION));
        
        registerAchievement(new Achievement("explorer", "Explorer", 
            "Travel 10,000 blocks", 10000, AchievementCategory.EXPLORATION));
        
        registerAchievement(new Achievement("world_walker", "World Walker", 
            "Travel 100,000 blocks", 100000, AchievementCategory.EXPLORATION));
        
        // Economy achievements
        registerAchievement(new Achievement("first_payment", "First Transaction", 
            "Send your first payment to another player", 1, AchievementCategory.ECONOMY));
        
        registerAchievement(new Achievement("wealthy", "Wealthy", 
            "Accumulate $10,000", 10000, AchievementCategory.ECONOMY));
        
        registerAchievement(new Achievement("millionaire", "Millionaire", 
            "Accumulate $1,000,000", 1000000, AchievementCategory.ECONOMY));
        
        // Social achievements
        registerAchievement(new Achievement("social", "Social Butterfly", 
            "Send 100 chat messages", 100, AchievementCategory.SOCIAL));
        
        registerAchievement(new Achievement("helpful", "Helpful Player", 
            "Help 10 new players", 10, AchievementCategory.SOCIAL));
        
        // Teleportation achievements
        registerAchievement(new Achievement("homey", "Home Sweet Home", 
            "Set your first home", 1, AchievementCategory.TELEPORTATION));
        
        registerAchievement(new Achievement("teleporter", "Teleporter", 
            "Use 50 warps", 50, AchievementCategory.TELEPORTATION));
        
        // Admin achievements
        registerAchievement(new Achievement("moderator", "Moderator", 
            "Successfully moderate 10 players", 10, AchievementCategory.MODERATION));
        
        LOGGER.info("Initialized {} default achievements", achievements.size());
    }
    
    /**
     * Register a new achievement
     */
    public void registerAchievement(Achievement achievement) {
        achievements.put(achievement.getId(), achievement);
        LOGGER.debug("Registered achievement: {}", achievement.getId());
    }
    
    /**
     * Get achievement by ID
     */
    public Achievement getAchievement(String achievementId) {
        return achievements.get(achievementId);
    }
    
    /**
     * Get all achievements
     */
    public Collection<Achievement> getAllAchievements() {
        return achievements.values();
    }
    
    /**
     * Get achievements by category
     */
    public List<Achievement> getAchievementsByCategory(AchievementCategory category) {
        return achievements.values().stream()
            .filter(achievement -> achievement.getCategory() == category)
            .sorted(Comparator.comparing(Achievement::getRequiredProgress))
            .toList();
    }
    
    /**
     * Check if player has achievement
     */
    public boolean hasAchievement(UUID playerUUID, String achievementId) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        return playerData.hasAchievement(achievementId);
    }
    
    /**
     * Award achievement to player
     */
    public boolean awardAchievement(UUID playerUUID, String achievementId) {
        Achievement achievement = getAchievement(achievementId);
        if (achievement == null) {
            LOGGER.warn("Attempted to award unknown achievement: {}", achievementId);
            return false;
        }
        
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        if (playerData.hasAchievement(achievementId)) {
            return false; // Already has achievement
        }
        
        playerData.addAchievement(achievementId);
        playerDataManager.updatePlayerData(playerData);
        
        LOGGER.info("Awarded achievement '{}' to player {}", achievementId, playerUUID);
        return true;
    }
    
    /**
     * Award achievement to online player with notification
     */
    public boolean awardAchievement(ServerPlayer player, String achievementId) {
        boolean awarded = awardAchievement(player.getUUID(), achievementId);
        
        if (awarded) {
            Achievement achievement = getAchievement(achievementId);
            if (achievement != null) {
                // Send achievement notification
                Component message = Component.literal(String.format(
                    "§6§l[ACHIEVEMENT] §r§e%s§r\n§7%s", 
                    achievement.getName(), achievement.getDescription()));
                player.sendSystemMessage(message);
                
                // Play achievement sound (if available)
                // player.playNotifySound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.0F, 1.0F);
            }
        }
        
        return awarded;
    }
    
    /**
     * Update achievement progress
     */
    public void updateProgress(UUID playerUUID, String achievementId, int progress) {
        Achievement achievement = getAchievement(achievementId);
        if (achievement == null) {
            return;
        }
        
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        
        // Don't update if already completed
        if (playerData.hasAchievement(achievementId)) {
            return;
        }
        
        playerData.setAchievementProgress(achievementId, progress);
        
        // Check if achievement is completed
        if (progress >= achievement.getRequiredProgress()) {
            awardAchievement(playerUUID, achievementId);
        }
        
        playerDataManager.updatePlayerData(playerData);
    }
    
    /**
     * Increment achievement progress
     */
    public void incrementProgress(UUID playerUUID, String achievementId, int amount) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        int currentProgress = playerData.getAchievementProgress(achievementId);
        updateProgress(playerUUID, achievementId, currentProgress + amount);
    }
    
    /**
     * Increment achievement progress for online player
     */
    public void incrementProgress(ServerPlayer player, String achievementId, int amount) {
        incrementProgress(player.getUUID(), achievementId, amount);
    }
    
    /**
     * Get player's achievement progress
     */
    public int getProgress(UUID playerUUID, String achievementId) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        return playerData.getAchievementProgress(achievementId);
    }
    
    /**
     * Get player's completed achievements
     */
    public List<Achievement> getCompletedAchievements(UUID playerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        Map<String, Long> completedAchievements = playerData.getAchievements();
        
        return completedAchievements.keySet().stream()
            .map(this::getAchievement)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(Achievement::getName))
            .toList();
    }
    
    /**
     * Get player's achievement progress summary
     */
    public AchievementProgress getPlayerProgress(UUID playerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        Map<String, Long> completed = playerData.getAchievements();
        Map<String, Integer> progress = playerData.getAchievementProgress();
        
        return new AchievementProgress(playerUUID, completed.size(), achievements.size(), 
            completed, progress, calculateAchievementScore(playerUUID));
    }
    
    /**
     * Calculate achievement score for player
     */
    public int calculateAchievementScore(UUID playerUUID) {
        PlayerData playerData = playerDataManager.getPlayerData(playerUUID);
        Map<String, Long> completedAchievements = playerData.getAchievements();
        
        return completedAchievements.keySet().stream()
            .map(this::getAchievement)
            .filter(Objects::nonNull)
            .mapToInt(Achievement::getPoints)
            .sum();
    }
    
    /**
     * Get achievement leaderboard
     */
    public List<AchievementLeaderboardEntry> getLeaderboard(int limit) {
        // This would need to iterate through all player data files
        // For now, return empty list as this is performance-intensive
        return new ArrayList<>();
    }
    
    /**
     * Check and award automatic achievements based on player stats
     */
    public void checkAutomaticAchievements(ServerPlayer player) {
        UUID playerUUID = player.getUUID();
        
        // Get player statistics
        StatisticsManager.PlayerStatistics stats = statisticsManager.getPlayerStatistics(playerUUID);
        
        // Check playtime achievements
        PlaytimeTracker playtimeTracker = PlaytimeTracker.getInstance();
        long totalPlaytimeSeconds = playtimeTracker.getTotalPlaytime(playerUUID) / 1000;
        
        updateProgress(playerUUID, "first_hour", (int) totalPlaytimeSeconds);
        updateProgress(playerUUID, "dedication", (int) totalPlaytimeSeconds);
        updateProgress(playerUUID, "veteran", (int) totalPlaytimeSeconds);
        
        // Combat achievements
        updateProgress(playerUUID, "first_kill", stats.getKills());
        updateProgress(playerUUID, "serial_killer", stats.getKills());
        updateProgress(playerUUID, "massacre", stats.getKills());
        updateProgress(playerUUID, "mob_hunter", stats.getMobKills());
        
        // Check K/D ratio for survivor achievement
        if (stats.getKDR() >= 2.0 && !hasAchievement(playerUUID, "survivor")) {
            awardAchievement(player, "survivor");
        }
        
        // Building achievements
        updateProgress(playerUUID, "first_block", stats.getBlocksBroken());
        updateProgress(playerUUID, "demolition", stats.getBlocksBroken());
        updateProgress(playerUUID, "architect", stats.getBlocksPlaced());
        updateProgress(playerUUID, "master_builder", stats.getBlocksPlaced());
        
        // Movement achievements
        updateProgress(playerUUID, "wanderer", (int) stats.getDistanceTraveled());
        updateProgress(playerUUID, "explorer", (int) stats.getDistanceTraveled());
        updateProgress(playerUUID, "world_walker", (int) stats.getDistanceTraveled());
        
        // Social achievements
        Object chatMessages = stats.getStatistic("chat_messages");
        if (chatMessages instanceof Number) {
            updateProgress(playerUUID, "social", ((Number) chatMessages).intValue());
        }
        
        // First join achievement
        if (!hasAchievement(playerUUID, "first_join")) {
            awardAchievement(player, "first_join");
        }
    }
    
    /**
     * Achievement categories
     */
    public enum AchievementCategory {
        GENERAL("General"),
        PLAYTIME("Playtime"),
        ECONOMY("Economy"),
        SOCIAL("Social"),
        TELEPORTATION("Teleportation"),
        MODERATION("Moderation"),
        BUILDING("Building"),
        COMBAT("Combat"),
        EXPLORATION("Exploration");
        
        private final String displayName;
        
        AchievementCategory(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    /**
     * Achievement data class
     */
    public static class Achievement {
        private final String id;
        private final String name;
        private final String description;
        private final int requiredProgress;
        private final AchievementCategory category;
        private final int points;
        private final boolean isSecret;
        
        public Achievement(String id, String name, String description, int requiredProgress, 
                         AchievementCategory category) {
            this(id, name, description, requiredProgress, category, 1, false);
        }
        
        public Achievement(String id, String name, String description, int requiredProgress, 
                         AchievementCategory category, int points, boolean isSecret) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.requiredProgress = requiredProgress;
            this.category = category;
            this.points = points;
            this.isSecret = isSecret;
        }
        
        // Getters
        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public int getRequiredProgress() { return requiredProgress; }
        public AchievementCategory getCategory() { return category; }
        public int getPoints() { return points; }
        public boolean isSecret() { return isSecret; }
        
        @Override
        public String toString() {
            return String.format("Achievement{id=%s, name=%s, points=%d}", id, name, points);
        }
    }
    
    /**
     * Achievement progress summary
     */
    public static class AchievementProgress {
        private final UUID playerUUID;
        private final int completedCount;
        private final int totalCount;
        private final Map<String, Long> completedAchievements;
        private final Map<String, Integer> progressMap;
        private final int totalScore;
        
        public AchievementProgress(UUID playerUUID, int completedCount, int totalCount,
                                 Map<String, Long> completedAchievements, 
                                 Map<String, Integer> progressMap, int totalScore) {
            this.playerUUID = playerUUID;
            this.completedCount = completedCount;
            this.totalCount = totalCount;
            this.completedAchievements = completedAchievements;
            this.progressMap = progressMap;
            this.totalScore = totalScore;
        }
        
        // Getters
        public UUID getPlayerUUID() { return playerUUID; }
        public int getCompletedCount() { return completedCount; }
        public int getTotalCount() { return totalCount; }
        public Map<String, Long> getCompletedAchievements() { return completedAchievements; }
        public Map<String, Integer> getProgressMap() { return progressMap; }
        public int getTotalScore() { return totalScore; }
        
        public double getCompletionPercentage() {
            return totalCount > 0 ? (completedCount * 100.0) / totalCount : 0.0;
        }
    }
    
    /**
     * Leaderboard entry
     */
    public static class AchievementLeaderboardEntry {
        private final UUID playerUUID;
        private final String playerName;
        private final int completedAchievements;
        private final int totalScore;
        
        public AchievementLeaderboardEntry(UUID playerUUID, String playerName, 
                                         int completedAchievements, int totalScore) {
            this.playerUUID = playerUUID;
            this.playerName = playerName;
            this.completedAchievements = completedAchievements;
            this.totalScore = totalScore;
        }
        
        // Getters
        public UUID getPlayerUUID() { return playerUUID; }
        public String getPlayerName() { return playerName; }
        public int getCompletedAchievements() { return completedAchievements; }
        public int getTotalScore() { return totalScore; }
    }
}
