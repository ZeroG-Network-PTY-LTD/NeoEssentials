package com.zerog.neoessentials.ui.shop;

import com.zerog.neoessentials.economy.Shop;
import com.zerog.neoessentials.economy.ShopManager;
import com.zerog.neoessentials.economy.ShopEmployeeManager;
import com.zerog.neoessentials.util.LanguageUtil;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.SimpleContainer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;

/**
 * Shop Employee Management GUI for NeoEssentials
 * Provides a user interface for managing shop employees, roles, and permissions
 */
public class ShopEmployeeGUI {
    
    private final Shop shop;
    private final ShopManager shopManager;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM dd, yyyy");
    
    public ShopEmployeeGUI(Shop shop, ShopManager shopManager) {
        this.shop = shop;
        this.shopManager = shopManager;
    }
    
    /**
     * Opens the employee management interface
     */
    public void openEmployeeMenu(ServerPlayer player) {
        // Check permissions - only owner and managers can manage employees
        if (!shop.getOwnerId().equals(player.getUUID()) && 
            !shop.getEmployeeManager().hasPermission(player.getUUID(), 
                ShopEmployeeManager.ShopPermission.HIRE_EMPLOYEES)) {
            LanguageUtil.sendMessage(player, "§cYou don't have permission to manage shop employees!");
            return;
        }
        
        var container = new SimpleContainer(54); // 6 rows
        setupEmployeeMenu(container, player);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§bEmployees§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopEmployeeMenu(containerId, playerInventory, container, shop, shopManager);
            }
        });
    }
    
    /**
     * Sets up the employee management menu
     */
    private void setupEmployeeMenu(SimpleContainer container, ServerPlayer player) {
        // Add employee button (top center)
        ItemStack addEmployeeItem = new ItemStack(Items.PLAYER_HEAD);
        addEmployeeItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§a§lHire Employee"));
        addEmployeeItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Click to hire a new employee"),
            Component.literal("§7Requires player to be online"),
            Component.literal("§8Right-click to open")
        )));
        
        CompoundTag addData = new CompoundTag();
        addData.putString("Action", "hire_employee");
        addEmployeeItem.set(DataComponents.CUSTOM_DATA, CustomData.of(addData));
        
        container.setItem(4, addEmployeeItem);
        
        // Role management button
        ItemStack roleItem = new ItemStack(Items.DIAMOND);
        roleItem.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6§lRole Templates"));
        roleItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7View available roles"),
            Component.literal("§7and their permissions"),
            Component.literal("§8Click to view")
        )));
        
        CompoundTag roleData = new CompoundTag();
        roleData.putString("Action", "view_roles");
        roleItem.set(DataComponents.CUSTOM_DATA, CustomData.of(roleData));
        
        container.setItem(8, roleItem);
        
        // Current employees list
        List<ShopEmployeeManager.ShopEmployee> employees = shop.getEmployeeManager().getAllEmployees();
        int startSlot = 18; // Start from third row
        int slot = startSlot;
        
        for (ShopEmployeeManager.ShopEmployee employee : employees) {
            if (slot >= 45) break; // Don't overflow into bottom row
            
            ItemStack employeeItem = createEmployeeItem(employee, player);
            container.setItem(slot, employeeItem);
            slot++;
            
            // Skip to next row if we hit the right border
            if ((slot + 1) % 9 == 0) {
                slot += 2; // Skip right border and left border of next row
            }
        }
        
        // Add border items
        ItemStack borderItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        borderItem.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        
        // Fill border
        for (int i = 0; i < 9; i++) {
            if (i != 4 && i != 8) container.setItem(i, borderItem); // Top row except center and role button
        }
        for (int i = 45; i < 54; i++) {
            container.setItem(i, borderItem); // Bottom row
        }
        for (int i = 9; i < 45; i += 9) {
            container.setItem(i, borderItem); // Left column
            container.setItem(i + 8, borderItem); // Right column
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Items.BARRIER);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lBack"));
        backItem.set(DataComponents.LORE, new ItemLore(List.of(
            Component.literal("§7Return to shop management")
        )));
        
        CompoundTag backData = new CompoundTag();
        backData.putString("Action", "back_to_main");
        backItem.set(DataComponents.CUSTOM_DATA, CustomData.of(backData));
        
        container.setItem(45, backItem);
    }
    
    /**
     * Creates an employee display item
     */
    private ItemStack createEmployeeItem(ShopEmployeeManager.ShopEmployee employee, ServerPlayer viewer) {
        ItemStack item = new ItemStack(employee.isActive() ? Items.PLAYER_HEAD : Items.SKELETON_SKULL);
        
        item.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§b" + employee.getPlayerName()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Role: §f" + employee.getRole().getDisplayName()));
        lore.add(Component.literal("§7Status: " + (employee.isActive() ? "§aActive" : "§cInactive")));
        lore.add(Component.literal("§7Hired: §f" + DATE_FORMAT.format(new Date(employee.getHiredDate()))));
        
        // Show permissions
        lore.add(Component.literal(""));
        lore.add(Component.literal("§6Permissions:"));
        for (ShopEmployeeManager.ShopPermission perm : employee.getAllPermissions()) {
            lore.add(Component.literal("§8• §7" + perm.getDisplayName()));
        }
        
        // Add management options if viewer can manage this employee
        boolean canManage = shop.getOwnerId().equals(viewer.getUUID()) || 
            (shop.getEmployeeManager().hasPermission(viewer.getUUID(), 
                ShopEmployeeManager.ShopPermission.HIRE_EMPLOYEES) &&
             !employee.getPlayerId().equals(shop.getOwnerId()));
        
        if (canManage) {
            lore.add(Component.literal(""));
            lore.add(Component.literal("§eLeft-click: §7Edit role"));
            lore.add(Component.literal("§eRight-click: §7" + (employee.isActive() ? "Suspend" : "Activate")));
            lore.add(Component.literal("§eShift-click: §cRemove employee"));
        }
        
        item.set(DataComponents.LORE, new ItemLore(lore));
        
        // Store employee data
        CompoundTag employeeData = new CompoundTag();
        employeeData.putString("Action", "manage_employee");
        employeeData.putString("EmployeeId", employee.getPlayerId().toString());
        employeeData.putString("EmployeeName", employee.getPlayerName());
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(employeeData));
        
        return item;
    }
    
    /**
     * Opens the role selection interface
     */
    public void openRoleSelection(ServerPlayer player) {
        var container = new SimpleContainer(27); // 3 rows
        
        // Add role items
        int slot = 10;
        for (ShopEmployeeManager.EmployeeRole role : ShopEmployeeManager.EmployeeRole.values()) {
            if (role == ShopEmployeeManager.EmployeeRole.OWNER) continue; // Skip owner role
            
            ItemStack roleItem = createRoleItem(role);
            container.setItem(slot, roleItem);
            slot++;
            
            if (slot == 17) slot = 19; // Skip to next row
        }
        
        // Add border
        ItemStack borderItem = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
        borderItem.set(DataComponents.CUSTOM_NAME, Component.literal(" "));
        
        for (int i = 0; i < 27; i++) {
            if (container.getItem(i).isEmpty()) {
                container.setItem(i, borderItem);
            }
        }
        
        // Back button
        ItemStack backItem = new ItemStack(Items.BARRIER);
        backItem.set(DataComponents.CUSTOM_NAME, Component.literal("§c§lBack"));
        CompoundTag backData = new CompoundTag();
        backData.putString("Action", "back_to_employees");
        backItem.set(DataComponents.CUSTOM_DATA, CustomData.of(backData));
        container.setItem(18, backItem);
        
        player.openMenu(new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("§8[§6Roles§8] §f" + shop.getShopName());
            }
            
            @Override
            public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
                return new ShopEmployeeMenu(containerId, playerInventory, container, shop, shopManager);
            }
        });
    }
    
    /**
     * Creates a role display item
     */
    private ItemStack createRoleItem(ShopEmployeeManager.EmployeeRole role) {
        ItemStack item = new ItemStack(getRoleIcon(role));
        
        item.set(DataComponents.CUSTOM_NAME, 
            Component.literal("§6" + role.getDisplayName()));
        
        List<Component> lore = new ArrayList<>();
        lore.add(Component.literal("§7Priority: §f" + role.getPriority()));
        lore.add(Component.literal(""));
        lore.add(Component.literal("§6Permissions:"));
        
        for (ShopEmployeeManager.ShopPermission perm : role.getPermissions()) {
            lore.add(Component.literal("§8• §7" + perm.getDisplayName()));
        }
        
        item.set(DataComponents.LORE, new ItemLore(lore));
        
        return item;
    }
    
    /**
     * Gets the appropriate icon for a role
     */
    private Item getRoleIcon(ShopEmployeeManager.EmployeeRole role) {
        return switch (role) {
            case MANAGER -> Items.DIAMOND;
            case CASHIER -> Items.GOLD_INGOT;
            case STOCKER -> Items.CHEST;
            case SALES_ASSOCIATE -> Items.EMERALD;
            case VIEWER -> Items.BOOK;
            default -> Items.IRON_INGOT;
        };
    }
}
