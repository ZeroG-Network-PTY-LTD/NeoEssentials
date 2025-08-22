package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;

public class EnderChestCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("enderchest")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ENDERCHEST))
            .executes(EnderChestCommand::openEnderChest)
        );
        dispatcher.register(Commands.literal("ec")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ENDERCHEST))
            .executes(EnderChestCommand::openEnderChest)
        );
        dispatcher.register(Commands.literal("echest")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ENDERCHEST))
            .executes(EnderChestCommand::openEnderChest)
        );
    }

    /**
     * Open ender chest for command sender
     */
    private static int openEnderChest(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MenuProvider enderChestProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> ChestMenu.threeRows(
                    windowId,
                    playerInventory,
                    player.getEnderChestInventory()
                ),
                Component.translatable("container.enderchest")
            );
            player.openMenu(enderChestProvider);
            context.getSource().sendSuccess(() -> Component.literal("Opened ender chest"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to open ender chest: " + e.getMessage()));
            return 0;
        }
    }

}
