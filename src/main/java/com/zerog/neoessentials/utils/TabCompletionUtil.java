package com.zerog.neoessentials.utils;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.managers.HomeManager;
import com.zerog.neoessentials.managers.WarpManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Advanced tab completion utility for NeoEssentials commands
 * Provides intelligent suggestions based on context and permissions
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class TabCompletionUtil {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(TabCompletionUtil.class);
    
    // Player name suggestions
    public static final SuggestionProvider<CommandSourceStack> PLAYER_NAMES = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            context.getSource().getServer().getPlayerNames(),
            builder
        );
    };
    
    // Online player names (excluding command sender)
    public static final SuggestionProvider<CommandSourceStack> OTHER_PLAYERS = (context, builder) -> {
        String[] playerNames = context.getSource().getServer().getPlayerNames();
        ServerPlayer sender = context.getSource().getPlayer();
        
        List<String> otherPlayers = Arrays.stream(playerNames)
            .filter(name -> sender == null || !name.equals(sender.getName().getString()))
            .collect(Collectors.toList());
            
        return SharedSuggestionProvider.suggest(otherPlayers, builder);
    };
    
    // Home name suggestions for the command sender
    public static final SuggestionProvider<CommandSourceStack> HOME_NAMES = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return Suggestions.empty();
        }
        
        HomeManager homeManager = HomeManager.getInstance();
        List<String> homes = homeManager.getPlayerHomes(player.getUUID());
        
        return SharedSuggestionProvider.suggest(homes, builder);
    };
    
    // Warp name suggestions
    public static final SuggestionProvider<CommandSourceStack> WARP_NAMES = (context, builder) -> {
        WarpManager warpManager = WarpManager.getInstance();
        List<String> warps = warpManager.getAllWarps().stream()
            .map(warp -> warp.name)
            .collect(Collectors.toList());
            
        return SharedSuggestionProvider.suggest(warps, builder);
    };
    
    // Kit name suggestions
    public static final SuggestionProvider<CommandSourceStack> KIT_NAMES = (context, builder) -> {
        ServerPlayer player = context.getSource().getPlayer();
        if (player == null) {
            return SharedSuggestionProvider.suggest(Arrays.asList("starter", "tools", "food"), builder);
        }
        
        // Get available kits for the player based on permissions
        List<String> availableKits = new ArrayList<>();
        
        // Add basic kits that all players can access
        availableKits.add("starter");
        
        // Add permission-based kits
        if (player.hasPermissions(2)) {
            availableKits.addAll(Arrays.asList("tools", "food", "building", "armor"));
        }
        
        if (player.hasPermissions(3)) {
            availableKits.addAll(Arrays.asList("admin", "creative", "debug"));
        }
        
        return SharedSuggestionProvider.suggest(availableKits, builder);
    };
    
    // World/dimension suggestions
    public static final SuggestionProvider<CommandSourceStack> WORLD_NAMES = (context, builder) -> {
        Set<String> worldNames = new HashSet<>();
        for (var level : context.getSource().getServer().getAllLevels()) {
            worldNames.add(level.dimension().location().getPath());
        }
            
        return SharedSuggestionProvider.suggest(worldNames, builder);
    };
    
    // Boolean suggestions (true/false)
    public static final SuggestionProvider<CommandSourceStack> BOOLEAN_VALUES = (context, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.asList("true", "false"), builder);
    };
    
    // Gamemode suggestions
    public static final SuggestionProvider<CommandSourceStack> GAMEMODE_NAMES = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.asList("survival", "creative", "adventure", "spectator"), 
            builder
        );
    };
    
    // Time format suggestions (for teleport warmup, cooldowns, etc.)
    public static final SuggestionProvider<CommandSourceStack> TIME_VALUES = (context, builder) -> {
        List<String> timeExamples = Arrays.asList(
            "5s", "10s", "30s", "1m", "2m", "5m", "10m", "30m", "1h"
        );
        return SharedSuggestionProvider.suggest(timeExamples, builder);
    };
    
    // Currency amount suggestions
    public static final SuggestionProvider<CommandSourceStack> CURRENCY_AMOUNTS = (context, builder) -> {
        List<String> amounts = Arrays.asList(
            "10", "50", "100", "500", "1000", "5000", "10000"
        );
        return SharedSuggestionProvider.suggest(amounts, builder);
    };
    
    // Mail management suggestions
    public static final SuggestionProvider<CommandSourceStack> MAIL_ACTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.asList("read", "clear", "send"), 
            builder
        );
    };
    
    // Moderation action suggestions
    public static final SuggestionProvider<CommandSourceStack> MODERATION_ACTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.asList("kick", "mute", "unmute", "jail", "unjail", "ban", "unban"), 
            builder
        );
    };
    
    // Economy action suggestions
    public static final SuggestionProvider<CommandSourceStack> ECONOMY_ACTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.asList("give", "take", "set", "reset", "top", "balance"), 
            builder
        );
    };
    
    // Permission node suggestions (basic ones)
    public static final SuggestionProvider<CommandSourceStack> PERMISSION_NODES = (context, builder) -> {
        List<String> permissions = Arrays.asList(
            "neoessentials.home",
            "neoessentials.warp",
            "neoessentials.kit",
            "neoessentials.teleport",
            "neoessentials.economy",
            "neoessentials.admin",
            "neoessentials.moderator",
            "neoessentials.bypass.cooldown",
            "neoessentials.bypass.cost"
        );
        return SharedSuggestionProvider.suggest(permissions, builder);
    };
    
    // TPA action suggestions
    public static final SuggestionProvider<CommandSourceStack> TPA_ACTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            Arrays.asList("accept", "deny", "cancel", "here"), 
            builder
        );
    };
    
    // Advanced home name suggestions with fuzzy matching
    public static CompletableFuture<Suggestions> suggestPlayerHomes(CommandContext<CommandSourceStack> context, 
                                                                   SuggestionsBuilder builder, 
                                                                   String targetPlayerName) {
        try {
            ServerPlayer targetPlayer = context.getSource().getServer().getPlayerList()
                .getPlayerByName(targetPlayerName);
            
            if (targetPlayer != null) {
                HomeManager homeManager = HomeManager.getInstance();
                List<String> homes = homeManager.getPlayerHomes(targetPlayer.getUUID());
                return SharedSuggestionProvider.suggest(homes, builder);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get home suggestions for player {}: {}", targetPlayerName, e.getMessage());
        }
        
        return Suggestions.empty();
    }
    
    // Smart suggestions based on partial input
    public static CompletableFuture<Suggestions> smartSuggest(List<String> options, 
                                                             SuggestionsBuilder builder) {
        String input = builder.getRemaining().toLowerCase();
        
        // Exact matches first
        List<String> exactMatches = options.stream()
            .filter(option -> option.toLowerCase().startsWith(input))
            .sorted()
            .collect(Collectors.toList());
        
        // Fuzzy matches if no exact matches
        if (exactMatches.isEmpty() && input.length() > 1) {
            exactMatches = options.stream()
                .filter(option -> option.toLowerCase().contains(input))
                .sorted()
                .collect(Collectors.toList());
        }
        
        return SharedSuggestionProvider.suggest(exactMatches, builder);
    }
    
    // Context-aware suggestions for complex commands
    public static final SuggestionProvider<CommandSourceStack> CONTEXT_AWARE = (context, builder) -> {
        String[] args = context.getInput().split(" ");
        
        // Suggest based on command structure
        if (args.length <= 2) {
            return PLAYER_NAMES.getSuggestions(context, builder);
        } else if (args.length == 3) {
            String command = args[0];
            if (command.contains("home")) {
                return HOME_NAMES.getSuggestions(context, builder);
            } else if (command.contains("warp")) {
                return WARP_NAMES.getSuggestions(context, builder);
            } else if (command.contains("kit")) {
                return KIT_NAMES.getSuggestions(context, builder);
            }
        }
        
        return Suggestions.empty();
    };
    
    /**
     * Get suggestions for numeric values with reasonable ranges
     */
    public static CompletableFuture<Suggestions> suggestNumericRange(SuggestionsBuilder builder, 
                                                                    int min, int max, int step) {
        List<String> values = new ArrayList<>();
        for (int i = min; i <= max; i += step) {
            values.add(String.valueOf(i));
        }
        return SharedSuggestionProvider.suggest(values, builder);
    }
    
    /**
     * Get suggestions with permission checking
     */
    public static CompletableFuture<Suggestions> suggestWithPermission(CommandContext<CommandSourceStack> context,
                                                                      SuggestionsBuilder builder,
                                                                      List<String> options,
                                                                      String requiredPermission) {
        ServerPlayer player = context.getSource().getPlayer();
        if (player != null && !player.hasPermissions(2)) { // Basic permission check
            return Suggestions.empty();
        }
        
        return SharedSuggestionProvider.suggest(options, builder);
    }
}
