package com.zerog.neoessentials.tags;

import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PlayerTagManager - Handles player tags/badges for above-head display only.
 * This system is decoupled from chat and is intended for visual tags (e.g., icons, images, text) above player heads.
when i have /home  * Chat badges are handled in BadgeManager.
 *
 * Migration notes:
 * - Above-head badge/tag logic is now here (was in BadgeManager).
 * - Config option: badges.aboveHeadTagsEnabled (see chat config)
 * - See README for migration details.
 */
public class PlayerTagManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerTagManager.class);
    private static volatile PlayerTagManager instance;

    // Cache of loaded custom tag image paths (key: tag name, value: file)
    private final Map<String, File> customTagFiles = new ConcurrentHashMap<>();
    private boolean customImagesLoaded = false;

    private PlayerTagManager() {}

    public static PlayerTagManager getInstance() {
        if (instance == null) {
            synchronized (PlayerTagManager.class) {
                if (instance == null) {
                    instance = new PlayerTagManager();
                }
            }
        }
        return instance;
    }

    /**
     * Get the tag for a player (for above-head display).
     * This can be an emoji, text, or custom image, based on config/permissions.
     * Only used for above-head display, not chat.
     */
    public String getPlayerTag(ServerPlayer player) {
        if (!isAboveHeadTagsEnabled()) {
            return "";
        }
        // Placeholder: return group name for now
        return getPrimaryGroup(player);
    }

    private String getPrimaryGroup(ServerPlayer player) {
        try {
            var permManager = com.zerog.neoessentials.api.permissions.PermissionAPI.getManager();
            if (permManager != null) {
                var user = permManager.getUser(player.getUUID());
                if (user != null) {
                    return user.getGroup();
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting primary group: {}", e.getMessage());
        }
        return "default";
    }

    /**
     * Load custom tag images from the configured assets directory.
     * Scans for .png, .jpg, .jpeg, .gif files and populates customTagFiles.
     * Tag name is the file name (without extension).
     */
    public void loadCustomTagImages(File assetsDir) {
        customTagFiles.clear();
        if (assetsDir == null || !assetsDir.exists() || !assetsDir.isDirectory()) {
            LOGGER.warn("PlayerTagManager: Provided assetsDir is invalid: {}", assetsDir);
            customImagesLoaded = false;
            return;
        }
        File[] files = assetsDir.listFiles((dir, name) -> name.matches("[a-zA-Z0-9_-]+\\.(png|jpg|jpeg|gif)"));
        if (files != null) {
            for (File file : files) {
                String tagName = file.getName().replaceFirst("\\.[^.]+$", "");
                customTagFiles.put(tagName, file);
                LOGGER.debug("Loaded custom tag image: {} -> {}", tagName, file.getAbsolutePath());
            }
        }
        customImagesLoaded = true;
        LOGGER.info("PlayerTagManager: Loaded {} custom tag images from {}", customTagFiles.size(), assetsDir.getAbsolutePath());
    }

    /**
     * Reload custom tag images from the assets directory.
     * Useful for hot-reloading without server restart.
     */
    public void reloadCustomTagImages(File assetsDir) {
        loadCustomTagImages(assetsDir);
    }

    /**
     * Get all available tag names (for admin UI, tab completion, etc).
     */
    public java.util.Set<String> getAvailableTagNames() {
        return customTagFiles.keySet();
    }

    /**
     * Get the custom image file for a tag, if available.
     */
    public File getCustomTagFile(String tagName) {
        return customTagFiles.get(tagName);
    }

    /**
     * Check if above-head tags are enabled in config.
     */
    public boolean isAboveHeadTagsEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("aboveHeadTagsEnabled")) {
                    return badges.get("aboveHeadTagsEnabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    // ...future expansion: methods for updating tags, reloading config, etc.
}
