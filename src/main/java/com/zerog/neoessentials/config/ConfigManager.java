package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.HashMap;

public class ConfigManager {
		private final ConfigStatus configStatus = new ConfigStatus();

	public void saveAll() {
		LOGGER.info("Saving individual configuration files (no merging)...");
		
		// Save separate config files that load independently
		saveConfig("general.json", createGeneralConfig());
		saveConfig("economy.json", createEconomyConfig());
		saveConfig("shops.json", shopsConfig != null ? shopsConfig : new ShopsConfig());
		saveConfig("teleports.json", createTeleportsConfig());
		saveConfig("tablist.json", createTablistConfig());
		
		LOGGER.info("All configuration files saved as separate JSONs");
	}	public void reloadAll() {
		loadAllConfigurations();
		// Bossbar disable logic on config reload
		try {
			MainConfig mainConfig = getMainConfig();
			if (mainConfig == null || !mainConfig.modules.bossbar) {
				com.zerog.neoessentials.features.CustomBossbarManager.getInstance().shutdown();
				net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
				if (server != null) {
					for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
						com.zerog.neoessentials.features.CustomBossbarManager.getInstance().removeBossbar(player);
					}
				}
			}
		} catch (Exception e) {
			LOGGER.error("Failed to shutdown bossbar manager on config reload", e);
		}
	}

	public String[] getAllConfigFiles() {
		return new String[] {
			// Legacy config files (maintained for backward compatibility)
			"config.json", "commands.json", "permissions.json", "placeholders.json", 
			"customPlaceholders.json", "tablist.json", "scoreboard.json", 
			"animations.json", "settings.json", "shops.json",
			// Phase 2: Consolidated config files
			"core.json", "features.json", "display.json"
		};
	}

	public boolean configExists(String fileName) {
		return configPath.resolve(fileName).toFile().exists();
	}

	public File getConfigFile(String fileName) {
		return configPath.resolve(fileName).toFile();
	}

	public Path getConfigPath() {
		return configPath;
	}

	public ConfigStatus getConfigStatus() {
		return configStatus;
	}
	// Add stub for shutdownHotReload if needed
	public void shutdownHotReload() {
		// No-op stub
	}
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);
	private static ConfigManager instance;
	private final Gson gson;
	private final Path configPath = FMLPaths.CONFIGDIR.get().resolve("neoessentials");

	// Legacy configuration fields (maintained for backward compatibility)
	private MainConfig mainConfig;
	private TablistConfig tablistConfig;
	private UnifiedConfig unifiedConfig;
	private CommandsConfig commandsConfig;
	private PermissionsConfig permissionsConfig;
	private PlaceholdersConfig placeholdersConfig;
	private SettingsConfig settingsConfig;
	private AnimationsConfig animationsConfig;
	private ScoreboardConfig scoreboardConfig;
	private ShopsConfig shopsConfig;
	
	// Phase 2: Consolidated configuration fields
	private CoreConfig coreConfig;
	private FeatureConfig featureConfig;
	private DisplayConfig displayConfig;
		
	// Getters for legacy configs (maintained for backward compatibility)
	public TablistConfig getTablistConfig() { return tablistConfig != null ? tablistConfig : new TablistConfig(); }
	public UnifiedConfig getUnifiedConfig() { return unifiedConfig != null ? unifiedConfig : new UnifiedConfig(); }
	public CommandsConfig getCommandsConfig() { return commandsConfig != null ? commandsConfig : new CommandsConfig(); }
	public PermissionsConfig getPermissionsConfig() { return permissionsConfig != null ? permissionsConfig : new PermissionsConfig(); }
	public PlaceholdersConfig getPlaceholdersConfig() { return placeholdersConfig != null ? placeholdersConfig : new PlaceholdersConfig(); }
	public SettingsConfig getSettingsConfig() { return settingsConfig != null ? settingsConfig : new SettingsConfig(); }
	public AnimationsConfig getAnimationsConfig() { return animationsConfig != null ? animationsConfig : new AnimationsConfig(); }
	public ScoreboardConfig getScoreboardConfig() { return scoreboardConfig != null ? scoreboardConfig : new ScoreboardConfig(); }
	public ShopsConfig getShopsConfig() { return shopsConfig != null ? shopsConfig : new ShopsConfig(); }
	
	// Phase 2: Consolidated configuration getters
	public CoreConfig getCoreConfig() { return coreConfig != null ? coreConfig : new CoreConfig(); }
	public FeatureConfig getFeatureConfig() { return featureConfig != null ? featureConfig : new FeatureConfig(); }
	public DisplayConfig getDisplayConfig() { return displayConfig != null ? displayConfig : new DisplayConfig(); }
	
	/**
	 * Phase 2 Consolidation: Enhanced configuration access patterns
	 * This method demonstrates the improved architecture with 3 logical config groups
	 */
	public void demonstrateConsolidatedConfigAccess() {
		// Core system settings access
		CoreConfig core = getCoreConfig();
		boolean discordEnabled = core.discord.enabled;
		String serverName = core.general.serverName;
		boolean asyncOperations = core.performance.enableAsyncOperations;
		
		// Feature configuration access
		FeatureConfig features = getFeatureConfig();
		boolean economyEnabled = features.economy.enabled;
		double setHomeCost = features.homes.setHomeCost;
		int maxHomesAdmin = features.homes.maxHomesAdmin;
		
		// Display configuration access
		DisplayConfig display = getDisplayConfig();
		boolean tablistEnabled = display.tablist.enabled;
		String scoreboardTitle = display.scoreboard.title;
		boolean animationsEnabled = display.animations.enabled;
		
		LOGGER.info("Phase 2 Consolidated Config System Active - Core: {}, Features: {}, Display: {}", 
			core != null, features != null, display != null);
	}

	private ConfigManager() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();
	}

	public static ConfigManager getInstance() {
		if (instance == null) {
			instance = new ConfigManager();
		}
		return instance;
	}

	   public void initialize() {
		   createConfigDirectories();
		   cleanupConfigDirectory();
		   loadAllConfigurations();
	   }
   /**
	* Remove any files in config/neoessentials/ except our unified config files
	*/
   private void cleanupConfigDirectory() {
	   try {
		   File dir = configPath.toFile();
		   String[] allowed = getAllConfigFiles();
		   File[] files = dir.listFiles();
		   if (files != null) {
			   for (File file : files) {
				   if (file.isFile()) {
					   boolean keep = false;
					   for (String name : allowed) {
						   if (file.getName().equals(name)) {
							   keep = true;
							   break;
						   }
					   }
					   // Always keep specific files
					   if (file.getName().equals("customPlaceholders.json") || 
						   file.getName().endsWith(".json")) {
						   keep = true;
					   }
					   if (!keep) {
						   file.delete();
						   LOGGER.info("Deleted unwanted config file: {}", file.getName());
					   }
				   }
			   }
		   }
	   } catch (Exception e) {
		   LOGGER.error("Failed to clean up config directory", e);
	   }
   }

	private void createConfigDirectories() {
		try {
			Files.createDirectories(configPath);
		} catch (Exception e) {
			LOGGER.error("Failed to create config directories", e);
		}
	}

	private void loadAllConfigurations() {
		// Load from the legacy unified config system (maintained for backward compatibility)
		mainConfig = loadConfig("config.json", MainConfig.class);
		unifiedConfig = loadConfig("config.json", UnifiedConfig.class);
		commandsConfig = loadConfig("commands.json", CommandsConfig.class);
		permissionsConfig = loadConfig("permissions.json", PermissionsConfig.class);
		placeholdersConfig = loadConfig("placeholders.json", PlaceholdersConfig.class);
		settingsConfig = loadConfig("settings.json", SettingsConfig.class);
		animationsConfig = loadConfig("animations.json", AnimationsConfig.class);
		scoreboardConfig = loadConfig("scoreboard.json", ScoreboardConfig.class);
		shopsConfig = loadConfig("shops.json", ShopsConfig.class);
		tablistConfig = loadConfig("tablist.json", TablistConfig.class);
		
		// Phase 2: Load consolidated configs
		coreConfig = loadConfig("core.json", CoreConfig.class);
		featureConfig = loadConfig("features.json", FeatureConfig.class);
		displayConfig = loadConfig("display.json", DisplayConfig.class);
		
		// Load separate configuration files independently
		LOGGER.info("Configuration files loaded separately - no unified config generation");
	}

	/**
	 * Create general configuration
	 */
	private MainConfig createGeneralConfig() {
		if (mainConfig != null) return mainConfig;
		
		MainConfig general = new MainConfig();
		// Set basic server settings
		general.serverName = "NeoEssentials Server";
		general.defaultLanguage = "en";
		general.debugMode = false;
		
		// Disable features we're removing
		if (general.modules == null) general.modules = new MainConfig.Modules();
		general.modules.bossbar = false;
		
		return general;
	}
	
	/**
	 * Create economy configuration with enhancements
	 */
	private Object createEconomyConfig() {
		Map<String, Object> economyConfig = new HashMap<>();
		economyConfig.put("enabled", true);
		economyConfig.put("currencySymbol", "$");
		economyConfig.put("startingBalance", 100.0);
		economyConfig.put("enableTaxes", true);
		economyConfig.put("transactionTaxRate", 0.02); // 2% tax on transactions
		economyConfig.put("payCommandEnabled", true);
		economyConfig.put("balanceCommandEnabled", true);
		economyConfig.put("economyCommandEnabled", true);
		return economyConfig;
	}
	
	/**
	 * Create teleports configuration
	 */
	private Object createTeleportsConfig() {
		Map<String, Object> teleportsConfig = new HashMap<>();
		teleportsConfig.put("homesEnabled", true);
		teleportsConfig.put("warpsEnabled", true);
		teleportsConfig.put("backEnabled", true);
		teleportsConfig.put("spawnEnabled", true);
		teleportsConfig.put("tpEnabled", true);
		teleportsConfig.put("maxHomes", 5);
		teleportsConfig.put("maxWarps", 10);
		teleportsConfig.put("homeCost", 50.0);
		teleportsConfig.put("warpCost", 25.0);
		return teleportsConfig;
	}
	
	/**
	 * Create tablist configuration (keeping tablist, removing scoreboard/bossbar)
	 */
	private TablistConfig createTablistConfig() {
		if (tablistConfig != null) return tablistConfig;
		
		try {
			TablistConfig config = com.zerog.neoessentials.features.TabListManager.createDefaultUnifiedConfigStatic();
			// Disable scoreboard and bossbar sections
			if (config.scoreboard != null) {
				config.scoreboard.enabled = false;
			}
			if (config.bossbar != null) {
				config.bossbar.enabled = false;
			}
			return config;
		} catch (Exception e) {
			LOGGER.warn("Failed to create tablist config, using basic default", e);
			return new TablistConfig();
		}
	}

	/**
	 * Generate all unified configuration files
	 */
	private void generateUnifiedConfigs() {
		try {
			// Generate config.json - main unified configuration
			generateMainConfig();
			
			// Generate commands.json
			generateCommandsConfig();
			
			// Generate permissions.json
			generatePermissionsConfig();
			
			// Generate placeholders.json
			generatePlaceholdersConfig();
			
			// Generate settings.json
			generateSettingsConfig();
			
			// Generate animations.json
			generateAnimationsConfig();
			
			// Generate scoreboard.json
			generateScoreboardConfig();
			
			// Generate shops.json
			generateShopsConfig();
			
			LOGGER.info("All unified configuration files generated successfully!");
		} catch (Exception e) {
			LOGGER.error("Failed to generate unified configuration files", e);
		}
	}

	private void generateMainConfig() {
		String configContent = """
		{
		  "_comment": "Main Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "Unified configuration for all NeoEssentials features with Discord integration support",
		  
		  "general": {
		    "serverName": "NeoEssentials Server",
		    "version": "2.0.0",
		    "language": "en",
		    "enableDebugMode": false,
		    "enableMetrics": true,
		    "enableUpdateChecker": true,
		    "configVersion": "2.0.0"
		  },
		  
		  "modules": {
		    "economy": true,
		    "homes": true,
		    "kits": true,
		    "warps": true,
		    "moderation": true,
		    "chat": true,
		    "tablist": true,
		    "scoreboard": true,
		    "bossbar": true,
		    "teleportation": true,
		    "shops": true,
		    "discord": true
		  },
		  
		  "discord": {
		    "enabled": true,
		    "useSimpleDiscordLink": true,
		    "enhancedIntegration": {
		      "enabled": true,
		      "roleSync": true,
		      "notifications": true,
		      "statusUpdates": true,
		      "chatSync": true
		    }
		  },
		  
		  "integrations": {
		    "ftbTeams": true,
		    "ftbRanks": true,
		    "worldEdit": true,
		    "journeyMap": true,
		    "jei": true
		  },
		  
		  "performance": {
		    "enableAsyncOperations": true,
		    "enableCaching": true,
		    "cacheTimeout": 300,
		    "maxCacheSize": 1000
		  }
		}
		""";
		writeConfigFile("config.json", configContent);
	}

	private void generateCommandsConfig() {
		String configContent = """
		{
		  "_comment": "Command Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "All command configurations with costs, cooldowns, permissions, and Discord integration",
		  
		  "settings": {
		    "enableCosts": true,
		    "enableCooldowns": true,
		    "enableWarmups": true,
		    "enableDiscordLogging": true,
		    "defaultCooldown": 3,
		    "defaultWarmup": 0
		  },
		  
		  "commands": {
		    "heal": {
		      "enabled": true,
		      "cost": 50.0,
		      "cooldown": 30,
		      "warmup": 0,
		      "permission": "neoessentials.heal",
		      "logToDiscord": true
		    },
		    "feed": {
		      "enabled": true,
		      "cost": 25.0,
		      "cooldown": 30,
		      "warmup": 0,
		      "permission": "neoessentials.feed",
		      "logToDiscord": true
		    },
		    "fly": {
		      "enabled": true,
		      "cost": 0.0,
		      "cooldown": 5,
		      "warmup": 0,
		      "permission": "neoessentials.fly",
		      "logToDiscord": true
		    }
		  }
		}
		""";
		writeConfigFile("commands.json", configContent);
	}

	private void generatePermissionsConfig() {
		String configContent = """
		{
		  "_comment": "Permission System Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "8-tier permission system with Discord role mapping and FTB integration",
		  
		  "settings": {
		    "enabled": true,
		    "useDiscordRoles": true,
		    "enableInheritance": true,
		    "enableFTBIntegration": true,
		    "defaultGroup": "default"
		  },
		  
		  "groups": {
		    "owner": {
		      "priority": 1000,
		      "permissions": ["*"],
		      "discordRole": "Owner",
		      "prefix": "&4[OWNER]&r",
		      "suffix": "",
		      "inheritance": []
		    },
		    "admin": {
		      "priority": 800,
		      "permissions": ["neoessentials.*"],
		      "discordRole": "Admin",
		      "prefix": "&c[ADMIN]&r",
		      "suffix": "",
		      "inheritance": ["moderator"]
		    },
		    "default": {
		      "priority": 0,
		      "permissions": ["neoessentials.basic"],
		      "discordRole": null,
		      "prefix": "&7[PLAYER]&r",
		      "suffix": "",
		      "inheritance": []
		    }
		  }
		}
		""";
		writeConfigFile("permissions.json", configContent);
	}

	private void generatePlaceholdersConfig() {
		String configContent = """
		{
		  "_comment": "Placeholder System Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "50+ placeholders including Discord integration and FTB support",
		  
		  "settings": {
		    "enabled": true,
		    "refreshInterval": 20,
		    "enableCaching": true,
		    "enableDiscordPlaceholders": true,
		    "enableFTBPlaceholders": true
		  },
		  
		  "placeholders": {
		    "server_name": "NeoEssentials Server",
		    "server_players": "Online Players Count",
		    "server_max_players": "Maximum Players",
		    "server_tps": "Server TPS",
		    "player_name": "Player Name",
		    "player_health": "Player Health",
		    "player_ping": "Player Ping",
		    "discord_members": "Discord Member Count",
		    "discord_online": "Discord Online Count",
		    "ftb_team_name": "FTB Team Name",
		    "ftb_rank_name": "FTB Rank Name"
		  }
		}
		""";
		writeConfigFile("placeholders.json", configContent);
	}

	private void generateSettingsConfig() {
		String configContent = """
		{
		  "_comment": "General Settings Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "General settings and feature toggles for NeoEssentials mod",
		  
		  "general": {
		    "pluginName": "NeoEssentials",
		    "version": "2.0.0",
		    "language": "en",
		    "enableDebugMode": false,
		    "enableMetrics": true,
		    "enableUpdateChecker": true
		  },
		  
		  "features": {
		    "tablist": {
		      "enabled": true,
		      "useUnifiedConfig": true,
		      "enableAnimations": true,
		      "updateInterval": 20
		    },
		    "discord": {
		      "enabled": true,
		      "useSimpleDiscordLink": true,
		      "enableEnhancedIntegration": true,
		      "enableRoleSync": true
		    }
		  }
		}
		""";
		writeConfigFile("settings.json", configContent);
	}

	private void generateAnimationsConfig() {
		String configContent = """
		{
		  "_comment": "Animation Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "Defines animated text sequences for tablist, scoreboard, bossbar, and other features",
		  
		  "global": {
		    "enabled": true,
		    "defaultSpeed": 20,
		    "enableRainbowColors": true,
		    "enableGradients": true
		  },
		  
		  "animations": [
		    {
		      "name": "rainbow_welcome",
		      "type": "rainbow",
		      "text": "Welcome to NeoEssentials Server!",
		      "speed": 20,
		      "colors": ["&c", "&6", "&e", "&a", "&b", "&9", "&d"],
		      "loop": true
		    }
		  ]
		}
		""";
		writeConfigFile("animations.json", configContent);
	}

	private void generateScoreboardConfig() {
		String configContent = """
		{
		  "_comment": "Enhanced Admin Scoreboard Configuration for NeoEssentials",
		  "_description": "Comprehensive scoreboard system with permission-based layouts, animations, and admin controls",
		  "_version": "2.0.0",
		  
		  "scoreboard": {
		    "enabled": true,
		    "updateInterval": 20,
		    "maxLines": 15,
		    "title": "&6&lNeoEssentials Server",
		    "enableAnimations": true,
		    "enablePlaceholders": true,
		    "enableConditionalDisplay": true,
		    
		    "titleAnimation": {
		      "enabled": true,
		      "frames": [
		        "&6&lNeoEssentials Server",
		        "&e&lNeoEssentials Server",
		        "&f&lNeoEssentials Server",
		        "&e&lNeoEssentials Server"
		      ],
		      "duration": 2.0,
		      "loop": true
		    },
		    
		    "adminSettings": {
		      "allowPlayerToggle": true,
		      "debugMode": false,
		      "logUpdates": false,
		      "maxUpdateFrequency": 5,
		      "enableAutoReload": true,
		      "adminCommand": "/neoessentials scoreboard"
		    },
		    
		    "layouts": [
		      {
		        "priority": 1000,
		        "conditionType": "permission",
		        "condition": "neoessentials.scoreboard.owner",
		        "title": "&c&lOWNER PANEL",
		        "enabled": true,
		        "description": "Server owner administrative panel with full server info",
		        "lines": [
		          "&c&m─────────────────────",
		          "&c&l● OWNER PANEL ●",
		          "&c&m─────────────────────",
		          "",
		          "&f▶ &7Server: &a{server_name}",
		          "&f▶ &7TPS: &e{server_tps}",
		          "&f▶ &7RAM: &b{server_memory_used}&7/&b{server_memory_max}",
		          "&f▶ &7Players: &e{server_players}&7/&e{server_max_players}",
		          "",
		          "&f▶ &7Admin Level: &cOWNER",
		          "&f▶ &7Permissions: &aALL",
		          "&f▶ &7Balance: &6${player_balance}",
		          "",
		          "&f▶ &7Coords: &f{player_x}&7, &f{player_y}&7, &f{player_z}",
		          "&c&m─────────────────────"
		        ]
		      },
		      {
		        "priority": 800,
		        "conditionType": "permission",
		        "condition": "neoessentials.scoreboard.admin",
		        "title": "&6&lADMIN PANEL",
		        "enabled": true,
		        "description": "Server administrator panel with management info",
		        "lines": [
		          "&6&m─────────────────────",
		          "&6&l● ADMIN PANEL ●",
		          "&6&m─────────────────────",
		          "",
		          "&f▶ &7Player: &f{player_name}",
		          "&f▶ &7Rank: &6ADMIN",
		          "&f▶ &7Balance: &6${player_balance}",
		          "&f▶ &7Playtime: &e{player_playtime}",
		          "",
		          "&f▶ &7Server Info:",
		          "&f  &7TPS: &a{server_tps}",
		          "&f  &7Players: &e{server_players}&7/&e{server_max_players}",
		          "&f  &7Uptime: &b{server_uptime}",
		          "",
		          "&6&m─────────────────────"
		        ]
		      },
		      {
		        "priority": 600,
		        "conditionType": "permission",
		        "condition": "neoessentials.scoreboard.moderator",
		        "title": "&e&lMODERATOR",
		        "enabled": true,
		        "description": "Moderator panel with player management info",
		        "lines": [
		          "&e&m─────────────────────",
		          "&e&l● MODERATOR PANEL ●",
		          "&e&m─────────────────────",
		          "",
		          "&f▶ &7Player: &f{player_name}",
		          "&f▶ &7Rank: &eMODERATOR",
		          "&f▶ &7Health: &c{player_health}&7/&c{player_max_health}",
		          "&f▶ &7Level: &a{player_level}",
		          "",
		          "&f▶ &7Online Players: &e{server_players}",
		          "&f▶ &7World: &b{player_world}",
		          "&f▶ &7Location: &f{player_x}&7, &f{player_z}",
		          "",
		          "&e&m─────────────────────"
		        ]
		      },
		      {
		        "priority": 400,
		        "conditionType": "permission",
		        "condition": "neoessentials.scoreboard.vip",
		        "title": "&d&lVIP PLAYER",
		        "enabled": true,
		        "description": "VIP member panel with enhanced features",
		        "lines": [
		          "&d&m─────────────────────",
		          "&d&l● VIP PANEL ●",
		          "&d&m─────────────────────",
		          "",
		          "&f▶ &7Player: &d{player_name}",
		          "&f▶ &7Status: &dVIP MEMBER",
		          "&f▶ &7Balance: &6${player_balance}",
		          "&f▶ &7Homes: &b{player_homes}&7/&b{player_max_homes}",
		          "",
		          "&f▶ &7Health: &c{player_health}",
		          "&f▶ &7Experience: &a{player_exp}",
		          "&f▶ &7Playtime: &e{player_playtime}",
		          "",
		          "&d&m─────────────────────"
		        ]
		      },
		      {
		        "priority": 1,
		        "conditionType": "default",
		        "condition": "",
		        "title": "&7&lPLAYER INFO",
		        "enabled": true,
		        "description": "Default player information panel",
		        "lines": [
		          "&7&m─────────────────────",
		          "&7&l● PLAYER INFO ●",
		          "&7&m─────────────────────",
		          "",
		          "&f▶ &7Player: &f{player_name}",
		          "&f▶ &7Level: &a{player_level}",
		          "&f▶ &7Health: &c{player_health}&7/&c20",
		          "&f▶ &7Food: &6{player_food}&7/&620",
		          "",
		          "&f▶ &7Location:",
		          "&f  &7World: &e{player_world}",
		          "&f  &7X: &e{player_x} &7Y: &e{player_y} &7Z: &e{player_z}",
		          "",
		          "&f▶ &7Online: &e{server_players}&7/&e{server_max_players}",
		          "&7&m─────────────────────"
		        ]
		      }
		    ]
		  }
		}
		""";
		writeConfigFile("scoreboard.json", configContent);
	}

	private void generateShopsConfig() {
		String configContent = """
		{
		  "_comment": "Shop System Configuration for NeoEssentials",
		  "_version": "2.0.0",
		  "_description": "Configuration for player shops, admin shops, and economy integration",
		  
		  "general": {
		    "enabled": true,
		    "allowAdminShops": true,
		    "allowPlayerShops": true,
		    "defaultTaxRate": 0.00
		  },
		  
		  "discord": {
		    "enabled": true,
		    "notifications": {
		      "shopCreated": {
		        "enabled": true,
		        "channel": "general"
		      }
		    }
		  }
		}
		""";
		writeConfigFile("shops.json", configContent);
	}

	private void writeConfigFile(String fileName, String content) {
		try {
			File configFile = configPath.resolve(fileName).toFile();
			if (!configFile.exists()) {
				try (FileWriter writer = new FileWriter(configFile)) {
					writer.write(content);
				}
				LOGGER.info("Generated configuration file: {}", fileName);
			}
		} catch (IOException e) {
			LOGGER.error("Failed to write configuration file: {}", fileName, e);
		}
	}



	private <T> T loadConfig(String fileName, Class<T> configClass) {
		File configFile = configPath.resolve(fileName).toFile();
		if (!configFile.exists()) {
			T defaultConfig = createDefaultConfig(configClass);
			saveConfig(fileName, defaultConfig);
			return defaultConfig;
		}
		try (FileReader reader = new FileReader(configFile)) {
			return gson.fromJson(reader, configClass);
		} catch (Exception e) {
			LOGGER.error("Failed to load configuration: {}", fileName, e);
			return createDefaultConfig(configClass);
		}
	}

	private <T> void saveConfig(String fileName, T config) {
		try (FileWriter writer = new FileWriter(configPath.resolve(fileName).toFile())) {
			gson.toJson(config, writer);
		} catch (IOException e) {
			LOGGER.error("Failed to save configuration: {}", fileName, e);
		}
	}

	private <T> T createDefaultConfig(Class<T> configClass) {
		try {
			return configClass.getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			LOGGER.error("Failed to create default configuration for: {}", configClass.getSimpleName(), e);
			return null;
		}
	}

	// Getters
	public MainConfig getMainConfig() { 
		// Use unified config as the main config source
		if (unifiedConfig != null) {
			// Convert UnifiedConfig to MainConfig format for backward compatibility
			MainConfig mainConfig = new MainConfig();
			if (unifiedConfig.modules != null) {
				mainConfig.modules = new MainConfig.Modules();
				mainConfig.modules.chat = unifiedConfig.modules.chat;
				mainConfig.modules.economy = unifiedConfig.modules.economy;
				mainConfig.modules.bossbar = unifiedConfig.modules.bossbar;
				mainConfig.modules.tablist = unifiedConfig.modules.tablist;
			}
			return mainConfig;
		}
		return mainConfig != null ? mainConfig : new MainConfig(); 
	}
	// Bossbar, scoreboard, animation config getters if present
}

	// ...existing code...
