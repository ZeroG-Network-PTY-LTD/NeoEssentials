	// ...existing imports and class declaration...
package com.zerog.neoessentials.features;


import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class TabListManager {
	/**
	 * Refresh tablist and header/footer for all online players. Call after group/permission changes.
	 */
	public void refreshTablistForAll(Collection<ServerPlayer> players) {
		updateTabList(players);
		for (ServerPlayer player : players) {
			updateHeaderFooter(player, null);
		}
	}
	// LOGGER removed (was unused)
	private static TabListManager instance;
	public com.zerog.neoessentials.config.TablistConfig config;
	private final Map<UUID, String> playerDisplayNames = new HashMap<>();
	private final com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();

	public TabListManager() {
		loadConfig();
		instance = this;
		registerTablistPermissions();
	}

	public static TabListManager getInstance() {
		return instance;
	}

	public void reloadConfig() {
		loadConfig();
		registerTablistPermissions();
		com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Tablist config reloaded!");
	}

	/**
	 * Scan playerSets for permission filters and register them as custom permission nodes
	 */
	private void registerTablistPermissions() {
	// Permissions for tablist are now registered via PermSets in config. No legacy playerSets.
	}

	private void loadConfig() {
		try {
			String configPath = "config/neoessentials/tablist.json";
			java.io.File configFile = new java.io.File(configPath);
			com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
			if (!configFile.exists()) {
				config = new com.zerog.neoessentials.config.TablistConfig();
				try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
					gson.toJson(config, writer);
				}
				com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Generated default tablist.json using TablistConfig.");
			} else {
				try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
					config = gson.fromJson(reader, com.zerog.neoessentials.config.TablistConfig.class);
				}
			}
		} catch (Exception e) {
			config = new com.zerog.neoessentials.config.TablistConfig();
			e.printStackTrace();
		}
	}

	// Update tablist for all players with config-driven layout
	public void updateTabList(Collection<ServerPlayer> players) {
	com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] DEBUG: updateTabList called for " + players.size() + " player(s).");
		if (config == null || !config.enableTablist) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Tablist is disabled in config, skipping updateTabList.");
			return;
		}
		com.zerog.neoessentials.util.DebugUtil.infoLog("[TabListManager] updateTabList called for " + players.size() + " player(s).");
		List<ServerPlayer> sortedPlayers = new ArrayList<>(players);
		String order = config != null ? config.teamFiltersOrder : null;
		com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
		if (order != null) {
			switch (order) {
				case "priority-desc":
					sortedPlayers.sort((a, b) -> Integer.compare(
						permMgr.getPlayerPriority(b.getUUID()),
						permMgr.getPlayerPriority(a.getUUID())));
					break;
				case "priority-asc":
					sortedPlayers.sort((a, b) -> Integer.compare(
						permMgr.getPlayerPriority(a.getUUID()),
						permMgr.getPlayerPriority(b.getUUID())));
					break;
				case "name-asc":
					sortedPlayers.sort(Comparator.comparing(p -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER));
					break;
				case "name-desc":
					sortedPlayers.sort(Comparator.comparing((ServerPlayer p) -> p.getGameProfile().getName(), String.CASE_INSENSITIVE_ORDER).reversed());
					break;
				// Add more custom orders as needed
			}
		}
		for (ServerPlayer player : sortedPlayers) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("[NeoEssentials] updateTabList called for player " + player.getUUID());
			int highestPriority = Integer.MIN_VALUE;
			com.zerog.neoessentials.config.TablistConfig.TablistLayout matchedLayout = null;
			List<String> matchedConditions = new ArrayList<>();
			if (config.tablistLayouts != null) {
				String playerGroup = com.zerog.neoessentials.features.NameFormatManager.getInstance().getGroup(player);
				for (com.zerog.neoessentials.config.TablistConfig.TablistLayout layout : config.tablistLayouts) {
					int priority = layout.priority;
					String conditionType = layout.conditionType != null ? layout.conditionType : "default";
					String condition = layout.condition != null ? layout.condition : "";
					boolean match = false;
					if ("permission".equalsIgnoreCase(conditionType)) {
						boolean hasPerm = permMgr.hasPermission(player, condition);
						com.zerog.neoessentials.util.DebugUtil.debugLog("TabListManager: Checking permission '" + condition + "' for player " + player.getGameProfile().getName() + ": " + hasPerm);
						if (hasPerm) match = true;
					} else if ("group".equalsIgnoreCase(conditionType)) {
						boolean groupMatch = playerGroup != null && playerGroup.equalsIgnoreCase(condition);
						com.zerog.neoessentials.util.DebugUtil.debugLog("TabListManager: Checking group '" + condition + "' for player " + player.getGameProfile().getName() + ": " + groupMatch);
						if (groupMatch) match = true;
					} else if ("default".equalsIgnoreCase(conditionType)) {
						match = true;
					}
					if (match) {
						matchedConditions.add("priority=" + priority + ", type=" + conditionType + ", condition=" + condition);
						if (priority > highestPriority) {
							highestPriority = priority;
							matchedLayout = layout;
						}
					}
				}
			}
			com.zerog.neoessentials.util.DebugUtil.debugLog("Player: " + player.getGameProfile().getName() + " matched conditions: " + matchedConditions);
			if (matchedLayout != null) {
				com.zerog.neoessentials.util.DebugUtil.debugLog("Player: " + player.getGameProfile().getName() + " selected tablist layout: priority=" + matchedLayout.priority + ", type=" + matchedLayout.conditionType + ", condition=" + matchedLayout.condition);
			} else {
				com.zerog.neoessentials.util.DebugUtil.debugLog("Player: " + player.getGameProfile().getName() + " using fallback tablistFormat");
			}

			// Use automatic prefix/suffix logic, matching chat
			String prefix = com.zerog.neoessentials.features.NameFormatManager.getInstance().getPrefix(player);
			String suffix = com.zerog.neoessentials.features.NameFormatManager.getInstance().getSuffix(player);
			String displayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player);
			// Process placeholders (including animated/custom) for prefix/suffix/displayName
			prefix = placeholderManager.processPlaceholders(prefix, player);
			suffix = placeholderManager.processPlaceholders(suffix, player);
			displayName = placeholderManager.processPlaceholders(displayName, player);
			// Apply color codes
			prefix = prefix.replace('&', '\u00A7');
			suffix = suffix.replace('&', '\u00A7');
			displayName = displayName.replace('&', '\u00A7');
			// Debug output for prefix/suffix/displayName
			com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist prefix for " + player.getGameProfile().getName() + ": " + prefix);
			com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist suffix for " + player.getGameProfile().getName() + ": " + suffix);
			com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist displayName for " + player.getGameProfile().getName() + ": " + displayName);

			net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
			String username = player.getGameProfile().getName();
			net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(username);
			if (team == null) {
				team = scoreboard.addPlayerTeam(username);
			}
			team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(prefix));
			team.setPlayerSuffix(net.minecraft.network.chat.Component.literal(suffix));
			scoreboard.addPlayerToTeam(username, team);
			playerDisplayNames.put(player.getUUID(), displayName);
		}
	}

	/**
	 * Checks if the player has the permission explicitly (not just OP status)
	 */
	private boolean hasExplicitPermission(com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr, ServerPlayer player, String permission) {
		if (permission == null || permission.isEmpty()) return false;
		UUID playerId = player.getUUID();
		// Check player-specific permissions
		Set<String> perms = permMgr.getPlayerPermissions(playerId);
		return perms.contains(permission);
	}

	// Tablist header/footer selection using PermSets
	public void updateHeaderFooter(ServerPlayer player, String displayName) {
	com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] updateHeaderFooter called for " + player.getName().getString() + " (UUID: " + player.getUUID() + ")");
	// ...existing code...
		// Refactor: Use tablistLayouts for header/footer selection
	// Removed duplicate declarations of headerLines and footerLines
		if (config == null || !config.enableTablist) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist is disabled in config, skipping updateHeaderFooter for " + player.getName().getString());
			return;
		}
		int highestPriority = Integer.MIN_VALUE;
		com.zerog.neoessentials.config.TablistConfig.TablistLayout matchedLayout = null;
		com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
		if (config.tablistLayouts != null) {
			String playerGroup = com.zerog.neoessentials.features.NameFormatManager.getInstance().getGroup(player);
			for (com.zerog.neoessentials.config.TablistConfig.TablistLayout layout : config.tablistLayouts) {
				int priority = layout.priority;
				String conditionType = layout.conditionType != null ? layout.conditionType : "default";
				String condition = layout.condition != null ? layout.condition : "";
				boolean match = false;
				if ("permission".equalsIgnoreCase(conditionType)) {
					if (permMgr.hasPermission(player, condition)) match = true;
				} else if ("group".equalsIgnoreCase(conditionType)) {
					if (playerGroup != null && playerGroup.equalsIgnoreCase(condition)) match = true;
				} else if ("default".equalsIgnoreCase(conditionType)) {
					match = true;
				}
				if (match && priority > highestPriority) {
					highestPriority = priority;
					matchedLayout = layout;
				}
			}
		}
		List<String> headerLines = matchedLayout != null && matchedLayout.header != null ? matchedLayout.header : new ArrayList<>();
		List<String> footerLines = matchedLayout != null && matchedLayout.footer != null ? matchedLayout.footer : new ArrayList<>();
		String headerText = String.join("\n", headerLines);
		String footerText = String.join("\n", footerLines);
		String parsedHeader = parsePlaceholders(player, headerText);
		parsedHeader = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance().processPlaceholders(parsedHeader, player).replace('&', '\u00A7');
		String parsedFooter = parsePlaceholders(player, footerText);
		parsedFooter = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance().processPlaceholders(parsedFooter, player).replace('&', '\u00A7');
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(
			net.minecraft.network.chat.Component.literal(parsedHeader),
			net.minecraft.network.chat.Component.literal(parsedFooter)
		));
		if (displayName != null) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("DisplayName for " + player.getName().getString() + ": " + displayName);
		}
	}


	public void updatePlayerEntry(ServerPlayer player) {
		if (config == null || !config.enableTablist) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist is disabled in config, skipping updatePlayerEntry for " + player.getName().getString());
			return;
		}
		String prefix = com.zerog.neoessentials.features.NameFormatManager.getInstance().getPrefix(player);
		playerDisplayNames.put(player.getUUID(), prefix);
		net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
		String username = player.getGameProfile().getName();
		net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(username);
		if (team == null) {
			team = scoreboard.addPlayerTeam(username);
		}
		team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(prefix != null ? prefix : ""));
		scoreboard.addPlayerToTeam(username, team);
	}

	public String parsePlaceholders(ServerPlayer player, String text) {
		return placeholderManager.processPlaceholders(text, player);
	}
}
