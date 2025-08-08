package com.zerog.neoessentials.commands;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.managers.ModerationManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/**
 * Moderation command implementation
 * Handles /kick, /ban, /mute, /jail, /tempban commands
 */
public class ModerationCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /kick <player> [reason] - Kick player
        dispatcher.register(Commands.literal("kick")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> kickPlayer(context, EntityArgument.getPlayer(context, "player"), "Kicked by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(context -> kickPlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "reason")))
                )
            )
        );
        
        // /ban <player> [reason] - Ban player permanently (using vanilla ban)
        dispatcher.register(Commands.literal("ban")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> banPlayer(context,
                    EntityArgument.getPlayer(context, "player"),
                    "Banned by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(context -> banPlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        StringArgumentType.getString(context, "reason")))
                )
            )
        );
        
        // /mute <player> [duration] [reason] - Mute player
        dispatcher.register(Commands.literal("mute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> mutePlayer(context,
                    EntityArgument.getPlayer(context, "player"),
                    "Muted by admin",
                    -1))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> mutePlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        "Muted by admin",
                        IntegerArgumentType.getInteger(context, "duration")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> mutePlayer(context,
                            EntityArgument.getPlayer(context, "player"),
                            StringArgumentType.getString(context, "reason"),
                            IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
        );
        
        // /unmute <player> - Unmute player
        dispatcher.register(Commands.literal("unmute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> unmutePlayer(context, EntityArgument.getPlayer(context, "player")))
            )
        );
        
        // /jail <player> [duration] [reason] - Jail player
        dispatcher.register(Commands.literal("jail")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> jailPlayer(context,
                    EntityArgument.getPlayer(context, "player"),
                    "Jailed by admin",
                    -1))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> jailPlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        "Jailed by admin",
                        IntegerArgumentType.getInteger(context, "duration")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> jailPlayer(context,
                            EntityArgument.getPlayer(context, "player"),
                            StringArgumentType.getString(context, "reason"),
                            IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
        );
        
        // /unjail <player> - Unjail player
        dispatcher.register(Commands.literal("unjail")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> unjailPlayer(context, EntityArgument.getPlayer(context, "player")))
            )
        );
        
        // /tempban <player> <duration> [reason] - Temporarily ban player
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> tempBanPlayer(context,
                        EntityArgument.getPlayer(context, "player"),
                        IntegerArgumentType.getInteger(context, "duration"),
                        "Temporarily banned by admin"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> tempBanPlayer(context,
                            EntityArgument.getPlayer(context, "player"),
                            IntegerArgumentType.getInteger(context, "duration"),
                            StringArgumentType.getString(context, "reason")))
                    )
                )
            )
        );
    }
    
    private static int kickPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        boolean success = moderationManager.kickPlayer(target, admin, reason);
        return success ? 1 : 0;
    }
    
    private static int banPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        
        try {
            // For now, use a simple kick - ban functionality can be enhanced later
            target.connection.disconnect(Component.literal("You have been banned: " + reason));
            
            // Send confirmation to admin
            MessageUtil.sendMessage(admin, "§cBanned player " + target.getName().getString() + " for: " + reason);
            MessageUtil.sendMessage(admin, "§eNote: Full ban system implementation pending");
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendMessage(admin, "§cFailed to ban player: " + e.getMessage());
            return 0;
        }
    }
    
    private static int mutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String reason, int durationMinutes) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        long duration = durationMinutes == -1 ? -1 : durationMinutes * 60L; // Convert to seconds
        boolean success = moderationManager.mutePlayer(target.getUUID(), target.getName().getString(), admin, reason, duration);
        return success ? 1 : 0;
    }
    
    private static int unmutePlayer(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        boolean success = moderationManager.unmutePlayer(target.getUUID(), target.getName().getString(), admin);
        return success ? 1 : 0;
    }
    
    private static int jailPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, String reason, int durationMinutes) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        long duration = durationMinutes == -1 ? -1 : durationMinutes * 60L; // Convert to seconds
        String jailName = "default"; // Use default jail
        boolean success = moderationManager.jailPlayer(target.getUUID(), target.getName().getString(), admin, jailName, reason, duration);
        return success ? 1 : 0;
    }
    
    private static int unjailPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        boolean success = moderationManager.unjailPlayer(target.getUUID(), target.getName().getString(), admin);
        return success ? 1 : 0;
    }
    
    private static int tempBanPlayer(CommandContext<CommandSourceStack> context, ServerPlayer target, int durationMinutes, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Validate duration
        if (durationMinutes < 1) {
            MessageUtil.sendMessage(admin, "&cDuration must be at least 1 minute!");
            return 0;
        }
        
        if (durationMinutes > 525600) { // 1 year in minutes
            MessageUtil.sendMessage(admin, "&cDuration cannot exceed 1 year!");
            return 0;
        }
        
        // Check if trying to ban themselves
        if (target.getUUID().equals(admin.getUUID())) {
            MessageUtil.sendMessage(admin, "&cYou cannot ban yourself!");
            return 0;
        }
        
        boolean success = moderationManager.tempBanPlayer(target, admin, reason, durationMinutes);
        return success ? 1 : 0;
    }
}
