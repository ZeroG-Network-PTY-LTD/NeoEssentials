package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.crates.gui.CrateOpeningMenu;
import com.zerog.neoessentials.crates.gui.CratePreviewMenu;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-clicking a physical crate block (registered via {@code /crate admin setblock}) with a
 *  key in hand — or a virtual key balance for that crate — opens it. Left-clicking it shows the
 *  no-cost reward-odds preview, same as {@code /crate preview}. */
@EventBusSubscriber(modid = "neoessentials")
public class CrateBlockInteractHandler {

    /** Left-click fires repeatedly while the mouse button is held (attack-swing timing, not a
     *  click-rate limiter) — without this, holding left-click on a crate block would reopen the
     *  preview GUI over and over. Same fix/rationale as {@code ShopInteractHandler}'s identical
     *  cooldown for shop sign left-clicks. */
    private static final long PREVIEW_COOLDOWN_MS = 400L;
    private static final java.util.Map<java.util.UUID, Long> lastPreviewMs = new java.util.concurrent.ConcurrentHashMap<>();

    private static boolean tryConsumePreviewCooldown(ServerPlayer player) {
        long now = System.currentTimeMillis();
        Long last = lastPreviewMs.get(player.getUUID());
        if (last != null && now - last < PREVIEW_COOLDOWN_MS) return false;
        lastPreviewMs.put(player.getUUID(), now);
        return true;
    }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (!ConfigManager.isCratesModuleEnabled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        // Sneak+left-click bypasses the preview entirely, letting the swing fall through as a
        // normal (uncanceled) attack/break attempt — otherwise the block could never actually
        // be broken via left-click. Same idiom as ShopInteractHandler's sign left-click.
        if (player.isShiftKeyDown()) return;

        CrateDefinition crate = CrateManager.getInstance().getCrateAt(level, event.getPos());
        if (crate == null) return;

        event.setCanceled(true);
        if (!tryConsumePreviewCooldown(player)) return;
        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.crate.preview")) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.general.no_permission"));
            return;
        }
        CratePreviewMenu.open(player, crate);
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ConfigManager.isCratesModuleEnabled()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;

        CrateDefinition crate = CrateManager.getInstance().getCrateAt(level, event.getPos());
        if (crate == null) return;

        event.setCanceled(true);
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!PermissionAPI.hasPermission(player.getUUID(), "neoessentials.crate.open")) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.general.no_permission"));
            return;
        }

        if (!CrateManager.getInstance().hasAnyReward(crate)) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.crate.no_rewards", crate.displayName));
            return;
        }

        ItemStack held = player.getMainHandItem();
        String heldCrateId = CrateManager.getInstance().getKeyCrateId(held);
        boolean hasVirtualKey = CrateKeyManager.getInstance().getKeys(player.getUUID(), crate.id) > 0;
        boolean hasPhysicalKey = crate.id.equalsIgnoreCase(heldCrateId);

        if (!hasVirtualKey && !hasPhysicalKey) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.crate.no_keys", crate.displayName));
            return;
        }

        CrateReward won = CrateManager.getInstance().tryConsumeKeyAndPick(player, crate, hasPhysicalKey ? held : null);
        if (won == null) {
            player.sendSystemMessage(MessageUtil.error("commands.neoessentials.crate.no_keys", crate.displayName));
            return;
        }
        CrateOpeningMenu.open(player, crate, won);
    }
}
