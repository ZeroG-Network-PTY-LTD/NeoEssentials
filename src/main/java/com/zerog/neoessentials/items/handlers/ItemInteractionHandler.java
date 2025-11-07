package com.zerog.neoessentials.items.handlers;

import com.zerog.neoessentials.items.commands.PowertoolCommand;
import com.zerog.neoessentials.items.commands.PowertoolToggleCommand;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.UUID;

/**
 * Handles item interaction events for powertool functionality.
 * When a player right-clicks with an item that has a powertool command assigned,
 * this handler executes the stored command.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ItemInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemInteractionHandler.class);

    /**
     * Handle right-click item events for powertool execution.
     */
    @SubscribeEvent
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            UUID playerUUID = player.getUUID();
            int slot = player.getInventory().selected;

            // Check if player has powertool data
            if (!PowertoolCommand.hasPowertoolData(playerUUID)) {
                return;
            }

            // Check if this slot has a powertool command assigned
            String command = PowertoolCommand.getPowertoolCommand(playerUUID, slot);
            if (command == null || command.trim().isEmpty()) {
                return;
            }

            // Check if powertool is enabled for this slot
            if (!PowertoolToggleCommand.isPowertoolEnabled(playerUUID, slot)) {
                return;
            }

            // Check permission to use powertools
            if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(playerUUID, "neoessentials.item.powertool")) {
                return;
            }

            // Execute the command
            event.setCanceled(true); // Prevent normal item use
            
            try {
                // Execute command as the player
                player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    command.startsWith("/") ? command.substring(1) : command
                );
                
                LOGGER.debug("Executed powertool command '{}' for player {}", command, player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Failed to execute powertool command '{}' for player {}", command, player.getName().getString(), e);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.powertool.execution_failed"));
            }

        } catch (Exception e) {
            LOGGER.error("Error in powertool item interaction handler", e);
        }
    }
}