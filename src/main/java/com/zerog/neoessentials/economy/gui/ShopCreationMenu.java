package com.zerog.neoessentials.economy.gui;

import com.zerog.neoessentials.NeoEssentials;
import com.zerog.neoessentials.economy.EconomyManager;
import com.zerog.neoessentials.economy.shop.ShopItem;
import com.zerog.neoessentials.economy.shop.ShopManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Menu for creating new shop items through GUI
 */
public class ShopCreationMenu extends AnvilMenu {
    
    private final ServerPlayer player;
    private final EconomyManager economyManager;
    private final ItemStack sourceItem;
    private final Container inputContainer;
    
    public ShopCreationMenu(int containerId, Inventory playerInventory, ServerPlayer player, 
                           EconomyManager economyManager, ItemStack sourceItem) {
        super(containerId, playerInventory, ContainerLevelAccess.NULL);
        this.player = player;
        this.economyManager = economyManager;
        this.sourceItem = sourceItem;
        this.inputContainer = new SimpleContainer(3);
        
        // Setup the anvil-like interface
        setupInterface();
    }
    
    private void setupInterface() {
        // Place the source item in the first slot
        inputContainer.setItem(0, sourceItem.copy());
        
        // Create a paper with instructions in the second slot
        ItemStack instructionPaper = new ItemStack(Items.PAPER);
        instructionPaper.set(DataComponents.CUSTOM_NAME, Component.literal("§eType price in chat"));
        inputContainer.setItem(1, instructionPaper);
        
        // Create result item showing what will be created
        ItemStack resultItem = sourceItem.copy();
        resultItem.set(DataComponents.CUSTOM_NAME, Component.literal("§aShop Item Preview"));
        inputContainer.setItem(2, resultItem);
    }
    
    @Override
    public boolean stillValid(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
        return menuPlayer == player && menuPlayer.isAlive() && !menuPlayer.isRemoved();
    }
    
    @Override
    public void removed(@Nonnull net.minecraft.world.entity.player.Player menuPlayer) {
        super.removed(menuPlayer);
        
        // Give back the source item if the player closes without creating
        if (menuPlayer == player && !sourceItem.isEmpty()) {
            if (!player.getInventory().add(sourceItem)) {
                player.drop(sourceItem, false);
            }
        }
    }
    
    /**
     * Called when the player types in chat to set the price
     */
    public void setPriceFromChat(String priceText) {
        try {
            double price = Double.parseDouble(priceText);
            
            if (price <= 0) {
                player.sendSystemMessage(Component.literal("§cPrice must be greater than 0"));
                return;
            }
            
            // Create the shop item directly
            createShopItem(price);
            
        } catch (NumberFormatException e) {
            player.sendSystemMessage(Component.literal("§cInvalid price format. Please enter a number."));
        }
    }
    
    private void createShopItem(double price) {
        try {
            ShopManager shopManager = economyManager.getShopManager();
            
            if (shopManager == null) {
                player.sendSystemMessage(Component.literal("§cShop manager is not available"));
                return;
            }
            
            // Create the shop item
            ShopItem shopItem = new ShopItem.Builder()
                .id(UUID.randomUUID())
                .itemStack(sourceItem.copy())
                .type(ShopItem.Type.BUY)
                .buyPrice(BigDecimal.valueOf(price))
                .currency(economyManager.getDefaultCurrency())
                .stock(sourceItem.getCount())
                .maxStock(sourceItem.getCount())
                .createdBy(player.getUUID())
                .createdAt(LocalDateTime.now())
                .description("Player shop item")
                .adminItem(false)
                .build();
            
            // Add to shop
            if (shopManager.addShopItem(shopItem)) {
                player.sendSystemMessage(Component.literal("§aSuccessfully created shop listing for " + 
                    sourceItem.getHoverName().getString() + " at " + 
                    economyManager.getDefaultCurrency().format(BigDecimal.valueOf(price))));
                
                // Remove item from player's inventory
                ItemStack heldItem = player.getMainHandItem();
                if (!heldItem.isEmpty() && ItemStack.isSameItemSameComponents(heldItem, sourceItem)) {
                    heldItem.shrink(sourceItem.getCount());
                }
                
                // Close the menu
                player.closeContainer();
                
                // Reopen personal shop to show the new item
                try {
                    var server = player.getServer();
                    if (server != null) {
                        server.execute(() -> {
                            EnhancedShopInterface.openPersonalShop(player, economyManager);
                        });
                    } else {
                        EnhancedShopInterface.openPersonalShop(player, economyManager);
                    }
                } catch (Exception e) {
                    NeoEssentials.LOGGER.error("Error reopening personal shop", e);
                    EnhancedShopInterface.openPersonalShop(player, economyManager);
                }
                
            } else {
                player.sendSystemMessage(Component.literal("§cFailed to create shop listing"));
            }
            
        } catch (Exception e) {
            NeoEssentials.LOGGER.error("Error creating shop item", e);
            player.sendSystemMessage(Component.literal("§cError creating shop item: " + e.getMessage()));
        }
    }
}
