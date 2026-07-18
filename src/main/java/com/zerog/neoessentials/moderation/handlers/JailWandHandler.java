package com.zerog.neoessentials.moderation.handlers;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.moderation.JailManager;
import com.zerog.neoessentials.moderation.JailSelectionManager;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Handles the configurable jail-region selection wand: right-click a block = corner 1,
 * left-click a block = corner 2 (WorldEdit-style), used by {@code /setjail} to create a cuboid
 * jail cell. Which item acts as the wand is configurable via
 * {@code moderation.jailSettings.wandItem} (default {@code minecraft:stick}).
 *
 * <p>The wand only records a selection — it never places/breaks blocks, and the left-click is
 * always canceled while holding it so it can't accidentally break the selected block.</p>
 */
@EventBusSubscriber(modid = "neoessentials")
public class JailWandHandler {

    private static boolean isWandItem(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key != null && key.toString().equals(ConfigManager.getJailWandItem());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!isWandItem(player.getItemInHand(InteractionHand.MAIN_HAND))) return;

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.jail.wand")) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.jail.wand.no_permission"));
            return;
        }

        event.setCanceled(true);
        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        BlockPos pos = event.getPos();
        String dimension = level.dimension().identifier().toString();
        JailSelectionManager.getInstance().setPos1(player.getUUID(), pos, dimension);
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.jail.wand.pos1_set",
            pos.getX(), pos.getY(), pos.getZ()));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClick(PlayerInteractEvent.LeftClickBlock event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!isWandItem(player.getItemInHand(InteractionHand.MAIN_HAND))) return;

        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.jail.wand")) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.jail.wand.no_permission"));
            return;
        }

        // Always cancel while holding the wand so left-click never actually breaks the block —
        // the wand is purely a selection tool, not a real tool.
        event.setCanceled(true);
        ServerLevel level = com.zerog.neoessentials.util.LevelCompat.of(player);
        BlockPos pos = event.getPos();
        String dimension = level.dimension().identifier().toString();
        JailSelectionManager.getInstance().setPos2(player.getUUID(), pos, dimension);
        player.sendSystemMessage(MessageUtil.success("commands.neoessentials.jail.wand.pos2_set",
            pos.getX(), pos.getY(), pos.getZ()));
    }
}
