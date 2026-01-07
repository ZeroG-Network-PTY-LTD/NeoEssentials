package com.zerog.neoessentials.chat;

import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.api.permissions.PermissionAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Badge and Icon Manager - Phase 3
 *
 * Handles:
 * - Rank badges (👑⭐🛡️💎)
 * - Status icons (💤👻📺🔇)
 * - Custom badge images from filesystem
 */
public class BadgeManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BadgeManager.class);
    private static volatile BadgeManager instance;

    // Cache of loaded custom badge image paths
    private final Map<String, File> customBadgeFiles = new ConcurrentHashMap<>();
    private boolean customImagesLoaded = false;

    private BadgeManager() {}

    public static BadgeManager getInstance() {
        if (instance == null) {
            synchronized (BadgeManager.class) {
                if (instance == null) {
                    instance = new BadgeManager();
                }
            }
        }
        return instance;
    }

    /**
     * Get rank badge for a player based on their primary group.
     * Supports Unicode emojis and custom image assets from filesystem.
     */
    public String getRankBadge(ServerPlayer player) {
        if (!isBadgesEnabled()) {
            return "";
        }

        try {
            String group = getPrimaryGroup(player);
            if (group == null || group.isEmpty()) {
                return "";
            }

            String groupLower = group.toLowerCase();

            // Check if custom badge image exists for this rank
            // Note: Currently just checks if file exists, actual rendering requires resource pack
            if (isCustomImagesEnabled() && hasCustomBadgeImage(groupLower)) {
                // Custom badge exists - for now, show a marker
                // In future, this will integrate with resource pack system
                LOGGER.debug("Custom badge image found for rank: {}", groupLower);
            }

            // Use emoji badges from config (fallback or primary depending on setup)
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("rankBadges")) {
                    var rankBadges = badges.getAsJsonObject("rankBadges");
                    if (rankBadges.has(groupLower)) {
                        return rankBadges.get(groupLower).getAsString() + " ";
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting rank badge: {}", e.getMessage());
        }

        return "";
    }

    /**
     * Check if custom images are enabled in config.
     */
    private boolean isCustomImagesEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("useCustomImages")) {
                    return badges.get("useCustomImages").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    /**
     * Get status icons for a player (AFK, vanished, etc.).
     */
    public String getStatusIcons(ServerPlayer player) {
        if (!isBadgesEnabled() || !isStatusIconsEnabled()) {
            return "";
        }

        StringBuilder icons = new StringBuilder();

        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("statusIcons")) {
                    var statusIcons = badges.getAsJsonObject("statusIcons");

                    // Check AFK
                    if (isPlayerAfk(player) && statusIcons.has("afk")) {
                        icons.append(statusIcons.get("afk").getAsString());
                    }

                    // Check Vanished
                    if (isPlayerVanished(player) && statusIcons.has("vanished")) {
                        if (!icons.isEmpty()) icons.append(" ");
                        icons.append(statusIcons.get("vanished").getAsString());
                    }

                    // Check Muted
                    if (isPlayerMuted(player) && statusIcons.has("muted")) {
                        if (!icons.isEmpty()) icons.append(" ");
                        icons.append(statusIcons.get("muted").getAsString());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Error getting status icons: {}", e.getMessage());
        }

        return !icons.isEmpty() ? icons + " " : "";
    }

    /**
     * Apply badges and icons to a chat format template.
     */
    public String applyBadgesAndIcons(ServerPlayer player, String template) {
        if (!isBadgesEnabled()) {
            return template;
        }

        String result = template;

        try {
            String badgePosition = getBadgePosition();
            String iconPosition = getIconPosition();

            String rankBadge = getRankBadge(player);
            String statusIcons = getStatusIcons(player);

            // Apply rank badge
            if (!rankBadge.isEmpty()) {
                switch (badgePosition) {
                    case "before_prefix":
                        result = rankBadge + result;
                        break;
                    case "after_prefix":
                        result = result.replace("{neoessentials_prefix}", "{neoessentials_prefix}" + rankBadge);
                        break;
                    case "before_name":
                        result = result.replace("{neoessentials_username}", rankBadge + "{neoessentials_username}");
                        result = result.replace("{neoessentials_name}", rankBadge + "{neoessentials_name}");
                        result = result.replace("{neoessentials_displayname}", rankBadge + "{neoessentials_displayname}");
                        break;
                    case "after_name":
                        result = result.replace("{neoessentials_username}", "{neoessentials_username}" + rankBadge);
                        result = result.replace("{neoessentials_name}", "{neoessentials_name}" + rankBadge);
                        result = result.replace("{neoessentials_displayname}", "{neoessentials_displayname}" + rankBadge);
                        break;
                }
            }

            // Apply status icons
            if (!statusIcons.isEmpty()) {
                switch (iconPosition) {
                    case "before_name":
                        result = result.replace("{neoessentials_username}", statusIcons + "{neoessentials_username}");
                        result = result.replace("{neoessentials_name}", statusIcons + "{neoessentials_name}");
                        result = result.replace("{neoessentials_displayname}", statusIcons + "{neoessentials_displayname}");
                        break;
                    case "after_name":
                        result = result.replace("{neoessentials_username}", "{neoessentials_username}" + statusIcons);
                        result = result.replace("{neoessentials_name}", "{neoessentials_name}" + statusIcons);
                        result = result.replace("{neoessentials_displayname}", "{neoessentials_displayname}" + statusIcons);
                        break;
                    case "after_message":
                        result = result + " " + statusIcons;
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error applying badges and icons: {}", e.getMessage());
        }

        return result;
    }

    /**
     * Load custom badge images from config/neoessentials/badges/ folder.
     */
    public void loadCustomBadgeImages() {
        if (!isCustomImagesEnabled() || customImagesLoaded) {
            return;
        }

        try {
            String badgePath = getCustomImagePath();
            File badgeDir = new File(badgePath);

            // Create directory if it doesn't exist
            if (!badgeDir.exists()) {
                badgeDir.mkdirs();
                LOGGER.info("Created custom badge images directory at: {}", badgeDir.getAbsolutePath());
                createReadmeFile(badgeDir);
                customImagesLoaded = true;
                return;
            }

            // Scan for PNG files
            File[] imageFiles = badgeDir.listFiles((dir, name) -> name.toLowerCase().endsWith(".png"));

            if (imageFiles == null || imageFiles.length == 0) {
                LOGGER.warn("No badge images found in {}. Place PNG files named after ranks (e.g., admin.png, vip.png)", badgePath);
                createReadmeFile(badgeDir);
                customImagesLoaded = true;
                return;
            }

            LOGGER.info("Found {} custom badge images in {}", imageFiles.length, badgePath);

            for (File imageFile : imageFiles) {
                String rankName = imageFile.getName().replace(".png", "").toLowerCase();
                customBadgeFiles.put(rankName, imageFile);
                LOGGER.debug("Registered custom badge image for rank: {}", rankName);
            }

            LOGGER.info("Successfully registered {} custom badge images", customBadgeFiles.size());
            customImagesLoaded = true;

        } catch (Exception e) {
            LOGGER.error("Error loading custom badge images: {}", e.getMessage(), e);
        }
    }

    /**
     * Create README.txt in badges directory.
     */
    private void createReadmeFile(File badgeDir) {
        try {
            File readmeFile = new File(badgeDir, "README.txt");
            if (!readmeFile.exists()) {
                String readme = """
                    ╔══════════════════════════════════════════════════════════╗
                    ║           NeoEssentials Custom Badge Images              ║
                    ╚══════════════════════════════════════════════════════════╝
                    
                    Place your custom badge PNG images in this folder!
                    
                    HOW TO USE:
                    -----------
                    1. Create or find PNG images for your ranks
                       - Recommended size: 16x16 or 32x32 pixels
                       - Use transparency (alpha channel) for best results
                       - Keep file size small (under 50KB)
                    
                    2. Name the files after your ranks (lowercase):
                       - admin.png
                       - moderator.png
                       - vip.png
                       - helper.png
                       - builder.png
                       - owner.png
                       - etc.
                    
                    3. Enable custom images in config:
                       Edit: config/neoessentials/config.json
                       Set: "useCustomImages": true
                    
                    4. Restart the server
                    
                    EXAMPLE STRUCTURE:
                    ------------------
                    config/neoessentials/badges/
                    ├── admin.png       (Crown image)
                    ├── moderator.png   (Shield image)
                    ├── vip.png         (Diamond image)
                    ├── helper.png      (Wrench image)
                    └── README.txt      (This file)
                    
                    TIPS:
                    -----
                    • Use bright colors for visibility in chat
                    • Keep designs simple and recognizable
                    • Add a 1-2px outline for better contrast
                    • Test on both light and dark backgrounds
                    • File names must match your LuckPerms group names
                    
                    IMPORTANT NOTES:
                    ----------------
                    • This feature requires clients to have a compatible resource pack
                    • The mod will use Unicode emoji badges as fallback
                    • Custom images work best with 16x16 or 32x32 pixel sizes
                    • PNG format with transparency is recommended
                    
                    For more information and resource pack setup:
                    See: docs/CUSTOM_BADGES.md
                    
                    ══════════════════════════════════════════════════════════
                    """;

                Files.writeString(readmeFile.toPath(), readme);
                LOGGER.info("Created README.txt in badges directory");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to create README file: {}", e.getMessage());
        }
    }

    /**
     * Get custom image path from config.
     */
    private String getCustomImagePath() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("customImagePath")) {
                    return badges.get("customImagePath").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "config/neoessentials/badges";
    }

    /**
     * Check if a rank has a custom badge image file.
     */
    public boolean hasCustomBadgeImage(String rankName) {
        return customBadgeFiles.containsKey(rankName.toLowerCase());
    }

    // Helper methods

    private boolean isBadgesEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                return chatConfig.getAsJsonObject("badges").get("enabled").getAsBoolean();
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private boolean isStatusIconsEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("statusIcons")) {
                    return badges.getAsJsonObject("statusIcons").get("enabled").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return true;
    }

    private String getBadgePosition() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                return chatConfig.getAsJsonObject("badges").get("badgePosition").getAsString();
            }
        } catch (Exception e) {
            // Ignore
        }
        return "before_prefix";
    }

    private String getIconPosition() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("statusIcons")) {
                    return badges.getAsJsonObject("statusIcons").get("iconPosition").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "after_name";
    }

    private String getPrimaryGroup(ServerPlayer player) {
        try {
            // Try to get from PermissionAPI
            var permManager = PermissionAPI.getManager();
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

    private boolean isPlayerAfk(ServerPlayer player) {
        try {
            var afkManager = com.zerog.neoessentials.chat.AfkManager.getInstance();
            return afkManager.isAfk(player);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPlayerVanished(ServerPlayer player) {
        try {
            var vanishManager = com.zerog.neoessentials.moderation.VanishManager.getInstance();
            return vanishManager.isPlayerVanished(player.getUUID());
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isPlayerMuted(ServerPlayer player) {
        try {
            return com.zerog.neoessentials.chat.MuteManager.isMuted(player);
        } catch (Exception e) {
            return false;
        }
    }
}




