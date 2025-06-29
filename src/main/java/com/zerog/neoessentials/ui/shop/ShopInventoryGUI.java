package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;

import java.util.List;
import java.util.Map;

/**
 * Shop Inventory Management GUI
 * Allows shop owners to manage their shop's inventory
 */
public class ShopInventoryGUI {
    
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopInventoryGUI(Shop shop, ShopManager shopManager) {
        this.shop = shop;
        this.shopManager = shopManager;
    }
    
    /**
     * Opens the shop inventory management interface
     */
    public void openInventoryMenu(ServerPlayer player) {
        // Check permissions
        if (!shop.getOwnerId().equals(player.getUUID()) && 
            !shop.getEmployeeManager().hasPermission(player.getUUID(), 
                com.zerog.neoessentials.economy.ShopEmployeeManager.ShopPermission.MANAGE_INVENTORY)) {
            MessageUtil.sendMessage(player, "§cYou don't have permission to manage this shop's inventory!");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupInventoryMenu(container);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§6Shop Inventory§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopInventoryMenu(containerId, playerInventory, container, shop, shopManager);
            }
        });
    }
    
    /**
     * Sets up the inventory management menu
     */
    private void setupInventoryMenu(SimpleContainer container) {
        // Back button
        ItemStack backItem = new ItemStack(Items.ARROW);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lBack to Main Menu"));
        backItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Return to shop management")
        )));
        
        CompoundTag backData = new CompoundTag();
        backData.putString("Action", "back_to_main");
        backItem.set(DataComponents.CUSTOM_DATA, CustomData.of(backData));
        container.setItem(0, backItem);
        
        // Add item button
        ItemStack addItem = new ItemStack(Items.GREEN_WOOL);
        addItem.set(DataComponents.CUSTOM_NAME, Component.literal("§a§lAdd Items from Inventory"));
        addItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Add items from your inventory"),
            Component.literal("§7to the shop's stock"),
            Component.literal("§8Click to select items")
        )));
        
        CompoundTag addData = new CompoundTag();
        addData.putString("Action", "add_items");
        addItem.set(DataComponents.CUSTOM_DATA, CustomData.of(addData));
        container.setItem(4, addItem);
        
        // Remove all items button (owner only)
        ItemStack removeAllItem = new ItemStack(Items.RED_WOOL);
        removeAllItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lRemove All Items"));
        removeAllItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Remove all items from shop"),
            Component.literal("§7and return them to your inventory"),
            Component.literal("§c§lWarning: This cannot be undone!"),
            Component.literal("§8Owner only")
        )));
        
        CompoundTag removeAllData = new CompoundTag();
        removeAllData.putString("Action", "remove_all_items");
        removeAllItem.set(DataComponents.CUSTOM_DATA, CustomData.of(removeAllData));
        container.setItem(8, removeAllItem);
        
        // Display current shop items
        Map<String, Shop.ShopItem> shopItems = shop.getInventory();
        int slot = 18; // Start from second row
        
        for (Map.Entry<String, Shop.ShopItem> entry : shopItems.entrySet()) {
            if (slot >= 45) break; // Don't fill bottom row
            
            Shop.ShopItem shopItem = entry.getValue();
            
            // Create display stack using ItemHandler
            String itemId = shopItem.getItemId();
            net.minecraft.world.item.Item item = com.zerog.neoessentials.economy.ItemHandler.getItemFromId(itemId);
            ItemStack displayStack = item != null ? new ItemStack(item) : new ItemStack(Items.BARRIER);
            
            // Update display name and lore
            displayStack.set(DataComponents.CUSTOM_NAME, 
                Component.literal("§e" + com.zerog.neoessentials.economy.ItemHandler.formatItemName(itemId)));
            
            displayStack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Stock: §a" + shopItem.getQuantity()),
                Component.literal("§7Price: §6$" + String.format("%.2f", shop.getItemPrice(itemId))),
                Component.literal(""),
                Component.literal("§8Left Click: Remove 1"),
                Component.literal("§8Right Click: Remove Stack"),
                Component.literal("§8Shift+Click: Remove All")
            )));
            
            // Add action data
            CompoundTag itemData = new CompoundTag();
            itemData.putString("Action", "modify_item");
            itemData.putString("ItemId", entry.getKey());
            displayStack.set(DataComponents.CUSTOM_DATA, CustomData.of(itemData));
            
            container.setItem(slot, displayStack);
            slot++;
        }
        
        // Add border items
        ItemStack borderItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        borderItem.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        
        // Fill borders
        for (int i = 9; i < 18; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, borderItem);
            }
        }
        
        for (int i = 45; i < 54; i++) {
            container.setItem(i, borderItem);
        }
    }
}
