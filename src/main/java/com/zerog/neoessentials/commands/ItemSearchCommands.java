package com.zerog.neoessentials.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.ItemHandler;
import com.zerog.neoessentials.util.LanguageUtil;
import com.zerog.neoessentials.util.PermissionUtil;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

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
                LanguageUtil.sendMessage(player, "§cNo items found matching: §e" + query);
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "§aFound §e" + results.size() + "§a items matching §e'" + query + "'§a:");
            
            int shown = 0;
            for (String itemId : results) {
                if (shown >= 20) {
                    LanguageUtil.sendMessage(player, "§7... and " + (results.size() - shown) + " more. Use a more specific search.");
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
                
                LanguageUtil.sendMessage(player, "§7- §e" + itemId + "§7 (§f" + displayName + "§7)");
                shown++;
            }
            
            return 1;
        } catch (Exception e) {
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                LanguageUtil.sendErrorMessage(player, "Error searching items: " + e.getMessage());
            } catch (Exception ex) {
                // Command source is not a player, log error instead
                NeoEssentials.LOGGER.error("Error in item search command: " + e.getMessage());
            }
            return 0;
        }
    }
    
    private static int listModItems(CommandContext<CommandSourceStack> context, String modId) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            List<String> items = ItemHandler.getItemsFromMod(modId);
            
            if (items.isEmpty()) {
                LanguageUtil.sendMessage(player, "§cNo items found for mod: §e" + modId);
                LanguageUtil.sendMessage(player, "§7Use §e/itemsearch mods §7to see available mods.");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "§aItems from mod §e" + modId + "§a (showing first 25):");
            
            int shown = 0;
            for (String itemId : items) {
                if (shown >= 25) {
                    LanguageUtil.sendMessage(player, "§7... and " + (items.size() - shown) + " more items from this mod.");
                    break;
                }
                
                LanguageUtil.sendMessage(player, "§7- §e" + itemId);
                shown++;
            }
            
            return 1;
        } catch (Exception e) {
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                LanguageUtil.sendErrorMessage(player, "Error listing mod items: " + e.getMessage());
            } catch (Exception ex) {
                NeoEssentials.LOGGER.error("Error in mod items command: " + e.getMessage());
            }
            return 0;
        }
    }
    
    private static int listLoadedMods(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            List<String> mods = ItemHandler.getLoadedModIds();
            
            if (mods.isEmpty()) {
                LanguageUtil.sendMessage(player, "§cNo modded items found. Only vanilla Minecraft items are available.");
                return 0;
            }
            
            LanguageUtil.sendMessage(player, "§aLoaded mods with items (§e" + mods.size() + "§a):");
            
            int shown = 0;
            for (String modId : mods) {
                if (shown >= 20) {
                    LanguageUtil.sendMessage(player, "§7... and " + (mods.size() - shown) + " more mods.");
                    break;
                }
                
                int itemCount = ItemHandler.getItemsFromMod(modId).size();
                LanguageUtil.sendMessage(player, "§7- §e" + modId + "§7 (§f" + itemCount + " items§7)");
                shown++;
            }
            
            LanguageUtil.sendMessage(player, "");
            LanguageUtil.sendMessage(player, "§7Use §e/itemsearch mod <modid> §7to see items from a specific mod.");
            
            return 1;
        } catch (Exception e) {
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                LanguageUtil.sendErrorMessage(player, "Error listing mods: " + e.getMessage());
            } catch (Exception ex) {
                NeoEssentials.LOGGER.error("Error in list mods command: " + e.getMessage());
            }
            return 0;
        }
    }
    
    private static int getItemInfo(CommandContext<CommandSourceStack> context, String itemId) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            var item = ItemHandler.getItemFromId(itemId);
            if (item == null) {
                LanguageUtil.sendMessage(player, "§cItem not found: §e" + itemId);
                return 0;
            }
            
            String displayName = item.getDescription().getString();
            String namespace = itemId.split(":")[0];
            ItemStack dummyStack = new ItemStack(item, 1);
            int maxStackSize = dummyStack.getMaxStackSize();
            
            LanguageUtil.sendMessage(player, "§e§l=== Item Information ===");
            LanguageUtil.sendMessage(player, "§7Item ID: §e" + itemId);
            LanguageUtil.sendMessage(player, "§7Display Name: §f" + displayName);
            LanguageUtil.sendMessage(player, "§7Mod/Namespace: §e" + namespace);
            LanguageUtil.sendMessage(player, "§7Max Stack Size: §f" + maxStackSize);
            
            // Check if item is valid for economy
            boolean validForEconomy = ItemHandler.isValidItem(itemId);
            LanguageUtil.sendMessage(player, "§7Economy Compatible: " + 
                (validForEconomy ? "§aYes" : "§cNo"));
            
            if (validForEconomy) {
                LanguageUtil.sendMessage(player, "§7You can use this item in shops, trading, and economy commands.");
            }
            
            return 1;
        } catch (Exception e) {
            try {
                ServerPlayer player = context.getSource().getPlayerOrException();
                LanguageUtil.sendErrorMessage(player, "Error getting item info: " + e.getMessage());
            } catch (Exception ex) {
                NeoEssentials.LOGGER.error("Error in item info command: " + e.getMessage());
            }
            return 0;
        }
    }
}
