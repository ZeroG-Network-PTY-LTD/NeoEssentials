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
		saveConfig("main.json", mainConfig);
		// Bossbar, scoreboard, animation configs if present
	}

	public void reloadAll() {
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
			"main.json", "tablist.json" // Bossbar, scoreboard, animation configs if present
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
	// Bossbar, scoreboard, animation configs if present

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
	// Bossbar, scoreboard, animation configs if present
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
	// Bossbar, scoreboard, animation config getters if present
}
