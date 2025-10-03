package com.zerog.neoessentials.util;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized resource path management for NeoEssentials
 * Provides consistent paths for configuration, data, and JAR resources
 */
public class ResourceUtil {
    
    // Standard Minecraft server directory structure
    public static final String CONFIG_DIR = "config/neoessentials/";
    public static final String DATA_DIR = "neoessentials/"; // Server runtime data
    
    // JAR resource paths (internal mod resources)
    public static final String JAR_CONFIG_PATH = "/data/config/neoessentials/";
    public static final String JAR_LANG_PATH = "/data/lang/";
    public static final String JAR_ASSETS_PATH = "/assets/neoessentials/";
    
    /**
     * Get a configuration file path (stored in config/neoessentials/)
     */
    public static File getConfigFile(String filename) {
        return new File(CONFIG_DIR + filename);
    }
    
    /**
     * Get a data file path (stored in neoessentials/ for runtime data)
     */
    public static File getDataFile(String filename) {
        return new File(DATA_DIR + filename);
    }
    
    /**
     * Get a Path for configuration files
     */
    public static Path getConfigPath(String filename) {
        return Paths.get(CONFIG_DIR + filename);
    }
    
    /**
     * Get a Path for data files
     */
    public static Path getDataPath(String filename) {
        return Paths.get(DATA_DIR + filename);
    }
    
    /**
     * Get InputStream for JAR configuration resource
     */
    public static InputStream getJarConfigResource(String filename) {
        return ResourceUtil.class.getResourceAsStream(JAR_CONFIG_PATH + filename);
    }
    
    /**
     * Get InputStream for JAR language resource
     */
    public static InputStream getJarLangResource(String filename) {
        return ResourceUtil.class.getResourceAsStream(JAR_LANG_PATH + filename);
    }
    
    /**
     * Get InputStream for JAR asset resource
     */
    public static InputStream getJarAssetResource(String filename) {
        return ResourceUtil.class.getResourceAsStream(JAR_ASSETS_PATH + filename);
    }
    
    /**
     * Ensure a directory exists
     */
    public static void ensureDirectoryExists(String dirPath) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    /**
     * Ensure the config directory exists
     */
    public static void ensureConfigDirectory() {
        ensureDirectoryExists(CONFIG_DIR);
    }
    
    /**
     * Ensure the data directory exists
     */
    public static void ensureDataDirectory() {
        ensureDirectoryExists(DATA_DIR);
    }
    
    /**
     * Get standard language file (tries server directory first, then JAR)
     */
    public static File getLanguageFile(String locale) {
        return getDataFile("lang/" + locale + ".json");
    }
    
    /**
     * Get JAR language resource stream
     */
    public static InputStream getJarLanguageResource(String locale) {
        return getJarLangResource(locale + ".json");
    }
}