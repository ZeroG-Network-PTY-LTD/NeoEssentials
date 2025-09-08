package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.localization.LanguageManager;
import com.zerog.neoessentials.util.ColorUtil;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
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
    private static int openStonecutter(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            LanguageManager langManager = LanguageManager.getInstance();
            
            MenuProvider stonecutterProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new StonecutterMenu(
                    windowId,
                    playerInventory,
                    ContainerLevelAccess.NULL
                ),
                Component.translatable("container.stonecutter")
            );
            player.openMenu(stonecutterProvider);
            
            String message = langManager.getMessage(player, "neoessentials.command.stonecutter.opened");
            context.getSource().sendSuccess(() -> ColorUtil.colorize(message), false);
            return 1;
        } catch (Exception e) {
            ServerPlayer player = context.getSource().getPlayer();
            LanguageManager langManager = LanguageManager.getInstance();
            
            String errorMessage = player != null ? 
                langManager.getMessage(player, "neoessentials.command.stonecutter.failed", e.getMessage()) :
                "Failed to open stonecutter: " + e.getMessage();
                
            context.getSource().sendFailure(ColorUtil.colorize(errorMessage));
            return 0;
        }
    }
}
