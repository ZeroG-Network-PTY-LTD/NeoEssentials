package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.CommandConfig;

public class repair {
    /**
     * Register the /repair and /fix commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!CommandConfig.isCommandEnabled("repair")) return;
        dispatcher.register(
            Commands.literal("repair")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.repair")) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    repairItem(player);
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.repair.success"), false);
                    return 1;
                })
        );
        dispatcher.register(
            Commands.literal("fix")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.repair")) {
                        ctx.getSource().sendFailure(net.minecraft.network.chat.Component.translatable("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    repairItem(player);
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.translatable("commands.neoessentials.repair.success"), false);
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
