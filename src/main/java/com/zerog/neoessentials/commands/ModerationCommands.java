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
import net.minecraft.server.level.ServerPlayer;

/**
 * Moderation command implementation
 * Handles /kick, /ban, /mute, /jail, /tempban commands
 */
public class ModerationCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /kick <player> [reason] - Kick player
        dispatcher.register(Commands.literal("kick")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> kickPlayer(context, StringArgumentType.getString(context, "player"), "Kicked by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(context -> kickPlayer(context,
                        StringArgumentType.getString(context, "player"),
                        StringArgumentType.getString(context, "reason")))
                )
            )
        );
        
        // /ban <player> [reason] - Ban player permanently (using vanilla ban)
        dispatcher.register(Commands.literal("ban")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> banPlayer(context,
                    StringArgumentType.getString(context, "player"),
                    "Banned by admin"))
                .then(Commands.argument("reason", StringArgumentType.greedyString())
                    .executes(context -> banPlayer(context,
                        StringArgumentType.getString(context, "player"),
                        StringArgumentType.getString(context, "reason")))
                )
            )
        );
        
        // /mute <player> [duration] [reason] - Mute player
        dispatcher.register(Commands.literal("mute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> mutePlayer(context,
                    StringArgumentType.getString(context, "player"),
                    "Muted by admin",
                    -1))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> mutePlayer(context,
                        StringArgumentType.getString(context, "player"),
                        "Muted by admin",
                        IntegerArgumentType.getInteger(context, "duration")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> mutePlayer(context,
                            StringArgumentType.getString(context, "player"),
                            StringArgumentType.getString(context, "reason"),
                            IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
        );
        
        // /unmute <player> - Unmute player
        dispatcher.register(Commands.literal("unmute")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.MODERATION_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> unmutePlayer(context, StringArgumentType.getString(context, "player")))
            )
        );

        // /jail <player> [duration] [reason] - Jail player
        dispatcher.register(Commands.literal("jail")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> jailPlayer(context,
                    StringArgumentType.getString(context, "player"),
                    "Jailed by admin",
                    -1))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> jailPlayer(context,
                        StringArgumentType.getString(context, "player"),
                        "Jailed by admin",
                        IntegerArgumentType.getInteger(context, "duration")))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> jailPlayer(context,
                            StringArgumentType.getString(context, "player"),
                            StringArgumentType.getString(context, "reason"),
                            IntegerArgumentType.getInteger(context, "duration")))
                    )
                )
            )
        );

        // /unjail <player> - Unjail player
        dispatcher.register(Commands.literal("unjail")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .executes(context -> unjailPlayer(context, StringArgumentType.getString(context, "player")))
            )
        );

        // /tempban <player> <duration> [reason] - Temporarily ban player
        dispatcher.register(Commands.literal("tempban")
            .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
            .then(Commands.argument("player", StringArgumentType.word())
                .then(Commands.argument("duration", IntegerArgumentType.integer(1))
                    .executes(context -> tempBanPlayer(context,
                        StringArgumentType.getString(context, "player"),
                        IntegerArgumentType.getInteger(context, "duration"),
                        "Temporarily banned by admin"))
                    .then(Commands.argument("reason", StringArgumentType.greedyString())
                        .executes(context -> tempBanPlayer(context,
                            StringArgumentType.getString(context, "player"),
                            IntegerArgumentType.getInteger(context, "duration"),
                            StringArgumentType.getString(context, "reason")))
                    )
                )
            )
        );
    }
    
    private static int kickPlayer(CommandContext<CommandSourceStack> context, String targetName, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Find the target player by name
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(admin, "§cPlayer '" + targetName + "' not found or not online.");
            return 0;
        }
        
        boolean success = moderationManager.kickPlayer(target, admin, reason);
        return success ? 1 : 0;
    }
    
    private static int banPlayer(CommandContext<CommandSourceStack> context, String targetName, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        
        // Find the target player by name
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(admin, "§cPlayer '" + targetName + "' not found or not online.");
            return 0;
        }
        
        try {
            // For now, use a simple kick - ban functionality can be enhanced later
            // Use server PlayerList to remove player instead of direct disconnect
            context.getSource().getServer().getPlayerList().remove(target);
            
            // Send confirmation to admin
            MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "moderation.ban.success", target.getName().getString(), reason));
            MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "moderation.ban.note_pending"));
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "moderation.ban.failed", e.getMessage()));
            return 0;
        }
    }
    
    private static int mutePlayer(CommandContext<CommandSourceStack> context, String targetName, String reason, int durationMinutes) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Find the target player by name
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(admin, "§cPlayer '" + targetName + "' not found or not online.");
            return 0;
        }
        
        long duration = durationMinutes == -1 ? -1 : durationMinutes * 60L; // Convert to seconds
        boolean success = moderationManager.mutePlayer(target.getUUID(), target.getName().getString(), admin, reason, duration);
        return success ? 1 : 0;
    }
    
    private static int unmutePlayer(CommandContext<CommandSourceStack> context, String targetName) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Find the target player by name (they don't need to be online for unmute)
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        java.util.UUID targetUUID = null;
        String targetNameResolved = targetName;
        
        if (target != null) {
            targetUUID = target.getUUID();
            targetNameResolved = target.getName().getString();
        } else {
            // Try to find offline player - this requires additional logic
            // For now, just use the provided name
            MessageUtil.sendMessage(admin, "§eNote: Player '" + targetName + "' is not online. Attempting to unmute by name...");
        }
        
        boolean success = moderationManager.unmutePlayer(targetUUID, targetNameResolved, admin);
        return success ? 1 : 0;
    }
    
    private static int jailPlayer(CommandContext<CommandSourceStack> context, String targetName, String reason, int durationMinutes) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Find the target player by name
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(admin, "§cPlayer '" + targetName + "' not found or not online.");
            return 0;
        }
        
        long duration = durationMinutes == -1 ? -1 : durationMinutes * 60L; // Convert to seconds
        String jailName = "default"; // Use default jail
        boolean success = moderationManager.jailPlayer(target.getUUID(), target.getName().getString(), admin, jailName, reason, duration);
        return success ? 1 : 0;
    }
    
    private static int unjailPlayer(CommandContext<CommandSourceStack> context, String targetName) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Find the target player by name (they don't need to be online for unjail)
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        java.util.UUID targetUUID = null;
        String targetNameResolved = targetName;
        
        if (target != null) {
            targetUUID = target.getUUID();
            targetNameResolved = target.getName().getString();
        } else {
            MessageUtil.sendMessage(admin, "§eNote: Player '" + targetName + "' is not online. Attempting to unjail by name...");
        }
        
        boolean success = moderationManager.unjailPlayer(targetUUID, targetNameResolved, admin);
        return success ? 1 : 0;
    }
    
    private static int tempBanPlayer(CommandContext<CommandSourceStack> context, String targetName, int durationMinutes, String reason) throws CommandSyntaxException {
        ServerPlayer admin = context.getSource().getPlayerOrException();
        ModerationManager moderationManager = ModerationManager.getInstance();
        
        // Find the target player by name
        ServerPlayer target = context.getSource().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            MessageUtil.sendMessage(admin, "§cPlayer '" + targetName + "' not found or not online.");
            return 0;
        }
        
        // Validate duration
        if (durationMinutes < 1) {
            MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "moderation.tempban.duration_too_short"));
            return 0;
        }
        
        if (durationMinutes > 525600) { // 1 year in minutes
            MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "moderation.tempban.duration_too_long"));
            return 0;
        }
        
        // Check if trying to ban themselves
        if (target.getUUID().equals(admin.getUUID())) {
            MessageUtil.sendMessage(admin, com.zerog.neoessentials.localization.LanguageManager.getInstance().getMessage(admin, "moderation.tempban.cannot_ban_self"));
            return 0;
        }
        
        boolean success = moderationManager.tempBanPlayer(target, admin, reason, durationMinutes);
        return success ? 1 : 0;
    }
}
