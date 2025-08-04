package com.zerog.neoessentials.commands.discord;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.discord.DiscordManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/**
 * Discord commands for NeoEssentials
 * Handles Discord integration commands
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class DiscordCommand {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // /discord - Show Discord status and info
        dispatcher.register(Commands.literal("discord")
            .requires(source -> source.hasPermission(0))
            .executes(DiscordCommand::showDiscordInfo)
            .then(Commands.literal("status")
                .requires(source -> source.hasPermission(2))
                .executes(DiscordCommand::showDiscordStatus)
            )
            .then(Commands.literal("link")
                .requires(source -> source.hasPermission(0))
                .then(Commands.argument("discordId", StringArgumentType.word())
                    .executes(DiscordCommand::linkAccount)
                )
            )
            .then(Commands.literal("unlink")
                .requires(source -> source.hasPermission(0))
                .executes(DiscordCommand::unlinkAccount)
            )
            .then(Commands.literal("linked")
                .requires(source -> source.hasPermission(0))
                .executes(DiscordCommand::checkLinked)
                .then(Commands.argument("player", EntityArgument.player())
                    .requires(source -> source.hasPermission(2))
                    .executes(DiscordCommand::checkOtherLinked)
                )
            )
            .then(Commands.literal("broadcast")
                .requires(source -> source.hasPermission(3))
                .then(Commands.argument("message", StringArgumentType.greedyString())
                    .executes(DiscordCommand::broadcastToDiscord)
                )
            )
        );
        
        // /discordsrv - Alias for compatibility
        dispatcher.register(Commands.literal("discordsrv")
            .requires(source -> source.hasPermission(0))
            .executes(DiscordCommand::showDiscordInfo)
        );
    }
    
    /**
     * Show Discord information
     */
    private static int showDiscordInfo(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordManager discord = DiscordManager.getInstance();
        
        try {
            source.sendSuccess(() -> Component.literal("§6=== Discord Integration ==="), false);
            source.sendSuccess(() -> Component.literal("§7Discord integration allows you to link your Minecraft account"), false);
            source.sendSuccess(() -> Component.literal("§7with Discord for enhanced features and notifications."), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            if (source.getEntity() instanceof ServerPlayer player) {
                String uuid = player.getUUID().toString();
                if (discord.isLinked(uuid)) {
                    String discordId = discord.getDiscordId(uuid);
                    source.sendSuccess(() -> Component.literal("§aYour account is linked to Discord ID: " + discordId), false);
                } else {
                    source.sendSuccess(() -> Component.literal("§cYour account is not linked to Discord"), false);
                    source.sendSuccess(() -> Component.literal("§7Use §e/discord link <discord-id>§7 to link your account"), false);
                }
            }
            
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§7Commands:"), false);
            source.sendSuccess(() -> Component.literal("§e/discord link <id>§7 - Link your account"), false);
            source.sendSuccess(() -> Component.literal("§e/discord unlink§7 - Unlink your account"), false);
            source.sendSuccess(() -> Component.literal("§e/discord linked§7 - Check link status"), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to show Discord info: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Show Discord status (admin command)
     */
    private static int showDiscordStatus(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        DiscordManager discord = DiscordManager.getInstance();
        
        try {
            Map<String, Object> status = discord.getStatus();
            
            source.sendSuccess(() -> Component.literal("§6=== Discord Integration Status ==="), false);
            source.sendSuccess(() -> Component.literal("§7Enabled: " + (Boolean) status.get("enabled")), false);
            source.sendSuccess(() -> Component.literal("§7Webhook Configured: " + (Boolean) status.get("webhook_configured")), false);
            source.sendSuccess(() -> Component.literal("§7Linked Accounts: " + status.get("linked_accounts")), false);
            source.sendSuccess(() -> Component.literal("§7Server Name: " + status.get("server_name")), false);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to show Discord status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Link account to Discord
     */
    private static int linkAccount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        String discordId = StringArgumentType.getString(context, "discordId");
        
        try {
            DiscordManager discord = DiscordManager.getInstance();
            String uuid = player.getUUID().toString();
            
            // Check if already linked
            if (discord.isLinked(uuid)) {
                source.sendFailure(Component.literal("Your account is already linked to Discord. Use /discord unlink first."));
                return 0;
            }
            
            // Check if Discord ID is already linked to another account
            String existingPlayer = discord.getPlayerUuid(discordId);
            if (existingPlayer != null) {
                source.sendFailure(Component.literal("This Discord ID is already linked to another player."));
                return 0;
            }
            
            // Link the account
            discord.linkAccount(uuid, discordId);
            
            source.sendSuccess(() -> Component.literal("§aSuccessfully linked your account to Discord ID: " + discordId), false);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to link account: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Unlink account from Discord
     */
    private static int unlinkAccount(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        try {
            DiscordManager discord = DiscordManager.getInstance();
            String uuid = player.getUUID().toString();
            
            if (!discord.isLinked(uuid)) {
                source.sendFailure(Component.literal("Your account is not linked to Discord."));
                return 0;
            }
            
            discord.unlinkAccount(uuid);
            source.sendSuccess(() -> Component.literal("§aSuccessfully unlinked your account from Discord"), false);
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to unlink account: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Check if current player is linked
     */
    private static int checkLinked(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        
        try {
            DiscordManager discord = DiscordManager.getInstance();
            String uuid = player.getUUID().toString();
            
            if (discord.isLinked(uuid)) {
                String discordId = discord.getDiscordId(uuid);
                source.sendSuccess(() -> Component.literal("§aYour account is linked to Discord ID: " + discordId), false);
            } else {
                source.sendSuccess(() -> Component.literal("§cYour account is not linked to Discord"), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to check link status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Check if another player is linked (admin command)
     */
    private static int checkOtherLinked(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer targetPlayer = EntityArgument.getPlayer(context, "player");
        
        try {
            DiscordManager discord = DiscordManager.getInstance();
            String uuid = targetPlayer.getUUID().toString();
            
            if (discord.isLinked(uuid)) {
                String discordId = discord.getDiscordId(uuid);
                source.sendSuccess(() -> Component.literal("§a" + targetPlayer.getDisplayName().getString() + " is linked to Discord ID: " + discordId), false);
            } else {
                source.sendSuccess(() -> Component.literal("§c" + targetPlayer.getDisplayName().getString() + " is not linked to Discord"), false);
            }
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to check link status: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Broadcast message to Discord
     */
    private static int broadcastToDiscord(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        String message = StringArgumentType.getString(context, "message");
        
        try {
            DiscordManager discord = DiscordManager.getInstance();
            ServerPlayer sender = null;
            
            if (source.getEntity() instanceof ServerPlayer) {
                sender = (ServerPlayer) source.getEntity();
            }
            
            discord.broadcastToDiscord(message, sender);
            source.sendSuccess(() -> Component.literal("§aBroadcast sent to Discord: " + message), true);
            
            return 1;
            
        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to broadcast to Discord: " + e.getMessage()));
            return 0;
        }
    }
}
