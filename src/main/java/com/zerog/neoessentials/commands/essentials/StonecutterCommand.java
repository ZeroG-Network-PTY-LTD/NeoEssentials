package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.StonecutterMenu;

public class StonecutterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stonecutter")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.STONECUTTER))
            .executes(StonecutterCommand::openStonecutter)
        );
    }

    /**
     * Open stonecutter for command sender
     */
    private static int openStonecutter(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MenuProvider stonecutterProvider = new SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> new StonecutterMenu(
                windowId,
                playerInventory,
                ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.stonecutter")
        );
        player.openMenu(stonecutterProvider);
        context.getSource().sendSuccess(() -> Component.literal("Opened stonecutter"), false);
        return 1;
    }
}
