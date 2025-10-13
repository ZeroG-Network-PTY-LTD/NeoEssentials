package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

public class RepairCommand {
    /**
     * Register the /repair and /fix commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Note: Item commands use general commandsEnabled module (if implemented) + individual command check
        if (!ConfigManager.getInstance().isCommandEnabled("repair")) return;
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

    /**
     * Repairs the item in the player's main hand if it is damageable.
     */
    public static void repairItem(ServerPlayer player) {
        var stack = player.getMainHandItem();
        if (!stack.isEmpty() && stack.isDamageableItem()) {
            stack.setDamageValue(0);
        }
    }
}
