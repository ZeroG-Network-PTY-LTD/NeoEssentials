package com.zerog.neoessentials.features;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public class TabListManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(TabListManager.class);
	private static TabListManager instance;
	private com.zerog.neoessentials.config.TablistConfig config;
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
		System.out.println("[TabListManager] Tablist config reloaded!");
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
				System.out.println("[TabListManager] Generated default tablist.json using TablistConfig.");
			} else {
				try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
					// Only parse relevant fields, ignore commented-out stuff
					com.google.gson.JsonObject json = gson.fromJson(reader, com.google.gson.JsonObject.class);
					config = new com.zerog.neoessentials.config.TablistConfig();
					if (json.has("tablistFormat")) config.tablistFormat = json.get("tablistFormat").getAsString();
					if (json.has("PermSets")) {
						java.lang.reflect.Type permSetType = new com.google.gson.reflect.TypeToken<java.util.Map<String, com.zerog.neoessentials.config.TablistConfig.PermSet>>(){}.getType();
						config.PermSets = gson.fromJson(json.get("PermSets"), permSetType);
					}
					if (json.has("defaultTablist")) config.defaultTablist = gson.fromJson(json.get("defaultTablist"), com.zerog.neoessentials.config.TablistConfig.DefaultTablist.class);
					// teamFilters.order
					if (json.has("PermSets")) {
						com.google.gson.JsonObject permSetsObj = json.getAsJsonObject("PermSets");
						if (permSetsObj.has("teamFilters")) {
							com.google.gson.JsonObject teamFiltersObj = permSetsObj.getAsJsonObject("teamFilters");
							if (teamFiltersObj.has("order")) {
								config.teamFiltersOrder = teamFiltersObj.get("order").getAsString();
							}
						}
					}
				}
			}
		} catch (Exception e) {
			config = new com.zerog.neoessentials.config.TablistConfig();
			e.printStackTrace();
		}
	}

	// Update tablist for all players with config-driven layout
	public void updateTabList(Collection<ServerPlayer> players) {
		// Sort players using config.teamFiltersOrder if present
		List<ServerPlayer> sortedPlayers = new ArrayList<>(players);
		String playerOrder = config.teamFiltersOrder;
		if (playerOrder != null) {
			String[] rules = playerOrder.split(",");
			Comparator<ServerPlayer> comparator = null;
			for (String rule : rules) {
				String[] parts = rule.trim().split(" ");
				String placeholder = parts[0];
				String direction = parts.length > 1 ? parts[1] : "asc";
				Comparator<ServerPlayer> ruleComp;
				if (rule.contains("as number")) {
					ruleComp = Comparator.comparingDouble(p -> {
						String value = parsePlaceholders(p, "%" + placeholder + "%");
						try {
							return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
						} catch (Exception e) {
							return 0.0;
						}
					});
				} else {
					ruleComp = Comparator.comparing(p -> {
						String value = parsePlaceholders(p, "%" + placeholder + "%");
						return value != null ? value : "";
					}, Comparator.nullsLast(String::compareTo));
				}
				if (direction.equalsIgnoreCase("desc")) {
					ruleComp = ruleComp.reversed();
				}
				comparator = comparator == null ? ruleComp : comparator.thenComparing(ruleComp);
			}
			if (comparator != null) {
				sortedPlayers.sort(comparator);
			}
		}
		for (ServerPlayer player : sortedPlayers) {
			List<String> lines = new ArrayList<>();
			// Per-permission tablist layout
			boolean matched = false;
			if (config.PermSets != null) {
				for (java.util.Map.Entry<String, com.zerog.neoessentials.config.TablistConfig.PermSet> entry : config.PermSets.entrySet()) {
					com.zerog.neoessentials.config.TablistConfig.PermSet set = entry.getValue();
					if (set.permission != null && set.permission.startsWith("permission:")) {
						String perm = set.permission.substring("permission:".length());
						if (perm.equalsIgnoreCase("neo.staff")) perm = "neoessentials.tablist.staff";
						if (perm.equalsIgnoreCase("neo.vip")) perm = "neoessentials.tablist.vip";
						com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
						boolean hasPerm = permMgr.hasPermission(player, perm);
						String logMsg = "[TabListManager] Checking tablist PermSet '" + entry.getKey() + "' for player " + player.getName().getString() + ": permission='" + perm + "', hasPermission=" + hasPerm;
						System.out.println(logMsg);
						LOGGER.info(logMsg);
						if (hasPerm) {
							if (set.tablist != null && set.tablist.header != null && !set.tablist.header.isEmpty()) {
								for (String h : set.tablist.header) {
									String replaced = parsePlaceholders(player, h);
									replaced = replaced.replace('&', '§');
									lines.add(replaced);
								}
							}
							matched = true;
							String matchMsg = "[TabListManager] Matched PermSet '" + entry.getKey() + "' for player " + player.getName().getString();
							System.out.println(matchMsg);
							LOGGER.info(matchMsg);
							break;
						}
					}
				}
				if (!matched) {
					String noMatchMsg = "[TabListManager] No PermSet matched for player " + player.getName().getString() + ", using defaultTablist.";
					System.out.println(noMatchMsg);
					LOGGER.info(noMatchMsg);
				}
			}
			// Fallback to defaultTablist if no permission match
			if (!matched && config.defaultTablist != null && config.defaultTablist.tablist != null && config.defaultTablist.tablist.header != null) {
				for (String h : config.defaultTablist.tablist.header) {
					String replaced = parsePlaceholders(player, h);
					replaced = replaced.replace('&', '§');
					lines.add(replaced);
				}
			}
			// If still empty, use tablistFormat as last fallback
			if (lines.isEmpty() && config.tablistFormat != null && !config.tablistFormat.isEmpty()) {
				String replaced = parsePlaceholders(player, config.tablistFormat);
				replaced = replaced.replace('&', '§');
				lines.add(replaced);
			}
			String tabEntry = String.join(" | ", lines);
			playerDisplayNames.put(player.getUUID(), tabEntry);
			net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
			net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(player.getScoreboardName());
			if (team == null) {
				team = scoreboard.addPlayerTeam(player.getScoreboardName());
			}
			team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(tabEntry));
			scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
		}
	}

	// Per-permission tablist header/footer selection (uses PermSets and defaultTablist)
	public void updateHeaderFooter(ServerPlayer player, String displayName) {
		List<String> headerLines = null;
		List<String> footerLines = null;
		// Check PermSets for matching permission
		if (config.PermSets != null) {
			for (Map.Entry<String, com.zerog.neoessentials.config.TablistConfig.PermSet> entry : config.PermSets.entrySet()) {
				com.zerog.neoessentials.config.TablistConfig.PermSet set = entry.getValue();
				if (set.permission != null && set.permission.startsWith("permission:")) {
					String perm = set.permission.substring("permission:".length());
					// Update to use neoessentials.tablist.<group> permission nodes
					if (perm.equalsIgnoreCase("neo.staff")) perm = "neoessentials.tablist.staff";
					if (perm.equalsIgnoreCase("neo.vip")) perm = "neoessentials.tablist.vip";
					com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
					if (permMgr.hasPermission(player, perm)) {
						if (set.tablist != null && set.tablist.header != null && !set.tablist.header.isEmpty()) {
							headerLines = set.tablist.header;
						}
						if (set.tablist != null && set.tablist.footer != null && !set.tablist.footer.isEmpty()) {
							footerLines = set.tablist.footer;
						}
						break;
					}
				}
			}
		}
		// Fallback to defaultTablist if no permission match
		if (headerLines == null && config.defaultTablist != null && config.defaultTablist.tablist != null) {
			headerLines = config.defaultTablist.tablist.header;
		}
		if (footerLines == null && config.defaultTablist != null && config.defaultTablist.tablist != null) {
			footerLines = config.defaultTablist.tablist.footer;
		}
		if (headerLines == null) headerLines = new ArrayList<>();
		if (footerLines == null) footerLines = new ArrayList<>();
		String headerText = String.join("\n", headerLines);
		String footerText = String.join("\n", footerLines);
		String parsedHeader = parsePlaceholders(player, headerText).replace('&', '§');
		String parsedFooter = parsePlaceholders(player, footerText).replace('&', '§');
		player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(
			net.minecraft.network.chat.Component.literal(parsedHeader),
			net.minecraft.network.chat.Component.literal(parsedFooter)
		));
		if (displayName != null) {
			System.out.println("[TabListManager] DisplayName for " + player.getName().getString() + ": " + displayName);
		}
	}


	public void updatePlayerEntry(ServerPlayer player) {
		String displayName = com.zerog.neoessentials.features.DisplayNameManager.getDisplayName(player).replace('&', '§');
		playerDisplayNames.put(player.getUUID(), displayName);
		net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
		net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(player.getScoreboardName());
		if (team == null) {
			team = scoreboard.addPlayerTeam(player.getScoreboardName());
		}
		team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(displayName));
		scoreboard.addPlayerToTeam(player.getScoreboardName(), team);
	}

	public String parsePlaceholders(ServerPlayer player, String text) {
		return placeholderManager.processPlaceholders(text, player);
	}
}
