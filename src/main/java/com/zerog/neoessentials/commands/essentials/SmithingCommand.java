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
    private static int openSmithing(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            LanguageManager langManager = LanguageManager.getInstance();
            
            MenuProvider smithingProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new SmithingMenu(
                    windowId,
                    playerInventory,
                    ContainerLevelAccess.NULL
                ),
                Component.translatable("container.upgrade")
            );
            player.openMenu(smithingProvider);
            
            String message = langManager.getMessage(player, "neoessentials.command.smithing.opened");
            context.getSource().sendSuccess(() -> ColorUtil.colorize(message), false);
            return 1;
        } catch (Exception e) {
            ServerPlayer player = context.getSource().getPlayer();
            LanguageManager langManager = LanguageManager.getInstance();
            
            String errorMessage = player != null ? 
                langManager.getMessage(player, "neoessentials.command.smithing.failed", e.getMessage()) :
                "Failed to open smithing table: " + e.getMessage();
                
            context.getSource().sendFailure(ColorUtil.colorize(errorMessage));
            return 0;
        }
    }
}
