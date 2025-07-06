package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Command-based shop GUI that provides interactive shop interface
 */
public class ShopGuiCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("shopgui")
            .requires(source -> source.hasPermission(0))
            .executes(ShopGuiCommands::openShopGui));
            
        // Also add as subcommand to shop
        dispatcher.register(Commands.literal("shop")
            .then(Commands.literal("gui")
                .executes(ShopGuiCommands::openShopGui)));
    }
    
    private static int openShopGui(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can open the shop GUI"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            ShopManager shopManager = economyManager.getShopManager();
            List<ShopItem> items = shopManager.getBuyableItems(); // Get buyable items
            
            if (items.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§eThe shop is currently empty"), false);
                return 0;
            }
            
            // Header
            source.sendSuccess(() -> Component.literal("§6=== NeoEssentials Shop ==="), false);
            source.sendSuccess(() -> Component.literal("§7Click on items to buy/sell"), false);
            source.sendSuccess(() -> Component.literal(""), false);
            
            // Display items (limit to first 10 for readability)
            for (int i = 0; i < Math.min(10, items.size()); i++) {
                ShopItem item = items.get(i);
                displayShopItem(source, item, economyManager);
            }
            
            if (items.size() > 10) {
                source.sendSuccess(() -> Component.literal("§7... and " + (items.size() - 10) + " more items"), false);
                source.sendSuccess(() -> Component.literal("§7Use §e/shop list§7 to see all items"), false);
            }
            
            // Footer
            source.sendSuccess(() -> Component.literal(""), false);
            source.sendSuccess(() -> Component.literal("§7Use §e/shop help§7 for more commands"), false);
            
            return items.size();
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error in shop GUI command", e);
            context.getSource().sendFailure(Component.literal("An error occurred while opening the shop"));
            return 0;
        }
    }
    
    private static void displayShopItem(CommandSourceStack source, ShopItem item, EconomyManager economyManager) {
        String itemName = item.getItemStack().getHoverName().getString();
        String buyPriceStr = item.canBuy() ? economyManager.formatCurrency(item.getBuyPrice()) : "N/A";
        String sellPriceStr = item.canSell() ? economyManager.formatCurrency(item.getSellPrice()) : "N/A";
        
        // Create clickable component for buying
        Component buyButton = Component.literal("§a[BUY]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop buy " + item.getId() + " 1"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal("§aClick to buy 1 " + itemName + "\n§7Price: " + buyPriceStr))));
        
        // Create clickable component for selling
        Component sellButton = Component.literal("§c[SELL]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop sell " + item.getId() + " 1"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal("§cClick to sell 1 " + itemName + "\n§7Price: " + sellPriceStr))));
        
        // Create info component
        Component infoButton = Component.literal("§e[INFO]")
            .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/shop info " + item.getId()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, 
                    Component.literal("§eClick for more information about " + itemName))));
        
        // Combine components
        Component itemLine = Component.literal("§b" + itemName + " §7- ")
            .append(item.canBuy() ? buyButton : Component.literal("§8[---]"))
            .append(Component.literal(" "))
            .append(item.canSell() ? sellButton : Component.literal("§8[---]"))
            .append(Component.literal(" "))
            .append(infoButton);
        
        source.sendSuccess(() -> itemLine, false);
    }
}
