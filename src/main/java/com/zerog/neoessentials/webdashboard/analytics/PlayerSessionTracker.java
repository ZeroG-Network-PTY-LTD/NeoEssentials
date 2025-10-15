package com.zerog.neoessentials.webdashboard.analytics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Player session tracker for analytics
 * Tracks player join/leave events, session duration, and activity patterns
 */
public class PlayerSessionTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSessionTracker.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static PlayerSessionTracker INSTANCE;
    
    private static final Path ANALYTICS_DIR = Paths.get("neoessentials", "analytics");
    private static final Path SESSIONS_FILE = ANALYTICS_DIR.resolve("sessions.json");
    private static final Path DAILY_STATS_FILE = ANALYTICS_DIR.resolve("daily_stats.json");
    
    // Active sessions: UUID -> Session start time
    private final Map<UUID, PlayerSession> activeSessions = new ConcurrentHashMap<>();
    
    // Historical sessions: Date -> List of sessions
    private final Map<String, List<PlayerSession>> historicalSessions = new ConcurrentHashMap<>();
    
    // Daily statistics
    private final Map<String, DailyStats> dailyStats = new ConcurrentHashMap<>();
    
    private PlayerSessionTracker() {
        try {
            if (!Files.exists(ANALYTICS_DIR)) {
                Files.createDirectories(ANALYTICS_DIR);
            }
            loadHistoricalData();
        } catch (IOException e) {
            LOGGER.error("Failed to initialize analytics directory", e);
        }
    }
    
    public static PlayerSessionTracker getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new PlayerSessionTracker();
        }
        return INSTANCE;
    }
    
    /**
     * Track player join event
     */
    public void trackPlayerJoin(UUID playerId, String playerName) {
        PlayerSession session = new PlayerSession();
        session.playerId = playerId;
        session.playerName = playerName;
        session.joinTime = System.currentTimeMillis();
        session.lastActivity = session.joinTime;
        
        activeSessions.put(playerId, session);
        
        // Update daily stats
        String today = getDateKey(session.joinTime);
        DailyStats stats = dailyStats.computeIfAbsent(today, k -> new DailyStats(today));
        stats.uniquePlayers.add(playerId);
        stats.totalJoins++;
        
        LOGGER.debug("Player joined: {} ({})", playerName, playerId);
    }
    
    /**
     * Track player leave event
     */
    public void trackPlayerLeave(UUID playerId) {
        PlayerSession session = activeSessions.remove(playerId);
        if (session == null) {
            LOGGER.warn("No active session found for player: {}", playerId);
            return;
        }
        
        session.leaveTime = System.currentTimeMillis();
        session.duration = session.leaveTime - session.joinTime;
        
        // Store historical session
        String dateKey = getDateKey(session.joinTime);
        historicalSessions.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(session);
        
        // Update daily stats
        DailyStats stats = dailyStats.get(dateKey);
        if (stats != null) {
            stats.totalPlaytime += session.duration;
            stats.totalSessions++;
            stats.averageSessionDuration = stats.totalPlaytime / stats.totalSessions;
        }
        
        // Save to disk periodically
        saveHistoricalData();
        
        LOGGER.debug("Player left: {} - Session duration: {}ms", session.playerName, session.duration);
    }
    
    /**
     * Update player activity timestamp
     */
    public void updateActivity(UUID playerId) {
        PlayerSession session = activeSessions.get(playerId);
        if (session != null) {
            session.lastActivity = System.currentTimeMillis();
        }
    }
    
    /**
     * Get active player count
     */
    public int getActivePlayerCount() {
        return activeSessions.size();
    }
    
    /**
     * Get active sessions
     */
    public Collection<PlayerSession> getActiveSessions() {
        return new ArrayList<>(activeSessions.values());
    }
    
    /**
     * Get statistics for a date range
     */
    public AnalyticsReport getAnalyticsReport(long startTime, long endTime) {
        AnalyticsReport report = new AnalyticsReport();
        report.startTime = startTime;
        report.endTime = endTime;
        report.generatedAt = System.currentTimeMillis();
        
        Set<UUID> uniquePlayers = new HashSet<>();
        long totalPlaytime = 0;
        int totalSessions = 0;
        Map<Integer, Integer> hourlyActivity = new HashMap<>();
        
        // Analyze sessions in date range
        for (Map.Entry<String, List<PlayerSession>> entry : historicalSessions.entrySet()) {
            for (PlayerSession session : entry.getValue()) {
                if (session.joinTime >= startTime && session.joinTime <= endTime) {
                    uniquePlayers.add(session.playerId);
                    totalPlaytime += session.duration;
                    totalSessions++;
                    
                    // Track hourly activity
                    int hour = getHourOfDay(session.joinTime);
                    hourlyActivity.merge(hour, 1, Integer::sum);
                }
            }
        }
        
        report.uniquePlayers = uniquePlayers.size();
        report.totalSessions = totalSessions;
        report.totalPlaytime = totalPlaytime;
        report.averageSessionDuration = totalSessions > 0 ? totalPlaytime / totalSessions : 0;
        report.hourlyActivity = hourlyActivity;
        report.peakHour = getPeakHour(hourlyActivity);
        
        return report;
    }
    
    /**
     * Get retention statistics
     */
    public RetentionReport getRetentionReport(int days) {
        RetentionReport report = new RetentionReport();
        report.days = days;
        report.generatedAt = System.currentTimeMillis();
        
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (days * 24L * 60 * 60 * 1000);
        
        // Track unique players per day
        Map<String, Set<UUID>> playersByDay = new HashMap<>();
        
        for (Map.Entry<String, List<PlayerSession>> entry : historicalSessions.entrySet()) {
            long dayTime = parseDateKey(entry.getKey());
            if (dayTime >= startTime && dayTime <= endTime) {
                Set<UUID> dayPlayers = new HashSet<>();
                for (PlayerSession session : entry.getValue()) {
                    dayPlayers.add(session.playerId);
                }
                playersByDay.put(entry.getKey(), dayPlayers);
            }
        }
        
        // Calculate retention
        if (!playersByDay.isEmpty()) {
            List<String> sortedDays = new ArrayList<>(playersByDay.keySet());
            Collections.sort(sortedDays);
            
            if (sortedDays.size() > 1) {
                Set<UUID> firstDayPlayers = playersByDay.get(sortedDays.get(0));
                Set<UUID> lastDayPlayers = playersByDay.get(sortedDays.get(sortedDays.size() - 1));
                
                Set<UUID> retained = new HashSet<>(firstDayPlayers);
                retained.retainAll(lastDayPlayers);
                
                report.totalPlayersStart = firstDayPlayers.size();
                report.totalPlayersEnd = lastDayPlayers.size();
                report.retainedPlayers = retained.size();
                report.retentionRate = firstDayPlayers.size() > 0 ? 
                    (retained.size() * 100.0 / firstDayPlayers.size()) : 0;
            }
        }
        
        report.dailyPlayerCounts = new HashMap<>();
        playersByDay.forEach((day, players) -> report.dailyPlayerCounts.put(day, players.size()));
        
        return report;
    }
    
    /**
     * Get daily statistics
     */
    public Map<String, DailyStats> getDailyStats(int days) {
        Map<String, DailyStats> result = new HashMap<>();
        long now = System.currentTimeMillis();
        
        for (int i = 0; i < days; i++) {
            long dayTime = now - (i * 24L * 60 * 60 * 1000);
            String dateKey = getDateKey(dayTime);
            DailyStats stats = dailyStats.get(dateKey);
            if (stats != null) {
                result.put(dateKey, stats);
            }
        }
        
        return result;
    }
    
    /**
     * Load historical data from disk
     */
    private void loadHistoricalData() {
        try {
            // Load historical sessions
            if (Files.exists(SESSIONS_FILE)) {
                String json = Files.readString(SESSIONS_FILE, StandardCharsets.UTF_8);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                
                if (data.has("sessions") && data.get("sessions").isJsonObject()) {
                    JsonObject sessionsObj = data.getAsJsonObject("sessions");
                    for (String dateKey : sessionsObj.keySet()) {
                        List<PlayerSession> sessions = GSON.fromJson(
                            sessionsObj.get(dateKey),
                            new com.google.gson.reflect.TypeToken<List<PlayerSession>>(){}.getType()
                        );
                        if (sessions != null && !sessions.isEmpty()) {
                            historicalSessions.put(dateKey, new ArrayList<>(sessions));
                        }
                    }
                    LOGGER.info("Loaded {} days of historical session data", historicalSessions.size());
                } else {
                    LOGGER.debug("No historical sessions found in file");
                }
            }
            
            // Load daily statistics
            if (Files.exists(DAILY_STATS_FILE)) {
                String json = Files.readString(DAILY_STATS_FILE, StandardCharsets.UTF_8);
                JsonObject data = JsonParser.parseString(json).getAsJsonObject();
                
                if (data.has("stats") && data.get("stats").isJsonObject()) {
                    JsonObject statsObj = data.getAsJsonObject("stats");
                    for (String dateKey : statsObj.keySet()) {
                        JsonObject statData = statsObj.getAsJsonObject(dateKey);
                        DailyStats stats = new DailyStats(dateKey);
                        
                        // Restore daily stats fields
                        if (statData.has("uniquePlayers") && statData.get("uniquePlayers").isJsonArray()) {
                            statData.getAsJsonArray("uniquePlayers").forEach(element -> {
                                try {
                                    stats.uniquePlayers.add(UUID.fromString(element.getAsString()));
                                } catch (IllegalArgumentException e) {
                                    LOGGER.warn("Invalid UUID in daily stats: {}", element.getAsString());
                                }
                            });
                        }
                        
                        if (statData.has("totalJoins")) {
                            stats.totalJoins = statData.get("totalJoins").getAsInt();
                        }
                        if (statData.has("totalSessions")) {
                            stats.totalSessions = statData.get("totalSessions").getAsInt();
                        }
                        if (statData.has("totalPlaytime")) {
                            stats.totalPlaytime = statData.get("totalPlaytime").getAsLong();
                        }
                        if (statData.has("averageSessionDuration")) {
                            stats.averageSessionDuration = statData.get("averageSessionDuration").getAsLong();
                        }
                        if (statData.has("peakOnline")) {
                            stats.peakOnline = statData.get("peakOnline").getAsInt();
                        }
                        
                        dailyStats.put(dateKey, stats);
                    }
                    LOGGER.info("Loaded {} days of daily statistics", dailyStats.size());
                } else {
                    LOGGER.debug("No daily statistics found in file");
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load historical data", e);
        }
    }
    
    /**
     * Save historical data to disk
     */
    private void saveHistoricalData() {
        try {
            // Save sessions (limit to last 30 days)
            JsonObject sessionsData = new JsonObject();
            sessionsData.addProperty("lastUpdated", System.currentTimeMillis());
            
            // Add historical sessions
            JsonObject sessionsObj = new JsonObject();
            long cutoffTime = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000); // 30 days ago
            
            for (Map.Entry<String, List<PlayerSession>> entry : historicalSessions.entrySet()) {
                String dateKey = entry.getKey();
                long dateTime = parseDateKey(dateKey);
                
                // Only save sessions from last 30 days
                if (dateTime >= cutoffTime) {
                    sessionsObj.add(dateKey, GSON.toJsonTree(entry.getValue()));
                }
            }
            sessionsData.add("sessions", sessionsObj);
            
            Files.writeString(SESSIONS_FILE, GSON.toJson(sessionsData), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // Save daily stats
            JsonObject statsData = new JsonObject();
            statsData.addProperty("lastUpdated", System.currentTimeMillis());
            
            // Add daily statistics
            JsonObject statsObj = new JsonObject();
            for (Map.Entry<String, DailyStats> entry : dailyStats.entrySet()) {
                String dateKey = entry.getKey();
                long dateTime = parseDateKey(dateKey);
                
                // Only save stats from last 30 days
                if (dateTime >= cutoffTime) {
                    DailyStats stats = entry.getValue();
                    JsonObject statData = new JsonObject();
                    
                    statData.addProperty("date", stats.date);
                    statData.add("uniquePlayers", GSON.toJsonTree(stats.uniquePlayers.stream()
                        .map(UUID::toString)
                        .toList()));
                    statData.addProperty("totalJoins", stats.totalJoins);
                    statData.addProperty("totalSessions", stats.totalSessions);
                    statData.addProperty("totalPlaytime", stats.totalPlaytime);
                    statData.addProperty("averageSessionDuration", stats.averageSessionDuration);
                    statData.addProperty("peakOnline", stats.peakOnline);
                    
                    statsObj.add(dateKey, statData);
                }
            }
            statsData.add("stats", statsObj);
            
            Files.writeString(DAILY_STATS_FILE, GSON.toJson(statsData), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
                
        } catch (Exception e) {
            LOGGER.error("Failed to save historical data", e);
        }
    }
    
    /**
     * Get date key from timestamp
     */
    private String getDateKey(long timestamp) {
        return LocalDate.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toString();
    }
    
    /**
     * Parse date key to timestamp
     */
    private long parseDateKey(String dateKey) {
        try {
            return LocalDate.parse(dateKey)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        } catch (Exception e) {
            return 0;
        }
    }
    
    /**
     * Get hour of day from timestamp (0-23)
     */
    private int getHourOfDay(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .getHour();
    }
    
    /**
     * Get peak hour from hourly activity map
     */
    private int getPeakHour(Map<Integer, Integer> hourlyActivity) {
        return hourlyActivity.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(0);
    }
    
    /**
     * Player session data
     */
    public static class PlayerSession {
        public UUID playerId;
        public String playerName;
        public long joinTime;
        public long leaveTime;
        public long lastActivity;
        public long duration;
    }
    
    /**
     * Daily statistics
     */
    public static class DailyStats {
        public String date;
        public Set<UUID> uniquePlayers = new HashSet<>();
        public int totalJoins = 0;
        public int totalSessions = 0;
        public long totalPlaytime = 0;
        public long averageSessionDuration = 0;
        public int peakOnline = 0;
        
        public DailyStats(String date) {
            this.date = date;
        }
    }
    
    /**
     * Analytics report
     */
    public static class AnalyticsReport {
        public long startTime;
        public long endTime;
        public long generatedAt;
        public int uniquePlayers;
        public int totalSessions;
        public long totalPlaytime;
        public long averageSessionDuration;
        public Map<Integer, Integer> hourlyActivity;
        public int peakHour;
    }
    
    /**
     * Retention report
     */
    public static class RetentionReport {
        public int days;
        public long generatedAt;
        public int totalPlayersStart;
        public int totalPlayersEnd;
        public int retainedPlayers;
        public double retentionRate;
        public Map<String, Integer> dailyPlayerCounts;
    }
}
