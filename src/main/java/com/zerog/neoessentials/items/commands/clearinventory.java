package com.zerog.neoessentials.items.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigUtil;
import com.zerog.neoessentials.util.MessageUtil;

public class clearinventory {
    /**
     * Register the /clearinventory, /ci, and /clearinv commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigUtil.isCommandEnabled("clearinventory")) return;
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
     * Returns an int array: [mainCleared, armorCleared, offhandCleared]
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

        return new int[] { mainCleared, armorCleared, offhandCleared };
    }
}
