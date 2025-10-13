package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implements the /seen command - Shows when a player was last online
 * Tracks player login/logout times and total play time
 */
public class SeenCommand {
    
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Map<UUID, PlayerActivity> PLAYER_ACTIVITY = new ConcurrentHashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path SEEN_DATA_FILE = Paths.get("config", "neoessentials", "seen_data.json");
    
    /**
     * Internal class to track player activity
     */
    private static class PlayerActivity {
        String playerName;
        String lastSeen;
        String firstSeen;
        boolean isOnline;
        String lastLoginTime;
        String lastLogoutTime;
        long totalPlayTime; // in minutes
        
        PlayerActivity(String playerName) {
            this.playerName = playerName;
            this.isOnline = false;
            this.totalPlayTime = 0;
            String now = LocalDateTime.now().format(TIME_FORMAT);
            this.firstSeen = now;
            this.lastSeen = now;
        }
    }
    
    /**
     * Register the /seen command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("seen")) return;
        
        // Load seen data on registration
        loadSeenData();
        
        dispatcher.register(
            Commands.literal("seen")
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.seen");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        String playerName = StringArgumentType.getString(ctx, "player");
                        return showPlayerActivity(ctx.getSource(), playerName);
                    })
                )
                // /seen - Show usage
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.info("commands.neoessentials.seen.usage"));
                    return 0;
                })
        );
    }
    
    /**
     * Show player activity information
     */
    private static int showPlayerActivity(CommandSourceStack source, String playerName) {
        // Try to find player by name (case-insensitive)
        PlayerActivity foundActivity = null;
        UUID playerId = null;
        
        // First check online players
        ServerPlayer onlinePlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
        if (onlinePlayer != null) {
            playerId = onlinePlayer.getUUID();
            foundActivity = PLAYER_ACTIVITY.get(playerId);
            
            if (foundActivity == null) {
                // Player is online but not in our records yet
                foundActivity = new PlayerActivity(onlinePlayer.getName().getString());
                foundActivity.isOnline = true;
                foundActivity.lastLoginTime = LocalDateTime.now().format(TIME_FORMAT);
                PLAYER_ACTIVITY.put(playerId, foundActivity);
                saveSeenData();
            }
        } else {
            // Search through recorded players
            for (Map.Entry<UUID, PlayerActivity> entry : PLAYER_ACTIVITY.entrySet()) {
                if (entry.getValue().playerName.equalsIgnoreCase(playerName)) {
                    playerId = entry.getKey();
                    foundActivity = entry.getValue();
                    break;
                }
            }
        }
        
        if (foundActivity == null) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.seen.player_not_found", playerName));
            return 0;
        }
        
        // Make final for lambda usage
        final PlayerActivity activity = foundActivity;
        
        // Display player information
        String displayName = activity.playerName;
        
        if (activity.isOnline) {
            // Player is currently online
            source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.seen.online", displayName), false);
            
            if (activity.lastLoginTime != null) {
                String loginTime = activity.lastLoginTime;
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.login_time", loginTime), false);
            }
            
            // Show current location if we have access to the player
            if (onlinePlayer != null) {
                String world = onlinePlayer.level().toString();
                String coords = String.format("%.1f, %.1f, %.1f", 
                    onlinePlayer.getX(), onlinePlayer.getY(), onlinePlayer.getZ());
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.current_location", world, coords), false);
            }
        } else {
            // Player is offline
            String timeSince = getTimeSince(activity.lastSeen);
            String lastSeen = activity.lastSeen;
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.offline", displayName, timeSince), false);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.last_seen", lastSeen), false);
        }
        
        // Show additional information if available
        if (activity.firstSeen != null) {
            String firstSeen = activity.firstSeen;
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.first_seen", firstSeen), false);
        }
        
        if (activity.totalPlayTime > 0) {
            String playTimeStr = formatPlayTime(activity.totalPlayTime);
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.seen.play_time", playTimeStr), false);
        }
        
        return 1;
    }
    
    /**
     * Calculate human-readable time since a given timestamp
     */
    private static String getTimeSince(String timestamp) {
        try {
            LocalDateTime lastTime = LocalDateTime.parse(timestamp, TIME_FORMAT);
            LocalDateTime now = LocalDateTime.now();
            
            long days = ChronoUnit.DAYS.between(lastTime, now);
            long hours = ChronoUnit.HOURS.between(lastTime, now) % 24;
            long minutes = ChronoUnit.MINUTES.between(lastTime, now) % 60;
            
            if (days > 0) {
                return String.format("%d days, %d hours ago", days, hours);
            } else if (hours > 0) {
                return String.format("%d hours, %d minutes ago", hours, minutes);
            } else {
                return String.format("%d minutes ago", minutes);
            }
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * Format play time in human-readable format
     */
    private static String formatPlayTime(long minutes) {
        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;
        long days = hours / 24;
        long remainingHours = hours % 24;
        
        if (days > 0) {
            return String.format("%d days, %d hours, %d minutes", days, remainingHours, remainingMinutes);
        } else if (hours > 0) {
            return String.format("%d hours, %d minutes", hours, remainingMinutes);
        } else {
            return String.format("%d minutes", minutes);
        }
    }
    
    /**
     * Called when a player joins the server
     */
    public static void onPlayerJoin(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerActivity activity = PLAYER_ACTIVITY.computeIfAbsent(playerId, 
            k -> new PlayerActivity(player.getName().getString()));
        
        activity.isOnline = true;
        activity.lastLoginTime = LocalDateTime.now().format(TIME_FORMAT);
        
        // Update player name in case it changed
        activity.playerName = player.getName().getString();
        
        saveSeenData();
    }
    
    /**
     * Called when a player leaves the server
     */
    public static void onPlayerLeave(ServerPlayer player) {
        UUID playerId = player.getUUID();
        PlayerActivity activity = PLAYER_ACTIVITY.get(playerId);
        
        if (activity != null) {
            activity.isOnline = false;
            String logoutTime = LocalDateTime.now().format(TIME_FORMAT);
            activity.lastSeen = logoutTime;
            activity.lastLogoutTime = logoutTime;
            
            // Calculate session time if we have login time
            if (activity.lastLoginTime != null) {
                try {
                    LocalDateTime loginTime = LocalDateTime.parse(activity.lastLoginTime, TIME_FORMAT);
                    LocalDateTime logoutDateTime = LocalDateTime.parse(logoutTime, TIME_FORMAT);
                    long sessionMinutes = ChronoUnit.MINUTES.between(loginTime, logoutDateTime);
                    activity.totalPlayTime += sessionMinutes;
                } catch (Exception e) {
                    // Unable to calculate session time
                }
            }
            
            saveSeenData();
        }
    }
    
    /**
     * Load seen data from file
     */
    private static void loadSeenData() {
        try {
            if (!Files.exists(SEEN_DATA_FILE)) {
                return;
            }
            
            String content = Files.readString(SEEN_DATA_FILE);
            JsonObject data = GSON.fromJson(content, JsonObject.class);
            
            for (String uuidStr : data.keySet()) {
                try {
                    UUID uuid = UUID.fromString(uuidStr);
                    JsonObject activityObj = data.getAsJsonObject(uuidStr);
                    
                    PlayerActivity activity = new PlayerActivity(
                        activityObj.get("playerName").getAsString()
                    );
                    
                    if (activityObj.has("lastSeen")) {
                        activity.lastSeen = activityObj.get("lastSeen").getAsString();
                    }
                    if (activityObj.has("firstSeen")) {
                        activity.firstSeen = activityObj.get("firstSeen").getAsString();
                    }
                    if (activityObj.has("isOnline")) {
                        activity.isOnline = activityObj.get("isOnline").getAsBoolean();
                    }
                    if (activityObj.has("lastLoginTime")) {
                        activity.lastLoginTime = activityObj.get("lastLoginTime").getAsString();
                    }
                    if (activityObj.has("lastLogoutTime")) {
                        activity.lastLogoutTime = activityObj.get("lastLogoutTime").getAsString();
                    }
                    if (activityObj.has("totalPlayTime")) {
                        activity.totalPlayTime = activityObj.get("totalPlayTime").getAsLong();
                    }
                    
                    PLAYER_ACTIVITY.put(uuid, activity);
                } catch (Exception e) {
                    System.err.println("Failed to load player activity for UUID: " + uuidStr);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Failed to load seen data: " + e.getMessage());
        }
    }
    
    /**
     * Save seen data to file
     */
    private static void saveSeenData() {
        try {
            JsonObject data = new JsonObject();
            
            for (Map.Entry<UUID, PlayerActivity> entry : PLAYER_ACTIVITY.entrySet()) {
                JsonObject activityObj = new JsonObject();
                PlayerActivity activity = entry.getValue();
                
                activityObj.addProperty("playerName", activity.playerName);
                activityObj.addProperty("lastSeen", activity.lastSeen);
                activityObj.addProperty("firstSeen", activity.firstSeen);
                activityObj.addProperty("isOnline", activity.isOnline);
                activityObj.addProperty("lastLoginTime", activity.lastLoginTime);
                activityObj.addProperty("lastLogoutTime", activity.lastLogoutTime);
                activityObj.addProperty("totalPlayTime", activity.totalPlayTime);
                
                data.add(entry.getKey().toString(), activityObj);
            }
            
            Files.createDirectories(SEEN_DATA_FILE.getParent());
            Files.writeString(SEEN_DATA_FILE, GSON.toJson(data));
            
        } catch (Exception e) {
            System.err.println("Failed to save seen data: " + e.getMessage());
        }
    }
}