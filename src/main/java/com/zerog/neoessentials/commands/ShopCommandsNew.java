package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;

/**
 * Redesigned shop commands for better functionality and user experience.
 */
public class ShopCommandsNew {
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("shop")
                .executes(context -> showShopHelp(context.getSource()))
                
                // Shop Management
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("category", StringArgumentType.string())
                            .suggests(TabCompletionUtil.SHOP_CATEGORY_SUGGESTIONS)
                            .then(Commands.argument("type", StringArgumentType.string())
                                .suggests(TabCompletionUtil.SHOP_TYPE_SUGGESTIONS)
                                .executes(context -> createShop(context.getSource(),
                                    StringArgumentType.getString(context, "name"),
                                    StringArgumentType.getString(context, "category"),
                                    StringArgumentType.getString(context, "type")))))))
                
                .then(Commands.literal("delete")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.PLAYER_SHOP_SUGGESTIONS)
                        .executes(context -> deleteShop(context.getSource(),
                            StringArgumentType.getString(context, "shop_name")))))
                
                .then(Commands.literal("list")
                    .executes(context -> listShops(context.getSource(), null))
                    .then(Commands.argument("category", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_CATEGORY_SUGGESTIONS)
                        .executes(context -> listShops(context.getSource(),
                            StringArgumentType.getString(context, "category")))))
                
                .then(Commands.literal("info")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> showShopInfo(context.getSource(),
                            StringArgumentType.getString(context, "shop_name")))))
                
                .then(Commands.literal("myshops")
                    .executes(context -> listMyShops(context.getSource())))
                
                // Shop Item Management
                .then(Commands.literal("stock")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.PLAYER_SHOP_SUGGESTIONS)
                        .then(Commands.argument("item_id", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .then(Commands.argument("buy_price", DoubleArgumentType.doubleArg(0.01))
                                    .executes(context -> stockShop(context.getSource(),
                                        StringArgumentType.getString(context, "shop_name"),
                                        StringArgumentType.getString(context, "item_id"),
                                        IntegerArgumentType.getInteger(context, "quantity"),
                                        DoubleArgumentType.getDouble(context, "buy_price"), -1))
                                    .then(Commands.argument("sell_price", DoubleArgumentType.doubleArg(0.01))
                                        .executes(context -> stockShop(context.getSource(),
                                            StringArgumentType.getString(context, "shop_name"),
                                            StringArgumentType.getString(context, "item_id"),
                                            IntegerArgumentType.getInteger(context, "quantity"),
                                            DoubleArgumentType.getDouble(context, "buy_price"),
                                            DoubleArgumentType.getDouble(context, "sell_price")))))))))
                
                .then(Commands.literal("setprice")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.PLAYER_SHOP_SUGGESTIONS)
                        .then(Commands.argument("item_id", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .then(Commands.argument("buy_price", DoubleArgumentType.doubleArg(0.01))
                                .executes(context -> setItemPrice(context.getSource(),
                                    StringArgumentType.getString(context, "shop_name"),
                                    StringArgumentType.getString(context, "item_id"),
                                    DoubleArgumentType.getDouble(context, "buy_price"), -1))
                                .then(Commands.argument("sell_price", DoubleArgumentType.doubleArg(0.01))
                                    .executes(context -> setItemPrice(context.getSource(),
                                        StringArgumentType.getString(context, "shop_name"),
                                        StringArgumentType.getString(context, "item_id"),
                                        DoubleArgumentType.getDouble(context, "buy_price"),
                                        DoubleArgumentType.getDouble(context, "sell_price"))))))))
                
                .then(Commands.literal("price")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item_id", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> showItemPrice(context.getSource(),
                                StringArgumentType.getString(context, "shop_name"),
                                StringArgumentType.getString(context, "item_id"))))))
                
                .then(Commands.literal("remove")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.PLAYER_SHOP_SUGGESTIONS)
                        .then(Commands.argument("item_id", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> removeItem(context.getSource(),
                                StringArgumentType.getString(context, "shop_name"),
                                StringArgumentType.getString(context, "item_id"))))))
                
                // Trading Operations
                .then(Commands.literal("buy")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item_id", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> buyItem(context.getSource(),
                                StringArgumentType.getString(context, "shop_name"),
                                StringArgumentType.getString(context, "item_id"), 1))
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .executes(context -> buyItem(context.getSource(),
                                    StringArgumentType.getString(context, "shop_name"),
                                    StringArgumentType.getString(context, "item_id"),
                                    IntegerArgumentType.getInteger(context, "quantity")))))))
                
                .then(Commands.literal("sell")
                    .then(Commands.argument("shop_name", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item_id", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> sellItem(context.getSource(),
                                StringArgumentType.getString(context, "shop_name"),
                                StringArgumentType.getString(context, "item_id"), 1))
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .executes(context -> sellItem(context.getSource(),
                                    StringArgumentType.getString(context, "shop_name"),
                                    StringArgumentType.getString(context, "item_id"),
                                    IntegerArgumentType.getInteger(context, "quantity")))))))
                
                // Search and Discovery
                .then(Commands.literal("search")
                    .then(Commands.argument("item_name", StringArgumentType.string())
                        .executes(context -> searchItems(context.getSource(),
                            StringArgumentType.getString(context, "item_name")))))
                
                .then(Commands.literal("categories")
                    .executes(context -> listCategories(context.getSource())))
                
                .then(Commands.literal("stats")
                    .executes(context -> showShopStats(context.getSource())))
        );
    }
    
    private int showShopHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            LanguageUtil.sendMessage(player, "§6=== NeoEssentials Shop System ===");
            LanguageUtil.sendMessage(player, "§7");
            LanguageUtil.sendMessage(player, "§6Shop Management:");
            LanguageUtil.sendMessage(player, "§e/shop create <name> <category> <type> §7- Create a new shop");
            LanguageUtil.sendMessage(player, "§e/shop delete <shop_name> §7- Delete your shop");
            LanguageUtil.sendMessage(player, "§e/shop list [category] §7- List all shops");
            LanguageUtil.sendMessage(player, "§e/shop info <shop_name> §7- Get shop information");
            LanguageUtil.sendMessage(player, "§e/shop myshops §7- List your shops");
            LanguageUtil.sendMessage(player, "§7");
            LanguageUtil.sendMessage(player, "§6Item Management:");
            LanguageUtil.sendMessage(player, "§e/shop stock <shop> <item> <qty> <buy_price> [sell_price] §7- Add items");
            LanguageUtil.sendMessage(player, "§e/shop setprice <shop> <item> <buy_price> [sell_price] §7- Set item prices");
            LanguageUtil.sendMessage(player, "§e/shop price <shop> <item> §7- Check item price");
            LanguageUtil.sendMessage(player, "§e/shop remove <shop> <item> §7- Remove item from shop");
            LanguageUtil.sendMessage(player, "§7");
            LanguageUtil.sendMessage(player, "§6Trading:");
            LanguageUtil.sendMessage(player, "§e/shop buy <shop> <item> [quantity] §7- Buy items");
            LanguageUtil.sendMessage(player, "§e/shop sell <shop> <item> [quantity] §7- Sell items");
            LanguageUtil.sendMessage(player, "§7");
            LanguageUtil.sendMessage(player, "§6Discovery:");
            LanguageUtil.sendMessage(player, "§e/shop search <item_name> §7- Search for items");
            LanguageUtil.sendMessage(player, "§e/shop categories §7- List available categories");
            LanguageUtil.sendMessage(player, "§e/shop stats §7- Show shop statistics");
            LanguageUtil.sendMessage(player, "§7");
            LanguageUtil.sendMessage(player, "§6Shop Types: §ePlayer, Server, Auction");
            LanguageUtil.sendMessage(player, "§7Shop creation fee: §e" + 
                CurrencyManager.getInstance().getDefaultCurrency().format(ShopManagerNew.getInstance().getShopCreationFee()));
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("Command can only be used by players."));
            return 0;
        }
    }
    
    private int createShop(CommandSourceStack source, String name, String category, String typeStr) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            // Parse shop type
            ShopNew.ShopType shopType;
            try {
                shopType = ShopNew.ShopType.valueOf(typeStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                LanguageUtil.sendErrorMessage(player, "Invalid shop type. Available types: Player, Server, Auction");
                return 0;
            }
            
            // Create the shop
            ShopManagerNew.ShopCreationResult result = shopManager.createShop(
                player.getUUID(), name, category, shopType);
            
            if (result.isSuccess()) {
                LanguageUtil.sendMessage(player, "Shop created successfully!");
                LanguageUtil.sendMessage(player, "§7Shop Name: §e" + name);
                LanguageUtil.sendMessage(player, "§7Category: §e" + category);
                LanguageUtil.sendMessage(player, "§7Type: §e" + shopType.getDisplayName());
                LanguageUtil.sendMessage(player, "§7Shop ID: §e" + result.getShop().getShopId().toString().substring(0, 8));
                LanguageUtil.sendMessage(player, "§7Use §e/shop stock " + name + " <item> <quantity> <price>§7 to add items.");
            } else {
                LanguageUtil.sendErrorMessage(player, result.getMessage());
            }
            
            return result.isSuccess() ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int deleteShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            if (!shop.getOwnerId().equals(player.getUUID())) {
                LanguageUtil.sendErrorMessage(player, "You can only delete your own shops.");
                return 0;
            }
            
            boolean success = shopManager.deleteShop(shop.getShopId(), player.getUUID());
            if (success) {
                LanguageUtil.sendMessage(player, "Shop deleted successfully!");
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to delete shop.");
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listShops(CommandSourceStack source, String category) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            List<ShopNew> shops = (category != null) ? 
                shopManager.getShopsByCategory(category) : 
                shopManager.getAllActiveShops();
            
            if (shops.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No shops found" + (category != null ? " in category: " + category : ""));
                return 1;
            }
            
            String title = (category != null) ? 
                "=== " + category.toUpperCase() + " Shops ===" : 
                "=== All Shops ===";
            LanguageUtil.sendMessage(player, "§6" + title);
            
            for (ShopNew shop : shops.stream().limit(10).toList()) {
                String ownerName = "Unknown"; // TODO: Get player name from UUID
                LanguageUtil.sendMessage(player, "§e" + shop.getShopName() + " §7- §a" + shop.getCategory() + 
                    " §7- §e" + shop.getShopType().getDisplayName() + " §7- §b" + ownerName);
            }
            
            if (shops.size() > 10) {
                LanguageUtil.sendMessage(player, "§7... and " + (shops.size() - 10) + " more shops.");
            }
            
            LanguageUtil.sendMessage(player, "§7Use §e/shop info <shop_name>§7 for detailed information.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int showShopInfo(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            LanguageUtil.sendMessage(player, "§6=== Shop Information ===");
            LanguageUtil.sendMessage(player, "§7Name: §e" + shop.getShopName());
            LanguageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            LanguageUtil.sendMessage(player, "§7Type: §e" + shop.getShopType().getDisplayName());
            LanguageUtil.sendMessage(player, "§7Status: §e" + (shop.isActive() ? "Active" : "Inactive"));
            LanguageUtil.sendMessage(player, "§7Total Sales: §e" + shop.getTotalSales());
            LanguageUtil.sendMessage(player, "§7Total Revenue: §e" + currency.format(shop.getTotalRevenue()));
            LanguageUtil.sendMessage(player, "§7Items Available: §e" + shop.getAvailableItems().size());
            
            if (!shop.getDescription().isEmpty()) {
                LanguageUtil.sendMessage(player, "§7Description: §e" + shop.getDescription());
            }
            
            LanguageUtil.sendMessage(player, "§7Use §e/shop buy " + shopName + " <item>§7 to purchase items.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listMyShops(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            List<ShopNew> shops = shopManager.getPlayerShops(player.getUUID());
            
            if (shops.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7You don't own any shops.");
                LanguageUtil.sendMessage(player, "§7Use §e/shop create <name> <category> <type>§7 to create a shop.");
                return 1;
            }
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            LanguageUtil.sendMessage(player, "§6=== Your Shops ===");
            
            for (ShopNew shop : shops) {
                String status = shop.isActive() ? "§aActive" : "§cInactive";
                LanguageUtil.sendMessage(player, "§e" + shop.getShopName() + " §7- §a" + shop.getCategory() + 
                    " §7- " + status + " §7- §e" + currency.format(shop.getTotalRevenue()) + " total");
            }
            
            LanguageUtil.sendMessage(player, "§7Use §e/shop info <shop_name>§7 for detailed information.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int stockShop(CommandSourceStack source, String shopName, String itemId, int quantity, double buyPrice, double sellPrice) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            if (!shop.getOwnerId().equals(player.getUUID())) {
                LanguageUtil.sendErrorMessage(player, "You can only stock your own shops.");
                return 0;
            }
            
            // Validate item ID
            if (!ItemHandler.isValidItem(itemId)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item ID: " + itemId);
                return 0;
            }
            
            // Use sell price if provided, otherwise set to -1 (no selling)
            double actualSellPrice = sellPrice > 0 ? sellPrice : -1;
            
            boolean success = shop.addItem(itemId, quantity, buyPrice, actualSellPrice);
            if (success) {
                Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
                LanguageUtil.sendMessage(player, "Item stocked successfully!");
                LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.getItemDisplayName(itemId));
                LanguageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
                LanguageUtil.sendMessage(player, "§7Buy Price: §e" + currency.format(buyPrice));
                if (actualSellPrice > 0) {
                    LanguageUtil.sendMessage(player, "§7Sell Price: §e" + currency.format(actualSellPrice));
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to stock item.");
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int setItemPrice(CommandSourceStack source, String shopName, String itemId, double buyPrice, double sellPrice) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            if (!shop.getOwnerId().equals(player.getUUID())) {
                LanguageUtil.sendErrorMessage(player, "You can only modify your own shops.");
                return 0;
            }
            
            // Validate item ID
            if (!ItemHandler.isValidItem(itemId)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item ID: " + itemId);
                return 0;
            }
            
            double actualSellPrice = sellPrice > 0 ? sellPrice : -1;
            
            boolean success = shop.updateItemPricing(itemId, buyPrice, actualSellPrice);
            if (success) {
                Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
                LanguageUtil.sendMessage(player, "Item price updated successfully!");
                LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.getItemDisplayName(itemId));
                LanguageUtil.sendMessage(player, "§7Buy Price: §e" + currency.format(buyPrice));
                if (actualSellPrice > 0) {
                    LanguageUtil.sendMessage(player, "§7Sell Price: §e" + currency.format(actualSellPrice));
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "Item not found in shop. Use /shop stock to add items first.");
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int showItemPrice(CommandSourceStack source, String shopName, String itemId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            // Validate item ID
            if (!ItemHandler.isValidItem(itemId)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item ID: " + itemId);
                return 0;
            }
            
            double buyPrice = shop.getItemBuyPrice(itemId);
            double sellPrice = shop.getItemSellPrice(itemId);
            
            if (buyPrice <= 0 && sellPrice <= 0) {
                LanguageUtil.sendErrorMessage(player, "Item not available in this shop: " + itemId);
                return 0;
            }
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            LanguageUtil.sendMessage(player, "§6=== Item Price Information ===");
            LanguageUtil.sendMessage(player, "§7Shop: §e" + shop.getShopName());
            LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.getItemDisplayName(itemId));
            
            if (buyPrice > 0) {
                LanguageUtil.sendMessage(player, "§7Buy Price: §e" + currency.format(buyPrice));
            } else {
                LanguageUtil.sendMessage(player, "§7Buy Price: §cNot available");
            }
            
            if (sellPrice > 0) {
                LanguageUtil.sendMessage(player, "§7Sell Price: §e" + currency.format(sellPrice));
            } else {
                LanguageUtil.sendMessage(player, "§7Sell Price: §cNot available");
            }
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int removeItem(CommandSourceStack source, String shopName, String itemId) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            if (!shop.getOwnerId().equals(player.getUUID())) {
                LanguageUtil.sendErrorMessage(player, "You can only modify your own shops.");
                return 0;
            }
            
            boolean success = shop.removeItem(itemId);
            if (success) {
                LanguageUtil.sendMessage(player, "Item removed from shop successfully!");
                LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.getItemDisplayName(itemId));
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to remove item from shop.");
            }
            
            return success ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int buyItem(CommandSourceStack source, String shopName, String itemId, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            // Validate item ID
            if (!ItemHandler.isValidItem(itemId)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item ID: " + itemId);
                return 0;
            }
            
            ShopNew.PurchaseResult result = shop.purchaseItem(player.getUUID(), itemId, quantity);
            
            if (result.isSuccess()) {
                LanguageUtil.sendMessage(player, "Purchase successful!");
                LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.getItemDisplayName(itemId));
                LanguageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
                LanguageUtil.sendMessage(player, "§7Shop: §e" + shop.getShopName());
                // TODO: Add item to player's inventory
            } else {
                LanguageUtil.sendErrorMessage(player, result.getMessage());
            }
            
            return result.isSuccess() ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int sellItem(CommandSourceStack source, String shopName, String itemId, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopNew shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop not found: " + shopName);
                return 0;
            }
            
            // Validate item ID
            if (!ItemHandler.isValidItem(itemId)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item ID: " + itemId);
                return 0;
            }
            
            ShopNew.SaleResult result = shop.sellItem(player.getUUID(), itemId, quantity);
            
            if (result.isSuccess()) {
                LanguageUtil.sendMessage(player, "Sale successful!");
                LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.getItemDisplayName(itemId));
                LanguageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
                LanguageUtil.sendMessage(player, "§7Shop: §e" + shop.getShopName());
                // TODO: Remove item from player's inventory
            } else {
                LanguageUtil.sendErrorMessage(player, result.getMessage());
            }
            
            return result.isSuccess() ? 1 : 0;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int searchItems(CommandSourceStack source, String itemName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            List<ShopManagerNew.ShopItemSearchResult> results = shopManager.searchItems(itemName);
            
            if (results.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No items found matching: " + itemName);
                return 1;
            }
            
            LanguageUtil.sendMessage(player, "§6=== Search Results for: " + itemName + " ===");
            
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            for (ShopManagerNew.ShopItemSearchResult result : results.stream().limit(10).toList()) {
                ShopNew shop = result.getShop();
                ShopNew.ShopItem item = result.getItem();
                double buyPrice = shop.getItemBuyPrice(item.getItemId());
                
                LanguageUtil.sendMessage(player, "§e" + item.getItemName() + " §7- §a" + currency.format(buyPrice) + 
                    " §7- §e" + shop.getShopName() + " §7- §b" + item.getQuantity() + " available");
            }
            
            if (results.size() > 10) {
                LanguageUtil.sendMessage(player, "§7... and " + (results.size() - 10) + " more results.");
            }
            
            LanguageUtil.sendMessage(player, "§7Use §e/shop buy <shop_name> <item>§7 to purchase items.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int listCategories(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            LanguageUtil.sendMessage(player, "§6=== Available Shop Categories ===");
            
            for (String category : shopManager.getValidCategories()) {
                List<ShopNew> shops = shopManager.getShopsByCategory(category);
                LanguageUtil.sendMessage(player, "§e" + category + " §7- §a" + shops.size() + " shops");
            }
            
            LanguageUtil.sendMessage(player, "§7Use §e/shop list <category>§7 to view shops in a category.");
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
    
    private int showShopStats(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            ShopManagerNew shopManager = ShopManagerNew.getInstance();
            
            ShopManagerNew.ShopStatistics stats = shopManager.getGlobalStatistics();
            Currency currency = CurrencyManager.getInstance().getDefaultCurrency();
            
            LanguageUtil.sendMessage(player, "§6=== Shop System Statistics ===");
            LanguageUtil.sendMessage(player, "§7Total Shops: §e" + stats.getTotalShops());
            LanguageUtil.sendMessage(player, "§7Active Shops: §e" + stats.getActiveShops());
            LanguageUtil.sendMessage(player, "§7Total Revenue: §e" + currency.format(stats.getTotalRevenue()));
            LanguageUtil.sendMessage(player, "§7Total Sales: §e" + stats.getTotalSales());
            LanguageUtil.sendMessage(player, "§7Your Shops: §e" + shopManager.getPlayerShops(player.getUUID()).size());
            
            return 1;
        } catch (Exception e) {
            source.sendFailure(net.minecraft.network.chat.Component.literal("An error occurred: " + e.getMessage()));
            return 0;
        }
    }
}
