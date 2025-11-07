package com.zerog.neoessentials.webdashboard.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.world.item.ItemStack;
import com.zerog.neoessentials.teleportation.HomeManager;
import com.zerog.neoessentials.teleportation.TeleportLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.List;
import java.util.Map;
import java.io.IOException;

/**
 * Player Data Collector
 * Collects all player-related data for the Dashboard API
 * 
 * Endpoints served:
 * - Player Profiles (name, UUID, join date, play time)
 * - Player Statistics (kills, deaths, blocks broken/placed)
 * - Player Achievements (Minecraft achievements)
 * - Player Inventory (items, armor, hotbar)
 * - Player Online Status (online/offline, last seen)
 * - Player Health Status (health, food, armor points)
 * - Player XP (levels, points, progress)
 * - Player World Location (coordinates, dimension)
 * - Player Homes and Warps (personal teleport locations)
 */
public class PlayerDataCollector {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerDataCollector.class);
    private final MinecraftServer server;
    
    public PlayerDataCollector(MinecraftServer server) {
        this.server = server;
    }
    
    /**
     * Get complete player profile
     * Endpoint: GET /api/players/{uuid}/profile
     */
    public JsonObject getPlayerProfile(UUID playerUuid) {
        JsonObject profile = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            // Online player
            profile.addProperty("uuid", playerUuid.toString());
            profile.addProperty("username", player.getName().getString());
            profile.addProperty("displayName", player.getDisplayName().getString());
            profile.addProperty("online", true);
            profile.addProperty("gameMode", player.gameMode.getGameModeForPlayer().getName());
            profile.addProperty("operator", player.hasPermissions(4));
            
            // FUTURE: Add persistent player data from database
            // profile.addProperty("firstJoin", getFirstJoinTime(playerUuid));
            // profile.addProperty("lastJoin", getLastJoinTime(playerUuid));
            // profile.addProperty("totalPlayTime", getTotalPlayTime(playerUuid));
        } else {
            // Offline player - get stored data
            profile.addProperty("uuid", playerUuid.toString());
            profile.addProperty("online", false);
            
            // Try to get username from cache
            net.minecraft.server.players.GameProfileCache cache = server.getProfileCache();
            if (cache != null) {
                java.util.Optional<com.mojang.authlib.GameProfile> profileOpt = cache.get(playerUuid);
                if (profileOpt.isPresent()) {
                    profile.addProperty("username", profileOpt.get().getName());
                }
            }
            
            // Try to load game mode from player data file
            CompoundTag playerData = loadOfflinePlayerData(playerUuid);
            if (playerData != null) {
                int gameModeId = playerData.getInt("playerGameType");
                String gameMode = switch (gameModeId) {
                    case 0 -> "survival";
                    case 1 -> "creative";
                    case 2 -> "adventure";
                    case 3 -> "spectator";
                    default -> "unknown";
                };
                profile.addProperty("gameMode", gameMode);
            }
        }
        
        return profile;
    }
    
    /**
     * Get player statistics
     * Endpoint: GET /api/players/{uuid}/statistics
     */
    public JsonObject getPlayerStatistics(UUID playerUuid) {
        JsonObject stats = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            // Combat stats
            JsonObject combat = new JsonObject();
            // FUTURE: Implement comprehensive stat tracking system using event listeners
            // combat.addProperty("kills", getPlayerKills(playerUuid));
            // combat.addProperty("deaths", getPlayerDeaths(playerUuid));
            // combat.addProperty("damageDealt", getDamageDealt(playerUuid));
            // combat.addProperty("damageTaken", getDamageTaken(playerUuid));
            stats.add("combat", combat);
            
            // Building stats
            JsonObject building = new JsonObject();
            // building.addProperty("blocksBroken", getBlocksBroken(playerUuid));
            // building.addProperty("blocksPlaced", getBlocksPlaced(playerUuid));
            stats.add("building", building);
            
            // Movement stats
            JsonObject movement = new JsonObject();
            // movement.addProperty("distanceWalked", getDistanceWalked(playerUuid));
            // movement.addProperty("distanceFlown", getDistanceFlown(playerUuid));
            stats.add("movement", movement);
        }
        
        return stats;
    }
    
    /**
     * Get player achievements
     * Endpoint: GET /api/players/{uuid}/achievements
     */
    public JsonObject getPlayerAchievements(UUID playerUuid) {
        JsonObject achievements = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            JsonArray completed = new JsonArray();
            JsonArray inProgress = new JsonArray();
            
            // Access player advancement progress
            net.minecraft.server.PlayerAdvancements playerAdvancements = player.getAdvancements();
            
            // Get all advancements from the server
            server.getAdvancements().getAllAdvancements().forEach(advancement -> {
                // Only include advancements with display (visible advancements)
                if (advancement.value().display().isPresent()) {
                    net.minecraft.advancements.AdvancementProgress progress = playerAdvancements.getOrStartProgress(advancement);
                    net.minecraft.advancements.DisplayInfo display = advancement.value().display().get();
                    
                    JsonObject adv = new JsonObject();
                    adv.addProperty("id", advancement.id().toString());
                    adv.addProperty("title", display.getTitle().getString());
                    adv.addProperty("description", display.getDescription().getString());
                    adv.addProperty("isDone", progress.isDone());
                    
                    if (progress.isDone()) {
                        completed.add(adv);
                    } else if (progress.hasProgress()) {
                        adv.addProperty("progress", progress.getPercent());
                        inProgress.add(adv);
                    }
                }
            });
            
            achievements.add("completed", completed);
            achievements.add("inProgress", inProgress);
            achievements.addProperty("total", completed.size() + inProgress.size());
            achievements.addProperty("completedCount", completed.size());
        } else {
            achievements.add("completed", new JsonArray());
            achievements.add("inProgress", new JsonArray());
            achievements.addProperty("total", 0);
            achievements.addProperty("completedCount", 0);
            achievements.addProperty("error", "Player not found or offline");
        }
        
        return achievements;
    }
    
    /**
     * Get player inventory
     * Endpoint: GET /api/players/{uuid}/inventory
     */
    public JsonObject getPlayerInventory(UUID playerUuid) {
        JsonObject inventory = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            // Online player - get from live data
            JsonArray mainInventory = new JsonArray();
            JsonArray armor = new JsonArray();
            JsonArray offhand = new JsonArray();
            
            // Main inventory
            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                JsonObject item = serializeItemStack(player.getInventory().getItem(i));
                item.addProperty("slot", i);
                mainInventory.add(item);
            }
            
            // Armor
            player.getInventory().armor.forEach(itemStack -> {
                armor.add(serializeItemStack(itemStack));
            });
            
            // Offhand
            player.getInventory().offhand.forEach(itemStack -> {
                offhand.add(serializeItemStack(itemStack));
            });
            
            inventory.add("main", mainInventory);
            inventory.add("armor", armor);
            inventory.add("offhand", offhand);
        } else {
            // Offline player - read from player data file
            CompoundTag playerData = loadOfflinePlayerData(playerUuid);
            if (playerData != null) {
                inventory = parseInventoryFromNBT(playerData);
            }
        }
        
        return inventory;
    }
    
    /**
     * Load offline player data from NBT file
     */
    private CompoundTag loadOfflinePlayerData(UUID playerUuid) {
        try {
            java.nio.file.Path playerDataFile = server.getServerDirectory().resolve("world/playerdata/" + playerUuid.toString() + ".dat");
            
            if (java.nio.file.Files.exists(playerDataFile)) {
                return NbtIo.readCompressed(playerDataFile, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            }
        } catch (IOException e) {
            LOGGER.error("Failed to load offline player data for UUID: {}", playerUuid, e);
        }
        return null;
    }
    
    /**
     * Parse inventory data from NBT
     */
    private JsonObject parseInventoryFromNBT(CompoundTag playerData) {
        JsonObject inventory = new JsonObject();
        JsonArray mainInventory = new JsonArray();
        JsonArray armor = new JsonArray();
        JsonArray offhand = new JsonArray();
        
        // Parse main inventory
        if (playerData.contains("Inventory", 9)) { // 9 = ListTag
            ListTag invList = playerData.getList("Inventory", 10); // 10 = CompoundTag
            for (int i = 0; i < invList.size(); i++) {
                CompoundTag itemTag = invList.getCompound(i);
                byte slot = itemTag.getByte("Slot");
                
                ItemStack itemStack = ItemStack.parseOptional(server.registryAccess(), itemTag);
                if (itemStack == null) {
                    itemStack = ItemStack.EMPTY;
                }
                JsonObject item = serializeItemStack(itemStack);
                item.addProperty("slot", slot);
                
                // Determine which list to add to based on slot
                if (slot >= 100 && slot <= 103) {
                    // Armor slots (100-103)
                    armor.add(item);
                } else if (slot == -106) {
                    // Offhand slot
                    offhand.add(item);
                } else {
                    // Main inventory
                    mainInventory.add(item);
                }
            }
        }
        
        inventory.add("main", mainInventory);
        inventory.add("armor", armor);
        inventory.add("offhand", offhand);
        
        return inventory;
    }
    
    /**
     * Get player online status
     * Endpoint: GET /api/players/{uuid}/status
     */
    public JsonObject getPlayerStatus(UUID playerUuid) {
        JsonObject status = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        status.addProperty("online", player != null);
        if (player != null) {
            status.addProperty("username", player.getName().getString());
            status.addProperty("ping", player.connection.latency());
            
            // AFK status integration
            com.zerog.neoessentials.chat.AfkManager afkManager = com.zerog.neoessentials.chat.AfkManager.getInstance();
            boolean isAfk = afkManager.isAfk(playerUuid);
            status.addProperty("afk", isAfk);
            
            if (isAfk) {
                long afkDuration = afkManager.getAfkDuration(playerUuid);
                status.addProperty("afkDuration", afkDuration);
                status.addProperty("afkDurationSeconds", afkDuration / 1000);
                
                // Get AFK reason if available
                String afkReason = afkManager.getAfkReason(playerUuid);
                if (afkReason != null) {
                    status.addProperty("afkReason", afkReason);
                }
            }
        } else {
            // FUTURE: Retrieve last seen time from persistent storage
            // status.addProperty("lastSeen", getLastSeenTime(playerUuid));
        }
        
        return status;
    }
    
    /**
     * Get player health status
     * Endpoint: GET /api/players/{uuid}/health
     */
    public JsonObject getPlayerHealth(UUID playerUuid) {
        JsonObject health = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            health.addProperty("health", player.getHealth());
            health.addProperty("maxHealth", player.getMaxHealth());
            health.addProperty("healthPercent", (player.getHealth() / player.getMaxHealth()) * 100);
            health.addProperty("foodLevel", player.getFoodData().getFoodLevel());
            health.addProperty("saturation", player.getFoodData().getSaturationLevel());
            health.addProperty("exhaustion", player.getFoodData().getExhaustionLevel());
            health.addProperty("armor", player.getArmorValue());
            health.addProperty("absorptionAmount", player.getAbsorptionAmount());
            health.addProperty("air", player.getAirSupply());
            health.addProperty("maxAir", player.getMaxAirSupply());
        }
        
        return health;
    }
    
    /**
     * Get player XP information
     * Endpoint: GET /api/players/{uuid}/xp
     */
    public JsonObject getPlayerXP(UUID playerUuid) {
        JsonObject xp = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            xp.addProperty("level", player.experienceLevel);
            xp.addProperty("progress", player.experienceProgress);
            xp.addProperty("totalXp", player.totalExperience);
            xp.addProperty("xpToNextLevel", player.getXpNeededForNextLevel());
        }
        
        return xp;
    }
    
    /**
     * Get player location
     * Endpoint: GET /api/players/{uuid}/location
     */
    public JsonObject getPlayerLocation(UUID playerUuid) {
        JsonObject location = new JsonObject();
        ServerPlayer player = server.getPlayerList().getPlayer(playerUuid);
        
        if (player != null) {
            BlockPos pos = player.blockPosition();
            location.addProperty("x", pos.getX());
            location.addProperty("y", pos.getY());
            location.addProperty("z", pos.getZ());
            location.addProperty("exactX", player.getX());
            location.addProperty("exactY", player.getY());
            location.addProperty("exactZ", player.getZ());
            location.addProperty("yaw", player.getYRot());
            location.addProperty("pitch", player.getXRot());
            location.addProperty("dimension", getDimensionName(player.level()));
            location.addProperty("world", player.level().dimension().location().toString());
            location.addProperty("biome", getBiomeName(player.level(), pos));
        }
        
        return location;
    }
    
    /**
     * Get player homes and warps
     * Endpoint: GET /api/player/homes/{username}
     */
    public JsonObject getPlayerHomes(String username) {
        JsonObject response = new JsonObject();
        JsonArray homesList = new JsonArray();
        
        ServerPlayer player = server.getPlayerList().getPlayerByName(username);
        
        if (player == null) {
            response.addProperty("error", "Player not found or offline");
            response.add("homes", homesList);
            response.addProperty("count", 0);
            return response;
        }
        
        // Get player homes from HomeManager
        HomeManager homeManager = HomeManager.getInstance();
        Map<String, TeleportLocation> playerHomes = homeManager.getPlayerHomes(player);
        
        // Convert each home to JSON
        for (Map.Entry<String, TeleportLocation> entry : playerHomes.entrySet()) {
            JsonObject homeObj = new JsonObject();
            TeleportLocation location = entry.getValue();
            
            homeObj.addProperty("name", entry.getKey());
            homeObj.addProperty("x", location.getX());
            homeObj.addProperty("y", location.getY());
            homeObj.addProperty("z", location.getZ());
            homeObj.addProperty("yaw", location.getYaw());
            homeObj.addProperty("pitch", location.getPitch());
            homeObj.addProperty("dimension", location.getWorldName());
            homeObj.addProperty("createdBy", location.getCreatedBy());
            homeObj.addProperty("timestamp", location.getTimestamp());
            
            homesList.add(homeObj);
        }
        
        response.add("homes", homesList);
        response.addProperty("count", homesList.size());
        response.addProperty("maxHomes", homeManager.getMaxHomesForPlayer(player));
        
        return response;
    }
    
    /**
     * Get list of all online players and offline players
     * Endpoint: GET /api/players/online
     */
    public JsonObject getOnlinePlayers() {
        JsonObject response = new JsonObject();
        JsonArray onlinePlayers = new JsonArray();
        JsonArray offlinePlayers = new JsonArray();
        
        LOGGER.info("=== Starting getOnlinePlayers data collection ===");
        
        // Get online players
        List<ServerPlayer> online = server.getPlayerList().getPlayers();
        java.util.Set<String> onlineUsernames = new java.util.HashSet<>();
        
        online.forEach(player -> {
            JsonObject playerObj = new JsonObject();
            playerObj.addProperty("uuid", player.getUUID().toString());
            playerObj.addProperty("username", player.getName().getString());
            playerObj.addProperty("displayName", player.getDisplayName().getString());
            playerObj.addProperty("ping", player.connection.latency());
            playerObj.addProperty("gameMode", player.gameMode.getGameModeForPlayer().getName());
            playerObj.addProperty("health", player.getHealth());
            playerObj.addProperty("foodLevel", player.getFoodData().getFoodLevel());
            playerObj.addProperty("experienceLevel", player.experienceLevel);
            onlinePlayers.add(playerObj);
            onlineUsernames.add(player.getName().getString());
        });
        
        LOGGER.info("Found {} online players", onlinePlayers.size());
        
        // Get offline players from playerdata directory
        try {
            java.nio.file.Path playerDataDir = server.getServerDirectory().resolve("world/playerdata");
            LOGGER.info("Looking for offline players in: {}", playerDataDir);
            
            if (java.nio.file.Files.exists(playerDataDir)) {
                net.minecraft.server.players.GameProfileCache cache = server.getProfileCache();
                
                LOGGER.info("Player data directory exists, cache available: {}", (cache != null));
                
                // Limit to last 50 offline players to avoid performance issues
                int maxOffline = 50;
                
                java.nio.file.Files.list(playerDataDir)
                    .filter(path -> path.toString().endsWith(".dat"))
                    .limit(maxOffline)
                    .forEach(path -> {
                        try {
                            String fileName = path.getFileName().toString();
                            String uuidStr = fileName.replace(".dat", "");
                            UUID uuid = UUID.fromString(uuidStr);
                            
                            // Check if player is not online
                            if (server.getPlayerList().getPlayer(uuid) == null) {
                                // Get profile from cache
                                if (cache != null) {
                                    java.util.Optional<com.mojang.authlib.GameProfile> profileOpt = cache.get(uuid);
                                    if (profileOpt.isPresent()) {
                                        com.mojang.authlib.GameProfile profile = profileOpt.get();
                                        if (profile.getName() != null && !onlineUsernames.contains(profile.getName())) {
                                            JsonObject playerObj = new JsonObject();
                                            playerObj.addProperty("uuid", uuid.toString());
                                            playerObj.addProperty("username", profile.getName());
                                            
                                            // Get last modified time of player data file
                                            try {
                                                long lastModified = java.nio.file.Files.getLastModifiedTime(path).toMillis();
                                                playerObj.addProperty("lastSeen", formatLastSeenTimestamp(lastModified));
                                            } catch (Exception e) {
                                                playerObj.addProperty("lastSeen", "Unknown");
                                            }
                                            
                                            offlinePlayers.add(playerObj);
                                        }
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Skip invalid files
                        }
                    });
                    
                LOGGER.info("Found {} offline players", offlinePlayers.size());
            } else {
                LOGGER.warn("Player data directory does not exist: {}", playerDataDir);
            }
        } catch (Exception e) {
            // If we can't get offline players, just continue with online players only
            LOGGER.warn("Could not load offline players from playerdata: {}", e.getMessage());
        }
        
        response.add("players", onlinePlayers);
        response.add("offlinePlayers", offlinePlayers);
        response.addProperty("count", onlinePlayers.size());
        response.addProperty("offlineCount", offlinePlayers.size());
        response.addProperty("max", server.getMaxPlayers());
        
        LOGGER.info("=== Completed getOnlinePlayers: {} online, {} offline ===", 
            onlinePlayers.size(), offlinePlayers.size());
        
        return response;
    }
    
    private String formatLastSeenTimestamp(long timestamp) {
        long now = System.currentTimeMillis();
        long diff = now - timestamp;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) return days + " day" + (days > 1 ? "s" : "") + " ago";
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "") + " ago";
        if (minutes > 0) return minutes + " minute" + (minutes > 1 ? "s" : "") + " ago";
        return "Just now";
    }
    
    // Helper methods
    
    private JsonObject serializeItemStack(net.minecraft.world.item.ItemStack itemStack) {
        JsonObject item = new JsonObject();
        if (!itemStack.isEmpty()) {
            // Get the registry name (e.g., "minecraft:diamond_sword")
            String registryName = net.minecraft.core.registries.BuiltInRegistries.ITEM
                .getKey(itemStack.getItem()).toString();
            
            item.addProperty("id", registryName);
            item.addProperty("count", itemStack.getCount());
            item.addProperty("displayName", itemStack.getDisplayName().getString());
            
            // Add namespace and path separately for easier texture lookup
            String[] parts = registryName.split(":", 2);
            if (parts.length == 2) {
                item.addProperty("namespace", parts[0]);
                item.addProperty("path", parts[1]);
            }
            
            // FUTURE: Add NBT data serialization for enchantments, custom names, etc.
        }
        return item;
    }
    
    private String getDimensionName(Level level) {
        String dimensionKey = level.dimension().location().toString();
        return switch (dimensionKey) {
            case "minecraft:overworld" -> "Overworld";
            case "minecraft:the_nether" -> "Nether";
            case "minecraft:the_end" -> "End";
            default -> dimensionKey;
        };
    }
    
    private String getBiomeName(Level level, BlockPos pos) {
        try {
            var biomeKey = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.BIOME)
                .getKey(level.getBiome(pos).value());
            return biomeKey != null ? biomeKey.toString() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
