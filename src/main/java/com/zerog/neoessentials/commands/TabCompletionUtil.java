package com.zerog.neoessentials.commands;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.data.HomeManager;
import com.zerog.neoessentials.data.WarpManager;
import com.zerog.neoessentials.data.KitManager;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.BankManager;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.Shop;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class providing common tab completion suggestions for NeoEssentials commands.
 * Centralizes suggestion logic to ensure consistency across all commands.
 */
public class TabCompletionUtil {
    
    // Common amount suggestions for economy commands
    public static final String[] COMMON_AMOUNTS = {
        "10", "25", "50", "100", "250", "500", "1000", "2500", "5000", "10000"
    };
    
    // Bank account types
    public static final String[] BANK_ACCOUNT_TYPES = {
        "checking", "savings", "business", "investment"
    };
    
    // Shop types
    public static final String[] SHOP_TYPES = {
        "general", "food", "tools", "weapons", "armor", "blocks", "redstone", "magic"
    };
    
    // Shop ownership types
    public static final String[] SHOP_OWNERSHIP_TYPES = {
        "player", "server", "auction"
    };
    
    // Time units for various commands
    public static final String[] TIME_UNITS = {
        "s", "seconds", "m", "minutes", "h", "hours", "d", "days", "w", "weeks"
    };
    
    // Weather types
    public static final String[] WEATHER_TYPES = {
        "clear", "rain", "thunder"
    };
    
    // Time of day options
    public static final String[] TIME_OPTIONS = {
        "day", "night", "noon", "midnight", "sunrise", "sunset"
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
                new String[]{"home", "base", "shop", "farm", "mine"}, 
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
                new String[]{"spawn", "mall", "pvp", "resource", "end", "nether"}, 
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
     * Provides bank account suggestions for the current player.
     */
    public static final SuggestionProvider<CommandSourceStack> BANK_ACCOUNT_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            EconomyManager economyManager = EconomyManager.getInstance();
            BankManager bankManager = economyManager.getBankManager();
            
            List<String> accountNames = bankManager.getPlayerAccounts(player.getUUID())
                .stream()
                .map(account -> account.getAccountNumber())
                .collect(Collectors.toList());
                
            return SharedSuggestionProvider.suggest(accountNames, builder);
        } catch (Exception e) {
            // Fallback to common account names
            return SharedSuggestionProvider.suggest(
                new String[]{"main", "savings", "business", "shared"}, 
                builder
            );
        }
    };
    
    /**
     * Provides shop name suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> SHOP_SUGGESTIONS = (context, builder) -> {
        try {
            // For now, use searchShops with empty search to get shops
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            List<String> shopNames = shopManager.searchShops("", 50)
                .stream()
                .map(Shop::getName)
                .collect(Collectors.toList());
                
            return SharedSuggestionProvider.suggest(shopNames, builder);
        } catch (Exception e) {
            // Fallback to common shop names
            return SharedSuggestionProvider.suggest(
                new String[]{"general", "tools", "food", "materials"}, 
                builder
            );
        }
    };
    
    /**
     * Provides shop name suggestions based on player permissions (only shops they can manage).
     */
    public static final SuggestionProvider<CommandSourceStack> MANAGEABLE_SHOP_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            List<String> shopNames = shopManager.getAllShops()
                .stream()
                .filter(shop -> shop.hasPermission(player.getUUID(), 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.VIEW_BASIC_INFO))
                .map(Shop::getName)
                .collect(Collectors.toList());
                
            return SharedSuggestionProvider.suggest(shopNames, builder);
        } catch (Exception e) {
            // Fallback to SHOP_SUGGESTIONS
            return SHOP_SUGGESTIONS.getSuggestions(context, builder);
        }
    };
    
    /**
     * Provides employee role suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> EMPLOYEE_ROLE_SUGGESTIONS = (context, builder) -> {
        String[] roles = java.util.Arrays.stream(com.zerog.neoessentials.economy.ShopEmployeeManager.EmployeeRole.values())
            .map(role -> role.name().toLowerCase())
            .toArray(String[]::new);
        return SharedSuggestionProvider.suggest(roles, builder);
    };
    
    /**
     * Provides online player suggestions for hiring employees.
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
     * Provides shop item suggestions (items currently in the shop).
     */
    public static final SuggestionProvider<CommandSourceStack> SHOP_ITEM_SUGGESTIONS = (context, builder) -> {
        try {
            // Try to get shop name from previous argument
            String shopName = getShopNameFromContext(context);
            if (shopName != null) {
                EconomyManager economyManager = EconomyManager.getInstance();
                ShopManager shopManager = economyManager.getShopManager();
                Shop shop = shopManager.getShopByName(shopName);
                
                if (shop != null) {
                    List<String> itemNames = shop.getInventory().values()
                        .stream()
                        .map(item -> item.getItemName())
                        .collect(Collectors.toList());
                    return SharedSuggestionProvider.suggest(itemNames, builder);
                }
            }
            
            // Fallback to general item registry
            List<String> itemNames = BuiltInRegistries.ITEM.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .filter(name -> name.contains(builder.getRemaining().toLowerCase()))
                .limit(20)
                .collect(Collectors.toList());
            return SharedSuggestionProvider.suggest(itemNames, builder);
        } catch (Exception e) {
            // Final fallback to common items
            return SharedSuggestionProvider.suggest(
                new String[]{
                    "minecraft:diamond", "minecraft:iron_ingot", "minecraft:gold_ingot",
                    "minecraft:coal", "minecraft:emerald", "minecraft:stone"
                }, 
                builder
            );
        }
    };
    
    /**
     * Provides shop category suggestions based on existing shops.
     */
    public static final SuggestionProvider<CommandSourceStack> DYNAMIC_SHOP_CATEGORY_SUGGESTIONS = (context, builder) -> {
        try {
            EconomyManager economyManager = EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Set<String> categories = shopManager.getAllShops()
                .stream()
                .map(Shop::getCategory)
                .filter(category -> category != null && !category.isEmpty())
                .collect(Collectors.toSet());
            
            // Add common categories
            categories.addAll(List.of(SHOP_TYPES));
            
            return SharedSuggestionProvider.suggest(categories, builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(SHOP_TYPES, builder);
        }
    };
    
    /**
     * Helper method to extract shop name from command context.
     */
    private static String getShopNameFromContext(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        try {
            return com.mojang.brigadier.arguments.StringArgumentType.getString(context, "shop");
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Provides item name suggestions (Minecraft items).
     */
    public static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        try {
            List<String> itemNames = BuiltInRegistries.ITEM.stream()
                .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                .filter(name -> name.contains(builder.getRemaining().toLowerCase()))
                .limit(20) // Limit suggestions to prevent overwhelming
                .collect(Collectors.toList());
                
            return SharedSuggestionProvider.suggest(itemNames, builder);
        } catch (Exception e) {
            // Fallback to common items
            return SharedSuggestionProvider.suggest(
                new String[]{
                    "minecraft:diamond", "minecraft:iron_ingot", "minecraft:gold_ingot",
                    "minecraft:coal", "minecraft:emerald", "minecraft:stone",
                    "minecraft:dirt", "minecraft:wood", "minecraft:food"
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
     * Provides bank account type suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> BANK_ACCOUNT_TYPE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(BANK_ACCOUNT_TYPES, builder);
    };
    
    /**
     * Provides shop type suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> SHOP_TYPE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(SHOP_TYPES, builder);
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
     * Provides price suggestions for items.
     */
    public static final SuggestionProvider<CommandSourceStack> PRICE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(
            new String[]{"1.0", "5.0", "10.0", "25.0", "50.0", "100.0", "250.0", "500.0"}, 
            builder
        );
    };
    
    /**
     * Provides shop ownership type suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> SHOP_OWNERSHIP_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(SHOP_OWNERSHIP_TYPES, builder);
    };
}
