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
import net.minecraft.world.inventory.SmithingMenu;

public class SmithingCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("smithing")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.SMITHING))
            .executes(SmithingCommand::openSmithing)
        );
    }

    /**
     * Open smithing table for command sender
     */
    private static int openSmithing(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        MenuProvider smithingProvider = new SimpleMenuProvider(
            (windowId, playerInventory, playerEntity) -> new SmithingMenu(
                windowId,
                playerInventory,
                ContainerLevelAccess.create(player.level(), player.blockPosition())
            ),
            Component.translatable("container.upgrade")
        );
        player.openMenu(smithingProvider);
        context.getSource().sendSuccess(() -> Component.literal("Opened smithing table"), false);
        return 1;
    }
}
