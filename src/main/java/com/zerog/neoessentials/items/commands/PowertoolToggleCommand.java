
package com.zerog.neoessentials.items.commands;
import java.util.HashMap;
import java.util.Map;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.util.MessageUtil;

public class PowertoolToggleCommand {
    // Server-side powertool toggles: player UUID -> item ID -> enabled
    private static final Map<java.util.UUID, Map<String, Boolean>> TOGGLES = new HashMap<>();
    /**
     * Register the /powertooltoggle and /pttoggle commands.
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!ConfigManager.getInstance().isCommandEnabled("powertooltoggle")) return;
        dispatcher.register(
            Commands.literal("powertooltoggle")
                .requires(cs -> cs.getEntity() instanceof ServerPlayer)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayer();
                    if (!com.zerog.neoessentials.api.permissions.PermissionAPI.hasPermission(player.getUUID(), "neoessentials.item.powertool.toggle")) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.no_permission"));
                        return 0;
                    }

                    ItemStack heldItem = player.getMainHandItem();
                    if (heldItem.isEmpty()) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.no_item"));
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

                    ItemStack heldItem = player.getMainHandItem();
                    if (heldItem.isEmpty()) {
                        ctx.getSource().sendFailure(MessageUtil.error("commands.neoessentials.powertool.no_item"));
                        return 0;
                    }

                    toggle(player);
                    ctx.getSource().sendSuccess(() -> MessageUtil.success("commands.neoessentials.powertool.toggle.success"), false);
                    return 1;
                })
        );
    }

    /**
     * Toggles powertool activation state for the item in the player's main hand.
     */
    public static void toggle(ServerPlayer player) {
        ItemStack heldItem = player.getMainHandItem();
        ResourceLocation itemKey = BuiltInRegistries.ITEM.getKey(heldItem.getItem());
        String itemId = itemKey.toString();

        Map<String, Boolean> map = TOGGLES.computeIfAbsent(player.getUUID(), k -> new HashMap<>());
        boolean enabled = map.getOrDefault(itemId, true);
        map.put(itemId, !enabled);
    }

    /**
     * Check if powertool is enabled for a specific item.
     */
    public static boolean isPowertoolEnabled(java.util.UUID playerUUID, String itemId) {
        Map<String, Boolean> playerToggles = TOGGLES.get(playerUUID);
        return playerToggles == null || playerToggles.getOrDefault(itemId, true);
    }

    /**
     * Set powertool enabled state for a specific item.
     */
    public static void setPowertoolEnabled(java.util.UUID playerUUID, String itemId, boolean enabled) {
        TOGGLES.computeIfAbsent(playerUUID, k -> new HashMap<>()).put(itemId, enabled);
    }
}
