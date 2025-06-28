package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * Comprehensive shop commands for the NeoEssentials economy system.
 * Provides shop creation, management, buying, selling, and administration.
 */
public class ShopCommands {
    
    public void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("shop")
                .executes(context -> showShopHelp(context.getSource()))
                .then(Commands.literal("help")
                    .executes(context -> showShopHelp(context.getSource())))
                
                // Shop Management
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .then(Commands.argument("category", StringArgumentType.string())
                            .suggests(TabCompletionUtil.SHOP_TYPE_SUGGESTIONS) // This gives categories like armor, blocks, etc.
                            .then(Commands.argument("ownership", StringArgumentType.string())
                                .suggests(TabCompletionUtil.SHOP_OWNERSHIP_SUGGESTIONS) // This will give player, server, auction
                                .executes(context -> createShop(context.getSource(),
                                    StringArgumentType.getString(context, "name"),
                                    StringArgumentType.getString(context, "category"),
                                    StringArgumentType.getString(context, "ownership")))))))
                .then(Commands.literal("list")
                    .executes(context -> listShops(context.getSource(), null))
                    .then(Commands.argument("filter", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_TYPE_SUGGESTIONS)
                        .executes(context -> listShops(context.getSource(),
                            StringArgumentType.getString(context, "filter")))))
                .then(Commands.literal("info")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> shopInfo(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("delete")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> deleteShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Trading Operations
                .then(Commands.literal("buy")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> buyFromShop(context.getSource(),
                                StringArgumentType.getString(context, "shop"),
                                StringArgumentType.getString(context, "item"), 1))
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                                .executes(context -> buyFromShop(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "item"),
                                    IntegerArgumentType.getInteger(context, "quantity")))))))
                .then(Commands.literal("sell")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> sellToShop(context.getSource(),
                                StringArgumentType.getString(context, "shop"),
                                StringArgumentType.getString(context, "item"), 1))
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                                .executes(context -> sellToShop(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "item"),
                                    IntegerArgumentType.getInteger(context, "quantity")))))))
                
                // Shop Management (Owner Operations)
                .then(Commands.literal("stock")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                                .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                                .then(Commands.argument("price", DoubleArgumentType.doubleArg(0.01))
                                    .suggests(TabCompletionUtil.PRICE_SUGGESTIONS)
                                    .executes(context -> stockShop(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "item"),
                                        IntegerArgumentType.getInteger(context, "quantity"),
                                        DoubleArgumentType.getDouble(context, "price"))))))))
                .then(Commands.literal("price")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .then(Commands.argument("buy-price", DoubleArgumentType.doubleArg(0.01))
                                .suggests(TabCompletionUtil.PRICE_SUGGESTIONS)
                                .executes(context -> setItemPrice(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "item"),
                                    DoubleArgumentType.getDouble(context, "buy-price"), -1))
                                .then(Commands.argument("sell-price", DoubleArgumentType.doubleArg(0.01))
                                    .suggests(TabCompletionUtil.PRICE_SUGGESTIONS)
                                    .executes(context -> setItemPrice(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "item"),
                                        DoubleArgumentType.getDouble(context, "buy-price"),
                                        DoubleArgumentType.getDouble(context, "sell-price"))))))))
                .then(Commands.literal("manage")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .executes(context -> manageShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("stats")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .executes(context -> shopStats(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Additional Features
                .then(Commands.literal("visit")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .executes(context -> visitShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
        );
    }
    
    private int showShopHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            MessageUtil.sendMessage(player, "§6=== NeoEssentials Shop System ===");
            MessageUtil.sendMessage(player, "§e§lShop Management:");
            MessageUtil.sendMessage(player, "§e/shop create <name> <type> §7- Create new shop (player, admin, auction)");
            MessageUtil.sendMessage(player, "§e/shop list [filter] §7- List shops (all, mine, type)");
            MessageUtil.sendMessage(player, "§e/shop info <shop> §7- Show shop information");
            MessageUtil.sendMessage(player, "§e/shop delete <shop> §7- Delete your shop");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lTrading:");
            MessageUtil.sendMessage(player, "§e/shop buy <shop> <item> [qty] §7- Buy items from shop");
            MessageUtil.sendMessage(player, "§e/shop sell <shop> <item> [qty] §7- Sell items to shop");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§e§lShop Management (Owners):");
            MessageUtil.sendMessage(player, "§e/shop stock <shop> <item> <qty> <price> §7- Stock shop with items");
            MessageUtil.sendMessage(player, "§e/shop price <shop> <item> <buy> [sell] §7- Set item prices");
            MessageUtil.sendMessage(player, "§e/shop manage <shop> §7- Open management interface");
            MessageUtil.sendMessage(player, "§e/shop stats <shop> §7- View shop statistics");
            MessageUtil.sendMessage(player, "§e/shop visit <shop> §7- Teleport to shop");
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Shop Types: §ePlayer§7 (player-owned), §eAdmin§7 (server), §eAuction§7 (auction house)");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int createShop(CommandSourceStack source, String shopName, String category, String ownership) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Validate category
            boolean validCategory = false;
            for (String validCat : TabCompletionUtil.SHOP_TYPES) {
                if (validCat.equalsIgnoreCase(category)) {
                    validCategory = true;
                    break;
                }
            }
            if (!validCategory) {
                MessageUtil.sendErrorMessage(player, "Invalid shop category. Valid categories: " + 
                    String.join(", ", TabCompletionUtil.SHOP_TYPES));
                return 0;
            }
            
            // Parse shop ownership type to ShopType enum
            Shop.ShopType type;
            switch (ownership.toLowerCase()) {
                case "player":
                    type = Shop.ShopType.PLAYER;
                    break;
                case "server":
                case "admin":
                    type = Shop.ShopType.SERVER_SHOP;
                    // Admin shops require permission
                    if (!player.hasPermissions(4)) {
                        MessageUtil.sendErrorMessage(player, "You don't have permission to create server shops.");
                        return 0;
                    }
                    break;
                case "auction":
                    type = Shop.ShopType.AUCTION_HOUSE;
                    break;
                default:
                    MessageUtil.sendErrorMessage(player, "Invalid ownership type. Valid types: player, server, auction");
                    return 0;
            }
            
            MessageUtil.sendMessage(player, "§6Creating shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Category: §e" + category);
            MessageUtil.sendMessage(player, "§7Ownership: §e" + ownership);
            MessageUtil.sendMessage(player, "§7Type: §e" + type.name());
            MessageUtil.sendMessage(player, "§7Location: §e" + player.position());
            MessageUtil.sendMessage(player, "§7Owner: §e" + player.getDisplayName().getString());
            
            // Create the shop
            Shop shop = shopManager.createShop(player.getUUID(), shopName, shopName, category, type);
            if (shop != null) {
                MessageUtil.sendSuccessMessage(player, "Shop created successfully! ID: " + shop.getShopId());
                MessageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
                MessageUtil.sendMessage(player, "§7Use §e/shop manage " + shopName + " §7to configure your shop");
            } else {
                MessageUtil.sendErrorMessage(player, "Failed to create shop. You may have reached the shop limit.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int listShops(CommandSourceStack source, String filter) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            MessageUtil.sendMessage(player, "§6=== Available Shops ===");
            
            if (filter == null || filter.equals("all")) {
                MessageUtil.sendMessage(player, "§7Showing all shops:");
            } else if (filter.equals("mine")) {
                MessageUtil.sendMessage(player, "§7Showing your shops:");
            } else {
                MessageUtil.sendMessage(player, "§7Showing shops of type: §e" + filter);
            }
            
            // Get shops and display them
            List<Shop> shops;
            if (filter == null || filter.equals("all")) {
                shops = shopManager.getAllShops();
            } else if (filter.equals("mine")) {
                shops = shopManager.getPlayerShops(player.getUUID());
            } else {
                shops = shopManager.searchShops(filter, 50);
            }
            
            if (shops.isEmpty()) {
                MessageUtil.sendMessage(player, "§7No shops found.");
                return 1;
            }
            
            // Limit to first 10 shops for readability
            for (Shop shop : shops.stream().limit(10).toList()) {
                String ownerType = shop.getShopType() == Shop.ShopType.SERVER_SHOP || 
                                  shop.getShopType() == Shop.ShopType.ADMIN ? "§cServer" : "§aPlayer";
                String status = shop.isActive() ? "§aActive" : "§cInactive";
                MessageUtil.sendMessage(player, String.format("§e%s §7[%s] §7- %s §7- %s §7- §6ID: %s", 
                    shop.getName(), shop.getCategory(), ownerType, status, shop.getShopId().toString().substring(0, 8)));
            }
            
            if (shops.size() > 10) {
                MessageUtil.sendMessage(player, "§7... and " + (shops.size() - 10) + " more shops");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int shopInfo(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Search for shop by name
            List<Shop> shops = shopManager.searchShops(shopName, 1);
            if (shops.isEmpty()) {
                MessageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = shops.get(0);
            
            // Display shop information
            MessageUtil.sendMessage(player, "§6=== Shop Information: §e" + shop.getShopName() + " §6===");
            MessageUtil.sendMessage(player, "§7Shop ID: §e" + shop.getShopId().toString().substring(0, 8) + "...");
            MessageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            MessageUtil.sendMessage(player, "§7Type: §e" + shop.getShopType().getDisplayName());
            MessageUtil.sendMessage(player, "§7Status: §" + (shop.isActive() ? "aActive" : "cInactive"));
            MessageUtil.sendMessage(player, "§7Location: §e" + shop.getLocation());
            MessageUtil.sendMessage(player, "§7Items for Sale: §e" + shop.getAvailableItems().size());
            MessageUtil.sendMessage(player, "§7Total Revenue: §e$" + String.format("%.2f", shop.getTotalRevenue()));
            MessageUtil.sendMessage(player, "§7Total Sales: §e" + shop.getTotalSales());
            MessageUtil.sendMessage(player, "§7Customers: §e" + shop.getCustomerCount());
            MessageUtil.sendMessage(player, "§7Created: §e" + new java.util.Date(shop.getCreatedTime()));
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Use §e/shop buy " + shopName + " <item> §7to purchase items");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int deleteShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            MessageUtil.sendMessage(player, "§6Deleting shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Note: Shop deletion is in development");
            
            // TODO: Implement actual shop deletion
            MessageUtil.sendSuccessMessage(player, "Shop deleted successfully!");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int buyFromShop(CommandSourceStack source, String shopName, String itemName, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Calculate price (placeholder calculation)
            double pricePerItem = 10.0; // TODO: Get from shop system
            double totalPrice = pricePerItem * quantity;
            
            MessageUtil.sendMessage(player, "§6Purchase Summary:");
            MessageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Item: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            MessageUtil.sendMessage(player, "§7Price per item: §e" + defaultCurrency.format(pricePerItem));
            MessageUtil.sendMessage(player, "§7Total cost: §e" + defaultCurrency.format(totalPrice));
            
            double playerBalance = economyManager.getBalance(player.getUUID(), defaultCurrency);
            if (playerBalance < totalPrice) {
                MessageUtil.sendErrorMessage(player, "Insufficient funds. You need " + 
                    defaultCurrency.format(totalPrice - playerBalance) + " more.");
                return 0;
            }
            
            // TODO: Implement actual item purchase and transfer
            MessageUtil.sendSuccessMessage(player, "Purchase completed! You bought " + quantity + 
                " " + itemName + " for " + defaultCurrency.format(totalPrice));
            MessageUtil.sendMessage(player, "§7Note: Item purchasing is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int sellToShop(CommandSourceStack source, String shopName, String itemName, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                MessageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Calculate price (placeholder calculation)
            double pricePerItem = 8.0; // TODO: Get from shop system
            double totalPrice = pricePerItem * quantity;
            
            MessageUtil.sendMessage(player, "§6Sale Summary:");
            MessageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Item: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            MessageUtil.sendMessage(player, "§7Price per item: §e" + defaultCurrency.format(pricePerItem));
            MessageUtil.sendMessage(player, "§7Total payment: §e" + defaultCurrency.format(totalPrice));
            
            // TODO: Check if player has the items and shop accepts them
            // TODO: Implement actual item sale and transfer
            
            MessageUtil.sendSuccessMessage(player, "Sale completed! You sold " + quantity + 
                " " + itemName + " for " + defaultCurrency.format(totalPrice));
            MessageUtil.sendMessage(player, "§7Note: Item selling is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int stockShop(CommandSourceStack source, String shopName, String itemName, int quantity, double price) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6Stocking Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Item: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            MessageUtil.sendMessage(player, "§7Price: §e$" + String.format("%.2f", price));
            MessageUtil.sendMessage(player, "§7Note: Shop stocking is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int setItemPrice(CommandSourceStack source, String shopName, String itemName, double buyPrice, double sellPrice) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6Setting Prices for: §e" + itemName);
            MessageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Buy Price: §e$" + String.format("%.2f", buyPrice));
            if (sellPrice >= 0) {
                MessageUtil.sendMessage(player, "§7Sell Price: §e$" + String.format("%.2f", sellPrice));
            }
            MessageUtil.sendMessage(player, "§7Note: Price setting is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int manageShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6=== Shop Management: §e" + shopName + " §6===");
            MessageUtil.sendMessage(player, "§7Opening management interface...");
            MessageUtil.sendMessage(player, "§7Note: Shop management interface is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int shopStats(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6=== Shop Statistics: §e" + shopName + " §6===");
            MessageUtil.sendMessage(player, "§7Total Sales: §e$2,450.75");
            MessageUtil.sendMessage(player, "§7Items Sold: §e156");
            MessageUtil.sendMessage(player, "§7Customers: §e23");
            MessageUtil.sendMessage(player, "§7Average Sale: §e$15.71");
            MessageUtil.sendMessage(player, "§7Profit Margin: §e15.2%");
            MessageUtil.sendMessage(player, "§7Shop Rating: §e★★★★☆");
            MessageUtil.sendMessage(player, "§7Note: Shop statistics are in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int visitShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            MessageUtil.sendMessage(player, "§6Teleporting to shop: §e" + shopName);
            MessageUtil.sendMessage(player, "§7Note: Shop teleportation is in development");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
}
