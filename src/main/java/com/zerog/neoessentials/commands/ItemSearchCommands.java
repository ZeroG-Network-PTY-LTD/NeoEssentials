package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.economy.ItemHandler;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Commands for searching and exploring modded items.
 * Helps players discover and use modded items in the economy system.
 */
public class ItemSearchCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("itemsearch")
            .requires(source -> source.hasPermission(0))
            
            // Search for items by name
            .then(Commands.literal("find")
                .then(Commands.argument("query", StringArgumentType.string())
                    .executes(context -> searchItems(context, StringArgumentType.getString(context, "query")))))
            
            // List items from a specific mod
            .then(Commands.literal("mod")
                .then(Commands.argument("modid", StringArgumentType.string())
                    .suggests(TabCompletionUtil.MOD_ID_SUGGESTIONS)
                    .executes(context -> listModItems(context, StringArgumentType.getString(context, "modid")))))
            
            // List all loaded mods
            .then(Commands.literal("mods")
                .executes(context -> listLoadedMods(context)))
            
            // Get detailed info about a specific item
            .then(Commands.literal("info")
                .then(Commands.argument("item", StringArgumentType.string())
                    .suggests(TabCompletionUtil.ITEM_SUGGESTIONS)
                    .executes(context -> getItemInfo(context, StringArgumentType.getString(context, "item")))))
        );
    }
    
    private static int searchItems(CommandContext<CommandSourceStack> context, String query) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            List<String> results = ItemHandler.searchItems(query);
            
            if (results.isEmpty()) {
                MessageUtil.sendMessage(player, "§cNo items found matching: §e" + query);
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§aFound §e" + results.size() + "§a items matching §e'" + query + "'§a:");
            
            int shown = 0;
            for (String itemId : results) {
                if (shown >= 20) {
                    MessageUtil.sendMessage(player, "§7... and " + (results.size() - shown) + " more. Use a more specific search.");
                    break;
                }
                
                String displayName = "Unknown";
                try {
                    var item = ItemHandler.getItemFromId(itemId);
                    if (item != null) {
                        displayName = item.getDescription().getString();
                    }
                } catch (Exception e) {
                    // Use item ID as fallback
                }
                
                MessageUtil.sendMessage(player, "§7- §e" + itemId + "§7 (§f" + displayName + "§7)");
                shown++;
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(context.getSource(), "Error searching items: " + e.getMessage());
            return 0;
        }
    }
    
    private static int listModItems(CommandContext<CommandSourceStack> context, String modId) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            List<String> items = ItemHandler.getItemsFromMod(modId);
            
            if (items.isEmpty()) {
                MessageUtil.sendMessage(player, "§cNo items found for mod: §e" + modId);
                MessageUtil.sendMessage(player, "§7Use §e/itemsearch mods §7to see available mods.");
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§aItems from mod §e" + modId + "§a (showing first 25):");
            
            int shown = 0;
            for (String itemId : items) {
                if (shown >= 25) {
                    MessageUtil.sendMessage(player, "§7... and " + (items.size() - shown) + " more items from this mod.");
                    break;
                }
                
                MessageUtil.sendMessage(player, "§7- §e" + itemId);
                shown++;
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(context.getSource(), "Error listing mod items: " + e.getMessage());
            return 0;
        }
    }
    
    private static int listLoadedMods(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            List<String> mods = ItemHandler.getLoadedModIds();
            
            if (mods.isEmpty()) {
                MessageUtil.sendMessage(player, "§cNo modded items found. Only vanilla Minecraft items are available.");
                return 0;
            }
            
            MessageUtil.sendMessage(player, "§aLoaded mods with items (§e" + mods.size() + "§a):");
            
            int shown = 0;
            for (String modId : mods) {
                if (shown >= 20) {
                    MessageUtil.sendMessage(player, "§7... and " + (mods.size() - shown) + " more mods.");
                    break;
                }
                
                int itemCount = ItemHandler.getItemsFromMod(modId).size();
                MessageUtil.sendMessage(player, "§7- §e" + modId + "§7 (§f" + itemCount + " items§7)");
                shown++;
            }
            
            MessageUtil.sendMessage(player, "");
            MessageUtil.sendMessage(player, "§7Use §e/itemsearch mod <modid> §7to see items from a specific mod.");
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(context.getSource(), "Error listing mods: " + e.getMessage());
            return 0;
        }
    }
    
    private static int getItemInfo(CommandContext<CommandSourceStack> context, String itemId) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            var item = ItemHandler.getItemFromId(itemId);
            if (item == null) {
                MessageUtil.sendMessage(player, "§cItem not found: §e" + itemId);
                return 0;
            }
            
            String displayName = item.getDescription().getString();
            String namespace = itemId.split(":")[0];
            int maxStackSize = item.getMaxStackSize();
            
            MessageUtil.sendMessage(player, "§e§l=== Item Information ===");
            MessageUtil.sendMessage(player, "§7Item ID: §e" + itemId);
            MessageUtil.sendMessage(player, "§7Display Name: §f" + displayName);
            MessageUtil.sendMessage(player, "§7Mod/Namespace: §e" + namespace);
            MessageUtil.sendMessage(player, "§7Max Stack Size: §f" + maxStackSize);
            
            // Check if item is valid for economy
            boolean validForEconomy = ItemHandler.isValidItem(itemId);
            MessageUtil.sendMessage(player, "§7Economy Compatible: " + 
                (validForEconomy ? "§aYes" : "§cNo"));
            
            if (validForEconomy) {
                MessageUtil.sendMessage(player, "§7You can use this item in shops, trading, and economy commands.");
            }
            
            return 1;
        } catch (Exception e) {
            MessageUtil.sendErrorMessage(context.getSource(), "Error getting item info: " + e.getMessage());
            return 0;
        }
    }
}
