package com.zerog.neoessentials.ui.economy.base;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Base class for professional chest-based economy GUIs
 * Provides common functionality for creating interactive inventory interfaces
 * 
 * @author ZeroG
 * @since 1.0.2.134
 */
public abstract class BaseEconomyChestGUI extends ChestMenu {
    
    protected final ServerPlayer player;
    protected final SimpleContainer container;
    protected final Component title;
    
    // GUI Size Constants
    public static final int SMALL_GUI_SIZE = 27; // 3 rows
    public static final int MEDIUM_GUI_SIZE = 45; // 5 rows  
    public static final int LARGE_GUI_SIZE = 54; // 6 rows
    
    // Common slot positions for consistent layout
    protected static final int[] BORDER_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,          // Top row
        9, 17, 18, 26, 27, 35, 36, 44,      // Sides
        45, 46, 47, 48, 49, 50, 51, 52, 53  // Bottom row (for large GUI)
    };
    
    protected static final int[] SMALL_BORDER_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,          // Top row
        9, 17,                               // Sides
        18, 19, 20, 21, 22, 23, 24, 25, 26  // Bottom row
    };
    
    protected static final int[] MEDIUM_BORDER_SLOTS = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,          // Top row
        9, 17, 18, 26, 27, 35,               // Sides
        36, 37, 38, 39, 40, 41, 42, 43, 44  // Bottom row
    };
    
    // Common items for GUI decoration
    protected static final ItemStack BORDER_ITEM = createGuiItem(Items.GRAY_STAINED_GLASS_PANE, " ");
    protected static final ItemStack CLOSE_ITEM = createGuiItem(Items.BARRIER, "§cClose");
    protected static final ItemStack BACK_ITEM = createGuiItem(Items.ARROW, "§6← Back");
    protected static final ItemStack INFO_ITEM = createGuiItem(Items.BOOK, "§eInformation");
    
    public BaseEconomyChestGUI(MenuType<?> menuType, int containerId, Inventory playerInventory, 
                              SimpleContainer container, Component title, ServerPlayer player) {
        super(menuType, containerId, playerInventory, container, container.getContainerSize() / 9);
        this.player = player;
        this.container = container;
        this.title = title;
        
        setupGUI();
    }
    
    /**
     * Creates a GUI item with custom name and no interaction
     */
    protected static ItemStack createGuiItem(net.minecraft.world.item.Item item, String name) {
        ItemStack stack = new ItemStack(item);
        stack.setDisplayName(Component.literal(name));
        return stack;
    }
    
    /**
     * Creates a GUI item with custom name and lore
     */
    protected static ItemStack createGuiItem(net.minecraft.world.item.Item item, String name, String... lore) {
        ItemStack stack = createGuiItem(item, name);
        return stack;
    }
    
    /**
     * Sets up the basic GUI layout - to be implemented by subclasses
     */
    protected abstract void setupGUI();
    
    /**
     * Handles click events for GUI items
     */
    protected abstract void handleClick(int slot, ItemStack clickedItem);
    
    /**
     * Fills border slots with decorative items
     */
    protected void fillBorder() {
        int[] borderSlots = getBorderSlots();
        for (int slot : borderSlots) {
            if (slot < container.getContainerSize()) {
                container.setItem(slot, BORDER_ITEM.copy());
            }
        }
    }
    
    /**
     * Gets the appropriate border slots based on container size
     */
    protected int[] getBorderSlots() {
        return switch (container.getContainerSize()) {
            case SMALL_GUI_SIZE -> SMALL_BORDER_SLOTS;
            case MEDIUM_GUI_SIZE -> MEDIUM_BORDER_SLOTS;
            case LARGE_GUI_SIZE -> BORDER_SLOTS;
            default -> SMALL_BORDER_SLOTS;
        };
    }
    
    /**
     * Adds a close button in the bottom right corner
     */
    protected void addCloseButton() {
        int closeSlot = container.getContainerSize() - 1;
        container.setItem(closeSlot, CLOSE_ITEM.copy());
    }
    
    /**
     * Adds a back button in the bottom left corner
     */
    protected void addBackButton() {
        int backSlot = container.getContainerSize() - 9;
        container.setItem(backSlot, BACK_ITEM.copy());
    }
    
    /**
     * Prevents players from taking items from the GUI
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
    
    /**
     * Handle container clicks - delegates to handleClick method
     */
    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < container.getContainerSize()) {
            ItemStack clickedItem = container.getItem(slotId);
            handleClick(slotId, clickedItem);
        }
        // Don't call super to prevent item movement
    }
    
    /**
     * Updates the GUI content - to be implemented by subclasses for dynamic content
     */
    protected void updateGUI() {
        // Default implementation does nothing
        // Subclasses can override for dynamic updates
    }
    
    /**
     * Utility method to center text for GUI items
     */
    protected static String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int spaces = (width - text.length()) / 2;
        return " ".repeat(spaces) + text;
    }
    
    /**
     * Creates a money display item showing the player's balance
     */
    protected ItemStack createMoneyDisplayItem(double balance) {
        return createGuiItem(Items.GOLD_INGOT, 
            "§6§lYour Balance", 
            "§e$" + String.format("%.2f", balance),
            "",
            "§7Your current money balance"
        );
    }
}
