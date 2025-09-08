package com.zerog.neoessentials.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Map;

/**
 * Kit data structure for NeoEssentials
 * Represents a kit with items, commands, and configuration
 * 
 * @author ZeroG
 * @since 2.0.0
 */
public class Kit {
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("display_name")
    private String displayName;
    
    @SerializedName("description")
    private String description;
    
    @SerializedName("cooldown")
    private long cooldown; // in seconds
    
    @SerializedName("cost")
    private double cost;
    
    @SerializedName("permission")
    private String permission;
    
    @SerializedName("enabled")
    private boolean enabled = true;
    
    @SerializedName("one_time_only")
    private boolean oneTimeOnly = false;
    
    @SerializedName("items")
    private List<KitItem> items;
    
    @SerializedName("commands")
    private List<String> commands;
    
    @SerializedName("metadata")
    private Map<String, Object> metadata;
    
    @SerializedName("created_at")
    private long createdAt;
    
    @SerializedName("updated_at")
    private long updatedAt;
    
    @SerializedName("created_by")
    private String createdBy;
    
    // Constructors
    public Kit() {
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
    
    public Kit(String name, String displayName, String description) {
        this();
        this.name = name;
        this.displayName = displayName;
        this.description = description;
    }
    
    // Getters and Setters
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        updateTimestamp();
    }
    
    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
        updateTimestamp();
    }
    
    public String getDescription() {
        return description != null ? description : "";
    }
    
    public void setDescription(String description) {
        this.description = description;
        updateTimestamp();
    }
    
    public long getCooldown() {
        return cooldown;
    }
    
    public void setCooldown(long cooldown) {
        this.cooldown = cooldown;
        updateTimestamp();
    }
    
    public double getCost() {
        return cost;
    }
    
    public void setCost(double cost) {
        this.cost = cost;
        updateTimestamp();
    }
    
    public String getPermission() {
        return permission;
    }
    
    public void setPermission(String permission) {
        this.permission = permission;
        updateTimestamp();
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        updateTimestamp();
    }
    
    public boolean isOneTimeOnly() {
        return oneTimeOnly;
    }
    
    public void setOneTimeOnly(boolean oneTimeOnly) {
        this.oneTimeOnly = oneTimeOnly;
        updateTimestamp();
    }
    
    public List<KitItem> getItems() {
        return items;
    }
    
    public void setItems(List<KitItem> items) {
        this.items = items;
        updateTimestamp();
    }
    
    public List<String> getCommands() {
        return commands;
    }
    
    public void setCommands(List<String> commands) {
        this.commands = commands;
        updateTimestamp();
    }
    
    public Map<String, Object> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
        updateTimestamp();
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public long getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    // Utility methods
    private void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }
    
    /**
     * Check if the kit has any items
     */
    public boolean hasItems() {
        return items != null && !items.isEmpty();
    }
    
    /**
     * Check if the kit has any commands
     */
    public boolean hasCommands() {
        return commands != null && !commands.isEmpty();
    }
    
    /**
     * Get cooldown in milliseconds
     */
    public long getCooldownMillis() {
        return cooldown * 1000;
    }
    
    /**
     * Check if this kit requires permission
     */
    public boolean requiresPermission() {
        return permission != null && !permission.trim().isEmpty();
    }
    
    /**
     * Check if this kit has a cost
     */
    public boolean hasCost() {
        return cost > 0;
    }
    
    @Override
    public String toString() {
        return String.format("Kit{name='%s', displayName='%s', enabled=%s, cooldown=%d, cost=%.2f}", 
                name, displayName, enabled, cooldown, cost);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Kit kit = (Kit) obj;
        return name != null && name.equals(kit.name);
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
}
