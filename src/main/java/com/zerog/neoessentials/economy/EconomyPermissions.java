package com.zerog.neoessentials.economy;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import java.util.UUID;

public class EconomyPermissions {
    // Permission nodes (keep in sync with nodes.txt)
    /** Allows viewing your own balance (balance, bal) */
    public static final String VIEW_BALANCE = "neoessentials.economy.view_balance";
    /** Allows paying another player (pay) */
    public static final String PAY = "neoessentials.economy.pay";
    /** Allows use of all /eco admin commands (give, take, set, view others' balance/history) */
    public static final String ADMIN = "neoessentials.economy.admin";
    /** Allows viewing the balance leaderboard (baltop) */
    public static final String LEADERBOARD = "neoessentials.economy.leaderboard";

    // Check if a player can view their balance
    public static boolean canViewBalance(UUID uuid) {
        return PermissionAPI.hasPermission(uuid, VIEW_BALANCE);
    }

    // Check if a player can pay another player
    public static boolean canPay(UUID uuid) {
        return PermissionAPI.hasPermission(uuid, PAY);
    }

    // Check if a player has admin economy permissions
    public static boolean isAdmin(UUID uuid) {
        return PermissionAPI.hasPermission(uuid, ADMIN);
    }

    // Check if a player can view the leaderboard
    public static boolean canViewLeaderboard(UUID uuid) {
        return PermissionAPI.hasPermission(uuid, LEADERBOARD);
    }
}