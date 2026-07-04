package com.zerog.neoessentials.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.entity.player.Player;

/**
 * 26.1 port note: {@code CommandSourceStack.hasPermission(int)} and
 * {@code Player.hasPermissions(int)}/{@code hasPermission(int)} — the old OP-level
 * integer checks (0-4) — were removed entirely in favor of a granular
 * {@code PermissionSet}/{@code Permission} object model. {@link PermissionLevel#byId(int)}
 * maps 1:1 onto the old integer levels (0=ALL, 1=MODERATORS, 2=GAMEMASTERS, 3=ADMINS,
 * 4=OWNERS), so this just wraps that mapping to keep the rest of the codebase's
 * existing {@code hasPermission(N)} call sites working unchanged.
 */
public final class PermissionLevelCompat {

    private PermissionLevelCompat() {}

    public static boolean hasPermission(CommandSourceStack src, int level) {
        return src.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }

    public static boolean hasPermission(Player player, int level) {
        return player.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.byId(level)));
    }
}
