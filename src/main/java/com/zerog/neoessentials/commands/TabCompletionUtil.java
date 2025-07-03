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

import java.util.Collections;
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
    
    // Common stack amounts for item enhancement commands
    public static final String[] STACK_AMOUNTS = {
        "1", "8", "16", "32", "48", "64"
    };
    
    // Common item names for demonstration
    public static final String[] EXAMPLE_ITEM_NAMES = {
        "§cFire Sword", "§9Ice Axe", "§aEnchanted Bow", "§6Golden Tool", 
        "§5Magic Wand", "§bDiamond Pickaxe", "§eShiny Helmet", "§dRare Gem"
    };
    
    // Bank account types
    public static final String[] BANK_ACCOUNT_TYPES = {
        "checking", "savings", "business", "investment"
    };
    
    // Shop types
    public static final String[] SHOP_TYPES = {
        "general", "food", "tools", "weapons", "armor", "blocks", "redstone", "magic"
    };
    
    // Shop categories for the new shop system
    public static final String[] SHOP_CATEGORIES = {
        "general", "armor", "weapons", "tools", "blocks", "food", "potions", 
        "enchanted", "rare", "building", "decoration", "redstone", "transportation", 
        "farming", "mining"
    };
    
    // Shop ownership types
    public static final String[] SHOP_OWNERSHIP_TYPES = {
        "player", "server", "auction"
    };
    
    // Auction types for the new auction system
    public static final String[] AUCTION_TYPES = {
        "standard", "buyitnow", "reserve", "dutch"
    };
    
    // Auction categories for filtering
    public static final String[] AUCTION_CATEGORIES = {
        "all", "armor", "weapons", "tools", "blocks", "food", "potions", 
        "enchanted", "rare", "building", "decoration", "redstone", "transportation", 
        "farming", "mining", "ended", "active"
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
     * Provides currency suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> CURRENCY_SUGGESTIONS = (context, builder) -> {
        try {
            com.zerog.neoessentials.economy.CurrencyManager currencyManager = 
                com.zerog.neoessentials.economy.CurrencyManager.getInstance();
            
            if (currencyManager != null) {
                List<String> currencyIds = currencyManager.getAllCurrencies().stream()
                    .map(com.zerog.neoessentials.economy.Currency::getId)
                    .collect(Collectors.toList());
                return SharedSuggestionProvider.suggest(currencyIds, builder);
            }
        } catch (Exception e) {
            // Fallback to common currency names
            return SharedSuggestionProvider.suggest(
                new String[]{"coins", "gold", "emeralds", "gems", "credits"}, 
                builder
            );
        }
        return Suggestions.empty();
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
                .map(Shop::getShopName)
                .sorted()
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
                .filter(shop -> {
                    // Player can manage if they own the shop or have management permissions
                    return shop.getOwnerId().equals(player.getUUID()) || 
                           shop.getEmployeeManager().hasPermission(player.getUUID(), 
                               com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY);
                })
                .map(Shop::getShopName)
                .sorted()
                .collect(Collectors.toList());
                
            return SharedSuggestionProvider.suggest(shopNames, builder);
        } catch (Exception e) {
            // Fallback to basic shop names
            try {
                EconomyManager economyManager = EconomyManager.getInstance();
                ShopManager shopManager = economyManager.getShopManager();
                
                List<String> allShopNames = shopManager.getAllShops()
                    .stream()
                    .map(Shop::getShopName)
                    .sorted()
                    .collect(Collectors.toList());
                    
                return SharedSuggestionProvider.suggest(allShopNames, builder);
            } catch (Exception ex) {
                return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
            }
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
     * Provides item name suggestions (Minecraft + Modded items) with enhanced filtering.
     */
    public static final SuggestionProvider<CommandSourceStack> ITEM_SUGGESTIONS = (context, builder) -> {
        try {
            String remaining = builder.getRemaining().toLowerCase();
            
            // Use the enhanced search from ItemHandler
            List<String> itemNames = com.zerog.neoessentials.economy.ItemHandler.searchItems(remaining);
            
            // If no matches found with search, fall back to basic filtering
            if (itemNames.isEmpty() && !remaining.isEmpty()) {
                itemNames = BuiltInRegistries.ITEM.stream()
                    .map(item -> BuiltInRegistries.ITEM.getKey(item).toString())
                    .filter(name -> name.toLowerCase().contains(remaining))
                    .limit(20)
                    .collect(Collectors.toList());
            }
            
            // If still no matches, provide some common items
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
     * Provides mod-specific item suggestions for advanced users.
     */
    public static final SuggestionProvider<CommandSourceStack> MODDED_ITEM_SUGGESTIONS = (context, builder) -> {
        try {
            String remaining = builder.getRemaining().toLowerCase();
            
            // If user typed a mod namespace (e.g., "thermal:"), suggest items from that mod
            if (remaining.contains(":")) {
                String[] parts = remaining.split(":", 2);
                String modId = parts[0];
                String itemPart = parts.length > 1 ? parts[1] : "";
                
                List<String> modItems = com.zerog.neoessentials.economy.ItemHandler.getItemsFromMod(modId)
                    .stream()
                    .filter(item -> item.toLowerCase().contains(itemPart))
                    .limit(20)
                    .collect(Collectors.toList());
                
                if (!modItems.isEmpty()) {
                    return SharedSuggestionProvider.suggest(modItems, builder);
                }
            }
            
            // Otherwise, suggest loaded mod namespaces
            List<String> modIds = com.zerog.neoessentials.economy.ItemHandler.getLoadedModIds();
            List<String> suggestions = modIds.stream()
                .filter(modId -> modId.toLowerCase().contains(remaining))
                .map(modId -> modId + ":")
                .limit(10)
                .collect(Collectors.toList());
            
            // Also include some items that match the query
            suggestions.addAll(
                com.zerog.neoessentials.economy.ItemHandler.searchItems(remaining)
                    .stream()
                    .filter(item -> !item.startsWith("minecraft:"))
                    .limit(10)
                    .collect(Collectors.toList())
            );
            
            return SharedSuggestionProvider.suggest(suggestions, builder);
        } catch (Exception e) {
            return ITEM_SUGGESTIONS.getSuggestions(context, builder);
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
    
    /**
     * Provides mod ID suggestions for modded item commands.
     */
    public static final SuggestionProvider<CommandSourceStack> MOD_ID_SUGGESTIONS = (context, builder) -> {
        try {
            List<String> modIds = com.zerog.neoessentials.economy.ItemHandler.getLoadedModIds();
            String remaining = builder.getRemaining().toLowerCase();
            
            List<String> filtered = modIds.stream()
                .filter(modId -> modId.toLowerCase().contains(remaining))
                .limit(15)
                .collect(Collectors.toList());
            
            return SharedSuggestionProvider.suggest(filtered, builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(new String[]{"minecraft"}, builder);
        }
    };
    
    /**
     * Provides auction ID suggestions for auction commands.
     */
    public static final SuggestionProvider<CommandSourceStack> AUCTION_ID_SUGGESTIONS = (context, builder) -> {
        try {
            // Try new auction system first
            com.zerog.neoessentials.economy.AuctionManagerNew auctionManagerNew = 
                com.zerog.neoessentials.economy.AuctionManagerNew.getInstance();
            if (auctionManagerNew != null) {
                List<String> auctionIds = auctionManagerNew.getActiveAuctions()
                    .stream()
                    .map(auction -> auction.getAuctionId().toString())
                    .limit(20)
                    .collect(Collectors.toList());
                
                // Also provide shortened versions (first 8 characters)
                List<String> shortIds = auctionManagerNew.getActiveAuctions()
                    .stream()
                    .map(auction -> auction.getAuctionId().toString().substring(0, 8))
                    .limit(20)
                    .collect(Collectors.toList());
                
                auctionIds.addAll(shortIds);
                return SharedSuggestionProvider.suggest(auctionIds, builder);
            }
            
            // Fallback to legacy auction system
            EconomyManager economyManager = EconomyManager.getInstance();
            if (economyManager != null) {
                ShopManager shopManager = economyManager.getShopManager();
                if (shopManager != null && shopManager.getAuctionHouse() != null) {
                    List<String> auctionIds = shopManager.getAuctionHouse().getActiveAuctions()
                        .stream()
                        .map(auction -> auction.getAuctionId().toString())
                        .limit(20)
                        .collect(Collectors.toList());
                    
                    // Also provide shortened versions (first 8 characters)
                    List<String> shortIds = shopManager.getAuctionHouse().getActiveAuctions()
                        .stream()
                        .map(auction -> auction.getAuctionId().toString().substring(0, 8))
                        .limit(20)
                        .collect(Collectors.toList());
                    
                    auctionIds.addAll(shortIds);
                    return SharedSuggestionProvider.suggest(auctionIds, builder);
                }
            }
            return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
        }
    };
    
    /**
     * Provides shop category suggestions for the new shop system.
     */
    public static final SuggestionProvider<CommandSourceStack> SHOP_CATEGORY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(List.of(SHOP_CATEGORIES), builder);
    };
    
    /**
     * Provides player's shop name suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> PLAYER_SHOP_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            // TODO: Get actual player shops from ShopManagerNew
            return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(Collections.emptyList(), builder);
        }
    };
    
    /**
     * Provides auction type suggestions for auction commands.
     */
    public static final SuggestionProvider<CommandSourceStack> AUCTION_TYPE_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(List.of(AUCTION_TYPES), builder);
    };
    
    /**
     * Provides auction category suggestions for filtering auctions.
     */
    public static final SuggestionProvider<CommandSourceStack> AUCTION_CATEGORY_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(List.of(AUCTION_CATEGORIES), builder);
    };
    
    /**
     * Provides auction list filter suggestions.
     */
    public static final SuggestionProvider<CommandSourceStack> AUCTION_LIST_FILTER_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(List.of(AUCTION_CATEGORIES), builder);
    };
    
    /**
     * Provides stack amount suggestions for item enhancement commands.
     */
    public static final SuggestionProvider<CommandSourceStack> STACK_AMOUNT_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(List.of(STACK_AMOUNTS), builder);
    };
    
    /**
     * Provides example item name suggestions for item renaming commands.
     */
    public static final SuggestionProvider<CommandSourceStack> ITEM_NAME_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(List.of(EXAMPLE_ITEM_NAMES), builder);
    };
}
