package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.afk.AFKManager;
import com.zerog.neoessentials.permissions.PermissionNodes;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * AFK Command - Handles player AFK status
 * Commands: /afk [true|false], /afk [player] [true|false]
 * 
 * @author NeoEssentials Team
 * @since 1.0.0
 */
public class AFKCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("afk")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.AFK))
            // /afk - Toggle own AFK status
            .executes(AFKCommand::toggleSelfAFK)
            // /afk <true|false> - Set own AFK status
            .then(Commands.argument("status", BoolArgumentType.bool())
                .executes(context -> setSelfAFK(context, BoolArgumentType.getBool(context, "status"))))
            // /afk <player> - Toggle another player's AFK status (admin only)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.AFK_OTHERS))
                .executes(AFKCommand::toggleOtherAFK)
                // /afk <player> <true|false> - Set another player's AFK status (admin only)
                .then(Commands.argument("status", BoolArgumentType.bool())
                    .executes(context -> setOtherAFK(context, 
                        EntityArgument.getPlayer(context, "player"),
                        BoolArgumentType.getBool(context, "status")))))
        );
        
        // Register /away as an alias
        dispatcher.register(Commands.literal("away")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.AFK))
            .executes(AFKCommand::toggleSelfAFK)
            .then(Commands.argument("status", BoolArgumentType.bool())
                .executes(context -> setSelfAFK(context, BoolArgumentType.getBool(context, "status"))))
        );
    }
    
    /**
     * Toggle self AFK status
     */
    private static int toggleSelfAFK(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        AFKManager afkManager = AFKManager.getInstance();
        boolean currentStatus = afkManager.isAFK(player);
        return setSelfAFK(context, !currentStatus);
    }
    
    /**
     * Set self AFK status
     */
    private static int setSelfAFK(CommandContext<CommandSourceStack> context, boolean status) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        AFKManager afkManager = AFKManager.getInstance();
        
        // Check if status is already the same
        if (afkManager.isAFK(player) == status) {
            String messageKey = status ? "neoessentials.afk.already_afk" : "neoessentials.afk.already_active";
            MessageUtil.sendTranslatedMessage(player, messageKey);
            return 0;
        }
        
        afkManager.setAFK(player, status);
        
        // Send confirmation message
        String messageKey = status ? "neoessentials.afk.now_afk" : "neoessentials.afk.back";
        MessageUtil.sendTranslatedMessage(player, messageKey, player.getName().getString());
        
        return 1;
    }
    
    /**
     * Toggle another player's AFK status (admin only)
     */
    private static int toggleOtherAFK(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(context, "player");
        AFKManager afkManager = AFKManager.getInstance();
        boolean currentStatus = afkManager.isAFK(target);
        return setOtherAFK(context, target, !currentStatus);
    }
    
    /**
     * Set another player's AFK status (admin only)
     */
    private static int setOtherAFK(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean status) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        AFKManager afkManager = AFKManager.getInstance();
        
        // Check if status is already the same
        if (afkManager.isAFK(target) == status) {
            String messageKey = status ? "neoessentials.afk.player_already_afk" : "neoessentials.afk.player_already_active";
            source.sendSuccess(() -> Component.translatable(messageKey, target.getName().getString()), false);
            return 0;
        }
        
        afkManager.setAFK(target, status);
        
        // Send confirmation to admin
        String adminMessageKey = status ? "neoessentials.afk.admin_set_afk" : "neoessentials.afk.admin_set_active";
        source.sendSuccess(() -> Component.translatable(adminMessageKey, target.getName().getString()), false);
        
        // Send message to target player
        String messageKey = status ? "neoessentials.afk.admin_forced_afk" : "neoessentials.afk.admin_forced_active";
        MessageUtil.sendTranslatedMessage(target, messageKey);
        
        return 1;
    }
}
