package com.zerog.neoessentials.moderation.handlers;

import com.zerog.neoessentials.moderation.FreezeManager;
import com.zerog.neoessentials.moderation.JailManager;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Event handler for moderation system integration
 * Handles freeze interaction blocking and jail restrictions
 */
@EventBusSubscriber(modid = "neoessentials")
public class ModerationEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ModerationEventHandler.class);

    /**
     * Handle player right-click interaction for freeze system
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRightClick(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        try {
            UUID playerId = player.getUUID();
            FreezeManager freezeManager = FreezeManager.getInstance();

            // Cancel interaction if player is frozen
            if (freezeManager.isPlayerFrozen(playerId)) {
                event.setCanceled(true);
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
            UUID playerId = player.getUUID();

            // Prevent frozen players from breaking blocks
            FreezeManager freezeManager = FreezeManager.getInstance();
            if (freezeManager.isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Prevent jailed players from breaking blocks
            JailManager jailManager = JailManager.getInstance();
            if (jailManager.isPlayerJailed(playerId)) {
                event.setCanceled(true);
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
            UUID playerId = player.getUUID();

            // Prevent frozen players from placing blocks
            FreezeManager freezeManager = FreezeManager.getInstance();
            if (freezeManager.isPlayerFrozen(playerId)) {
                event.setCanceled(true);
                return;
            }

            // Prevent jailed players from placing blocks
            JailManager jailManager = JailManager.getInstance();
            if (jailManager.isPlayerJailed(playerId)) {
                event.setCanceled(true);
            }
        } catch (Exception e) {
            LOGGER.error("Error handling block place for moderation", e);
        }
    }
}
