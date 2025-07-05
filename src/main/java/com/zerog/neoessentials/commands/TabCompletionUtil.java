package com.zerog.neoessentials.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.HomeManager;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.data.WarpManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utilities for providing tab completion suggestions in commands.
 * This class contains only non-economy related tab completion providers.
 */
public class TabCompletionUtil {

    // Common amounts for various commands
    public static final String[] COMMON_AMOUNTS = {
        "1", "5", "10", "25", "50", "100", "500", "1000"
    };

    // Stack amount suggestions
    public static final String[] STACK_AMOUNTS = {
        "1", "8", "16", "32", "64"
    };

    // Price suggestions for economy-related commands (default values)
    public static final String[] PRICE_VALUES = {
        "0.0", "1.0", "5.0", "10.0", "25.0", "50.0", "100.0", "500.0", "1000.0"
    };

    // Item name suggestions (simple strings)
    public static final String[] ITEM_NAMES = {
        "sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots"
    };

    // Time units
    public static final String[] TIME_UNITS = {
        "s", "m", "h", "d", "w", "seconds", "minutes", "hours", "days", "weeks"
    };

    // Weather types
    public static final String[] WEATHER_TYPES = {
        "clear", "rain", "thunder"
    };

    // Time options
    public static final String[] TIME_OPTIONS = {
        "day", "night", "noon", "midnight"
    };

    /**
     * Provides home name suggestions for the current player.
     */
    public static final SuggestionProvider<CommandSourceStack> HOME_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            HomeManager homeManager = NeoEssentials.getInstance().getDataManager().getHomeManager();
            
            if (homeManager != null) {
                Set<String> homeNames = homeManager.getHomes(player.getUUID()).keySet();
                return SharedSuggestionProvider.suggest(homeNames, builder);
            }
        } catch (Exception e) {
            // Fallback to common home names
            return SharedSuggestionProvider.suggest(
                new String[]{"home", "base", "farm", "mine"}, 
                builder
            );
        }
        return Suggestions.empty();
    };

    /**
     * Provides warp name suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> WARP_SUGGESTIONS = (context, builder) -> {
        try {
            WarpManager warpManager = NeoEssentials.getInstance().getDataManager().getWarpManager();
            
            if (warpManager != null) {
                Set<String> warpNames = warpManager.getAllWarps().keySet();
                return SharedSuggestionProvider.suggest(warpNames, builder);
            }
        } catch (Exception e) {
            // Fallback to common warp names
            return SharedSuggestionProvider.suggest(
                new String[]{"spawn", "pvp", "resource", "end", "nether"}, 
                builder
            );
        }
        return Suggestions.empty();
    };

    /**
     * Provides kit name suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> KIT_SUGGESTIONS = (context, builder) -> {
        try {
            KitManager kitManager = NeoEssentials.getInstance().getDataManager().getKitManager();
            
            if (kitManager != null) {
                Set<String> kitNames = kitManager.getAllKits().keySet();
                return SharedSuggestionProvider.suggest(kitNames, builder);
            }
        } catch (Exception e) {
            // Fallback to common kit names
            return SharedSuggestionProvider.suggest(
                new String[]{"starter", "tools", "food", "armor", "building"}, 
                builder
            );
        }
        return Suggestions.empty();
    };

    /**
     * Provides online player suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> ONLINE_PLAYER_SUGGESTIONS = (context, builder) -> {
        try {
            List<String> playerNames = context.getSource().getServer().getPlayerList().getPlayers()
                .stream()
                .map(player -> player.getGameProfile().getName())
                .collect(Collectors.toList());
            return SharedSuggestionProvider.suggest(playerNames, builder);
        } catch (Exception e) {
            return Suggestions.empty();
        }
    };

    /**
     * Provides item name suggestions (Minecraft items only).
     */
    public static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        try {
            String remaining = builder.getRemaining().toLowerCase();
            
            List<String> itemNames = BuiltInRegistries.ITEM.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .filter(name -> name.toLowerCase().contains(remaining))
                .limit(20)
                .collect(Collectors.toList());
            
            // If no matches, provide some common items
            if (itemNames.isEmpty()) {
                itemNames = List.of(
                    "minecraft:diamond", "minecraft:iron_ingot", "minecraft:gold_ingot",
                    "minecraft:coal", "minecraft:emerald", "minecraft:stone"
                );
            }
                
            return SharedSuggestionProvider.suggest(itemNames, builder);
        } catch (Exception e) {
            // Fallback to common items
            return SharedSuggestionProvider.suggest(
                new String[]{
                    "minecraft:diamond", "minecraft:iron_ingot", "minecraft:gold_ingot",
                    "minecraft:coal", "minecraft:emerald", "minecraft:stone",
                    "minecraft:dirt", "minecraft:wood", "minecraft:bread"
                }, 
                builder
            );
        }
    };

    /**
     * Provides common amount suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> AMOUNT_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(COMMON_AMOUNTS, builder);
    };

    /**
     * Provides time unit suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> TIME_UNIT_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(TIME_UNITS, builder);
    };

    /**
     * Provides weather type suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> WEATHER_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(WEATHER_TYPES, builder);
    };

    /**
     * Provides time of day suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> TIME_OF_DAY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(TIME_OPTIONS, builder);
    };

    /**
     * Provides quantity suggestions based on context.
     */
    public static final SuggestionProvider<CommandSourceStack> QUANTITY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            new String[]{"1", "4", "8", "16", "32", "64", "128", "256"}, 
            builder
        );
    };

    /**
     * Provides stack amount suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> STACK_AMOUNT_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(STACK_AMOUNTS, builder);
    };

    /**
     * Provides price suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> PRICE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(PRICE_VALUES, builder);
    };

    /**
     * Provides item name suggestions (simple names).
     */
    public static final SuggestionProvider<CommandSourceStack> ITEM_NAME_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(ITEM_NAMES, builder);
    };
}
