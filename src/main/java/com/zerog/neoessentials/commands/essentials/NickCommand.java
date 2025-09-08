package com.zerog.neoessentials.commands.essentials;
import java.util.Optional;

import com.zerog.neoessentials.util.PermissionUtil;
import com.zerog.neoessentials.permissions.PermissionNodes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;

public class NickCommand {
    
    // Use NickManager for persistent nickname storage
    
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
                .then(Commands.argument("player", StringArgumentType.word())
                    .then(Commands.argument("nickname", StringArgumentType.string())
                        .executes(context -> setNicknameOther(context)))))
            .then(Commands.literal("clear")
                .requires(source -> PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC))
                .then(Commands.argument("player", StringArgumentType.word())
                    .executes(context -> clearNicknameOther(context))))
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
        
        // EssentialsX-style permission nodes
    // PermissionNodes.NICK is checked by command registration, no need for local variable
    boolean canUseColors = PermissionUtil.hasPermissionOrOp(source, PermissionNodes.NICK_COLOR);
    // Use admin permission for bypass (since NICK_BYPASS does not exist)
    boolean canBypass = PermissionUtil.hasPermissionOrOp(source, PermissionNodes.ADMIN_BASIC);

        // Config checks
        var nickManager = com.zerog.neoessentials.managers.NickManager.get();
        if (!nickManager.enabled) {
            sendTranslatedMessage(source, "neoessentials.nick.disabled");
            return 0;
        }

        // Validate nickname length
        if (nickname.length() > 16 && !canBypass) {
            sendTranslatedMessage(source, "neoessentials.nick.too_long");
            return 0;
        }

        // Unsafe character filtering
        if (!nickManager.allowUnsafeCharacters && !nickname.matches("^[a-zA-Z0-9_§&]+$")) {
            sendTranslatedMessage(source, "neoessentials.nick.unsafe_chars");
            return 0;
        }

        // Color code permission
        if ((nickname.contains("&") || nickname.contains("§")) && !canUseColors && !canBypass) {
            sendTranslatedMessage(source, "neoessentials.nick.no_color_permission");
            return 0;
        }

        // Staff-related nick filtering
        if ((nickname.toLowerCase().contains("admin") || 
            nickname.toLowerCase().contains("mod") ||
            nickname.toLowerCase().contains("owner")) && !canBypass) {
            sendTranslatedMessage(source, "neoessentials.nick.no_staff_nick");
            return 0;
        }

        // Set the nickname
        if (target != null) {
            com.zerog.neoessentials.managers.NickManager.get().setNick(target.getUUID(), nickname);

            var sourceEntity = source.getEntity();
            UUID sourceUUID = sourceEntity != null ? sourceEntity.getUUID() : null;
            if (target.getUUID().equals(sourceUUID)) {
                sendTranslatedMessage(source, "neoessentials.nick.set_self", nickname);
            } else {
                sendTranslatedMessage(source, "neoessentials.nick.set_other", target.getName().getString(), nickname);
                MessageUtil.sendTranslatedMessage(target, "neoessentials.nick.set_by_admin", nickname);
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
        if (target != null && com.zerog.neoessentials.managers.NickManager.get().getNick(target.getUUID()).isEmpty()) {
            var sourceEntity = source.getEntity();
            UUID sourceUUID = sourceEntity != null ? sourceEntity.getUUID() : null;
            if (target.getUUID().equals(sourceUUID)) {
                sendTranslatedMessage(source, "neoessentials.nick.not_set_self");
            } else {
                sendTranslatedMessage(source, "neoessentials.nick.not_set_other", target.getName().getString());
            }
            return 0;
        }

        // Clear the nickname
        if (target != null) {
            Optional<String> oldNickname = com.zerog.neoessentials.managers.NickManager.get().getNick(target.getUUID());
            com.zerog.neoessentials.managers.NickManager.get().clearNick(target.getUUID());

            var sourceEntity = source.getEntity();
            UUID sourceUUID = sourceEntity != null ? sourceEntity.getUUID() : null;
            if (target.getUUID().equals(sourceUUID)) {
                sendTranslatedMessage(source, "neoessentials.nick.cleared_self");
            } else {
                sendTranslatedMessage(source, "neoessentials.nick.cleared_other", target.getName().getString());
                MessageUtil.sendTranslatedMessage(target, "neoessentials.nick.cleared_by_admin");
            }

            // Log the change
            source.getServer().sendSystemMessage(Component.literal(
                "§7[Nick] " + getSourceName(source) + " cleared " + target.getName().getString() + "'s nickname (" + oldNickname.orElse("") + ")"));
        }

        return 1;
    }
    
    /**
     * List all active nicknames (admin only)
     */
    private static int listNicknames(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        
        Map<UUID, String> allNicks = com.zerog.neoessentials.managers.NickManager.get().getAllNicks();
        if (allNicks.isEmpty()) {
            sendTranslatedMessage(source, "neoessentials.nick.list_empty");
            return 1;
        }

        sendTranslatedMessage(source, "neoessentials.nick.list_header");
        for (Map.Entry<UUID, String> entry : allNicks.entrySet()) {
            ServerPlayer player = source.getServer().getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                sendTranslatedMessage(source, "neoessentials.nick.list_entry", player.getName().getString(), entry.getValue());
            }
        }

        return 1;
    }
    
    /**
     * Get a player's nickname, or their real name if no nickname is set
     */
    public static String getNickname(ServerPlayer player) {
    Optional<String> nickname = com.zerog.neoessentials.managers.NickManager.get().getNick(player.getUUID());
    return nickname.orElse(player.getName().getString());
    }
    
    /**
     * Get a player's nickname, or null if no nickname is set
     */
    public static String getNicknameOnly(UUID playerUUID) {
    return com.zerog.neoessentials.managers.NickManager.get().getNick(playerUUID).orElse(null);
    }
    
    /**
     * Clear nickname data when a player leaves
     */
    public static void clearPlayerData(UUID playerUUID) {
    com.zerog.neoessentials.managers.NickManager.get().clearNick(playerUUID);
    }
    
    // sendMessage method removed (unused)

    private static void sendTranslatedMessage(CommandSourceStack source, String key, Object... placeholders) {
        if (source.getEntity() instanceof ServerPlayer player) {
            MessageUtil.sendTranslatedMessage(player, key, placeholders);
        } else {
            source.sendSuccess(() -> Component.translatable(key, placeholders), false);
        }
    }
    
    private static String getSourceName(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            return player.getName().getString();
        } else {
            return "Console";
        }
    }
    
    /**
     * Set nickname for another player (admin command)
     */
    private static int setNicknameOther(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("§cPlayer '" + playerName + "' not found or not online"));
            return 0;
        }
        
        return setNickname(context, targetPlayer);
    }
    
    /**
     * Clear nickname for another player (admin command)
     */
    private static int clearNicknameOther(CommandContext<CommandSourceStack> context) {
        String playerName = StringArgumentType.getString(context, "player");
        ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList().getPlayerByName(playerName);
        
        if (targetPlayer == null) {
            context.getSource().sendFailure(Component.literal("§cPlayer '" + playerName + "' not found or not online"));
            return 0;
        }
        
        return clearNickname(context, targetPlayer);
    }
}
