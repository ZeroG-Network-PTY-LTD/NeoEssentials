package com.zerog.neoessentials.resourcepack;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resource Pack Manager - Automatically sends badge resource pack to players
 *
 * Handles:
 * - Sending resource pack to players on join
 * - Tracking pack application status
 * - Fallback to emoji badges if pack declined
 */
@EventBusSubscriber(modid = "neoessentials", bus = EventBusSubscriber.Bus.GAME)
public class ResourcePackManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePackManager.class);
    private static volatile ResourcePackManager instance;

    private String resourcePackUrl = null;
    private String resourcePackHash = null;
    private boolean autoSendEnabled = false;

    private ResourcePackManager() {}

    public static ResourcePackManager getInstance() {
        if (instance == null) {
            synchronized (ResourcePackManager.class) {
                if (instance == null) {
                    instance = new ResourcePackManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initialize resource pack system.
     * Generates pack if needed and sets up URL.
     */
    public void initialize() {
        try {
            if (!isAutoSendEnabled()) {
                LOGGER.info("Auto-send resource pack is disabled in config");
                return;
            }

            // Check if we should generate pack
            if (shouldGeneratePack()) {
                LOGGER.info("Generating badge resource pack...");
                Path packPath = ResourcePackGenerator.generateResourcePack();

                if (packPath != null) {
                    // Load SHA-1
                    loadResourcePackInfo(packPath);
                    autoSendEnabled = true;
                    LOGGER.info("Resource pack system initialized successfully");
                } else {
                    LOGGER.warn("Failed to generate resource pack - will use emoji badges");
                    autoSendEnabled = false;
                }
            } else {
                LOGGER.info("Resource pack generation skipped (custom images not enabled)");
            }

        } catch (Exception e) {
            LOGGER.error("Failed to initialize resource pack system: {}", e.getMessage(), e);
            autoSendEnabled = false;
        }
    }

    /**
     * Load resource pack URL and hash.
     */
    private void loadResourcePackInfo(Path packPath) throws Exception {
        // Get configured URL or generate local URL
        String configuredUrl = getConfiguredPackUrl();

        if (configuredUrl != null && !configuredUrl.isEmpty()) {
            resourcePackUrl = configuredUrl;
            LOGGER.info("Using configured resource pack URL: {}", resourcePackUrl);
        } else {
            // Use local file path (requires players to download separately)
            // In production, you'd host this on a web server
            resourcePackUrl = packPath.toAbsolutePath().toString();
            LOGGER.warn("No resource pack URL configured. Pack generated at: {}", resourcePackUrl);
            LOGGER.warn("To use auto-send, upload pack to a web server and set 'resourcePackUrl' in config");
        }

        // Load SHA-1 hash
        Path sha1File = Paths.get("config/neoessentials/NeoEssentials-Badges.sha1");
        if (Files.exists(sha1File)) {
            resourcePackHash = Files.readString(sha1File).trim();
            LOGGER.info("Loaded resource pack SHA-1: {}", resourcePackHash);
        }
    }

    /**
     * Send resource pack to a player.
     */
    public void sendResourcePack(ServerPlayer player) {
        if (!autoSendEnabled || resourcePackUrl == null) {
            return;
        }

        try {
            boolean required = isPackRequired();

            Component prompt = Component.literal(getPackPrompt());

            if (resourcePackHash != null) {
                player.sendTexturePack(resourcePackUrl, resourcePackHash, required, prompt);
            } else {
                player.sendTexturePack(resourcePackUrl, "", required, prompt);
            }

            LOGGER.debug("Sent resource pack to player: {}", player.getName().getString());

        } catch (Exception e) {
            LOGGER.error("Failed to send resource pack to {}: {}", player.getName().getString(), e.getMessage());
        }
    }

    /**
     * Handle player login - send resource pack.
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Send resource pack after a short delay
            player.getServer().execute(() -> {
                try {
                    Thread.sleep(1000); // 1 second delay
                    getInstance().sendResourcePack(player);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
    }

    // Config helper methods

    private boolean isAutoSendEnabled() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("autoSendResourcePack")) {
                    return badges.get("autoSendResourcePack").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private boolean shouldGeneratePack() {
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

    private boolean isPackRequired() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("requireResourcePack")) {
                    return badges.get("requireResourcePack").getAsBoolean();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return false;
    }

    private String getConfiguredPackUrl() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("resourcePackUrl")) {
                    return badges.get("resourcePackUrl").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return null;
    }

    private String getPackPrompt() {
        try {
            var chatConfig = com.zerog.neoessentials.config.ConfigManager.getInstance().getConfig("chat");
            if (chatConfig.has("badges")) {
                var badges = chatConfig.getAsJsonObject("badges");
                if (badges.has("resourcePackPrompt")) {
                    return badges.get("resourcePackPrompt").getAsString();
                }
            }
        } catch (Exception e) {
            // Ignore
        }
        return "This server uses custom badge images. Please accept the resource pack for the best experience!";
    }
}

