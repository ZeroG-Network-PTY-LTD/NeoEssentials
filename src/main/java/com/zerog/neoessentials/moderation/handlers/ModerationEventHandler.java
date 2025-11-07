package com.zerog.neoessentials.moderation.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.moderation.FreezeManager;
import com.zerog.neoessentials.moderation.JailManager;
import com.zerog.neoessentials.moderation.VanishManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Event handler for moderation system integration
 * Handles freeze interaction blocking and jail restrictions
 */
@EventBusSubscriber(modid = "neoessentials")
public class ModerationEventHandler {
    private static final Map<UUID, BlockPos> lastPlayerPosition = new ConcurrentHashMap<>();

    /**
     * Prevent jailed players from escaping jail radius (movement enforcement)
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationEventHandler.class);

    /**
     * Prevent jailed players from escaping jail radius (movement enforcement)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server == null) return;
        
        // Check all players for jail enforcement
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BlockPos currentPos = player.blockPosition();
            BlockPos lastPos = lastPlayerPosition.get(player.getUUID());
            
            // Update last position
            lastPlayerPosition.put(player.getUUID(), currentPos);
            
            if (lastPos != null && !lastPos.equals(currentPos)) {
                // Player moved, check jail restrictions
                try {
                    JailManager jailManager = JailManager.getInstance();
                    if (!JailManager.isJailSystemEnabled() || !jailManager.isPlayerJailed(player.getUUID())) continue;

                    // Check if player is outside jail bounds
                    if (!jailManager.canPlayerMove(player, currentPos)) {
                        // Teleport player back to jail
                        JailManager.JailEntry jailEntry = jailManager.getJailEntry(player.getUUID());
                        JailManager.JailLocation jailLoc = jailManager.getJailLocation(jailEntry.jailName);
                        if (jailLoc != null) {
                            player.teleportTo(player.serverLevel(), jailLoc.position.getX() + 0.5, jailLoc.position.getY() + 1, jailLoc.position.getZ() + 0.5, player.getYRot(), player.getXRot());
                        }
                        // Send jail escape message
                        String msg = ConfigManager.getInstance().getJailMessageFormat();
                        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(msg));
                    }
                } catch (Exception e) {
                    LOGGER.error("Error enforcing jail movement restriction", e);
                }
            }
        }
    }

    /**
     * Handle player right-click interaction for freeze system
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        try {
            if (!com.zerog.neoessentials.config.ConfigManager.isFreezeSystemEnabled()) return;
            UUID playerId = player.getUUID();
            FreezeManager freezeManager = FreezeManager.getInstance();

            // Cancel interaction if player is frozen
            if (freezeManager.isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Cancel interaction if player is vanished and preventInteraction is enabled (unless staff)
            if (com.zerog.neoessentials.config.ConfigManager.isVanishPreventInteractionEnabled()) {
                VanishManager vanishManager = VanishManager.getInstance();
                if (vanishManager.isPlayerVanished(playerId)) {
                    String seePerm = com.zerog.neoessentials.config.ConfigManager.getInstance().getSeeVanishedPermission();
                    if (!PermissionAPI.hasPermission(playerId, seePerm)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling player interaction for freeze system", e);
        }
    }

    /**
     * Handle block break for frozen/jailed players
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        try {
            if (!com.zerog.neoessentials.config.ConfigManager.isFreezeSystemEnabled()) return;
            UUID playerId = player.getUUID();

            // Prevent frozen players from breaking blocks
            FreezeManager freezeManager = FreezeManager.getInstance();
            if (freezeManager.isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Prevent jailed players from breaking blocks
            JailManager jailManager = JailManager.getInstance();
            if (JailManager.isJailSystemEnabled() && jailManager.isPlayerJailed(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Prevent vanished players from breaking blocks if preventInteraction is enabled (unless staff)
            if (com.zerog.neoessentials.config.ConfigManager.isVanishPreventInteractionEnabled()) {
                VanishManager vanishManager = VanishManager.getInstance();
                if (vanishManager.isPlayerVanished(playerId)) {
                    String seePerm = com.zerog.neoessentials.config.ConfigManager.getInstance().getSeeVanishedPermission();
                    if (!PermissionAPI.hasPermission(playerId, seePerm)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling block break for moderation", e);
        }
    }

    /**
     * Handle block place for frozen/jailed players
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        try {
            if (!com.zerog.neoessentials.config.ConfigManager.isFreezeSystemEnabled()) return;
            UUID playerId = player.getUUID();

            // Prevent frozen players from placing blocks
            FreezeManager freezeManager = FreezeManager.getInstance();
            if (freezeManager.isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Prevent jailed players from placing blocks
            JailManager jailManager = JailManager.getInstance();
            if (JailManager.isJailSystemEnabled() && jailManager.isPlayerJailed(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Prevent vanished players from placing blocks if preventInteraction is enabled (unless staff)
            if (com.zerog.neoessentials.config.ConfigManager.isVanishPreventInteractionEnabled()) {
                VanishManager vanishManager = VanishManager.getInstance();
                if (vanishManager.isPlayerVanished(playerId)) {
                    String seePerm = com.zerog.neoessentials.config.ConfigManager.getInstance().getSeeVanishedPermission();
                    if (!PermissionAPI.hasPermission(playerId, seePerm)) {
                        event.setCanceled(true);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Error handling block place for moderation", e);
        }
    }
}
