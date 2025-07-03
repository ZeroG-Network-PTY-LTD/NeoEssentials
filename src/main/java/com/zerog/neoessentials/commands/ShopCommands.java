package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.*;
import com.zerog.neoessentials.economy.Shop.ShopItem;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

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
                .then(Commands.literal("status")
                    .executes(context -> showShopStatus(context.getSource())))
                
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
                // Add setprice alias for backwards compatibility and discoverability
                .then(Commands.literal("setprice")
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
                
                // Inventory Management - Add Item from Hand
                .then(Commands.literal("additem")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                        .executes(context -> addItemFromHand(context.getSource(),
                            StringArgumentType.getString(context, "shop"), 1))
                        .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                            .suggests(TabCompletionUtil.QUANTITY_SUGGESTIONS)
                            .executes(context -> addItemFromHand(context.getSource(),
                                StringArgumentType.getString(context, "shop"),
                                IntegerArgumentType.getInteger(context, "quantity"))))))
                
                .then(Commands.literal("manage")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                        .executes(context -> manageShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("stats")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> shopStats(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Additional Features
                .then(Commands.literal("visit")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> visitShop(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                .then(Commands.literal("setlocation")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                        .executes(context -> setShopLocation(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                // Employee Management
                .then(Commands.literal("employee")
                    .then(Commands.literal("add")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.string())
                                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                                .then(Commands.argument("role", StringArgumentType.string())
                                    .suggests(TabCompletionUtil.EMPLOYEE_ROLE_SUGGESTIONS)
                                    .executes(context -> addEmployee(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "role")))))))
                    .then(Commands.literal("remove")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.string())
                                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                                .executes(context -> removeEmployee(context.getSource(),
                                    StringArgumentType.getString(context, "shop"),
                                    StringArgumentType.getString(context, "player"))))))
                    .then(Commands.literal("role")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .then(Commands.argument("player", StringArgumentType.string())
                                .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                                .then(Commands.argument("role", StringArgumentType.string())
                                    .suggests(TabCompletionUtil.EMPLOYEE_ROLE_SUGGESTIONS)
                                    .executes(context -> changeEmployeeRole(context.getSource(),
                                        StringArgumentType.getString(context, "shop"),
                                        StringArgumentType.getString(context, "player"),
                                        StringArgumentType.getString(context, "role")))))))
                    .then(Commands.literal("list")
                        .then(Commands.argument("shop", StringArgumentType.string())
                            .suggests(TabCompletionUtil.MANAGEABLE_SHOP_SUGGESTIONS)
                            .executes(context -> listEmployees(context.getSource(),
                                StringArgumentType.getString(context, "shop"))))))
                
                // GUI Interface
                .then(Commands.literal("gui")
                    .then(Commands.argument("shop", StringArgumentType.string())
                        .suggests(TabCompletionUtil.SHOP_SUGGESTIONS)
                        .executes(context -> openShopGUI(context.getSource(),
                            StringArgumentType.getString(context, "shop")))))
                
                .then(Commands.literal("search")
                    .then(Commands.literal("item")
                        .then(Commands.argument("item", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                            .executes(context -> searchShopsByItem(context.getSource(),
                                StringArgumentType.getString(context, "item")))))
                    .then(Commands.literal("price")
                        .then(Commands.argument("minPrice", DoubleArgumentType.doubleArg(0))
                            .then(Commands.argument("maxPrice", DoubleArgumentType.doubleArg(0))
                                .executes(context -> searchShopsByPrice(context.getSource(),
                                    DoubleArgumentType.getDouble(context, "minPrice"),
                                    DoubleArgumentType.getDouble(context, "maxPrice"))))))
                    .then(Commands.literal("owner")
                        .then(Commands.argument("playerName", StringArgumentType.string())
                            .suggests(TabCompletionUtil.ONLINE_PLAYER_SUGGESTIONS)
                            .executes(context -> searchShopsByOwner(context.getSource(),
                                StringArgumentType.getString(context, "playerName")))))
                    .then(Commands.literal("category")
                        .then(Commands.argument("category", StringArgumentType.string())
                            .suggests(TabCompletionUtil.SHOP_TYPE_SUGGESTIONS)
                            .executes(context -> searchShopsByCategory(context.getSource(),
                                StringArgumentType.getString(context, "category"))))))
        );
    }
    
    private int showShopHelp(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            LanguageUtil.sendMessage(player, "§6=== NeoEssentials Shop System ===");
            LanguageUtil.sendMessage(player, "§e§lShop Management:");
            LanguageUtil.sendMessage(player, "§e/shop create <name> <type> §7- Create new shop (player, admin, auction)");
            LanguageUtil.sendMessage(player, "§e/shop list [filter] §7- List shops (all, mine, type)");
            LanguageUtil.sendMessage(player, "§e/shop info <shop> §7- Show shop information");
            LanguageUtil.sendMessage(player, "§e/shop delete <shop> §7- Delete your shop");
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§e§lTrading:");
            LanguageUtil.sendMessage(player, "§e/shop buy <shop> <item> [qty] §7- Buy items from shop");
            LanguageUtil.sendMessage(player, "§e/shop sell <shop> <item> [qty] §7- Sell items to shop");
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§e§lShop Management (Owners):");
            LanguageUtil.sendMessage(player, "§e/shop stock <shop> <item> <qty> <price> §7- Stock shop with items");
            LanguageUtil.sendMessage(player, "§e/shop additem <shop> [qty] §7- Add held item to shop");
            LanguageUtil.sendMessage(player, "§e/shop price <shop> <item> <buy> [sell] §7- Set item prices");
            LanguageUtil.sendMessage(player, "§e/shop setprice <shop> <item> <buy> [sell] §7- Set item prices (alias)");
            LanguageUtil.sendMessage(player, "§e/shop manage <shop> §7- Open management interface");
            LanguageUtil.sendMessage(player, "§e/shop gui <shop> §7- Open buy/sell interface");
            LanguageUtil.sendMessage(player, "§e/shop stats <shop> §7- View shop statistics");
            LanguageUtil.sendMessage(player, "§e/shop visit <shop> §7- Teleport to shop");
            LanguageUtil.sendMessage(player, "§e/shop setlocation <shop> §7- Set shop teleport location");
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§e§lEmployee Management:");
            LanguageUtil.sendMessage(player, "§e/shop employee add <shop> <player> <role> §7- Hire employee");
            LanguageUtil.sendMessage(player, "§e/shop employee remove <shop> <player> §7- Fire employee");
            LanguageUtil.sendMessage(player, "§e/shop employee role <shop> <player> <role> §7- Change role");
            LanguageUtil.sendMessage(player, "§e/shop employee list <shop> §7- List employees");
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§7Categories: §earmor, blocks, food, tools, weapons, magic, redstone, general");
            LanguageUtil.sendMessage(player, "§7Shop Types: §ePlayer§7 (player-owned), §eServer§7 (admin), §eAuction§7 (auction house)");
            LanguageUtil.sendMessage(player, "§7Employee Roles: §eOwner, Manager, Cashier, Stocker, Sales_Associate, Viewer");
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int showShopStatus(CommandSourceStack source) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Get player info
            com.zerog.neoessentials.economy.EconomyManager economyManager2 = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
            double playerBalance = economyManager2.getBalance(player.getUUID(), defaultCurrency);
            
            List<Shop> playerShops = shopManager.getPlayerShops(player.getUUID());
            int maxShops = shopManager.getMaxShopsPerPlayer();
            double creationFee = shopManager.getShopCreationFee();
            int totalShops = shopManager.getAllShops().size();
            
            LanguageUtil.sendMessage(player, "§6=== Your Shop Status ===");
            LanguageUtil.sendMessage(player, "§7Player: §e" + player.getDisplayName().getString());
            LanguageUtil.sendMessage(player, "§7UUID: §e" + player.getUUID());
            LanguageUtil.sendMessage(player, "§7Balance: §e$" + String.format("%.2f", playerBalance));
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§7Your Shops: §e" + playerShops.size() + "§7/§e" + maxShops);
            LanguageUtil.sendMessage(player, "§7Creation Fee: §e$" + String.format("%.2f", creationFee));
            LanguageUtil.sendMessage(player, "§7Total System Shops: §e" + totalShops);
            LanguageUtil.sendMessage(player, "");
            
            if (playerBalance < creationFee) {
                LanguageUtil.sendMessage(player, "§c⚠ Insufficient funds to create a shop!");
                LanguageUtil.sendMessage(player, "§7You need §e$" + String.format("%.2f", creationFee - playerBalance) + " §7more.");
            } else if (playerShops.size() >= maxShops) {
                LanguageUtil.sendMessage(player, "§c⚠ You have reached the shop limit!");
                LanguageUtil.sendMessage(player, "§7Delete a shop to create a new one.");
            } else {
                LanguageUtil.sendMessage(player, "§a✓ You can create a shop!");
                LanguageUtil.sendMessage(player, "§7Use: §e/shop create <name> <category> player");
            }
            
            if (!playerShops.isEmpty()) {
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§7Your shops:");
                for (Shop shop : playerShops) {
                    String status = shop.isActive() ? "§aActive" : "§cInactive";
                    LanguageUtil.sendMessage(player, "  §e" + shop.getName() + " §7[" + shop.getCategory() + "] - " + status);
                }
            }
            
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
                LanguageUtil.sendErrorMessage(player, "Invalid shop category. Valid categories: " + 
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
                        LanguageUtil.sendErrorMessage(player, "You don't have permission to create server shops.");
                        return 0;
                    }
                    break;
                case "auction":
                    type = Shop.ShopType.AUCTION_HOUSE;
                    break;
                default:
                    LanguageUtil.sendErrorMessage(player, "Invalid ownership type. Valid types: player, server, auction");
                    return 0;
            }
            
            LanguageUtil.sendMessage(player, "§6Creating shop: §e" + shopName);
            LanguageUtil.sendMessage(player, "§7Category: §e" + category);
            LanguageUtil.sendMessage(player, "§7Ownership: §e" + ownership);
            LanguageUtil.sendMessage(player, "§7Type: §e" + type.name());
            LanguageUtil.sendMessage(player, "§7Location: §e" + player.position());
            LanguageUtil.sendMessage(player, "§7Owner: §e" + player.getDisplayName().getString());
            
            // Check shop limits and fees before creation
            List<Shop> playerShops = shopManager.getPlayerShops(player.getUUID());
            int maxShops = shopManager.getMaxShopsPerPlayer();
            double creationFee = shopManager.getShopCreationFee();
            
            LanguageUtil.sendMessage(player, "§7Current shops: §e" + playerShops.size() + "§7/§e" + maxShops);
            LanguageUtil.sendMessage(player, "§7Creation fee: §e$" + String.format("%.2f", creationFee));
            
            if (playerShops.size() >= maxShops) {
                LanguageUtil.sendErrorMessage(player, "You have reached the maximum number of shops (" + maxShops + ").");
                return 0;
            }
            
            // Check player balance (both direct balance and bank accounts)
            com.zerog.neoessentials.economy.EconomyManager economyManager2 = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            com.zerog.neoessentials.economy.Currency defaultCurrency = 
                com.zerog.neoessentials.economy.CurrencyManager.getInstance().getDefaultCurrency();
            BankManager bankManager = economyManager.getBankManager();
            
            if (defaultCurrency == null) {
                LanguageUtil.sendErrorMessage(player, "No default currency configured. Please contact an administrator.");
                return 0;
            }
            
            // Get direct balance
            double directBalance = economyManager2.getBalance(player.getUUID(), defaultCurrency);
            
            // Get bank account balances
            List<BankAccount> playerAccounts = bankManager.getPlayerAccounts(player.getUUID());
            double totalBankBalance = 0.0;
            
            LanguageUtil.sendMessage(player, "§6Balance Summary:");
            LanguageUtil.sendMessage(player, "§7Direct balance: §e$" + String.format("%.2f", directBalance));
            
            for (BankAccount account : playerAccounts) {
                double accountBalance = account.getBalance(defaultCurrency);
                totalBankBalance += accountBalance;
                LanguageUtil.sendMessage(player, "§7Bank account " + account.getAccountNumber() + 
                    " (" + account.getAccountType().getDisplayName() + "): §e$" + String.format("%.2f", accountBalance));
            }
            
            double totalAvailable = directBalance + totalBankBalance;
            LanguageUtil.sendMessage(player, "§7Total available: §e$" + String.format("%.2f", totalAvailable));
            
            if (totalAvailable < creationFee) {
                LanguageUtil.sendErrorMessage(player, "Insufficient funds! You need $" + 
                    String.format("%.2f", creationFee) + " but only have $" + 
                    String.format("%.2f", totalAvailable) + " total (direct + bank accounts).");
                return 0;
            }
            
            // Deduct fee (if any)
            boolean paymentSuccessful = true;
            if (creationFee > 0) {
                double remainingFee = creationFee;
                
                if (directBalance >= remainingFee) {
                    // Pay entirely from direct balance
                    economyManager2.setBalance(player.getUUID(), defaultCurrency, directBalance - remainingFee);
                    LanguageUtil.sendMessage(player, "§7Payment: §e$" + String.format("%.2f", remainingFee) + " deducted from direct balance");
                } else {
                    // Use direct balance first, then bank accounts
                    if (directBalance > 0) {
                        economyManager2.setBalance(player.getUUID(), defaultCurrency, 0);
                        remainingFee -= directBalance;
                        LanguageUtil.sendMessage(player, "§7Payment: §e$" + String.format("%.2f", directBalance) + " deducted from direct balance");
                    }
                    
                    // Deduct remaining from bank accounts (largest balance first)
                    playerAccounts.sort((a, b) -> Double.compare(b.getBalance(defaultCurrency), a.getBalance(defaultCurrency)));
                    
                    for (BankAccount account : playerAccounts) {
                        if (remainingFee <= 0) break;
                        
                        double accountBalance = account.getBalance(defaultCurrency);
                        if (accountBalance > 0) {
                            double deduction = Math.min(accountBalance, remainingFee);
                            account.withdraw(defaultCurrency, deduction, "Shop creation fee");
                            remainingFee -= deduction;
                            LanguageUtil.sendMessage(player, "§7Payment: §e$" + String.format("%.2f", deduction) + 
                                " deducted from account " + account.getAccountNumber());
                        }
                    }
                    
                    paymentSuccessful = (remainingFee <= 0.01); // Allow for small rounding errors
                }
            } else {
                LanguageUtil.sendMessage(player, "§7No creation fee required.");
            }
            
            if (!paymentSuccessful) {
                LanguageUtil.sendErrorMessage(player, "Payment failed. Please try again.");
                return 0;
            }
            
            // Create the shop (skip payment since we already handled it above)
            Shop shop = shopManager.createShop(player.getUUID(), shopName, shopName, category, type, true);
            if (shop != null) {
                MessageUtil.sendSuccessMessage(player, "Shop created successfully! ID: " + shop.getShopId());
                LanguageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
                LanguageUtil.sendMessage(player, "§7Use §e/shop manage " + shopName + " §7to configure your shop");
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to create shop. Please check server logs for details.");
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
            
            LanguageUtil.sendMessage(player, "§6=== Available Shops ===");
            
            if (filter == null || filter.equals("all")) {
                LanguageUtil.sendMessage(player, "§7Showing all shops:");
            } else if (filter.equals("mine")) {
                LanguageUtil.sendMessage(player, "§7Showing your shops:");
            } else {
                LanguageUtil.sendMessage(player, "§7Showing shops of type: §e" + filter);
            }
            
            // Get shops and display them
            List<Shop> shops;
            if (filter == null || filter.equals("all")) {
                shops = shopManager.getAllShops();
                LanguageUtil.sendMessage(player, "§8Debug: Total shops in system: " + shops.size());
            } else if (filter.equals("mine")) {
                shops = shopManager.getPlayerShops(player.getUUID());
                LanguageUtil.sendMessage(player, "§8Debug: Your shops: " + shops.size());
            } else {
                shops = shopManager.searchShops(filter, 50);
                LanguageUtil.sendMessage(player, "§8Debug: Shops matching '" + filter + "': " + shops.size());
            }
            
            if (shops.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No shops found.");
                if (filter == null || filter.equals("mine")) {
                    LanguageUtil.sendMessage(player, "§8Debug: Player UUID: " + player.getUUID());
                    LanguageUtil.sendMessage(player, "§8Debug: Shop creation fee: $" + 
                        String.format("%.2f", shopManager.getShopCreationFee()));
                }
                return 1;
            }
            
            // Limit to first 10 shops for readability
            for (Shop shop : shops.stream().limit(10).toList()) {
                String ownerType = shop.getShopType() == Shop.ShopType.SERVER_SHOP || 
                                  shop.getShopType() == Shop.ShopType.ADMIN ? "§cServer" : "§aPlayer";
                String status = shop.isActive() ? "§aActive" : "§cInactive";
                LanguageUtil.sendMessage(player, String.format("§e%s §7[%s] §7- %s §7- %s §7- §6ID: %s", 
                    shop.getName(), shop.getCategory(), ownerType, status, shop.getShopId().toString().substring(0, 8)));
            }
            
            if (shops.size() > 10) {
                LanguageUtil.sendMessage(player, "§7... and " + (shops.size() - 10) + " more shops");
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
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = shops.get(0);
            
            // Display shop information
            LanguageUtil.sendMessage(player, "§6=== Shop Information: §e" + shop.getShopName() + " §6===");
            LanguageUtil.sendMessage(player, "§7Shop ID: §e" + shop.getShopId().toString().substring(0, 8) + "...");
            LanguageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            LanguageUtil.sendMessage(player, "§7Type: §e" + shop.getShopType().getDisplayName());
            LanguageUtil.sendMessage(player, "§7Status: §" + (shop.isActive() ? "aActive" : "cInactive"));
            LanguageUtil.sendMessage(player, "§7Location: §e" + shop.getLocation());
            LanguageUtil.sendMessage(player, "§7Items for Sale: §e" + shop.getAvailableItems().size());
            LanguageUtil.sendMessage(player, "§7Total Revenue: §e$" + String.format("%.2f", shop.getTotalRevenue()));
            LanguageUtil.sendMessage(player, "§7Total Sales: §e" + shop.getTotalSales());
            LanguageUtil.sendMessage(player, "§7Customers: §e" + shop.getCustomerCount());
            LanguageUtil.sendMessage(player, "§7Created: §e" + new java.util.Date(shop.getCreatedTime()));
            
            // Show available items and prices
            if (!shop.getAvailableItems().isEmpty()) {
                LanguageUtil.sendMessage(player, "");
                LanguageUtil.sendMessage(player, "§6Available Items:");
                for (Shop.ShopItem shopItem : shop.getAvailableItems()) {
                    String itemName = shopItem.getItemName();
                    double price = shop.getItemPrice(shopItem.getItemId());
                    double sellPrice = price * 0.7; // 70% sell price
                    int stock = shopItem.getQuantity();
                    LanguageUtil.sendMessage(player, String.format("§7  %s: §aBuy $%.2f §7| §eSell $%.2f §7(Stock: %d)", 
                        itemName, price, sellPrice, stock));
                }
            } else {
                LanguageUtil.sendMessage(player, "§7No items currently in stock");
            }
            
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§7Use §e/shop buy " + shopName + " <item> [qty] §7to purchase items");
            LanguageUtil.sendMessage(player, "§7Use §e/shop sell " + shopName + " <item> [qty] §7to sell items");
            
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
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Verify exact name match
            if (!shop.getShopName().equalsIgnoreCase(shopName)) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Check ownership (players can only delete their own shops, admins can delete any)
            if (!hasShopPermission(player, shop, 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.DELETE_SHOP)) {
                LanguageUtil.sendErrorMessage(player, "You don't have permission to delete this shop.");
                return 0;
            }
            
            // Confirm deletion
            LanguageUtil.sendMessage(player, "§6Deleting shop: §e" + shopName);
            LanguageUtil.sendMessage(player, "§7Owner: §e" + (shop.getOwnerId() == null ? "Server" : "Player"));
            LanguageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            
            // Delete the shop
            boolean success = shopManager.deleteShop(shop.getShopId());
            
            if (success) {
                MessageUtil.sendSuccessMessage(player, "Shop '" + shopName + "' deleted successfully!");
                
                // If it was a player shop, could potentially refund part of creation fee
                if (shop.getOwnerId() != null && shop.getOwnerId().equals(player.getUUID())) {
                    LanguageUtil.sendMessage(player, "§7Shop deletion completed. Items and currency remain with owner.");
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to delete shop. Please try again.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int buyFromShop(CommandSourceStack source, String shopName, String itemName, int quantity) {
        // Open the shop GUI instead of command-based purchasing
        // This provides a better user experience and avoids command conflicts
        return openShopGUI(source, shopName);
    }
    
    private int sellToShop(CommandSourceStack source, String shopName, String itemName, int quantity) {
        // Open the shop GUI instead of command-based selling
        // This provides a better user experience and avoids command conflicts
        return openShopGUI(source, shopName);
    }
    
    private int stockShop(CommandSourceStack source, String shopName, String itemName, int quantity, double price) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Verify exact name match
            if (!shop.getShopName().equalsIgnoreCase(shopName)) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Check permissions
            if (!hasShopPermission(player, shop, 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
                LanguageUtil.sendErrorMessage(player, "You don't have permission to manage inventory for this shop.");
                return 0;
            }
            
            // Validate price
            if (price <= 0) {
                LanguageUtil.sendErrorMessage(player, "Price must be greater than 0.");
                return 0;
            }
            
            if (quantity <= 0) {
                LanguageUtil.sendErrorMessage(player, "Quantity must be greater than 0.");
                return 0;
            }
            
            // Validate that item exists
            if (!ItemHandler.isValidItem(itemName)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item: " + itemName);
                LanguageUtil.sendMessage(player, "§7Use tab completion or valid Minecraft item IDs (e.g., minecraft:diamond_sword)");
                return 0;
            }
            
            // Check if player has enough items in inventory (for player shops)
            boolean isOwner = shop.getOwnerId() != null && shop.getOwnerId().equals(player.getUUID());
            boolean isAdmin = player.hasPermissions(4);
            if (isOwner && !isAdmin && !ItemHandler.hasEnoughItems(player, itemName, quantity)) {
                LanguageUtil.sendErrorMessage(player, "You don't have enough " + ItemHandler.formatItemName(itemName) + " to stock. " +
                    "You have " + ItemHandler.getItemCount(player, itemName) + ", but need " + quantity + ".");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "§6Stocking Shop: §e" + shopName);
            LanguageUtil.sendMessage(player, "§7Item: §e" + ItemHandler.formatItemName(itemName));
            LanguageUtil.sendMessage(player, "§7Quantity: §e" + quantity);
            LanguageUtil.sendMessage(player, "§7Price per item: §e$" + String.format("%.2f", price));
            
            // Add item to shop inventory
            boolean success = shop.addItem(itemName, quantity, price, ItemHandler.formatItemName(itemName));
            
            if (success) {
                // Remove items from player inventory (for player shops)
                boolean itemsRemoved = true;
                if (isOwner && !isAdmin) {
                    itemsRemoved = ItemHandler.removeItemsFromPlayer(player, itemName, quantity);
                }
                
                if (itemsRemoved) {
                    MessageUtil.sendSuccessMessage(player, "Successfully stocked " + quantity + "x " + ItemHandler.formatItemName(itemName) + 
                        " in shop '" + shopName + "' for $" + String.format("%.2f", price) + " each");
                } else {
                    LanguageUtil.sendErrorMessage(player, "Failed to remove items from your inventory. Contact an administrator.");
                    // Remove the items from shop since we couldn't take them from player
                    shop.removeItem(itemName, quantity);
                    success = false;
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to stock item. Shop may be inactive or item may be invalid.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int setItemPrice(CommandSourceStack source, String shopName, String itemName, double buyPrice, double sellPrice) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Validate prices
            if (buyPrice <= 0) {
                LanguageUtil.sendErrorMessage(player, "Buy price must be greater than 0.");
                return 0;
            }
            
            if (sellPrice >= 0 && sellPrice > buyPrice) {
                LanguageUtil.sendErrorMessage(player, "Sell price cannot be higher than buy price.");
                return 0;
            }
            
            // Validate item exists
            if (!ItemHandler.isValidItem(itemName)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item: " + itemName);
                return 0;
            }
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Check permissions
            if (!hasShopPermission(player, shop, 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.SET_PRICES)) {
                LanguageUtil.sendErrorMessage(player, "You don't have permission to set prices for this shop.");
                return 0;
            }
            
            // Check if shop type allows price changes
            if (!shop.getShopType().allowsPriceChanges()) {
                LanguageUtil.sendErrorMessage(player, "This shop type doesn't allow price changes.");
                return 0;
            }
            
            // Set the price
            boolean success = shop.setItemPrice(itemName, buyPrice);
            
            if (success) {
                MessageUtil.sendSuccessMessage(player, "Price updated for " + ItemHandler.formatItemName(itemName) + " in shop '" + shopName + "'");
                LanguageUtil.sendMessage(player, "§7Buy Price: §e$" + String.format("%.2f", buyPrice));
                if (sellPrice >= 0) {
                    LanguageUtil.sendMessage(player, "§7Sell Price: §e$" + String.format("%.2f", sellPrice));
                } else {
                    double autoSellPrice = buyPrice * 0.7; // 70% of buy price
                    LanguageUtil.sendMessage(player, "§7Auto Sell Price: §e$" + String.format("%.2f", autoSellPrice) + " §7(70% of buy price)");
                }
            } else {
                LanguageUtil.sendErrorMessage(player, "Failed to update price. Item may not exist in shop inventory.");
            }
            
            return success ? 1 : 0;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int manageShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Find the shop by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 10);
            Shop targetShop = null;
            
            // First try exact match
            for (Shop shop : foundShops) {
                if (shop.getShopName().equalsIgnoreCase(shopName)) {
                    targetShop = shop;
                    break;
                }
            }
            
            // If no exact match, try partial match but only if player owns it or has permissions
            if (targetShop == null && !foundShops.isEmpty()) {
                for (Shop shop : foundShops) {
                    if (shop.getOwnerId().equals(player.getUUID()) || 
                        shop.getEmployeeManager().hasPermission(player.getUUID(), 
                            com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
                        targetShop = shop;
                        break;
                    }
                }
            }
            
            if (targetShop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found or you don't have permission to manage it.");
                return 0;
            }
            
            // Check permissions
            if (!targetShop.getOwnerId().equals(player.getUUID()) && 
                !targetShop.getEmployeeManager().hasPermission(player.getUUID(), 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
                LanguageUtil.sendErrorMessage(player, "You don't have permission to manage this shop!");
                return 0;
            }
            
            // Open the shop management GUI
            try {
                com.zerog.neoessentials.ui.shop.ShopManagementGUI gui = 
                    new com.zerog.neoessentials.ui.shop.ShopManagementGUI(targetShop, shopManager);
                gui.openMainMenu(player);
                
                MessageUtil.sendSuccessMessage(player, "Opening shop management interface for: " + targetShop.getShopName());
            } catch (Exception e) {
                LanguageUtil.sendErrorMessage(player, "Failed to open shop management interface: " + e.getMessage());
                NeoEssentials.LOGGER.error("Failed to open shop management GUI for shop: " + targetShop.getShopName(), e);
                return 0;
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int shopStats(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                LanguageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Get shop by searching for it by name
            List<Shop> foundShops = shopManager.searchShops(shopName, 1);
            if (foundShops.isEmpty()) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            Shop shop = foundShops.get(0);
            
            // Check ownership for detailed stats
            boolean isOwner = shop.getOwnerId() != null && shop.getOwnerId().equals(player.getUUID());
            boolean isAdmin = player.hasPermissions(2);
            boolean canViewDetailedStats = isOwner || isAdmin;
            
            LanguageUtil.sendMessage(player, "§6=== Shop Statistics: §e" + shopName + " §6===");
            LanguageUtil.sendMessage(player, "§7Shop Type: §e" + shop.getShopType().getDisplayName());
            LanguageUtil.sendMessage(player, "§7Category: §e" + shop.getCategory());
            LanguageUtil.sendMessage(player, "§7Status: " + (shop.isActive() ? "§aActive" : "§cInactive"));
            
            if (canViewDetailedStats) {
                // Detailed statistics for owners/admins
                double totalRevenue = shop.getTotalRevenue();
                int totalSales = shop.getTotalSales();
                int uniqueCustomers = shop.getCustomerCount();
                double averageSale = totalSales > 0 ? totalRevenue / totalSales : 0.0;
                
                LanguageUtil.sendMessage(player, "§7Total Revenue: §e" + defaultCurrency.format(totalRevenue));
                LanguageUtil.sendMessage(player, "§7Total Sales: §e" + totalSales);
                LanguageUtil.sendMessage(player, "§7Unique Customers: §e" + uniqueCustomers);
                LanguageUtil.sendMessage(player, "§7Average Sale: §e" + defaultCurrency.format(averageSale));
                
                // Inventory summary
                int totalItems = shop.getInventory().values().stream()
                    .mapToInt(item -> item.getQuantity())
                    .sum();
                int uniqueItems = shop.getInventory().size();
                
                LanguageUtil.sendMessage(player, "§7Inventory Items: §e" + totalItems + " §7(§e" + uniqueItems + " §7types)");
                
                // Recent activity
                long daysSinceCreation = (System.currentTimeMillis() - shop.getCreatedTime()) / (1000 * 60 * 60 * 24);
                LanguageUtil.sendMessage(player, "§7Shop Age: §e" + daysSinceCreation + " days");
                
                if (totalSales > 0) {
                    List<Shop.Sale> recentSales = shop.getSalesHistory(7).stream()
                        .sorted((a, b) -> Long.compare(b.getTimestamp(), a.getTimestamp()))
                        .limit(3)
                        .toList();
                    
                    LanguageUtil.sendMessage(player, "§7Recent Sales:");
                    for (Shop.Sale sale : recentSales) {
                        long hoursAgo = (System.currentTimeMillis() - sale.getTimestamp()) / (1000 * 60 * 60);
                        LanguageUtil.sendMessage(player, "  §8- §e" + sale.getQuantity() + "x " + 
                            ItemHandler.formatItemName(sale.getItemId()) + " §7(§e" + 
                            defaultCurrency.format(sale.getTotalPrice()) + "§7, " + hoursAgo + "h ago)");
                    }
                }
            } else {
                // Public statistics
                LanguageUtil.sendMessage(player, "§7Total Sales: §e" + shop.getTotalSales());
                LanguageUtil.sendMessage(player, "§7Shop Rating: §e★★★★☆ §7(Coming Soon)");
                LanguageUtil.sendMessage(player, "§7§oUse §e/shop info " + shopName + " §7§ofor inventory details");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int visitShop(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendMessage(player, "§cShop '" + shopName + "' does not exist");
                return 0;
            }
            
            if (!shop.hasLocation()) {
                LanguageUtil.sendMessage(player, "§cShop '" + shopName + "' has no teleport location set");
                LanguageUtil.sendMessage(player, "§7The shop owner can use §e/shop setlocation " + shopName + " §7to set one");
                return 0;
            }
            
            ShopLocation location = shop.getShopLocation();
            if (!location.teleportPlayer(player)) {
                LanguageUtil.sendMessage(player, "§cFailed to teleport to shop location");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "§6Successfully teleported to shop: §e" + shopName);
            LanguageUtil.sendMessage(player, "§7Location: " + shop.getFormattedLocation());
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    // Employee Management Methods
    
    private int addEmployee(CommandSourceStack source, String shopName, String playerName, String roleName) {
        try {
            ServerPlayer manager = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendMessage(manager, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Parse role
            ShopEmployeeManager.EmployeeRole role;
            try {
                role = ShopEmployeeManager.EmployeeRole.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                LanguageUtil.sendMessage(manager, "§cInvalid role: " + roleName);
                LanguageUtil.sendMessage(manager, "§7Valid roles: manager, cashier, stocker, sales_associate, viewer");
                return 0;
            }
            
            // Find player by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                LanguageUtil.sendMessage(manager, "§cPlayer not found: " + playerName);
                return 0;
            }
            
            // Add employee
            if (shop.addEmployee(manager.getUUID(), targetPlayer.getUUID(), targetPlayer.getDisplayName().getString(), role)) {
                LanguageUtil.sendMessage(manager, "§aSuccessfully added §e" + playerName + " §aas §e" + role.getDisplayName() + " §ato shop §e" + shopName);
                
                // Notify the new employee
                LanguageUtil.sendMessage(targetPlayer, "§aYou have been hired as §e" + role.getDisplayName() + " §aat shop §e" + shopName);
            } else {
                LanguageUtil.sendMessage(manager, "§cFailed to add employee. Check permissions and employee status.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int changeEmployeeRole(CommandSourceStack source, String shopName, String playerName, String roleName) {
        try {
            ServerPlayer manager = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendMessage(manager, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Parse role
            ShopEmployeeManager.EmployeeRole newRole;
            try {
                newRole = ShopEmployeeManager.EmployeeRole.valueOf(roleName.toUpperCase());
            } catch (IllegalArgumentException e) {
                LanguageUtil.sendMessage(manager, "§cInvalid role: " + roleName);
                LanguageUtil.sendMessage(manager, "§7Valid roles: manager, cashier, stocker, sales_associate, viewer");
                return 0;
            }
            
            // Find player by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            if (targetPlayer == null) {
                LanguageUtil.sendMessage(manager, "§cPlayer not found: " + playerName);
                return 0;
            }
            
            // Change role
            if (shop.changeEmployeeRole(manager.getUUID(), targetPlayer.getUUID(), newRole)) {
                LanguageUtil.sendMessage(manager, "§aSuccessfully changed §e" + playerName + "§a's role to §e" + newRole.getDisplayName() + " §ain shop §e" + shopName);
                
                // Notify the employee
                LanguageUtil.sendMessage(targetPlayer, "§aYour role in shop §e" + shopName + " §ahas been changed to §e" + newRole.getDisplayName());
            } else {
                LanguageUtil.sendMessage(manager, "§cFailed to change employee role. Check permissions.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int listEmployees(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendMessage(player, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Check if player has permission to view employees
            if (!shop.hasPermission(player.getUUID(), ShopEmployeeManager.ShopPermission.VIEW_BASIC_INFO)) {
                LanguageUtil.sendMessage(player, "§cYou don't have permission to view employees for this shop");
                return 0;
            }
            
            List<ShopEmployeeManager.ShopEmployee> employees = shop.getActiveEmployees();
            
            LanguageUtil.sendMessage(player, "§6=== Employees of §e" + shopName + " §6===");
            if (employees.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No employees found");
            } else {
                for (ShopEmployeeManager.ShopEmployee employee : employees) {
                    String roleColor = employee.getRole() == ShopEmployeeManager.EmployeeRole.OWNER ? "§c" : 
                                     employee.getRole() == ShopEmployeeManager.EmployeeRole.MANAGER ? "§6" : "§e";
                    
                    LanguageUtil.sendMessage(player, "§7• " + roleColor + employee.getPlayerName() + " §7- " + 
                                          roleColor + employee.getRole().getDisplayName());
                }
            }
            LanguageUtil.sendMessage(player, "§7Total employees: §e" + employees.size());
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    // Search Methods
    
    private int searchShopsByItem(CommandSourceStack source, String itemName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                LanguageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            // Validate item exists
            if (!ItemHandler.isValidItem(itemName)) {
                LanguageUtil.sendErrorMessage(player, "Invalid item: " + itemName);
                LanguageUtil.sendMessage(player, "§7Use tab completion or valid Minecraft item IDs");
                return 0;
            }
            
            String formattedItemName = ItemHandler.formatItemName(itemName);
            LanguageUtil.sendMessage(player, "§6=== Shops Selling: §e" + formattedItemName + " §6===");
            
            List<Shop> allShops = shopManager.getAllShops();
            List<Shop> shopsWithItem = allShops.stream()
                .filter(shop -> shop.isActive() && shop.hasItemForSale(itemName))
                .sorted((a, b) -> Double.compare(a.getItemPrice(itemName), b.getItemPrice(itemName)))
                .toList();
            
            if (shopsWithItem.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No shops currently sell " + formattedItemName);
                return 0;
            }
            
            for (Shop shop : shopsWithItem.stream().limit(10).toList()) {
                double price = shop.getItemPrice(itemName);
                ShopItem item = shop.getInventory().get(itemName);
                int quantity = item != null ? item.getQuantity() : 0;
                
                LanguageUtil.sendMessage(player, "§e" + shop.getShopName() + " §7- §e" + 
                    defaultCurrency.format(price) + " §7each (§e" + quantity + " §7available)");
            }
            
            if (shopsWithItem.size() > 10) {
                LanguageUtil.sendMessage(player, "§7... and " + (shopsWithItem.size() - 10) + " more shops");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int searchShopsByPrice(CommandSourceStack source, double minPrice, double maxPrice) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            CurrencyManager currencyManager = economyManager.getCurrencyManager();
            Currency defaultCurrency = currencyManager.getDefaultCurrency();
            
            if (defaultCurrency == null) {
                LanguageUtil.sendErrorMessage(player, "No default currency configured.");
                return 0;
            }
            
            if (minPrice > maxPrice) {
                LanguageUtil.sendErrorMessage(player, "Minimum price cannot be higher than maximum price.");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "§6=== Shops with Items Priced §e" + 
                defaultCurrency.format(minPrice) + " §6- §e" + defaultCurrency.format(maxPrice) + " §6===");
            
            List<Shop> allShops = shopManager.getAllShops();
            Map<Shop, List<String>> matchingShops = new HashMap<>();
            
            for (Shop shop : allShops) {
                if (!shop.isActive()) continue;
                
                List<String> matchingItems = new ArrayList<>();
                for (String itemId : shop.getInventory().keySet()) {
                    double price = shop.getItemPrice(itemId);
                    if (price >= minPrice && price <= maxPrice) {
                        matchingItems.add(itemId);
                    }
                }
                
                if (!matchingItems.isEmpty()) {
                    matchingShops.put(shop, matchingItems);
                }
            }
            
            if (matchingShops.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No shops have items in that price range");
                return 0;
            }
            
            int count = 0;
            for (Map.Entry<Shop, List<String>> entry : matchingShops.entrySet()) {
                if (count >= 10) break;
                
                Shop shop = entry.getKey();
                List<String> items = entry.getValue();
                
                LanguageUtil.sendMessage(player, "§e" + shop.getShopName() + " §7(§e" + items.size() + " §7items):");
                
                for (String itemId : items.stream().limit(3).toList()) {
                    double price = shop.getItemPrice(itemId);
                    LanguageUtil.sendMessage(player, "  §8- §e" + ItemHandler.formatItemName(itemId) + 
                        " §7at §e" + defaultCurrency.format(price));
                }
                
                if (items.size() > 3) {
                    LanguageUtil.sendMessage(player, "  §8... and " + (items.size() - 3) + " more items");
                }
                
                count++;
            }
            
            if (matchingShops.size() > 10) {
                LanguageUtil.sendMessage(player, "§7... and " + (matchingShops.size() - 10) + " more shops");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int searchShopsByCategory(CommandSourceStack source, String category) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            LanguageUtil.sendMessage(player, "§6=== Shops in Category: §e" + category + " §6===");
            
            List<Shop> allShops = shopManager.getAllShops();
            List<Shop> categoryShops = allShops.stream()
                .filter(shop -> shop.isActive() && 
                    shop.getCategory().equalsIgnoreCase(category))
                .sorted((a, b) -> a.getShopName().compareToIgnoreCase(b.getShopName()))
                .toList();
            
            if (categoryShops.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No active shops in category '" + category + "'");
                return 0;
            }
            
            for (Shop shop : categoryShops.stream().limit(15).toList()) {
                int itemTypes = shop.getInventory().size();
                String ownerName = "Server";
                
                if (shop.getOwnerId() != null) {
                    if (player.getServer() != null) {
                        ServerPlayer owner = player.getServer().getPlayerList().getPlayer(shop.getOwnerId());
                        if (owner != null) {
                            ownerName = owner.getName().getString();
                        } else {
                            ownerName = "Offline Player";
                        }
                    } else {
                        ownerName = "Unknown";
                    }
                }
                
                LanguageUtil.sendMessage(player, "§e" + shop.getShopName() + " §7by §e" + ownerName + 
                    " §7(§e" + itemTypes + " §7item types)");
            }
            
            if ( categoryShops.size() > 15) {
                LanguageUtil.sendMessage(player, "§7... and " + (categoryShops.size() - 15) + " more shops");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int setShopLocation(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendMessage(player, "§cShop '" + shopName + "' does not exist");
                return 0;
            }
            
            // Check if player owns the shop or is an admin
            if (!shop.getOwnerId().equals(player.getUUID()) && !player.hasPermissions(4)) {
               
                LanguageUtil.sendMessage(player, "§cYou don't have permission to set the location for this shop");
                return 0;
            }
            
            // Set the shop location to the player's current position
            shop.setLocationFromPlayer(player);
            shopManager.saveShop(shop); // Save the updated shop data
            
            LanguageUtil.sendMessage(player, "§6Shop location set successfully!");
            LanguageUtil.sendMessage(player, "§7Shop: §e" + shopName);
            LanguageUtil.sendMessage(player, "§7Location: " + shop.getFormattedLocation());
            LanguageUtil.sendMessage(player, "§7Players can now use §e/shop visit " + shopName + " §7to teleport here");
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int removeEmployee(CommandSourceStack source, String shopName, String playerName) {
        try {
            ServerPlayer manager = source.getPlayerOrException();
            
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                com.zerog.neoessentials.economy.EconomyManager.getInstance();
            ShopManager shopManager = economyManager.getShopManager();
            
            Shop shop = shopManager.getShopByName(shopName);
            if (shop == null) {
                LanguageUtil.sendMessage(manager, "§cShop not found: " + shopName);
                return 0;
            }
            
            // Find player by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(playerName);
            UUID targetUUID;
            if (targetPlayer != null) {
                targetUUID = targetPlayer.getUUID();
            } else {
                // Try to find offline player UUID (simplified approach)
                LanguageUtil.sendMessage(manager, "§cPlayer not found or offline: " + playerName);
                return 0;
            }
            
            // Remove employee
            if (shop.removeEmployee(manager.getUUID(), targetUUID)) {
                LanguageUtil.sendMessage(manager, "§aSuccessfully removed §e" + playerName + " §afrom shop §e" + shopName);
                
                // Notify the removed employee if online
                if (targetPlayer != null) {
                    LanguageUtil.sendMessage(targetPlayer, "§cYou have been removed from shop §e" + shopName);
                }
            } else {
                LanguageUtil.sendMessage(manager, "§cFailed to remove employee. Check permissions and employee status.");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    private int searchShopsByOwner(CommandSourceStack source, String ownerName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            LanguageUtil.sendMessage(player, "§6=== Shops owned by: §e" + ownerName + " §6===");
            
            // Find player UUID by name
            ServerPlayer targetPlayer = source.getServer().getPlayerList().getPlayerByName(ownerName);
            final UUID ownerUUID;
            if (targetPlayer != null) {
                ownerUUID = targetPlayer.getUUID();
            } else {
                // For offline players, we'd need a different approach
                // For now, just search by name in shop data
                LanguageUtil.sendMessage(player, "§cPlayer not found online: " + ownerName);
                return 0;
            }
            
            List<Shop> allShops = shopManager.getAllShops();
            List<Shop> ownerShops = allShops.stream()
                .filter(shop -> shop.isActive() && shop.getOwnerId().equals(ownerUUID))
                .toList();
            
            if (ownerShops.isEmpty()) {
                LanguageUtil.sendMessage(player, "§7No active shops found for " + ownerName);
                return 0;
            }
            
            for (Shop shop : ownerShops.stream().limit(15).toList()) {
                int itemTypes = shop.getInventory().size();
                int totalItems = shop.getInventory().values().stream()
                    .mapToInt(item -> item.getQuantity())
                    .sum();
                
                LanguageUtil.sendMessage(player, "§e" + shop.getShopName() + " §7(§e" + shop.getCategory() + 
                    "§7) - §e" + itemTypes + " §7types, §e" + totalItems + " §7total items");
            }
            
            if (ownerShops.size() > 15) {
                LanguageUtil.sendMessage(player, "§7... and " + (ownerShops.size() - 15) + " more shops");
            }
            
            return 1;
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    /**
     * Add item from player's hand to shop inventory
     */
    private int addItemFromHand(CommandSourceStack source, String shopName, int quantity) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Check if player is holding an item
            net.minecraft.world.item.ItemStack heldItem = player.getMainHandItem();
            if (heldItem.isEmpty()) {
                LanguageUtil.sendErrorMessage(player, "You must be holding an item to add it to the shop.");
                return 0;
            }
            
            // Get item information
            String itemName = heldItem.getItem().toString();
            String displayName = ItemHandler.formatItemName(itemName);
            
            // Validate quantity
            if (quantity <= 0) {
                LanguageUtil.sendErrorMessage(player, "Quantity must be greater than 0.");
                return 0;
            }
            
            if (quantity > heldItem.getCount()) {
                LanguageUtil.sendErrorMessage(player, "You only have " + heldItem.getCount() + " of this item.");
                return 0;
            }
            
            // Find shop
            List<Shop> foundShops = shopManager.searchShops(shopName, 10);
            Shop targetShop = null;
            
            for (Shop shop : foundShops) {
                if (shop.getShopName().equalsIgnoreCase(shopName)) {
                    targetShop = shop;
                    break;
                }
            }
            
            if (targetShop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Check permissions - use proper shop permission system
            boolean hasPermission = targetShop.getOwnerId().equals(player.getUUID()) ||
                targetShop.getEmployeeManager().hasPermission(player.getUUID(), 
                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY) ||
                player.hasPermissions(4); // Admin override
            
            if (!hasPermission) {
                LanguageUtil.sendErrorMessage(player, "You don't have permission to manage inventory for this shop.");
                return 0;
            }
            
            // Check if shop is active
            if (!targetShop.isActive()) {
                LanguageUtil.sendErrorMessage(player, "Cannot add items to an inactive shop.");
                return 0;
            }
            
            // Check if item already exists in shop
            if (targetShop.getInventory().containsKey(itemName)) {
                // Add to existing stock
                targetShop.addItem(itemName, quantity, 10.0, displayName); // This will add to existing
                MessageUtil.sendSuccessMessage(player, "Added " + quantity + " " + displayName + 
                    " to existing stock in shop '" + shopName + "'");
                LanguageUtil.sendMessage(player, "§7New stock: §e" + targetShop.getInventory().get(itemName).getQuantity());
            } else {
                // Add as new item with default pricing
                double defaultPrice = 10.0; // Default price, can be changed later
                boolean success = targetShop.addItem(itemName, quantity, defaultPrice, displayName);
                
                if (success) {
                    MessageUtil.sendSuccessMessage(player, "Added " + quantity + " " + displayName + 
                        " to shop '" + shopName + "' with default price $" + String.format("%.2f", defaultPrice));
                    LanguageUtil.sendMessage(player, "§7Use §e/shop price " + shopName + " " + itemName + " <buy-price>§7 to set a custom price");
                } else {
                    LanguageUtil.sendErrorMessage(player, "Failed to add item to shop. Shop may be full or item may be invalid.");
                    return 0;
                }
            }
            
            // Remove items from player's inventory
            heldItem.shrink(quantity);
            if (heldItem.getCount() <= 0) {
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
            }
            
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    /**
     * Opens the shop GUI interface for buying/selling
     */
    private int openShopGUI(CommandSourceStack source, String shopName) {
        try {
            ServerPlayer player = source.getPlayerOrException();
            com.zerog.neoessentials.economy.EconomyManager economyManager = 
                NeoEssentials.getInstance().getDataManager().getNewEconomyManager();
            ShopManager shopManager = economyManager.getShopManager();
            
            // Find shop
            List<Shop> foundShops = shopManager.searchShops(shopName, 10);
            Shop targetShop = null;
            
            for (Shop shop : foundShops) {
                if (shop.getShopName().equalsIgnoreCase(shopName)) {
                    targetShop = shop;
                    break;
                }
            }
            
            if (targetShop == null) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' not found.");
                return 0;
            }
            
            // Check if shop is active
            if (!targetShop.isActive()) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' is currently inactive.");
                return 0;
            }
            
            // Check if shop has any items
            if (targetShop.getInventory().isEmpty()) {
                LanguageUtil.sendErrorMessage(player, "Shop '" + shopName + "' has no items for sale.");
                return 0;
            }
            
            // Open the GUI
            com.zerog.neoessentials.ui.shop.ShopInterfaceGUI gui = 
                new com.zerog.neoessentials.ui.shop.ShopInterfaceGUI(targetShop, economyManager);
            gui.openShopInterface(player);
            
            LanguageUtil.sendMessage(player, "§7Opening shop interface for §e" + targetShop.getShopName());
            return 1;
            
        } catch (CommandSyntaxException e) {
            source.sendFailure(Component.literal("§cOnly players can use shop commands"));
            return 0;
        }
    }
    
    /**
     * Helper method to check if a player has permission to perform an action on a shop
     */
    private boolean hasShopPermission(ServerPlayer player, Shop shop, 
                                    com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission permission) {
        if (player.hasPermissions(4)) return true; // Server admin override
        if (shop.getOwnerId().equals(player.getUUID())) return true; // Owner has all permissions
        return shop.getEmployeeManager().hasPermission(player.getUUID(), permission);
    }
    
    /**
     * Helper method to check if a player can manage a shop (owner, manager, or admin)
     */
    private boolean canManageShop(ServerPlayer player, Shop shop) {
        return hasShopPermission(player, shop, 
            com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY);
    }
}
