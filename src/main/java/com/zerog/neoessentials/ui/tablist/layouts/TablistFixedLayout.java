package com.zerog.neoessentials.ui.tablist.layouts;

/**
 * Implementation of a fixed tablist layout with predefined slots
 */
public class TablistFixedLayout implements TablistLayout {
    private int rows = 4;
    private int columns = 5;
    private boolean useVerticalOrder = false;
    
    /**
     * Gets the name of this layout
     */
    @Override
    public String getName() {
        return "Fixed Layout";
    }
    
    /**
     * Gets the type of this layout
     */
    @Override
    public LayoutType getType() {
        return LayoutType.FIXED;
    }
    
    /**
     * Gets the number of rows in this layout
     * @return The number of rows
     */
    public int getRows() {
        return rows;
    }
    
    /**
     * Sets the number of rows in this layout
     * @param rows The number of rows
     */
    public void setRows(int rows) {
        this.rows = Math.max(1, Math.min(20, rows));
    }
      /**
     * Gets the number of columns in this layout
     * @return The number of columns
     */
    public int getColumns() {
        return columns;
    }
    
    /**
     * Sets the number of columns in this layout
     * @param columns The number of columns
     */
    public void setColumns(int columns) {
        this.columns = Math.max(1, Math.min(10, columns));
    }
    
    /**
     * Gets whether this layout uses vertical ordering
     * @return True if vertical ordering is used, false for horizontal
     */
    public boolean isUsingVerticalOrder() {
        return useVerticalOrder;
    }
    
    /**
     * Sets whether this layout uses vertical ordering
     * @param useVerticalOrder True for vertical order, false for horizontal
     */
    public void setUseVerticalOrder(boolean useVerticalOrder) {
        this.useVerticalOrder = useVerticalOrder;
    }
    
    /**
     * Gets the slot index for a position in the grid
     * @param row The row (0-based)
     * @param col The column (0-based)
     * @return The slot index
     */
    public int getSlotIndex(int row, int col) {
        if (useVerticalOrder) {
            // Vertical order: fill columns first, then rows
            return col * rows + row;
        } else {
            // Horizontal order: fill rows first, then columns
            return row * columns + col;
        }
    }
      /**
     * Gets the total number of slots in the layout
     * @return The total number of slots
     */
    public int getTotalSlots() {
        return rows * columns;
    }
     */
    public void setColumns(int columns) {
        this.columns = Math.max(1, Math.min(20, columns));
    }
}
