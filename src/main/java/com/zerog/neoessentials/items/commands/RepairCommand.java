package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zerog.neoessentials.logging.LogCategory;
import com.zerog.neoessentials.logging.NeoLog;

/**
 * Provides item repair functionality allowing players to instantly repair damaged items.
 * 
 * <p>Commands:</p>
 * <ul>
 *   <li>/repair - Repair the item in main hand</li>
 *   <li>/fix - Alias for /repair</li>
 * </ul>
 * 
 * <p>Permissions:</p>
 * <ul>
 *   <li>neoessentials.item.repair - Use repair commands</li>
 * </ul>
 * 
 * <p>Configuration:</p>
 * <ul>
 *   <li>commands.repair.enabled - Enable/disable repair command</li>
 * </ul>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Instant repair of damageable items</li>
 *   <li>Works on tools, armor, weapons</li>
 *   <li>No resource cost</li>
 *   <li>Audit logging for administrative oversight</li>
 * </ul>
 */
public class RepairCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RepairCommand.class);
    
    /**
     * Register the /repair and /fix commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Note: Item commands use general commandsEnabled module (if implemented) + individual command check
        if (ConfigManager.getInstance().isCommandEnabled("repair")) {
        dispatcher.register(
            Commands.literal("repair")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.repair");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    repairItem(permResult.getPlayer());
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.repair.success"), false);
                    return 1;
                })
        );
        }
        if (ConfigManager.getInstance().isCommandEnabled("fix")) {
        dispatcher.register(
            Commands.literal("fix")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult =
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.item.repair");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    repairItem(permResult.getPlayer());
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.repair.success"), false);
                    return 1;
                })
        );
        }
    }

    /**
     * Repairs the item in the player's main hand if it is damageable.
     * Logs the repair action for audit trail purposes.
     * 
     * @param player The player whose item to repair
     */
    public static void repairItem(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            String itemName = stack.getDisplayName().getString();
            int damageBefore = stack.getDamageValue();
            
            stack.setDamageValue(0);
            
            // Log repair action for audit trail
            NeoLog.info(LOGGER, LogCategory.GENERAL, "Player {} repaired item: {} (damage: {} -> 0)", 
                player.getName().getString(), 
                itemName,
                damageBefore);
        }
    }
}
