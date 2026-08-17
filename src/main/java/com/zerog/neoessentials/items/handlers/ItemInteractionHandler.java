package com.zerog.neoessentials.items.handlers;

import com.zerog.neoessentials.items.commands.PowertoolCommand;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

import java.util.UUID;

/**
 * Handles item interaction events for powertool functionality.
 * Fires on RightClickItem (air), RightClickBlock, and RightClickEmpty
 * so powertools work regardless of what the player clicks on.
 */
@EventBusSubscriber(modid = "neoessentials")
public class ItemInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemInteractionHandler.class);

    /** Right-click in air */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        handlePowertool(event);
    }

    /** Right-click on a block */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        handlePowertool(event);
    }

    /** Right-click on empty (some edge cases) */
    @SubscribeEvent
    public static void onRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        handlePowertool(event);
    }

    /**
     * Core powertool handler — checks item type, global toggle, and permission,
     * then executes the bound command.
     */
    private static void handlePowertool(PlayerInteractEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        try {
            UUID playerUUID = player.getUUID();

            // Only continue if this player has any powertool bindings
            if (!PowertoolCommand.hasPowertoolData(playerUUID)) {
                return;
            }

            // Must be holding an item
            ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) {
                return;
            }

            // Look up by item TYPE (not slot)
            Identifier itemKey = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
            String itemId = itemKey.toString();

            String command = PowertoolCommand.getPowertoolCommand(playerUUID, itemId);
            if (command == null || command.isBlank()) {
                return;
            }

            // Global per-player toggle
            if (!PowertoolCommand.isPowertoolEnabled(playerUUID)) {
                return;
            }

            // Permission check
            if (!com.zerog.neoessentials.api.permissions.PermissionAPI
                    .hasPermission(playerUUID, "neoessentials.item.powertool")) {
                return;
            }

            // Cancel the vanilla interaction and run the bound command
            if (event instanceof ICancellableEvent cancellable) {
                cancellable.setCanceled(true);
            }

            var server = player.level().getServer();
            if (server == null) return;

            try {
                server.getCommands().performPrefixedCommand(
                    player.createCommandSourceStack(),
                    command.startsWith("/") ? command.substring(1) : command
                );
                NeoLog.debug(LOGGER, LogCategory.GENERAL, "Executed powertool '{}' for {}", command, player.getName().getString());
            } catch (Exception e) {
                LOGGER.error("Failed to execute powertool command '{}' for player {}",
                    command, player.getName().getString(), e);
                player.sendSystemMessage(MessageUtil.error("commands.neoessentials.powertool.execution_failed"));
            }

        } catch (Exception e) {
            LOGGER.error("Error in powertool interaction handler", e);
        }
    }
}