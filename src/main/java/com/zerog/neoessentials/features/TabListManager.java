
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
		if (config != null && config.tablist.layouts != null) {
			for (com.zerog.neoessentials.config.TablistConfig.Layout layout : config.tablist.layouts) {
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
				// Create new unified configuration
				config = createDefaultUnifiedConfig();
				try (java.io.FileWriter writer = new java.io.FileWriter(configFile)) {
					gson.toJson(config, writer);
				}
				com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Generated default unified tablist.json configuration.");
			} else {
				try (java.io.FileReader reader = new java.io.FileReader(configFile)) {
					config = gson.fromJson(reader, com.zerog.neoessentials.config.TablistConfig.class);
					
					// Migration: Convert legacy tablistLayouts to new tablist.layouts format
					if (config.tablist.layouts.isEmpty() && config.tablist.layouts != null && !config.tablist.layouts.isEmpty()) {
						// Migration already handled by new structure
						com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Using new unified tablist format.");
					}
					
					// Ensure default values for new sections
					if (config.tablist == null) config.tablist = new com.zerog.neoessentials.config.TablistConfig.TablistSection();
					if (config.scoreboard == null) config.scoreboard = new com.zerog.neoessentials.config.TablistConfig.ScoreboardSection();
					if (config.bossbar == null) config.bossbar = new com.zerog.neoessentials.config.TablistConfig.BossbarSection();
					if (config.animations == null) config.animations = new com.zerog.neoessentials.config.TablistConfig.AnimationSection();
				}
				com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Loaded tablist configuration from " + configPath);
			}
		} catch (Exception e) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("[TabListManager] Error loading tablist config: " + e.getMessage());
			config = createDefaultUnifiedConfig();
		}
	}
	
	/**
	 * Create default unified configuration with FTB integration examples
	 */
	/**
	 * Static version of createDefaultUnifiedConfig for use by ConfigManager
	 */
	public static com.zerog.neoessentials.config.TablistConfig createDefaultUnifiedConfigStatic() {
		return createDefaultUnifiedConfigInternal();
	}
	
	private com.zerog.neoessentials.config.TablistConfig createDefaultUnifiedConfig() {
		return createDefaultUnifiedConfigInternal();
	}
	
	private static com.zerog.neoessentials.config.TablistConfig createDefaultUnifiedConfigInternal() {
		com.zerog.neoessentials.config.TablistConfig defaultConfig = new com.zerog.neoessentials.config.TablistConfig();
		
		// Configure tablist section
		defaultConfig.tablist.enabled = true;
		defaultConfig.tablist.updateInterval = 20;
		defaultConfig.tablist.format = "{ftb_combined_prefix}[{team_name}] {player_name}{ftb_combined_suffix}";
		
		// Add default tablist layout - Simple example
		com.zerog.neoessentials.config.TablistConfig.Layout defaultLayout = new com.zerog.neoessentials.config.TablistConfig.Layout();
		defaultLayout.priority = 1;
		defaultLayout.conditionType = "default";
		defaultLayout.header = java.util.Arrays.asList(
			"&6&l╔═══════════════════════════════════╗",
			"&6&l║         &f&lNeoEssentials         &6&l║",
			"&6&l║ &7Welcome &e{player_name}           &6&l║",
			"&6&l╚═══════════════════════════════════╝"
		);
		defaultLayout.footer = java.util.Arrays.asList(
			"&6&l╔═══════════════════════════════════╗",
			"&6&l║ &7Online: &e{server_players}&7/&e{server_max_players}              &6&l║",
			"&6&l║ &7Time: &f{time}                   &6&l║",
			"&6&l╚═══════════════════════════════════╝"
		);
		defaultConfig.tablist.layouts.add(defaultLayout);

		// Add comprehensive multi-line example for VIP players
		com.zerog.neoessentials.config.TablistConfig.Layout vipLayout = new com.zerog.neoessentials.config.TablistConfig.Layout();
		vipLayout.priority = 500;
		vipLayout.conditionType = "permission";
		vipLayout.condition = "neoessentials.vip";
		vipLayout.header = java.util.Arrays.asList(
			"&d&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
			"&5&l✦             &d&lVIP NEOESSENTIALS SERVER            &5&l✦",
			"&d&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
			"",
			"&f🎮 &7Player: &d&l{player_name} &5[VIP]",
			"&f💎 &7Rank: &e{ftb_rank_display_name} &7| &bTeam: &3{ftb_team_display_name}",
			"&f❤️ &7Health: &c{player_health}&7/&c{player_max_health} &7| &f🍖 Food: &6{player_food}",
			"&f📍 &7Location: &a{player_x}&7, &a{player_y}&7, &a{player_z} &7in &e{player_world}",
			"&f⚡ &7Ping: &{ping_colored}{player_ping}ms &7| &fLevel: &a{player_level}",
			"",
			"&f🌐 &7Server Info:",
			"&f└─ &7Players Online: &a{server_players}&7/&a{server_max_players}",
			"&f└─ &7Server TPS: &{server_tps > 18 ? '&a' : server_tps > 15 ? '&e' : '&c'}{server_tps}",
			"&f└─ &7Memory Usage: &b{server_memory_percent}%",
			"",
			"&d&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
		);
		vipLayout.footer = java.util.Arrays.asList(
			"&d&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━",
			"",
			"&f🔗 &d&lLINKS &7&m▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬",
			"&f├─ &7Website: &fwww.neoessentials.com",
			"&f├─ &7Discord: &9discord.gg/neoessentials",
			"&f├─ &7Store: &6store.neoessentials.com",
			"&f└─ &7Wiki: &ewiki.neoessentials.com",
			"",
			"&f⏰ &7Current Time: &f{datetime} &7| &fUptime: &a{server_uptime}",
			"&f🎯 &7Today's Goal: &6{daily_goal} &7| &fProgress: &e{goal_progress}%",
			"",
			"&d&l━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
		);
		defaultConfig.tablist.layouts.add(vipLayout);
		
		// Add player ordering
		com.zerog.neoessentials.config.TablistConfig.PlayerOrder orderByRank = new com.zerog.neoessentials.config.TablistConfig.PlayerOrder();
		orderByRank.placeholder = "ftb_rank_weight";
		orderByRank.direction = "desc";
		orderByRank.asNumber = true;
		defaultConfig.tablist.playerOrder.add(orderByRank);
		
		com.zerog.neoessentials.config.TablistConfig.PlayerOrder orderByPing = new com.zerog.neoessentials.config.TablistConfig.PlayerOrder();
		orderByPing.placeholder = "ping";
		orderByPing.direction = "asc";
		orderByPing.asNumber = true;
		defaultConfig.tablist.playerOrder.add(orderByPing);
		
		// Configure scoreboard section
		defaultConfig.scoreboard.enabled = true;
		defaultConfig.scoreboard.updateInterval = 20;
		defaultConfig.scoreboard.title = "&6&lNeoEssentials";
		
		// Add default scoreboard layout
		com.zerog.neoessentials.config.TablistConfig.Layout scoreboardLayout = new com.zerog.neoessentials.config.TablistConfig.Layout();
		scoreboardLayout.priority = 1;
		scoreboardLayout.conditionType = "default";
		scoreboardLayout.title = "&e&lPLAYER INFO";
		scoreboardLayout.lines = java.util.Arrays.asList(
			"&7&m─────────────────",
			"&e&lPLAYER INFO",
			"&7&m─────────────────",
			"&f● &7Player: &e{player_name}",
			"&f● &7Level: &a{player_level}",
			"&f● &7Health: &c{player_health}&7/&c{player_max_health}",
			"&f● &7Food: &6{player_food}",
			"",
			"&f● &7Team: &b{ftb_team_display_name}",
			"&f● &7Rank: &a{ftb_rank_display_name}",
			"",
			"&f● &7Online: &e{server_players}&7/&e{server_max_players}",
			"&f● &7Time: &f{time}",
			"&7&m─────────────────"
		);
		defaultConfig.scoreboard.layouts.add(scoreboardLayout);
		
		// Configure bossbar section
		defaultConfig.bossbar.enabled = true;
		defaultConfig.bossbar.updateInterval = 20;
		
		// Add default bossbar layout
		com.zerog.neoessentials.config.TablistConfig.BossbarLayout bossbarLayout = new com.zerog.neoessentials.config.TablistConfig.BossbarLayout();
		bossbarLayout.priority = 1;
		bossbarLayout.conditionType = "default";
		
		com.zerog.neoessentials.config.TablistConfig.BossbarInfo bossbarInfo = new com.zerog.neoessentials.config.TablistConfig.BossbarInfo();
		bossbarInfo.id = "default";
		bossbarInfo.text = "&7Welcome &f{player_name} &7| &eOnline: {server_players}/{server_max_players} &7| &fTime: {time}";
		bossbarInfo.color = "YELLOW";
		bossbarInfo.style = "PROGRESS";
		bossbarInfo.progress = 1.0;
		bossbarLayout.bars.add(bossbarInfo);
		defaultConfig.bossbar.layouts.add(bossbarLayout);
		
		return defaultConfig;
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
		
		// Use new unified configuration structure or fall back to legacy
		boolean useNewConfig = config.tablist != null && config.tablist.enabled && 
			config.tablist.layouts != null && !config.tablist.layouts.isEmpty();
		
		if (useNewConfig) {
			updateHeaderFooterUnified(player, viewState);
		} else {
			updateHeaderFooterLegacy(player, viewState);
		}
	}
	
	/**
	 * Update header/footer using new unified configuration
	 */
	private void updateHeaderFooterUnified(ServerPlayer player, TabViewState viewState) {
		// Find matching layout using new unified structure
		com.zerog.neoessentials.config.TablistConfig.Layout matchedLayout = findMatchingLayout(player, config.tablist.layouts);

		List<String> headerLines = (matchedLayout != null && matchedLayout.header != null && !matchedLayout.header.isEmpty()) 
			? matchedLayout.header : java.util.Arrays.asList("&6&lNeoEssentials Server");
		List<String> footerLines = (matchedLayout != null && matchedLayout.footer != null && !matchedLayout.footer.isEmpty()) 
			? matchedLayout.footer : java.util.Arrays.asList("&7Powered by NeoEssentials");

		// Process placeholders in header and footer
		String processedHeader = processMultilineContent(headerLines, player);
		String processedFooter = processMultilineContent(footerLines, player);

		// Update header/footer if changed
		if (!processedHeader.equals(viewState.lastHeader) || !processedFooter.equals(viewState.lastFooter)) {
			viewState.lastHeader = processedHeader;
			viewState.lastFooter = processedFooter;
			
			net.minecraft.server.MinecraftServer server = player.getServer();
			if (server != null) {
				server.execute(() -> {
					player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(
						net.minecraft.network.chat.Component.literal(processedHeader),
						net.minecraft.network.chat.Component.literal(processedFooter)
					));
				});
			}
		}
	}
	
	/**
	 * Update header/footer using legacy configuration (backward compatibility)
	 */
	@SuppressWarnings("deprecation")
	private void updateHeaderFooterLegacy(ServerPlayer player, TabViewState viewState) {
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
			net.minecraft.server.MinecraftServer server = player.getServer();
			if (server != null) {
				server.execute(() -> {
					player.connection.send(new net.minecraft.network.protocol.game.ClientboundTabListPacket(headerComponent, footerComponent));
				});
			}
			viewState.lastSentHeader = headerComponent;
			viewState.lastSentFooter = footerComponent;
		}
	}

	public void updatePlayerEntry(ServerPlayer player) {
		boolean isEnabled = (config.tablist != null && config.tablist.enabled);
		if (config == null || !isEnabled) {
			com.zerog.neoessentials.util.DebugUtil.debugLog("Tablist is disabled in config, skipping updatePlayerEntry for " + player.getName().getString());
			return;
		}
		
		try {
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

			// Enhanced Discord integration
			com.zerog.neoessentials.integration.DiscordIntegrationManager discordMgr = 
				com.zerog.neoessentials.integration.DiscordIntegrationManager.getInstance();
			
			if (discordMgr.isEnabled()) {
				Map<String, Object> data = new HashMap<>();
				data.put("layout_name", "unified_tablist");
				data.put("update_type", "player_entry");
				data.put("prefix", prefix != null ? prefix : "");
				discordMgr.sendEnrichedNotification("tablist_update", player, data);
			}

		} catch (Exception e) {
			com.zerog.neoessentials.util.DebugUtil.errorLog("[TabListManager] Error updating player entry: " + e.getMessage());
		}
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
	
	/**
	 * Find the highest priority matching layout for a player
	 */
	private com.zerog.neoessentials.config.TablistConfig.Layout findMatchingLayout(ServerPlayer player, 
			java.util.List<com.zerog.neoessentials.config.TablistConfig.Layout> layouts) {
		
		com.zerog.neoessentials.config.TablistConfig.Layout bestMatch = null;
		int highestPriority = -1;

		for (com.zerog.neoessentials.config.TablistConfig.Layout layout : layouts) {
			if (layout.priority <= highestPriority) continue;

			boolean matches = false;
			String conditionType = layout.conditionType != null ? layout.conditionType : "default";
			String condition = layout.condition != null ? layout.condition : "";

			switch (conditionType.toLowerCase()) {
				case "permission":
					matches = hasPermission(player, condition);
					break;
				case "placeholder":
					matches = checkPlaceholderCondition(player, condition);
					break;
				case "default":
				default:
					matches = true;
					break;
			}

			if (matches) {
				bestMatch = layout;
				highestPriority = layout.priority;
			}
		}

		return bestMatch;
	}
	
	/**
	 * Process multiline content with placeholder replacement
	 */
	private String processMultilineContent(java.util.List<String> lines, ServerPlayer player) {
		if (lines == null || lines.isEmpty()) return "";
		
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i > 0) result.append("\n");
			String processedLine = placeholderManager.processPlaceholders(lines.get(i), player);
			result.append(processedLine);
		}
		return result.toString();
	}
	
	/**
	 * Check placeholder-based condition (e.g., "ftb_team_role:Owner")
	 */
	private boolean checkPlaceholderCondition(ServerPlayer player, String condition) {
		if (condition == null || !condition.contains(":")) return false;
		
		String[] parts = condition.split(":", 2);
		String placeholderName = parts[0];
		String expectedValue = parts[1];
		
		String actualValue = placeholderManager.processPlaceholders("{" + placeholderName + "}", player);
		
		return expectedValue.equals(actualValue);
	}
	
	/**
	 * Check if player has permission
	 */
	private boolean hasPermission(ServerPlayer player, String permission) {
		com.zerog.neoessentials.permissions.CustomPermissionsManager permMgr = 
			com.zerog.neoessentials.permissions.CustomPermissionsManager.getInstance();
		return permMgr.hasPermission(player, permission);
	}

	/**
	 * Notify Discord of tablist changes
	 */
	public void notifyDiscordTablistUpdate(ServerPlayer player, String updateType) {
		try {
			com.zerog.neoessentials.integration.DiscordIntegrationManager discordMgr = 
				com.zerog.neoessentials.integration.DiscordIntegrationManager.getInstance();
			
			if (discordMgr.isEnabled()) {
				Map<String, Object> data = new HashMap<>();
				data.put("layout_name", "unified_tablist");
				data.put("update_type", updateType);
				data.put("viewer_count", tabViewStates.size());
				discordMgr.sendEnrichedNotification("tablist_update", player, data);
			}

		} catch (Exception e) {
			com.zerog.neoessentials.util.DebugUtil.errorLog("[TabListManager] Error notifying Discord: " + e.getMessage());
		}
	}

	/**
	 * Enhanced refresh with Discord integration
	 */
	public void refreshTablistWithDiscordNotification(Collection<ServerPlayer> players, String reason) {
		refreshTablistForAll(players);

		// Notify Discord for each player
		for (ServerPlayer player : players) {
			notifyDiscordTablistUpdate(player, reason);
		}
	}
	
	/**
	 * TabViewState class to track individual player's tablist state
	 */
	public static class TabViewState {
		public final Map<UUID, EntryState> lastEntries = new HashMap<>();
		public String lastHeader = "";
		public String lastFooter = "";
		public Component lastSentHeader = null;
		public Component lastSentFooter = null;
		
		/**
		 * EntryState class to track individual entry states
		 */
		public static class EntryState {
			public final String displayName;
			public final String teamId;
			public final int latencyBucket;
			
			public EntryState(String displayName, String teamId, int latencyBucket) {
				this.displayName = displayName;
				this.teamId = teamId;
				this.latencyBucket = latencyBucket;
			}
		}
	}
}
