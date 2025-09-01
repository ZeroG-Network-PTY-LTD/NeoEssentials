
package com.zerog.neoessentials.features;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import net.minecraft.network.chat.Component;

public class TabListManager {
	// Scheduler for tablist animation updates
	private static TabListManager instance;
	public com.zerog.neoessentials.config.TablistConfig config;
	private final Map<UUID, String> playerDisplayNames = new HashMap<>();
	private final com.zerog.neoessentials.placeholders.PlaceholderManager placeholderManager = com.zerog.neoessentials.placeholders.PlaceholderManager.getInstance();
	private final Map<UUID, TabViewState> tabViewStates = new HashMap<>(); // Per-viewer tablist state

	/**
	 * Refresh tablist and header/footer for all online players. Call after group/permission changes.
	 */
	public void refreshTablistForAll(Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			if (!tabViewStates.containsKey(player.getUUID())) {
				tabViewStates.put(player.getUUID(), new TabViewState());
			}
			updateHeaderFooter(player, com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player)); // Always update header/footer for each player
		}
		updateTabList(players);
	}

	public TabListManager() {
		loadConfig();
		instance = this;
		registerTablistPermissions();
	}

	// Animated placeholder refresh logic
	private final Map<String, java.util.concurrent.ScheduledFuture<?>> animatedPlaceholderTasks = new HashMap<>();
	private final Map<String, java.util.concurrent.ScheduledFuture<?>> headerFooterAnimationTasks = new HashMap<>();

	/**
	 * Call this to start animation updates for a specific animated placeholder.
	 * Updates tablist for players using that placeholder, and header/footer if used there.
	 */
	public void startAnimatedPlaceholderRefresh(String placeholderId, double intervalSeconds) {
		if (animatedPlaceholderTasks.containsKey(placeholderId)) return;
		java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
		java.util.concurrent.ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
			try {
				net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
				if (server == null) return;
				java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
				// Force tablist update by clearing lastEntries cache for all players
				for (ServerPlayer player : players) {
					TabViewState viewState = tabViewStates.get(player.getUUID());
					if (viewState != null) {
						viewState.lastEntries.remove(player.getUUID());
					}
				}
				updateTabList(players);
			} catch (Exception e) {
				com.zerog.neoessentials.util.DebugUtil.debugLog("Animated placeholder refresh error: " + e.getMessage());
			}
		}, 0, (long)(intervalSeconds * 1000), java.util.concurrent.TimeUnit.MILLISECONDS);
		animatedPlaceholderTasks.put(placeholderId, future);

		// Schedule header/footer refresh if any tablist layout uses this animated placeholder
		boolean usedInHeaderFooter = false;
		if (config != null && config.tablistLayouts != null) {
			for (com.zerog.neoessentials.config.TablistConfig.TablistLayout layout : config.tablistLayouts) {
				for (String headerLine : layout.header) {
					if (headerLine.contains("${" + placeholderId + "}")) usedInHeaderFooter = true;
				}
				for (String footerLine : layout.footer) {
					if (footerLine.contains("${" + placeholderId + "}")) usedInHeaderFooter = true;
				}
			}
		}
		if (usedInHeaderFooter && !headerFooterAnimationTasks.containsKey(placeholderId)) {
			java.util.concurrent.ScheduledExecutorService hfScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
			java.util.concurrent.ScheduledFuture<?> hfFuture = hfScheduler.scheduleAtFixedRate(() -> {
				try {
					net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
					if (server == null) return;
					java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
					for (ServerPlayer player : players) {
						updateHeaderFooter(player, com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player));
					}
				} catch (Exception e) {
					com.zerog.neoessentials.util.DebugUtil.debugLog("Header/footer animation refresh error: " + e.getMessage());
				}
			}, 0, (long)(intervalSeconds * 1000), java.util.concurrent.TimeUnit.MILLISECONDS);
			headerFooterAnimationTasks.put(placeholderId, hfFuture);
		}
	}

	public static TabListManager getInstance() {
		return instance;
	}

	public void reloadConfig() {
		loadConfig();
		registerTablistPermissions();
		com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Tablist config reloaded!");
		// After reload, update tablist and header/footer for all online players
		net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			java.util.List<net.minecraft.server.level.ServerPlayer> players = server.getPlayerList().getPlayers();
			refreshTablistForAll(players);
		}
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
		com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] updateTabList START for " + players.size() + " players");
		for (ServerPlayer player : players) {
			TabViewState viewState = tabViewStates.get(player.getUUID());
			if (viewState == null) continue;

			// Compute new displayName, teamId, latencyBucket
			String prefix = com.zerog.neoessentials.features.NameFormatManager.getInstance().getPrefix(player);
			String suffix = com.zerog.neoessentials.features.NameFormatManager.getInstance().getSuffix(player);
			String displayName = com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player);
			int latency = player.connection.latency();
			int latencyBucket = latency < 50 ? 0 : latency < 150 ? 1 : latency < 300 ? 2 : 3;
			String teamId = "neo_" + player.getUUID();

			// TabViewState.EntryState lastEntry = viewState.lastEntries.get(player.getUUID());

			// Always update scoreboard team for prefix/suffix
			net.minecraft.world.scores.Scoreboard scoreboard = player.getScoreboard();
			net.minecraft.world.scores.PlayerTeam team = scoreboard.getPlayerTeam(teamId);
			if (team == null) {
				team = scoreboard.addPlayerTeam(teamId);
			}
			team.setPlayerPrefix(net.minecraft.network.chat.Component.literal(prefix));
			team.setPlayerSuffix(net.minecraft.network.chat.Component.literal(suffix));
			scoreboard.addPlayerToTeam(player.getGameProfile().getName(), team);

			// Update player display name in internal map for future use (not sent to client)
			playerDisplayNames.put(player.getUUID(), displayName);

			viewState.lastEntries.put(player.getUUID(), new TabViewState.EntryState(displayName, teamId, latencyBucket));
		}
		com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] updateTabList END");
	}

	public void updateHeaderFooter(ServerPlayer player, String displayName) {
		// Best-practice: diffing, placeholder parsing, rate-limited update
		TabViewState viewState = tabViewStates.get(player.getUUID());
		if (viewState == null) return;
		if (config == null || !config.enableTablist) return;
		int highestPriority = Integer.MIN_VALUE;
		com.zerog.neoessentials.config.TablistConfig.TablistLayout matchedLayout = null;
		com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
		String playerGroup = com.zerog.neoessentials.features.NameFormatManager.getInstance().getGroup(player);
		if (config.tablistLayouts != null) {
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
		List<String> headerLines = (matchedLayout != null && matchedLayout.header != null && !matchedLayout.header.isEmpty()) ? matchedLayout.header : java.util.Arrays.asList("NeoEssentials Tablist");
		List<String> footerLines = (matchedLayout != null && matchedLayout.footer != null && !matchedLayout.footer.isEmpty()) ? matchedLayout.footer : java.util.Arrays.asList("Powered by NeoEssentials");
		String headerText = String.join("\n", headerLines);
		String footerText = String.join("\n", footerLines);
		String parsedHeader = parsePlaceholders(player, headerText).replace('&', '\u00A7');
		String parsedFooter = parsePlaceholders(player, footerText).replace('&', '\u00A7');
		Component headerComponent = net.minecraft.network.chat.Component.literal(parsedHeader);
		Component footerComponent = net.minecraft.network.chat.Component.literal(parsedFooter);
		// Only send header/footer if changed (diffing)
		if (viewState.lastSentHeader == null || !viewState.lastSentHeader.equals(headerComponent) || viewState.lastSentFooter == null || !viewState.lastSentFooter.equals(footerComponent)) {
			player.getServer().execute(() -> {
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(headerComponent, footerComponent));
			});
			viewState.lastSentHeader = headerComponent;
			viewState.lastSentFooter = footerComponent;
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

	public void onPlayerJoin(ServerPlayer player) {
		com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] onPlayerJoin START for " + player.getGameProfile().getName());
		if (!tabViewStates.containsKey(player.getUUID())) {
			tabViewStates.put(player.getUUID(), new TabViewState());
		}
		updateHeaderFooter(player, com.zerog.neoessentials.features.NameFormatManager.getInstance().getDisplayName(player));
		updateTabList(java.util.Collections.singletonList(player));
		com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] onPlayerJoin END for " + player.getGameProfile().getName());
	}
}
