package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
        if (!ConfigManager.getInstance().isCommandEnabled("clearinventory")) return;
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

        // 26.1 port: Inventory.items/armor/offhand are private now (armor/offhand were folded
        // into a separate EntityEquipment object entirely) — count via the still-public
        // getNonEquipmentItems()/getItemBySlot() accessors instead of the old field lists.
        // clearContent() already clears both the main items AND the equipment in one call.

        // Main inventory
        for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
            if (!stack.isEmpty()) {
                mainCleared++;
            }
        }

        // Armor
        for (net.minecraft.world.entity.EquipmentSlot slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
                net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD}) {
            if (!player.getItemBySlot(slot).isEmpty()) {
                armorCleared++;
            }
        }

        // Offhand
        if (!player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND).isEmpty()) {
            offhandCleared++;
        }

        player.getInventory().clearContent();

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
