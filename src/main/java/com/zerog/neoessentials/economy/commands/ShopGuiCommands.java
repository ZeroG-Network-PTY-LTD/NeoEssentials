package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.gui.SimpleShopInterface;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for opening shop GUI interfaces
 */
public class ShopGuiCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main shop command - opens GUI
        dispatcher.register(Commands.literal("shop")
            .requires(source -> source.isPlayer())
            .executes(ShopGuiCommands::openShopGui));
            
        // Alternative GUI command
        dispatcher.register(Commands.literal("shopgui")
            .requires(source -> source.isPlayer())
            .executes(ShopGuiCommands::openShopGui));
        
        // Aliases
        dispatcher.register(Commands.literal("sgui").redirect(dispatcher.getRoot().getChild("shop")));
        dispatcher.register(Commands.literal("market").redirect(dispatcher.getRoot().getChild("shop")));
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
            
            // Create and open the shop menu
            SimpleMenuProvider menuProvider = new SimpleMenuProvider(
                (containerId, inventory, menuPlayer) -> new ShopMenu(containerId, inventory, economyManager),
                Component.literal("Shop")
            );
            
            player.openMenu(menuProvider);
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening shop GUI", e);
            context.getSource().sendFailure(Component.literal("An error occurred while opening the shop"));
            return 0;
        }
    }
}
