package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;
import com.zerog.neoessentials.moderation.VanishManager;
import com.zerog.neoessentials.chat.AfkManager;
import java.util.List;

/**
 * Implements the /realname command - Shows the real username of a player who has a nickname
 * Helps identify players who are using custom display names
 */
public class RealnameCommand {
    
    // Suggestion provider for player names (real names and nicknames)
    private static final SuggestionProvider<CommandSourceStack> SUGGEST_PLAYERS_AND_NICKS = (context, builder) -> {
        String input = builder.getRemaining().toLowerCase();
        
        for (ServerPlayer player : context.getSource().getServer().getPlayerList().getPlayers()) {
            String realName = player.getName().getString();
            String nickname = NickCommand.getNickname(player.getUUID());
            
            // Suggest real name
            if (realName.toLowerCase().startsWith(input)) {
                builder.suggest(realName);
            }
            
            // Suggest nickname (stripped of color codes)
            if (nickname != null) {
                String cleanNickname = nickname.replaceAll("&[0-9a-fk-or#]", "").replaceAll("&#[0-9a-fA-F]{6}", "");
                if (cleanNickname.toLowerCase().startsWith(input)) {
                    builder.suggest(cleanNickname, com.zerog.neoessentials.util.MessageUtil.component(
                        "commands.neoessentials.realname.nickname_of_hover", realName));
                }
            }
        }
        
        return builder.buildFuture();
    };
    
    /**
     * Register the /realname command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("realname")) return;
        
        dispatcher.register(
            Commands.literal("realname")
                .then(Commands.argument("player", StringArgumentType.word())
                    .suggests(SUGGEST_PLAYERS_AND_NICKS)
                    .executes(ctx -> {
                        PermissionValidator.PermissionResult permResult = 
                            PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.realname");
                        if (!permResult.hasPermission()) {
                            ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                            return 0;
                        }
                        
                        String playerName = StringArgumentType.getString(ctx, "player");
                        return showRealName(ctx.getSource(), playerName);
                    })
                )
                // /realname - Show usage
                .executes(ctx -> {
                    ctx.getSource().sendFailure(MessageUtil.info("commands.neoessentials.realname.usage"));
                    return 0;
                })
        );
    }
    
    /**
     * Show the real name of a player
     */
    private static int showRealName(CommandSourceStack source, String query) {
        List<ServerPlayer> players = source.getServer().getPlayerList().getPlayers();
        
        // First try to find by exact real username
        ServerPlayer exactMatch = players.stream()
            .filter(p -> p.getName().getString().equalsIgnoreCase(query))
            .findFirst()
            .orElse(null);
        
        if (exactMatch != null) {
            return showPlayerInfo(source, exactMatch, true);
        }
        
        // Try to find by nickname (display name)
        List<ServerPlayer> nicknameMatches = players.stream()
            .filter(p -> {
                String nickname = NickCommand.getNickname(p.getUUID());
                if (nickname == null) return false;
                
                // Remove color codes for comparison
                String cleanNickname = nickname.replaceAll("&[0-9a-fk-or]", "").toLowerCase();
                return cleanNickname.equals(query.toLowerCase());
            })
            .toList();
        
        if (nicknameMatches.size() == 1) {
            return showPlayerInfo(source, nicknameMatches.getFirst(), false);
        } else if (nicknameMatches.size() > 1) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.realname.multiple_matches", query));
            
            // Show all matches
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.matches_header"), false);
            for (ServerPlayer player : nicknameMatches) {
                String nickname = NickCommand.getDisplayName(player);
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.match_entry", 
                    nickname, player.getName().getString()), false);
            }
            return 1;
        }
        
        // Try partial matches on both real names and nicknames
        List<ServerPlayer> partialMatches = players.stream()
            .filter(p -> {
                // Check real name
                if (p.getName().getString().toLowerCase().contains(query.toLowerCase())) {
                    return true;
                }
                
                // Check nickname
                String nickname = NickCommand.getNickname(p.getUUID());
                if (nickname != null) {
                    String cleanNickname = nickname.replaceAll("&[0-9a-fk-or]", "").toLowerCase();
                    return cleanNickname.contains(query.toLowerCase());
                }
                
                return false;
            })
            .toList();
        
        if (partialMatches.isEmpty()) {
            source.sendFailure(MessageUtil.error("commands.neoessentials.realname.not_found", query));
            return 0;
        } else if (partialMatches.size() == 1) {
            return showPlayerInfo(source, partialMatches.getFirst(), false);
        } else {
            // Multiple partial matches
            source.sendFailure(MessageUtil.error("commands.neoessentials.realname.multiple_matches", query));
            
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.partial_matches_header"), false);
            for (ServerPlayer player : partialMatches.subList(0, Math.min(10, partialMatches.size()))) {
                String nickname = NickCommand.getNickname(player.getUUID());
                if (nickname != null) {
                    String formattedNick = nickname.replace("&", "§");
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.match_entry", 
                        formattedNick, player.getName().getString()), false);
                } else {
                    source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.no_nick_entry", 
                        player.getName().getString()), false);
                }
            }
            
            if (partialMatches.size() > 10) {
                source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.more_matches", 
                    partialMatches.size() - 10), false);
            }
            
            return 1;
        }
    }
    
    /**
     * Show detailed information about a player
     */
    private static int showPlayerInfo(CommandSourceStack source, ServerPlayer player, boolean searchedByRealName) {
        String realName = player.getName().getString();
        String nickname = NickCommand.getNickname(player.getUUID());
        
        if (nickname == null) {
            // Player has no nickname
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.no_nickname", realName), false);
        } else {
            // Player has a nickname
            String formattedNick = nickname.replace("&", "§");
            
            if (searchedByRealName) {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.realname.by_realname", 
                    realName, formattedNick), false);
            } else {
                source.sendSuccess(() -> MessageUtil.success("commands.neoessentials.realname.by_nickname", 
                    formattedNick, realName), false);
            }
        }
        
        // Additional player info
        source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.player_info",
            realName,
            player.getUUID().toString(),
            player.level().toString(),
            String.format("%.1f, %.1f, %.1f", player.getX(), player.getY(), player.getZ())), false);
        
        // Show if player has any special status
        StringBuilder statusBuilder = new StringBuilder();
        
        if (com.zerog.neoessentials.util.PermissionLevelCompat.hasPermission(player, 4)) {
            statusBuilder.append("§cOperator");
        }
        
        // Check for vanish status
        VanishManager vanishManager = VanishManager.getInstance();
        if (vanishManager.isPlayerVanished(player.getUUID())) {
            if (statusBuilder.length() > 0) statusBuilder.append("§7, ");
            statusBuilder.append("§7Vanished");
        }
        
        // Check for AFK status
        AfkManager afkManager = AfkManager.getInstance();
        if (afkManager.isAfk(player)) {
            if (statusBuilder.length() > 0) statusBuilder.append("§7, ");
            statusBuilder.append("§eAFK");
        }
        
        if (statusBuilder.length() > 0) {
            source.sendSuccess(() -> MessageUtil.info("commands.neoessentials.realname.status", statusBuilder.toString()), false);
        }
        
        return 1;
    }
}