package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;

public class InvSeeCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("invsee")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.INVSEE))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(InvSeeCommand::openPlayerInventory)
            )
        );
        
        // Alternative commands
        dispatcher.register(Commands.literal("openinv")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.INVSEE))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(InvSeeCommand::openPlayerInventory)
            )
        );
        
        dispatcher.register(Commands.literal("oi")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.INVSEE))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(InvSeeCommand::openPlayerInventory)
            )
        );
    }

    /**
     * Open specified player's inventory
     */
    private static int openPlayerInventory(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer opener = context.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        return openInventoryForPlayer(context.getSource(), opener, target);
    }

    /**
     * Core method to open a player's inventory for another player
     */
    private static int openInventoryForPlayer(CommandSourceStack source, ServerPlayer opener, ServerPlayer target) {
        try {
            // Don't allow opening own inventory
            if (opener == target) {
                sendLocalizedMessage(source, "neoessentials.invsee.cannot_open_self");
                return 0;
            }

            // Config: allowModify
            boolean canModify = PermissionUtil.hasPermissionOrOp(source, PermissionNodes.INVSEE_MODIFY)
                && com.zerog.neoessentials.config.ConfigManager.getInstance().getMainConfig().invseeConfig.allowModify;

            MenuProvider inventoryProvider = new SimpleMenuProvider(
                (windowId, playerInventory, playerEntity) -> new com.zerog.neoessentials.menus.InvseeMenu(windowId, playerInventory, target.getInventory(), canModify),
                Component.literal(target.getDisplayName().getString() + "'s Inventory")
            );

            opener.openMenu(inventoryProvider);

            sendLocalizedMessage(source, "neoessentials.invsee.opened", target.getDisplayName().getString());
            return 1;

        } catch (Exception e) {
            sendLocalizedMessage(source, "neoessentials.invsee.failed", e.getMessage());
            return 0;
        }
    }

    private static void sendLocalizedMessage(CommandSourceStack source, String key, Object... placeholders) {
        if (source.getEntity() instanceof ServerPlayer player) {
            com.zerog.neoessentials.util.MessageUtil.sendMessage(player, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(player, key, placeholders));
        } else {
            source.sendSuccess(() -> Component.literal(com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage("en_us", key, placeholders)), false);
        }
    }
}
