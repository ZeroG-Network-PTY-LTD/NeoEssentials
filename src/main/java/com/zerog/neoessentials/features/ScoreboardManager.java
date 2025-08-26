package com.zerog.neoessentials.features;

import com.zerog.neoessentials.util.DebugUtil;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import com.zerog.neoessentials.NeoEssentials;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.*;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.network.protocol.game.ClientboundSetDisplayObjectivePacket;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.bus.api.SubscribeEvent;
import com.google.gson.Gson;
import java.io.FileReader;
import java.util.*;

public class ScoreboardManager {
    // Group data loaded from groups.json
    private final Map<String, String> groupPrefixes = new HashMap<>();
    private final Map<String, String> groupSuffixes = new HashMap<>();
    private static final String OBJECTIVE_PREFIX = "neoess_sidebar_";
    private static final String TEAM_PREFIX = "neo_";
    private MinecraftServer server;

    // Config structure: group -> title/lines
    private final Map<String, String> groupTitles = new HashMap<>();
    private final Map<String, List<String>> groupLines = new HashMap<>();
    private final Map<UUID, Long> playerJoinTime = new HashMap<>();

    public ScoreboardManager() {
    NeoForge.EVENT_BUS.register(this);
    loadConfig();
    loadGroupConfig();
        NeoForge.EVENT_BUS.register(this);
        loadConfig();
    }

    private void loadConfig() {
        try {
            String configPath = "config/neoessentials/scoreboard.json";
            Gson gson = new Gson();
            FileReader reader = new FileReader(configPath);
            java.util.Map<?,?> config = gson.fromJson(reader, java.util.Map.class);
            java.util.Map<?,?> groups = (java.util.Map<?,?>) config.get("groups");
            groupTitles.clear();
            groupLines.clear();
            for (Map.Entry<?,?> entry : groups.entrySet()) {
                String group = entry.getKey().toString();
                java.util.Map<?,?> groupConfig = (java.util.Map<?,?>) entry.getValue();
                String title = groupConfig.get("title").toString();
                List<String> lines = new ArrayList<>();
                Object linesObj = groupConfig.get("lines");
                if (linesObj instanceof List<?>) {
                    for (Object line : (List<?>) linesObj) {
                        lines.add(line.toString());
                    }
                }
                groupTitles.put(group, title);
                groupLines.put(group, lines);
            }
            reader.close();
        } catch (Exception e) {
            // Fallback to defaults if error
            groupTitles.put("default", "&e&lPlayer Stats");
            groupLines.put("default", List.of(
                "&aName: &f%player%",
                "&bRank: &f%group%",
                "&cHealth: &f%health% &7| &cHunger: &f%hunger%",
                "&dCoords: &f%x% &7| &f%y% &7| &f%z%",
                "&eOnline: &f%onlinetime%",
                "&6Kills: &f%kills% &7| &6Deaths: &f%deaths%"
            ));
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        this.server = event.getServer();
    }


    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            playerJoinTime.put(player.getUUID(), System.currentTimeMillis());
            updateScoreboard(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            cleanupPlayer(player);
            playerJoinTime.remove(player.getUUID());
        }
    }

    public void updateAllScoreboards() {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            updateScoreboard(player);
        }
    }

    public void updateScoreboard(ServerPlayer player) {
        com.zerog.neoessentials.config.TablistConfig config = com.zerog.neoessentials.features.TabListManager.getInstance().config;
        if (player == null) {
            DebugUtil.debugLog("ScoreboardManager: player is null in updateScoreboard. Skipping update.");
            return;
        }
        if (player.getServer() == null) {
            DebugUtil.debugLog("ScoreboardManager: server is null for player " + player.getName().getString() + ". Scheduling delayed scoreboard update.");
            // Schedule a delayed update using a simple tick event (NeoForge example)
            net.minecraft.server.MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server != null) {
                server.execute(() -> updateScoreboard(player));
            }
            return;
        }
        if (config == null || !config.enableScoreboard) {
            com.zerog.neoessentials.util.DebugUtil.debugLog("[ScoreboardManager] Scoreboard is disabled in config, skipping updateScoreboard for " + player.getName().getString());
            return;
        }
        Scoreboard scoreboard = server.getScoreboard();
        String objectiveName = OBJECTIVE_PREFIX + player.getUUID();

        // Create individual objective for this player
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective == null) {
            objective = scoreboard.addObjective(
                objectiveName,
                ObjectiveCriteria.DUMMY,
                Component.literal(getTitle(player)),
                ObjectiveCriteria.RenderType.INTEGER,
                true,
                null
            );
        }

        // Only update title if changed
        Component newTitle = Component.literal(getTitle(player).replace("&", "§"));
        if (!objective.getDisplayName().equals(newTitle)) {
            objective.setDisplayName(newTitle);
        }

        // Set this objective as sidebar ONLY for this player
        player.connection.send(new ClientboundSetDisplayObjectivePacket(
            DisplaySlot.SIDEBAR, objective
        ));

        // Remove previous scores for this objective (NeoForge API fix)
        // Remove previous scores for this objective (manual tracking)
        // Remove previous scores for this objective (reset player's own score)
        try {
            var score = scoreboard.getOrCreatePlayerScore(player, objective);
            score.set(0);
        } catch (Exception ignored) {}

        // Add only THIS player's lines
    var score = scoreboard.getOrCreatePlayerScore(player, objective);
    score.set(getKills(player)); // Example: show kills as score

        // Team management (prefix/suffix/colors)
        String teamName = TEAM_PREFIX + player.getUUID();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        team.setPlayerPrefix(Component.literal(getPrefix(player)));
        team.setPlayerSuffix(Component.literal(getSuffix(player)));
        scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
    }

    public void cleanupPlayer(ServerPlayer player) {
        if (server == null) return;
        Scoreboard scoreboard = server.getScoreboard();
        String objectiveName = OBJECTIVE_PREFIX + player.getUUID();
        Objective objective = scoreboard.getObjective(objectiveName);
        if (objective != null) scoreboard.removeObjective(objective);
        String teamName = TEAM_PREFIX + player.getUUID();
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team != null) scoreboard.removePlayerTeam(team);
    }

    // PlaceholderManager integration and color code conversion

    // Load group prefix/suffix from groups.json in server root
    private void loadGroupConfig() {
        try {
            String groupConfigPath = "neoessentials/permissions/groups.json";
            Gson gson = new Gson();
            FileReader reader = new FileReader(groupConfigPath);
            java.util.Map<?,?> config = gson.fromJson(reader, java.util.Map.class);
            groupPrefixes.clear();
            groupSuffixes.clear();
            for (Map.Entry<?,?> entry : config.entrySet()) {
                String group = entry.getKey().toString();
                java.util.Map<?,?> groupData = (java.util.Map<?,?>) entry.getValue();
                String prefix = groupData.containsKey("prefix") ? groupData.get("prefix").toString() : "";
                String suffix = groupData.containsKey("suffix") ? groupData.get("suffix").toString() : "";
                groupPrefixes.put(group, prefix);
                groupSuffixes.put(group, suffix);
            }
            reader.close();
        } catch (Exception e) {
            // Fallback: no group config
        }
    }

    // Get title for player/group
    private String getTitle(ServerPlayer player) {
        String group = getGroup(player);
        return groupTitles.getOrDefault(group, groupTitles.get("default"));
    }


    // Get group from permissions (replace with your actual logic)
    private String getGroup(ServerPlayer player) {
        // Use your existing permission system or default
        // Example: get group from CustomPermissionsManager if available
        try {
            Class<?> permMgrClass = Class.forName("com.zerog.neoessentials.permissions.CustomPermissionsManager");
            Object permMgr = permMgrClass.getMethod("getInstance").invoke(null);
            String group = (String) permMgrClass.getMethod("getPlayerGroup", UUID.class).invoke(permMgr, player.getUUID());
            if (group != null && !group.isEmpty()) return group;
        } catch (Exception ignored) {}
        return "default";
    }

    // Get prefix/suffix from permissions (replace with your actual logic)
    private String getPrefix(ServerPlayer player) {
        String group = getGroup(player);
        String prefix = groupPrefixes.getOrDefault(group, "");
        return prefix.replace("&", "§");
    }
    private String getSuffix(ServerPlayer player) {
        String group = getGroup(player);
        String suffix = groupSuffixes.getOrDefault(group, "");
        return suffix.replace("&", "§");
    }

    // Online time calculation
    // Integrate with your existing score system
    public static int getPlayerScore(UUID playerId) {
        // Use your actual score system here
        return 0;
    }

    private int getKills(ServerPlayer player) {
        return com.zerog.neoessentials.features.ScoreboardManager.getPlayerScore(player.getUUID());
    }
}