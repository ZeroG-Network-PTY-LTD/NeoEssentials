package com.zerog.neoessentials.util.commands;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.network.chat.Component;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;
import com.zerog.neoessentials.util.PermissionValidator;

/**
 * Implements the /anvil command - Opens a virtual anvil GUI interface
 * Allows players to repair, rename, and combine items anywhere
 */
public class AnvilCommand {
    
    /**
     * Register the /anvil command
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("anvil")) return;
        
        dispatcher.register(
            Commands.literal("anvil")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    PermissionValidator.PermissionResult permResult = 
                        PermissionValidator.validatePermission(ctx.getSource(), "neoessentials.anvil");
                    if (!permResult.hasPermission()) {
                        ctx.getSource().sendFailure(MessageUtil.error(permResult.getErrorMessage()));
                        return 0;
                    }
                    
                    ServerPlayer player = permResult.getPlayer();
                    
                    // Open anvil GUI
                    openAnvilGui(player);
                    player.sendSystemMessage(MessageUtil.success("commands.neoessentials.anvil.opened"));
                    
                    return 1;
                })
        );
    }
    
    /**
     * Opens the anvil GUI for the player
     */
    private static void openAnvilGui(ServerPlayer player) {
        MenuProvider menuProvider = new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return MessageUtil.info("commands.neoessentials.anvil.title");
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                // Create anvil menu with virtual access (no physical anvil required)
                return new AnvilMenu(containerId, playerInventory, ContainerLevelAccess.create(player.level(), player.blockPosition())) {
                    @Override
                    public boolean stillValid(Player player) {
                        // Always valid since it's a virtual anvil
                        return true;
                    }
                    
                    @Override
                    protected boolean mayPickup(Player player, boolean hasStack) {
                        // Allow pickup if player can afford the experience cost
                        return hasStack && player.experienceLevel >= this.repairItemCountCost;
                    }
                    
                    @Override
                    protected void onTake(Player player, net.minecraft.world.item.ItemStack stack) {
                        // Handle experience cost and durability damage (anvils normally take damage)
                        if (!player.getAbilities().instabuild) {
                            player.giveExperienceLevels(-this.repairItemCountCost);
                        }
                        
                        // Clear the input slots
                        this.inputSlots.setItem(0, net.minecraft.world.item.ItemStack.EMPTY);
                        this.inputSlots.setItem(1, net.minecraft.world.item.ItemStack.EMPTY);
                        
                        // Since this is virtual, we don't damage a physical anvil
                        this.repairItemCountCost = 0;
                        this.access.execute((level, pos) -> {
                            level.levelEvent(1030, pos, 0);
                        });
                    }
                };
            }
        };
        
        player.openMenu(menuProvider);
    }
}