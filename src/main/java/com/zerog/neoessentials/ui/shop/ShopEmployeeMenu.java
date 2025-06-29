package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.ShopEmployeeManager;
import com.zerog.neoessentials.utils.MessageUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;

import java.util.UUID;

/**
 * Menu handler for Shop Employee Management GUI
 */
public class ShopEmployeeMenu extends AbstractContainerMenu {
    
    private final SimpleContainer container;
    private final Shop shop;
    private final ShopManager shopManager;
    
    public ShopEmployeeMenu(int containerId, Inventory playerInventory, SimpleContainer container, 
                           Shop shop, ShopManager shopManager) {
        super(MenuType.GENERIC_9x6, containerId);
        this.container = container;
        this.shop = shop;
        this.shopManager = shopManager;
        
        // Add container slots
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(container, row * 9 + col, 8 + col * 18, 18 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false; // Don't allow placing items
                    }
                });
            }
        }
        
        // Add player inventory slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        
        // Add player hotbar slots
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        return ItemStack.EMPTY; // Don't allow quick move
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId < 0 || slotId >= container.getContainerSize()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }
        
        ItemStack clickedItem = container.getItem(slotId);
        if (clickedItem.isEmpty() || !clickedItem.has(DataComponents.CUSTOM_DATA)) {
            return;
        }
        
        CustomData customData = clickedItem.get(DataComponents.CUSTOM_DATA);
        if (customData == null) return;
        
        CompoundTag actionData = customData.copyTag();
        String action = actionData.getString("Action");
        
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        handleAction(action, actionData, serverPlayer, clickType);
    }
    
    /**
     * Handles GUI actions
     */
    private void handleAction(String action, CompoundTag actionData, ServerPlayer player, ClickType clickType) {
        switch (action) {
            case "hire_employee":
                MessageUtil.sendMessage(player, "§eTo hire an employee, use: §f/shop employee hire <playername> <role>");
                MessageUtil.sendMessage(player, "§7Available roles: Manager, Cashier, Stocker, Sales_Associate, Viewer");
                player.closeContainer();
                break;
                
            case "view_roles":
                new ShopEmployeeGUI(shop, shopManager).openRoleSelection(player);
                break;
                
            case "manage_employee":
                String employeeIdStr = actionData.getString("EmployeeId");
                String employeeName = actionData.getString("EmployeeName");
                
                try {
                    UUID employeeId = UUID.fromString(employeeIdStr);
                    handleEmployeeAction(employeeId, employeeName, player, clickType);
                } catch (IllegalArgumentException e) {
                    MessageUtil.sendMessage(player, "§cInvalid employee ID!");
                }
                break;
                
            case "back_to_main":
                new ShopManagementGUI(shop, shopManager).openMainMenu(player);
                break;
                
            case "back_to_employees":
                new ShopEmployeeGUI(shop, shopManager).openEmployeeMenu(player);
                break;
                
            default:
                MessageUtil.sendMessage(player, "§cUnknown action: " + action);
                break;
        }
    }
    
    /**
     * Handles employee-specific actions
     */
    private void handleEmployeeAction(UUID employeeId, String employeeName, ServerPlayer player, ClickType clickType) {
        ShopEmployeeManager.ShopEmployee employee = shop.getEmployeeManager().getEmployee(employeeId);
        if (employee == null) {
            MessageUtil.sendMessage(player, "§cEmployee not found!");
            return;
        }
        
        // Check if player can manage this employee
        boolean canManage = shop.getOwnerId().equals(player.getUUID()) || 
            (shop.getEmployeeManager().hasPermission(player.getUUID(), 
                ShopEmployeeManager.ShopPermission.HIRE_EMPLOYEES) &&
             !employee.getPlayerId().equals(shop.getOwnerId()));
        
        if (!canManage) {
            MessageUtil.sendMessage(player, "§cYou don't have permission to manage this employee!");
            return;
        }
        
        switch (clickType) {
            case LEFT: // Edit role
                MessageUtil.sendMessage(player, "§eTo change " + employeeName + "'s role, use:");
                MessageUtil.sendMessage(player, "§f/shop employee setrole " + employeeName + " <role>");
                MessageUtil.sendMessage(player, "§7Available roles: Manager, Cashier, Stocker, Sales_Associate, Viewer");
                player.closeContainer();
                break;
                
            case RIGHT: // Toggle active status
                if (employee.isActive()) {
                    employee.setActive(false);
                    MessageUtil.sendMessage(player, "§6" + employeeName + " has been suspended.");
                } else {
                    employee.setActive(true);
                    MessageUtil.sendMessage(player, "§6" + employeeName + " has been reactivated.");
                }
                // Refresh the GUI
                new ShopEmployeeGUI(shop, shopManager).openEmployeeMenu(player);
                break;
                
            case SHIFT_LEFT: // Remove employee
                if (shop.getEmployeeManager().removeEmployee(employeeId, player.getUUID())) {
                    MessageUtil.sendMessage(player, "§6" + employeeName + " has been removed from the shop.");
                    // Refresh the GUI
                    new ShopEmployeeGUI(shop, shopManager).openEmployeeMenu(player);
                } else {
                    MessageUtil.sendMessage(player, "§cFailed to remove employee!");
                }
                break;
                
            default:
                // Show employee details
                MessageUtil.sendMessage(player, "§6=== " + employeeName + " ===");
                MessageUtil.sendMessage(player, "§7Role: §f" + employee.getRole().getDisplayName());
                MessageUtil.sendMessage(player, "§7Status: " + (employee.isActive() ? "§aActive" : "§cInactive"));
                MessageUtil.sendMessage(player, "§7Use left-click to edit, right-click to toggle status");
                break;
        }
    }
}
