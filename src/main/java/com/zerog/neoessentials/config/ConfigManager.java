package com.zerog.neoessentials.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigManager {
	private final ConfigStatus configStatus = new ConfigStatus();
	public void saveAll() {
		saveConfig("main.json", mainConfig);
		saveConfig("economy.json", economyConfig);
		saveConfig("homes.json", homeConfig);
		saveConfig("kits.json", kitConfig);
		saveConfig("warps.json", warpConfig);
		saveConfig("moderation.json", moderationConfig);
		saveConfig("messaging.json", messagingConfig);
		saveConfig("chat.json", chatConfig);
		saveConfig("tablist.json", tablistConfig);
		saveConfig("spawn.json", spawnConfig);
	}

	public void reloadAll() {
		loadAllConfigurations();
	}

	public String[] getAllConfigFiles() {
		return new String[] {
			"main.json", "economy.json", "homes.json", "kits.json", "warps.json",
			"moderation.json", "messaging.json", "chat.json", "tablist.json", "spawn.json"
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
	private final Map<String, Object> configCache = new ConcurrentHashMap<>();

	private MainConfig mainConfig;
	private EconomyConfig economyConfig;
	private HomeConfig homeConfig;
	private KitConfig kitConfig;
	private WarpConfig warpConfig;
	private ModerationConfig moderationConfig;
	private MessagingConfig messagingConfig;
	private ChatConfig chatConfig;
	private TablistConfig tablistConfig;
	private SpawnConfig spawnConfig;

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
		loadAllConfigurations();
	}

	private void createConfigDirectories() {
		try {
			Files.createDirectories(configPath);
		} catch (Exception e) {
			LOGGER.error("Failed to create config directories", e);
		}
	}

	private void loadAllConfigurations() {
		mainConfig = loadConfig("main.json", MainConfig.class);
		economyConfig = loadConfig("economy.json", EconomyConfig.class);
		homeConfig = loadConfig("homes.json", HomeConfig.class);
		kitConfig = loadConfig("kits.json", KitConfig.class);
		warpConfig = loadConfig("warps.json", WarpConfig.class);
		moderationConfig = loadConfig("moderation.json", ModerationConfig.class);
		messagingConfig = loadConfig("messaging.json", MessagingConfig.class);
		chatConfig = loadConfig("chat.json", ChatConfig.class);
		tablistConfig = loadConfig("tablist.json", TablistConfig.class);
		spawnConfig = loadConfig("spawn.json", SpawnConfig.class);
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
	public MainConfig getMainConfig() { return mainConfig != null ? mainConfig : new MainConfig(); }
	public EconomyConfig getEconomyConfig() { return economyConfig != null ? economyConfig : new EconomyConfig(); }
	public HomeConfig getHomeConfig() { return homeConfig != null ? homeConfig : new HomeConfig(); }
	public KitConfig getKitConfig() { return kitConfig != null ? kitConfig : new KitConfig(); }
	public WarpConfig getWarpConfig() { return warpConfig != null ? warpConfig : new WarpConfig(); }
	public ModerationConfig getModerationConfig() { return moderationConfig != null ? moderationConfig : new ModerationConfig(); }
	public MessagingConfig getMessagingConfig() { return messagingConfig != null ? messagingConfig : new MessagingConfig(); }
	public ChatConfig getChatConfig() { return chatConfig != null ? chatConfig : new ChatConfig(); }
	public TablistConfig getTablistConfig() { return tablistConfig != null ? tablistConfig : new TablistConfig(); }
	public SpawnConfig getSpawnConfig() { return spawnConfig != null ? spawnConfig : new SpawnConfig(); }
}
