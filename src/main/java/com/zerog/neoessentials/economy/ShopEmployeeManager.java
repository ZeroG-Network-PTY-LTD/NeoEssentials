package com.zerog.neoessentials.economy;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages shop employee roles, permissions, and access control.
 * Supports hierarchical permissions for shop management.
 */
public class ShopEmployeeManager {
    
    public enum EmployeeRole {
        OWNER("Owner", 100, EnumSet.allOf(ShopPermission.class)),
        MANAGER("Manager", 80, EnumSet.of(
            ShopPermission.MANAGE_INVENTORY,
            ShopPermission.SET_PRICES,
            ShopPermission.HIRE_EMPLOYEES,
            ShopPermission.VIEW_SALES,
            ShopPermission.VIEW_STATS,
            ShopPermission.MODIFY_SHOP_INFO,
            ShopPermission.PROCESS_TRANSACTIONS
        )),
        CASHIER("Cashier", 60, EnumSet.of(
            ShopPermission.MANAGE_INVENTORY,
            ShopPermission.VIEW_SALES,
            ShopPermission.PROCESS_TRANSACTIONS
        )),
        STOCKER("Stocker", 40, EnumSet.of(
            ShopPermission.MANAGE_INVENTORY,
            ShopPermission.VIEW_INVENTORY
        )),
        SALES_ASSOCIATE("Sales Associate", 30, EnumSet.of(
            ShopPermission.VIEW_INVENTORY,
            ShopPermission.VIEW_SALES,
            ShopPermission.PROCESS_TRANSACTIONS
        )),
        VIEWER("Viewer", 10, EnumSet.of(
            ShopPermission.VIEW_INVENTORY,
            ShopPermission.VIEW_BASIC_INFO
        ));
        
        private final String displayName;
        private final int priority;
        private final Set<ShopPermission> permissions;
        
        EmployeeRole(String displayName, int priority, Set<ShopPermission> permissions) {
            this.displayName = displayName;
            this.priority = priority;
            this.permissions = permissions;
        }
        
        public String getDisplayName() { return displayName; }
        public int getPriority() { return priority; }
        public Set<ShopPermission> getPermissions() { return permissions; }
        
        public boolean hasPermission(ShopPermission permission) {
            return permissions.contains(permission);
        }
        
        public boolean canManage(EmployeeRole other) {
            return this.priority >= other.priority;
        }
    }
    
    public enum ShopPermission {
        // Inventory Management
        MANAGE_INVENTORY("Manage Inventory", "Add, remove, and modify shop inventory"),
        VIEW_INVENTORY("View Inventory", "View current shop inventory"),
        SET_PRICES("Set Prices", "Modify item prices and bulk pricing"),
        
        // Employee Management
        HIRE_EMPLOYEES("Hire Employees", "Add new employees to the shop"),
        FIRE_EMPLOYEES("Fire Employees", "Remove employees from the shop"),
        CHANGE_ROLES("Change Roles", "Modify employee roles and permissions"),
        
        // Financial Access
        VIEW_SALES("View Sales", "Access sales history and revenue data"),
        VIEW_STATS("View Statistics", "Access detailed shop analytics"),
        WITHDRAW_FUNDS("Withdraw Funds", "Remove money from shop account"),
        
        // Shop Management
        MODIFY_SHOP_INFO("Modify Shop Info", "Change shop name, description, category"),
        DELETE_SHOP("Delete Shop", "Permanently delete the shop"),
        TRANSFER_OWNERSHIP("Transfer Ownership", "Transfer shop ownership to another player"),
        
        // Operations
        PROCESS_TRANSACTIONS("Process Transactions", "Handle buy/sell transactions"),
        MANAGE_DISCOUNTS("Manage Discounts", "Set up sales and discount rates"),
        
        // Basic Access
        VIEW_BASIC_INFO("View Basic Info", "View shop name, location, and basic details");
        
        private final String displayName;
        private final String description;
        
        ShopPermission(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }
        
        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
    }
    
    public static class ShopEmployee {
        private final UUID playerId;
        private final String playerName;
        private EmployeeRole role;
        private final Set<ShopPermission> customPermissions;
        private final long hiredDate;
        private final UUID hiredBy;
        private boolean isActive;
        private String notes;
        
        public ShopEmployee(UUID playerId, String playerName, EmployeeRole role, UUID hiredBy) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.role = role;
            this.customPermissions = new HashSet<>();
            this.hiredDate = System.currentTimeMillis();
            this.hiredBy = hiredBy;
            this.isActive = true;
            this.notes = "";
        }
        
        public boolean hasPermission(ShopPermission permission) {
            return customPermissions.contains(permission) || role.hasPermission(permission);
        }
        
        public void grantPermission(ShopPermission permission) {
            customPermissions.add(permission);
        }
        
        public void revokePermission(ShopPermission permission) {
            customPermissions.remove(permission);
        }
        
        public Set<ShopPermission> getAllPermissions() {
            Set<ShopPermission> allPermissions = new HashSet<>(role.getPermissions());
            allPermissions.addAll(customPermissions);
            return allPermissions;
        }
        
        // Getters and setters
        public UUID getPlayerId() { return playerId; }
        public String getPlayerName() { return playerName; }
        public EmployeeRole getRole() { return role; }
        public void setRole(EmployeeRole role) { this.role = role; }
        public Set<ShopPermission> getCustomPermissions() { return new HashSet<>(customPermissions); }
        public long getHiredDate() { return hiredDate; }
        public UUID getHiredBy() { return hiredBy; }
        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { this.isActive = active; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes != null ? notes : ""; }
    }
    
    private final UUID shopId;
    private final Map<UUID, ShopEmployee> employees; // Player ID -> Employee
    
    public ShopEmployeeManager(UUID shopId) {
        this.shopId = shopId;
        this.employees = new ConcurrentHashMap<>();
    }
    
    /**
     * Add an employee to the shop
     * 
     * @param employeeId The employee's UUID
     * @param employeeName The employee's name
     * @param role The role to assign
     * @param hiredBy Who hired this employee
     * @return true if employee was added successfully
     */
    public boolean addEmployee(UUID employeeId, String employeeName, EmployeeRole role, UUID hiredBy) {
        if (employees.containsKey(employeeId)) {
            return false; // Employee already exists
        }
        
        ShopEmployee employee = new ShopEmployee(employeeId, employeeName, role, hiredBy);
        employees.put(employeeId, employee);
        return true;
    }
    
    /**
     * Remove an employee from the shop
     * 
     * @param employeeId The employee to remove
     * @param removedBy Who is removing the employee
     * @return true if employee was removed successfully
     */
    public boolean removeEmployee(UUID employeeId, UUID removedBy) {
        ShopEmployee employee = employees.get(employeeId);
        if (employee == null) {
            return false;
        }
        
        // Can't remove the owner
        if (employee.getRole() == EmployeeRole.OWNER) {
            return false;
        }
        
        employee.setActive(false);
        employees.remove(employeeId);
        return true;
    }
    
    /**
     * Change an employee's role
     * 
     * @param employeeId The employee whose role to change
     * @param newRole The new role
     * @param changedBy Who is making the change
     * @return true if role was changed successfully
     */
    public boolean changeEmployeeRole(UUID employeeId, EmployeeRole newRole, UUID changedBy) {
        ShopEmployee employee = employees.get(employeeId);
        if (employee == null || !employee.isActive()) {
            return false;
        }
        
        // Can't change owner role
        if (employee.getRole() == EmployeeRole.OWNER) {
            return false;
        }
        
        employee.setRole(newRole);
        return true;
    }
    
    /**
     * Check if a player has a specific permission in this shop
     * 
     * @param playerId The player to check
     * @param permission The permission to check
     * @return true if player has the permission
     */
    public boolean hasPermission(UUID playerId, ShopPermission permission) {
        ShopEmployee employee = employees.get(playerId);
        if (employee == null || !employee.isActive()) {
            return false;
        }
        
        return employee.hasPermission(permission);
    }
    
    /**
     * Check if a player can manage another employee
     * 
     * @param managerId The manager's UUID
     * @param targetId The target employee's UUID
     * @return true if manager can manage the target
     */
    public boolean canManageEmployee(UUID managerId, UUID targetId) {
        ShopEmployee manager = employees.get(managerId);
        ShopEmployee target = employees.get(targetId);
        
        if (manager == null || target == null || !manager.isActive() || !target.isActive()) {
            return false;
        }
        
        return manager.getRole().canManage(target.getRole());
    }
    
    /**
     * Get an employee by ID
     * 
     * @param employeeId The employee's UUID
     * @return The employee, or null if not found
     */
    public ShopEmployee getEmployee(UUID employeeId) {
        return employees.get(employeeId);
    }
    
    /**
     * Get all active employees
     * 
     * @return List of active employees
     */
    public List<ShopEmployee> getActiveEmployees() {
        return employees.values().stream()
                .filter(ShopEmployee::isActive)
                .sorted((e1, e2) -> Integer.compare(e2.getRole().getPriority(), e1.getRole().getPriority()))
                .toList();
    }
    
    /**
     * Get all employees (active and inactive)
     * 
     * @return List of all employees
     */
    public List<ShopEmployee> getAllEmployees() {
        return employees.values().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getRole().getPriority(), e1.getRole().getPriority()))
                .toList();
    }
    
    /**
     * Get employees with a specific role
     * 
     * @param role The role to filter by
     * @return List of employees with the specified role
     */
    public List<ShopEmployee> getEmployeesByRole(EmployeeRole role) {
        return employees.values().stream()
                .filter(emp -> emp.isActive() && emp.getRole() == role)
                .toList();
    }
    
    /**
     * Get the shop owner
     * 
     * @return The shop owner, or null if not found
     */
    public ShopEmployee getOwner() {
        return employees.values().stream()
                .filter(emp -> emp.getRole() == EmployeeRole.OWNER)
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if a player is an employee of this shop
     * 
     * @param playerId The player to check
     * @return true if player is an employee
     */
    public boolean isEmployee(UUID playerId) {
        ShopEmployee employee = employees.get(playerId);
        return employee != null && employee.isActive();
    }
    
    /**
     * Get employee count by role
     * 
     * @return Map of role to count
     */
    public Map<EmployeeRole, Integer> getEmployeeCountByRole() {
        Map<EmployeeRole, Integer> counts = new HashMap<>();
        for (EmployeeRole role : EmployeeRole.values()) {
            counts.put(role, 0);
        }
        
        employees.values().stream()
                .filter(ShopEmployee::isActive)
                .forEach(emp -> counts.merge(emp.getRole(), 1, Integer::sum));
        
        return counts;
    }
    
    /**
     * Grant a custom permission to an employee
     * 
     * @param employeeId The employee's UUID
     * @param permission The permission to grant
     * @param grantedBy Who is granting the permission
     * @return true if permission was granted
     */
    public boolean grantPermission(UUID employeeId, ShopPermission permission, UUID grantedBy) {
        ShopEmployee employee = employees.get(employeeId);
        if (employee == null || !employee.isActive()) {
            return false;
        }
        
        employee.grantPermission(permission);
        return true;
    }
    
    /**
     * Revoke a custom permission from an employee
     * 
     * @param employeeId The employee's UUID
     * @param permission The permission to revoke
     * @param revokedBy Who is revoking the permission
     * @return true if permission was revoked
     */
    public boolean revokePermission(UUID employeeId, ShopPermission permission, UUID revokedBy) {
        ShopEmployee employee = employees.get(employeeId);
        if (employee == null || !employee.isActive()) {
            return false;
        }
        
        employee.revokePermission(permission);
        return true;
    }
    
    /**
     * Transfer shop ownership to another employee
     * 
     * @param newOwnerId The new owner's UUID
     * @param currentOwnerId The current owner's UUID
     * @return true if ownership was transferred
     */
    public boolean transferOwnership(UUID newOwnerId, UUID currentOwnerId) {
        ShopEmployee currentOwner = employees.get(currentOwnerId);
        ShopEmployee newOwner = employees.get(newOwnerId);
        
        if (currentOwner == null || newOwner == null || 
            currentOwner.getRole() != EmployeeRole.OWNER || 
            !newOwner.isActive()) {
            return false;
        }
        
        // Change current owner to manager
        currentOwner.setRole(EmployeeRole.MANAGER);
        
        // Promote new owner
        newOwner.setRole(EmployeeRole.OWNER);
        
        return true;
    }
    
    public UUID getShopId() { return shopId; }
    public int getEmployeeCount() { return (int) employees.values().stream().filter(ShopEmployee::isActive).count(); }
}
