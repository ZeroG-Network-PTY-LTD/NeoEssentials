package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides inventory clearing functionality for players.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/clearinventory - Clear all inventory slots</li>
 *   <li>/ci - Short alias</li>
 *   <li>/clearinv - Alternative alias</li>
 * </ul>
 * 
 * <p>Permissions:</p>
 * <ul>
 *   <li>neoessentials.item.clearinventory - Clear own inventory</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>commands.clearinventory.enabled - Enable/disable command</li>
 * </ul>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Clears main inventory (36 slots)</li>
 *   <li>Clears armor slots (4 pieces)</li>
 *   <li>Clears offhand slot</li>
 *   <li>Provides detailed feedback on items cleared per section</li>
 *   <li>Audit logging for administrative oversight</li>
 * </ul>
 */
public class ClearInventoryCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClearInventoryCommand.class);
    
    /**
     * Register the /clearinventory, /ci, and /clearinv commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        ConfigManager cfg = ConfigManager.getInstance();

        if (cfg.isCommandEnabled("clearinventory")) {
        dispatcher.register(
            Commands.literal("clearinventory")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.clearinventory")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    int[] cleared = clear(player);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success(
                        "commands.neoessentials.clearinventory.detailed_success",
                        cleared[0], cleared[1], cleared[2]
                    ), false);
                    return 1;
                })
        );
        }
        if (cfg.isCommandEnabled("ci")) {
        dispatcher.register(
            Commands.literal("ci")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.clearinventory")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    int[] cleared = clear(player);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success(
                        "commands.neoessentials.clearinventory.detailed_success",
                        cleared[0], cleared[1], cleared[2]
                    ), false);
                    return 1;
                })
        );
        }
        if (cfg.isCommandEnabled("clearinv")) {
        dispatcher.register(
            Commands.literal("clearinv")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.clearinventory")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    int[] cleared = clear(player);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success(
                        "commands.neoessentials.clearinventory.detailed_success",
                        cleared[0], cleared[1], cleared[2]
                    ), false);
                    return 1;
                })
        );
        }
    }

    /**
     * Clears the player's inventory, including main, armor, and offhand slots.
     * Logs the clear action for audit trail purposes.
     * 
     * @param player The player whose inventory to clear
     * @return An int array: [mainCleared, armorCleared, offhandCleared]
     */
    public static int[] clear(ServerPlayer player) {
        int mainCleared = 0;
        int armorCleared = 0;
        int offhandCleared = 0;

        // Main inventory
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            if (!player.getInventory().items.get(i).isEmpty()) {
                mainCleared++;
            }
        }
        player.getInventory().clearContent();

        // Armor
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            if (!player.getInventory().armor.get(i).isEmpty()) {
                armorCleared++;
            }
        }
        player.getInventory().armor.clear();

        // Offhand
        for (int i = 0; i < player.getInventory().offhand.size(); i++) {
            if (!player.getInventory().offhand.get(i).isEmpty()) {
                offhandCleared++;
            }
        }
        player.getInventory().offhand.clear();

        // Log inventory clear for audit trail
        LOGGER.info("Player {} cleared inventory: {} main items, {} armor pieces, {} offhand items (total: {})", 
            player.getName().getString(), 
            mainCleared, 
            armorCleared, 
            offhandCleared,
            mainCleared + armorCleared + offhandCleared);

        return new int[] { mainCleared, armorCleared, offhandCleared };
    }
}
