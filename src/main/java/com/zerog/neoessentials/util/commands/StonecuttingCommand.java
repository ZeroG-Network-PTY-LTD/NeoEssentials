package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /stonecutting command - Opens a virtual stonecutter GUI
 * Allows players to cut stone blocks into different variants and shapes
 */
public class StonecuttingCommand {
    
    /**
     * Register the /stonecutting command with aliases
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("stonecutting")) return;
        
        // Register main command
        registerStonecuttingCommand(dispatcher, "stonecutting");
        // Register aliases
        registerStonecuttingCommand(dispatcher, "stonecutter");
        registerStonecuttingCommand(dispatcher, "stonecut");
    }
    
    private static void registerStonecuttingCommand(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        dispatcher.register(
            Commands.literal(commandName)
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.stonecutting");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    
                    // Open stonecutting GUI
                    openStonecuttingGui(player);
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.stonecutting.opened"));
                    
                    return 1;
                })
        );
    }
    
    /**
     * Opens the stonecutter GUI for the player
     */
    private static void openStonecuttingGui(ServerPlayer player) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return MessageUtil.info("commands.neoessentials.stonecutting.title");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                // Create stonecutter menu with virtual access (no physical stonecutter required)
                return new StonecutterMenu(containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())) {
                    @Override
                    public boolean stillValid(Player player) {
                        // Always valid since it's a virtual stonecutter
                        return true;
                    }
                };
            }
        };
        
        player.openMenu(menuProvider);
    }
}