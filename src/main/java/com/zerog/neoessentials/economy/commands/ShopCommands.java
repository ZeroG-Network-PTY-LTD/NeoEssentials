package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.Currency;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Commands for shop system
 */
public class ShopCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shop")
            .then(Commands.literal("list")
                .executes(ShopCommands::listItems)
                .then(Commands.argument("type", StringArgumentType.word())
                    .executes(ShopCommands::listItemsByType)))
            
            .then(Commands.literal("buy")
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .executes(ctx -> buyItem(ctx, 1))
                    .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                        .executes(ctx -> buyItem(ctx, IntegerArgumentType.getInteger(ctx, "quantity"))))))
            
            .then(Commands.literal("sell")
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .executes(ctx -> sellItem(ctx, 1))
                    .then(Commands.argument("quantity", IntegerArgumentType.integer(1))
                        .executes(ctx -> sellItem(ctx, IntegerArgumentType.getInteger(ctx, "quantity"))))))
            
            .then(Commands.literal("search")
                .then(Commands.argument("query", StringArgumentType.greedyString())
                    .executes(ShopCommands::searchItems)))
            
            .then(Commands.literal("info")
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .executes(ShopCommands::showItemInfo)))
            
            .then(Commands.literal("add")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("type", StringArgumentType.word())
                    .then(Commands.argument("buyPrice", StringArgumentType.word())
                        .then(Commands.argument("sellPrice", StringArgumentType.word())
                            .then(Commands.argument("stock", IntegerArgumentType.integer(0))
                                .executes(ShopCommands::addShopItem))))))
            
            .then(Commands.literal("remove")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("itemId", StringArgumentType.string())
                    .executes(ShopCommands::removeShopItem)))
            
            .then(Commands.literal("stats")
                .requires(source -> source.hasPermission(2))
                .executes(ShopCommands::showStats))
        );
    }
    
    private static int listItems(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        List<ShopItem> items = shopManager.getBuyableItems();
        if (items.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No items available in the shop"), false);
            return 1;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== Shop Items ==="), false);
        for (int i = 0; i < Math.min(10, items.size()); i++) {
            ShopItem item = items.get(i);
            String message = String.format("§e%s §7- §a%s §7(Stock: §b%d§7) §8[ID: %s]",
                    item.getItemStack().getDisplayName().getString(),
                    item.getCurrency().format(item.getBuyPrice()),
                    item.getStock(),
                    item.getId().toString().substring(0, 8));
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        
        if (items.size() > 10) {
            context.getSource().sendSuccess(() -> Component.literal("§7... and " + (items.size() - 10) + " more items"), false);
        }
        
        return 1;
    }
    
    private static int listItemsByType(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        String typeStr = StringArgumentType.getString(context, "type");
        ShopItem.Type type;
        
        try {
            type = ShopItem.Type.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid type. Use: buy, sell, or both"));
            return 0;
        }
        
        List<ShopItem> items = shopManager.searchItems(null, type);
        if (items.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No " + typeStr + " items found"), false);
            return 1;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== " + typeStr.toUpperCase() + " Items ==="), false);
        for (int i = 0; i < Math.min(10, items.size()); i++) {
            ShopItem item = items.get(i);
            String price = item.canBuy() ? item.getCurrency().format(item.getBuyPrice()) : 
                          item.canSell() ? item.getCurrency().format(item.getSellPrice()) : "N/A";
            String message = String.format("§e%s §7- §a%s §7(Stock: §b%d§7) §8[ID: %s]",
                    item.getItemStack().getDisplayName().getString(),
                    price,
                    item.getStock(),
                    item.getId().toString().substring(0, 8));
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        
        return 1;
    }
    
    private static int buyItem(CommandContext<CommandSourceStack> context, int quantity) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        String itemIdStr = StringArgumentType.getString(context, "itemId");
        UUID itemId;
        
        try {
            itemId = UUID.fromString(itemIdStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid item ID format"));
            return 0;
        }
        
        ShopManager.BuyResult result = shopManager.buyItem(player, itemId, quantity);
        
        if (result.isSuccess()) {
            context.getSource().sendSuccess(() -> Component.literal("§a" + result.getMessage()), false);
        } else {
            context.getSource().sendFailure(Component.literal("§c" + result.getMessage()));
        }
        
        return result.isSuccess() ? 1 : 0;
    }
    
    private static int sellItem(CommandContext<CommandSourceStack> context, int quantity) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        String itemIdStr = StringArgumentType.getString(context, "itemId");
        UUID itemId;
        
        try {
            itemId = UUID.fromString(itemIdStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid item ID format"));
            return 0;
        }
        
        ShopManager.SellResult result = shopManager.sellItem(player, itemId, quantity);
        
        if (result.isSuccess()) {
            context.getSource().sendSuccess(() -> Component.literal("§a" + result.getMessage()), false);
        } else {
            context.getSource().sendFailure(Component.literal("§c" + result.getMessage()));
        }
        
        return result.isSuccess() ? 1 : 0;
    }
    
    private static int searchItems(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        String query = StringArgumentType.getString(context, "query");
        List<ShopItem> items = shopManager.searchItems(query, null);
        
        if (items.isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("No items found matching: " + query), false);
            return 1;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== Search Results for: " + query + " ==="), false);
        for (int i = 0; i < Math.min(10, items.size()); i++) {
            ShopItem item = items.get(i);
            String typeInfo = item.canBuy() && item.canSell() ? "Buy/Sell" :
                            item.canBuy() ? "Buy only" : "Sell only";
            String message = String.format("§e%s §7- §b%s §7(Stock: §b%d§7) §8[ID: %s]",
                    item.getItemStack().getDisplayName().getString(),
                    typeInfo,
                    item.getStock(),
                    item.getId().toString().substring(0, 8));
            context.getSource().sendSuccess(() -> Component.literal(message), false);
        }
        
        return 1;
    }
    
    private static int showItemInfo(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        String itemIdStr = StringArgumentType.getString(context, "itemId");
        UUID itemId;
        
        try {
            itemId = UUID.fromString(itemIdStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid item ID format"));
            return 0;
        }
        
        ShopItem item = shopManager.getShopItem(itemId).orElse(null);
        if (item == null) {
            context.getSource().sendFailure(Component.literal("Item not found"));
            return 0;
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== Item Info ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§eName: §f" + item.getItemStack().getDisplayName().getString()), false);
        context.getSource().sendSuccess(() -> Component.literal("§eType: §f" + item.getType()), false);
        
        if (item.canBuy()) {
            context.getSource().sendSuccess(() -> Component.literal("§eBuy Price: §a" + item.getCurrency().format(item.getBuyPrice())), false);
        }
        if (item.canSell()) {
            context.getSource().sendSuccess(() -> Component.literal("§eSell Price: §a" + item.getCurrency().format(item.getSellPrice())), false);
        }
        
        context.getSource().sendSuccess(() -> Component.literal("§eStock: §b" + item.getStock() + " / " + item.getMaxStock()), false);
        context.getSource().sendSuccess(() -> Component.literal("§eAdmin Item: §f" + (item.isAdminItem() ? "Yes" : "No")), false);
        
        if (!item.getDescription().isEmpty()) {
            context.getSource().sendSuccess(() -> Component.literal("§eDescription: §f" + item.getDescription()), false);
        }
        
        return 1;
    }
    
    private static int addShopItem(CommandContext<CommandSourceStack> context) {
        if (!(context.getSource().getEntity() instanceof ServerPlayer player)) {
            context.getSource().sendFailure(Component.literal("Only players can use this command"));
            return 0;
        }
        
        ShopManager shopManager = getShopManager();
        EconomyManager economyManager = getEconomyManager();
        if (shopManager == null || economyManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        ItemStack heldItem = player.getMainHandItem();
        if (heldItem.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must hold an item to add to the shop"));
            return 0;
        }
        
        String typeStr = StringArgumentType.getString(context, "type");
        String buyPriceStr = StringArgumentType.getString(context, "buyPrice");
        String sellPriceStr = StringArgumentType.getString(context, "sellPrice");
        int stock = IntegerArgumentType.getInteger(context, "stock");
        
        ShopItem.Type type;
        try {
            type = ShopItem.Type.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid type. Use: buy, sell, or both"));
            return 0;
        }
        
        BigDecimal buyPrice = null;
        BigDecimal sellPrice = null;
        
        try {
            if (!buyPriceStr.equals("-")) {
                buyPrice = new BigDecimal(buyPriceStr);
            }
            if (!sellPriceStr.equals("-")) {
                sellPrice = new BigDecimal(sellPriceStr);
            }
        } catch (NumberFormatException e) {
            context.getSource().sendFailure(Component.literal("Invalid price format. Use numbers or '-' to skip"));
            return 0;
        }
        
        Currency defaultCurrency = economyManager.getDefaultCurrency();
        
        ShopItem.Builder builder = new ShopItem.Builder()
                .itemStack(heldItem.copy())
                .type(type)
                .currency(defaultCurrency)
                .stock(stock)
                .maxStock(999)
                .createdBy(player.getUUID())
                .adminItem(true);
        
        if (buyPrice != null) {
            builder.buyPrice(buyPrice);
        }
        if (sellPrice != null) {
            builder.sellPrice(sellPrice);
        }
        
        try {
            ShopItem item = builder.build();
            if (shopManager.addShopItem(item)) {
                context.getSource().sendSuccess(() -> Component.literal("§aAdded item to shop with ID: " + item.getId().toString().substring(0, 8)), false);
                return 1;
            } else {
                context.getSource().sendFailure(Component.literal("Failed to add item to shop"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error creating shop item: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int removeShopItem(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        String itemIdStr = StringArgumentType.getString(context, "itemId");
        UUID itemId;
        
        try {
            itemId = UUID.fromString(itemIdStr);
        } catch (IllegalArgumentException e) {
            context.getSource().sendFailure(Component.literal("Invalid item ID format"));
            return 0;
        }
        
        if (shopManager.removeShopItem(itemId)) {
            context.getSource().sendSuccess(() -> Component.literal("§aRemoved item from shop"), false);
            return 1;
        } else {
            context.getSource().sendFailure(Component.literal("Item not found or could not be removed"));
            return 0;
        }
    }
    
    private static int showStats(CommandContext<CommandSourceStack> context) {
        ShopManager shopManager = getShopManager();
        if (shopManager == null) {
            context.getSource().sendFailure(Component.literal("Shop system is not available"));
            return 0;
        }
        
        ShopManager.ShopStatistics stats = shopManager.getStatistics();
        
        context.getSource().sendSuccess(() -> Component.literal("§6=== Shop Statistics ==="), false);
        context.getSource().sendSuccess(() -> Component.literal("§eTotal Items: §f" + stats.getTotalItems()), false);
        context.getSource().sendSuccess(() -> Component.literal("§eBuyable Items: §f" + stats.getBuyableItems()), false);
        context.getSource().sendSuccess(() -> Component.literal("§eSellable Items: §f" + stats.getSellableItems()), false);
        context.getSource().sendSuccess(() -> Component.literal("§eAdmin Items: §f" + stats.getAdminItems()), false);
        
        return 1;
    }
    
    private static ShopManager getShopManager() {
        EconomyManager economyManager = getEconomyManager();
        return economyManager != null ? economyManager.getShopManager() : null;
    }
    
    private static EconomyManager getEconomyManager() {
        return NeoEssentials.getInstance().getEconomyManager();
    }
}
