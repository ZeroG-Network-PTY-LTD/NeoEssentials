package com.zerog.neoessentials.chat.handlers;

import com.zerog.neoessentials.api.ChatAPI;
import com.zerog.neoessentials.api.PlaceholderAPI;
import com.zerog.neoessentials.chat.ChatManager;
import com.zerog.neoessentials.integrations.ChatIntegrationManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for custom player join and quit messages.
 * Manages displaying customized join/quit messages based on server configuration.
 */
@EventBusSubscriber(modid = "neoessentials")
public class PlayerJoinQuitHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerJoinQuitHandler.class);

    /**
     * Handles player join events and displays custom join messages.
     * This event fires when a player successfully joins the server.
     * 
     * @param event The PlayerLoggedInEvent containing the joining player
     */
    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        // --- Vanish-on-join logic for staff ---
        try {
            com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();
            if (config.isVanishSystemEnabled() && config.isVanishOnJoinEnabled()) {
                // Check if player has staff vanish permission
                java.util.UUID playerUuid = player.getUUID();
                String playerName = player.getName().getString();
                boolean hasVanishPerm = com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerUuid, "neoessentials.moderation.vanish");
                if (hasVanishPerm) {
                    com.zerog.neoessentials.moderation.VanishManager vanishManager = com.zerog.neoessentials.moderation.VanishManager.getInstance();
                    if (!vanishManager.isPlayerVanished(playerUuid)) {
                        vanishManager.vanishPlayer(playerUuid, playerName, "AutoVanishOnJoin", true);
                    }
                }
            }
        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error handling vanish-on-join for player " + player.getName().getString(), e);
        }

        // --- newPlayerKit logic: Give kit on first join if enabled ---
        try {
            com.zerog.neoessentials.config.ConfigManager config = com.zerog.neoessentials.config.ConfigManager.getInstance();
            if (config.isNewPlayerKitEnabled()) {
                String kitName = config.getNewPlayerKitName();
                if (kitName != null && !kitName.trim().isEmpty()) {
                    // Use ResourceUtil for a consistent data path on all server hosts
                    java.io.File firstJoinFile = com.zerog.neoessentials.util.ResourceUtil.getDataFile("first_joined.json");
                    java.util.Set<java.util.UUID> joined = new java.util.HashSet<>();
                    if (firstJoinFile.exists()) {
                        try (java.io.FileReader r = new java.io.FileReader(firstJoinFile)) {
                            com.google.gson.JsonArray arr = com.google.gson.JsonParser.parseReader(r).getAsJsonArray();
                            for (com.google.gson.JsonElement el : arr) {
                                try {
                                    joined.add(java.util.UUID.fromString(el.getAsString()));
                                } catch (Exception e) {
                                    NeoLog.debug(LOGGER, LogCategory.CHAT, "Skipping malformed UUID entry in first_joined.json", e);
                                }
                            }
                        } catch (Exception e) {
                            NeoLog.warn(LOGGER, LogCategory.CHAT, "Failed to read first_joined.json, treating as empty: {}", e.getMessage());
                        }
                    }
                    boolean isFirstJoin = !joined.contains(player.getUUID());
                    if (isFirstJoin) {
                        // Give kit items directly, bypassing permission/cooldown checks for the starter kit
                        com.zerog.neoessentials.kits.KitManager kitManager = com.zerog.neoessentials.kits.KitManager.getInstance();
                        com.zerog.neoessentials.kits.Kit starterKit = kitManager.getKit(kitName);
                        if (starterKit != null) {
                            net.minecraft.world.entity.player.Inventory inv = player.getInventory();
                            for (net.minecraft.world.item.ItemStack item : starterKit.getItems()) {
                                if (item.isEmpty()) continue;
                                if (!inv.add(item.copy())) {
                                    player.drop(item.copy(), false);
                                }
                            }
                        }
                        // Add to joined set and save
                        joined.add(player.getUUID());
                        try {
                            java.io.File parent = firstJoinFile.getParentFile();
                            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                                NeoLog.warn(LOGGER, LogCategory.CHAT, "PlayerJoinQuitHandler: failed to create parent directory: {}", parent.getAbsolutePath());
                            }
                        } catch (Exception e) {
                            NeoLog.debug(LOGGER, LogCategory.CHAT, "Error preparing first_joined.json parent directory", e);
                        }
                        try (java.io.FileWriter w = new java.io.FileWriter(firstJoinFile, false)) {
                            com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
                            for (java.util.UUID id : joined) arr.add(id.toString());
                            w.write(arr.toString());
                        } catch (Exception e) {
                            NeoLog.warn(LOGGER, LogCategory.CHAT, "Failed to persist first_joined.json: {}", e.getMessage());
                        }
                        // Optionally, send a message to the player
                        player.sendSystemMessage(MessageUtil.component("commands.neoessentials.kits.starter_kit_received"));
                    }
                    // --- spawnOnJoin logic: Teleport to spawn on first join if enabled ---
                    try {
                        // Check spawnOnJoin config (teleportation.spawnSettings.spawnOnJoin)
                        boolean spawnOnJoin = false;
                        com.google.gson.JsonObject mainConfig = config.getConfig(com.zerog.neoessentials.config.ConfigManager.MAIN_CONFIG);
                        if (mainConfig.has("teleportation")) {
                            com.google.gson.JsonObject tp = mainConfig.getAsJsonObject("teleportation");
                            if (tp.has("spawnSettings")) {
                                com.google.gson.JsonObject spawnSettings = tp.getAsJsonObject("spawnSettings");
                                if (spawnSettings.has("spawnOnJoin")) {
                                    spawnOnJoin = spawnSettings.get("spawnOnJoin").getAsBoolean();
                                }
                            }
                        }
                        if (isFirstJoin && spawnOnJoin) {
                            com.zerog.neoessentials.teleportation.Spawn.SpawnManager.getInstance().teleportToSpawn(player);
                        }
                    } catch (Exception e) {
                        NeoLog.error(LOGGER, LogCategory.CHAT, "Error handling spawnOnJoin for player " + player.getName().getString(), e);
                    }
                }
            }
        } catch (Exception e) {
            // Log but do not interrupt join
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error handling newPlayerKit for player " + player.getName().getString(), e);
        }

        try {
            // Get the ChatManager instance
            ChatManager chatManager = ChatAPI.getChatManager();
            if (chatManager == null) {
                NeoLog.warn(LOGGER, LogCategory.CHAT, "ChatManager not available, using default join messages");
                return;
            }

            // Get custom join message from config
            String customJoinMessage = chatManager.getCustomJoinMessage();
            
            // Only apply custom message if configured (not "none")
            if (customJoinMessage != null && !customJoinMessage.equals("none") && !customJoinMessage.trim().isEmpty()) {
                // Cancel the default join message by setting it to null
                // Note: This doesn't cancel the event, just modifies the message
                
                // Format the custom message with placeholders using PlaceholderAPI
                String resolvedMessage = PlaceholderAPI.setPlaceholders(player, customJoinMessage);
                
                // Convert color codes and create component
                String coloredMessage = resolvedMessage.replaceAll("&([0-9a-fk-or])", "§$1");
                Component formattedMessage = Component.literal(coloredMessage);
                
                // Broadcast the custom join message to all players
                var server = player.getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
                }
                
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Displayed custom join message for player {}: {}",
                    player.getName().getString(), formattedMessage.getString());
            } else {
                // Use default join message behavior
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Using default join message for player {}", player.getName().getString());
            }

            // Notify chat integrations about the join
            ChatIntegrationManager.broadcastPlayerJoin(player);

            // Mail login notification (Essentials: notify of unread mail on join)
            try {
                com.zerog.neoessentials.util.commands.MailCommand.notifyOnLogin(player);
            } catch (Exception e) {
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Could not send mail notification to " + player.getName().getString(), e);
            }

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error handling join event for player " + player.getName().getString(), e);
        }
    }

    /**
     * Handles player quit events and displays custom quit messages.
     * This event fires when a player disconnects from the server.
     * 
     * @param event The PlayerLoggedOutEvent containing the leaving player
     */
    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        com.zerog.neoessentials.inventory.InventoryViewCommands.releaseEditLocks(player.getUUID());

        try {
            // Get the ChatManager instance
            ChatManager chatManager = ChatAPI.getChatManager();
            if (chatManager == null) {
                NeoLog.warn(LOGGER, LogCategory.CHAT, "ChatManager not available, using default quit messages");
                return;
            }

            // Get custom quit message from config
            String customQuitMessage = chatManager.getCustomQuitMessage();
            
            // Only apply custom message if configured (not "none")
            if (customQuitMessage != null && !customQuitMessage.equals("none") && !customQuitMessage.trim().isEmpty()) {
                // Format the custom message with placeholders using PlaceholderAPI
                String resolvedMessage = PlaceholderAPI.setPlaceholders(player, customQuitMessage);
                
                // Convert color codes and create component
                String coloredMessage = resolvedMessage.replaceAll("&([0-9a-fk-or])", "§$1");
                Component formattedMessage = Component.literal(coloredMessage);
                
                // Broadcast the custom quit message to all players
                var server = player.getServer();
                if (server != null) {
                    server.getPlayerList().broadcastSystemMessage(formattedMessage, false);
                }
                
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Displayed custom quit message for player {}: {}",
                    player.getName().getString(), formattedMessage.getString());
            } else {
                // Use default quit message behavior
                NeoLog.debug(LOGGER, LogCategory.CHAT, "Using default quit message for player {}", player.getName().getString());
            }

            // Notify chat integrations about the quit
            ChatIntegrationManager.broadcastPlayerQuit(player);

        } catch (Exception e) {
            NeoLog.error(LOGGER, LogCategory.CHAT, "Error handling quit event for player " + player.getName().getString(), e);
        }
    }
}