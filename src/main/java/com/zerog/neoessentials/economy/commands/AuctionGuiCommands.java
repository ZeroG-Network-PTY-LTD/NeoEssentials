package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.gui.SimpleAuctionInterface;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Commands for opening auction GUI interfaces
 */
public class AuctionGuiCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Main auction command - opens GUI
        dispatcher.register(Commands.literal("auction")
            .requires(source -> source.isPlayer())
            .executes(AuctionGuiCommands::openAuctionGui));
            
        // Alternative GUI command
        dispatcher.register(Commands.literal("auctiongui")
            .requires(source -> source.isPlayer())
            .executes(AuctionGuiCommands::openAuctionGui));
        
        // Aliases
        dispatcher.register(Commands.literal("agui").redirect(dispatcher.getRoot().getChild("auction")));
        dispatcher.register(Commands.literal("auctionhouse").redirect(dispatcher.getRoot().getChild("auction")));
        dispatcher.register(Commands.literal("ah").redirect(dispatcher.getRoot().getChild("auction")));
    }
    
    private static int openAuctionGui(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            
            if (!source.isPlayer()) {
                source.sendFailure(Component.literal("Only players can open the auction GUI"));
                return 0;
            }
            
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null || !economyManager.isEnabled()) {
                source.sendFailure(Component.literal("Economy system is not available"));
                return 0;
            }
            
            // Use the new simplified auction interface
            SimpleAuctionInterface auctionInterface = new SimpleAuctionInterface(economyManager);
            auctionInterface.openAuctionMenu(player);
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening auction GUI", e);
            context.getSource().sendFailure(Component.literal("An error occurred while opening the auction house"));
            return 0;
        }
    }
}