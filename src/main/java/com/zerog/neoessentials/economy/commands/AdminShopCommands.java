package com.zerog.neoessentials.economy.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.gui.AdminShopManagementInterface;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Admin commands for managing the shop system - GUI-based only
 * All admin shop management functions are handled through the GUI interface
 */
public class AdminShopCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adminshop")
            .requires(source -> source.hasPermission(3)) // Admin level permission
            .requires(source -> source.isPlayer())
            .executes(AdminShopCommands::openAdminShopGUI));
    }
    
    /**
     * Opens the admin shop management GUI
     * This is the only admin shop command - all management is done through the GUI
     */
    private static int openAdminShopGUI(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            ServerPlayer player = source.getPlayerOrException();
            EconomyManager economyManager = NeoEssentials.getInstance().getEconomyManager();
            
            if (economyManager == null) {
                source.sendFailure(Component.literal("§cEconomy system is not available"));
                return 0;
            }
            
            if (!economyManager.isEnabled()) {
                source.sendFailure(Component.literal("§cEconomy system is disabled"));
                return 0;
            }
            
            if (economyManager.getShopManager() == null) {
                source.sendFailure(Component.literal("§cShop manager is not available"));
                return 0;
            }
            
            // Open the admin shop management GUI
            AdminShopManagementInterface.openAdminShopManagement(player, economyManager);
            player.sendSystemMessage(Component.literal("§aOpened admin shop management GUI"));
            player.sendSystemMessage(Component.literal("§7All admin shop management is handled through this interface"));
            
            return 1;
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error opening admin shop GUI", e);
            context.getSource().sendFailure(Component.literal("§cError opening admin shop GUI: " + e.getMessage()));
            return 0;
        }
    }
}
