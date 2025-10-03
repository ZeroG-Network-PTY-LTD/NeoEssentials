
package com.zerog.neoessentials.items.commands;
import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import com.zerog.neoessentials.config.ConfigUtil;
import com.zerog.neoessentials.util.MessageUtil;

public class powertooltoggle {
    // Server-side powertool toggles: player UUID -> slot -> enabled
    private static final Map<java.util.UUID, Map<Integer, Boolean>> TOGGLES = new HashMap<>();
    /**
     * Register the /powertooltoggle and /pttoggle commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigUtil.isCommandEnabled("powertooltoggle")) return;
        dispatcher.register(
            Commands.literal("powertooltoggle")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool.toggle")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    toggle(player);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.toggle.success"), false);
                    return 1;
                })
        );
        dispatcher.register(
            Commands.literal("pttoggle")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool.toggle")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }
                    toggle(player);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.toggle.success"), false);
                    return 1;
                })
        );
    }

    /**
     * Toggles powertool activation state for the item in the player's main hand using vanilla NBT.
     */
    public static void toggle(ServerPlayer player) {
        int slot = player.getInventory().selected;
        Map<Integer, Boolean> map = TOGGLES.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        boolean enabled = map.getOrDefault(slot, true);
        map.put(slot, !enabled);
    }

    /**
     * Check if powertool is enabled for a specific slot.
     */
    public static boolean isPowertoolEnabled(java.util.UUID playerUUID, int slot) {
        Map<Integer, Boolean> playerToggles = TOGGLES.get(playerUUID);
        return playerToggles == null || playerToggles.getOrDefault(slot, true);
    }

    /**
     * Set powertool enabled state for a specific slot.
     */
    public static void setPowertoolEnabled(java.util.UUID playerUUID, int slot, boolean enabled) {
        TOGGLES.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(slot, enabled);
    }
}
