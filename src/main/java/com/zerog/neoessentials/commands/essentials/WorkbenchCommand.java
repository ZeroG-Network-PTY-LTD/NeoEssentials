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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.CraftingMenu;

public class WorkbenchCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("workbench")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WORKBENCH))
            .executes(WorkbenchCommand::openWorkbench)
        );
        dispatcher.register(Commands.literal("wb")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WORKBENCH))
            .executes(WorkbenchCommand::openWorkbench)
        );
        dispatcher.register(Commands.literal("craft")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WORKBENCH))
            .executes(WorkbenchCommand::openWorkbench)
        );
        dispatcher.register(Commands.literal("crafting")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.WORKBENCH))
            .executes(WorkbenchCommand::openWorkbench)
        );
    }

    /**
     * Open workbench for command sender
     */
    private static int openWorkbench(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            MenuProvider workbenchProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new CraftingMenu(
                    windowId,
                    playerInventory,
                    ContainerLevelAccess.create(player.level(), player.blockPosition())
                ),
                Component.translatable("container.crafting")
            );
            player.openMenu(workbenchProvider);
            context.getSource().sendSuccess(() -> Component.literal("Opened workbench"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Failed to open workbench: " + e.getMessage()));
            return 0;
        }
    }

}
