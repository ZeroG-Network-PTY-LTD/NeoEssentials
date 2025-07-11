package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.gui.EnhancedShopInterface;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Enhanced commands for shop GUI interfaces and shop management
 */
public class EnhancedShopGuiCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main shop command - opens global shop GUI
        dispatcher.register(Commands.literal("shop")
            .requires(source -> source.isPlayer())
            .executes(EnhancedShopGuiCommands::openGlobalShop)
            .then(Commands.literal("my")
                .executes(EnhancedShopGuiCommands::openPersonalShop))
            .then(Commands.literal("global")
                .executes(EnhancedShopGuiCommands::openGlobalShop)));
            
        // Alternative GUI command
        dispatcher.register(Commands.literal("shopgui")
            .requires(source -> source.isPlayer())
            .executes(EnhancedShopGuiCommands::openGlobalShop));
        
        // Personal shop command
        dispatcher.register(Commands.literal("myshop")
            .requires(source -> source.isPlayer())
            .executes(EnhancedShopGuiCommands::openPersonalShop));
        
        // Aliases
        dispatcher.register(Commands.literal("sgui").redirect(dispatcher.getRoot().getChild("shop")));
        dispatcher.register(Commands.literal("market").redirect(dispatcher.getRoot().getChild("shop")));
        dispatcher.register(Commands.literal("pshop").redirect(dispatcher.getRoot().getChild("myshop")));
    }
    
    private static int openGlobalShop(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can open the shop GUI"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            NeoEssentials.LOGGER.info("Player {} is opening global shop GUI", player.getName().getString());
            
            if (economyManager == null) {
                source.sendFailure(Component.literal("§cEconomy manager is not initialized"));
                NeoEssentials.LOGGER.error("Economy manager is null for player {}", player.getName().getString());
                return 0;
            }
            
            if (!economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is disabled"));
                NeoEssentials.LOGGER.warn("Economy system is disabled for player {}", player.getName().getString());
                return 0;
            }
            
            // Check if shop manager exists
            if (economyManager.getShopManager() == null) {
                source.sendFailure(Component.literal("§cShop manager is not available"));
                NeoEssentials.LOGGER.error("Shop manager is null for player {}", player.getName().getString());
                return 0;
            }
            
            // Show some debug info
            player.sendSystemMessage(Component.literal("§7Opening shop GUI..."));
            player.sendSystemMessage(Component.literal("§7Economy enabled: §a" + economyManager.isEnabled()));
            player.sendSystemMessage(Component.literal("§7Your balance: §e" + 
                economyManager.getDefaultCurrency().format(economyManager.getBalance(player.getUUID()))));
            
            EnhancedShopInterface.openShop(player, economyManager);
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open global shop GUI", e);
            context.getSource().sendFailure(Component.literal("§cFailed to open shop interface: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int openPersonalShop(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can open personal shops"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            NeoEssentials.LOGGER.info("Player {} is opening personal shop GUI", player.getName().getString());
            
            if (economyManager == null) {
                source.sendFailure(Component.literal("§cEconomy manager is not initialized"));
                NeoEssentials.LOGGER.error("Economy manager is null for player {}", player.getName().getString());
                return 0;
            }
            
            if (!economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is disabled"));
                NeoEssentials.LOGGER.warn("Economy system is disabled for player {}", player.getName().getString());
                return 0;
            }
            
            // Check if shop manager exists
            if (economyManager.getShopManager() == null) {
                source.sendFailure(Component.literal("§cShop manager is not available"));
                NeoEssentials.LOGGER.error("Shop manager is null for player {}", player.getName().getString());
                return 0;
            }
            
            // Show some debug info
            player.sendSystemMessage(Component.literal("§7Opening personal shop GUI..."));
            player.sendSystemMessage(Component.literal("§7Your balance: §e" + 
                economyManager.getDefaultCurrency().format(economyManager.getBalance(player.getUUID()))));
            
            EnhancedShopInterface.openPersonalShop(player, economyManager);
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Failed to open personal shop GUI", e);
            context.getSource().sendFailure(Component.literal("§cFailed to open personal shop interface: " + e.getMessage()));
            return 0;
        }
    }
}