package com.zerog.neoessentials.ui.tab.features;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.ui.tab.TabManager;
import com.zerog.neoessentials.ui.tab.TabPlayerData;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the player list in the tab menu
 * Handles per-world/global playerlist, name formatting, sorting, and ping spoofing
 */
public class PlayerListFeature extends AbstractFeature {
    // Sort types
    public enum SortType {
        NAME, GROUP, PLAYTIME
    }
    
    // Cache of last sent display names
    private final Map<UUID, String> lastDisplayNames = new ConcurrentHashMap<>();
    
    // Cache of last sent ping values
    private final Map<UUID, Integer> lastPingValues = new ConcurrentHashMap<>();
    
    // Configuration
    private boolean enabled = false;
    private boolean perWorldPlayerList = false;
    private boolean enableSorting = true;
    private SortType sortType = SortType.GROUP;
    private boolean enablePingSpoof = false;
    private boolean spectatorFix = true;
    private boolean playerlistObjective = false;
    
    // Group prefixes and suffixes for tablist
    private Map<String, String> groupPrefixes = new HashMap<>();
    private Map<String, String> groupSuffixes = new HashMap<>();
    
    /**
     * Creates a new player list feature
     * 
     * @param tabManager The tab manager
     */
    public PlayerListFeature(TabManager tabManager) {
        super(tabManager);
    }
    
    @Override
    public void initialize() {
        NeoEssentials.LOGGER.info("Initializing player list feature");
    }
    
    @Override
    public void loadConfig() {
        // TODO: Load from config
        // In a real implementation, you'd load these from TablistTomlConfig
        enabled = true;
        perWorldPlayerList = false; // false = global player list, true = per-world
        enableSorting = true;
        sortType = SortType.GROUP;
        enablePingSpoof = false;
        spectatorFix = true;
        playerlistObjective = false;
        
        // Example group prefixes and suffixes for tablist
        Map<String, String> prefixes = new HashMap<>();
        prefixes.put("admin", "&c[A] ");
        prefixes.put("mod", "&2[M] ");
        prefixes.put("vip", "&e[V] ");
        prefixes.put("default", "&7");
        
        Map<String, String> suffixes = new HashMap<>();
        suffixes.put("admin", " &7| &c%ping%ms");
        suffixes.put("mod", " &7| &2%ping%ms");
        suffixes.put("vip", " &7| &e%ping%ms");
        suffixes.put("default", " &7| &7%ping%ms");
        
        this.groupPrefixes = prefixes;
        this.groupSuffixes = suffixes;
    }
    
    @Override
    public void update() {
        if (!isEnabled() || server == null) return;
        
        // Get all online players
        List<ServerPlayer> allPlayers = tabManager.getOnlinePlayers();
        if (allPlayers.isEmpty()) return;
        
        // Group players by world if per-world playerlist is enabled
        Map<String, List<ServerPlayer>> worldPlayers = new HashMap<>();
        
        if (perWorldPlayerList) {
            // Group players by world
            for (ServerPlayer player : allPlayers) {
                String worldKey = player.level().dimension().location().toString();
                worldPlayers.computeIfAbsent(worldKey, k -> new ArrayList<>()).add(player);
            }
        } else {
            // Global playerlist - all players in one group
            worldPlayers.put("global", allPlayers);
        }
        
        // Process each world group
        for (Map.Entry<String, List<ServerPlayer>> entry : worldPlayers.entrySet()) {
            String worldKey = entry.getKey();
            List<ServerPlayer> worldPlayerList = entry.getValue();
            
            // Sort players if enabled
            if (enableSorting) {
                sortPlayers(worldPlayerList);
            }
            
            // Update each player's view of others in their world
            for (ServerPlayer viewer : worldPlayerList) {
                updatePlayerListForViewer(viewer, worldPlayerList, worldKey);
            }
        }
    }
    
    /**
     * Updates the player list for a specific viewer
     * 
     * @param viewer The player viewing the list
     * @param visiblePlayers The players visible to this viewer
     * @param worldKey The world key
     */
    private void updatePlayerListForViewer(ServerPlayer viewer, List<ServerPlayer> visiblePlayers, String worldKey) {
        executeWithErrorLogging(() -> {
            TabPlayerData viewerData = tabManager.getPlayerData(viewer);
            if (viewerData == null) return;
            
            // Prepare packets for adding players to the viewer's player list
            ClientboundPlayerInfoUpdatePacket addPacket = null;
            List<ClientboundPlayerInfoUpdatePacket.Entry> addEntries = new ArrayList<>();
            
            for (ServerPlayer player : visiblePlayers) {
                TabPlayerData playerData = tabManager.getPlayerData(player);
                if (playerData == null) continue;
                
                // Skip vanished players unless viewer has permission to see them
                if (playerData.isVanished() && !viewerData.getGroup().equals("admin")) {
                    continue;
                }
                
                // Get player's group
                String group = playerData.getGroup();
                if (group == null || group.isEmpty()) {
                    group = "default";
                }
                
                // Get prefix for this player
                String rawPrefix = groupPrefixes.getOrDefault(group, "");
                String processedPrefix = tabManager.getPlaceholderManager().replacePlaceholders(rawPrefix, player);
                
                // Get suffix for this player
                String rawSuffix = groupSuffixes.getOrDefault(group, "");
                String processedSuffix = tabManager.getPlaceholderManager().replacePlaceholders(rawSuffix, player);
                
                // Combine into display name
                String displayName = processedPrefix + player.getScoreboardName() + processedSuffix;
                
                // Check if display name has changed
                String lastDisplayName = lastDisplayNames.getOrDefault(player.getUUID(), "");
                if (!displayName.equals(lastDisplayName)) {
                    // Update player's display name in tablist
                    Component nameComponent = Component.literal(displayName);
                      // Create entry for the update packet                    // Convert ServerPlayerGameMode to GameType
                    net.minecraft.world.level.GameType gameType = net.minecraft.world.level.GameType.DEFAULT_MODE;
                    try {
                        // Get the gameType from the player's gameMode using reflection
                        java.lang.reflect.Method getGameModeMethod = 
                            player.gameMode.getClass().getMethod("getGameMode");
                        gameType = (net.minecraft.world.level.GameType) getGameModeMethod.invoke(player.gameMode);
                    } catch (Exception e) {
                        NeoEssentials.LOGGER.warn("Could not get player game mode, using default");
                    }
                    
                    ClientboundPlayerInfoUpdatePacket.Entry entry = new ClientboundPlayerInfoUpdatePacket.Entry(
                        player.getUUID(),
                        player.getGameProfile(),
                        true, // Listed in tab
                        playerData.getPing(), // Use ping from TabPlayerData
                        gameType, // Game mode - converted to GameType
                        nameComponent, // Display name
                        null // No profile text component
                    );
                    
                    addEntries.add(entry);
                    
                    // Update cache
                    lastDisplayNames.put(player.getUUID(), displayName);
                    
                    // Update player data
                    playerData.setTablistPrefix(processedPrefix);
                    playerData.setTablistSuffix(processedSuffix);
                }
                
                // Handle ping spoofing if enabled
                if (enablePingSpoof) {
                    // TODO: Implement ping spoofing logic
                    // This would typically set a fixed or custom ping value per group
                }
            }
              // Send the packet if any entries were added
            if (!addEntries.isEmpty()) {
                // Create EnumSet with the UPDATE_DISPLAY_NAME action
                java.util.EnumSet<ClientboundPlayerInfoUpdatePacket.Action> actions = 
                    java.util.EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME);
                
                // Get the actual ServerPlayer objects since the packet constructor requires Collection<ServerPlayer>
                java.util.List<ServerPlayer> playerList = new java.util.ArrayList<>();
                for (ClientboundPlayerInfoUpdatePacket.Entry entry : addEntries) {
                    ServerPlayer player = server.getPlayerList().getPlayer(entry.profileId());
                    if (player != null) {
                        playerList.add(player);
                    }
                }
                
                // Only send if we have players
                if (!playerList.isEmpty()) {
                    addPacket = new ClientboundPlayerInfoUpdatePacket(actions, playerList);
                    viewer.connection.send(addPacket);
                }
            }
        }, "Error updating player list for viewer " + viewer.getScoreboardName());
    }
    
    /**
     * Sorts players according to the configured sort type
     * 
     * @param players The list of players to sort
     */
    private void sortPlayers(List<ServerPlayer> players) {
        if (sortType == SortType.NAME) {
            // Sort alphabetically by name
            players.sort(Comparator.comparing(ServerPlayer::getScoreboardName));
        } else if (sortType == SortType.GROUP) {
            // Sort by group priority then name
            players.sort((p1, p2) -> {
                TabPlayerData data1 = tabManager.getPlayerData(p1);
                TabPlayerData data2 = tabManager.getPlayerData(p2);
                
                if (data1 == null || data2 == null) {
                    return p1.getScoreboardName().compareTo(p2.getScoreboardName());
                }
                
                // First compare sort priority (higher values first)
                int priorityCompare = Integer.compare(data2.getSortPriority(), data1.getSortPriority());
                if (priorityCompare != 0) {
                    return priorityCompare;
                }
                
                // Then compare groups (would need a group priority map)
                int groupCompare = data1.getGroup().compareTo(data2.getGroup());
                if (groupCompare != 0) {
                    return groupCompare;
                }
                
                // Finally sort by name
                return p1.getScoreboardName().compareTo(p2.getScoreboardName());
            });
        } else if (sortType == SortType.PLAYTIME) {
            // Sort by playtime (longest first)
            players.sort((p1, p2) -> {
                TabPlayerData data1 = tabManager.getPlayerData(p1);
                TabPlayerData data2 = tabManager.getPlayerData(p2);
                
                if (data1 == null || data2 == null) {
                    return p1.getScoreboardName().compareTo(p2.getScoreboardName());
                }
                
                // Compare playtime (descending order)
                int playtimeCompare = Long.compare(data2.getPlaytime(), data1.getPlaytime());
                if (playtimeCompare != 0) {
                    return playtimeCompare;
                }
                
                // Fall back to name comparison
                return p1.getScoreboardName().compareTo(p2.getScoreboardName());
            });
        }
    }
    
    @Override
    public void onPlayerJoin(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Update player list when a player joins
        update();
    }
    
    @Override
    public void onPlayerLeave(ServerPlayer player) {
        if (!isEnabled()) return;
        
        // Clean up cached data
        lastDisplayNames.remove(player.getUUID());
        lastPingValues.remove(player.getUUID());
        
        // Update player list for remaining players
        update();
    }
    
    @Override
    public void onPlayerChangeWorld(ServerPlayer player, String worldName) {
        if (!isEnabled()) return;
        
        // Update player lists when a player changes worlds
        update();
    }
    
    /**
     * Get whether the per-world playerlist is enabled
     */
    public boolean isPerWorldPlayerList() {
        return perWorldPlayerList;
    }
    
    /**
     * Sets whether to use per-world player list
     */
    public void setPerWorldPlayerList(boolean perWorldPlayerList) {
        this.perWorldPlayerList = perWorldPlayerList;
    }
    
    /**
     * Get whether playerlist sorting is enabled
     */
    public boolean isEnableSorting() {
        return enableSorting;
    }
    
    /**
     * Sets whether to enable playerlist sorting
     */
    public void setEnableSorting(boolean enableSorting) {
        this.enableSorting = enableSorting;
    }
    
    /**
     * Get the current sort type
     */
    public SortType getSortType() {
        return sortType;
    }
    
    /**
     * Sets the sort type
     */
    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }
}
