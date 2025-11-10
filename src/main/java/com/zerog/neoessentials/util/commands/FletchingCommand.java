package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /fletching command - Opens a virtual fletching table GUI
 * 
 * NOTE: Vanilla Minecraft does not provide a FletchingTableMenu class.
 * The fletching table in vanilla has no functional GUI - it's purely decorative.
 * This command currently opens a crafting table as a workaround to allow
 * players to craft arrows and other items that would logically use a fletching table.
 * 
 * FUTURE ENHANCEMENT: Could implement a custom GUI with fletching-specific recipes.
 */
public class FletchingCommand {
    
    /**
     * Register the /fletching command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("fletching")) return;
        
        dispatcher.register(
            Commands.literal("fletching")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.fletching");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    
                    // Open fletching GUI
                    openFletchingGui(player);
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.fletching.opened"));
                    
                    return 1;
                })
        );
    }
    
    /**
     * Opens the fletching table GUI for the player
     * 
     * NOTE: Uses CraftingMenu as workaround since Minecraft doesn't provide FletchingTableMenu.
     * The fletching table has no functional GUI in vanilla - it's decorative only.
     */
    private static void openFletchingGui(ServerPlayer player) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return MessageUtil.info("commands.neoessentials.fletching.title");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                // Since FletchingTableMenu doesn't exist in vanilla, use crafting table for arrow crafting
                // In the future, this could be replaced with a custom GUI for fletching recipes
                return new CraftingMenu(containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())) {
                    @Override
                    public boolean stillValid(Player player) {
                        // Always valid since it's a virtual fletching table
                        return true;
                    }
                };
            }
        };
        
        player.openMenu(menuProvider);
    }
}