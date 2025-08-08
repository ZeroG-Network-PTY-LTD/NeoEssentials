package com.zerog.neoessentials.commands.essentials;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class NickCommand {
    
    // Store player nicknames
    private static final Map<UUID, String> nicknames = new HashMap<>();
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("nick")
            // Set your own nickname
            .then(Commands.argument("nickname", StringArgumentType.string())
                .executes(context -> setNickname(context, null)))
            // Clear your own nickname
            .then(Commands.literal("off")
                .executes(context -> clearNickname(context, null)))
            // Admin commands
            .then(Commands.literal("set")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("nickname", StringArgumentType.string())
                        .executes(context -> setNickname(context, EntityArgument.getPlayer(context, "player"))))))
            .then(Commands.literal("clear")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> clearNickname(context, EntityArgument.getPlayer(context, "player")))))
            .then(Commands.literal("list")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .executes(NickCommand::listNicknames)));
    }
    
    /**
     * Set a player's nickname
     */
    private static int setNickname(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        CommandSourceStack source = context.getSource();
        String nickname = StringArgumentType.getString(context, "nickname");
        
        // Determine target player
        ServerPlayer target = targetPlayer;
        if (target == null) {
            if (!(source.getEntity() instanceof ServerPlayer)) {
                source.sendFailure(Component.literal("§cOnly players can set their own nickname"));
                return 0;
            }
            target = (ServerPlayer) source.getEntity();
        }
        
        // Validate nickname
        if (nickname.length() > 16) {
            sendMessage(source, "§cNickname must be 16 characters or less!");
            return 0;
        }
        
        if (nickname.contains("&") || nickname.contains("§")) {
            // Allow color codes for admins
            if (!PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC)) {
                sendMessage(source, "§cYou cannot use color codes in nicknames!");
                return 0;
            }
        }
        
        // Check for inappropriate content (basic)
        if (nickname.toLowerCase().contains("admin") || 
            nickname.toLowerCase().contains("mod") ||
            nickname.toLowerCase().contains("owner")) {
            if (!PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_FULL)) {
                sendMessage(source, "§cYou cannot use staff-related nicknames!");
                return 0;
            }
        }
        
        // Set the nickname
        if (target != null) {
            nicknames.put(target.getUUID(), nickname);
            
            // Update display name (this would require mixins for full functionality)
            // For now, just store it in our map
            
            var sourceEntity = source.getEntity();
            UUID sourceUUID = sourceEntity != null ? sourceEntity.getUUID() : null;
            if (target.getUUID().equals(sourceUUID)) {
                sendMessage(source, "§aYour nickname has been set to: §f" + nickname);
            } else {
                sendMessage(source, "§aSet " + target.getName().getString() + "'s nickname to: §f" + nickname);
                MessageUtil.sendMessage(target, "§aYour nickname has been set to: §f" + nickname + " §7by an admin");
            }
            
            // Log the change
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Nick] " + getSourceName(source) + " set " + target.getName().getString() + "'s nickname to: " + nickname));
        }
        
        return 1;
    }
    
    /**
     * Clear a player's nickname
     */
    private static int clearNickname(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer) {
        CommandSourceStack source = context.getSource();
        
        // Determine target player
        ServerPlayer target = targetPlayer;
        if (target == null) {
            if (!(source.getEntity() instanceof ServerPlayer)) {
                source.sendFailure(Component.literal("§cOnly players can clear their own nickname"));
                return 0;
            }
            target = (ServerPlayer) source.getEntity();
        }
        
        // Check if player has a nickname
        if (target != null && !nicknames.containsKey(target.getUUID())) {
            var sourceEntity = source.getEntity();
            UUID sourceUUID = sourceEntity != null ? sourceEntity.getUUID() : null;
            if (target.getUUID().equals(sourceUUID)) {
                sendMessage(source, "§cYou don't have a nickname set!");
            } else {
                sendMessage(source, "§c" + target.getName().getString() + " doesn't have a nickname set!");
            }
            return 0;
        }
        
        // Clear the nickname
        if (target != null) {
            String oldNickname = nicknames.remove(target.getUUID());
            
            var sourceEntity = source.getEntity();
            UUID sourceUUID = sourceEntity != null ? sourceEntity.getUUID() : null;
            if (target.getUUID().equals(sourceUUID)) {
                sendMessage(source, "§aYour nickname has been cleared");
            } else {
                sendMessage(source, "§aCleared " + target.getName().getString() + "'s nickname");
                MessageUtil.sendMessage(target, "§aYour nickname has been cleared by an admin");
            }
            
            // Log the change
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Nick] " + getSourceName(source) + " cleared " + target.getName().getString() + "'s nickname (" + oldNickname + ")"));
        }
        
        return 1;
    }
    
    /**
     * List all active nicknames (admin only)
     */
    private static int listNicknames(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        if (nicknames.isEmpty()) {
            sendMessage(source, "§eNo players have nicknames set");
            return 1;
        }
        
        sendMessage(source, "§6===== §eActive Nicknames §6=====");
        for (Map.Entry<UUID, String> entry : nicknames.entrySet()) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                sendMessage(source, "§a" + player.getName().getString() + " §7→ §f" + entry.getValue());
            }
        }
        
        return 1;
    }
    
    /**
     * Get a player's nickname, or their real name if no nickname is set
     */
    public static String getNickname(ServerPlayer player) {
        String nickname = nicknames.get(player.getUUID());
        return nickname != null ? nickname : player.getName().getString();
    }
    
    /**
     * Get a player's nickname, or null if no nickname is set
     */
    public static String getNicknameOnly(UUID playerUUID) {
        return nicknames.get(playerUUID);
    }
    
    /**
     * Clear nickname data when a player leaves
     */
    public static void clearPlayerData(UUID playerUUID) {
        nicknames.remove(playerUUID);
    }
    
    private static void sendMessage(CommandSourceStack source, String message) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendMessage(player, message);
        } else {
            source.sendSuccess(() -> Component.literal(message), false);
        }
    }
    
    private static String getSourceName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        } else {
            return "Console";
        }
    }
}
