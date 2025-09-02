package com.zerog.neoessentials.commands.essentials;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.afk.AFKManager;
import com.zerog.neoessentials.permissions.PermissionNodes;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * AFK Command - Handles player AFK status
 * Commands: /afk [true|false], /afk [player] [true|false], /away [true|false]
 * 
 * Permission Nodes:
 * - neoessentials.afk - Toggle/set own AFK status
 * - neoessentials.afk.others - Manage other players' AFK status
 * - neoessentials.afk.exempt - Immune to auto-AFK
 * - neoessentials.afk.* - All AFK permissions
 * 
 * @author NeoEssentials Team
 * @since 1.0.0
 */
public class AFKCommand implements IEssentialCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /afk - Toggle/set own AFK status
        dispatcher.register(Commands.literal("afk")
            .requires(source -> EssentialCommandHelper.hasPermission(source, PermissionNodes.AFK))
            // /afk - Toggle own AFK status
            .executes(AFKCommand::toggleSelfAFK)
            // /afk <true|false> - Set own AFK status
            .then(Commands.argument("status", BoolArgumentType.bool())
                .executes(context -> setSelfAFK(context, BoolArgumentType.getBool(context, "status"))))
            // /afk <player> - Toggle another player's AFK status (admin only)
            .then(Commands.argument("player", EntityArgument.player())
                .requires(source -> EssentialCommandHelper.hasPermission(source, PermissionNodes.AFK_OTHERS))
                .executes(AFKCommand::toggleOtherAFK)
                // /afk <player> <true|false> - Set another player's AFK status (admin only)
                .then(Commands.argument("status", BoolArgumentType.bool())
                    .executes(context -> setOtherAFK(context, 
                        EntityArgument.getPlayer(context, "player"),
                        BoolArgumentType.getBool(context, "status")))))
        );
        
        // Register /away as an alias
        dispatcher.register(Commands.literal("away")
            .requires(source -> EssentialCommandHelper.hasPermission(source, PermissionNodes.AFK))
            .executes(AFKCommand::toggleSelfAFK)
            .then(Commands.argument("status", BoolArgumentType.bool())
                .executes(context -> setSelfAFK(context, BoolArgumentType.getBool(context, "status"))))
        );
    }
    
    /**
     * Toggle self AFK status
     */
    private static int toggleSelfAFK(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return EssentialCommandHelper.executeWithPermission(context, PermissionNodes.AFK, player -> {
            AFKManager afkManager = AFKManager.getInstance();
            boolean currentStatus = afkManager.isAFK(player);
            return setSelfAFKInternal(context.getSource(), player, !currentStatus);
        });
    }
    
    /**
     * Set self AFK status
     */
    private static int setSelfAFK(CommandContext<CommandSourceStack> context, boolean status) throws CommandSyntaxException {
        return EssentialCommandHelper.executeWithPermission(context, PermissionNodes.AFK, player -> {
            return setSelfAFKInternal(context.getSource(), player, status);
        });
    }
    
    /**
     * Internal method to set self AFK status
     */
    private static int setSelfAFKInternal(CommandSourceStack source, ServerPlayer player, boolean status) {
        AFKManager afkManager = AFKManager.getInstance();
        
        // Check if status is already the same
        if (afkManager.isAFK(player) == status) {
            String messageKey = status ? "neoessentials.afk.already_afk" : "neoessentials.afk.already_active";
            EssentialCommandHelper.sendFailure(source, messageKey);
            return 0;
        }
        
        afkManager.setAFK(player, status);
        
        // Send confirmation message
        String messageKey = status ? "neoessentials.afk.set_afk" : "neoessentials.afk.set_active";
        EssentialCommandHelper.sendSuccess(source, messageKey);
        
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
        return EssentialCommandHelper.executeOnTarget(context, PermissionNodes.AFK_OTHERS, target, (targetPlayer) -> {
            AFKManager afkManager = AFKManager.getInstance();
            CommandSourceStack source = context.getSource();
            
            // Check if status is already the same
            if (afkManager.isAFK(targetPlayer) == status) {
                String messageKey = status ? "neoessentials.afk.player_already_afk" : "neoessentials.afk.player_already_active";
                EssentialCommandHelper.sendFailure(source, messageKey, targetPlayer.getName().getString());
                return 0;
            }
            
            afkManager.setAFK(targetPlayer, status);
            
            // Send confirmation to admin
            String adminMessageKey = status ? "neoessentials.afk.admin_set_afk" : "neoessentials.afk.admin_set_active";
            EssentialCommandHelper.sendSuccessWithBroadcast(source, adminMessageKey, targetPlayer.getName().getString());
            
            // Send message to target player
            String messageKey = status ? "neoessentials.afk.admin_forced_afk" : "neoessentials.afk.admin_forced_active";
            EssentialCommandHelper.sendPlayerMessage(targetPlayer, messageKey);
            
            return 1;
        });
    }
    
    @Override
    public String getCommandName() {
        return "afk";
    }
    
    @Override
    public String[] getAliases() {
        return new String[]{"away"};
    }
    
    @Override
    public String getDescription() {
        return "Toggle or set AFK (Away From Keyboard) status";
    }
    
    @Override
    public String getUsage() {
        return "/afk [true|false] [player]";
    }
    
    @Override
    public String getPermission() {
        return PermissionNodes.AFK;
    }
}
