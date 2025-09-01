package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;

public class ConfigManager {
		private final ConfigStatus configStatus = new ConfigStatus();

	public void saveAll() {
		saveConfig("config.json", unifiedConfig != null ? unifiedConfig : new UnifiedConfig());
		saveConfig("commands.json", commandsConfig != null ? commandsConfig : new CommandsConfig());
		saveConfig("permissions.json", permissionsConfig != null ? permissionsConfig : new PermissionsConfig());
		saveConfig("placeholders.json", placeholdersConfig != null ? placeholdersConfig : new PlaceholdersConfig());
		saveConfig("settings.json", settingsConfig != null ? settingsConfig : new SettingsConfig());
		saveConfig("animations.json", animationsConfig != null ? animationsConfig : new AnimationsConfig());
		saveConfig("scoreboard.json", scoreboardConfig != null ? scoreboardConfig : new ScoreboardConfig());
		saveConfig("shops.json", shopsConfig != null ? shopsConfig : new ShopsConfig());
		// Use TabListManager to create tablist config with comprehensive examples
		if (tablistConfig == null || (tablistConfig.tablist != null && tablistConfig.tablist.layouts.isEmpty())) {
			// Get the comprehensive default configuration from TabListManager
			try {
				LOGGER.info("Generating comprehensive tablist configuration with multi-line examples...");
				tablistConfig = com.zerog.neoessentials.features.TabListManager.createDefaultUnifiedConfigStatic();
			} catch (Exception e) {
				LOGGER.warn("Failed to create comprehensive tablist config, using basic default", e);
				tablistConfig = new TablistConfig();
			}
		}
		saveConfig("tablist.json", tablistConfig);
		
		// Generate all the unified config files from our templates
		generateUnifiedConfigs();
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
			"config.json", "commands.json", "permissions.json", "placeholders.json", 
			"customPlaceholders.json", "tablist.json", "scoreboard.json", 
			"animations.json", "settings.json", "shops.json"
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
		
		// Getters for new configs
		public TablistConfig getTablistConfig() { return tablistConfig != null ? tablistConfig : new TablistConfig(); }
		public UnifiedConfig getUnifiedConfig() { return unifiedConfig != null ? unifiedConfig : new UnifiedConfig(); }
		public CommandsConfig getCommandsConfig() { return commandsConfig != null ? commandsConfig : new CommandsConfig(); }
		public PermissionsConfig getPermissionsConfig() { return permissionsConfig != null ? permissionsConfig : new PermissionsConfig(); }
		public PlaceholdersConfig getPlaceholdersConfig() { return placeholdersConfig != null ? placeholdersConfig : new PlaceholdersConfig(); }
		public SettingsConfig getSettingsConfig() { return settingsConfig != null ? settingsConfig : new SettingsConfig(); }
		public AnimationsConfig getAnimationsConfig() { return animationsConfig != null ? animationsConfig : new AnimationsConfig(); }
		public ScoreboardConfig getScoreboardConfig() { return scoreboardConfig != null ? scoreboardConfig : new ScoreboardConfig(); }
		public ShopsConfig getShopsConfig() { return shopsConfig != null ? shopsConfig : new ShopsConfig(); }

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
		// Load from the new unified config system
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
		
		// Unified configuration system - no legacy bridge needed
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
		  "_comment": "Dedicated Scoreboard Configuration",
		  "_description": "Advanced scoreboard configuration with multiline support, animations, and conditional displays",
		  
		  "scoreboard": {
		    "enabled": true,
		    "updateInterval": 20,
		    "maxLines": 15,
		    "title": "&6&lNeoEssentials Server",
		    
		    "layouts": [
		      {
		        "priority": 1,
		        "conditionType": "default",
		        "title": "&7&lPLAYER INFO",
		        "lines": [
		          "&7Player: &f{player_name}",
		          "&7Health: &c{player_health}",
		          "&7Online: &e{server_players}"
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
