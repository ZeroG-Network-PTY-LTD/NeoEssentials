package com.zerog.neoessentials.commands.economy;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command to manually save shop data - useful for debugging shop persistence issues
 */
public class SaveShopsCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveShopsCommand.class);
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("saveshops")
                .requires(source -> source.hasPermission(4)) // OP level 4
                .executes(SaveShopsCommand::saveShops)
        );
    }
    
    private static int saveShops(CommandContext<CommandSourceStack> context) {
        try {
            var shopManager = com.zerog.neoessentials.economy.shops.ShopManager.getInstance();
            if (shopManager == null) {
                context.getSource().sendSystemMessage(Component.literal("§cShop manager not available!"));
                return 0;
            }
            
            int shopCount = shopManager.getSignShops().size();
            context.getSource().sendSystemMessage(
                Component.literal("§aSaving " + shopCount + " shops to storage...")
            );
            
            // Force synchronous save
            shopManager.saveShopsToStorageSync();
            
            context.getSource().sendSystemMessage(
                Component.literal("§aSuccessfully saved " + shopCount + " shops to storage!")
            );
            
            LOGGER.info("Manual shop save completed - {} shops saved", shopCount);
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendSystemMessage(
                Component.literal("§cError saving shops: " + e.getMessage())
            );
            LOGGER.error("Error in manual shop save command", e);
            return 0;
        }
    }
}
