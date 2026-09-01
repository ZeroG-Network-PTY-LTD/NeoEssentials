package com.zerog.neoessentials.crates;

import com.zerog.neoessentials.api.permissions.PermissionAPI;
import com.zerog.neoessentials.config.ConfigManager;
import com.zerog.neoessentials.crates.gui.CrateOpeningMenu;
import com.zerog.neoessentials.util.MessageUtil;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Right-clicking a physical crate block (registered via {@code /crate admin setblock}) with a
 *  key in hand — or a virtual key balance for that crate — opens it. */
@EventBusSubscriber(modid = "neoessentials")
public class CrateBlockInteractHandler {

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
